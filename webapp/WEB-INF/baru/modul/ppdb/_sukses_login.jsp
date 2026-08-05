<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.KelasSiswa"%>
<%@page import="ais.database.model.sekolah.GelombangPendaftaranPsb"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.common.VerifikasiPSBHtmlHelper"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    String idParam = request.getParameter("id");
    
    Long casisId = null;
    
    if (idParam != null && !idParam.trim().isEmpty()) {
        try { casisId = Long.parseLong(idParam.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_sukses_login.jsp:35");}
    } else if (tbmuser != null && tbmuser.getCalonSiswa() != null) {
        casisId = tbmuser.getCalonSiswa().getId();
    }

    String cbidS = casisId == null ? null : casisId.toString();
    CalonSiswa casis = (cbidS != null && !cbidS.trim().isEmpty()) ? 
            (CalonSiswa) GeneralValueObject.ambilData(CalonSiswa.class, cbidS, true) : null;

    if (casis == null || casis.getId() == null) {
%>
        <div class="container py-5 text-center">
            <div class="alert alert-danger shadow-sm rounded-4 d-inline-block px-5 py-4 border-0">
                <i class="fas fa-exclamation-triangle fa-3x mb-3 text-danger"></i>
                <h5 class="fw-bold"><%= Common.getBahasaConfig("Data Tidak Ditemukan") %></h5>
                <p class="text-muted mb-0"><%= Common.getBahasaConfig("Sesi pendaftaran Anda telah berakhir atau data calon siswa tidak valid. Silakan masuk kembali ke dalam sistem.") %></p>
                <button type="button" class="btn btn-primary rounded-pill px-4 mt-4 fw-bold shadow-sm" onclick="if(typeof window.bukaModalLoginPSB === 'function') window.bukaModalLoginPSB();">
                    <i class="fas fa-sign-in-alt me-2"></i><%= Common.getBahasaConfig("Masuk ke Sistem") %>
                </button>
            </div>
        </div>
<%
        return;
    }

    // Variabel Visibilitas Komponen & Tombol (Dari Pengaturan Gelombang)
    GelombangPendaftaranPsb gelombang = casis.getGelombangPendaftaranPsb();
    boolean tampilAlur = gelombang != null && gelombang.getTampilAlur() != null ? gelombang.getTampilAlur() : false;
    boolean tampilLengkapiBerkas = gelombang != null && gelombang.getTampilLengkapiBerkas() != null ? gelombang.getTampilLengkapiBerkas() : true;
    boolean tampilUjian = gelombang != null && gelombang.getTampilUjian() != null ? gelombang.getTampilUjian() : false;
    boolean tampilCetakNoReg = gelombang != null && gelombang.getTampilCetakNoReg() != null ? gelombang.getTampilCetakNoReg() : true;
    boolean tampilCetakBiodata = gelombang != null && gelombang.getTampilCetakBiodata() != null ? gelombang.getTampilCetakBiodata() : true;
    boolean tampilCetakKartuUjian = gelombang != null && gelombang.getTampilCetakKartuUjian() != null ? gelombang.getTampilCetakKartuUjian() : false;
    boolean tampilKetDiterima = gelombang != null && gelombang.getTampilKeteranganDiterima() != null ? gelombang.getTampilKeteranganDiterima() : false;
    boolean tampilLogout = gelombang != null && gelombang.getTampilLogout() != null ? gelombang.getTampilLogout() : true;
    
    boolean tampilFormTambahan = gelombang != null && gelombang.getTampilFormTambahanDiHalamanUtama() != null ? gelombang.getTampilFormTambahanDiHalamanUtama() : true;
    boolean tampilFormLampiran = gelombang != null && gelombang.getTampilFormLampiranDiHalamanUtama() != null ? gelombang.getTampilFormLampiranDiHalamanUtama() : true;
    boolean tampilWawancara    = gelombang != null && gelombang.getTampilWawancara();

    // Variabel Profil Calon Siswa
    String namaGelombang = gelombang != null ? gelombang.getNama() : "-";
    String periode = gelombang != null ? gelombang.getTahunAjaran() : "-";
    String sekolah = casis.getSekolah() != null ? casis.getSekolah().getNama() : "-";
    String penjurusan = casis.getPenjurusanSekolah() != null ? casis.getPenjurusanSekolah().getNama() : "-";
    String noRegistrasi = casis.getNoRegistrasi() != null ? casis.getNoRegistrasi() : "-";
    String noUjian = casis.getNoUjian() != null ? casis.getNoUjian() : "-";
    String namaLengkap = casis.getNama() != null ? casis.getNama().toUpperCase() : "-";
    
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
    String tempatLahir = casis.getTempatLahir() != null ? casis.getTempatLahir() : "-";
    String tglLahir = casis.getTanggalLahir() != null ? sdf.format(casis.getTanggalLahir()) : "";
    String ttl = tempatLahir + (tglLahir.isEmpty() ? "" : ", " + tglLahir);
    
    String namaOrtu = (casis.getNamaAyah() != null ? casis.getNamaAyah() : "-") + " / " + (casis.getNamaIbu() != null ? casis.getNamaIbu() : "-");
    String telpOrtu = casis.getTeleponOrangTua() != null ? casis.getTeleponOrangTua() : "-";
    
    String jadwalPertemuan = "-";
    if (casis.getJadwalPertemuanPSB() != null && casis.getJadwalPertemuanPSB().getAktif()) {
        jadwalPertemuan = casis.getJadwalPertemuanPSB().getNama() + ", " + Common.getBahasaConfig("Jadwal") + ": " + 
                          Common.dateFormat51.get().format(casis.getJadwalPertemuanPSB().getWaktuMulai()) + " sd " + 
                          Common.dateFormat51.get().format(casis.getJadwalPertemuanPSB().getWaktuSampai());
    }

    // Logika Status Diterima
    String diterima = "";
    if (casis.getMengundurkanDiri() != null && casis.getMengundurkanDiri()) {
        diterima = Common.getBahasaConfig("Mengundurkan diri");
    } else if (casis.getDitolak() != null && casis.getDitolak()) {
        diterima = Common.getBahasaConfig("Tidak diterima (ditolak)");
    } else if (casis.getTelahDiterima() != null && casis.getTelahDiterima()) {
        diterima = Common.getBahasaConfig("Telah diterima");
    } else if (casis.getTerverifikasi() != null && casis.getTerverifikasi()) {
        diterima = Common.getBahasaConfig("Telah diverifikasi");
    } else {
        diterima = Common.getBahasaConfig("Belum dinyatakan lulus / diterima");
    }
    
    String ketStatus = casis.getKeterangan() != null && !casis.getKeterangan().trim().isEmpty() ? casis.getKeterangan() : "-";
    String fotoUrl = Common.ROOT + "/img/default-avatar.png";
    try {
        String tempUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(casis));
        if(tempUrl != null && !tempUrl.trim().isEmpty()) fotoUrl = tempUrl;
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_sukses_login.jsp:118");}

    // Variabel Siswa Lulus (Jika Diterima)
    Siswa siswaDiterima = casis.getSiswa();
    boolean isSiswaTelahDiterima = (siswaDiterima != null);
    String nis = "-", kelasSiswa = "-", waliKelas = "-";
    
    // Variabel Lampiran & Tambahan
    String paramRaw = casis.getParameterTambahan();
    boolean hasParam = paramRaw != null && !paramRaw.trim().isEmpty();
    List<Map<String, Object>> kelengkapanList = new ArrayList<Map<String, Object>>();

    Session hibSession = null;

    try {
        // MEMBUKA SESI HIBERNATE SECARA TERPUSAT
        hibSession = HibernateUtil.getSessionFactory().openSession();
        
        if (isSiswaTelahDiterima) {
            nis = siswaDiterima.getNomorInduk() != null ? siswaDiterima.getNomorInduk() : "-";
            KelasSiswa ks = siswaDiterima.getKelas();
            if (ks != null) {
                kelasSiswa = ks.getNama() != null ? ks.getNama() : "-";
                if (ks.getGuruPembina() != null) {
                    waliKelas = ks.getGuruPembina().getNama() != null ? ks.getGuruPembina().getNama() : "-";
                }
            }
        }
        
        // Ekstraksi Kelengkapan Berkas (Read-Only)
        if (tampilFormLampiran) {
            List<Object[]> daftarVerifikasi = VerifikasiPSBHtmlHelper.getDaftarVerifikasi(casis, casis.getGelombangPendaftaranPsb(), hibSession);
            if (daftarVerifikasi != null) {
                for (Object[] rowData : daftarVerifikasi) {
                    ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa v = (ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa) rowData[0];
                    ais.database.model.sekolah.CalonSiswaPunyaVerifikasiBerkas ex = (ais.database.model.sekolah.CalonSiswaPunyaVerifikasiBerkas) rowData[1];
                    
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("nama", v.getNama());
                    map.put("verified", ex != null && ex.getVerified() != null && ex.getVerified());
                    map.put("keterangan", ex != null && ex.getKeterangan() != null ? ex.getKeterangan() : "");
                    
                    boolean adaFile = false;
                    String urlDownload = "";
                    if (ex != null && ex.getId() != null) {
                        LampiranLain lampiranLain = LampiranLain.ambil(ex.getId(), ais.database.model.sekolah.CalonSiswaPunyaVerifikasiBerkas.class.getName());
                        adaFile = lampiranLain != null && lampiranLain.getId() != null;
                        urlDownload = lampiranLain != null && lampiranLain.getId() != null ? lampiranLain.createLinkUri() : null;
                    }
                    map.put("adaFile", adaFile);
                    map.put("urlDownload", urlDownload);
                    kelengkapanList.add(map);
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_sukses_login.jsp:174");
    } finally {
        if (hibSession != null && hibSession.isOpen()) {
            try { hibSession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_sukses_login.jsp:177");}
        }
    }
%>

<style>
    .profile-card-<%=rnd%> { border-top: 5px solid #0d6efd; background: #ffffff; }
    .action-buttons-<%=rnd%> .btn { font-size: 0.85rem; font-weight: 600; transition: all 0.2s ease-in-out; border-radius: 50rem; padding: 0.5rem 1.25rem; box-shadow: 0 2px 4px rgba(0,0,0,0.05); margin-bottom: 0.5rem; }
    .action-buttons-<%=rnd%> .btn:hover { transform: translateY(-2px); box-shadow: 0 4px 10px rgba(0,0,0,0.15); }
    .table-profile-<%=rnd%> td { padding: 0.8rem 0.5rem; vertical-align: middle; font-size: 0.95rem; border-bottom: 1px dashed #e9ecef; }
    .table-profile-<%=rnd%> tr:last-child td { border-bottom: none; }
    .table-profile-<%=rnd%> td:first-child { font-weight: 600; color: #6c757d; width: 35%; }
    .foto-mhs-<%=rnd%> { width: 100%; max-width: 180px; aspect-ratio: 3/4; object-fit: cover; border: 4px solid #fff; }
    .bg-gradient-header-<%=rnd%> { background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); }
</style>

<div class="container-fluid py-4 bg-light min-vh-100">
    <div class="row justify-content-center">
        <div class="col-xl-10 col-lg-11">
            
            <% if (isSiswaTelahDiterima) { %>
            <div class="alert alert-success shadow-sm rounded-4 border-0 mb-4 d-flex align-items-center p-4">
                <i class="fas fa-check-circle fa-3x me-4 text-success opacity-75"></i>
                <div>
                    <h5 class="fw-bold mb-1"><%= Common.getBahasaConfig("Selamat, Anda telah dinyatakan diterima sebagai Siswa!") %></h5>
                    <p class="mb-0">
                        <span class="badge bg-white text-dark border me-2"><%= Common.getBahasaConfig("NIS") %>: <strong><%= nis %></strong></span>
                        <span class="badge bg-white text-dark border me-2"><%= Common.getBahasaConfig("Kelas") %>: <strong><%= kelasSiswa %></strong></span>
                        <span class="badge bg-white text-dark border"><%= Common.getBahasaConfig("Wali Kelas") %>: <strong><%= waliKelas %></strong></span>
                    </p>
                </div>
            </div>
            <% } %>

            <div class="card shadow-sm border-0 rounded-4 profile-card-<%=rnd%> mb-4 overflow-hidden">
                <div class="card-header bg-gradient-header-<%=rnd%> border-bottom-0 pt-4 pb-3 px-4 text-center">
                    <h5 class="fw-bold text-dark mb-4">
                        <i class="fas fa-id-badge text-primary me-2 fs-4"></i><%= Common.getBahasaConfig("Profil Calon Siswa") %>
                    </h5>
                    
                    <div class="d-flex flex-wrap justify-content-center gap-2 mb-2 action-buttons-<%=rnd%>">
                        <% if (tampilAlur) { %>
                            <button type="button" class="btn btn-outline-dark"><i class="fas fa-sitemap me-2 text-primary"></i><%= Common.getBahasaConfig("Alur Pendaftaran") %></button>
                        <% } %>
                        
                        <% if (tampilLengkapiBerkas) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalEditBiodata<%=rnd%>('<%=casis.getId()%>')">
                                <i class="fas fa-user-edit me-2 text-success"></i><%= Common.getBahasaConfig("Lengkapi Biodata & Berkas") %>
                            </button>
                        <% } %>

                        <% if (tampilUjian) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalIkutUjianPSB<%=rnd%>('<%=casis.getId()%>')"><i class="fas fa-pencil-square me-2 text-danger"></i><%= Common.getBahasaConfig("Ikut Ujian Sekarang") %></button>
                        <% } %>

                        <% if (tampilCetakNoReg) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalCetakPDFPSB<%=rnd%>('_cetak_kartu_pendaftaran')"><i class="fas fa-print me-2 text-dark"></i><%= Common.getBahasaConfig("Cetak No. Registrasi") %></button>
                        <% } %>
                        
                        <% if (tampilCetakBiodata) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalCetakPDFPSB<%=rnd%>('_cetak_biodata_calon_siswa')"><i class="fas fa-user-circle me-2 text-primary"></i><%= Common.getBahasaConfig("Cetak Biodata") %></button>
                        <% } %>
                        
                        <% if (tampilCetakKartuUjian) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalCetakPDFPSB<%=rnd%>('_cetak_kartu_ujian')"><i class="fas fa-id-card-alt me-2 text-info"></i><%= Common.getBahasaConfig("Cetak Kartu Ujian") %></button>
                        <% } %>

                        <% if (tampilKetDiterima && casis.getTelahDiterima()) { %>
                            <button type="button" class="btn btn-success"><i class="fas fa-check-square-o me-2"></i><%= Common.getBahasaConfig("Bukti Diterima") %></button>
                        <% } %>

                        <% if (tampilWawancara) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalWawancaraPSB<%=rnd%>('<%=casis.getId()%>')">
                                <i class="fas fa-comments me-2 text-primary"></i><%= Common.getBahasaConfig("Wawancara (Interview)") %>
                            </button>
                        <% } %>

                        <% if (tampilLogout) { %>
                            <button type="button" class="btn btn-danger" onclick="if(typeof window.konfirmasiLogoutPSB === 'function') window.konfirmasiLogoutPSB();"><i class="fas fa-sign-out-alt me-2"></i><%= Common.getBahasaConfig("Keluar") %></button>
                        <% } %>
                    </div>
                </div>

                <div class="card-body p-4 p-md-5">
                    <div class="row align-items-start">
                        <div class="col-md-4 col-lg-3 text-center mb-4 mb-md-0">
                            <div class="position-relative d-inline-block">
                                <img src="<%= fotoUrl %>" alt="Foto" class="foto-mhs-<%=rnd%> rounded-4 shadow-sm" onerror="this.src='<%=Common.ROOT%>/img/default-avatar.png'">
                                <span class="position-absolute top-100 start-50 translate-middle badge bg-dark bg-gradient rounded-pill px-3 py-2 shadow border border-2 border-white">
                                    ID: <%= casis.getId() %>
                                </span>
                            </div>
                        </div>

                        <div class="col-md-8 col-lg-9">
                            <table class="table table-borderless table-profile-<%=rnd%> m-0 w-100">
                                <tbody>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Gelombang") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fw-semibold text-primary"><%= namaGelombang %></span></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Periode") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= periode %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Sekolah") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= sekolah %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Penjurusan") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= penjurusan %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("No. Registrasi") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fw-bold fs-6"><%= noRegistrasi %></span></td>
                                    </tr>
                                    <% if (tampilUjian) { %>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("No. Ujian") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fw-bold text-danger fs-6"><%= noUjian %></span></td>
                                    </tr>
                                    <% } %>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Nama Lengkap") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fw-bold text-dark fs-5"><%= namaLengkap %></span></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Tempat dan Tanggal Lahir") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= ttl %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Nama Orang Tua") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= namaOrtu %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Telp. Orang Tua") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= telpOrtu %></td>
                                    </tr>
                                    <% if (casis.getJadwalPertemuanPSB() != null && casis.getJadwalPertemuanPSB().getAktif()) { %>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Jadwal Pertemuan Siswa / Orang Tua") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= jadwalPertemuan %></td>
                                    </tr>
                                    <% } %>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Status Diterima") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fst-italic <%= casis.getTelahDiterima() ? "fw-bold text-success" : "" %>"><%= diterima %></span></td>
                                    </tr>
                                    <% if (!ketStatus.equals("-")) { %>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Keterangan Status") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= ketStatus %></td>
                                    </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <% if (tampilFormTambahan && hasParam) { %>
            <div class="card shadow-sm border-0 rounded-4 profile-card-<%=rnd%> mb-4 overflow-hidden" style="border-top-color: #17a2b8;">
                <div class="card-header bg-gradient-header-<%=rnd%> border-bottom-0 pt-4 pb-3 px-4">
                    <h5 class="fw-bold text-dark mb-0"><i class="fas fa-clipboard-list text-info me-2 fs-5"></i><%= Common.getBahasaConfig("Form Calon Siswa (Informasi Tambahan)") %></h5>
                </div>
                <div class="card-body p-4">
                    <div class="table-responsive">
                        <table class="table table-borderless table-profile-<%=rnd%> m-0 w-100">
                            <tbody>
                                <%
                                String[] barisParams = paramRaw.split("\n");
                                for(String baris : barisParams) {
                                    if(baris.trim().isEmpty()) continue;
                                    String[] col = baris.split("<=>");
                                    String label = col[0].replace("->", " - ");
                                    String val = col.length > 1 ? col[1] : "-";
                                    String url = col.length > 2 ? col[2] : "";
                                    
                                    if (val.trim().isEmpty() || val.equals("null")) val = "-";
                                %>
                                <tr>
                                    <td><%= label %></td>
                                    <td><span class="d-none d-sm-inline me-2 text-muted">:</span>
                                        <% if (!url.trim().isEmpty() && !url.equals("null")) { %>
                                            <a href="<%= url %>" target="_blank" class="btn btn-sm btn-outline-primary rounded-pill px-3 shadow-sm"><i class="fas fa-external-link-alt me-2"></i><%= Common.getBahasaConfig("Lihat Tautan") %></a>
                                        <% } else { %>
                                            <span class="fw-semibold text-dark"><%= val %></span>
                                        <% } %>
                                    </td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <% } %>

            <% if (tampilFormLampiran && !kelengkapanList.isEmpty()) { %>
            <div class="card shadow-sm border-0 rounded-4 profile-card-<%=rnd%> mb-4 overflow-hidden" style="border-top-color: #ffc107;">
                <div class="card-header bg-gradient-header-<%=rnd%> border-bottom-0 pt-4 pb-3 px-4">
                    <h5 class="fw-bold text-dark mb-0"><i class="fas fa-folder-open text-warning me-2 fs-5"></i><%= Common.getBahasaConfig("Berkas Calon Siswa") %></h5>
                </div>
                <div class="card-body p-4">
                    <div class="table-responsive border border-secondary border-opacity-25 rounded-3">
                        <table class="table table-hover table-striped align-middle mb-0">
                            <thead class="table-light text-secondary">
                                <tr>
                                    <th width="5%" class="text-center py-3">#</th>
                                    <th width="35%" class="py-3"><i class="far fa-file-alt me-2"></i><%= Common.getBahasaConfig("Nama Kelengkapan") %></th>
                                    <th width="15%" class="text-center py-3"><i class="fas fa-cloud-upload-alt me-2"></i><%= Common.getBahasaConfig("Status Berkas") %></th>
                                    <th width="15%" class="text-center py-3"><i class="fas fa-user-check me-2"></i><%= Common.getBahasaConfig("Verifikasi") %></th>
                                    <th width="30%" class="py-3"><i class="far fa-comment-dots me-2"></i><%= Common.getBahasaConfig("Catatan") %></th>
                                </tr>
                            </thead>
                            <tbody>
                                <%
                                int noBerkas = 1;
                                for (Map<String, Object> map : kelengkapanList) {
                                    String nama = (String) map.get("nama");
                                    boolean verified = (Boolean) map.get("verified");
                                    String keterangan = (String) map.get("keterangan");
                                    boolean adaFile = (Boolean) map.get("adaFile");
                                    String urlDownload = (String) map.get("urlDownload");
                                    
                                    if (keterangan == null || keterangan.trim().isEmpty()) {
                                        keterangan = "<span class='text-muted fst-italic'>" + Common.getBahasaConfig("Tidak ada catatan") + "</span>";
                                    }
                                    
                                    String badgeUnggah = adaFile ? 
                                        "<span class='badge bg-success rounded-pill px-3 py-2 shadow-sm mb-1 d-inline-block'><i class='fas fa-check me-1'></i>" + Common.getBahasaConfig("Diunggah") + "</span>" : 
                                        "<span class='badge bg-danger rounded-pill px-3 py-2 shadow-sm d-inline-block'><i class='fas fa-times me-1'></i>" + Common.getBahasaConfig("Belum") + "</span>";
                                        
                                    if(adaFile && urlDownload != null && !urlDownload.trim().isEmpty()){
                                        badgeUnggah += "<br><a href='" + urlDownload + "' target='_blank' class='btn btn-xs btn-outline-primary rounded-pill mt-1' style='font-size: 0.75rem;'><i class='fas fa-download me-1'></i>" + Common.getBahasaConfig("Unduh") + "</a>";
                                    }
                                        
                                    String badgeVerifikasi = verified ? 
                                        "<span class='badge bg-primary rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-check-double me-1'></i>" + Common.getBahasaConfig("Valid") + "</span>" : 
                                        "<span class='badge bg-secondary rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-hourglass-half me-1'></i>" + Common.getBahasaConfig("Menunggu") + "</span>";
                                %>
                                <tr>
                                    <td class="text-center text-muted fw-bold"><%= noBerkas++ %></td>
                                    <td class="fw-semibold text-dark"><%= nama %></td>
                                    <td class="text-center"><%= badgeUnggah %></td>
                                    <td class="text-center"><%= badgeVerifikasi %></td>
                                    <td class="small"><%= keterangan %></td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <% } %>

        </div>
    </div>
</div>

<script>
// =========================================================================
// FUNGSI UNTUK MEMBUKA POPUP PENGISIAN BIODATA (EDIT MODE)
// =========================================================================
window.bukaModalEditBiodata<%=rnd%> = async function(idCasis) {
    var modalId = 'modalEditCasis<%=rnd%>';
    var existingModal = document.getElementById(modalId);
    if(existingModal) existingModal.remove();

    var endpoint = '<%=Common.ROOT%>/psb?hanya_tampil_jsp=true&p=psb&s=_pendaftaran_siswa&baru=true&id=' + idCasis;
    var titleText = '<%= Common.getBahasaConfigJS("Lengkapi Biodata & Kelengkapan Berkas") %>';

    var modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index: 1055;">' +
            '<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl-custom-<%=rnd%>">' +
                '<div class="modal-content shadow-lg border-0 rounded-4">' +
                    '<div class="modal-header bg-light border-0 py-3 px-4">' +
                        '<h5 class="modal-title fw-bold text-dark">' +
                            '<i class="fas fa-user-edit text-success me-2"></i>' + titleText +
                        '</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0 bg-light" id="bodyModalEditCasis<%=rnd%>">' +
                        '<div class="text-center py-5">' +
                            '<div class="spinner-border text-primary" role="status"></div>' +
                            '<div class="mt-2 text-muted fw-bold"><%= Common.getBahasaConfig("Sedang Memuat Formulir...") %></div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    var modalElement = document.getElementById(modalId);
    new bootstrap.Modal(modalElement).show();

    try {
        var response = await fetch(endpoint);
        if(!response.ok) throw new Error("Gagal load formulir");
        var htmlContent = await response.text();
        
        var bodyEl = document.getElementById('bodyModalEditCasis<%=rnd%>');
        bodyEl.innerHTML = htmlContent;

        var scriptsArray = Array.from(bodyEl.getElementsByTagName('script')); 
        for (var i = 0; i < scriptsArray.length; i++) {
            var oldScript = scriptsArray[i];
            if (oldScript.src && oldScript.src.includes('email-decode')) continue; 
            var scriptNode = document.createElement('script');
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            Array.from(oldScript.attributes).forEach(function(attr) { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
            scriptNode.type = 'text/javascript';
            if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode); }
            else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); }
        }
    } catch (e) {
        document.getElementById('bodyModalEditCasis<%=rnd%>').innerHTML = '<div class="text-center py-5 text-danger"><i class="fas fa-exclamation-triangle fa-3x mb-3"></i><h5><%= Common.getBahasaConfig("Gagal memuat formulir pendaftaran.") %></h5></div>';
    }

    // WAJIB RELOAD KETIKA MODAL DITUTUP AGAR TABEL/PROFIL DIPERBARUI
    modalElement.addEventListener('hidden.bs.modal', function () { 
        this.remove(); 
        window.location.reload(); 
    });
};

/**
 * FUNGSI MODAL WAWANCARA PSB
 * Memuat _wawancara.jsp ke dalam modal Bootstrap fullscreen, mirip pola editBiodata.
 */
window.bukaModalWawancaraPSB<%=rnd%> = async function(idCasis) {
    var modalId = 'modalWawancaraPSB<%=rnd%>';
    var existingModal = document.getElementById(modalId);
    if (existingModal) { existingModal.remove(); }

    var endpoint = '<%=Common.ROOT%>/psb?hanya_tampil_jsp=true&p=psb&s=_wawancara&id=' + idCasis;

    var modalHtml =
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index:1055;">' +
            '<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl-custom-<%=rnd%>">' +
                '<div class="modal-content shadow-lg border-0 rounded-4">' +
                    '<div class="modal-header bg-light border-0 py-3 px-4">' +
                        '<h5 class="modal-title fw-bold text-dark">' +
                            '<i class="fas fa-comments text-primary me-2"></i>' +
                            '<%= Common.getBahasaConfigJS("Wawancara (Interview)") %>' +
                        '</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-3 bg-light" id="bodyModalWawancara<%=rnd%>">' +
                        '<div class="text-center py-5">' +
                            '<div class="spinner-border text-primary" role="status"></div>' +
                            '<div class="mt-2 text-muted fw-bold"><%= Common.getBahasaConfig("Sedang Memuat Data Wawancara...") %></div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    var modalElement = document.getElementById(modalId);
    new bootstrap.Modal(modalElement).show();

    try {
        var response = await fetch(endpoint);
        if (!response.ok) { throw new Error('Gagal load halaman wawancara'); }
        var htmlContent = await response.text();

        var bodyEl = document.getElementById('bodyModalWawancara<%=rnd%>');
        bodyEl.innerHTML = htmlContent;

        var scriptsArray = Array.from(bodyEl.getElementsByTagName('script'));
        for (var i = 0; i < scriptsArray.length; i++) {
            var oldScript = scriptsArray[i];
            if (oldScript.src && oldScript.src.includes('email-decode')) { continue; }
            var scriptNode = document.createElement('script');
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            Array.from(oldScript.attributes).forEach(function(attr) {
                if (attr.name.toLowerCase() !== 'type') { scriptNode.setAttribute(attr.name, attr.value); }
            });
            scriptNode.type = 'text/javascript';
            if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode); }
            else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); }
        }
    } catch (e) {
        document.getElementById('bodyModalWawancara<%=rnd%>').innerHTML =
            '<div class="text-center py-5 text-danger">' +
            '<i class="fas fa-exclamation-triangle fa-3x mb-3"></i>' +
            '<h5><%= Common.getBahasaConfig("Gagal memuat halaman wawancara.") %></h5>' +
            '</div>';
    }

    modalElement.addEventListener('hidden.bs.modal', function () { this.remove(); });
};

/**
 * FUNGSI MODAL IKUT UJIAN ONLINE PSB
 * Memuat _ikut_ujian_online.jsp ke dalam modal Bootstrap fullscreen.
 */
window.bukaModalIkutUjianPSB<%=rnd%> = async function(idCasis) {
    var modalId = 'modalIkutUjianPSB<%=rnd%>';
    var existingModal = document.getElementById(modalId);
    if (existingModal) { existingModal.remove(); }

    var endpoint = '<%=Common.ROOT%>/psb?hanya_tampil_jsp=true&p=psb&s=_ikut_ujian_online&id=' + idCasis;

    var modalHtml =
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index:1055;">' +
            '<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl-custom-<%=rnd%>">' +
                '<div class="modal-content shadow-lg border-0 rounded-4">' +
                    '<div class="modal-header bg-light border-0 py-3 px-4">' +
                        '<h5 class="modal-title fw-bold text-dark">' +
                            '<i class="fas fa-laptop-code text-primary me-2"></i>' +
                            '<%= Common.getBahasaConfigJS("Ujian Seleksi Online") %>' +
                        '</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-3 bg-light" id="bodyModalIkutUjian<%=rnd%>">' +
                        '<div class="text-center py-5">' +
                            '<div class="spinner-border text-primary" role="status"></div>' +
                            '<div class="mt-2 text-muted fw-bold"><%= Common.getBahasaConfig("Sedang Memuat Data Ujian...") %></div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    var modalElement = document.getElementById(modalId);
    new bootstrap.Modal(modalElement).show();

    try {
        var response = await fetch(endpoint);
        if (!response.ok) { throw new Error('Gagal load halaman ujian'); }
        var htmlContent = await response.text();

        var bodyEl = document.getElementById('bodyModalIkutUjian<%=rnd%>');
        bodyEl.innerHTML = htmlContent;

        var scriptsArray = Array.from(bodyEl.getElementsByTagName('script'));
        for (var i = 0; i < scriptsArray.length; i++) {
            var oldScript = scriptsArray[i];
            if (oldScript.src && oldScript.src.includes('email-decode')) { continue; }
            var scriptNode = document.createElement('script');
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            Array.from(oldScript.attributes).forEach(function(attr) {
                if (attr.name.toLowerCase() !== 'type') { scriptNode.setAttribute(attr.name, attr.value); }
            });
            scriptNode.type = 'text/javascript';
            if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode); }
            else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); }
        }
    } catch (e) {
        document.getElementById('bodyModalIkutUjian<%=rnd%>').innerHTML =
            '<div class="text-center py-5 text-danger">' +
            '<i class="fas fa-exclamation-triangle fa-3x mb-3"></i>' +
            '<h5><%= Common.getBahasaConfig("Gagal memuat halaman ujian.") %></h5>' +
            '</div>';
    }

    modalElement.addEventListener('hidden.bs.modal', function () { this.remove(); });
};

/**
 * FUNGSI UNTUK MENCETAK PDF (Termasuk Refresh untuk Kartu Ujian)
 */
window.bukaModalCetakPDFPSB<%=rnd%> = async function(serviceName) {
    const modalId = 'modalCetakPDF<%=rnd%>';
    const existingModal = document.getElementById(modalId);
    if(existingModal) existingModal.remove();
    
    if (typeof window.showLoadingPSB === 'function') window.showLoadingPSB();
    
    try {
        let urlService = '<%=Common.ROOT%>/psb?hanya_tampil_jsp=true&p=psb&s=' + serviceName + '&id=<%=casis.getId()%>';
        const response = await fetch(urlService);
        const data = await response.json();
        
        if(data.status === 'success' && data.url) {
            let judulModal = '<%= Common.getBahasaConfigJS("Dokumen Pendaftaran") %>';
            if (serviceName.includes('kartu_ujian')) judulModal = '<%= Common.getBahasaConfigJS("Kartu Ujian Calon Siswa") %>';
            else if (serviceName.includes('biodata')) judulModal = '<%= Common.getBahasaConfigJS("Biodata Calon Siswa") %>';
            else if (serviceName.includes('registrasi')) judulModal = '<%= Common.getBahasaConfigJS("Bukti Registrasi Pendaftaran") %>';

            var modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 1080;">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                    '<div class="modal-content rounded-4 border-0 shadow-lg">' +
                        '<div class="modal-header bg-light border-0 py-3">' +
                            '<h5 class="modal-title fw-bold text-primary"><i class="fas fa-file-pdf me-2"></i>' + judulModal + '</h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0 bg-secondary" style="height: 80vh;">' +
                            '<iframe src="' + data.url + '" style="width:100%; height:100%; border:none;"></iframe>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            let modalEl = document.getElementById(modalId);
            let modalObj = new bootstrap.Modal(modalEl);
            
            if (typeof window.hideLoadingPSB === 'function') window.hideLoadingPSB();
            modalObj.show();
            
            modalEl.addEventListener('hidden.bs.modal', function () { 
                this.remove(); 
                if (serviceName.includes('kartu_ujian')) window.location.reload();
            });
        } else {
            if (typeof window.hideLoadingPSB === 'function') window.hideLoadingPSB();
            if (typeof tampilkanToast === 'function') tampilkanToast(data.message || '<%= Common.getBahasaConfigJS("Gagal membuat dokumen PDF.") %>', 'bg-danger text-white');
            else alert(data.message || '<%= Common.getBahasaConfigJS("Gagal membuat dokumen PDF.") %>');
        }
    } catch (e) {
        if (typeof window.hideLoadingPSB === 'function') window.hideLoadingPSB();
        console.error(e);
        if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat mencetak PDF.") %>', 'bg-danger text-white');
        else alert('<%= Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat mencetak PDF.") %>');
    }
};
</script>