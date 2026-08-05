<%@page import="java.net.URLEncoder"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.TreeMap"%>
<%@page import="ais.action.servlet.api.FileHelper"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.file.PertemuanFileContent"%>
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

    // 2. Optimization: Buka Session HANYA SEKALI di luar loop
    Session sessionmy = StreamingHibernateUtil.getInstance().openSession();

    try {
        String[] itemIds = itemParam.split(",");
        
        for (String itemId : itemIds) {
            if (itemId.trim().isEmpty()) continue;

            // 3. Fetch Data
            PertemuanFileContent pfc = (PertemuanFileContent) sessionmy.createCriteria(PertemuanFileContent.class)
                    .add(Restrictions.idEq(Long.parseLong(itemId.trim())))
                    .uniqueResult();

            if (pfc == null || pfc.getId() == null || pfc.getPertemuan() == null) continue;

            Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pfc.getPertemuan().toString(), true);
            if (pertemuan == null) continue;

            // 4. Data Preparation
            List<Dosen> dosens = pertemuan.ambilDosen();
            
            // Format Waktu
            String tgl = (pertemuan.getTanggal() != null) 
                    ? SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), null) + ", " + Common.dateFormat6.get().format(pertemuan.getTanggal()) 
                    : "-";
            String wkt = pertemuan.getWaktuMulai() + " - " + pertemuan.getWaktuSelesai();

            // File & Mime Logic
            String url = pfc.createLinkUri();
            String linkAsli = pfc.getLink();
            if (linkAsli != null && !linkAsli.isEmpty()) url = linkAsli;
            
            String mimeType = (pfc.getFileMimeType() != null) ? pfc.getFileMimeType().toLowerCase() : "";
            String subMime = mimeType;
            if (mimeType.contains("/")) {
                String[] mimeParts = mimeType.split("/");
                if (mimeParts.length > 1) {
                    subMime = mimeParts[1];
                }
            }
            FileHelper.IconStyle iconStyle = FileHelper.getIconAndColor(mimeType);

            // Tentukan Warna Card Berdasarkan Tipe File (UI Candy)
            String headerColorClass = "bg-primary"; // Default Blue
            String iconClass = "fa-file-alt";
            
            if (mimeType.contains("pdf")) { headerColorClass = "bg-danger"; iconClass = "fa-file-pdf"; }
            else if (mimeType.contains("word") || mimeType.contains("doc")) { headerColorClass = "bg-primary"; iconClass = "fa-file-word"; }
            else if (mimeType.contains("sheet") || mimeType.contains("xls")) { headerColorClass = "bg-success"; iconClass = "fa-file-excel"; }
            else if (mimeType.contains("presentation") || mimeType.contains("ppt")) { headerColorClass = "bg-warning text-dark"; iconClass = "fa-file-powerpoint"; }
            else if (mimeType.contains("image")) { headerColorClass = "bg-info text-dark"; iconClass = "fa-file-image"; }
            else if (mimeType.contains("video")) { headerColorClass = "bg-dark"; iconClass = "fa-film"; }

            headerColorClass = "bg-primary";
            
            // 5. Logic Preview Content (Cleaned up)
            boolean showPreview = true;
            String previewHtml = "";

            if (pfc.getGdrive() != null && !pfc.getGdrive().isEmpty()) {
                 previewHtml = "<div class='ratio ratio-16x9'><iframe src='https://drive.google.com/file/d/" + pfc.getGdrive() + "/preview' allowfullscreen></iframe></div>";
            } else if (pfc.merupakanGambar()) {
                previewHtml = "<img onclick='tampilGambar(this)' src='" + pfc.createLinkUri() + "' class='img-fluid rounded shadow-sm border' style='cursor:pointer; max-height:400px; object-fit:contain;' alt='" + pfc.getNama() + "'>";
            } else if (pfc.getNama().toLowerCase().endsWith(".mp3")) {
                previewHtml = "<audio controls class='w-100 mt-2'><source src='" + url + "' type='audio/mpeg'>Browser Anda tidak mendukung audio player.</audio>";
            } else if (linkAsli != null && linkAsli.startsWith("https://drive.google.com/drive/folders")) {
                 // Logic folder view
                 try {
                    String folderId = StringUtils.substringBetween(linkAsli, "folders/", "?");
                    if(folderId == null) folderId = StringUtils.substringAfter(linkAsli, "folders/");
                    if(folderId.contains("&")) { String[] _fp = StringUtils.split(folderId, "&"); if (_fp != null && _fp.length > 0) folderId = _fp[0]; }
                    previewHtml = "<div class='ratio ratio-16x9'><iframe src='https://drive.google.com/embeddedfolderview?id=" + folderId + "#list'></iframe></div>";
                 } catch(Exception e) { showPreview = false; }
            } else if (url != null && (url.contains("docs.google.com") || url.contains("drive.google.com"))) {
                 previewHtml = "<div class='ratio ratio-16x9'><iframe src='" + url + "' allowfullscreen></iframe></div>";
            } else {
                showPreview = false; // Tidak ada preview, hanya tombol download
            }
            
            // Hide preview logic from original code
            if(mimeType.contains("book") || pfc.getLokasiFisik() != null) showPreview = false;
%>

    <div class="card border-0 shadow-lg rounded-4 overflow-hidden mb-4 animate__animated animate__fadeInUp" data-id="<%=pfc.getId()%>">
        
        <div class="card-header <%=headerColorClass%> bg-gradient p-3 border-0 text-white">
            <div class="d-flex align-items-center">
                <div class="bg-opacity-25 rounded-circle p-2 me-3 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                    <i class="fas <%=iconClass%> fs-3"></i>
                </div>
                <div class="overflow-hidden">
                    <h5 class="mb-0 fw-bold text-white" title="<%=pfc.getNama()%>"><%=pfc.getNama() %></h5>
                    <small><i class="fas fa-link me-1"></i> Materi Pertemuan ke-<%=pertemuan.getPertemuanKe() %></small>
                </div>
                <div class="ms-auto d-flex gap-2 flex-shrink-0">
                    <% if (pertemuan.bolehUbahAbsenSaja(tbmuser)) { %>
                    <button class="btn btn-light btn-sm rounded-pill fw-bold shadow-sm text-dark"
                            onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Kelola Materi Pertemuan") %>', '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta_absen&pertemuan=<%=pertemuan.getId()%>&contentModal='+cm, '90%', null, '', cm);">
                        <i class="fas fa-cog me-1"></i> <span class="d-none d-md-inline"><%=Common.getBahasaConfig("Kelola") %></span>
                    </button>
                    <% } %>
                    <a href="<%=url%>" target="_blank" class="btn btn-light btn-sm rounded-pill fw-bold shadow-sm text-dark">
                        <i class="fas fa-download me-1"></i> <span class="d-none d-md-inline">Buka File</span>
                    </a>
                </div>
            </div>
        </div>

        <div class="card-body p-4 bg-light bg-opacity-10">
            <div class="row">
                <div class="col-lg-4 col-md-12 border-end-lg mb-3 mb-lg-0">
                    
                    <div class="d-flex flex-wrap gap-2 mb-3">
                        <span class="badge bg-white text-secondary border shadow-sm">
                            <i class="far fa-eye me-1"></i> Akses: <%=pertemuan.ambilData("bahan_perkulaiahan_" + pfc.getId(), null).size() %>
                        </span>
                        <% if(!mimeType.isEmpty()) { %>
                        <span class="badge bg-white text-secondary border shadow-sm text-uppercase">
                            <%=subMime %>
                        </span>
                        <% } %>
                    </div>

                    <div class="mb-3">
                        <h6 class="text-uppercase text-muted fw-bold small mb-2">Detail Perkuliahan</h6>
                        <% if(pertemuan.getPerkuliahan() != null){ %>
                            <p class="mb-1 small fw-bold text-dark"><%=pertemuan.getPerkuliahan().getMatakuliah().getNama() %></p>
                            <p class="mb-1 small text-muted"><%=pertemuan.getPerkuliahan().getMatakuliah().getKode() %> • Kelas <%=pertemuan.getPerkuliahan().getKelas() %></p>
                            <p class="mb-2 small text-muted"><%=pertemuan.getPerkuliahan().getTahunAjaran() %> (<%=pertemuan.getPerkuliahan().getGanjilGenap() %>)</p>
                        <% } %>
                        <div class="alert alert-light border d-flex align-items-center p-2 rounded-3">
                            <i class="far fa-calendar-alt text-primary me-3 fs-4"></i>
                            <div style="line-height: 1.2;">
                                <small class="d-block fw-bold"><%=tgl %></small>
                                <small class="text-muted"><%=wkt %></small>
                            </div>
                        </div>
                    </div>

                    <div class="mb-3">
                        <h6 class="text-uppercase text-muted fw-bold small mb-2">Dosen Pengampu</h6>
                        <div class="d-flex flex-column gap-2">
                            <% for(Dosen dosen : dosens){ 
                                String fotoUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen));
                            %>
                            <div class="d-flex align-items-center bg-white p-2 rounded-3 border shadow-sm">
                                <img src="<%=fotoUrl%>" class="rounded-circle me-2 object-fit-cover" width="35" height="35">
                                <div>
                                    <div class="small fw-bold text-dark"><%=dosen.getNama()%></div>
                                    <div class="small text-muted" style="font-size: 0.7rem;"><%=dosen.getNidn()%></div>
                                </div>
                            </div>
                            <% } %>
                        </div>
                    </div>
                </div>

                <div class="col-lg-8 col-md-12 ps-lg-4">
                    <% if(pfc.getKeterangan() != null && !pfc.getKeterangan().isEmpty()) { %>
                        <div class="alert alert-info border-0 bg-info bg-opacity-10 text-dark mb-3">
                            <i class="fas fa-info-circle me-2"></i> <%=pfc.getKeterangan() %>
                        </div>
                    <% } %>

                    <div class="bg-white rounded-4 border p-1 shadow-sm">
                        <% if(showPreview) { %>
                            <%=previewHtml %>
                        <% } else { %>
                            <div class="text-center py-5">
                                <i class="fas <%=iconClass%> fa-4x text-muted mb-3 opacity-25"></i>
                                <h6 class="fw-bold text-dark">Pratinjau tidak tersedia</h6>
                                <p class="small text-muted mb-3">File ini tidak dapat dipratinjau langsung di sini.</p>
                                <a href="<%=url%>" target="_blank" class="btn btn-primary rounded-pill px-4 shadow-sm">
                                    <i class="fas fa-download me-2"></i> Download File
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
        // 6. Optimization: Tutup session HANYA SEKALI di block finally
        if (sessionmy != null && sessionmy.isOpen()) {
            sessionmy.disconnect();
            sessionmy.close();
        }
        StreamingHibernateUtil.getInstance().closeSession();
    }
%>