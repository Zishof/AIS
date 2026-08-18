<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
    boolean pt = true;
    boolean ya = true;
    Tbmuser tbmuser = Common.getCurrentUser(request);
    try {
        if (tbmuser != null) {
            boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
            pt = ptYa[0];
            ya = ptYa[1];
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/akademik/index.jsp:14");
    } finally {
        // Blok finally untuk memastikan penutupan session atau pelepasan resource (jika ada pemanggilan koneksi database secara langsung).
    }

    // Variabel acak untuk memastikan ID tab tidak bentrok jika dipanggil berkali-kali
    String rndDash = Common.getGeneratedBarCode(5);
%>

<%-- KONDISI 1: JIKA KEDUANYA TRUE (Tampilkan Tab) --%>
<% if (pt && ya) { %>
    <div class="container-fluid p-0 animate__animated animate__fadeIn">
        <ul class="nav nav-tabs mb-3 shadow-sm border-bottom-0" id="dashTabs<%=rndDash%>" role="tablist">
            <li class="nav-item" role="presentation">
                <button class="nav-link active fw-bold px-4" id="tab-pt-<%=rndDash%>" data-bs-toggle="tab" data-bs-target="#content-pt-<%=rndDash%>" type="button" role="tab">
                    <i class="fas fa-university me-2 text-primary"></i>Dashboard Perguruan Tinggi
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link fw-bold px-4" id="tab-sekolah-<%=rndDash%>" data-bs-toggle="tab" data-bs-target="#content-sekolah-<%=rndDash%>" type="button" role="tab">
                    <i class="fas fa-school me-2 text-success"></i>Dashboard Sekolah
                </button>
            </li>
        </ul>

        <div class="tab-content" id="dashTabsContent<%=rndDash%>">
            <div class="tab-pane fade show active" id="content-pt-<%=rndDash%>" role="tabpanel">
                <jsp:include page="/WEB-INF/baru/modul/dsh/_dashboard.jsp">
                    <jsp:param value="false" name="tampilkan_header" />
                </jsp:include>
            </div>
            
            <div class="tab-pane fade" id="content-sekolah-<%=rndDash%>" role="tabpanel">
                <jsp:include page="/WEB-INF/baru/modul/dsh/_dashboard_sekolah.jsp">
                    <jsp:param value="false" name="tampilkan_header" />
                </jsp:include>
            </div>
        </div>
    </div>

<%-- KONDISI 2: JIKA HANYA PT YANG TRUE (Tampil Langsung Tanpa Tab) --%>
<% } else if (pt) { %>
    <jsp:include page="/WEB-INF/baru/modul/dsh/_dashboard.jsp">
        <jsp:param value="false" name="tampilkan_header" />
    </jsp:include>

<%-- KONDISI 3: JIKA HANYA SEKOLAH YANG TRUE (Tampil Langsung Tanpa Tab) --%>
<% } else if (ya) { %>
    <jsp:include page="/WEB-INF/baru/modul/dsh/_dashboard_sekolah.jsp">
        <jsp:param value="false" name="tampilkan_header" />
    </jsp:include>

<%-- KONDISI 4: JIKA KEDUANYA FALSE (Penanganan Pengecualian) --%>
<% } else { %>
    <div class="container-fluid mt-5">
        <div class="alert alert-warning text-center shadow-sm rounded-4 p-5">
            <i class="fas fa-exclamation-triangle fa-3x text-warning mb-3 d-block"></i>
            <h5 class="fw-bold">Akses Terbatas</h5>
            <p class="mb-0 text-muted">Anda tidak memiliki hak akses untuk melihat Dashboard Perguruan Tinggi maupun Dashboard Sekolah.</p>
        </div>
    </div>
<% } %>