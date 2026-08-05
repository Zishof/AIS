<%@page import="ais.database.model.VOMahasiswa"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.CommonHelperClass"%> 
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="java.util.Collection"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>

<%
    // --- 1. PENGAMANAN SESI PENGGUNA ---
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali.") + "\"}");
        return;
    }

    String rnd = request.getParameter("rnd");
    if(rnd == null) rnd = Common.getGeneratedBarCode(7);

    // --- 2. LOGIKA PARAMETER KUSTOM ---
    boolean disableEdit = "true".equalsIgnoreCase(request.getParameter("bolehEditJenisPembayaranDanMahasiswa"));
    boolean hanyaUntukMahasiswa = "true".equalsIgnoreCase(request.getParameter("hanya_untuk_mahasiswa"));
    String paramHanyaJkId = request.getParameter("hanya_untuk_jenis_kegiatan");
    boolean hanyaUntukJk = false;
    Long jkIdKhusus = null;

    if (paramHanyaJkId != null && !paramHanyaJkId.trim().isEmpty()) {
        try {
            jkIdKhusus = Long.parseLong(paramHanyaJkId.trim());
            hanyaUntukJk = true;
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs.jsp:38");}
    }

    // --- 3. PENANGANAN MAKSIMAL SEMESTER ---
    int maxSemesterPilihan = 25;
    try {
        Konfigurasi konfSmt = Common.getKonfigurasi("max_semester_pilihan", "25");
        if (konfSmt != null && konfSmt.getNilai() != null) {
            maxSemesterPilihan = Integer.parseInt(konfSmt.getNilai().trim());
        }
    } catch (Exception e) {
        maxSemesterPilihan = 25;
    }

    // --- 4. PENCARIAN DEFAULT JENIS KEGIATAN PEMBAYARAN ---
    Long defaultJkId = null;
    if (hanyaUntukJk && jkIdKhusus != null) {
        defaultJkId = jkIdKhusus;
    } else {
        if (CommonHelperClass.jenisKegiatansTanpaDaftarUlang == null) {
            Common.reloadJenisKegiatans();
        }
        
        if (CommonHelperClass.jenisKegiatansTanpaDaftarUlang != null) {
            for (JenisKegiatan jk : (Collection<JenisKegiatan>) CommonHelperClass.jenisKegiatansTanpaDaftarUlang) {
                if (jk.getDefaultPembayaran() != null && jk.getDefaultPembayaran()) {
                    defaultJkId = jk.getId();
                    break;
                }
            }
        }
        
        if (!hanyaUntukMahasiswa) {
            if (defaultJkId == null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && 
                ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getDefaultPembayaran() != null && 
                ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getDefaultPembayaran()) {
                defaultJkId = ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId();
            }
            
            if (defaultJkId == null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && 
                ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getDefaultPembayaran() != null && 
                ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getDefaultPembayaran()) {
                defaultJkId = ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId();
            }
        }
    }

    // --- 5. PEMBACAAN PARAMETER UNTUK PEMUATAN OTOMATIS (AUTO-LOAD) ---
    String paramId = request.getParameter("id");
    String paramJkId = request.getParameter("jkId");
    String paramSmt = request.getParameter("smt");
    String paramIsMhs = request.getParameter("isMahasiswa");
    String paramRefresh = request.getParameter("refresh");

    Long autoPersonId = null;
    Long autoJkId = null;
    Integer autoSmt = null;
    Boolean autoIsMhs = null;
    String autoPersonNimStr = "";
    String autoPersonNamaStr = "";
    
    try {
        if (paramId != null && !paramId.trim().isEmpty()) {

            long kegIdParam = Long.parseLong(paramId);
            // Muat dari session agar asosiasi (calonMahasiswa/mahasiswa) bisa di-lazy-load;
            // fallback ke cache jika session tidak tersedia (misal di luar OpenSessionInView)
            Kegiatan kLoad = null;
            try {
                // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
                // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
                kLoad = (Kegiatan) HibernateUtil.currentNativeSession().get(Kegiatan.class, kegIdParam);
            } catch (Exception eKegLoad) {
                kLoad = (Kegiatan) ConstantValues.ambil(Kegiatan.class.getName(), kegIdParam, true);
            }
            
            if (kLoad != null && kLoad.getJenisKegiatan() != null) {
                autoJkId = kLoad.getJenisKegiatan().getId();
                autoSmt = kLoad.getSemster();
                
                boolean isPMBActivity = false;
                if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(autoJkId)) isPMBActivity = true;
                if (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId().equals(autoJkId)) isPMBActivity = true;
                
                // PERBAIKAN: Jika kegiatannya adalah PMB/Daftar Ulang, WAJIB arahkan ID ke BiodataCalonMahasiswa
                if (isPMBActivity && kLoad.getCalonMahasiswa() != null) {
                    autoPersonId = kLoad.getCalonMahasiswa().getId();
                    autoIsMhs = false;
                    autoPersonNimStr = kLoad.getCalonMahasiswa().getNoRegistrasi() != null ? kLoad.getCalonMahasiswa().getNoRegistrasi() : (kLoad.getCalonMahasiswa().getKode() != null ? kLoad.getCalonMahasiswa().getKode() : "");
                    autoPersonNamaStr = kLoad.getCalonMahasiswa().getNama() != null ? kLoad.getCalonMahasiswa().getNama() : "";
                } 
                else if (kLoad.getMahasiswa() != null) {
                    autoPersonId = kLoad.getMahasiswa().getId();
                    autoIsMhs = true;
                    autoPersonNimStr = kLoad.getMahasiswa().getNim() != null ? kLoad.getMahasiswa().getNim() : "";
                    autoPersonNamaStr = kLoad.getMahasiswa().getNama() != null ? kLoad.getMahasiswa().getNama() : "";
                } 
                else if (kLoad.getCalonMahasiswa() != null && !hanyaUntukMahasiswa) {
                    autoPersonId = kLoad.getCalonMahasiswa().getId();
                    autoIsMhs = false;
                    autoPersonNimStr = kLoad.getCalonMahasiswa().getNoRegistrasi() != null ? kLoad.getCalonMahasiswa().getNoRegistrasi() : (kLoad.getCalonMahasiswa().getKode() != null ? kLoad.getCalonMahasiswa().getKode() : "");
                    autoPersonNamaStr = kLoad.getCalonMahasiswa().getNama() != null ? kLoad.getCalonMahasiswa().getNama() : "";
                }
            } 
            else if (paramJkId != null && !paramJkId.trim().isEmpty()) {
                autoPersonId = Long.parseLong(paramId);
                autoJkId = Long.parseLong(paramJkId);
                autoSmt = (paramSmt != null && !paramSmt.trim().isEmpty()) ? Integer.parseInt(paramSmt) : null;
                autoIsMhs = "true".equalsIgnoreCase(paramIsMhs);
                
                if (autoIsMhs || !hanyaUntukMahasiswa) {
                    VOMahasiswa person = (VOMahasiswa) (autoIsMhs 
                                ? ConstantValues.ambil(Mahasiswa.class.getName(), autoPersonId, true) 
                                : ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), autoPersonId, true));
                    
                    if (person != null) {
                        if (person instanceof Mahasiswa) {
                            autoPersonNimStr = ((Mahasiswa)person).getNim() != null ? ((Mahasiswa)person).getNim() : "";
                        } else if (person instanceof BiodataCalonMahasiswa) {
                            BiodataCalonMahasiswa c = (BiodataCalonMahasiswa) person;
                            autoPersonNimStr = c.getNoRegistrasi() != null ? c.getNoRegistrasi() : (c.getKode() != null ? c.getKode() : "");
                        }
                        autoPersonNamaStr = person.getNama() != null ? person.getNama() : "";
                    }
                }
            }
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs.jsp:165");}

    if (hanyaUntukJk && jkIdKhusus != null) {
        autoJkId = jkIdKhusus;
    } else if (autoJkId == null && defaultJkId != null) {
        autoJkId = defaultJkId;
    }

    String serviceUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=bayarmhs&s=pembayaran_online_mhs_services";

    // --- 6. DINAMIKA TATA LETAK GRID (UI) ---
    String classSmt = disableEdit ? "d-none" : (hanyaUntukJk ? "col-lg-4 col-md-4" : "col-lg-2 col-md-4");
    String classPelanggan = disableEdit 
        ? (hanyaUntukJk ? "col-12" : "col-lg-8 col-md-12") 
        : (hanyaUntukJk ? "col-lg-8 col-md-8" : "col-lg-6 col-md-8");
%>

<style>
    .card-header-<%=rnd%> { border-bottom: 3px solid rgba(0,0,0,0.05); }
    .btn-cari-<%=rnd%> { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    .btn-cari-<%=rnd%>:hover { transform: translateY(-2px); box-shadow: 0 0.5rem 1rem rgba(0,0,0,.1) !important; }
    .readonly-text-<%=rnd%> { background-color: #f8f9fa !important; opacity: 1 !important; }
</style>

<div class="container-fluid py-4 animate__animated animate__fadeIn">
    <div class="row mb-4 justify-content-center <%= disableEdit ? "d-none" : "" %>">
        <div class="col-12 col-xl-11">
            <div class="card border-0 shadow-lg rounded-4 overflow-hidden">
                <div class="card-header bg-gradient bg-primary text-white p-4 card-header-<%=rnd%> d-flex align-items-center">
                    <div class="bg-white bg-opacity-25 p-3 rounded-circle me-3 shadow-sm d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                        <i class="fas fa-wallet fs-4"></i>
                    </div>
                    <h5 class="mb-0 fw-bold tracking-wide"><%=Common.getBahasaConfig("Portal Pembayaran Mahasiswa")%></h5>
                </div>
                
                <div class="card-body p-4 p-md-5 bg-light">
                    <div class="row g-4 align-items-end">
                        
                        <div class="col-lg-4 col-md-12 <%= hanyaUntukJk ? "d-none" : "" %>">
                            <label class="form-label fw-bold text-secondary mb-2" style="font-size: 0.85rem; letter-spacing: 0.5px;">
                                <i class="fas fa-tasks me-2 text-primary"></i><%=Common.getBahasaConfig("Jenis Pembayaran")%>
                            </label>
                            <select class="form-select form-select-lg border-0 shadow-sm fw-semibold bg-white <%= disableEdit ? "readonly-text-"+rnd : "" %>" id="selectJenisKegiatan<%=rnd%>" onchange="window.evaluasiEntitas<%=rnd%>()" <%= disableEdit ? "disabled" : "" %>>
                                <% if (hanyaUntukJk && jkIdKhusus != null) { 
                                    JenisKegiatan jkKhusus = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(), jkIdKhusus, true);
                                    String nmJk = jkKhusus != null ? jkKhusus.getNama() : "Kegiatan " + jkIdKhusus;
                                %>
                                    <option value="<%=jkIdKhusus%>" selected><%=nmJk%></option>
                                <% } else { %>
                                    <option value=""><%=Common.getBahasaConfig("-- Pilih Kegiatan --")%></option>
                                    
                                    <% if (!hanyaUntukMahasiswa) { %>
                                    <optgroup label="<%=Common.getBahasaConfig("Calon Mahasiswa Baru")%>">
                                        <% if(ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null) { 
                                            boolean sel = autoJkId != null && autoJkId.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId());
                                        %>
                                            <option value="<%=ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()%>" <%= sel ? "selected" : "" %>><%=ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getNama()%></option>
                                        <% } %>
                                        <% if(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null) { 
                                            boolean sel = autoJkId != null && autoJkId.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId());
                                        %>
                                            <option value="<%=ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()%>" <%= sel ? "selected" : "" %>><%=ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getNama()%></option>
                                        <% } %>
                                    </optgroup>
                                    <% } %>
                                   
                                    <optgroup label="<%=Common.getBahasaConfig("Pembayaran Mahasiswa")%>">
                                        <% 
                                            if (CommonHelperClass.jenisKegiatansTanpaDaftarUlang != null) {
                                                for (JenisKegiatan jk : (Collection<JenisKegiatan>) CommonHelperClass.jenisKegiatansTanpaDaftarUlang) {
                                                    boolean sel = autoJkId != null && autoJkId.equals(jk.getId());
                                        %>
                                            <option value="<%=jk.getId()%>" <%= sel ? "selected" : "" %>><%=jk.getNama()%></option>
                                        <%      }
                                            } 
                                        %>
                                    </optgroup>
                                <% } %>
                            </select>
                        </div>

                        <div class="<%=classSmt%>">
                            <label class="form-label fw-bold text-secondary mb-2" style="font-size: 0.85rem; letter-spacing: 0.5px;">
                                <i class="fas fa-layer-group me-2 text-primary"></i><%=Common.getBahasaConfig("Semester")%>
                            </label>
                            <select class="form-select form-select-lg border-0 shadow-sm fw-bold bg-white text-center text-primary" id="selectSemester<%=rnd%>" onchange="if(document.getElementById('idTerpilih<%=rnd%>').value) window.loadTagihanMhs<%=rnd%>();">
                                <option value=""><%=Common.getBahasaConfig("Smt saat ini")%></option>
                                <% for (int i = 1; i < maxSemesterPilihan; i++) { %>
                                    <option value="<%=i%>"><%=i%></option>
                                <% } %>
                            </select>
                        </div>

                        <div class="<%=classPelanggan%>">
                            <label class="form-label fw-bold text-secondary mb-2" style="font-size: 0.85rem; letter-spacing: 0.5px;">
                                <i class="fas fa-user-circle me-2 text-primary"></i><%=Common.getBahasaConfig("Data Pelanggan")%>
                            </label>
                            <div class="input-group input-group-lg shadow-sm rounded-3 overflow-hidden">
                                <input type="text" class="form-control border-0 fw-semibold <%= disableEdit ? "readonly-text-"+rnd : "bg-white" %> px-4" id="labelMahasiswa<%=rnd%>" placeholder="<%= disableEdit ? "" : Common.getBahasaConfig("Pilih jenis kegiatan dahulu...")%>" readonly>
                                <% if (!disableEdit) { %>
                                <button class="btn btn-primary border-0 fw-bold px-4 btn-cari-<%=rnd%>" type="button" onclick="window.bukaPicker<%=rnd%>()">
                                    <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Cari Data")%>
                                </button>
                                <% } %>
                            </div>
                            <input type="hidden" id="idTerpilih<%=rnd%>">
                            <input type="hidden" id="isMahasiswa<%=rnd%>" value="true">
                        </div>
                        
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-12 col-xl-11" id="areaTagihan<%=rnd%>">
            <div class="card border-0 shadow-sm rounded-4 text-center py-5 text-secondary bg-white">
                <div class="card-body">
                    <div class="bg-light rounded-circle d-inline-flex p-4 mb-4 border">
                        <i class="fas fa-receipt fa-4x text-primary opacity-25"></i>
                    </div>
                    <h4 class="fw-bold text-dark tracking-wide"><%=Common.getBahasaConfig("Sistem Siap Memproses Tagihan")%></h4>
                    <p class="mb-0 text-muted"><%=Common.getBahasaConfig("Rincian kewajiban pembayaran akan muncul secara otomatis setelah pelanggan dipilih.")%></p>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    (() => {
        const idCalonMaba = '<%= ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null ? ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId() : "" %>';
        const idUlangMaba = '<%= ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null ? ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId() : "" %>';
        
        window.evaluasiEntitas<%=rnd%> = function(forceIsMhs) {
            const jkId = document.getElementById('selectJenisKegiatan<%=rnd%>').value;
            const inputIsMhs = document.getElementById('isMahasiswa<%=rnd%>');
            const label = document.getElementById('labelMahasiswa<%=rnd%>');
            const selectSmt = document.getElementById('selectSemester<%=rnd%>');
            
            // PERBAIKAN: Jangan overide ke 'false' jika secara eksplisit ditugaskan dari autoLoad
            if (typeof forceIsMhs !== 'undefined' && forceIsMhs !== null && forceIsMhs !== '') {
                inputIsMhs.value = forceIsMhs.toString();
            } else {
                if (jkId === idCalonMaba || jkId === idUlangMaba) {
                    inputIsMhs.value = "false";
                } else {
                    inputIsMhs.value = "true";
                }
            }

            if (inputIsMhs.value === "false") {
                <% if (!disableEdit) { %>
                label.placeholder = '<%=Common.getBahasaConfigJS("Mencari Data Calon Mahasiswa...")%>';
                <% } %>
                if (selectSmt) { selectSmt.disabled = true; selectSmt.value = ""; }
            } else {
                <% if (!disableEdit) { %>
                label.placeholder = '<%=Common.getBahasaConfigJS("Mencari Data Mahasiswa Aktif...")%>';
                <% } %>
                if (selectSmt) selectSmt.disabled = false;
            }
        };

        window.injectModalPicker<%=rnd%> = function() {
            if (!document.getElementById('modalPicker<%=rnd%>')) {
                const modalHtml = '<div class="modal fade" id="modalPicker' + '<%=rnd%>' + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                                  '  <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                                  '    <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">' +
                                  '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                                  '        <h5 class="modal-title fw-bold"><i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Jendela Pencarian Data Pelanggan")%></h5>' +
                                  '        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                                  '      </div>' +
                                  '      <div class="modal-body p-0 bg-light" id="modalBodyPicker' + '<%=rnd%>' + '"></div>' +
                                  '    </div>' +
                                  '  </div>' +
                                  '</div>';
                document.body.insertAdjacentHTML('beforeend', modalHtml);
                
                const mEl = document.getElementById('modalPicker<%=rnd%>');
                mEl.addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
                mEl.addEventListener('hidden.bs.modal', function() { mEl.remove(); });
            }
        };

        window.bukaPicker<%=rnd%> = function() {
            const jkId = document.getElementById('selectJenisKegiatan<%=rnd%>').value;
            if (!jkId) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Harap tentukan Jenis Pembayaran terlebih dahulu!")%>', 'bg-warning text-dark');
                return;
            }

            const isMahasiswa = document.getElementById('isMahasiswa<%=rnd%>').value === "true";
            const className = isMahasiswa ? 'ais.database.model.Mahasiswa' : 'ais.database.model.BiodataCalonMahasiswa';
            const cols = isMahasiswa ? 'id,nim,nama' : 'id,noRegistrasi,nama';
            const orderParams = isMahasiswa ? "tahunangkatan desc, nim asc" : "tahunAkademik desc, noRegistrasi asc";

            if (typeof window.globalSelectedIds<%=rnd%> !== 'undefined') window.globalSelectedIds<%=rnd%>.clear();
            const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker' +
                        '&class=' + className + '&cols=' + encodeURIComponent(cols) + 
                        '&order=' + encodeURIComponent(orderParams) + '&radio=true' +
                        '&rnd=<%=rnd%>&callback=onDataTerpilih<%=rnd%>';
            
            window.injectModalPicker<%=rnd%>();
            document.getElementById('modalBodyPicker<%=rnd%>').innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary mb-3" style="width: 3rem; height: 3rem; border-width: 0.35em;"></div><h6 class="fw-bold text-secondary"><%=Common.getBahasaConfig("Menyiapkan Antarmuka Pencarian...")%></h6></div>';
            
            const modalObj = new bootstrap.Modal(document.getElementById('modalPicker<%=rnd%>'));
            modalObj.show();
            
            fetch(url).then(r => r.text()).then(html => {
                const body = document.getElementById('modalBodyPicker<%=rnd%>');
                body.innerHTML = html;
                body.querySelectorAll('script').forEach(s => {
                    const ns = document.createElement('script');
                    if (s.src) ns.src = s.src; else ns.textContent = s.textContent;
                    document.body.appendChild(ns); document.body.removeChild(ns);
                });
            }).catch(err => {
                document.getElementById('modalBodyPicker<%=rnd%>').innerHTML = '<div class="alert alert-danger m-4 shadow-sm rounded-3"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal memuat modul pencarian.")%></div>';
            });
        };

        window.onDataTerpilih<%=rnd%> = function(data, isRefresh) {
            if (data && data.length > 0) {
                const item = data[0];
                document.getElementById('idTerpilih<%=rnd%>').value = item.id;
                document.getElementById('labelMahasiswa<%=rnd%>').value = (item.nim || item.noRegistrasi || '') + " - " + item.nama;
                
                const smtEl = document.getElementById('selectSemester<%=rnd%>');
                if (smtEl && !isRefresh) smtEl.value = '';
                
                const modalEl = document.getElementById('modalPicker<%=rnd%>');
                if(modalEl) {
                    const modalObj = bootstrap.Modal.getInstance(modalEl);
                    if (modalObj) modalObj.hide();
                }
                
                window.loadTagihanMhs<%=rnd%>(isRefresh);
            }
        };

        window.loadTagihanMhs<%=rnd%> = function(isRefresh) {
            const id = document.getElementById('idTerpilih<%=rnd%>').value;
            const jkId = document.getElementById('selectJenisKegiatan<%=rnd%>').value;
            const isMhs = document.getElementById('isMahasiswa<%=rnd%>').value;
            
            const selectSmt = document.getElementById('selectSemester<%=rnd%>');
            const smt = selectSmt ? selectSmt.value : "";
            if(!id) return; 

            const container = document.getElementById('areaTagihan<%=rnd%>');
            const textLoading = isRefresh 
                ? '<%=Common.getBahasaConfigJS("Menyegarkan data tagihan pada sistem...")%>' 
                : '<%=Common.getBahasaConfigJS("Sistem sedang mengalkulasi rincian kewajiban pembayaran...")%>';
            
            container.innerHTML = '<div class="card border-0 shadow-sm rounded-4"><div class="card-body text-center py-5"><div class="spinner-border text-primary mb-3" style="width: 4rem; height: 4rem; border-width: 0.35em;"></div><h5 class="fw-bold text-dark">' + textLoading + '</h5></div></div>';
            
            let fetchUrl = '<%=serviceUrl%>&action=LOAD_TAGIHAN&id=' + id + '&jkId=' + jkId + '&isMahasiswa=' + isMhs + '&smt=' + smt + '&rnd=<%=rnd%>';
            if (isRefresh === true) {
                fetchUrl += '&refresh=true';
            }

            fetch(fetchUrl)
                .then(res => res.text())
                .then(html => { 
                    container.innerHTML = html; 
                    container.querySelectorAll('script').forEach(s => {
                        const ns = document.createElement('script');
                        if (s.src) ns.src = s.src; else ns.textContent = s.textContent;
                        document.body.appendChild(ns); document.body.removeChild(ns);
                    });
                })
                .catch(err => { 
                    container.innerHTML = '<div class="alert alert-danger shadow-sm border-0 m-0 rounded-4 p-4"><i class="fas fa-exclamation-triangle fa-2x mb-2 text-danger"></i><br><b class="d-block mb-1"><%=Common.getBahasaConfig("Galat Sistem:")%></b> <%=Common.getBahasaConfig("Gagal menghubungi peladen atau memuat rincian tagihan.")%></div>'; 
                });
        };

        // --- LOGIKA PEMUATAN OTOMATIS (AUTO-LOAD DARI PARAMETER URL) ---
        setTimeout(function() {
            <% if (autoPersonId != null) { %>
                const jkEl = document.getElementById('selectJenisKegiatan<%=rnd%>');
                if (jkEl) jkEl.value = '<%=autoJkId != null ? autoJkId : ""%>';
                
                // Meneruskan parameter secara eksplisit jika tersedia
                window.evaluasiEntitas<%=rnd%>('<%= autoIsMhs != null ? autoIsMhs : "" %>');
                
                const smtEl = document.getElementById('selectSemester<%=rnd%>');
                <% if (autoSmt != null) { %>
                    if (smtEl) smtEl.value = '<%=autoSmt%>';
                <% } %>
                
                const isRefreshURL = <%= "true".equalsIgnoreCase(paramRefresh) ? "true" : "false" %>;
                
                const dataAutoLoad = [{
                    id: '<%=autoPersonId%>',
                    <% String jsNim = autoPersonNimStr.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", ""); %>
                    <% String jsNama = autoPersonNamaStr.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", ""); %>
                    <% if (autoIsMhs != null && autoIsMhs) { %> nim: '<%=jsNim%>',
                    <% } else { %> noRegistrasi: '<%=jsNim%>', <% } %>
                    nama: '<%=jsNama%>'
                }];
                
                window.onDataTerpilih<%=rnd%>(dataAutoLoad, isRefreshURL);
            <% } else { %>
                window.evaluasiEntitas<%=rnd%>();
            <% } %>
        }, 150);
    })();
</script>