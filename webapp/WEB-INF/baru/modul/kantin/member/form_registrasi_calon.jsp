<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.common.Common"%>
<%
String rnd = Common.getGeneratedBarCode(7);
%>

<jsp:include page="/WEB-INF/baru/include/header.jsp"></jsp:include>

  <body>

    <!-- ===============================================-->
    <!--    Main Content-->
    <!-- ===============================================-->
    <main class="main" id="top">
      <div class="container-fluid">

<style>
    .upload-box-<%=rnd%> {
        border: 2px dashed #0d6efd;
        border-radius: 12px;
        padding: 20px;
        text-align: center;
        background-color: #f8f9fa;
        cursor: pointer;
        transition: all 0.3s;
    }
    .upload-box-<%=rnd%>:hover {
        background-color: #e9ecef;
        border-color: #0b5ed7;
    }
    .form-control:focus, .form-select:focus {
        box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.15);
    }
    .readonly-overlay-<%=rnd%> {
        pointer-events: none;
        opacity: 0.7;
    }
</style>

<div class="container py-4 animate__animated animate__fadeIn" id="mainContainer<%=rnd%>">
    <div class="row justify-content-center">
    	<div class="content">
    		<%
	          try{
	              %>
	              <jsp:include page="/WEB-INF/baru/include/navbar.jsp"></jsp:include>
	              <%
	    	  }catch(Exception e){
	    		  e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/form_registrasi_calon.jsp:49");
	    	  }
	          %>
	    
	        <div class="col-lg-12">
	            
	            <div class="text-center mb-4">
	                <div class="d-inline-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary rounded-circle mb-3" style="width: 70px; height: 70px;">
	                    <i class="fas fa-user-plus fa-2x"></i>
	                </div>
	                <h3 class="fw-bold text-dark mb-1"><%=Common.getBahasaConfig("Formulir Pendaftaran Keanggotaan")%></h3>
	                <p class="text-muted"><%=Common.getBahasaConfig("Silakan lengkapi data diri Anda di bawah ini untuk bergabung menjadi anggota.")%></p>
	            </div>
	
	            <div class="alert alert-warning shadow-sm border-0 rounded-4 mb-4" id="alertStatus<%=rnd%>" style="display: none;">
	                <div class="d-flex align-items-center">
	                    <i class="fas fa-info-circle fa-2x me-3 text-warning"></i>
	                    <div>
	                        <h6 class="fw-bold mb-0 text-dark" id="alertTitle<%=rnd%>"><%=Common.getBahasaConfig("Status")%></h6>
	                        <span class="small text-dark" id="alertMessage<%=rnd%>"><%=Common.getBahasaConfig("Pesan")%></span>
	                    </div>
	                </div>
	            </div>
	
	            <div class="card border-0 shadow-lg rounded-4 overflow-hidden">
	                <div class="card-header bg-primary bg-gradient text-white py-3 border-0">
	                    <h6 class="fw-bold mb-0"><i class="fas fa-id-card me-2"></i><%=Common.getBahasaConfig("Data Calon Anggota")%></h6>
	                </div>
	                
	                <div class="card-body p-4 p-md-5" id="formWrapper<%=rnd%>">
	                    <form id="formPendaftaran<%=rnd%>" onsubmit="event.preventDefault(); prosesSimpanPendaftaran<%=rnd%>();">
	                        <input type="hidden" id="inputIdCalon<%=rnd%>" value="">
	                        
	                        <div class="row g-4">
	                            
	                            <div class="col-12 mb-2">
	                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-user me-2"></i><%=Common.getBahasaConfig("Identitas Utama (Wajib Diisi)")%></h6>
	                                <small class="text-muted"><i class="fas fa-asterisk text-danger me-1" style="font-size: 10px;"></i><%=Common.getBahasaConfig("Ketiga data ini digunakan untuk memverifikasi status pendaftaran Anda.")%></small>
	                            </div>
	
	                            <div class="col-md-12">
	                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nama Lengkap")%> <span class="text-danger">*</span></label>
	                                <div class="input-group shadow-sm">
	                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-font text-muted"></i></span>
	                                    <input type="text" class="form-control border-start-0 fw-bold" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Masukkan nama lengkap sesuai identitas")%>" required onblur="cekPendaftaranAktif<%=rnd%>()">
	                                </div>
	                            </div>
	
	                            <div class="col-md-6">
	                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor HP / WhatsApp")%> <span class="text-danger">*</span></label>
	                                <div class="input-group shadow-sm">
	                                    <span class="input-group-text bg-light border-end-0"><i class="fab fa-whatsapp text-success"></i></span>
	                                    <input type="tel" class="form-control border-start-0 fw-medium" id="inputHp<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: 08123456789")%>" required onblur="cekPendaftaranAktif<%=rnd%>()">
	                                </div>
	                            </div>
	
	                            <div class="col-md-6">
	                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Alamat Email Aktif")%> <span class="text-danger">*</span></label>
	                                <div class="input-group shadow-sm">
	                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-envelope text-muted"></i></span>
	                                    <input type="email" class="form-control border-start-0" id="inputEmail<%=rnd%>" placeholder="<%=Common.getBahasaConfig("nama@email.com")%>" required onblur="cekPendaftaranAktif<%=rnd%>()">
	                                </div>
	                            </div>
	
	                            <div class="col-12 mt-4 mb-2">
	                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-sitemap me-2"></i><%=Common.getBahasaConfig("Kategori & Jenis Keanggotaan")%></h6>
	                            </div>
	
	                            <div class="col-md-6">
	                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kategori Referensi Sivitas")%> <span class="text-danger">*</span></label>
	                                <select class="form-select shadow-sm fw-semibold" id="inputTipeAnggota<%=rnd%>" onchange="aturTampilanPencarian<%=rnd%>()" required>
	                                    <option value=""><%=Common.getBahasaConfig("Memuat data kategori...")%></option>
	                                </select>
	                            </div>
	
	                            <div class="col-md-6">
	                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Pendaftaran Koperasi")%> <span class="text-danger">*</span></label>
	                                <select class="form-select shadow-sm fw-semibold" id="inputJenisAnggota<%=rnd%>" required onchange="generateAndSetKodeMember<%=rnd%>()">
	                                    <option value=""><%=Common.getBahasaConfig("Memuat data jenis...")%></option>
	                                </select>
	                            </div>
	
	                            <div class="col-12 p-3 bg-primary bg-opacity-10 rounded-3 border border-primary border-opacity-25" id="areaPencarianRef<%=rnd%>" style="display: none;">
	                                <label class="form-label small fw-bold text-primary mb-1" id="labelPencarianRef<%=rnd%>"><%=Common.getBahasaConfig("Cari Data Induk Sivitas...")%></label>
	                                <div class="input-group shadow-sm">
	                                    <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-primary"></i></span>
	                                    <input type="text" class="form-control border-start-0 fw-bold" list="listReferensiSivitas<%=rnd%>" id="inputCariReferensi<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik minimal 3 huruf/angka...")%>" autocomplete="off">
	                                </div>
	                                <datalist id="listReferensiSivitas<%=rnd%>"></datalist>
	                                <input type="hidden" id="idReferensiTerpilih<%=rnd%>" value="">
	                                <small class="text-muted mt-2 d-block"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Pilih data Anda dari sistem agar terintegrasi secara otomatis.")%></small>
	                            </div>
                                
                                <div class="col-md-6 mt-4">
                                    <label class="form-label small fw-semibold text-secondary"><%=Common.getBahasaConfig("Kode Member Koperasi")%> <span class="text-danger">*</span></label>
                                    <div class="input-group input-group-sm shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-fingerprint text-primary"></i></span>
                                        <input type="text" class="form-control fw-bold border-start-0 bg-light text-primary" id="inputKodeMember<%=rnd%>" readonly placeholder="<%=Common.getBahasaConfig("Otomatis Digenerate...")%>" required>
                                    </div>
                                    <small class="text-muted" style="font-size: 11px;"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Kode ini dibuat otomatis oleh sistem dan tidak dapat diubah.")%></small>
                                </div>
	
	                            <div class="col-12 mt-4 mb-2">
	                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-user-lock me-2"></i><%=Common.getBahasaConfig("Kredensial Akun (Untuk Login)")%></h6>
	                            </div>
	
	                            <div class="col-md-6">
	                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("User ID (Username)")%> <span class="text-danger">*</span></label>
	                                <div class="input-group shadow-sm">
	                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-user-circle text-muted"></i></span>
	                                    <input type="text" class="form-control border-start-0" id="inputUserId<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Buat username yang mudah diingat")%>" required onblur="cekKetersediaanUserId<%=rnd%>()">
	                                </div>
	                                <small id="userIdFeedback<%=rnd%>" class="text-danger mt-1 d-block" style="display: none;"></small>
	                            </div>
	
	                            <div class="col-md-6">
	                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kata Sandi (Password)")%> <span class="text-danger">*</span></label>
	                                <div class="input-group shadow-sm">
	                                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-key text-muted"></i></span>
	                                    <input type="password" class="form-control border-start-0" id="inputPassword<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Buat kata sandi akun Anda")%>" required>
	                                </div>
	                                <small class="text-muted fst-italic" id="hintPassword<%=rnd%>" style="display: none; font-size: 11px;"><%=Common.getBahasaConfig("Kosongkan jika tidak ingin mengubah kata sandi lama.")%></small>
	                            </div>
	
	                            <div class="col-12 mt-4 mb-2">
	                                <h6 class="fw-bold text-primary border-bottom pb-2"><i class="fas fa-camera me-2"></i><%=Common.getBahasaConfig("Unggah Pas Foto")%></h6>
	                            </div>
	
	                            <div class="col-12">
	                                <div class="upload-box-<%=rnd%> shadow-sm" onclick="document.getElementById('inputFileFoto<%=rnd%>').click()">
	                                    <i class="fas fa-cloud-upload-alt fa-3x text-primary mb-3"></i>
	                                    <h6 class="fw-bold text-dark"><%=Common.getBahasaConfig("Klik di sini untuk memilih foto")%></h6>
	                                    <p class="small text-muted mb-0"><%=Common.getBahasaConfig("Format yang didukung: JPG, JPEG, PNG. Maksimal 2MB.")%></p>
	                                    <p class="small text-success fw-bold mt-2 mb-0" id="namaFileTerpilih<%=rnd%>" style="display: none;"></p>
	                                </div>
	                                <input type="file" id="inputFileFoto<%=rnd%>" accept="image/jpeg, image/png, image/jpg" style="display: none;" onchange="tampilkanNamaFile<%=rnd%>(this)">
	                            </div>
	
	                            <div class="col-12 mt-5 border-top pt-4 text-center d-flex flex-column flex-md-row justify-content-center gap-3">
	                                <button type="button" class="btn btn-light btn-lg rounded-pill px-5 shadow-sm fw-bold border" onclick="history.back()">
	                                    <i class="fas fa-arrow-left me-2 text-secondary"></i><%=Common.getBahasaConfig("Kembali")%>
	                                </button>
	                                <button type="submit" class="btn btn-primary btn-lg rounded-pill px-5 shadow fw-bold" id="btnKirimPendaftaran<%=rnd%>">
	                                    <i class="fas fa-paper-plane me-2"></i><%=Common.getBahasaConfig("Kirim Pendaftaran")%>
	                                </button>
	                            </div>
	                        </div>
	                    </form>
	                </div>
	            </div>
	            
	        </div>
	    	
	    	
    	</div>
    </div>
</div>

<script>
    // ==========================================
    // UTILITIES & VARIABEL GLOBAL
    // ==========================================
    let timerPengecekan<%=rnd%> = null;
    let timerPencarianRef<%=rnd%> = null;
    let daftarReferensiSivitas<%=rnd%> = [];
    let isDataTerkunci<%=rnd%> = false; 
    let isUserIdAvailable<%=rnd%> = true; // Flag status ketersediaan User ID

    const fetchDataAPI<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql", tanpaLogin: "true" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("API Error:", e); 
            return []; 
        }
    };

    const showToastUI<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    const tampilkanNamaFile<%=rnd%> = (input) => {
        const lbl = document.getElementById('namaFileTerpilih<%=rnd%>');
        if (input.files && input.files[0]) {
            lbl.innerHTML = '<i class="fas fa-check-circle me-1"></i> <%=Common.getBahasaConfig("File siap diunggah: ")%>' + input.files[0].name;
            lbl.style.display = 'block';
        } else {
            lbl.style.display = 'none';
        }
    };

    // ==========================================
    // INIT MASTER DATA (Combo Jenis & Tipe Anggota)
    // ==========================================
    const muatJenisAnggotaKoperasi<%=rnd%> = async () => {
        const sql = "SELECT id, nama, kode FROM koperasi.jenis_anggota_koperasi WHERE aktif = true AND dipilih = true ORDER BY nama ASC";
        const res = await fetchDataAPI<%=rnd%>(sql);
        let optHtml = '<option value=""><%=Common.getBahasaConfig("-- Pilih Jenis Keanggotaan --")%></option>';
        res.forEach(item => {
            optHtml += '<option value="' + item.id + '" data-kode="' + (item.kode || 'MEM') + '">' + item.nama + '</option>';
        });
        document.getElementById('inputJenisAnggota<%=rnd%>').innerHTML = optHtml;
    };

    const loadComboTipeAnggota<%=rnd%> = async () => {
        // Ambil data TipeAnggotaKoperasi (Kategori Referensi Sivitas)
        const sql = "SELECT id, nama, kode FROM koperasi.tipe_anggota_koperasi WHERE aktif = true ORDER BY nama ASC";
        const res = await fetchDataAPI<%=rnd%>(sql);
        let optHtml = '<option value="" data-nama="UMUM"><%=Common.getBahasaConfig("-- Pilih Kategori Sivitas --")%></option>';
        res.forEach(item => {
            const safeNama = item.nama ? item.nama.toUpperCase() : 'UMUM';
            optHtml += '<option value="' + item.id + '" data-nama="' + safeNama + '">' + item.nama + '</option>';
        });
        document.getElementById('inputTipeAnggota<%=rnd%>').innerHTML = optHtml;
    };

    // ==========================================
    // CEK KETERSEDIAAN USER ID
    // ==========================================
    const cekKetersediaanUserId<%=rnd%> = async () => {
        const inputUserId = document.getElementById('inputUserId<%=rnd%>');
        const feedback = document.getElementById('userIdFeedback<%=rnd%>');
        const userId = inputUserId.value.trim().replace(/'/g, "''");

        if (userId === '') {
            feedback.style.display = 'none';
            inputUserId.classList.remove('is-invalid');
            isUserIdAvailable<%=rnd%> = false;
            return;
        }

        feedback.style.display = 'block';
        feedback.className = 'text-info small mt-1 d-block';
        feedback.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i><%=Common.getBahasaConfig("Mengecek ketersediaan username...")%>';

        // Query cek apakah username sudah ada di tabel tbmuser
        const sqlCekUser = "SELECT userid FROM public.tbmuser WHERE userid = '" + userId + "' LIMIT 1";
        
        try {
            const res = await fetchDataAPI<%=rnd%>(sqlCekUser);
            if (res && res.length > 0) {
                // Username sudah terpakai
                feedback.className = 'text-danger small fw-bold mt-1 d-block';
                feedback.innerHTML = '<i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Username sudah terpakai, silakan gunakan username lain.")%>';
                inputUserId.classList.add('is-invalid');
                isUserIdAvailable<%=rnd%> = false;
                showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Username sudah terpakai, silakan gunakan username lain.")%>', 'bg-danger text-white');
            } else {
                // Username tersedia
                feedback.className = 'text-success small fw-bold mt-1 d-block';
                feedback.innerHTML = '<i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Username tersedia.")%>';
                inputUserId.classList.remove('is-invalid');
                inputUserId.classList.add('is-valid');
                isUserIdAvailable<%=rnd%> = true;
            }
        } catch (error) {
            console.error("Gagal mengecek User ID", error);
            feedback.style.display = 'none';
        }
    };


    // ==========================================
    // AUTO GENERATOR KODE MEMBER (KOLOM 'KODE')
    // ==========================================
    const extractPrefix<%=rnd%> = () => {
        const elJenis = document.getElementById('inputJenisAnggota<%=rnd%>');
        if (elJenis && elJenis.selectedIndex > 0) {
            const kode = elJenis.options[elJenis.selectedIndex].getAttribute('data-kode');
            if (kode && kode.trim() !== '') return kode.toUpperCase();
        } 
        return "MEM";
    };

    const generateAndSetKodeMember<%=rnd%> = async () => {
        const elKode = document.getElementById('inputKodeMember<%=rnd%>');
        elKode.value = '<%=Common.getBahasaConfigJS("Menghitung Kode...")%>';

        const prefix = extractPrefix<%=rnd%>();
        const idJenis = document.getElementById('inputJenisAnggota<%=rnd%>').value;
        
        const date = new Date();
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const yyyy = date.getFullYear();

        try {
            // Gunakan COALESCE(MAX(id), 0) untuk memastikan sequence tidak mundur walau ada data yang dihapus
            const sqlAll = "SELECT COALESCE(MAX(id), 0) as max_id FROM koperasi.anggota_koperasi";
            const resAll = await fetchDataAPI<%=rnd%>(sqlAll);
            let countAll = (resAll && resAll[0]) ? parseInt(resAll[0].max_id) : 0;

            // Hitung sequence berdasarkan Jenis Anggota
            let countJenis = 0;
            if (idJenis) {
                const sqlJenis = "SELECT COUNT(id) as jml FROM koperasi.anggota_koperasi WHERE jenis_anggota_koperasi = " + idJenis;
                const resJenis = await fetchDataAPI<%=rnd%>(sqlJenis);
                countJenis = (resJenis && resJenis[0]) ? parseInt(resJenis[0].jml) : 0;
            }

            let numJenis = countJenis + 1;
            let numAll = countAll + 1;
            let proposedCode = "";
            let isUnique = false;

            // Loop Pengecekan Ketersediaan 'kode' di database
            while (!isUnique) {
                proposedCode = prefix + "-" + numJenis + "/" + mm + "/" + yyyy + "/" + numAll;
                let sqlCheck = "SELECT id FROM koperasi.anggota_koperasi WHERE kode = '" + proposedCode + "'";
                let resCheck = await fetchDataAPI<%=rnd%>(sqlCheck);
                
                if (resCheck && resCheck.length > 0) {
                    numJenis++;
                    numAll++;
                } else {
                    isUnique = true;
                }
            }
            
            elKode.value = proposedCode;
            
        } catch (e) {
            console.error("Error generating code", e);
            elKode.value = prefix + "-" + new Date().getTime(); // Fallback darurat
        }
    };


    // ==========================================
    // LOGIKA TAMPILAN PENCARIAN SIVITAS
    // ==========================================
    const aturTampilanPencarian<%=rnd%> = () => {
        const elKategori = document.getElementById('inputTipeAnggota<%=rnd%>');
        const idTipe = elKategori.value;
        const optSelected = elKategori.options[elKategori.selectedIndex];
        const namaTipe = optSelected ? (optSelected.getAttribute('data-nama') || '') : '';

        // Deteksi mapping internal berdasarkan nama tabel
        let internalCat = 'UMUM';
        if (namaTipe.includes('MAHASISWA')) internalCat = 'MAHASISWA';
        else if (namaTipe.includes('SISWA')) internalCat = 'SISWA';
        else if (namaTipe.includes('GURU')) internalCat = 'GURU';
        else if (namaTipe.includes('DOSEN')) internalCat = 'DOSEN';
        else if (namaTipe.includes('PEGAWAI')) internalCat = 'PEGAWAI';

        const areaRef = document.getElementById('areaPencarianRef<%=rnd%>');
        const labelRef = document.getElementById('labelPencarianRef<%=rnd%>');
        const inputCari = document.getElementById('inputCariReferensi<%=rnd%>');
        
        document.getElementById('idReferensiTerpilih<%=rnd%>').value = '';
        inputCari.value = '';

        if (internalCat === 'UMUM' || idTipe === '') {
            areaRef.style.display = 'none';
        } else {
            areaRef.style.display = 'block';
            labelRef.innerHTML = '<%=Common.getBahasaConfigJS("Cari Data Induk ")%>' + optSelected.text;
        }

        // Generate kode saat kategori diubah
        generateAndSetKodeMember<%=rnd%>();
    };

    // Listener Live Search Data Sivitas
    document.getElementById('inputCariReferensi<%=rnd%>').addEventListener('input', function() {
        const keyword = this.value.trim();
        const isMatch = Array.from(document.getElementById('listReferensiSivitas<%=rnd%>').options).some(opt => opt.value === keyword);
        
        if (!isMatch && keyword.length >= 3) {
            clearTimeout(timerPencarianRef<%=rnd%>);
            timerPencarianRef<%=rnd%> = setTimeout(() => {
                tarikDataReferensiSivitas<%=rnd%>(keyword);
            }, 600); 
        }
    });

    const tarikDataReferensiSivitas<%=rnd%> = async (keyword) => {
        const elKategori = document.getElementById('inputTipeAnggota<%=rnd%>');
        const optSelected = elKategori.options[elKategori.selectedIndex];
        const namaTipe = optSelected ? (optSelected.getAttribute('data-nama') || '') : '';
        
        let internalCat = 'UMUM';
        if (namaTipe.includes('MAHASISWA')) internalCat = 'MAHASISWA';
        else if (namaTipe.includes('SISWA')) internalCat = 'SISWA';
        else if (namaTipe.includes('GURU')) internalCat = 'GURU';
        else if (namaTipe.includes('DOSEN')) internalCat = 'DOSEN';
        else if (namaTipe.includes('PEGAWAI')) internalCat = 'PEGAWAI';

        if (internalCat === 'UMUM') return;

        const safeKeyword = keyword.replace(/'/g, "''");
        let sql = '';
        let prefixIdentitas = '';

        if (internalCat === 'MAHASISWA') {
            sql = "SELECT id, nama, nim as kode FROM public.mahasiswa WHERE aktif = true AND (nama ILIKE '%" + safeKeyword + "%' OR nim ILIKE '%" + safeKeyword + "%') ORDER BY nama ASC LIMIT 50";
            prefixIdentitas = "NIM";
        } else if (internalCat === 'SISWA') {
            sql = "SELECT id, nama_siswa as nama, nomor_induk as kode FROM sekolah.siswa WHERE aktif = true AND (nama_siswa ILIKE '%" + safeKeyword + "%' OR nomor_induk ILIKE '%" + safeKeyword + "%') ORDER BY nama_siswa ASC LIMIT 50";
            prefixIdentitas = "NIS";
        } else if (internalCat === 'GURU') {
            sql = "SELECT id, nama_guru as nama, nuptk as kode FROM sekolah.guru WHERE aktif = true AND (nama_guru ILIKE '%" + safeKeyword + "%' OR nuptk ILIKE '%" + safeKeyword + "%') ORDER BY nama_guru ASC LIMIT 50";
            prefixIdentitas = "NUPTK";
        } else if (internalCat === 'DOSEN') {
            sql = "SELECT id, nama, nidn as kode FROM public.dosen WHERE aktif = true AND (nama ILIKE '%" + safeKeyword + "%' OR nidn ILIKE '%" + safeKeyword + "%') ORDER BY nama ASC LIMIT 50";
            prefixIdentitas = "NIDN";
        } else if (internalCat === 'PEGAWAI') {
            sql = "SELECT id, nama, code as kode FROM public.pegawai WHERE aktif = true AND (nama ILIKE '%" + safeKeyword + "%' OR code ILIKE '%" + safeKeyword + "%') ORDER BY nama ASC LIMIT 50";
            prefixIdentitas = "NIK";
        }

        const data = await fetchDataAPI<%=rnd%>(sql);
        daftarReferensiSivitas<%=rnd%> = data;
        
        let htmlDatalist = '';
        data.forEach(item => {
            const label = item.nama + ' (' + prefixIdentitas + ': ' + (item.kode || '-') + ')';
            htmlDatalist += '<option value="' + label + '" data-id="' + item.id + '" data-nama="' + item.nama + '">';
        });
        
        document.getElementById('listReferensiSivitas<%=rnd%>').innerHTML = htmlDatalist;
    };

    document.getElementById('inputCariReferensi<%=rnd%>').addEventListener('change', function() {
        const val = this.value;
        const opts = document.getElementById('listReferensiSivitas<%=rnd%>').childNodes;
        let match = null;
        
        for (let i = 0; i < opts.length; i++) {
            if (opts[i].value === val) {
                match = {
                    id: opts[i].getAttribute('data-id'),
                    nama: opts[i].getAttribute('data-nama')
                };
                break;
            }
        }
        if (match) {
            document.getElementById('idReferensiTerpilih<%=rnd%>').value = match.id;
            // Opsional: Isi otomatis nama agar lebih mudah, tp biarkan user ngedit
            if (document.getElementById('inputNama<%=rnd%>').value === '') {
                document.getElementById('inputNama<%=rnd%>').value = match.nama;
            }
        } else {
            document.getElementById('idReferensiTerpilih<%=rnd%>').value = '';
        }
    });


    // ==========================================
    // LOGIKA PENGECEKAN PENDAFTARAN AKTIF (KUNCI/EDIT)
    // ==========================================
    const cekPendaftaranAktif<%=rnd%> = () => {
        clearTimeout(timerPengecekan<%=rnd%>);
        timerPengecekan<%=rnd%> = setTimeout(async () => {
            
            const nama = document.getElementById('inputNama<%=rnd%>').value.trim().replace(/'/g, "''");
            const hp = document.getElementById('inputHp<%=rnd%>').value.trim().replace(/'/g, "''");
            const email = document.getElementById('inputEmail<%=rnd%>').value.trim().replace(/'/g, "''");

            if (nama === '' || hp === '' || email === '') return;

            const sqlCek = "SELECT id, userid, pass, jenis_anggota_koperasi " + 
                           "FROM koperasi.anggota_koperasi " +
                           "WHERE nama ILIKE '" + nama + "' AND hp = '" + hp + "' AND email_nasabah ILIKE '" + email + "' LIMIT 1";
            
            const res = await fetchDataAPI<%=rnd%>(sqlCek);

            const alertBox = document.getElementById('alertStatus<%=rnd%>');
            const formWrap = document.getElementById('formWrapper<%=rnd%>');
            const btnSubmit = document.getElementById('btnKirimPendaftaran<%=rnd%>');

            if (res && res.length > 0) {
                const dataCalon = res[0];
                
                // Jika data sudah ada di AnggotaKoperasi, berarti sudah terdaftar & disetujui
                isDataTerkunci<%=rnd%> = true;
                alertBox.className = 'alert alert-success shadow-sm border-0 rounded-4 mb-4';
                alertBox.innerHTML = '<div class="d-flex align-items-center"><i class="fas fa-check-circle fa-2x me-3 text-success"></i><div><h6 class="fw-bold mb-0 text-dark"><%=Common.getBahasaConfig("Anda Sudah Terdaftar")%></h6><span class="small text-dark"><%=Common.getBahasaConfig("Data Anda telah tercatat sebagai Anggota Koperasi yang sah. Silakan masuk ke dalam sistem.")%></span></div></div>';
                alertBox.style.display = 'block';
                
                formWrap.classList.add('readonly-overlay-<%=rnd%>');
                btnSubmit.style.display = 'none';

            } else {
                // PENDAFTARAN BARU
                isDataTerkunci<%=rnd%> = false;
                document.getElementById('inputIdCalon<%=rnd%>').value = '';
                document.getElementById('inputPassword<%=rnd%>').required = true;
                document.getElementById('hintPassword<%=rnd%>').style.display = 'none';
                alertBox.style.display = 'none';
                formWrap.classList.remove('readonly-overlay-<%=rnd%>');
                btnSubmit.innerHTML = '<i class="fas fa-paper-plane me-2"></i><%=Common.getBahasaConfig("Kirim Pendaftaran")%>';
                btnSubmit.style.display = 'inline-block';
            }

        }, 800);
    };


    // ==========================================
    // LOGIKA PENYIMPANAN DATA (AUTO-APPROVE) & UPLOAD FOTO
    // ==========================================
    const prosesSimpanPendaftaran<%=rnd%> = async () => {
        if(isDataTerkunci<%=rnd%>) return;

        if (!isUserIdAvailable<%=rnd%>) {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal. Username yang Anda masukkan sudah terpakai. Silakan ganti terlebih dahulu.")%>', 'bg-danger text-white');
            document.getElementById('inputUserId<%=rnd%>').focus();
            return;
        }

        const btnSimpan = document.getElementById('btnKirimPendaftaran<%=rnd%>');
        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Sedang memproses...")%>';
        btnSimpan.disabled = true;

        const idCalon = document.getElementById('inputIdCalon<%=rnd%>').value;
        const idTipeAnggota = document.getElementById('inputTipeAnggota<%=rnd%>').value;
        
        // Deteksi kategori internal berdasarkan nama untuk menentukan kolom relasi di backend
        const elKategori = document.getElementById('inputTipeAnggota<%=rnd%>');
        const optSelected = elKategori.options[elKategori.selectedIndex];
        const namaTipe = optSelected ? (optSelected.getAttribute('data-nama') || '') : '';

        let internalCat = 'UMUM';
        if (namaTipe.includes('MAHASISWA')) internalCat = 'MAHASISWA';
        else if (namaTipe.includes('SISWA')) internalCat = 'SISWA';
        else if (namaTipe.includes('GURU')) internalCat = 'GURU';
        else if (namaTipe.includes('DOSEN')) internalCat = 'DOSEN';
        else if (namaTipe.includes('PEGAWAI')) internalCat = 'PEGAWAI';

        const idRef = document.getElementById('idReferensiTerpilih<%=rnd%>').value;

        if (internalCat !== 'UMUM' && idRef === '') {
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Karena Anda memilih kategori Sivitas Akademika, silakan cari dan pilih Data Induk Anda terlebih dahulu.")%>', 'bg-warning text-dark');
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
            document.getElementById('inputCariReferensi<%=rnd%>').focus();
            return;
        }

        // Pengecekan Kode Member Koperasi Ulang Saat Menyimpan menggunakan loop pengaman ekstra
        let proposedCode = document.getElementById('inputKodeMember<%=rnd%>').value.trim();
        
        let isCodeUnique = false;
        let retryCount = 0;
        // Loop agresif untuk memastikan kode benar-benar belum dipakai sebelum ditembak ke backend
        while (!isCodeUnique && retryCount < 5) {
            let checkSql = "SELECT id FROM koperasi.anggota_koperasi WHERE kode = '" + proposedCode + "'";
            let resCheck = await fetchDataAPI<%=rnd%>(checkSql);
            
            if (resCheck && resCheck.length > 0) {
                // Jika terpakai, pisahkan formatnya misal: MR-1/03/2026/1 dan naikkan angkanya otomatis
                const parts = proposedCode.split('/');
                if (parts.length === 4) {
                    let prefixPart = parts[0].split('-'); 
                    if (prefixPart.length >= 2) {
                        let numJenis = parseInt(prefixPart[prefixPart.length - 1]);
                        if (!isNaN(numJenis)) {
                            prefixPart[prefixPart.length - 1] = (numJenis + 1).toString();
                        }
                        parts[0] = prefixPart.join('-');
                    }
                    
                    let numAll = parseInt(parts[3]);
                    if (!isNaN(numAll)) {
                        parts[3] = (numAll + 1).toString();
                    }
                    proposedCode = parts.join('/');
                } else {
                    // Jaga-jaga jika format kode bukan bawaan sistem
                    proposedCode = proposedCode + "-" + Math.floor(Math.random() * 100);
                }
                document.getElementById('inputKodeMember<%=rnd%>').value = proposedCode;
                retryCount++;
            } else {
                isCodeUnique = true;
            }
        }

        // SIMPAN LANGSUNG KE ANGGOTA KOPERASI AGAR AUTO APPROVE
        // Menggunakan target class ais.database.model.koperasi.AnggotaKoperasi
        const dataObj = {
            nama: document.getElementById('inputNama<%=rnd%>').value.trim(),
            hp: document.getElementById('inputHp<%=rnd%>').value.trim(),
            emailNasabah: document.getElementById('inputEmail<%=rnd%>').value.trim(),
            userid: document.getElementById('inputUserId<%=rnd%>').value.trim(),
            jenisAnggotaKoperasi: document.getElementById('inputJenisAnggota<%=rnd%>').value,
            kode: proposedCode,
            aktif: true // Auto Aktif
        };

        if (idTipeAnggota !== '') {
            dataObj.tipeAnggotaKoperasi = idTipeAnggota;
        }

        const pass = document.getElementById('inputPassword<%=rnd%>').value;
        if(pass !== "") {
            dataObj.pass = pass;
        }

        // Relasi Referensi
        if (internalCat === 'MAHASISWA') dataObj.mahasiswa = idRef;
        else if (internalCat === 'SISWA') dataObj.siswa = idRef;
        else if (internalCat === 'GURU') dataObj.guru = idRef;
        else if (internalCat === 'DOSEN') dataObj.dosen = idRef;
        else if (internalCat === 'PEGAWAI') dataObj.pegawai = idRef;

        const payload = { 
            action: "simpanDataRinci", 
            class: "<%=AnggotaKoperasi.class.getName()%>", // Disimpan langsung ke AnggotaKoperasi
            data: dataObj,
            tanpaLogin: "true"
        };

        if (idCalon !== '') {
            payload.id = idCalon;
        }

        try {
            // EKSEKUSI SIMPAN DATA TEKS
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                const finalRecordId = result.id || idCalon;

                // JIKA ADA FOTO, LAKUKAN PROSES UPLOAD
                const fileInput = document.getElementById('inputFileFoto<%=rnd%>');
                if (fileInput.files && fileInput.files.length > 0) {
                    await prosesUploadFoto<%=rnd%>(finalRecordId, fileInput.files[0]);
                } else {
                    tampilkanSuksesAkhir<%=rnd%>();
                }

            } else {
                // Tangkap khusus masalah Constraint Unik/Duplicate Key langsung dari Database jika tembus pengecekan JS
                const errorStr = (result.description || "").toLowerCase();
                if (errorStr.includes("duplicate key") || errorStr.includes("unique constraint") || errorStr.includes("sudah ada")) {
                    showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Kode member berbenturan dengan transaksi pendaftaran lain. Sistem sudah menyiapkan kode baru, silakan klik Kirim Pendaftaran lagi.")%>', 'bg-warning text-dark');
                    await generateAndSetKodeMember<%=rnd%>(); // Segarkan ulang kodenya secara otomatis
                } else {
                    showToastUI<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal mengirim pendaftaran.")%>', 'bg-danger text-white');
                }
                btnSimpan.innerHTML = oriBtn;
                btnSimpan.disabled = false;
            }
        } catch (e) {
            console.error(e);
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi. Silakan coba lagi.")%>', 'bg-danger text-white');
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    // Fungsi Upload Foto Spesifik Menggunakan FormData API
    const prosesUploadFoto<%=rnd%> = async (recordId, fileObj) => {
        const btnSimpan = document.getElementById('btnKirimPendaftaran<%=rnd%>');
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Mengunggah Foto...")%>';

        var formData = new FormData();
        formData.append('nama', fileObj.name);
        formData.append('clazz', '<%=ais.database.model.file.LampiranLain.class.getName()%>');
        formData.append('jenis', '<%=AnggotaKoperasi.class.getName()%>');
        formData.append('id', recordId); 
        formData.append('tanpaLogin', 'true');
        formData.append('fileContent', fileObj);

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                body: formData 
            });
            const result = await response.json();
            tampilkanSuksesAkhir<%=rnd%>();
        } catch (error) {
            console.error("Upload Error", error);
            showToastUI<%=rnd%>('<%=Common.getBahasaConfigJS("Data pendaftaran tersimpan, namun gagal mengunggah foto.")%>', 'bg-warning text-dark');
            tampilkanSuksesAkhir<%=rnd%>();
        }
    };

    const tampilkanSuksesAkhir<%=rnd%> = () => {
        const formWrap = document.getElementById('formWrapper<%=rnd%>');
        
        let htmlSukses = '<div class="text-center py-5 animate__animated animate__zoomIn">';
        htmlSukses += '<i class="fas fa-check-circle fa-4x text-success mb-3"></i>';
        htmlSukses += '<h4 class="fw-bold text-dark"><%=Common.getBahasaConfig("Pendaftaran Berhasil !")%></h4>';
        htmlSukses += '<p class="text-muted mb-4"><%=Common.getBahasaConfig("Selamat! Data Anda telah tercatat oleh sistem. Anda sekarang adalah Anggota yang sah. Silakan masuk menggunakan User ID dan Kata Sandi yang telah Anda buat.")%></p>';
        htmlSukses += '<a href="<%=Common.ROOT%>/login" class="btn btn-primary rounded-pill px-5 fw-bold shadow-sm"><i class="fas fa-sign-in-alt me-2"></i><%=Common.getBahasaConfig("Masuk ke Sistem")%></a>';
        htmlSukses += '</div>';

        formWrap.innerHTML = htmlSukses;
        document.getElementById('alertStatus<%=rnd%>').style.display = 'none';
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    // ==========================================
    // INISIALISASI SAAT HALAMAN DIMUAT
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        muatJenisAnggotaKoperasi<%=rnd%>();
        loadComboTipeAnggota<%=rnd%>();
        
        // Generate kode pertama kali saat halaman dimuat
        setTimeout(generateAndSetKodeMember<%=rnd%>, 500);
    });
</script>

<jsp:include page="/WEB-INF/baru/include/dialog-modal.jsp"></jsp:include>
        
      </div>
    </main>
    <!-- ===============================================-->
    <!--    End of Main Content-->
    <!-- ===============================================-->

    <jsp:include page="/WEB-INF/baru/include/foot.jsp"></jsp:include>

  </body>
</html>