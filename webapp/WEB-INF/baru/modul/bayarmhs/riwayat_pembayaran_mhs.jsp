<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Calendar"%>

<%
    // =========================================================================================
    // 1. PENGECEKAN HAK AKSES & PENYIAPAN AWAL
    // =========================================================================================
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(Common.ROOT + "/login.jsp");
        return;
    }

    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(6);
    String paramHanyaJkId = request.getParameter("hanya_untuk_jenis_kegiatan");
    String paramHanyaMhs = request.getParameter("hanya_untuk_mahasiswa");

    if (paramHanyaJkId != null && !paramHanyaJkId.trim().isEmpty()) {
        paramHanyaJkId = paramHanyaJkId.trim();
    }
    Long jkId = null;
    try { if (paramHanyaJkId != null && !paramHanyaJkId.isEmpty()) jkId = Long.parseLong(paramHanyaJkId); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/riwayat_pembayaran_mhs.jsp:32");}
    
    Session sess = null;
    List<JenisKegiatan> listJk = new ArrayList<JenisKegiatan>();
    List<String> listTa = new ArrayList<String>();

    try {
        sess = HibernateUtil.openSession();
        listJk = ConstantValues.simpleList(sess.createCriteria(JenisKegiatan.class).addOrder(Order.asc("namaKegiatan")), JenisKegiatan.class);
        listTa = sess.createQuery("select distinct k.tahunAkademik from Kegiatan k where k.tahunAkademik is not null order by k.tahunAkademik desc").list();
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/riwayat_pembayaran_mhs.jsp:43");
    } finally {
        if(sess != null && sess.isOpen()){ sess.disconnect(); sess.close(); }
        try{ HibernateUtil.closeSessionQuietly(sess); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/riwayat_pembayaran_mhs.jsp:46");}
    }

    String serviceUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_riwayat_pembayaran_mhs_services";
    
    boolean editPriv = false, deletePriv = false, isAdminPriv = false;
    if (tbmuser != null && tbmuser.getUserId() != null) {
        editPriv = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
        deletePriv = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
        if (Common.getApakahAdminLain(tbmuser)) { editPriv = true; deletePriv = true; isAdminPriv = true; }
    }
    
    boolean hideJk = paramHanyaJkId != null && !paramHanyaJkId.trim().isEmpty();
%>

<style>
    .hover-elevate-<%=rnd%> { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    .hover-elevate-<%=rnd%>:hover { transform: translateY(-2px); box-shadow: 0 0.5rem 1rem rgba(0,0,0,.1) !important; }
    
    /* Konfigurasi Responsif untuk Modal/Popup Edit */
    .modal-xl-custom-<%=rnd%> { max-width: 95%; margin: 2.5% auto; }
    .modal-xl-custom-<%=rnd%> .modal-content { height: 95vh; display: flex; flex-direction: column; }
    .modal-xl-custom-<%=rnd%> .modal-body { flex: 1 1 auto; overflow-y: auto; overflow-x: hidden; padding: 0; }
    
    @media (max-width: 768px) {
        .modal-xl-custom-<%=rnd%> { max-width: 100%; margin: 0; }
        .modal-xl-custom-<%=rnd%> .modal-content { height: 100vh; border-radius: 0 !important; }
    }
</style>

<div class="container-fluid py-4 animate__animated animate__fadeIn">
    
    <div class="d-flex align-items-center mb-4 p-3 bg-white shadow-sm rounded-4 border-start border-4 border-primary">
        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 50px; height: 50px;">
            <i class="fas fa-history fs-4"></i>
        </div>
        <div>
            <h4 class="mb-0 fw-bold text-dark"><%= Common.getBahasaConfig("Riwayat Transaksi Pelanggan") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Daftar histori pembayaran dan penagihan yang terekam pada pangkalan data.") %></p>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4 mb-4 overflow-hidden">
        <div class="card-header bg-light border-bottom py-3 d-flex justify-content-between align-items-center">
            <h6 class="fw-bold mb-0 text-primary"><i class="fas fa-filter me-2"></i> <%= Common.getBahasaConfig("Penyaringan Data") %></h6>
            <div>
                <button type="button" class="btn btn-sm btn-success fw-bold me-2 shadow-sm hover-elevate-<%=rnd%>" onclick="window.bukaModalExport<%=rnd%>('excel')">
                    <i class="fas fa-file-excel me-1"></i> <%= Common.getBahasaConfig("Ekspor Excel") %>
                </button>
                <button type="button" class="btn btn-sm btn-danger fw-bold shadow-sm hover-elevate-<%=rnd%>" onclick="window.bukaModalExport<%=rnd%>('pdf')">
                    <i class="fas fa-print me-1"></i> <%= Common.getBahasaConfig("Cetak Laporan PDF") %>
                </button>
            </div>
        </div>
        <div class="card-body bg-white p-4">
            <form id="formFilterRiwayat<%=rnd%>" method="GET" action="" onsubmit="event.preventDefault(); window.loadDataRiwayat<%=rnd%>(1);">
                
                <% if(paramHanyaMhs != null) { %><input type="hidden" name="hanya_untuk_mahasiswa" value="<%= paramHanyaMhs %>"><% } %>
                <% if(hideJk) { %><input type="hidden" name="jkId" value="<%= paramHanyaJkId %>"><% } %>

                <div class="row g-3">
                    <% if(paramHanyaMhs == null) { %>
                        <div class="col-md-3">
                            <label class="form-label fw-bold text-secondary small"><i class="fas fa-users me-1"></i> <%= Common.getBahasaConfig("Kategori Pelanggan") %></label>
                            <select class="form-select border-secondary border-opacity-25" name="hanya_untuk_mahasiswa" id="fKategori<%=rnd%>" onchange="window.loadDataRiwayat<%=rnd%>(1)">
                                <option value=""><%= Common.getBahasaConfig("-- Semua Kategori --") %></option>
                                <option value="true"><%= Common.getBahasaConfig("Mahasiswa Aktif") %></option>
                                <option value="false"><%= Common.getBahasaConfig("Calon Mahasiswa") %></option>
                            </select>
                        </div>
                    <% } %>

                    <div class="col-md-<%= paramHanyaMhs != null ? "4" : "3" %>">
                        <label class="form-label fw-bold text-secondary small"><i class="fas fa-search me-1"></i> <%= Common.getBahasaConfig("Pencarian Identitas") %></label>
                        <input type="text" class="form-control border-secondary border-opacity-25" name="q" id="fq<%=rnd%>" placeholder="<%= Common.getBahasaConfig("NIM, No.Registrasi, atau Nama...") %>">
                    </div>
                    
                    <div class="col-md-<%= paramHanyaMhs != null ? "4" : "3" %> <%= hideJk ? "d-none" : "" %>">
                        <label class="form-label fw-bold text-secondary small"><i class="fas fa-tasks me-1"></i> <%= Common.getBahasaConfig("Jenis Kegiatan") %></label>
                        <select class="form-select border-secondary border-opacity-25" name="jkId" id="fjkId<%=rnd%>" onchange="window.loadItemBiaya<%=rnd%>(this.value);" <%= hideJk ? "disabled" : "" %>>
                            <option value=""><%= Common.getBahasaConfig("-- Semua Jenis Kegiatan --") %></option>
                            <% for(JenisKegiatan jk : listJk) { %>
                                <option value="<%= jk.getId() %>" <%= (jkId != null && jkId.equals(jk.getId())) ? "selected" : "" %>><%= jk.getNamaKegiatan() %></option>
                            <% } %>
                        </select>
                    </div>
                    
                    <div class="col-md-<%= paramHanyaMhs != null ? (hideJk ? "6" : "4") : "3" %>">
                        <label class="form-label fw-bold text-secondary small"><i class="fas fa-calendar-alt me-1"></i> <%= Common.getBahasaConfig("Tahun Akademik") %></label>
                        <select class="form-select border-secondary border-opacity-25" name="ta" id="fta<%=rnd%>">
                            <option value=""><%= Common.getBahasaConfig("-- Semua Tahun Akademik --") %></option>
                            <% for(String t : listTa) { %>
                                <option value="<%= t %>"><%= t %></option>
                            <% } %>
                        </select>
                    </div>

                    <div class="col-md-3">
                        <label class="form-label fw-bold text-secondary small"><i class="far fa-calendar-plus me-1"></i> <%= Common.getBahasaConfig("Mulai Tanggal Transaksi") %></label>
                        <input type="date" class="form-control border-secondary border-opacity-25" name="startDate" id="fStart<%=rnd%>">
                    </div>
                    
                    <div class="col-md-3">
                        <label class="form-label fw-bold text-secondary small"><i class="far fa-calendar-minus me-1"></i> <%= Common.getBahasaConfig("Sampai Tanggal Transaksi") %></label>
                        <input type="date" class="form-control border-secondary border-opacity-25" name="endDate" id="fEnd<%=rnd%>">
                    </div>

                    <div class="col-md-3">
                        <label class="form-label fw-bold text-secondary small"><i class="fas fa-layer-group me-1"></i> <%= Common.getBahasaConfig("Semester Tagihan") %></label>
                        <select class="form-select border-secondary border-opacity-25" name="smt" id="fsmt<%=rnd%>">
                            <option value=""><%= Common.getBahasaConfig("-- Semua Semester --") %></option>
                            <% for(int i=1; i<=14; i++) { %>
                                <option value="<%= i %>"><%= Common.getBahasaConfig("Semester") %> <%= i %></option>
                            <% } %>
                        </select>
                    </div>

                    <div class="col-md-3">
                        <label class="form-label fw-bold text-secondary small"><i class="fas fa-receipt me-1"></i> <%= Common.getBahasaConfig("Pencarian Rincian Biaya") %></label>
                        <input type="text" class="form-control border-secondary border-opacity-25" name="keywordItem" id="fItem<%=rnd%>" placeholder="<%= Common.getBahasaConfig("Nama Item atau Keterangan...") %>">
                    </div>
                </div>

                <div id="wrapperItemBiaya<%=rnd%>" class="mt-4 pt-3 border-top" style="display: none;">
                    <label class="form-label fw-bold text-secondary small mb-2"><i class="fas fa-tags me-1"></i> <%= Common.getBahasaConfig("Filter Khusus Rincian Item Biaya") %></label>
                    <div class="p-3 bg-light border border-secondary border-opacity-25 rounded-3" style="max-height: 180px; overflow-y: auto;">
                        <div class="row g-2" id="containerItemBiaya<%=rnd%>"></div>
                    </div>
                </div>

                <div class="mt-4 text-end">
                    <button type="button" class="btn btn-light border fw-bold shadow-sm px-4 me-2 hover-elevate-<%=rnd%>" onclick="window.resetFilterRyg<%=rnd%>()">
                        <i class="fas fa-redo-alt me-2"></i><%= Common.getBahasaConfig("Bersihkan Parameter") %>
                    </button>
                    <button type="submit" class="btn btn-primary fw-bold shadow-sm px-4 hover-elevate-<%=rnd%>">
                        <i class="fas fa-search me-2"></i><%= Common.getBahasaConfig("Terapkan Pencarian") %>
                    </button>
                </div>
            </form>
        </div>
    </div>

    <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
        <div class="table-responsive">
            <table class="table table-hover table-borderless align-middle mb-0" style="width: 100%;">
                <thead class="bg-dark text-white">
                    <tr>
                        <th class="py-3 px-4 fw-semibold" style="letter-spacing: 0.5px;"><%= Common.getBahasaConfig("Pelanggan (Identitas - Nama Lengkap)") %></th>
                        <th class="py-3 fw-semibold" style="letter-spacing: 0.5px;"><%= Common.getBahasaConfig("Deskripsi Transaksi & Kegiatan") %></th>
                        <th class="py-3 fw-semibold" style="letter-spacing: 0.5px;"><%= Common.getBahasaConfig("Catatan Tambahan") %></th>
                        <th class="py-3 text-end fw-semibold" style="letter-spacing: 0.5px;"><%= Common.getBahasaConfig("Nominal Transaksi (Rp)") %></th>
                        <th class="py-3 text-center fw-semibold" style="width: 120px; letter-spacing: 0.5px;"><%= Common.getBahasaConfig("Tindakan") %></th>
                    </tr>
                </thead>
                <tbody id="riwayatTableBody<%=rnd%>"></tbody>
            </table>
        </div>
    </div>

    <div id="paginationContainer<%=rnd%>"></div>
</div>

<div class="modal fade" id="modalExport<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-4">
            <form action="<%=serviceUrl%>" method="POST" id="formExport<%=rnd%>" target="_blank">
                <input type="hidden" name="action" value="EXPORT_DATA">
                <input type="hidden" name="exportType" id="exportTypeVal<%=rnd%>" value="">
                
                <div id="hiddenFilters<%=rnd%>"></div>

                <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">
                    <h5 class="modal-title fw-bold" id="modalExportTitle<%=rnd%>">
                        <i class="fas fa-file-export me-2"></i> <%= Common.getBahasaConfig("Penarikan Laporan Transaksi") %>
                    </h5>
                    <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light text-center">
                    <p class="text-muted fw-bold mb-4">
                        <%= Common.getBahasaConfig("Silakan tentukan batasan baris data yang akan ditarik agar tidak membebani sistem peladen.") %> 
                        <br><i class="fas fa-info-circle text-primary me-1 mt-2"></i> <i><%= Common.getBahasaConfig("Rekomendasi maksimal: 1.000 baris") %></i>
                    </p>
                    <div class="row g-3 justify-content-center align-items-center">
                        <div class="col-5">
                            <label class="form-label fw-bold text-secondary"><%= Common.getBahasaConfig("Dimulai dari Baris Ke-") %></label>
                            <input type="number" class="form-control form-control-lg text-center fw-bold text-primary shadow-sm" name="startRow" value="1" min="1">
                        </div>
                        <div class="col-2 pt-4"><i class="fas fa-arrow-right text-secondary opacity-50 fs-4"></i></div>
                        <div class="col-5">
                            <label class="form-label fw-bold text-secondary"><%= Common.getBahasaConfig("Sampai Baris Ke-") %></label>
                            <input type="number" class="form-control form-control-lg text-center fw-bold text-primary shadow-sm" name="endRow" value="1000" min="1">
                        </div>
                    </div>
                </div>
                <div class="modal-footer border-0 bg-white d-flex justify-content-between py-3">
                    <button type="button" class="btn btn-light border rounded-pill px-4 fw-bold shadow-sm hover-elevate-<%=rnd%>" data-bs-dismiss="modal">
                        <%= Common.getBahasaConfig("Batal") %>
                    </button>
                    <button type="submit" class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm hover-elevate-<%=rnd%>" onclick="setTimeout(function(){ bootstrap.Modal.getInstance(document.getElementById('modalExport<%=rnd%>')).hide(); }, 800);">
                        <i class="fas fa-cloud-download-alt me-2"></i> <%= Common.getBahasaConfig("Eksekusi Laporan") %>
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    var pDeleteRyg<%=rnd%> = <%=deletePriv || isAdminPriv%>;
    
    let dEnd<%=rnd%> = new Date();
    let dStart<%=rnd%> = new Date();
    dStart<%=rnd%>.setFullYear(dStart<%=rnd%>.getFullYear() - 3);
    document.getElementById('fStart<%=rnd%>').value = dStart<%=rnd%>.toISOString().split('T')[0];
    document.getElementById('fEnd<%=rnd%>').value = dEnd<%=rnd%>.toISOString().split('T')[0];

    window.loadItemBiaya<%=rnd%> = function(selectedJkId) {
        const wrapper = document.getElementById('wrapperItemBiaya<%=rnd%>');
        const container = document.getElementById('containerItemBiaya<%=rnd%>');

        if (!selectedJkId) {
            wrapper.style.display = 'none';
            container.innerHTML = '';
            return;
        }

        wrapper.style.display = 'block';
        container.innerHTML = '<div class="col-12 text-center py-3"><div class="spinner-border spinner-border-sm text-primary" role="status"></div> <span class="text-secondary small fw-bold ms-2"><%= Common.getBahasaConfig("Sedang menyinkronkan rincian item biaya...") %></span></div>';

        const formData = new URLSearchParams();
        formData.append('action', 'GET_ITEM_BIAYA');
        formData.append('jkId', selectedJkId);

        fetch('<%=serviceUrl%>', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(async res => {
            const text = await res.text();
            try { return JSON.parse(text); } 
            catch(e) { console.error("Error Parse JSON (GET_ITEM_BIAYA):", text); throw e; }
        })
        .then(result => {
            if(result && result.status === '00' && result.data.length > 0) {
                let html = '';
                result.data.forEach(ib => {
                    html += '<div class="col-md-6 col-lg-4">' +
                                '<div class="form-check">' +
                                    '<input class="form-check-input filter-ib border-secondary" type="checkbox" name="itemBiayaIds" value="' + ib.id + '" id="ib_' + ib.id + '_<%=rnd%>">' +
                                    '<label class="form-check-label text-dark fw-semibold" for="ib_' + ib.id + '_<%=rnd%>" style="font-size: 0.85rem;">' + ib.nama + '</label>' +
                                '</div>' +
                            '</div>';
                });
                container.innerHTML = html;
            } else {
                container.innerHTML = '<div class="col-12 text-muted small"><i class="fas fa-info-circle me-1 text-primary"></i> <%= Common.getBahasaConfig("Tidak terdapat rincian item biaya spesifik yang dikonfigurasi untuk Jenis Kegiatan ini.") %></div>';
            }
            window.loadDataRiwayat<%=rnd%>(1);
        })
        .catch(err => {
            container.innerHTML = '<div class="col-12 text-danger small"><i class="fas fa-exclamation-triangle me-1"></i> <%= Common.getBahasaConfig("Gagal menyinkronkan data dari peladen.") %></div>';
        });
    };

    window.loadDataRiwayat<%=rnd%> = function(page) {
        const form = document.getElementById('formFilterRiwayat<%=rnd%>');
        const formData = new URLSearchParams(new FormData(form));
        
        formData.delete('p');
        formData.delete('s');
        
        formData.set('action', 'LOAD_DATA');
        formData.set('page', page);

        const tBody = document.getElementById('riwayatTableBody<%=rnd%>');
        tBody.innerHTML = '<tr><td colspan="5" class="text-center py-5 bg-white"><div class="spinner-border text-primary" style="width: 3rem; height: 3rem; border-width: 0.35em;"></div><h6 class="mt-3 fw-bold text-secondary"><%= Common.getBahasaConfig("Sedang mengambil data transaksi dari peladen...") %></h6></td></tr>';

        fetch('<%=serviceUrl%>', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(async res => {
            const text = await res.text();
            try { return JSON.parse(text); } 
            catch(e) { console.error("Error Parse JSON (LOAD_DATA):", text.substring(0, 150)); throw e; }
        })
        .then(result => {
            if(result && result.status === '00') {
                window.renderTableRiwayat<%=rnd%>(result.data, result.total, result.totalPages, page);
            } else {
                tBody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-danger"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><br><b><%= Common.getBahasaConfig("Galat Sistem") %></b><br>' + (result.message || '<%= Common.getBahasaConfigJS("Respons data tidak valid.") %>') + '</td></tr>';
                document.getElementById('paginationContainer<%=rnd%>').innerHTML = '';
            }
        })
        .catch(err => {
            console.error(err);
            tBody.innerHTML = '<tr><td colspan="5" class="text-center py-5 text-danger"><i class="fas fa-wifi fa-3x mb-3 opacity-50"></i><br><b><%= Common.getBahasaConfig("Sambungan Komunikasi Terputus") %></b><br><%= Common.getBahasaConfig("Gagal menghubungi peladen basis data.") %></td></tr>';
            document.getElementById('paginationContainer<%=rnd%>').innerHTML = '';
        });
    };

    window.renderTableRiwayat<%=rnd%> = function(dataArr, totalRecords, totalPages, pageNum) {
        const tBody = document.getElementById('riwayatTableBody<%=rnd%>');
        const paginator = document.getElementById('paginationContainer<%=rnd%>');
        
        if(!dataArr || dataArr.length === 0) {
            tBody.innerHTML = '<tr><td colspan="5" class="text-center py-5 text-muted bg-white"><i class="fas fa-box-open fa-3x mb-3 text-secondary opacity-25"></i><h6 class="fw-bold text-dark"><%= Common.getBahasaConfig("Tidak Ada Transaksi Ditemukan") %></h6><p class="small mb-0"><%= Common.getBahasaConfig("Data transaksi tidak tersedia berdasarkan kriteria penyaringan yang Anda tentukan.") %></p></td></tr>';
            paginator.innerHTML = '';
            return;
        }

        let html = '';
        let lastGroup = "";
        const formatter = new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 });

        dataArr.forEach(r => {
            if(r.groupId !== lastGroup) {
                lastGroup = r.groupId;
                let btnCetakHtml = '<button class="btn btn-sm btn-outline-success rounded-circle shadow-sm ms-3 hover-elevate-<%=rnd%>" onclick="window.cetakStrukGlobal<%=rnd%>(' + r.idPelanggan + ', ' + r.isMahasiswa + ', \'' + r.jkId + '\', ' + r.smt + ')" title="<%= Common.getBahasaConfig("Cetak Dokumen Kuitansi Untuk Transaksi Ini") %>"><i class="fas fa-print"></i></button>';

                html += '<tr class="bg-light border-bottom border-top border-secondary border-opacity-25">' +
                            '<td colspan="5" class="px-4 py-2">' +
                                '<div class="d-flex align-items-center justify-content-between">' +
                                    '<div class="d-flex align-items-center"><i class="far fa-clock text-primary me-2"></i><span class="fw-bold text-dark"><%= Common.getBahasaConfig("Waktu Transaksi Direkam:") %> ' + r.waktu + '</span>' + btnCetakHtml + '</div>' +
                                    '<span class="badge bg-white text-secondary border border-secondary shadow-sm"><i class="fas fa-wallet text-success me-1"></i>' + r.via + '</span>' +
                                '</div>' +
                            '</td>' +
                        '</tr>';
            }

            let badgColor = r.isMahasiswa ? "bg-success" : "bg-info";
            let badgText = r.isMahasiswa ? '<%= Common.getBahasaConfigJS("Mahasiswa Aktif") %>' : '<%= Common.getBahasaConfigJS("Calon Mahasiswa") %>';
            
            // Konfigurasi URL Statis pemanggilan antarmuka agar selalu lari ke modul "bayarmhs"
            let urlEdit = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=pembayaran_online_mhs&id=' + r.idPelanggan + '&jkId=' + r.jkId + '&smt=' + r.smt + '&isMahasiswa=' + r.isMahasiswa + '&bolehEditJenisPembayaranDanMahasiswa=true&autoLoad=true';
            
            html += '<tr class="border-bottom bg-white">' +
                        '<td class="px-4">' +
                            '<div class="fw-bold text-primary fs-6">' + r.nis + '</div>' +
                            '<div class="small text-secondary fw-bold mt-1">' + r.namaSiswa + '</div>' +
                            '<span class="badge ' + badgColor + ' mt-2 shadow-sm" style="font-size: 0.65rem;">' + badgText + '</span>' +
                        '</td>' +
                        '<td>' +
                            '<div class="fw-bold text-dark">' + r.item + '</div>' +
                            '<div class="text-muted small mt-1"><i class="fas fa-tag me-1 text-primary"></i>' + r.kegiatan + ' <span class="mx-1">|</span> <span class="fw-semibold"><%= Common.getBahasaConfig("Semester:") %> ' + r.smt + '</span></div>' +
                        '</td>' +
                        '<td><div class="small text-secondary text-wrap" style="max-width: 250px;">' + r.ket + '</div></td>' +
                        '<td class="text-end fw-bold text-success fs-5">' + formatter.format(r.nominal) + '</td>' +
                        '<td class="text-center" data-aksi-baris>' +
                            '<div class="d-flex justify-content-center gap-2">';
            
            if(r.bisaEdit) {
                html += '<button type="button" onclick="window.bukaPopupEdit<%=rnd%>(\'' + urlEdit + '\')" class="btn btn-outline-primary rounded-circle shadow-sm hover-elevate-<%=rnd%>" style="width:32px; height:32px; display:inline-flex; align-items:center; justify-content:center;" title="<%= Common.getBahasaConfig("Buka Profil Penagihan Pelanggan (Perubahan)") %>"><i class="fas fa-user-edit" style="font-size: 0.8rem;"></i></button>';
            }
            if(pDeleteRyg<%=rnd%> && r.bisaEdit) {
                html += '<button type="button" onclick="window.hapusRiwayatGlobal<%=rnd%>(' + r.idCicilan + ')" class="btn btn-outline-danger rounded-circle shadow-sm hover-elevate-<%=rnd%>" style="width:32px; height:32px; display:inline-flex; align-items:center; justify-content:center;" title="<%= Common.getBahasaConfig("Batalkan atau Hapus Transaksi Ini") %>"><i class="fas fa-trash-alt" style="font-size: 0.8rem;"></i></button>';
            }
            
            html +=         '</div>' +
                        '</td>' +
                    '</tr>';
        });

        tBody.innerHTML = html;

        if(totalPages > 1) {
            let pHtml = '<nav aria-label="Navigasi Halaman">' +
                        '<ul class="pagination justify-content-center shadow-sm rounded-pill overflow-hidden d-inline-flex m-0 mb-3">' +
                        '<li class="page-item ' + (pageNum <= 1 ? "disabled" : "") + '"><a class="page-link px-3 fw-bold" href="javascript:void(0)" onclick="window.loadDataRiwayat<%=rnd%>(' + (pageNum - 1) + ')"><%= Common.getBahasaConfig("Sebelumnya") %></a></li>';
            
            let startP = Math.max(1, pageNum - 3);
            let endP = Math.min(totalPages, pageNum + 3);
            for (let p = startP; p <= endP; p++) {
                pHtml += '<li class="page-item ' + (p === pageNum ? "active" : "") + '"><a class="page-link fw-bold" href="javascript:void(0)" onclick="window.loadDataRiwayat<%=rnd%>(' + p + ')">' + p + '</a></li>';
            }
            
            pHtml +=    '<li class="page-item ' + (pageNum >= totalPages ? "disabled" : "") + '"><a class="page-link px-3 fw-bold" href="javascript:void(0)" onclick="window.loadDataRiwayat<%=rnd%>(' + (pageNum + 1) + ')"><%= Common.getBahasaConfig("Selanjutnya") %></a></li>' +
                        '</ul></nav>' +
                        '<div class="text-center text-muted small fw-semibold"><%= Common.getBahasaConfig("Menampilkan Halaman") %> ' + pageNum + ' <%= Common.getBahasaConfig("dari Total") %> ' + totalPages + ' <%= Common.getBahasaConfig("Halaman") %> <span class="mx-1">&bull;</span> (' + totalRecords + ' <%= Common.getBahasaConfig("Data Transaksi Terekam") %>)</div>';
            paginator.innerHTML = pHtml;
        } else {
            paginator.innerHTML = '';
        }
    };

    // =========================================================================================
    // FUNGSI POPUP: UBAH DATA PEMBAYARAN MENGGUNAKAN INJEKSI DOM (SESUAI INSTRUKSI)
    // =========================================================================================
    window.bukaPopupEdit<%=rnd%> = async function(urlEdit) {
        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Memuat antarmuka modul penagihan pelanggan...")%>', 'bg-info text-white');
        
        const modalId = 'modalEdit<%=rnd%>';
        const existingModal = document.getElementById(modalId);
        if (existingModal) { existingModal.remove(); document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); }

        var modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index: 1055;">' +
                '<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl-custom-<%=rnd%>">' +
                    '<div class="modal-content shadow-lg border-0 rounded-4">' +
                        '<div class="modal-header bg-gradient bg-primary text-white border-0 py-3 px-4">' +
                            '<h5 class="modal-title fw-bold">' +
                                '<i class="fas fa-user-edit me-2"></i> <%= Common.getBahasaConfig("Profil Penagihan & Perubahan Transaksi") %>' +
                            '</h5>' +
                            '<button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0 bg-light" id="bodyModalEdit<%=rnd%>">' +
                            '<div class="text-center py-5 mt-5">' +
                                '<div class="spinner-border text-primary" style="width: 4rem; height: 4rem; border-width: 0.35em;" role="status"></div>' +
                                '<h5 class="mt-4 text-secondary fw-bold"><%= Common.getBahasaConfig("Sedang Memuat Formulir...") %></h5>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        var modalElement = document.getElementById(modalId);
        var modalObj = new bootstrap.Modal(modalElement);
        modalObj.show();

        modalElement.addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
        modalElement.addEventListener('hidden.bs.modal', function () { 
            modalElement.remove(); 
            window.loadDataRiwayat<%=rnd%>(1); 
        });

        try {
            var response = await fetch(urlEdit);
            if(!response.ok) throw new Error("Gagal load formulir");
            var htmlContent = await response.text();
            
            var bodyEl = document.getElementById('bodyModalEdit<%=rnd%>');
            bodyEl.innerHTML = htmlContent;

            var scriptsArray = Array.from(bodyEl.getElementsByTagName('script')); 
            for (var i = 0; i < scriptsArray.length; i++) {
                var oldScript = scriptsArray[i];
                if (oldScript.src && oldScript.src.includes('email-decode')) continue; 
                var scriptNode = document.createElement('script');
                var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
                Array.from(oldScript.attributes).forEach(function(attr) { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
                scriptNode.type = 'text/javascript';
                if (srcEff) {
                    scriptNode.src = srcEff;
                    document.body.appendChild(scriptNode);
                } else { 
                    // Teknik Scope Block ({...}) untuk menipu "Let Redeclaration Error"
                    scriptNode.text = "{" + oldScript.innerHTML + "}"; 
                    document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); 
                }
            }
        } catch (e) {
            document.getElementById('bodyModalEdit<%=rnd%>').innerHTML = '<div class="alert alert-danger m-4 shadow-sm rounded-4 text-center py-5"><i class="fas fa-exclamation-triangle fa-4x mb-3 text-danger opacity-50"></i><br><h5><%=Common.getBahasaConfig("Gagal memuat modul pembayaran.")%></h5><p><%=Common.getBahasaConfig("Silakan periksa koneksi Anda dan coba kembali.")%></p></div>';
        }
    };

    // =========================================================================================
    // FUNGSI LAINNYA: HAPUS & CETAK STRUK 
    // =========================================================================================
    window.hapusRiwayatGlobal<%=rnd%> = function(idCicilan) {
        if(typeof prosesDeleteData === 'function') {
            prosesDeleteData('ais.database.model.CicilanPembayaran', idCicilan, function() {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Data riwayat transaksi berhasil dibatalkan dan dihapus secara permanen.") %>', 'bg-success text-white');
                window.loadDataRiwayat<%=rnd%>(1); 
            });
        } else {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Galat Sistem: Fungsi pembatalan tidak tersedia pada lingkungan ini.") %>', 'bg-danger text-white');
        }
    };

    window.cetakStrukGlobal<%=rnd%> = function(idPelanggan, isMahasiswa, jkId, smt) {
        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Mempersiapkan dokumen struk...")%>', 'bg-info text-white');
        
        const urlCetak = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_riwayat_pembayaran_mhs_services&action=CETAK_STRUK&id=' + idPelanggan + '&jkId=' + jkId + '&isMahasiswa=' + isMahasiswa + '&smt=' + smt;
        console.log("urlCetak ", urlCetak);
        
        fetch(urlCetak).then(response => {
            if (!response.ok) throw new Error("Gagal");
            return response.text();
        }).then(urlPdf => {
            const modalId = 'modalKwitansi<%=rnd%>';
            const existingModal = document.getElementById(modalId);
            if (existingModal) { existingModal.remove(); document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); }

            const modalHtml = '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                              '  <div class="modal-dialog modal-xl modal-dialog-centered">' +
                              '    <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">' +
                              '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                              '        <h5 class="modal-title fw-bold"><i class="fas fa-file-pdf me-2"></i> <%= Common.getBahasaConfig("Dokumen Kuitansi Pembayaran") %></h5>' +
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
        }).catch(err => {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi galat saat membuat dokumen struk.")%>', 'bg-danger text-white');
        });
    };

    window.resetFilterRyg<%=rnd%> = function() {
        document.getElementById('formFilterRiwayat<%=rnd%>').reset();
        
        let dEnd = new Date();
        let dStart = new Date();
        dStart.setFullYear(dStart.getFullYear() - 3);
        document.getElementById('fStart<%=rnd%>').value = dStart.toISOString().split('T')[0];
        document.getElementById('fEnd<%=rnd%>').value = dEnd.toISOString().split('T')[0];
        
        document.getElementById('wrapperItemBiaya<%=rnd%>').style.display = 'none';
        document.getElementById('containerItemBiaya<%=rnd%>').innerHTML = '';
        
        window.loadDataRiwayat<%=rnd%>(1);
    };

    window.bukaModalExport<%=rnd%> = function(type) {
        const modalId = 'modalExport<%=rnd%>';
        let existingModal = document.getElementById(modalId);
        
        // Bersihkan modal lama jika ada agar tidak duplikat di memori
        if (existingModal) {
            existingModal.remove();
            document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
        }

        let titleIcon = type === 'pdf' ? '<i class="fas fa-print me-2"></i>' : '<i class="fas fa-file-excel me-2"></i>';
        let titleText = type === 'pdf' ? '<%= Common.getBahasaConfigJS("Pencetakan Laporan Format PDF") %>' : '<%= Common.getBahasaConfigJS("Ekspor Laporan Format Excel") %>';

        // Merakit data filter pencarian dari form utama ke form modal ekspor
        let hiddenInputsHtml = '';
        const mainForm = document.getElementById('formFilterRiwayat<%=rnd%>');
        const mainFormData = new FormData(mainForm);
        for(let [key, val] of mainFormData.entries()) {
            // Jangan duplikasi parameter navigasi dan parameter form bawaan
            if(val && key !== 'startRow' && key !== 'endRow' && key !== 'exportType' && key !== 'p' && key !== 's') {
                hiddenInputsHtml += '<input type="hidden" name="' + key + '" value="' + val + '">';
            }
        }

        const modalHtml = `
            <div class="modal fade" id="\${modalId}" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg rounded-4">
                        <form action="<%=serviceUrl%>" method="POST" target="_blank">
                            <input type="hidden" name="action" value="EXPORT_DATA">
                            <input type="hidden" name="exportType" value="\${type}">
                            \${hiddenInputsHtml}
                            <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">
                                <h5 class="modal-title fw-bold">\${titleIcon} \${titleText}</h5>
                                <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body p-4 bg-light text-center">
                                <p class="text-muted fw-bold mb-4">
                                    <%= Common.getBahasaConfig("Silakan tentukan batasan baris data yang akan ditarik agar proses berjalan optimal.") %> 
                                </p>
                                <div class="row g-3 justify-content-center align-items-center">
                                    <div class="col-5">
                                        <label class="form-label fw-bold text-secondary"><%= Common.getBahasaConfig("Mulai Baris") %></label>
                                        <input type="number" class="form-control text-center fw-bold shadow-sm" name="startRow" value="1" min="1">
                                    </div>
                                    <div class="col-2 pt-4"><i class="fas fa-arrow-right text-secondary opacity-50"></i></div>
                                    <div class="col-5">
                                        <label class="form-label fw-bold text-secondary"><%= Common.getBahasaConfig("Sampai Baris") %></label>
                                        <input type="number" class="form-control text-center fw-bold shadow-sm" name="endRow" value="1000" min="1">
                                    </div>
                                </div>
                            </div>
                            <div class="modal-footer border-0 bg-white d-flex justify-content-between py-3">
                                <button type="button" class="btn btn-light border rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal">
                                    <%= Common.getBahasaConfig("Batal") %>
                                </button>
                                <button type="submit" class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" onclick="setTimeout(function(){ bootstrap.Modal.getInstance(document.getElementById('\${modalId}')).hide(); }, 800);">
                                    <i class="fas fa-cloud-download-alt me-2"></i> <%= Common.getBahasaConfig("Eksekusi Laporan") %>
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>`;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        new bootstrap.Modal(document.getElementById(modalId)).show();
    };

    setTimeout(function() {
        const initJkId = document.getElementById('fjkId<%=rnd%>');
        if (initJkId && initJkId.value) {
            window.loadItemBiaya<%=rnd%>(initJkId.value);
        } else {
            window.loadDataRiwayat<%=rnd%>(1);
        }
    }, 150);
</script>