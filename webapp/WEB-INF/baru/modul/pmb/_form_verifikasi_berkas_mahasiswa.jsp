<%@page import="java.util.List"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.VerifikasiPMBHtmlHelper"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7); // Pengaman ruang lingkup CSS khusus
    String gelombangId = request.getParameter("gelombangId");
    String seleksiId = request.getParameter("seleksiId");
    String biodataId = request.getParameter("id");
    
    // DETEKSI PENGGUNA (Apakah Publik/Pendaftar ATAU Admin/Petugas)
    Tbmuser tbmuser = Common.getCurrentUser(request);
    boolean isPendaftar = (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null);
    
    Session hibSession = null;
    
    try {
        // Membuka session secara aman
        hibSession = HibernateUtil.openSession();
        
        // Pengambilan data menggunakan GeneralValueObject untuk mencegah LazyInitializationException
        JenisSeleksi sel = (seleksiId != null && !seleksiId.trim().isEmpty()) ? 
            (JenisSeleksi) GeneralValueObject.ambilData(JenisSeleksi.class, seleksiId, true) : null;
            
        BiodataCalonMahasiswa bio = (biodataId != null && !biodataId.trim().isEmpty()) ? 
            (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, biodataId, true) : null;
            
        GelombangPendaftaran gel = (gelombangId != null && !gelombangId.trim().isEmpty()) ? 
            (GelombangPendaftaran) GeneralValueObject.ambilData(GelombangPendaftaran.class, gelombangId, true) : null;
        
        // Ambil Data List Verifikasi beserta Data yang sudah tersimpan sebelumnya
        List<Object[]> daftarVerifikasi = VerifikasiPMBHtmlHelper.getDaftarVerifikasi(bio, gel, sel, hibSession);

        if (daftarVerifikasi != null && !daftarVerifikasi.isEmpty()) {
%>

<style>
    .chk-verifikasi-<%=rnd%> {
        width: 1.5rem;
        height: 1.5rem;
        cursor: pointer;
    }
    .table-verifikasi-<%=rnd%> th {
        background-color: #f8f9fa !important;
        color: #495057;
        font-weight: 600;
    }
    
    /* PENYESUAIAN KHUSUS UNTUK KOMPONEN UPLOAD */
    .upload-wrapper-<%=rnd%> .btn {
        padding: 0.25rem 0.5rem !important; 
        font-size: 0.7rem !important;       
        border-radius: 0.25rem !important;
        margin: 2px !important;
        line-height: 1.2 !important;
    }
    .upload-wrapper-<%=rnd%> .btn i {
        font-size: 0.7rem !important;       
    }
    .upload-wrapper-<%=rnd%> img {
        max-height: 140px !important;       
        width: auto !important;
        object-fit: cover;
        border-radius: 0.3rem;
        margin-bottom: 0.5rem;
        box-shadow: 0 3px 6px rgba(0,0,0,0.15);
    }
    .upload-wrapper-<%=rnd%> .btn-group, 
    .upload-wrapper-<%=rnd%> .d-grid {
        display: flex !important;
        flex-wrap: wrap !important;
        justify-content: center !important;
        gap: 2px;
    }
</style>

<div class="table-responsive border border-secondary border-opacity-25 rounded-3 bg-white shadow-sm mt-3">
    <table class="table table-hover align-middle mb-0 table-verifikasi-<%=rnd%>">
        <thead>
            <tr>
                <th width="5%" class="text-center py-3">#</th>
                <th width="30%" class="py-3"><i class="fas fa-folder-open me-2 text-primary"></i><%= Common.getBahasaConfig("Nama Kelengkapan / Berkas") %></th>
                <th width="25%" class="text-center py-3"><i class="fas fa-file-arrow-up me-2 text-primary"></i><%= Common.getBahasaConfig("Lampiran Berkas") %></th>
                <th width="15%" class="text-center py-3"><i class="fas fa-circle-check me-2 text-success"></i><%= Common.getBahasaConfig("Terverifikasi") %></th>
                <th width="25%" class="py-3"><i class="fas fa-pen-to-square me-2 text-warning"></i><%= Common.getBahasaConfig("Keterangan / Catatan") %></th>
            </tr>
        </thead>
        <tbody>
<%
            int no = 1;

            for (Object[] rowData : daftarVerifikasi) {
                VerifikasiKelengkapanCalonMahasiswa v = (VerifikasiKelengkapanCalonMahasiswa) rowData[0];
                BiodataCalonMahasiswaPunyaVerifikasiBerkas ex = (BiodataCalonMahasiswaPunyaVerifikasiBerkas) rowData[1];
                
                // =========================================================================
                // LOGIKA AUTO-CREATE RECORD BERKAS AGAR UPLOAD KOMPONEN LANGSUNG AKTIF
                // =========================================================================
                if (ex == null && bio != null && bio.getId() != null) {
                    ex = new BiodataCalonMahasiswaPunyaVerifikasiBerkas();
                    ex.setBiodataCalonMahasiswa(bio);
                    ex.setVerifikasiKelengkapanCalonMahasiswa(v);
                    ex.setVerified(false);
                    ex.setKeterangan("");

                    Transaction tx = null;
                    try {
                        tx = hibSession.beginTransaction();
                        hibSession.save(ex);
                        tx.commit();
                    } catch (Exception exe) {
                        if (tx != null && tx.isActive()) { 
                            try { tx.rollback(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_form_verifikasi_berkas_mahasiswa.jsp:118");} 
                        }
                    }
                }
                // =========================================================================
                
                boolean isChecked = ex != null && ex.getVerified() != null && ex.getVerified();
                String ket = ex != null && ex.getKeterangan() != null ? ex.getKeterangan().replace("\"", "&quot;") : "";
                String tampilUpload = ex.getVerified() ? "false" : "true";
 %>
            <tr>
                <td class="text-center text-muted fw-bold"><%= no++ %></td>
                <td>
                    <span class="fw-semibold text-dark"><%= v.getNama() %></span>
                    <% if (v.getKeterangan() != null && !v.getKeterangan().trim().isEmpty()) { %>
                        <br><small class="text-muted"><i class="fas fa-circle-info me-1"></i><%= v.getKeterangan() %></small>
                    <% } %>
                </td>
                
                <td class="text-center align-middle p-2">
                    <% if (ex != null && ex.getId() != null) { %>
                        <div class="border border-secondary border-opacity-25 rounded bg-light p-2 shadow-sm d-inline-block text-center w-100 upload-wrapper-<%=rnd%>">
                            <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
                                <jsp:param name="clazz" value="<%=ais.database.model.file.LampiranLain.class.getName()%>"/>
                                <jsp:param name="jenis" value="<%=ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName()%>"/>
                                <jsp:param name="id" value="<%=ex.getId().toString()%>"/>
                                <jsp:param name="tampilUpload" value="<%=tampilUpload%>"/>
                                <jsp:param name="loadPreview" value="true"/>
                            </jsp:include>
                        </div>
                    <% } else { %>
                        <div class="alert alert-warning mb-0 px-2 py-3 text-center rounded-3 shadow-sm border-warning border-opacity-50" style="font-size: 12px;">
                            <i class="fas fa-triangle-exclamation d-block mb-1 fs-5 text-warning"></i>
                            <span class="fw-bold text-dark"><%= Common.getBahasaConfig("Simpan Formulir") %></span><br>
                            <span class="text-muted"><%= Common.getBahasaConfig("terlebih dahulu untuk unggah berkas.") %></span>
                        </div>
                    <% } %>
                </td>

                <td class="text-center align-middle">
                    <% if (isPendaftar) { %>
                        <% if (isChecked) { %>
                            <span class="badge bg-success rounded-pill px-3 py-2 shadow-sm"><i class="fas fa-circle-check me-1"></i><%= Common.getBahasaConfig("Valid") %></span>
                        <% } else { %>
                            <span class="badge bg-secondary rounded-pill px-3 py-2 shadow-sm"><i class="fas fa-hourglass-half me-1"></i><%= Common.getBahasaConfig("Menunggu") %></span>
                        <% } %>
                    <% } else { %>
                        <div class="form-check d-flex justify-content-center align-items-center mb-0">
                            <input type="checkbox" class="form-check-input verifikasi-checkbox chk-verifikasi-<%=rnd%> border-secondary" 
                                   data-id="<%= v.getId() %>" <%= isChecked ? "checked " : "" %>>
                        </div>
                    <% } %>
                </td>
                
                <td class="align-middle">
                    <% if (isPendaftar) { %>
                        <div class="p-2 bg-light rounded-3 border border-secondary border-opacity-10 text-dark small" style="min-height: 2.8rem;">
                            <%= ket.isEmpty() ? "<span class=\"text-muted fst-italic\">" + Common.getBahasaConfig("Tidak ada catatan") + "</span>" : ket.replace("&quot;", "\"") %>
                        </div>
                    <% } else { %>
                        <textarea class="form-control form-control-sm shadow-none border-secondary border-opacity-50 verifikasi-keterangan" 
                                  data-id="<%= v.getId() %>" rows="2" placeholder="<%= Common.getBahasaConfig("Ketik catatan opsional di sini...") %>"><%= ket %></textarea>
                    <% } %>
                </td>
            </tr>
<%
            }
%>
        </tbody>
    </table>
</div>

<%
        } else {
%>
        <div class="alert alert-info d-flex align-items-center rounded-3 shadow-sm mt-3" role="alert">
            <i class="fas fa-circle-info fa-2x me-3 text-info"></i>
            <div>
                <strong><%= Common.getBahasaConfig("Informasi") %></strong><br>
                <span class="small"><%= Common.getBahasaConfig("Tidak terdapat prasyarat verifikasi kelengkapan berkas untuk jalur atau gelombang ini.") %></span>
            </div>
        </div>
<%
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_form_verifikasi_berkas_mahasiswa.jsp:203");
        out.print("<div class=\"alert alert-danger shadow-sm mt-3 rounded-3\"><i class=\"fas fa-circle-xmark me-2\"></i>" + Common.getBahasaConfig("Terjadi kesalahan sistem saat memuat data verifikasi kelengkapan berkas.") + "</div>");
    } finally {
        if (hibSession != null) {
            try { hibSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_form_verifikasi_berkas_mahasiswa.jsp:207");}
            try { hibSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_form_verifikasi_berkas_mahasiswa.jsp:208");}
            try { hibSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_form_verifikasi_berkas_mahasiswa.jsp:209");}
        }
    }
%>