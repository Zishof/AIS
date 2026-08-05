<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">
<link rel="stylesheet" href="https://cdn.datatables.net/buttons/1.7.0/css/buttons.bootstrap4.min.css">

<div class="container-fluid py-4">
    
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5 border-primary">
        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px;">
            <i class="fas fa-boxes fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Dasbor Pengadaan & Pembelian") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Pantauan komprehensif Pengajuan, Persetujuan, Pemesanan PO, Pembayaran, dan Perjanjian Kerjasama.") %></p>
        </div>
    </div>

    <div class="row mb-4" id="kpi-container">
        <div class="col-12 text-center text-primary py-4">
            <i class="fas fa-spinner fa-spin fa-2x mb-2"></i> <br> 
            <span class="fw-medium"><%=Common.getBahasaConfig("Memuat data pengadaan, mohon tunggu...")%></span>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-3">
                <div class="card-header pt-3 pb-0 border-bottom-0">
                    <ul class="nav nav-tabs nav-tabs-scrollable" id="pengadaanTabKategori" role="tablist">
                        <li class="nav-item">
                            <button class="nav-link active fw-semibold" data-tipe="pengajuan_pr">
                                <i class="fas fa-file-import me-1"></i> <%=Common.getBahasaConfig("Pengajuan PR")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="persetujuan_pr">
                                <i class="fas fa-file-signature me-1"></i> <%=Common.getBahasaConfig("Persetujuan PR")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="pengajuan_po">
                                <i class="fas fa-shopping-cart me-1"></i> <%=Common.getBahasaConfig("Pengajuan PO")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="persetujuan_po">
                                <i class="fas fa-check-double me-1"></i> <%=Common.getBahasaConfig("Persetujuan PO")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="pembayaran_po">
                                <i class="fas fa-money-bill-wave me-1"></i> <%=Common.getBahasaConfig("Pembayaran PO")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="perjanjian_ks">
                                <i class="fas fa-handshake me-1"></i> <%=Common.getBahasaConfig("Perjanjian KS")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold text-danger" data-tipe="tolak_pr">
                                <i class="fas fa-times-circle me-1"></i> <%=Common.getBahasaConfig("PR Ditolak")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold text-danger" data-tipe="tolak_po">
                                <i class="fas fa-ban me-1"></i> <%=Common.getBahasaConfig("PO Ditolak")%>
                            </button>
                        </li>
                    </ul>
                </div>
                
                <div class="card-body p-4 table-responsive">
                    <table id="tabelPengadaanUtama" class="table table-hover table-striped table-bordered align-middle" style="width:100%">
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/jquery.dataTables.min.js"></script>
<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/dataTables.bootstrap4.min.js"></script>

<script>
    var urlServiceBase = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pengadaan&s=_monitor_pengadaan_service';
    var tableUtama = null;

    $(document).ready(function() {
        loadKpiDashboard();
        initTableUtama('pengajuan_pr');

        $('#pengadaanTabKategori .nav-link').on('click', function(e) {
            e.preventDefault();
            $('#pengadaanTabKategori .nav-link').removeClass('active');
            $(this).addClass('active');
            initTableUtama($(this).data('tipe'));
        });

        $(document).on('click', '.kpi-card-clickable', function() {
            var tipe = $(this).attr('data-tipe');
            var judul = $(this).find('.kpi-title-sop').text();
            tampilkanPopupDetail(tipe, judul);
        });
    });

    function loadKpiDashboard() {
        $.ajax({
            url: urlServiceBase + "&action=get_summary",
            type: "GET",
            dataType: "json",
            success: function(data) {
                if(data.error) {
                    $('#kpi-container').html('<div class="alert alert-danger w-100 shadow-sm">' + data.error + '</div>');
                    return;
                }
                var kpiHtml = '';
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pengajuan PR")%>', data.pengajuan_pr, "fa-file-import", "pengajuan_pr", "col-md-3");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Persetujuan PR")%>', data.persetujuan_pr, "fa-file-signature", "persetujuan_pr", "col-md-3");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("PR Ditolak")%>', data.tolak_pr, "fa-times-circle text-danger", "tolak_pr", "col-md-3");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Perjanjian KS")%>', data.perjanjian_ks, "fa-handshake", "perjanjian_ks", "col-md-3");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pengajuan PO")%>', data.pengajuan_po, "fa-shopping-cart", "pengajuan_po", "col-md-3");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Persetujuan PO")%>', data.persetujuan_po, "fa-check-double", "persetujuan_po", "col-md-3");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("PO Ditolak")%>', data.tolak_po, "fa-ban text-danger", "tolak_po", "col-md-3");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pembayaran PO")%>', data.pembayaran_po, "fa-money-bill-wave text-success", "pembayaran_po", "col-md-3");
                
                $('#kpi-container').html(kpiHtml);
            },
            error: function() {
                $('#kpi-container').html('<div class="alert alert-danger w-100 shadow-sm"><%=Common.getBahasaConfig("Gagal memuat data Dasbor.")%></div>');
            }
        });
    }

    function createKpiCard(title, value, iconClass, tipe, gridClass) {
        return '<div class="' + gridClass + ' col-sm-6 mb-3">' +
                   '<div class="kpi-card-sop h-100 shadow-sm kpi-card-clickable" data-tipe="' + tipe + '" title="<%=Common.getBahasaConfig("Klik untuk melihat rincian")%>">' +
                       '<i class="fas ' + iconClass + ' kpi-icon-watermark"></i>' +
                       '<div class="kpi-title-sop">' + title + '</div>' +
                       '<div class="kpi-value-sop">' + value + '</div>' +
                   '</div>' +
               '</div>';
    }

    function buildTableConfig(tipe) {
        var colsHtml = '<thead class="table-light"><tr><th class="text-center" width="5%"><%=Common.getBahasaConfig("No")%></th>';
        var dtColumns = [{ "data": null, "className": "text-center", "render": function (data, type, row, meta) { return meta.row + 1; } }];

        if (tipe === 'pengajuan_pr' || tipe === 'pengajuan_po') {
            colsHtml += '<th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Waktu Pengajuan")%></th><th><%=Common.getBahasaConfig("Unit Pemohon")%></th><th class="text-end"><%=Common.getBahasaConfig("Jumlah")%></th>';
            dtColumns.push({ "data": "kode" }, { "data": "waktu_buat" }, { "data": "unit_pemohon" }, { "data": "jumlah", "className": "text-end fw-bold" });
        } 
        else if (tipe === 'persetujuan_pr' || tipe === 'persetujuan_po' || tipe === 'perjanjian_ks') {
            colsHtml += '<th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Wkt Pengajuan")%></th><th><%=Common.getBahasaConfig("Wkt Persetujuan")%></th><th><%=Common.getBahasaConfig("Disetujui Oleh")%></th><th class="text-end"><%=Common.getBahasaConfig("Jumlah")%></th>';
            dtColumns.push({ "data": "kode" }, { "data": "waktu_buat" }, { "data": "waktu_acc" }, { "data": "user_acc" }, { "data": "jumlah", "className": "text-end fw-bold" });
        } 
        else if (tipe === 'pembayaran_po') {
            colsHtml += '<th><%=Common.getBahasaConfig("Kode PO")%></th><th><%=Common.getBahasaConfig("Wkt Diterima")%></th><th><%=Common.getBahasaConfig("Wkt Bayar")%></th><th><%=Common.getBahasaConfig("Penyedia")%></th><th class="text-end"><%=Common.getBahasaConfig("Jumlah")%></th>';
            dtColumns.push({ "data": "kode" }, { "data": "waktu_terima" }, { "data": "waktu_bayar" }, { "data": "penyedia" }, { "data": "jumlah", "className": "text-end fw-bold text-success" });
        } 
        else if (tipe === 'tolak_pr' || tipe === 'tolak_po') {
            colsHtml += '<th><%=Common.getBahasaConfig("Kode")%></th><th><%=Common.getBahasaConfig("Wkt Pengajuan")%></th><th><%=Common.getBahasaConfig("Wkt Ditolak")%></th><th><%=Common.getBahasaConfig("Ditolak Oleh")%></th><th class="text-end"><%=Common.getBahasaConfig("Jumlah")%></th>';
            dtColumns.push({ "data": "kode" }, { "data": "waktu_buat" }, { "data": "waktu_tolak", "className": "text-danger" }, { "data": "user_tolak", "className": "text-danger" }, { "data": "jumlah", "className": "text-end fw-bold" });
        }

        colsHtml += '</tr></thead><tbody></tbody>';
        return { html: colsHtml, columns: dtColumns };
    }

    function initTableUtama(tipe) {
        if (tableUtama !== null) {
            tableUtama.destroy();
            $('#tabelPengadaanUtama').empty(); 
        }
        
        var config = buildTableConfig(tipe);
        $('#tabelPengadaanUtama').html(config.html);

        tableUtama = $('#tabelPengadaanUtama').DataTable({
            "ajax": urlServiceBase + "&action=get_data_tabel&tipe=" + tipe,
            "columns": config.columns,
            "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
            "pageLength": 10
        });
    }

    function tampilkanPopupDetail(tipe, judul) {
        var modalId = 'modal_pengadaan_' + new Date().getTime();
        var config = buildTableConfig(tipe);

        var modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered">' +
                    '<div class="modal-content border-0 shadow-lg">' +
                        '<div class="modal-header py-3">' +
                            '<h5 class="mb-0"><i class="fas fa-list-ul me-2"></i> ' + judul + '</h5>' +
                            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4">' +
                            '<div class="table-responsive">' +
                                '<table id="tabel_' + modalId + '" class="table table-hover table-bordered align-middle" style="width:100%">' +
                                    config.html +
                                '</table>' +
                            '</div>' +
                        '</div>' +
                        '<div class="modal-footer bg-light">' +
                            '<button type="button" class="btn btn-secondary rounded-pill px-4" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        var targetModal = new bootstrap.Modal(document.getElementById(modalId));
        targetModal.show();

        document.getElementById(modalId).addEventListener('shown.bs.modal', function () {
            var tableDetail = $('#tabel_' + modalId).DataTable({
                "ajax": urlServiceBase + "&action=get_data_tabel&tipe=" + tipe,
                "columns": config.columns,
                "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
                "destroy": true, 
                "pageLength": 10
            });

            document.getElementById(modalId).addEventListener('hidden.bs.modal', function () {
                tableDetail.destroy(); 
                this.remove();        
            });
        });
    }
</script>