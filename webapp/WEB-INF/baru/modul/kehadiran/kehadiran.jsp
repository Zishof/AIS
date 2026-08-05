<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<% 
    String rnd = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8); 
    ais.database.model.Tbmuser tbmuser = ais.common.Common.getCurrentUser(request);
    Long mhsId = (tbmuser != null && tbmuser.getMahasiswa() != null) ? tbmuser.getMahasiswa().getId() : 0L;
%>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">
<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>

<style>
    #wrapKehadiran_<%=rnd%> .hover-elevate { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    #wrapKehadiran_<%=rnd%> .hover-elevate:hover { transform: translateY(-3px); box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important; }
    .table-kehadiran th { background-color: #f8f9fa; text-transform: uppercase; font-size: 0.85rem; letter-spacing: 0.5px; }
    .table-kehadiran td { vertical-align: middle; font-size: 0.95rem; }
</style>

<div class="container-fluid py-4" id="wrapKehadiran_<%=rnd%>">
    
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5" style="border-color: #6f42c1 !important;">
        <div class="text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px; flex-shrink: 0; background-color: #6f42c1;">
            <i class="fas fa-fingerprint fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Riwayat Kehadiran Mahasiswa") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Modul untuk memantau rekapitulasi persentase absensi dan rincian kehadiran di setiap pertemuan perkuliahan.") %></p>
        </div>
    </div>

    <div class="row mb-4">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4">
                <div class="card-body p-3 d-flex flex-column flex-md-row justify-content-between align-items-center bg-light rounded-4 gap-3">
                    <div class="d-flex align-items-center w-100" style="max-width: 500px;">
                        <span class="fw-bold text-secondary me-3 text-nowrap"><i class="fas fa-filter me-1"></i> <%= Common.getBahasaConfig("Periode:") %></span>
                        <select id="filterSmtKehadiran_<%=rnd%>" class="form-select rounded-pill shadow-sm" style="border-color: #6f42c1;">
                            <option value=""><%= Common.getBahasaConfig("Memuat Periode...") %></option>
                        </select>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="row mb-4">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4 border-top border-4" style="border-color: #6f42c1 !important;">
                <div class="card-header bg-white pt-4 pb-2 border-bottom-0">
                    <h5 class="fw-bold mb-0 d-flex align-items-center" style="color: #6f42c1;">
                        <i class="fas fa-clipboard-check me-2"></i> <%= Common.getBahasaConfig("Rekapitulasi Kehadiran") %>
                    </h5>
                </div>
                <div class="card-body p-4 bg-white table-responsive rounded-bottom-4">
                    <table id="tabelKehadiran_<%=rnd%>" class="table table-hover table-bordered table-kehadiran align-middle w-100 mb-0">
                        <thead class="table-light">
                            <tr>
                                <th width="35%"><%= Common.getBahasaConfig("Mata Kuliah") %></th>
                                <th width="10%" class="text-center"><%= Common.getBahasaConfig("SKS") %></th>
                                <th width="25%"><%= Common.getBahasaConfig("Dosen & Jadwal") %></th>
                                <th width="20%" class="text-center"><%= Common.getBahasaConfig("Rekap") %></th>
                                <th width="10%" class="text-center"><i class="fas fa-cog"></i></th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // Memastikan eksekusi langsung saat DOM siap
    $(document).ready(function() {
        if (typeof $.fn.DataTable === 'undefined') {
            $.getScript("https://cdn.datatables.net/1.10.24/js/jquery.dataTables.min.js", function() {
                $.getScript("https://cdn.datatables.net/1.10.24/js/dataTables.bootstrap4.min.js", function() {
                    initKehadiranApp_<%=rnd%>();
                });
            });
        } else {
            initKehadiranApp_<%=rnd%>();
        }
    });

    function initKehadiranApp_<%=rnd%>() {
        var urlSvc = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kehadiran&s=_kehadiran_service';
        var dtKehadiran = null;

        // 1. Load Filter dengan AJAX
        $.ajax({
            url: urlSvc + "&action=get_filter_smt", 
            type: "GET", 
            dataType: "json",
            success: function(res) {
                var $sel = $('#filterSmtKehadiran_<%=rnd%>').empty();
                if (res.status === 'success' && res.data) {
                    $sel.append(new Option('<%= Common.getBahasaConfigJS("Semua Semester & Tahun Ajaran") %>', ''));
                    $.each(res.data, function(i, v) { 
                        var $opt = $('<option></option>').val(v.idSmt).text(v.label);
                        $sel.append($opt);
                    });
                    
                    // PERBAIKAN: Mengatur default terpilih ke semester aktif (default_id)
                    if (res.default_id && $sel.find("option[value='" + res.default_id + "']").length > 0) {
                        $sel.val(res.default_id);
                    } else {
                        $sel.val(''); 
                    }
                } else {
                    $sel.append(new Option('<%= Common.getBahasaConfigJS("Gagal memuat periode") %>', ''));
                }
                
                initTables(); 
                loadDataKehadiran(); // Akan memicu Load Data Pertama Kali secara otomatis
            },
            error: function(xhr, status, error) {
                $('#filterSmtKehadiran_<%=rnd%>').empty().append(new Option('Error 500: Server Gagal', ''));
                Swal.fire('Kesalahan Sistem', 'Gagal memuat data periode.', 'error');
            }
        });

        $('#filterSmtKehadiran_<%=rnd%>').on('change', loadDataKehadiran);

        function initTables() {
            dtKehadiran = $('#tabelKehadiran_<%=rnd%>').DataTable({
                "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
                "pageLength": 25, "ordering": false,
                "columns": [
                    { "data": "matakuliah" }, 
                    { "data": "sks", "className": "text-center" }, 
                    { "data": "dosen_jadwal", "className": "small text-secondary" }, 
                    { "data": "rekap", "className": "text-center" },
                    { "data": null, "className": "text-center", "render": function(data, type, row) {
                        return '<button class="btn btn-sm btn-outline-primary rounded-pill shadow-sm px-3" onclick="lihatDetailKehadiran_<%=rnd%>('+row.perkuliahan_id+')"><i class="fas fa-search me-1"></i> <%= Common.getBahasaConfig("Detail") %></button>';
                    }}
                ]
            });
        }

        function loadDataKehadiran() {
            var selectedSmt = $('#filterSmtKehadiran_<%=rnd%>').val() || '';
            
            if(dtKehadiran) { 
                dtKehadiran.clear().draw(); 
                $('#tabelKehadiran_<%=rnd%> tbody').html('<tr><td colspan="5" class="text-center py-4 text-muted"><i class="fas fa-spinner fa-spin me-2"></i> <%= Common.getBahasaConfig("Memuat data kehadiran...") %></td></tr>');
            }

            $.ajax({
                url: urlSvc + "&action=get_kehadiran",
                type: "POST", 
                dataType: "json", 
                data: { filter_id_smt: selectedSmt },
                success: function(response) {
                    if (response.status === 'success' && response.data) {
                        dtKehadiran.clear(); // Bersihkan pesan loading
                        dtKehadiran.rows.add(response.data).draw();
                    } else if (response.status === 'error') {
                        Swal.fire('Peringatan', response.message, 'warning');
                    }
                },
                error: function(xhr, status, error) {
                    Swal.fire('Kesalahan Sistem', 'Gagal mengambil rekapitulasi kehadiran.', 'error');
                }
            });
        }

        // Ekspose fungsi detail ke global untuk tombol action
        window.lihatDetailKehadiran_<%=rnd%> = function(idPerkuliahan) {
            var modalId = 'modalDetailKehadiran_<%=rnd%>';
            
            if ($('#' + modalId).length === 0) {
                var modalHtml = 
                    '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 1060;">' +
                        '<div class="modal-dialog modal-xl modal-dialog-scrollable animate__animated animate__zoomIn">' + 
                            '<div class="modal-content border-0 shadow-lg rounded-4">' +
                                '<div class="modal-header text-white border-0 py-3" style="background-color: #6f42c1;">' +
                                    '<h5 class="modal-title fw-bold"><i class="fas fa-list-ol me-2"></i><%=Common.getBahasaConfig("Rincian Kehadiran Perkuliahan")%></h5>' +
                                    '<button type="button" class="btn-close btn-close-white" onclick="$(\'#' + modalId + '\').modal(\'hide\');" aria-label="Close"></button>' +
                                '</div>' +
                                '<div class="modal-body p-4 bg-light">' +
                                    '<div class="alert alert-light border shadow-sm mb-4">' +
                                        '<h6 class="fw-bold mb-0 text-dark" id="lblDetailMk_'+modalId+'"><i class="fas fa-spinner fa-spin me-2"></i> <%= Common.getBahasaConfig("Memuat data...") %></h6>' +
                                    '</div>' +
                                    '<div class="card shadow-sm border-0 rounded-4">' +
                                        '<div class="card-body p-0 table-responsive">' +
                                            '<table id="tabelDetailPertemuan_'+modalId+'" class="table table-hover table-striped w-100 mb-0">' +
                                                '<thead class="table-light"><tr>' +
                                                    '<th width="5%" class="text-center"><%= Common.getBahasaConfig("Ke") %></th>' +
                                                    '<th width="20%"><%= Common.getBahasaConfig("Tanggal") %></th>' +
                                                    '<th width="10%"><%= Common.getBahasaConfig("Ruang") %></th>' +
                                                    '<th width="25%"><%= Common.getBahasaConfig("Materi / Topik") %></th>' +
                                                    '<th width="20%"><%= Common.getBahasaConfig("Dosen") %></th>' +
                                                    '<th width="20%" class="text-center"><%= Common.getBahasaConfig("Kehadiran") %></th>' +
                                                '</tr></thead>' +
                                                '<tbody id="bodyDetailPertemuan_'+modalId+'"></tbody>' +
                                            '</table>' +
                                        '</div>' +
                                    '</div>' +
                                '</div>' +
                                '<div class="modal-footer bg-white border-top py-3">' +
                                    '<button type="button" class="btn btn-secondary rounded-pill px-4 fw-bold shadow-sm" onclick="$(\'#' + modalId + '\').modal(\'hide\');"><i class="fas fa-times me-2"></i> <%= Common.getBahasaConfig("Tutup") %></button>' +
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
            }

            $.ajax({
                url: urlSvc + "&action=get_detail_kehadiran",
                type: "POST", 
                dataType: "json", 
                data: { perkuliahan_id: idPerkuliahan },
                success: function(res) {
                    if(res.status === 'success') {
                        $('#lblDetailMk_' + modalId).html("<i class='fas fa-book me-2' style='color:#6f42c1;'></i> " + res.mk_title);
                        
                        var tbody = $('#bodyDetailPertemuan_' + modalId);
                        tbody.empty();
                        
                        if (res.data && res.data.length > 0) {
                            $.each(res.data, function(i, v) {
                                tbody.append(
                                    "<tr>" +
                                        "<td class='text-center'>" + v.pertemuan_ke + "</td>" +
                                        "<td>" + v.tanggal + "</td>" +
                                        "<td>" + v.ruang + "</td>" +
                                        "<td>" + v.materi + "</td>" +
                                        "<td>" + v.dosen + "</td>" +
                                        "<td class='text-center'>" + v.kehadiran + "</td>" +
                                    "</tr>"
                                );
                            });
                        } else {
                            tbody.append("<tr><td colspan='6' class='text-center text-muted fst-italic py-4'><%= Common.getBahasaConfig("Belum ada data pertemuan yang diselenggarakan.") %></td></tr>");
                        }
                        
                        var detailModal = new bootstrap.Modal(document.getElementById(modalId));
                        detailModal.show();
                    } else {
                        Swal.fire({ icon: 'error', title: 'Perhatian', text: res.message });
                    }
                },
                error: function(xhr, status, error) {
                    Swal.fire('Kesalahan Sistem', 'Gagal memuat rincian absensi.', 'error');
                }
            });
        };
    }
</script>