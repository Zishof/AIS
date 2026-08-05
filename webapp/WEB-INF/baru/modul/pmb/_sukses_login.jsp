<%@page import="ais.action.master.helper.KegiatanHelper"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.GelombangPendaftaran"%>
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
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.JadwalPembayaran"%>
<%@page import="ais.database.model.RuangPaketPMB"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.common.VerifikasiPMBHtmlHelper"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
String rnd = Common.getGeneratedBarCode(7);

// =========================================================================
// LOGIKA PENGAMBILAN DATA CALON MAHASISWA
// =========================================================================
// Ambil kembali sesi tbmuser untuk kebutuhan validasi tombol/akses di bawahnya
Tbmuser tbmuser = Common.getCurrentUser(request);
// 1. Prioritaskan ambil data dari sesi login aktif
BiodataCalonMahasiswa cama = Common.isLogin(request);
if(tbmuser != null && cama != null){
	tbmuser.setBiodataCalonMahasiswa(cama);
}
if(tbmuser == null && cama != null){
	tbmuser = new Tbmuser(cama);
	Common.setLogin(request, response, cama);
}
// 2. Jika di sesi tidak ada (null), coba cari dari parameter 'id' atau sesi Tbmuser
if (cama == null) {
    String idParam = request.getParameter("id");
    Long camaId = null;

    // Identifikasi ID Calon Mahasiswa dari parameter
    if (idParam != null && !idParam.trim().isEmpty()) {
        try { camaId = Long.parseLong(idParam.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:53");}
    } 
    // Identifikasi ID dari sesi Tbmuser (bila pendaftar adalah admin/pegawai yang login)
    else {
        if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
            camaId = tbmuser.getBiodataCalonMahasiswa().getId();
        }
    }

    // Ambil data menggunakan GeneralValueObject jika ID ditemukan
    if (camaId != null) {
        cama = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, camaId.toString(), true);
    }
}

// 3. Jika masih null (baik dari isLogin, parameter ID, maupun Tbmuser) -> Kembalikan ke Halaman Utama PMB
if (cama == null) {
    // Melakukan redirect menggunakan javascript ke halaman awal pmb
    out.print("<script>window.location.replace('" + Common.ROOT + "/pmb');</script>");
    return;
}



// =========================================================================
// VARIABEL PENAMPUNG DATA UI PROFIL
// =========================================================================
boolean tampilkanInformasiPembyaran = false, tampilkanInterview = false, tampilkanInformasiUjian = false;
boolean tampilkanUjianOnline = false, tampilkanBuktiDiterima = false, harusLulusSblmDaftarUlang = false;
boolean allowEditBiodata = true; String denyEditMessage = "";

String gelombang = "-", seleksi = "-", paket = "-", periode = "-";
String noRegistrasi = "-", noUjian = "-", namaLengkap = "-", ttl = "-";
String prodiStr = "-", prodiDiterima = "-", program = "-", ruangUjian = "-";
String teksBayarReg = "-", teksBayarDU = "-", fotoUrl = Common.ROOT + "/img/default-avatar.png";
boolean showBtnBayarReg = false, showBtnBayarDU = false;

// Variabel Penampung Data TAMBAHAN, VERIFIKASI & PEMBAYARAN
String paramRaw = "";
boolean hasParam = false;
Kegiatan kegReg = null;
Kegiatan kegDaftarUlang = null;
Session hibSession = null;
// PENAMBAHAN: Ambil Batas Tanggal dari Gelombang

            String batasLengkapiData = "-";
            String batasLogin = "-";
            String tglTagihanReg = "-";
            String tglTagihanDU = "-";
try {
    // MEMBUKA SESI HIBERNATE SECARA AMAN
    hibSession = HibernateUtil.openSession();

			try{
				// Muat ulang dari hibSession (managed instance) agar tidak terjadi
				// NonUniqueObjectException / LazyInitializationException saat
				// checkKegiatanCalonMahasiswa menyimpan Kegiatan baru — pola sama dengan
				// PMBAction.buatTagihanCalonMahasiswaJikaBelumAda.
				BiodataCalonMahasiswa camaFresh = (BiodataCalonMahasiswa) hibSession.get(
						BiodataCalonMahasiswa.class, cama.getId());
				if (camaFresh != null) {
					cama = camaFresh;
					if (tbmuser != null) {
						tbmuser.setBiodataCalonMahasiswa(camaFresh);
					}
					JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
					Kegiatan pembayaranRegistrasi = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan,
							camaFresh, 0, camaFresh.getTahunAkademik(), true, false, null, hibSession);
					cama.setPembayaranRegistrasi(pembayaranRegistrasi);
					kegReg = pembayaranRegistrasi;
					jenisKegiatan = ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
					Kegiatan kegiatanDaftarUlang = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan,
							camaFresh, 1, camaFresh.getTahunAkademik(), true, false, null, hibSession);
					cama.setPembayaranDaftarUlang(kegiatanDaftarUlang);
					kegDaftarUlang = kegiatanDaftarUlang;
				}
			}catch(Exception e){
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:119");
			}
   
    
		    // =========================================================================
		    // VALIDASI IZIN EDIT BIODATA (Berdasarkan Gelombang & Pembayaran)
		    // =========================================================================
		    if (tbmuser == null || (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null)) {
		        boolean harusBayarSblmLogin = cama.getGelombangPendaftaran() != null && cama.getGelombangPendaftaran().getHarusBayarSebelumBisaLogin() != null ? cama.getGelombangPendaftaran().getHarusBayarSebelumBisaLogin() : false;
		        
		        if (harusBayarSblmLogin) {
		            Kegiatan reg = cama.getPembayaranRegistrasi();
		            if (reg == null || reg.getPersentaseLunas() < 0.01) {
		                allowEditBiodata = false;
		                denyEditMessage = Common.getBahasaConfig("Calon mahasiswa harus melakukan pembayaran registrasi terlebih dahulu sebelum dapat melengkapi biodata dan berkas.");
		            }
		        }
		
		        if (allowEditBiodata && cama.getGelombangPendaftaran() != null && cama.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir() != null) {
		            Date tglBerakhir = cama.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir();
		            Date sekarang = WaktuUtil.getDate();
		            if (tglBerakhir.before(sekarang)) {
		                if (!Common.dateFormat1.get().format(tglBerakhir).equals(Common.dateFormat1.get().format(sekarang))) {
		                    allowEditBiodata = false;
		                    denyEditMessage = Common.getBahasaConfig("Masa untuk melengkapi biodata dan berkas belum dimulai atau telah berakhir. Harap periksa kembali jadwal gelombang Anda.");
		                }
		            }
		        }
		    }

            // =========================================================================
            // 2. EKSTRAKSI DATA PROFIL & KONFIGURASI UMUM
            // =========================================================================
            tampilkanInformasiPembyaran = Common.getKonfigurasi("tampilkan_informasi_pembyaran_di_pmb", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            tampilkanInterview = Common.getKonfigurasi("tampilkan_interview_di_pmb", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            tampilkanInformasiUjian = Common.getKonfigurasi("tampilkan_informasi_ujian_di_pmb", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            tampilkanUjianOnline = Common.getKonfigurasi("tampilkan_ujian_online_di_pmb", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            tampilkanBuktiDiterima = Common.getKonfigurasi("tampilkan_informasi_bukti_diterima_di_pmb", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            harusLulusSblmDaftarUlang = Common.getKonfigurasi("calon_mahasiswa_harus_lulus_sebelum_bayar_daftar_ulang", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            if (cama.getGelombangPendaftaran() != null) {
                if (cama.getGelombangPendaftaran().getTerdapatInterview() != null) tampilkanInterview = cama.getGelombangPendaftaran().getTerdapatInterview();
                if (cama.getGelombangPendaftaran().getTerdapatUjianOnline() != null) tampilkanUjianOnline = cama.getGelombangPendaftaran().getTerdapatUjianOnline();
            }
            
         // EKSTRAKSI VARIABEL VIEW UNTUK PRESET NILAI UI
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            
            
            
           
            if (cama.getGelombangPendaftaran() != null) {
                GelombangPendaftaran gp = cama.getGelombangPendaftaran();
                
                try { if (gp.getTanggalDaftarUlangBerakhir() != null) batasLengkapiData = sdf.format(gp.getTanggalDaftarUlangBerakhir()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:172");}
                try { if (gp.getTanggalLoginCalonMahasiswaBerakhir() != null) batasLogin = sdf.format(gp.getTanggalLoginCalonMahasiswaBerakhir()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:173");}
                try { if (gp.getTanggalTagihanRegistrasi() != null) tglTagihanReg = sdf.format(gp.getTanggalTagihanRegistrasi()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:174");}
                try { if (gp.getTanggalTagihanDaftarUlang() != null) tglTagihanDU = sdf.format(gp.getTanggalTagihanDaftarUlang()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:175");}
            }

            gelombang = cama.getGelombangPendaftaran() != null ? cama.getGelombangPendaftaran().getNama() : "-";
            seleksi = cama.getJenisSeleksi() != null ? cama.getJenisSeleksi().getNama() : "-";
            paket = cama.getPaket() != null ? cama.getPaket().getNama() : "-";
            periode = (cama.getTahunAkademik() != null ? cama.getTahunAkademik() : "-") + " / " + 
                      (cama.getGelombangPendaftaran() != null && cama.getGelombangPendaftaran().getJenisSemester() != null ? cama.getGelombangPendaftaran().getJenisSemester() : "-");
            noRegistrasi = cama.getNoRegistrasi() != null ? cama.getNoRegistrasi() : "-";
            noUjian = cama.getNoUjian() != null ? cama.getNoUjian() : "-";
            namaLengkap = cama.getNama() != null ? cama.getNama().toUpperCase() : "-";
            
            String tempatLahir = cama.getTempatLahir() != null ? cama.getTempatLahir() : "-";
            String tglLahir = cama.getTanggalLahir() != null ? sdf.format(cama.getTanggalLahir()) : "-";
            ttl = tempatLahir + ", " + tglLahir;
            
            StringBuilder prodiPilihan = new StringBuilder();
            if(cama.getProdi1() != null) prodiPilihan.append(cama.getProdi1().getNama());
            if(cama.getProdi2() != null) prodiPilihan.append(", ").append(cama.getProdi2().getNama());
            if(cama.getProdi3() != null) prodiPilihan.append(", ").append(cama.getProdi3().getNama());
            if(cama.getProdi4() != null) prodiPilihan.append(", ").append(cama.getProdi4().getNama());
            if(cama.getProdi5() != null) prodiPilihan.append(", ").append(cama.getProdi5().getNama());
            prodiStr = prodiPilihan.length() > 0 ? prodiPilihan.toString() : "-";

            if (cama.getMundur() != null && cama.getMundur()) {
                prodiDiterima = Common.getBahasaConfig("Mengundurkan diri");
            } else if (cama.getDitolak() != null && cama.getDitolak()) {
                prodiDiterima = Common.getBahasaConfig("Tidak diterima (Ditolak)");
            } else if (cama.getProdiLulus() != null) {
                prodiDiterima = cama.getProdiLulus().getNama();
            } else {
                prodiDiterima = Common.getBahasaConfig("Belum dinyatakan lulus atau diterima");
            }

            program = cama.getProgram() != null ? cama.getProgram() : "-";
            
            RuangPaketPMB ruangPaketPMB = (RuangPaketPMB) hibSession.createCriteria(RuangPaketPMB.class)
                    .add(Restrictions.eq("biodataCalonMahasiswa.id", cama.getId())).setMaxResults(1).uniqueResult();
            ruangUjian = ruangPaketPMB != null && ruangPaketPMB.getRuangPMB() != null ? ruangPaketPMB.getRuangPMB().getNama() : Common.getBahasaConfig("Belum ditentukan / Ujian Daring");
            
            // Pengambilan Data Kegiatan Pembayaran
            if (kegReg == null) {
                kegReg = cama.chekPembayaranRegistrasi(hibSession);
            }
            if (kegReg != null && (kegReg.getAmount() + kegReg.getAmountTerhutang()) < 0.01) {
                teksBayarReg = "<span class='badge bg-success rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-circle-check me-1'></i>" + Common.getBahasaConfig("Bebas Pembayaran (Gratis)") + "</span>";
            } else if (kegReg == null || (kegReg.getAmount() < 0.01 && kegReg.getPersentaseLunas() < 0.01)) {
                teksBayarReg = "<span class='badge bg-danger rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-circle-exclamation me-1'></i>" + Common.getBahasaConfig("Belum Membayar") + " " + (kegReg == null ? "" : Common.numberFormat.get().format(kegReg.getAmount() + kegReg.getAmountTerhutang())) + "</span>";
            } else if (kegReg.getPersentaseLunas().intValue() == 100) {
                teksBayarReg = "<span class='badge bg-success rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-circle-check me-1'></i>" + Common.getBahasaConfig("Lunas") + " " + Common.numberFormat.get().format(kegReg.getAmount()) + "</span>";
            } else {
                teksBayarReg = "<span class='badge bg-warning text-dark rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-circle-half-stroke me-1'></i>" + Common.getBahasaConfig("Dicicil") + " " + Common.numberFormat.get().format(kegReg.getAmount()) + " " + Common.getBahasaConfig("dari total") + " " + Common.numberFormat.get().format(kegReg.getAmount() + kegReg.getAmountTerhutang()) + "</span>";
            }

            if (kegDaftarUlang == null) {
                kegDaftarUlang = cama.chekPembayaranDaftarUlang(hibSession);
            }
            boolean jadwalDaftarUlangMasihBerlangsung = false;
            if (kegDaftarUlang != null && kegDaftarUlang.getJadwalPembayaran() != null) {
                JadwalPembayaran jadwalDU = kegDaftarUlang.getJadwalPembayaran();
                Date sekarangDU = WaktuUtil.getDate();
                boolean mulaiOk = jadwalDU.getStartDate() == null || !jadwalDU.getStartDate().after(sekarangDU);
                boolean selesaiOk = jadwalDU.getEndDate() == null || !jadwalDU.getEndDate().before(sekarangDU)
                        || Common.dateFormat1.get().format(jadwalDU.getEndDate()).equals(Common.dateFormat1.get().format(sekarangDU));
                jadwalDaftarUlangMasihBerlangsung = mulaiOk && selesaiOk;
            }
            if (cama.getProdiLulus() == null && !jadwalDaftarUlangMasihBerlangsung) {
                teksBayarDU = "<span class='text-muted fst-italic'>" + prodiDiterima + "</span>";
            } else if (kegDaftarUlang == null) {
                teksBayarDU = "<span class='badge bg-secondary rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-circle-info me-1'></i>" + Common.getBahasaConfig("Belum ada tagihan") + "</span>";
            } else if ((kegDaftarUlang.getAmount() + kegDaftarUlang.getAmountTerhutang()) < 0.01) {
                teksBayarDU = "<span class='badge bg-success rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-circle-check me-1'></i>" + Common.getBahasaConfig("Bebas Pembayaran (Gratis)") + "</span>";
            } else if (kegDaftarUlang.getAmount() < 0.01 && kegDaftarUlang.getPersentaseLunas() < 0.01) {
                teksBayarDU = "<span class='badge bg-danger rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-circle-exclamation me-1'></i>" + Common.getBahasaConfig("Belum Membayar") + " " + Common.numberFormat.get().format(kegDaftarUlang.getAmount() + kegDaftarUlang.getAmountTerhutang()) + "</span>";
            } else if (kegDaftarUlang.getPersentaseLunas().intValue() == 100) {
                teksBayarDU = "<span class='badge bg-success rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-circle-check me-1'></i>" + Common.getBahasaConfig("Lunas") + " " + Common.numberFormat.get().format(kegDaftarUlang.getAmount()) + "</span>";
            } else {
                teksBayarDU = "<span class='badge bg-warning text-dark rounded-pill px-3 py-2 shadow-sm'><i class='fas fa-circle-half-stroke me-1'></i>" + Common.getBahasaConfig("Dicicil") + " " + Common.numberFormat.get().format(kegDaftarUlang.getAmount()) + " " + Common.getBahasaConfig("dari total") + " " + Common.numberFormat.get().format(kegDaftarUlang.getAmount() + kegDaftarUlang.getAmountTerhutang()) + "</span>";
            }

            try {
                String tempUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(cama));
                if(tempUrl != null && !tempUrl.trim().isEmpty()) fotoUrl = tempUrl;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:245");}
            
            showBtnBayarReg = tampilkanInformasiPembyaran && kegReg != null
                    && kegReg.getPersentaseLunas() < 99.0
                    && (kegReg.getAmount() + kegReg.getAmountTerhutang()) > 0.0;
            showBtnBayarDU = tampilkanInformasiPembyaran && kegDaftarUlang != null
                    && kegDaftarUlang.getPersentaseLunas() < 100.0
                    && (kegDaftarUlang.getAmount() + kegDaftarUlang.getAmountTerhutang()) > 0.0;
            if (harusLulusSblmDaftarUlang && (cama.getProdiLulus() == null || cama.getProdiLulus().getId() == null)) {
                showBtnBayarDU = jadwalDaftarUlangMasihBerlangsung && showBtnBayarDU;
            }
            if (showBtnBayarDU && cama.getGelombangPendaftaran() != null
                    && cama.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir() != null) {
                Date tglBerakhirDU = cama.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir();
                Date sekarang = WaktuUtil.getDate();
                if (tglBerakhirDU.before(sekarang)
                        && !Common.dateFormat1.get().format(tglBerakhirDU).equals(
                                Common.dateFormat1.get().format(sekarang))) {
                    showBtnBayarDU = false;
                }
            }

            // =========================================================================
            // 3. EKSTRAKSI INFORMASI TAMBAHAN (Read-Only)
            // =========================================================================
            paramRaw = cama.getParameterTambahan();
            hasParam = paramRaw != null && !paramRaw.trim().isEmpty();

            
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:261");
    } finally {
        if (hibSession != null) {
            try { hibSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:264");}
            try { hibSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:265");}
            try { hibSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:266");}
        }
    }
%>

<style>
    .profile-card-<%=rnd%> { border-top: 5px solid #0d6efd; background: #ffffff; }
    .action-buttons-<%=rnd%> .btn { font-size: 0.85rem; font-weight: 600; transition: all 0.2s ease-in-out; border-radius: 50rem; padding: 0.5rem 1.25rem; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
    .action-buttons-<%=rnd%> .btn:hover { transform: translateY(-2px); box-shadow: 0 4px 10px rgba(0,0,0,0.15); }
    .table-profile-<%=rnd%> td { padding: 0.8rem 0.5rem; vertical-align: middle; font-size: 0.95rem; border-bottom: 1px dashed #e9ecef; }
    .table-profile-<%=rnd%> tr:last-child td { border-bottom: none; }
    .table-profile-<%=rnd%> td:first-child { font-weight: 600; color: #6c757d; width: 35%; }
    .foto-mhs-<%=rnd%> { width: 100%; max-width: 180px; aspect-ratio: 3/4; object-fit: cover; border: 4px solid #fff; }
    .bg-gradient-header-<%=rnd%> { background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); }
    
    /* CSS Responsif untuk Modal Edit & Pembayaran */
    @media (min-width: 992px) { 
        .modal-xl-custom-<%=rnd%> { max-width: 90%; } 
        .modal-95w-<%=rnd%> { max-width: 95%; }
    }
    @media (max-width: 991px) { 
        .modal-xl-custom-<%=rnd%>, .modal-95w-<%=rnd%> { max-width: 100%; margin: 0; } 
        .modal-xl-custom-<%=rnd%> .modal-content, .modal-95w-<%=rnd%> .modal-content { min-height: 100vh; border-radius: 0; } 
    }
</style>

<div class="container-fluid py-4 bg-light min-vh-100">
    <div class="row justify-content-center">
        <div class="col-xl-10 col-lg-11">
        
        	<% if (!allowEditBiodata && denyEditMessage != null && !denyEditMessage.isEmpty()) { %>
                <div class="alert alert-danger rounded-4 shadow-sm border-0 mb-4 px-4 py-3">
                    <div class="d-flex align-items-center">
                        <i class="fas fa-calendar-xmark fa-3x me-3 opacity-75"></i>
                        <div>
                            <h6 class="fw-bold mb-1"><%= Common.getBahasaConfig("Status Pendaftaran Terkunci") %></h6>
                            <p class="mb-0 small"><%= denyEditMessage %></p>
                        </div>
                    </div>
                </div>
            <% } %>
            
            <div class="card shadow-sm border-0 rounded-4 profile-card-<%=rnd%> mb-4 overflow-hidden">
                
                <div class="card-header bg-gradient-header-<%=rnd%> border-bottom-0 pt-4 pb-3 px-4 text-center">
                    <h5 class="fw-bold text-dark mb-4">
                        <i class="fas fa-id-badge text-primary me-2 fs-4"></i><%= Common.getBahasaConfig("Profil Calon Mahasiswa") %>
                    </h5>
                    
                    <div class="d-flex flex-wrap justify-content-center gap-2 mb-2 action-buttons-<%=rnd%>">
                        <button type="button" class="btn btn-outline-dark" onclick="window.bukaAlurPendaftaran()"><i class="fas fa-sitemap me-2 text-primary"></i><%= Common.getBahasaConfig("Alur Pendaftaran") %></button>
                        
                        <% if (allowEditBiodata) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalEditBiodata<%=rnd%>('<%=cama.getId()%>')">
                                <i class="fas fa-user-pen me-2 text-success"></i><%= Common.getBahasaConfig("Lengkapi Biodata & Berkas") %>
                            </button>
                        <% } else { %>
                            <button type="button" class="btn btn-outline-dark" onclick="tampilkanToast('<%=Common.jsEscape(denyEditMessage)%>', 'bg-warning text-dark');">
                                <i class="fas fa-user-pen me-2 text-secondary"></i><%= Common.getBahasaConfig("Lengkapi Biodata & Berkas") %>
                            </button>
                        <% } %>

                        <% if (tampilkanInformasiPembyaran) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalBuktiBayar<%=rnd%>('<%=cama.getId()%>')">
                                <i class="fas fa-upload me-2 text-info"></i><%= Common.getBahasaConfig("Unggah Bukti Bayar") %>
                            </button>

                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalPilihanCetakBayar<%=rnd%>()"><i class="fas fa-print me-2 text-secondary"></i><%= Common.getBahasaConfig("Cetak Bukti Bayar") %></button>
                            
                            <% if (showBtnBayarReg && kegReg != null) { 
                                String urlReg = Common.ROOT+"/pmb?hanya_tampil_jsp=true&p=bayarmhs&s=pembayaran_online_mhs&id="+kegReg.getId()+"&refresh=false&autoLoad=true&bolehEditJenisPembayaranDanMahasiswa=true";
                            %>
                                <button type="button" class="btn btn-warning text-dark fw-bold shadow-sm" onclick="window.bukaModalPembayaran<%=rnd%>('<%=urlReg%>', '<%= Common.getBahasaConfigJS("Pembayaran Registrasi") %>')">
                                    <i class="fas fa-money-check-dollar me-2"></i><%= Common.getBahasaConfig("Bayar Registrasi") %>
                                </button>
                            <% } %>
                            
                            <% if (showBtnBayarDU && kegDaftarUlang != null) { 
                                String urlDU = Common.ROOT+"/pmb?hanya_tampil_jsp=true&p=bayarmhs&s=pembayaran_online_mhs&id="+kegDaftarUlang.getId()+"&refresh=false&autoLoad=true&bolehEditJenisPembayaranDanMahasiswa=true";
                            %>
                                <button type="button" class="btn btn-warning text-dark fw-bold shadow-sm" onclick="window.bukaModalPembayaran<%=rnd%>('<%=urlDU%>', '<%= Common.getBahasaConfigJS("Pembayaran Daftar Ulang") %>')">
                                    <i class="fas fa-money-check-dollar me-2"></i><%= Common.getBahasaConfig("Bayar Daftar Ulang") %>
                                </button>
                            <% } %>
                        <% } %>

                        <% if (tampilkanUjianOnline) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalUjianOnline<%=rnd%>('<%=cama.getId()%>')">
                                <i class="fas fa-laptop-code me-2 text-danger"></i><%= Common.getBahasaConfig("Ikut Ujian Sekarang") %>
                            </button>
                        <% } %>
                        
                        <% if (tampilkanInterview) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalWawancara<%=rnd%>('<%=cama.getId()%>')">
                                <i class="fas fa-comments me-2 text-warning"></i><%= Common.getBahasaConfig("Wawancara") %>
                            </button>
                        <% } %>

                        <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalCetakPDFAIS<%=rnd%>('_cetak_kartu_pendaftaran')"><i class="fas fa-barcode me-2 text-dark"></i><%= Common.getBahasaConfig("Cetak No. Registrasi") %></button>
                        <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalCetakPDFAIS<%=rnd%>('_cetak_biodata_calon_mahasiswa')"><i class="fas fa-address-book me-2 text-primary"></i><%= Common.getBahasaConfig("Cetak Biodata") %></button>
                        
                        <% if (tampilkanInformasiUjian) { %>
                            <button type="button" class="btn btn-outline-dark" onclick="window.bukaModalCetakPDFAIS<%=rnd%>('_cetak_kartu_ujian')"><i class="fas fa-id-card me-2 text-info"></i><%= Common.getBahasaConfig("Cetak Kartu Ujian") %></button>
                        <% } %>

                       <% if (cama.getProdiLulus() != null && tampilkanBuktiDiterima) { 
						    // Evaluasi kelengkapan berkas langsung dari server
						    boolean isBerkasLengkap = true;
						    try { isBerkasLengkap = ais.action.master.pmb.BiodataCalonMahasiswaAction.lengkap(cama); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_sukses_login.jsp:374");}
						    
						    boolean harusLengkapCetak = cama.getGelombangPendaftaran() != null && 
						                                cama.getGelombangPendaftaran().getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian() != null && 
						                                cama.getGelombangPendaftaran().getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian();
						    
						    String blockMsg = Common.getBahasaConfig("Maaf, Anda belum bisa melanjutkan, data dan berkas belum lengkap, harap lengkapi data diri di menu Lengkapi Biodata & Berkas.");
						%>
						    <button type="button" class="btn btn-success shadow-sm fw-bold" 
						        onclick="<% if(harusLengkapCetak && !isBerkasLengkap) { %>tampilkanToast('<%=Common.jsEscape(blockMsg)%>', 'bg-warning text-dark');<% } else { %>window.bukaModalCetakPDFAIS<%=rnd%>('_cetak_bukti_diterima');<% } %>">
						        <i class="fas fa-square-check me-2"></i><%= Common.getBahasaConfig("Cetak Bukti Diterima") %>
						    </button>
						<% } %>

                        <% if (cama.getMahasiswa() != null) { %>
						    <button type="button" class="btn btn-primary shadow-sm fw-bold" onclick="window.bukaModalCetakPDFAIS<%=rnd%>('_cetak_e_ktm')">
						        <i class="fas fa-id-card me-2"></i><%= Common.getBahasaConfig("Cetak E-KTM") %>
						    </button>
						<% } %>

                        <% if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) { %>
                            <button type="button" class="btn btn-danger" onclick="if(typeof window.konfirmasiLogoutPMB === 'function') window.konfirmasiLogoutPMB();"><i class="fas fa-power-off me-2"></i><%= Common.getBahasaConfig("Keluar") %></button>
                        <% } %>
                    </div>
                </div>

                <div class="card-body p-4 p-md-5">
                    <div class="row align-items-start">
                        <div class="col-md-4 col-lg-3 text-center mb-4 mb-md-0">
                            <div class="position-relative d-inline-block">
                                <img src="<%= fotoUrl %>" alt="Foto Calon Mahasiswa" class="foto-mhs-<%=rnd%> rounded-4 shadow-sm" onerror="this.src='<%=Common.ROOT%>/img/default-avatar.png'">
                                <span class="position-absolute top-100 start-50 translate-middle badge bg-dark bg-gradient rounded-pill px-3 py-2 shadow border border-2 border-white">
                                    <%= Common.getBahasaConfig("ID Pengguna") %>: <%= cama.getId() %>
                                </span>
                            </div>
                        </div>

                        <div class="col-md-8 col-lg-9">
                        
                        	<div class="d-flex align-items-center mb-2">
						        <i class="fas fa-history text-danger me-2"></i>
						        <h6 class="fw-bold text-dark mb-0" style="font-size: 0.9rem;">
						            <%= Common.getBahasaConfig("Informasi Batas Waktu (Deadline)") %>
						        </h6>
						    </div>
                            
                            <div class="row g-2 mb-4">
                                <div class="col-6 col-lg-3">
                                    <div class="p-2 bg-light border border-success border-opacity-25 rounded-3 h-100 shadow-sm">
                                        <div class="text-muted fw-bold text-truncate mb-1" style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.5px;" title="<%= Common.getBahasaConfig("Batas Upload Berkas") %>">
                                            <i class="fas fa-file-arrow-up text-success me-1"></i><%= Common.getBahasaConfig("Upload Berkas") %>
                                        </div>
                                        <div class="fw-bold text-dark" style="font-size: 0.85rem;"><%= batasLengkapiData %></div>
                                    </div>
                                </div>
                                <div class="col-6 col-lg-3">
                                    <div class="p-2 bg-light border border-danger border-opacity-25 rounded-3 h-100 shadow-sm">
                                        <div class="text-muted fw-bold text-truncate mb-1" style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.5px;" title="<%= Common.getBahasaConfig("Batas Akhir Login") %>">
                                            <i class="fas fa-right-to-bracket text-danger me-1"></i><%= Common.getBahasaConfig("Akhir Login") %>
                                        </div>
                                        <div class="fw-bold text-dark" style="font-size: 0.85rem;"><%= batasLogin %></div>
                                    </div>
                                </div>
                                <div class="col-6 col-lg-3">
                                    <div class="p-2 bg-light border border-warning border-opacity-50 rounded-3 h-100 shadow-sm">
                                        <div class="text-muted fw-bold text-truncate mb-1" style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.5px;" title="<%= Common.getBahasaConfig("Tagihan Pendaftaran") %>">
                                            <i class="fas fa-file-invoice-dollar text-warning me-1"></i><%= Common.getBahasaConfig("Tagihan Daftar") %>
                                        </div>
                                        <div class="fw-bold text-dark" style="font-size: 0.85rem;"><%= tglTagihanReg %></div>
                                    </div>
                                </div>
                                <div class="col-6 col-lg-3">
                                    <div class="p-2 bg-light border border-warning border-opacity-50 rounded-3 h-100 shadow-sm">
                                        <div class="text-muted fw-bold text-truncate mb-1" style="font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.5px;" title="<%= Common.getBahasaConfig("Tagihan Daftar Ulang") %>">
                                            <i class="fas fa-file-invoice-dollar text-warning me-1"></i><%= Common.getBahasaConfig("Tagihan Dftr Ulang") %>
                                        </div>
                                        <div class="fw-bold text-dark" style="font-size: 0.85rem;"><%= tglTagihanDU %></div>
                                    </div>
                                </div>
                            </div>
                            <table class="table table-borderless table-profile-<%=rnd%> m-0 w-100">
                                <tbody>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Gelombang Pendaftaran") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fw-semibold text-primary"><%= gelombang %></span></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Jalur Seleksi") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= seleksi %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Pilihan Paket") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= paket %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Periode / Semester") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= periode %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Nomor Registrasi") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fw-bold fs-6"><%= noRegistrasi %></span></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Nomor Ujian") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fw-bold text-danger fs-6"><%= noUjian %></span></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Nama Lengkap") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fw-bold text-dark fs-5"><%= namaLengkap %></span></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Tempat, Tanggal Lahir") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= ttl %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Program Studi Pilihan") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="badge bg-light text-dark border border-secondary border-opacity-25 text-wrap text-start lh-base"><%= prodiStr %></span></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Program Studi Diterima") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <span class="fst-italic <%= cama.getProdiLulus() != null ? "fw-bold text-success" : "" %>"><%= prodiDiterima %></span></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Program Kuliah") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= program %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Ruangan Ujian (Jika Ada)") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= ruangUjian %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Status Pembayaran Registrasi") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= teksBayarReg %></td>
                                    </tr>
                                    <tr>
                                        <td><%= Common.getBahasaConfig("Status Pembayaran Daftar Ulang") %></td>
                                        <td><span class="d-none d-sm-inline me-2 text-muted">:</span> <%= teksBayarDU %></td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <% if (hasParam) { %>
            <div class="card shadow-sm border-0 rounded-4 profile-card-<%=rnd%> mb-4 overflow-hidden" style="border-top-color: #17a2b8;">
                <div class="card-header bg-gradient-header-<%=rnd%> border-bottom-0 pt-4 pb-3 px-4">
                    <h5 class="fw-bold text-dark mb-0"><i class="fas fa-clipboard-list text-info me-2 fs-5"></i><%= Common.getBahasaConfig("Informasi Tambahan Pendaftaran") %></h5>
                </div>
                <div class="card-body p-4 p-md-5">
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
                                            <a href="<%= url %>" target="_blank" class="btn btn-sm btn-outline-primary rounded-pill px-3 shadow-sm"><i class="fas fa-up-right-from-square me-2"></i><%= Common.getBahasaConfig("Lihat Dokumen / Tautan") %></a>
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

            <div id="containerBerkasPendaftaran<%=rnd%>">
			    <div class="text-center p-5">
			        <div class="spinner-border text-warning" role="status"></div>
			        <p class="mt-2 text-muted fw-bold"><%= Common.getBahasaConfig("Memuat Daftar Berkas...") %></p>
			    </div>
			</div>

        </div>
    </div>
</div>

<script>
// =========================================================================
// FUNGSI UNTUK MEMBUKA POPUP PEMBAYARAN ONLINE (AJAX FETCH TANPA IFRAME)
// LAYER PALING BAWAH (z-index default 1055)
// =========================================================================
window.bukaModalPembayaran<%=rnd%> = async function(url, judul) {
    const modalId = 'modalPembayaran<%=rnd%>';
    const existingModal = document.getElementById(modalId);
    if(existingModal) existingModal.remove();
    
    var modalHtml = 
    '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">' +
        '<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-95w-<%=rnd%>">' +
            '<div class="modal-content rounded-4 border-0 shadow-lg">' +
                '<div class="modal-header bg-light border-0 py-3 px-4">' +
                    '<h5 class="modal-title fw-bold text-primary"><i class="fas fa-credit-card me-2"></i>' + judul + '</h5>' +
                    '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                '</div>' +
                '<div class="modal-body p-0 bg-light" id="bodyModalPembayaran<%=rnd%>">' +
                    '<div class="text-center py-5">' +
                        '<div class="spinner-border text-primary" role="status"></div>' +
                        '<div class="mt-2 text-muted fw-bold"><%= Common.getBahasaConfig("Sedang Memuat Halaman Pembayaran...") %></div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>' +
    '</div>';
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    let modalEl = document.getElementById(modalId);
    let modalObj = new bootstrap.Modal(modalEl);
    modalObj.show();
    try {
        var response = await fetch(url);
        if(!response.ok) throw new Error("Gagal load formulir pembayaran");
        var htmlContent = await response.text();
        
        var bodyEl = document.getElementById('bodyModalPembayaran<%=rnd%>');
        bodyEl.innerHTML = htmlContent;
        var scriptsArray = Array.from(bodyEl.getElementsByTagName('script'));
        for (var i = 0; i < scriptsArray.length; i++) {
            var oldScript = scriptsArray[i];
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            if (srcEff && srcEff.includes('email-decode')) continue;
            var scriptNode = document.createElement('script');
            Array.from(oldScript.attributes).forEach(function(attr) {
                if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value);
            });
            scriptNode.type = 'text/javascript';
            if (srcEff) { scriptNode.src = srcEff;
            document.body.appendChild(scriptNode); }
            else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
            }
        }
    } catch (e) {
        document.getElementById('bodyModalPembayaran<%=rnd%>').innerHTML = '<div class="text-center py-5 text-danger"><i class="fas fa-triangle-exclamation fa-3x mb-3"></i><h5><%= Common.getBahasaConfig("Gagal memuat halaman pembayaran.") %></h5></div>';
    }

    modalEl.addEventListener('hidden.bs.modal', function () { 
        this.remove(); 
        window.location.reload(); 
    });
};

// =========================================================================
// FUNGSI UNTUK MEMBUKA POPUP PENGISIAN BIODATA (EDIT MODE)
// =========================================================================
window.bukaModalEditBiodata<%=rnd%> = async function(idCama) {
    var modalId = 'modalEditCama<%=rnd%>';
    var existingModal = document.getElementById(modalId);
    if(existingModal) existingModal.remove();

    var endpoint = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_pendaftaran_mahasiswa&baru=true&id=' + idCama;
    var titleText = '<%= Common.getBahasaConfigJS("Lengkapi Biodata & Kelengkapan Berkas") %>';
    var modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index: 1055;">' +
            '<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl-custom-<%=rnd%>">' +
                '<div class="modal-content shadow-lg border-0 rounded-4">' +
                    '<div class="modal-header bg-light border-0 py-3 px-4">' +
                        '<h5 class="modal-title fw-bold text-dark">' +
                            '<i class="fas fa-user-pen text-success me-2"></i>' + titleText +
                        '</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0 bg-light" id="bodyModalEditCama<%=rnd%>">' +
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
        
        var bodyEl = document.getElementById('bodyModalEditCama<%=rnd%>');
        bodyEl.innerHTML = htmlContent;
        var scriptsArray = Array.from(bodyEl.getElementsByTagName('script'));
        for (var i = 0; i < scriptsArray.length; i++) {
            var oldScript = scriptsArray[i];
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            if (srcEff && srcEff.includes('email-decode')) continue;
            var scriptNode = document.createElement('script');
            Array.from(oldScript.attributes).forEach(function(attr) {
                if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value);
            });
            scriptNode.type = 'text/javascript';
            if (srcEff) { scriptNode.src = srcEff;
            document.body.appendChild(scriptNode); }
            else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
            }
        }
    } catch (e) {
        document.getElementById('bodyModalEditCama<%=rnd%>').innerHTML = '<div class="text-center py-5 text-danger"><i class="fas fa-triangle-exclamation fa-3x mb-3"></i><h5><%= Common.getBahasaConfig("Gagal memuat formulir pendaftaran.") %></h5></div>';
    }

    modalElement.addEventListener('hidden.bs.modal', function () { 
        this.remove(); 
        window.location.reload(); 
    });
};

//=========================================================================
//FUNGSI UNTUK MENCETAK PDF (MENDUKUNG RESPONS JSON, HTML, SCRIPT, & TEXT)
//=========================================================================
window.bukaModalCetakPDFAIS<%=rnd%> = async function(serviceName) {
  const modalId = 'modalCetakPDF<%=rnd%>';
  const existingModal = document.getElementById(modalId);
  if (existingModal) existingModal.remove();
  
  if (typeof window.showLoadingPMB === 'function') window.showLoadingPMB();
  
  try {
      let urlService = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=' + serviceName + '&id=<%=cama.getId()%>';
      const response = await fetch(urlService);
      const textResponse = await response.text(); // Ambil respons sebagai teks (mentah)
      
      let isJson = false;
      let data = null;
      
      // 1. Coba parsing teks menjadi JSON
      try {
          data = JSON.parse(textResponse);
          isJson = true;
      } catch(e) {
          isJson = false; // Jika gagal, berarti responsnya adalah HTML/Script/Text
      }

      // ====================================================================
      // KONDISI A: RESPONS BERUPA JSON (Tampilkan via Modal Iframe)
      // ====================================================================
      if (isJson) {
          if (data.status === 'success' && data.url) {
              let judulModal = '<%= Common.getBahasaConfigJS("Dokumen Pendaftaran") %>';
              if (serviceName.includes('kartu_ujian')) judulModal = '<%= Common.getBahasaConfigJS("Kartu Ujian Calon Mahasiswa") %>';
              else if (serviceName.includes('biodata')) judulModal = '<%= Common.getBahasaConfigJS("Biodata Calon Mahasiswa") %>';
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
              
              if (typeof window.hideLoadingPMB === 'function') window.hideLoadingPMB();
              modalObj.show();
              
              modalEl.addEventListener('hidden.bs.modal', function () { 
                  this.remove(); 
                  if (serviceName.includes('kartu_ujian')) { window.location.reload(); }
              });
          } else {
              if (typeof window.hideLoadingPMB === 'function') window.hideLoadingPMB();
              tampilkanToast(data.message || '<%= Common.getBahasaConfigJS("Gagal membuat dokumen PDF.") %>', 'bg-danger text-white');
          }
      } 
      // ====================================================================
      // KONDISI B: RESPONS BERUPA HTML, SCRIPT, ATAU TEXT BIASA
      // ====================================================================
      else {
          if (typeof window.hideLoadingPMB === 'function') window.hideLoadingPMB();
          
          let trimmedResponse = textResponse.trim();
          
          // Deteksi keberadaan tag HTML menggunakan Regex
          let hasHTML = /<[a-z][\s\S]*>/i.test(trimmedResponse);
          
          if (!hasHTML) {
              // KONDISI B.1: Teks Biasa (Langsung munculkan Toast)
              if (trimmedResponse.length > 0) {
                  tampilkanToast(trimmedResponse, 'bg-info text-white');
              }
          } else {
              // KONDISI B.2 & B.3: HTML atau Script
              const tempContainer = document.createElement('div');
              tempContainer.innerHTML = trimmedResponse;
              
              // Ekstrak dan Eksekusi semua tag <script>
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
                  // Hapus tag script dari container agar tidak tersisa
                  oldScript.remove();
              }
              
              // Periksa sisa konten setelah tag script dihapus
              if (tempContainer.innerHTML.trim().length > 0) {
                  if (/<[a-z][\s\S]*>/i.test(tempContainer.innerHTML)) {
                      // KONDISI B.2: Terdapat Sisa Tag HTML -> Inject ke Document Body
                      while (tempContainer.firstChild) {
                          document.body.appendChild(tempContainer.firstChild);
                      }
                  } else {
                      // KONDISI B.3: Sisa konten ternyata hanya teks biasa tanpa tag HTML
                      tampilkanToast(tempContainer.textContent.trim(), 'bg-info text-white');
                  }
              }
          }
      }
  } catch (e) {
      if (typeof window.hideLoadingPMB === 'function') window.hideLoadingPMB();
      console.error(e);
      tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat memproses dokumen.") %>', 'bg-danger text-white');
  }
};

// =========================================================================
// FUNGSI UNTUK MEMANGGIL UNGGAH BUKTI PEMBAYARAN
// =========================================================================
window.bukaModalBuktiBayar<%=rnd%> = async function(idCama) {
    const modalId = 'modalBuktiBayar<%=rnd%>';
    const existingModal = document.getElementById(modalId);
    if(existingModal) existingModal.remove();

    const endpoint = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_bukti_bayar&id=' + idCama;
    const titleText = '<%= Common.getBahasaConfigJS("Unggah Bukti Pembayaran") %>';
    const modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index: 1055;">' +
            '<div class="modal-dialog modal-dialog-centered modal-xl">' +
                '<div class="modal-content shadow-lg border-0 rounded-4">' +
                    '<div class="modal-header bg-light border-0 py-3 px-4">' +
                        '<h5 class="modal-title fw-bold text-dark">' +
                            '<i class="fas fa-upload text-info me-2"></i>' + titleText +
                        '</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0 bg-light" id="bodyModalBuktiBayar<%=rnd%>">' +
                        '<div class="text-center py-5">' +
                            '<div class="spinner-border text-primary" role="status"></div>' +
                            '<div class="mt-3 text-muted fw-bold"><%= Common.getBahasaConfig("Sedang Memuat Formulir Unggahan...") %></div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    const modalElement = document.getElementById(modalId);
    new bootstrap.Modal(modalElement).show();

    try {
        const response = await fetch(endpoint);
        if(!response.ok) throw new Error("Gagal load formulir unggah");
        const htmlContent = await response.text();
        
        const bodyEl = document.getElementById('bodyModalBuktiBayar<%=rnd%>');
        bodyEl.innerHTML = htmlContent;
        const scriptsArray = Array.from(bodyEl.getElementsByTagName('script'));
        for (let i = 0; i < scriptsArray.length; i++) {
            let oldScript = scriptsArray[i];
            let srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            let scriptNode = document.createElement('script');
            Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
            scriptNode.type = 'text/javascript';
            if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode);
            }
            else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
            }
        }
    } catch (e) {
        document.getElementById('bodyModalBuktiBayar<%=rnd%>').innerHTML = '<div class="text-center py-5 text-danger"><i class="fas fa-triangle-exclamation fa-3x mb-3"></i><h5><%= Common.getBahasaConfig("Gagal memuat formulir unggahan.") %></h5></div>';
    }

    modalElement.addEventListener('hidden.bs.modal', function () { 
        this.remove(); 
    });
};


//=========================================================================
//FUNGSI UNTUK MEMBUKA PILIHAN CETAK BUKTI BAYAR (REGISTRASI / DAFTAR ULANG)
//=========================================================================
window.bukaModalPilihanCetakBayar<%=rnd%> = async function() {
 const modalId = 'modalCetakBuktiBayar<%=rnd%>';
 const existingModal = document.getElementById(modalId);
 if(existingModal) existingModal.remove();

 const endpoint = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_cetak_bukti_bayar&id=<%=cama.getId()%>';
 const titleText = '<%= Common.getBahasaConfigJS("Cetak Bukti Pembayaran") %>';
 const modalHtml = 
     '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 1055;">' +
         '<div class="modal-dialog modal-dialog-centered">' +
             '<div class="modal-content shadow-lg border-0 rounded-4">' +
                 '<div class="modal-header bg-light border-0 py-3 px-4">' +
                     '<h5 class="modal-title fw-bold text-dark">' +
                         '<i class="fas fa-print text-secondary me-2"></i>' + titleText +
                     '</h5>' +
                     '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                 '</div>' +
                 '<div class="modal-body p-0" id="bodyModalCetakBayar<%=rnd%>">' +
                     '<div class="text-center py-5">' +
                         '<div class="spinner-border text-primary" role="status"></div>' +
                         '<div class="mt-3 text-muted fw-bold"><%= Common.getBahasaConfig("Memeriksa Status Tagihan...") %></div>' +
                     '</div>' +
                 '</div>' +
             '</div>' +
         '</div>' +
     '</div>';
 document.body.insertAdjacentHTML('beforeend', modalHtml);
 const modalElement = document.getElementById(modalId);
 new bootstrap.Modal(modalElement).show();

 try {
     const response = await fetch(endpoint);
     if(!response.ok) throw new Error("Gagal load data");
     const htmlContent = await response.text();
     
     const bodyEl = document.getElementById('bodyModalCetakBayar<%=rnd%>');
     bodyEl.innerHTML = htmlContent;
     // Eksekusi script di dalam konten yang di-fetch (jika ada)
     const scriptsArray = Array.from(bodyEl.getElementsByTagName('script'));
     for (let i = 0; i < scriptsArray.length; i++) {
         let oldScript = scriptsArray[i];
         let scriptNode = document.createElement('script');
         Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
         scriptNode.type = 'text/javascript';
         scriptNode.text = oldScript.innerHTML;
         document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
     }
 } catch (e) {
     document.getElementById('bodyModalCetakBayar<%=rnd%>').innerHTML = '<div class="text-center py-5 text-danger"><i class="fas fa-triangle-exclamation fa-2x mb-2"></i><h5><%= Common.getBahasaConfig("Gagal memuat data pembayaran.") %></h5></div>';
 }

 modalElement.addEventListener('hidden.bs.modal', function () { this.remove(); });
};

// =========================================================================
// FUNGSI UNTUK MEMANGGIL ALUR PENDAFTARAN
// =========================================================================
window.bukaAlurPendaftaran = function() {
    const modalId = 'modalAlurPendaftaranPMB';
    const existingModal = document.getElementById(modalId);
    if (existingModal) existingModal.remove();

    const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_alur_pendaftaran';
    const modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 1060;">' +
            '<div class="modal-dialog modal-xl modal-dialog-centered">' +
                '<div class="modal-content rounded-4 border-0 shadow">' +
                    '<div class="modal-header bg-primary text-white py-3">' +
                        '<h5 class="modal-title fw-bold"><i class="fas fa-route me-2"></i><%= Common.getBahasaConfig("Alur Pendaftaran Mahasiswa Baru") %></h5>' +
                        '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0 bg-light" style="height: 85vh;">' +
                        '<iframe src="' + url + '" style="width:100%; height:100%; border:none;"></iframe>' +
                    '</div>' +
                    '<div class="modal-footer bg-light py-2">' +
                        '<button type="button" class="btn btn-secondary px-4 rounded-pill" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Tutup") %></button>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    const modalEl = document.getElementById(modalId);
    const modalObj = new bootstrap.Modal(modalEl);
    modalObj.show();

    modalEl.addEventListener('hidden.bs.modal', function () { this.remove(); });
    window.tutupModalAlur = function() { modalObj.hide(); };
};


// =========================================================================
// FUNGSI UNTUK MEMANGGIL UJIAN ONLINE
// =========================================================================
window.bukaModalUjianOnline<%=rnd%> = async function(idCama) {
    const modalId = 'modalUjianOnline<%=rnd%>';
    const existingModal = document.getElementById(modalId);
    if(existingModal) existingModal.remove();

    const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_ikut_ujian_online&id=' + idCama;
    // MENGGUNAKAN STRING CONCATENATION BIASA AGAR TIDAK BENTROK DENGAN JSP EL
    const modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index: 1060;">' +
            '<div class="modal-dialog modal-fullscreen">' +
                '<div class="modal-content border-0">' +
                    '<div class="modal-header bg-dark text-white border-0 py-3">' +
                        '<h5 class="modal-title fw-bold"><i class="fas fa-pen-to-square me-2 text-warning"></i>Sistem Ujian Online CBT</h5>' +
                        '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0 bg-light" id="bodyModalUjian<%=rnd%>">' +
                        '<div class="text-center py-5">' +
                            '<div class="spinner-border text-primary" role="status"></div>' +
                            '<div class="mt-2 text-muted fw-bold">Menyiapkan Ruang Ujian...</div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Inisialisasi Modal Bootstrap
    const modalEl = document.getElementById(modalId);
    const modalObj = new bootstrap.Modal(modalEl);
    modalObj.show();
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error("Gagal mengambil data sistem ujian.");
        const htmlContent = await response.text();
        
        const bodyEl = document.getElementById('bodyModalUjian<%=rnd%>');
        bodyEl.innerHTML = htmlContent;
        
        // Eksekusi ulang script yang ditarik dari file _ikut_ujian_online.jsp
        const scriptsArray = Array.from(bodyEl.getElementsByTagName('script'));
        for (let i = 0; i < scriptsArray.length; i++) {
            let oldScript = scriptsArray[i];
            let srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            let scriptNode = document.createElement('script');
            Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
            scriptNode.type = 'text/javascript';
            if (srcEff) {
                scriptNode.src = srcEff;
                document.body.appendChild(scriptNode);
            } else {
                scriptNode.text = oldScript.innerHTML;
                document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
            }
        }
    } catch (e) {
        console.error(e);
        document.getElementById('bodyModalUjian<%=rnd%>').innerHTML = 
            '<div class="p-5 text-center text-danger"><i class="fas fa-triangle-exclamation fa-3x mb-3"></i><br><h5>Gagal memuat sistem ujian.</h5></div>';
    }
    
    // Pastikan DOM dibersihkan saat modal ditutup
    modalEl.addEventListener('hidden.bs.modal', function () { 
        this.remove(); 
    });
};

// =========================================================================
// FUNGSI UNTUK MEMANGGIL SESI WAWANCARA
// =========================================================================
window.bukaModalWawancara<%=rnd%> = async function(idCama) {
    const modalId = 'modalWawancara<%=rnd%>';
    const existingModal = document.getElementById(modalId);
    if(existingModal) existingModal.remove();

    const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_wawancara&id=' + idCama;
    
    const modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" data-bs-backdrop="static" aria-hidden="true" style="z-index: 1060;">' +
            '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                '<div class="modal-content border-0 rounded-4 shadow-lg">' +
                    '<div class="modal-header bg-dark text-white border-0 py-3">' +
                        '<h5 class="modal-title fw-bold"><i class="fas fa-comments me-2 text-warning"></i>' + '<%= Common.getBahasaConfigJS("Sesi Wawancara") %>' + '</h5>' +
                        '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0 bg-light" id="bodyModalWawancara<%=rnd%>">' +
                        '<div class="text-center py-5">' +
                            '<div class="spinner-border text-primary" role="status"></div>' +
                            '<div class="mt-2 text-muted fw-bold">' + '<%= Common.getBahasaConfigJS("Menyiapkan Ruang Wawancara...") %>' + '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    const modalEl = document.getElementById(modalId);
    const modalObj = new bootstrap.Modal(modalEl);
    modalObj.show();

    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error("Gagal mengambil data sistem wawancara.");
        const htmlContent = await response.text();
        
        const bodyEl = document.getElementById('bodyModalWawancara<%=rnd%>');
        bodyEl.innerHTML = htmlContent;

        const scriptsArray = Array.from(bodyEl.getElementsByTagName('script'));
        for (let i = 0; i < scriptsArray.length; i++) {
            let oldScript = scriptsArray[i];
            let srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            let scriptNode = document.createElement('script');
            Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
            scriptNode.type = 'text/javascript';
            if (srcEff) {
                scriptNode.src = srcEff;
                document.body.appendChild(scriptNode);
            } else {
                scriptNode.text = oldScript.innerHTML;
                document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
            }
        }
    } catch (e) {
        console.error(e);
        document.getElementById('bodyModalWawancara<%=rnd%>').innerHTML = 
            '<div class="p-5 text-center text-danger"><i class="fas fa-triangle-exclamation fa-3x mb-3"></i><br><h5>' + '<%= Common.getBahasaConfigJS("Gagal memuat sistem wawancara.") %>' + '</h5></div>';
    }
    
    modalEl.addEventListener('hidden.bs.modal', function () { 
        this.remove(); 
    });
};


//Fungsi untuk memuat ulang tabel berkas saja tanpa refresh halaman penuh
window.reloadBerkas = async function(isForceRefresh = false) {
    const container = document.getElementById('containerBerkasPendaftaran<%=rnd%>');
    if (!container) return;

    // 1. Tampilkan Indikator Loading (Spinner) sebelum proses fetch dimulai
    container.innerHTML = 
        '<div class="text-center p-5">' +
            '<div class="spinner-border text-warning shadow-sm" style="width: 3rem; height: 3rem;" role="status"></div>' +
            '<p class="mt-3 text-muted fw-bold">' + '<%= Common.getBahasaConfigJS("Memperbarui Daftar Berkas...") %>' + '</p>' +
        '</div>';

    // 2. Menyiapkan parameter aturan akses edit dari Java ke JavaScript
    const canEdit = "<%= allowEditBiodata %>";
    const msgDeny = '<%= Common.jsEscape(denyEditMessage) %>';

    // 3. Menyusun URL target ke service JSP dengan menyertakan parameter izin edit
    const url = '<%=Common.ROOT%>/pmb?hanya_tampil_jsp=true&p=pmb&s=_tampilkan_berkas_di_sukses_login' +
                '&id=<%=cama.getId()%>&rnd=<%=rnd%>' + 
                '&allowEditBiodata=' + canEdit + 
                '&denyEditMessage=' + encodeURIComponent(msgDeny) +
                (isForceRefresh ? '&refresh=true' : '');
    
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error("Gagal memuat data berkas");
        
        const html = await response.text();
        
        // 4. Timpa indikator loading dengan konten HTML hasil fetch
        container.innerHTML = html;
        
        // 5. Eksekusi ulang script untuk inisialisasi upload_component (AJAX script handling)
        const scripts = Array.from(container.getElementsByTagName('script'));
        for (const oldScript of scripts) {
            const newScript = document.createElement('script');
            Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') newScript.setAttribute(attr.name, attr.value); });
            newScript.type = 'text/javascript';
            newScript.text = oldScript.innerHTML;
            document.body.appendChild(newScript).parentNode.removeChild(newScript);
        }
        if (typeof window.pmbEnsureFontAwesome === 'function') {
            window.pmbEnsureFontAwesome(container);
            window.setTimeout(function(){ window.pmbEnsureFontAwesome(container); }, 300);
        }
    } catch (e) {
        console.error("Gagal melakukan pembaruan berkas:", e);
        
        // 6. Tampilkan pesan kesalahan visual jika proses gagal (Error State)
        container.innerHTML = 
            '<div class="alert alert-danger shadow-sm rounded-4 p-4 text-center">' +
                '<i class="fas fa-triangle-exclamation fa-2x mb-3"></i>' +
                '<h6 class="fw-bold mb-1">' + '<%= Common.getBahasaConfigJS("Gagal Memperbarui Daftar Berkas") %>' + '</h6>' +
                '<p class="small mb-0">' + '<%= Common.getBahasaConfigJS("Terjadi kendala koneksi dengan peladen. Silakan klik tombol refresh atau muat ulang halaman.") %>' + '</p>' +
            '</div>';
    }
};

//=========================================================================
//AUTO-TRIGGER CETAK KARTU & KIRIM EMAIL SETELAH PENDAFTARAN BARU
//=========================================================================
setTimeout(() => {
 const urlParams = new URLSearchParams(window.location.search);
 if (urlParams.get('action') === 'sekaligus_cetak_kartu_pendaftaran') {
     
     // Panggil fungsi modal cetak PDF dengan menyisipkan parameter kirimEmail=true
     if (typeof window.bukaModalCetakPDFAIS<%=rnd%> === 'function') {
         window.bukaModalCetakPDFAIS<%=rnd%>('_cetak_kartu_pendaftaran&kirimEmail=true');
     }
     
     // Membersihkan URL dari parameter 'action' tanpa me-refresh halaman, 
     // agar popup tidak muncul berulang kali jika peserta menekan tombol Refresh (F5)
     const cleanUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
     window.history.replaceState({path: cleanUrl}, '', cleanUrl);
 }
 window.reloadBerkas();
}, 800); // Delay 800 milidetik agar transisi UI terlihat lebih halus
</script>
