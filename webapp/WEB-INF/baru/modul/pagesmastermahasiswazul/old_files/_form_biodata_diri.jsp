<%@ page import="ais.common.Common" %>
<%
    String rnd = request.getParameter("rnd");
%>

<!-- 1. DATA INDUK MAHASISWA (Mahasiswa.java) -->
<h6 class="fw-bold text-primary mb-3 mt-2"><i class="fas fa-id-badge me-2"></i>Data Induk Mahasiswa</h6>
<div class="row g-3 p-3 border rounded-3 bg-light mb-4">
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("NIM")%></label>
        <input type="text" class="form-control shadow-sm bg-white fw-bold" id="inputBioNim<%=rnd%>" readonly>
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Lengkap")%></label>
        <input type="text" class="form-control shadow-sm bg-white fw-bold" id="inputBioNama<%=rnd%>" readonly 
               onkeyup="document.getElementById('inputBioNamaIjazah<%=rnd%>').value = this.value">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jurusan / Prodi")%></label>
        <input type="text" class="form-control shadow-sm bg-white fw-bold" id="inputBioProdi<%=rnd%>" readonly>
    </div>

    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Kelamin")%></label>
        <select class="form-select shadow-sm bg-white" id="selectBioKelamin<%=rnd%>">
            <option value="">-- Pilih --</option>
            <option value="Laki-laki">Laki-laki</option>
            <option value="Perempuan">Perempuan</option>
        </select>
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tempat Lahir")%></label>
        <input type="text" class="form-control shadow-sm bg-white" id="inputBioTempatLahir<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Lahir")%></label>
        <input type="date" class="form-control shadow-sm bg-white" id="inputBioTglLahir<%=rnd%>">
    </div>

    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Agama")%></label>
        <select class="form-select shadow-sm bg-white" id="selectBioAgama<%=rnd%>"></select>
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Status Nikah")%></label>
        <select class="form-select shadow-sm bg-white" id="selectBioStatusNikah<%=rnd%>">
            <option value="0">Belum Nikah</option>
            <option value="1">Nikah</option>
            <option value="2">Janda</option>
            <option value="3">Duda</option>
        </select>
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Bahasa Utama")%></label>
        <select class="form-select shadow-sm bg-white" id="selectBioBahasa<%=rnd%>">
            <option value="Indonesia">Bahasa Indonesia</option>
            <option value="Inggris">Bahasa Inggris</option>
            <option value="Arab">Bahasa Arab</option>
            <option value="Lainnya">Lainnya</option>
        </select>
    </div>
</div>

<!-- 2. DATA ADMINISTRASI & KONTAK (BiodataMahasiswa.java) -->
<h6 class="fw-bold text-primary mb-3"><i class="fas fa-address-book me-2"></i>Data Administrasi dan Kontak</h6>
<div class="row g-3 p-3 border rounded-3 bg-white mb-4 shadow-sm">
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor Induk Siswa Nasional (NISN)")%></label>
        <input type="text" class="form-control shadow-sm border-primary" id="inputBioNisn<%=rnd%>">
        <div class="form-text text-muted" style="font-size: 0.75rem;">
            <i class="fas fa-info-circle me-1"></i> Jika Anda tidak memiliki NISN, boleh diisi dengan <strong>0000000000</strong> (0 sepuluh kali).
        </div>
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor Induk Registrasi Mahasiswa (NIRM)")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioNirm<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("NPWP")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioNpwp<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Alamat Email")%></label>
        <input type="email" class="form-control shadow-sm" id="inputBioEmail<%=rnd%>" placeholder="contoh@email.com">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor Surat Ijin Mengemudi (SIM)")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioSim<%=rnd%>">
    </div>

    <div class="col-12"><hr class="my-1 text-muted opacity-25"></div>

    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("No. HP (WhatsApp)")%></label>
        <div class="input-group">
            <span class="input-group-text bg-light"><i class="fab fa-whatsapp text-success"></i></span>
            <input type="text" class="form-control shadow-sm" id="inputBioHp<%=rnd%>">
        </div>
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Operator Seluler")%></label>
        <select class="form-select shadow-sm" id="selectBioOperator<%=rnd%>"></select>
    </div>

    <div class="col-12"><hr class="my-1 text-muted opacity-25"></div>

    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Bank")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioBank<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Atas Nama Rekening Bank")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioAtasNamaRek<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor Rekening Bank")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioNoRek<%=rnd%>">
    </div>
</div>

<!-- 3. DATA PENDIDIKAN SEBELUMNYA -->
<h6 class="fw-bold text-primary mb-3"><i class="fas fa-graduation-cap me-2"></i>Pendidikan Sebelumnya</h6>
<div class="row g-3 p-3 border rounded-3 bg-white mb-4 shadow-sm">
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Pendidikan Sebelumnya")%></label>
        <select class="form-select shadow-sm" id="selectBioJenisSekolah<%=rnd%>"></select>
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Asal Sekolah (SMA / Sederajat)")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioAsalSma<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("NPSN Sekolah Asal")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioNpsn<%=rnd%>">
    </div>
    <div class="col-md-12">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Alamat Sekolah Asal")%></label>
        <textarea class="form-control shadow-sm" id="inputBioAlamatSma<%=rnd%>" rows="2"></textarea>
    </div>

    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama yang tertera di Ijazah")%></label>
        <input type="text" class="form-control shadow-sm border-info" id="inputBioNamaIjazah<%=rnd%>">
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor Ijazah Sekolah Asal")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioNoIjazah<%=rnd%>">
    </div>

    <div class="col-12"><hr class="my-1 text-muted opacity-25"></div>

    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Asal SMP / Sederajat")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioAsalSmp<%=rnd%>">
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Alamat SMP / Sederajat")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioAlamatSmp<%=rnd%>">
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Asal SD / Sederajat")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioAsalSd<%=rnd%>">
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Alamat SD / Sederajat")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioAlamatSd<%=rnd%>">
    </div>

    <div class="col-md-6 mt-3">
        <div class="form-check form-switch">
            <input class="form-check-input" type="checkbox" id="checkBioPaud<%=rnd%>">
            <label class="form-check-label small fw-bold text-dark" for="checkBioPaud<%=rnd%>"><%=Common.getBahasaConfig("Pernah PAUD (Pendidikan Anak Usia Dini)")%></label>
        </div>
    </div>
    <div class="col-md-6 mt-3">
        <div class="form-check form-switch">
            <input class="form-check-input" type="checkbox" id="checkBioTk<%=rnd%>">
            <label class="form-check-label small fw-bold text-dark" for="checkBioTk<%=rnd%>"><%=Common.getBahasaConfig("Pernah TK (Taman Kanak-Kanak)")%></label>
        </div>
    </div>
</div>

<!-- 4. DATA FISIK & PENUNJANG -->
<h6 class="fw-bold text-primary mb-3"><i class="fas fa-walking me-2"></i>Data Fisik dan Penunjang Kuliah</h6>
<div class="row g-3 p-3 border rounded-3 bg-white mb-4 shadow-sm">
    <div class="col-md-2">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Golongan Darah")%></label>
        <select class="form-select shadow-sm" id="selectBioGolDarah<%=rnd%>">
            <option value="">--</option>
            <option value="A">A</option>
            <option value="B">B</option>
            <option value="AB">AB</option>
            <option value="O">O</option>
        </select>
    </div>
    <div class="col-md-2">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tinggi (cm)")%></label>
        <input type="number" class="form-control shadow-sm" id="inputBioTinggi<%=rnd%>">
    </div>
    <div class="col-md-2">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Berat (kg)")%></label>
        <input type="number" class="form-control shadow-sm" id="inputBioBerat<%=rnd%>">
    </div>
    <div class="col-md-2">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Ukuran Jaket")%></label>
        <select class="form-select shadow-sm" id="selectBioJaket<%=rnd%>">
            <option value="">--</option>
            <option value="S">S</option>
            <option value="M">M</option>
            <option value="L">L</option>
            <option value="XL">XL</option>
            <option value="XXL">XXL</option>
        </select>
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tempat Tinggal Saat Kuliah")%></label>
        <select class="form-select shadow-sm" id="selectBioTinggal<%=rnd%>"></select>
    </div>

    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Transportasi Saat Kuliah")%></label>
        <select class="form-select shadow-sm" id="selectBioTransportasi<%=rnd%>"></select>
    </div>
    <div class="col-md-6 mt-4">
        <div class="form-check form-switch pt-2">
            <input class="form-check-input" type="checkbox" id="checkBioLuarNegeri<%=rnd%>">
            <label class="form-check-label small fw-bold text-dark" for="checkBioLuarNegeri<%=rnd%>"><%=Common.getBahasaConfig("Pernah Menetap di Luar Negeri")%></label>
        </div>
    </div>
</div>

<!-- 5. ORGANISASI & MINAT -->
<h6 class="fw-bold text-primary mb-3"><i class="fas fa-users-cog me-2"></i>Organisasi dan Minat Bakat</h6>
<div class="row g-3 p-3 border rounded-3 bg-white mb-4 shadow-sm">
    <div class="col-md-12 mt-2">
        <div class="form-check form-switch bg-light p-3 rounded-3 border">
            <input class="form-check-input ms-0 me-2" type="checkbox" id="checkBioOrganisasi<%=rnd%>" 
                   onchange="document.getElementById('divOrgDetail<%=rnd%>').style.display = this.checked ? 'flex' : 'none'">
            <label class="form-check-label small fw-bold text-primary" for="checkBioOrganisasi<%=rnd%>">
                <%=Common.getBahasaConfig("Apakah mengikuti Organisasi Intra Kampus?")%>
            </label>
        </div>
    </div>

    <div class="col-md-12 row g-3 mt-1 mx-0 px-0" id="divOrgDetail<%=rnd%>" style="display: none;">
        <div class="col-md-6">
            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Organisasi Mahasiswa")%></label>
            <input type="text" class="form-control shadow-sm border-primary" id="inputBioNamaOrganisasi<%=rnd%>">
        </div>
        <div class="col-md-6">
            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Upload Surat Penunjukan Pengurus")%></label>
            <input type="file" class="form-control shadow-sm" id="fileBioSuratOrganisasi<%=rnd%>">
        </div>
    </div>

    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Hobi")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioHobi<%=rnd%>">
    </div>
    <div class="col-md-6">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Minat Seni")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioSeni<%=rnd%>">
    </div>

    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kemampuan Bahasa 1 (Utama)")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioBahasa1<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kemampuan Bahasa 2")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioBahasa2<%=rnd%>">
    </div>
    <div class="col-md-4">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kemampuan Bahasa 3")%></label>
        <input type="text" class="form-control shadow-sm" id="inputBioBahasa3<%=rnd%>">
    </div>
</div>

<!-- 6. VERIFIKASI (TANDA TANGAN) -->
<h6 class="fw-bold text-primary mb-3"><i class="fas fa-signature me-2"></i>Verifikasi dan Tanda Tangan</h6>
<div class="row g-3 p-3 border rounded-3 bg-white mb-4 shadow-sm">
    <div class="col-md-12">
        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Upload Tanda Tangan (Format .PNG tanpa background)")%></label>
        <input type="file" class="form-control shadow-sm border-warning" id="fileBioTtd<%=rnd%>" accept="image/png">
        <div class="form-text text-danger" style="font-size: 0.7rem;">
            * Pastikan file tanda tangan memiliki latar belakang transparan (PNG).
        </div>
    </div>

    <!-- TOMBOL SIMPAN BIODATA (Pindah ke sini sesuai requirement terakhir) -->
    <div class="col-12 text-end mt-4 pt-4 border-top">
        <button type="button" class="btn btn-success px-5 fw-bold shadow rounded-pill" id="btnSimpanBiodata<%=rnd%>" onclick="simpanDataBiodata<%=rnd%>()">
            <i class="fas fa-check-circle me-2"></i>Simpan Biodata Lengkap
        </button>
    </div>
</div>