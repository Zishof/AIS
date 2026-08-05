<%@page import="ais.database.model.streaming.AudioPertemuan"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.TreeMap"%>
<%@page import="ais.action.servlet.api.FileHelper"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. Validasi User
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        return;
    }

    String itemParam = request.getParameter("item");
    if (itemParam == null || itemParam.isEmpty()) return;

    // 2. Performance Fix: Buka Session SEKALI di luar loop
    Session sessionmy = StreamingHibernateUtil.getInstance().openSession();

    try {
        String[] itemIds = itemParam.split(",");
        
        for (String itemId : itemIds) {
            if (itemId.trim().isEmpty()) continue;

            // 3. Fetch Data
            AudioPertemuan audio = (AudioPertemuan) sessionmy.createCriteria(AudioPertemuan.class)
                    .add(Restrictions.idEq(Long.parseLong(itemId.trim())))
                    .uniqueResult();
            
            if (audio == null || audio.getId() == null || audio.getPertemuan() == null) continue;

            Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, audio.getPertemuan().toString(), true);
            if (pertemuan == null) continue;

            // 4. Data Preparation
            List<Dosen> dosens = pertemuan.ambilDosen();
            
            String tgl = (pertemuan.getTanggal() != null) 
                    ? SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), null) + ", " + Common.dateFormat6.get().format(pertemuan.getTanggal()) 
                    : "-";
            String wkt = pertemuan.getWaktuMulai() + " - " + pertemuan.getWaktuSelesai();

            // Link & Mime Logic
            String url = audio.createLinkUri();
            String linkAsli = audio.getLink();
            if (linkAsli != null && !linkAsli.isEmpty()) url = linkAsli;
            
            // Note: Kode asli menggunakan getKeterangan() sebagai MimeType
            String mimeType = (audio.getKeterangan() != null) ? audio.getKeterangan() : "";
            FileHelper.IconStyle iconStyle = FileHelper.getIconAndColor(mimeType);
            
            int accessCount = pertemuan.ambilData("audio_" + audio.getId(), null).size();

            // 5. Preview Logic Generator
            String previewHtml = "";
            boolean isHidden = false;

            if (audio.getGdrive() != null && !audio.getGdrive().isEmpty()) {
                previewHtml = "<div class='ratio ratio-16x9 border rounded overflow-hidden'><iframe src='https://drive.google.com/file/d/" + audio.getGdrive() + "/preview' allowfullscreen></iframe></div>";
            } else if (audio.merupakanGambar()) {
                previewHtml = "<img onclick='tampilGambar(this)' src='" + audio.createLinkUri() + "' class='img-fluid rounded border shadow-sm' style='cursor:pointer; max-height:400px; width:100%; object-fit:contain;' alt='" + audio.getNama() + "'>";
            } else if (audio.getNama().toLowerCase().endsWith(".mp3")) {
                // Modern HTML5 Audio Player
                previewHtml = "<div class='bg-light p-3 rounded-3 border d-flex align-items-center'>" +
                              "  <div class='bg-primary text-white rounded-circle p-2 me-3'><i class='fas fa-music'></i></div>" +
                              "  <audio controls class='w-100'><source src='" + url + "' type='audio/mpeg'>Browser Anda tidak mendukung audio.</audio>" +
                              "</div>";
            } else if (linkAsli != null && linkAsli.startsWith("https://drive.google.com/drive/folders")) {
                 try {
                    String folderId = StringUtils.substringBetween(linkAsli, "folders/", "?");
                    if(folderId == null) folderId = StringUtils.substringAfter(linkAsli, "folders/");
                    if(folderId.contains("&")) folderId = StringUtils.split(folderId, "&")[0];
                    previewHtml = "<div class='ratio ratio-16x9 border rounded overflow-hidden'><iframe src='https://drive.google.com/embeddedfolderview?id=" + folderId + "#list'></iframe></div>";
                 } catch(Exception e) { isHidden = true; }
            } else if (url != null && !url.isEmpty()) {
                 // Generic fallback untuk PDF/Dokumen/berkas lain.
                 // BUGFIX: dulu memakai audio.getGdrive() yang PASTI null di cabang ini
                 // (cabang gdrive sudah ditangani di atas) sehingga menghasilkan
                 // iframe .../d/null/preview yang kosong. Kini tampilkan tombol
                 // Buka/Unduh dari URL asli yang benar-benar berfungsi.
                 previewHtml = "<a href='" + url + "' target='_blank' rel='noopener' class='btn btn-outline-primary btn-sm rounded-pill fw-bold'><i class='fas fa-external-link-alt me-1'></i> " + Common.getBahasaConfig("Buka / Unduh Berkas") + "</a>";
            }
            
            if (mimeType.toLowerCase().contains("book")) {
                previewHtml = ""; // Hide logic from original code
            }
%>

    <div class="card border-0 shadow-lg rounded-4 overflow-hidden mb-4 animate__animated animate__fadeInUp" data-id="<%=audio.getId()%>">
        
        <div class="card-header bg-primary text-dark bg-gradient text-white p-3 border-0 d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center overflow-hidden">
                <div class="bg-opacity-25 rounded-circle p-2 me-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width: 45px; height: 45px;">
                    <i class="far fa-file-audio fs-4"></i>
                </div>
                <div>
                    <h5 class="mb-0 fw-bold text-white"><%=audio.getNama() %></h5>
                    <small>Media Pembelajaran Pertemuan ke-<%=pertemuan.getPertemuanKe() %></small>
                </div>
            </div>
            
            <div class="ms-2 d-flex gap-2 flex-shrink-0">
                <% if (pertemuan.bolehUbahAbsenSaja(tbmuser)) { %>
                <button class="btn btn-light btn-sm text-dark fw-bold rounded-pill shadow-sm"
                        onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Kelola Media Pertemuan") %>', '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta_absen&pertemuan=<%=pertemuan.getId()%>&contentModal='+cm, '90%', null, '', cm);">
                    <i class="fas fa-cog me-1"></i> <span class="d-none d-md-inline"><%=Common.getBahasaConfig("Kelola") %></span>
                </button>
                <% } %>
                <a href="<%=url%>" target="_blank" class="btn btn-light btn-sm text-success fw-bold rounded-pill shadow-sm">
                    <i class="fas fa-download me-1"></i> <span class="d-none d-md-inline">Download</span>
                </a>
            </div>
        </div>

        <div class="card-body p-4 bg-light bg-opacity-10">
            <div class="row">
                <div class="col-lg-4 col-md-12 border-end-lg mb-4 mb-lg-0">
                    
                    <div class="mb-3">
                        <span class="badge bg-white text-secondary border shadow-sm rounded-pill mb-1">
                            <i class="fas fa-eye me-1"></i> Diakses: <%=accessCount %> kali
                        </span>
                        <% if(!mimeType.isEmpty()) { %>
                        <span class="badge bg-info bg-opacity-10 text-info border border-info rounded-pill mb-1">
                            <%=mimeType.toUpperCase() %>
                        </span>
                        <% } %>
                    </div>

                    <div class="mb-3">
                        <h6 class="text-uppercase text-muted fw-bold small mb-2">Informasi Kelas</h6>
                        <% if(pertemuan.getPerkuliahan() != null){ %>
                            <div class="fw-bold text-dark"><%=pertemuan.getPerkuliahan().getMatakuliah().getNama() %></div>
                            <div class="small text-muted mb-2"><%=pertemuan.getPerkuliahan().getMatakuliah().getKode() %> • Kelas <%=pertemuan.getPerkuliahan().getKelas() %></div>
                            <div class="d-flex align-items-center text-secondary small bg-white p-2 rounded border">
                                <i class="far fa-clock me-2 text-success"></i> 
                                <div><%=tgl %><br><%=wkt %></div>
                            </div>
                        <% } %>
                    </div>
                    
                    <div>
                        <h6 class="text-uppercase text-muted fw-bold small mb-2">Dosen Pengampu</h6>
                        <div class="d-flex flex-wrap gap-2">
                            <% for(Dosen dosen : dosens){ 
                                String fotoUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen));
                            %>
                            <div class="d-flex align-items-center bg-white p-1 pe-3 rounded-pill border shadow-sm" style="max-width: 100%;">
                                <img src="<%=fotoUrl%>" class="rounded-circle me-2" width="30" height="30" style="object-fit: cover;">
                                <div class="text-truncate">
                                    <small class="d-block fw-bold text-dark lh-1"><%=dosen.getNama()%></small>
                                </div>
                            </div>
                            <% } %>
                        </div>
                    </div>
                </div>

                <div class="col-lg-8 col-md-12 ps-lg-4">
                    <% if(audio.getKeteranganTambahan() != null && !audio.getKeteranganTambahan().isEmpty()) { %>
                        <div class="alert alert-secondary border-0 bg-secondary bg-opacity-10 text-dark mb-3">
                            <i class="fas fa-info-circle me-2"></i> <%=audio.getKeteranganTambahan() %>
                        </div>
                    <% } %>

                    <div class="preview-container">
                        <%=previewHtml %>
                    </div>
                    
                    <% if(previewHtml.isEmpty()) { %>
                         <div class="text-center py-5 bg-white border rounded-4 border-dashed">
                            <i class="fas fa-external-link-alt fa-3x text-muted mb-3 opacity-25"></i>
                            <p class="text-muted mb-3">File ini tidak dapat diputar langsung.</p>
                            <a href="<%=url%>" target="_blank" class="btn btn-outline-success rounded-pill">
                                Buka di Tab Baru
                            </a>
                        </div>
                    <% } %>
                </div>
            </div>
        </div>
    </div>

<%
        } // End Loop
    } finally {
        // 6. Cleanup: Tutup session di finally
        if (sessionmy != null && sessionmy.isOpen()) {
            sessionmy.disconnect();
            sessionmy.close();
        }
        StreamingHibernateUtil.getInstance().closeSession();
    }
%>