<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">
<link rel="stylesheet" href="https://cdn.datatables.net/buttons/1.7.0/css/buttons.bootstrap4.min.css">
<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<div class="container-fluid py-4">
    
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded border-start border-primary">
        <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm p-3">
            <i class="fas fa-users-cog fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-primary"><%= Common.getBahasaConfig("Dasbor Kepegawaian") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Pantauan komprehensif data kepegawaian, distribusi unit kerja, dan demografi personel.") %></p>
        </div>
    </div>

    <div class="row mb-4" id="kpi-container">
        <div class="col-12 text-center text-primary py-4">
            <i class="fas fa-circle-notch fa-spin fa-2x mb-2"></i> <br> 
            <span class="fw-medium"><%=Common.getBahasaConfig("Memuat ringkasan data, mohon tunggu...")%></span>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-3">
                <div class="card-header pt-3 pb-0 bg-white border-bottom-0">
                    <ul class="nav nav-tabs nav-tabs-scrollable" id="kepegawaianTabKategori" role="tablist">
                        <li class="nav-item">
                            <button class="nav-link active fw-semibold" data-tipe="unit_kerja">
                                <i class="fas fa-building me-1"></i> <%=Common.getBahasaConfig("Unit Kerja")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="pendidikan">
                                <i class="fas fa-graduation-cap me-1"></i> <%=Common.getBahasaConfig("Pendidikan")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="status">
                                <i class="fas fa-id-badge me-1"></i> <%=Common.getBahasaConfig("Status")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="agama">
                                <i class="fas fa-praying-hands me-1"></i> <%=Common.getBahasaConfig("Agama")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="tipe">
                                <i class="fas fa-user-tag me-1"></i> <%=Common.getBahasaConfig("Tipe")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="ptkp">
                                <i class="fas fa-file-invoice-dollar me-1"></i> <%=Common.getBahasaConfig("PTKP")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="ikatan">
                                <i class="fas fa-link me-1"></i> <%=Common.getBahasaConfig("Ikatan")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="masa_kerja">
                                <i class="fas fa-business-time me-1"></i> <%=Common.getBahasaConfig("Masa Kerja")%>
                            </button>
                        </li>
                        <li class="nav-item">
                            <button class="nav-link fw-semibold" data-tipe="asuransi">
                                <i class="fas fa-shield-alt me-1"></i> <%=Common.getBahasaConfig("Asuransi")%>
                            </button>
                        </li>
                    </ul>
                </div>
                
                <div class="card-body p-4">
                    <div class="mb-5 pb-3 border-bottom">
                        <canvas id="kepegawaianChart" height="80"></canvas>
                    </div>

                    <div class="table-responsive">
                        <table id="tabelKepegawaianUtama" class="table table-hover table-striped table-bordered align-middle w-100">
                        </table>
                    </div>
                    
                    <div class="mt-3 text-end">
                        <h5 class="fw-bold mb-0 text-dark">
                            <%=Common.getBahasaConfig("Total Keseluruhan")%> : 
                            <span id="labelTotalKeseluruhan" class="text-primary fs-4 ms-2">0</span>
                        </h5>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/jquery.dataTables.min.js"></script>
<script data-cfasync="false" src="https://cdn.datatables.net/1.10.24/js/dataTables.bootstrap4.min.js"></script>

<script>
    var urlServiceKepegawaian = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kepegawaian&s=_monitor_kepegawaian_service';
    var tableKepegawaian = null;
    var chartKepegawaianObj = null; // Menyimpan instansi grafik

    $(document).ready(function() {
        loadKpiKepegawaian();
        initTableKepegawaian('unit_kerja');

        $('#kepegawaianTabKategori .nav-link').on('click', function(e) {
            e.preventDefault();
            $('#kepegawaianTabKategori .nav-link').removeClass('active');
            $(this).addClass('active');
            initTableKepegawaian($(this).data('tipe'));
        });
    });

    function loadKpiKepegawaian() {
        $.ajax({
            url: urlServiceKepegawaian + "&action=get_summary",
            type: "GET",
            dataType: "json",
            success: function(data) {
                if(data.error) {
                    $('#kpi-container').html('<div class="alert alert-danger w-100 shadow-sm">' + data.error + '</div>');
                    return;
                }
                var kpiHtml = '';
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pegawai Aktif")%>', data.total_aktif, "fa-user-check", "col-md-4");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Pegawai Non-Aktif")%>', data.total_nonaktif, "fa-user-times", "col-md-4");
                kpiHtml += createKpiCard('<%=Common.getBahasaConfigJS("Total Unit Kerja")%>', data.total_unit_kerja, "fa-building", "col-md-4");
                
                $('#kpi-container').html(kpiHtml);
            },
            error: function() {
                $('#kpi-container').html('<div class="alert alert-danger w-100 shadow-sm"><%=Common.getBahasaConfig("Gagal memuat data ringkasan kepegawaian.")%></div>');
            }
        });
    }

    function createKpiCard(title, value, iconClass, gridClass) {
        return '<div class="' + gridClass + ' col-sm-12 mb-3">' +
                   '<div class="kpi-card-sop shadow-sm">' +
                       '<i class="fas ' + iconClass + ' kpi-icon-watermark"></i>' +
                       '<div class="kpi-title-sop">' + title + '</div>' +
                       '<div class="kpi-value-sop">' + value + '</div>' +
                   '</div>' +
               '</div>';
    }

    // Fungsi Render Grafik (Chart.js)
    function renderChartKepegawaian(dataJson) {
        var labelKategori = $('#kepegawaianTabKategori .nav-link.active').text().trim();
        var labels = [];
        var values = [];
        
        // Sorting data dari jumlah terbanyak agar grafiknya membentuk tangga piramida (enak dipandang)
        var sortedData = dataJson.slice().sort(function(a, b) {
            return b.jumlah_raw - a.jumlah_raw;
        });

        // Loop untuk memilah data yang siap diputar di grafik (Limit maks 15 kategori terbanyak agar grafik tidak menumpuk)
        for(var i = 0; i < sortedData.length && i < 15; i++) {
            var cat = sortedData[i].kategori;
            // Memotong label jika kepanjangan (misal nama unit kerja yang super panjang)
            if (cat.length > 25) { cat = cat.substring(0, 22) + "..."; }
            
            labels.push(cat);
            values.push(sortedData[i].jumlah_raw);
        }

        var ctx = document.getElementById('kepegawaianChart').getContext('2d');
        
        // Destroy grafik lama sebelum membangun grafik dari tab baru
        if (chartKepegawaianObj) {
            chartKepegawaianObj.destroy();
        }

        chartKepegawaianObj = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: '<%=Common.getBahasaConfigJS("Jumlah Pegawai")%>',
                    data: values,
                    backgroundColor: 'rgba(40, 167, 69, 0.75)', // Mengikuti warna hijau (theme primary-dark dari kuning_hijau.css)
                    borderColor: '#28a745',
                    borderWidth: 1,
                    borderRadius: 6,
                    hoverBackgroundColor: '#ffc107' // Efek hover kuning terang
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: { display: false }, // Sembunyikan legenda karena sudah diwakili judul
                    title: {
                        display: true,
                        text: '<%=Common.getBahasaConfigJS("Distribusi Berdasarkan")%> ' + labelKategori,
                        font: { size: 16, weight: 'bold' }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 1 } // Memaksa agar sumbu Y selalu bulat (tidak ada pecahan pegawai)
                    },
                    x: {
                        ticks: {
                            autoSkip: false,
                            maxRotation: 45,
                            minRotation: 0
                        }
                    }
                }
            }
        });
    }

    function initTableKepegawaian(tipe) {
        if (tableKepegawaian !== null) {
            tableKepegawaian.destroy();
            $('#tabelKepegawaianUtama').empty(); 
        }
        
        var colsHtml = '<thead class="table-light"><tr>' + 
                       '<th class="text-center w-auto"><%=Common.getBahasaConfig("No")%></th>' +
                       '<th><%=Common.getBahasaConfig("Kategori / Klasifikasi")%></th>' +
                       '<th class="text-center w-auto"><%=Common.getBahasaConfig("Jumlah Pegawai")%></th>' +
                       '</tr></thead><tbody></tbody>';

        $('#tabelKepegawaianUtama').html(colsHtml);

        tableKepegawaian = $('#tabelKepegawaianUtama').DataTable({
            "ajax": {
                "url": urlServiceKepegawaian + "&action=get_data_tabel&tipe=" + tipe,
                "dataSrc": function(json) {
                    $('#labelTotalKeseluruhan').text(json.total_keseluruhan);
                    
                    // Render/Update Chart dengan data JSON yang baru diambil
                    renderChartKepegawaian(json.data);
                    
                    return json.data;
                }
            },
            "columns": [
                { "data": null, "className": "text-center", "render": function (data, type, row, meta) { return meta.row + 1; } },
                { "data": "kategori", "className": "fw-medium" },
                { "data": "jumlah", "className": "text-center fw-bold text-primary" }
            ],
            "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
            "pageLength": 10,
            "ordering": true,
            "order": [[ 2, "desc" ]] // Sort otomatis berdasarkan jumlah terbanyak
        });
    }
</script>