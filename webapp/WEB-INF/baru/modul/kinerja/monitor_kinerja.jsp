<%@ page isELIgnored="true" %>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
    Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();
    boolean bolehLihatpegwaiLain = tbmrole == null ? true : tbmrole.getMelihatDataPegawaiLain();
%>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">
<link rel="stylesheet" href="https://cdn.datatables.net/buttons/1.7.0/css/buttons.bootstrap4.min.css">
<script data-cfasync="false" src="https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js" integrity="sha512-GsLlZN/3F2ErC5ifS5QtgpiJtWd43JWSuIgh7mbzZ8zBps+dvLusV+eNQATqgA/HdeKFVgA5v3S/cIrLF7QnIg==" crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<style>
    .nav-tabs-scrollable { overflow-x: auto; flex-wrap: nowrap; border-bottom: 2px solid #dee2e6; }
    .nav-tabs-scrollable .nav-link { white-space: nowrap; border: none; color: #495057; font-weight: 500; padding: 12px 20px; transition: all 0.3s ease; }
    .nav-tabs-scrollable .nav-link.active { color: #0d6efd; border-bottom: 3px solid #0d6efd; background: transparent; }
    .nav-tabs-scrollable .nav-link:hover:not(.active) { color: #0d6efd; background-color: #f8f9fa; border-radius: 6px 6px 0 0; }
    .kpi-card-kinerja { transition: transform 0.2s; border-radius: 12px; border-left: 5px solid #0d6efd; background-color: #fff; padding: 20px; position: relative; overflow: hidden;}
    .kpi-card-kinerja:hover { transform: translateY(-5px); box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important; cursor: pointer;}
    .kpi-icon-watermark { position: absolute; right: 10%; top: 50%; transform: translateY(-50%); font-size: 4rem; opacity: 0.1; }
</style>

<div class="container-fluid py-4">
    
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5 border-primary">
        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px;">
            <i class="fas fa-chart-line fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Dasbor Kinerja Pegawai") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Pantauan komprehensif Catatan Harian, Penilaian Capaian, dan Statistik Kinerja.") %></p>
        </div>
    </div>

    <div class="row mb-4" id="kpi-container">
        <div class="col-12 text-center text-primary py-4">
            <i class="fas fa-spinner fa-spin fa-2x mb-2"></i> <br> 
            <span class="fw-medium"><%=Common.getBahasaConfig("Memuat data kinerja, mohon tunggu...")%></span>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4">
                <div class="card-header bg-white pt-3 pb-0 border-bottom-0 rounded-top-4">
                    <ul class="nav nav-tabs nav-tabs-scrollable" id="kinerjaTabKategori" role="tablist">
                        <li class="nav-item">
                            <button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tab-catatan-harian" type="button" role="tab">
                                <i class="fas fa-calendar-check me-2"></i> <%=Common.getBahasaConfig("Catatan Harian")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-penilaian" type="button" role="tab">
                                <i class="fas fa-medal me-2"></i> <%=Common.getBahasaConfig("Penilaian Capaian Kerja")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-rincian" type="button" role="tab">
                                <i class="fas fa-list-alt me-2"></i> <%=Common.getBahasaConfig("Rincian Capaian Kerja")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-rekap" type="button" role="tab">
                                <i class="fas fa-folder-open me-2"></i> <%=Common.getBahasaConfig("Rekap Catatan Harian")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-statistik" type="button" role="tab">
                                <i class="fas fa-chart-pie me-2"></i> <%=Common.getBahasaConfig("Statistik")%>
                            </button>
                        </li>
                    </ul>
                </div>
                
                <div class="card-body p-4 tab-content">
                    <div class="tab-pane fade show active" id="tab-catatan-harian" role="tabpanel">
                        
                        <div class="d-flex justify-content-between align-items-center mb-4 bg-light p-3 rounded-3 border">
                            <div>
                                <button type="button" class="btn btn-primary shadow-sm" id="btnTambahCatatan">
                                    <i class="fas fa-plus-circle me-2"></i> <%=Common.getBahasaConfig("Tambah Catatan Harian")%>
                                </button>
                                <button type="button" class="btn btn-outline-secondary shadow-sm ms-2" id="btnRefreshData">
                                    <i class="fas fa-sync-alt me-1"></i> <%=Common.getBahasaConfig("Segarkan")%>
                                </button>
                            </div>
                            
                            <div class="text-end">
                                <% if (bolehLihatpegwaiLain) { %>
                                    <span class="badge bg-primary bg-opacity-10 text-primary border border-primary px-3 py-2 rounded-pill shadow-sm">
                                        <i class="fas fa-users me-1"></i> <%=Common.getBahasaConfig("Akses: Seluruh Pegawai")%>
                                    </span>
                                <% } else { %>
                                    <span class="badge bg-secondary bg-opacity-10 text-secondary border border-secondary px-3 py-2 rounded-pill shadow-sm">
                                        <i class="fas fa-user-check me-1"></i> <%=Common.getBahasaConfig("Menampilkan data:")%> <%= pegawai != null ? pegawai.getNama() : "-" %>
                                    </span>
                                <% } %>
                            </div>
                        </div>

                        <div class="table-responsive">
                            <table id="tabelKinerjaUtama" class="table table-hover table-striped table-bordered align-middle w-100">
                                <thead class="table-light">
                                    <tr>
                                        <th class="text-center" width="5%"><%=Common.getBahasaConfig("No")%></th>
                                        
                                        <%-- KONDISI: Hanya tampilkan kolom ini jika user punya akses melihat pegawai lain --%>
                                        <% if (bolehLihatpegwaiLain) { %>
                                            <th class="text-center" width="5%"><%=Common.getBahasaConfig("Foto")%></th>
                                            <th width="15%"><%=Common.getBahasaConfig("Pegawai")%></th>
                                        <% } %>

                                        <th width="25%"><%=Common.getBahasaConfig("Kegiatan")%></th>
                                        <th class="text-center"><%=Common.getBahasaConfig("Kuantitas")%></th>
                                        <th class="text-center"><%=Common.getBahasaConfig("Waktu")%></th>
                                        <th class="text-end"><%=Common.getBahasaConfig("Biaya")%></th>
                                        <th class="text-center"><%=Common.getBahasaConfig("Verifikasi Asesor")%></th>
                                        <th><%=Common.getBahasaConfig("Catatan Asesor")%></th>
                                        <th class="text-center" width="8%"><i class="fas fa-cog"></i></th>
                                    </tr>
                                </thead>
                                <tbody></tbody>
                            </table>
                        </div>
                    </div>

                    <div class="tab-pane fade" id="tab-penilaian" role="tabpanel">
                        
                        <div class="card bg-light border-0 shadow-sm mb-4">
                            <div class="card-body">
                                <div class="row align-items-end">
                                    <%-- KONDISI HAK AKSES PEGAWAI --%>
                                    <% if (bolehLihatpegwaiLain) { %>
                                        <div class="col-md-4 mb-3 mb-md-0">
                                            <label class="form-label fw-bold text-muted small"><i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Cari Pegawai (NIP / Nama / Kode)")%></label>
                                            <input type="text" class="form-control form-control-sm" id="filterPenilaianSearch" placeholder="<%=Common.getBahasaConfig("Masukkan kata kunci...")%>">
                                        </div>
                                    <% } else { %>
                                        <div class="col-md-4 mb-3 mb-md-0">
                                            <label class="form-label fw-bold text-muted small"><i class="fas fa-user me-1"></i> <%=Common.getBahasaConfig("Pegawai")%></label>
                                            <input type="text" class="form-control form-control-sm bg-light text-muted fw-bold" value="<%= pegawai != null ? pegawai.getNama() : "-" %>" readonly disabled>
                                            <input type="hidden" id="filterPenilaianSearch" value="">
                                        </div>
                                    <% } %>
                                    <div class="col-md-3 mb-3 mb-md-0">
                                        <label class="form-label fw-bold text-muted small"><i class="fas fa-calendar-alt me-1"></i> <%=Common.getBahasaConfig("Bulan")%></label>
                                        <select class="form-select form-select-sm" id="filterPenilaianBulan">
                                            <option value="1"><%=Common.getBahasaConfig("Januari")%></option>
                                            <option value="2"><%=Common.getBahasaConfig("Februari")%></option>
                                            <option value="3"><%=Common.getBahasaConfig("Maret")%></option>
                                            <option value="4" selected><%=Common.getBahasaConfig("April")%></option>
                                            <option value="5"><%=Common.getBahasaConfig("Mei")%></option>
                                            <option value="6"><%=Common.getBahasaConfig("Juni")%></option>
                                            <option value="7"><%=Common.getBahasaConfig("Juli")%></option>
                                            <option value="8"><%=Common.getBahasaConfig("Agustus")%></option>
                                            <option value="9"><%=Common.getBahasaConfig("September")%></option>
                                            <option value="10"><%=Common.getBahasaConfig("Oktober")%></option>
                                            <option value="11"><%=Common.getBahasaConfig("November")%></option>
                                            <option value="12"><%=Common.getBahasaConfig("Desember")%></option>
                                        </select>
                                    </div>
                                    <div class="col-md-2 mb-3 mb-md-0">
                                        <label class="form-label fw-bold text-muted small"><i class="fas fa-calendar me-1"></i> <%=Common.getBahasaConfig("Tahun")%></label>
                                        <input type="number" class="form-control form-control-sm" id="filterPenilaianTahun" value="2026">
                                    </div>
                                    <div class="col-md-3 text-end">
                                        <button class="btn btn-primary btn-sm w-100 shadow-sm" id="btnProsesPenilaian">
                                            <i class="fas fa-sync-alt me-2"></i> <%=Common.getBahasaConfig("Tampilkan Penilaian")%>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div id="container-penilaian-kosong" class="text-center py-5">
                            <i class="fas fa-file-invoice text-muted fa-4x mb-3 opacity-25"></i>
                            <h5 class="text-secondary"><%=Common.getBahasaConfig("Data Belum Ditampilkan")%></h5>
                            <p class="text-muted"><%=Common.getBahasaConfig("Silakan gunakan filter di atas untuk memuat data Penilaian Capaian LKP.")%></p>
                        </div>
                        
                        <div id="container-penilaian-laporan" class="d-none">
                            <div class="d-flex justify-content-end mb-3 gap-2">
                                <button class="btn btn-sm btn-outline-danger shadow-sm" onclick="unduhPDF()">
                                    <i class="fas fa-file-pdf me-1"></i> <%=Common.getBahasaConfig("Unduh PDF")%>
                                </button>
                                <button class="btn btn-sm btn-outline-success shadow-sm ms-2" onclick="unduhExcel()">
                                    <i class="fas fa-file-excel me-1"></i> <%=Common.getBahasaConfig("Unduh Excel")%>
                                </button>
                                <button class="btn btn-sm btn-outline-secondary shadow-sm ms-2" onclick="window.print()">
                                    <i class="fas fa-print me-1"></i> <%=Common.getBahasaConfig("Cetak")%>
                                </button>
                            </div>
                            
                            <div class="report-paper table-responsive" id="render-area-laporan">
                                </div>
                        </div>

                    </div>
                    <div class="tab-pane fade text-center py-5" id="tab-rincian" role="tabpanel">
                        <i class="fas fa-list-alt text-muted fa-4x mb-3 opacity-50"></i>
                        <h5 class="text-secondary"><%=Common.getBahasaConfig("Rincian Capaian Kerja")%></h5>
                    </div>
                    <div class="tab-pane fade text-center py-5" id="tab-rekap" role="tabpanel">
                        <i class="fas fa-folder-open text-muted fa-4x mb-3 opacity-50"></i>
                        <h5 class="text-secondary"><%=Common.getBahasaConfig("Rekapitulasi Catatan Harian")%></h5>
                    </div>
                    <div class="tab-pane fade text-center py-5" id="tab-statistik" role="tabpanel">
                        <i class="fas fa-chart-pie text-muted fa-4x mb-3 opacity-50"></i>
                        <h5 class="text-secondary"><%=Common.getBahasaConfig("Statistik Kinerja Pegawai")%></h5>
                    </div>

                </div>
            </div>
        </div>
    </div>
</div>

<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/jquery.dataTables.min.js"></script>
<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/dataTables.bootstrap4.min.js"></script>

<script>
    var urlServiceBaseKinerja = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kinerja&s=_monitor_kinerja_service';
    var tableUtamaKinerja = null;

    $(document).ready(function() {
        loadKpiKinerjaDashboard();
        initTableKinerjaUtama();

        // Event Listener Tombol Refresh
        $('#btnRefreshData').on('click', function() {
            if(tableUtamaKinerja != null) {
                tableUtamaKinerja.ajax.reload(null, false); // Reload tanpa mereset paging
            }
            loadKpiKinerjaDashboard();
        });

        // Event Listener Tombol Tambah
        $('#btnTambahCatatan').on('click', function() {
            // Panggil fungsi eksternal untuk Tambah (Integrasi dengan RealisasiKerjaPegawaiDetailAction di Java/JSP yang ada)
            alert('<%=Common.getBahasaConfigJS("Membuka form Tambah Catatan Harian...")%>');
        });

        // Event Listener Aksi Tabel (Ubah & Hapus)
        $('#tabelKinerjaUtama').on('click', '.btn-edit', function() {
            var id = $(this).data('id');
            alert('<%=Common.getBahasaConfigJS("Membuka form Ubah untuk ID:")%> ' + id);
        });

        $('#tabelKinerjaUtama').on('click', '.btn-delete', function() {
            var id = $(this).data('id');
            if(confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data ini?")%>')) {
                // Lakukan AJAX Delete kesini, kemudian refresh tabel
                alert('<%=Common.getBahasaConfigJS("Data berhasil dihapus (Simulasi).")%>');
                tableUtamaKinerja.ajax.reload(null, false);
            }
        });
    });

    // Fungsi Memuat Dashboard KPI
    function loadKpiKinerjaDashboard() {
        $.ajax({
            url: urlServiceBaseKinerja + "&action=get_summary",
            type: "GET",
            dataType: "json",
            success: function(data) {
                if(data.error) {
                    $('#kpi-container').html('<div class="alert alert-danger w-100 shadow-sm"><i class="fas fa-exclamation-triangle me-2"></i>' + data.error + '</div>');
                    return;
                }
                var kpiHtml = '';
                kpiHtml += createKpiCardKinerja('<%=Common.getBahasaConfigJS("Total Catatan")%>', data.total_catatan, "fa-file-alt text-primary", "col-md-4", "border-primary");
                kpiHtml += createKpiCardKinerja('<%=Common.getBahasaConfigJS("Menunggu Verifikasi")%>', data.total_belum_verifikasi, "fa-clock text-warning", "col-md-4", "border-warning");
                kpiHtml += createKpiCardKinerja('<%=Common.getBahasaConfigJS("Terverifikasi")%>', data.total_verifikasi, "fa-check-circle text-success", "col-md-4", "border-success");
                
                $('#kpi-container').html(kpiHtml);
            },
            error: function() {
                $('#kpi-container').html('<div class="alert alert-danger w-100 shadow-sm"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal memuat data Statistik Dasbor Kinerja.")%></div>');
            }
        });
    }

    // Fungsi Render HTML Kartu KPI
    function createKpiCardKinerja(title, value, iconClass, gridClass, borderColor) {
        return '<div class="' + gridClass + ' col-sm-12 mb-3">' +
                   '<div class="kpi-card-kinerja shadow-sm ' + borderColor + '">' +
                       '<i class="fas ' + iconClass + ' kpi-icon-watermark"></i>' +
                       '<div class="text-muted fw-bold text-uppercase small mb-2">' + title + '</div>' +
                       '<div class="fs-2 fw-bolder text-dark">' + value + '</div>' +
                   '</div>' +
               '</div>';
    }

 // Inisialisasi DataTables Server-Side Processing
    function initTableKinerjaUtama() {
        if (tableUtamaKinerja !== null) {
            tableUtamaKinerja.destroy();
        }

        // Menyusun Kolom secara dinamis berdasarkan Hak Akses (RBAC)
        var dtColumns = [
            { "data": null, "className": "text-center align-middle", "orderable": false, "render": function (data, type, row, meta) { 
                return meta.row + meta.settings._iDisplayStart + 1; 
            }}
        ];

        // Jika Java memvalidasi user BISA melihat pegawai lain, inject kolom Foto dan Pegawai
        <% if (bolehLihatpegwaiLain) { %>
            dtColumns.push({ "data": "foto", "className": "text-center align-middle", "orderable": false });
            dtColumns.push({ "data": "pegawai", "className": "align-middle fw-medium" });
        <% } %>

        // Sisa kolom standar
        dtColumns.push(
            { "data": "kegiatan", "className": "align-middle" },
            { "data": "kuantitas", "className": "text-center align-middle" },
            { "data": "waktu", "className": "text-center align-middle" },
            { "data": "biaya", "className": "text-end align-middle fw-bold text-success" },
            { "data": "verifikasi_asesor", "className": "text-center align-middle" },
            { "data": "catatan_asesor", "className": "align-middle" },
            { "data": "aksi", "className": "text-center align-middle", "orderable": false }
        );
        
        tableUtamaKinerja = $('#tabelKinerjaUtama').DataTable({
            "processing": true,
            "serverSide": true,
            "ajax": {
                "url": urlServiceBaseKinerja + "&action=get_data_tabel",
                "type": "POST"
            },
            "columns": dtColumns,
            "language": { 
                "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json",
                "processing": '<i class="fas fa-spinner fa-spin fa-2x text-primary"></i><br><%=Common.getBahasaConfig("Memproses Data...")%>'
            },
            "pageLength": 10,
            "order": [], // Hindari pengurutan default frontend, biarkan server menangani
            "dom": "<'row'<'col-sm-12 col-md-6'l><'col-sm-12 col-md-6'f>>" +
                   "<'row'<'col-sm-12'tr>>" +
                   "<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>"
        });
    }
    
    
 // Event Listener untuk Tab Penilaian
    $(document).ready(function() {
        $('#btnProsesPenilaian').on('click', function() {
            loadLaporanPenilaian();
        });
    });

    function loadLaporanPenilaian() {
        var search = $('#filterPenilaianSearch').val();
        var bulan = $('#filterPenilaianBulan').val();
        var tahun = $('#filterPenilaianTahun').val();
        var namaBulan = $('#filterPenilaianBulan option:selected').text();

        $('#container-penilaian-kosong').html('<i class="fas fa-spinner fa-spin fa-3x text-primary mb-3"></i><br><b><%=Common.getBahasaConfig("Menyiapkan Dokumen...")%></b>').removeClass('d-none');
        $('#container-penilaian-laporan').addClass('d-none');

        $.ajax({
            url: urlServiceBaseKinerja + "&action=get_penilaian_capaian",
            type: "POST",
            data: { search: search, bulan: bulan, tahun: tahun },
            dataType: "json",
            success: function(response) {
                if(response.error) {
                    alert('<%=Common.getBahasaConfigJS("Gagal memuat data:")%> ' + response.error);
                    $('#container-penilaian-kosong').html('<span class="text-danger"><i class="fas fa-exclamation-triangle"></i> <%=Common.getBahasaConfig("Terjadi kesalahan teknis.")%></span>');
                    return;
                }
                
                if(!response.data || response.data.length === 0) {
                    $('#container-penilaian-kosong').html('<i class="fas fa-folder-open text-muted fa-4x mb-3 opacity-25"></i><br><h5 class="text-secondary"><%=Common.getBahasaConfig("Data Tidak Ditemukan")%></h5><p class="text-muted"><%=Common.getBahasaConfig("Tidak ada data capaian untuk pegawai dan periode tersebut.")%></p>');
                    return;
                }

                renderLaporanCapaian(response.data, tahun, namaBulan);
                $('#container-penilaian-kosong').addClass('d-none');
                $('#container-penilaian-laporan').removeClass('d-none');
            },
            error: function() {
                alert('<%=Common.getBahasaConfigJS("Gagal terhubung ke server.")%>');
            }
        });
    }

    function renderLaporanCapaian(data, tahun, namaBulan) {
        var h = data[0]; // Header Data dari row pertama
        
        // Akumulator Total
        var tWaktuTarget = 0, tWaktuRealisasi = 0;
        var sumAvgKuantitas = 0, sumAvgKualitas = 0, sumAvgWaktu = 0;
        
        var htmlBody = '';
        $.each(data, function(idx, row) {
            // Kalkulasi sesuai JRXML
            var rKuan = parseFloat(row.realisasi_kuantitas) || 0;
            var tKuan = parseFloat(row.kuantitas) || 1; 
            var avgKuan = (rKuan * 100.0) / tKuan;

            var rKual = parseFloat(row.kualitasrealisasi) || 0;
            var tKual = parseFloat(row.kualitas) || 1;
            var avgKual = (rKual * 100.0) / tKual;

            var rWak = parseFloat(row.realisasi_waktu) || 0;
            var tWak = parseFloat(row.waktu) || 1;
            var avgWak = (rWak * 100.0) / tWak;

            tWaktuTarget += tWak;
            tWaktuRealisasi += rWak;
            sumAvgKuantitas += avgKuan;
            sumAvgKualitas += avgKual;
            sumAvgWaktu += avgWak;

            htmlBody += '<tr>';
            htmlBody += '  <td class="text-center">' + (idx + 1) + '</td>';
            htmlBody += '  <td>' + (row.kegiatan || '-') + '</td>';
            htmlBody += '  <td class="text-center">' + (row.angkakredit || '0') + '</td>';
            htmlBody += '  <td class="text-end">' + (row.kuantitas || '0') + '<br><small>' + (row.satuan_kuantitas || '') + '</small></td>';
            htmlBody += '  <td class="text-end">' + (row.kualitas || '0') + '</td>';
            htmlBody += '  <td class="text-end">' + (row.waktu || '0') + ' ' + (row.satuanwaktu || '') + '</td>';
            htmlBody += '  <td class="text-end">' + (row.biaya || '0') + '</td>';
            htmlBody += '  <td class="text-center">' + (row.angkakredit || '0') + '</td>';
            htmlBody += '  <td class="text-end">' + rKuan + ' <small>' + (row.satuan_kuantitas || '') + '</small></td>';
            htmlBody += '  <td class="text-end">' + rKual + '</td>';
            htmlBody += '  <td class="text-end">' + rWak + ' ' + (row.satuanwaktu || '') + '</td>';
            htmlBody += '  <td class="text-end">' + (row.realisasi_biaya || '0') + '</td>';
            htmlBody += '  <td class="text-end fw-bold">' + Math.round(avgKuan) + '</td>';
            htmlBody += '  <td class="text-end fw-bold">' + Math.round(avgKual) + '</td>';
            htmlBody += '  <td class="text-end fw-bold">' + Math.round(avgWak) + '</td>';
            htmlBody += '</tr>';
        });

        // Rata-rata Total
        var count = data.length || 1;
        var avgTotalKuan = sumAvgKuantitas / count;
        var avgTotalKual = sumAvgKualitas / count;
        var avgTotalWak = sumAvgWaktu / count;
        
        // Final Score (Bobot: Kuantitas 70%, Kualitas 10%, Waktu 20% sesuai Parameter JRXML)
        var finalScore = (avgTotalKuan * 0.70) + (avgTotalKual * 0.10) + (avgTotalWak * 0.20);

        var html = `
            <div class="text-center mb-4">
                <img src="<%=Common.ROOT%>/img/logo.png" alt="Logo" style="height:70px; margin-bottom: 10px;">
                <h4 class="fw-bold mb-1"><%=Common.getBahasaConfig("PENILAIAN CAPAIAN LKP")%></h4>
                <h6 class="fw-bold mb-1"><%=Common.getBahasaConfig("Universitas / Sekolah Tinggi")%></h6>
                <p class="mb-0"><%=Common.getBahasaConfig("Tahun")%> `+tahun+` <%=Common.getBahasaConfig("bulan")%> `+namaBulan+`</p>
            </div>
            
            <div class="d-flex justify-content-between mb-3 align-items-end">
                <div class="d-flex">
                    <div class="me-4">
                        <img src="`+(h.url_foto || '')+`" style="width: 100px; height: 130px; object-fit: cover; border: 2px solid #ccc;">
                    </div>
                    <div>
                        <h6 class="fw-bold text-decoration-underline mb-2"><%=Common.getBahasaConfig("BIODATA")%></h6>
                        <table class="table-borderless table-sm">
                            <tr><td class="biodata-label"><%=Common.getBahasaConfig("No. Sertifikat")%></td><td class="biodata-value">: -</td></tr>
                            <tr><td class="biodata-label"><%=Common.getBahasaConfig("NIK / NIP")%></td><td class="biodata-value">: `+(h.nip || '-')+`</td></tr>
                            <tr><td class="biodata-label"><%=Common.getBahasaConfig("NAMA")%></td><td class="biodata-value">: `+(h.nama_pegawai || '-')+`</td></tr>
                            <tr><td class="biodata-label"><%=Common.getBahasaConfig("JAB. STRUKTURAL")%></td><td class="biodata-value">: `+(h.jabatan_struktural || '-')+`</td></tr>
                            <tr><td class="biodata-label"><%=Common.getBahasaConfig("JAB. FUNGSIONAL")%></td><td class="biodata-value">: `+(h.jabatan_fungsional || '-')+`</td></tr>
                            <tr><td class="biodata-label"><%=Common.getBahasaConfig("GOLONGAN")%></td><td class="biodata-value">: `+(h.golongan || '-')+`</td></tr>
                            <tr><td class="biodata-label"><%=Common.getBahasaConfig("SATUAN / UNIT KERJA")%></td><td class="biodata-value">: `+(h.satuan_kerja || '-')+`</td></tr>
                            <tr><td class="biodata-label"><%=Common.getBahasaConfig("TELP / NO. HP")%></td><td class="biodata-value">: `+(h.telp || '-')+`</td></tr>
                            <tr><td class="biodata-label"><%=Common.getBahasaConfig("EMAIL")%></td><td class="biodata-value">: `+(h.email || '-')+`</td></tr>
                        </table>
                    </div>
                </div>
            </div>

            <table>
                <thead class="report-table-header">
                    <tr>
                        <th rowspan="2" width="3%"><%=Common.getBahasaConfig("No.")%></th>
                        <th rowspan="2" width="20%"><%=Common.getBahasaConfig("Kegiatan Tugas Jabatan")%></th>
                        <th rowspan="2" width="3%"><%=Common.getBahasaConfig("AK")%></th>
                        <th colspan="4"><%=Common.getBahasaConfig("TARGET")%></th>
                        <th rowspan="2" width="3%"><%=Common.getBahasaConfig("AK")%></th>
                        <th colspan="4"><%=Common.getBahasaConfig("REALISASI")%></th>
                        <th rowspan="2" width="5%"><%=Common.getBahasaConfig("NILAI KUANTITAS")%></th>
                        <th rowspan="2" width="5%"><%=Common.getBahasaConfig("NILAI KUALITAS")%></th>
                        <th rowspan="2" width="5%"><%=Common.getBahasaConfig("NILAI WAKTU")%></th>
                    </tr>
                    <tr>
                        <th width="6%"><%=Common.getBahasaConfig("Kuan/Output")%></th>
                        <th width="5%"><%=Common.getBahasaConfig("Kual/Mutu")%></th>
                        <th width="6%"><%=Common.getBahasaConfig("Waktu")%></th>
                        <th width="6%"><%=Common.getBahasaConfig("Biaya")%></th>
                        <th width="6%"><%=Common.getBahasaConfig("Kuan/Output")%></th>
                        <th width="5%"><%=Common.getBahasaConfig("Kual/Mutu")%></th>
                        <th width="6%"><%=Common.getBahasaConfig("Waktu")%></th>
                        <th width="6%"><%=Common.getBahasaConfig("Biaya")%></th>
                    </tr>
                </thead>
                <tbody>
                    ` + htmlBody + `
                </tbody>
                <tfoot>
                    <tr>
                        <td colspan="5" class="text-end fw-bold"><%=Common.getBahasaConfig("Total")%></td>
                        <td class="text-end">`+tWaktuTarget+` `+(h.satuanwaktu || '')+`</td>
                        <td colspan="4"></td>
                        <td class="text-end">`+tWaktuRealisasi+` `+(h.satuanwaktu || '')+`</td>
                        <td></td>
                        <td class="text-end fw-bold">`+Math.round(avgTotalKuan)+`</td>
                        <td class="text-end fw-bold">`+Math.round(avgTotalKual)+`</td>
                        <td class="text-end fw-bold">`+Math.round(avgTotalWak)+`</td>
                    </tr>
                    <tr>
                        <td colspan="12" class="text-end fw-bold border-0 pt-3"><%=Common.getBahasaConfig("Nilai Capaian SKP")%> `+(h.nama_pegawai || '')+`</td>
                        <td colspan="3" class="text-end border-0 pt-3"><h5 class="fw-bold mb-0">`+Math.round(finalScore)+`</h5></td>
                    </tr>
                </tfoot>
            </table>

            <div class="row mt-5">
                <div class="col-6 text-center">
                    <br>
                    <p class="mb-5"><%=Common.getBahasaConfig("Pegawai")%></p>
                    <p class="fw-bold text-decoration-underline mb-0">`+(h.nama_pegawai || '-')+`</p>
                    <p>`+(h.nip || '-')+`</p>
                </div>
                <div class="col-6 text-center">
                    <p class="mb-0"><%=Common.getBahasaConfig("Samarinda")%>, `+new Date().toLocaleDateString('id-ID', {day: 'numeric', month: 'long', year: 'numeric'})+`</p>
                    <p class="mb-5"><%=Common.getBahasaConfig("Pejabat Penilai")%></p>
                    <p class="fw-bold text-decoration-underline mb-0">`+(h.asesor || '-')+`</p>
                    <p>NIP : `+(h.asesor_nip || '-')+`</p>
                </div>
            </div>
        `;

        $('#render-area-laporan').html(html);
    }
    
    
 // ==========================================================
    // FUNGSI EXPORT PDF (Menggunakan html2pdf.js)
    // ==========================================================
    function unduhPDF() {
        var elemenLaporan = document.getElementById('render-area-laporan');
        var namaPegawai = elemenLaporan.innerText.match(/NAMA\s*:\s*(.*)/); // Ekstrak nama untuk nama file
        var fileName = "Penilaian_Capaian_LKP" + (namaPegawai ? "_" + namaPegawai[1].trim() : "") + ".pdf";

        var opsiPDF = {
            margin:       [10, 10, 10, 10], // Margin atas, kiri, bawah, kanan (mm)
            filename:     fileName,
            image:        { type: 'jpeg', quality: 0.98 },
            html2canvas:  { scale: 2, useCORS: true }, // scale 2 untuk resolusi tinggi, useCORS agar foto profil ter-render
            jsPDF:        { unit: 'mm', format: 'a4', orientation: 'landscape' }
        };

        // Memunculkan loading state pada tombol
        var btn = event.currentTarget;
        var originalText = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> <%=Common.getBahasaConfig("Memproses...")%>';
        btn.disabled = true;

        html2pdf().set(opsiPDF).from(elemenLaporan).save().then(function() {
            // Kembalikan tombol ke semula setelah selesai
            btn.innerHTML = originalText;
            btn.disabled = false;
        });
    }

    // ==========================================================
    // FUNGSI EXPORT EXCEL (Menggunakan HTML Blob)
    // ==========================================================
    function unduhExcel() {
        var elemenLaporan = document.getElementById('render-area-laporan').innerHTML;
        var namaPegawai = document.getElementById('render-area-laporan').innerText.match(/NAMA\s*:\s*(.*)/);
        var fileName = "Penilaian_Capaian_LKP" + (namaPegawai ? "_" + namaPegawai[1].trim() : "") + ".xls";

        // Tambahkan meta UTF-8 dan XML schema agar format tabel (termasuk colspan/rowspan) terbaca sempurna di MS Excel
        var templateExcel = `
        <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
        <head>
            <meta charset="utf-8">
            <style>
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid black; padding: 5px; font-family: Arial, sans-serif; font-size: 11px; }
                th { background-color: #f2f2f2; font-weight: bold; text-align: center; }
                .text-center { text-align: center; }
                .text-end { text-align: right; }
                .fw-bold { font-weight: bold; }
            </style>
        </head>
        <body>
            ${elemenLaporan}
        </body>
        </html>`;

        var blobObject = new Blob([templateExcel], { type: 'application/vnd.ms-excel' });
        var blobUrl = URL.createObjectURL(blobObject);

        // Buat elemen <a> virtual untuk memicu download
        var anchor = document.createElement("a");
        anchor.href = blobUrl;
        anchor.download = fileName;
        document.body.appendChild(anchor);
        anchor.click();
        
        // Bersihkan DOM
        document.body.removeChild(anchor);
        URL.revokeObjectURL(blobUrl);
    }
</script>