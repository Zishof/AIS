<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String dmsContentH(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String dmsContentStr(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
%>
<%
    if (!Boolean.TRUE.equals(request.getAttribute("DMS_SERVICE_ALLOWED"))) {
        response.sendError(403, "Konten DMS hanya dapat dipanggil melalui servlet Document.");
        return;
    }

    Boolean loggedObjContent = (Boolean) request.getAttribute("DMS_LOGGED_IN");
    boolean loggedContent = loggedObjContent != null && loggedObjContent.booleanValue();
    String modeContent = dmsContentStr(request.getAttribute("DMS_MODE"));
    String akreditasiIdContent = dmsContentStr(request.getAttribute("DMS_AKREDITASI_ID"));
    String indukIdContent = dmsContentStr(request.getAttribute("DMS_INDUK_ID"));
    String keywordContent = dmsContentStr(request.getAttribute("DMS_KEYWORD"));
    String errorMessageContent = dmsContentStr(request.getAttribute("DMS_ERROR_MESSAGE"));
    List entriesContent = (List) request.getAttribute("DMS_ENTRIES");
    List breadcrumbsContent = (List) request.getAttribute("DMS_BREADCRUMBS");
    Integer totalAkreditasiObjContent = (Integer) request.getAttribute("DMS_TOTAL_AKREDITASI");
    Integer totalFolderObjContent = (Integer) request.getAttribute("DMS_TOTAL_FOLDER");
    Integer totalFileObjContent = (Integer) request.getAttribute("DMS_TOTAL_FILE");
    Integer totalLampiranObjContent = (Integer) request.getAttribute("DMS_TOTAL_LAMPIRAN");
    Integer totalItemObjContent = (Integer) request.getAttribute("DMS_TOTAL_ITEM");
    int totalAkreditasiContent = totalAkreditasiObjContent == null ? 0 : totalAkreditasiObjContent.intValue();
    int totalFolderContent = totalFolderObjContent == null ? 0 : totalFolderObjContent.intValue();
    int totalFileContent = totalFileObjContent == null ? 0 : totalFileObjContent.intValue();
    int totalLampiranContent = totalLampiranObjContent == null ? 0 : totalLampiranObjContent.intValue();
    int totalItemContent = totalItemObjContent == null ? 0 : totalItemObjContent.intValue();
%>
<div class="dms-service-result" data-dms-rendered="true" data-mode="<%=dmsContentH(modeContent)%>" data-akreditasi="<%=dmsContentH(akreditasiIdContent)%>" data-induk="<%=dmsContentH(indukIdContent)%>">
    <% if (errorMessageContent != null && errorMessageContent.trim().length() > 0) { %>
        <div class="alert alert-info rounded-4 border-0 mb-3">
            <i class="fa fa-circle-info me-2"></i><%=dmsContentH(errorMessageContent)%>
        </div>
    <% } %>

    <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-center gap-3 mb-3">
        <div class="dms-breadcrumb d-flex flex-wrap gap-2 align-items-center">
            <% if (breadcrumbsContent != null) { for (int i = 0; i < breadcrumbsContent.size(); i++) { Map crumb = (Map) breadcrumbsContent.get(i); %>
                <button type="button" class="btn btn-sm <%= i == breadcrumbsContent.size() - 1 ? "btn-primary" : "btn-outline-primary" %>"
                        data-dms-crumb="true"
                        data-dms-akreditasi="<%=dmsContentH(crumb.get("akreditasiId"))%>"
                        data-dms-induk="<%=dmsContentH(crumb.get("indukId"))%>">
                    <i class="fa <%= i == 0 ? "fa-house" : "fa-folder-open" %> me-1"></i><%=dmsContentH(crumb.get("label"))%>
                </button>
            <% }} %>
        </div>
        <form class="d-flex gap-2" data-dms-search-form="true" data-current-akreditasi="<%=dmsContentH(akreditasiIdContent)%>" data-current-induk="<%=dmsContentH(indukIdContent)%>">
            <input type="text" name="q" class="form-control rounded-pill" placeholder="Cari nama, kode, atau keterangan..." value="<%=dmsContentH(keywordContent)%>">
            <button class="btn btn-dms-gradient rounded-pill px-3" type="submit" title="Cari dokumen"><i class="fa fa-magnifying-glass"></i></button>
        </form>
    </div>

    <div class="row g-3 mb-4">
        <% if ("root".equalsIgnoreCase(modeContent)) { %>
            <div class="col-sm-6 col-lg-3">
                <div class="dms-metric-card">
                    <div class="d-flex justify-content-between align-items-center mb-2"><div class="small text-muted">Ruang Arsip</div><div class="dms-metric-icon"><i class="fa fa-folder-tree"></i></div></div>
                    <div class="h4 fw-bold mb-1"><%=totalAkreditasiContent%></div>
                    <div class="small dms-muted">Jumlah kategori dokumen yang dapat dibuka pengguna.</div>
                </div>
            </div>
            <div class="col-sm-6 col-lg-3">
                <div class="dms-metric-card">
                    <div class="d-flex justify-content-between align-items-center mb-2"><div class="small text-muted">Sumber Data</div><div class="dms-metric-icon"><i class="fa fa-database"></i></div></div>
                    <div class="fw-bold mb-1">Akreditasi</div>
                    <div class="small dms-muted">Kategori utama diambil dari tabel Akreditasi.</div>
                </div>
            </div>
        <% } else { %>
            <div class="col-sm-6 col-lg-3">
                <div class="dms-metric-card">
                    <div class="d-flex justify-content-between align-items-center mb-2"><div class="small text-muted">Sub Ruang</div><div class="dms-metric-icon"><i class="fa fa-folder-open"></i></div></div>
                    <div class="h4 fw-bold mb-1"><%=totalFolderContent%></div>
                    <div class="small dms-muted">Bagian dokumen yang masih memiliki isi di dalamnya.</div>
                </div>
            </div>
            <div class="col-sm-6 col-lg-3">
                <div class="dms-metric-card">
                    <div class="d-flex justify-content-between align-items-center mb-2"><div class="small text-muted">Dokumen</div><div class="dms-metric-icon"><i class="fa fa-file-lines"></i></div></div>
                    <div class="h4 fw-bold mb-1"><%=totalFileContent%></div>
                    <div class="small dms-muted">Item dokumen pada ruang yang sedang dibuka.</div>
                </div>
            </div>
            <div class="col-sm-6 col-lg-3">
                <div class="dms-metric-card">
                    <div class="d-flex justify-content-between align-items-center mb-2"><div class="small text-muted">Lampiran</div><div class="dms-metric-icon"><i class="fa fa-paperclip"></i></div></div>
                    <div class="h4 fw-bold mb-1"><%=totalLampiranContent%></div>
                    <div class="small dms-muted">Dokumen yang sudah memiliki file untuk diunduh setelah login.</div>
                </div>
            </div>
        <% } %>
        <div class="col-sm-6 col-lg-3">
            <div class="dms-metric-card">
                <div class="d-flex justify-content-between align-items-center mb-2"><div class="small text-muted">Status Download</div><div class="dms-metric-icon"><i class="fa <%= loggedContent ? "fa-lock-open" : "fa-lock" %>"></i></div></div>
                <div class="fw-bold mb-1"><%= loggedContent ? "Aktif" : "Disembunyikan" %></div>
                <div class="small dms-muted"><%= loggedContent ? "Tombol download tampil untuk file yang tersedia." : "Login diperlukan agar tombol download muncul." %></div>
            </div>
        </div>
    </div>

    <% if (!loggedContent) { %>
        <div class="alert alert-light border rounded-4 mb-3 d-flex gap-2 align-items-start">
            <i class="fa fa-lock text-primary mt-1"></i>
            <div>
                <div class="fw-bold">Katalog dokumen tetap ditampilkan untuk publik.</div>
                <div class="small text-muted">Tombol download sengaja tidak dirender sampai pengguna berhasil login melalui popup.</div>
            </div>
        </div>
    <% } %>

    <% if (entriesContent == null || entriesContent.isEmpty()) { %>
        <div class="dms-empty">
            <i class="fa fa-folder-open fa-3x mb-3 text-muted"></i>
            <h5 class="fw-bold">Belum ada dokumen yang tampil</h5>
            <p class="mb-0">Data tidak ditemukan pada tabel Akreditasi/DokumenAkreditasi, atau kata kunci pencarian tidak cocok.</p>
        </div>
    <% } else { %>
        <div class="row g-3">
            <% for (int i = 0; i < entriesContent.size(); i++) { Map row = (Map) entriesContent.get(i);
                boolean isAkreditasi = "akreditasi".equalsIgnoreCase(dmsContentStr(row.get("type")));
                boolean canOpen = Boolean.TRUE.equals(row.get("canOpen"));
                boolean hasAttachment = Boolean.TRUE.equals(row.get("hasAttachment"));
                String rowAkreditasiId = isAkreditasi ? dmsContentStr(row.get("id")) : dmsContentStr(row.get("akreditasiId"));
                String rowIndukId = isAkreditasi ? "" : dmsContentStr(row.get("id"));
            %>
                <div class="col-md-6 col-xl-4">
                    <div class="dms-entry">
                        <div class="d-flex gap-3 align-items-start">
                            <div class="dms-entry-icon"><i class="<%=dmsContentH(row.get("iconClass"))%>"></i></div>
                            <div class="flex-grow-1 min-w-0">
                                <div class="d-flex flex-wrap gap-2 align-items-center mb-2">
                                    <span class="badge text-bg-light border rounded-pill px-3 py-2"><%=dmsContentH(row.get("typeLabel"))%></span>
                                    <% if (dmsContentStr(row.get("code")).trim().length() > 0) { %>
                                        <span class="badge text-bg-primary rounded-pill px-3 py-2"><%=dmsContentH(row.get("code"))%></span>
                                    <% } %>
                                </div>
                                <div class="dms-entry-title mb-1"><%=dmsContentH(row.get("name"))%></div>
                                <div class="small text-muted mb-2">
                                    <% if (dmsContentStr(row.get("jenis")).trim().length() > 0) { %><span><%=dmsContentH(row.get("jenis"))%></span><% } %>
                                    <% if (dmsContentStr(row.get("dateLabel")).trim().length() > 0) { %><span class="mx-1">•</span><i class="fa fa-calendar me-1"></i><%=dmsContentH(row.get("dateLabel"))%><% } %>
                                </div>
                                <% if (dmsContentStr(row.get("description")).trim().length() > 0) { %>
                                    <div class="small text-muted mb-3 dms-clamp"><%=dmsContentH(row.get("description"))%></div>
                                <% } %>
                                <div class="d-flex flex-wrap gap-2 align-items-center">
                                    <% if (canOpen) { %>
                                        <button type="button" class="btn btn-sm btn-outline-primary rounded-pill px-3"
                                                data-dms-folder="true"
                                                data-dms-akreditasi="<%=dmsContentH(rowAkreditasiId)%>"
                                                data-dms-induk="<%=dmsContentH(rowIndukId)%>">
                                            <i class="fa fa-folder-open me-1"></i>Buka Ruang
                                        </button>
                                    <% } %>
                                    <% if (loggedContent && hasAttachment) { %>
                                        <a class="btn btn-sm btn-dms-gradient rounded-pill px-3" href="<%=dmsContentH(row.get("downloadUrl"))%>"><i class="fa fa-download me-1"></i>Download</a>
                                    <% } %>
                                    <% if (!hasAttachment && !canOpen) { %>
                                        <span class="badge text-bg-light border rounded-pill px-3 py-2"><i class="fa fa-paperclip me-1"></i>Belum ada lampiran</span>
                                    <% } else { %>
                                        <span class="badge text-bg-light border rounded-pill px-3 py-2"><%=dmsContentH(row.get("sizeLabel"))%></span>
                                    <% } %>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            <% } %>
        </div>
        <% if (totalItemContent >= 300) { %>
            <div class="alert alert-warning rounded-4 mt-3 mb-0"><i class="fa fa-triangle-exclamation me-2"></i>Data dibatasi 300 item agar halaman tetap ringan. Gunakan pencarian untuk mempersempit hasil.</div>
        <% } %>
    <% } %>
</div>
