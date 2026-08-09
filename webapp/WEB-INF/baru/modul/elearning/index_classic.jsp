<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
Tbmuser classicUser = Common.getCurrentUser(request);
if (classicUser == null || classicUser.getUserId() == null) {
    response.sendRedirect(request.getContextPath() + "/logoff");
    return;
}
String[][] classicTabs = {
    {"Linimasa", "fas fa-stream", "Linimasa"},
    {"Ringkasan", "fas fa-book-open", "Ringkasan"},
    {"Ujian", "fas fa-clipboard-list", "Ujian"},
    {"Tugas", "fas fa-check-square", "Tugas"},
    {"Materi", "fas fa-file-alt", "Materi"},
    {"Kalender", "fas fa-calendar-alt", "Kalender"},
    {"Dasbor", "fas fa-chart-pie", "Dasbor"},
    {"Obe", "fas fa-chart-line", "OBE"},
    {"Diskusi", "fas fa-comments", "Diskusi"},
    {"Laporan", "fas fa-file-export", "Laporan"}
};
%>
<div class="elearning-shell">
  <div class="elearning-card card overflow-hidden mb-3">
    <div class="card-header p-3">
      <div class="text-uppercase small fw-bold text-primary">e-Learning</div>
      <h4 class="mb-1 elearning-tab-title">Ruang Belajar Digital</h4>
      <div class="elearning-tab-desc d-block">Tampilan klasik dengan seluruh fungsi JSP dan service existing.</div>
    </div>
    <div class="card-header p-0 overflow-auto">
      <ul class="nav nav-tabs border-0 flex-nowrap" role="tablist">
        <% for (int i = 0; i < classicTabs.length; i++) { %>
        <li class="nav-item" role="presentation">
          <button class="nav-link <%=i == 0 ? "active" : ""%> text-nowrap" id="Classic-<%=classicTabs[i][0]%>-tab" data-bs-toggle="tab" data-bs-target="#Classic-<%=classicTabs[i][0]%>" type="button" role="tab">
            <i class="<%=classicTabs[i][1]%> me-2"></i><%=Common.getBahasaConfig(classicTabs[i][2])%>
          </button>
        </li>
        <% } %>
      </ul>
    </div>
    <div class="card-body p-3">
      <div class="tab-content">
        <div class="tab-pane fade show active" id="Classic-Linimasa" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/linimasa.jsp" /></div>
        <div class="tab-pane fade" id="Classic-Ringkasan" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/ringkasan.jsp" /></div>
        <div class="tab-pane fade" id="Classic-Ujian" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/ujian.jsp" /></div>
        <div class="tab-pane fade" id="Classic-Tugas" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/tugas.jsp" /></div>
        <div class="tab-pane fade" id="Classic-Materi" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/materi.jsp" /></div>
        <div class="tab-pane fade" id="Classic-Kalender" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/kalender.jsp" /></div>
        <div class="tab-pane fade" id="Classic-Dasbor" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/dasbor.jsp" /></div>
        <div class="tab-pane fade" id="Classic-Obe" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/obe.jsp" /></div>
        <div class="tab-pane fade" id="Classic-Diskusi" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/diskusi.jsp" /></div>
        <div class="tab-pane fade" id="Classic-Laporan" role="tabpanel"><jsp:include page="/WEB-INF/baru/modul/elearning/laporan.jsp" /></div>
      </div>
    </div>
  </div>
</div>
<jsp:include page="/WEB-INF/baru/modul/elearning/_footer_elearning.jsp" />
