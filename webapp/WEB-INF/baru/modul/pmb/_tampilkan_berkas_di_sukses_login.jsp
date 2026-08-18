<%@page import="ais.database.model.VerifikasiKelengkapanCalonMahasiswa"%>
<%@page import="ais.database.model.file.FileFotoLain"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.VerifikasiPMBHtmlHelper"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(7);
    String idParam = request.getParameter("id");
    
    boolean isRefresh = "true".equalsIgnoreCase(request.getParameter("refresh"));
    boolean allowEditBiodata = "true".equalsIgnoreCase(request.getParameter("allowEditBiodata")); 
    String denyEditMessage = request.getParameter("denyEditMessage") != null ? request.getParameter("denyEditMessage") : "";
    
    Session hibSession = null;
    List<Map<String, Object>> kelengkapanList = new ArrayList<Map<String, Object>>();

    try {
        hibSession = HibernateUtil.openSession();
        Long camaId = null;

        if (idParam != null && !idParam.trim().isEmpty() && !idParam.equals("null")) {
            try { camaId = Long.parseLong(idParam.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_tampilkan_berkas_di_sukses_login.jsp:29");}
        } 

        if (camaId != null) {
            BiodataCalonMahasiswa cama = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, camaId.toString(), true);
            if (cama != null) {
                List<Object[]> daftarVerifikasi = VerifikasiPMBHtmlHelper.getDaftarVerifikasi(cama, cama.getGelombangPendaftaran(), cama.getJenisSeleksi(), hibSession);
                if (daftarVerifikasi != null) {
                    for (Object[] rowData : daftarVerifikasi) {
                        VerifikasiKelengkapanCalonMahasiswa v = (VerifikasiKelengkapanCalonMahasiswa) rowData[0];
                        ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas ex = (ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas) rowData[1];
                        
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("id", ex != null ? ex.getId() : null);
                        map.put("nama", v.getNama());
                        map.put("verified", ex != null && Boolean.TRUE.equals(ex.getVerified()));
                        map.put("keterangan", ex != null && ex.getKeterangan() != null ? ex.getKeterangan() : "");
                        
                        // AMBIL FORMAT LAMPIRAN DARI ADMIN (TEMPLATE)
                        LampiranLain fileFormat = LampiranLain.ambil(v.getId(), VerifikasiKelengkapanCalonMahasiswa.class.getName());
                        if(fileFormat != null && fileFormat.getId() != null){
                             map.put("nama_lampiran", fileFormat.getNama());
                             map.put("lampiran_url", fileFormat.createLinkUri());
                        }
                        
                        boolean adaFile = false;
                        String urlDownload = "";
                        if (ex != null && ex.getId() != null) {
                            LampiranLain lampiranLain = LampiranLain.ambil(ex.getId(), ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName(), isRefresh);
                            adaFile = lampiranLain != null && lampiranLain.getId() != null;
                            urlDownload = adaFile ? lampiranLain.createLinkUri() : null;
                        }
                        map.put("adaFile", adaFile);
                        map.put("urlDownload", urlDownload);
                        kelengkapanList.add(map);
                    }
                }
            }
        }
%>

<% if (!kelengkapanList.isEmpty()) { %>
<div class="card shadow-sm border-0 rounded-4 mb-4 overflow-hidden" style="border-top: 4px solid #ffc107;">
    <div class="card-header bg-light border-bottom-0 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center">
        <h5 class="fw-bold text-dark mb-0">
            <i class="fas fa-folder-open text-warning me-2 fs-5"></i><%= Common.getBahasaConfig("Daftar Kelengkapan Berkas") %>
        </h5>
        <button type="button" class="btn btn-sm btn-outline-secondary rounded-pill px-3 fw-bold shadow-sm" onclick="window.reloadBerkas(true)">
            <i class="fas fa-arrows-rotate me-1"></i> <%= Common.getBahasaConfig("Perbarui Data") %>
        </button>
    </div>
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light text-secondary">
                    <tr>
                        <th width="5%" class="text-center py-3">#</th>
                        <th width="30%" class="py-3"><%= Common.getBahasaConfig("Nama Kelengkapan Dokumen") %></th>
                        <th width="15%" class="text-center py-3"><%= Common.getBahasaConfig("Status Berkas") %></th>
                        <th width="15%" class="text-center py-3"><%= Common.getBahasaConfig("Status Verifikasi") %></th>
                        <th width="20%" class="py-3"><%= Common.getBahasaConfig("Catatan Pemeriksa") %></th>
                        <th width="15%" class="text-center py-3"><%= Common.getBahasaConfig("Aksi Tindakan") %></th>
                    </tr>
                </thead>
                <tbody>
                    <%
                    int no = 1;
                    for (Map<String, Object> map : kelengkapanList) {
                        String collapseId = "collapse_" + rnd + "_" + no;
                        boolean adaFile = Boolean.TRUE.equals(map.get("adaFile"));
                        boolean verified = Boolean.TRUE.equals(map.get("verified"));
                        String namaBerkas = map.get("nama") != null ? map.get("nama").toString() : "-";
                        String formatUrl = map.get("lampiran_url") != null ? map.get("lampiran_url").toString() : "";
                        String formatNama = map.get("nama_lampiran") != null ? map.get("nama_lampiran").toString() : "";
                        String fileMhsUrl = map.get("urlDownload") != null ? map.get("urlDownload").toString() : "";
                        String idBerkas = map.get("id") != null ? map.get("id").toString() : null;
                        String keterangan = map.get("keterangan") != null ? map.get("keterangan").toString() : "";
                        boolean canUpload = allowEditBiodata && !verified;
                        String berkasId = "up_"+idBerkas;
                    %>
                    <tr>
                        <td class="text-center text-muted fw-bold"><%= no %></td>
                        <td class="fw-semibold text-dark">
                            <div class="mb-1"><%= namaBerkas %></div>
                            <% if (!formatUrl.isEmpty()) { %>
                                <div class="mt-2">
                                    <button type="button" class="btn btn-sm btn-outline-info rounded-pill px-3 shadow-sm py-1" 
                                            onclick="window.bukaPreviewDokumenPMB<%=rnd%>('<%= formatUrl %>', '<%= formatNama %>')" 
                                            title="<%= formatNama %>">
                                        <i class="fas fa-eye me-1"></i> <%= Common.getBahasaConfig("Lihat Format") %>
                                    </button>
                                </div>
                            <% } %>
                        </td>
                        <td class="text-center">
                            <span class="badge rounded-pill <%= adaFile ? "bg-success" : "bg-danger" %> px-3 py-2 shadow-sm">
                                <i class="fas <%= adaFile ? "fa-check" : "fa-xmark" %> me-1"></i>
                                <%= Common.getBahasaConfig(adaFile ? "Telah Diunggah" : "Belum Diunggah") %>
                            </span>
                        </td>
                        <td class="text-center">
                            <span class="badge rounded-pill <%= verified ? "bg-primary" : "bg-secondary" %> px-3 py-2 shadow-sm">
                                <i class="fas <%= verified ? "fa-check-double" : "fa-hourglass-half" %> me-1"></i>
                                <%= Common.getBahasaConfig(verified ? "Valid" : "Menunggu") %>
                            </span>
                        </td>
                        <td class="small">
                            <%= keterangan.trim().isEmpty() ? "<span class='text-muted fst-italic'>" + Common.getBahasaConfig("Tidak ada catatan.") + "</span>" : keterangan %>
                        </td>
                        <td class="text-center px-2">
                            <% if (allowEditBiodata || adaFile) { %>
                                <button class="btn btn-sm <%= adaFile ? "btn-outline-primary" : "btn-primary" %> rounded-pill fw-bold shadow-sm w-100 d-inline-flex align-items-center justify-content-center" 
                                        type="button" data-bs-toggle="collapse" data-bs-target="#<%= collapseId %>">
                                    <i class="fas <%= adaFile ? "fa-eye" : "fa-cloud-arrow-up" %> me-2"></i>
                                    <span><%= Common.getBahasaConfig(adaFile ? "Kelola" : "Unggah") %></span>
                                </button>
                            <% } else { %>
                                <button class="btn btn-sm btn-outline-secondary rounded-pill fw-bold shadow-sm w-100" 
                                        type="button" onclick="window.tampilkanToast('<%= denyEditMessage %>', 'bg-danger text-white fw-bold')">
                                    <i class="fas fa-lock me-2"></i><span><%= Common.getBahasaConfig("Terkunci") %></span>
                                </button>
                            <% } %>
                        </td>
                    </tr>
                    
                    <tr>
                        <td colspan="6" class="p-0 border-0">
                            <div class="collapse" id="<%= collapseId %>">
                                <div class="p-4 bg-white border-top">
                                    <div class="row justify-content-center">
                                        <div class="col-md-10 col-lg-8">
                                            <div class="p-4 border rounded-4 bg-light shadow-sm text-center">
                                                <h6 class="fw-bold text-dark mb-4"><i class="fas fa-file-signature text-primary me-2"></i><%= namaBerkas %></h6>
                                                
                                                <% if (adaFile) { %>
                                                    <button type="button" class="btn btn-sm btn-primary-pmb pmb-btn-preview-dokumen rounded-pill px-4 mb-4 fw-bold shadow-sm d-inline-flex align-items-center justify-content-center"
                                                            style="background-color:var(--theme-primary,#1a4087) !important;background-image:var(--theme-gradient,linear-gradient(90deg,#1a4087 0%,#4682b4 100%)) !important;color:var(--theme-text-on-primary,#ffffff) !important;border:1px solid var(--theme-primary-dark,#102a5c) !important;min-width:210px;opacity:1 !important;visibility:visible !important;"
                                                            title="<%= Common.getBahasaConfig("Pratinjau Berkas Saya") %>"
                                                            aria-label="<%= Common.getBahasaConfig("Pratinjau Berkas Saya") %>"
                                                            onclick="window.bukaPreviewDokumenPMB<%=rnd%>('<%= fileMhsUrl %>', '<%= namaBerkas %>')">
                                                        <i class="fas fa-eye me-2" aria-hidden="true"></i><span><%= Common.getBahasaConfig("Pratinjau Berkas Saya") %></span>
                                                    </button>
                                                <% } %>

                                                <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
                                                    <jsp:param name="clazz" value="<%=ais.database.model.file.LampiranLain.class.getName()%>"/>
                                                    <jsp:param name="jenis" value="<%=ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName()%>"/>
                                                    <jsp:param name="id" value="<%=idBerkas%>"/>
                                                    <jsp:param name="rnd" value="<%=berkasId%>"/>
                                                    <jsp:param name="tampilUpload" value="<%=String.valueOf(canUpload)%>"/>
                                                    <jsp:param name="loadPreview" value="true"/>
                                                    <jsp:param name="afterSave" value="window.reloadBerkas(true)"/>
                                                </jsp:include>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </td>
                    </tr>
                    <% no++; } %>
                </tbody>
            </table>
        </div>
    </div>
</div>
<% } %>

<script>
window.bukaPreviewDokumenPMB<%=rnd%> = function(url, namaFile) {
    if (!url || url === "" || url === "null") {
        if(typeof window.tampilkanToast === 'function') window.tampilkanToast("Dokumen tidak ditemukan.", "bg-danger text-white");
        return;
    }

    const modalId = 'modalPreviewPMB<%=rnd%>';
    const existing = document.getElementById(modalId);
    if (existing) existing.remove();

    const ext = url.split('.').pop().toLowerCase().split('?')[0];
    let contentHtml = '';

    // LOGIKA PENENTUAN TIPE PREVIEW
    if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) {
        contentHtml = '<div class="text-center p-2"><img src="' + url + '" class="img-fluid rounded shadow-sm border" style="max-height: 75vh;"></div>';
    } 
    else if (ext === 'pdf') {
        contentHtml = '<iframe src="' + url + '" style="width: 100%; height: 75vh; border: none;" allowfullscreen></iframe>';
    } 
    else if (['mp4', 'webm', 'mov', 'ogg'].includes(ext)) {
        contentHtml = '<div class="ratio ratio-16x9"><video controls autoplay><source src="' + url + '" type="video/' + (ext==='mov'?'mp4':ext) + '">Browser Anda tidak mendukung preview video.</video></div>';
    } 
    else if (['mp3', 'wav', 'm4a'].includes(ext)) {
        contentHtml = '<div class="d-flex flex-column align-items-center justify-content-center p-5 bg-white rounded">' +
                      '<i class="fas fa-music fa-4x text-info mb-4"></i>' +
                      '<audio controls class="w-100"><source src="' + url + '" type="audio/mpeg">Browser Anda tidak mendukung preview audio.</audio></div>';
    } 
    else {
        // FALLBACK: JIKA TIDAK BOLEH/BISA DI-PREVIEW (Contoh: Docx, Xlsx, Zip)
        contentHtml = 
            '<div class="text-center py-5 px-4">' +
                '<div class="mb-4"><i class="fas fa-file-arrow-down fa-5x text-secondary opacity-25"></i></div>' +
                '<h5 class="fw-bold text-dark"><%= Common.getBahasaConfig("Pratinjau Tidak Tersedia") %></h5>' +
                '<p class="text-muted small mb-4"><%= Common.getBahasaConfig("Format file ini (.[ext]) tidak mendukung pratinjau langsung. Silakan unduh dokumen untuk melihat isinya.") %>'.replace('[ext]', ext) + '</p>' +
                '<a href="' + url + '" target="_blank" class="btn btn-primary rounded-pill px-5 fw-bold shadow-sm">' +
                    '<i class="fas fa-download me-2"></i><%= Common.getBahasaConfig("Unduh File Sekarang") %>' +
                '</a>' +
            '</div>';
    }

    const modalHtml = 
        '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" style="z-index: 2000;">' +
            '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                '<div class="modal-content rounded-4 border-0 shadow-lg">' +
                    '<div class="modal-header bg-dark text-white py-3 border-0">' +
                        '<h6 class="modal-title fw-bold text-truncate pe-4"><i class="fas fa-eye me-2 text-info"></i>' + namaFile + '</h6>' +
                        '<button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                    '</div>' +
                    '<div class="modal-body p-0 bg-secondary bg-opacity-10">' + contentHtml + '</div>' +
                    '<div class="modal-footer bg-light py-2 border-top-0">' +
                        '<a href="' + url + '" target="_blank" class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-bold"><i class="fas fa-up-right-from-square me-1"></i> <%= Common.getBahasaConfig("Buka Jendela Baru") %></a>' +
                        '<button type="button" class="btn btn-sm btn-secondary rounded-pill px-3 fw-bold" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Tutup") %></button>' +
                    '</div>' +
                '</div>' +
            '</div>' +
        '</div>';

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    if (typeof window.pmbEnsureFontAwesome === 'function') { window.pmbEnsureFontAwesome(document.getElementById(modalId)); }
    new bootstrap.Modal(document.getElementById(modalId)).show();
    document.getElementById(modalId).addEventListener('hidden.bs.modal', function(){ this.remove(); });
};
</script>

<%
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_tampilkan_berkas_di_sukses_login.jsp:265");
    } finally {
        if (hibSession != null) {
            try { hibSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_tampilkan_berkas_di_sukses_login.jsp:268");}
            try { hibSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_tampilkan_berkas_di_sukses_login.jsp:269");}
            try { hibSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_tampilkan_berkas_di_sukses_login.jsp:270");}
        }
    }
%>