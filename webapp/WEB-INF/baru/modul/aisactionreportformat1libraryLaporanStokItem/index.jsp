<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="org.hibernate.Session"%>
<%@ page import="org.hibernate.criterion.Order"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.database.hibernate.HibernateUtil"%>
<%@ page import="ais.database.model.library.Perpustakaan"%>
<%
    // Menu lama URL = ais.action.report.format1.library.LaporanStokItem (ZK) -> versi JSP.
    // Filter sama spt ZK (Perpustakaan, Mulai, Sampai); render PDF/HTML lewat /jalankan-laporan
    // yang memantulkan param-gen REUSABLE di class tsb.
    String KELAS = "ais.action.report.format1.library.LaporanStokItem";
    String runner = Common.ROOT + "/jalankan-laporan";
    String rnd = Common.getGeneratedBarCode(6);

    SimpleDateFormat htmlDate = new SimpleDateFormat("yyyy-MM-dd");
    Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
    String tglSampai = htmlDate.format(cal.getTime());
    cal.add(Calendar.MONTH, -3);
    String tglMulai = htmlDate.format(cal.getTime());

    List perpustakaans = new ArrayList();
    try {
        // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
        // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
        Session ses = HibernateUtil.currentNativeSession();
        perpustakaans = ses.createCriteria(Perpustakaan.class).addOrder(Order.asc("nama")).list();
    } catch (Exception e) {
        try { perpustakaans = HibernateUtil.currentNativeSession().createCriteria(Perpustakaan.class).list(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/aisactionreportformat1libraryLaporanStokItem/index.jsp:30");}
    }
%>
<div class="container-fluid px-0">
  <h5 class="fw-bold mb-1"><i class="fas fa-warehouse me-2 text-primary"></i><%=Common.getBahasaConfig("Laporan Stok Item")%></h5>
  <p class="text-muted small mb-3"><%=Common.getBahasaConfig("Pilih perpustakaan dan rentang tanggal, lalu tampilkan laporan.")%></p>

  <div class="card border-0 shadow-sm rounded-4 mb-3">
    <div class="card-body">
      <div class="row g-2 align-items-end">
        <div class="col-md-4">
          <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Perpustakaan")%> <span class="text-danger">*</span></label>
          <select id="perpus<%=rnd%>" class="form-select form-select-sm">
            <option value="">-- <%=Common.getBahasaConfig("pilih")%> --</option>
            <%
              for (int i = 0; i < perpustakaans.size(); i++) {
                  Perpustakaan p = (Perpustakaan) perpustakaans.get(i);
                  String label = p == null ? "" : String.valueOf(p);
            %>
            <option value="<%=p.getId()%>"><%=label%></option>
            <% } %>
          </select>
        </div>
        <div class="col-md-3">
          <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Mulai Tanggal")%></label>
          <input type="date" id="mulai<%=rnd%>" class="form-control form-control-sm" value="<%=tglMulai%>">
        </div>
        <div class="col-md-3">
          <label class="form-label small fw-semibold"><%=Common.getBahasaConfig("Sampai Tanggal")%></label>
          <input type="date" id="sampai<%=rnd%>" class="form-control form-control-sm" value="<%=tglSampai%>">
        </div>
        <div class="col-md-2 d-grid">
          <button class="btn btn-primary btn-sm" onclick="tampilLap<%=rnd%>('pdf',false)"><i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Tampilkan")%></button>
        </div>
      </div>
      <div class="mt-2 d-flex gap-2 flex-wrap">
        <button class="btn btn-outline-danger btn-sm" onclick="tampilLap<%=rnd%>('pdf',true)"><i class="fas fa-file-pdf me-1"></i>PDF</button>
        <button class="btn btn-outline-success btn-sm" onclick="tampilLap<%=rnd%>('xls',true)"><i class="fas fa-file-excel me-1"></i>Excel</button>
        <button class="btn btn-outline-secondary btn-sm" onclick="tampilLap<%=rnd%>('html',false)"><i class="fas fa-code me-1"></i>HTML</button>
      </div>
    </div>
  </div>

  <div class="card border-0 shadow-sm rounded-4">
    <div class="card-body p-2">
      <iframe id="frame<%=rnd%>" style="width:100%;min-height:1200px;border:0;border-radius:8px;background:#fff;" src="about:blank"></iframe>
    </div>
  </div>
</div>

<script>
(function(){
  var RUNNER='<%=runner%>', KELAS='<%=KELAS%>', rnd='<%=rnd%>';
  function val(id){ var el=document.getElementById(id+rnd); return el?el.value:''; }
  window['tampilLap'+rnd]=function(format, unduh){
    var perpus=val('perpus');
    if(!perpus){ alert('<%= Common.getBahasaConfigJS("Mohon pilih perpustakaan terlebih dahulu.") %>'); return; }
    var q = RUNNER + '?kelas=' + encodeURIComponent(KELAS)
      + '&format=' + encodeURIComponent(format)
      + '&unduh=' + (unduh?'1':'0')
      + '&perpustakaan=' + encodeURIComponent(perpus)
      + '&mulai=' + encodeURIComponent(val('mulai'))
      + '&sampai=' + encodeURIComponent(val('sampai'));
    if(unduh){ window.open(q, '_blank'); }
    else { document.getElementById('frame'+rnd).src = q; }
  };
})();
</script>
