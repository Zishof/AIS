<%@page import="java.util.Set"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Arrays"%>
<%@page import="ais.ui.util.MyWindow"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Menu"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
private String baruErrorH(Object value){if(value==null)return "";return String.valueOf(value).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
%>

<%
Throwable pageFailure=(Throwable)request.getAttribute("baru.error.throwable");
if(pageFailure!=null){
    String pageTrace=(String)request.getAttribute("baru.error.trace");
    String pageInfo=(String)request.getAttribute("baru.error.info");
    String pageContent=(String)request.getAttribute("baru.error.content");
    Object pageLogId=request.getAttribute("baru.error.log_id");
    boolean pageShowDetail=Boolean.TRUE.equals(request.getAttribute("baru.error.show_detail"));
%>
<div class="card shadow border-danger rounded-lg mb-4 animate__animated animate__fadeInUp">
  <div class="card-header bg-danger text-white py-3"><h5 class="mb-0 fw-bold"><i class="fas fa-exclamation-triangle me-2"></i>Terjadi error saat mengakses menu</h5></div>
  <div class="card-body p-4">
    <p>Error tidak lagi disembunyikan. Detail berikut sudah dicatat ke log server.</p>
    <div class="table-responsive"><table class="table table-sm table-bordered align-middle">
      <tr><th style="width:160px">Trace ID</th><td><code><%=baruErrorH(pageTrace)%></code></td></tr>
      <tr><th>Jenis error</th><td><%=baruErrorH(pageFailure.getClass().getName())%></td></tr>
      <tr><th>Pesan</th><td><%=baruErrorH(pageFailure.getMessage())%></td></tr>
      <tr><th>Lokasi/target</th><td><%=baruErrorH(pageInfo)%></td></tr>
      <tr><th>Error Log ID</th><td><%=baruErrorH(pageLogId==null?"Tidak tersimpan / duplikat":pageLogId)%></td></tr>
    </table></div>
    <%if(pageShowDetail&&pageContent!=null){%><details class="mt-3"><summary class="fw-bold">Stack trace lengkap</summary><pre class="mt-2 p-3 rounded bg-dark text-light" style="white-space:pre-wrap;max-height:520px;overflow:auto;font-size:12px"><%=baruErrorH(pageContent)%></pre></details><%}else{%>
    <div class="alert alert-warning mt-3 mb-0">Stack trace lengkap tersedia di log server. Untuk diagnosis terkontrol, aktifkan konfigurasi <code>global_error_tampilkan_stacktrace_ui</code>.</div><%}%>
  </div>
</div>
<%
    request.removeAttribute("baru.error.trace");request.removeAttribute("baru.error.info");
    request.removeAttribute("baru.error.throwable");request.removeAttribute("baru.error.content");
    request.removeAttribute("baru.error.log_id");request.removeAttribute("baru.error.show_detail");
    return;
}
%>

<%
Menu menu = null;
String p = request.getParameter("p");
String menuParam = request.getParameter("menu");

// 1. Optimasi Logika: Menggunakan Set untuk pencarian parameter (O(1)) yang lebih cepat dan bersih
Set<String> validModules = new HashSet<String>(Arrays.asList(
    "prestasi", "pustaka", "pagesmastersoppengajuananda", "akademik", "suratmenyurat",
    "pengadaan", "pembayaran", "keuangan", "akuntansi", "kepegawaian",
    "kinerja", "presensi"
));

boolean isValidModule = (p != null && validModules.contains(p.toLowerCase()));
boolean hasAccess = false;

// TODO: Deklarasikan objek session database Anda di sini (misal: org.hibernate.Session)
// Session dbSession = null;

try {
    // dbSession = HibernateUtil.getSessionFactory().openSession(); // Contoh pembukaan session
    
    // 2. Keamanan Data: Pengecekan null dan kosong sebelum query ke database
    if (menuParam != null && !menuParam.trim().isEmpty()) {
        menu = (Menu) GeneralValueObject.ambilData(Menu.class, menuParam.trim(), true);
    }
    
    hasAccess = (menu != null || isValidModule);
    
} catch (Exception e) {
    // Tangani error jika terjadi kegagalan koneksi atau data tidak valid
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/componen/tidak_ketemu_page.jsp:40"); 
} finally {
    // 3. Penutupan Session: Pastikan koneksi database selalu ditutup untuk mencegah memory/connection leak
    // if (dbSession != null && dbSession.isOpen()) {
    //     dbSession.close();
    // }
}
%>

<div class="card shadow border-0 rounded-lg mb-4 animate__animated animate__fadeInUp">
    <div class="card-header bg-white border-bottom py-3 d-flex align-items-center">
        <h5 class="mb-0 text-warning fw-bold">
            <i class="fas fa-tools me-2"></i>
            <%= hasAccess 
                ? Common.getBahasaConfig("Antarmuka pengguna baru sedang dalam tahap pengembangan") 
                : Common.getBahasaConfig("Menu sedang dalam tahap pengembangan") %>
        </h5>
    </div>

    <div class="card-body p-4">
        <div class="w-100">
            <% if (hasAccess) { %>
                <div class="alert alert-info d-flex align-items-center shadow-sm border-0" role="alert">
                    <i class="fas fa-info-circle fa-2x me-3 text-info"></i>
                    <div class="w-100">
                        <strong><%= Common.getBahasaConfig("Halaman versi JSP belum tersedia.") %></strong><br>
                        <%= Common.getBahasaConfig("Aplikasi tidak lagi memuat halaman ZK/ZUL di dalam tampilan JSP. Silakan buka Tampilan Klasik dari menu profil bila menu ini masih diperlukan.") %>
                    </div>
                </div>
            <% } else { %>
                <div class="alert alert-info d-flex align-items-center shadow-sm border-0" role="alert">
                    <i class="fas fa-info-circle fa-2x me-3 text-info"></i>
                    <div class="w-100">
                        <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu.jsp">
                            <jsp:param name="judul" value='<%= Common.getBahasaConfigJS("Proses pengembangan sedang berlangsung") %>' />
                            <jsp:param name="jenis" value="alert" />
                            <jsp:param name="pesan" value='<%= Common.getBahasaConfigJS("Mohon maaf, modul yang Anda pilih saat ini sedang dalam tahap pengembangan.") %>' />
                        </jsp:include>
                    </div>
                </div>
            <% } %>
        </div>
    </div>
</div>
