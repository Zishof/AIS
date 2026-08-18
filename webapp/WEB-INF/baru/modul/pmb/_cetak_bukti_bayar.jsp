<%@page import="ais.action.ws.util.ConstantUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.action.master.helper.KegiatanHelper"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.CommonPMB"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    String idCama = request.getParameter("id");
    
    // Menggunakan GeneralValueObject.ambilData sesuai standar framework
    BiodataCalonMahasiswa cama = idCama == null || idCama.trim().isEmpty() ? null : 
        (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, idCama.trim(), true);
        
    Session sessionLocal = null;
    
    // Variabel Penampung Data Pembayaran
    Kegiatan kegReg = null;
    Kegiatan kegDU = null;
    String fotoUrl = Common.ROOT + "/img/default-avatar.png";

    if (cama == null) {
        out.print("<div class='text-center py-5 text-danger'>" + Common.getBahasaConfig("Gagal memuat data profil.") + "</div>");
        return;
    }

    try {
        sessionLocal = HibernateUtil.openSession();
        
        // 1. Logika Sinkronisasi Pembayaran Registrasi (Sesuai Java)
        JenisKegiatan jkReg = CommonPMB.pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
        kegReg = cama.ambilKegiatans(null, jkReg);
        
        if (kegReg == null || kegReg.getId() == null) {
            kegReg = KegiatanHelper.checkKegiatanCalonMahasiswa(jkReg, cama, 0, cama.getTahunAkademik(), true, false, null, sessionLocal);
            if (kegReg != null) {
                Transaction tx = sessionLocal.beginTransaction();
                cama.setPembayaranRegistrasi(kegReg);
                sessionLocal.update(cama);
                tx.commit();
            }
        }

        // 2. Logika Sinkronisasi Pembayaran Daftar Ulang (Sesuai Java)
        if (cama.getProdiLulus() != null) {
            JenisKegiatan jkDU = CommonPMB.pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
            kegDU = cama.ambilKegiatans(null, jkDU);
            
            if (kegDU == null || kegDU.getId() == null) {
                kegDU = KegiatanHelper.checkKegiatanCalonMahasiswa(jkDU, cama, 1, cama.getTahunAkademik(), true, false, null, sessionLocal);
                if (kegDU != null) {
                    Transaction tx = sessionLocal.beginTransaction();
                    cama.setPembayaranDaftarUlang(kegDU);
                    sessionLocal.update(cama);
                    tx.commit();
                }
            }
        }

        // Foto
        try {
            String tempUrl = CommonMedia.getUrlFotoPengguna(new ais.database.model.Tbmuser(cama));
            if(tempUrl != null && !tempUrl.trim().isEmpty()) fotoUrl = tempUrl;
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_bayar.jsp:72");}

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_bayar.jsp:75");
    } finally {
        if (sessionLocal != null) {
            try { sessionLocal.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_bayar.jsp:78");}
            try { sessionLocal.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_bayar.jsp:79");}
            try { sessionLocal.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_bukti_bayar.jsp:80");}
        }
    }
%>

<style>
    .card-print-<%=rnd%> { border-radius: 1rem; border: 1px solid #e9ecef; }
    .label-print-<%=rnd%> { color: #6c757d; font-size: 0.85rem; font-weight: 600; margin-bottom: 2px; }
    .val-print-<%=rnd%> { color: #212529; font-weight: 700; font-size: 1rem; }
    .status-box-<%=rnd%> { background-color: #f8f9fa; border-radius: 0.75rem; padding: 1rem; margin-top: 0.5rem; }
</style>

<div class="container-fluid py-3">
    <div class="row align-items-center mb-4 pb-3 border-bottom">
        <div class="col-auto">
            <img src="<%= fotoUrl %>" class="rounded-circle border border-3 border-white shadow-sm" style="width: 80px; height: 80px; object-fit: cover;" onerror="this.src='<%=Common.ROOT%>/img/default-avatar.png'">
        </div>
        <div class="col">
            <h5 class="fw-bold mb-1 text-dark"><%= cama.getNama().toUpperCase() %></h5>
            <div class="badge bg-primary rounded-pill px-3"><%= Common.getBahasaConfig("No. Registrasi") %>: <%= cama.getNoRegistrasi() %></div>
            <% if(cama.getNoUjian() != null) { %>
                <div class="badge bg-danger rounded-pill px-3 ms-1"><%= Common.getBahasaConfig("No. Ujian") %>: <%= cama.getNoUjian() %></div>
            <% } %>
        </div>
    </div>

    <div class="row g-3">
        <div class="col-md-6">
            <div class="status-box-<%=rnd%> h-100 d-flex flex-column">
                <div class="label-print-<%=rnd%>"><i class="fas fa-file-invoice me-1"></i> <%= Common.getBahasaConfig("Pembayaran Registrasi") %></div>
                <div class="flex-grow-1 my-3 text-center">
                    <% if (kegReg != null && (kegReg.getAmount() + kegReg.getAmountTerhutang()) < 0.01) { %>
                        <p class="text-success small mb-0"><i class="fas fa-circle-check me-1"></i> <%= Common.getBahasaConfig("Bebas biaya pendaftaran") %></p>
                    <% } else if (kegReg == null || kegReg.getPersentaseLunas() < 0.01) { %>
                        <p class="text-danger small mb-0"><i class="fas fa-circle-xmark me-1"></i> <%= Common.getBahasaConfig("Belum melakukan pembayaran") %></p>
                    <% } else { %>
                        <button type="button" class="btn btn-primary w-100 rounded-pill mb-2 shadow-sm" onclick="window.eksekusiCetakBayar<%=rnd%>('<%= kegReg.getId() %>')">
                            <i class="fas fa-print me-2"></i><%= Common.getBahasaConfig("Cetak Bukti Registrasi") %>
                        </button>
                        <small class="text-muted d-block">
                            <%= kegReg.getPersentaseLunas().intValue() == 100 ? Common.getBahasaConfig("Lunas") : Common.getBahasaConfig("Terbayar") %>: 
                            <%= Common.numberFormat.get().format(kegReg.getAmount()) %>
                        </small>
                    <% } %>
                </div>
            </div>
        </div>

        <div class="col-md-6">
            <div class="status-box-<%=rnd%> h-100 d-flex flex-column border-start border-3 border-success">
                <div class="label-print-<%=rnd%>"><i class="fas fa-building-columns me-1"></i> <%= Common.getBahasaConfig("Pembayaran Daftar Ulang") %></div>
                <div class="flex-grow-1 my-3 text-center">
                    <% if (cama.getProdiLulus() == null) { %>
                        <p class="text-muted small mb-0"><i class="fas fa-hourglass-half me-1"></i> <%= Common.getBahasaConfig("Menunggu pengumuman kelulusan") %></p>
                    <% } else if (kegDU == null || kegDU.getPersentaseLunas() < 0.01) { %>
                        <p class="text-warning small mb-0"><i class="fas fa-triangle-exclamation me-1"></i> 
                            <%= (kegDU != null && (kegDU.getAmount() + kegDU.getAmountTerhutang()) < 0.01) ? Common.getBahasaConfig("Tidak ada tagihan") : Common.getBahasaConfig("Belum bayar") + " " + (kegDU == null ? "" : Common.numberFormat.get().format(kegDU.getAmount() + kegDU.getAmountTerhutang())) %>
                        </p>
                    <% } else { %>
                        <button type="button" class="btn btn-success w-100 rounded-pill mb-2 shadow-sm" onclick="window.eksekusiCetakBayar<%=rnd%>('<%= kegDU.getId() %>')">
                            <i class="fas fa-print me-2"></i><%= Common.getBahasaConfig("Cetak Bukti Daftar Ulang") %>
                        </button>
                        <small class="text-muted d-block">
                            <%= kegDU.getPersentaseLunas().intValue() == 100 ? Common.getBahasaConfig("Lunas") : Common.getBahasaConfig("Bayar") %>: 
                            <%= Common.numberFormat.get().format(kegDU.getAmount()) %>
                        </small>
                    <% } %>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    window.eksekusiCetakBayar<%=rnd%> = function(idKegiatan) {
        // 1. Cari fungsi cetak PDF bawaan dari halaman utama yang sedang aktif di memori
        const targetFunctionKey = Object.keys(window).find(key => key.startsWith('bukaModalCetakPDFAIS'));
        
        // 2. Jika ditemukan, eksekusi dengan parameter cetak bukti pembayaran
        if (targetFunctionKey && typeof window[targetFunctionKey] === 'function') {
            window[targetFunctionKey]('_cetak_bukti_pembayaran_mhs&kegiatanId=' + idKegiatan);
        } else {
            // 3. Fallback jika fungsi di halaman utama belum selesai dimuat
            console.error("Fungsi cetak PDF utama tidak ditemukan.");
            if (typeof window.tampilkanToast === 'function') {
                window.tampilkanToast('<%= Common.getBahasaConfigJS("Fungsi cetak belum termuat sempurna. Silakan muat ulang (Refresh) halaman.") %>', 'bg-danger text-white');
            } else {
                alert('<%= Common.getBahasaConfigJS("Fungsi cetak belum termuat sempurna.") %>');
            }
        }
    };
</script>