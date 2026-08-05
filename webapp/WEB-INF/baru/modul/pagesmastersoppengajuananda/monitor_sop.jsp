<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">
<link rel="stylesheet" href="https://cdn.datatables.net/buttons/1.7.0/css/buttons.bootstrap4.min.css">

<style>
    /* Style khusus agar card KPI terlihat interaktif */
    .kpi-card-clickable { cursor: pointer; }
    /* Penyesuaian modal agar serasi dengan base-theme.css */
    .modal-content { border-radius: 15px; overflow: hidden; }
    .modal-header-custom { background: var(--theme-gradient); color: var(--theme-text-on-primary); }
</style>

<div class="container-fluid py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h3 class="mb-0 text-primary fw-bolder">
            <i class="fas fa-chart-line me-2"></i> <%=Common.getBahasaConfig("Dasbor Pantauan SOP")%>
        </h3>
    </div>

    <div class="row mb-4" id="kpi-container">
        <div class="col-12 text-center text-primary py-4">
            <i class="fas fa-spinner fa-spin fa-2x mb-2"></i> <br> 
            <span class="fw-medium"><%=Common.getBahasaConfig("Memuat data dasbor, mohon tunggu...")%></span>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-3">
                <div class="card-header pt-3 pb-0 border-bottom-0">
                    <ul class="nav nav-tabs" id="sopTabKategori" role="tablist">
                        <li class="nav-item">
                            <button class="nav-link active fw-semibold" data-tipe="menunggu_disposisi">
                                <i class="fas fa-hourglass-half me-1"></i> <%=Common.getBahasaConfig("Menunggu Disposisi")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="baru_disposisi">
                                <i class="fas fa-paper-plane me-1"></i> <%=Common.getBahasaConfig("Disposisi Terbaru")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="pengajuan_baru">
                                <i class="fas fa-file-signature me-1"></i> <%=Common.getBahasaConfig("Pengajuan Diproses")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="selesai">
                                <i class="fas fa-check-circle me-1"></i> <%=Common.getBahasaConfig("Telah Selesai")%>
                            </button>
                        </li>
                    </ul>
                </div>
                
                <div class="card-body p-4 table-responsive">
                    <table id="tabelSopUtama" class="table table-hover table-striped table-bordered align-middle" style="width:100%">
                        <thead class="table-light">
                            <tr>
                                <th width="5%" class="text-center"><%=Common.getBahasaConfig("No")%></th>
                                <th width="25%"><%=Common.getBahasaConfig("Nama SOP & Pengaju")%></th>
                                <th><%=Common.getBahasaConfig("Waktu Pengajuan")%></th>
                                <th><%=Common.getBahasaConfig("Tahapan SOP")%></th>
                                <th><%=Common.getBahasaConfig("Status Terakhir")%></th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/jquery.dataTables.min.js"></script>
<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/dataTables.bootstrap4.min.js"></script>
<script data-cfasync="false" src="https://cdn.datatables.net/buttons/1.7.0/js/dataTables.buttons.min.js"></script>
<script data-cfasync="false" src="https://cdn.datatables.net/buttons/1.7.0/js/buttons.bootstrap4.min.js"></script>
<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/jszip/3.1.3/jszip.min.js"></script>
<script data-cfasync="false" src="https://cdn.datatables.net/buttons/1.7.0/js/buttons.html5.min.js"></script>

<script>
    const urlServiceBase = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=pagesmastersoppengajuananda&s=sop_monitor_data';

    $(document).ready(function() {
        
        // 1. Inisialisasi Tabel Utama (Halaman Depan)
        var tableUtama = $('#tabelSopUtama').DataTable({
            "ajax": urlServiceBase + "&action=get_data_tabel&tipe=menunggu_disposisi",
            "columns": [
                { "data": null, "className": "text-center fw-bold text-muted", "render": function (data, type, row, meta) { return meta.row + 1; } },
                { "data": "sop_nama" }, 
                { "data": "waktu_pengajuan" },
                { "data": "tahap" },
                { "data": "status_detail" } 
            ],
            "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" }
        });

        // Tab Switcher
        $('#sopTabKategori .nav-link').on('click', function (e) {
            e.preventDefault();
            $('#sopTabKategori .nav-link').removeClass('active');
            $(this).addClass('active');
            tableUtama.ajax.url(urlServiceBase + "&action=get_data_tabel&tipe=" + $(this).data('tipe')).load();
        });

        // 2. Load KPI Summary
        loadKpiDashboard();

        // =======================================================================
        // AKSI KLIK KPI CARD -> MUNCULKAN POPUP MODAL SECARA DINAMIS
        // =======================================================================
        $(document).on('click', '.kpi-card-clickable', function() {
            var tipe = $(this).attr('data-tipe');
            var judul = $(this).find('.kpi-title-sop').text();
            
            // Tampilkan Popup Detail
            tampilkanPopupDetail(tipe, judul);
        });
    });

    /**
     * Fungsi untuk menyuntikkan Modal ke DOM dan mengaktifkan DataTable di dalamnya
     */
    function tampilkanPopupDetail(tipe, judul) {
        var modalId = 'modal_sop_' + new Date().getTime();
        
        // Buat HTML Modal secara dinamis
        var modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered">' +
                    '<div class="modal-content border-0 shadow-lg">' +
                        '<div class="card-header py-3 d-flex justify-content-between align-items-center">' +
                            '<h5 class="mb-0"><i class="fas fa-list-ul me-2"></i> ' + judul + '</h5>' +
                            '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-4">' +
                            '<div class="table-responsive">' +
                                '<table id="tabel_' + modalId + '" class="table table-hover table-bordered align-middle" style="width:100%">' +
                                    '<thead class="table-light">' +
                                        '<tr>' +
                                            '<th class="text-center"><%=Common.getBahasaConfig("No")%></th>' +
                                            '<th><%=Common.getBahasaConfig("SOP & Pengaju")%></th>' +
                                            '<th><%=Common.getBahasaConfig("Waktu")%></th>' +
                                            '<th><%=Common.getBahasaConfig("Aktor")%></th>' +
                                            '<th><%=Common.getBahasaConfig("Tahap")%></th>' +
                                            '<th><%=Common.getBahasaConfig("Tenggat")%></th>' +
                                            '<th><%=Common.getBahasaConfig("Keterangan Status")%></th>' +
                                        '</tr>' +
                                    '</thead>' +
                                    '<tbody></tbody>' +
                                '</table>' +
                            '</div>' +
                        '</div>' +
                        '<div class="modal-footer bg-light">' +
                            '<button type="button" class="btn btn-outline-success rounded-pill px-4" onclick="window.location.href=\'' + urlServiceBase + '&action=download_excel&tipe=' + tipe + '\'">' +
                                '<i class="fas fa-file-excel me-1"></i> <%=Common.getBahasaConfig("Unduh Seluruh Data")%>' +
                            '</button>' +
                            '<button type="button" class="btn btn-secondary rounded-pill px-4" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';

        // 1. Suntikkan HTML ke akhir Body
        document.body.insertAdjacentHTML('beforeend', modalHtml);

        // 2. Inisialisasi DataTable di dalam Modal tersebut
        var tableDetail = $('#tabel_' + modalId).DataTable({
            "ajax": urlServiceBase + "&action=get_data_tabel&tipe=" + tipe,
            "columns": [
                { "data": null, "className": "text-center", "render": function (data, type, row, meta) { return meta.row + 1; } },
                { "data": "sop_nama" }, 
                { "data": "waktu_pengajuan" },
                { "data": "aktor" },
                { "data": "tahap" },
                { "data": "deadline" },
                { "data": "status_detail" } 
            ],
            "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" }
        });

        // 3. Tampilkan Modal menggunakan Bootstrap API
        var targetModal = new bootstrap.Modal(document.getElementById(modalId));
        targetModal.show();

        // 4. Bersihkan DOM setelah modal ditutup (Memory Management)
        document.getElementById(modalId).addEventListener('hidden.bs.modal', function () {
            tableDetail.destroy(); // Hancurkan instance datatable
            this.remove();        // Hapus elemen dari body
        });
    }

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

                let kpiHtml = '';
                if(data.urutanSopAktif) {
                    kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Menunggu Disposisi")%>', data.menunggu, "fa-hourglass-half", "menunggu_disposisi");
                    kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Disposisi Terbaru")%>', data.disposisi, "fa-paper-plane", "baru_disposisi");
                    kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pengajuan Baru")%>', data.pengajuan, "fa-file-signature", "pengajuan_baru");
                } else {
                    kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pengajuan Baru")%>', data.pengajuan, "fa-file-signature", "pengajuan_baru");
                    kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Disposisi Terbaru")%>', data.disposisi, "fa-paper-plane", "baru_disposisi");
                    kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Menunggu Disposisi")%>', data.menunggu, "fa-hourglass-half", "menunggu_disposisi");
                }

                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Telah Selesai")%>', data.selesai, "fa-check-circle", "selesai");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Menunggu Aktor Lain")%>', data.menunggu_aktor, "fa-users", "menunggu_aktor");
                
                if(data.deadline > 0) {
                    kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Mendekati Tenggat Waktu")%>', data.deadline, "fa-exclamation-triangle", "deadline");
                }
                
                $('#kpi-container').html(kpiHtml);
            },
            error: function(jqXHR, textStatus, errorThrown) {
                console.error("Gagal memuat get_summary:", errorThrown);
                $('#kpi-container').html('<div class="alert alert-danger w-100 shadow-sm"><%=Common.getBahasaConfig("Gagal memuat data Dasbor.")%></div>');
            }
        });
    }

    function createKpiCard(title, value, iconClass, tipe) {
        return '<div class="col-md-2 col-sm-4 mb-3">' +
                   '<div class="kpi-card-sop h-100 kpi-card-clickable" data-tipe="' + tipe + '" title="<%=Common.getBahasaConfig("Klik untuk melihat rincian")%>">' +
                       '<i class="fas ' + iconClass + ' kpi-icon-watermark"></i>' +
                       '<div class="kpi-title-sop">' + title + '</div>' +
                       '<div class="kpi-value-sop">' + value + '</div>' +
                   '</div>' +
               '</div>';
    }
</script>