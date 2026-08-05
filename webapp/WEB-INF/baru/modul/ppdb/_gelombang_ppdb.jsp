<%@page import="ais.common.ConstantValues"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Comparator"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.sekolah.GelombangPendaftaranPsb"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // Variabel Link Pendaftaran untuk Popup AJAX
    String linkPendaftaran = Common.ROOT + "/ppdb?hanya_tampil_jsp=true&p=ppdb&s=_pendaftaran_siswa&baru=true";

    String ta = request.getParameter("ta");
    String seleksi = request.getParameter("seleksi");
    String statusFilter = request.getParameter("status"); 
    String rnd = request.getParameter("rnd"); 
    String pageStr = request.getParameter("page");
    
    if (rnd == null) rnd = "";
    int currentPage = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
    
    // Menampilkan 10 gelombang per halaman
    int limitPerPage = 10;

    Session sess = null;
    List<GelombangPendaftaranPsb> rawList = new ArrayList<GelombangPendaftaranPsb>();
    
    try {
        sess = HibernateUtil.openSession();
        Criteria criteria = sess.createCriteria(GelombangPendaftaranPsb.class)
                .add(Restrictions.eq("aktif", true));
                
        // Filter Tahun Ajaran
        if (ta != null && !ta.trim().isEmpty()) {
            criteria.add(Restrictions.eq("tahunAjaran", ta.trim()));
        }
        
        // Filter Penjurusan Sekolah
        if (seleksi != null && !seleksi.trim().isEmpty()) {
            criteria.createAlias("penjurusanSekolah", "psAlias");
            try {
                criteria.add(Restrictions.eq("psAlias.id", Long.parseLong(seleksi)));
            } catch (NumberFormatException e) {
                criteria.add(Restrictions.eq("psAlias.id", seleksi));
            }
        }
        
        rawList = ConstantValues.simpleList(criteria, GelombangPendaftaranPsb.class);
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_gelombang_ppdb.jsp:57");
    } finally {
        if (sess != null) {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_gelombang_ppdb.jsp:60");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_gelombang_ppdb.jsp:61");}
        }
        HibernateUtil.closeSessionQuietly(sess);
    }

    if (rawList == null || rawList.isEmpty()) {
%>
        <div class="col-12 text-center py-5 bg-white shadow-sm rounded-4">
            <i class="fas fa-folder-open fa-3x text-muted mb-3 opacity-50 d-block"></i>
            <h5 class="text-muted fw-bold"><%= Common.getBahasaConfig("Tidak Terdapat Data Pendaftaran") %></h5>
            <p class="text-secondary small"><%= Common.getBahasaConfig("Silakan menyesuaikan kembali filter pencarian Anda.") %></p>
        </div>
<%
    } else {
        final long nowTime = new Date().getTime();
        List<GelombangPendaftaranPsb> filteredList = new ArrayList<GelombangPendaftaranPsb>();
        
        // 1. FILTERING STATUS BUKA/TUTUP
        for (GelombangPendaftaranPsb g : rawList) {
            boolean isBuka = false;
            if (g.getMulai() != null && g.getSampai() != null) {
                long mulaiTime = g.getMulai().getTime();
                long selesaiTime = g.getSampai().getTime() + (1000 * 60 * 60 * 24) - 1; 
                if (nowTime >= mulaiTime && nowTime <= selesaiTime) isBuka = true;
            }
            if ("buka".equalsIgnoreCase(statusFilter) && !isBuka) continue;
            if ("tutup".equalsIgnoreCase(statusFilter) && isBuka) continue;
            
            filteredList.add(g);
        }

        // 2. KUSTOMISASI PENGURUTAN MANUAL (SORTING)
        Collections.sort(filteredList, new Comparator<GelombangPendaftaranPsb>() {
            @Override
            public int compare(GelombangPendaftaranPsb g1, GelombangPendaftaranPsb g2) {
                boolean b1 = false, b2 = false;
                if (g1.getMulai() != null && g1.getSampai() != null) {
                    long s1 = g1.getSampai().getTime() + (1000 * 60 * 60 * 24) - 1;
                    b1 = (nowTime >= g1.getMulai().getTime() && nowTime <= s1);
                }
                if (g2.getMulai() != null && g2.getSampai() != null) {
                    long s2 = g2.getSampai().getTime() + (1000 * 60 * 60 * 24) - 1;
                    b2 = (nowTime >= g2.getMulai().getTime() && nowTime <= s2);
                }

                if (b1 && !b2) return -1;
                if (!b1 && b2) return 1;

                if (g1.getSampai() != null && g2.getSampai() != null) {
                    int cmpSelesai = g2.getSampai().compareTo(g1.getSampai());
                    if (cmpSelesai != 0) return cmpSelesai;
                } else if (g1.getSampai() != null) return -1;
                else if (g2.getSampai() != null) return 1;

                if (g1.getMulai() != null && g2.getMulai() != null) {
                    return g2.getMulai().compareTo(g1.getMulai());
                }
                return 0;
            }
        });

        // 3. PAGINASI
        int totalData = filteredList.size();
        if (totalData == 0) {
%>
            <div class="col-12 text-center py-5 bg-white shadow-sm rounded-4">
                <i class="fas fa-filter fa-3x text-muted mb-3 opacity-50 d-block"></i>
                <h5 class="text-muted fw-bold"><%= Common.getBahasaConfig("Data Tidak Ditemukan") %></h5>
                <p class="text-secondary small"><%= Common.getBahasaConfig("Tidak terdapat gelombang pendaftaran yang sesuai dengan status yang Anda pilih.") %></p>
            </div>
<%      } else {
            int totalPages = (int) Math.ceil((double) totalData / limitPerPage);
            if (currentPage < 1) currentPage = 1;
            if (currentPage > totalPages) currentPage = totalPages;

            int startIdx = (currentPage - 1) * limitPerPage;
            int endIdx = Math.min(startIdx + limitPerPage, totalData);
            List<GelombangPendaftaranPsb> pagedList = filteredList.subList(startIdx, endIdx);

            // 4. RENDER DATA FULL WIDTH (col-12) HORIZONTAL CARD
            for (GelombangPendaftaranPsb g : pagedList) {
                String nama = g.getNama() != null ? g.getNama() : "-";
                String taStr = g.getTahunAjaran() != null ? g.getTahunAjaran() : "-";
                
                String informasi = g.getInformasi() != null && !g.getInformasi().trim().isEmpty() ? g.getInformasi() : "-";
                String tglMulai = g.getMulai() != null ? Common.dateFormat41.get().format(g.getMulai()) : "-";
                String tglSampai = g.getSampai() != null ? Common.dateFormat41.get().format(g.getSampai()) : "-";
                
                boolean isBuka = false;
                if (g.getMulai() != null && g.getSampai() != null) {
                    long mulaiTime = g.getMulai().getTime();
                    long selesaiTime = g.getSampai().getTime() + (1000 * 60 * 60 * 24) - 1; 
                    if (nowTime >= mulaiTime && nowTime <= selesaiTime) isBuka = true;
                }

                String statusText = isBuka ? Common.getBahasaConfig("Sedang Dibuka") : Common.getBahasaConfig("Telah Ditutup");
                String statusColor = isBuka ? "bg-success" : "bg-danger";
                String btnClass = isBuka ? "btn-primary" : "btn-secondary";
                String btnText = isBuka ? "<i class=\"fas fa-edit me-1\"></i> " + Common.getBahasaConfig("Mulai Pendaftaran") : "<i class=\"fas fa-lock me-1\"></i> " + Common.getBahasaConfig("Pendaftaran Ditutup");
                String disabledStr = isBuka ? "" : "disabled";
                
                String keteranganTambahan = g.getKeterangan() != null && !g.getKeterangan().trim().isEmpty() 
                                ? g.getKeterangan() 
                                : Common.getBahasaConfig("Tidak terdapat informasi tambahan terkait gelombang ini.");
%>
            <div class="col-12 animate__animated animate__fadeInUp">
                <div class="card card-gelombang-<%=rnd%> rounded-4 <%= isBuka ? "" : "bg-light border-0 opacity-75" %> border border-light shadow-sm mb-3 transition-base">
                    <div class="card-body p-4 d-flex flex-column flex-md-row justify-content-between align-items-md-center">
                        <div class="mb-3 mb-md-0 flex-grow-1 pe-md-4">
                            <div class="d-flex align-items-center mb-2">
                                <span class="badge bg-primary bg-opacity-10 text-primary rounded-pill border border-primary border-opacity-25 px-2 py-1 me-2 shadow-sm">
                                    <i class="fas fa-graduation-cap me-1"></i> TA <%= taStr %>
                                </span>
                                <span class="badge <%= statusColor %> rounded-pill shadow-sm px-3 py-1"><%= statusText %></span>
                            </div>
                            <h5 class="fw-bold text-dark mb-1"><%= nama %></h5>
                            
                            <div class="text-secondary small fw-semibold mb-2"><i class="fas fa-info-circle me-1 text-info"></i><%= informasi %></div>
                            
                            <div class="d-flex flex-wrap gap-3 small text-muted mt-3">
                                <div class="bg-white px-2 py-1 rounded shadow-sm border"><i class="far fa-calendar-alt text-primary me-1"></i><strong><%= Common.getBahasaConfig("Tanggal Mulai") %>:</strong> <%= tglMulai %></div>
                                <div class="bg-white px-2 py-1 rounded shadow-sm border"><i class="far fa-calendar-check text-danger me-1"></i><strong><%= Common.getBahasaConfig("Batas Akhir") %>:</strong> <%= tglSampai %></div>
                            </div>
                            
                            <% if (g.getKeterangan() != null && !g.getKeterangan().trim().isEmpty()) { %>
                            <div class="alert alert-light border small text-secondary mt-3 mb-0 py-2 px-3 shadow-sm rounded-3">
                                <i class="fas fa-info-circle me-1 text-info"></i> <%= keteranganTambahan %>
                            </div>
                            <% } %>
                        </div>
                        <div class="mt-3 mt-md-0 text-md-end border-start-md ps-md-4" style="min-width: 200px;">
                            <button class="btn <%= btnClass %> w-100 rounded-pill fw-bold shadow-sm py-2" <%= disabledStr %> onclick="daftarGelombangPPDB<%=rnd%>('<%= g.getId() %>')">
                                <%= btnText %>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
<%
            } // end perulangan render

            // 5. RENDER PAGINASI UI
            if (totalPages > 1) {
%>
            <div class="col-12 mt-4 d-flex justify-content-center">
                <nav aria-label="Navigasi Halaman Gelombang PPDB">
                    <ul class="pagination pagination-sm shadow-sm rounded-pill bg-white mb-0 border">
                        <li class="page-item <%= (currentPage == 1) ? "disabled" : "" %>">
                            <button class="page-link rounded-start-pill text-primary fw-bold px-3 py-2 border-0" onclick="loadGelombangPPDB<%=rnd%>(<%= currentPage - 1 %>)"><i class="fas fa-chevron-left me-1"></i><%= Common.getBahasaConfig("Sebelumnya") %></button>
                        </li>
                        <% for (int i = 1; i <= totalPages; i++) { 
                            if (i == 1 || i == totalPages || (i >= currentPage - 2 && i <= currentPage + 2)) {
                        %>
                            <li class="page-item <%= (currentPage == i) ? "active" : "" %>">
                                <button class="page-link <%= (currentPage == i) ? "bg-primary text-white fw-bold shadow-sm" : "text-secondary border-0" %>" onclick="loadGelombangPPDB<%=rnd%>(<%= i %>)"><%= i %></button>
                            </li>
                        <%  } else if (i == currentPage - 3 || i == currentPage + 3) { %>
                            <li class="page-item disabled"><span class="page-link text-secondary border-0">...</span></li>
                        <%  }
                        } %>
                        <li class="page-item <%= (currentPage == totalPages) ? "disabled" : "" %>">
                            <button class="page-link rounded-end-pill text-primary fw-bold px-3 py-2 border-0" onclick="loadGelombangPPDB<%=rnd%>(<%= currentPage + 1 %>)"><%= Common.getBahasaConfig("Selanjutnya") %><i class="fas fa-chevron-right ms-1"></i></button>
                        </li>
                    </ul>
                </nav>
            </div>
<%
            }
        }
    }
%>

<style>
    .border-start-md { border-left: 1px solid #e9ecef; }
    .transition-base { transition: all 0.2s ease-in-out; }
    .transition-base:hover { transform: translateY(-2px); box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.1) !important; }
    @media (max-width: 768px) {
        .border-start-md { border-left: none; }
    }
</style>

<script>
window.daftarGelombangPPDB<%=rnd%> = async (idGelombang) => {
    if(typeof window.showLoadingPPDB === 'function') window.showLoadingPPDB();
    try {
        const response = await fetch('<%=linkPendaftaran%>&gelombangId=' + idGelombang);
        const content = await response.text();
        
        const modalId = 'modalDaftarPpdb<%=rnd%>';
        if(document.getElementById(modalId)) document.getElementById(modalId).remove();
        
        const modalHtml = 
            '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">' +
                '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                    '<div class="modal-content shadow-lg border-0 rounded-4">' +
                        '<div class="modal-header bg-light border-0 py-3 px-4">' +
                            '<h5 class="modal-title fw-bold text-dark"><i class="fas fa-file-signature text-primary me-2"></i><%= Common.getBahasaConfig("Formulir Pendaftaran Siswa Baru") %></h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body p-0 bg-light">' + content + '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalElement = document.getElementById(modalId);
        
        // Render ulang elemen <script> yang dikembalikan dari file _pendaftaran_siswa.jsp
        const scriptsArray = Array.from(modalElement.getElementsByTagName('script'));
        for (let i = 0; i < scriptsArray.length; i++) {
            const oldScript = scriptsArray[i];
            if (oldScript.src && oldScript.src.includes('email-decode')) continue; 
            const scriptNode = document.createElement('script');
            var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
            Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
            scriptNode.type = 'text/javascript';
            if (srcEff) { scriptNode.src = srcEff; document.body.appendChild(scriptNode); }
            else { scriptNode.text = oldScript.innerHTML; document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode); }
        }

        if(typeof window.hideLoadingPPDB === 'function') window.hideLoadingPPDB();
        new bootstrap.Modal(modalElement).show();
        modalElement.addEventListener('hidden.bs.modal', function () { this.remove(); });
        
    } catch (error) {
        if(typeof window.hideLoadingPPDB === 'function') window.hideLoadingPPDB();
        if(typeof tampilkanToast === 'function') {
            tampilkanToast('<%= Common.getBahasaConfigJS("Gagal memuat formulir pendaftaran. Silakan coba kembali.") %>', 'bg-danger text-white');
        } else {
            alert('<%= Common.getBahasaConfigJS("Gagal memuat formulir pendaftaran. Silakan coba kembali.") %>');
        }
    }
};
</script>