<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.VOMahasiswa"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Calendar"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(Common.ROOT + "/login.jsp");
        return;
    }

    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(6);
    
    String mhsIdStr = request.getParameter("mahasiswa");
    String calMhsIdStr = request.getParameter("calonMahasiswa");
    
    Mahasiswa mhs = null;
    BiodataCalonMahasiswa calMhs = null;
    VOMahasiswa mahasiswaTarget = null;
    
    Session sess = null;
    JenisKegiatan selectedJenisKegiatan = null;
    List<JenisKegiatan> listJk = null;
    int maxSemesterPilihan = 25;
    
    try {
        sess = HibernateUtil.openSession();
        
        if (mhsIdStr != null && !mhsIdStr.trim().isEmpty()) {
            mhs = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), Long.parseLong(mhsIdStr), true);
            mahasiswaTarget = mhs;
        } else if (calMhsIdStr != null && !calMhsIdStr.trim().isEmpty()) {
            calMhs = (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), Long.parseLong(calMhsIdStr), true);
            mahasiswaTarget = calMhs;
        } else {
            if (tbmuser.getMahasiswa() != null) {
                mhs = tbmuser.getMahasiswa();
                mahasiswaTarget = mhs;
            }
        }
        
        if (mahasiswaTarget == null) {
            out.print("<div class='alert alert-danger m-4 shadow-sm rounded-4 text-center'><i class='fas fa-exclamation-triangle fa-2x mb-3 text-danger'></i><br><b>" + Common.getBahasaConfig("Akses Ditolak") + "</b><br>" + Common.getBahasaConfig("Data mahasiswa tidak ditemukan atau Anda tidak memiliki akses.") + "</div>");
            return;
        }

        listJk = ConstantValues.simpleList(sess.createCriteria(JenisKegiatan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), JenisKegiatan.class);
        
        String sjkId = request.getParameter("selectedJenisKegiatan");
        if (sjkId != null && !sjkId.trim().isEmpty()) {
            selectedJenisKegiatan = (JenisKegiatan) sess.get(JenisKegiatan.class, Long.parseLong(sjkId));
        } else {
            selectedJenisKegiatan = (JenisKegiatan) sess.createCriteria(JenisKegiatan.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("defaultPembayaran", true)).setMaxResults(1).uniqueResult();
        }

        try { maxSemesterPilihan = Integer.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/informasi_pembayaran_mahasiswa.jsp:69");}

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/informasi_pembayaran_mahasiswa.jsp:72");
    } finally {
        if (sess != null && sess.isOpen()) { sess.disconnect(); sess.close(); }
        try { HibernateUtil.closeSessionQuietly(sess); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/informasi_pembayaran_mahasiswa.jsp:75");}
    }

    // Ekstraksi Info Visual Mahasiswa
    String identitas = ""; String nama = ""; String prodi = ""; String kontak = "";
    Tbmuser dummyUser = new Tbmuser();
    if (mhs != null) {
        dummyUser.setMahasiswa(mhs);
        identitas = mhs.getNim() != null ? mhs.getNim() : "-";
        nama = mhs.getNama() != null ? mhs.getNama() : "-";
        prodi = (mhs.getJurusan() != null ? mhs.getJurusan().getNama() : "-");
        kontak = (mhs.getTelp() != null ? mhs.getTelp() : "") + (mhs.getEmail() != null ? " | " + mhs.getEmail() : "");
    } else if (calMhs != null) {
        dummyUser.setBiodataCalonMahasiswa(calMhs);
        identitas = calMhs.getNoRegistrasi() != null ? calMhs.getNoRegistrasi() : "-";
        nama = calMhs.getNama() != null ? calMhs.getNama() : "-";
        prodi = (calMhs.ambilJurusan() != null ? calMhs.ambilJurusan().getNama() : "-");
        kontak = (calMhs.getHp() != null ? calMhs.getHp() : "") + (calMhs.getEmail() != null ? " | " + calMhs.getEmail() : "");
    }
    
    String urlFotoDB = CommonMedia.getUrlFotoPengguna(dummyUser);
    String linkFoto = (urlFotoDB == null || urlFotoDB.trim().isEmpty()) ? request.getContextPath() + "/component/uiux/assets/img/team/avatar.png" : urlFotoDB;

    boolean isAdmin = (tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
    boolean showVaTab = isAdmin && Konfigurasi.AKTIF.equals(Common.getKonfigurasi("virtual_account_muncul_di_halaman_mahasiswa", Konfigurasi.AKTIF).getNilai());
%>

<style>
    .nav-tabs-custom-<%=rnd%> .nav-link { color: #6c757d; font-weight: 600; border: none; border-bottom: 3px solid transparent; padding: 0.75rem 1.25rem; transition: all 0.2s; }
    .nav-tabs-custom-<%=rnd%> .nav-link:hover { color: #0d6efd; border-bottom-color: rgba(13, 110, 253, 0.3); }
    .nav-tabs-custom-<%=rnd%> .nav-link.active { color: #0d6efd; border-bottom-color: #0d6efd; background-color: transparent; }
    .hover-elevate-<%=rnd%> { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    .hover-elevate-<%=rnd%>:hover { transform: translateY(-2px); box-shadow: 0 0.5rem 1rem rgba(0,0,0,.1) !important; }
    .filter-panel-<%=rnd%> { background-color: #fcfcfc; border: 1px solid rgba(0,0,0,0.08); border-radius: 0.75rem; padding: 1.25rem; }
    .iframe-tab-<%=rnd%> { width: 100%; height: 75vh; border: none; border-radius: 0.75rem; background: #fff; }
</style>

<div class="container-fluid py-4 animate__animated animate__fadeIn">

    <div class="card border-0 shadow-sm rounded-4 mb-4 overflow-hidden">
        <div class="row g-0">
            <div class="col-md-8 p-4 d-flex align-items-center">
                <img src="<%=linkFoto%>" alt="<%=Common.getBahasaConfig("Foto")%>" class="rounded-circle border border-4 border-white shadow-sm me-4" style="width: 85px; height: 85px; object-fit: cover;">
                <div>
                    <h5 class="fw-bold text-dark mb-1"><%=nama%></h5>
                    <div class="d-flex flex-wrap gap-2 mb-2">
                        <span class="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25"><i class="fas fa-id-card me-1"></i><%=identitas%></span>
                        <span class="badge bg-info bg-opacity-10 text-info border border-info border-opacity-25"><i class="fas fa-graduation-cap me-1"></i><%=prodi%></span>
                    </div>
                    <small class="text-secondary fw-semibold"><i class="fas fa-address-book me-1"></i><%=kontak%></small>
                </div>
            </div>
            <div class="col-md-4 bg-light p-4 d-flex align-items-center justify-content-md-end justify-content-center border-start">
                <div class="d-flex flex-column gap-2 w-100">
                    <button class="btn btn-primary fw-bold rounded-pill shadow-sm btn-sm hover-elevate-<%=rnd%>" onclick="window.muatUlangData<%=rnd%>()">
                        <i class="fas fa-sync-alt me-2"></i><%=Common.getBahasaConfig("Segarkan Data")%>
                    </button>
                    <% if (isAdmin) { %>
                    <button class="btn btn-outline-success fw-bold rounded-pill shadow-sm btn-sm hover-elevate-<%=rnd%>" onclick="window.cetakTagihan<%=rnd%>()">
                        <i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Cetak Tagihan")%>
                    </button>
                    <% if (mhs != null) { %>
                    <button class="btn btn-outline-danger fw-bold rounded-pill shadow-sm btn-sm hover-elevate-<%=rnd%>" onclick="window.cetakBebasTunggakan<%=rnd%>()">
                        <i class="fas fa-file-signature me-2"></i><%=Common.getBahasaConfig("Surat Bebas Tunggakan")%>
                    </button>
                    <% } } %>
                </div>
            </div>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4">
        <div class="card-header bg-white border-bottom p-0 pt-2 px-3">
            <ul class="nav nav-tabs nav-tabs-custom-<%=rnd%>" id="tabPembayaran<%=rnd%>" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active" id="nav-tagihan-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#nav-tagihan-<%=rnd%>" type="button" role="tab"><i class="fas fa-file-invoice-dollar me-2"></i><%=Common.getBahasaConfig("Tagihan & Pembayaran")%></button>
                </li>
                <% if (isAdmin) { %>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="nav-proses-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#nav-proses-<%=rnd%>" type="button" role="tab" onclick="window.muatIframe<%=rnd%>('iframe-proses-<%=rnd%>', '/common/daftarulang_mahasiswa_lama.zul?mahasiswa=<%=mhs!=null?mhs.getId():calMhs.getId()%>')"><i class="fas fa-cash-register me-2"></i><%=Common.getBahasaConfig("Proses Pembayaran")%></button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="nav-bukti-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#nav-bukti-<%=rnd%>" type="button" role="tab" onclick="window.muatIframe<%=rnd%>('iframe-bukti-<%=rnd%>', '/pages/master/bukti_pembayaran.zul?mahasiswa=<%=mhs!=null?mhs.getId():calMhs.getId()%>')"><i class="fas fa-receipt me-2"></i><%=Common.getBahasaConfig("Bukti Pembayaran")%></button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="nav-tabungan-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#nav-tabungan-<%=rnd%>" type="button" role="tab" onclick="window.muatIframe<%=rnd%>('iframe-tabungan-<%=rnd%>', '/pages/master/deposit.zul?mahasiswa=<%=mhs!=null?mhs.getId():calMhs.getId()%>')"><i class="fas fa-piggy-bank me-2"></i><%=Common.getBahasaConfig("Tabungan / Deposit")%></button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="nav-sejarah-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#nav-sejarah-<%=rnd%>" type="button" role="tab" onclick="window.muatIframe<%=rnd%>('iframe-sejarah-<%=rnd%>', '/pages/master/log_pembayaran.zul?mahasiswa=<%=mhs!=null?mhs.getId():calMhs.getId()%>')"><i class="fas fa-history me-2"></i><%=Common.getBahasaConfig("Sejarah Pembayaran")%></button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="nav-info-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#nav-info-<%=rnd%>" type="button" role="tab"><i class="fas fa-info-circle me-2"></i><%=Common.getBahasaConfig("Info Pembayaran")%></button>
                </li>
                <% if (showVaTab) { %>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="nav-va-tab-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#nav-va-<%=rnd%>" type="button" role="tab" onclick="window.muatIframe<%=rnd%>('iframe-va-<%=rnd%>', '/pages/master/virtual_account_bank.zul')"><i class="fas fa-qrcode me-2"></i><%=Common.getBahasaConfig("Tagihan VA")%></button>
                </li>
                <% } } %>
            </ul>
        </div>

        <div class="card-body p-4">
            <div class="tab-content" id="tabPembayaranContent<%=rnd%>">
                
                <div class="tab-pane fade show active" id="nav-tagihan-<%=rnd%>" role="tabpanel">
                    <form id="formFilterTagihan<%=rnd%>" onsubmit="event.preventDefault(); window.loadDaftarTagihan<%=rnd%>(false);">
                        <input type="hidden" name="action" value="LOAD_TAGIHAN">
                        <input type="hidden" name="personId" value="<%=mhs!=null?mhs.getId():calMhs.getId()%>">
                        <input type="hidden" name="isMahasiswa" value="<%=mhs!=null?"true":"false"%>">
                        
                        <div class="filter-panel-<%=rnd%> mb-4">
                            <div class="row g-3 align-items-end">
                                <div class="col-lg-4 col-md-12">
                                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-receipt me-1 text-primary"></i> <%=Common.getBahasaConfig("Jenis Kegiatan / Tagihan")%></label>
                                    <select class="form-select border-secondary border-opacity-25 shadow-sm fw-bold text-dark" name="jkId" id="fJkId<%=rnd%>" required onchange="window.aturStatusSemester<%=rnd%>()">
                                        <option value="BELUM_LUNAS" <%=selectedJenisKegiatan==null?"selected":""%>><%=Common.getBahasaConfig(">> Tampilkan Semua Yang Belum Lunas")%></option>
                                        <option value="SEMUA_TAGIHAN"><%=Common.getBahasaConfig(">> Tampilkan Semua Tagihan (Lunas & Belum)")%></option>
                                        <% if (listJk != null) { for(JenisKegiatan jk : listJk) { 
                                            boolean isSel = (selectedJenisKegiatan != null && selectedJenisKegiatan.getId().equals(jk.getId()));
                                        %>
                                            <option value="<%=jk.getId()%>" data-is-calon="<%=ConstantValues.PENDAFTARAN_CALON_MAHASISWA!=null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(jk.getId())%>" data-is-daftar="<%=ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU!=null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId().equals(jk.getId())%>" <%=isSel?"selected":""%>><%=jk.getNamaKegiatan()%></option>
                                        <% } } %>
                                    </select>
                                </div>
                                <div class="col-lg-3 col-md-6">
                                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-hourglass-start me-1 text-success"></i> <%=Common.getBahasaConfig("Semester Mulai")%></label>
                                    <select class="form-select border-secondary border-opacity-25 shadow-sm" name="smtMulai" id="fSmtMulai<%=rnd%>">
                                        <% int defMulai = (mhs != null) ? 1 : 0; int startI = (mhs != null) ? 1 : 0;
                                           for (int i = startI; i <= maxSemesterPilihan; i++) { %>
                                           <option value="<%=i%>" <%=i==defMulai?"selected":""%>><%=i%></option>
                                        <% } %>
                                    </select>
                                </div>
                                <div class="col-lg-3 col-md-6">
                                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-hourglass-end me-1 text-danger"></i> <%=Common.getBahasaConfig("Semester Sampai")%></label>
                                    <select class="form-select border-secondary border-opacity-25 shadow-sm" name="smtSampai" id="fSmtSampai<%=rnd%>">
                                        <% int defSampai = (mhs != null) ? mhs.currentSemester() : 1;
                                           for (int i = startI; i <= maxSemesterPilihan; i++) { %>
                                           <option value="<%=i%>" <%=i==defSampai?"selected":""%>><%=i%></option>
                                        <% } %>
                                    </select>
                                </div>
                                <div class="col-lg-2 col-md-12">
                                    <button type="submit" class="btn btn-primary w-100 fw-bold shadow-sm hover-elevate-<%=rnd%>">
                                        <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Tampilkan")%>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </form>

                    <div id="wrapperTagihan<%=rnd%>" class="position-relative" style="min-height: 300px;">
                        <div class="text-center py-5 text-muted">
                            <i class="fas fa-inbox fa-3x mb-3 opacity-25"></i>
                            <h6 class="fw-bold"><%=Common.getBahasaConfig("Klik Tampilkan untuk memuat data tagihan.")%></h6>
                        </div>
                    </div>
                </div>

                <% if (isAdmin) { %>
                <div class="tab-pane fade" id="nav-proses-<%=rnd%>" role="tabpanel"><iframe id="iframe-proses-<%=rnd%>" class="iframe-tab-<%=rnd%>"></iframe></div>
                <div class="tab-pane fade" id="nav-bukti-<%=rnd%>" role="tabpanel"><iframe id="iframe-bukti-<%=rnd%>" class="iframe-tab-<%=rnd%>"></iframe></div>
                <div class="tab-pane fade" id="nav-tabungan-<%=rnd%>" role="tabpanel"><iframe id="iframe-tabungan-<%=rnd%>" class="iframe-tab-<%=rnd%>"></iframe></div>
                <div class="tab-pane fade" id="nav-sejarah-<%=rnd%>" role="tabpanel"><iframe id="iframe-sejarah-<%=rnd%>" class="iframe-tab-<%=rnd%>"></iframe></div>
                <div class="tab-pane fade" id="nav-info-<%=rnd%>" role="tabpanel">
                    <div class="alert alert-info text-center m-5 shadow-sm rounded-4"><i class="fas fa-info-circle fa-2x mb-2 d-block"></i><%=Common.getBahasaConfig("Modul Info Pembayaran / Dasbor akan dimuat di sini.")%></div>
                </div>
                <% if (showVaTab) { %>
                <div class="tab-pane fade" id="nav-va-<%=rnd%>" role="tabpanel"><iframe id="iframe-va-<%=rnd%>" class="iframe-tab-<%=rnd%>"></iframe></div>
                <% } } %>

            </div>
        </div>
    </div>
</div>

<script>
    window.aturStatusSemester<%=rnd%> = function() {
        const sel = document.getElementById('fJkId<%=rnd%>');
        const opt = sel.options[sel.selectedIndex];
        const smtMulai = document.getElementById('fSmtMulai<%=rnd%>');
        const smtSampai = document.getElementById('fSmtSampai<%=rnd%>');
        
        if (opt.dataset.isCalon === 'true') {
            smtMulai.value = "0"; smtSampai.value = "0";
            smtMulai.disabled = true; smtSampai.disabled = true;
        } else if (opt.dataset.isDaftar === 'true') {
            smtMulai.value = "1"; smtSampai.value = "1";
            smtMulai.disabled = false; smtSampai.disabled = false;
        } else {
            smtMulai.disabled = false; smtSampai.disabled = false;
        }
    };

    window.muatIframe<%=rnd%> = function(idIframe, src) {
        const iframe = document.getElementById(idIframe);
        if (iframe && !iframe.src) {
            iframe.src = '<%=Common.ROOT%>' + src;
        }
    };

    window.loadDaftarTagihan<%=rnd%> = function(isRefresh) {
        const wrapper = document.getElementById('wrapperTagihan<%=rnd%>');
        wrapper.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary" style="width: 3rem; height: 3rem; border-width: 0.35em;"></div><h6 class="mt-4 text-secondary fw-bold"><%=Common.getBahasaConfig("Mengkalkulasi tagihan dan denda, mohon tunggu...")%></h6><small class="text-muted"><%=Common.getBahasaConfig("Proses ini menggunakan sistem komputasi paralel.")%></small></div>';
        
        const form = document.getElementById('formFilterTagihan<%=rnd%>');
        const formData = new URLSearchParams(new FormData(form));
        if (isRefresh) formData.append("refresh", "true");

        // Paksa baca nilai yang disabled
        if (document.getElementById('fSmtMulai<%=rnd%>').disabled) formData.set("smtMulai", document.getElementById('fSmtMulai<%=rnd%>').value);
        if (document.getElementById('fSmtSampai<%=rnd%>').disabled) formData.set("smtSampai", document.getElementById('fSmtSampai<%=rnd%>').value);

        const urlFetch = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_service_informasi_pembayaran_mahasiswa';
        
        fetch(urlFetch, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(res => res.json())
        .then(res => {
            if (res.status === '00') {
                wrapper.innerHTML = res.html;
            } else {
                wrapper.innerHTML = '<div class="alert alert-danger m-4 shadow-sm rounded-4 text-center"><i class="fas fa-exclamation-triangle fa-2x mb-3 text-danger"></i><br><b><%=Common.getBahasaConfig("Galat Sistem")%></b><br>' + res.message + '</div>';
            }
        })
        .catch(err => {
            wrapper.innerHTML = '<div class="alert alert-danger m-4 shadow-sm rounded-4 text-center"><i class="fas fa-wifi fa-2x mb-3 text-danger"></i><br><b><%=Common.getBahasaConfig("Galat Koneksi")%></b><br><%=Common.getBahasaConfig("Gagal menghubungi peladen.")%></div>';
        });
    };

    window.muatUlangData<%=rnd%> = function() {
        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Menyegarkan pangkalan data...")%>', 'bg-info text-white');
        window.loadDaftarTagihan<%=rnd%>(true);
    };

    window.cetakTagihan<%=rnd%> = function() {
        const form = document.getElementById('formFilterTagihan<%=rnd%>');
        const formData = new URLSearchParams(new FormData(form));
        formData.set("action", "CETAK_TAGIHAN");
        
        if (document.getElementById('fSmtMulai<%=rnd%>').disabled) formData.set("smtMulai", document.getElementById('fSmtMulai<%=rnd%>').value);
        if (document.getElementById('fSmtSampai<%=rnd%>').disabled) formData.set("smtSampai", document.getElementById('fSmtSampai<%=rnd%>').value);

        const urlCetak = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_service_informasi_pembayaran_mahasiswa';
        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Mempersiapkan dokumen surat tagihan...")%>', 'bg-primary text-white');
        
        fetch(urlCetak, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(res => res.json())
        .then(res => {
            if (res.status === '00' && res.urlPdf) {
                window.tampilkanPdfModal<%=rnd%>(res.urlPdf, '<%=Common.getBahasaConfigJS("Surat Tagihan Pembayaran")%>');
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast(res.message || '<%=Common.getBahasaConfigJS("Gagal mencetak dokumen.")%>', 'bg-danger text-white');
            }
        }).catch(e => alert('<%=Common.getBahasaConfigJS("Terjadi galat jaringan saat mencetak dokumen.")%>'));
    };

    window.cetakBebasTunggakan<%=rnd%> = function() {
        const form = document.getElementById('formFilterTagihan<%=rnd%>');
        const formData = new URLSearchParams(new FormData(form));
        formData.set("action", "CETAK_BEBAS_TUNGGAKAN");
        
        if (document.getElementById('fSmtMulai<%=rnd%>').disabled) formData.set("smtMulai", document.getElementById('fSmtMulai<%=rnd%>').value);
        if (document.getElementById('fSmtSampai<%=rnd%>').disabled) formData.set("smtSampai", document.getElementById('fSmtSampai<%=rnd%>').value);

        const urlCetak = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_service_informasi_pembayaran_mahasiswa';
        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Mempersiapkan surat bebas tunggakan...")%>', 'bg-primary text-white');
        
        fetch(urlCetak, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: formData.toString() })
        .then(res => res.json())
        .then(res => {
            if (res.status === '00' && res.urlPdf) {
                window.tampilkanPdfModal<%=rnd%>(res.urlPdf, '<%=Common.getBahasaConfigJS("Surat Bebas Tunggakan")%>');
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast(res.message || '<%=Common.getBahasaConfigJS("Gagal mencetak dokumen.")%>', 'bg-danger text-white');
            }
        }).catch(e => alert('<%=Common.getBahasaConfigJS("Terjadi galat jaringan saat mencetak dokumen.")%>'));
    };

    window.tampilkanPdfModal<%=rnd%> = function(urlPdf, titleStr) {
        const modalId = 'modalCetakPdf<%=rnd%>';
        const existingModal = document.getElementById(modalId);
        if (existingModal) { existingModal.remove(); document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); }

        const modalHtml = '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                          '  <div class="modal-dialog modal-xl modal-dialog-centered">' +
                          '    <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">' +
                          '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                          '        <h5 class="modal-title fw-bold"><i class="fas fa-file-pdf me-2"></i> ' + titleStr + '</h5>' +
                          '        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                          '      </div>' +
                          '      <div class="modal-body p-0 bg-light">' +
                          '        <iframe src="' + urlPdf + '" width="100%" height="600px" style="border: none;"></iframe>' +
                          '      </div>' +
                          '      <div class="modal-footer border-0 bg-white d-flex justify-content-center py-3">' +
                          '        <button type="button" class="btn btn-secondary px-5 py-2 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Tutup") %></button>' +
                          '      </div>' +
                          '    </div>' +
                          '  </div>' +
                          '</div>';
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalObj = new bootstrap.Modal(document.getElementById(modalId));
        modalObj.show();
        
        document.getElementById(modalId).addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
        document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
    };

    // Auto-load awal
    setTimeout(function() {
        window.aturStatusSemester<%=rnd%>();
        window.loadDaftarTagihan<%=rnd%>(false);
    }, 200);
</script>