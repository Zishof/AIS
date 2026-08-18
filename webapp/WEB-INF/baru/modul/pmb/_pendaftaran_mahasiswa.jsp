<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.List"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="ais.action.master.KonfigurasiTampilanBiodataCalonMahasiswaAction"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.GelombangPendaftaran"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.AfiliasiCalonMahasiswa"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // =========================================================================
    // 1. HELPER CLASS UNTUK LOGIKA VISIBILITAS & REQUIRED[cite: 6]
    // =========================================================================
    class FieldConfig {
        Tbmuser u; boolean b;
        public FieldConfig(Tbmuser tbmuser, boolean berbintang) { this.u = tbmuser; this.b = berbintang; }
        public boolean isVisible(String field) {
            String stat = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(u, field);
            if (stat == null ) stat = Konfigurasi.AKTIF;
            boolean vis = stat.equals(Konfigurasi.AKTIF) || stat.equals(Konfigurasi.AKTIF_TIDAK_WAJIB);
            return vis;
        }
        public boolean isRequired(String field) {
            String stat = KonfigurasiTampilanBiodataCalonMahasiswaAction.statusWajibIsi(u, field);
            if (stat == null ) stat = Konfigurasi.AKTIF;
            boolean req = stat.equals(Konfigurasi.AKTIF) && b;
            return req;
        }
        public String reqClass(String field) { return isRequired(field) ? "required" : ""; }
        public String reqAttr(String field) { return isRequired(field) ? "required" : ""; }

    }

    // =========================================================================
    // 2. INISIALISASI PARAMETER UTAMA[cite: 6]
    // =========================================================================
    String rnd = Common.getGeneratedBarCode(7);
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    String gelombangId = request.getParameter("gelombangId");
    GelombangPendaftaran gelombangPendaftaran = (gelombangId == null || gelombangId.trim().isEmpty()) ? null : 
        (GelombangPendaftaran) GeneralValueObject.ambilData(GelombangPendaftaran.class, gelombangId.trim(), true);

    String biodataCalonMahasiswaId = request.getParameter("id");
    BiodataCalonMahasiswa biodataCalonMahasiswa = (biodataCalonMahasiswaId == null || biodataCalonMahasiswaId.trim().isEmpty()) ? null : 
        (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, biodataCalonMahasiswaId.trim(), true);
    boolean modeEdit = (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null);
    Long randomPhotoId = -(long)(Math.random() * 1000000000L);
    String idUploadFoto = modeEdit ? biodataCalonMahasiswa.getId().toString() : randomPhotoId.toString();
    
    if (biodataCalonMahasiswa == null) biodataCalonMahasiswa = new BiodataCalonMahasiswa();
    else if (biodataCalonMahasiswa.getGelombangPendaftaran() != null) gelombangPendaftaran = biodataCalonMahasiswa.getGelombangPendaftaran();

    String tahunAkademik = gelombangPendaftaran == null ? Common.getCurrentTahunAkademik() : gelombangPendaftaran.getTahunAkademik();
    String namaGelombang = gelombangPendaftaran == null ? "-" : gelombangPendaftaran.getNama();
    String idGelombang = gelombangPendaftaran == null ? "" : String.valueOf(gelombangPendaftaran.getId());
    boolean allowSelectGelombang = (gelombangPendaftaran == null || modeEdit);

    biodataCalonMahasiswa.setGelombangPendaftaran(gelombangPendaftaran);
    biodataCalonMahasiswa.setTahunAkademik(tahunAkademik);

    String judulNamaPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
    String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
    String Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
    String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
    
    // 1. Identifikasi apakah ini pengguna baru (belum login/belum punya akun)
    boolean isUserBaru = (tbmuser == null || tbmuser.getUserId() == null);

    // 2. Tentukan variabel berbintang
    boolean berbintang;

    if (isUserBaru) {
        berbintang = true;
    } else {
        berbintang = (gelombangPendaftaran != null && Boolean.TRUE.equals(gelombangPendaftaran.getDokumenHarusDiverivikasiSebelumBisaSimpan()));
    }

    FieldConfig fc = new FieldConfig(tbmuser, berbintang);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    // =========================================================================
    // 3. KONFIGURASI SURVEI SUMBER INFORMASI PMB[cite: 6]
    // =========================================================================
    boolean tampilkanInfoPMB = Common.getKonfigurasi("tampilkan_info_sekolah_dari_mana_pada_pmb", Konfigurasi.TIDAK_AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);
    String opsiInfoPmbStr = Common.getKonfigurasi("info_dari_mana_pmb", "Website,Teman,Radio,Koran,Lain-lain").getNilai();
    boolean pilihSalahSatuInfoPMB = Common.getKonfigurasi("pilih_salah_satu_info_pmb_dari_mana", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);
    
    // =========================================================================
    // 4. EKSTRAKSI VARIABEL VIEW UNTUK PRESET NILAI[cite: 6]
    // =========================================================================
    Long paketId = biodataCalonMahasiswa.getPaket() == null ? null : biodataCalonMahasiswa.getPaket().getId();
    String program = biodataCalonMahasiswa.getProgram() == null ? "" : biodataCalonMahasiswa.getProgram();
    Long prodi1 = biodataCalonMahasiswa.getProdi1() == null ? null : biodataCalonMahasiswa.getProdi1().getId();
    Long prodi2 = biodataCalonMahasiswa.getProdi2() == null ? null : biodataCalonMahasiswa.getProdi2().getId();
    Long prodi3 = biodataCalonMahasiswa.getProdi3() == null ? null : biodataCalonMahasiswa.getProdi3().getId();
    Long prodi4 = biodataCalonMahasiswa.getProdi4() == null ? null : biodataCalonMahasiswa.getProdi4().getId();
    Long prodi5 = biodataCalonMahasiswa.getProdi5() == null ? null : biodataCalonMahasiswa.getProdi5().getId();

    String vGelar = modeEdit && biodataCalonMahasiswa.getGelar() != null ? biodataCalonMahasiswa.getGelar() : "";
    String vNama = modeEdit && biodataCalonMahasiswa.getNama() != null ? biodataCalonMahasiswa.getNama().replace("\"", "&quot;") : "";
    String vNoIdentitas = modeEdit && biodataCalonMahasiswa.getNoIdentitas() != null ? biodataCalonMahasiswa.getNoIdentitas() : "";
    String vKk = modeEdit && biodataCalonMahasiswa.getKk() != null ? biodataCalonMahasiswa.getKk() : "";
    String vNisn = modeEdit && biodataCalonMahasiswa.getNisn() != null ? biodataCalonMahasiswa.getNisn() : "";
    String vTempatLahir = modeEdit && biodataCalonMahasiswa.getTempatLahir() != null ? biodataCalonMahasiswa.getTempatLahir().replace("\"", "&quot;") : "";
    String vTanggalLahir = modeEdit && biodataCalonMahasiswa.getTanggalLahir() != null ? sdf.format(biodataCalonMahasiswa.getTanggalLahir()) : "";
    String vKelamin = modeEdit && biodataCalonMahasiswa.getJenisKelamin() != null ? biodataCalonMahasiswa.getJenisKelamin() : "";
    String vStatusNikah = modeEdit && biodataCalonMahasiswa.getStatusNikah() != null ? String.valueOf(biodataCalonMahasiswa.getStatusNikah()) : "";
    String vKewarganegaraan = modeEdit && biodataCalonMahasiswa.getKewarganegaraan() != null ? biodataCalonMahasiswa.getKewarganegaraan() : "WNI";
    String vAsalNegara = modeEdit && biodataCalonMahasiswa.getAsalNegara() != null ? String.valueOf(biodataCalonMahasiswa.getAsalNegara().getId()) : "";
    String vHp = modeEdit && biodataCalonMahasiswa.getHp() != null ? biodataCalonMahasiswa.getHp() : "";
    String vTeleponRumah = modeEdit && biodataCalonMahasiswa.getTeleponRumah() != null ? biodataCalonMahasiswa.getTeleponRumah() : "";
    String vEmail = modeEdit && biodataCalonMahasiswa.getEmail() != null ? biodataCalonMahasiswa.getEmail() : "";
    String vAlamat = modeEdit && biodataCalonMahasiswa.getAlamat() != null ? biodataCalonMahasiswa.getAlamat().replace("\"", "&quot;") : "";
    String vDusunCalon = modeEdit && biodataCalonMahasiswa.getDusunCalon() != null ? biodataCalonMahasiswa.getDusunCalon().replace("\"", "&quot;") : "";
    String vRt = modeEdit && biodataCalonMahasiswa.getRt() != null ? biodataCalonMahasiswa.getRt() : "";
    String vRw = modeEdit && biodataCalonMahasiswa.getRw() != null ? biodataCalonMahasiswa.getRw() : "";
    String vKodePos = modeEdit && biodataCalonMahasiswa.getKodePos() != null ? biodataCalonMahasiswa.getKodePos() : "";
    String vKelurahanCalon = modeEdit && biodataCalonMahasiswa.getKelurahanCalon() != null ? biodataCalonMahasiswa.getKelurahanCalon().replace("\"", "&quot;") : "";
    Long vKecamatanCalon = modeEdit && biodataCalonMahasiswa.getKecamatanCalon() != null ? biodataCalonMahasiswa.getKecamatanCalon().getId() : null;
    String vKecamatanCalonNama = modeEdit && biodataCalonMahasiswa.getKecamatanCalon() != null ? biodataCalonMahasiswa.getKecamatanCalon().getNama().replace("\"", "&quot;") : "";
    Long vNamaSekolahAsal = modeEdit && biodataCalonMahasiswa.getNamaSekolahAsal() != null ? biodataCalonMahasiswa.getNamaSekolahAsal().getId() : null;
    String vNamaSekolahAsalNama = modeEdit && biodataCalonMahasiswa.getNamaSekolahAsal() != null ? biodataCalonMahasiswa.getNamaSekolahAsal().getNama().replace("\"", "&quot;") : "";
    String vJurusanSekolahLain = modeEdit && biodataCalonMahasiswa.getJurusanSekolahLain() != null ? biodataCalonMahasiswa.getJurusanSekolahLain().replace("\"", "&quot;") : "";
    String vAkreditasiSekolah = modeEdit && biodataCalonMahasiswa.getAkreditasiSekolah() != null ? biodataCalonMahasiswa.getAkreditasiSekolah() : "";
    String vAlamatAsalSma = modeEdit && biodataCalonMahasiswa.getAlamatAsalSma() != null ? biodataCalonMahasiswa.getAlamatAsalSma().replace("\"", "&quot;") : "";
    String vKodePosSekolah = modeEdit && biodataCalonMahasiswa.getKodePosSekolah() != null ? biodataCalonMahasiswa.getKodePosSekolah() : "";
    String vNoTelpSekolah = modeEdit && biodataCalonMahasiswa.getNoTelpSekolah() != null ? biodataCalonMahasiswa.getNoTelpSekolah() : "";
    String vTahunLulus = biodataCalonMahasiswa.getTahunKelulusan();
    
    String vNamaAyah = modeEdit && biodataCalonMahasiswa.getNamaAyah() != null ? biodataCalonMahasiswa.getNamaAyah().replace("\"", "&quot;") : "";
    String vNamaIbu = modeEdit && biodataCalonMahasiswa.getNamaIbu() != null ? biodataCalonMahasiswa.getNamaIbu().replace("\"", "&quot;") : "";
    String vNamaWali = modeEdit && biodataCalonMahasiswa.getNamaWali() != null ? biodataCalonMahasiswa.getNamaWali().replace("\"", "&quot;") : "";
    String vAlamatOrtu = modeEdit && biodataCalonMahasiswa.getAlamatOrtu() != null ? biodataCalonMahasiswa.getAlamatOrtu().replace("\"", "&quot;") : "";
    String vRtOrtu = modeEdit && biodataCalonMahasiswa.getRtOrtu() != null ? biodataCalonMahasiswa.getRtOrtu() : "";
    String vRwOrtu = modeEdit && biodataCalonMahasiswa.getRwOrtu() != null ? biodataCalonMahasiswa.getRwOrtu() : "";
    String vKodePosOrtu = modeEdit && biodataCalonMahasiswa.getKodePosOrtu() != null ? biodataCalonMahasiswa.getKodePosOrtu() : "";
    String vKelurahanOrtu = modeEdit && biodataCalonMahasiswa.getKelurahanOrtu() != null ? biodataCalonMahasiswa.getKelurahanOrtu().replace("\"", "&quot;") : "";
    String vNoTelpOrtu = modeEdit && biodataCalonMahasiswa.getNoTelpOrtu() != null ? biodataCalonMahasiswa.getNoTelpOrtu() : "";
    boolean vAlamatSama = modeEdit && biodataCalonMahasiswa.getAlamatSama() != null ? biodataCalonMahasiswa.getAlamatSama() : false;
    
 // =========================================================================
    // 4B. PENGECEKAN PARAMETER KODE AFILIASI (NEW) 
    // =========================================================================
    String vAfiliasi = modeEdit && biodataCalonMahasiswa.getAfiliasiCalonMahasiswa() != null ? String.valueOf(biodataCalonMahasiswa.getAfiliasiCalonMahasiswa().getId()) : "";
    String paramAfiliasiKode = request.getParameter("afiliasi"); // Menangkap parameter kode afiliasi dari URL
    boolean isAfiliasiLockedByParam = false;
    String afiliasiNamaLocked = "";
    
    // Jika tidak sedang mode edit (pendaftar baru) dan parameter afiliasi diberikan
    if (!modeEdit && paramAfiliasiKode != null && !paramAfiliasiKode.trim().isEmpty()) {
        org.hibernate.Session sessionAfiliasi = null;
        try {
            // Membuka session Hibernate secara manual
            sessionAfiliasi = ais.database.hibernate.HibernateUtil.openSession();
            
            // Pencarian data manual menggunakan Hibernate Criteria berdasarkan kolom "kode"[cite: 7]
            java.util.List<AfiliasiCalonMahasiswa> listAfiliasi = sessionAfiliasi.createCriteria(AfiliasiCalonMahasiswa.class)
                .add(org.hibernate.criterion.Restrictions.eq("kode", paramAfiliasiKode.trim()))
                .list();
                
            if (listAfiliasi != null && !listAfiliasi.isEmpty()) {
                AfiliasiCalonMahasiswa afiliasiFound = listAfiliasi.get(0);
                if (Boolean.TRUE.equals(afiliasiFound.getAktif())) { // Memastikan afiliasi tersebut aktif[cite: 7]
                    vAfiliasi = String.valueOf(afiliasiFound.getId());
                    afiliasiNamaLocked = afiliasiFound.getNama(); // Mengambil nama untuk ditampilkan di UI[cite: 7]
                    isAfiliasiLockedByParam = true;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa.jsp:177");
            // Abaikan kesalahan pembacaan database, biarkan pendaftar memilih manual
        } finally {
            if (sessionAfiliasi != null) {
                try { sessionAfiliasi.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa.jsp:181");}
                try { sessionAfiliasi.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa.jsp:182");}
                try { sessionAfiliasi.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa.jsp:183");}
            }
        }
    }
    // =========================================================================

    boolean vMerupakanPindahan = modeEdit && biodataCalonMahasiswa.getMerupakanPindahan() != null ? biodataCalonMahasiswa.getMerupakanPindahan() : false;
    String vPindahanDariKampus = modeEdit && biodataCalonMahasiswa.getPindahanDariKampus() != null ? biodataCalonMahasiswa.getPindahanDariKampus().replace("\"", "&quot;") : "";
    String vPindahanDariProdi = modeEdit && biodataCalonMahasiswa.getPindahanDariProdi() != null ? biodataCalonMahasiswa.getPindahanDariProdi().replace("\"", "&quot;") : "";
    String vNimLamaSebelumPindah = modeEdit && biodataCalonMahasiswa.getNimLamaSebelumPindah() != null ? biodataCalonMahasiswa.getNimLamaSebelumPindah() : "";
    String vPindahDariKampusLamaDiSemester = modeEdit && biodataCalonMahasiswa.getPindahDariKampusLamaDiSemester() != null ? String.valueOf(biodataCalonMahasiswa.getPindahDariKampusLamaDiSemester()) : "";
    boolean vPernyataan = modeEdit && biodataCalonMahasiswa.getPernyataan() != null ? biodataCalonMahasiswa.getPernyataan() : false;
    String vInfoKampus = modeEdit && biodataCalonMahasiswa.getInfoKampusDariMana() != null ? biodataCalonMahasiswa.getInfoKampusDariMana().toLowerCase() : "";
    String vNamaTemanInfo = modeEdit && biodataCalonMahasiswa.getNamaTemanInfoKampusDariMana() != null ? biodataCalonMahasiswa.getNamaTemanInfoKampusDariMana().replace("\"", "&quot;") : "";
    String vNamaDosenInfo = modeEdit && biodataCalonMahasiswa.getDariNamaDosenKaryawan() != null ? biodataCalonMahasiswa.getDariNamaDosenKaryawan().replace("\"", "&quot;") : "";
    String vKetInfoLain = modeEdit && biodataCalonMahasiswa.getKeteranganInfoKampusDariMana() != null ? biodataCalonMahasiswa.getKeteranganInfoKampusDariMana().replace("\"", "&quot;") : "";
    
    String rawParam = biodataCalonMahasiswa.getParameterTambahan() != null ? biodataCalonMahasiswa.getParameterTambahan().replace("\"", "&quot;") : "";
    String rawParamInds = biodataCalonMahasiswa.getParameterTambahanInds() != null ? biodataCalonMahasiswa.getParameterTambahanInds().replace("\"", "&quot;") : "";
    Long vJenisIdentitas = biodataCalonMahasiswa.getJenisKartuIdentitas() != null ? biodataCalonMahasiswa.getJenisKartuIdentitas().getId() : null;
%>

<input type="hidden" id="rawParameterTambahan<%=rnd%>" value="<%= rawParam %>">
<input type="hidden" id="rawParameterTambahanInds<%=rnd%>" value="<%= rawParamInds %>">

<div class="page-bg-<%=rnd%>"></div>

<div class="hero-pendaftaran-pmb hero-section-<%=rnd%> py-3 mb-4 text-center">
    <div class="container py-2">
        <% if (logo_PerguruanTinggi != null && !logo_PerguruanTinggi.trim().isEmpty()) { %>
            <img src="<%= logo_PerguruanTinggi %>" alt="Logo Institusi" class="img-fluid mb-2" style="max-height: 60px; filter: drop-shadow(0px 4px 6px rgba(0,0,0,0.4));">
        <% } %>
        <h4 class="fw-bold text-white mb-1"><%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : Common.getBahasaConfig("Formulir Pendaftaran") %></h4>
        <p class="text-white opacity-75 mb-2 small"><i class="fas fa-location-dot me-1 text-warning"></i><%= Alamat1 != null ? Alamat1 : "-" %></p>
        <div class="d-flex justify-content-center gap-4 small text-white opacity-75 pb-2">
            <span><i class="fas fa-phone me-1 text-success"></i><%= Telepon != null ? Telepon : "-" %></span>
            <span><i class="fas fa-envelope me-1 text-info"></i><%= Email != null ? Email : "-" %></span>
        </div>
    </div>
</div>

<div class="container-fluid mb-5" style="position: relative; z-index: 1;">
    <div class="row justify-content-center">
        <div class="col-lg-11">
            
            <div class="text-center mb-4">
                <h5 class="fw-bold text-primary"><i class="fas fa-file-signature me-2"></i><%= modeEdit ? Common.getBahasaConfig("Ubah Data Pendaftaran") : Common.getBahasaConfig("Formulir Pendaftaran Mahasiswa Baru") %></h5>
                <p class="text-muted small"><%= Common.getBahasaConfig("Mohon isi data di bawah ini dengan lengkap dan benar secara bertahap.") %></p>
            </div>

            <div class="card form-card-<%=rnd%> p-4 shadow-sm">
                
                <% if(berbintang) { %>
                    <div class="alert alert-info py-2 px-3 small rounded-3 mb-4 border-info">
                        <i class="fas fa-circle-info me-2"></i> <%= Common.getBahasaConfig("Tanda") %> <span class="text-danger fw-bold">*</span> <%= Common.getBahasaConfig("merupakan isian yang wajib diisi.") %>
                    </div>
                <% } %>

                <div class="wizard-nav-pmb wizard-nav-<%=rnd%>">
                    <div class="wizard-step-<%=rnd%> active" id="navStep1<%=rnd%>"><div class="step-icon"><i class="fas fa-user"></i></div><div class="small d-none d-md-block"><%= Common.getBahasaConfig("Identitas Diri") %></div></div>
                    <div class="wizard-step-<%=rnd%>" id="navStep2<%=rnd%>"><div class="step-icon"><i class="fas fa-map-location-dot"></i></div><div class="small d-none d-md-block"><%= Common.getBahasaConfig("Alamat") %></div></div>
                    <div class="wizard-step-<%=rnd%>" id="navStep3<%=rnd%>"><div class="step-icon"><i class="fas fa-school"></i></div><div class="small d-none d-md-block"><%= Common.getBahasaConfig("Pendidikan") %></div></div>
                    <div class="wizard-step-<%=rnd%>" id="navStep4<%=rnd%>"><div class="step-icon"><i class="fas fa-users"></i></div><div class="small d-none d-md-block"><%= Common.getBahasaConfig("Keluarga") %></div></div>
                    <div class="wizard-step-<%=rnd%>" id="navStep5<%=rnd%>"><div class="step-icon"><i class="fas fa-graduation-cap"></i></div><div class="small d-none d-md-block"><%= Common.getBahasaConfig("Lainnya") %></div></div>
                </div>

                <form id="formBiodata<%=rnd%>" novalidate>
                    
                    <div class="wizard-pane-<%=rnd%> active" id="paneStep1<%=rnd%>">
                        <h6 class="fw-bold mb-3 text-primary border-bottom pb-2"><%= Common.getBahasaConfig("Langkah 1: Data & Identitas Diri") %></h6>

                        <div class="row g-3 mb-3">
                            <div class="col-md">
                                <label class="form-label fw-semibold small <%= allowSelectGelombang ? "required" : "text-muted" %>"><%= Common.getBahasaConfig("Tahun Akademik") %></label>
                                <% if (allowSelectGelombang) { %>
                                    <select class="form-select form-select-sm shadow-none border-primary" id="tahunAkademik<%=rnd%>" required onchange="window.onTahunAkademikChange<%=rnd%>()">
                                        <option value=""><%= Common.getBahasaConfig("Pilih TA") %></option>
                                        <% if(Common.tahunAngkatans != null) { 
                                            for(String ta_iter : Common.tahunAngkatans) { %>
                                            <option value="<%= ta_iter %>" <%= ta_iter.equals(tahunAkademik) ? "selected" : "" %>><%= ta_iter %></option>
                                        <% }} %>
                                    </select>
                                <% } else { %>
                                    <div class="readonly-label-pmb readonly-label-<%=rnd%> py-1"><i class="far fa-calendar-days me-2 text-primary"></i><%= tahunAkademik %></div>
                                    <input type="hidden" id="tahunAkademik<%=rnd%>" value="<%= tahunAkademik %>">
                                <% } %>
                            </div>
                            <div class="col-md">
							    <label class="form-label fw-semibold small <%= allowSelectGelombang ? "required" : "text-muted" %>"><%= Common.getBahasaConfig("Gelombang Pendaftaran") %></label>
							    <% if (allowSelectGelombang) { %>
							        <select class="form-select form-select-sm shadow-none border-primary" id="gelombangPendaftaran<%=rnd%>" required onchange="window.onGelombangChange<%=rnd%>(); window.cekWajibFotoGelombang<%=rnd%>();"></select>
							    <% } else { 
							        boolean isFotoWajibFixed = gelombangPendaftaran != null && Boolean.TRUE.equals(gelombangPendaftaran.getFotoWajibDiuplad());
							    %>
							        <div class="readonly-label-pmb readonly-label-<%=rnd%> py-1"><i class="fas fa-tags me-2 text-success"></i><%= namaGelombang %></div>
							        <input type="hidden" id="gelombangPendaftaran<%=rnd%>" value="<%= idGelombang %>" data-foto-wajib="<%= isFotoWajibFixed %>">
							    <% } %>
							</div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-12">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Jenis Seleksi (Jalur Pendaftaran)") %></label>
                                <select class="form-select form-select-sm shadow-none border-primary" id="jenisSeleksi<%=rnd%>" required onchange="window.onSeleksiChange<%=rnd%>()"></select>
                            </div>
                        </div>
                        
                        <hr class="my-3">
                        
                        <div class="row g-3 mb-3">
                            <div class="col-md-3" style="<%= fc.isVisible("gelar") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("gelar")%>"><%= Common.getBahasaConfig("Gelar") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="gelar<%=rnd%>" value="<%= vGelar %>" placeholder="<%= Common.getBahasaConfig("Contoh: S.Kom, dll") %>" <%=fc.reqAttr("gelar")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("nama") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("nama")%>"><%= Common.getBahasaConfig("Nama Lengkap") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="nama<%=rnd%>" value="<%= vNama %>" placeholder="<%= Common.getBahasaConfig("Sesuai Ijazah / KTP") %>" <%=fc.reqAttr("nama")%>>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md" style="<%= fc.isVisible("jenisKartuIdentitas") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("jenisKartuIdentitas")%>"><%= Common.getBahasaConfig("Jenis Identitas") %></label>
                                <select class="form-select form-select-sm shadow-none" id="jenisKartuIdentitas<%=rnd%>" <%=fc.reqAttr("jenisKartuIdentitas")%>></select>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("noIdentitas") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("noIdentitas")%>"><%= Common.getBahasaConfig("No. Identitas (KTP/SIM)") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="noIdentitas<%=rnd%>" value="<%= vNoIdentitas %>" <%=fc.reqAttr("noIdentitas")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("kk") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("kk")%>"><%= Common.getBahasaConfig("No. Kartu Keluarga (KK)") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="kk<%=rnd%>" value="<%= vKk %>" <%=fc.reqAttr("kk")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("nisn") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("nisn")%>"><%= Common.getBahasaConfig("NISN") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="nisn<%=rnd%>" value="<%= vNisn %>" maxlength="15" <%=fc.reqAttr("nisn")%>>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md" style="<%= fc.isVisible("tempatLahir") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("tempatLahir")%>"><%= Common.getBahasaConfig("Tempat Lahir") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="tempatLahir<%=rnd%>" value="<%= vTempatLahir %>" <%=fc.reqAttr("tempatLahir")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("tanggalLahir") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("tanggalLahir")%>"><%= Common.getBahasaConfig("Tanggal Lahir") %></label>
                                <input type="date" class="form-control form-control-sm shadow-none" id="tanggalLahir<%=rnd%>" value="<%= vTanggalLahir %>" <%=fc.reqAttr("tanggalLahir")%>>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md" style="<%= fc.isVisible("jenisKelamin") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("jenisKelamin")%>"><%= Common.getBahasaConfig("Jenis Kelamin") %></label>
                                <select class="form-select form-select-sm shadow-none" id="jenisKelamin<%=rnd%>" <%=fc.reqAttr("jenisKelamin")%> onchange="window.aturProdiBerdasarkanPaket<%=rnd%>()">
                                    <option value=""><%= Common.getBahasaConfig("Pilih") %></option>
                                    <option value="Laki-laki" <%= "Laki-laki".equals(vKelamin) ? "selected" : "" %>><%= Common.getBahasaConfig("Laki-laki") %></option>
                                    <option value="Perempuan" <%= "Perempuan".equals(vKelamin) ? "selected" : "" %>><%= Common.getBahasaConfig("Perempuan") %></option>
                                </select>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("agama") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("agama")%>"><%= Common.getBahasaConfig("Agama") %></label>
                                <select class="form-select form-select-sm shadow-none" id="agama<%=rnd%>" <%=fc.reqAttr("agama")%>></select>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("statusNikah") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("statusNikah")%>"><%= Common.getBahasaConfig("Status Nikah") %></label>
                                <select class="form-select form-select-sm shadow-none" id="statusNikah<%=rnd%>" <%=fc.reqAttr("statusNikah")%>>
                                    <option value="Belum Kawin" <%= "Belum Kawin".equals(vStatusNikah) ? "selected" : "" %>><%= Common.getBahasaConfig("Belum Kawin") %></option>
                                    <option value="Kawin" <%= "Kawin".equals(vStatusNikah) ? "selected" : "" %>><%= Common.getBahasaConfig("Kawin") %></option>
                                    <option value="Cerai" <%= "Cerai".equals(vStatusNikah) ? "selected" : "" %>><%= Common.getBahasaConfig("Cerai") %></option>
                                </select>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("kewarganegaraan") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("kewarganegaraan")%>"><%= Common.getBahasaConfig("Kewarganegaraan") %></label>
                                <select class="form-select form-select-sm shadow-none" id="kewarganegaraan<%=rnd%>" <%=fc.reqAttr("kewarganegaraan")%> onchange="window.toggleAsalNegara<%=rnd%>()">
                                    <option value="WNI" <%= "WNI".equals(vKewarganegaraan) ? "selected" : "" %>><%= Common.getBahasaConfig("WNI (Indonesia)") %></option>
                                    <option value="WNA" <%= "WNA".equals(vKewarganegaraan) ? "selected" : "" %>><%= Common.getBahasaConfig("WNA (Asing)") %></option>
                                </select>
                            </div>
                        </div>

                        <div style="<%= fc.isVisible("asalNegara") ? "" : "display:none;" %>">
                            <div class="row g-3 mb-3" id="divAsalNegara<%=rnd%>" style="<%= "WNA".equals(vKewarganegaraan) ? "" : "display:none;" %>">
                                <div class="col-12">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("asalNegara")%>"><%= Common.getBahasaConfig("Asal Negara") %></label>
                                    <select class="form-select form-select-sm shadow-none border-warning" id="asalNegara<%=rnd%>" data-wajib="<%=fc.isRequired("asalNegara")%>" <%=fc.reqAttr("asalNegara")%>></select>
                                </div>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("No. Handphone / WA") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="hp<%=rnd%>" value="<%= vHp %>" required>
                            </div>
                            
                            <div class="col-md" style="<%= fc.isVisible("teleponRumah") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("teleponRumah")%>"><%= Common.getBahasaConfig("Telepon Rumah") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="teleponRumah<%=rnd%>" value="<%= vTeleponRumah %>" <%=fc.reqAttr("teleponRumah")%>>
                            </div>

                            <div class="col-md">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Email Aktif") %></label>
                                <input type="email" class="form-control form-control-sm shadow-none" id="email<%=rnd%>" value="<%= vEmail %>" required>
                            </div>
                        </div>

                        <div class="text-end mt-4 pt-3 border-top">
                            <button type="button" class="btn btn-secondary btn-sm px-4 rounded-pill me-2" data-bs-dismiss="modal"><i class="fas fa-xmark me-1"></i> <%= Common.getBahasaConfig("Batal") %></button>
                            <button type="button" class="btn btn-primary btn-sm px-5 rounded-pill" onclick="window.goToStep<%=rnd%>(2)"><%= Common.getBahasaConfig("Selanjutnya") %> <i class="fas fa-arrow-right ms-2"></i></button>
                        </div>
                    </div>

                    <div class="wizard-pane-<%=rnd%>" id="paneStep2<%=rnd%>">
                        <h6 class="fw-bold mb-3 text-primary border-bottom pb-2"><%= Common.getBahasaConfig("Langkah 2: Alamat Tempat Tinggal") %></h6>
                        
                        <div class="row g-3 mb-3">
                            <div class="col-12" style="<%= fc.isVisible("alamat") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("alamat")%>"><%= Common.getBahasaConfig("Alamat / Jalan / Gang Rumah") %></label>
                                <textarea class="form-control form-control-sm shadow-none" id="alamat<%=rnd%>" rows="2" <%=fc.reqAttr("alamat")%>><%= vAlamat %></textarea>
                             </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md" style="<%= fc.isVisible("dusunCalon") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("dusunCalon")%>"><%= Common.getBahasaConfig("Dusun / Kampung") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="dusunCalon<%=rnd%>" value="<%= vDusunCalon %>" <%=fc.reqAttr("dusunCalon")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("rt") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("rt")%>"><%= Common.getBahasaConfig("RT") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="rt<%=rnd%>" value="<%= vRt %>" maxlength="5" <%=fc.reqAttr("rt")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("rw") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("rw")%>"><%= Common.getBahasaConfig("RW") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="rw<%=rnd%>" value="<%= vRw %>" maxlength="5" <%=fc.reqAttr("rw")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("kodePos") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("kodePos")%>"><%= Common.getBahasaConfig("Kode Pos") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="kodePos<%=rnd%>" value="<%= vKodePos %>" maxlength="8" <%=fc.reqAttr("kodePos")%>>
                            </div>
                        </div>
                        
                        <div class="row g-3">
						    <div class="col-md-4" style="<%= fc.isVisible("propinsiCalon") ? "" : "display:none;" %>">
						        <label class="form-label fw-semibold small <%=fc.reqClass("propinsiCalon")%>"><%= Common.getBahasaConfig("Provinsi") %></label>
						        <input type="hidden" id="propinsiCalon<%=rnd%>" name="propinsiCalon" value="<%= (biodataCalonMahasiswa.getPropinsiCalon() != null) ? biodataCalonMahasiswa.getPropinsiCalon().getId() : "" %>">
						        <input type="text" class="form-control form-control-sm shadow-none bg-light fw-bold" 
						               id="textPropinsiCalon<%=rnd%>" 
						               value="<%= (biodataCalonMahasiswa.getPropinsiCalon() != null) ? biodataCalonMahasiswa.getPropinsiCalon().getNama() : "" %>" 
						               placeholder="---" readonly>
						    </div>
						
						    <div class="col-md-4" style="<%= fc.isVisible("kotaCalon") ? "" : "display:none;" %>">
						        <label class="form-label fw-semibold small <%=fc.reqClass("kotaCalon")%>"><%= Common.getBahasaConfig("Kota / Kabupaten") %></label>
						        <input type="hidden" id="kotaCalon<%=rnd%>" name="kotaCalon" value="<%= (biodataCalonMahasiswa.getKotaCalon() != null) ? biodataCalonMahasiswa.getKotaCalon().getId() : "" %>">
						        <input type="text" class="form-control form-control-sm shadow-none bg-light fw-bold" 
						               id="textKotaCalon<%=rnd%>" 
						               value="<%= (biodataCalonMahasiswa.getKotaCalon() != null) ? biodataCalonMahasiswa.getKotaCalon().getNama() : "" %>" 
						               placeholder="---" readonly>
						    </div>
						
						    <div class="col-md-4" style="<%= fc.isVisible("kecamatanCalon") ? "" : "display:none;" %>">
						        <label class="form-label fw-semibold small <%=fc.reqClass("kecamatanCalon")%>"><%= Common.getBahasaConfig("Kecamatan") %></label>
						        <div class="input-group input-group-sm">
						            <input type="hidden" id="kecamatanCalon<%=rnd%>" name="kecamatanCalon" value="<%= (biodataCalonMahasiswa.getKecamatanCalon() != null) ? biodataCalonMahasiswa.getKecamatanCalon().getId() : "" %>">
						            <input type="text" class="form-control shadow-none bg-white" id="textKecamatan<%=rnd%>" 
						                   value="<%= (biodataCalonMahasiswa.getKecamatanCalon() != null) ? biodataCalonMahasiswa.getKecamatanCalon().getNama() : "" %>" 
						                   placeholder="<%= Common.getBahasaConfig("Cari Kecamatan...") %>" readonly <%=fc.reqAttr("kecamatanCalon")%>>
						            <button class="btn btn-outline-primary" type="button" onclick="window.bukaModalCariWilayah<%=rnd%>('pendaftar')">
						                <i class="fas fa-magnifying-glass"></i>
						            </button>
						        </div>
						    </div>
						</div>
                        
                        <div class="row g-3 mb-3">
                            

                            <div class="col-md" style="<%= fc.isVisible("kelurahanCalon") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("kelurahanCalon")%>"><%= Common.getBahasaConfig("Kelurahan / Desa") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="kelurahanCalon<%=rnd%>" value="<%= vKelurahanCalon %>" <%=fc.reqAttr("kelurahanCalon")%>>
                            </div>
                        </div>

                        <div class="d-flex justify-content-between mt-4 pt-3 border-top">
                            <button type="button" class="btn btn-light btn-sm border px-4 rounded-pill" onclick="window.goToStep<%=rnd%>(1)"><i class="fas fa-arrow-left me-2"></i> <%= Common.getBahasaConfig("Kembali") %></button>
                            <button type="button" class="btn btn-primary btn-sm px-5 rounded-pill" onclick="window.goToStep<%=rnd%>(3)"><%= Common.getBahasaConfig("Selanjutnya") %> <i class="fas fa-arrow-right ms-2"></i></button>
                        </div>
                    </div>

                    <div class="wizard-pane-<%=rnd%>" id="paneStep3<%=rnd%>">
                        <h6 class="fw-bold mb-3 text-primary border-bottom pb-2"><%= Common.getBahasaConfig("Langkah 3: Data Pendidikan Asal") %></h6>
                        
                        <div class="row g-3 mb-3">
                            <div class="col-md-4" style="<%= fc.isVisible("jenisSekolah") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("jenisSekolah")%>"><%= Common.getBahasaConfig("Jenis Pendidikan Asal") %></label>
                                <select class="form-select form-select-sm shadow-none border-primary" id="jenisSekolah<%=rnd%>" onchange="window.onJenisSekolahChange<%=rnd%>(this.value);" <%=fc.reqAttr("jenisSekolah")%>></select>
                            </div>

                            <div class="col-md-4" style="<%= fc.isVisible("jurusanSekolah") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("jurusanSekolah")%>"><%= Common.getBahasaConfig("Jurusan Asal") %></label>
                                <select class="form-select form-select-sm shadow-none border-primary" id="jurusanSekolah<%=rnd%>" onchange="window.onJurusanSekolahChange<%=rnd%>();" <%=fc.reqAttr("jurusanSekolah")%>></select>
                            </div>
                        </div>

                        <div style="<%= fc.isVisible("jurusanSekolahLain") ? "" : "display:none;" %>">
                            <div class="row g-3 mb-3" id="divJurusanLain<%=rnd%>" style="display:none;">
                                <div class="col-12">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("jurusanSekolahLain")%>"><%= Common.getBahasaConfig("Deskripsi Jurusan Pendidikan Asal") %></label>
                                    <input type="text" class="form-control form-control-sm shadow-none border-warning" id="jurusanSekolahLain<%=rnd%>" value="<%= vJurusanSekolahLain %>" data-wajib="<%=fc.isRequired("jurusanSekolahLain")%>" <%=fc.reqAttr("jurusanSekolahLain")%>>
                                </div>
                            </div>
                        </div>
                        
                        <div class="row g-3 mb-3">
                            <div class="col-md-10" style="<%= fc.isVisible("asalSma") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("asalSma")%>"><%= Common.getBahasaConfig("Nama Pendidikan Asal (Sekolah)") %></label>
                                <div class="input-group input-group-sm">
                                    <input type="hidden" id="namaSekolahAsal<%=rnd%>" value="<%= vNamaSekolahAsal != null ? vNamaSekolahAsal : "" %>">
                                    <input type="text" class="form-control shadow-none bg-white" id="textNamaSekolah<%=rnd%>" value="<%= vNamaSekolahAsalNama %>" placeholder="<%= Common.getBahasaConfig("Klik cari untuk memilih sekolah") %>" readonly <%=fc.reqAttr("asalSma")%>>
                                    <button class="btn btn-outline-primary" type="button" onclick="window.bukaModalCariSekolah<%=rnd%>()">
                                        <i class="fas fa-magnifying-glass"></i> <%= Common.getBahasaConfig("Cari Sekolah") %>
                                    </button>
                                </div>
                            </div>

                            <div class="col-md-2" style="<%= fc.isVisible("akreditasiSekolah") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("akreditasiSekolah")%>"><%= Common.getBahasaConfig("Akreditasi") %></label>
                                <select class="form-select form-select-sm shadow-none" id="akreditasiSekolah<%=rnd%>" <%=fc.reqAttr("akreditasiSekolah")%>>
                                    <option value="">-</option>
                                    <option value="A" <%= "A".equals(vAkreditasiSekolah) ? "selected" : "" %>>A</option>
                                    <option value="B" <%= "B".equals(vAkreditasiSekolah) ? "selected" : "" %>>B</option>
                                    <option value="C" <%= "C".equals(vAkreditasiSekolah) ? "selected" : "" %>>C</option>
                                </select>
                            </div>
                        </div>
                        
                        <div class="row g-3 mb-3" style="<%= fc.isVisible("alamatAsalSma") ? "" : "display:none;" %>">
                            <div class="col-md">
                                <label class="form-label fw-semibold small <%=fc.reqClass("alamatAsalSma")%>"><%= Common.getBahasaConfig("Alamat Pendidikan Asal") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="alamatAsalSma<%=rnd%>" value="<%= vAlamatAsalSma %>" <%=fc.reqAttr("alamatAsalSma")%>>
                            </div>
                        </div>
                        
                        
                        <div class="row g-3">
						    <div class="col-md-4" style="<%= fc.isVisible("propinsiSekolah") ? "" : "display:none;" %>">
						        <label class="form-label fw-semibold small <%=fc.reqClass("propinsiSekolah")%>"><%= Common.getBahasaConfig("Provinsi Sekolah") %></label>
						        <input type="hidden" id="propinsiSekolah<%=rnd%>" name="propinsiSekolah" value="<%= (biodataCalonMahasiswa.getPropinsiSekolah() != null) ? biodataCalonMahasiswa.getPropinsiSekolah().getId() : "" %>">
						        <input type="text" class="form-control form-control-sm shadow-none bg-light fw-bold" 
						               id="textPropinsiSekolah<%=rnd%>" 
						               value="<%= (biodataCalonMahasiswa.getPropinsiSekolah() != null) ? biodataCalonMahasiswa.getPropinsiSekolah().getNama() : "" %>" 
						               placeholder="---" readonly>
						    </div>
						
						    <div class="col-md-4" style="<%= fc.isVisible("kotaSekolah") ? "" : "display:none;" %>">
						        <label class="form-label fw-semibold small <%=fc.reqClass("kotaSekolah")%>"><%= Common.getBahasaConfig("Kota/Kabupaten Sekolah") %></label>
						        <input type="hidden" id="kotaSekolah<%=rnd%>" name="kotaSekolah" value="<%= (biodataCalonMahasiswa.getKotaSekolah() != null) ? biodataCalonMahasiswa.getKotaSekolah().getId() : "" %>">
						        <input type="text" class="form-control form-control-sm shadow-none bg-light fw-bold" 
						               id="textKotaSekolah<%=rnd%>" 
						               value="<%= (biodataCalonMahasiswa.getKotaSekolah() != null) ? biodataCalonMahasiswa.getKotaSekolah().getNama() : "" %>" 
						               placeholder="---" readonly>
						    </div>
						
						    <div class="col-md-4" style="<%= fc.isVisible("kecamatanSekolah") ? "" : "display:none;" %>">
						        <label class="form-label fw-semibold small <%=fc.reqClass("kecamatanSekolah")%>"><%= Common.getBahasaConfig("Kecamatan Sekolah") %></label>
						        <div class="input-group input-group-sm">
						            <input type="hidden" id="kecamatanSekolah<%=rnd%>" name="kecamatanSekolah" value="<%= (biodataCalonMahasiswa.getKecamatanSekolah() != null) ? biodataCalonMahasiswa.getKecamatanSekolah().getId() : "" %>">
						            <input type="text" class="form-control shadow-none bg-white" id="textKecamatanSekolah<%=rnd%>" 
						                   value="<%= (biodataCalonMahasiswa.getKecamatanSekolah() != null) ? biodataCalonMahasiswa.getKecamatanSekolah().getNama() : "" %>" 
						                   placeholder="<%= Common.getBahasaConfig("Cari Kecamatan...") %>" readonly <%=fc.reqAttr("kecamatanSekolah")%>>
						            <button class="btn btn-outline-primary" type="button" onclick="window.bukaModalCariWilayah<%=rnd%>('sekolah')">
						                <i class="fas fa-magnifying-glass"></i>
						            </button>
						        </div>
						    </div>
						</div>
                        

                        <div class="row g-3 mb-3">
                            <div class="col-md" style="<%= fc.isVisible("kodePosSekolah") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("kodePosSekolah")%>"><%= Common.getBahasaConfig("Kode Pos Sekolah") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="kodePosSekolah<%=rnd%>" value="<%= vKodePosSekolah %>" <%=fc.reqAttr("kodePosSekolah")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("noTelpSekolah") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("noTelpSekolah")%>"><%= Common.getBahasaConfig("No. Telp Sekolah") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="noTelpSekolah<%=rnd%>" value="<%= vNoTelpSekolah %>" <%=fc.reqAttr("noTelpSekolah")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("tahunKelulusan") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("tahunKelulusan")%>"><%= Common.getBahasaConfig("Tahun Kelulusan") %></label>
                                <select class="form-select form-select-sm shadow-none border-primary" id="tahunLulus<%=rnd%>" <%=fc.reqAttr("tahunKelulusan")%>></select>
                            </div>
                        </div>

                        <div class="d-flex justify-content-between mt-4 pt-3 border-top">
                            <button type="button" class="btn btn-light btn-sm border px-4 rounded-pill" onclick="window.goToStep<%=rnd%>(2)"><i class="fas fa-arrow-left me-2"></i> <%= Common.getBahasaConfig("Kembali") %></button>
                            <button type="button" class="btn btn-primary btn-sm px-5 rounded-pill" onclick="window.goToStep<%=rnd%>(4)"><%= Common.getBahasaConfig("Selanjutnya") %> <i class="fas fa-arrow-right ms-2"></i></button>
                        </div>
                    </div>

                    <div class="wizard-pane-<%=rnd%>" id="paneStep4<%=rnd%>">
                        <h6 class="fw-bold mb-3 text-primary border-bottom pb-2"><%= Common.getBahasaConfig("Langkah 4: Data Orang Tua / Wali") %></h6>
                        
                        <div class="row g-3 mb-4">
                            <div class="col-md">
                                <h6 class="fw-bold text-success border-bottom pb-1 small"><i class="fas fa-male me-2"></i><%= Common.getBahasaConfig("Data Ayah") %></h6>
                                <div style="<%= fc.isVisible("namaAyah") ? "" : "display:none;" %>">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("namaAyah")%>"><%= Common.getBahasaConfig("Nama Ayah") %></label>
                                    <input type="text" class="form-control form-control-sm shadow-none mb-2" id="namaAyah<%=rnd%>" value="<%= vNamaAyah %>" <%=fc.reqAttr("namaAyah")%>>
                                </div>
                                
                                <div style="<%= fc.isVisible("pendidikanOrtu") ? "" : "display:none;" %>">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("pendidikanOrtu")%>"><%= Common.getBahasaConfig("Pendidikan Ayah") %></label>
                                    <select class="form-select form-select-sm shadow-none mb-2" id="pendidikanOrtu<%=rnd%>" <%=fc.reqAttr("pendidikanOrtu")%>></select>
                                </div>

                                <div style="<%= fc.isVisible("pekerjaanAyah") ? "" : "display:none;" %>">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("pekerjaanAyah")%>"><%= Common.getBahasaConfig("Pekerjaan Ayah") %></label>
                                    <select class="form-select form-select-sm shadow-none mb-2" id="pekerjaanAyah<%=rnd%>" <%=fc.reqAttr("pekerjaanAyah")%>></select>
                                </div>
                                
                                <div style="<%= fc.isVisible("pendapatanOrtu") ? "" : "display:none;" %>">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("pendapatanOrtu")%>"><%= Common.getBahasaConfig("Pendapatan Ayah") %></label>
                                    <select class="form-select form-select-sm shadow-none" id="pendapatanOrtu<%=rnd%>" <%=fc.reqAttr("pendapatanOrtu")%>></select>
                                </div>
                            </div>
                            
                            <div class="col-md border-start border-light">
                                <h6 class="fw-bold text-success border-bottom pb-1 small"><i class="fas fa-female me-2"></i><%= Common.getBahasaConfig("Data Ibu") %></h6>
                                <div style="<%= fc.isVisible("namaIbu") ? "" : "display:none;" %>">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("namaIbu")%>"><%= Common.getBahasaConfig("Nama Ibu") %></label>
                                    <input type="text" class="form-control form-control-sm shadow-none mb-2" id="namaIbu<%=rnd%>" value="<%= vNamaIbu %>" <%=fc.reqAttr("namaIbu")%>>
                                </div>
                                
                                <div style="<%= fc.isVisible("pendidikanOrtuIbu") ? "" : "display:none;" %>">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("pendidikanOrtuIbu")%>"><%= Common.getBahasaConfig("Pendidikan Ibu") %></label>
                                    <select class="form-select form-select-sm shadow-none mb-2" id="pendidikanOrtuIbu<%=rnd%>" <%=fc.reqAttr("pendidikanOrtuIbu")%>></select>
                                </div>

                                <div style="<%= fc.isVisible("pekerjaanAyahIbu") ? "" : "display:none;" %>">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("pekerjaanAyahIbu")%>"><%= Common.getBahasaConfig("Pekerjaan Ibu") %></label>
                                    <select class="form-select form-select-sm shadow-none mb-2" id="pekerjaanAyahIbu<%=rnd%>" <%=fc.reqAttr("pekerjaanAyahIbu")%>></select>
                                </div>
                                
                                <div style="<%= fc.isVisible("pendapatanOrtuIbu") ? "" : "display:none;" %>">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("pendapatanOrtuIbu")%>"><%= Common.getBahasaConfig("Pendapatan Ibu") %></label>
                                    <select class="form-select form-select-sm shadow-none" id="pendapatanOrtuIbu<%=rnd%>" <%=fc.reqAttr("pendapatanOrtuIbu")%>></select>
                                </div>
                            </div>
                        </div>

                        <div style="<%= fc.isVisible("alamatOrtu") ? "" : "display:none;" %>">
                            <div class="col-12 mt-2">
                                <div class="form-check form-switch mb-3 bg-light py-1 px-2 rounded border border-secondary">
                                    <input class="form-check-input ms-1 me-2" type="checkbox" id="alamatSama<%=rnd%>" onchange="window.syncAlamatOrtu<%=rnd%>()" <%= vAlamatSama ? "checked" : "" %>>
                                    <label class="form-check-label fw-bold text-primary small" for="alamatSama<%=rnd%>"><%= Common.getBahasaConfig("Alamat Orang Tua SAMA dengan alamat Pendaftar") %></label>
                                </div>
                            </div>
                            <div class="row g-3 mb-3">
                                <div class="col-12">
                                    <label class="form-label fw-semibold small <%=fc.reqClass("alamatOrtu")%>"><%= Common.getBahasaConfig("Alamat Lengkap Orang Tua") %></label>
                                    <textarea class="form-control form-control-sm shadow-none" id="alamatOrtu<%=rnd%>" rows="2" <%=fc.reqAttr("alamatOrtu")%>><%= vAlamatOrtu %></textarea>
                                </div>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md" style="<%= fc.isVisible("rtOrtu") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("rtOrtu")%>"><%= Common.getBahasaConfig("RT") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="rtOrtu<%=rnd%>" value="<%= vRtOrtu %>" maxlength="5" <%=fc.reqAttr("rtOrtu")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("rwOrtu") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("rwOrtu")%>"><%= Common.getBahasaConfig("RW") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="rwOrtu<%=rnd%>" value="<%= vRwOrtu %>" maxlength="5" <%=fc.reqAttr("rwOrtu")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("kodePosOrtu") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("kodePosOrtu")%>"><%= Common.getBahasaConfig("Kode Pos") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="kodePosOrtu<%=rnd%>" value="<%= vKodePosOrtu %>" maxlength="8" <%=fc.reqAttr("kodePosOrtu")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("noTelpOrtu") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("noTelpOrtu")%>"><%= Common.getBahasaConfig("No. Telp / HP Orang Tua") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="noTelpOrtu<%=rnd%>" value="<%= vNoTelpOrtu %>" <%=fc.reqAttr("noTelpOrtu")%>>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md" style="<%= fc.isVisible("kelurahanOrtu") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("kelurahanOrtu")%>"><%= Common.getBahasaConfig("Kelurahan / Desa Orang Tua") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="kelurahanOrtu<%=rnd%>" value="<%= vKelurahanOrtu %>" <%=fc.reqAttr("kelurahanOrtu")%>>
                            </div>
                        </div>
                        
                        <div class="row g-3">
						    <div class="col-md-4" style="<%= fc.isVisible("propinsiOrtu") ? "" : "display:none;" %>">
						        <label class="form-label fw-semibold small <%=fc.reqClass("propinsiOrtu")%>"><%= Common.getBahasaConfig("Provinsi Orang Tua") %></label>
						        <input type="hidden" id="propinsiOrtu<%=rnd%>" name="propinsiOrtu" value="<%= (biodataCalonMahasiswa.getPropinsiOrtu() != null) ? biodataCalonMahasiswa.getPropinsiOrtu().getId() : "" %>">
						        <input type="text" class="form-control form-control-sm shadow-none bg-light fw-bold" 
						               id="textPropinsiOrtu<%=rnd%>" 
						               value="<%= (biodataCalonMahasiswa.getPropinsiOrtu() != null) ? biodataCalonMahasiswa.getPropinsiOrtu().getNama() : "" %>" 
						               placeholder="---" readonly>
						    </div>
						
						    <div class="col-md-4" style="<%= fc.isVisible("kotaOrtu") ? "" : "display:none;" %>">
						        <label class="form-label fw-semibold small <%=fc.reqClass("kotaOrtu")%>"><%= Common.getBahasaConfig("Kota/Kab. Orang Tua") %></label>
						        <input type="hidden" id="kotaOrtu<%=rnd%>" name="kotaOrtu" value="<%= (biodataCalonMahasiswa.getKotaOrtu() != null) ? biodataCalonMahasiswa.getKotaOrtu().getId() : "" %>">
						        <input type="text" class="form-control form-control-sm shadow-none bg-light fw-bold" 
						               id="textKotaOrtu<%=rnd%>" 
						               value="<%= (biodataCalonMahasiswa.getKotaOrtu() != null) ? biodataCalonMahasiswa.getKotaOrtu().getNama() : "" %>" 
						               placeholder="---" readonly>
						    </div>
						
						    <div class="col-md-4" style="<%= fc.isVisible("kecamatanOrtu") ? "" : "display:none;" %>">
						        <label class="form-label fw-semibold small <%=fc.reqClass("kecamatanOrtu")%>"><%= Common.getBahasaConfig("Kecamatan Orang Tua") %></label>
						        <div class="input-group input-group-sm">
						            <input type="hidden" id="kecamatanOrtu<%=rnd%>" name="kecamatanOrtu" value="<%= (biodataCalonMahasiswa.getKecamatanOrtu() != null) ? biodataCalonMahasiswa.getKecamatanOrtu().getId() : "" %>">
						            <input type="text" class="form-control shadow-none bg-white" id="textKecamatanOrtu<%=rnd%>" 
						                   value="<%= (biodataCalonMahasiswa.getKecamatanOrtu() != null) ? biodataCalonMahasiswa.getKecamatanOrtu().getNama() : "" %>" 
						                   placeholder="<%= Common.getBahasaConfig("Cari Kecamatan...") %>" readonly <%=fc.reqAttr("kecamatanOrtu")%>>
						            <button class="btn btn-outline-primary" type="button" onclick="window.bukaModalCariWilayah<%=rnd%>('ortu')">
						                <i class="fas fa-magnifying-glass"></i>
						            </button>
						        </div>
						    </div>
						</div>

                        <hr class="my-3">
                        <h6 class="fw-bold text-secondary mb-3 small"><%= Common.getBahasaConfig("Data Wali (Kosongkan jika tidak ada)") %></h6>
                        
                        <div class="row g-3">
                            <div class="col-md" style="<%= fc.isVisible("namaWali") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("namaWali")%>"><%= Common.getBahasaConfig("Nama Wali") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="namaWali<%=rnd%>" value="<%= vNamaWali %>" <%=fc.reqAttr("namaWali")%>>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("pendidikanOrtuWali") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("pendidikanOrtuWali")%>"><%= Common.getBahasaConfig("Pendidikan Wali") %></label>
                                <select class="form-select form-select-sm shadow-none" id="pendidikanOrtuWali<%=rnd%>" <%=fc.reqAttr("pendidikanOrtuWali")%>></select>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("pekerjaanAyahWali") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("pekerjaanAyahWali")%>"><%= Common.getBahasaConfig("Pekerjaan Wali") %></label>
                                <select class="form-select form-select-sm shadow-none" id="pekerjaanAyahWali<%=rnd%>" <%=fc.reqAttr("pekerjaanAyahWali")%>></select>
                            </div>

                            <div class="col-md" style="<%= fc.isVisible("pendapatanOrtuWali") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("pendapatanOrtuWali")%>"><%= Common.getBahasaConfig("Pendapatan Wali") %></label>
                                <select class="form-select form-select-sm shadow-none" id="pendapatanOrtuWali<%=rnd%>" <%=fc.reqAttr("pendapatanOrtuWali")%>></select>
                            </div>
                        </div>

                        <div class="d-flex justify-content-between mt-4 pt-3 border-top">
                            <button type="button" class="btn btn-light btn-sm border px-4 rounded-pill" onclick="window.goToStep<%=rnd%>(3)"><i class="fas fa-arrow-left me-2"></i> <%= Common.getBahasaConfig("Kembali") %></button>
                            <button type="button" class="btn btn-primary btn-sm px-5 rounded-pill" onclick="window.goToStep<%=rnd%>(5)"><%= Common.getBahasaConfig("Selanjutnya") %> <i class="fas fa-arrow-right ms-2"></i></button>
                        </div>
                    </div>

                    <div class="wizard-pane-<%=rnd%>" id="paneStep5<%=rnd%>">
                        <h6 class="fw-bold mb-3 text-primary border-bottom pb-2"><%= Common.getBahasaConfig("Langkah 5: Pilihan Prodi & Informasi Lainnya") %></h6>
                        
                        <div class="row g-3 mb-3">
                            <div class="col-md" id="colPaket<%=rnd%>">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Pilihan Paket") %></label>
                                <select class="form-select form-select-sm border-success shadow-none fw-bold text-success" id="paket<%=rnd%>" onchange="window.aturProdiBerdasarkanPaket<%=rnd%>()" required></select>
                            </div>
                            <div class="col-md">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Program Kuliah") %></label>
                                <select class="form-select form-select-sm border-success shadow-none fw-bold" id="program<%=rnd%>" required></select>
                            </div>
                            
                            <!-- 4C. PENERAPAN KUNCI OTOMATIS PADA FIELD AFILIASI -->
                            <div class="col-md" style="<%= fc.isVisible("afiliasiCalonMahasiswa") ? "" : "display:none;" %>">
                                <label class="form-label fw-semibold small <%=fc.reqClass("afiliasiCalonMahasiswa")%>"><%= Common.getBahasaConfig("Afiliasi / Referensi") %></label>
                                <% if (isAfiliasiLockedByParam) { %>
                                    <!-- Jika terkunci oleh URL, tampilkan dalam bentuk label dan simpan ID di hidden input -->
                                    <div class="readonly-label-pmb py-1 ps-2 bg-light border rounded">
                                        <i class="fas fa-link me-2 text-info"></i><span class="fw-bold text-dark"><%= afiliasiNamaLocked %></span>
                                    </div>
                                    <input type="hidden" id="afiliasiCalonMahasiswa<%=rnd%>" value="<%= vAfiliasi %>">
                                    <div class="form-text text-success small mt-1"><i class="fas fa-circle-check me-1"></i><%= Common.getBahasaConfig("Kode afiliasi valid terdeteksi dari tautan Anda.") %></div>
                                <% } else { %>
                                    <select class="form-select form-select-sm shadow-none border-info" id="afiliasiCalonMahasiswa<%=rnd%>" <%=fc.reqAttr("afiliasiCalonMahasiswa")%>></select>
                                <% } %>
                            </div>
                        </div>

                        <div id="containerProdi<%=rnd%>" class="mt-2 p-3 bg-light rounded border border-success"></div>

                        <% if (tampilkanInfoPMB) { %>
                        <hr class="my-3">
                        <h6 class="fw-bold mb-3 text-primary"><i class="fas fa-bullhorn me-2"></i>Sumber Informasi Pendaftaran</h6>
                        <div class="row g-3 mb-3">
                            <div class="col-12">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Anda mendapatkan informasi Pendaftaran Mahasiswa Baru ini dari mana ?") %></label>
                                <div class="d-flex flex-wrap gap-3">
                                    <%
                                        String[] opsis = opsiInfoPmbStr.split(",");
                                        String inputType = pilihSalahSatuInfoPMB ? "radio" : "checkbox";
                                        String inputName = "infoKampus" + rnd;
                                        for(int i=0; i<opsis.length; i++) {
                                            String r = opsis[i].trim();
                                            if(r.isEmpty()) continue;
                                            String rLower = r.toLowerCase();
                                            boolean isChecked = false;
                                            if (rLower.equals("teman") || rLower.equals("kawan")) {
                                                isChecked = vInfoKampus.contains(";teman;") || vInfoKampus.contains(";kawan;");
                                            } else if (rLower.equals("siswa") || rLower.equals("mahasiswa") || rLower.equals("mahasiswa/alumni kampus")) {
                                                isChecked = vInfoKampus.contains(";mahasiswa/alumni kampus;") || vInfoKampus.contains(";mahasiswa;") || vInfoKampus.contains(";siswa;");
                                            } else if (rLower.equals("lain-lain") || rLower.equals("lain") || rLower.equals("lainnya")) {
                                                isChecked = vInfoKampus.contains(";lain-lain;") || vInfoKampus.contains(";lain;") || vInfoKampus.contains(";lainnya;");
                                            } else if (rLower.contains("dosen")) {
                                                isChecked = vInfoKampus.contains(";dosen;");
                                            } else {
                                                isChecked = vInfoKampus.contains(";" + rLower + ";");
                                            }
                                    %>
                                    <div class="form-check">
                                        <input class="form-check-input info-kampus-check-<%=rnd%>" type="<%=inputType%>" name="<%=inputName%>" id="info_<%=i%>_<%=rnd%>" value="<%=r%>" onchange="window.toggleInfoKampus<%=rnd%>()" <%= isChecked ? "checked" : "" %>>
                                        <label class="form-check-label small" for="info_<%=i%>_<%=rnd%>"><%= r %></label>
                                    </div>
                                    <% } %>
                                </div>
                            </div>
                            
                            <div class="col-md-12" id="wrapNamaTeman<%=rnd%>" style="display:none;">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Sebutkan Nama dan NIM Teman/Mahasiswa") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none border-warning" id="namaTemanInfoKampusDariMana<%=rnd%>" value="<%= vNamaTemanInfo %>" data-wajib="true">
                            </div>
                            <div class="col-md-12" id="wrapNamaDosen<%=rnd%>" style="display:none;">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Sebutkan Nama Dosen / Karyawan") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none border-warning" id="dariNamaDosenKaryawan<%=rnd%>" value="<%= vNamaDosenInfo %>" data-wajib="true">
                            </div>
                            <div class="col-md-12" id="wrapKetLain<%=rnd%>" style="display:none;">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Sebutkan dari mana") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none border-warning" id="keteranganInfoKampusDariMana<%=rnd%>" value="<%= vKetInfoLain %>" data-wajib="true">
                            </div>
                        </div>
                        <% } %>

                        <div id="containerFormTambahan<%=rnd%>"></div>

                        <% if (modeEdit) { %>
                        <div id="containerHeaderVerifikasi<%=rnd%>">
                            <hr class="my-3">
                            <h6 class="fw-bold mb-2 text-primary"><i class="fas fa-list-check me-2"></i><%= Common.getBahasaConfig("Verifikasi Kelengkapan Berkas") %></h6>
                        </div>
                        <div id="containerVerifikasiBerkas<%=rnd%>"></div>
                        <% } %>

                        <div id="wrapperContainerFoto<%=rnd%>" style="display: none;">
						    <hr class="my-3">
						    <div class="row mb-4 justify-content-center" id="containerFoto<%=rnd%>">
						        <div class="col-md-5 col-sm-8 text-center">
						            <%
									    boolean isWajibFotoDefault = (gelombangPendaftaran != null && Boolean.TRUE.equals(gelombangPendaftaran.getFotoWajibDiuplad()));
									    String teksLabelFoto = isWajibFotoDefault ? 
									        Common.getBahasaConfig("Foto Profil") + " <span class='text-danger fw-bold'>* (" + Common.getBahasaConfig("Wajib Diunggah") + ")</span>" : 
									        Common.getBahasaConfig("Foto Profil (Opsional/Bila Ada)");
									%>
									<label id="labelUploadFoto<%=rnd%>" class="form-label fw-bold text-primary">
									    <i class="fas fa-camera me-1"></i> <%= teksLabelFoto %>
									</label>
						            <div class="p-3 border rounded-4 bg-white shadow-sm">
						                <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
						                    <jsp:param name="clazz" value="ais.database.model.file.FotoBiodataCalonMahasiswa"/>
						                    <jsp:param name="jenis" value="<%=ais.database.model.file.FotoBiodataCalonMahasiswa.DEFAULT_JENIS%>"/>
						                    <jsp:param name="id" value="<%=idUploadFoto%>"/>
						                    <jsp:param name="accept" value="image/*"/>
						                    <jsp:param name="tampilUpload" value="true"/>
						                    <jsp:param name="loadPreview" value="true"/>
						                    <jsp:param name="tanpaLogin" value="true"/>
						                </jsp:include>
						            </div>
						        </div>
						    </div>
						</div>

                        <hr class="my-3">
                        
                        <div class="form-check form-switch mb-3" id="wrapperPindahan<%=rnd%>" style="display: none;">
                            <input class="form-check-input" type="checkbox" id="merupakanPindahan<%=rnd%>" onchange="window.togglePindahan<%=rnd%>()" <%= vMerupakanPindahan ? "checked" : "" %>>
                            <label class="form-check-label fw-bold text-dark ms-2 small mb-0" for="merupakanPindahan<%=rnd%>">
                                <i class="fas fa-right-left me-2 text-warning"></i><%= Common.getBahasaConfig("Centang jika Mahasiswa Pindahan (Transfer)") %>
                            </label>
                        </div>
                        
                        <div class="row g-3 p-3 bg-light rounded border border-warning" id="panelPindahan<%=rnd%>" style="display: none;">
                            <div class="col-md">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Nama Kampus Asal") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="pindahanDariKampus<%=rnd%>" value="<%= vPindahanDariKampus %>" data-wajib="true">
                            </div>
                            <div class="col-md">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Program Studi Asal") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="pindahanDariProdi<%=rnd%>" value="<%= vPindahanDariProdi %>" data-wajib="true">
                            </div>
                            <div class="col-md">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("NIM Asal") %></label>
                                <input type="text" class="form-control form-control-sm shadow-none" id="nimLamaSebelumPindah<%=rnd%>" value="<%= vNimLamaSebelumPindah %>" data-wajib="true">
                            </div>
                            <div class="col-md">
                                <label class="form-label fw-semibold small required"><%= Common.getBahasaConfig("Semester Terakhir di Kampus Asal") %></label>
                                <input type="number" class="form-control form-control-sm shadow-none" id="pindahDariKampusLamaDiSemester<%=rnd%>" value="<%= vPindahDariKampusLamaDiSemester %>" min="1" data-wajib="true">
                            </div>
                        </div>

                        <div class="card mt-4 bg-light border-0">
                            <div class="card-body py-2 px-3">
                                <div class="form-check">
                                    <input class="form-check-input border-danger" type="checkbox" id="pernyataan<%=rnd%>" style="transform: scale(1.1); margin-top: 0.3rem;" required <%= vPernyataan ? "checked" : "" %>>
                                    <label class="form-check-label ms-2 text-secondary small" for="pernyataan<%=rnd%>">
                                        <strong><%= Common.getBahasaConfig("Pernyataan Persetujuan") %>:</strong><br>
                                        <%= Common.getBahasaConfig("Dengan ini saya menyatakan bahwa data yang saya masukkan benar adanya. Jika dikemudian hari ditemukan pemalsuan data, saya bersedia menerima sanksi dan resiko pembatalan status penerimaan.") %>
                                    </label>
                                </div>
                            </div>
                        </div>

                        <!-- Panel error pendaftaran — muncul saat simpan gagal, tidak hilang otomatis -->
                        <div id="panelErrorPmb<%=rnd%>" style="display:none;" class="mt-3 rounded-3 border border-2 border-danger overflow-hidden">
                            <!-- Header merah -->
                            <div class="d-flex align-items-center gap-2 px-3 py-2" style="background:#c0392b;">
                                <span class="text-white" style="font-size:1.05rem;">&#9888;</span>
                                <span class="text-white fw-bold" style="font-size:.92rem;">Pendaftaran Belum Berhasil</span>
                                <span id="statusDataPmb<%=rnd%>" class="badge ms-1" style="background:rgba(0,0,0,.3);font-size:.72rem;letter-spacing:.03em;">DATA BELUM TERSIMPAN</span>
                                <button type="button" class="btn-close btn-close-white btn-close-sm ms-auto" aria-label="Tutup"
                                        onclick="document.getElementById('panelErrorPmb<%=rnd%>').style.display='none'"></button>
                            </div>
                            <!-- Body -->
                            <div class="p-3" style="background:#fff8f8;">
                                <!-- Apa yang terjadi -->
                                <div class="mb-3">
                                    <div class="fw-semibold mb-1" style="color:#c0392b;font-size:.84rem;">&#9679; Apa yang terjadi?</div>
                                    <div id="pesanApaPmb<%=rnd%>" class="text-dark" style="font-size:.88rem;line-height:1.5;"></div>
                                </div>
                                <!-- Langkah -->
                                <div class="mb-3">
                                    <div class="fw-semibold mb-1" style="color:#c0392b;font-size:.84rem;">&#9679; Yang perlu Anda lakukan:</div>
                                    <ol id="langkahErrorPmb<%=rnd%>" class="mb-0 ps-4" style="font-size:.88rem;line-height:1.8;"></ol>
                                </div>
                                <!-- Detail teknis (tersembunyi, untuk petugas) -->
                                <details style="border-top:1px solid #f0cccc;padding-top:.6rem;">
                                    <summary class="text-muted" style="font-size:.78rem;cursor:pointer;user-select:none;">
                                        &#128274; Informasi teknis untuk petugas administrasi PMB (klik untuk tampilkan)
                                    </summary>
                                    <div class="mt-2 p-2 rounded-2" style="background:#f5f5f5;font-size:.8rem;">
                                        <div class="mb-1">
                                            <span class="text-muted">Kode Kesalahan:</span>
                                            <span id="kodeErrorPmb<%=rnd%>" class="badge text-bg-secondary ms-1" style="font-family:monospace;letter-spacing:.04em;"></span>
                                            <button type="button" onclick="(function(){var k=document.getElementById('kodeErrorPmb<%=rnd%>').textContent,w=document.getElementById('waktuErrorPmb<%=rnd%>').textContent;if(navigator.clipboard){navigator.clipboard.writeText(k+' | '+w).then(function(){alert('<%= Common.getBahasaConfigJS("Kode dan waktu berhasil disalin. Silakan kirimkan kepada petugas PMB.") %>')});}else{alert(k+' | '+w);}})()" class="btn btn-outline-secondary py-0 px-1 ms-1" style="font-size:.72rem;"><%= Common.getBahasaConfig("Salin") %></button>
                                        </div>
                                        <div class="mb-1"><span class="text-muted">Waktu Kejadian:</span> <span id="waktuErrorPmb<%=rnd%>" class="fw-semibold"></span></div>
                                        <div id="infoAdminPmb<%=rnd%>" class="text-muted mt-1" style="font-style:italic;"></div>
                                    </div>
                                </details>
                            </div>
                        </div>

                        <div class="d-flex justify-content-between mt-4 pt-3 border-top">
                            <button type="button" class="btn btn-light btn-sm border px-4 rounded-pill" onclick="window.goToStep<%=rnd%>(4)"><i class="fas fa-arrow-left me-2"></i> <%= Common.getBahasaConfig("Kembali") %></button>
                            <button type="button" class="btn btn-success btn-sm px-5 rounded-pill shadow-sm fw-bold" id="btnSubmit<%=rnd%>" onclick="window.simpanBiodata<%=rnd%>()">
                                <i class="fas fa-save me-2"></i><%= Common.getBahasaConfig("Simpan Data Pendaftaran") %>
                            </button>
                        </div>
                    </div>
                </form>

            </div>
        </div>
    </div>
</div>

<script>
window.gelombangData<%=rnd%> = [];
window.isInitialLoad<%=rnd%> = true;
window.masterModelClass<%=rnd%> = "ais.database.model.BiodataCalonMahasiswa";

window.presetJenisSeleksi<%=rnd%> = '<%= biodataCalonMahasiswa.getJenisSeleksi() != null ? biodataCalonMahasiswa.getJenisSeleksi().getId() : "" %>';
window.presetPaket<%=rnd%> = '<%= paketId != null ? paketId : "" %>';
window.presetProgram<%=rnd%> = '<%= program != null ? program.replace("'", "\\'") : "" %>';
window.presetProdi<%=rnd%> = {
 1: '<%= prodi1 != null ? prodi1 : "" %>', 2: '<%= prodi2 != null ? prodi2 : "" %>',
 3: '<%= prodi3 != null ? prodi3 : "" %>', 4: '<%= prodi4 != null ? prodi4 : "" %>', 5: '<%= prodi5 != null ? prodi5 : "" %>'
};
window.presetJenisSekolah<%=rnd%> = '<%= biodataCalonMahasiswa.getJenisSekolah() != null ? biodataCalonMahasiswa.getJenisSekolah().getId() : "" %>';
window.presetJurusanSekolah<%=rnd%> = '<%= biodataCalonMahasiswa.getJurusanSekolah() != null ? biodataCalonMahasiswa.getJurusanSekolah().getId() : "" %>';

window.getVal<%=rnd%> = function(id) { 
    let el = document.getElementById(id);
    if (!el) return null;
    let val = el.value;
    return (val === undefined || val === null || String(val).trim() === "" || String(val) === "undefined" || String(val) === "null") ? null : String(val).trim();
};
window.getInt<%=rnd%> = function(id) { 
    let val = window.getVal<%=rnd%>(id);
    return val ? parseInt(val) : null;
};
window.getCheck<%=rnd%> = function(id) { let el = document.getElementById(id); return el ? el.checked : false; };

window.loadDataReferensi<%=rnd%> = async function() {
    try {
        const res = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=referensi_lengkap');
        const json = await res.json();
        
        const map = {
            'agama': 'agama<%=rnd%>', 'identitas': 'jenisKartuIdentitas<%=rnd%>', 'negara': 'asalNegara<%=rnd%>',
            'afiliasi': 'afiliasiCalonMahasiswa<%=rnd%>', 'pendidikan': ['pendidikanOrtu<%=rnd%>', 'pendidikanOrtuIbu<%=rnd%>', 'pendidikanOrtuWali<%=rnd%>'],
            'pekerjaan': ['pekerjaanAyah<%=rnd%>', 'pekerjaanAyahIbu<%=rnd%>', 'pekerjaanAyahWali<%=rnd%>'],
            'pendapatan': ['pendapatanOrtu<%=rnd%>', 'pendapatanOrtuIbu<%=rnd%>', 'pendapatanOrtuWali<%=rnd%>']
        };
        const preset = {
            'agama<%=rnd%>': '<%= biodataCalonMahasiswa.getAgama()!=null?biodataCalonMahasiswa.getAgama().getId():"" %>',
            'jenisKartuIdentitas<%=rnd%>': '<%= biodataCalonMahasiswa.getJenisKartuIdentitas()!=null?biodataCalonMahasiswa.getJenisKartuIdentitas().getId():"" %>',
            'asalNegara<%=rnd%>': '<%= vAsalNegara %>',
            'afiliasiCalonMahasiswa<%=rnd%>': '<%= vAfiliasi %>',
            'pendidikanOrtu<%=rnd%>': '<%= biodataCalonMahasiswa.getPendidikanOrtu()!=null?biodataCalonMahasiswa.getPendidikanOrtu().getId():"" %>',
            'pendidikanOrtuIbu<%=rnd%>': '<%= biodataCalonMahasiswa.getPendidikanOrtuIbu()!=null?biodataCalonMahasiswa.getPendidikanOrtuIbu().getId():"" %>',
            'pendidikanOrtuWali<%=rnd%>': '<%= biodataCalonMahasiswa.getPendidikanOrtuWali()!=null?biodataCalonMahasiswa.getPendidikanOrtuWali().getId():"" %>',
            'pekerjaanAyah<%=rnd%>': '<%= biodataCalonMahasiswa.getPekerjaanAyah()!=null?biodataCalonMahasiswa.getPekerjaanAyah().getId():"" %>',
            'pekerjaanAyahIbu<%=rnd%>': '<%= biodataCalonMahasiswa.getPekerjaanAyahIbu()!=null?biodataCalonMahasiswa.getPekerjaanAyahIbu().getId():"" %>',
            'pekerjaanAyahWali<%=rnd%>': '<%= biodataCalonMahasiswa.getPekerjaanAyahWali()!=null?biodataCalonMahasiswa.getPekerjaanAyahWali().getId():"" %>',
            'pendapatanOrtu<%=rnd%>': '<%= biodataCalonMahasiswa.getPendapatanOrtu()!=null?biodataCalonMahasiswa.getPendapatanOrtu().getId():"" %>',
            'pendapatanOrtuIbu<%=rnd%>': '<%= biodataCalonMahasiswa.getPendapatanOrtuIbu()!=null?biodataCalonMahasiswa.getPendapatanOrtuIbu().getId():"" %>',
            'pendapatanOrtuWali<%=rnd%>': '<%= biodataCalonMahasiswa.getPendapatanOrtuWali()!=null?biodataCalonMahasiswa.getPendapatanOrtuWali().getId():"" %>'
        };

        for (const key in map) {
            let list = json[key] || [];
            let opt = '<option value="">== Pilih ==</option>';
            list.forEach(i => opt += '<option value="'+i.id+'">'+i.nama+'</option>');
            
            let targets = Array.isArray(map[key]) ? map[key] : [map[key]];
            targets.forEach(t => {
                let el = document.getElementById(t);
                // 4D. PENCEGAHAN OVERRIDE DROPDOWN AFILIASI JIKA TERKUNCI (NEW)
                if(el && el.tagName === 'SELECT') { 
                    el.innerHTML = opt; 
                    if(preset[t]) el.value = preset[t]; 
                }
            });
        }
    } catch(e) { console.error("Error Load Referensi", e); }
};

window.onTahunAkademikChange<%=rnd%> = function() { window.loadGelombang<%=rnd%>(); };
window.loadGelombang<%=rnd%> = async function() {
    let ta = window.getVal<%=rnd%>('tahunAkademik<%=rnd%>');
    try {
        const res = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=gelombang' + (ta ? '&ta='+ta : ''));
        window.gelombangData<%=rnd%> = (await res.json()).data || [];
        
        let opt = '<option value="">== Pilih Gelombang ==</option>';
        window.gelombangData<%=rnd%>.forEach(g => opt += '<option value="'+g.id+'">'+g.nama+'</option>');
        let elGel = document.getElementById('gelombangPendaftaran<%=rnd%>');
        if(elGel) elGel.innerHTML = opt;

        <% if (modeEdit) { %>
            if (!elGel.dataset.changed && '<%= idGelombang %>') elGel.value = '<%= idGelombang %>';
        <% } %>
        
        window.onGelombangChange<%=rnd%>();
    } catch(e) { console.error("Error load Gelombang", e); }
};

window.onGelombangChange<%=rnd%> = async function() {
    let elGel = document.getElementById('gelombangPendaftaran<%=rnd%>');
    if(elGel) elGel.dataset.changed = 'true';
    let gelId = window.getVal<%=rnd%>('gelombangPendaftaran<%=rnd%>');
    
    await window.loadJenisSeleksi<%=rnd%>(gelId);
    await window.loadJenisSekolah<%=rnd%>(gelId);
    window.applyGelombangVisibilityLogic<%=rnd%>(gelId);
};

window.loadJenisSeleksi<%=rnd%> = async function(gelId) {
    let elSel = document.getElementById('jenisSeleksi<%=rnd%>');
    if(!elSel) return;
    if (!gelId) { elSel.innerHTML = '<option value="">== Pilih Gelombang Dulu ==</option>'; return; }
    
    elSel.innerHTML = '<option value="">== Sedang memuat... ==</option>';
    try {
        const res = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=jenis_seleksi&gelId=' + gelId);
        const json = await res.json();
        let opt = '<option value="">== Pilih ==</option>';
        (json.data || []).forEach(js => { opt += '<option value="' + js.id + '">' + js.nama + '</option>'; });
        elSel.innerHTML = opt;
        
        if (window.presetJenisSeleksi<%=rnd%>) {
            if(Array.from(elSel.options).some(o => o.value === String(window.presetJenisSeleksi<%=rnd%>))) elSel.value = window.presetJenisSeleksi<%=rnd%>;
            else if (json.data && json.data.length === 1) elSel.selectedIndex = 1;
        } else if (json.data && json.data.length === 1) {
            elSel.selectedIndex = 1;
        }
        window.onSeleksiChange<%=rnd%>();
    } catch (e) { console.error("Error Load Jenis Seleksi", e); }
};

window.onSeleksiChange<%=rnd%> = function() { window.loadVerifikasiBerkas<%=rnd%>(); };

window.loadJenisSekolah<%=rnd%> = async function(gelId) {
    let elJen = document.getElementById('jenisSekolah<%=rnd%>');
    let elTahun = document.getElementById('tahunLulus<%=rnd%>');
    
    if(!elJen) return;
    elJen.innerHTML = '<option value="">== Sedang memuat... ==</option>';
    
    let gelObj = window.gelombangData<%=rnd%> ? window.gelombangData<%=rnd%>.find(g => String(g.id) === String(gelId)) : null;
    if (elTahun) {
        elTahun.innerHTML = '<option value="">- <%= Common.getBahasaConfig("Pilih Tahun") %> -</option>';
        let currentYear = new Date().getFullYear();
        let savedTahun = '<%=vTahunLulus%>'; 
        
        let forceDefault = <%= (tbmuser == null || !modeEdit) %>;
        
        let minTahun = gelObj && gelObj.tahunAngkatanMinimal ? parseInt(gelObj.tahunAngkatanMinimal) : 1970;
        let maxTahun = gelObj && gelObj.tahunAngkatanMaksimal ? parseInt(gelObj.tahunAngkatanMaksimal) : currentYear;
        
        for (let i = (maxTahun + 5); i >= minTahun; i--) {
            let isSelected = false;
            if (forceDefault) {
                isSelected = (i === currentYear);
            } else {
                isSelected = (savedTahun !== '' && savedTahun !== 'null' && savedTahun !== '0') ? (savedTahun === String(i)) : (i === currentYear);
            }
            elTahun.add(new Option(i, i, isSelected, isSelected));
        }
    }
    
    try {
        const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=jenis_sekolah' + (gelId ? '&gelId=' + gelId : '');
        const res = await fetch(url);
        const json = await res.json();
        
        let opt = '<option value="">== Klik disini untuk pilih ==</option>';
        if (json.data) {
            json.data.forEach(js => { opt += '<option value="' + js.id + '">' + js.nama + '</option>'; });
        }
        elJen.innerHTML = opt;
        elJen.disabled = false;
        if (elJen.options.length === 2 && !window.presetJenisSekolah<%=rnd%>) {
            elJen.selectedIndex = 1;
        } else if (window.presetJenisSekolah<%=rnd%>) {
            let optionExists = Array.from(elJen.options).some(o => o.value === String(window.presetJenisSekolah<%=rnd%>));
            if (optionExists) elJen.value = window.presetJenisSekolah<%=rnd%>;
        }
        window.onJenisSekolahChange<%=rnd%>(elJen.value);
    } catch (e) { console.error("Error Load Jenis Sekolah", e); }
};

window.onJenisSekolahChange<%=rnd%> = async function(jenisSekolahId) {
    let elJur = document.getElementById('jurusanSekolah<%=rnd%>');
    let gelId = window.getVal<%=rnd%>('gelombangPendaftaran<%=rnd%>');
    
    if(!elJur) return;
    if (!jenisSekolahId) {
        elJur.innerHTML = '<option value="">== Pilih Jenis Pendidikan Dulu ==</option>';
        window.onJurusanSekolahChange<%=rnd%>();
        return;
    }
    
    elJur.innerHTML = '<option value="">== Sedang memuat... ==</option>';
    try {
        const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=jurusan_sekolah&jenisSekolahId=' + jenisSekolahId + (gelId ? '&gelId=' + gelId : '');
        const res = await fetch(url);
        const json = await res.json();
        
        let opt = '<option value="">== Klik disini untuk pilih ==</option>';
        if (json.data) {
            json.data.forEach(js => { opt += '<option value="' + js.id + '">' + js.nama + '</option>'; });
        }
        elJur.innerHTML = opt;
        if (elJur.options.length === 2 && !window.presetJurusanSekolah<%=rnd%>) {
            elJur.selectedIndex = 1;
        } else if (window.presetJurusanSekolah<%=rnd%>) {
            let optionExists = Array.from(elJur.options).some(o => o.value === String(window.presetJurusanSekolah<%=rnd%>));
            if (optionExists) elJur.value = window.presetJurusanSekolah<%=rnd%>;
        }
        window.onJurusanSekolahChange<%=rnd%>();
    } catch (e) { console.error("Error Load Jurusan Sekolah", e); }
};

window.onJurusanSekolahChange<%=rnd%> = function() {
    window.toggleJurusanLain<%=rnd%>();
    window.loadPaketOptions<%=rnd%>();
};

window.loadPaketOptions<%=rnd%> = async function() {
    let gelId = window.getVal<%=rnd%>('gelombangPendaftaran<%=rnd%>');
    let jurId = window.getVal<%=rnd%>('jurusanSekolah<%=rnd%>');
    let elPaket = document.getElementById('paket<%=rnd%>');
    let colPaket = document.getElementById('colPaket<%=rnd%>');
    
    if(!elPaket) return;
    if (!gelId) {
        elPaket.innerHTML = '<option value="">== Pilih Gelombang Dulu ==</option>';
        if(colPaket) colPaket.style.display = 'block';
        window.aturProdiBerdasarkanPaket<%=rnd%>();
        return;
    }
    
    try {
        const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=paket&gelId=' + gelId + (jurId ? '&jurId=' + jurId : '');
        const res = await fetch(url);
        const json = await res.json();
        
        let opt = '<option value="">== Pilih Paket ==</option>';
        let data = json.data || [];
        
        data.forEach(p => {
            opt += '<option value="' + p.id + '" data-max="' + p.maxProdi + '">' + p.nama + '</option>';
        });
        elPaket.innerHTML = opt;
        
        let hidePaket = false;
        if (data.length === 1) {
            elPaket.selectedIndex = 1;
            elPaket.disabled = true;
            hidePaket = true;
        } else {
            elPaket.disabled = false;
            if (window.presetPaket<%=rnd%>) {
                let optionExists = Array.from(elPaket.options).some(o => o.value === String(window.presetPaket<%=rnd%>));
                if (optionExists) {
                    elPaket.value = window.presetPaket<%=rnd%>;
                } else {
                    let namaPaketLama = '<%= biodataCalonMahasiswa.getPaket() != null ? biodataCalonMahasiswa.getPaket().getNama().replace("'", "\\'") : "Paket Tersimpan" %>';
                    elPaket.add(new Option(namaPaketLama, window.presetPaket<%=rnd%>, true, true));
                }
            } else if (data.length > 0) {
                elPaket.selectedIndex = 1;
            }
        }
        
        if(colPaket) colPaket.style.display = hidePaket ? 'none' : 'block';
        
        window.aturProdiBerdasarkanPaket<%=rnd%>();
    } catch(e) { console.error("Error Load Paket", e); }
};

window.aturProdiBerdasarkanPaket<%=rnd%> = async function() {
    let gelId = window.getVal<%=rnd%>('gelombangPendaftaran<%=rnd%>');
    let pktId = window.getVal<%=rnd%>('paket<%=rnd%>');
    let jk = window.getVal<%=rnd%>('jenisKelamin<%=rnd%>') || 'Semua';
    let containerProdi = document.getElementById('containerProdi<%=rnd%>');
    let programSelect = document.getElementById('program<%=rnd%>');
    
    if(!containerProdi || !programSelect) return;
    if (!pktId) {
        containerProdi.innerHTML = '';
        programSelect.innerHTML = '<option value="">== Pilih Paket Dulu ==</option>';
        return;
    }
    
    try {
        const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=program_prodi&paketId=' + pktId;
        const res = await fetch(url);
        const json = await res.json();
        
        // 1. Program
        let optProg = '<option value="">== Pilih Program ==</option>';
        let hasReguler = false;
        if (json.programs) {
            json.programs.forEach(p => {
                optProg += '<option value="' + p.nama + '">' + p.namaBaru + '</option>';
                if (p.nama.toLowerCase() === 'reguler') hasReguler = true;
            });
        }
        programSelect.innerHTML = optProg;
        
        let selectedValue = hasReguler ? "Reguler" : "";
        if (window.presetProgram<%=rnd%>) selectedValue = window.presetProgram<%=rnd%>;
        
        let gelObj = window.gelombangData<%=rnd%> ? window.gelombangData<%=rnd%>.find(g => String(g.id) === String(gelId)) : null;
        if (gelObj) {
            if (<%= !modeEdit %> && gelObj.program && gelObj.program !== 'null') selectedValue = gelObj.program;
            programSelect.disabled = (gelObj.tidakBolehMemilihProgramLain === true || String(gelObj.tidakBolehMemilihProgramLain).toLowerCase() === 'true');
        } else {
            programSelect.disabled = false;
        }
        
        if (selectedValue) {
            let optionExists = Array.from(programSelect.options).some(o => o.value === selectedValue);
            if (!optionExists) programSelect.add(new Option(selectedValue, selectedValue, true, true));
            programSelect.value = selectedValue;
        }

        // 2. Prodi
        let prodiMaster = json.prodis || [];
        const paketSelect = document.getElementById('paket<%=rnd%>');
        const maxProdi = paketSelect.options[paketSelect.selectedIndex] ? parseInt(paketSelect.options[paketSelect.selectedIndex].getAttribute('data-max') || 3) : 3;
        let html = '<div class="row g-3">';
        for (let i = 1; i <= maxProdi; i++) {
            if (i > 5) break;
            let optProdi = '<option value="">== Klik disini untuk pilih ==</option>';
            prodiMaster.filter(p => p["pilihan"+i] === true && (p.kelamin === "Semua" || p.kelamin.toLowerCase() === jk.toLowerCase())).forEach(p => {
                optProdi += '<option value="'+p.id+'">'+p.nama+' ('+p.fakultas+' - '+p.jenjang+')</option>';
            });
            let reqStar = (i === 1) ? '<span class="text-danger">*</span>' : '';
            let reqAttr = (i === 1) ? 'required' : '';
            
            html += '<div class="col-md-12 mb-2"><label class="form-label fw-bold small text-success"><i class="fas fa-circle-check me-1"></i> Pilihan Prodi ' + i + ' ' + reqStar + '</label><select class="form-select border-success shadow-sm" id="prodi' + i + '<%=rnd%>" ' + reqAttr + '>' + optProdi + '</select></div>';
        }
        html += '</div>';
        containerProdi.innerHTML = prodiMaster.length > 0 ? html : '<div class="alert alert-warning small py-2 my-0"><i class="fas fa-circle-exclamation me-2"></i> Prodi belum tersedia untuk paket ini.</div>';
        for (let i = 1; i <= maxProdi; i++) {
            let el = document.getElementById('prodi' + i + '<%=rnd%>');
            if (el) {
                if (window.presetProdi<%=rnd%>[i]) {
                    let optionExists = Array.from(el.options).some(o => o.value === String(window.presetProdi<%=rnd%>[i]));
                    if (optionExists) el.value = window.presetProdi<%=rnd%>[i];
                } else if (el.options.length === 2) {
                    el.selectedIndex = 1;
                }
            }
        }
        window.loadFormTambahan<%=rnd%>();
    } catch(e) { console.error("Error Load Prodi", e); }
};

window.applyGelombangVisibilityLogic<%=rnd%> = function(gelId) {
    if (!gelId) return;
    let gelObj = window.gelombangData<%=rnd%> ? window.gelombangData<%=rnd%>.find(g => String(g.id) === String(gelId)) : null;
    if (gelObj) {
        let showPhoto = gelObj.tampilkanUploadFoto === true || String(gelObj.tampilkanUploadFoto) === 'true';
        let wrapperFoto = document.getElementById('wrapperContainerFoto<%=rnd%>');
        if (wrapperFoto) wrapperFoto.style.display = showPhoto ? 'block' : 'none';

        let allowPindah = gelObj.mahasiswaPindahanBolehMendaftar === true || String(gelObj.mahasiswaPindahanBolehMendaftar) === 'true';
        let wrapperPindah = document.getElementById('wrapperPindahan<%=rnd%>');
        let chkPindah = document.getElementById('merupakanPindahan<%=rnd%>');
        if (wrapperPindah) {
            wrapperPindah.style.display = allowPindah ? 'block' : 'none';
            if (!allowPindah && chkPindah) {
                chkPindah.checked = false;
                window.togglePindahan<%=rnd%>(); 
            }
        }
    }
};

window.toggleInfoKampus<%=rnd%> = function() {
    let chks = document.querySelectorAll('.info-kampus-check-<%=rnd%>');
    if(chks.length === 0) return;
    let showTeman = false, showDosen = false, showLain = false;
    chks.forEach(chk => {
        if(chk.checked) {
            let val = chk.value.toLowerCase();
            if(val === 'teman' || val === 'kawan' || val === 'siswa' || val === 'mahasiswa' || val === 'mahasiswa/alumni kampus') showTeman = true;
            if(val.includes('dosen')) showDosen = true;
            if(val === 'lain-lain' || val === 'lain' || val === 'lainnya') showLain = true;
        }
    });

    const wTeman = document.getElementById('wrapNamaTeman<%=rnd%>'), iTeman = document.getElementById('namaTemanInfoKampusDariMana<%=rnd%>');
    if(wTeman && iTeman) { wTeman.style.display = showTeman ? 'block' : 'none'; if(!showTeman) { iTeman.removeAttribute('required'); iTeman.classList.remove('is-invalid'); } }

    const wDosen = document.getElementById('wrapNamaDosen<%=rnd%>'), iDosen = document.getElementById('dariNamaDosenKaryawan<%=rnd%>');
    if(wDosen && iDosen) { wDosen.style.display = showDosen ? 'block' : 'none'; if(!showDosen) { iDosen.removeAttribute('required'); iDosen.classList.remove('is-invalid'); } }

    const wLain = document.getElementById('wrapKetLain<%=rnd%>'), iLain = document.getElementById('keteranganInfoKampusDariMana<%=rnd%>');
    if(wLain && iLain) { wLain.style.display = showLain ? 'block' : 'none'; if(!showLain) { iLain.removeAttribute('required'); iLain.classList.remove('is-invalid'); } }
};

window.loadFormTambahan<%=rnd%> = async function() {
    let gelId = window.getVal<%=rnd%>('gelombangPendaftaran<%=rnd%>');
    let pktId = window.getVal<%=rnd%>('paket<%=rnd%>');
    let containerForm = document.getElementById('containerFormTambahan<%=rnd%>');
    if (!gelId || !pktId || !containerForm) {
        if(containerForm) containerForm.innerHTML = '';
        return;
    }

    let gelObj = window.gelombangData<%=rnd%> ? window.gelombangData<%=rnd%>.find(g => String(g.id) === String(gelId)) : null;
    let isTbmuserNull = <%= tbmuser == null %>;
    let formPendaftaran = !<%= modeEdit %>;
    if (gelObj) {
        let tampilReg = gelObj.tampilFormTambahanSaatRegistrasi === true || String(gelObj.tampilFormTambahanSaatRegistrasi) === 'true';
        let tampilLog = gelObj.tampilFormTambahanSaatLoginCalonMhs === true || String(gelObj.tampilFormTambahanSaatLoginCalonMhs) === 'true';
        
        if (formPendaftaran && !tampilReg && isTbmuserNull) { containerForm.innerHTML = ''; return; } 
        else if (!formPendaftaran && !tampilLog) { containerForm.innerHTML = ''; return; }
    }
    
    let camaId = '<%= modeEdit ? biodataCalonMahasiswa.getId() : "" %>';
    let endpoint = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_form_tambahan_pendaftaran_mahasiswa&gelombangId=' + gelId + '&paketId=' + pktId + '&id=' + camaId;
    try {
        let response = await fetch(endpoint);
        if (response.ok) {
            let htmlText = await response.text();
            containerForm.innerHTML = htmlText;
            
            let scriptsArray = Array.from(containerForm.getElementsByTagName('script')); 
            for (let i = 0; i < scriptsArray.length; i++) {
                let oldScript = scriptsArray[i];
                let scriptNode = document.createElement('script');
                var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
                Array.from(oldScript.attributes).forEach(function(attr) { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
                scriptNode.type = 'text/javascript';
                if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode); }
                else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); }
            }
        }
    } catch(e) { console.error("Gagal load form tambahan", e); }
};

window.loadVerifikasiBerkas<%=rnd%> = async function() {
    <% if (!modeEdit) { %> return; <% } %>

    let gelId = window.getVal<%=rnd%>('gelombangPendaftaran<%=rnd%>');
    let selId = window.getVal<%=rnd%>('jenisSeleksi<%=rnd%>');
    let containerVerifikasi = document.getElementById('containerVerifikasiBerkas<%=rnd%>');
    let headerVerifikasi = document.getElementById('containerHeaderVerifikasi<%=rnd%>');
    
    if (!containerVerifikasi) return;
    
    if (!gelId || !selId) {
        containerVerifikasi.innerHTML = '';
        if(headerVerifikasi) headerVerifikasi.style.display = 'none';
        return;
    }
    
    let camaId = '<%= modeEdit ? biodataCalonMahasiswa.getId() : "" %>';
    let endpoint = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_form_verifikasi_berkas_mahasiswa&gelombangId=' + gelId + '&seleksiId=' + selId + '&id=' + camaId;
    try {
        let response = await fetch(endpoint);
        if (response.ok) {
            let htmlText = await response.text();
            containerVerifikasi.innerHTML = htmlText;
            if(htmlText.trim() === "") { if(headerVerifikasi) headerVerifikasi.style.display = 'none'; } 
            else { if(headerVerifikasi) headerVerifikasi.style.display = 'block'; }
            
            let scriptsArray = Array.from(containerVerifikasi.getElementsByTagName('script'));
            for (let i = 0; i < scriptsArray.length; i++) {
                let oldScript = scriptsArray[i];
                let scriptNode = document.createElement('script');
                var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
                Array.from(oldScript.attributes).forEach(function(attr) { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
                scriptNode.type = 'text/javascript';
                if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode); }
                else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); }
            }
        }
    } catch(e) { console.error("Gagal load verifikasi berkas", e); }
};

window.isElementHiddenSpecifically<%=rnd%> = function(inp, tabPane) {
    if (inp.type === 'hidden') return true;
    try { let compStyle = window.getComputedStyle(inp); if (compStyle && (compStyle.display === 'none' || compStyle.visibility === 'hidden')) return true; } catch(e) {}
    let p = inp;
    while (p && p !== tabPane && p !== document.body) {
        if (p.style && (p.style.display === 'none' || p.style.visibility === 'hidden')) return true;
        if (p.classList && p.classList.contains('d-none')) return true;
        p = p.parentElement;
    }
    return false;
};

window.validateElement<%=rnd%> = function(inp, tabPane) {
    if (window.isElementHiddenSpecifically<%=rnd%>(inp, tabPane)) {
        if (inp.hasAttribute('required')) { inp.dataset.wasRequired = 'true'; inp.removeAttribute('required'); }
        inp.classList.remove('border-danger', 'is-invalid'); return true;
    }
    if (inp.dataset.wasRequired === 'true' || inp.getAttribute('data-wajib') === 'true' || inp.dataset.required === 'true') inp.setAttribute('required', 'required');
    let isValid = true;
    if (inp.hasAttribute('required')) {
        if (inp.type === 'checkbox') isValid = inp.checked;
        else if (!inp.value || inp.value.trim() === '') isValid = false;
    } 
    if (isValid && inp.value && inp.value.trim() !== '' && typeof inp.checkValidity === 'function' && !inp.checkValidity()) isValid = false;
    if (!isValid) inp.classList.add('border-danger', 'is-invalid'); else inp.classList.remove('border-danger', 'is-invalid');
    return isValid;
};

window.getPayloadBiodata<%=rnd%> = function() {
    const dataObj = {
        <% if (modeEdit) { %> id: <%= biodataCalonMahasiswa.getId() %>, <% } %>
        idUploadFoto: '<%= idUploadFoto %>',
        tahunAkademik: window.getVal<%=rnd%>('tahunAkademik<%=rnd%>'), gelar: window.getVal<%=rnd%>('gelar<%=rnd%>'), nama: window.getVal<%=rnd%>('nama<%=rnd%>'), noIdentitas: window.getVal<%=rnd%>('noIdentitas<%=rnd%>'),
        kk: window.getVal<%=rnd%>('kk<%=rnd%>'), nisn: window.getVal<%=rnd%>('nisn<%=rnd%>'), tempatLahir: window.getVal<%=rnd%>('tempatLahir<%=rnd%>'), tanggalLahir: window.getVal<%=rnd%>('tanggalLahir<%=rnd%>'),
        jenisKelamin: window.getVal<%=rnd%>('jenisKelamin<%=rnd%>'), statusNikah: window.getVal<%=rnd%>('statusNikah<%=rnd%>'), kewarganegaraan: window.getVal<%=rnd%>('kewarganegaraan<%=rnd%>'),
        hp: window.getVal<%=rnd%>('hp<%=rnd%>'), teleponRumah: window.getVal<%=rnd%>('teleponRumah<%=rnd%>'), email: window.getVal<%=rnd%>('email<%=rnd%>'), alamat: window.getVal<%=rnd%>('alamat<%=rnd%>'),
        dusunCalon: window.getVal<%=rnd%>('dusunCalon<%=rnd%>'), rt: window.getVal<%=rnd%>('rt<%=rnd%>'), rw: window.getVal<%=rnd%>('rw<%=rnd%>'), kodePos: window.getVal<%=rnd%>('kodePos<%=rnd%>'),
        kelurahanCalon: window.getVal<%=rnd%>('kelurahanCalon<%=rnd%>'), jurusanSekolahLain: window.getVal<%=rnd%>('jurusanSekolahLain<%=rnd%>'), akreditasiSekolah: window.getVal<%=rnd%>('akreditasiSekolah<%=rnd%>'),
        alamatAsalSma: window.getVal<%=rnd%>('alamatAsalSma<%=rnd%>'), kodePosSekolah: window.getVal<%=rnd%>('kodePosSekolah<%=rnd%>'), noTelpSekolah: window.getVal<%=rnd%>('noTelpSekolah<%=rnd%>'),
        tahun: window.getInt<%=rnd%>('tahunLulus<%=rnd%>'), tahunKelulusan: window.getVal<%=rnd%>('tahunLulus<%=rnd%>'), namaAyah: window.getVal<%=rnd%>('namaAyah<%=rnd%>'),
        namaIbu: window.getVal<%=rnd%>('namaIbu<%=rnd%>'), namaWali: window.getVal<%=rnd%>('namaWali<%=rnd%>'), alamatOrtu: window.getVal<%=rnd%>('alamatOrtu<%=rnd%>'), rtOrtu: window.getVal<%=rnd%>('rtOrtu<%=rnd%>'),
        rwOrtu: window.getVal<%=rnd%>('rwOrtu<%=rnd%>'), kodePosOrtu: window.getVal<%=rnd%>('kodePosOrtu<%=rnd%>'), kelurahanOrtu: window.getVal<%=rnd%>('kelurahanOrtu<%=rnd%>'), noTelpOrtu: window.getVal<%=rnd%>('noTelpOrtu<%=rnd%>'),
        merupakanPindahan: window.getCheck<%=rnd%>('merupakanPindahan<%=rnd%>'), pindahanDariKampus: window.getVal<%=rnd%>('pindahanDariKampus<%=rnd%>'), pindahanDariProdi: window.getVal<%=rnd%>('pindahanDariProdi<%=rnd%>'),
        nimLamaSebelumPindah: window.getVal<%=rnd%>('nimLamaSebelumPindah<%=rnd%>'), pindahDariKampusLamaDiSemester: window.getInt<%=rnd%>('pindahDariKampusLamaDiSemester<%=rnd%>'),
        alamatSama: window.getCheck<%=rnd%>('alamatSama<%=rnd%>'), pernyataan: window.getCheck<%=rnd%>('pernyataan<%=rnd%>'), program: window.getVal<%=rnd%>('program<%=rnd%>')
    };

    let chks = document.querySelectorAll('.info-kampus-check-<%=rnd%>');
    if(chks.length > 0) {
        let selectedInfos = [];
        chks.forEach(c => { if(c.checked) selectedInfos.push(c.value); });
        dataObj.infoKampusDariMana = selectedInfos.length > 0 ? ";" + selectedInfos.join(";") + ";" : "";
        dataObj.namaTemanInfoKampusDariMana = window.getVal<%=rnd%>('namaTemanInfoKampusDariMana<%=rnd%>');
        dataObj.dariNamaDosenKaryawan = window.getVal<%=rnd%>('dariNamaDosenKaryawan<%=rnd%>');
        dataObj.keteranganInfoKampusDariMana = window.getVal<%=rnd%>('keteranganInfoKampusDariMana<%=rnd%>');
    }

    const numIds = [
        'gelombangPendaftaran', 'jenisKartuIdentitas', 'jenisSeleksi', 'agama', 'asalNegara', 'afiliasiCalonMahasiswa', 
        'kecamatanCalon', 'kotaCalon', 'propinsiCalon',
        'kecamatanOrtu', 'kotaOrtu', 'propinsiOrtu',
        'kecamatanSekolah', 'kotaSekolah', 'propinsiSekolah', 
        'jenisSekolah', 'jurusanSekolah', 'namaSekolahAsal', 'pendidikanOrtu', 'pendidikanOrtuIbu', 'pendidikanOrtuWali', 
        'pekerjaanAyah', 'pekerjaanAyahIbu', 'pekerjaanAyahWali', 'pendapatanOrtu', 'pendapatanOrtuIbu', 'pendapatanOrtuWali', 'paket'
    ];
    
    numIds.forEach(id => { 
        let v = window.getInt<%=rnd%>(id + '<%=rnd%>'); 
        if (v !== null && v !== 0) dataObj[id] = v; 
    });

    for(let i=1; i<=5; i++){ let v = window.getInt<%=rnd%>('prodi'+i+'<%=rnd%>'); if(v !== null) dataObj['prodi'+i] = v; }

    let paramDinamisInputs = document.querySelectorAll('.param-dinamis');
    if (paramDinamisInputs.length > 0) {
        let pStr = "", pInds = "";
        paramDinamisInputs.forEach(inp => {
            let strFull = inp.getAttribute('data-kelompok-nama') + "->" + inp.getAttribute('data-param-label') + "<=>" + inp.value.trim() + "<=>" + "" + "<=>" + inp.getAttribute('data-param-urut') + "<=>" + inp.getAttribute('data-param-id') + "<=>" + inp.getAttribute('data-kelompok-id') + "<=>" + (inp.getAttribute('data-param-ket') || "");
            let strIds  = inp.getAttribute('data-param-jenis') + "<=>" + inp.value.trim() + "<=>" + "" + "<=>" + (inp.getAttribute('data-param-ket') || "");
            pStr += (pStr === "" ? strFull : "\n" + strFull); 
            pInds += (pInds === "" ? strIds : "\n" + strIds);
        });
        dataObj.parameterTambahan = pStr; dataObj.parameterTambahanInds = pInds;
    } else { dataObj.parameterTambahan = window.getVal<%=rnd%>('rawParameterTambahan<%=rnd%>'); dataObj.parameterTambahanInds = window.getVal<%=rnd%>('rawParameterTambahanInds<%=rnd%>'); }

    <% if (modeEdit) { %>
    let verifStr = "";
    document.querySelectorAll('.verifikasi-checkbox').forEach(chk => {
        let ket = document.querySelector('.verifikasi-keterangan[data-id="' + chk.getAttribute('data-id') + '"]');
        let vRow = chk.getAttribute('data-id') + "<=>" + chk.checked + "<=>" + (ket ? ket.value.trim() : "");
        verifStr += (verifStr === "" ? vRow : "\n" + vRow);
    });
    dataObj.verifikasiBerkasRaw = verifStr; 
    <% } %>
    return dataObj;
};

window.eksekusiHtmlScriptRespons<%=rnd%> = function(textResponse) {
 let trimmedResponse = textResponse.trim();
 let hasHTML = /<[a-z][\s\S]*>/i.test(trimmedResponse);
 
 if (!hasHTML) {
     if (trimmedResponse.length > 0 && typeof window.tampilkanToast === 'function') {
         window.tampilkanToast(trimmedResponse, 'bg-info text-white');
     }
 } else {
     const tempContainer = document.createElement('div');
     tempContainer.innerHTML = trimmedResponse;
     
     const scriptsArray = Array.from(tempContainer.getElementsByTagName('script'));
     for (let i = 0; i < scriptsArray.length; i++) {
         let oldScript = scriptsArray[i];
         let scriptNode = document.createElement('script');
         var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
         Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
         scriptNode.type = 'text/javascript';
         if (srcEff) {
             scriptNode.src = srcEff;
             document.body.appendChild(scriptNode);
         } else {
             scriptNode.text = oldScript.innerHTML;
             document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
         }
         oldScript.remove();
     }
     
     if (tempContainer.innerHTML.trim().length > 0) {
         if (/<[a-z][\s\S]*>/i.test(tempContainer.innerHTML)) {
             while (tempContainer.firstChild) {
                 document.body.appendChild(tempContainer.firstChild);
             }
         } else {
             if (typeof window.tampilkanToast === 'function') window.tampilkanToast(tempContainer.textContent.trim(), 'bg-info text-white');
         }
     }
 }
};

// ── Helper utama: tampilkan panel error PMB terstruktur ──
// config = { kode, status, apa, langkah:[], infoAdmin }
// status: 'belum' (default) | 'tidak_pasti' | 'tersimpan'
window._tampilkanErrorDetailPmb<%=rnd%> = function(config) {
    var panel = document.getElementById('panelErrorPmb<%=rnd%>');
    if (!panel) return;
    if (typeof config === 'string') {
        // backward-compat: dipanggil dengan (kode, apa, langkahStr)
        var k = config, a = arguments[1] || '', l = arguments[2] || '';
        config = { kode: k, apa: a, langkah: l ? l.split(/\n\d+\.\s*/) : [] };
    }
    // Status badge
    var elStatus = document.getElementById('statusDataPmb<%=rnd%>');
    if (elStatus) {
        var st = config.status || 'belum';
        elStatus.textContent = st === 'tidak_pasti' ? 'STATUS TIDAK PASTI — PERIKSA KE PETUGAS'
                             : st === 'tersimpan'   ? 'DATA TERSIMPAN'
                             : 'DATA BELUM TERSIMPAN';
    }
    // Apa yang terjadi
    var elApa = document.getElementById('pesanApaPmb<%=rnd%>');
    if (elApa) elApa.textContent = config.apa || 'Terjadi kendala yang tidak diketahui.';
    // Langkah — bisa array atau string
    var elLangkah = document.getElementById('langkahErrorPmb<%=rnd%>');
    if (elLangkah) {
        elLangkah.innerHTML = '';
        var steps = Array.isArray(config.langkah) ? config.langkah
                  : (config.langkah || '').split(/\n/).filter(function(s){ return s.trim().length > 0; });
        steps.forEach(function(s) {
            var li = document.createElement('li');
            li.textContent = s.replace(/^\d+\.\s*/, '');
            li.style.marginBottom = '.2rem';
            elLangkah.appendChild(li);
        });
    }
    // Kode & waktu
    var elKode = document.getElementById('kodeErrorPmb<%=rnd%>');
    if (elKode) elKode.textContent = config.kode || '';
    var elWaktu = document.getElementById('waktuErrorPmb<%=rnd%>');
    if (elWaktu) {
        var now = new Date();
        elWaktu.textContent = now.toLocaleDateString('id-ID') + ' pukul ' + now.toLocaleTimeString('id-ID');
    }
    // Info admin
    var elAdmin = document.getElementById('infoAdminPmb<%=rnd%>');
    if (elAdmin) elAdmin.textContent = config.infoAdmin || '';

    panel.style.display = 'block';
    panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    if (typeof tampilkanToast === 'function')
        tampilkanToast('<%= Common.getBahasaConfigJS("Pendaftaran belum berhasil. Silakan periksa keterangan berwarna merah di bawah tombol Simpan.") %>', 'bg-danger text-white');
};

// ── Parsing pesan terstruktur dari server (format [KODE] apa... Langkah yang disarankan: ...) ──
window._parsePesanServerPmb<%=rnd%> = function(desc) {
    var kode = '', apa = desc || '', langkahArr = [];
    // ekstrak [KODE]
    var m = apa.match(/^\[([A-Z0-9\-]+)\]\s*/);
    if (m) { kode = m[1]; apa = apa.substring(m[0].length); }
    // pisahkan antara deskripsi dan langkah
    var sepIdx = apa.indexOf('Langkah yang disarankan:');
    if (sepIdx === -1) sepIdx = apa.indexOf('Yang perlu Anda lakukan:');
    if (sepIdx > 0) {
        var langkahStr = apa.substring(sepIdx + apa.substring(sepIdx).indexOf(':') + 1).trim();
        apa = apa.substring(0, sepIdx).trim();
        // pisah per nomor "1. " "2. " dll
        langkahArr = langkahStr.split(/(?=\(\d+\)|(?<!\d)\d+\.\s)/).map(function(s){ return s.trim(); })
                               .filter(function(s){ return s.length > 0; });
        if (langkahArr.length <= 1) {
            // fallback split by \n
            langkahArr = langkahStr.split(/\n|\,(?=\s*\()/).map(function(s){ return s.trim().replace(/^\(\d+\)\s*/, ''); })
                                   .filter(function(s){ return s.length > 0; });
        }
    }
    return { kode: kode, apa: apa, langkah: langkahArr };
};

window.eksekusiSimpanDataLatarBelakang<%=rnd%> = async function() {
 const payloadData = window.getPayloadBiodata<%=rnd%>();

 try {
     const valRes = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_validasi_service', {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify({ data: payloadData })
     });
     
     const textValResponse = await valRes.text();
     let valJson = null;
     let isValJson = false;

     try {
         valJson = JSON.parse(textValResponse);
         isValJson = true;
     } catch(e) { isValJson = false; }
     
     if (isValJson) {
         if (valJson.status === 'error') {
             if (typeof window.tampilkanToast === 'function') window.tampilkanToast(valJson.message, 'bg-danger text-white');
             else alert(valJson.message);
             return { status: "validation_error", message: valJson.message }; 
         }
     } else {
         window.eksekusiHtmlScriptRespons<%=rnd%>(textValResponse);
         window._tampilkanErrorDetailPmb<%=rnd%>({
             kode: 'PMB-SRV-V01',
             status: 'belum',
             apa: 'Server kampus memberikan respons yang tidak dapat dibaca oleh sistem saat memeriksa formulir. Kemungkinan server sedang dalam proses pemeliharaan atau mengalami gangguan sementara. Data Anda BELUM tersimpan.',
             langkah: [
                 'Tunggu 5–10 menit, kemudian muat ulang halaman pendaftaran (tekan F5 atau tombol refresh).',
                 'Isi kembali formulir dari awal dan coba simpan lagi.',
                 'Jika masalah berlangsung lebih dari 30 menit, hubungi petugas PMB dan sampaikan kode PMB-SRV-V01 beserta waktu kejadian.'
             ],
             infoAdmin: 'Validasi service mengembalikan respons non-JSON. Periksa status server dan log aplikasi.'
         });
         return { status: "validation_error", message: "Respons validasi tidak dikenali." };
     }

 } catch (e) {
     console.error("Gagal melakukan validasi", e);
     window._tampilkanErrorDetailPmb<%=rnd%>({
         kode: 'PMB-NET-V01',
         status: 'belum',
         apa: 'Koneksi internet Anda tidak stabil sehingga sistem tidak dapat memeriksa kelengkapan formulir. Data pendaftaran Anda BELUM tersimpan — jangan tutup halaman ini.',
         langkah: [
             'Periksa apakah Wi-Fi atau data seluler Anda aktif dan berjalan normal.',
             'Jangan tutup, refresh, atau berpindah dari halaman ini.',
             'Tunggu hingga koneksi stabil, lalu klik kembali tombol "Simpan Data Pendaftaran".',
             'Jika sudah dicoba 3 kali dan tetap gagal, hubungi petugas PMB dan sampaikan kode PMB-NET-V01 beserta waktu kejadian.'
         ],
         infoAdmin: 'Gagal fetch ke validasi service. Kemungkinan koneksi client. Cek apakah endpoint validasi service dapat diakses.'
     });
     return { status: "validation_error" };
 }

 const payload = { 
     action: "simpanDataRinci", 
     log: "true", 
     tanpaLogin: "true", 
     class: window.masterModelClass<%=rnd%>, 
     data: payloadData 
 };
 
 try {
     const response = await fetch('<%=Common.ROOT%>/Data?baru=true', { 
         method: 'POST', 
         headers: { 'Content-Type': 'application/json' }, 
         body: JSON.stringify(payload) 
     });
     
     const textSaveResponse = await response.text();
     let saveJson = null;
     let isSaveJson = false;

     try {
         saveJson = JSON.parse(textSaveResponse);
         isSaveJson = true;
     } catch(e) { isSaveJson = false; }

     if (isSaveJson) {
         return saveJson;
     } else {
         window.eksekusiHtmlScriptRespons<%=rnd%>(textSaveResponse);
         window._tampilkanErrorDetailPmb<%=rnd%>({
             kode: 'PMB-SRV-S01',
             status: 'tidak_pasti',
             apa: 'Server menerima permintaan penyimpanan data Anda, tetapi memberikan respons yang tidak dapat dibaca sistem. Kami TIDAK DAPAT MEMASTIKAN apakah data Anda sudah tersimpan atau belum.',
             langkah: [
                 'Jangan langsung mendaftar ulang — data Anda mungkin sudah masuk.',
                 'Hubungi petugas PMB dalam waktu dekat dan tanyakan apakah data Anda dengan nama "' + (document.querySelector('[name="nama"]') ? document.querySelector('[name="nama"]').value : '...') + '" sudah tercatat.',
                 'Jika data belum ada, petugas akan membantu Anda mendaftar ulang.',
                 'Sampaikan kode PMB-SRV-S01 dan waktu kejadian kepada petugas.'
             ],
             infoAdmin: 'Endpoint /Data simpanDataRinci mengembalikan respons non-JSON. Periksa log server dan tabel biodata_calon_mahasiswa untuk nama tersebut.'
         });
         return { status: "validation_error", message: "Respons simpan peladen tidak dikenali." };
     }

 } catch (error) {
     console.error("Gagal mengirim data ke server", error);
     window._tampilkanErrorDetailPmb<%=rnd%>({
         kode: 'PMB-NET-S01',
         status: 'belum',
         apa: 'Koneksi internet Anda terputus tepat saat sistem mencoba mengirim dan menyimpan data pendaftaran ke server. Data Anda BELUM tersimpan — jangan tutup halaman ini karena formulir masih terisi.',
         langkah: [
             'Periksa koneksi internet Anda — pastikan Wi-Fi atau data seluler aktif dan stabil.',
             'Jangan tutup, refresh, atau berpindah dari halaman ini (data formulir yang sudah diisi tidak akan hilang).',
             'Tunggu hingga koneksi kembali stabil, lalu klik kembali tombol "Simpan Data Pendaftaran".',
             'Jika sudah 3 kali mencoba dan tetap gagal, hubungi petugas PMB. Sampaikan kode PMB-NET-S01 dan waktu kejadian.'
         ],
         infoAdmin: 'Gagal fetch ke endpoint /Data?baru=true. Kemungkinan putus koneksi dari sisi client. Tidak ada data yang tersimpan.'
     });
     return null;
 }
};

window.forceGoToStep<%=rnd%> = function(step) {
    for(let i=1; i<=5; i++){
        let p = document.getElementById('paneStep'+i+'<%=rnd%>');
        if(p) p.classList.remove('active');
        let nav = document.getElementById('navStep'+i+'<%=rnd%>');
        if(nav) { nav.classList.remove('active'); if(i < step) nav.classList.add('completed'); else nav.classList.remove('completed'); }
    }
    let targetPane = document.getElementById('paneStep'+step+'<%=rnd%>'); if(targetPane) targetPane.classList.add('active');
    let targetNav = document.getElementById('navStep'+step+'<%=rnd%>'); if(targetNav) targetNav.classList.add('active');
    const modalEl = document.querySelector('.modal-dialog-scrollable .modal-body'); if(modalEl) modalEl.scrollTo({ top: 0, behavior: 'smooth' });
};

window.goToStep<%=rnd%> = async function(step, btnElement) {
 let currentPane = document.querySelector('.wizard-pane-<%=rnd%>.active') || document.querySelector('.step-content-<%=rnd%>.active');
 if(!currentPane) return;

 let isValid = true, firstInvalid = null;
 let inputs = currentPane.querySelectorAll('input:not([type="button"]):not([type="submit"]):not([type="file"]), select, textarea');

 inputs.forEach(inp => {
     if (!window.isElementHiddenSpecifically<%=rnd%>(inp, currentPane)) {
         if (!window.validateElement<%=rnd%>(inp, currentPane)) { 
             isValid = false; 
             if (!firstInvalid) firstInvalid = inp; 
         }
     }
 });

 let chks = currentPane.querySelectorAll('.info-kampus-check-<%=rnd%>');
 if(chks.length > 0 && !window.isElementHiddenSpecifically<%=rnd%>(chks[0], currentPane)) {
     let hasChecked = Array.from(chks).some(c => c.checked);
     if(!hasChecked) { isValid = false; if(!firstInvalid) firstInvalid = chks[0]; }
 }
 
 if(!isValid) {
     if(firstInvalid) { 
         firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
         firstInvalid.focus(); 
         if (typeof firstInvalid.reportValidity === 'function') firstInvalid.reportValidity(); 
     }
     if (typeof window.tampilkanToast === 'function') {
         window.tampilkanToast('<%= Common.getBahasaConfigJS("Harap lengkapi isian wajib pada form ini sebelum melanjutkan.") %>', 'bg-warning text-dark');
     } else {
         alert('<%= Common.getBahasaConfigJS("Harap lengkapi isian wajib pada form ini sebelum melanjutkan.") %>');
     }
     return; 
 }
 
 <% if (modeEdit) { %>
     if (btnElement) {
         let originalText = btnElement.innerHTML;
         btnElement.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> <%= Common.getBahasaConfig("Menyimpan...") %>'; 
         btnElement.disabled = true;
         
         let result = await window.eksekusiSimpanDataLatarBelakang<%=rnd%>();
         let success = result && (result.status === '00' || result.status === 'success' || result.id);
         
         btnElement.innerHTML = originalText;
         btnElement.disabled = false;

         if (!success) {
             if (!result || result.status !== 'validation_error') {
                 if (typeof window.tampilkanToast === 'function') window.tampilkanToast('<%= Common.getBahasaConfigJS("Gagal menyimpan data otomatis.") %>', 'bg-danger text-white');
             }
             return; 
         }
     }
 <% } %>
 
 if (typeof window.forceGoToStep<%=rnd%> === 'function') window.forceGoToStep<%=rnd%>(step);
 else if (typeof window.showStep<%=rnd%> === 'function') window.showStep<%=rnd%>(step);
};

window.simpanBiodata<%=rnd%> = async function() {
 let currentPane = document.getElementById('paneStep5<%=rnd%>');
 let inputs = currentPane ? currentPane.querySelectorAll('input:not([type="button"]):not([type="submit"]):not([type="file"]), select, textarea') : [];
 let isValid = true, firstInvalid = null;

 inputs.forEach(inp => { if (!window.validateElement<%=rnd%>(inp, currentPane)) { isValid = false; if (!firstInvalid) firstInvalid = inp; } });

 let chks = document.querySelectorAll('.info-kampus-check-<%=rnd%>');
 if(chks.length > 0 && !window.isElementHiddenSpecifically<%=rnd%>(chks[0], currentPane)) {
     if(!Array.from(chks).some(c => c.checked)) { isValid = false; if(!firstInvalid) firstInvalid = chks[0]; }
 }

 if(!isValid) {
     if(firstInvalid) { 
         firstInvalid.focus();
         if (typeof firstInvalid.reportValidity === 'function') firstInvalid.reportValidity(); 
     }
     tampilkanToast('<%= Common.getBahasaConfigJS("Harap lengkapi semua isian wajib sebelum menyimpan.") %>', 'bg-warning text-dark');
     return;
 }

 let btn = document.getElementById('btnSubmit<%=rnd%>');
 let originalText = btn ? btn.innerHTML : "";

 if (btn) { 
     btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> <%= Common.getBahasaConfig("Menyimpan...") %>'; 
     btn.disabled = true;
 }

 let result = await window.eksekusiSimpanDataLatarBelakang<%=rnd%>();
 let success = result && (result.status === '00' || result.status === 'success' || result.id);

 if (success) {
     let camaId = result.id || '<%= modeEdit ? biodataCalonMahasiswa.getId() : "" %>';
     tampilkanToast('<%= Common.getBahasaConfigJS("Data Pendaftaran Berhasil Disimpan! Sedang menyiapkan dokumen...") %>', 'bg-success text-white');

     <% if (!modeEdit) { %>
     try {
         const tempUploadId = '<%= idUploadFoto %>';
         if (tempUploadId && camaId) {
             const reqObjFoto = {
                 "action": "update_file",
                 "id": tempUploadId,
                 "class": "ais.database.model.file.FotoBiodataCalonMahasiswa",
                 "jenis": "<%= ais.database.model.file.FotoBiodataCalonMahasiswa.DEFAULT_JENIS %>",
                 "ref": camaId,
                 "tanpaLogin": "true",
                 "log": "true",
                 "log_response": "true"
             };
             
             await fetch('<%=Common.ROOT%>/Data', {
                 method: 'POST',
                 headers: { 'Content-Type': 'application/json' },
                 body: JSON.stringify(reqObjFoto)
             });
         }
     } catch (err) {
         console.error("Gagal memperbarui referensi foto ke ID Mahasiswa:", err);
     }
     <% } %>

     try {
         if (camaId) {
             if (btn) btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> <%= Common.getBahasaConfig("Menyiapkan Dasbor...") %>';

             const formBody = new URLSearchParams();
             formBody.append('auth_action', 'login');
             formBody.append('cama_id', camaId);
             
             const loginRes = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=landing_page', {
                 method: 'POST',
                 headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                 body: formBody.toString()
             });

             const loginData = await loginRes.json();
             if (loginData.status === 'success') {
                 window.location.replace('<%=Common.ROOT%>/pmb?action=sekaligus_cetak_kartu_pendaftaran');
                 return; 
             }
         }
     } catch (e) { console.error("Post-save actions error:", e); }

     setTimeout(() => {
         let formModal = btn ? btn.closest('.modal') : null;
         if (formModal) {
             bootstrap.Modal.getInstance(formModal).hide();
             if (typeof window.cariData === 'function') window.cariData(); else if (typeof window.loadDataData === 'function') window.loadDataData();
         } else { window.location.reload(); }
     }, 1200);

 } else {
     if (!result || result.status !== 'validation_error') {
         if (!result) {
             // null → sudah ditangani di catch fetch (PMB-NET-S01 terpanggil di sana), hanya enable tombol
         } else if (result.description) {
             var _parsed = window._parsePesanServerPmb<%=rnd%>(result.description);
             if (_parsed.kode) {
                 // Pesan terstruktur dengan [KODE] dari server
                 var _langkah = _parsed.langkah.length > 0 ? _parsed.langkah : [
                     'Baca baik-baik keterangan di atas.',
                     'Hubungi petugas administrasi PMB dan sampaikan kode kesalahan serta waktu kejadian.'
                 ];
                 window._tampilkanErrorDetailPmb<%=rnd%>({
                     kode: _parsed.kode,
                     status: 'belum',
                     apa: _parsed.apa,
                     langkah: _langkah,
                     infoAdmin: 'Kode ' + _parsed.kode + ' dikirim server. Cari di log DB/Hibernate sesuai kode.'
                 });
             } else {
                 var _rawDesc = _parsed.apa || result.description || '';
                 var _isTechErr = _rawDesc.indexOf('Error :') === 0 || _rawDesc.indexOf('[SRV') === 0 || _rawDesc.indexOf('[DATA') === 0;
                 if (_isTechErr) {
                     var _techDetail = _rawDesc.replace(/^\[SRV[^\]]*\]\s*|^\[DATA[^\]]*\]\s*|^Error :\s*/, '');
                     window._tampilkanErrorDetailPmb<%=rnd%>({
                         kode: 'PMB-SRV-ERR',
                         status: 'belum',
                         apa: 'Terjadi kesalahan teknis pada sistem saat memproses data pendaftaran Anda. Data Anda BELUM tersimpan.',
                         langkah: [
                             'Ambil screenshot halaman ini sekarang (termasuk kode dan waktu kejadian di bagian bawah).',
                             'Hubungi petugas administrasi PMB dan kirimkan screenshot tersebut.',
                             'Petugas akan memeriksa log sistem dan membantu menyelesaikan masalah.',
                             'Jangan mendaftar ulang sebelum berkonsultasi dengan petugas.'
                         ],
                         infoAdmin: 'Detail teknis dari server: ' + _techDetail
                     });
                 } else {
                     window._tampilkanErrorDetailPmb<%=rnd%>({
                         kode: 'PMB-WARN',
                         status: 'belum',
                         apa: _rawDesc,
                         langkah: [
                             'Baca baik-baik keterangan di atas untuk mengetahui apa yang perlu diperbaiki.',
                             'Perbaiki isian formulir yang bermasalah, lalu coba simpan kembali.',
                             'Jika tidak jelas apa yang harus diperbaiki, hubungi petugas PMB dan sampaikan pesan di atas beserta kode PMB-WARN.'
                         ],
                         infoAdmin: 'Pesan dari server tidak memiliki kode terstruktur. Periksa log warnings di ElearningApiUtil.'
                     });
                 }
             }
         } else {
             window._tampilkanErrorDetailPmb<%=rnd%>({
                 kode: 'PMB-SRV-S02',
                 status: 'tidak_pasti',
                 apa: 'Server menerima permintaan Anda namun tidak memberikan keterangan apapun tentang hasil penyimpanan. Kami tidak dapat memastikan apakah data Anda sudah tersimpan atau belum.',
                 langkah: [
                     'Jangan langsung mendaftar ulang — data Anda mungkin sudah masuk.',
                     'Hubungi petugas PMB dan tanyakan apakah data Anda sudah tercatat di sistem.',
                     'Sampaikan nama lengkap Anda, kode PMB-SRV-S02, dan waktu kejadian kepada petugas.'
                 ],
                 infoAdmin: 'Server mengembalikan JSON valid tapi tanpa field description dan status bukan 00/success. Periksa log simpanDataRinci.'
             });
         }
     }
     if (btn) { btn.innerHTML = originalText; btn.disabled = false; }
 }
};

window.toggleAsalNegara<%=rnd%> = function() {
    const el = document.getElementById('kewarganegaraan<%=rnd%>'), div = document.getElementById('divAsalNegara<%=rnd%>'), input = document.getElementById('asalNegara<%=rnd%>');
    if(!el || !div) return;
    if(el.value === 'WNA') { div.style.display = 'flex'; if(input && input.getAttribute('data-wajib') === 'true') input.setAttribute('required', 'required'); } 
    else { div.style.display = 'none'; if(input) { if(input.hasAttribute('required')) input.dataset.required = 'true'; input.removeAttribute('required'); input.classList.remove('border-danger', 'is-invalid'); } }
};

window.toggleJurusanLain<%=rnd%> = function() {
    const sel = document.getElementById('jurusanSekolah<%=rnd%>'), div = document.getElementById('divJurusanLain<%=rnd%>'), input = document.getElementById('jurusanSekolahLain<%=rnd%>');
    if(!sel || !div) return;
    if((sel.options[sel.selectedIndex] ? sel.options[sel.selectedIndex].text.toLowerCase() : "").includes('lain')) { div.style.display = 'flex'; if(input && input.getAttribute('data-wajib') === 'true') input.setAttribute('required', 'required'); } 
    else { div.style.display = 'none'; if(input) { if(input.hasAttribute('required')) input.dataset.required = 'true'; input.removeAttribute('required'); input.classList.remove('border-danger', 'is-invalid'); } }
};

window.togglePindahan<%=rnd%> = function() {
    const isChecked = window.getCheck<%=rnd%>('merupakanPindahan<%=rnd%>'), panel = document.getElementById('panelPindahan<%=rnd%>');
    if(!panel) return;
    if(isChecked) { panel.style.display = 'flex'; panel.querySelectorAll('input').forEach(inp => { if(inp.getAttribute('data-wajib') === 'true') inp.setAttribute('required', 'required'); }); } 
    else { panel.style.display = 'none'; panel.querySelectorAll('input').forEach(inp => { if(inp.hasAttribute('required')) inp.dataset.required = 'true'; inp.removeAttribute('required'); inp.classList.remove('border-danger', 'is-invalid'); inp.value = ''; }); }
};

window.targetWilayah<%=rnd%> = 'pendaftar';
window.bolehTambahKecamatan<%=rnd%> = <%= Common.bolehKonfigurasi("pmb_tambah_kecamatan_aktif") %>;

window.bukaModalCariWilayah<%=rnd%> = function(target) {
    window.targetWilayah<%=rnd%> = target;
    const modalId = 'modalWilayah<%=rnd%>';
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
    document.body.insertAdjacentHTML('beforeend', '<div class="modal fade" id="' + modalId + '" tabindex="-1" style="z-index: 1060;"><div class="modal-dialog modal-lg modal-dialog-centered"><div class="modal-content rounded-4 border-0 shadow"><div class="modal-header bg-light border-0 py-3"><h5 class="modal-title fw-bold"><i class="fas fa-map-location-dot me-2 text-primary"></i>Cari Kecamatan/Kota</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body p-4"><div class="input-group mb-3"><input type="text" id="inputKeywordWilayah<%=rnd%>" class="form-control shadow-none" placeholder="Ketik nama wilayah..." onkeydown="if(event.key === \'Enter\') window.cariDataWilayah<%=rnd%>()"><button class="btn btn-primary px-4" type="button" onclick="window.cariDataWilayah<%=rnd%>()"><i class="fas fa-magnifying-glass"></i></button></div><div id="hasilPencarianWilayah<%=rnd%>" class="list-group list-group-flush border rounded-3" style="max-height: 300px; overflow-y: auto;"><div class="p-3 text-center text-muted small">Silakan lakukan pencarian.</div></div></div></div></div></div>');
    new bootstrap.Modal(document.getElementById(modalId)).show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
};

window.cariDataWilayah<%=rnd%> = async function() {
    const keyword = document.getElementById('inputKeywordWilayah<%=rnd%>').value.trim();
    if (keyword.length < 3) { tampilkanToast('<%= Common.getBahasaConfigJS("Silakan ketik minimal 3 huruf.") %>', 'bg-warning text-dark'); return; }
    document.getElementById('hasilPencarianWilayah<%=rnd%>').innerHTML = '<div class="p-3 text-center"><span class="spinner-border spinner-border-sm text-primary"></span></div>';
    try {
        const res = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=wilayah&keyword=' + encodeURIComponent(keyword));
        const data = (await res.json()).data || [];
        if(data.length === 0) {
            let notFoundHtml = '<div class="p-3 text-center text-danger small">Tidak ditemukan.</div>';
            if (window.bolehTambahKecamatan<%=rnd%>) {
                notFoundHtml += '<div class="text-center pb-2"><button type="button" class="btn btn-sm btn-outline-success rounded-pill" onclick="window.bukaModalTambahKecamatan<%=rnd%>(\'' + keyword.replace(/'/g, "\\'") + '\')"><i class="fas fa-plus me-1"></i>Tambahkan Kecamatan</button></div>';
            }
            document.getElementById('hasilPencarianWilayah<%=rnd%>').innerHTML = notFoundHtml;
            return;
        }
        let html = '';
        data.forEach(r => html += '<button type="button" class="list-group-item list-group-item-action py-2 hover-bg-light" onclick="window.pilihWilayah<%=rnd%>(\'' + r.id + '\', \'' + r.nama.replace(/'/g, "\\'") + '\')"><i class="fas fa-location-dot text-muted me-2"></i>' + r.nama + '</button>');
        document.getElementById('hasilPencarianWilayah<%=rnd%>').innerHTML = html;
    } catch (e) { document.getElementById('hasilPencarianWilayah<%=rnd%>').innerHTML = '<div class="p-3 text-center text-danger small">Kesalahan Koneksi</div>'; }
};

window.pilihWilayah<%=rnd%> = function(id, nama) {
    const target = window.targetWilayah<%=rnd%>; 

    if (target === 'pendaftar') { 
        document.getElementById('kecamatanCalon<%=rnd%>').value = id; 
        document.getElementById('textKecamatan<%=rnd%>').value = nama; 
        if (typeof window.autoFillKotaPropinsi<%=rnd%> === 'function') window.autoFillKotaPropinsi<%=rnd%>(id, 'Calon');
    } 
    else if (target === 'ortu') { 
        document.getElementById('kecamatanOrtu<%=rnd%>').value = id; 
        document.getElementById('textKecamatanOrtu<%=rnd%>').value = nama; 
        if (typeof window.autoFillKotaPropinsi<%=rnd%> === 'function') window.autoFillKotaPropinsi<%=rnd%>(id, 'Ortu');
    }
    else if (target === 'sekolah') {
        let idInput = document.getElementById('kecamatanSekolah<%=rnd%>'), textInput = document.getElementById('textKecamatanSekolah<%=rnd%>');
        if (idInput) idInput.value = id;
        if (textInput) textInput.value = nama;
        if (typeof window.autoFillKotaPropinsi<%=rnd%> === 'function') window.autoFillKotaPropinsi<%=rnd%>(id, 'Sekolah');
    }

    const modalEl = document.getElementById('modalWilayah<%=rnd%>');
    if (modalEl) {
        const modalInst = bootstrap.Modal.getInstance(modalEl);
        if (modalInst) modalInst.hide();
    }
};

window.autoFillKotaPropinsi<%=rnd%> = async function(kecamatanId, tipeTarget) {
    if (!kecamatanId) return;
    try {
        const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=get_kota_propinsi_by_kecamatan&id=' + kecamatanId;
        const response = await fetch(url);
        const data = await response.json();
        
        if (data.status === 'success') {
            const idProp = 'propinsi' + tipeTarget + '<%=rnd%>', txtProp = 'textPropinsi' + tipeTarget + '<%=rnd%>';
            if (document.getElementById(idProp)) document.getElementById(idProp).value = data.propinsi_id || "";
            if (document.getElementById(txtProp)) document.getElementById(txtProp).value = data.propinsi_nama || "";
            
            const idKota = 'kota' + tipeTarget + '<%=rnd%>', txtKota = 'textKota' + tipeTarget + '<%=rnd%>';
            if (document.getElementById(idKota)) document.getElementById(idKota).value = data.kota_id || "";
            if (document.getElementById(txtKota)) document.getElementById(txtKota).value = data.kota_nama || "";
        } else {
            ['propinsi', 'textPropinsi', 'kota', 'textKota'].forEach(prefix => {
                const el = document.getElementById(prefix + tipeTarget + '<%=rnd%>');
                if (el) el.value = "";
            });
        }
    } catch (e) { console.error("Gagal sinkronisasi wilayah:", e); }
};

window.bukaModalTambahKecamatan<%=rnd%> = function(keyword) {
    const modalWilEl = document.getElementById('modalWilayah<%=rnd%>');
    if (modalWilEl) { const mi = bootstrap.Modal.getInstance(modalWilEl); if(mi) mi.hide(); }
    const modalId = 'modalTambahKecamatan<%=rnd%>';
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
    document.body.insertAdjacentHTML('beforeend',
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" style="z-index:1070;">' +
        '<div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-4 border-0 shadow">' +
        '<div class="modal-header bg-light border-0 py-3"><h5 class="modal-title fw-bold text-success">' +
        '<i class="fas fa-circle-plus me-2"></i>Tambah Kecamatan Baru</h5>' +
        '<button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>' +
        '<div class="modal-body p-4">' +
        '<div class="mb-3"><label class="form-label fw-semibold">Nama Kecamatan</label>' +
        '<input type="text" id="inputBaruKecamatan<%=rnd%>" class="form-control shadow-none"></div>' +
        '<div class="mb-3"><label class="form-label fw-semibold">Kabupaten / Kota ' +
        '<span class="fw-normal text-muted" style="font-size:12px">(opsional, untuk auto-isi Kota/Propinsi)</span></label>' +
        '<div class="input-group mb-2"><input type="text" id="inputKabKecamatan<%=rnd%>" class="form-control shadow-none" placeholder="Ketik nama kab/kota..."' +
        ' onkeydown="if(event.key===\'Enter\') window.cariDataKabKecamatan<%=rnd%>()">' +
        '<button class="btn btn-outline-secondary px-3" type="button" onclick="window.cariDataKabKecamatan<%=rnd%>()"><i class="fas fa-magnifying-glass"></i></button></div>' +
        '<div id="hasilKabKecamatan<%=rnd%>" class="list-group list-group-flush border rounded-3" style="max-height:160px;overflow-y:auto;display:none;"></div>' +
        '<input type="hidden" id="selectedKabId<%=rnd%>" value="">' +
        '<div id="selectedKabLabel<%=rnd%>" class="text-success small mt-1" style="display:none;"></div></div>' +
        '<button type="button" class="btn btn-success w-100 rounded-pill fw-bold shadow-sm" onclick="window.simpanKecamatanBaru<%=rnd%>()"><i class="fas fa-save me-2"></i>Simpan Kecamatan</button>' +
        '</div></div></div></div>');
    document.getElementById('inputBaruKecamatan<%=rnd%>').value = keyword || '';
    new bootstrap.Modal(document.getElementById(modalId)).show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function() { this.remove(); });
};

window.cariDataKabKecamatan<%=rnd%> = async function() {
    const keyword = document.getElementById('inputKabKecamatan<%=rnd%>').value.trim();
    if (keyword.length < 2) { return; }
    const el = document.getElementById('hasilKabKecamatan<%=rnd%>');
    el.style.display = 'block';
    el.innerHTML = '<div class="p-2 text-center"><span class="spinner-border spinner-border-sm text-primary"></span></div>';
    try {
        const res = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=wilayah_kab&keyword=' + encodeURIComponent(keyword));
        const data = (await res.json()).data || [];
        if(data.length === 0) { el.innerHTML = '<div class="p-2 text-center text-danger small">Tidak ditemukan.</div>'; return; }
        let html = '';
        data.forEach(r => html += '<button type="button" class="list-group-item list-group-item-action py-2" onclick="window.pilihKabKecamatan<%=rnd%>(\'' + r.id + '\', \'' + r.nama.replace(/'/g, "\\'") + '\')"><i class="fas fa-city text-muted me-2"></i>' + r.nama + '</button>');
        el.innerHTML = html;
    } catch(e) { el.innerHTML = '<div class="p-2 text-center text-danger small">Kesalahan koneksi</div>'; }
};

window.pilihKabKecamatan<%=rnd%> = function(id, nama) {
    document.getElementById('selectedKabId<%=rnd%>').value = id;
    const lbl = document.getElementById('selectedKabLabel<%=rnd%>');
    lbl.textContent = '✓ ' + nama;
    lbl.style.display = 'block';
    document.getElementById('hasilKabKecamatan<%=rnd%>').style.display = 'none';
    document.getElementById('inputKabKecamatan<%=rnd%>').value = nama;
};

window.simpanKecamatanBaru<%=rnd%> = async function() {
    const nama = document.getElementById('inputBaruKecamatan<%=rnd%>').value.trim();
    if (!nama) { tampilkanToast('<%= Common.getBahasaConfigJS("Nama kecamatan tidak boleh kosong.") %>', 'bg-warning text-dark'); return; }
    const kabId = document.getElementById('selectedKabId<%=rnd%>').value;
    try {
        const params = new URLSearchParams({ action: 'tambah_kecamatan', nama: nama });
        if (kabId) params.append('kab_id', kabId);
        const res = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service', { method: 'POST', body: params });
        const result = await res.json();
        if (result.status === 'success' && result.id) {
            tampilkanToast('<%= Common.getBahasaConfigJS("Kecamatan berhasil ditambahkan.") %>', 'bg-success text-white');
            window.pilihWilayah<%=rnd%>(result.id, result.nama);
            const modalEl = document.getElementById('modalTambahKecamatan<%=rnd%>');
            if (modalEl) { const mi = bootstrap.Modal.getInstance(modalEl); if(mi) mi.hide(); }
        } else {
            tampilkanToast(result.message || 'Gagal menyimpan.', 'bg-danger text-white');
        }
    } catch(e) { tampilkanToast('Kesalahan koneksi.', 'bg-danger text-white'); }
};

window.bukaModalCariSekolah<%=rnd%> = function() {
    const modalId = 'modalSekolah<%=rnd%>';
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
    document.body.insertAdjacentHTML('beforeend', '<div class="modal fade" id="' + modalId + '" tabindex="-1" style="z-index: 1060;"><div class="modal-dialog modal-lg modal-dialog-centered"><div class="modal-content rounded-4 border-0 shadow"><div class="modal-header bg-light border-0 py-3"><h5 class="modal-title fw-bold"><i class="fas fa-school me-2 text-primary"></i>Cari Sekolah Asal</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body p-4"><div class="input-group mb-3"><input type="text" id="inputKeywordSekolah<%=rnd%>" class="form-control shadow-none" placeholder="Ketik nama sekolah..." onkeydown="if(event.key === \'Enter\') window.cariDataSekolah<%=rnd%>()"><button class="btn btn-primary px-4" type="button" onclick="window.cariDataSekolah<%=rnd%>()"><i class="fas fa-magnifying-glass"></i></button></div><div id="hasilPencarianSekolah<%=rnd%>" class="list-group list-group-flush border rounded-3 mb-3" style="max-height: 250px; overflow-y: auto;"></div><div class="text-center mt-2"><button type="button" class="btn btn-sm btn-outline-success rounded-pill mt-1" onclick="window.bukaModalTambahSekolah<%=rnd%>()"><i class="fas fa-plus me-1"></i>Tambah Sekolah Baru</button></div></div></div></div></div>');
    new bootstrap.Modal(document.getElementById(modalId)).show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
};

window.cariDataSekolah<%=rnd%> = async function() {
    const keyword = document.getElementById('inputKeywordSekolah<%=rnd%>').value.trim();
    if (keyword.length < 3) { tampilkanToast('<%= Common.getBahasaConfigJS("Silakan ketik minimal 3 huruf.") %>', 'bg-warning text-dark'); return; }
    document.getElementById('hasilPencarianSekolah<%=rnd%>').innerHTML = '<div class="p-3 text-center"><span class="spinner-border spinner-border-sm text-primary"></span></div>';
    try {
        const res = await fetch('<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa_service&action=sekolah_asal&keyword=' + encodeURIComponent(keyword));
        const data = (await res.json()).data || [];
        if(data.length === 0) { document.getElementById('hasilPencarianSekolah<%=rnd%>').innerHTML = '<div class="p-3 text-center text-danger small">Tidak ditemukan.</div>'; return; }
        let html = '';
        data.forEach(r => html += '<button type="button" class="list-group-item list-group-item-action py-2 hover-bg-light" onclick="window.pilihSekolah<%=rnd%>(\'' + r.id + '\', \'' + r.nama.replace(/'/g, "\\'") + '\')"><i class="fas fa-building text-muted me-2"></i> ' + r.nama + '</button>');
        document.getElementById('hasilPencarianSekolah<%=rnd%>').innerHTML = html;
    } catch (e) { document.getElementById('hasilPencarianSekolah<%=rnd%>').innerHTML = '<div class="p-3 text-center text-danger small">Kesalahan Koneksi</div>'; }
};

window.pilihSekolah<%=rnd%> = function(id, nama) {
    document.getElementById('namaSekolahAsal<%=rnd%>').value = id; document.getElementById('textNamaSekolah<%=rnd%>').value = nama; 
    bootstrap.Modal.getInstance(document.getElementById('modalSekolah<%=rnd%>')).hide();
};

window.bukaModalTambahSekolah<%=rnd%> = function() {
    bootstrap.Modal.getInstance(document.getElementById('modalSekolah<%=rnd%>')).hide();
    const modalId = 'modalTambahSekolah<%=rnd%>';
    if(document.getElementById(modalId)) document.getElementById(modalId).remove();
    document.body.insertAdjacentHTML('beforeend', '<div class="modal fade" id="' + modalId + '" tabindex="-1" style="z-index: 1060;"><div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-4 border-0 shadow"><div class="modal-header bg-light border-0 py-3"><h5 class="modal-title fw-bold text-success"><i class="fas fa-circle-plus me-2"></i>Tambah Sekolah Baru</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body p-4"><input type="text" id="inputBaruSekolah<%=rnd%>" class="form-control shadow-none mb-3" placeholder="Ketik Nama Sekolah Baru..."><button type="button" class="btn btn-success w-100 rounded-pill fw-bold shadow-sm" onclick="window.simpanSekolahBaru<%=rnd%>()"><i class="fas fa-save me-2"></i> Simpan Sekolah</button></div></div></div></div>');
    new bootstrap.Modal(document.getElementById(modalId)).show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
};

window.simpanSekolahBaru<%=rnd%> = async function() {
    const namaBaru = document.getElementById('inputBaruSekolah<%=rnd%>').value.trim();
    if(namaBaru === '') return;
    try {
        const res = await fetch('<%=Common.ROOT%>/Data?baru=true', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: "ais.database.model.NamaSekolahAsal", tanpaLogin: "true", data: { nama: namaBaru, aktif: true, tingkat: "SMA" } }) });
        const result = await res.json();
        if (result.status === '00' || result.status === 'success' || result.id) {
            tampilkanToast('<%= Common.getBahasaConfigJS("Sekolah baru berhasil ditambahkan.") %>', 'bg-success text-white');
            window.pilihSekolah<%=rnd%>(result.id, namaBaru);
            bootstrap.Modal.getInstance(document.getElementById('modalTambahSekolah<%=rnd%>')).hide();
        }
    } catch (e) { console.error(e); }
};

window.syncAlamatOrtu<%=rnd%> = function() {
    if(window.getCheck<%=rnd%>('alamatSama<%=rnd%>')) {
        ['alamat','rt','rw','kodePos'].forEach(f => { if(document.getElementById(f+'Ortu<%=rnd%>')) document.getElementById(f+'Ortu<%=rnd%>').value = window.getVal<%=rnd%>(f+'<%=rnd%>') || ""; });
        if(document.getElementById('kelurahanOrtu<%=rnd%>')) document.getElementById('kelurahanOrtu<%=rnd%>').value = window.getVal<%=rnd%>('kelurahanCalon<%=rnd%>') || "";
        
        if(document.getElementById('kecamatanOrtu<%=rnd%>')) document.getElementById('kecamatanOrtu<%=rnd%>').value = window.getVal<%=rnd%>('kecamatanCalon<%=rnd%>') || "";
        if(document.getElementById('textKecamatanOrtu<%=rnd%>')) document.getElementById('textKecamatanOrtu<%=rnd%>').value = window.getVal<%=rnd%>('textKecamatan<%=rnd%>') || "";
        
        if(document.getElementById('kotaOrtu<%=rnd%>')) document.getElementById('kotaOrtu<%=rnd%>').value = window.getVal<%=rnd%>('kotaCalon<%=rnd%>') || "";
        if(document.getElementById('textKotaOrtu<%=rnd%>')) document.getElementById('textKotaOrtu<%=rnd%>').value = window.getVal<%=rnd%>('textKotaCalon<%=rnd%>') || "";
        
        if(document.getElementById('propinsiOrtu<%=rnd%>')) document.getElementById('propinsiOrtu<%=rnd%>').value = window.getVal<%=rnd%>('propinsiCalon<%=rnd%>') || "";
        if(document.getElementById('textPropinsiOrtu<%=rnd%>')) document.getElementById('textPropinsiOrtu<%=rnd%>').value = window.getVal<%=rnd%>('textPropinsiCalon<%=rnd%>') || "";
    }
};

setTimeout(async () => {
    await window.loadDataReferensi<%=rnd%>();
    
    <% if (modeEdit) { %>
        if (document.getElementById('jenisKartuIdentitas<%=rnd%>')) { document.getElementById('jenisKartuIdentitas<%=rnd%>').value = '<%= vJenisIdentitas %>'; }
        if (document.getElementById('merupakanPindahan<%=rnd%>') && document.getElementById('merupakanPindahan<%=rnd%>').checked) window.togglePindahan<%=rnd%>();
        window.toggleAsalNegara<%=rnd%>();
        window.toggleJurusanLain<%=rnd%>();
        window.toggleInfoKampus<%=rnd%>();
    <% } %>
    
    if ('<%= vTahunLulus %>' !== '') {
        if (document.getElementById('tahunLulus<%=rnd%>')) document.getElementById('tahunLulus<%=rnd%>').value = '<%= vTahunLulus %>';
    }
    window.onTahunAkademikChange<%=rnd%>();
    
    setTimeout(() => { window.isInitialLoad<%=rnd%> = false; }, 1000);
}, 150);

if (typeof tampilkanToast !== 'function') window.tampilkanToast = function(msg) { alert(msg.replace(/<\/?[^>]+(>|$)/g, "")); };

window.currentStep<%=rnd%> = 1;

window.showStep<%=rnd%> = function(step) {
    document.querySelectorAll('.step-content-<%=rnd%>').forEach(el => el.classList.remove('active'));
    document.getElementById('stepContent<%=rnd%>_' + step).classList.add('active');
    document.querySelectorAll('.step-item-<%=rnd%>').forEach(el => {
        let s = parseInt(el.getAttribute('data-step'));
        el.classList.remove('active', 'completed');
        if (s === step) el.classList.add('active');
        else if (s < step) el.classList.add('completed');
    });
    window.currentStep<%=rnd%> = step;
    window.scrollTo({ top: 0, behavior: 'smooth' });
};

window.gelombangFotoWajibMap<%=rnd%> = {
    <%
        org.hibernate.Session sessGel = null;
        try {
            sessGel = ais.database.hibernate.HibernateUtil.openSession();
            java.util.List<Object[]> listGel = sessGel.createCriteria(ais.database.model.GelombangPendaftaran.class)
                .setProjection(org.hibernate.criterion.Projections.projectionList()
                    .add(org.hibernate.criterion.Projections.property("id"))
                    .add(org.hibernate.criterion.Projections.property("fotoWajibDiuplad"))
                ).list();
                
            for (Object[] row : listGel) {
                Long idGel = (Long) row[0]; Boolean fotoWajib = (Boolean) row[1];
                boolean isWajib = fotoWajib != null && fotoWajib;
                out.print("'" + idGel + "': " + isWajib + ",");
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa.jsp:2223");
        } finally {
            if (sessGel != null) {
                try { sessGel.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa.jsp:2226");}
                try { sessGel.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa.jsp:2227");}
                try { sessGel.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa.jsp:2228");}
            }
        }
    %>
};

window.cekWajibFotoGelombang<%=rnd%> = function() {
    const inputGelombang = document.getElementById('gelombangPendaftaran<%=rnd%>');
    const labelFoto = document.getElementById('labelUploadFoto<%=rnd%>');
    if (inputGelombang && labelFoto) {
        let isWajib = false;
        if (inputGelombang.tagName === 'SELECT') {
            const selectedId = inputGelombang.value;
            if (selectedId && window.gelombangFotoWajibMap<%=rnd%>[selectedId] === true) isWajib = true;
        } else {
            isWajib = inputGelombang.getAttribute('data-foto-wajib') === 'true';
        }
        
        if (isWajib) {
            labelFoto.innerHTML = '<i class="fas fa-camera me-1"></i> <%= Common.getBahasaConfig("Foto Profil") %> <span class="text-danger fw-bold">* (<%= Common.getBahasaConfig("Wajib Diunggah") %>)</span>';
        } else {
            labelFoto.innerHTML = '<i class="fas fa-camera me-1"></i> <%= Common.getBahasaConfig("Foto Profil (Opsional/Bila Ada)") %>';
        }
    }
};

setTimeout(() => {
    window.cekWajibFotoGelombang<%=rnd%>();
}, 1800);
</script>