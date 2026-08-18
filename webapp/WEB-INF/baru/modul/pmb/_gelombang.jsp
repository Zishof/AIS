<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Comparator"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GelombangPendaftaran"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    /** Escape teks user agar aman di HTML (XSS prevention). */
    private static String vbGel(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>

<style>
/* ── PMB Gelombang Card ──────────────────────────────────────── */
.pmb-gl-card {
    border: none;
    border-radius: 1.25rem;
    transition: transform .2s ease, box-shadow .2s ease;
    position: relative;
}
.pmb-gl-card:hover { transform: translateY(-3px); }

.pmb-gl-open  { box-shadow: -5px 0 0 #0d6efd, 0 6px 24px rgba(13,110,253,.13); }
.pmb-gl-open:hover { box-shadow: -5px 0 0 #0d6efd, 0 12px 32px rgba(13,110,253,.22); }
.pmb-gl-closed { box-shadow: 0 2px 10px rgba(0,0,0,.07); opacity: .85; }
.pmb-gl-closed:hover { box-shadow: 0 6px 20px rgba(0,0,0,.12); }

/* Garis tipis di paling atas kartu */
.pmb-gl-ribbon {
    position: absolute; top: 0; left: 0; right: 0;
    height: 4px; border-radius: 1.25rem 1.25rem 0 0;
}
.pmb-ribbon-open   { background: linear-gradient(90deg, #0d6efd, #6610f2); }
.pmb-ribbon-closed { background: linear-gradient(90deg, #adb5bd, #6c757d); }

/* Badge prodi */
.pmb-prodi-badge {
    font-size: .72rem; font-weight: 600; letter-spacing: .01em;
    padding: .3rem .65rem; cursor: default;
}
.pmb-prodi-more {
    font-size: .72rem; font-weight: 700; cursor: pointer;
    background: rgba(108,117,125,.13); color: #495057;
    border: 1px solid rgba(108,117,125,.3); border-radius: 50rem;
    padding: .3rem .65rem; transition: background .15s;
}
.pmb-prodi-more:hover { background: rgba(13,110,253,.12); color: #0d6efd; border-color: rgba(13,110,253,.3); }

/* Kotak deadline */
.pmb-dl-box {
    border-radius: .75rem; padding: .55rem .75rem;
    min-width: 0;
}

/* Kolom aksi kanan */
.pmb-action-col {
    flex-shrink: 0; width: 200px;
    display: flex !important; flex-direction: column;
    align-items: center; justify-content: center;
    text-align: center; gap: .55rem;
    padding-left: 1.5rem;
    border-left: 1px dashed rgba(13,110,253,.22);
}
@media (max-width: 991.98px) {
    .pmb-action-col {
        width: 100% !important; padding-left: 0;
        border-left: none;
        border-top: 1px dashed rgba(13,110,253,.18);
        padding-top: 1.25rem;
    }
}

/* Tombol CTA */
.btn-pmb-cta {
    background: linear-gradient(135deg, #0d6efd 0%, #6610f2 100%);
    border: none; color: #fff !important;
    border-radius: 50rem; font-weight: 700; font-size: .88rem;
    padding: .55rem 1.4rem;
    box-shadow: 0 4px 14px rgba(13,110,253,.35);
    transition: transform .2s ease, box-shadow .2s ease;
    white-space: nowrap; cursor: pointer;
}
.btn-pmb-cta:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 22px rgba(13,110,253,.55);
    color: #fff !important;
}
.btn-pmb-cta:disabled {
    background: linear-gradient(135deg, #adb5bd, #6c757d);
    box-shadow: none; opacity: .72; cursor: not-allowed;
}

/* Status dot */
.pmb-dot {
    display: inline-block; width: 8px; height: 8px;
    border-radius: 50%; margin-right: 4px; vertical-align: middle;
}
.pmb-dot-open   { background: #198754; animation: pmbDotPulse 1.6s ease-in-out infinite; }
.pmb-dot-closed { background: #adb5bd; }
@keyframes pmbDotPulse { 0%,100%{opacity:1} 50%{opacity:.25} }

/* Countdown chip */
.pmb-chip {
    font-size: .7rem; font-weight: 700;
    border-radius: .5rem; padding: .22rem .6rem;
    display: inline-block;
}

/* Empty state */
.pmb-empty {
    background: #fff; border: 1px solid rgba(0,0,0,.07);
    border-radius: 1.25rem;
}

/* Pagination override */
.pmb-page-link {
    border: none; font-weight: 700; padding: .45rem .85rem;
    transition: background .15s, color .15s;
}
.pmb-page-link:not(.active-page):hover { background: rgba(13,110,253,.08); }
.pmb-page-link.active-page { background: #0d6efd !important; color: #fff !important; }
</style>

<%
    /* ── Parameter ──────────────────────────────────────────────────────── */
    String ta           = request.getParameter("ta");
    String seleksiParam = request.getParameter("seleksi");
    String statusFilter = request.getParameter("status");
    String rnd          = request.getParameter("rnd");
    String pageStr      = request.getParameter("page");

    if (rnd == null) rnd = "";

    int currentPage = 1;
    try {
        if (pageStr != null && !pageStr.trim().isEmpty())
            currentPage = Integer.parseInt(pageStr.trim());
    } catch (Exception ePageParse) { ais.common.ErrorAuditUtil.record(ePageParse, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:154"); /* tetap 1 */ }
    if (currentPage < 1) currentPage = 1;

    final int    LIMIT     = 10;
    final long   NOW       = System.currentTimeMillis();
    final long   MS_DAY    = 86400000L;

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();

        /* ── 1. Ambil semua gelombang aktif ─────────────────────────────── */
        List<GelombangPendaftaran> rawList = new ArrayList<GelombangPendaftaran>();
        try {
            Criteria cr = sess.createCriteria(GelombangPendaftaran.class)
                    .add(Restrictions.eq("aktif", true));
            if (ta != null && !ta.trim().isEmpty())
                cr.add(Restrictions.eq("tahunAkademik", ta.trim()));
            if (seleksiParam != null && !seleksiParam.trim().isEmpty()) {
                try {
                    long seleksiId = Long.parseLong(seleksiParam.trim());
                    cr.add(Restrictions.eq("jenisSeleksi.id", seleksiId));
                } catch (NumberFormatException eSelId) { ais.common.ErrorAuditUtil.record(eSelId, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:176"); /* id tidak valid, lewati */ }
            }
            List<GelombangPendaftaran> tmp = ConstantValues.simpleList(cr, GelombangPendaftaran.class);
            if (tmp != null) rawList = tmp;
        } catch (Exception eQuery) {
            eQuery.printStackTrace(); ais.common.ErrorAuditUtil.record(eQuery, "auto-audit webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:181");
        }

        /* ── 2. Filter status ───────────────────────────────────────────── */
        List<GelombangPendaftaran> filteredList = new ArrayList<GelombangPendaftaran>();
        boolean rawEmpty = rawList.isEmpty();
        for (GelombangPendaftaran g : rawList) {
            boolean gBuka = false;
            if (g.getMulai() != null && g.getSampai() != null) {
                long selesai = g.getSampai().getTime() + MS_DAY - 1L;
                gBuka = NOW >= g.getMulai().getTime() && NOW <= selesai;
            }
            if ("buka".equalsIgnoreCase(statusFilter)  && !gBuka) continue;
            if ("tutup".equalsIgnoreCase(statusFilter) &&  gBuka) continue;
            filteredList.add(g);
        }

        /* ── 3. Urutkan: buka dulu → sampai DESC → mulai DESC ──────────── */
        Collections.sort(filteredList, new Comparator<GelombangPendaftaran>() {
            @Override
            public int compare(GelombangPendaftaran a, GelombangPendaftaran b) {
                boolean ba = a.getMulai() != null && a.getSampai() != null
                        && NOW >= a.getMulai().getTime()
                        && NOW <= a.getSampai().getTime() + MS_DAY - 1L;
                boolean bb = b.getMulai() != null && b.getSampai() != null
                        && NOW >= b.getMulai().getTime()
                        && NOW <= b.getSampai().getTime() + MS_DAY - 1L;
                if (ba != bb) return ba ? -1 : 1;
                if (a.getSampai() != null && b.getSampai() != null) {
                    int c = b.getSampai().compareTo(a.getSampai());
                    if (c != 0) return c;
                } else if (a.getSampai() != null) return -1;
                  else if (b.getSampai() != null) return  1;
                if (a.getMulai() != null && b.getMulai() != null)
                    return b.getMulai().compareTo(a.getMulai());
                return 0;
            }
        });

        /* ── 4. Paginasi ────────────────────────────────────────────────── */
        int totalData = filteredList.size();

        if (totalData == 0) {
            if (rawEmpty) {
%>
<div class="col-12">
    <div class="pmb-empty text-center py-5 px-3 shadow-sm">
        <i class="fas fa-folder-open fa-3x text-muted mb-3 opacity-50 d-block"></i>
        <h5 class="text-muted fw-bold mb-1"><%= Common.getBahasaConfig("Tidak Terdapat Data Pendaftaran") %></h5>
        <p class="text-secondary small mb-0"><%= Common.getBahasaConfig("Saat ini belum ada gelombang pendaftaran yang tersedia.") %></p>
    </div>
</div>
<%
            } else {
%>
<div class="col-12">
    <div class="pmb-empty text-center py-5 px-3 shadow-sm">
        <i class="fas fa-filter fa-3x text-muted mb-3 opacity-50 d-block"></i>
        <h5 class="text-muted fw-bold mb-1"><%= Common.getBahasaConfig("Data Tidak Ditemukan") %></h5>
        <p class="text-secondary small mb-0"><%= Common.getBahasaConfig("Tidak terdapat gelombang yang sesuai dengan filter Anda.") %></p>
    </div>
</div>
<%
            }
        } else {
            int totalPages = (int) Math.ceil((double) totalData / LIMIT);
            if (currentPage > totalPages) currentPage = totalPages;
            int startIdx = (currentPage - 1) * LIMIT;
            int endIdx   = Math.min(startIdx + LIMIT, totalData);
            List<GelombangPendaftaran> pagedList = filteredList.subList(startIdx, endIdx);

            /* ── 5. BATCH QUERY PRODI (hindari N+1) ──────────────────────── */
            Map<Long, List<String>> prodiMap = new LinkedHashMap<Long, List<String>>();
            List<Long> gelIds = new ArrayList<Long>();
            for (GelombangPendaftaran g : pagedList) {
                if (g.getId() != null) gelIds.add(g.getId());
            }

            if (!gelIds.isEmpty()) {
                /* Query prodi dari paket yang terkait gelombang — satu round-trip */
                try {
                    List<Object[]> batchRows = sess.createQuery(
                        "SELECT ppg.gelombangPendaftaran.id, pj.jurusan.nama " +
                        "FROM PaketJurusanPmb pj JOIN pj.paket p, " +
                        "PaketPunyaGelombangPendaftaran ppg " +
                        "WHERE ppg.paket = p " +
                        "AND p.aktif = true " +
                        "AND (pj.jurusan.aktif is null OR pj.jurusan.aktif = true) " +
                        "AND ppg.gelombangPendaftaran.id IN (:ids) " +
                        "ORDER BY ppg.gelombangPendaftaran.id, pj.jurusan.nama"
                    ).setParameterList("ids", gelIds).list();

                    for (int ri = 0; ri < batchRows.size(); ri++) {
                        Object[] row = batchRows.get(ri);
                        Long   gId  = (Long)   row[0];
                        String nm   = (String) row[1];
                        if (!prodiMap.containsKey(gId))
                            prodiMap.put(gId, new ArrayList<String>());
                        List<String> lst = prodiMap.get(gId);
                        if (!lst.contains(nm)) lst.add(nm);
                    }
                } catch (Exception eBatch) { ais.common.ErrorAuditUtil.record(eBatch, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:282");
                    /* batch gagal — fallback: prodiMap kosong, ditangani di bawah */
                }

                /* Gelombang tanpa paket spesifik → tampilkan semua prodi aktif (query sekali) */
                List<String> allProdiAktif = null;
                for (GelombangPendaftaran g : pagedList) {
                    Long gId = g.getId();
                    if (gId != null && !prodiMap.containsKey(gId)) {
                        if (allProdiAktif == null) {
                            try {
                                allProdiAktif = sess.createQuery(
                                    "SELECT j.nama FROM Jurusan j " +
                                    "WHERE (j.aktif is null OR j.aktif = true) " +
                                    "ORDER BY j.nama"
                                ).list();
                                if (allProdiAktif == null) allProdiAktif = new ArrayList<String>();
                            } catch (Exception eAllProdi) {
                                allProdiAktif = new ArrayList<String>();
                            }
                        }
                        prodiMap.put(gId, allProdiAktif);
                    }
                }
            }

            /* ── 6. Render kartu per gelombang ──────────────────────────── */
            final int MAX_PRODI = 4;

            for (GelombangPendaftaran g : pagedList) {

                /* -- Teks & status ---------------------------------------- */
                String gNama    = vbGel(g.getNama()        != null ? g.getNama()                    : "-");
                String gTa      = vbGel(g.getTahunAkademik()!= null ? g.getTahunAkademik()           : "-");
                String gSeleksi = vbGel(g.getJenisSeleksi()!= null ? g.getJenisSeleksi().getNama()  : "-");
                String gInfo    = (g.getInfo() != null && !g.getInfo().trim().isEmpty())
                                  ? vbGel(g.getInfo()) : null;

                /* -- Tanggal ----------------------------------------------- */
                String tglMulai  = g.getMulai()  != null ? Common.dateFormat41.get().format(g.getMulai())  : "-";
                String tglSampai = g.getSampai() != null ? Common.dateFormat41.get().format(g.getSampai()) : "-";
                String tglUpload = "-", tglLogin = "-", tglTagReg = "-", tglTagDU = "-";
                try { if (g.getTanggalDaftarUlangBerakhir()       != null) tglUpload = Common.dateFormat41.get().format(g.getTanggalDaftarUlangBerakhir()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:324");}
                try { if (g.getTanggalLoginCalonMahasiswaBerakhir()!= null) tglLogin  = Common.dateFormat41.get().format(g.getTanggalLoginCalonMahasiswaBerakhir()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:325");}
                try { if (g.getTanggalTagihanRegistrasi()          != null) tglTagReg = Common.dateFormat41.get().format(g.getTanggalTagihanRegistrasi()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:326");}
                try { if (g.getTanggalTagihanDaftarUlang()         != null) tglTagDU  = Common.dateFormat41.get().format(g.getTanggalTagihanDaftarUlang()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:327");}

                /* -- Hitung status buka ------------------------------------ */
                boolean isBuka = false;
                if (g.getMulai() != null && g.getSampai() != null) {
                    long selesai = g.getSampai().getTime() + MS_DAY - 1L;
                    isBuka = NOW >= g.getMulai().getTime() && NOW <= selesai;
                }

                /* -- Sisa hari --------------------------------------------- */
                long daysLeft = -1L;
                if (isBuka && g.getSampai() != null) {
                    daysLeft = (g.getSampai().getTime() + MS_DAY - NOW) / MS_DAY;
                    if (daysLeft < 0L) daysLeft = 0L;
                }

                /* -- CSS & label ------------------------------------------- */
                String cardCls     = isBuka ? "pmb-gl-open"       : "pmb-gl-closed";
                String ribbonCls   = isBuka ? "pmb-ribbon-open"   : "pmb-ribbon-closed";
                String statusText  = isBuka ? Common.getBahasaConfig("Sedang Dibuka")  : Common.getBahasaConfig("Telah Ditutup");
                String statusBdg   = isBuka ? "bg-success" : "bg-danger";
                String dotCls      = isBuka ? "pmb-dot-open" : "pmb-dot-closed";
                String disabledStr = isBuka ? "" : "disabled";
                String btnLabel    = isBuka
                    ? "<i class='fas fa-pen-to-square me-1'></i>" + Common.getBahasaConfig("Mulai Pendaftaran")
                    : "<i class='fas fa-lock me-1'></i>" + Common.getBahasaConfig("Pendaftaran Ditutup");
                String iconCls  = isBuka ? "fas fa-user-pen text-primary" : "fas fa-lock text-secondary";
                String iconBg   = isBuka ? "rgba(13,110,253,.1)"   : "rgba(108,117,125,.1)";
                String iconBdr  = isBuka ? "rgba(13,110,253,.28)"  : "rgba(108,117,125,.22)";
                String actionTxtCls = isBuka ? "text-success" : "text-secondary";

                /* -- Prodi ------------------------------------------------- */
                String gelIdStr = g.getId() != null ? String.valueOf(g.getId()) : "0";
                List<String> listProdi = prodiMap.containsKey(g.getId())
                        ? prodiMap.get(g.getId())
                        : new ArrayList<String>();
                int totalProdi   = listProdi.size();
                int prodiHidden  = Math.max(0, totalProdi - MAX_PRODI);
                int prodiVisible = Math.min(MAX_PRODI, totalProdi);
%>
<div class="col-12 animate__animated animate__fadeInUp">
    <div class="card pmb-gl-card <%= cardCls %> mb-3 overflow-hidden">

        <%-- ribbon warna di atas kartu --%>
        <div class="<%= ribbonCls %> pmb-gl-ribbon"></div>

        <div class="card-body p-3 p-md-4 d-flex flex-column flex-lg-row align-items-lg-center gap-3 gap-lg-0" style="padding-top:1.25rem !important;">

            <%-- ══ Kolom info kiri ══════════════════════════════════════ --%>
            <div class="flex-grow-1 pe-lg-4" style="min-width:0;">

                <%-- Badge baris atas --%>
                <div class="d-flex flex-wrap align-items-center gap-2 mb-2">
                    <span class="badge rounded-pill bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 small fw-semibold">
                        <i class="fas fa-graduation-cap me-1"></i>TA <%= gTa %>
                    </span>
                    <span class="badge <%= statusBdg %> rounded-pill px-3 small">
                        <span class="pmb-dot <%= dotCls %>"></span><%= statusText %>
                    </span>
                    <% if (isBuka && daysLeft >= 0L) {
                        String chipCls = daysLeft <= 3L
                            ? "bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25"
                            : "bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25";
                        String daysTeks = daysLeft == 0L
                            ? Common.getBahasaConfig("Hari Terakhir!")
                            : daysLeft + " " + Common.getBahasaConfig("hari lagi");
                    %>
                    <span class="pmb-chip <%= chipCls %>">
                        <i class="fas fa-hourglass-half me-1"></i><%= daysTeks %>
                    </span>
                    <% } %>
                </div>

                <%-- Judul gelombang --%>
                <h5 class="fw-bold text-dark mb-1 lh-sm"><%= gNama %></h5>
                <div class="text-secondary small fw-semibold mb-3">
                    <i class="fas fa-tags me-1 text-warning"></i><%= gSeleksi %>
                </div>

                <%-- Program Studi tersedia --%>
                <div class="mb-3">
                    <div class="small text-muted fw-bold mb-2">
                        <i class="fas fa-building-columns me-1 text-info"></i><%= Common.getBahasaConfig("Program Studi") %>
                        <% if (totalProdi > 0) { %>
                        <span class="badge bg-secondary bg-opacity-10 text-secondary rounded-pill ms-1 fw-normal"><%= totalProdi %></span>
                        <% } %>
                    </div>
                    <div class="d-flex flex-wrap gap-1 align-items-center">
                        <% if (totalProdi == 0) { %>
                        <span class="text-muted small fst-italic"><%= Common.getBahasaConfig("Belum dikonfigurasi") %></span>
                        <% } else {
                            for (int pi = 0; pi < prodiVisible; pi++) { %>
                            <span class="badge rounded-pill bg-info bg-opacity-10 text-info border border-info border-opacity-25 pmb-prodi-badge">
                                <%= vbGel(listProdi.get(pi)) %>
                            </span>
                        <%  }
                            if (prodiHidden > 0) {
                                /* Badge tersembunyi — ditampilkan saat klik "+N lainnya" */
                                for (int pi2 = MAX_PRODI; pi2 < totalProdi; pi2++) { %>
                            <span class="badge rounded-pill bg-info bg-opacity-10 text-info border border-info border-opacity-25 pmb-prodi-badge"
                                  id="pmb-ph-<%= gelIdStr %>-<%= pi2 %>" style="display:none">
                                <%= vbGel(listProdi.get(pi2)) %>
                            </span>
                        <%          }
                                /* Tombol "+N lainnya" — inline onclick, tidak perlu fungsi global */
                                String showMoreOncl = "var i,n=" + totalProdi + ";for(i=" + MAX_PRODI + ";i<n;i++){var x=document.getElementById('pmb-ph-" + gelIdStr + "-'+i);if(x)x.style.display='';}this.style.display='none';";
                        %>
                            <span class="pmb-prodi-more" onclick="<%= showMoreOncl %>">
                                +<%= prodiHidden %> <%= Common.getBahasaConfig("lainnya") %>
                            </span>
                        <%      }
                           }
                        %>
                    </div>
                </div>

                <%-- Periode pendaftaran --%>
                <div class="d-flex flex-wrap gap-2 mb-3 small">
                    <span class="d-inline-flex align-items-center bg-light border rounded-3 px-3 py-2 shadow-sm text-nowrap">
                        <i class="far fa-calendar-days text-primary me-2"></i>
                        <strong class="me-1"><%= Common.getBahasaConfig("Mulai") %>:</strong><%= tglMulai %>
                    </span>
                    <span class="d-inline-flex align-items-center bg-light border rounded-3 px-3 py-2 shadow-sm text-nowrap">
                        <i class="far fa-calendar-check text-danger me-2"></i>
                        <strong class="me-1"><%= Common.getBahasaConfig("Berakhir") %>:</strong><%= tglSampai %>
                    </span>
                </div>

                <%-- Grid deadline --%>
                <div class="border-top pt-3">
                    <div class="small text-muted fw-bold mb-2">
                        <i class="fas fa-hourglass-half text-danger me-1"></i><%= Common.getBahasaConfig("Batas Waktu & Deadline") %>
                    </div>
                    <div class="row g-2">
                        <div class="col-6 col-md-3">
                            <div class="pmb-dl-box bg-success bg-opacity-10 border border-success border-opacity-25 h-100">
                                <div class="small text-success fw-bold text-truncate mb-1"
                                     title="<%= Common.getBahasaConfig("Batas Upload Berkas") %>">
                                    <i class="fas fa-file-arrow-up me-1"></i><%= Common.getBahasaConfig("Upload Berkas") %>
                                </div>
                                <div class="fw-bold text-dark small"><%= tglUpload %></div>
                            </div>
                        </div>
                        <div class="col-6 col-md-3">
                            <div class="pmb-dl-box bg-danger bg-opacity-10 border border-danger border-opacity-25 h-100">
                                <div class="small text-danger fw-bold text-truncate mb-1"
                                     title="<%= Common.getBahasaConfig("Batas Akhir Login") %>">
                                    <i class="fas fa-right-to-bracket me-1"></i><%= Common.getBahasaConfig("Akhir Login") %>
                                </div>
                                <div class="fw-bold text-dark small"><%= tglLogin %></div>
                            </div>
                        </div>
                        <div class="col-6 col-md-3">
                            <div class="pmb-dl-box bg-warning bg-opacity-10 border border-warning border-opacity-50 h-100">
                                <div class="small text-warning fw-bold text-truncate mb-1"
                                     title="<%= Common.getBahasaConfig("Tagihan Pendaftaran") %>">
                                    <i class="fas fa-file-invoice-dollar me-1"></i><%= Common.getBahasaConfig("Tagihan Daftar") %>
                                </div>
                                <div class="fw-bold text-dark small"><%= tglTagReg %></div>
                            </div>
                        </div>
                        <div class="col-6 col-md-3">
                            <div class="pmb-dl-box bg-warning bg-opacity-10 border border-warning border-opacity-50 h-100">
                                <div class="small text-warning fw-bold text-truncate mb-1"
                                     title="<%= Common.getBahasaConfig("Tagihan Daftar Ulang") %>">
                                    <i class="fas fa-file-invoice-dollar me-1"></i><%= Common.getBahasaConfig("Daftar Ulang") %>
                                </div>
                                <div class="fw-bold text-dark small"><%= tglTagDU %></div>
                            </div>
                        </div>
                    </div>
                </div>

                <%-- Info tambahan (opsional) --%>
                <% if (gInfo != null) { %>
                <div class="alert alert-info border-info border-opacity-25 bg-info bg-opacity-10 small text-dark mt-3 mb-0 py-2 px-3 rounded-3 shadow-sm">
                    <i class="fas fa-circle-info me-2 text-info"></i><%= gInfo %>
                </div>
                <% } %>

            </div><%-- /kolom kiri --%>

            <%-- ══ Kolom aksi kanan ══════════════════════════════════════ --%>
            <div class="pmb-action-col">
                <div class="rounded-circle d-inline-flex align-items-center justify-content-center mb-1"
                     style="width:52px;height:52px;background:<%= iconBg %>;border:1.5px solid <%= iconBdr %>;">
                    <i class="<%= iconCls %>" style="font-size:1.2rem;"></i>
                </div>
                <button class="btn btn-pmb-cta" <%= disabledStr %>
                        onclick="daftarGelombang<%= rnd %>('<%= gelIdStr %>')">
                    <%= btnLabel %>
                </button>
                <div class="small fw-semibold <%= actionTxtCls %>">
                    <span class="pmb-dot <%= dotCls %>"></span><%= statusText %>
                </div>
            </div>

        </div><%-- /card-body --%>
    </div><%-- /card --%>
</div>
<%
            } /* end for each gelombang */

            /* ── 7. Paginasi ─────────────────────────────────────────────── */
            if (totalPages > 1) {
%>
<div class="col-12 mt-2 d-flex justify-content-center">
    <nav aria-label="<%= Common.getBahasaConfig("Navigasi Halaman") %>">
        <ul class="pagination pagination-sm mb-0 bg-white shadow-sm border rounded-pill overflow-hidden">
            <li class="page-item <%= (currentPage == 1) ? "disabled" : "" %>">
                <button class="page-link pmb-page-link" aria-label="<%= Common.getBahasaConfig("Sebelumnya") %>"
                        onclick="loadGelombang<%= rnd %>(<%= currentPage - 1 %>)">
                    <i class="fas fa-chevron-left small"></i>
                </button>
            </li>
            <%
                for (int pi = 1; pi <= totalPages; pi++) {
                    boolean showPage     = pi == 1 || pi == totalPages
                            || (pi >= currentPage - 2 && pi <= currentPage + 2);
                    boolean showEllipsis = !showPage
                            && (pi == currentPage - 3 || pi == currentPage + 3);
                    if (!showPage && !showEllipsis) continue;
            %>
            <% if (showEllipsis) { %>
            <li class="page-item disabled">
                <span class="page-link pmb-page-link text-muted">…</span>
            </li>
            <% } else { %>
            <li class="page-item">
                <button class="page-link pmb-page-link <%= (currentPage == pi) ? "active-page" : "text-secondary" %>"
                        onclick="loadGelombang<%= rnd %>(<%= pi %>)"><%= pi %></button>
            </li>
            <% } %>
            <% } %>
            <li class="page-item <%= (currentPage == totalPages) ? "disabled" : "" %>">
                <button class="page-link pmb-page-link" aria-label="<%= Common.getBahasaConfig("Selanjutnya") %>"
                        onclick="loadGelombang<%= rnd %>(<%= currentPage + 1 %>)">
                    <i class="fas fa-chevron-right small"></i>
                </button>
            </li>
        </ul>
    </nav>
</div>
<%
            } /* end if totalPages > 1 */
        } /* end else totalData > 0 */

    } finally {
        if (sess != null) {
            try { sess.clear();      } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:576");}
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:577");}
            try { sess.close();      } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_gelombang.jsp:578");}
        }
    }
%>
