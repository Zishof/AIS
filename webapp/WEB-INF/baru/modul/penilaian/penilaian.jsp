<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<% 
    String rnd = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8); 
    
    ais.database.model.Tbmuser tbmuser = ais.common.Common.getCurrentUser(request);
    Long mhsId = (tbmuser != null && tbmuser.getMahasiswa() != null) ? tbmuser.getMahasiswa().getId() : 0L;
    
    String pLaporan = "laporan%2Fgeneral_mahasiswa"; 
    try { pLaporan = java.net.URLEncoder.encode("laporan/general_mahasiswa", "UTF-8"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/penilaian/penilaian.jsp:10");}
    String baseReportUrl = ais.common.Common.ROOT + "/baru?hanya_tampil_jsp=true&p=" + pLaporan + "&s=index&mahasiswa=" + mhsId;
%>

<link rel="stylesheet" href="https://cdn.datatables.net/1.10.24/css/dataTables.bootstrap4.min.css">

<style>
    #wrapPenilaian_<%=rnd%> .hover-elevate { transition: transform 0.2s ease, box-shadow 0.2s ease; }
    #wrapPenilaian_<%=rnd%> .hover-elevate:hover { transform: translateY(-3px); box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important; }
    .table-nilai th { background-color: #f8f9fa; text-transform: uppercase; font-size: 0.85rem; letter-spacing: 0.5px; }
    .table-nilai td { vertical-align: middle; font-size: 0.95rem; }
    .btn-action-penilaian { min-width: 140px; }
</style>

<div class="container-fluid py-4" id="wrapPenilaian_<%=rnd%>">
    
    <div class="d-flex align-items-center mb-4 p-4 bg-white shadow-sm rounded-4 border-start border-5 border-info">
        <div class="bg-info text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 55px; height: 55px; flex-shrink: 0;">
            <i class="fas fa-award fs-3"></i>
        </div>
        <div>
            <h4 class="mb-1 fw-bold text-dark"><%= Common.getBahasaConfig("Daftar Penilaian Akademik") %></h4>
            <p class="text-muted mb-0 small"><%= Common.getBahasaConfig("Modul untuk memantau rincian nilai matakuliah yang telah dipublikasikan oleh dosen serta mencetak Kartu Hasil Studi (KHS).") %></p>
        </div>
    </div>

    <div class="row mb-4">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4">
                <div class="card-body p-3 d-flex flex-column flex-md-row justify-content-between align-items-center bg-light rounded-4 gap-3">
                    <div class="d-flex align-items-center w-100" style="max-width: 500px;">
                        <span class="fw-bold text-secondary me-3 text-nowrap"><i class="fas fa-filter me-1"></i> <%= Common.getBahasaConfig("Periode:") %></span>
                        <select id="filterSmtNilai_<%=rnd%>" class="form-select rounded-pill shadow-sm border-info">
                            <option value=""><%= Common.getBahasaConfig("Semua Semester & Tahun Ajaran") %></option>
                        </select>
                    </div>
                    
                    <div class="d-flex flex-wrap gap-2">
                        <button class="btn btn-outline-info rounded-pill shadow-sm px-4 fw-bold hover-elevate btn-action-penilaian" type="button" 
                            onclick="bukaModalLaporan_<%=rnd%>('<%=Common.getBahasaConfigJS("Cetak IPK")%>', 'Rekaman_Nilai', 'Transkrip+IPK')">
                            <i class="fas fa-chart-line me-2"></i> <%=Common.getBahasaConfig("Cetak IPK") %>
                        </button>

                        <button class="btn btn-outline-primary rounded-pill shadow-sm px-4 fw-bold hover-elevate btn-action-penilaian" type="button" 
                            onclick="bukaModalLaporan_<%=rnd%>('<%=Common.getBahasaConfigJS("Cetak Transkrip")%>', 'Transkrip_Akademik', 'Transkrip+Akademik')">
                            <i class="fas fa-file-alt me-2"></i> <%=Common.getBahasaConfig("Cetak Transkrip") %>
                        </button>

                        <button class="btn btn-primary rounded-pill shadow-sm px-4 fw-bold hover-elevate btn-action-penilaian" type="button" 
                            onclick="bukaModalLaporanKHS_<%=rnd%>('<%=Common.getBahasaConfigJS("Cetak KHS")%>', 'Kartu_Hasil_Studi', 'Kartu+Hasil+Studi')">
                            <i class="fas fa-print me-2"></i> <%=Common.getBahasaConfig("Cetak KHS") %>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="row mb-4">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4 border-top border-4 border-success">
                <div class="card-header bg-white pt-4 pb-2 border-bottom-0">
                    <h5 class="fw-bold text-success mb-0 d-flex align-items-center">
                        <i class="fas fa-check-circle me-2"></i> <%= Common.getBahasaConfig("Matakuliah Telah Dinilai") %>
                    </h5>
                    <small class="text-muted"><%= Common.getBahasaConfig("Daftar matakuliah yang nilainya sudah diverifikasi dan dipublikasikan.") %></small>
                </div>

                <div class="card-body p-4 bg-white table-responsive rounded-bottom-4">
                    <table id="tabelSudahDinilai_<%=rnd%>" class="table table-hover table-bordered table-nilai align-middle w-100 mb-0">
                        <thead class="table-light">
                            <tr>
                                <th width="40%"><%= Common.getBahasaConfig("Mata Kuliah") %></th>
                                <th width="15%" class="text-center"><%= Common.getBahasaConfig("Semester") %></th>
                                <th width="15%" class="text-center"><%= Common.getBahasaConfig("SKS") %></th>
                                <th width="15%" class="text-center"><%= Common.getBahasaConfig("Nilai Angka") %></th>
                                <th width="15%" class="text-center"><%= Common.getBahasaConfig("Nilai Huruf") %></th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <div class="card shadow-sm border-0 rounded-4 border-top border-4 border-warning">
                <div class="card-header bg-white pt-4 pb-2 border-bottom-0">
                    <h5 class="fw-bold text-warning text-dark mb-0 d-flex align-items-center">
                        <i class="fas fa-hourglass-half me-2"></i> <%= Common.getBahasaConfig("Menunggu Penilaian") %>
                    </h5>
                    <small class="text-muted"><%= Common.getBahasaConfig("Daftar matakuliah yang sedang berjalan atau nilainya belum dipublikasikan oleh Dosen Pengampu.") %></small>
                </div>

                <div class="card-body p-4 bg-white table-responsive rounded-bottom-4">
                    <table id="tabelBelumDinilai_<%=rnd%>" class="table table-hover table-bordered table-nilai align-middle w-100 mb-0">
                        <thead class="table-light">
                            <tr>
                                <th width="40%"><%= Common.getBahasaConfig("Mata Kuliah") %></th>
                                <th width="15%" class="text-center"><%= Common.getBahasaConfig("Semester") %></th>
                                <th width="15%" class="text-center"><%= Common.getBahasaConfig("SKS") %></th>
                                <th width="15%" class="text-center"><%= Common.getBahasaConfig("Status KRS") %></th>
                                <th width="15%" class="text-center"><%= Common.getBahasaConfig("Keterangan") %></th>
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
    /**
     * Fungsi Helper Laporan Kumulatif (Transkrip/IPK)
     */
    function bukaModalLaporan_<%=rnd%>(labelTombol, namaFile, judulLaporan) {
        if (typeof window.variable === 'undefined') window.variable = 0; 
        var cm = 'contentModalLaporan_' + (++window.variable);
        
        var baseUrl = '<%=baseReportUrl%>'; 
        var $selectedOpt = $('#filterSmtNilai_<%=rnd%>').find('option:selected');
        var selectedPeriode = $selectedOpt.val(); 
        
        var smt = $selectedOpt.attr('data-smt') || '';
        var sp = $selectedOpt.attr('data-sp') || '';
        
        var fullUrl = baseUrl + '&fileLaporan=' + namaFile + '&judulLaporan=' + judulLaporan + '&contentModal=' + cm;
        if (selectedPeriode) { fullUrl += '&periode=' + selectedPeriode + '&smt=' + smt + '&sp=' + sp; }

        if (typeof loadModalContentCustomSimpan === "function") {
            loadModalContentCustomSimpan(labelTombol, fullUrl, '85%', null, '', cm);
        }
    }

    /**
     * Fungsi Khusus Cetak KHS (Periodik)
     */
    function bukaModalLaporanKHS_<%=rnd%>(labelTombol, namaFile, judulLaporan) {
        if (typeof window.variable === 'undefined') window.variable = 0; 
        var cm = 'contentModalLaporan_' + (++window.variable);
        
        var baseUrl = '<%=baseReportUrl%>'; 
        var $selectedOpt = $('#filterSmtNilai_<%=rnd%>').find('option:selected');
        var selectedPeriode = $selectedOpt.val(); 
        
        var smt = $selectedOpt.attr('data-smt') || '';
        var sp = $selectedOpt.attr('data-sp') || '';
        
        var fullUrl = baseUrl + '&fileLaporan=' + namaFile + '&judulLaporan=' + judulLaporan + '&contentModal=' + cm;
        if (selectedPeriode) { fullUrl += '&periode=' + selectedPeriode + '&smt=' + smt + '&sp=' + sp; }

        if (typeof loadModalContentCustomSimpan === "function") {
            loadModalContentCustomSimpan(labelTombol, fullUrl, '85%', null, '', cm);
        }
    }

    var cekIntervalNilai = setInterval(function() {
        if (typeof $.fn.DataTable !== 'undefined') {
            clearInterval(cekIntervalNilai);
            initPenilaianApp_<%=rnd%>();
        } else {
            $.getScript("https://cdn.datatables.net/1.10.24/js/jquery.dataTables.min.js", function() {
                $.getScript("https://cdn.datatables.net/1.10.24/js/dataTables.bootstrap4.min.js");
            });
        }
    }, 100);

    function initPenilaianApp_<%=rnd%>() {
        var urlServiceNilai = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=penilaian&s=_penilaian_service';
        var dtSudah = null; var dtBelum = null;

        loadFilterDropdown();

        function loadFilterDropdown() {
            $.ajax({
                url: urlServiceNilai + "&action=get_filter_smt", type: "GET", dataType: "json",
                success: function(res) {
                    var $sel = $('#filterSmtNilai_<%=rnd%>');
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
                    initDataTables();
                    loadDataNilai();
                }
            });
        }

        $('#filterSmtNilai_<%=rnd%>').on('change', function() {
            loadDataNilai();
        });

        function initDataTables() {
            dtSudah = $('#tabelSudahDinilai_<%=rnd%>').DataTable({
                "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
                "pageLength": 25, "ordering": false,
                "columns": [
                    { "data": "matakuliah" }, { "data": "semester", "className": "text-center fw-bold" },
                    { "data": "sks", "className": "text-center" }, { "data": "nilai_angka", "className": "text-center" },
                    { "data": "nilai_huruf", "className": "text-center" }
                ]
            });

            dtBelum = $('#tabelBelumDinilai_<%=rnd%>').DataTable({
                "language": { "url": "//cdn.datatables.net/plug-ins/1.10.24/i18n/Indonesian.json" },
                "pageLength": 25, "ordering": false,
                "columns": [
                    { "data": "matakuliah" }, { "data": "semester", "className": "text-center fw-bold" },
                    { "data": "sks", "className": "text-center" }, { "data": "status_krs", "className": "text-center" },
                    { "data": "keterangan", "className": "text-center" }
                ]
            });
        }

        function loadDataNilai() {
            var selectedSmt = $('#filterSmtNilai_<%=rnd%>').val();
            if(dtSudah) { dtSudah.clear().draw(); }
            if(dtBelum) { dtBelum.clear().draw(); }

            $.ajax({
                url: urlServiceNilai + "&action=get_nilai",
                type: "POST", dataType: "json", data: { filter_id_smt: selectedSmt },
                success: function(response) {
                    if (response.data_sudah && dtSudah) dtSudah.rows.add(response.data_sudah).draw();
                    if (response.data_belum && dtBelum) dtBelum.rows.add(response.data_belum).draw();
                }
            });
        }
    }
</script>