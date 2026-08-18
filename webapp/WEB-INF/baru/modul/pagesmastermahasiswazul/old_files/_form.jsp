<%@page import="ais.common.Common"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="java.util.Calendar"%>
<%@ page isELIgnored="true"%>

<%
String rnd = request.getParameter("rnd");
boolean canAdd = Boolean.parseBoolean(request.getParameter("canAdd"));
boolean canEdit = Boolean.parseBoolean(request.getParameter("canEdit"));
String masterClass = request.getParameter("masterClass");

//Mendapatkan nama folder tempat file ini berada secara dinamis (misal: "master")
String currentPath = request.getServletPath();
String[] pathParts = currentPath.split("/");
String folderP = pathParts.length > 2 ? pathParts[pathParts.length - 2] : "master";
%>

<div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn"
	style="display: none;">
	<div class="card shadow-sm rounded-4 mt-3">
		<div class="card-header bg-light pt-4 px-4 border-bottom-0">
			<h5 class="fw-bold text-dark mb-4" id="formTitle<%=rnd%>"><%=Common.getBahasaConfig("Tambah Mahasiswa Baru")%></h5>

			<ul class="nav nav-tabs" id="mhsTab<%=rnd%>" role="tablist">
				<li class="nav-item">
                    <a class="nav-link active fw-semi-bold"
					id="main-tab-<%=rnd%>" data-bs-toggle="tab"
					href="#mainContent-<%=rnd%>" role="tab"
					aria-controls="mainContent-<%=rnd%>" aria-selected="true"> 
                        <i class="fas fa-id-card me-2"></i>1. <%=Common.getBahasaConfig("Data Akademik & Sistem")%>
				    </a>
                </li>
				<li class="nav-item">
                    <a class="nav-link fw-semi-bold"
					id="bio-tab-<%=rnd%>" data-bs-toggle="tab"
					href="#bioContent-<%=rnd%>" role="tab"
					aria-controls="bioContent-<%=rnd%>" aria-selected="false"> 
                        <i class="fas fa-user-edit me-2"></i>2. <%=Common.getBahasaConfig("Biodata Lengkap")%>
				    </a>
                </li>
			</ul>
		</div>

		<div class="card-body p-4">
			<form id="formInput<%=rnd%>">
				<input type="hidden" id="inputId<%=rnd%>">
                <input type="hidden" id="inputBioId<%=rnd%>">
				
				<div class="tab-content mt-2" id="mhsTabContent<%=rnd%>">
					<!-- TAB 1: DATA AKADEMIK -->
					<div class="tab-pane fade show active" id="mainContent-<%=rnd%>" role="tabpanel">
						<jsp:include page="_form_akademik.jsp">
							<jsp:param name="rnd" value="<%=rnd%>" />
						</jsp:include>
					</div>

					<!-- TAB 2: BIODATA LENGKAP (Sub-tabs) -->
					<div class="tab-pane fade" id="bioContent-<%=rnd%>" role="tabpanel">
						<jsp:include page="_form_biodata.jsp">
							<jsp:param name="rnd" value="<%=rnd%>" />
						</jsp:include>
					</div>
				</div>

				<div class="col-12 text-end mt-3 border-top pt-3">
					<button type="button"
						class="btn btn-light border-0 px-4 rounded-pill fw-semibold shadow-sm"
						onclick="tutupFormData<%=rnd%>()">
						<i class="fas fa-arrow-left text-danger me-1"></i> Kembali / Tutup
					</button>
                    <!-- Tombol simpan global dihapus sesuai instruksi, 
                         tombol simpan sekarang ada di masing-masing tab -->
				</div>
			</form>
		</div>
	</div>
</div>

<script>
    // --- 1. HELPER UNTUK FORMAT TANGGAL ---
    const formatToDateInput = (val) => {
        if(!val) return '';
        if(typeof val === 'number') return new Date(val).toISOString().substring(0, 10);
        if(typeof val === 'string') {
            if(val.includes('T')) return val.substring(0, 10);
            if(val.includes('-') && val.length >= 10) {
                const parts = val.split(' ')[0].split('-');
                if(parts.length === 3 && parts[2].length === 4) return parts[2] + '-' + parts[1] + '-' + parts[0];
                return val.substring(0, 10);
            }
        }
        return '';
    };

    // --- 2. FUNGSI EKSTRAK ID & NAMA (Untuk format "ID-NAMA") ---
    const extractRel = (val) => {
        if (!val) return { id: '', nama: '' };
        const s = String(val);
        const dash = s.indexOf('-');
        if (dash > -1) {
            return { 
                id: s.substring(0, dash).trim(), 
                nama: s.substring(dash + 1).trim() 
            };
        }
        return { id: s, nama: s };
    };

    // --- 3. FUNGSI MODAL ALERT CANTIK ---
    const tampilkanPesan<%=rnd%> = (tipe, pesan, callback) => {
        const modalId = 'modalPesan_<%=rnd%>';
        let modalEl = document.getElementById(modalId);
        
        if (!modalEl) {
            modalEl = document.createElement('div');
            modalEl.id = modalId;
            modalEl.className = 'modal fade';
            modalEl.setAttribute('data-bs-backdrop', 'static'); 
            document.body.appendChild(modalEl);
        }
        
        let iconHtml = tipe === 'success' 
            ? '<i class="fas fa-check-circle fa-4x text-success mb-3 animate__animated animate__tada"></i>'
            : '<i class="fas fa-times-circle fa-4x text-danger mb-3 animate__animated animate__headShake"></i>';
        let warnaText = tipe === 'success' ? 'text-success' : 'text-danger';
        let judul = tipe === 'success' ? 'Berhasil!' : 'Oops, Gagal!';
        
        modalEl.innerHTML = 
            '<div class="modal-dialog modal-dialog-centered modal-sm">' +
                '<div class="modal-content border-0 shadow-lg rounded-4 text-center p-4">' +
                    '<div class="modal-body">' +
                        iconHtml +
                        '<h4 class="fw-bold ' + warnaText + '">' + judul + '</h4>' +
                        '<p class="text-secondary mb-4">' + pesan + '</p>' +
                        '<button type="button" class="btn btn-' + (tipe === 'success' ? 'success' : 'danger') + ' rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal">OK, Mengerti</button>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
        const modalInstance = new bootstrap.Modal(modalEl);
        if (typeof callback === 'function') {
            modalEl.addEventListener('hidden.bs.modal', function handler() {
                callback();
                modalEl.removeEventListener('hidden.bs.modal', handler); 
            });
        }
        modalInstance.show();
    };

    // --- 4. FORM UI LOGIC ---
    const bukaFormData<%=rnd%> = () => {
        document.getElementById('formInput<%=rnd%>').reset();
        document.getElementById('inputId<%=rnd%>').value = '';
        document.getElementById('inputBioId<%=rnd%>').value = '';
        
        // Reset manual picker dan info bio
        ['inputKelasId','inputKelasNama','inputDosenPaId','inputDosenPaNama','inputTglMasuk','inputBioNim','inputBioNama','inputBioProdi'].forEach(id => {
            const el = document.getElementById(id + '<%=rnd%>');
            if(el) el.value = '';
        });

        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-plus-circle text-primary me-2"></i>Tambah Data Mahasiswa';
        
        // Reset Tabs
        const triggerEl = document.querySelector('#mhsTab<%=rnd%> li:first-child a');
        if(triggerEl) bootstrap.Tab.getInstance(triggerEl)?.show() || new bootstrap.Tab(triggerEl).show();

        // Load dropdown Akademik (Wajib Aktif)
        loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', null);
        loadDropdown<%=rnd%>('selectProgram<%=rnd%>', 'ais.database.model.Program', null, 'nama', 'nama', 'nama', 'aktif = true');
        loadDropdown<%=rnd%>('selectStatusAwal<%=rnd%>', 'ais.database.model.StatusAwalMahasiswa', null, 'nama', 'id', 'nama', 'aktif = true');
        loadDropdown<%=rnd%>('selectJenisSeleksi<%=rnd%>', 'ais.database.model.JenisSeleksi', null, 'nama', 'id', 'nama', 'aktif = true');
        loadDropdown<%=rnd%>('selectJenisPembiayaan<%=rnd%>', 'ais.database.model.JenisPembiayaanMahasiswa', null, 'nama', 'id', 'nama', 'aktif = true');
        
        // Load dropdown Biodata (Wajib Aktif)
        loadDropdown<%=rnd%>('selectNegara<%=rnd%>', 'ais.database.model.Negara', null, 'nama', 'id', 'nama', 'aktif = true');
        loadDropdown<%=rnd%>('selectBioAgama<%=rnd%>', 'ais.database.model.Agama', null, 'nama', 'id', 'nama', 'aktif = true');
        loadDropdown<%=rnd%>('selectBioJenisSekolah<%=rnd%>', 'ais.database.model.JenisSekolahMahasiswaBaru', null, 'nama', 'id', 'nama', 'aktif = true');
        loadDropdown<%=rnd%>('selectBioTinggal<%=rnd%>', 'ais.database.model.JenisTinggalMahasiswa', null, 'nama', 'id', 'nama', 'aktif = true');
        loadDropdown<%=rnd%>('selectBioOperator<%=rnd%>', 'ais.database.model.OperatorSeluler', null, 'nama', 'id', 'nama', 'aktif = true');
        loadDropdown<%=rnd%>('selectBioTransportasi<%=rnd%>', 'ais.database.model.AlatTransportasiMahasiswa', null, 'nama', 'id', 'nama', 'aktif = true');

        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupFormData<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        if(typeof loadData<%=rnd%> === 'function') loadData<%=rnd%>();
    };

    // ========================================================================
    // 5. SIMPAN DATA AKADEMIK (Mahasiswa.java)
    // ========================================================================
    const simpanDataAkademik<%=rnd%> = async () => {
        const val = (id) => { const el = document.getElementById(id + '<%=rnd%>'); return el ? el.value.trim() : ''; };
        const check = (id) => { const el = document.getElementById(id + '<%=rnd%>'); return el ? el.checked : false; };

        // Validasi Manual
        if (!val('inputNim')) { tampilkanPesan<%=rnd%>('error', 'NIM wajib diisi!'); return; }
        if (!val('inputNama')) { tampilkanPesan<%=rnd%>('error', 'Nama Lengkap wajib diisi!'); return; }
        if (!val('selectJurusan')) { tampilkanPesan<%=rnd%>('error', 'Jurusan wajib dipilih!'); return; }

        const btn = document.getElementById('btnSimpanAkademik<%=rnd%>');
        const originalBtn = btn ? btn.innerHTML : 'Simpan';
        if(btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Menyimpan...'; }

        const dataObj = {
            nim: val('inputNim'),
            nama: val('inputNama'),
            namaArab: val('inputNamaArab'),
            namaTionghoa: val('inputNamaTionghoa'),
            tahunangkatan: parseInt(val('inputAngkatan')) || null,
            tanggalMasuk: val('inputTglMasuk') || null, 
            tanggalKegiatanBelajarMengajar: val('inputTglMulaiKBM') || null,
            semesterMulai: val('inputSmtMulai'),
            jurusan: parseInt(val('selectJurusan')) || null,
            dosen: parseInt(val('inputDosenPaId')) || null,
            kelas: val('inputKelasNama'), 
            program: val('selectProgram'),
            statusAwalMahasiswa: parseInt(val('selectStatusAwal')) || null,
            jenisSeleksi: parseInt(val('selectJenisSeleksi')) || null,
            jenisPembiayaanMahasiswa: parseInt(val('selectJenisPembiayaan')) || null,
            usernameOjs: val('inputOjs'),
            idfinger: val('inputFinger'),
            lockId: val('inputLockId'),
            feeder: val('inputFeeder'),
            idRegPd: val('inputIdRegPd'),
            linkValidasiEksternal: val('inputLinkValidasi'),
            
            // Biodata Dasar di Mahasiswa.java
            warganegara: val('selectWarganegara'),
            negara: parseInt(val('selectNegara')) || null,
            tempatlahir: val('inputBioTempatLahir'),
            tanggallahir: val('inputBioTglLahir') || null,
            kelamin: val('selectBioKelamin'),
            golongan_darah: val('selectBioGolDarah'),
            tinggi_badan: parseInt(val('inputBioTinggi')) || null,
            berat_badan: parseInt(val('inputBioBerat')) || null,
            keteranganBeasiswa: val('inputKip'),
            keterangan: val('inputKeterangan'),
            agama: parseInt(val('selectBioAgama')) || null,
            bahasa: val('selectBioBahasa')
        };

        try {
            const formData = new FormData();
            formData.append('action', 'simpanDataRinci');
            formData.append('class', '<%=masterClass%>');
            formData.append('id', document.getElementById('inputId<%=rnd%>').value);
            formData.append('data', JSON.stringify(dataObj));

            const res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', body: formData });
            const result = await res.json();
            if(result.status === '00' || result.id) {
                if(result.id) document.getElementById('inputId<%=rnd%>').value = result.id;
                tampilkanPesan<%=rnd%>('success', 'Data Akademik berhasil disimpan.');
            } else {
                tampilkanPesan<%=rnd%>('error', result.description || "Gagal menyimpan data akademik.");
            }
        } catch (e) { tampilkanPesan<%=rnd%>('error', "Terjadi kesalahan koneksi."); }
        finally { if(btn) { btn.disabled = false; btn.innerHTML = originalBtn; } }
    };

    // ========================================================================
    // 6. SIMPAN DATA BIODATA LENGKAP (Memisahkan Class Secara Murni)
    // ========================================================================
    const simpanDataBiodata<%=rnd%> = async () => {
        const idMhs = document.getElementById('inputId<%=rnd%>').value;
        const idBio = document.getElementById('inputBioId<%=rnd%>').value;
        
        if (!idMhs) {
            tampilkanPesan<%=rnd%>('error', 'Silakan simpan Data Akademik terlebih dahulu.');
            return;
        }

        const val = (id) => { const el = document.getElementById(id + '<%=rnd%>'); return el ? el.value.trim() : ''; };
        const check = (id) => { const el = document.getElementById(id + '<%=rnd%>'); return el ? el.checked : false; };

        const btn = document.getElementById('btnSimpanBiodata<%=rnd%>');
        const originalBtn = btn ? btn.innerHTML : 'Simpan Biodata Lengkap';
        if(btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Menyimpan Data...'; }

        try {
            // --- TAHAP 1: UPDATE DATA FISIK KE TABEL MAHASISWA (Jika Ada Perubahan di Tab 2) ---
            const dataFisikMhs = {
                golongan_darah: val('selectBioGolDarah'),
                tinggi_badan: parseInt(val('inputBioTinggi')) || 0,
                berat_badan: parseInt(val('inputBioBerat')) || 0,
                kelamin: val('selectBioKelamin'),
                tempatlahir: val('inputBioTempatLahir'),
                tanggallahir: val('inputBioTglLahir') || null,
                agama: parseInt(val('selectBioAgama')) || null,
                bahasa: val('selectBioBahasa')
            };

            await fetch('<%=Common.ROOT%>/Data', { 
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'simpanDataRinci', class: '<%=masterClass%>', id: idMhs, data: dataFisikMhs }) 
            });

            // --- TAHAP 2: SIMPAN DATA DETAIL KE TABEL BIODATAMAHASISWA (Tanpa Prefix) ---
            const dataBioDetail = {
                mahasiswa: parseInt(idMhs), 
                nisn: val('inputBioNisn'),
                nirm: val('inputBioNirm'),
                npwp: val('inputBioNpwp'),
                email: val('inputBioEmail'), 
                hp: val('inputBioHp'),       
                suratIzinMengemudi: val('inputBioSim'), 
                kodeKerjaan: val('inputBioBank'),
                cabangBri: val('inputBioAtasNamaRek'),
                no_rek_bri: val('inputBioNoRek'),
                namaUntukIjazah: val('inputBioNamaIjazah'),
                noIjazah: val('inputBioNoIjazah'),
                
                asalSma: val('inputBioAsalSma'),
                npsn: val('inputBioNpsn'),
                alamatAsalSma: val('inputBioAlamatSma'),
                
                asalSmp: val('inputBioAsalSmp'),
                alamatAsalSmp: val('inputBioAlamatSmp'), 
                asalSd: val('inputBioAsalSd'),
                alamatAsalSd: val('inputBioAlamatSd'),   
                apakahPernahPaud: check('checkBioPaud'), 
                apakahPernahTk: check('checkBioTk'),     
                
                pernahMenetapDiLuarNegeri: check('checkBioLuarNegeri') ? 1 : 0, 
                pernahMemimpinOrganisasi: check('checkBioOrganisasi') ? 1 : 0,
                namaOrganisasi: val('inputBioNamaOrganisasi'),
                ukuranJaket: val('selectBioJaket'),
                hobi: val('inputBioHobi'),
                minatSeni: val('inputBioSeni'),
                kemampuanBahasa1: val('inputBioBahasa1'),
                kemampuanBahasa2: val('inputBioBahasa2'),
                kemampuanBahasa3: val('inputBioBahasa3'),
                statusNikah: parseInt(val('selectBioStatusNikah')) || 0,
                
                operatorSeluler: parseInt(val('selectBioOperator')) || null,
                jenisSekolah: parseInt(val('selectBioJenisSekolah')) || null,
                jenisTinggalMahasiswa: parseInt(val('selectBioTinggal')) || null,
                alatTransportasiMahasiswa: parseInt(val('selectBioTransportasi')) || null
            };

            const reqJsonBio = { action: 'simpanDataRinci', class: 'ais.database.model.BiodataMahasiswa', data: dataBioDetail };
            if (idBio) reqJsonBio.id = idBio; 

            const resJsonBio = await fetch('<%=Common.ROOT%>/Data', { 
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqJsonBio) 
            });
            const resultBio = await resJsonBio.json();

         // --- TAHAP 3: UPLOAD FILE (VIA NATIVE JSON INJECTION & JAVA CONSTANTS) ---
            if (resultBio.status === '00' || resultBio.id) {
                let finalBioId = idBio || resultBio.id;
                document.getElementById('inputBioId<%=rnd%>').value = finalBioId;

                const fileTtd = document.getElementById('fileBioTtd<%=rnd%>');
                const fileOrg = document.getElementById('fileBioSuratOrganisasi<%=rnd%>');
                
                let uploadError = false;
                let adaUpload = false;

                // Konversi File ke Base64 (Data URL)
                const getBase64 = (file) => {
                    return new Promise((resolve, reject) => {
                        const reader = new FileReader();
                        reader.readAsDataURL(file);
                        reader.onload = () => resolve(reader.result); 
                        reader.onerror = error => reject(error);
                    });
                };

                if ((fileTtd && fileTtd.files[0]) || (fileOrg && fileOrg.files[0])) {
                    adaUpload = true;
                    if(btn) btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Menyimpan Lampiran...';
                    
                    // A. TANDA TANGAN MAHASISWA
                    if (fileTtd && fileTtd.files[0]) {
                        try {
                            const base64Str = await getBase64(fileTtd.files[0]);
                            
                            const reqTtd = {
                                action: 'simpanDataRinci',
                                class: 'ais.database.model.file.LampiranLain',
                                data: {
                                    ref: parseInt(idMhs),
                                    // Mengambil konstanta langsung dari class Java!
                                    jenis: "<%=ais.database.model.file.LampiranLain.TTD_MAHASISWA%>", 
                                    nama: "Tanda Tangan",
                                    foto: base64Str.split(",")[1] 
                                }
                            };

                            const resTtd = await fetch('<%=Common.ROOT%>/Data', { 
                                method: 'POST', 
                                headers: { 'Content-Type': 'application/json' }, 
                                body: JSON.stringify(reqTtd) 
                            });
                            const jsonTtd = await resTtd.json();
                            if (jsonTtd.status !== '00' && !jsonTtd.id) uploadError = true;
                        } catch(e) { 
                            console.error("Gagal TTD:", e);
                            uploadError = true; 
                        }
                    }

                    // B. SURAT PENUNJUKAN ORGANISASI
                    if (fileOrg && fileOrg.files[0]) {
                        try {
                            const base64Org = await getBase64(fileOrg.files[0]);
                            
                            const reqOrg = {
                                action: 'simpanDataRinci',
                                class: 'ais.database.model.file.LampiranLainMahasiswa',
                                data: {
                                    mahasiswa: parseInt(idMhs), 
                                    ref: parseInt(idMhs),
                                    // Mengambil konstanta langsung dari class Java!
                                    jenis: "<%=ais.database.model.file.LampiranLainMahasiswa.SURAT_PENUNJUKAN_PENGURUS_ORGANISASI%>", 
                                    nama: "Surat Penunjukan Pengurus Organisasi",
                                    foto: base64Org.split(",")[1]
                                }
                            };

                            const resOrg = await fetch('<%=Common.ROOT%>/Data', { 
                                method: 'POST', 
                                headers: { 'Content-Type': 'application/json' }, 
                                body: JSON.stringify(reqOrg) 
                            });
                            const jsonOrg = await resOrg.json();
                            if (jsonOrg.status !== '00' && !jsonOrg.id) uploadError = true;
                        } catch(e) { 
                            console.error("Gagal ORG:", e);
                            uploadError = true; 
                        }
                    }
                }

                if (adaUpload && uploadError) {
                    tampilkanPesan<%=rnd%>('warning', 'Biodata tersimpan, tetapi ada kendala sistem saat memproses konversi gambar lampiran Anda. Server mungkin tidak mendukung injeksi Base64 secara langsung.');
                } else {
                    tampilkanPesan<%=rnd%>('success', 'Biodata Lengkap beserta File Lampiran berhasil disimpan!');
                }
            } else {
                tampilkanPesan<%=rnd%>('error', resultBio.description || "Gagal menyimpan detail biodata.");
            }
        } catch (e) { 
             console.error("Fetch Error:", e);
             tampilkanPesan<%=rnd%>('error', "Terjadi kesalahan koneksi saat mengirim data."); 
        }
        finally { if(btn) { btn.disabled = false; btn.innerHTML = originalBtn; } }
    };

 // ========================================================================
    // 7. FUNGSI KHUSUS LOAD BIODATA (BiodataMahasiswa.java)
    // ========================================================================
    const loadDataBiodata<%=rnd%> = async (idMhs) => {
        try {
            // Kita gunakan 'daftar' untuk mencari Biodata berdasarkan ID Mahasiswa
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', 
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    action: "daftar", 
                    class: "ais.database.model.BiodataMahasiswa", 
                    where1: "mahasiswa = " + idMhs 
                })
            });
            const result = await res.json();
            
            //console.log('response biodata ' + JSON.stringify(result));
            
            const setVal = (elemId, val) => { const el = document.getElementById(elemId + '<%=rnd%>'); if(el) el.value = val || ''; };
            const setCheck = (elemId, val) => { const el = document.getElementById(elemId + '<%=rnd%>'); if(el) el.checked = (val === true || val === 'true' || val === 1 || val === '1'); };

            // Jika biodata ditemukan, masukkan ke form
            if (result.data && result.data.length > 0) {
                const b = result.data[0]; // Ambil record biodata pertama
                
                // Simpan ID Biodata untuk proses UPDATE nanti
                document.getElementById('inputBioId<%=rnd%>').value = b.id || '';

                // Mapping Property sesuai BiodataMahasiswaAction.java
                setVal('inputBioNisn', b.nisn);
                setVal('inputBioNirm', b.nirm);
                setVal('inputBioNpwp', b.npwp);
                setVal('inputBioEmail', b.email || b.alamatEmail); 
                setVal('inputBioHp', b.hp || b.noHp); 
                setVal('inputBioSim', b.suratIzinMengemudi); 
                setVal('inputBioBank', b.kodeKerjaan);         // Nama Bank disimpan di kodeKerjaan
                setVal('inputBioAtasNamaRek', b.cabangBri);    // Atas nama rek disimpan di cabangBri
                setVal('inputBioNoRek', b.no_rek_bri);         // No Rekening
                setVal('inputBioNamaIjazah', b.namaUntukIjazah);
                setVal('inputBioNoIjazah', b.noIjazah);
                
                setVal('inputBioAsalSma', b.asalSma);
                setVal('inputBioNpsn', b.npsn);
                setVal('inputBioAlamatSma', b.alamatAsalSma);
                setVal('inputBioAsalSmp', b.asalSmp);
                setVal('inputBioAlamatSmp', b.alamatAsalSmp);
                setVal('inputBioAsalSd', b.asalSd);
                setVal('inputBioAlamatSd', b.alamatAsalSd);
                
                setVal('inputBioNamaOrganisasi', b.namaOrganisasi);
                setVal('selectBioJaket', b.ukuranJaket);
                setVal('inputBioHobi', b.hobi);
                setVal('inputBioSeni', b.minatSeni);
                setVal('inputBioBahasa1', b.kemampuanBahasa1);
                setVal('inputBioBahasa2', b.kemampuanBahasa2);
                setVal('inputBioBahasa3', b.kemampuanBahasa3);
                setVal('selectBioStatusNikah', b.statusNikah || 0);

                // Mapping Checkbox (Boolean)
                setCheck('checkBioPaud', b.apakahPernahPaud);
                setCheck('checkBioTk', b.apakahPernahTk);
                setCheck('checkBioLuarNegeri', b.pernahMenetapDiLuarNegeri);
                
                // Organisasi
                setCheck('checkBioOrganisasi', b.pernahMemimpinOrganisasi);
                const divOrg = document.getElementById('divOrgDetail<%=rnd%>');
                if(divOrg) {
                    divOrg.style.display = (b.pernahMemimpinOrganisasi == 1 || b.pernahMemimpinOrganisasi === '1' || b.pernahMemimpinOrganisasi === true) ? 'flex' : 'none';
                }

                // Load Dropdown Relasi Khusus Biodata
                const d = (elemId, cls, val) => loadDropdown<%=rnd%>(elemId + '<%=rnd%>', 'ais.database.model.' + cls, val, 'nama', 'id', 'nama', 'aktif = true');
                
                d('selectBioOperator', 'OperatorSeluler', extractRel(b.operatorSeluler).id);
                d('selectBioJenisSekolah', 'JenisSekolahMahasiswaBaru', extractRel(b.jenisSekolah).id);
                d('selectBioTinggal', 'JenisTinggalMahasiswa', extractRel(b.jenisTinggalMahasiswa).id);
                d('selectBioTransportasi', 'AlatTransportasiMahasiswa', extractRel(b.alatTransportasiMahasiswa).id);
                
            } else {
                // Jika belum ada biodata sama sekali (INSERT baru)
                document.getElementById('inputBioId<%=rnd%>').value = '';
                console.log("Biodata detail belum diisi untuk mahasiswa ini.");
            }
        } catch(e) {
            console.error("Gagal menarik data biodata detail", e);
        }
    };

    // ========================================================================
    // 8. EDIT DATA UTAMA (DUAL FETCH)
    // ========================================================================
    const editData<%=rnd%> = async (id) => {
        bukaFormData<%=rnd%>();
        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-edit text-warning me-2"></i>Edit Data Mahasiswa';
        document.getElementById('inputId<%=rnd%>').value = id;

        // Fetch Data Akademik Utama
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({action: "load", class: '<%=masterClass%>', id: id})
        });
        
        const result = await res.json();
        const data = result.data;
        if (!data) return;

        const setVal = (id, val) => { const el = document.getElementById(id + '<%=rnd%>'); if(el) el.value = val || ''; };
        const setCheck = (id, val) => { const el = document.getElementById(id + '<%=rnd%>'); if(el) el.checked = (val === true || val === 'true'); };
        
        // --- DATA AKADEMIK (Mahasiswa.java) ---
        setVal('inputNim', data.nim);
        setVal('inputNama', data.nama);
        setVal('inputNamaArab', data.namaArab);
        setVal('inputNamaTionghoa', data.namaTionghoa);
        setVal('inputAngkatan', data.tahunangkatan);
        setVal('inputTglMasuk', formatToDateInput(data["tanggalMasuk.iso"] || data.tanggalMasuk));
        setVal('inputSmtMulai', data.semesterMulai);
        setVal('inputTglMulaiKBM', formatToDateInput(data["tanggalKegiatanBelajarMengajar.iso"] || data.tanggalKegiatanBelajarMengajar));

        const dosen = extractRel(data.dosenPa || data.dosen);
        setVal('inputDosenPaId', dosen.id);
        setVal('inputDosenPaNama', dosen.nama);
        setVal('inputKelasNama', data.kelas);
        setVal('inputKelasId', ''); 

        const jur = extractRel(data.jurusan);
        await loadFakultas<%=rnd%>('selectFakultas<%=rnd%>', data["jurusan.fakultas"], async (foundFakId) => {
            if(foundFakId) await loadJurusan<%=rnd%>(foundFakId, jur.id);
        });

        setCheck('checkDosenPaSelaluSama', data.dosenPaSelaluSama);
        setCheck('checkKelasSelaluSama', data.kelasSelaluSama);
        setCheck('checkProgramSelaluIkut', data.programSelaluIkutDataUtama);
        setCheck('checkStatusAwalSelaluIkut', data.statusAwalSelaluIkutDataUtama);
        setVal('inputOjs', data.usernameOjs);
        setVal('inputFinger', data.idfinger);
        setVal('inputLockId', data.lockId);
        setVal('inputFeeder', data.feeder);
        setVal('inputIdRegPd', data.idRegPd);
        setVal('inputLinkValidasi', data.linkValidasiEksternal);

        // --- BIODATA INDUK (Property di Mahasiswa.java) ---
        setVal('inputBioNim', data.nim);
        setVal('inputBioNama', data.nama);
        setVal('inputBioProdi', jur.nama);
        setVal('selectWarganegara', data.warganegara);
        setVal('inputBioTempatLahir', data.tempatlahir);
        setVal('inputBioTglLahir', formatToDateInput(data["tanggallahir.iso"] || data.tanggallahir));
        setVal('selectBioKelamin', data.kelamin);
        setVal('selectBioGolDarah', data.golongan_darah);
        setVal('inputBioTinggi', data.tinggi_badan);
        setVal('inputBioBerat', data.berat_badan);
        setVal('selectBioBahasa', data.bahasa);
        setVal('inputKip', data.keteranganBeasiswa);
        setVal('inputKeterangan', data.keterangan);

        // Load Dropdown Relasi Induk
        const d = (elemId, cls, val) => loadDropdown<%=rnd%>(elemId + '<%=rnd%>', 'ais.database.model.' + cls, val, 'nama', 'id', 'nama', 'aktif = true');
        d('selectProgram', 'Program', data.program);
        d('selectStatusAwal', 'StatusAwalMahasiswa', extractRel(data.statusAwalMahasiswa).id);
        d('selectJenisSeleksi', 'JenisSeleksi', extractRel(data.jenisSeleksi).id);
        d('selectJenisPembiayaan', 'JenisPembiayaanMahasiswa', extractRel(data.jenisPembiayaanMahasiswa).id);
        d('selectNegara', 'Negara', extractRel(data.negara).id);
        d('selectBioAgama', 'Agama', extractRel(data.agama).id);

        // --- FETCH & LOAD DATA BIODATA DETAIL ---
        // Kita panggil fungsi khusus yang baru saja dibuat di atas
        loadDataBiodata<%=rnd%>(id);
    };

    // --- 8. UNIVERSAL DROPDOWN LOADER ---
    const loadDropdown<%=rnd%> = async (targetId, entityClass, selectedId, sortField = "nama", valField = "id", txtField = "nama", where = "") => {
        const req = { action: "daftar", class: entityClass, sort1: sortField, max: 1000 };
        if(where) req.where1 = where;
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(req)
            });
            const d = await res.json();
            let opt = '<option value="">-- Pilih --</option>';
            (d.data || []).forEach(item => {
                let isSelected = (selectedId == item[valField] || selectedId == item[txtField]) ? 'selected' : '';
                opt += '<option value="' + item[valField] + '" ' + isSelected + '>' + item[txtField] + '</option>';
            });
            const el = document.getElementById(targetId);
            if(el) el.innerHTML = opt;
        } catch(e) { console.error(e); }
    };

    const loadFakultas<%=rnd%> = async (targetId, selectedRef, callback) => {
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({action:"daftar", class:"ais.database.model.Fakultas", where1: "aktif = true", sort1:"nama"})
        });
        const d = await res.json();
        let opt = '<option value="">-- Pilih Fakultas --</option>';
        let foundId = '';
        (d.data || []).forEach(f => {
            let isSelected = (selectedRef == f.id || selectedRef == f.nama) ? 'selected' : '';
            if(isSelected) foundId = f.id;
            opt += '<option value="' + f.id + '" ' + isSelected + '>' + f.nama + '</option>';
        });
        document.getElementById(targetId).innerHTML = opt;
        if(callback) callback(foundId);
    };

    const loadJurusan<%=rnd%> = async (idFak, selectedId) => {
        let where = "aktif = true";
        if (idFak) where += " and fakultas = " + idFak;
        const res = await fetch('<%=Common.ROOT%>/Data', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({action:"daftar", class:"ais.database.model.Jurusan", where1: where, sort1: "nama"})
        });
        const d = await res.json();
        let opt = '<option value="">-- Pilih Jurusan --</option>';
        (d.data || []).forEach(j => {
            let isSelected = (selectedId == j.id) ? 'selected' : '';
            opt += '<option value="' + j.id + '" ' + isSelected + '>' + j.nama + '</option>';
        });
        const el = document.getElementById('selectJurusan<%=rnd%>');
        if(el) el.innerHTML = opt;
    };
</script>