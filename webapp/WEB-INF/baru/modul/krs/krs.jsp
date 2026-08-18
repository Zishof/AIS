<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<% 
    String rnd = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8); 
    
    ais.database.model.Tbmuser tbmuser = ais.common.Common.getCurrentUser(request);
    Long mhsId = (tbmuser != null && tbmuser.getMahasiswa() != null) ? tbmuser.getMahasiswa().getId() : 0L;
    String pLaporan = "laporan%2Fgeneral_mahasiswa"; 
    try { pLaporan = java.net.URLEncoder.encode("laporan/general_mahasiswa", "UTF-8"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/krs/krs.jsp:9");}
    String baseReportUrl = ais.common.Common.ROOT + "/baru?hanya_tampil_jsp=true&p=" + pLaporan + "&s=index&mahasiswa=" + mhsId;
%>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">

<style>
    #wrapKrs_<%=rnd%> .bg-soft-primary { background-color: #f0f4ff !important; }
    #wrapKrs_<%=rnd%> .hover-elevate { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    #wrapKrs_<%=rnd%> .hover-elevate:hover { transform: translateY(-3px); box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important; }
    .table-krs th { background-color: #f8f9fa; text-transform: uppercase; font-size: 0.85rem; letter-spacing: 0.5px; }
    .table-krs td { vertical-align: middle; font-size: 0.95rem; }
    .check-krs { width: 1.5rem; height: 1.5rem; cursor: pointer; }
    .btn-hapus-krs { width: 35px; height: 35px; display: inline-flex; justify-content: center; align-items: center; }
</style>

<div class="container-fluid py-4" id="wrapKrs_<%=rnd%>">
    
    <!-- Banner Informasi -->
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5 border-primary">
        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px; flex-shrink: 0;">
            <i class="fas fa-book-open fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Pengambilan Kartu Rencana Studi (KRS)") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Modul untuk melakukan pengambilan, perbaikan, serta pemantauan data perkuliahan mahasiswa pada semester berjalan.") %></p>
        </div>
    </div>

    <!-- Papan Status Pengambilan -->
    <div class="row mb-4">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4 hover-elevate">
                <div class="card-body p-4 d-flex justify-content-between align-items-center bg-soft-primary rounded-4 flex-wrap gap-3">
                    <div class="pe-3">
                        <h5 class="fw-bold mb-1 text-primary">
                            <i class="fas fa-info-circle me-2"></i> <%= Common.getBahasaConfig("Status Verifikasi Akademik") %>
                        </h5>
                        <p class="text-secondary mb-0 small"><%= Common.getBahasaConfig("Klik tombol di samping untuk memvalidasi kelayakan akademik dan keuangan Anda sebelum memilih daftar mata kuliah.") %></p>
                    </div>
                    <button id="btnValidasiKrs_<%=rnd%>" class="btn btn-primary px-4 py-3 fw-bold rounded-pill shadow-sm text-nowrap d-flex align-items-center" style="display:none;">
                        <i class="fas fa-edit fs-5 me-2"></i> <span><%= Common.getBahasaConfig("Mulai Ambil / Perbaiki KRS") %></span>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Tabel KRS Yang Telah Diambil -->
    <div class="row">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4">
                <div class="card-header pt-4 pb-3 border-bottom d-flex flex-column flex-md-row justify-content-between align-items-center rounded-top-4" style="background-color: transparent;">
                    <h5 class="fw-bold mb-3 mb-md-0 text-dark d-flex align-items-center">
                        <i class="fas fa-check-square text-primary me-2"></i> <%= Common.getBahasaConfig("Daftar KRS Anda Saat Ini") %>
                    </h5>
                    
                    <div class="d-flex align-items-center w-100 w-md-auto ms-auto" style="max-width: 550px;">
                        <span class="fw-bold text-white me-2 text-nowrap small"><i class="fas fa-filter me-1"></i> <%= Common.getBahasaConfig("Periode:") %></span>
                        <select id="filterSmtKrs_<%=rnd%>" class="form-select form-select-sm rounded-pill shadow-sm">
                            <option value=""><%= Common.getBahasaConfig("Semua Semester & Tahun Ajaran") %></option>
                        </select>
                        <button id="btnRefreshKrs_<%=rnd%>" class="btn btn-sm btn-light border ms-2 rounded-circle shadow-sm" title="<%= Common.getBahasaConfig("Muat Ulang") %>">
                            <i class="fas fa-sync-alt text-primary"></i>
                        </button>
                        
                        <button class="btn btn-primary btn-sm ms-2 rounded-pill shadow-sm text-nowrap" type="button" 
                            onclick="bukaModalLaporan<%=rnd%>('<%=Common.getBahasaConfigJS("Cetak KRS")%>', 'Cetak_KRS_Mahasiswa', 'Kartu+Rencana+Studi')">
                            <i class="fas fa-print me-1"></i> <%=Common.getBahasaConfig("Cetak KRS") %>
                        </button>
                    </div>
                </div>

                <div class="card-body p-4 bg-white table-responsive rounded-bottom-4">
                    <table id="tabelKrsDiambil_<%=rnd%>" class="table table-hover table-striped table-bordered align-middle w-100 mb-0">
                        <thead class="table-light">
                            <tr>
                                <th width="30%"><%= Common.getBahasaConfig("Mata Kuliah") %></th>
                                <th width="5%" class="text-center"><%= Common.getBahasaConfig("SKS") %></th>
                                <th width="20%"><%= Common.getBahasaConfig("Dosen") %></th>
                                <th width="20%"><%= Common.getBahasaConfig("Jadwal & Ruang") %></th>
                                <th width="10%" class="text-center"><%= Common.getBahasaConfig("Smt / Kelas") %></th>
                                <th width="10%" class="text-center"><%= Common.getBahasaConfig("Persetujuan") %></th>
                                <th width="5%" class="text-center"><i class="fas fa-cog"></i></th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- MODAL POPUP: DAFTAR MATAKULIAH (FILTER CERDAS) -->
<div class="modal fade" id="modalPerkuliahan_<%=rnd%>" data-bs-backdrop="static" tabindex="-1" aria-hidden="true" style="z-index: 9999;">
    <div class="modal-dialog modal-xl modal-dialog-scrollable">
        <div class="modal-content border-0 shadow-lg rounded-4">
            <div class="modal-header bg-primary text-white py-3 border-bottom-0">
                <h5 class="mb-0 fw-bold d-flex align-items-center">
                    <i class="fas fa-list-ul me-3"></i> <%= Common.getBahasaConfig("Pemilihan Matakuliah Tersedia") %>
                </h5>
                <button type="button" class="btn-close btn-close-white" onclick="$('#modalPerkuliahan_<%=rnd%>').modal('hide');" aria-label="<%= Common.getBahasaConfig("Tutup") %>"></button>
            </div>
            
            <div class="modal-body p-4 bg-light">
                <div class="row g-2 mb-3">
                    <div class="col-md-3">
                        <label class="form-label fw-bold text-secondary small mb-1"><%= Common.getBahasaConfig("Fakultas") %></label>
                        <select id="advFilterFak_<%=rnd%>" class="form-select form-select-sm rounded-pill shadow-sm"><option value=""><%= Common.getBahasaConfig("Semua Fakultas") %></option></select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label fw-bold text-secondary small mb-1"><%= Common.getBahasaConfig("Program Studi") %></label>
                        <select id="advFilterJur_<%=rnd%>" class="form-select form-select-sm rounded-pill shadow-sm"><option value=""><%= Common.getBahasaConfig("Semua Program Studi") %></option></select>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label fw-bold text-secondary small mb-1"><%= Common.getBahasaConfig("Program") %></label>
                        <select id="advFilterProg_<%=rnd%>" class="form-select form-select-sm rounded-pill shadow-sm"><option value=""><%= Common.getBahasaConfig("Semua") %></option></select>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label fw-bold text-secondary small mb-1"><%= Common.getBahasaConfig("Kelas") %></label>
                        <input type="text" id="advFilterKls_<%=rnd%>" class="form-control form-control-sm rounded-pill shadow-sm" placeholder="<%= Common.getBahasaConfig("Ketik...") %>">
                    </div>
                    <div class="col-md-2">
                        <label class="form-label fw-bold text-secondary small mb-1"><%= Common.getBahasaConfig("Kode / Nama MK") %></label>
                        <input type="text" id="advFilterMk_<%=rnd%>" class="form-control form-control-sm rounded-pill shadow-sm" placeholder="<%= Common.getBahasaConfig("Cari MK...") %>">
                    </div>
                </div>

                <div class="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
                    <div class="badge bg-white text-secondary shadow-sm px-3 py-2 rounded-pill border fs-6 d-flex align-items-center">
                        <i class="far fa-calendar-alt text-primary me-2"></i> 
                        <span id="lblPeriodeAktif_<%=rnd%>"><%= Common.getBahasaConfig("Memuat Data...") %></span>
                    </div>
                    
                    <div class="badge bg-white text-dark shadow-sm px-3 py-2 rounded-pill border border-warning fs-6">
                        <i class="fas fa-layer-group text-warning me-2"></i> <%= Common.getBahasaConfig("Batas Maksimal") %> : <span id="lblMaxSks_<%=rnd%>" class="fw-bold text-primary">0</span> SKS
                        <span id="lblWarningSks_<%=rnd%>" class="text-danger ms-2 d-none"><i class="fas fa-exclamation-circle"></i> <%= Common.getBahasaConfig("SKS Berlebih!") %></span>
                    </div>
                </div>

                <div class="card shadow-sm border-0 rounded-4 mt-2">
                    <div class="card-body p-0 table-responsive">
                        <table id="tabelListMk_<%=rnd%>" class="table table-hover table-krs w-100 mb-0">
                            <thead>
                                <tr>
                                    <th width="5%" class="text-center"><i class="far fa-check-square fs-5 text-primary"></i></th>
                                    <th width="25%"><%= Common.getBahasaConfig("Mata Kuliah") %></th>
                                    <th width="5%" class="text-center"><%= Common.getBahasaConfig("SKS") %></th>
                                    <th width="20%"><%= Common.getBahasaConfig("Dosen") %></th>
                                    <th width="20%"><%= Common.getBahasaConfig("Jadwal & Ruang") %></th>
                                    <th width="5%" class="text-center"><%= Common.getBahasaConfig("Kelas") %></th>
                                    <th width="5%" class="text-center"><%= Common.getBahasaConfig("Kap.") %></th>
                                    <th width="15%" class="text-center"><%= Common.getBahasaConfig("Status") %></th>
                                </tr>
                            </thead>
                            <tbody></tbody>
                        </table>
                    </div>
                </div>
            </div>
            
            <div class="modal-footer bg-white border-top py-3 justify-content-between">
                <button type="button" class="btn btn-light rounded-pill border shadow-sm px-4 fw-bold text-secondary" onclick="$('#modalPerkuliahan_<%=rnd%>').modal('hide');">
                    <i class="fas fa-times me-2"></i> <%= Common.getBahasaConfig("Batal") %>
                </button>
                <button type="button" id="btnSimpanKrs_<%=rnd%>" class="btn btn-success rounded-pill px-5 py-2 fw-bold shadow-sm hover-elevate">
                    <i class="fas fa-save me-2"></i> <%= Common.getBahasaConfig("Simpan KRS Pilihan") %>
                </button>
            </div>
        </div>
    </div>
</div>

<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script>
    // PERBAIKAN: Penamaan fungsi menjadi unik di Global Scope
    window.bukaModalLaporan<%=rnd%> = function(labelTombol, namaFile, judulLaporan) {
        if (typeof window.variable === 'undefined') window.variable = 0; 
        var cm = 'contentModal_' + (++window.variable);
        
        var baseUrl = '<%=baseReportUrl%>'; 
        var $selectedOpt = $('#filterSmtKrs_<%=rnd%>').find('option:selected');
        var selectedPeriode = $selectedOpt.val(); 
        
        var smt = $selectedOpt.attr('data-smt');
        var sp = $selectedOpt.attr('data-sp');
        
        var fullUrl = baseUrl + '&fileLaporan=' + namaFile + '&judulLaporan=' + judulLaporan + '&contentModal=' + cm;
        
        if (selectedPeriode) fullUrl += '&periode=' + selectedPeriode;
        if (smt) fullUrl += '&smt=' + smt;
        if (sp) fullUrl += '&sp=' + sp;

        if (typeof loadModalContentCustomSimpan === "function") {
            loadModalContentCustomSimpan(labelTombol, fullUrl, '85%', null, '', cm);
        } else {
            console.error("Fungsi loadModalContentCustomSimpan tidak ditemukan.");
        }
    };

    var cekInterval = setInterval(function() {
        if (typeof $.fn.DataTable !== 'undefined') {
            clearInterval(cekInterval);
            initKRSApp_<%=rnd%>();
        } else {
            $.getScript("https://cdn.datatables.net/1.10.24/js/jquery.dataTables.min.js", function() {
                $.getScript("https://cdn.datatables.net/1.10.24/js/dataTables.bootstrap4.min.js");
            });
        }
    }, 100);

    function initKRSApp_<%=rnd%>() {
        var urlServiceKrs = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=krs&s=_krs_service';
        var dtTabelKrs = null, dtTabelPilih = null, allJurusanData = [];
        var isKrsRefresh = "false"; 
        
        var gMaxSks = 0;
        var gSksTerambil = 0;
        var gSksDraft = 0; 

        $('#modalPerkuliahan_<%=rnd%>').on('hide.bs.modal', function(e) {
            e.stopPropagation(); 
        });

        $('#modalPerkuliahan_<%=rnd%>').on('hidden.bs.modal', function (e) {
            e.stopPropagation(); 
            
            if ($('.modal.show').length > 0) {
                $('body').addClass('modal-open');
            }
        });

        $('#btnValidasiKrs_<%=rnd%>').fadeIn();
        loadFilterSmtDropdown();

        function loadFilterSmtDropdown() {
            $.ajax({
                url: urlServiceKrs + "&action=get_filter_smt", type: "GET", dataType: "json",
                success: function(res) {
                    var $sel = $('#filterSmtKrs_<%=rnd%>');
                    $sel.empty().append(new Option('<%= Common.getBahasaConfigJS("Semua Semester & Tahun Ajaran") %>', ''));
                    if (res && res.data) {
                        $.each(res.data, function(idx, item) { 
                            var $opt = $('<option></option>').val(item.idSmt).text(item.label);
                            if (item.smt) $opt.attr('data-smt', item.smt);
                            if (item.sp) $opt.attr('data-sp', item.sp);
                            $sel.append($opt); 
                        });
                        if (res.default_id && $sel.find("option[value='" + res.default_id + "']").length > 0) $sel.val(res.default_id);
                    }
                    loadTabelKrsDiambil();
                }
            });
        }

        $('#filterSmtKrs_<%=rnd%>').on('change', function() {
            isKrsRefresh = "false";
            if (dtTabelKrs) dtTabelKrs.ajax.reload(null, false);
        });

        $('#btnRefreshKrs_<%=rnd%>').on('click', function() {
            isKrsRefresh = "true";
            if (dtTabelKrs) dtTabelKrs.ajax.reload(function() { isKrsRefresh = "false"; }, false);
        });

        function loadTabelKrsDiambil() {
            if (dtTabelKrs !== null) { dtTabelKrs.destroy(); }
            dtTabelKrs = $('#tabelKrsDiambil_<%=rnd%>').DataTable({
                "ajax": {
                    "url": urlServiceKrs + "&action=get_krs_diambil", "type": "POST",
                    "data": function(d) { 
                        d.filter_id_smt = $('#filterSmtKrs_<%=rnd%>').val(); 
                        d.refresh = isKrsRefresh;
                    }
                },
                "columns": [
                    { "data": "matakuliah" },
                    { "data": "sks", "className": "text-center fw-bold text-primary" },
                    { "data": "dosen", "className": "small text-muted" },
                    { "data": "jadwal", "className": "small text-secondary" },
                    { "data": "smt_kelas", "className": "text-center small" },
                    { "data": "status_acc", "className": "text-center" },
                    { "data": null, "className": "text-center", "render": function(data, type, row) {
                        if (row.is_belum) {
                            return '<button class="btn btn-outline-danger btn-hapus-krs shadow-sm rounded-circle" data-id="'+row.id+'" title="<%= Common.getBahasaConfig("Hapus Matakuliah") %>"><i class="fas fa-trash"></i></button>';
                        }
                        return '-';
                    }}
                ],
                "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
                "pageLength": 20, "ordering": false
            });
            
            dtTabelKrs.on('xhr.dt', function ( e, settings, json, xhr ) {
                if(json && json.debug_info) console.log("SERVER DEBUG: ", json.debug_info);
            });
        }

        $('#tabelKrsDiambil_<%=rnd%>').on('click', '.btn-hapus-krs', function() {
            var idDetail = $(this).data('id');
            
            if (typeof prosesDeleteData === "function") {
                prosesDeleteData('Detailperkuliahan', idDetail, function() {
                    isKrsRefresh = "true";
                    dtTabelKrs.ajax.reload(function() { isKrsRefresh = "false"; }, false);
                });
            } else {
                console.error("Fungsi global prosesDeleteData tidak ditemukan.");
            }
        });

        function loadMasterFiltersModal() {
            $.ajax({
                url: urlServiceKrs + "&action=get_master_filters", type: "GET", dataType: "json",
                success: function(res) {
                    var $fak = $('#advFilterFak_<%=rnd%>'); var $jur = $('#advFilterJur_<%=rnd%>'); var $prog = $('#advFilterProg_<%=rnd%>');
                    
                    if(res.fakultas) $.each(res.fakultas, function(i, v){ $fak.append(new Option(v.nama, v.id)); });
                    if(res.program) $.each(res.program, function(i, v){ $prog.append(new Option(v.nama, v.id)); });
                    if(res.jurusan) allJurusanData = res.jurusan;

                    if (res.def_fakultas) $fak.val(res.def_fakultas).trigger('change');
                    if (res.def_program) $prog.val(res.def_program);
                    setTimeout(function(){ if(res.def_jurusan) $jur.val(res.def_jurusan).trigger('change'); }, 100);
                }
            });
        }

        $('#advFilterFak_<%=rnd%>').on('change', function() {
            var fid = $(this).val();
            var $jur = $('#advFilterJur_<%=rnd%>');
            $jur.empty().append(new Option('<%= Common.getBahasaConfigJS("Semua Program Studi") %>', ''));
            $.each(allJurusanData, function(i, v) {
                if(!fid || v.fakultas_id == fid) $jur.append(new Option(v.nama, v.id));
            });
            if(dtTabelPilih) dtTabelPilih.ajax.reload(null, false);
        });

        $('#btnValidasiKrs_<%=rnd%>').on('click', function(e) {
            e.preventDefault(); var $btn = $(this); var oriHtml = $btn.html();
            $btn.html('<i class="fas fa-spinner fa-spin fs-5 me-2"></i> <span><%= Common.getBahasaConfig("Mengecek Syarat...") %></span>').prop('disabled', true);

            $.ajax({
                url: urlServiceKrs + "&action=validasi_ambil_krs", type: "POST", dataType: "json", 
                data: { filter_id_smt: $('#filterSmtKrs_<%=rnd%>').val(), tahapan: 1 }, 
                success: function(response) {
                    $btn.html(oriHtml).prop('disabled', false);
                    
                    if (response.status === "error") {
                        Swal.fire({ icon: 'error', title: '<%= Common.getBahasaConfigJS("Tidak Memenuhi Syarat") %>', text: response.message });
                    } else if (response.status === "success") {
                        
                        gMaxSks = response.max_sks || 0;
                        gSksTerambil = response.sks_terambil || 0;
                        gSksDraft = 0; 
                        
                        $('#lblMaxSks_<%=rnd%>').text(gMaxSks);
                        
                        var labelTa = $('#filterSmtKrs_<%=rnd%> option:selected').text();
                        if (!labelTa || labelTa.includes("Semua Semester")) {
                            labelTa = "<%= Common.getBahasaConfig("TA Berjalan / Aktif") %>";
                        }
                        $('#lblPeriodeAktif_<%=rnd%>').text('<%= Common.getBahasaConfigJS("Periode:") %> ' + labelTa);

                        if(allJurusanData.length === 0) loadMasterFiltersModal(); 
                        
                        var myModal = new bootstrap.Modal($('#modalPerkuliahan_<%=rnd%>')[0]); myModal.show();
                        loadTabelPilihMatakuliah();
                    }
                },
                error: function() { $btn.html(oriHtml).prop('disabled', false); Swal.fire('Error', '<%= Common.getBahasaConfigJS("Gagal menghubungi server.") %>', 'error'); }
            });
        });

        function evaluasiLimitSks() {
            var totalSksPilihan = gSksTerambil + gSksDraft;
            if(totalSksPilihan > gMaxSks) {
                $('#lblWarningSks_<%=rnd%>').removeClass('d-none');
                $('#btnSimpanKrs_<%=rnd%>').prop('disabled', true);
            } else {
                $('#lblWarningSks_<%=rnd%>').addClass('d-none');
                $('#btnSimpanKrs_<%=rnd%>').prop('disabled', false);
            }
        }

        $('#tabelListMk_<%=rnd%>').on('change', '.chk-pilih-<%=rnd%>', function() {
            var inputSksRaw = parseInt($(this).data('sks'));
            if($(this).is(':checked')) {
                gSksDraft += inputSksRaw;
            } else {
                gSksDraft -= inputSksRaw;
            }
            evaluasiLimitSks();
        });

        function loadTabelPilihMatakuliah() {
            if (dtTabelPilih !== null) { dtTabelPilih.destroy(); }
            dtTabelPilih = $('#tabelListMk_<%=rnd%>').DataTable({
                "ajax": {
                    "url": urlServiceKrs + "&action=get_perkuliahan", "type": "POST",
                    "data": function(d) {
                        d.filter_id_smt = $('#filterSmtKrs_<%=rnd%>').val(); 
                        d.tahapan = 1;
                        d.fak_id = $('#advFilterFak_<%=rnd%>').val();
                        d.jur_id = $('#advFilterJur_<%=rnd%>').val();
                        d.prog = $('#advFilterProg_<%=rnd%>').val();
                        d.kls = $('#advFilterKls_<%=rnd%>').val();
                        d.q_mk = $('#advFilterMk_<%=rnd%>').val();
                    }
                },
                "columns": [
                    { "data": null, "className": "text-center", "render": function(data, type, row) {
                        if (!row.show_checkbox) return '';
                        return '<input type="checkbox" class="form-check-input check-krs border-secondary chk-pilih-<%=rnd%>" value="' + row.id + '" data-sks="' + row.sks_raw + '" ' + (row.checked ? "checked" : "") + ' ' + (row.disabled ? "disabled" : "") + '>';
                    }},
                    { "data": "matakuliah", "className": "fw-bold text-dark" },
                    { "data": "sks", "className": "text-center fw-bold text-primary" },
                    { "data": "dosen", "className": "small text-muted" },
                    { "data": "jadwal", "className": "small text-secondary" },
                    { "data": "kelas", "className": "text-center" },
                    { "data": "kapasitas", "className": "text-center small" },
                    { "data": "status_html", "className": "text-center" }
                ],
                "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
                "pageLength": 10, "ordering": false,
                "dom": '<"top">rt<"bottom d-flex justify-content-between p-3"ip><"clear">',
                "drawCallback": function( settings ) {
                    evaluasiLimitSks();
                }
            });
        }

        var searchTimeout;
        $('#advFilterJur_<%=rnd%>, #advFilterProg_<%=rnd%>').on('change', function() { if(dtTabelPilih) dtTabelPilih.ajax.reload(null, false); });
        $('#advFilterKls_<%=rnd%>, #advFilterMk_<%=rnd%>').on('keyup', function() {
            clearTimeout(searchTimeout); searchTimeout = setTimeout(function() { if(dtTabelPilih) dtTabelPilih.ajax.reload(null, false); }, 500);
        });

        $('#btnSimpanKrs_<%=rnd%>').on('click', function() {
            var selectedIds = [];
            $('.chk-pilih-<%=rnd%>:checked:not(:disabled)').each(function() { selectedIds.push($(this).val()); });

            if (selectedIds.length === 0) {
                Swal.fire({ icon: 'warning', title: '<%= Common.getBahasaConfigJS("Peringatan") %>', text: '<%= Common.getBahasaConfigJS("Anda belum memilih matakuliah baru apapun.") %>' }); return;
            }

            var $btn = $(this); var oriSimpan = $btn.html();
            $btn.html('<i class="fas fa-spinner fa-spin me-2"></i> <%= Common.getBahasaConfig("Menyimpan...") %>').prop('disabled', true);

            $.ajax({
                url: urlServiceKrs + "&action=simpan_krs", type: "POST", dataType: "json", 
                data: { filter_id_smt: $('#filterSmtKrs_<%=rnd%>').val(), tahapan: 1, perkuliahan_ids: selectedIds },
                success: function(response) {
                    $btn.html(oriSimpan).prop('disabled', false);
                    
                    if (response.status === "success") {
                        Swal.fire({ icon: 'success', title: '<%= Common.getBahasaConfigJS("Berhasil") %>', text: response.message, timer: 2000, showConfirmButton: false });
                        $('#modalPerkuliahan_<%=rnd%>').modal('hide');
                        isKrsRefresh = "true";
                        if (dtTabelKrs) dtTabelKrs.ajax.reload(function() { isKrsRefresh = "false"; }, false); 
                    } else if (response.status === "warning") {
                        Swal.fire({ icon: 'warning', title: '<%= Common.getBahasaConfigJS("Disimpan dengan Peringatan") %>', text: response.message });
                        isKrsRefresh = "true";
                        if (dtTabelKrs) dtTabelKrs.ajax.reload(function() { isKrsRefresh = "false"; }, false); 
                        if(dtTabelPilih) dtTabelPilih.ajax.reload(null, false);
                    } else {
                        Swal.fire('Error', response.message, 'error');
                    }
                },
                error: function() { $btn.html(oriSimpan).prop('disabled', false); Swal.fire('Error', '<%= Common.getBahasaConfigJS("Gagal menghubungi server untuk penyimpanan.") %>', 'error'); }
            });
        });
    }
</script>