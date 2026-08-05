<%@page import="java.util.Set"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Arrays"%>
<%@page import="ais.ui.util.MyWindow"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Menu"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

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
