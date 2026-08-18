<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.Ujian"%>
<%@page import="ais.database.model.PenjelasanBankSoal"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
// =================================================================================
// 1. PENGECEKAN KEAMANAN (SESI PENGGUNA)
// =================================================================================
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    return; 
}

String random = Common.getGeneratedBarCode(7);
String ppuParam = request.getParameter("ppu");
String actionParam = request.getParameter("action"); 

if (ppuParam == null || ppuParam.trim().isEmpty()) {
    out.println("<div class='alert alert-danger text-center mt-4 shadow-sm'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Parameter sesi ujian tidak valid atau tidak ditemukan di dalam sistem.") + "</div>");
    return;
}

// =================================================================================
// 2. MENGAMBIL DATA UJIAN
// =================================================================================
PertemuanPunyaUjian pertemuanPunyaUjian = null;
boolean bolehKelolaSoal = false; // Hak kelola dihitung saat sesi masih terbuka (hindari LazyInit)
Session mySession = null;
try {
    mySession = HibernateUtil.openSession();
    Long idData = Long.parseLong(ppuParam.trim());
    pertemuanPunyaUjian = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, idData);
    if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getPertemuan() != null) {
        bolehKelolaSoal = pertemuanPunyaUjian.getPertemuan().bolehUbahAbsenSaja(tbmuser);
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/daftar_soal.jsp:44");
} finally {
    if (mySession != null) {
        if (mySession.isConnected()) try { mySession.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/daftar_soal.jsp:47");}
        if (mySession.isOpen()) ais.common.ElearningSessionUtil.closeQuietly(mySession);
    }
}

if (pertemuanPunyaUjian == null || pertemuanPunyaUjian.getUjian() == null) {
    out.println("<div class='alert alert-danger text-center mt-4 shadow-sm'><i class='fas fa-info-circle me-2'></i>" + Common.getBahasaConfig("Data informasi ujian tidak ditemukan di dalam sistem. Pastikan pengaturan ujian telah dilakukan dengan benar.") + "</div>");
    return;
}

// Guard peran: halaman kelola soal (Ubah Ujian / Tambah / Ubah / Hapus soal) hanya untuk
// pengajar & admin. Peserta didik (mahasiswa/siswa/calon) yang mengakses via URL langsung ditolak.
if (!bolehKelolaSoal) {
    out.println("<div class='alert alert-warning text-center mt-4 shadow-sm'><i class='fas fa-lock me-2'></i>" + Common.getBahasaConfig("Anda tidak memiliki hak akses untuk mengelola soal ujian ini.") + "</div>");
    return;
}

// =================================================================================
// 3. EKSTRAKSI INFORMASI UJIAN
// =================================================================================
String jenisKoreksi = pertemuanPunyaUjian.getUjian().getJenisKoreksi() != null ? pertemuanPunyaUjian.getUjian().getJenisKoreksi() : PenjelasanBankSoal.KOREKSI_OTOMATIS;
String labelJenisKoreksi = PenjelasanBankSoal.KOREKSI_MANUAL.equals(jenisKoreksi) ? 
        Common.getBahasaConfig("Esai / Uraian Bebas (Koreksi Manual)") : 
        Common.getBahasaConfig("Pilihan Ganda (Koreksi Otomatis)");
String namaUjian = pertemuanPunyaUjian.getUjian().getNama() != null ? pertemuanPunyaUjian.getUjian().getNama() : "-";
String keterangan = pertemuanPunyaUjian.getKeterangan() != null && !pertemuanPunyaUjian.getKeterangan().trim().isEmpty() ?
        pertemuanPunyaUjian.getKeterangan() : Common.getBahasaConfig("Tidak ada deskripsi atau keterangan tambahan untuk ujian ini.");

int jmlSoal = pertemuanPunyaUjian.getJmlDitampilkan() != null ? pertemuanPunyaUjian.getJmlDitampilkan() : 0;
int jmlPercobaan = pertemuanPunyaUjian.getJumlahBolehIkut() != null ? pertemuanPunyaUjian.getJumlahBolehIkut() : 0;

java.util.Date lamaPengerjaan = pertemuanPunyaUjian.getLama();
String durasi = lamaPengerjaan != null ? Common.timeFormat1.get().format(lamaPengerjaan) : "-";

boolean isDibatasiWaktu = pertemuanPunyaUjian.getDibatasiWaktu() != null ? pertemuanPunyaUjian.getDibatasiWaktu() : true;
java.util.Date tglMulai = pertemuanPunyaUjian.getMulaiUjian();
java.util.Date tglSelesai = pertemuanPunyaUjian.getSampaiUjian();

String strMulai = tglMulai != null ? Common.dateFormat5.get().format(tglMulai) : "-";
String strSelesai = (tglSelesai != null && isDibatasiWaktu) ? Common.dateFormat5.get().format(tglSelesai) : Common.getBahasaConfig("Tidak Ada Batas Waktu");
boolean isRandom = pertemuanPunyaUjian.getRandom() != null ? pertemuanPunyaUjian.getRandom() : true;
boolean lihatNilai = pertemuanPunyaUjian.getLihatNilaiSetelahUjian() != null ? pertemuanPunyaUjian.getLihatNilaiSetelahUjian() : false;
boolean lihatJawaban = pertemuanPunyaUjian.getLihatJawabanSetelahUjian() != null ? pertemuanPunyaUjian.getLihatJawabanSetelahUjian() : false;
boolean blokirTangkapLayar = pertemuanPunyaUjian.getAntiCurangBlokirTangkapLayar() != null ? pertemuanPunyaUjian.getAntiCurangBlokirTangkapLayar() : true;
boolean larangMultiDevice = pertemuanPunyaUjian.getAntiCurangLarangMultiDevice() != null ? pertemuanPunyaUjian.getAntiCurangLarangMultiDevice() : true;
Double lokasiLat = pertemuanPunyaUjian.getAntiCurangLokasiLatitude();
Double lokasiLon = pertemuanPunyaUjian.getAntiCurangLokasiLongitude();
Integer lokasiRadius = pertemuanPunyaUjian.getAntiCurangLokasiRadius();

String linkAmbilData = Common.ROOT+"/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_ambil_daftar_soal";

// Konversi tanggal untuk nilai default form edit (datetime-local HTML5)
SimpleDateFormat sdfDatetimeLocal = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
String valTglMulai = tglMulai != null ? sdfDatetimeLocal.format(tglMulai) : "";
String valTglSelesai = tglSelesai != null ? sdfDatetimeLocal.format(tglSelesai) : "";
String valDurasi = lamaPengerjaan != null ? sdfTime.format(lamaPengerjaan) : "";

// =================================================================================
// BLOK RENDER PARSIAL (AJAX RELOAD KHUSUS INFO UJIAN)
// =================================================================================
if ("info".equals(actionParam)) {
%>
    <div class="row mb-4">
        <div class="col-12">
            <small class="text-muted text-uppercase fw-bold"><%= Common.getBahasaConfig("Mata Ujian / Tugas") %></small>
            <h4 class="fw-bold text-dark mt-1"><%= namaUjian %></h4>
            <p class="text-secondary mb-0"><%= keterangan %></p>
        </div>
    </div>
    
    <div class="row g-4 border-top border-bottom py-3 mb-4 bg-light rounded-2">
        <div class="col-md-6 col-lg-3 border-end">
            <div class="d-flex align-items-center text-muted mb-1">
                <i class="far fa-check-circle me-2 text-primary"></i>
                <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Jenis Evaluasi") %></small>
            </div>
            <span class="fw-bold text-dark"><%= labelJenisKoreksi %></span>
        </div>
        <div class="col-md-6 col-lg-2 border-end">
            <div class="d-flex align-items-center text-muted mb-1">
                <i class="far fa-list-alt me-2 text-primary"></i>
                <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Kapasitas Soal") %></small>
            </div>
            <span class="fw-bold text-dark"><%= jmlSoal %> <%= Common.getBahasaConfig("Butir") %></span>
        </div>
        <div class="col-md-6 col-lg-2 border-end">
            <div class="d-flex align-items-center text-muted mb-1">
                <i class="fas fa-arrow-circle-right me-2 text-primary"></i>
                <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Batas Percobaan") %></small>
            </div>
            <span class="fw-bold text-dark"><%= jmlPercobaan %> <%= Common.getBahasaConfig("Kali") %></span>
        </div>
        <div class="col-md-6 col-lg-2 border-end">
            <div class="d-flex align-items-center text-muted mb-1">
                <i class="fas fa-stopwatch me-2 text-primary"></i>
                <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Durasi Waktu") %></small>
            </div>
            <span class="fw-bold text-dark"><%= durasi %></span>
        </div>
        <div class="col-md-12 col-lg-3">
            <div class="d-flex align-items-center text-muted mb-1">
                <i class="far fa-calendar-alt me-2 text-primary"></i>
                <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Periode Pelaksanaan") %></small>
            </div>
            <div class="d-flex flex-column">
                <span class="fw-bold text-success" style="font-size: 0.85rem;"><i class="far fa-play-circle me-1"></i> <%= strMulai %></span>
                <span class="fw-bold text-danger" style="font-size: 0.85rem;"><i class="far fa-stop-circle me-1"></i> <%= strSelesai %></span>
            </div>
        </div>
    </div>
    
    <div>
        <small class="text-muted d-block text-uppercase fw-bold mb-2" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Pengaturan Lanjutan") %></small>
        <div class="d-flex flex-wrap gap-2">
            <%= isRandom ?
            "<span class='badge bg-white text-success border border-success px-3 py-2 rounded-pill shadow-sm'><i class='fas fa-random me-1'></i> " + Common.getBahasaConfig("Soal Diacak") + "</span>" : "<span class='badge bg-white text-secondary border border-secondary px-3 py-2 rounded-pill shadow-sm'><i class='fas fa-sort-amount-down me-1'></i> " + Common.getBahasaConfig("Soal Berurutan") + "</span>" %>
            
            <%= lihatNilai ?
            "<span class='badge bg-white text-info border border-info px-3 py-2 rounded-pill shadow-sm'><i class='far fa-eye me-1'></i> " + Common.getBahasaConfig("Tampilkan Nilai") + "</span>" : "<span class='badge bg-white text-secondary border border-secondary px-3 py-2 rounded-pill shadow-sm'><i class='far fa-eye-slash me-1'></i> " + Common.getBahasaConfig("Sembunyikan Nilai") + "</span>" %>
            
            <%= lihatJawaban ?
            "<span class='badge bg-white text-primary border border-primary px-3 py-2 rounded-pill shadow-sm'><i class='fas fa-key me-1'></i> " + Common.getBahasaConfig("Tampilkan Kunci Jawaban") + "</span>" : "<span class='badge bg-white text-secondary border border-secondary px-3 py-2 rounded-pill shadow-sm'><i class='fas fa-lock me-1'></i> " + Common.getBahasaConfig("Sembunyikan Kunci Jawaban") + "</span>" %>
        
        	  <button type="button" class="btn btn-sm btn-outline-warning text-dark fw-bold rounded-pill shadow-sm px-3 ms-auto" 
                            onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%= Common.getBahasaConfigJS("Pengaturan Format & Sinkron Nilai") %>', '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_format_nilai&ppu=<%=ppuParam%>&contentModal='+cm, '70%', 'hidden', '', cm);">
                        <i class="fas fa-percentage me-1"></i> <%= Common.getBahasaConfig("Format Nilai") %>
                    </button>
                    
                    <button type="button" class="btn btn-sm btn-outline-info text-dark fw-bold rounded-pill shadow-sm px-3 ms-2" 
        onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%= Common.getBahasaConfigJS("Rekapitulasi Hasil Ujian Mahasiswa") %>', '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_lihat_hasil_ujian&ppu=<%=ppuParam%>&contentModal='+cm, '95%', 'hidden', '', cm);">
    <i class="fas fa-poll-h me-1"></i> <%= Common.getBahasaConfig("Lihat Hasil Ujian") %>
</button>
        </div>
    </div>
<%
    return;
}
%>

<div class="container-fluid py-2">
    <div class="card mb-4 border-0 shadow-sm bg-white rounded-3 overflow-hidden">
        <div class="bg-primary text-white p-3 d-flex align-items-center justify-content-between">
            <div class="d-flex align-items-center">
                <i class="fas fa-info-circle fs-4 me-3"></i>
                <h5 class="fw-bold mb-0"><%= Common.getBahasaConfig("Informasi Rincian Ujian") %></h5>
            </div>
            <button type="button" class="btn btn-sm btn-light text-primary fw-bold rounded-pill shadow-sm px-3" onclick="$('#modalUbahUjian<%=random%>').modal('show');">
                <i class="fas fa-edit me-1"></i> <%= Common.getBahasaConfig("Ubah Ujian") %>
            </button>
        </div>
        
        <div class="card-body p-4" id="info-ujian-container-<%=random%>">
            
            <div class="row mb-4">
                <div class="col-12">
                    <small class="text-muted text-uppercase fw-bold"><%= Common.getBahasaConfig("Mata Ujian / Tugas") %></small>
                    <h4 class="fw-bold text-dark mt-1"><%= namaUjian %></h4>
                    <p class="text-secondary mb-0"><%= keterangan %></p>
                </div>
            </div>
            
            <div class="row g-4 border-top border-bottom py-3 mb-4 bg-light rounded-2">
                <div class="col-md-6 col-lg-3 border-end">
                    <div class="d-flex align-items-center text-muted mb-1">
                        <i class="far fa-check-circle me-2 text-primary"></i>
                        <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Jenis Evaluasi") %></small>
                    </div>
                    <span class="fw-bold text-dark"><%= labelJenisKoreksi %></span>
                </div>
                <div class="col-md-6 col-lg-2 border-end">
                    <div class="d-flex align-items-center text-muted mb-1">
                        <i class="far fa-list-alt me-2 text-primary"></i>
                        <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Kapasitas Soal") %></small>
                    </div>
                    <span class="fw-bold text-dark"><%= jmlSoal %> <%= Common.getBahasaConfig("Butir") %></span>
                </div>
                <div class="col-md-6 col-lg-2 border-end">
                    <div class="d-flex align-items-center text-muted mb-1">
                        <i class="fas fa-arrow-circle-right me-2 text-primary"></i>
                        <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Batas Percobaan") %></small>
                    </div>
                    <span class="fw-bold text-dark"><%= jmlPercobaan %> <%= Common.getBahasaConfig("Kali") %></span>
                </div>
                <div class="col-md-6 col-lg-2 border-end">
                    <div class="d-flex align-items-center text-muted mb-1">
                        <i class="fas fa-stopwatch me-2 text-primary"></i>
                        <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Durasi Waktu") %></small>
                    </div>
                    <span class="fw-bold text-dark"><%= durasi %></span>
                </div>
                <div class="col-md-12 col-lg-3">
                    <div class="d-flex align-items-center text-muted mb-1">
                        <i class="far fa-calendar-alt me-2 text-primary"></i>
                        <small class="text-uppercase fw-bold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Periode Pelaksanaan") %></small>
                    </div>
                    <div class="d-flex flex-column">
                        <span class="fw-bold text-success" style="font-size: 0.85rem;"><i class="far fa-play-circle me-1"></i> <%= strMulai %></span>
                        <span class="fw-bold text-danger" style="font-size: 0.85rem;"><i class="far fa-stop-circle me-1"></i> <%= strSelesai %></span>
                    </div>
                </div>
            </div>
            
            <div>
                <small class="text-muted d-block text-uppercase fw-bold mb-2" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Pengaturan Lanjutan") %></small>
                <div class="d-flex flex-wrap gap-2">
                    <%= isRandom ?
                    "<span class='badge bg-white text-success border border-success px-3 py-2 rounded-pill shadow-sm'><i class='fas fa-random me-1'></i> " + Common.getBahasaConfig("Soal Diacak") + "</span>" : "<span class='badge bg-white text-secondary border border-secondary px-3 py-2 rounded-pill shadow-sm'><i class='fas fa-sort-amount-down me-1'></i> " + Common.getBahasaConfig("Soal Berurutan") + "</span>" %>
                    
                    <%= lihatNilai ?
                    "<span class='badge bg-white text-info border border-info px-3 py-2 rounded-pill shadow-sm'><i class='far fa-eye me-1'></i> " + Common.getBahasaConfig("Tampilkan Nilai") + "</span>" : "<span class='badge bg-white text-secondary border border-secondary px-3 py-2 rounded-pill shadow-sm'><i class='far fa-eye-slash me-1'></i> " + Common.getBahasaConfig("Sembunyikan Nilai") + "</span>" %>
                    
                    <%= lihatJawaban ?
                    "<span class='badge bg-white text-primary border border-primary px-3 py-2 rounded-pill shadow-sm'><i class='fas fa-key me-1'></i> " + Common.getBahasaConfig("Tampilkan Kunci Jawaban") + "</span>" : "<span class='badge bg-white text-secondary border border-secondary px-3 py-2 rounded-pill shadow-sm'><i class='fas fa-lock me-1'></i> " + Common.getBahasaConfig("Sembunyikan Kunci Jawaban") + "</span>" %>
                    
                    <button type="button" class="btn btn-sm btn-outline-warning text-dark fw-bold rounded-pill shadow-sm px-3 ms-auto" 
                            onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%= Common.getBahasaConfigJS("Pengaturan Format & Sinkron Nilai") %>', '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_format_nilai&ppu=<%=ppuParam%>&contentModal='+cm, '70%', 'hidden', '', cm);">
                        <i class="fas fa-percentage me-1"></i> <%= Common.getBahasaConfig("Format Nilai") %>
                    </button>
                    
                    <button type="button" class="btn btn-sm btn-outline-info text-dark fw-bold rounded-pill shadow-sm px-3 ms-2" 
					        onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%= Common.getBahasaConfigJS("Rekapitulasi Hasil Ujian Mahasiswa") %>', '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_lihat_hasil_ujian&ppu=<%=ppuParam%>&contentModal='+cm, '95%', 'hidden', '', cm);">
					    <i class="fas fa-poll-h me-1"></i> <%= Common.getBahasaConfig("Lihat Hasil Ujian") %>
					</button>
                </div>
            </div>

        </div>
    </div>

    <div class="p-4 bg-white border border-bottom-0 shadow-sm d-flex flex-column flex-md-row justify-content-between align-items-center gap-3 rounded-top">
        <div class="input-group w-100 w-md-50 shadow-sm">
            <span class="input-group-text bg-light border-end-0"><i class="fas fa-search text-muted"></i></span>
            <input type="text" id="inputCariSoal<%=random%>" class="form-control border-start-0 bg-light" placeholder="<%= Common.getBahasaConfig("Ketikkan kata kunci soal untuk mencari...") %>" onkeypress="if(event.key === 'Enter') muatDaftarSoal<%=random%>(1)">
            <button class="btn btn-secondary px-4 fw-bold" type="button" onclick="muatDaftarSoal<%=random%>(1)"><%= Common.getBahasaConfig("Cari Soal") %></button>
        </div>
        <div class="d-flex gap-2">
            <button class="btn px-4 fw-bold shadow-sm text-white" type="button" style="background-color:#16a34a;" onclick="bukaModalBuatSoalAi<%=random%>()">
                <i class="fas fa-magic me-2"></i><%= Common.getBahasaConfig("Buat Soal Via AI") %>
            </button>
            <button class="btn btn-success px-4 fw-bold shadow-sm" type="button" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%= Common.getBahasaConfigJS("Buat Soal Baru") %>', '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=ubah_soal_ujian&ppu=<%=ppuParam%>&contentModal='+cm+'&afterSave='+encodeURIComponent('muatDaftarSoal<%=random%>(1);'), '95%', 'hidden', '', cm);">
                <i class="fas fa-plus-circle me-2"></i><%= Common.getBahasaConfig("Tambah Soal Baru") %>
            </button>
        </div>
    </div>

    <div class="table-responsive border shadow-sm">
        <table class="table table-hover table-striped table-bordered align-middle mb-0 bg-white">
            <thead class="table-light text-center">
                <tr>
                    <th width="5%" class="py-3 text-dark fw-bold text-uppercase" style="font-size: 0.85rem;"><%= Common.getBahasaConfig("No") %></th>
                    <th width="45%" class="py-3 text-dark fw-bold text-uppercase" style="font-size: 0.85rem;"><%= Common.getBahasaConfig("Uraian Pertanyaan") %></th>
                    <th width="35%" class="py-3 text-dark fw-bold text-uppercase" style="font-size: 0.85rem;"><%= Common.getBahasaConfig("Opsi Jawaban") %></th>
                    <th width="15%" class="py-3 text-dark fw-bold text-uppercase" style="font-size: 0.85rem;"><%= Common.getBahasaConfig("Tindakan") %></th>
                </tr>
            </thead>
            <tbody id="bodyTabelSoal<%=random%>">
                <tr>
                    <td colspan="4" class="text-center text-muted py-5">
                        <div class="spinner-border text-primary" role="status"></div>
                        <p class="mt-2 mb-0"><%= Common.getBahasaConfig("Sedang memuat data soal...") %></p>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <div class="p-3 border border-top-0 shadow-sm d-flex justify-content-between align-items-center bg-white rounded-bottom mb-4">
        <small class="text-muted fw-bold" id="infoPaging<%=random%>"><%= Common.getBahasaConfig("Menampilkan Halaman 0 dari 0") %></small>
        <nav>
            <ul class="pagination pagination-sm mb-0" id="navigasiPaging<%=random%>">
            </ul>
        </nav>
    </div>
</div>

<div class="modal fade" id="modalUbahUjian<%=random%>" tabindex="-1" aria-labelledby="labelModalUbahUjian" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content border-0 shadow-lg rounded-4">
            <div class="modal-header bg-primary bg-gradient text-white p-4">
                <h5 class="modal-title fw-bold" id="labelModalUbahUjian">
                    <i class="fas fa-sliders-h me-2"></i><%= Common.getBahasaConfig("Pengaturan Data Ujian") %>
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="<%= Common.getBahasaConfig("Tutup Panel") %>"></button>
            </div>
            <div class="modal-body bg-light p-4">
                <form id="formUbahUjian<%=random%>">
                    <input type="hidden" name="ppu" value="<%= ppuParam %>">
                    
                    <div class="row g-4">
                        <div class="col-md-8">
                            <label class="form-label fw-bold text-dark"><%= Common.getBahasaConfig("Nama Evaluasi / Ujian") %> <span class="text-danger">*</span></label>
                            <input type="text" class="form-control shadow-sm border-secondary-subtle rounded-3" name="namaUjian" value="<%= namaUjian.replace("\"", "&quot;") %>" required>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-bold text-dark"><%= Common.getBahasaConfig("Jenis Evaluasi Koreksi") %></label>
                            <select class="form-select shadow-sm border-secondary-subtle rounded-3" name="jenisKoreksi">
                                <option value="<%= PenjelasanBankSoal.KOREKSI_OTOMATIS %>" <%= PenjelasanBankSoal.KOREKSI_OTOMATIS.equals(jenisKoreksi) ? "selected" : "" %>><%= Common.getBahasaConfig("Pilihan Ganda (Otomatis)") %></option>
                                <option value="<%= PenjelasanBankSoal.KOREKSI_MANUAL %>" <%= PenjelasanBankSoal.KOREKSI_MANUAL.equals(jenisKoreksi) ? "selected" : "" %>><%= Common.getBahasaConfig("Uraian / Esai (Manual)") %></option>
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-bold text-dark"><%= Common.getBahasaConfig("Keterangan Tambahan / Instruksi") %></label>
                            <textarea class="form-control shadow-sm border-secondary-subtle rounded-3" name="keterangan" rows="3"><%= keterangan.replace("\"", "&quot;") %></textarea>
                        </div>
                        
                        <hr class="text-muted my-2">
                        
                        <div class="col-md-6 col-lg-3">
                            <label class="form-label fw-bold text-dark"><%= Common.getBahasaConfig("Jumlah Soal Diujikan") %></label>
                            <input type="number" class="form-control shadow-sm border-secondary-subtle rounded-3" name="jmlSoal" value="<%= jmlSoal %>" min="0">
                        </div>
                        <div class="col-md-6 col-lg-3">
                            <label class="form-label fw-bold text-dark"><%= Common.getBahasaConfig("Batas Maksimal Percobaan") %></label>
                            <input type="number" class="form-control shadow-sm border-secondary-subtle rounded-3" name="jmlPercobaan" value="<%= jmlPercobaan %>" min="1">
                        </div>
                        <div class="col-md-6 col-lg-3">
                            <label class="form-label fw-bold text-dark"><%= Common.getBahasaConfig("Durasi Pengerjaan") %> <small>(HH:mm)</small></label>
                            <input type="time" class="form-control shadow-sm border-secondary-subtle rounded-3" name="durasi" value="<%= valDurasi %>">
                        </div>
                        <div class="col-md-6 col-lg-3 d-flex align-items-center mt-md-5">
                            <div class="form-check form-switch fs-6">
                                <input class="form-check-input" type="checkbox" id="chkDibatasi<%=random%>" name="isDibatasiWaktu" value="true" <%= isDibatasiWaktu ? "checked" : "" %>>
                                <label class="form-check-label fw-bold text-dark" for="chkDibatasi<%=random%>"><%= Common.getBahasaConfig("Batas Waktu Ketat") %></label>
                            </div>
                        </div>
                        
                        <div class="col-md-6">
                            <label class="form-label fw-bold text-dark"><%= Common.getBahasaConfig("Waktu Mulai Tersedia") %></label>
                            <input type="datetime-local" class="form-control shadow-sm border-secondary-subtle rounded-3" name="tglMulai" value="<%= valTglMulai %>">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-bold text-dark"><%= Common.getBahasaConfig("Waktu Berakhir (Tenggat)") %></label>
                            <input type="datetime-local" class="form-control shadow-sm border-secondary-subtle rounded-3" name="tglSelesai" value="<%= valTglSelesai %>">
                        </div>

                        <hr class="text-muted my-2">

                        <div class="col-md-4">
                            <div class="form-check fs-6 bg-white p-3 rounded-3 shadow-sm border">
                                <input class="form-check-input ms-1 me-2" type="checkbox" id="chkRandom<%=random%>" name="isRandom" value="true" <%= isRandom ? "checked" : "" %>>
                                <label class="form-check-label fw-bold text-dark" for="chkRandom<%=random%>"><i class="fas fa-random text-primary me-2"></i><%= Common.getBahasaConfig("Acak Urutan Soal") %></label>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="form-check fs-6 bg-white p-3 rounded-3 shadow-sm border">
                                <input class="form-check-input ms-1 me-2" type="checkbox" id="chkNilai<%=random%>" name="lihatNilai" value="true" <%= lihatNilai ? "checked" : "" %>>
                                <label class="form-check-label fw-bold text-dark" for="chkNilai<%=random%>"><i class="fas fa-eye text-info me-2"></i><%= Common.getBahasaConfig("Tampilkan Nilai Akhir") %></label>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="form-check fs-6 bg-white p-3 rounded-3 shadow-sm border">
                                <input class="form-check-input ms-1 me-2" type="checkbox" id="chkJawaban<%=random%>" name="lihatJawaban" value="true" <%= lihatJawaban ? "checked" : "" %>>
                                <label class="form-check-label fw-bold text-dark" for="chkJawaban<%=random%>"><i class="fas fa-key text-success me-2"></i><%= Common.getBahasaConfig("Buka Kunci Jawaban") %></label>
                            </div>
                        </div>
                    </div>
                    <hr class="text-muted my-2">
                    <div class="row g-3">
                        <div class="col-12">
                            <small class="text-muted d-block text-uppercase fw-bold mb-2">Pengaturan Anti-Kecurangan</small>
                        </div>
                        <div class="col-md-6">
                            <div class="form-check fs-6 bg-white p-3 rounded-3 shadow-sm border">
                                <input class="form-check-input ms-1 me-2" type="checkbox" id="chkBlokirTangkap<%=random%>" name="blokirTangkapLayar" value="true" <%= blokirTangkapLayar ? "checked" : "" %>>
                                <label class="form-check-label fw-bold text-dark" for="chkBlokirTangkap<%=random%>">
                                    <i class="fas fa-ban text-danger me-1"></i> Larang tangkap layar browser saat ujian berlangsung
                                    <div class="small text-muted fw-normal">Default aktif (true) &mdash; mencegah PrintScreen</div>
                                </label>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="form-check fs-6 bg-white p-3 rounded-3 shadow-sm border">
                                <input class="form-check-input ms-1 me-2" type="checkbox" id="chkMultiDevice<%=random%>" name="larangMultiDevice" value="true" <%= larangMultiDevice ? "checked" : "" %>>
                                <label class="form-check-label fw-bold text-dark" for="chkMultiDevice<%=random%>">
                                    <i class="fas fa-mobile-alt text-warning me-1"></i> Jangan izinkan multi-device
                                    <div class="small text-muted fw-normal">Default aktif (true) &mdash; cegah dikerjakan di &gt;1 perangkat bersamaan</div>
                                </label>
                            </div>
                        </div>
                        <div class="col-12">
                            <div class="bg-white p-3 rounded-3 shadow-sm border">
                                <label class="fw-bold text-dark mb-2"><i class="fas fa-map-marker-alt text-danger me-1"></i> Restriksi Lokasi Ujian (opsional)</label>
                                <div class="row g-2 align-items-end">
                                    <div class="col-md-3">
                                        <label class="form-label small text-muted mb-1">Latitude</label>
                                        <input type="number" step="any" class="form-control form-control-sm" name="lokasiLat" id="lokasiLat<%=random%>" value="<%= lokasiLat != null ? lokasiLat : "" %>" placeholder="-6.200000">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small text-muted mb-1">Longitude</label>
                                        <input type="number" step="any" class="form-control form-control-sm" name="lokasiLon" id="lokasiLon<%=random%>" value="<%= lokasiLon != null ? lokasiLon : "" %>" placeholder="106.816666">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small text-muted mb-1">Radius (meter)</label>
                                        <input type="number" class="form-control form-control-sm" name="lokasiRadius" id="lokasiRadius<%=random%>" value="<%= lokasiRadius != null ? lokasiRadius : "" %>" placeholder="200">
                                    </div>
                                    <div class="col-md-3">
                                        <button type="button" class="btn btn-outline-secondary btn-sm w-100" onclick="pilihDiPeta<%=random%>()">
                                            <i class="fas fa-map me-1"></i> Pilih di Peta
                                        </button>
                                    </div>
                                </div>
                                <div class="small text-muted mt-1">Kosongkan semua field lokasi untuk menonaktifkan restriksi lokasi.</div>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer bg-white border-top p-3 d-flex justify-content-between">
                <button type="button" class="btn btn-outline-secondary fw-bold px-4 rounded-pill shadow-sm" data-bs-dismiss="modal">
                    <i class="fas fa-times-circle me-2"></i><%= Common.getBahasaConfig("Batalkan") %>
                </button>
                <button type="button" class="btn btn-success fw-bold px-5 rounded-pill shadow-sm" id="btnSimpanInfoUjian<%=random%>" onclick="simpanInfoUjian<%=random%>()">
                    <i class="fas fa-save me-2"></i><%= Common.getBahasaConfig("Simpan Pembaruan") %>
                </button>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalPetaLokasi<%=random%>" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h6 class="modal-title fw-bold"><i class="fas fa-map-marked-alt me-2"></i>Pilih Lokasi di Peta</h6>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body p-0">
                <iframe id="iframePeta<%=random%>" src="" style="width:100%;height:450px;border:none;"></iframe>
            </div>
            <div class="modal-footer">
                <div class="me-auto">
                    <span class="text-muted small">Lat: </span><strong id="petaLat<%=random%>">-</strong>
                    <span class="text-muted small ms-3">Lon: </span><strong id="petaLon<%=random%>">-</strong>
                </div>
                <button type="button" class="btn btn-primary btn-sm" onclick="simpanLokasiPeta<%=random%>()">Simpan Lokasi</button>
                <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Batal</button>
            </div>
        </div>
    </div>
</div>

<!-- ============ MODAL BUAT SOAL VIA AI ============ -->
<div class="modal fade" id="modalBuatSoalAi<%=random%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content border-0 shadow-lg rounded-4">
            <div class="modal-header text-white p-4" style="background-color:#16a34a;">
                <h5 class="modal-title fw-bold"><i class="fas fa-magic me-2"></i><%= Common.getBahasaConfig("Buat Soal Via AI") %></h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Tutup"></button>
            </div>
            <div class="modal-body bg-light p-4">
                <div class="mb-3">
                    <label class="form-label fw-bold small text-muted text-uppercase"><%= Common.getBahasaConfig("Mata Ujian / Tugas") %></label>
                    <input type="text" class="form-control bg-white" value="<%= namaUjian %>" readonly>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold small text-muted text-uppercase"><%= Common.getBahasaConfig("Tipe Soal (mengikuti data master ujian)") %></label>
                    <input type="text" class="form-control bg-white" value="<%= labelJenisKoreksi %>" readonly>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold small text-muted text-uppercase"><%= Common.getBahasaConfig("Fokus / Topik Soal (opsional)") %></label>
                    <textarea id="aiTopikSoal<%=random%>" class="form-control" rows="2" placeholder="<%= Common.getBahasaConfig("Mis. bab tertentu, sub-topik, tingkat kesulitan...") %>"></textarea>
                </div>
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label fw-bold small text-muted text-uppercase"><%= Common.getBahasaConfig("Jumlah Soal") %></label>
                        <input type="number" id="aiJumlahSoal<%=random%>" class="form-control" value="1" min="1" max="30">
                    </div>
                    <div class="col-md-6" id="aiRowOpsi<%=random%>">
                        <label class="form-label fw-bold small text-muted text-uppercase"><%= Common.getBahasaConfig("Jumlah Opsi (Pilihan Ganda)") %></label>
                        <input type="number" id="aiJumlahOpsi<%=random%>" class="form-control" value="5" min="2" max="8">
                    </div>
                </div>
                <div id="aiStatusBuatSoal<%=random%>" class="alert alert-info mt-3 mb-0 d-none small"></div>
            </div>
            <div class="modal-footer bg-light">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Batal") %></button>
                <button type="button" id="aiBtnBuatSoal<%=random%>" class="btn text-white fw-bold" style="background-color:#16a34a;" onclick="prosesBuatSoalAi<%=random%>()">
                    <i class="fas fa-magic me-1"></i><%= Common.getBahasaConfig("Generate & Simpan") %>
                </button>
            </div>
        </div>
    </div>
</div>

<script>
    function pilihDiPeta<%=random%>() {
        var lat = document.getElementById('lokasiLat<%=random%>').value || '-6.2';
        var lon = document.getElementById('lokasiLon<%=random%>').value || '106.8';
        document.getElementById('iframePeta<%=random%>').src =
            'https://www.openstreetmap.org/export/embed.html?bbox=' + (parseFloat(lon)-0.01) + ',' + (parseFloat(lat)-0.01) + ',' + (parseFloat(lon)+0.01) + ',' + (parseFloat(lat)+0.01) + '&layer=mapnik&marker=' + lat + ',' + lon;
        new bootstrap.Modal(document.getElementById('modalPetaLokasi<%=random%>')).show();
        window._petaRandId = '<%=random%>';
    }
    window.addEventListener('message', function(ev) {
        if (ev.data && ev.data.type === 'peta-lokasi') {
            var r = window._petaRandId;
            document.getElementById('petaLat'+r).textContent = ev.data.lat.toFixed(6);
            document.getElementById('petaLon'+r).textContent = ev.data.lon.toFixed(6);
        }
    });
    function simpanLokasiPeta<%=random%>() {
        var latTxt = document.getElementById('petaLat<%=random%>').textContent;
        var lonTxt = document.getElementById('petaLon<%=random%>').textContent;
        if (latTxt !== '-') document.getElementById('lokasiLat<%=random%>').value = latTxt;
        if (lonTxt !== '-') document.getElementById('lokasiLon<%=random%>').value = lonTxt;
        bootstrap.Modal.getInstance(document.getElementById('modalPetaLokasi<%=random%>')).hide();
    }
</script>
<script>
    (function() {
        var modalUbah = document.getElementById('modalUbahUjian<%=random%>');
        if (modalUbah) {
            if (typeof $ !== 'undefined') {
                $(modalUbah).on('hide.bs.modal', function(e) {
                    if (e.target === this) e.stopPropagation();
                });
                $(modalUbah).on('hidden.bs.modal', function(e) {
                    if (e.target === this) {
                        e.stopPropagation();
                        if ($('.modal.show').length > 0) $('body').addClass('modal-open');
                    }
                });
            }
            modalUbah.addEventListener('hide.bs.modal', function(e) {
                if (e.target === this) e.stopPropagation();
            });
            modalUbah.addEventListener('hidden.bs.modal', function(e) {
                if (e.target === this) {
                    e.stopPropagation();
                    if (document.querySelectorAll('.modal.show').length > 0) {
                        document.body.classList.add('modal-open');
                    }
                }
            });
        }
    })();

    let halamanSaatIni<%=random%> = 1;

    function muatUlangInfoUjian<%=random%>() {
        const containerInfo = document.getElementById('info-ujian-container-<%=random%>');
        if (!containerInfo) return;

        containerInfo.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary mb-2"></div><div class="fw-bold text-muted"><%= Common.getBahasaConfig("Memperbarui informasi ujian...") %></div></div>';

        const urlFetch = '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=daftar_soal&ppu=<%=ppuParam%>&action=info';
        
        fetch(urlFetch)
            .then(response => {
                if(!response.ok) throw new Error('<%= Common.getBahasaConfigJS("Gagal memuat respons dari server.") %>');
                return response.text();
            })
            .then(htmlResponse => {
                containerInfo.innerHTML = htmlResponse;
            })
            .catch(error => {
                console.error(error);
                if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Gagal memuat ulang data informasi. Silakan segarkan halaman.") %>', 'bg-danger');
            });
    }

    function simpanInfoUjian<%=random%>() {
        const formElement = document.getElementById('formUbahUjian<%=random%>');
        const formData = new URLSearchParams(new FormData(formElement));
        
        const btnSimpan = document.getElementById('btnSimpanInfoUjian<%=random%>');
        const teksAsliBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span><%= Common.getBahasaConfig("Menyimpan...") %>';
        btnSimpan.disabled = true;

        fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_simpan_info_ujian', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(response => {
            if(!response.ok) throw new Error('<%= Common.getBahasaConfigJS("Kendala jaringan ke server.") %>');
            return response.json();
        })
        .then(data => {
            if (data.status === 'success') {
                if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Informasi ujian berhasil diperbarui.") %>', 'bg-success');
                
                if (typeof bootstrap !== 'undefined') {
                    var modalInstance = bootstrap.Modal.getInstance(document.getElementById('modalUbahUjian<%=random%>'));
                    if (modalInstance) modalInstance.hide();
                    else $('#modalUbahUjian<%=random%>').modal('hide');
                } else {
                    $('#modalUbahUjian<%=random%>').modal('hide');
                }
                
                muatUlangInfoUjian<%=random%>();
            } else {
                alert(data.message || '<%= Common.getBahasaConfigJS("Terjadi kesalahan teknis saat menyimpan data.") %>');
            }
        })
        .catch(error => {
            console.error(error);
            alert('<%= Common.getBahasaConfigJS("Sistem gagal terhubung dengan peladen utama.") %>');
        })
        .finally(() => {
            btnSimpan.innerHTML = teksAsliBtn;
            btnSimpan.disabled = false;
        });
    }

    function muatFileOpsi<%=random%>(idSoal, huruf, wadahId) {
        var reqObj = { 
            "action": "file", 
            "class": "<%= ais.database.model.file.LampiranLain.class.getName() %>", 
            "ref": idSoal, 
            "jenis": "Gambar_Jawaban_" + huruf,
            "kondisiTambahan": "",
            "refresh": "false",
            "usingId": false
        };
        const servletUrl = '<%=Common.ROOT%>/Data';
        
        fetch(servletUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(reqObj)
        })
        .then(response => response.json())
        .then(dataResponse => {
            if(dataResponse.status == '00' && dataResponse.data) { 
                renderPreviewFile<%=random%>(dataResponse.data, wadahId);
            }
        })
        .catch(error => console.error("<%= Common.getBahasaConfig("Gagal mengambil file opsi jawaban") %>", error));
    }

    function renderPreviewFile<%=random%>(data, wadahId) {
        var url = data.url;
        var filename = data.nama;
        var ext = filename ? filename.split('.').pop().toLowerCase() : '';
        var container = document.getElementById(wadahId);
        if (!container) return;

        var html = '';
        if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].indexOf(ext) !== -1) {
            html = '<div class="mt-2"><a href="' + url + '" target="_blank" title="<%= Common.getBahasaConfig("Klik untuk memperbesar gambar") %>">' +
                   '<img src="' + url + '" class="img-fluid rounded shadow-sm border border-secondary-subtle" style="max-height: 120px; object-fit: cover;" alt="' + filename + '"></a></div>';
        } else if (['mp3', 'wav', 'ogg', 'm4a'].indexOf(ext) !== -1) {
            html = '<div class="mt-2"><audio controls class="w-100 rounded shadow-sm" style="max-width: 300px; height: 40px;"><source src="' + url + '" type="audio/' + (ext === 'mp3' ? 'mpeg' : ext) + '"></audio></div>';
        } else if (['mp4', 'webm', 'mov'].indexOf(ext) !== -1) {
            html = '<div class="mt-2"><video controls class="w-100 rounded shadow-sm border" style="max-width: 300px;"><source src="' + url + '" type="video/' + ext + '"></video></div>';
        } else {
            html = '<div class="mt-2"><a href="' + url + '" target="_blank" class="btn btn-sm btn-outline-primary shadow-sm fw-bold rounded-pill px-3">' +
                   '<i class="fas fa-download me-2"></i><%= Common.getBahasaConfig("Unduh Lampiran") %></a></div>';
        }
        
        container.innerHTML = html;
        container.style.display = 'block';
    }

    function muatDaftarSoal<%=random%>(halamanTarget) {
        halamanSaatIni<%=random%> = halamanTarget || 1;
        const inputPencarian = document.getElementById('inputCariSoal<%=random%>');
        const kataKunci = inputPencarian ? inputPencarian.value.trim() : '';

        const tbody = document.getElementById('bodyTabelSoal<%=random%>');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5"><div class="spinner-border spinner-border-sm text-primary me-2"></div><span class="text-muted fw-bold"><%= Common.getBahasaConfig("Sedang mengambil data dari sistem...") %></span></td></tr>';
        }

        const linkAmbilData = '<%=linkAmbilData%>&ppu=<%=pertemuanPunyaUjian.getId()%>&page=' + halamanSaatIni<%=random%> + '&keyword=' + encodeURIComponent(kataKunci);
        
        fetch(linkAmbilData)
            .then(response => response.json())
            .then(data => {
                if (data.status === 'success') {
                    renderTabelSoal<%=random%>(data.data, data.startNumber);
                    renderPaging<%=random%>(data.currentPage, data.totalPages);
                } else {
                    if (tbody) {
                        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fs-3 d-block mb-2"></i><span class="fw-bold">' + data.message + '</span></td></tr>';
                    }
                }
            })
            .catch(error => {
                console.error(error);
                if (tbody) {
                    tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-5"><i class="fas fa-plug fs-3 d-block mb-2"></i><span class="fw-bold"><%= Common.getBahasaConfig("Gagal terhubung ke peladen. Silakan periksa koneksi jaringan Anda.") %></span></td></tr>';
                }
            });
    }

    function renderTabelSoal<%=random%>(daftarSoal, nomorAwal) {
        const tbody = document.getElementById('bodyTabelSoal<%=random%>');
        if (!tbody) return;
        
        tbody.innerHTML = "";

        if (!daftarSoal || daftarSoal.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-5"><i class="fas fa-inbox fs-1 d-block mb-2 text-secondary"></i><span class="fw-bold"><%= Common.getBahasaConfig("Tidak ada data soal yang ditemukan di dalam sistem.") %></span></td></tr>';
            return;
        }

        let barisHtml = "";
        let antreanPencarianFile = [];

        daftarSoal.forEach((soal, indeks) => {
            const nomor = nomorAwal + indeks;
            let opsiHtml = "";

            if (soal.jenisKoreksi === '<%= PenjelasanBankSoal.KOREKSI_MANUAL %>') {
                opsiHtml = '<span class="badge bg-secondary px-3 py-2 shadow-sm"><i class="fas fa-pen me-2"></i><%= Common.getBahasaConfig("Esai / Uraian Bebas") %></span>';
            } else {
                opsiHtml = "<ul class='list-unstyled mb-0' style='font-size: 0.95rem;'>";
                
                soal.opsi.forEach((opsi, idxOpsi) => {
                    const huruf = opsi.huruf || String.fromCharCode(65 + idxOpsi); 
                    const idReferensi = soal.idBankSoal ? soal.idBankSoal : soal.id;
                    const idWadahFile = "wadah_file_opsi_<%=random%>_" + idReferensi + "_" + huruf;

                    const tandaBenar = opsi.isBenar ? '<i class="fas fa-check-circle text-success fs-5 ms-3" title="<%= Common.getBahasaConfig("Kunci Jawaban") %>"></i>' : '';
                    const teksClass = opsi.isBenar ? 'fw-bold text-dark' : 'text-secondary';
                    
                    opsiHtml += '<li class="mb-3 border-bottom pb-3 d-flex justify-content-between align-items-start">' +
                                   '<div class="d-flex flex-column w-100">' +
                                      '<div class="d-flex align-items-start">' +
                                         '<span class="' + teksClass + ' me-2 fw-bold">' + huruf + '.</span>' +
                                         '<span class="' + teksClass + '">' + opsi.teks + '</span>' +
                                      '</div>' +
                                      '<div id="' + idWadahFile + '" style="display: none; padding-left: 1.2rem;"></div>' +
                                   '</div>' + 
                                   tandaBenar + 
                                '</li>';
                    
                    antreanPencarianFile.push({ idSoal: idReferensi, huruf: huruf, wadahId: idWadahFile });
                });
                opsiHtml += "</ul>";
            }

            var labelUbahSoal = '<%= Common.getBahasaConfigJS("Ubah Soal") %>';
            var labelUbah = '<%= Common.getBahasaConfigJS("Ubah") %>';
            var labelHapus = '<%= Common.getBahasaConfigJS("Hapus") %>';

            <% String buatEditSoal = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=ubah_soal_ujian&ppu=" + pertemuanPunyaUjian.getId(); %>

            var urlBase = '<%= buatEditSoal %>';
            var callback = encodeURIComponent('muatDaftarSoal<%=random%>(' + halamanSaatIni<%=random%> + ');');
            var urlEdit = urlBase + '&afterSave=' + callback;

            // MENGGUNAKAN showConfirmModal GLOBAL
            barisHtml += '<tr style="vertical-align: top;">' +
                            '<td class="text-center fw-bold text-secondary">' + nomor + '</td>' +
                            '<td class="px-3">' +
                                '<div class="mb-2 text-dark">' + soal.teksSoal + '</div>' +
                                '<span class="badge bg-light text-primary border border-primary-subtle" style="font-size: 0.75rem;">' +
                                   '<i class="fas fa-tag me-1"></i> ID Soal: ' + soal.id +
                                '</span>' +
                            '</td>' +
                            '<td class="px-3">' + opsiHtml + '</td>' +
                            '<td class="text-center">' +
                                '<div class="d-flex flex-column flex-xl-row justify-content-center gap-2 px-2">' +
                                    '<button type="button" class="btn btn-sm btn-primary shadow-sm w-100 d-flex align-items-center justify-content-center fw-bold" ' +
                                             'onclick="var cm=\'cm_\'+(++variable); loadModalContentCustomSimpan(\'' + labelUbahSoal + '\', \'' + urlEdit + '&contentModal=\'+cm+\'&idSoal=' + soal.id + '\', \'95%\', \'hidden\', \'\', cm);">' +
                                        '<i class="far fa-edit me-2"></i> ' + labelUbah +
                                    '</button>' +
                                    '<button type="button" class="btn btn-sm btn-danger shadow-sm w-100 d-flex align-items-center justify-content-center fw-bold" ' +
                                            'onclick="showConfirmModal(\'<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus soal ini secara permanen?") %>\', function() { typeof hapusSoal<%=random%> === \'function\' ? hapusSoal<%=random%>(\'' + soal.id + '\') : console.log(\'Hapus: ' + soal.id + '\'); });">' +
                                        '<i class="far fa-trash-alt me-2"></i> ' + labelHapus +
                                    '</button>' +
                                '</div>' +
                            '</td>' +
                         '</tr>';
        });
       
        tbody.innerHTML = barisHtml;

        antreanPencarianFile.forEach(tugas => {
            muatFileOpsi<%=random%>(tugas.idSoal, tugas.huruf, tugas.wadahId);
        });
    }

    // Hapus 1 soal (UjianPunyaSoal) dari sesi ujian ini, lalu muat ulang daftar.
    // Memakai helper global prosesDeleteData(fqcn, id, callback) yang sama dipakai modul lain.
    window.hapusSoal<%=random%> = function(idSoal) {
        if (typeof prosesDeleteData === 'function') {
            prosesDeleteData('ais.database.model.UjianPunyaSoal', idSoal, function() {
                muatDaftarSoal<%=random%>(halamanSaatIni<%=random%>);
                if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Soal berhasil dihapus.") %>', 'bg-success');
            });
        } else if (typeof tampilkanToast === 'function') {
            tampilkanToast('<%= Common.getBahasaConfigJS("Fungsi hapus tidak tersedia pada halaman ini.") %>', 'bg-danger');
        }
    };

    function renderPaging<%=random%>(halamanSekarang, totalHalaman) {
        const wadahPaging = document.getElementById('navigasiPaging<%=random%>');
        const infoPaging = document.getElementById('infoPaging<%=random%>');
        
        if (!wadahPaging || !infoPaging) return;

        let teksInfo = '<%= Common.getBahasaConfigJS("Menampilkan Halaman {1} dari {2}") %>';
        infoPaging.innerText = teksInfo.replace("{1}", halamanSekarang).replace("{2}", totalHalaman);
      
        wadahPaging.innerHTML = "";

        if (totalHalaman <= 1) return;

        const prevClass = halamanSekarang === 1 ? "disabled" : "";
        wadahPaging.innerHTML += '<li class="page-item ' + prevClass + '">' +
                                 '<a class="page-link shadow-none fw-bold" href="javascript:void(0)" onclick="muatDaftarSoal<%=random%>(' + (halamanSekarang - 1) + ')">' + 
                                 '<i class="fas fa-chevron-left me-1"></i><%= Common.getBahasaConfig("Sebelumnya") %></a></li>';

        let startPage = Math.max(1, halamanSekarang - 2);
        let endPage = Math.min(totalHalaman, halamanSekarang + 2);

        for (let i = startPage; i <= endPage; i++) {
            const activeClass = (i === halamanSekarang) ? "active" : "";
            wadahPaging.innerHTML += '<li class="page-item ' + activeClass + '">' +
                                     '<a class="page-link shadow-none fw-bold" href="javascript:void(0)" onclick="muatDaftarSoal<%=random%>(' + i + ')">' + i + '</a></li>';
        }

        const nextClass = halamanSekarang === totalHalaman ? "disabled" : "";
        wadahPaging.innerHTML += '<li class="page-item ' + nextClass + '">' +
                                 '<a class="page-link shadow-none fw-bold" href="javascript:void(0)" onclick="muatDaftarSoal<%=random%>(' + (halamanSekarang + 1) + ')">' + 
                                 '<%= Common.getBahasaConfigJS("Selanjutnya") %><i class="fas fa-chevron-right ms-1"></i></a></li>';
    }
    
    // ================= BUAT SOAL VIA AI =================
    var isPgBuatSoal<%=random%> = <%= !PenjelasanBankSoal.KOREKSI_MANUAL.equals(jenisKoreksi) %>;

    function bukaModalBuatSoalAi<%=random%>() {
        var rowOpsi = document.getElementById('aiRowOpsi<%=random%>');
        if (rowOpsi) rowOpsi.style.display = isPgBuatSoal<%=random%> ? '' : 'none';
        var st = document.getElementById('aiStatusBuatSoal<%=random%>');
        if (st) { st.className = 'alert alert-info mt-3 mb-0 d-none small'; st.innerHTML = ''; }
        var el = document.getElementById('modalBuatSoalAi<%=random%>');
        if (typeof bootstrap !== 'undefined') { new bootstrap.Modal(el).show(); }
        else { $('#modalBuatSoalAi<%=random%>').modal('show'); }
    }

    function setStatusAiSoal<%=random%>(html, kelas) {
        var st = document.getElementById('aiStatusBuatSoal<%=random%>');
        if (!st) return;
        st.className = 'alert ' + (kelas || 'alert-info') + ' mt-3 mb-0 small';
        st.innerHTML = html;
    }

    function prosesBuatSoalAi<%=random%>() {
        var topik = (document.getElementById('aiTopikSoal<%=random%>').value || '').trim();
        var jumlah = parseInt(document.getElementById('aiJumlahSoal<%=random%>').value || '1', 10);
        if (isNaN(jumlah) || jumlah < 1) jumlah = 1;
        var opsi = parseInt((document.getElementById('aiJumlahOpsi<%=random%>') || {}).value || '5', 10);
        if (isNaN(opsi) || opsi < 2) opsi = 5;

        var btn = document.getElementById('aiBtnBuatSoal<%=random%>');
        var teksAsli = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%= Common.getBahasaConfig("Memproses...") %>';
        setStatusAiSoal<%=random%>('<i class="fas fa-cog fa-spin me-1"></i><%= Common.getBahasaConfig("Menyiapkan konteks materi & soal yang sudah ada...") %>', 'alert-info');

        var urlPrompt = '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_prompt_soal_ai';
        var bodyPrompt = new URLSearchParams();
        bodyPrompt.append('ppu', '<%=ppuParam%>');
        bodyPrompt.append('topik', topik);
        bodyPrompt.append('jumlah', String(jumlah));
        bodyPrompt.append('opsi', String(opsi));

        fetch(urlPrompt, { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: bodyPrompt.toString() })
        .then(function(r){ return r.json(); })
        .then(function(dp){
            if (!dp || dp.status !== 'success' || !dp.prompt) {
                throw new Error((dp && dp.message) ? dp.message : '<%= Common.getBahasaConfigJS("Gagal menyiapkan konteks soal.") %>');
            }
            setStatusAiSoal<%=random%>('<i class="fas fa-robot fa-spin me-1"></i><%= Common.getBahasaConfig("Menghubungi AI untuk membuat soal...") %>', 'alert-info');
            return fetch('<%= Common.ROOT %>/Ai', {
                method:'POST',
                headers:{'Content-Type':'application/json; charset=UTF-8'},
                body: JSON.stringify({ instruksi: dp.prompt, prompt: dp.prompt, maxTokens: 3072, temperature: 0.5, source:'buat_soal_ai' })
            }).then(function(r){ return r.json(); });
        })
        .then(function(da){
            var teks = (da && (da.text || da.response || da.message)) ? (da.text || da.response || da.message) : '';
            if (!da || da.success === false || !teks) {
                throw new Error((da && (da.error || da.raw)) ? (da.error || da.raw) : '<%= Common.getBahasaConfigJS("Respons AI kosong atau gagal.") %>');
            }
            setStatusAiSoal<%=random%>('<i class="fas fa-save fa-spin me-1"></i><%= Common.getBahasaConfig("Menyimpan soal ke bank soal...") %>', 'alert-info');
            var bodySimpan = new URLSearchParams();
            bodySimpan.append('ppu', '<%=ppuParam%>');
            bodySimpan.append('respon', teks);
            return fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_simpan_soal_ai', {
                method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: bodySimpan.toString()
            }).then(function(r){ return r.json(); });
        })
        .then(function(ds){
            if (!ds || ds.status !== 'success') {
                throw new Error((ds && ds.message) ? ds.message : '<%= Common.getBahasaConfigJS("Gagal menyimpan soal.") %>');
            }
            setStatusAiSoal<%=random%>('<i class="fas fa-check-circle me-1"></i>' + (ds.message || '<%= Common.getBahasaConfigJS("Soal berhasil dibuat.") %>'), 'alert-success');
            if (typeof tampilkanToast === 'function') tampilkanToast(ds.message || '<%= Common.getBahasaConfigJS("Soal berhasil dibuat via AI.") %>', 'bg-success');
            setTimeout(function(){
                var el = document.getElementById('modalBuatSoalAi<%=random%>');
                if (typeof bootstrap !== 'undefined') {
                    var mi = bootstrap.Modal.getInstance(el); if (mi) mi.hide(); else $('#modalBuatSoalAi<%=random%>').modal('hide');
                } else { $('#modalBuatSoalAi<%=random%>').modal('hide'); }
                muatDaftarSoal<%=random%>(1);
            }, 900);
        })
        .catch(function(err){
            console.error(err);
            setStatusAiSoal<%=random%>('<i class="fas fa-exclamation-triangle me-1"></i>' + (err && err.message ? err.message : err), 'alert-danger');
        })
        .finally(function(){
            btn.disabled = false;
            btn.innerHTML = teksAsli;
        });
    }

    // Panggil fungsi segera setelah script siap
    muatDaftarSoal<%=random%>(halamanSaatIni<%=random%>);
</script>