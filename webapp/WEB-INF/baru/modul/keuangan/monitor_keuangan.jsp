<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">
<link rel="stylesheet" href="https://cdn.datatables.net/buttons/1.7.0/css/buttons.bootstrap4.min.css">

<div class="container-fluid py-4">
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5 border-primary">
        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px; flex-shrink: 0;">
            <i class="fas fa-chart-line fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Dasbor Monitor Keuangan (Pengajuan & Persetujuan)") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Analisis komprehensif terkait Kasbon, Talangan, LPJ, Kas Kecil, dan Pemetaan Keuangan Terpadu.") %></p>
        </div>
    </div>

    <div class="row mb-4" id="kpi-container">
        <div class="col-12 text-center text-primary py-5">
            <i class="fas fa-circle-notch fa-spin fa-2x mb-3"></i> <br> 
            <span class="fw-medium"><%= Common.getBahasaConfig("Sedang memuat data rekapitulasi keuangan, mohon tunggu...") %></span>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4">
                <div class="card-header pt-3 pb-2 border-bottom-0 bg-white rounded-top-4">
                    <ul class="nav nav-tabs nav-tabs-scrollable" id="keuanganTabKategori" role="tablist">
                        <li class="nav-item">
                            <button class="nav-link active" data-tipe="pengajuan_kasbon">
                                <i class="fas fa-file-invoice-dollar me-1"></i> <%= Common.getBahasaConfig("Pengajuan Kasbon") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="persetujuan_kasbon">
                                <i class="fas fa-check-double me-1"></i> <%= Common.getBahasaConfig("Persetujuan Kasbon") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="pengajuan_talangan">
                                <i class="fas fa-hand-holding-usd me-1"></i> <%= Common.getBahasaConfig("Pengajuan Talangan") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="persetujuan_talangan">
                                <i class="fas fa-handshake me-1"></i> <%= Common.getBahasaConfig("Persetujuan Talangan") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="pengajuan_lpj">
                                <i class="fas fa-clipboard-list me-1"></i> <%= Common.getBahasaConfig("Pengajuan LPJ") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="persetujuan_lpj">
                                <i class="fas fa-clipboard-check me-1"></i> <%= Common.getBahasaConfig("Persetujuan LPJ") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="pengajuan_kaskecil">
                                <i class="fas fa-wallet me-1"></i> <%= Common.getBahasaConfig("Klr Kas Kecil") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="persetujuan_kaskecil">
                                <i class="fas fa-coins me-1"></i> <%= Common.getBahasaConfig("Acc Kas Kecil") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="pengajuan_gantikaskecil">
                                <i class="fas fa-exchange-alt me-1"></i> <%= Common.getBahasaConfig("Ganti Kas Kecil") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="persetujuan_gantikaskecil">
                                <i class="fas fa-sync-alt me-1"></i> <%= Common.getBahasaConfig("Acc Ganti Kas Kecil") %>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-tipe="persetujuan_kasbesar">
                                <i class="fas fa-university me-1"></i> <%= Common.getBahasaConfig("Acc Kas Besar") %>
                            </button>
                        </li>
                    </ul>
                </div>
                
                <div class="card-body p-4 table-responsive">
                    <table id="tabelKeuanganUtama" class="table table-hover table-striped table-bordered align-middle" style="width:100%">
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/jquery.dataTables.min.js"></script>
<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/dataTables.bootstrap4.min.js"></script>

<script>
    // Sesuaikan parameter 'p' dengan letak modul folder Anda
    var urlServiceBase = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=keuangan&s=_monitor_keuangan_service';
    var tableUtama = null;

    $(document).ready(function() {
        // 1. Memuat angka KPI di bagian atas
        loadKpiDashboard();

        // 2. Inisialisasi tabel untuk tab pertama
        initTableUtama('pengajuan_kasbon');

        // 3. Event Navigasi Tab Data
        $('#keuanganTabKategori .nav-link').on('click', function(e) {
            e.preventDefault();
            $('#keuanganTabKategori .nav-link').removeClass('active');
            $(this).addClass('active');
            initTableUtama($(this).data('tipe'));
        });

        // 4. Event Interaksi Klik KPI -> Buka Modal
        $(document).on('click', '.kpi-card-clickable', function() {
            var tipe = $(this).attr('data-tipe');
            var judul = $(this).find('.kpi-title-finance').text();
            tampilkanPopupDetail(tipe, judul);
        });
    });

    /**
     * Memuat ringkasan KPI Dashboard via AJAX
     */
    function loadKpiDashboard() {
        $.ajax({
            url: urlServiceBase + "&action=get_summary",
            type: "GET",
            dataType: "json",
            success: function(data) {
                if(data.error) {
                    $('#kpi-container').html('<div class="alert alert-danger w-100 shadow-sm"><i class="fas fa-exclamation-triangle me-2"></i> ' + data.error + '</div>');
                    return;
                }
                var kpiHtml = '';
                
                // Kategori Pengajuan (Ikon Standar)
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pengajuan Kasbon")%>', data.pengajuan_kasbon, "fa-file-invoice-dollar", "pengajuan_kasbon");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pengajuan Talangan")%>', data.pengajuan_talangan, "fa-hand-holding-usd", "pengajuan_talangan");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pengajuan LPJ")%>', data.pengajuan_lpj, "fa-clipboard-list", "pengajuan_lpj");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Keluar Kas Kecil")%>', data.pengajuan_kaskecil, "fa-wallet", "pengajuan_kaskecil");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Ganti Kas Kecil")%>', data.pengajuan_gantikaskecil, "fa-exchange-alt", "pengajuan_gantikaskecil");
                
                // Kategori Persetujuan (Ikon Warna Success untuk pembeda visual)
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Persetujuan Kasbon")%>', data.persetujuan_kasbon, "fa-check-double text-success", "persetujuan_kasbon");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Persetujuan Talangan")%>', data.persetujuan_talangan, "fa-handshake text-success", "persetujuan_talangan");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Persetujuan LPJ")%>', data.persetujuan_lpj, "fa-clipboard-check text-success", "persetujuan_lpj");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Persetujuan Kas Kecil")%>', data.persetujuan_kaskecil, "fa-coins text-success", "persetujuan_kaskecil");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Acc Ganti Kas Kecil")%>', data.persetujuan_gantikaskecil, "fa-sync-alt text-success", "persetujuan_gantikaskecil");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Persetujuan Kas Besar")%>', data.persetujuan_kasbesar, "fa-university text-success", "persetujuan_kasbesar");
                
                $('#kpi-container').html(kpiHtml);
            },
            error: function() {
                $('#kpi-container').html('<div class="alert alert-danger w-100 shadow-sm"><i class="fas fa-wifi me-2"></i> <%=Common.getBahasaConfig("Gagal memuat data Ringkasan Keuangan. Periksa koneksi Anda.")%></div>');
            }
        });
    }

    /**
     * Membangun HTML Kotak KPI
     */
    function createKpiCard(title, value, iconClass, tipe) {
        return '<div class="col-xl-2 col-lg-3 col-md-4 col-sm-6 mb-3">' +
                   '<div class="kpi-card-finance h-100 shadow-sm kpi-card-clickable" data-tipe="' + tipe + '" title="<%=Common.getBahasaConfig("Klik untuk melihat rincian data")%>">' +
                       '<i class="fas ' + iconClass + ' kpi-icon-watermark"></i>' +
                       '<div class="kpi-title-finance">' + title + '</div>' +
                       '<div class="kpi-value-finance">' + value + '</div>' +
                   '</div>' +
               '</div>';
    }

    /**
     * Konfigurasi Kolom Dinamis Berdasarkan Tipe Tab
     */
    function buildTableConfig(tipe) {
        var isPersetujuan = tipe.includes("persetujuan");
        
        var colsHtml = '<thead class="table-light"><tr><th class="text-center" width="5%"><%=Common.getBahasaConfig("No")%></th>';
        var dtColumns = [{ "data": null, "className": "text-center", "render": function (data, type, row, meta) { return meta.row + 1; } }];

        if (!isPersetujuan) {
            // Mode Pengajuan (Belum disetujui)
            colsHtml += '<th><%=Common.getBahasaConfig("Kode Dokumen")%></th><th><%=Common.getBahasaConfig("Waktu Pengajuan")%></th><th><%=Common.getBahasaConfig("Unit Pemohon")%></th><th class="text-end"><%=Common.getBahasaConfig("Jumlah (Rp)")%></th>';
            dtColumns.push(
                { "data": "kode", "className": "fw-bold text-primary" }, 
                { "data": "waktu_buat" }, 
                { "data": "unit_pemohon" }, 
                { "data": "jumlah", "className": "text-end fw-bold" }
            );
        } else {
            // Mode Persetujuan (Sudah disetujui)
            colsHtml += '<th><%=Common.getBahasaConfig("Kode Dokumen")%></th><th><%=Common.getBahasaConfig("Waktu Pengajuan")%></th><th><%=Common.getBahasaConfig("Waktu Persetujuan")%></th><th><%=Common.getBahasaConfig("Disetujui Oleh")%></th><th class="text-end"><%=Common.getBahasaConfig("Jumlah (Rp)")%></th>';
            dtColumns.push(
                { "data": "kode", "className": "fw-bold text-primary" }, 
                { "data": "waktu_buat" }, 
                { "data": "waktu_acc" }, 
                { "data": "user_acc" }, 
                { "data": "jumlah", "className": "text-end fw-bold text-success" }
            );
        }

        colsHtml += '</tr></thead><tbody></tbody>';
        return { html: colsHtml, columns: dtColumns };
    }

    /**
     * Render DataTables Utama
     */
    function initTableUtama(tipe) {
        if (tableUtama !== null) {
            tableUtama.destroy();
            $('#tabelKeuanganUtama').empty();
        }
        
        var config = buildTableConfig(tipe);
        $('#tabelKeuanganUtama').html(config.html);

        tableUtama = $('#tabelKeuanganUtama').DataTable({
            "ajax": urlServiceBase + "&action=get_data_tabel&tipe=" + tipe,
            "columns": config.columns,
            "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
            "pageLength": 10,
            "ordering": false // Diserahkan ke backend untuk ordering desc id
        });
    }

    /**
     * Menampilkan Modal Popup DataTables saat KPI diklik
     */
    function tampilkanPopupDetail(tipe, judul) {
        var modalId = 'modal_keuangan_' + new Date().getTime();
        var config = buildTableConfig(tipe);

        var modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered">' +
                    '<div class="modal-content border-0 shadow-lg">' +
                        '<div class="modal-header bg-primary text-white py-3">' +
                            '<h5 class="mb-0 fw-bold"><i class="fas fa-list-alt me-2"></i> <%=Common.getBahasaConfig("Rincian Data:")%> ' + judul + '</h5>' +
                            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4">' +
                            '<div class="table-responsive">' +
                                '<table id="tabel_' + modalId + '" class="table table-hover table-striped table-bordered align-middle" style="width:100%">' +
                                    config.html +
                                '</table>' +
                            '</div>' +
                        '</div>' +
                        '<div class="modal-footer bg-light">' +
                            '<button type="button" class="btn btn-outline-secondary rounded-pill px-4 fw-bold" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%=Common.getBahasaConfig("Tutup")%></button>' +
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
                "pageLength": 10,
                "ordering": false
            });

            // Pembersihan DOM saat modal ditutup
            document.getElementById(modalId).addEventListener('hidden.bs.modal', function () {
                tableDetail.destroy(); 
                this.remove();   
            });
        });
    }
</script>