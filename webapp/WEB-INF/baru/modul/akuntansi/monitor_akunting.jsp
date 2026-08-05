<%@page import="java.util.Date"%>
<%@page import="java.util.Calendar"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%
    Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
    Date s = cal.getTime();
    cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 2); 
    Date m = cal.getTime();
    String defaultMulai = Common.dateFormat.get().format(m);
    String defaultSampai = Common.dateFormat.get().format(s);
%>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">

<div class="container-fluid py-4">
    
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5 border-primary">
        <div class="d-flex align-items-center mb-3 mb-md-0">
            <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px; min-width: 55px;">
                <i class="fas fa-file-invoice-dollar fs-3"></i>
            </div>
            <div>
                <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Dasbor Akuntansi & Laporan Keuangan") %></h4>
                <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Pantau laporan keuangan, mutasi buku besar, dan komparasi saldo antar periode secara terpadu.") %></p>
            </div>
        </div>
        <div>
            <button class="btn btn-outline-primary rounded-pill px-4 shadow-sm fw-bold" onclick="refreshTabAktif()">
                <i class="fas fa-sync-alt me-2"></i> <%= Common.getBahasaConfig("Perbarui Data") %>
            </button>
        </div>
    </div>

    <div class="card mb-4 border-0 shadow-sm rounded-4">
        <div class="card-body p-4">
            <div class="row align-items-end g-3">
                <div class="col-md-4">
                    <label class="form-label fw-bold text-secondary small"><i class="fas fa-building me-1"></i> <%=Common.getBahasaConfig("Satuan Kerja")%></label>
                    <select id="filterSatuanKerja" class="form-select bg-light border-0 shadow-none">
                        <option value=""><%=Common.getBahasaConfig("Seluruh Unit Kerja / Konsolidasi")%></option>
                    </select>
                </div>
                <div class="col-md-8">
                    <label class="form-label fw-bold text-secondary small mb-2"><i class="fas fa-layer-group me-1"></i> <%=Common.getBahasaConfig("Buku Laporan Aktif")%></label>
                    <div id="filterJenisLaporanContainer" class="d-flex flex-wrap gap-3"></div>
                </div>
            </div>
        </div>
    </div>

    <div class="card shadow-sm border-0 rounded-4" id="laporanContainer" style="display:none;">
        <div class="card-header bg-white pt-4 pb-0 border-bottom-0">
            <h5 class="fw-bold text-primary mb-3" id="judulLaporanAktif"></h5>
            <ul class="nav nav-tabs" id="akuntingTabs" role="tablist">
                <li class="nav-item">
                    <button class="nav-link active fw-bold px-4" data-tab="utama">
                        <i class="fas fa-chart-line me-2"></i> <%=Common.getBahasaConfig("Laporan Keuangan")%>
                    </button>
                </li>
                <li class="nav-item">
                    <button class="nav-link text-secondary fw-semibold px-4" data-tab="komparasi_bulan">
                        <i class="far fa-calendar-alt me-2"></i> <%=Common.getBahasaConfig("Komparasi Bulanan")%>
                    </button>
                </li>
                <li class="nav-item">
                    <button class="nav-link text-secondary fw-semibold px-4" data-tab="komparasi_tahun">
                        <i class="fas fa-history me-2"></i> <%=Common.getBahasaConfig("Komparasi Tahunan")%>
                    </button>
                </li>
            </ul>
        </div>
        
        <div class="card-body p-0">
            <div id="tabContent_utama" class="tab-pane-akunting">
                <div class="bg-light p-3 border-bottom d-flex align-items-end gap-3">
                    <div style="width: 200px;">
                        <label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Tanggal Mulai")%></label>
                        <input type="text" id="filterMulai" class="form-control form-control-sm datepicker" value="<%=defaultMulai%>">
                    </div>
                    <div style="width: 200px;">
                        <label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Tanggal Akhir")%></label>
                        <input type="text" id="filterSampai" class="form-control form-control-sm datepicker" value="<%=defaultSampai%>">
                    </div>
                    <button class="btn btn-sm btn-primary fw-bold" onclick="loadDataLaporanUtama()"><%=Common.getBahasaConfig("Terapkan")%></button>
                </div>
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0 w-100">
                        <thead class="bg-white text-secondary">
                            <tr>
                                <th width="40%" class="ps-4 py-3 font-monospace text-uppercase small"><i class="fas fa-sitemap me-2"></i> <%=Common.getBahasaConfig("Uraian Laporan")%></th>
                                <th width="15%" class="text-end py-3 font-monospace text-uppercase small"><%=Common.getBahasaConfig("Saldo Awal")%></th>
                                <th width="15%" class="text-end py-3 font-monospace text-uppercase small"><%=Common.getBahasaConfig("Mutasi Debet")%></th>
                                <th width="15%" class="text-end py-3 font-monospace text-uppercase small"><%=Common.getBahasaConfig("Mutasi Kredit")%></th>
                                <th width="15%" class="text-end pe-4 py-3 font-monospace text-uppercase small"><%=Common.getBahasaConfig("Saldo Akhir")%></th>
                            </tr>
                        </thead>
                        <tbody id="tabelUtamaBody"></tbody>
                    </table>
                </div>
            </div>
            
            <div id="tabContent_komparasi_bulan" class="tab-pane-akunting" style="display:none;">
                <div class="bg-light p-3 border-bottom d-flex align-items-end gap-3 justify-content-between">
                    <div class="d-flex align-items-end gap-3">
                        <div style="width: 200px;">
                            <label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Tahun Komparasi")%></label>
                            <select id="filterTahunBulan" class="form-select form-select-sm"></select>
                        </div>
                        <button class="btn btn-sm btn-primary fw-bold" onclick="loadKomparasiBulan()"><%=Common.getBahasaConfig("Terapkan")%></button>
                    </div>
                    <button class="btn btn-sm btn-outline-secondary fw-bold" onclick="alert('<%=Common.getBahasaConfigJS("Fitur unduhan sedang disiapkan.")%>')"><i class="fas fa-download me-1"></i> Download</button>
                </div>
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0 w-100" style="min-width: 1500px;">
                        <thead class="bg-white text-secondary">
                            <tr>
                                <th width="28%" class="ps-4 py-3 font-monospace text-uppercase small position-sticky start-0 bg-white shadow-sm z-index-1"><%=Common.getBahasaConfig("Uraian Laporan")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("Januari")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("Februari")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("Maret")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("April")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("Mei")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("Juni")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("Juli")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("Agustus")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("September")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("Oktober")%></th>
                                <th class="text-end py-3 font-monospace small"><%=Common.getBahasaConfig("November")%></th>
                                <th class="text-end pe-4 py-3 font-monospace small"><%=Common.getBahasaConfig("Desember")%></th>
                            </tr>
                        </thead>
                        <tbody id="tabelBulanBody"></tbody>
                    </table>
                </div>
            </div>
            
            <div id="tabContent_komparasi_tahun" class="tab-pane-akunting" style="display:none;">
                <div class="bg-light p-3 border-bottom d-flex align-items-end gap-3 justify-content-between">
                    <div class="d-flex align-items-end gap-3">
                        <div style="width: 150px;">
                            <label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Tahun Mulai")%></label>
                            <select id="filterTahunMulai" class="form-select form-select-sm"></select>
                        </div>
                        <div class="pb-1 fw-bold text-secondary">s.d</div>
                        <div style="width: 150px;">
                            <label class="form-label text-secondary small fw-bold"><%=Common.getBahasaConfig("Tahun Akhir")%></label>
                            <select id="filterTahunSampai" class="form-select form-select-sm"></select>
                        </div>
                        <button class="btn btn-sm btn-primary fw-bold" onclick="loadKomparasiTahun()"><%=Common.getBahasaConfig("Terapkan")%></button>
                    </div>
                    <button class="btn btn-sm btn-outline-secondary fw-bold" onclick="alert('<%=Common.getBahasaConfigJS("Fitur unduhan sedang disiapkan.")%>')"><i class="fas fa-download me-1"></i> Download</button>
                </div>
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0 w-100">
                        <thead class="bg-white text-secondary">
                            <tr id="headKomparasiTahun">
                                <th width="30%" class="ps-4 py-3 font-monospace text-uppercase small"><%=Common.getBahasaConfig("Uraian Laporan")%></th>
                                </tr>
                        </thead>
                        <tbody id="tabelTahunBody"></tbody>
                    </table>
                </div>
            </div>

        </div>
    </div>
</div>

<script>
    var urlSvc = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=akuntansi&s=_monitor_akunting_service';
    var activeTab = 'utama';

    $(document).ready(function() {
        if(typeof $.fn.datepicker !== 'undefined') $('.datepicker').datepicker({ format: 'yyyy-mm-dd', autoclose: true });
        
        // Generate Combobox Tahun (25 Tahun Terakhir)
        var curY = new Date().getFullYear();
        var optThn = '';
        for(var i=0; i<=25; i++) { optThn += '<option value="'+(curY-i)+'">'+(curY-i)+'</option>'; }
        $('#filterTahunBulan').html(optThn).val(curY);
        $('#filterTahunMulai').html(optThn).val(curY - 5);
        $('#filterTahunSampai').html(optThn).val(curY);

        // Tab Navigation Logic
        $('#akuntingTabs .nav-link').on('click', function() {
            $('#akuntingTabs .nav-link').removeClass('active text-primary').addClass('text-secondary');
            $(this).addClass('active text-primary').removeClass('text-secondary');
            
            activeTab = $(this).data('tab');
            $('.tab-pane-akunting').hide();
            $('#tabContent_' + activeTab).fadeIn();
            
            refreshTabAktif(); // Auto load saat pindah tab
        });

        // Trigger Load saat Buku Laporan Berganti
        $(document).on('change', '.chk-jenis-laporan', function() { refreshTabAktif(); });

        initFilterAwal();
    });

    function initFilterAwal() {
        $.ajax({
            url: urlSvc + "&action=init_filter", type: "GET", dataType: "json",
            success: function(res) {
                if(res.status === 'success') {
                    var skHtml = '<option value=""><%=Common.getBahasaConfig("Seluruh Unit Kerja / Konsolidasi")%></option>';
                    $.each(res.satuan_kerja, function(i, sk) { skHtml += '<option value="'+sk.id+'">'+sk.nama+'</option>'; });
                    $('#filterSatuanKerja').html(skHtml);

                    var jlHtml = '';
                    $.each(res.jenis_laporan, function(i, jl) {
                        var checked = jl.checked ? 'checked' : '';
                        jlHtml += '<div class="form-check form-check-inline">' +
                                    '<input class="form-check-input chk-jenis-laporan cursor-pointer" type="radio" name="jl_rad" id="jl_'+jl.id+'" value="'+jl.id+'" '+checked+'>' +
                                    '<label class="form-check-label cursor-pointer text-dark fw-medium" for="jl_'+jl.id+'">'+jl.nama+'</label>' +
                                  '</div>';
                    });
                    $('#filterJenisLaporanContainer').html(jlHtml);
                    refreshTabAktif();
                }
            }
        });
    }

    function refreshTabAktif() {
        var jlId = $('input[name="jl_rad"]:checked').val();
        if(!jlId) return;
        var jlNama = $('label[for="jl_'+jlId+'"]').text();
        $('#judulLaporanAktif').html('<i class="fas fa-book-open me-2 text-muted"></i> <%=Common.getBahasaConfig("Buku Laporan:")%> ' + jlNama);
        $('#laporanContainer').fadeIn();

        if(activeTab === 'utama') loadDataLaporanUtama();
        else if(activeTab === 'komparasi_bulan') loadKomparasiBulan();
        else if(activeTab === 'komparasi_tahun') loadKomparasiTahun();
    }

    function formatRp(val) {
        var num = parseFloat(val);
        if(isNaN(num) || num === 0) return '<span class="text-muted">0</span>';
        var isNeg = num < 0;
        var fmt = Math.abs(num).toLocaleString('id-ID', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
        return isNeg ? '<span class="text-danger fw-bold">(' + fmt + ')</span>' : '<span class="text-primary fw-medium">' + fmt + '</span>';
    }

 // ================== RENDER TAB 1: UTAMA ==================
    function loadDataLaporanUtama() {
        $('#tabelUtamaBody').html(getLoadingStr(5));
        $.post(urlSvc + "&action=get_data_laporan", getBaseParams(), function(res) {
            // Validasi jika JSON gagal / terdapat error
            if (!res || res.error) { 
                $('#tabelUtamaBody').html(getErrorStr(5, res ? res.error : '<%=Common.getBahasaConfigJS("Gagal membaca respons server.")%>')); 
                return; 
            }
            
            // Pastikan objek data tersedia sebelum membaca length
            if (res.data) {
                var tbody = res.data.length === 0 ? getEmptyStr(5) : renderTreeRows(res.data, 4, true);
                $('#tabelUtamaBody').html(tbody);
            }
        }, "json").fail(function(xhr, status, err) { // Penanganan jika response bukan JSON
            $('#tabelUtamaBody').html(getErrorStr(5, '<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan atau format data tidak valid.")%>'));
        });
    }

    // ================== RENDER TAB 2: BULAN ==================
    function loadKomparasiBulan() {
        $('#tabelBulanBody').html(getLoadingStr(13));
        
        var data = getBaseParams();
        data.tahun = $('#filterTahunBulan').val() || new Date().getFullYear();
        
        $.post(urlSvc + "&action=get_data_komparasi_bulan", data, function(res) {
            if (!res || res.error) { 
                $('#tabelBulanBody').html(getErrorStr(13, res ? res.error : '<%=Common.getBahasaConfigJS("Gagal membaca respons server.")%>')); 
                return; 
            }
            
            if (res.data) {
                var tbody = res.data.length === 0 ? getEmptyStr(13) : renderTreeRows(res.data, 12, false);
                $('#tabelBulanBody').html(tbody);
            }
        }, "json").fail(function(xhr, status, err) {
            $('#tabelBulanBody').html(getErrorStr(13, '<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan atau format data tidak valid.")%>'));
        });
    }

    // ================== RENDER TAB 3: TAHUN ==================
    function loadKomparasiTahun() {
        $('#tabelTahunBody').html(getLoadingStr(10));
        
        var data = getBaseParams();
        data.tahun_mulai = $('#filterTahunMulai').val() || (new Date().getFullYear() - 5);
        data.tahun_sampai = $('#filterTahunSampai').val() || new Date().getFullYear();
        
        $.post(urlSvc + "&action=get_data_komparasi_tahun", data, function(res) {
            if (!res || res.error) { 
                $('#tabelTahunBody').html(getErrorStr(10, res ? res.error : '<%=Common.getBahasaConfigJS("Gagal membaca respons server.")%>')); 
                return; 
            }
            
            if (res.data && res.kolom_tahun) {
                // Build Dynamic Headers
                var colCount = res.kolom_tahun.length;
                var thHtml = '<th width="30%" class="ps-4 py-3 font-monospace text-uppercase small"><i class="fas fa-sitemap me-2"></i> <%=Common.getBahasaConfig("Uraian Laporan")%></th>';
                $.each(res.kolom_tahun, function(i, y) { thHtml += '<th class="text-end py-3 font-monospace small">'+y+'</th>'; });
                $('#headKomparasiTahun').html(thHtml);
                
                var tbody = res.data.length === 0 ? getEmptyStr(colCount + 1) : renderTreeRows(res.data, colCount, false);
                $('#tabelTahunBody').html(tbody);
            }
        }, "json").fail(function(xhr, status, err) {
            $('#tabelTahunBody').html(getErrorStr(10, '<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan atau format data tidak valid.")%>'));
        });
    }

    // ================== HELPERS ==================
    function getBaseParams() {
        return {
            satuan_kerja_id: $('#filterSatuanKerja').val(),
            jenis_laporan_id: $('input[name="jl_rad"]:checked').val(),
            tanggal_mulai: $('#filterMulai').val(),
            tanggal_sampai: $('#filterSampai').val()
        };
    }

    function renderTreeRows(dataList, valCount, isStickyLaporan) {
        var tbody = '';
        var m = $('#filterMulai').val(), s = $('#filterSampai').val();
        
        $.each(dataList, function(i, row) {
            var pl = (row.level * 25) + 20;
            var fw = row.level === 0 ? "fw-bolder text-primary" : (row.is_leaf ? "text-dark" : "fw-bold text-dark");
            var icon = row.is_leaf ? '<i class="far fa-file-alt text-secondary me-2 ms-1"></i>' : '<i class="fas fa-folder-open text-warning me-2 ms-1"></i>';
            var jsClick = "bukaDetailTransaksi('"+row.id_akun+"', '"+m+"', '"+s+"')";
            
            var stickyClass = isStickyLaporan ? "" : " position-sticky start-0 bg-white z-index-1";
            
            tbody += '<tr class="border-bottom border-light">';
            tbody += '<td style="padding-left: '+pl+'px;" class="py-2 '+fw+stickyClass+'">' + 
                     icon + '<a href="javascript:void(0)" class="text-decoration-none '+fw+'" onclick="'+jsClick+'">' + row.uraian + '</a></td>';
            
            $.each(row.values, function(idx, val) {
                var lastClass = (idx === valCount-1 && isStickyLaporan) ? " pe-4" : "";
                tbody += '<td class="text-end py-2'+lastClass+'"><a href="javascript:void(0)" class="text-decoration-none" onclick="'+jsClick+'">' + formatRp(val) + '</a></td>';
            });
            tbody += '</tr>';
        });
        return tbody;
    }

    function getLoadingStr(span) { return '<tr><td colspan="'+span+'" class="text-center py-5 text-primary"><i class="fas fa-circle-notch fa-spin fa-3x mb-3"></i><br><span class="fw-medium"><%=Common.getBahasaConfig("Memuat data laporan...")%></span></td></tr>'; }
    function getEmptyStr(span) { return '<tr><td colspan="'+span+'" class="text-center text-muted py-5"><i class="fas fa-box-open fa-3x mb-3 text-light"></i><br><%=Common.getBahasaConfig("Tidak terdapat data transaksi.")%></td></tr>'; }
    function getErrorStr(span, msg) { return '<tr><td colspan="'+span+'" class="text-center text-danger py-4 fw-bold"><i class="fas fa-exclamation-triangle me-2"></i>'+msg+'</td></tr>'; }

    function bukaDetailTransaksi(idAkun, m, s) {
        var urlZul = '<%=Common.ROOT%>/pages/master/akunting/grup_transaksi.zul?akun=' + idAkun + '&mulai=' + m + '&sampai=' + s;
        if(typeof Common !== 'undefined' && typeof Common.displayWindow === 'function') {
            Common.displayWindow(urlZul, true, "95%", "95%", null, "", false);
        } else { window.open(urlZul, '_blank', 'width=1100,height=750'); }
    }
</script>