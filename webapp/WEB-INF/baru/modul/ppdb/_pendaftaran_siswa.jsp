<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.sekolah.GelombangPendaftaranPsb"%>
<%@page import="ais.database.model.sekolah.PaketPsb"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================
    // BLOK AUTO-LOGIN (Terpicu otomatis jika ada parameter siswa_id setelah simpan)
    // =========================================================================
    String idSiswaStr = request.getParameter("siswa_id");
    if (idSiswaStr != null && !idSiswaStr.trim().isEmpty()) {
        try {
            CalonSiswa siswaLogin = (CalonSiswa) GeneralValueObject.ambilData(CalonSiswa.class, idSiswaStr.trim(), true);
            if (siswaLogin != null) {
                Common.setLogin(request, response, siswaLogin);
                out.print("<script>");
                out.print("window.location.replace('" + Common.ROOT + "/ppdb');");
                out.print("</script>");
                return; // Hentikan eksekusi JSP agar langsung berpindah halaman
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_pendaftaran_siswa.jsp:26");
        }
    }
    // =========================================================================

    // INISIALISASI HALAMAN FORMULIR PENDAFTARAN
    String rnd = Common.getGeneratedBarCode(7);
    String gelombangId = request.getParameter("gelombangId");
    String siswaId = request.getParameter("siswaId");
    
    boolean isEdit = (siswaId != null && !siswaId.trim().isEmpty());
    CalonSiswa calonSiswa = new CalonSiswa();
    GelombangPendaftaranPsb gelombang = null;

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        if (isEdit) {
            calonSiswa = (CalonSiswa) GeneralValueObject.ambilData(CalonSiswa.class, siswaId, true);
            if(calonSiswa != null) gelombang = calonSiswa.getGelombangPendaftaranPsb();
        } 
        else if (gelombangId != null && !gelombangId.trim().isEmpty()) {
            gelombang = (GelombangPendaftaranPsb) GeneralValueObject.ambilData(GelombangPendaftaranPsb.class, gelombangId, true);
        }
        
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_pendaftaran_siswa.jsp:52");
    } finally {
        if (sess != null) {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_pendaftaran_siswa.jsp:55");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_pendaftaran_siswa.jsp:56");}
        }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>

<style>
    .form-section-<%=rnd%> { background: #fff; padding: 2rem; border-radius: 1rem; box-shadow: 0 .125rem .25rem rgba(0,0,0,.075); margin-top: 1rem; }
    .nav-tabs-custom-<%=rnd%> .nav-link { font-weight: 600; color: #6c757d; border: none; border-bottom: 3px solid transparent; transition: all 0.3s; padding: 1rem 1.5rem; }
    .nav-tabs-custom-<%=rnd%> .nav-link:hover { color: #0d6efd; border-bottom: 3px solid #dee2e6; }
    .nav-tabs-custom-<%=rnd%> .nav-link.active { color: #0d6efd; border-bottom: 3px solid #0d6efd; background: transparent; }
    .floating-label-custom-<%=rnd%> label { font-size: 0.85rem; font-weight: bold; color: #495057; }
</style>

<div class="container-fluid py-3">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h4 class="fw-bold text-primary mb-0">
            <i class="fas fa-user-edit me-2"></i> 
            <%= isEdit ? Common.getBahasaConfig("Ubah Data Peserta Didik") : Common.getBahasaConfig("Formulir Pendaftaran Peserta Didik Baru") %>
        </h4>
        <% if (gelombang != null) { %>
            <span class="badge bg-info text-dark px-3 py-2 fs-6 rounded-pill shadow-sm"><i class="fas fa-bookmark me-1"></i> <%= gelombang.getNama() %></span>
        <% } %>
    </div>

    <ul class="nav nav-tabs nav-tabs-custom-<%=rnd%> border-bottom-0" id="tabPendaftaran<%=rnd%>" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tabDiri<%=rnd%>" type="button" role="tab"><i class="fas fa-user me-2"></i><%= Common.getBahasaConfig("Data Diri") %></button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabKeluarga<%=rnd%>" type="button" role="tab"><i class="fas fa-users me-2"></i><%= Common.getBahasaConfig("Data Keluarga") %></button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabSekolah<%=rnd%>" type="button" role="tab"><i class="fas fa-school me-2"></i><%= Common.getBahasaConfig("Asal & Tujuan Sekolah") %></button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabLainnya<%=rnd%>" type="button" role="tab"><i class="fas fa-file-alt me-2"></i><%= Common.getBahasaConfig("Kesejahteraan & Lainnya") %></button>
        </li>
    </ul>

    <form id="formPendaftaranSiswa<%=rnd%>">
        <input type="hidden" name="id" value="<%= isEdit ? calonSiswa.getId() : "" %>">
        <input type="hidden" name="gelombangPendaftaranPsb.id" value="<%= gelombang != null ? gelombang.getId() : "" %>">

        <div class="tab-content" id="tabPendaftaranContent<%=rnd%>">
            
            <div class="tab-pane fade show active form-section-<%=rnd%> border border-top-0 rounded-top-0" id="tabDiri<%=rnd%>" role="tabpanel">
                <h5 class="fw-bold border-bottom pb-2 mb-4 text-secondary"><%= Common.getBahasaConfig("Informasi Pribadi Peserta Didik") %></h5>
                
                <div class="row g-3 floating-label-custom-<%=rnd%>">
                    <div class="col-md-3">
                        <label class="form-label"><%= Common.getBahasaConfig("No Registrasi") %></label>
                        <input type="text" class="form-control bg-light" name="noRegistrasi" value="<%= calonSiswa.getNoRegistrasi() != null ? calonSiswa.getNoRegistrasi() : "" %>" readonly placeholder="<%= Common.getBahasaConfig("Otomatis") %>">
                    </div>
                    <div class="col-md-5">
                        <label class="form-label"><%= Common.getBahasaConfig("Nama Lengkap") %> <span class="text-danger">*</span></label>
                        <input type="text" class="form-control border-primary" name="namaSiswa" value="<%= calonSiswa.getNamaSiswa() != null ? calonSiswa.getNamaSiswa() : "" %>" required>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label"><%= Common.getBahasaConfig("Nama Panggilan") %></label>
                        <input type="text" class="form-control" name="panggilan" value="<%= calonSiswa.getPanggilan() != null ? calonSiswa.getPanggilan() : "" %>">
                    </div>

                    <div class="col-md-4">
                        <label class="form-label"><%= Common.getBahasaConfig("Tempat Lahir") %></label>
                        <input type="text" class="form-control" name="tempatLahir" value="<%= calonSiswa.getTempatLahir() != null ? calonSiswa.getTempatLahir() : "" %>">
                    </div>
                    <div class="col-md-4">
                        <label class="form-label"><%= Common.getBahasaConfig("Tanggal Lahir") %></label>
                        <input type="date" class="form-control" name="tanggalLahir" value="<%= calonSiswa.getTanggalLahir() != null ? Common.dateFormat41.get().format(calonSiswa.getTanggalLahir()) : "" %>">
                    </div>
                    <div class="col-md-4">
                        <label class="form-label"><%= Common.getBahasaConfig("Jenis Kelamin") %></label>
                        <select class="form-select" name="jenisKelamin">
                            <option value=""><%= Common.getBahasaConfig("Pilih Jenis Kelamin") %></option>
                            <option value="L" <%= "Laki-laki".equalsIgnoreCase(calonSiswa.getJenisKelamin()) ? "selected" : "" %>><%= Common.getBahasaConfig("Laki-Laki") %></option>
                            <option value="P" <%= "Perempuan".equalsIgnoreCase(calonSiswa.getJenisKelamin()) ? "selected" : "" %>><%= Common.getBahasaConfig("Perempuan") %></option>
                        </select>
                    </div>

                    <div class="col-md-3">
                        <label class="form-label"><%= Common.getBahasaConfig("Agama") %></label>
                        <input type="hidden" name="agama.id" value="<%= calonSiswa.getAgama() != null ? calonSiswa.getAgama().getId() : "" %>">
                        <input type="text" class="form-control" value="<%= calonSiswa.getAgama() != null ? calonSiswa.getAgama().getNama() : "" %>">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label"><%= Common.getBahasaConfig("Kewarganegaraan") %></label>
                        <input type="text" class="form-control" name="kewarganegaraan" value="<%= calonSiswa.getKewarganegaraan() != null ? calonSiswa.getKewarganegaraan() : "" %>">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label"><%= Common.getBahasaConfig("Negara") %></label>
                        <input type="hidden" name="negara.id" value="<%= calonSiswa.getNegara() != null ? calonSiswa.getNegara().getId() : "" %>">
                        <input type="text" class="form-control" value="<%= calonSiswa.getNegara() != null ? calonSiswa.getNegara().getNama() : "" %>">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label"><%= Common.getBahasaConfig("Golongan Darah") %></label>
                        <input type="text" class="form-control" name="golonganDarah" value="<%= calonSiswa.getGolonganDarah() != null ? calonSiswa.getGolonganDarah() : "" %>">
                    </div>

                    <div class="col-md-3">
                        <label class="form-label"><%= Common.getBahasaConfig("Anak Ke") %></label>
                        <input type="number" class="form-control" name="anakKe" value="<%= calonSiswa.getAnakKe() != null ? calonSiswa.getAnakKe() : "" %>">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label"><%= Common.getBahasaConfig("Dari Jumlah Anak") %></label>
                        <input type="number" class="form-control" name="dariAnakKe" value="<%= calonSiswa.getDariAnakKe() != null ? calonSiswa.getDariAnakKe() : "" %>">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label"><%= Common.getBahasaConfig("Jml Saudara Kandung") %></label>
                        <input type="number" class="form-control" name="jumlahSaudaraKandung" value="<%= calonSiswa.getJumlahSaudaraKandung() != null ? calonSiswa.getJumlahSaudaraKandung() : "" %>">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label"><%= Common.getBahasaConfig("Jml Saudara Tiri") %></label>
                        <input type="number" class="form-control" name="jumlahSaudaraTiri" value="<%= calonSiswa.getJumlahSaudaraTiri() != null ? calonSiswa.getJumlahSaudaraTiri() : "" %>">
                    </div>

                    <div class="col-md-4">
                        <label class="form-label"><%= Common.getBahasaConfig("Nomor Induk Siswa Nasional (NISN)") %></label>
                        <input type="text" class="form-control" name="nomorIndukNasional" value="<%= calonSiswa.getNomorIndukNasional() != null ? calonSiswa.getNomorIndukNasional() : "" %>">
                    </div>
                    <div class="col-md-4">
                        <label class="form-label"><%= Common.getBahasaConfig("NIK / No KTP Siswa") %></label>
                        <input type="text" class="form-control" name="nik" value="<%= calonSiswa.getNik() != null ? calonSiswa.getNik() : "" %>">
                    </div>
                    <div class="col-md-4">
                        <label class="form-label"><%= Common.getBahasaConfig("Nomor Akta Kelahiran") %></label>
                        <input type="text" class="form-control" name="noAktaKelahiran" value="<%= calonSiswa.getNoAktaKelahiran() != null ? calonSiswa.getNoAktaKelahiran() : "" %>">
                    </div>

                    <div class="col-md-12">
                        <label class="form-label"><%= Common.getBahasaConfig("Alamat Tempat Tinggal") %></label>
                        <textarea class="form-control" name="alamatSiswa" rows="2"><%= calonSiswa.getAlamatSiswa() != null ? calonSiswa.getAlamatSiswa() : "" %></textarea>
                    </div>

                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Dusun / Kampung") %></label><input type="text" class="form-control" name="dusunCalon" value="<%= calonSiswa.getDusunCalon() != null ? calonSiswa.getDusunCalon() : "" %>"></div>
                    <div class="col-md-2"><label class="form-label"><%= Common.getBahasaConfig("RT") %></label><input type="text" class="form-control" name="rt" value="<%= calonSiswa.getRt() != null ? calonSiswa.getRt() : "" %>"></div>
                    <div class="col-md-2"><label class="form-label"><%= Common.getBahasaConfig("RW") %></label><input type="text" class="form-control" name="rw" value="<%= calonSiswa.getRw() != null ? calonSiswa.getRw() : "" %>"></div>
                    <div class="col-md-5"><label class="form-label"><%= Common.getBahasaConfig("Desa / Kelurahan") %></label><input type="text" class="form-control" name="kelurahanCalon" value="<%= calonSiswa.getKelurahanCalon() != null ? calonSiswa.getKelurahanCalon() : "" %>"></div>

                    <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Kecamatan") %></label><input type="hidden" name="kecamatanCalon.id" value="<%= calonSiswa.getKecamatanCalon() != null ? calonSiswa.getKecamatanCalon().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getKecamatanCalon() != null ? calonSiswa.getKecamatanCalon().getNama() : "" %>"></div>
                    <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Kabupaten / Kota") %></label><input type="hidden" name="kotaCalon.id" value="<%= calonSiswa.getKotaCalon() != null ? calonSiswa.getKotaCalon().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getKotaCalon() != null ? calonSiswa.getKotaCalon().getNama() : "" %>"></div>
                    <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Propinsi") %></label><input type="hidden" name="propinsiCalon.id" value="<%= calonSiswa.getPropinsiCalon() != null ? calonSiswa.getPropinsiCalon().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPropinsiCalon() != null ? calonSiswa.getPropinsiCalon().getNama() : "" %>"></div>

                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Kode Pos") %></label><input type="text" class="form-control" name="kodePos" value="<%= calonSiswa.getKodePos() != null ? calonSiswa.getKodePos() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Telepon / HP Siswa") %></label><input type="text" class="form-control" name="teleponSiswa" value="<%= calonSiswa.getTeleponSiswa() != null ? calonSiswa.getTeleponSiswa() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Alamat Email") %></label><input type="email" class="form-control" name="alamatEmail" value="<%= calonSiswa.getAlamatEmail() != null ? calonSiswa.getAlamatEmail() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Koordinat Lokasi") %></label><input type="text" class="form-control" name="koordinat" placeholder="-7.xxx, 110.xxx" value="<%= calonSiswa.getKoordinat() != null ? calonSiswa.getKoordinat() : "" %>"></div>
                    
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Jenis Tinggal Siswa") %></label><input type="text" class="form-control" name="jenisTinggal" value="<%= calonSiswa.getJenisTinggal() != null ? calonSiswa.getJenisTinggal() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Alat Transportasi Siswa") %></label><input type="text" class="form-control" name="alatTransportasi" value="<%= calonSiswa.getAlatTransportasi() != null ? calonSiswa.getAlatTransportasi() : "" %>"></div>
                    <div class="col-md-2"><label class="form-label"><%= Common.getBahasaConfig("Berat Badan (kg)") %></label><input type="number" class="form-control" name="berat" value="<%= calonSiswa.getBerat() != null ? calonSiswa.getBerat() : "" %>"></div>
                    <div class="col-md-2"><label class="form-label"><%= Common.getBahasaConfig("Tinggi Badan (cm)") %></label><input type="number" class="form-control" name="tinggi" value="<%= calonSiswa.getTinggi() != null ? calonSiswa.getTinggi() : "" %>"></div>
                    <div class="col-md-2"><label class="form-label"><%= Common.getBahasaConfig("Status Keluarga") %></label><input type="text" class="form-control" name="statusDalamKeluarga" value="<%= calonSiswa.getStatusDalamKeluarga() != null ? calonSiswa.getStatusDalamKeluarga() : "" %>"></div>
                    
                    <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Kebutuhan Khusus") %></label><input type="text" class="form-control" name="kebutuhanKhusus" value="<%= calonSiswa.getKebutuhanKhusus() != null ? calonSiswa.getKebutuhanKhusus() : "" %>"></div>
                    <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Riwayat Penyakit") %></label><input type="text" class="form-control" name="riwayatPenyakit" value="<%= calonSiswa.getRiwayatPenyakit() != null ? calonSiswa.getRiwayatPenyakit() : "" %>"></div>
                    <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Hobby") %></label><input type="text" class="form-control" name="hobby" value="<%= calonSiswa.getHobby() != null ? calonSiswa.getHobby() : "" %>"></div>
                </div>
            </div>

            <div class="tab-pane fade form-section-<%=rnd%> border border-top-0 rounded-top-0" id="tabKeluarga<%=rnd%>" role="tabpanel">
                <div class="row mb-3">
                    <div class="col-md-4">
                        <label class="form-label fw-bold text-secondary"><%= Common.getBahasaConfig("Nomor Kartu Keluarga (KK)") %></label>
                        <input type="text" class="form-control border-primary" name="kk" value="<%= calonSiswa.getKk() != null ? calonSiswa.getKk() : "" %>">
                    </div>
                </div>

                <div class="row g-4">
                    <div class="col-md-6 border-end">
                        <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-male me-2"></i><%= Common.getBahasaConfig("Data Ayah Kandung") %></h6>
                        <div class="row g-2 floating-label-custom-<%=rnd%>">
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Nama Ayah") %></label><input type="text" class="form-control" name="namaAyah" value="<%= calonSiswa.getNamaAyah() != null ? calonSiswa.getNamaAyah() : "" %>"></div>
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("NIK Ayah") %></label><input type="text" class="form-control" name="nikAyah" value="<%= calonSiswa.getNikAyah() != null ? calonSiswa.getNikAyah() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Tempat Lahir") %></label><input type="text" class="form-control" name="tempatLahirAyah" value="<%= calonSiswa.getTempatLahirAyah() != null ? calonSiswa.getTempatLahirAyah() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Tanggal Lahir") %></label><input type="date" class="form-control" name="tanggalLahirAyah" value="<%= calonSiswa.getTanggalLahirAyah() != null ? Common.dateFormat41.get().format(calonSiswa.getTanggalLahirAyah()) : "" %>"></div>
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Pendidikan Terakhir") %></label><input type="hidden" name="pendidikanAyah.id" value="<%= calonSiswa.getPendidikanAyah() != null ? calonSiswa.getPendidikanAyah().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPendidikanAyah() != null ? calonSiswa.getPendidikanAyah().getNama() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Pekerjaan") %></label><input type="hidden" name="pekerjaanAyah.id" value="<%= calonSiswa.getPekerjaanAyah() != null ? calonSiswa.getPekerjaanAyah().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPekerjaanAyah() != null ? calonSiswa.getPekerjaanAyah().getNama() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Penghasilan per Bulan") %></label><input type="hidden" name="penghasilanAyah.id" value="<%= calonSiswa.getPenghasilanAyah() != null ? calonSiswa.getPenghasilanAyah().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPenghasilanAyah() != null ? calonSiswa.getPenghasilanAyah().getNama() : "" %>"></div>
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Alamat Lengkap Ayah") %></label><textarea class="form-control" name="alamatAyah" rows="2"><%= calonSiswa.getAlamatAyah() != null ? calonSiswa.getAlamatAyah() : "" %></textarea></div>
                            <div class="col-4"><label class="form-label"><%= Common.getBahasaConfig("No WA Ayah") %></label><input type="text" class="form-control" name="waAyah" value="<%= calonSiswa.getWaAyah() != null ? calonSiswa.getWaAyah() : "" %>"></div>
                            <div class="col-4"><label class="form-label"><%= Common.getBahasaConfig("No HP 1") %></label><input type="text" class="form-control" name="hp1ayah" value="<%= calonSiswa.getHp1ayah() != null ? calonSiswa.getHp1ayah() : "" %>"></div>
                            <div class="col-4"><label class="form-label"><%= Common.getBahasaConfig("No HP 2") %></label><input type="text" class="form-control" name="hp2ayah" value="<%= calonSiswa.getHp2ayah() != null ? calonSiswa.getHp2ayah() : "" %>"></div>
                        </div>
                    </div>

                    <div class="col-md-6">
                        <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-female me-2"></i><%= Common.getBahasaConfig("Data Ibu Kandung") %></h6>
                        <div class="row g-2 floating-label-custom-<%=rnd%>">
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Nama Ibu") %></label><input type="text" class="form-control" name="namaIbu" value="<%= calonSiswa.getNamaIbu() != null ? calonSiswa.getNamaIbu() : "" %>"></div>
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("NIK Ibu") %></label><input type="text" class="form-control" name="nikIbu" value="<%= calonSiswa.getNikIbu() != null ? calonSiswa.getNikIbu() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Tempat Lahir") %></label><input type="text" class="form-control" name="tempatLahirIbu" value="<%= calonSiswa.getTempatLahirIbu() != null ? calonSiswa.getTempatLahirIbu() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Tanggal Lahir") %></label><input type="date" class="form-control" name="tanggalLahirIbu" value="<%= calonSiswa.getTanggalLahirIbu() != null ? Common.dateFormat41.get().format(calonSiswa.getTanggalLahirIbu()) : "" %>"></div>
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Pendidikan Terakhir") %></label><input type="hidden" name="pendidikanIbu.id" value="<%= calonSiswa.getPendidikanIbu() != null ? calonSiswa.getPendidikanIbu().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPendidikanIbu() != null ? calonSiswa.getPendidikanIbu().getNama() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Pekerjaan") %></label><input type="hidden" name="pekerjaanIbu.id" value="<%= calonSiswa.getPekerjaanIbu() != null ? calonSiswa.getPekerjaanIbu().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPekerjaanIbu() != null ? calonSiswa.getPekerjaanIbu().getNama() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Penghasilan per Bulan") %></label><input type="hidden" name="penghasilanIbu.id" value="<%= calonSiswa.getPenghasilanIbu() != null ? calonSiswa.getPenghasilanIbu().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPenghasilanIbu() != null ? calonSiswa.getPenghasilanIbu().getNama() : "" %>"></div>
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Alamat Lengkap Ibu") %></label><textarea class="form-control" name="alamatIbu" rows="2"><%= calonSiswa.getAlamatIbu() != null ? calonSiswa.getAlamatIbu() : "" %></textarea></div>
                            <div class="col-4"><label class="form-label"><%= Common.getBahasaConfig("No WA Ibu") %></label><input type="text" class="form-control" name="waIbu" value="<%= calonSiswa.getWaIbu() != null ? calonSiswa.getWaIbu() : "" %>"></div>
                            <div class="col-4"><label class="form-label"><%= Common.getBahasaConfig("No HP 1") %></label><input type="text" class="form-control" name="hp1ibu" value="<%= calonSiswa.getHp1ibu() != null ? calonSiswa.getHp1ibu() : "" %>"></div>
                            <div class="col-4"><label class="form-label"><%= Common.getBahasaConfig("No HP 2") %></label><input type="text" class="form-control" name="hp2ibu" value="<%= calonSiswa.getHp2ibu() != null ? calonSiswa.getHp2ibu() : "" %>"></div>
                        </div>
                    </div>
                </div>

                <div class="row mt-4 pt-3 border-top g-4">
                    <div class="col-md-6 border-end">
                        <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
                            <h6 class="fw-bold text-primary mb-0"><i class="fas fa-user-shield me-2"></i><%= Common.getBahasaConfig("Data Wali (Jika Ada)") %></h6>
                            <div class="form-check form-switch">
                                <input class="form-check-input" type="checkbox" role="switch" id="punyaWali<%=rnd%>" name="mempunyaiWali" value="true" <%= (calonSiswa.getMempunyaiWali() != null && calonSiswa.getMempunyaiWali()) ? "checked" : "" %>>
                                <label class="form-check-label small" for="punyaWali<%=rnd%>"><%= Common.getBahasaConfig("Mempunyai Wali") %></label>
                            </div>
                        </div>
                        <div class="row g-2 floating-label-custom-<%=rnd%>">
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Nama Wali") %></label><input type="text" class="form-control" name="namaWali" value="<%= calonSiswa.getNamaWali() != null ? calonSiswa.getNamaWali() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("NIK Wali") %></label><input type="text" class="form-control" name="nikWali" value="<%= calonSiswa.getNikWali() != null ? calonSiswa.getNikWali() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Pendidikan Terakhir") %></label><input type="hidden" name="pendidikanWali.id" value="<%= calonSiswa.getPendidikanWali() != null ? calonSiswa.getPendidikanWali().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPendidikanWali() != null ? calonSiswa.getPendidikanWali().getNama() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Pekerjaan Wali") %></label><input type="hidden" name="pekerjaanWali.id" value="<%= calonSiswa.getPekerjaanWali() != null ? calonSiswa.getPekerjaanWali().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPekerjaanWali() != null ? calonSiswa.getPekerjaanWali().getNama() : "" %>"></div>
                            <div class="col-6"><label class="form-label"><%= Common.getBahasaConfig("Penghasilan per Bulan") %></label><input type="hidden" name="penghasilanWali.id" value="<%= calonSiswa.getPenghasilanWali() != null ? calonSiswa.getPenghasilanWali().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPenghasilanWali() != null ? calonSiswa.getPenghasilanWali().getNama() : "" %>"></div>
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Alamat Wali") %></label><textarea class="form-control" name="alamatWali" rows="2"><%= calonSiswa.getAlamatWali() != null ? calonSiswa.getAlamatWali() : "" %></textarea></div>
                            <div class="col-4"><label class="form-label"><%= Common.getBahasaConfig("No WA Wali") %></label><input type="text" class="form-control" name="waWali" value="<%= calonSiswa.getWaWali() != null ? calonSiswa.getWaWali() : "" %>"></div>
                            <div class="col-4"><label class="form-label"><%= Common.getBahasaConfig("Telepon Wali") %></label><input type="text" class="form-control" name="teleponWali" value="<%= calonSiswa.getTeleponWali() != null ? calonSiswa.getTeleponWali() : "" %>"></div>
                            <div class="col-4"><label class="form-label"><%= Common.getBahasaConfig("HP 1 Wali") %></label><input type="text" class="form-control" name="hp1wali" value="<%= calonSiswa.getHp1wali() != null ? calonSiswa.getHp1wali() : "" %>"></div>
                            <input type="hidden" name="tempatLahirWali" value="<%= calonSiswa.getTempatLahirWali() != null ? calonSiswa.getTempatLahirWali() : "" %>">
                            <input type="hidden" name="tanggalLahirWali" value="<%= calonSiswa.getTanggalLahirWali() != null ? Common.dateFormat41.get().format(calonSiswa.getTanggalLahirWali()) : "" %>">
                            <input type="hidden" name="hp2wali" value="<%= calonSiswa.getHp2wali() != null ? calonSiswa.getHp2wali() : "" %>">
                            <input type="hidden" name="hp3wali" value="<%= calonSiswa.getHp3wali() != null ? calonSiswa.getHp3wali() : "" %>">
                        </div>
                    </div>
                    
                    <div class="col-md-6">
                        <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-home me-2"></i><%= Common.getBahasaConfig("Alamat Domisili Orang Tua") %></h6>
                        <div class="row g-2 floating-label-custom-<%=rnd%>">
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Alamat Orang Tua") %></label><textarea class="form-control" name="alamatOrangTua" rows="2"><%= calonSiswa.getAlamatOrangTua() != null ? calonSiswa.getAlamatOrangTua() : "" %></textarea></div>
                            <div class="col-12"><label class="form-label"><%= Common.getBahasaConfig("Telepon Orang Tua / Rumah") %></label><input type="text" class="form-control" name="teleponOrangTua" value="<%= calonSiswa.getTeleponOrangTua() != null ? calonSiswa.getTeleponOrangTua() : "" %>"></div>
                            <div class="col-12 mt-3">
                                <div class="form-check form-switch bg-light p-2 rounded border border-info border-opacity-25">
                                    <input type="hidden" name="orangTuaPegawai.id" value="<%= calonSiswa.getOrangTuaPegawai() != null ? calonSiswa.getOrangTuaPegawai().getId() : "" %>">
                                    <input class="form-check-input ms-2" type="checkbox" role="switch" id="ortuPegawai<%=rnd%>" <%= (calonSiswa.getOrangTuaPegawai() != null) ? "checked" : "" %> disabled>
                                    <label class="form-check-label small ms-2 fw-bold text-info" for="ortuPegawai<%=rnd%>"><i class="fas fa-check-circle me-1"></i> <%= Common.getBahasaConfig("Orang Tua Merupakan Pegawai Sekolah") %></label>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="tab-pane fade form-section-<%=rnd%> border border-top-0 rounded-top-0" id="tabSekolah<%=rnd%>" role="tabpanel">
                <h6 class="fw-bold text-primary border-bottom pb-2 mb-3"><i class="fas fa-history me-2"></i><%= Common.getBahasaConfig("Pendidikan Terakhir / Sekolah Asal") %></h6>
                <div class="row g-3 floating-label-custom-<%=rnd%> mb-4">
                    <div class="col-md-6"><label class="form-label"><%= Common.getBahasaConfig("Nama Sekolah Asal") %></label><input type="text" class="form-control" name="sekolahAsal" value="<%= calonSiswa.getSekolahAsal() != null ? calonSiswa.getSekolahAsal() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Status Sekolah Asal") %></label><input type="text" class="form-control" name="statusSekolah" value="<%= calonSiswa.getStatusSekolah() != null ? calonSiswa.getStatusSekolah() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("NPSN Sekolah Asal") %></label><input type="text" class="form-control" name="nomorPokokSekolahNasional" value="<%= calonSiswa.getNomorPokokSekolahNasional() != null ? calonSiswa.getNomorPokokSekolahNasional() : "" %>"></div>
                    
                    <div class="col-md-12"><label class="form-label"><%= Common.getBahasaConfig("Alamat Sekolah Asal") %></label><textarea class="form-control" name="alamatSekolahAsal" rows="2"><%= calonSiswa.getAlamatSekolahAsal() != null ? calonSiswa.getAlamatSekolahAsal() : "" %></textarea></div>
                    
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Desa / Kelurahan Sekolah") %></label><input type="text" class="form-control" name="desaKelurahanSekolahAsal" value="<%= calonSiswa.getDesaKelurahanSekolahAsal() != null ? calonSiswa.getDesaKelurahanSekolahAsal() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Kecamatan Sekolah") %></label><input type="hidden" name="kecamatanSekolahAsal.id" value="<%= calonSiswa.getKecamatanSekolahAsal() != null ? calonSiswa.getKecamatanSekolahAsal().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getKecamatanSekolahAsal() != null ? calonSiswa.getKecamatanSekolahAsal().getNama() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Kota / Kabupaten Sekolah") %></label><input type="hidden" name="kotaSekolahAsal.id" value="<%= calonSiswa.getKotaSekolahAsal() != null ? calonSiswa.getKotaSekolahAsal().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getKotaSekolahAsal() != null ? calonSiswa.getKotaSekolahAsal().getNama() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Propinsi Sekolah") %></label><input type="hidden" name="propinsiSekolahAsal.id" value="<%= calonSiswa.getPropinsiSekolahAsal() != null ? calonSiswa.getPropinsiSekolahAsal().getId() : "" %>"><input type="text" class="form-control" value="<%= calonSiswa.getPropinsiSekolahAsal() != null ? calonSiswa.getPropinsiSekolahAsal().getNama() : "" %>"></div>
                    
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Tahun Lulus") %></label><input type="number" class="form-control" name="tahunLulus" value="<%= calonSiswa.getTahunLulus() != null ? calonSiswa.getTahunLulus() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Nomor Ujian Nasional") %></label><input type="text" class="form-control" name="nomorUjianNasional" value="<%= calonSiswa.getNomorUjianNasional() != null ? calonSiswa.getNomorUjianNasional() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Nomor Seri SKHUN") %></label><input type="text" class="form-control" name="nomorSeriSkhun" value="<%= calonSiswa.getNomorSeriSkhun() != null ? calonSiswa.getNomorSeriSkhun() : "" %>"></div>
                    <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Nomor Seri Ijazah") %></label><input type="text" class="form-control" name="nomorSeriIjazah" value="<%= calonSiswa.getNomorSeriIjazah() != null ? calonSiswa.getNomorSeriIjazah() : "" %>"></div>
                </div>

                <div class="p-3 bg-light rounded border border-warning border-opacity-50">
                    <div class="form-check form-switch mb-3">
                        <input class="form-check-input" type="checkbox" role="switch" id="isPindahan<%=rnd%>" name="merupakanPindahan" value="true" onchange="window.togglePindahan<%=rnd%>()" <%= (calonSiswa.getMerupakanPindahan() != null && calonSiswa.getMerupakanPindahan()) ? "checked" : "" %>>
                        <label class="form-check-label fw-bold text-warning" style="text-shadow: 0px 0px 1px #000;" for="isPindahan<%=rnd%>"><i class="fas fa-exchange-alt me-2"></i><%= Common.getBahasaConfig("Merupakan Siswa Pindahan") %></label>
                    </div>
                    
                    <div id="areaPindahan<%=rnd%>" style="display: none;">
                        <div class="row g-3 floating-label-custom-<%=rnd%>">
                            <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Pindahan Dari Sekolah") %></label><input type="text" class="form-control border-warning" name="pindahanDariSekolah" value="<%= calonSiswa.getPindahanDariSekolah() != null ? calonSiswa.getPindahanDariSekolah() : "" %>"></div>
                            <div class="col-md-2"><label class="form-label"><%= Common.getBahasaConfig("Tanggal Pindah") %></label><input type="date" class="form-control border-warning" name="tanggalPindah" value="<%= calonSiswa.getTanggalPindah() != null ? Common.dateFormat41.get().format(calonSiswa.getTanggalPindah()) : "" %>"></div>
                            <div class="col-md-3"><label class="form-label"><%= Common.getBahasaConfig("Kelas Sebelumnya") %></label><input type="text" class="form-control border-warning" name="kelasSekolahPindahan" value="<%= calonSiswa.getKelasSekolahPindahan() != null ? calonSiswa.getKelasSekolahPindahan() : "" %>"></div>
                            <div class="col-md-12"><label class="form-label"><%= Common.getBahasaConfig("Alamat Sekolah Pindahan") %></label><textarea class="form-control border-warning" name="alamatSekolahPindahan" rows="2"><%= calonSiswa.getAlamatSekolahPindahan() != null ? calonSiswa.getAlamatSekolahPindahan() : "" %></textarea></div>
                            <div class="col-md-12"><label class="form-label"><%= Common.getBahasaConfig("Alasan / Keterangan Pindah") %></label><textarea class="form-control border-warning" name="keteranganPindah" rows="2"><%= calonSiswa.getKeteranganPindah() != null ? calonSiswa.getKeteranganPindah() : "" %></textarea></div>
                        </div>
                    </div>
                </div>

                <input type="hidden" name="paketPsb.id" value="<%= calonSiswa.getPaketPsb() != null ? calonSiswa.getPaketPsb().getId() : "" %>">
                <input type="hidden" name="kelasSiswa.id" value="<%= calonSiswa.getKelasSiswa() != null ? calonSiswa.getKelasSiswa().getId() : "" %>">
                
                <input type="hidden" name="sekolah.id" value="<%= calonSiswa.getSekolah() != null ? calonSiswa.getSekolah().getId() : (gelombang != null && gelombang.getSekolah() != null ? gelombang.getSekolah().getId() : "") %>">
                <input type="hidden" name="penjurusanSekolah.id" value="<%= calonSiswa.getPenjurusanSekolah() != null ? calonSiswa.getPenjurusanSekolah().getId() : "" %>">
                <input type="hidden" name="tahunMasuk" value="<%= calonSiswa.getTahunMasuk() != null ? calonSiswa.getTahunMasuk() : (gelombang != null && gelombang.getTahunAjaran() != null ? gelombang.getTahunAjaran() : "") %>">
                
                <input type="hidden" name="statusAwalSiswa.id" value="<%= calonSiswa.getStatusAwalSiswa() != null ? calonSiswa.getStatusAwalSiswa().getId() : "" %>">
                <input type="hidden" name="nomorInduk" value="<%= calonSiswa.getNomorInduk() != null ? calonSiswa.getNomorInduk() : "" %>">
                <input type="hidden" name="nis" value="<%= calonSiswa.getNis() != null ? calonSiswa.getNis() : "" %>">
            </div>

            <div class="tab-pane fade form-section-<%=rnd%> border border-top-0 rounded-top-0" id="tabLainnya<%=rnd%>" role="tabpanel">
                
                <h6 class="fw-bold text-success border-bottom pb-2 mb-3"><i class="fas fa-hand-holding-heart me-2"></i><%= Common.getBahasaConfig("Kesejahteraan & Prestasi Siswa") %></h6>
                
                <div class="row g-3 floating-label-custom-<%=rnd%> mb-4">
                    <div class="col-md-12 d-flex gap-4 mb-2">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" name="layakPip" value="true" id="layakPip<%=rnd%>" <%= (calonSiswa.getLayakPip() != null && (calonSiswa.getLayakPip().equalsIgnoreCase("true") || calonSiswa.getLayakPip().equalsIgnoreCase("ya") || calonSiswa.getLayakPip().equals("1"))) ? "checked" : "" %>>
                            <label class="form-check-label" for="layakPip<%=rnd%>"><%= Common.getBahasaConfig("Siswa Layak PIP") %></label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" name="tidakLayakPip" value="true" id="tdkLayakPip<%=rnd%>" <%= (calonSiswa.getTidakLayakPip() != null && (calonSiswa.getTidakLayakPip().equalsIgnoreCase("true") || calonSiswa.getTidakLayakPip().equalsIgnoreCase("ya") || calonSiswa.getTidakLayakPip().equals("1"))) ? "checked" : "" %>>
                            <label class="form-check-label" for="tdkLayakPip<%=rnd%>"><%= Common.getBahasaConfig("Tidak Layak PIP") %></label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" name="penerimaBantuan" value="true" id="terimaBantuan<%=rnd%>" <%= (calonSiswa.getPenerimaBantuan() != null && calonSiswa.getPenerimaBantuan()) ? "checked" : "" %>>
                            <label class="form-check-label" for="terimaBantuan<%=rnd%>"><%= Common.getBahasaConfig("Menerima Bantuan Lain") %></label>
                        </div>
                    </div>
                   
                    <div class="col-md-12"><label class="form-label"><%= Common.getBahasaConfig("Nomor Kartu Indonesia Pintar (KIP) Jika Ada") %></label><input type="text" class="form-control border-success" name="noKip" value="<%= calonSiswa.getNoKip() != null ? calonSiswa.getNoKip() : "" %>"></div>
                    
                    <div class="col-md-12"><label class="form-label"><%= Common.getBahasaConfig("Catatan Prestasi Siswa") %></label><textarea class="form-control" name="formulaPrestasi" rows="2" placeholder="<%= Common.getBahasaConfig("Sebutkan riwayat prestasi siswa jika ada...") %>"><%= calonSiswa.getFormulaPrestasi() != null ? calonSiswa.getFormulaPrestasi() : "" %></textarea></div>
                </div>

                <h6 class="fw-bold text-info border-bottom pb-2 mb-3 mt-4"><i class="fas fa-question-circle me-2"></i><%= Common.getBahasaConfig("Informasi Tambahan") %></h6>
                
                <div class="row g-3 floating-label-custom-<%=rnd%>">
                    <div class="col-md-6 d-flex align-items-center">
                        <div class="form-check form-switch w-100 p-3 bg-light rounded">
                            <input class="form-check-input ms-0 me-2" type="checkbox" role="switch" name="apakahMempunyaiSaudaraKandung" value="true" id="punyaSaudara<%=rnd%>" <%= (calonSiswa.getApakahMempunyaiSaudaraKandung() != null && calonSiswa.getApakahMempunyaiSaudaraKandung()) ? "checked" : "" %>>
                            <label class="form-check-label fw-bold text-secondary" for="punyaSaudara<%=rnd%>"><%= Common.getBahasaConfig("Mempunyai Saudara Kandung di Sekolah Ini?") %></label>
                        </div>
                    </div>
                    <div class="col-md-6"><label class="form-label"><%= Common.getBahasaConfig("Detail Saudara Kandung") %></label><input type="text" class="form-control" name="infoMempunyaiSaudaraKandung" placeholder="<%= Common.getBahasaConfig("Nama / Kelas Saudara Kandung") %>" value="<%= calonSiswa.getInfoMempunyaiSaudaraKandung() != null ? calonSiswa.getInfoMempunyaiSaudaraKandung() : "" %>"></div>

                    <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Mengetahui Info Sekolah Dari") %></label><input type="text" class="form-control" name="infoKampusDariMana" value="<%= calonSiswa.getInfoKampusDariMana() != null ? calonSiswa.getInfoKampusDariMana() : "" %>"></div>
                    <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Nama Teman Pemberi Info") %></label><input type="text" class="form-control" name="namaTemanInfoKampusDariMana" value="<%= calonSiswa.getNamaTemanInfoKampusDariMana() != null ? calonSiswa.getNamaTemanInfoKampusDariMana() : "" %>"></div>
                    <div class="col-md-4"><label class="form-label"><%= Common.getBahasaConfig("Keterangan Info PPDB") %></label><input type="text" class="form-control" name="keteranganInfoKampusDariMana" value="<%= calonSiswa.getKeteranganInfoKampusDariMana() != null ? calonSiswa.getKeteranganInfoKampusDariMana() : "" %>"></div>
                    
                    <div class="col-md-6"><label class="form-label"><%= Common.getBahasaConfig("Keterangan Umum Tambahan") %></label><textarea class="form-control" name="keterangan" rows="2"><%= calonSiswa.getKeterangan() != null ? calonSiswa.getKeterangan() : "" %></textarea></div>
                    <div class="col-md-6"><label class="form-label"><%= Common.getBahasaConfig("Bahasa Sehari-hari") %></label><textarea class="form-control" name="bahasa" rows="2"><%= calonSiswa.getBahasa() != null ? calonSiswa.getBahasa() : "" %></textarea></div>
                    
                    <input type="hidden" name="statusSiswa" value="<%= calonSiswa.getStatusSiswa() != null ? calonSiswa.getStatusSiswa() : "" %>">
                    <input type="hidden" name="jadwalPertemuanPSB.id" value="<%= calonSiswa.getJadwalPertemuanPSB() != null ? calonSiswa.getJadwalPertemuanPSB().getId() : "" %>">
                    <input type="hidden" name="padaTanggal" value="<%= calonSiswa.getPadaTanggal() != null ? Common.dateFormat41.get().format(calonSiswa.getPadaTanggal()) : "" %>">
                    <input type="hidden" name="riwayatPembayaranPendaftaran" value="<%= calonSiswa.getRiwayatPembayaranPendaftaran() != null ? calonSiswa.getRiwayatPembayaranPendaftaran() : "" %>">
                    <input type="hidden" name="riwayatPembayaranDaftarUlang" value="<%= calonSiswa.getRiwayatPembayaranDaftarUlang() != null ? calonSiswa.getRiwayatPembayaranDaftarUlang() : "" %>">
                </div>

            </div>

        </div>

        <div class="d-flex justify-content-end gap-3 mt-4 pt-3 border-top">
            <button type="button" class="btn btn-light border shadow-sm rounded-pill fw-bold px-4" data-bs-dismiss="modal">
                <i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Batal") %>
            </button>
            <button type="button" class="btn btn-primary shadow rounded-pill fw-bold px-5" id="btnSubmit<%=rnd%>" onclick="window.simpanPendaftaranSiswa<%=rnd%>(this)">
                <i class="fas fa-save me-2"></i><%= Common.getBahasaConfig("Simpan Pendaftaran") %>
            </button>
        </div>
    </form>
</div>

<script>
window.masterModelClass<%=rnd%> = '<%= CalonSiswa.class.getName() %>';
window.pendingUploads<%=rnd%> = window.pendingUploads<%=rnd%> || [];

window.togglePindahan<%=rnd%> = function() {
    const isPindahan = document.getElementById('isPindahan<%=rnd%>').checked;
    const area = document.getElementById('areaPindahan<%=rnd%>');
    if (isPindahan) {
        area.style.display = 'block';
        area.classList.add('animate__animated', 'animate__fadeIn');
    } else {
        area.style.display = 'none';
    }
};

setTimeout(() => { window.togglePindahan<%=rnd%>(); }, 200);

window.validateElement<%=rnd%> = function(inp, pane) {
    if (inp.hasAttribute('required') && !inp.value.trim()) return false;
    if (!inp.checkValidity()) return false;
    return true;
};

window.forceGoToStep<%=rnd%> = function(tabId) {
    let triggerEl = document.querySelector('button[data-bs-target="#' + tabId + '"]');
    if (triggerEl) {
        let tab = new bootstrap.Tab(triggerEl);
        tab.show();
    }
};

window.getPayloadBiodata<%=rnd%> = function() {
    const form = document.getElementById('formPendaftaranSiswa<%=rnd%>');
    const formData = new FormData(form);
    const dataJson = {};

    // 1. Penanganan khusus untuk checkbox (boolean)
    const checkboxes = form.querySelectorAll('input[type="checkbox"]');
    checkboxes.forEach(cb => {
        dataJson[cb.name] = cb.checked ? "true" : "false";
    });

    // 2. Memasukkan seluruh data form ke dalam objek JSON
    formData.forEach((value, key) => {
        if(!dataJson.hasOwnProperty(key)) {
            dataJson[key] = value;
        }
    });

    // 3. Penambahan format nested object khusus untuk gelombangPendaftaranPsb
    const gelombangId = formData.get('gelombangPendaftaranPsb.id');
    if (gelombangId) {
        dataJson['gelombangPendaftaranPsb'] = gelombangId;
    }

    return dataJson;
};

window.simpanPendaftaranSiswa<%=rnd%> = async function(btn) {
    const form = document.getElementById('formPendaftaranSiswa<%=rnd%>');
    let firstInvalidGlobal = null;
    let invalidTabId = null;
    const tabs = ['tabDiri<%=rnd%>', 'tabKeluarga<%=rnd%>', 'tabSekolah<%=rnd%>', 'tabLainnya<%=rnd%>'];

    for (let i = 0; i < tabs.length; i++) {
        let pane = document.getElementById(tabs[i]);
        if (!pane) continue;

        let inputs = pane.querySelectorAll('input:not([type="button"]):not([type="submit"]):not([type="file"]):not([type="hidden"]), select, textarea');
        for(let j = 0; j < inputs.length; j++) {
            let inp = inputs[j];
            if (!window.validateElement<%=rnd%>(inp, pane)) {
                if (!firstInvalidGlobal) {
                    firstInvalidGlobal = inp;
                    invalidTabId = tabs[i];
                }
            }
        }
        if (firstInvalidGlobal) break;
    }

    if (firstInvalidGlobal) {
        window.forceGoToStep<%=rnd%>(invalidTabId);
        setTimeout(() => {
            firstInvalidGlobal.focus();
            if (typeof firstInvalidGlobal.reportValidity === 'function') {
                firstInvalidGlobal.reportValidity();
            }
            let parentDiv = firstInvalidGlobal.closest('.col-md, .col-12, .col-md-12, .col-md-6, .col-md-4, .col-md-3, .col-md-2, .form-check');
            let labelNode = parentDiv ? parentDiv.querySelector('label') : null;
            let namaField = labelNode ? labelNode.innerText.replace('*', '').trim() : '<%= Common.getBahasaConfigJS("Bidang ini") %>';
            
            if(typeof tampilkanToast === 'function') {
                tampilkanToast('<%= Common.getBahasaConfigJS("Harap lengkapi isian wajib pada:") %> <strong>' + namaField + '</strong>', 'bg-warning text-dark');
            } else {
                alert('<%= Common.getBahasaConfigJS("Harap lengkapi isian wajib pada:") %> ' + namaField);
            }
        }, 300);
        return;
    }

    const originalText = btn.innerHTML;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span> <%= Common.getBahasaConfig("Menyimpan...") %>';
    btn.disabled = true;

    const payload = { 
        action: "simpanDataRinci", 
        log: "true", 
        tanpaLogin: "true", 
        class: window.masterModelClass<%=rnd%>, 
        data: window.getPayloadBiodata<%=rnd%>() 
    };

    try {
        const response = await fetch('<%=Common.ROOT%>/Data?baru=true', { 
            method: 'POST', 
            headers: { 'Content-Type': 'application/json' }, 
            body: JSON.stringify(payload) 
        });
        const result = await response.json();
        
        if (result.status === '00' || result.status === 'success' || result.id || result.sukses) {
            
            let ppdbId = result.id || (result.data && result.data[0] && result.data[0].id) || payload.data.id;
            
            if (window.pendingUploads<%=rnd%> && window.pendingUploads<%=rnd%>.length > 0) {
                let promises = window.pendingUploads<%=rnd%>.map(item => {
                    var reqObj = {
                        "action": "update_file",
                        "id": item.id_val,
                        "class": item.class_val,
                        "jenis": item.jenis_val,
                        "ref": ppdbId
                    };
                    return fetch('<%=Common.ROOT%>/Data', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(reqObj)
                    }).then(res => res.json()).catch(err => console.error(err));
                });
                await Promise.all(promises);
                window.pendingUploads<%=rnd%> = []; 
            }

            if(typeof tampilkanToast === 'function') {
                tampilkanToast('<%= Common.getBahasaConfigJS("Pendaftaran Berhasil Disimpan!") %>', 'bg-success text-white');
            } else {
                alert('<%= Common.getBahasaConfigJS("Pendaftaran Berhasil Disimpan!") %>');
            }
            
            const modalEl = document.getElementById('modalDaftarPpdb<%=rnd%>');
            if (modalEl) {
                let modalObj = bootstrap.Modal.getInstance(modalEl);
                if(modalObj) modalObj.hide();
            }

            // Memberikan jeda 1.5 detik agar PostgreSQL selesai melakukan commit dan merilis lock tabel
            setTimeout(() => {
                if(typeof window.bukaModalCetakPDF<%=rnd%> === 'function' && ppdbId) {
                    window.bukaModalCetakPDF<%=rnd%>(ppdbId);
                } else if(typeof window.loadGelombangPPDB<%=rnd%> === 'function') {
                    window.loadGelombangPPDB<%=rnd%>(1);
                } else {
                    // JIKA MODAL PDF TIDAK TERSEDIA, LANGSUNG AUTO-LOGIN
                    <% if (!isEdit) { %>
                        window.location.replace('<%=Common.ROOT%>/ppdb?hanya_tampil_jsp=true&p=ppdb&s=_pendaftaran_siswa&siswa_id=' + ppdbId);
                    <% } else { %>
                        window.location.reload();
                    <% } %>
                }
            }, 1500);

        } else {
            if(typeof tampilkanToast === 'function') tampilkanToast(result.message || '<%= Common.getBahasaConfigJS("Gagal menyimpan data!") %>', 'bg-danger text-white');
            else alert(result.message || '<%= Common.getBahasaConfigJS("Gagal menyimpan data!") %>');
            
            btn.innerHTML = originalText; 
            btn.disabled = false;
        }
    } catch (error) {
        if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Koneksi terputus saat menyimpan data.") %>', 'bg-danger text-white');
        else alert('<%= Common.getBahasaConfigJS("Koneksi terputus saat menyimpan data.") %>');
        
        btn.innerHTML = originalText; 
        btn.disabled = false;
    }
};

window.bukaModalCetakPDF<%=rnd%> = async function(idSiswa) {
    const existingModal = document.getElementById('modalCetakPDF<%=rnd%>');
    if(existingModal) existingModal.remove();

    if (typeof tampilkanToast === 'function') {
        tampilkanToast('<%= Common.getBahasaConfigJS("Sedang menyiapkan dokumen PDF...") %>', 'bg-info text-white');
    }
    
    try {
        let urlService = '<%=Common.ROOT%>/ppdb?hanya_tampil_jsp=true&p=ppdb&s=_cetak_kartu_pendaftaran&id=' + idSiswa;
        const response = await fetch(urlService);
        const data = await response.json();

        if(data.status === 'success' && data.url) {
            var modalHtml = 
            '<div class="modal fade" id="modalCetakPDF<%=rnd%>" tabindex="-1" aria-hidden="true" style="z-index: 1080;" data-bs-backdrop="static">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                    '<div class="modal-content rounded-4 border-0 shadow">' +
                        '<div class="modal-header bg-light border-0 py-3">' +
                            '<h5 class="modal-title fw-bold text-primary"><i class="fas fa-file-pdf me-2"></i><%= Common.getBahasaConfig("Kartu Pendaftaran & Info Pembayaran") %></h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0" style="height: 80vh;">' +
                            '<iframe src="' + data.url + '" style="width:100%; height:100%; border:none;"></iframe>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            let modalEl = document.getElementById('modalCetakPDF<%=rnd%>');
            let modalObj = new bootstrap.Modal(modalEl);
            modalObj.show();

            modalEl.addEventListener('hidden.bs.modal', function () { 
                this.remove(); 
                <% if (!isEdit) { %>
                    // PERUBAHAN: Trigger Auto-Login dengan mengarahkan ke halaman ini lagi yang membawa siswa_id
                    window.location.replace('<%=Common.ROOT%>/ppdb?hanya_tampil_jsp=true&p=ppdb&s=_pendaftaran_siswa&siswa_id=' + idSiswa);
                <% } else { %>
                    const btn = document.getElementById('btnSubmit<%=rnd%>');
                    if (btn) {
                        const parentModal = btn.closest('.modal');
                        if (parentModal) {
                            const pModalInst = bootstrap.Modal.getInstance(parentModal);
                            if (pModalInst) pModalInst.hide();
                        }
                    }
                    if (typeof window.loadGelombangPPDB<%=rnd%> === 'function') {
                        window.loadGelombangPPDB<%=rnd%>(1);
                    }
                <% } %>
            });

        } else {
            if (typeof tampilkanToast === 'function') tampilkanToast(data.message || '<%= Common.getBahasaConfigJS("Gagal membuat PDF pendaftaran.") %>', 'bg-danger text-white');
            else alert(data.message || '<%= Common.getBahasaConfigJS("Gagal membuat PDF pendaftaran.") %>');
        }
    } catch (e) {
        console.error(e);
        if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat mencetak PDF.") %>', 'bg-danger text-white');
        else alert('<%= Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat mencetak PDF.") %>');
    }
};
</script>