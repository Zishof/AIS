<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.surat.SuratKeluar"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="java.text.SimpleDateFormat"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
String rnd = request.getParameter("rnd");
String idSuratParam = request.getParameter("id");
if (idSuratParam == null || idSuratParam.equals("undefined")) idSuratParam = "";

SuratKeluar suratKeluar = null;
if (!idSuratParam.isEmpty()) {
    suratKeluar = (SuratKeluar) GeneralValueObject.ambilData(SuratKeluar.class, idSuratParam, true);
}

SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
String valKlasifikasiId = (suratKeluar != null && suratKeluar.getKlasifikasiSuratKeluar() != null) ? suratKeluar.getKlasifikasiSuratKeluar().getId().toString() : "";
String valSatKerId      = (suratKeluar != null && suratKeluar.getSatuanKerja() != null) ? suratKeluar.getSatuanKerja().getId().toString() : "";
String valAlurId        = (suratKeluar != null && suratKeluar.getAlurPersetujuanSuratKeluar() != null) ? suratKeluar.getAlurPersetujuanSuratKeluar().getId().toString() : "";
String valKode          = (suratKeluar != null && suratKeluar.getKode() != null) ? suratKeluar.getKode() : "";
String valAgenda        = (suratKeluar != null && suratKeluar.getAgenda() != null) ? suratKeluar.getAgenda() : "";
String valPerihal       = (suratKeluar != null && suratKeluar.getPerihal() != null) ? suratKeluar.getPerihal() : "";
String valKeterangan    = (suratKeluar != null && suratKeluar.getKeterangan() != null) ? suratKeluar.getKeterangan() : "";
String valCatatanRevisi = (suratKeluar != null && suratKeluar.getCatatanRevisi() != null) ? suratKeluar.getCatatanRevisi() : "";
String valTanggal       = (suratKeluar != null && suratKeluar.getTanggal() != null) ? sdf.format(suratKeluar.getTanggal()) : "";
String valBroadcastUser = (suratKeluar != null && suratKeluar.getUsernamePengguna() != null) ? suratKeluar.getUsernamePengguna() : "";
boolean isBroadcast     = (suratKeluar != null && suratKeluar.getBroadcast() != null) ? suratKeluar.getBroadcast() : false;

boolean isAdmin = Common.getApakahAdminLain(tbmuser);
boolean isMahasiswaOrSiswa = (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null);
boolean isDosenOrGuru = (tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null);
%>

<div class="p-3" id="formSuratKeluarWadah<%=rnd%>">
    <input type="hidden" id="idSuratKeluarVal<%=rnd%>" value="<%=idSuratParam%>">

    <div class="row g-4">
        <div class="col-lg-5 border-end" style="max-height: 70vh; overflow-y: auto; overflow-x: hidden;">
            <h6 class="fw-bold text-primary mb-3"><i class="fas fa-edit me-2"></i><%=Common.getBahasaConfig("Pengisian Data Surat")%></h6>
            <div class="row g-3">
                
                <div class="col-12">
                    <label class="form-label fw-bold small text-dark mb-1"><%=Common.getBahasaConfig("Jenis Surat Keluar")%> <span class="text-danger">*</span></label>
                    <select class="form-select shadow-sm border-primary" id="idKlasifikasi<%=rnd%>" onchange="tanganiPerubahanKlasifikasi<%=rnd%>()">
                        <option value=""><%=Common.getBahasaConfig("-- Pilih Jenis Surat --")%></option>
                    </select>
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small text-dark mb-1"><%=Common.getBahasaConfig("Nomor Surat")%></label>
                    <input type="text" class="form-control shadow-sm bg-light" id="kodeSurat<%=rnd%>" value="<%=valKode%>" placeholder="<%=Common.getBahasaConfig("Otomatis")%>" readonly>
                    <% if (!isMahasiswaOrSiswa && !isDosenOrGuru) { %>
                    <div class="form-check mt-1">
                        <input class="form-check-input" type="checkbox" id="chkUbahManualKode<%=rnd%>" onchange="toggleNomorManual<%=rnd%>()">
                        <label class="form-check-label small text-muted"><%=Common.getBahasaConfig("Ubah manual")%></label>
                    </div>
                    <% } %>
                </div>
                
                <div class="col-md-6">
                    <label class="form-label fw-bold small text-dark mb-1"><%=Common.getBahasaConfig("Nomor Agenda")%></label>
                    <input type="text" class="form-control shadow-sm bg-light" id="agendaSurat<%=rnd%>" value="<%=valAgenda%>" placeholder="<%=Common.getBahasaConfig("Otomatis")%>" readonly>
                </div>

                <div class="col-12">
                    <label class="form-label fw-bold small text-dark mb-1"><%=Common.getBahasaConfig("Perihal")%> <span class="text-danger">*</span></label>
                    <input type="text" class="form-control shadow-sm" id="perihalSurat<%=rnd%>" value="<%=valPerihal%>" placeholder="<%=Common.getBahasaConfig("Masukkan Perihal")%>">
                </div>

                <div class="col-12">
                    <label class="form-label fw-bold small text-dark mb-1"><%=Common.getBahasaConfig("Satuan Kerja")%></label>
                    <select class="form-select shadow-sm" id="idSatuanKerja<%=rnd%>">
                        <option value=""><%=Common.getBahasaConfig("-- Pilih Satuan Kerja --")%></option>
                    </select>
                </div>

                <div class="col-12">
                    <label class="form-label fw-bold small text-dark mb-1"><%=Common.getBahasaConfig("Tanggal Surat")%> <span class="text-danger">*</span></label>
                    <input type="date" class="form-control shadow-sm" id="tanggalSurat<%=rnd%>" value="<%=valTanggal%>" onchange="muatPreviewIsi<%=rnd%>()">
                </div>
                
                <div class="col-12">
                    <label class="form-label fw-bold small text-dark mb-1"><%=Common.getBahasaConfig("Alur Persetujuan")%></label>
                    <select class="form-select shadow-sm" id="idAlurPersetujuan<%=rnd%>">
                        <option value=""><%=Common.getBahasaConfig("-- Pilih Alur (Jika Ada) --")%></option>
                    </select>
                </div>

                <% if (!isMahasiswaOrSiswa) { %>
                <div class="col-12 mt-3">
                    <div class="form-check form-switch shadow-sm p-2 bg-light rounded border">
                        <input class="form-check-input ms-2 me-3" type="checkbox" id="chkBroadcast<%=rnd%>" <%=isBroadcast ? "checked" : ""%> onchange="toggleBroadcast<%=rnd%>()">
                        <label class="form-check-label fw-bold text-dark"><%=Common.getBahasaConfig("Sebarkan / Broadcast Surat Ini")%></label>
                    </div>
                </div>
                <div class="col-12" id="rowBroadcastUsers<%=rnd%>" style="display: <%=isBroadcast ? "block" : "none"%>; ">
                    <label class="form-label fw-bold small text-dark mb-1"><%=Common.getBahasaConfig("Ke Username (Kosongkan = Semua)")%></label>
                    <div class="input-group input-group-sm shadow-sm">
                        <textarea class="form-control" id="txtBroadcastUsers<%=rnd%>" rows="2" placeholder="<%=Common.getBahasaConfig("Contoh: usera, userb")%>"><%=valBroadcastUser%></textarea>
                    </div>
                </div>
                <% } %>

                <% if (!valCatatanRevisi.isEmpty()) { %>
                <div class="col-12 mt-2">
                    <div class="alert alert-danger py-2 small fw-bold">
                        <i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Catatan Revisi:")%> <br> <%=valCatatanRevisi%>
                    </div>
                </div>
                <% } %>

                <div class="col-12">
                    <label class="form-label fw-bold small text-dark mb-1"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>
                    <textarea class="form-control shadow-sm" id="keteranganSurat<%=rnd%>" rows="2"><%=valKeterangan%></textarea>
                </div>
                
                <div id="wadahParameterDinamis<%=rnd%>" class="w-100"></div>

            </div>
        </div>

        <div class="col-lg-7">
            <div class="card shadow-sm border-0 h-100 rounded-3">
                <div class="card-header bg-white border-bottom p-0">
                    <ul class="nav nav-tabs nav-justified" id="tabDetailSurat<%=rnd%>" role="tablist">
                        <li class="nav-item">
                            <button class="nav-link active fw-bold text-dark" id="tab-isi-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#pane-isi-<%=rnd%>" type="button">
                                <i class="fas fa-file-pdf text-danger me-1"></i><%=Common.getBahasaConfig("Preview Surat")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-bold text-dark" id="tab-lampiran-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#pane-lampiran-<%=rnd%>" type="button">
                                <i class="fas fa-paperclip text-success me-1"></i><%=Common.getBahasaConfig("Lampiran")%>
                            </button>
                        </li>
                    </ul>
                </div>
                
                <div class="card-body p-0 bg-light" style="min-height: 500px;">
                    <div class="tab-content h-100" id="tabContentDetail<%=rnd%>">
                        <div class="tab-pane fade show active h-100 p-3" id="pane-isi-<%=rnd%>">
                            <div class="d-flex justify-content-between align-items-center mb-2 pb-2 border-bottom">
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="chkTampilPreview<%=rnd%>" checked onchange="togglePreviewIsi<%=rnd%>()">
                                    <label class="form-check-label fw-bold small text-dark"><%=Common.getBahasaConfig("Tampilkan preview surat")%></label>
                                </div>
                                <button class="btn btn-sm btn-primary rounded-pill fw-bold" onclick="muatPreviewIsi<%=rnd%>()"><i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan PDF")%></button>
                            </div>
                            <div id="wadahPreviewIsi<%=rnd%>" class="w-100 bg-white border rounded shadow-sm d-flex align-items-center justify-content-center text-muted" style="height: 600px; overflow: hidden;">
                                <div class="text-center">
                                    <i class="far fa-file-pdf fa-4x mb-3 text-secondary opacity-50"></i>
                                    <p class="mb-0 fw-medium"><%=Common.getBahasaConfig("Pilih jenis surat untuk melihat pratinjau.")%></p>
                                </div>
                            </div>
                        </div>

                        <div class="tab-pane fade h-100 p-3" id="pane-lampiran-<%=rnd%>">
                            <h6 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Lampiran Dokumen")%></h6>
                            <div class="alert alert-info py-2 small">
                                <i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Silakan simpan surat terlebih dahulu sebelum mengunggah berkas lampiran.")%>
                            </div>
                            <div class="table-responsive bg-white rounded border">
                                <table class="table table-sm table-hover align-middle mb-0">
                                    <thead class="table-light text-muted small">
                                        <tr>
                                            <th><%=Common.getBahasaConfig("Nama Berkas")%></th>
                                            <th class="text-center"><%=Common.getBahasaConfig("Ukuran")%></th>
                                            <th class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr><td colspan="3" class="text-center text-muted py-4 small"><%=Common.getBahasaConfig("Belum ada lampiran.")%></td></tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="d-flex justify-content-end gap-2 mt-4 pt-3 border-top bg-white sticky-bottom pb-2">
        <button type="button" class="btn btn-light border fw-bold px-4 rounded-pill shadow-sm" data-bs-dismiss="modal">
            <i class="fas fa-times me-2 text-danger"></i><%=Common.getBahasaConfig("Tutup")%>
        </button>
        <button type="button" class="btn btn-primary shadow-sm fw-bold px-5 rounded-pill" id="btnSimpanSurat<%=rnd%>" onclick="prosesSimpanSurat<%=rnd%>()">
            <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Surat Keluar")%>
        </button>
    </div>
</div>

<script>
    var kelasModelTarget<%=rnd%> = "ais.database.model.surat.SuratKeluar";
    var idSaatIni<%=rnd%> = document.getElementById('idSuratKeluarVal<%=rnd%>').value;
    var daftarKlasifikasiCached<%=rnd%> = [];

    var muatDropdownData<%=rnd%> = async (idElemen, kelasModel, idTerpilih) => {
        var ddl = document.getElementById(idElemen);
        if(!ddl) return;
        try {
            var req = { action: "daftar", class: kelasModel, max: 200, order1: "asc", sort1: "nama" };
            var res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(req)
            });
            var hasil = await res.json();
            var dataOpsi = hasil.data || [];
            if(idElemen === 'idKlasifikasi<%=rnd%>') daftarKlasifikasiCached<%=rnd%> = dataOpsi;

            var htmlOpsi = `<option value=""><%=Common.getBahasaConfig("-- Pilih --")%></option>`;
            dataOpsi.forEach(opsi => {
                var terPilihStr = (idTerpilih && opsi.id.toString() === idTerpilih.toString()) ? "selected" : "";
                var teksLabel = opsi.kode ? `[\${opsi.kode}] \${opsi.nama}` : opsi.nama;
                htmlOpsi += `<option value="\${opsi.id}" \${terPilihStr}>\${teksLabel}</option>`;
            });
            ddl.innerHTML = htmlOpsi;
        } catch(e) { console.error(e); }
    };

    var tanganiPerubahanKlasifikasi<%=rnd%> = () => {
        var idKlas = document.getElementById('idKlasifikasi<%=rnd%>').value;
        var klasTerpilih = daftarKlasifikasiCached<%=rnd%>.find(k => k.id.toString() === idKlas);
        var ddlAlur = document.getElementById('idAlurPersetujuan<%=rnd%>');

        if (!klasTerpilih) {
            ddlAlur.disabled = false;
            document.getElementById('wadahParameterDinamis<%=rnd%>').innerHTML = ''; 
            return;
        }
        
        if(klasTerpilih.tanpaAlur || klasTerpilih["alurPersetujuanSuratKeluar.id"]) {
            ddlAlur.disabled = true;
            if(klasTerpilih["alurPersetujuanSuratKeluar.id"]) ddlAlur.value = klasTerpilih["alurPersetujuanSuratKeluar.id"];
        } else {
            ddlAlur.disabled = false;
        }
        
        document.getElementById('chkTampilPreview<%=rnd%>').checked = true;
        togglePreviewIsi<%=rnd%>();
        muatFormParameterDinamis<%=rnd%>(idKlas);
    };

    var muatFormParameterDinamis<%=rnd%> = async (idKlas) => {
        var idSurat = document.getElementById('idSuratKeluarVal<%=rnd%>').value;
        var wadahParam = document.getElementById('wadahParameterDinamis<%=rnd%>');
        wadahParam.innerHTML = '<div class="text-center my-3"><div class="spinner-border spinner-border-sm text-primary"></div><span class="ms-2 small text-muted"><%=Common.getBahasaConfig("Memuat parameter...")%></span></div>';
        
        try {
            var urlService = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pagesmastersuratsuratkeluarzul%2Fservices&s=parameter&idKlasifikasi=' + idKlas + '&id=' + idSurat + '&rnd=<%=rnd%>';
            var response = await fetch(urlService);
            var htmlData = await response.text();
            wadahParam.innerHTML = htmlData;
            
            var daftarSkrip = wadahParam.querySelectorAll('script');
            daftarSkrip.forEach((skripLama) => {
                var skripBaru = document.createElement('script');
                skripBaru.appendChild(document.createTextNode(skripLama.innerHTML));
                skripLama.parentNode.replaceChild(skripBaru, skripLama);
            });
        } catch (e) {
            wadahParam.innerHTML = '<div class="alert alert-danger small"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal memuat parameter.")%></div>';
        }
    };

    var toggleNomorManual<%=rnd%> = () => {
        var chkUbahManual = document.getElementById('chkUbahManualKode<%=rnd%>');
        if (!chkUbahManual) return;
        var isManual = chkUbahManual.checked;
        var txtKode = document.getElementById('kodeSurat<%=rnd%>');
        txtKode.readOnly = !isManual;
        if(isManual && txtKode.value === '') txtKode.value = 'DRAFT/S-M/001'; 
        if(!isManual && !idSaatIni<%=rnd%>) txtKode.value = '';
    };

    var toggleBroadcast<%=rnd%> = () => {
        var isBroadcast = document.getElementById('chkBroadcast<%=rnd%>').checked;
        document.getElementById('rowBroadcastUsers<%=rnd%>').style.display = isBroadcast ? 'block' : 'none';
    };

    var togglePreviewIsi<%=rnd%> = () => {
        var isPreview = document.getElementById('chkTampilPreview<%=rnd%>').checked;
        var wadah = document.getElementById('wadahPreviewIsi<%=rnd%>');
        if(isPreview) {
            muatPreviewIsi<%=rnd%>();
        } else {
            wadah.innerHTML = '<div class="text-center"><i class="far fa-eye-slash fa-4x mb-3 text-secondary opacity-50"></i><p class="mb-0 fw-medium"><%=Common.getBahasaConfig("Preview Dinonaktifkan")%></p></div>';
        }
    };

    var muatPreviewIsi<%=rnd%> = async () => {
        var wadah = document.getElementById('wadahPreviewIsi<%=rnd%>');
        var isChecked = document.getElementById('chkTampilPreview<%=rnd%>').checked;
        var idSuratValid = document.getElementById('idSuratKeluarVal<%=rnd%>').value;
        var idKlasValid = document.getElementById('idKlasifikasi<%=rnd%>').value;
        
        if(!isChecked) return;
        if(idSuratValid === '' && idKlasValid === '') {
            wadah.innerHTML = `<div class="alert alert-warning m-4"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Silakan pilih Jenis Surat Keluar terlebih dahulu.")%></div>`;
            return;
        }

        wadah.innerHTML = '<div class="text-center my-5"><div class="spinner-border text-primary fa-3x mb-3"></div><p class="fw-bold text-muted"><%=Common.getBahasaConfig("Menghasilkan Dokumen PDF...")%></p></div>';
        var urlPdfService = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pagesmastersuratsuratkeluarzul%2Fservices&s=cetak_pdf&id=' + idSuratValid + '&idKlasifikasi=' + idKlasValid + '&rnd=<%=rnd%>';
        
        try {
            var response = await fetch(urlPdfService);
            var resultJSON = await response.json();
            
            if (resultJSON.status === 'success' && resultJSON.url) {
                wadah.innerHTML = `<iframe src="\${resultJSON.url}" class="w-100 h-100 border-0" style="min-height: 600px;"></iframe>`;
            } else {
                wadah.innerHTML = `<div class="alert alert-danger m-4"><i class="fas fa-exclamation-triangle me-2"></i>\${resultJSON.message}</div>`;
            }
        } catch (error) {
            wadah.innerHTML = `<div class="alert alert-danger m-4"><i class="fas fa-wifi me-2"></i><%=Common.getBahasaConfig("Terjadi kesalahan koneksi.")%></div>`;
        }
    };

    var prosesSimpanSurat<%=rnd%> = async () => {
        var idKlasifikasi = document.getElementById('idKlasifikasi<%=rnd%>').value;
        var tanggal = document.getElementById('tanggalSurat<%=rnd%>').value;
        var perihal = document.getElementById('perihalSurat<%=rnd%>').value.trim();

        if (!idKlasifikasi || !tanggal || !perihal) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Mohon isi Klasifikasi, Tanggal, dan Perihal.")%>', 'bg-warning text-dark');
            return;
        }

        var btnSimpan = document.getElementById('btnSimpanSurat<%=rnd%>');
        btnSimpan.disabled = true;
        btnSimpan.innerHTML = '<i class="fas fa-circle-notch fa-spin me-2"></i><%=Common.getBahasaConfig("Menyimpan...")%>';

        var dataSurat = {
            "klasifikasiSuratKeluar": idKlasifikasi,
            "tanggal.datetime_format": tanggal + " 00:00:00",
            "perihal": perihal,
            "keterangan": document.getElementById('keteranganSurat<%=rnd%>').value
        };

        var satuanKerjaId = document.getElementById('idSatuanKerja<%=rnd%>').value;
        if(satuanKerjaId) dataSurat["satuanKerja"] = satuanKerjaId;
        
        var alurId = document.getElementById('idAlurPersetujuan<%=rnd%>').value;
        if(alurId) dataSurat["alurPersetujuanSuratKeluar"] = alurId;

        var payload = {
            action: "simpanDataRinci",
            class: kelasModelTarget<%=rnd%>,
            data: dataSurat
        };
        if(idSaatIni<%=rnd%>) payload.id = idSaatIni<%=rnd%>;

        try {
            var res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            });
            var result = await res.json();
            
            if (result.status === "success" || result.data === "success" || result.data.id) {
                var wasNew = !idSaatIni<%=rnd%>;
                var savedId = result.data.id || idSaatIni<%=rnd%>;
                
                document.getElementById('idSuratKeluarVal<%=rnd%>').value = savedId;
                idSaatIni<%=rnd%> = savedId;
                
                // Jika surat baru, commit parameter di sesi ke database
                if (wasNew) {
                    await fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pagesmastersuratsuratkeluarzul%2Fservices&s=parameter&action=commit&idSurat=' + savedId + '&rnd=<%=rnd%>');
                }
                
                muatPreviewIsi<%=rnd%>();
                
                if (typeof window["berhasilSimpanSuratKeluar<%=rnd%>"] === "function") {
                    window["berhasilSimpanSuratKeluar<%=rnd%>"](); 
                } else {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Data berhasil disimpan.")%>', 'bg-success text-white');
                }
            } else {
                tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menyimpan data.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.disabled = false;
            btnSimpan.innerHTML = '<i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Surat Keluar")%>';
        }
    };

    var inisialisasiForm<%=rnd%> = async () => {
        // Membersihkan draf sesi lama saat membuka form surat baru
        if (!idSaatIni<%=rnd%>) {
            document.getElementById('tanggalSurat<%=rnd%>').valueAsDate = new Date();
            await fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pagesmastersuratsuratkeluarzul%2Fservices&s=parameter&action=clear&rnd=<%=rnd%>');
        }
        
        await Promise.all([
            muatDropdownData<%=rnd%>('idKlasifikasi<%=rnd%>', 'ais.database.model.surat.KlasifikasiSuratKeluar', '<%=valKlasifikasiId%>'),
            muatDropdownData<%=rnd%>('idSatuanKerja<%=rnd%>', 'ais.database.model.employ.SatuanKerja', '<%=valSatKerId%>'),
            muatDropdownData<%=rnd%>('idAlurPersetujuan<%=rnd%>', 'ais.database.model.surat.AlurPersetujuanSuratKeluar', '<%=valAlurId%>')
        ]);

        if (idSaatIni<%=rnd%> !== '') {
            tanganiPerubahanKlasifikasi<%=rnd%>();
        } else {
            document.getElementById('chkTampilPreview<%=rnd%>').checked = true;
        }
    };

    inisialisasiForm<%=rnd%>();
</script>