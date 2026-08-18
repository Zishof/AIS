<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Date"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Konfigurasi"%>

<%
    // =================================================================================
    // BLOK EKSEKUSI AJAX: REFRESH CACHE MAHASISWA
    // Akan dieksekusi secara asinkron sebelum halaman di-render ulang
    // =================================================================================
    if ("do_refresh_cache".equals(request.getParameter("ajax_action_tombol"))) {
        out.clear();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            Tbmuser userRef = Common.getCurrentUser(request);
            if (userRef != null && userRef.getMahasiswa() != null) {
                Mahasiswa mhsRef = userRef.getMahasiswa();
                Session sessRef = HibernateUtil.openSession();
                
                try {
                    // Eksekusi semua fungsi re-init sesuai instruksi
                    mhsRef.reInitBimbingan(sessRef);
                    mhsRef.reInitDetailperkuliahan(sessRef);
                    
                    // PERBAIKAN: Konversi java.util.Date menjadi java.util.Calendar
                    Calendar mulai = null;
                    if (mhsRef.getTanggalMasuk() != null) {
                        mulai = Calendar.getInstance();
                        mulai.setTime(mhsRef.getTanggalMasuk());
                    }
                    
                    Calendar sampai = Calendar.getInstance();
                    if (mhsRef.getTanggalLulus() != null) {
                        sampai.setTime(mhsRef.getTanggalLulus());
                    }
                    
                    mhsRef.reInitPertemuan(sessRef, mulai, sampai);
                    mhsRef.reInitChecklistBaruPenilaianDosenOlehMahasiswa(sessRef);
                    mhsRef.reInitFormulirKegiatanPeserta(sessRef);
                    mhsRef.reInitHasilUjianMahasiswa(sessRef);
                    mhsRef.reInitIsiAngketParameterUmum(sessRef);
                    mhsRef.reInitKegiatan(sessRef);
                    mhsRef.reInitKegiatanKemahasiswaanPunyaMahasiswa(sessRef);
                    mhsRef.reInitKkn(sessRef);
                    mhsRef.reInitKonsultasi(sessRef);
                    mhsRef.reInitKrs(sessRef);
                    mhsRef.reInitOrganisasiIntraKampusPunyaMahasiswa(sessRef);
                    mhsRef.reInitPendaftaranWisuda(sessRef);
                    mhsRef.reInitPenghargaanMahasiswa(sessRef);
                    mhsRef.reInitPkl(sessRef);
                    mhsRef.reInitPrestasiMahasiswa(sessRef);
                    mhsRef.reInitSkripsi(sessRef);
                } finally {
                    if(sessRef != null && sessRef.isOpen()) { try { sessRef.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/mahasiswa/tombol.jsp:61");} }
                }
            }
            out.print("{\"status\":\"success\"}");
        } catch (Exception e) {
            out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
        }
        out.flush();
        return; // Hentikan eksekusi JSP agar tidak merender HTML
    }
%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    if (tbmuser != null && tbmuser.getMahasiswa() != null) {
        Mahasiswa mhs = tbmuser.getMahasiswa();
        
        // 1. Buat Base URL tunggal untuk laporan
        // Kita hanya perlu ID Mahasiswa dan Path dasarnya
        String pLaporan = "laporan%2Fgeneral_mahasiswa"; 
        try { pLaporan = URLEncoder.encode("laporan/general_mahasiswa", "UTF-8"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/mahasiswa/tombol.jsp:82");}
        
        String mhsId = mhs.getId().toString();
        
        // URL dasar yang akan dipakai oleh JavaScript
        String baseReportUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=" + pLaporan + "&s=index&mahasiswa=" + mhsId;
        
        // URL Service Modal Inject
        String isiKrsUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=krs&s=krs&mahasiswa=" + mhsId;
        String isiKrsPaketUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=krs&s=krs_paket&mahasiswa=" + mhsId;
        String isiPenilaianUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=penilaian&s=penilaian&mahasiswa=" + mhsId;
        
        // ID Unik
        String rnd = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Hormati konfigurasi "Tampilkan tombol cetak kartu UTS/UAS" (KonfigurasiNewAction).
        // Bila di-set Tidak Aktif, tombol cetak kartu ujian TIDAK ditampilkan di dashboard
        // mahasiswa — sebelumnya tombol di JSP ini selalu tampil (mengabaikan konfigurasi),
        // sehingga mahasiswa tetap bisa mencetak walau admin sudah menonaktifkan.
        boolean tampilCetakUts = Konfigurasi.AKTIF.equals(
                Common.getKonfigurasi("tampilkan_tombol_cetak_kartu_uts", Konfigurasi.AKTIF).getNilai());
        boolean tampilCetakUas = Konfigurasi.AKTIF.equals(
                Common.getKonfigurasi("tampilkan_tombol_cetak_kartu_uas", Konfigurasi.AKTIF).getNilai());
%>

<style>
    /* Penyesuaian Antarmuka Tombol Pintasan */
    .shortcut-btn-wrapper .btn {
        border-radius: 8px;
        transition: all 0.2s ease-in-out;
        font-weight: 500;
        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
    }
    .shortcut-btn-wrapper .btn:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 8px rgba(0,0,0,0.1);
    }
    .shortcut-btn-wrapper .vr {
        width: 2px;
        background-color: #e9ecef;
        opacity: 1;
    }
</style>

<!-- Panggil Library SweetAlert (Jika belum ada di parent) -->
<script data-cfasync="false" src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>

<div class="card shadow-sm border-0 rounded-4 mb-4 animate__animated animate__fadeInUp">
    <div class="card-header bg-white border-bottom-0 pt-4 pb-2 rounded-top-4">
        <h5 class="mb-0 text-dark fw-bold">
            <i class="fas fa-link text-primary me-2"></i><%=Common.getBahasaConfig("Tombol Pintasan Akademik")%>
        </h5>
    </div>

    <div class="card-body">
        <div class="d-flex flex-wrap gap-2 shortcut-btn-wrapper align-items-center">
            
            <!-- Kelompok Tombol Aksi Akademik -->
            <button class="btn btn-light border btn-sm px-3" type="button" onclick="prosesRefreshDataMhs_<%=rnd%>()" id="btnRefreshMhs_<%=rnd%>">
                <i class="fas fa-sync-alt text-secondary me-1"></i> <%=Common.getBahasaConfig("Segarkan Data") %>
            </button>
            
            <!-- Tombol KRS Reguler Terintegrasi dengan Inject HTML Modal -->
            <button class="btn btn-outline-primary btn-sm px-3" type="button" onclick="bukaModalKrsLayarPenuh_<%=rnd%>()">
                <i class="fas fa-book-open me-1"></i> <%=Common.getBahasaConfig("Pengambilan KRS") %>
            </button>
            
            <!-- Tombol KRS PAKET Terintegrasi dengan Inject HTML Modal -->
            <button class="btn btn-outline-success btn-sm px-3" type="button" onclick="bukaModalKrsPaketLayarPenuh_<%=rnd%>()">
                <i class="fas fa-boxes me-1"></i> <%=Common.getBahasaConfig("KRS Paket") %>
            </button>
            
            <!-- Tombol PENILAIAN Terintegrasi dengan Inject HTML Modal -->
            <button class="btn btn-outline-info btn-sm px-3" type="button" onclick="bukaModalPenilaianLayarPenuh_<%=rnd%>()">
                <i class="far fa-check-circle me-1"></i> <%=Common.getBahasaConfig("Daftar Nilai") %>
            </button>
            
            <button class="btn btn-outline-info btn-sm px-3" type="button">
                <i class="fas fa-fingerprint me-1"></i> <%=Common.getBahasaConfig("Riwayat Kehadiran") %>
            </button>
            
            <button class="btn btn-outline-info btn-sm px-3" type="button">
                <i class="fas fa-clipboard-list me-1"></i> <%=Common.getBahasaConfig("Aktivitas Perkuliahan") %>
            </button>

            <!-- Garis Pemisah Visual -->
            <div class="vr mx-2 d-none d-md-block" style="height: 30px;"></div>

            <!-- Kelompok Tombol Cetak Laporan -->
            <button class="btn btn-primary btn-sm px-3" type="button" 
                onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Cetak KRS")%>', 'Cetak_KRS_Mahasiswa', 'Kartu+Rencana+Studi')">
                <i class="fas fa-print me-1"></i> <%=Common.getBahasaConfig("Cetak KRS") %>
            </button>

            <button class="btn btn-primary btn-sm px-3" type="button" 
                onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Cetak KHS")%>', 'Kartu_Hasil_Studi', 'Kartu+Hasil+Studi')">
                <i class="fas fa-print me-1"></i> <%=Common.getBahasaConfig("Cetak KHS") %>
            </button>

            <% if (tampilCetakUts) { %>
            <button class="btn btn-primary btn-sm px-3" type="button"
                onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Cetak Kartu UTS")%>', 'Cetak_KUTS_Mahasiswa', 'Kartu+UTS')">
                <i class="fas fa-print me-1"></i> <%=Common.getBahasaConfig("Cetak Kartu UTS") %>
            </button>
            <% } %>

            <% if (tampilCetakUas) { %>
            <button class="btn btn-primary btn-sm px-3" type="button"
                onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Cetak Kartu UAS")%>', 'Cetak_KUAS_Mahasiswa', 'Kartu+UAS')">
                <i class="fas fa-print me-1"></i> <%=Common.getBahasaConfig("Cetak Kartu UAS") %>
            </button>
            <% } %>

            <button class="btn btn-primary btn-sm px-3" type="button" 
                onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Cetak Transkrip Akademik")%>', 'Transkrip_Akademik', 'Transkrip+Akademik')">
                <i class="fas fa-file-alt me-1"></i> <%=Common.getBahasaConfig("Cetak Transkrip") %>
            </button>

            <button class="btn btn-primary btn-sm px-3" type="button" 
                onclick="bukaModalLaporan('<%=Common.getBahasaConfigJS("Cetak Transkrip IPK")%>', 'Rekaman_Nilai', 'Transkrip+IPK')">
                <i class="fas fa-chart-line me-1"></i> <%=Common.getBahasaConfig("Cetak IPK") %>
            </button>

            <button class="btn btn-dark btn-sm px-3" type="button">
                <i class="fas fa-certificate me-1"></i> <%=Common.getBahasaConfig("Cetak SKPI") %>
            </button>

        </div>
    </div>
</div>

<script>
    /**
     * Fungsi Asinkron: Refresh Cache / Data Mahasiswa
     */
    function prosesRefreshDataMhs_<%=rnd%>() {
        // Tampilkan Loading Indikator Interaktif
        Swal.fire({
            title: '<%=Common.getBahasaConfigJS("Menyegarkan Data")%>',
            html: '<%=Common.getBahasaConfigJS("Sistem sedang memperbarui dan menyinkronkan ulang seluruh riwayat akademik Anda. Mohon tunggu sejenak...")%>',
            allowOutsideClick: false,
            allowEscapeKey: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        // Tembak URL saat ini dengan tambahan parameter instruksi ke blok backend
        var targetUrl = window.location.href;
        
        $.ajax({
            url: targetUrl,
            type: "POST",
            data: { ajax_action_tombol: 'do_refresh_cache' },
            success: function(response) {
                // Beri jeda visual 500ms agar user merasa proses benar-benar berjalan
                setTimeout(function() {
                    Swal.fire({
                        icon: 'success',
                        title: '<%=Common.getBahasaConfigJS("Selesai")%>',
                        text: '<%=Common.getBahasaConfigJS("Sinkronisasi data akademik berhasil diselesaikan.")%>',
                        timer: 1500,
                        showConfirmButton: false
                    }).then(() => {
                        location.reload();
                    });
                }, 500);
            },
            error: function() {
                // Fallback jika terjadi galat koneksi, tetap force reload
                location.reload();
            }
        });
    }

    /**
     * Fungsi untuk memanggil Popup KRS secara khusus
     */
    function bukaModalKrsLayarPenuh_<%=rnd%>() {
        var judulModal = '<%=Common.getBahasaConfigJS("Papan Pengambilan Kartu Rencana Studi (KRS)")%>';
        var urlIsiKrs = '<%=isiKrsUrl%>';
        var modalIdKrs = 'modalKrsFull_<%=rnd%>';
        
        loadModalContentCustomSimpan(judulModal, urlIsiKrs, '100%', 'hidden', '', modalIdKrs);
    }
    
    /**
     * Fungsi untuk memanggil Popup KRS PAKET secara khusus
     */
    function bukaModalKrsPaketLayarPenuh_<%=rnd%>() {
        var judulModal = '<%=Common.getBahasaConfigJS("Papan Pengambilan KRS Paket")%>';
        var urlIsiKrsPaket = '<%=isiKrsPaketUrl%>';
        var modalIdKrsPaket = 'modalKrsPaketFull_<%=rnd%>';
        
        loadModalContentCustomSimpan(judulModal, urlIsiKrsPaket, '100%', 'hidden', '', modalIdKrsPaket);
    }

    /**
     * Fungsi untuk memanggil Popup PENILAIAN secara khusus
     */
    function bukaModalPenilaianLayarPenuh_<%=rnd%>() {
        var judulModal = '<%=Common.getBahasaConfigJS("Daftar Penilaian Akademik")%>';
        var urlIsiPenilaian = '<%=isiPenilaianUrl%>';
        var modalIdPenilaian = 'modalPenilaianFull_<%=rnd%>';
        
        loadModalContentCustomSimpan(judulModal, urlIsiPenilaian, '100%', 'hidden', '', modalIdPenilaian);
    }

    /**
     * Fungsi Helper untuk membuka Modal Laporan.
     */
    function bukaModalLaporan(labelTombol, namaFile, judulLaporan) {
        if (typeof window.variable === 'undefined') window.variable = 0; 
        var cm = 'contentModalLaporan_' + (++window.variable);
        
        var baseUrl = '<%=baseReportUrl%>'; 
        var fullUrl = baseUrl + '&fileLaporan=' + namaFile + '&judulLaporan=' + judulLaporan + '&contentModal=' + cm;

        // Memanggil global script yang sama untuk merender modal Laporan
        loadModalContentCustomSimpan(labelTombol, fullUrl, '85%', null, '', cm);
    }
</script>

<%
    }
%>