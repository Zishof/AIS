<%@page import="ais.database.model.streaming.VideoPertemuan"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
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
    if (tbmuser == null || tbmuser.getUserId() == null) return;

    String itemParam = request.getParameter("item");
    if (itemParam == null || itemParam.isEmpty()) return;

    // 2. Performance: Buka Session SEKALI di luar loop
    Session sessionmy = StreamingHibernateUtil.getInstance().openSession();

    try {
        String[] itemIds = itemParam.split(",");
        
        for (String itemId : itemIds) {
            if (itemId.trim().isEmpty()) continue;

            // 3. Fetch Data
            VideoPertemuan video = (VideoPertemuan) sessionmy.createCriteria(VideoPertemuan.class)
                    .add(Restrictions.idEq(Long.parseLong(itemId.trim())))
                    .uniqueResult();
            
            if (video == null || video.getId() == null || video.getPertemuan() == null) continue;

            Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, video.getPertemuan().toString(), true);
            if (pertemuan == null) continue;

            // 4. Data Preparation
            List<Dosen> dosens = pertemuan.ambilDosen();
            int aksesCount = pertemuan.ambilData("video_" + video.getId(), null).size();
            
            String tgl = (pertemuan.getTanggal() != null) 
                    ? SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), null) + ", " + Common.dateFormat6.get().format(pertemuan.getTanggal()) 
                    : "-";
            String wkt = pertemuan.getWaktuMulai() + " - " + pertemuan.getWaktuSelesai();

            // Link & Keterangan
            String url = (video.getLink() != null) ? video.getLink() : video.createLinkUri();
            String keteranganMime = (video.getKeterangan() != null) ? video.getKeterangan() : "";

            // 5. Logic Preview Video (Simplified & Modernized)
            String previewHtml = "";
            boolean isDownloadButtonShown = true;

            // Prioritas 1: YouTube (Paling Ringan & Stabil)
            if (video.getYoutube() != null && !video.getYoutube().trim().isEmpty()) {
                previewHtml = "<div class='ratio ratio-16x9 rounded-3 overflow-hidden shadow-sm'>" +
                              "<iframe src='https://www.youtube.com/embed/" + video.getYoutube().trim() + "' title='YouTube video' allowfullscreen></iframe>" +
                              "</div>";
                isDownloadButtonShown = false; // Biasanya youtube tidak perlu tombol download
            } 
            // Prioritas 2: Google Drive Preview
            else if (video.getGdrive() != null && !video.getGdrive().isEmpty()) {
                 previewHtml = "<div class='ratio ratio-16x9 rounded-3 overflow-hidden shadow-sm'>" +
                               "<iframe src='https://drive.google.com/file/d/" + video.getGdrive() + "/preview' allowfullscreen></iframe>" +
                               "</div>";
            }
            // Prioritas 3: MP4 Direct
            else if (video.getNama().toLowerCase().endsWith(".mp4")) {
                previewHtml = "<div class='ratio ratio-16x9 rounded-3 overflow-hidden shadow-sm bg-black'>" +
                              "<video controls class='w-100 h-100'><source src='" + url + "' type='video/mp4'>Browser tidak support video.</video>" +
                              "</div>";
            }
            // Prioritas 4: Folder View
            else if (url.startsWith("https://drive.google.com/drive/folders")) {
                 try {
                    String folderId = StringUtils.substringBetween(url, "folders/", "?");
                    if(folderId == null) folderId = StringUtils.substringAfter(url, "folders/");
                    if(folderId.contains("&")) folderId = StringUtils.split(folderId, "&")[0];
                    previewHtml = "<div class='ratio ratio-16x9 rounded-3 overflow-hidden shadow-sm'><iframe src='https://drive.google.com/embeddedfolderview?id=" + folderId + "#list'></iframe></div>";
                 } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_video.jsp:90");}
            }
            // Fallback: Gambar
            else if (video.merupakanGambar()) {
                 previewHtml = "<img onclick='tampilGambar(this)' src='" + video.createLinkUri() + "' class='img-fluid rounded border shadow-sm w-100' style='cursor:pointer; object-fit:contain;'>";
            }

            // Sembunyikan preview jika tipe buku (logic bawaan)
            if (keteranganMime.toLowerCase().contains("book")) {
                previewHtml = "";
            }
%>

    <div class="card border-0 shadow-lg rounded-4 overflow-hidden mb-4 animate__animated animate__fadeInUp" data-id="<%=video.getId()%>">
        
        <div class="card-header bg-primary text-dark bg-gradient text-white p-3 border-0 d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center overflow-hidden">
                <div class="bg-opacity-25 rounded-circle p-2 me-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width: 45px; height: 45px;">
                    <i class="fas fa-video fs-4"></i>
                </div>
                <div>
                    <h5 class="mb-0 fw-bold text-white"><%=video.getNama() %></h5>
                    <small style="color:black;">Video Pembelajaran Pertemuan ke-<%=pertemuan.getPertemuanKe() %></small>
                </div>
            </div>
            
            <div class="ms-2 d-flex gap-2 flex-shrink-0">
                <% if (pertemuan.bolehUbahAbsenSaja(tbmuser)) { %>
                <button class="btn btn-light btn-sm text-dark fw-bold rounded-pill shadow-sm"
                        onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Kelola Media Pertemuan") %>', '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta_absen&pertemuan=<%=pertemuan.getId()%>&contentModal='+cm, '90%', null, '', cm);">
                    <i class="fas fa-cog me-1"></i> <span class="d-none d-md-inline"><%=Common.getBahasaConfig("Kelola") %></span>
                </button>
                <% } %>
                <% if(isDownloadButtonShown) { %>
                <a href="<%=url%>" target="_blank" class="btn btn-light btn-sm text-danger fw-bold rounded-pill shadow-sm">
                    <i class="fas fa-external-link-alt me-1"></i> Buka
                </a>
                <% } %>
            </div>
        </div>

        <div class="card-body p-4 bg-light bg-opacity-10">
            <div class="row g-4">
                
                <div class="col-lg-5 col-md-12 border-end-lg">
                    <div class="d-flex flex-wrap gap-2 mb-3">
                        <span class="badge bg-white text-secondary border shadow-sm rounded-pill">
                            <i class="fas fa-users me-1"></i> Akses: <%=aksesCount %>
                        </span>
                        <% if(pertemuan.getTopik() != null && !pertemuan.getTopik().isEmpty() && !pertemuan.getTopik().startsWith("Belum")) { %>
                        <span class="badge bg-danger bg-opacity-10 text-danger border border-danger rounded-pill">
                            Topik: <%=pertemuan.getTopik() %>
                        </span>
                        <% } %>
                    </div>

                    <div class="mb-4">
                         <% if(pertemuan.getPerkuliahan() != null){ %>
                            <h6 class="fw-bold text-dark mb-1"><%=pertemuan.getPerkuliahan().getMatakuliah().getNama() %></h6>
                            <p class="small text-muted mb-2"><%=pertemuan.getPerkuliahan().getMatakuliah().getKode() %> • Kelas <%=pertemuan.getPerkuliahan().getKelas() %></p>
                            
                            <div class="d-flex align-items-center text-secondary small bg-white p-2 rounded border">
                                <i class="far fa-clock me-3 fs-5 text-danger opacity-75"></i> 
                                <div style="line-height: 1.2;">
                                    <span class="fw-bold d-block"><%=tgl %></span>
                                    <span><%=wkt %></span>
                                </div>
                            </div>
                        <% } %>
                    </div>

                    <% if(video.getKeteranganTambahan() != null && !video.getKeteranganTambahan().isEmpty()) { %>
                        <div class="alert alert-secondary border-0 bg-secondary bg-opacity-10 text-dark small mb-3">
                            <strong><i class="fas fa-info-circle me-1"></i> Info:</strong> <%=video.getKeteranganTambahan() %>
                        </div>
                    <% } %>

                    <div>
                        <small class="text-uppercase text-muted fw-bold" style="font-size: 0.7rem;">Dosen Pengampu</small>
                        <div class="d-flex flex-wrap gap-2 mt-1">
                            <% for(Dosen dosen : dosens){ 
                                String fotoUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen));
                            %>
                            <div class="d-flex align-items-center bg-white px-2 py-1 rounded-pill border shadow-sm">
                                <img src="<%=fotoUrl%>" class="rounded-circle me-2 object-fit-cover" width="24" height="24">
                                <small class="fw-bold text-dark"><%=dosen.getNama()%></small>
                            </div>
                            <% } %>
                        </div>
                    </div>
                </div>

                <div class="col-lg-7 col-md-12">
                    <div class="bg-white p-1 rounded-4 border shadow-sm">
                        <% if(!previewHtml.isEmpty()) { %>
                            <%=previewHtml %>
                        <% } else { %>
                            <div class="text-center py-5 bg-light rounded-3">
                                <i class="fas fa-video-slash fa-3x text-muted mb-3 opacity-25"></i>
                                <h6 class="fw-bold text-dark">Pratinjau Tidak Tersedia</h6>
                                <p class="small text-muted mb-3">Silakan klik tombol di bawah untuk membuka video.</p>
                                <a href="<%=url%>" target="_blank" class="btn btn-danger rounded-pill px-4 shadow-sm">
                                    <i class="fas fa-play me-2"></i> Tonton Video
                                </a>
                            </div>
                        <% } %>
                    </div>
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