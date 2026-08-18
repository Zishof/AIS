<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="ais.database.model.JadwalKegiatanKampus" %>
<%@ page import="ais.common.Common" %>

<%
    List<JadwalKegiatanKampus> jadwalList = new ArrayList<JadwalKegiatanKampus>();
    Session mySession = null;
    try {
        mySession = HibernateUtil.getSessionFactory().openSession();
        jadwalList = mySession.createCriteria(JadwalKegiatanKampus.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.desc("tanggalMulai")).list();
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/infokegiatan/index.jsp:21");
    } finally {
        if (mySession != null && mySession.isOpen()) {
            try { mySession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/infokegiatan/index.jsp:24");}
        }
    }

    SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy");
    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
    java.util.Date sekarang = new java.util.Date();
    String ctx = request.getContextPath();

    // Inline SVG snippets for icons not in /img/svg/
    String svgBullhorn =
        "<svg xmlns='http://www.w3.org/2000/svg' width='22' height='22' viewBox='0 0 24 24' fill='currentColor'>" +
        "<path d='M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05" +
        "c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5" +
        " 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z'/></svg>";

    String svgMapPin =
        "<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='#dc3545'>" +
        "<path d='M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" +
        "m0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z'/></svg>";

    String svgBuilding =
        "<svg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='#0d6efd'>" +
        "<path d='M4 2h16v20H4V2zm2 2v16h5v-3h2v3h5V4H6zm2 2h2v2H8V6zm4 0h2v2h-2V6zm4 0h2v2h-2V6z" +
        "M8 10h2v2H8v-2zm4 0h2v2h-2v-2zm4 0h2v2h-2v-2zM8 14h2v2H8v-2zm4 0h2v2h-2v-2zm4 0h2v2h-2v-2z'/></svg>";

    String svgBullseye =
        "<svg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none'" +
        " stroke='#0d6efd' stroke-width='2'>" +
        "<circle cx='12' cy='12' r='10'/><circle cx='12' cy='12' r='6'/>" +
        "<circle cx='12' cy='12' r='2' fill='#0d6efd' stroke='none'/></svg>";

    String svgBroadcast =
        "<svg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='currentColor'" +
        " class='ik-pulse'>" +
        "<path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14c-2.21 0-4-1.79-4-4s1.79-4 4-4" +
        " 4 1.79 4 4-1.79 4-4 4zm0-2c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z'/>" +
        "<path d='M12 6c-3.31 0-6 2.69-6 6 0 1.66.67 3.16 1.76 4.24l1.42-1.42A4 4 0 0 1 8 12c0-2.21 1.79-4 4-4" +
        " s4 1.79 4 4a4 4 0 0 1-1.18 2.82l1.42 1.42A6 6 0 0 0 18 12c0-3.31-2.69-6-6-6z'/></svg>";
%>

<style>
@keyframes ik-pulse-anim { 0%,100%{opacity:1;} 50%{opacity:.45;} }
.ik-pulse { animation: ik-pulse-anim 1.4s ease-in-out infinite; }
.ik-svg-icon { vertical-align: middle; display:inline-block; }
</style>

<div class="container my-5">
    <div class="row mb-5 align-items-center border-bottom pb-4">
        <div class="col-lg-7 col-md-12 mb-3 mb-lg-0">
            <h2 class="fw-bold agenda-header-title d-flex align-items-center gap-3">
                <span class="text-primary fs-3 ik-svg-icon"><%=svgBullhorn%></span>
                <%= Common.getBahasaConfig("Agenda Kampus") %>
            </h2>
            <p class="text-muted mb-0 ms-1" style="font-size: 1.05rem;">
                <%= Common.getBahasaConfig("Informasi terkini terkait jadwal kegiatan akademik dan kemahasiswaan.") %>
            </p>
        </div>
        <div class="col-lg-5 col-md-12">
            <div class="input-group shadow-sm" style="border-radius: 12px; overflow: hidden; border: 1px solid #e0e0e0;">
                <span class="input-group-text bg-white border-0 px-3 text-primary">
                    <img src="<%=ctx%>/img/svg/search.svg" class="ik-svg-icon" width="16" height="16" alt="">
                </span>
                <input type="text" class="form-control border-0 shadow-none py-2"
                    placeholder="<%= Common.getBahasaConfig("Cari nama atau keterangan kegiatan...") %>"
                    id="searchInput">
            </div>
        </div>
    </div>

    <div class="row g-4" id="kegiatanContainer">
        <%
            if (jadwalList != null && !jadwalList.isEmpty()) {
                for (JadwalKegiatanKampus kegiatan : jadwalList) {

                    java.util.Date tanggalMulai  = kegiatan.getTanggalMulai();
                    java.util.Date tanggalSelesai = kegiatan.getTanggalSelesai();

                    String status      = Common.getBahasaConfig("Mendatang");
                    String badgeClass  = "agenda-status-mendatang";
                    String statusIconHtml;

                    if (tanggalMulai != null && tanggalSelesai != null) {
                        if (sekarang.after(tanggalSelesai)) {
                            status        = Common.getBahasaConfig("Selesai");
                            badgeClass    = "agenda-status-selesai";
                            statusIconHtml = "<img src='" + ctx + "/img/svg/check2-circle.svg'" +
                                " class='ik-svg-icon' width='14' height='14' alt=''>";
                        } else if (sekarang.before(tanggalMulai)) {
                            status        = Common.getBahasaConfig("Mendatang");
                            badgeClass    = "agenda-status-mendatang";
                            statusIconHtml = "<img src='" + ctx + "/img/svg/calendar2.svg'" +
                                " class='ik-svg-icon' width='14' height='14' alt=''>";
                        } else {
                            status        = Common.getBahasaConfig("Berlangsung");
                            badgeClass    = "agenda-status-berlangsung";
                            statusIconHtml = svgBroadcast;
                        }
                    } else {
                        statusIconHtml = "<img src='" + ctx + "/img/svg/clock-history.svg'" +
                            " class='ik-svg-icon' width='14' height='14' alt=''>";
                    }

                    String tglMulai   = (tanggalMulai  != null) ? sdfDate.format(tanggalMulai)  : "-";
                    String tglSelesai = (tanggalSelesai != null) ? sdfDate.format(tanggalSelesai) : "-";
                    String wktMulai   = (kegiatan.getWaktuMulai()   != null) ? sdfTime.format(kegiatan.getWaktuMulai())   : "-";
                    String wktSelesai = (kegiatan.getWaktuSelesai() != null) ? sdfTime.format(kegiatan.getWaktuSelesai()) : "-";
        %>
            <div class="col-lg-4 col-md-6 col-sm-12 kegiatan-item">
                <div class="card agenda-event-card position-relative overflow-hidden h-100">

                    <span class="agenda-status-badge <%=badgeClass%> shadow-sm d-flex align-items-center gap-2">
                        <%=statusIconHtml%> <%=status%>
                    </span>

                    <div class="card-body p-4 d-flex flex-column mt-3">
                        <h4 class="agenda-event-title pe-2 mb-4"><%= kegiatan.getNama() != null ? kegiatan.getNama() : "" %></h4>

                        <span class="d-none agenda-hidden-desc"><%= kegiatan.getKeterangan() != null ? kegiatan.getKeterangan() : "" %></span>

                        <div class="d-flex flex-column gap-3 mb-4">
                            <div class="agenda-meta-info m-0">
                                <div class="bg-light rounded-circle d-flex align-items-center justify-content-center me-3"
                                     style="width: 35px; height: 35px; flex-shrink:0;">
                                    <img src="<%=ctx%>/img/svg/calendar-check.svg" class="ik-svg-icon" width="18" height="18" alt="">
                                </div>
                                <div>
                                    <small class="text-muted d-block fw-semibold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Tanggal Pelaksanaan") %></small>
                                    <span class="fw-medium text-dark">
                                        <%=tglMulai%>
                                        <% if (!tglMulai.equals(tglSelesai) && !tglSelesai.equals("-")) { %>
                                            <span class="text-muted mx-1">
                                                <img src="<%=ctx%>/img/svg/arrow-right.svg" class="ik-svg-icon" width="10" height="10" alt="">
                                            </span> <%=tglSelesai%>
                                        <% } %>
                                    </span>
                                </div>
                            </div>

                            <div class="agenda-meta-info m-0">
                                <div class="bg-light rounded-circle d-flex align-items-center justify-content-center me-3"
                                     style="width: 35px; height: 35px; flex-shrink:0;">
                                    <img src="<%=ctx%>/img/svg/clock-history.svg" class="ik-svg-icon" width="18" height="18" alt="">
                                </div>
                                <div>
                                    <small class="text-muted d-block fw-semibold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Waktu Pelaksanaan") %></small>
                                    <span class="fw-medium text-dark"><%=wktMulai%> <%= Common.getBahasaConfig("s/d") %> <%=wktSelesai%> <%= Common.getBahasaConfig("WIB") %></span>
                                </div>
                            </div>

                            <div class="agenda-meta-info m-0">
                                <div class="bg-light rounded-circle d-flex align-items-center justify-content-center me-3"
                                     style="width: 35px; height: 35px; flex-shrink:0;">
                                    <%=svgMapPin%>
                                </div>
                                <div>
                                    <small class="text-muted d-block fw-semibold" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Lokasi Kegiatan") %></small>
                                    <span class="fw-medium text-dark"><%= kegiatan.getTempat() != null ? kegiatan.getTempat() : "-" %></span>
                                </div>
                            </div>
                        </div>

                        <div class="mt-auto"></div>

                        <div class="pt-3 border-top d-flex justify-content-between align-items-center mt-2">
                            <div class="d-flex flex-column">
                                <span class="text-muted" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Penyelenggara") %></span>
                                <span class="fw-semibold text-dark" style="font-size: 0.9rem;">
                                    <%=svgBuilding%>
                                    <%= kegiatan.getPelaksana() != null ? kegiatan.getPelaksana() : Common.getBahasaConfig("Umum") %>
                                </span>
                            </div>
                            <button class="btn btn-primary btn-sm px-3 shadow-sm agenda-btn-detail d-flex align-items-center gap-2"
                                    data-bs-toggle="modal"
                                    data-bs-target="#modalDetail<%=kegiatan.getId()%>">
                                <img src="<%=ctx%>/img/svg/information-circle-outline.svg"
                                     class="ik-svg-icon" width="15" height="15" alt=""
                                     style="filter:brightness(0) invert(1);">
                                <%= Common.getBahasaConfig("Detail Informasi") %>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="modal fade" id="modalDetail<%=kegiatan.getId()%>" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered modal-lg">
                    <div class="modal-content border-0 shadow-lg" style="border-radius: 18px;">
                        <div class="modal-header bg-light border-bottom-0 pb-3" style="border-radius: 18px 18px 0 0;">
                            <h5 class="modal-title fw-bold agenda-header-title d-flex align-items-center gap-2">
                                <img src="<%=ctx%>/img/svg/clipboard-check.svg"
                                     class="ik-svg-icon" width="20" height="20" alt="">
                                <%= Common.getBahasaConfig("Rincian Agenda") %>
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body pt-4 px-4 px-md-5">
                            <h4 class="fw-bold mb-4 text-dark"><%= kegiatan.getNama() != null ? kegiatan.getNama() : "" %></h4>

                            <div class="card bg-light border-0 mb-4" style="border-radius: 12px;">
                                <div class="card-body p-3">
                                    <div class="row text-dark" style="font-size: 0.95rem;">
                                        <div class="col-sm-6 mb-2 mb-sm-0">
                                            <span class="text-muted d-block" style="font-size: 0.8rem;"><%= Common.getBahasaConfig("Kapasitas Peserta") %></span>
                                            <img src="<%=ctx%>/img/svg/users.svg" class="ik-svg-icon me-1" width="15" height="15" alt="">
                                            <span class="fw-semibold"><%= kegiatan.getPeserta() != null ? kegiatan.getPeserta() : "-" %> <%= Common.getBahasaConfig("Orang") %></span>
                                        </div>
                                        <div class="col-sm-6">
                                            <span class="text-muted d-block" style="font-size: 0.8rem;"><%= Common.getBahasaConfig("Penyelenggara") %></span>
                                            <%=svgBullseye%>
                                            <span class="fw-semibold"><%= kegiatan.getPelaksana() != null ? kegiatan.getPelaksana() : Common.getBahasaConfig("Umum") %></span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <h6 class="fw-bold text-muted mb-2 text-uppercase" style="font-size: 0.85rem; letter-spacing: 1px;"><%= Common.getBahasaConfig("Deskripsi Kegiatan") %></h6>
                            <p class="text-dark" style="line-height: 1.8; font-size: 1rem; white-space: pre-wrap;"><%= kegiatan.getKeterangan() != null ? kegiatan.getKeterangan() : Common.getBahasaConfig("Tidak ada deskripsi rinci untuk kegiatan ini.") %></p>

                            <hr class="my-4 text-muted">
                            <div class="text-end text-muted" style="font-size: 0.85rem;">
                                <img src="<%=ctx%>/img/svg/user-circle.svg" class="ik-svg-icon me-1" width="14" height="14" alt="">
                                <%= Common.getBahasaConfig("Dipublikasikan Oleh") %>: <strong><%= kegiatan.getOleh() != null ? kegiatan.getOleh() : Common.getBahasaConfig("Administrator") %></strong>
                            </div>
                        </div>
                        <div class="modal-footer bg-light border-top-0 py-3 px-4 px-md-5" style="border-radius: 0 0 18px 18px;">
                            <button type="button" class="btn btn-secondary px-4 fw-semibold shadow-sm" style="border-radius: 8px;" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Tutup Pengumuman") %></button>
                        </div>
                    </div>
                </div>
            </div>
        <%
                }
            } else {
        %>
            <div class="col-12 text-center py-5 my-5">
                <div class="d-inline-flex align-items-center justify-content-center bg-light rounded-circle mb-4"
                     style="width: 120px; height: 120px;">
                    <img src="<%=ctx%>/img/svg/calendar2.svg" class="ik-svg-icon" width="56" height="56" alt=""
                         style="opacity:0.45;">
                </div>
                <h4 class="fw-bold text-dark"><%= Common.getBahasaConfig("Belum Ada Agenda") %></h4>
                <p class="text-muted"><%= Common.getBahasaConfig("Saat ini tidak ada kegiatan kampus yang dijadwalkan.") %></p>
            </div>
        <% } %>
    </div>
</div>

<script>
    document.addEventListener("DOMContentLoaded", function() {
        var searchInput = document.getElementById("searchInput");
        if (searchInput) {
            searchInput.addEventListener("keyup", function() {
                var filter = searchInput.value.toLowerCase();
                var cards = document.querySelectorAll(".kegiatan-item");
                for (var i = 0; i < cards.length; i++) {
                    var titleEl = cards[i].querySelector(".agenda-event-title");
                    var descEl  = cards[i].querySelector(".agenda-hidden-desc");
                    var title   = titleEl ? titleEl.innerText.toLowerCase() : "";
                    var desc    = descEl  ? descEl.innerText.toLowerCase()  : "";
                    cards[i].style.display = (title.indexOf(filter) >= 0 || desc.indexOf(filter) >= 0) ? "" : "none";
                }
            });
        }
    });
</script>
