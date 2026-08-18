<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<% 
    String rnd = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8); 
    ais.database.model.Tbmuser tbmuser = ais.common.Common.getCurrentUser(request);
    Long mhsId = (tbmuser != null && tbmuser.getMahasiswa() != null) ? tbmuser.getMahasiswa().getId() : 0L;
    String pLaporan = "laporan%2Fgeneral_mahasiswa"; 
    try { pLaporan = java.net.URLEncoder.encode("laporan/general_mahasiswa", "UTF-8"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/krs/krs_paket.jsp:8");}
    String baseReportUrl = ais.common.Common.ROOT + "/baru?hanya_tampil_jsp=true&p=" + pLaporan + "&s=index&mahasiswa=" + mhsId;
%>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">

<style>
    #wrapKrsPaket_<%=rnd%> .bg-soft-success { background-color: #e6f9f0 !important; }
    #wrapKrsPaket_<%=rnd%> .hover-elevate { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    #wrapKrsPaket_<%=rnd%> .hover-elevate:hover { transform: translateY(-3px); box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important; }
    .table-krs th { background-color: #f8f9fa; text-transform: uppercase; font-size: 0.85rem; letter-spacing: 0.5px; }
    .table-krs td { vertical-align: middle; font-size: 0.95rem; }
    .btn-hapus-krs { width: 35px; height: 35px; display: inline-flex; justify-content: center; align-items: center; }
</style>

<div class="container-fluid py-4" id="wrapKrsPaket_<%=rnd%>">
    
    <!-- Banner Informasi -->
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5 border-success">
        <div class="bg-success text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px; flex-shrink: 0;">
            <i class="fas fa-boxes fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Pengambilan KRS (Paket Semester)") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Modul pengambilan Kartu Rencana Studi secara otomatis berdasarkan kurikulum paket yang telah disediakan oleh program studi.") %></p>
        </div>
    </div>

    <!-- Papan Status Pengambilan -->
    <div class="row mb-4">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4 hover-elevate">
                <div class="card-body p-4 d-flex justify-content-between align-items-center bg-soft-success rounded-4 flex-wrap gap-3">
                    <div class="pe-3">
                        <h5 class="fw-bold mb-1 text-success">
                            <i class="fas fa-box-open me-2"></i> <%= Common.getBahasaConfig("Ambil Paket Perkuliahan") %>
                        </h5>
                        <p class="text-secondary mb-0 small"><%= Common.getBahasaConfig("Klik tombol di samping untuk mengambil seluruh matakuliah sesuai dengan paket semester Anda.") %></p>
                    </div>
                    <button id="btnValidasiKrsPaket_<%=rnd%>" class="btn btn-success px-4 py-3 fw-bold rounded-pill shadow-sm text-nowrap d-flex align-items-center" style="display:none;">
                        <i class="fas fa-download fs-5 me-2"></i> <span><%= Common.getBahasaConfig("Ambil Paket KRS") %></span>
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
                        <i class="fas fa-check-square text-success me-2"></i> <%= Common.getBahasaConfig("Daftar KRS Anda Saat Ini") %>
                    </h5>
                    
                    <div class="d-flex align-items-center w-100 w-md-auto ms-auto" style="max-width: 550px;">
                        <span class="fw-bold text-dark me-2 text-nowrap small"><i class="fas fa-filter me-1"></i> <%= Common.getBahasaConfig("Periode:") %></span>
                        <select id="filterSmtKrsPaket_<%=rnd%>" class="form-select form-select-sm rounded-pill shadow-sm border-success">
                            <option value=""><%= Common.getBahasaConfig("Semua Semester & Tahun Ajaran") %></option>
                        </select>
                        <button id="btnRefreshKrsPaket_<%=rnd%>" class="btn btn-sm btn-light border ms-2 rounded-circle shadow-sm" title="<%= Common.getBahasaConfig("Muat Ulang") %>">
                            <i class="fas fa-sync-alt text-success"></i>
                        </button>
                        
                        <button class="btn btn-success btn-sm ms-2 rounded-pill shadow-sm text-nowrap" type="button" 
                            onclick="bukaModalLaporanPaket_<%=rnd%>('<%=Common.getBahasaConfigJS("Cetak KRS")%>', 'Cetak_KRS_Mahasiswa', 'Kartu+Rencana+Studi')">
                            <i class="fas fa-print me-1"></i> <%=Common.getBahasaConfig("Cetak KRS") %>
                        </button>
                    </div>
                </div>

                <div class="card-body p-4 bg-white table-responsive rounded-bottom-4">
                    <table id="tabelKrsDiambilPaket_<%=rnd%>" class="table table-hover table-striped table-bordered align-middle w-100 mb-0">
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

<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<script>

    function bukaModalLaporanPaket_<%=rnd%>(labelTombol, namaFile, judulLaporan) {
        if (typeof window.variable === 'undefined') window.variable = 0; 
        var cm = 'contentModal_' + (++window.variable);
        var baseUrl = '<%=baseReportUrl%>'; 
        var $selectedOpt = $('#filterSmtKrsPaket_<%=rnd%>').find('option:selected');
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
    }

    var cekIntervalPaket = setInterval(function() {
        if (typeof $.fn.DataTable !== 'undefined') {
            clearInterval(cekIntervalPaket);
            initKRSPaketApp_<%=rnd%>();
        } else {
            $.getScript("https://cdn.datatables.net/1.10.24/js/jquery.dataTables.min.js", function() {
                $.getScript("https://cdn.datatables.net/1.10.24/js/dataTables.bootstrap4.min.js");
            });
        }
    }, 100);

    function initKRSPaketApp_<%=rnd%>() {
        var urlServiceKrs = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=krs&s=_krs_paket_service';
        var dtTabelKrs = null, dtTabelPilih = null;
        var isKrsRefresh = "false"; 
        
        var gMaxSks = 0; var gSksTerambil = 0; var gSksPaket = 0; 

        $('#btnValidasiKrsPaket_<%=rnd%>').fadeIn();
        loadFilterSmtDropdown();

        function loadFilterSmtDropdown() {
            $.ajax({
                url: urlServiceKrs + "&action=get_filter_smt", type: "GET", dataType: "json",
                success: function(res) {
                    var $sel = $('#filterSmtKrsPaket_<%=rnd%>');
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

        $('#filterSmtKrsPaket_<%=rnd%>').on('change', function() {
            isKrsRefresh = "false";
            if (dtTabelKrs) dtTabelKrs.ajax.reload(null, false);
        });

        $('#btnRefreshKrsPaket_<%=rnd%>').on('click', function() {
            isKrsRefresh = "true";
            if (dtTabelKrs) dtTabelKrs.ajax.reload(function() { isKrsRefresh = "false"; }, false);
        });

        function loadTabelKrsDiambil() {
            if (dtTabelKrs !== null) { dtTabelKrs.destroy(); }
            dtTabelKrs = $('#tabelKrsDiambilPaket_<%=rnd%>').DataTable({
                "ajax": {
                    "url": urlServiceKrs + "&action=get_krs_diambil", "type": "POST",
                    "data": function(d) { 
                        d.filter_id_smt = $('#filterSmtKrsPaket_<%=rnd%>').val(); 
                        d.refresh = isKrsRefresh;
                    }
                },
                "columns": [
                    { "data": "matakuliah" },
                    { "data": "sks", "className": "text-center fw-bold text-success" },
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
        }

        $('#tabelKrsDiambilPaket_<%=rnd%>').on('click', '.btn-hapus-krs', function() {
            var idDetail = $(this).data('id');
            if (typeof prosesDeleteData === "function") {
                prosesDeleteData('Detailperkuliahan', idDetail, function() {
                    isKrsRefresh = "true";
                    dtTabelKrs.ajax.reload(function() { isKrsRefresh = "false"; }, false);
                });
            } else { console.error("Fungsi global prosesDeleteData tidak ditemukan."); }
        });

        $('#btnValidasiKrsPaket_<%=rnd%>').on('click', function(e) {
            e.preventDefault(); 
            var $btn = $(this); var oriHtml = $btn.html();
            var smtVal = $('#filterSmtKrsPaket_<%=rnd%>').val();
            
            if (!smtVal || smtVal.trim() === "") {
                Swal.fire({ icon: 'warning', title: '<%= Common.getBahasaConfigJS("Perhatian") %>', text: '<%= Common.getBahasaConfigJS("Silakan pilih Periode Semester terlebih dahulu.") %>' });
                return;
            }

            $btn.html('<i class="fas fa-spinner fa-spin fs-5 me-2"></i> <span><%= Common.getBahasaConfig("Mengecek Syarat...") %></span>').prop('disabled', true);

            $.ajax({
                url: urlServiceKrs + "&action=validasi_ambil_krs", type: "POST", dataType: "json", 
                data: { filter_id_smt: smtVal, tahapan: 1 }, 
                success: function(response) {
                    $btn.html(oriHtml).prop('disabled', false);
                    if (response.status === "error") {
                        Swal.fire({ icon: 'error', title: '<%= Common.getBahasaConfigJS("Tidak Memenuhi Syarat") %>', text: response.message });
                    } else if (response.status === "success") {
                        gMaxSks = response.max_sks || 0;
                        gSksTerambil = response.sks_terambil || 0;
                        bukaModalPaketLayar();
                    }
                },
                error: function() { $btn.html(oriHtml).prop('disabled', false); Swal.fire('Error', '<%= Common.getBahasaConfigJS("Gagal menghubungi server.") %>', 'error'); }
            });
        });

        // INJEKSI MODAL PAKET
        function bukaModalPaketLayar() {
            var modalId = 'modalKrsPaketFull_<%=rnd%>';
            
            if ($('#' + modalId).length === 0) {
                var labelTa = $('#filterSmtKrsPaket_<%=rnd%> option:selected').text();
                var modalHtml = 
                    '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" style="z-index: 9999;">' +
                        '<div class="modal-dialog modal-xl modal-dialog-scrollable">' + 
                            '<div class="modal-content border-0 shadow-lg rounded-4">' +
                                '<div class="modal-header bg-success text-white border-0 py-3">' +
                                    '<h5 class="modal-title fw-bold"><i class="fas fa-boxes me-2"></i><%=Common.getBahasaConfig("Daftar Matakuliah Paket")%></h5>' +
                                    '<button type="button" class="btn-close btn-close-white" onclick="$(\'#' + modalId + '\').modal(\'hide\');" aria-label="Close"></button>' +
                                '</div>' +
                                '<div class="modal-body p-4 bg-light">' +
                                    '<div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">' +
                                        '<div class="badge bg-white text-secondary shadow-sm px-3 py-2 rounded-pill border fs-6 d-flex align-items-center">' +
                                            '<i class="far fa-calendar-alt text-success me-2"></i> <span><%= Common.getBahasaConfig("Periode:") %> ' + labelTa + '</span>' +
                                        '</div>' +
                                        '<div class="badge bg-white text-dark shadow-sm px-3 py-2 rounded-pill border border-warning fs-6">' +
                                            '<i class="fas fa-layer-group text-warning me-2"></i> <%= Common.getBahasaConfig("Batas Maksimal") %> : <span class="fw-bold text-success">' + gMaxSks + '</span> SKS' +
                                            '<span id="lblWarningPaketSks_<%=rnd%>" class="text-danger ms-2 d-none"><i class="fas fa-exclamation-circle"></i> <%= Common.getBahasaConfig("SKS Berlebih!") %></span>' +
                                        '</div>' +
                                    '</div>' +
                                    '<div class="card shadow-sm border-0 rounded-4">' +
                                        '<div class="card-body p-0 table-responsive">' +
                                            '<table id="tabelListPaket_<%=rnd%>" class="table table-hover table-krs w-100 mb-0">' +
                                                '<thead><tr>' +
                                                    '<th width="30%"><%= Common.getBahasaConfig("Mata Kuliah") %></th>' +
                                                    '<th width="10%" class="text-center"><%= Common.getBahasaConfig("SKS") %></th>' +
                                                    '<th width="45%"><%= Common.getBahasaConfig("Dosen & Jadwal") %></th>' +
                                                    '<th width="15%" class="text-center"><%= Common.getBahasaConfig("Kapasitas") %></th>' +
                                                '</tr></thead>' +
                                                '<tbody></tbody>' +
                                            '</table>' +
                                        '</div>' +
                                    '</div>' +
                                '</div>' +
                                '<div class="modal-footer bg-white border-top py-3 justify-content-between">' +
                                    '<button type="button" class="btn btn-light rounded-pill border shadow-sm px-4 fw-bold text-secondary" onclick="$(\'#' + modalId + '\').modal(\'hide\');"><i class="fas fa-times me-2"></i> <%= Common.getBahasaConfig("Batal") %></button>' +
                                    '<button type="button" id="btnSimpanPaketKrs_<%=rnd%>" class="btn btn-success rounded-pill px-5 py-2 fw-bold shadow-sm hover-elevate"><i class="fas fa-download me-2"></i> <%= Common.getBahasaConfig("Ambil Paket Ini") %></button>' +
                                '</div>' +
                            '</div>' +
                        '</div>' +
                    '</div>';

                document.body.insertAdjacentHTML('beforeend', modalHtml);

                $('#' + modalId).on('hide.bs.modal', function(e) { e.stopPropagation(); });
                $('#' + modalId).on('hidden.bs.modal', function (e) {
                    e.stopPropagation();
                    if ($('.modal.show').length > 0) { $('body').addClass('modal-open'); }
                });
                
                $('#btnSimpanPaketKrs_<%=rnd%>').on('click', executeSimpanPaket);
            }

            var myModalKrsPaket = new bootstrap.Modal(document.getElementById(modalId));
            myModalKrsPaket.show();
            loadDataTabelPaket();
        }

        function loadDataTabelPaket() {
            if (dtTabelPilih !== null) { dtTabelPilih.destroy(); }
            dtTabelPilih = $('#tabelListPaket_<%=rnd%>').DataTable({
                "ajax": {
                    "url": urlServiceKrs + "&action=get_perkuliahan_paket", "type": "POST",
                    "data": function(d) { d.filter_id_smt = $('#filterSmtKrsPaket_<%=rnd%>').val(); d.tahapan = 1; }
                },
                "columns": [
                    { "data": "matakuliah", "className": "fw-bold text-dark" },
                    { "data": "sks", "className": "text-center fw-bold text-success" },
                    { "data": "dosen_jadwal", "className": "small text-secondary" },
                    { "data": "kapasitas", "className": "text-center small fw-bold" },
                ],
                "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
                "pageLength": 50, "ordering": false,
                "dom": '<"top">rt<"bottom d-flex justify-content-between p-3"ip><"clear">',
                "drawCallback": function( settings ) {
                    var api = this.api();
                    gSksPaket = 0;
                    api.rows().every(function() {
                        var d = this.data();
                        if (d && d.sks_raw) gSksPaket += parseInt(d.sks_raw);
                    });
                    
                    var total = gSksTerambil + gSksPaket;
                    if(total > gMaxSks) {
                        $('#lblWarningPaketSks_<%=rnd%>').removeClass('d-none');
                        $('#btnSimpanPaketKrs_<%=rnd%>').prop('disabled', true);
                    } else {
                        $('#lblWarningPaketSks_<%=rnd%>').addClass('d-none');
                        $('#btnSimpanPaketKrs_<%=rnd%>').prop('disabled', false);
                    }
                }
            });
            
            // Tangkap Data Error dari AJAX (Jika paket tidak ditemukan)
            dtTabelPilih.on('xhr.dt', function ( e, settings, json, xhr ) {
                if (json && json.status === 'error') {
                    $('#modalKrsPaketFull_<%=rnd%>').modal('hide');
                    Swal.fire({ icon: 'error', title: '<%= Common.getBahasaConfigJS("Perhatian") %>', text: json.message });
                }
            });
        }

        function executeSimpanPaket() {
            var allKpmIds = [];
            if (dtTabelPilih) {
                dtTabelPilih.rows().every(function() {
                    var d = this.data();
                    if(d && d.kpm_id) allKpmIds.push(d.kpm_id);
                });
            }

            if (allKpmIds.length === 0) {
                Swal.fire({ icon: 'warning', title: '<%= Common.getBahasaConfigJS("Peringatan") %>', text: '<%= Common.getBahasaConfigJS("Tidak ada data paket untuk disimpan.") %>' }); return;
            }

            var $btn = $(this); var oriSimpan = $btn.html();
            $btn.html('<i class="fas fa-spinner fa-spin me-2"></i> <%= Common.getBahasaConfig("Menyimpan...") %>').prop('disabled', true);

            $.ajax({
                url: urlServiceKrs + "&action=simpan_krs_paket", type: "POST", dataType: "json", 
                data: { filter_id_smt: $('#filterSmtKrsPaket_<%=rnd%>').val(), tahapan: 1, kpm_ids: allKpmIds },
                success: function(response) {
                    $btn.html(oriSimpan).prop('disabled', false);
                    if (response.status === "success") {
                        Swal.fire({ icon: 'success', title: '<%= Common.getBahasaConfigJS("Berhasil") %>', text: response.message, timer: 2000, showConfirmButton: false });
                        $('#modalKrsPaketFull_<%=rnd%>').modal('hide');
                        isKrsRefresh = "true";
                        if (dtTabelKrs) dtTabelKrs.ajax.reload(function() { isKrsRefresh = "false"; }, false); 
                    } else if (response.status === "warning") {
                        Swal.fire({ icon: 'warning', title: '<%= Common.getBahasaConfigJS("Disimpan dengan Peringatan") %>', text: response.message });
                        isKrsRefresh = "true";
                        if (dtTabelKrs) dtTabelKrs.ajax.reload(function() { isKrsRefresh = "false"; }, false); 
                        $('#modalKrsPaketFull_<%=rnd%>').modal('hide');
                    } else {
                        Swal.fire('Error', response.message, 'error');
                    }
                },
                error: function() { $btn.html(oriSimpan).prop('disabled', false); Swal.fire('Error', '<%= Common.getBahasaConfigJS("Gagal menghubungi server untuk penyimpanan.") %>', 'error'); }
            });
        }
    }
</script>