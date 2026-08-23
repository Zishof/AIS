<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Collections"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.file.TugasFileContent"%>
<%@page import="java.util.TreeMap"%>
<%@page import="ais.database.model.Tugas"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.FormatNilai"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.TugasPertemuan"%>
<%@page import="ais.database.model.TugasKelompok"%>
<%@page import="org.json.JSONObject"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================================
    // 1. BLOK PROSES AUTO-SAVE (AJAX POST DARI ONCHANGE)
    // =========================================================================================
    if ("POST".equalsIgnoreCase(request.getMethod()) && "save_nilai".equals(request.getParameter("action"))) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Session sessPost = null;
        try {
            // MENGGUNAKAN NATIVE SESSION AGAR BISA DITUTUP MANUAL UNTUK MENCEGAH DEADLOCK
            sessPost = HibernateUtil.openSession();
            
            String idTugasPost = request.getParameter("idTugas");
            String jenisPost = request.getParameter("jenisTugas");
            String userKey = request.getParameter("userKey");
            String tipeField = request.getParameter("tipeField"); // ket | nilai | nilai_obe
            String idFormatObe = request.getParameter("idFormatObe"); 
            String valueStr = request.getParameter("value");

            Tugas tgsPost = null;
            Long parsedId = Long.parseLong(idTugasPost);
            
            if ("TugasPertemuan".equalsIgnoreCase(jenisPost)) {
                tgsPost = (Tugas) sessPost.get(TugasPertemuan.class, parsedId);
            } else if ("TugasKelompok".equalsIgnoreCase(jenisPost)) {
                tgsPost = (Tugas) sessPost.get(TugasKelompok.class, parsedId);
            } else {
                tgsPost = (Tugas) sessPost.get(Pertemuan.class, parsedId);
            }

            if (tgsPost != null) {
                JSONObject jsonKetNilai = new JSONObject(tgsPost.getKeteranganNilai() != null && !tgsPost.getKeteranganNilai().trim().isEmpty() ? tgsPost.getKeteranganNilai() : "{}");
                
                if ("ket".equals(tipeField)) {
                    jsonKetNilai.put(userKey + "_ket", valueStr.trim());
                } else if ("nilai_obe".equals(tipeField)) {
                    double val = valueStr != null && !valueStr.trim().isEmpty() ? Double.parseDouble(valueStr) : 0.0;
                    jsonKetNilai.put(userKey + "_nilai_" + idFormatObe, val);
                } else {
                    double val = valueStr != null && !valueStr.trim().isEmpty() ? Double.parseDouble(valueStr) : 0.0;
                    jsonKetNilai.put(userKey + "_nilai", val);
                }
                
                tgsPost.setKeteranganNilai(jsonKetNilai.toString());
                Common.refreshUpdate(sessPost, tgsPost);
                
                out.print("{\"status\":\"ok\"}");
            } else {
                out.print("{\"status\":\"error\", \"msg\":\"" + Common.getBahasaConfig("Tugas tidak ditemukan") + "\"}");
            }
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas/tugas_peserta.jsp:79");
            out.print("{\"status\":\"error\", \"msg\":\"" + e.getMessage() + "\"}");
        } finally {
            // PENTING: WAJIB TUTUP SESSION SEBELUM RETURN AGAR TIDAK DEADLOCK
            try { if (sessPost != null) sessPost.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/tugas/tugas_peserta.jsp:83");}
            try { if (sessPost != null) sessPost.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/tugas/tugas_peserta.jsp:84");}
            HibernateUtil.closeSession();
        }
        return; 
    }

    // =========================================================================================
    // 2. BLOK TAMPILAN ANTARMUKA (GET)
    // =========================================================================================
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        return; 
    }
    
    // Pembuatan ID Unik untuk Scope JavaScript
    String rnd = Common.getGeneratedBarCode(6);
    String paramJenis = request.getParameter("jenis") != null ? request.getParameter("jenis") : "";

%>
    <img src="data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==" style="display:none;" onload="
        window.simpanNilaiRealtime_<%=rnd%> = function(elemen, idTugas, jenisTugas, userKey, tipeField, idFormatObe) {
            var nilaiInput = elemen.value;
            var classAwalan = elemen.className;
            
            // Visual Loading
            elemen.classList.add('bg-warning', 'bg-opacity-25');
            
            var formData = new URLSearchParams();
            formData.append('action', 'save_nilai');
            formData.append('idTugas', idTugas);
            formData.append('jenisTugas', jenisTugas);
            formData.append('userKey', userKey);
            formData.append('tipeField', tipeField); 
            formData.append('value', nilaiInput);
            if(idFormatObe) {
                formData.append('idFormatObe', idFormatObe);
            }

            fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=tugas_peserta', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: formData.toString()
            })
            .then(function(response) { return response.json(); })
            .then(function(data) {
                elemen.className = classAwalan; 
                if(data.status === 'ok') {
                    elemen.classList.add('is-valid');
                    setTimeout(function() { elemen.classList.remove('is-valid'); }, 2000);
                } else {
                    elemen.classList.add('is-invalid');
                }
            })
            .catch(function(error) {
                elemen.className = classAwalan;
                elemen.classList.add('is-invalid');
            });
        };
    ">
<%

    Session mySession = null;
    List<TugasFileContent> listTugasContent = new ArrayList<TugasFileContent>();
    boolean isDataEmpty = true;

    try {
        mySession = HibernateUtil.openSession();
        
        String idStr = request.getParameter("id");
        
        if (!paramJenis.isEmpty() && idStr != null && !idStr.trim().isEmpty()) {
            
            Tugas tugas = null;
            long idLong = 0;
            try { idLong = Long.parseLong(idStr); } catch (NumberFormatException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/tugas/tugas_peserta.jsp:160"); }

            if (idLong > 0) {
                if ("TugasPertemuan".equalsIgnoreCase(paramJenis)) {
                    tugas = (Tugas) mySession.createCriteria(TugasPertemuan.class).add(Restrictions.idEq(idLong)).uniqueResult();
                } else if ("TugasKelompok".equalsIgnoreCase(paramJenis)) {
                    tugas = (Tugas) mySession.createCriteria(TugasKelompok.class).add(Restrictions.idEq(idLong)).uniqueResult();
                } else if ("Pertemuan".equalsIgnoreCase(paramJenis)) {
                    tugas = (Tugas) GeneralValueObject.ambilData(Pertemuan.class, idStr, true);
                } else {
                    try {
                        TugasPertemuan tp = (TugasPertemuan) mySession.createCriteria(TugasPertemuan.class).add(Restrictions.idEq(idLong)).uniqueResult();
                        if (tp != null && tp.getPertemuan() != null) {
                            tugas = (Tugas) GeneralValueObject.ambilData(Pertemuan.class, tp.getPertemuan().toString(), true);
                        }
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/tugas/tugas_peserta.jsp:175"); }
                }
            }

            if (tugas != null && tugas.getJudultugas() != null) {
                Perkuliahan perkuliahan = null;
                if (tugas instanceof Pertemuan) {
                    perkuliahan = ((Pertemuan) tugas).getPerkuliahan();
                } else if (tugas instanceof TugasPertemuan && ((TugasPertemuan) tugas).ambilPertemuan() != null) {
                    perkuliahan = ((TugasPertemuan) tugas).ambilPertemuan().getPerkuliahan();
                } else if (tugas instanceof TugasKelompok && ((TugasKelompok) tugas).ambilPertemuan() != null) {
                    perkuliahan = ((TugasKelompok) tugas).ambilPertemuan().getPerkuliahan();
                }

                boolean isObe = false;
                List<FormatNilai> activeObeFormatNilais = new ArrayList<FormatNilai>();
                
                if (perkuliahan != null && perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
                    isObe = true;
                    List<FormatNilai> allFormatNilais = Common.getFormatNilais(mySession, perkuliahan);
                    JSONObject jsonFormatTugas = new JSONObject(tugas.getFormatNilais() != null && !tugas.getFormatNilais().trim().isEmpty() ? tugas.getFormatNilais() : "{}");
                    
                    for (FormatNilai nilai : allFormatNilais) {
                        if (nilai.getStatusPertemuan() != null && !jsonFormatTugas.isNull(nilai.getId().toString())) {
                            activeObeFormatNilais.add(nilai);
                        }
                    }
                }

                JSONObject jsonObjectTugas = new JSONObject(tugas.getKeteranganNilai() != null && !tugas.getKeteranganNilai().trim().isEmpty() ? tugas.getKeteranganNilai() : "{}");
                
                tugas.currentTugasFileContent = null;
                tugas.currentUser = null;
                TreeMap<Long, TugasFileContent> treemapData = new TreeMap<>();
                // Wajib refresh dari DB. Statistik pengumpulan dan panel peserta sebelumnya
                // dapat berbeda karena panel ini memakai indeks file lama: angka menunjukkan
                // sudah upload, tetapi daftar "Telah upload" kosong sehingga dosen tidak bisa
                // membuka dan menilai jawaban.
                TreeMap<Long, TugasFileContent> resultData = tugas.ambilTugasFileContentTotal(treemapData, "", null, 500, true);
                
                if (resultData != null && !resultData.isEmpty()) {
                    listTugasContent.addAll(resultData.values());
                    Collections.sort(listTugasContent); 
                    isDataEmpty = false;
                }

                if (!isDataEmpty) {
                    Mahasiswa curMhs = tbmuser.getMahasiswa();
                    Siswa curSiswa = tbmuser.getSiswa();
                    BiodataCalonMahasiswa curCamaba = tbmuser.getBiodataCalonMahasiswa();

                    for (TugasFileContent tfc : listTugasContent) {
                        boolean isOwner = false;
                        if (curSiswa != null && curSiswa.getId().equals(tfc.getSiswa())) isOwner = true;
                        else if (curMhs != null && curMhs.getId().equals(tfc.getMahasiswa())) isOwner = true;
                        else if (curCamaba != null && curCamaba.getId().equals(tfc.getBiodataCalonMahasiswa())) isOwner = true;
                        
                        if (isOwner) {
                            tugas.currentTugasFileContent = tfc;
                            break; 
                        }
                    }
                }
                
        // --- 3. Rendering View ---
        if (isDataEmpty) {
%>
            <div class="alert alert-light border text-center p-5 shadow-sm rounded-3" role="alert">
                <div class="mb-3">
                    <i class="fas fa-folder-open fa-3x text-secondary opacity-50"></i>
                </div>
                <h4 class="alert-heading fw-bold text-secondary">
                    <%= Common.getBahasaConfig("Belum Ada Pengumpulan Tugas") %>
                </h4>
                <p class="mb-0 text-muted">
                    <%= Common.getBahasaConfig("Informasi: Sampai saat ini, belum ada satupun peserta yang mengumpulkan berkas tugas untuk sesi ini.") %>
                </p>
                <hr>
                <p class="mb-0 small text-muted">
                    <%= Common.getBahasaConfig("Silakan kembali lagi nanti atau hubungi administrator jika Anda meyakini ini adalah kesalahan.") %>
                </p>
            </div>
<%
        } else {
            Mahasiswa viewerMhs = tbmuser.getMahasiswa();
            Siswa viewerSiswa = tbmuser.getSiswa();
            BiodataCalonMahasiswa viewerCamaba = tbmuser.getBiodataCalonMahasiswa();

            for (TugasFileContent tgs : listTugasContent) {
                String dispNama = "-", dispId = "-", dispTelp = "-", dispEmail = "-";
                String dispFotoUrl = "", dispNamaFile = "", dispLinkTugas = "#";
                String waktuUpload = tgs.getUploadDate() != null ? SmartDateTimeUtil.getDayString(tgs.getUploadDate(), null) : "-";
                String tglUploadSimple = tgs.getUploadDate() != null ? Common.dateFormat.get().format(tgs.getUploadDate()) : "";
                
                boolean isReadonly = !Common.getApakahAdminLain(tbmuser);
                
                Mahasiswa mhsFile = (tgs.getMahasiswa() != null) ? (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), tgs.getMahasiswa()) : null;
                Siswa siswaFile = (tgs.getSiswa() != null) ? (Siswa) ConstantValues.ambil(Siswa.class.getName(), tgs.getSiswa()) : null;
                BiodataCalonMahasiswa camabaFile = (tgs.getBiodataCalonMahasiswa() != null) ? (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), tgs.getBiodataCalonMahasiswa()) : null;
                
                String userKey = ""; 
                boolean nilaiSendiri = false;
                if (mhsFile != null) {
                    userKey = mhsFile.getId() + "_mhs";
                    tgs.ubahRealNameSesuaiDenganNIM(mhsFile);
                    dispNamaFile = tgs.ambilRealNameSesuaiDenganNIM(mhsFile);
                    dispFotoUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(mhsFile));
                    dispNama = mhsFile.getNama();  dispId = mhsFile.getNim();
                    dispTelp = mhsFile.getTelp();  dispEmail = mhsFile.getEmail();
                    if (viewerMhs != null && viewerMhs.getId().equals(mhsFile.getId())) nilaiSendiri = true;
                } else if (siswaFile != null) {
                    userKey = siswaFile.getId() + "_siswa";
                    tgs.ubahRealNameSesuaiDenganNIM(siswaFile);
                    dispNamaFile = tgs.ambilRealNameSesuaiDenganNIM(siswaFile);
                    dispFotoUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(siswaFile));
                    dispNama = siswaFile.getNama(); dispId = siswaFile.getNomorInduk();
                    dispTelp = siswaFile.getTeleponSiswa(); dispEmail = siswaFile.getAlamatEmail();
                    if (viewerSiswa != null && viewerSiswa.getId().equals(siswaFile.getId())) nilaiSendiri = true;
                } else if (camabaFile != null) {
                    userKey = camabaFile.getId() + "_cal_mhs";
                    tgs.ubahRealNameSesuaiDenganNIM(camabaFile);
                    dispNamaFile = tgs.ambilRealNameSesuaiDenganNIM(camabaFile);
                    dispFotoUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(camabaFile));
                    dispNama = camabaFile.getNama(); dispId = camabaFile.getNoRegistrasi();
                    dispTelp = camabaFile.getHp(); dispEmail = camabaFile.getEmail();
                    if (viewerCamaba != null && viewerCamaba.getId().equals(camabaFile.getId())) nilaiSendiri = true;
                } else {
                    continue; 
                }
                
                if(tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.getCalonSiswa() != null || tbmuser.getSiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null ){
                	isReadonly = true;
                } else {
                	isReadonly = false;
                }

                dispLinkTugas = tgs.createLinkUri();
                String keteranganVal = jsonObjectTugas.has(userKey + "_ket") ? jsonObjectTugas.getString(userKey + "_ket") : (tgs.getKeterangan() != null ? tgs.getKeterangan() : "");
%>
            <div class="list-group-item p-4 border bg-white mb-3 rounded-3 shadow-sm">
                <div class="row align-items-start">
                    
                    <div class="col-lg-3 col-md-4 border-end-md mb-3 mb-md-0">
                        <div class="d-flex align-items-center mb-3">
                            <div class="flex-shrink-0 me-3">
                                <img onclick="tampilGambar(this)"
                                     class="rounded-circle shadow-sm border border-2 border-white"
                                     style="width: 60px; height: 60px; object-fit: cover; cursor: pointer;"
                                     title="<%= dispNama %>" alt="Foto" src="<%= dispFotoUrl %>"
                                     onerror="this.src='img/default-user.png';"> 
                            </div>
                            <div class="flex-grow-1 overflow-hidden">
                                <h6 class="mb-0 fw-bold text-primary text-truncate" title="<%= dispNama %>"><%= dispNama %></h6>
                                <span class="badge bg-light text-dark border mt-1 font-monospace"><%= dispId %></span>
                            </div>
                        </div>

                        <div class="small text-muted mb-3">
                            <% if(dispTelp != null && !dispTelp.isEmpty()) { %>
                                <div class="mb-1 text-truncate" title="Telepon/WA"><i class="fab fa-whatsapp text-success me-2"></i><%= dispTelp %></div>
                            <% } %>
                            <% if(dispEmail != null && !dispEmail.isEmpty()) { %>
                                <div class="text-truncate" title="Email"><i class="fas fa-envelope text-info me-2"></i><%= dispEmail %></div>
                            <% } %>
                        </div>
                        
                        <div class="bg-light p-2 rounded text-center">
                            <small class="text-uppercase text-muted fw-bold d-block mb-1" style="font-size: 0.65rem;"><%= Common.getBahasaConfig("Waktu Unggah") %></small>
                            <span class="badge bg-success-subtle text-success border border-success-subtle d-block mb-1">
                                <i class="far fa-clock me-1"></i> <%= waktuUpload %>
                            </span>
                            <small class="text-muted font-monospace"><%= tglUploadSimple %></small>
                        </div>
                    </div>

                    <% if (isObe) { %>
                        <div class="col-lg-9 col-md-8 ps-md-4">
                            
                            <div class="row mb-3 pb-3 border-bottom border-light">
                                <div class="col-lg-5 mb-3 mb-lg-0">
                                    <label class="form-label small fw-bold text-uppercase text-secondary mb-2"><i class="fas fa-paperclip me-1"></i> <%= Common.getBahasaConfig("Berkas Tugas") %></label>
                                    <div>
                                        <% if (!isReadonly && !dispLinkTugas.equals("#")) { %>
                                            <a target="_blank" href="<%= dispLinkTugas %>" class="btn btn-outline-primary rounded-pill w-100 text-start text-truncate shadow-sm" title="<%= dispNamaFile %>">
                                                <i class="fas fa-download me-2"></i> <%= dispNamaFile %>
                                            </a>
                                        <% } else if (isReadonly && !dispLinkTugas.equals("#")) { %>
                                            <a target="_blank" href="<%= dispLinkTugas %>" class="btn btn-outline-primary rounded-pill w-100 text-start text-truncate shadow-sm" title="<%= dispNamaFile %>">
                                                <i class="fas fa-download me-2"></i> <%= dispNamaFile %>
                                            </a>
                                        <% } else { %>
                                            <span class="badge bg-light text-secondary border w-100 text-start py-2 text-truncate">
                                                <i class="fas fa-file-alt me-2"></i> <%= dispNamaFile %>
                                            </span>
                                        <% } %>
                                    </div>
                                </div>
                                <div class="col-lg-7">
                                    <label class="form-label small fw-bold text-uppercase text-secondary mb-1"><i class="far fa-comment-dots me-1"></i> <%= Common.getBahasaConfig("Catatan Pengajar") %></label>
                                    <% if (isReadonly) { %>
                                        <div class="bg-light p-2 rounded-3 small border" style="min-height: 60px; white-space: pre-wrap;"><%= keteranganVal.isEmpty() ? "<i class='text-muted'>— "+Common.getBahasaConfig("Tidak ada catatan")+" —</i>" : keteranganVal %></div>
                                    <% } else { %>
                                        <textarea class="form-control border-primary shadow-sm" rows="2" 
                                                  placeholder="<%= Common.getBahasaConfig("Tulis catatan untuk mahasiswa di sini...") %>"
                                                  onchange="window.simpanNilaiRealtime_<%=rnd%>(this, '<%= idLong %>', '<%= paramJenis %>', '<%= userKey %>', 'ket', null)"><%= keteranganVal %></textarea>
                                    <% } %>
                                </div>
                            </div>

                            <div>
                                <label class="form-label small fw-bold text-uppercase text-secondary mb-2"><i class="fas fa-award me-1"></i> <%= Common.getBahasaConfig("Penilaian (Berbasis OBE)") %></label>
                                <% if (!activeObeFormatNilais.isEmpty()) { %>
                                    <div class="row g-2">
                                    <% for (FormatNilai fn : activeObeFormatNilais) { 
                                         double nilaiObeVal = jsonObjectTugas.has(userKey + "_nilai_" + fn.getId()) ? jsonObjectTugas.getDouble(userKey + "_nilai_" + fn.getId()) : 0.0;
                                    %>
                                        <div class="col-xl-3 col-lg-4 col-sm-6">
                                            <div class="card bg-light border-0 h-100 shadow-none">
                                                <div class="card-body p-2 d-flex flex-row align-items-center justify-content-between">
                                                    <div class="text-truncate pe-2" title="<%= fn.getNama() %>" style="max-width: 65%;">
                                                        <small class="fw-bold text-dark"><%= fn.getNama() %></small>
                                                    </div>
                                                    <div style="width: 75px;">
                                                        <% if (isReadonly) { %>
                                                            <span class="badge bg-white text-dark border w-100 py-2">
                                                                <%= nilaiObeVal > 0 ? Common.numberFormat.get().format(nilaiObeVal) : "-" %>
                                                            </span>
                                                        <% } else { %>
                                                            <input type="number" class="form-control form-control-sm text-center fw-bold border-primary shadow-sm" 
                                                                   value="<%= nilaiObeVal %>" min="0" max="100" step="0.01"
                                                                   onchange="window.simpanNilaiRealtime_<%=rnd%>(this, '<%= idLong %>', '<%= paramJenis %>', '<%= userKey %>', 'nilai_obe', '<%= fn.getId() %>')">
                                                        <% } %>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    <% } %>
                                    </div>
                                <% } else { %>
                                    <div class="alert alert-warning small py-2 border-0"><i class="fas fa-exclamation-triangle me-1"></i> <%= Common.getBahasaConfig("Format nilai OBE belum diatur untuk tugas ini.") %></div>
                                <% } %>
                                
                                <% if (!isReadonly) { %>
                                <div class="mt-3 text-muted small fst-italic">
                                    <i class="fas fa-info-circle me-1"></i> <%= Common.getBahasaConfig("Nilai dan catatan akan otomatis tersimpan saat Anda selesai mengetik.") %>
                                </div>
                                <% } %>
                            </div>
                        </div>

                    <% } else { %>
                        <div class="col-lg-5 col-md-4 border-end-lg mb-3 mb-md-0 px-md-3">
                            <label class="form-label small fw-bold text-uppercase text-secondary mb-2"><i class="fas fa-paperclip me-1"></i> <%= Common.getBahasaConfig("Berkas Tugas") %></label>
                            <div class="mb-3">
                                <% if (!isReadonly && !dispLinkTugas.equals("#")) { %>
                                    <a target="_blank" href="<%= dispLinkTugas %>" class="btn btn-outline-primary rounded-pill w-100 text-start text-truncate shadow-sm" title="<%= dispNamaFile %>">
                                        <i class="fas fa-download me-2"></i> <%= dispNamaFile %>
                                    </a>
                                <% } else if (isReadonly && !dispLinkTugas.equals("#")) { %>
                                    <a target="_blank" href="<%= dispLinkTugas %>" class="btn btn-outline-primary rounded-pill w-100 text-start text-truncate shadow-sm" title="<%= dispNamaFile %>">
                                        <i class="fas fa-download me-2"></i> <%= dispNamaFile %>
                                    </a>
                                <% } else { %>
                                    <span class="badge bg-light text-secondary border w-100 text-start py-2 text-truncate">
                                        <i class="fas fa-file-alt me-2"></i> <%= dispNamaFile %>
                                    </span>
                                <% } %>
                            </div>

                            <div class="form-group">
                                <label class="form-label small fw-bold text-uppercase text-secondary mb-1"><i class="far fa-comment-dots me-1"></i> <%= Common.getBahasaConfig("Catatan Pengajar") %></label>
                                <% if (isReadonly) { %>
                                    <div class="bg-light p-2 rounded-3 small border" style="min-height: 60px; white-space: pre-wrap;"><%= keteranganVal.isEmpty() ? "<i class='text-muted'>— "+Common.getBahasaConfig("Tidak ada catatan")+" —</i>" : keteranganVal %></div>
                                <% } else { %>
                                    <textarea class="form-control form-control-sm border-primary shadow-sm" rows="3" 
                                              placeholder="<%= Common.getBahasaConfig("Tulis catatan untuk mahasiswa di sini...") %>"
                                              onchange="window.simpanNilaiRealtime_<%=rnd%>(this, '<%= idLong %>', '<%= paramJenis %>', '<%= userKey %>', 'ket', null)"><%= keteranganVal %></textarea>
                                <% } %>
                            </div>
                        </div>

                        <div class="col-lg-4 col-md-4 ps-md-3 d-flex flex-column justify-content-center">
                            <label class="form-label small fw-bold text-uppercase text-secondary mb-3 text-center"><i class="fas fa-award me-1"></i> <%= Common.getBahasaConfig("Penilaian") %></label>
                            <% 
                               double nilaiNonObeVal = jsonObjectTugas.has(userKey + "_nilai") ? jsonObjectTugas.getDouble(userKey + "_nilai") : (tgs.getNilai() != null ? tgs.getNilai() : 0.0);
                            %>
                            <div class="card bg-light border-0 text-center p-3 w-75 m-auto">
                                <small class="text-uppercase fw-bold text-muted mb-2"><%= Common.getBahasaConfig("Nilai Akhir") %></small>
                                <% if (!nilaiSendiri && isReadonly) { %>
                                	<h3 class="fw-bold text-primary mb-0"></h3>
                                <% } else if (isReadonly) { %>
                                    <h3 class="fw-bold text-primary mb-0"><%= nilaiNonObeVal > 0 ? Common.numberFormat.get().format(nilaiNonObeVal) : "" %></h3>
                                <% } else { %>
                                    <input type="number" class="form-control form-control-lg text-center fw-bold border-primary shadow-sm" 
                                           value="<%= nilaiNonObeVal %>" min="0" max="100" step="0.01"
                                           onchange="window.simpanNilaiRealtime_<%=rnd%>(this, '<%= idLong %>', '<%= paramJenis %>', '<%= userKey %>', 'nilai', null)">
                                <% } %>
                            </div>
                            
                            <% if (!isReadonly) { %>
                            <div class="text-center mt-3 text-muted small fst-italic px-3">
                                <i class="fas fa-info-circle me-1"></i> <%= Common.getBahasaConfig("Nilai dan catatan akan otomatis tersimpan saat Anda selesai mengetik.") %>
                            </div>
                            <% } %>
                        </div>
                    <% } %>

                </div>
            </div>
<%
            } // End Loop File Content
        } // End Else
            }
        } // End parameter check
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas/tugas_peserta.jsp:486");
%>
        <div class="alert alert-danger border-0 shadow-sm">
            <i class="fas fa-exclamation-triangle me-2"></i> <%= Common.getBahasaConfig("Terjadi kesalahan sistem saat memuat data: ") + e.getMessage() %>
        </div>
<%
    } finally {
        if (mySession != null) {
            try { mySession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/tugas/tugas_peserta.jsp:494"); }
            ais.common.ElearningSessionUtil.closeQuietly(mySession);
        }
        HibernateUtil.closeSession();
    }
%>
