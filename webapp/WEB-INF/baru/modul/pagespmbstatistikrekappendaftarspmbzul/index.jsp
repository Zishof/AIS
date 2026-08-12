<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Tampilan baru hanya menjadi shell responsif. Seluruh logika, filter, hak akses,
    // query, input peminat asli, dan perhitungan dasbor tetap bersumber dari ZK existing.
    String dashboard = request.getParameter("dashboard");
    String zkUrl = request.getContextPath() + "/pages/pmb/statistik/rekap_pendaftar_spmb.zul"
            + ("jalur".equalsIgnoreCase(dashboard) ? "?dashboard=jalur" : "");
%>
<style>
    .pmb-dashboard-shell{height:calc(100vh - 118px);min-height:760px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;overflow:hidden;box-shadow:0 4px 18px rgba(15,23,42,.07)}
    .pmb-dashboard-head{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:11px 16px;background:#fff;border-bottom:1px solid #e2e8f0}
    .pmb-dashboard-title{font-size:16px;font-weight:700;color:#0f172a;margin:0}
    .pmb-dashboard-note{font-size:12px;color:#64748b;margin:2px 0 0}
    .pmb-dashboard-open{display:inline-flex;align-items:center;gap:6px;padding:7px 11px;border-radius:8px;background:#eff6ff;color:#1d4ed8;text-decoration:none;font-size:12px;font-weight:600;white-space:nowrap}
    .pmb-dashboard-frame{display:block;width:100%;height:calc(100% - 61px);border:0;background:#fff}
    @media(max-width:768px){.pmb-dashboard-shell{height:calc(100vh - 86px);min-height:650px;border-radius:8px}.pmb-dashboard-head{padding:9px 11px}.pmb-dashboard-note{display:none}.pmb-dashboard-open{padding:6px 8px}.pmb-dashboard-frame{height:calc(100% - 50px)}}
</style>
<section class="pmb-dashboard-shell" aria-label="Dashboard PMB">
    <header class="pmb-dashboard-head">
        <div>
            <h2 class="pmb-dashboard-title">Dashboard dan Rekap PMB</h2>
            <p class="pmb-dashboard-note">Tampilan baru dengan logika dan data yang tetap bersumber dari modul ZK eCampus.</p>
        </div>
        <a class="pmb-dashboard-open" href="<%=zkUrl%>" target="_blank" rel="noopener">
            <i class="fas fa-up-right-from-square" aria-hidden="true"></i> Buka Penuh
        </a>
    </header>
    <iframe class="pmb-dashboard-frame" src="<%=zkUrl%>" title="Dashboard PMB eCampus" loading="eager"></iframe>
</section>
