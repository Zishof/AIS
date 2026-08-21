<%@page import="java.net.URLEncoder"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.DynamicJspCrudGenerator"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
String rnd = request.getParameter("rnd") == null ? "" : request.getParameter("rnd").trim();
String configJson = DynamicJspCrudGenerator.getConfigJson(request, rnd);
String apiUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common&s=dynamic_crud_api&rnd=" + URLEncoder.encode(rnd, "UTF-8");
String uploadUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=common&s=upload_component";
%>
<% if (configJson == null || configJson.trim().isEmpty()) { %>
<div class="alert alert-danger" style="border-radius:14px; padding:14px 16px;">
    Konfigurasi CRUD tidak ditemukan. Silakan refresh halaman atau panggil ulang DynamicJspCrudGenerator.render(...).
</div>
<% } else { %>
<div id="crud_<%=rnd%>" class="ais-crud-shell">
    <style>
        #crud_<%=rnd%>.ais-crud-shell{--crud-primary:var(--theme-primary,#2563eb);--crud-primary2:var(--theme-primary-dark,#7c3aed);--crud-soft:#f8fafc;--crud-line:#e2e8f0;--crud-text:#0f172a;--crud-muted:#64748b;font-family:Inter,system-ui,-apple-system,"Segoe UI",Arial,sans-serif;color:var(--crud-text);}
        #crud_<%=rnd%> .crud-card{background:#fff;border:1px solid rgba(148,163,184,.24);border-radius:22px;box-shadow:0 16px 40px rgba(15,23,42,.08);overflow:hidden;margin-bottom:18px;}
        #crud_<%=rnd%> .crud-hero{position:relative;padding:20px 22px;color:#fff;background:var(--theme-gradient,linear-gradient(135deg,var(--crud-primary),var(--crud-primary2)));overflow:hidden;}
        #crud_<%=rnd%> .crud-hero:after{content:"";position:absolute;right:-80px;top:-90px;width:240px;height:240px;border-radius:999px;background:rgba(255,255,255,.16);} 
        #crud_<%=rnd%> .crud-title{position:relative;z-index:1;display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;}
        #crud_<%=rnd%> .crud-title h3{margin:0;font-size:20px;font-weight:800;letter-spacing:.2px;color:#fff;}
        #crud_<%=rnd%> .crud-subtitle{margin:5px 0 0;font-size:12px;color:rgba(255,255,255,.82);} 
        #crud_<%=rnd%> .crud-actions{display:flex;gap:8px;flex-wrap:wrap;align-items:center;}
        #crud_<%=rnd%> .crud-btn{border:0;border-radius:12px;padding:9px 13px;font-size:13px;font-weight:700;cursor:pointer;transition:.18s;display:inline-flex;align-items:center;gap:7px;text-decoration:none;line-height:1.2;}
        #crud_<%=rnd%> .crud-btn:hover{transform:translateY(-1px);box-shadow:0 10px 24px rgba(15,23,42,.16);} 
        #crud_<%=rnd%> .crud-btn-primary{background:#fff;color:#1d4ed8;}
        #crud_<%=rnd%> .crud-btn-soft{background:rgba(255,255,255,.18);color:#fff;border:1px solid rgba(255,255,255,.28);} 
        #crud_<%=rnd%> .crud-btn-gray{background:#f1f5f9;color:#334155;border:1px solid #e2e8f0;}
        #crud_<%=rnd%> .crud-btn-danger{background:#fee2e2;color:#b91c1c;border:1px solid #fecaca;}
        #crud_<%=rnd%> .crud-body{padding:16px 18px 18px;background:linear-gradient(180deg,#fff 0,#fbfdff 100%);} 
        #crud_<%=rnd%> .crud-toolbar{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-bottom:12px;}
        #crud_<%=rnd%> .crud-search-main{position:relative;min-width:260px;flex:1;max-width:520px;}
        #crud_<%=rnd%> .crud-search-main input{width:100%;border:1px solid var(--crud-line);border-radius:14px;padding:11px 14px 11px 38px;outline:none;background:#fff;font-size:13px;box-sizing:border-box;}
        #crud_<%=rnd%> .crud-search-main span{position:absolute;left:14px;top:50%;transform:translateY(-50%);color:var(--crud-muted);} 
        #crud_<%=rnd%> .crud-filter-grid{display:none;background:#f8fafc;border:1px solid #e2e8f0;border-radius:16px;padding:12px;margin-bottom:12px;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:10px;}
        #crud_<%=rnd%> .crud-field label{display:block;font-size:12px;font-weight:700;color:#475569;margin-bottom:5px;}
        #crud_<%=rnd%> .crud-control{width:100%;border:1px solid #dbe3ef;border-radius:12px;padding:9px 10px;font-size:13px;outline:none;background:#fff;box-sizing:border-box;}
        #crud_<%=rnd%> .crud-control:focus{border-color:#93c5fd;box-shadow:0 0 0 4px rgba(59,130,246,.12);} 
        #crud_<%=rnd%> .crud-table-wrap{border:1px solid #e2e8f0;border-radius:18px;overflow:auto;background:#fff;}
        #crud_<%=rnd%> table.crud-table{width:100%;border-collapse:separate;border-spacing:0;font-size:13px;}
        #crud_<%=rnd%> .crud-table thead th{position:sticky;top:0;background:#f8fafc;color:#475569;text-transform:uppercase;font-size:11px;letter-spacing:.04em;border-bottom:1px solid #e2e8f0;padding:12px 12px;text-align:left;white-space:nowrap;z-index:1;}
        #crud_<%=rnd%> .crud-table tbody td{padding:12px;border-bottom:1px solid #eef2f7;vertical-align:top;color:#1e293b;}
        #crud_<%=rnd%> .crud-table tbody tr:hover{background:#f8fbff;}
        #crud_<%=rnd%> .crud-sort{cursor:pointer;user-select:none;}
        #crud_<%=rnd%> .crud-cell-muted{color:#94a3b8;font-style:italic;}
        #crud_<%=rnd%> .crud-badge{display:inline-flex;align-items:center;border-radius:999px;padding:4px 9px;font-weight:700;font-size:11px;background:#e0f2fe;color:#0369a1;} 
        #crud_<%=rnd%> .crud-badge.no{background:#fee2e2;color:#b91c1c;}
        #crud_<%=rnd%> .crud-row-actions{display:flex;gap:6px;justify-content:flex-end;white-space:nowrap;}
        #crud_<%=rnd%> .crud-photo-col{width:72px;text-align:center!important;}
        #crud_<%=rnd%> .crud-photo-cell{width:72px;text-align:center;vertical-align:middle!important;}
        #crud_<%=rnd%> .crud-photo-thumb{width:46px;height:46px;border-radius:16px;object-fit:cover;border:2px solid rgba(255,255,255,.95);box-shadow:0 7px 18px rgba(15,23,42,.18);background:#eef2f7;cursor:pointer;transition:transform .16s ease,box-shadow .16s ease;}
        #crud_<%=rnd%> .crud-photo-thumb:hover{transform:scale(1.06);box-shadow:0 10px 26px rgba(15,23,42,.26);}
        #crud_<%=rnd%> .crud-icon-btn{border:1px solid #e2e8f0;background:#fff;border-radius:10px;padding:7px 9px;cursor:pointer;color:#334155;}
        #crud_<%=rnd%> .crud-icon-btn:hover{background:#eff6ff;color:#1d4ed8;} 
        #crud_<%=rnd%> .crud-footer{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-top:12px;color:#64748b;font-size:12px;}
        #crud_<%=rnd%> .crud-pages{display:flex;gap:6px;flex-wrap:wrap;}
        #crud_<%=rnd%> .crud-page{border:1px solid #e2e8f0;background:#fff;border-radius:10px;padding:7px 10px;cursor:pointer;font-size:12px;}
        #crud_<%=rnd%> .crud-page.active{background:var(--theme-gradient,#2563eb);color:var(--theme-text-on-primary,#fff);border-color:var(--crud-primary);}
        #crud_<%=rnd%> .crud-modal-backdrop{display:none;position:fixed;inset:0;background:rgba(15,23,42,.68);z-index:2147483000;padding:24px;overflow:auto;overscroll-behavior:contain;isolation:isolate;}
        #crud_<%=rnd%> .crud-modal-backdrop.show{display:block;}
        #crud_<%=rnd%> .crud-modal{position:relative;z-index:2147483001;width:min(1120px,100%);max-width:1120px;max-height:calc(100vh - 48px);margin:20px auto;background:#fff;border-radius:24px;box-shadow:0 24px 90px rgba(0,0,0,.45);overflow:hidden;display:flex;flex-direction:column;}
        #crud_<%=rnd%> .crud-modal-head{padding:17px 20px;background:var(--theme-gradient,linear-gradient(135deg,#0f172a,#1d4ed8));color:#fff;display:flex;align-items:center;justify-content:space-between;gap:12px;flex:0 0 auto;}
        #crud_<%=rnd%> .crud-modal-head h4{margin:0;font-weight:800;font-size:18px;color:#fff;} 
        #crud_<%=rnd%> .crud-close{border:0;background:rgba(255,255,255,.16);color:#fff;border-radius:12px;width:36px;height:36px;font-size:20px;cursor:pointer;}
        #crud_<%=rnd%> .crud-modal-body{padding:16px 18px;max-height:calc(100vh - 150px);overflow:auto;background:#f8fafc;flex:1 1 auto;overscroll-behavior:contain;-webkit-overflow-scrolling:touch;}
        #crud_<%=rnd%> .crud-tabs{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px;}
        #crud_<%=rnd%> .crud-tab{border:1px solid #dbe3ef;background:#fff;border-radius:999px;padding:9px 13px;font-weight:800;color:#475569;cursor:pointer;font-size:13px;}
        #crud_<%=rnd%> .crud-tab.active{background:var(--theme-gradient,#2563eb);color:var(--theme-text-on-primary,#fff);border-color:var(--crud-primary);box-shadow:0 8px 18px rgba(37,99,235,.24);} 
        #crud_<%=rnd%> .crud-step-card{display:none;background:#fff;border:1px solid #e2e8f0;border-radius:18px;padding:14px;margin-bottom:12px;}
        #crud_<%=rnd%> .crud-step-card.active{display:block;}
        #crud_<%=rnd%> .crud-step-title{display:flex;align-items:center;gap:9px;margin:0 0 12px;color:#0f172a;font-size:15px;font-weight:900;}
        #crud_<%=rnd%> .crud-step-dot{width:26px;height:26px;border-radius:999px;background:#eff6ff;color:#1d4ed8;display:inline-flex;align-items:center;justify-content:center;font-size:12px;font-weight:900;}
        #crud_<%=rnd%> .crud-form-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:12px;} 
        #crud_<%=rnd%> .crud-form-grid .wide{grid-column:1/-1;}
        #crud_<%=rnd%> .crud-required{color:#dc2626;}
        #crud_<%=rnd%> .crud-switch{display:flex;align-items:center;gap:8px;padding:10px;border:1px solid #dbe3ef;border-radius:12px;background:#fff;}
        #crud_<%=rnd%> .crud-switch input{width:18px;height:18px;}
        #crud_<%=rnd%> .crud-lookup-results{display:none;position:absolute;background:#fff;border:1px solid #dbe3ef;border-radius:12px;box-shadow:0 14px 34px rgba(15,23,42,.18);margin-top:5px;max-height:230px;overflow:auto;z-index:10;width:100%;}
        #crud_<%=rnd%> .crud-lookup{position:relative;}
        #crud_<%=rnd%> .crud-lookup-item{padding:9px 11px;border-bottom:1px solid #eef2f7;cursor:pointer;}
        #crud_<%=rnd%> .crud-lookup-item:hover{background:#eff6ff;}
        #crud_<%=rnd%> .crud-alert{border-radius:14px;padding:11px 12px;margin-bottom:12px;font-size:13px;display:none;}
        #crud_<%=rnd%> .crud-alert.show{display:block;}
        #crud_<%=rnd%> .crud-alert.ok{background:#dcfce7;color:#166534;border:1px solid #bbf7d0;}
        #crud_<%=rnd%> .crud-alert.err{background:#fee2e2;color:#991b1b;border:1px solid #fecaca;}
        #crud_<%=rnd%> .crud-empty{text-align:center;padding:34px 20px;color:#64748b;}
        #crud_<%=rnd%> .crud-spinner{display:inline-block;width:16px;height:16px;border:2px solid #bfdbfe;border-top-color:var(--crud-primary);border-radius:999px;animation:crudspin .8s linear infinite;vertical-align:middle;margin-right:7px;}
        #crud_<%=rnd%> .crud-banbox{display:flex;gap:6px;align-items:center;}
        #crud_<%=rnd%> .crud-banbox .crud-control{flex:1;}
        #crud_<%=rnd%> .crud-lock-info{background:var(--theme-light,#eff6ff);border:1px solid var(--crud-primary);color:var(--theme-primary-dark,#1d4ed8);border-radius:12px;padding:9px 11px;font-weight:700;font-size:12px;}
        #crud_<%=rnd%> .crud-mini-btn{border:1px solid #dbe3ef;background:#fff;border-radius:10px;padding:8px 10px;cursor:pointer;color:var(--crud-primary);font-weight:800;}
        #crud_<%=rnd%> .crud-mini-btn:hover{background:var(--theme-light,#eff6ff);}
        #crud_<%=rnd%> .crud-lookup-table th{background:#f8fafc;position:sticky;top:0;z-index:1;}
        #crud_<%=rnd%> .crud-lookup-table td,#crud_<%=rnd%> .crud-lookup-table th{padding:10px;border-bottom:1px solid #eef2f7;}
        @keyframes crudspin{to{transform:rotate(360deg)}}
        @media(max-width:700px){#crud_<%=rnd%> .crud-modal-backdrop{padding:8px;}#crud_<%=rnd%> .crud-modal{margin:8px auto;border-radius:18px;max-height:calc(100vh - 16px);}#crud_<%=rnd%> .crud-modal-body{max-height:calc(100vh - 120px);}#crud_<%=rnd%> .crud-form-grid{grid-template-columns:1fr;}#crud_<%=rnd%> .crud-title{align-items:flex-start;} }

        /* Popup global: dipasang ke <body> supaya tidak tertutup header/navbar fixed */
        .ais-crud-global-<%=rnd%>.crud-modal-backdrop{display:none;position:fixed!important;left:0!important;top:0!important;right:0!important;bottom:0!important;width:100vw!important;height:100vh!important;background:rgba(15,23,42,.68)!important;z-index:2147483000!important;padding:24px!important;overflow:auto!important;overscroll-behavior:contain!important;isolation:isolate!important;box-sizing:border-box!important;}
        .ais-crud-global-<%=rnd%>.crud-modal-backdrop.show{display:block!important;}
        .ais-crud-global-<%=rnd%> .crud-modal{position:relative!important;z-index:2147483001!important;width:min(1120px,100%)!important;max-width:1120px!important;max-height:calc(100vh - 48px)!important;margin:20px auto!important;background:#fff!important;border-radius:24px!important;box-shadow:0 24px 90px rgba(0,0,0,.45)!important;overflow:hidden!important;display:flex!important;flex-direction:column!important;}
        .ais-crud-global-<%=rnd%> .crud-modal-head{padding:17px 20px!important;background:var(--theme-gradient,linear-gradient(135deg,#0f172a,#1d4ed8))!important;color:#fff!important;display:flex!important;align-items:center!important;justify-content:space-between!important;gap:12px!important;flex:0 0 auto!important;}
        .ais-crud-global-<%=rnd%> .crud-modal-head h4{margin:0!important;font-weight:800!important;font-size:18px!important;color:#fff!important;background:transparent!important;}
        .ais-crud-global-<%=rnd%> .crud-close{border:0!important;background:rgba(255,255,255,.16)!important;color:#fff!important;border-radius:12px!important;width:36px!important;height:36px!important;font-size:20px!important;cursor:pointer!important;}
        .ais-crud-global-<%=rnd%> .crud-modal-body{padding:16px 18px!important;max-height:calc(100vh - 150px)!important;overflow:auto!important;background:#f8fafc!important;flex:1 1 auto!important;overscroll-behavior:contain!important;-webkit-overflow-scrolling:touch!important;}
        .ais-crud-global-<%=rnd%> .crud-tabs{display:flex!important;gap:8px!important;flex-wrap:wrap!important;margin-bottom:12px!important;}
        .ais-crud-global-<%=rnd%> .crud-tab{border:1px solid #dbe3ef!important;background:#fff!important;border-radius:999px!important;padding:9px 13px!important;font-weight:800!important;color:#475569!important;cursor:pointer!important;font-size:13px!important;}
        .ais-crud-global-<%=rnd%> .crud-tab.active{background:var(--theme-gradient,#2563eb)!important;color:var(--theme-text-on-primary,#fff)!important;border-color:var(--theme-primary,#2563eb)!important;box-shadow:0 8px 18px rgba(37,99,235,.24)!important;}
        .ais-crud-global-<%=rnd%> .crud-step-card{display:none!important;background:#fff!important;border:1px solid #e2e8f0!important;border-radius:18px!important;padding:14px!important;margin-bottom:12px!important;}
        .ais-crud-global-<%=rnd%> .crud-step-card.active{display:block!important;}
        .ais-crud-global-<%=rnd%> .crud-step-title{display:flex!important;align-items:center!important;gap:9px!important;margin:0 0 12px!important;color:#0f172a!important;font-size:15px!important;font-weight:900!important;}
        .ais-crud-global-<%=rnd%> .crud-step-dot{width:26px!important;height:26px!important;border-radius:999px!important;background:#eff6ff!important;color:#1d4ed8!important;display:inline-flex!important;align-items:center!important;justify-content:center!important;font-size:12px!important;font-weight:900!important;}
        .ais-crud-global-<%=rnd%> .crud-form-grid{display:grid!important;grid-template-columns:repeat(auto-fit,minmax(240px,1fr))!important;gap:12px!important;}
        .ais-crud-global-<%=rnd%> .crud-form-grid .wide{grid-column:1/-1!important;}
        .ais-crud-global-<%=rnd%> .crud-required{color:#dc2626!important;}
        .ais-crud-global-<%=rnd%> .crud-field label{display:block!important;font-size:12px!important;font-weight:700!important;color:#475569!important;margin-bottom:5px!important;}
        .ais-crud-global-<%=rnd%> .crud-control{width:100%!important;border:1px solid #dbe3ef!important;border-radius:12px!important;padding:9px 10px!important;font-size:13px!important;outline:none!important;background:#fff!important;box-sizing:border-box!important;}
        .ais-crud-global-<%=rnd%> .crud-control:focus{border-color:var(--theme-primary,#2563eb)!important;box-shadow:0 0 0 4px rgba(59,130,246,.12)!important;}
        .ais-crud-global-<%=rnd%> .crud-switch{display:flex!important;align-items:center!important;gap:8px!important;padding:10px!important;border:1px solid #dbe3ef!important;border-radius:12px!important;background:#fff!important;}
        .ais-crud-global-<%=rnd%> .crud-banbox{display:flex!important;gap:6px!important;align-items:stretch!important;}
        .ais-crud-global-<%=rnd%> .crud-banbox input{flex:1!important;}
        .ais-crud-global-<%=rnd%> .crud-mini-btn{border:1px solid #dbe3ef!important;background:#fff!important;border-radius:12px!important;min-width:42px!important;color:var(--theme-primary,#2563eb)!important;cursor:pointer!important;}
        .ais-crud-global-<%=rnd%> .crud-table-wrap{border:1px solid #e2e8f0!important;border-radius:18px!important;overflow:auto!important;background:#fff!important;}
        .ais-crud-global-<%=rnd%> table.crud-table{width:100%!important;border-collapse:separate!important;border-spacing:0!important;font-size:13px!important;}
        .ais-crud-global-<%=rnd%> .crud-table thead th{position:sticky!important;top:0!important;background:#f8fafc!important;color:#475569!important;text-transform:uppercase!important;font-size:11px!important;letter-spacing:.04em!important;border-bottom:1px solid #e2e8f0!important;padding:12px!important;text-align:left!important;white-space:nowrap!important;z-index:1!important;}
        .ais-crud-global-<%=rnd%> .crud-table tbody td{padding:12px!important;border-bottom:1px solid #eef2f7!important;vertical-align:top!important;color:#1e293b!important;}
        .ais-crud-global-<%=rnd%> .crud-photo-col{width:72px!important;text-align:center!important;}
        .ais-crud-global-<%=rnd%> .crud-photo-cell{width:72px!important;text-align:center!important;vertical-align:middle!important;}
        .ais-crud-global-<%=rnd%> .crud-photo-thumb{width:46px!important;height:46px!important;border-radius:16px!important;object-fit:cover!important;border:2px solid rgba(255,255,255,.95)!important;box-shadow:0 7px 18px rgba(15,23,42,.18)!important;background:#eef2f7!important;cursor:pointer!important;}
        .ais-crud-global-<%=rnd%> .crud-toolbar{display:flex!important;align-items:center!important;justify-content:space-between!important;gap:12px!important;flex-wrap:wrap!important;margin-bottom:12px!important;}
        .ais-crud-global-<%=rnd%> .crud-search-main{position:relative!important;min-width:260px!important;flex:1!important;max-width:520px!important;}
        .ais-crud-global-<%=rnd%> .crud-search-main input{width:100%!important;border:1px solid #e2e8f0!important;border-radius:14px!important;padding:11px 14px 11px 38px!important;outline:none!important;background:#fff!important;font-size:13px!important;box-sizing:border-box!important;}
        .ais-crud-global-<%=rnd%> .crud-search-main span{position:absolute!important;left:14px!important;top:50%!important;transform:translateY(-50%)!important;color:#64748b!important;}
        .ais-crud-global-<%=rnd%> .crud-alert{border-radius:14px!important;padding:11px 12px!important;margin-bottom:12px!important;font-size:13px!important;display:none!important;}
        .ais-crud-global-<%=rnd%> .crud-alert.show{display:block!important;}
        .ais-crud-global-<%=rnd%> .crud-alert.ok{background:#dcfce7!important;color:#166534!important;border:1px solid #bbf7d0!important;}
        .ais-crud-global-<%=rnd%> .crud-alert.err{background:#fee2e2!important;color:#991b1b!important;border:1px solid #fecaca!important;}
        /* Progress download/upload sekarang hanya menggunakan popup blocking. Komponen progress inline sengaja dimatikan agar tidak muncul/nyangkut di atas tabel. */
        .ais-crud-global-<%=rnd%> .crud-progress-box,
        .ais-crud-global-<%=rnd%> .crud-progress-box.show,
        #crudDownloadProgress_<%=rnd%>{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;margin:0!important;padding:0!important;border:0!important;overflow:hidden!important;}
        .ais-crud-global-<%=rnd%> .crud-progress-title,
        .ais-crud-global-<%=rnd%> .crud-progress-track,
        .ais-crud-global-<%=rnd%> .crud-progress-bar{display:none!important;}
        .ais-crud-global-<%=rnd%> .crud-process-modal{max-width:520px!important;border-radius:22px!important;}
        .ais-crud-global-<%=rnd%> .crud-process-icon{width:54px!important;height:54px!important;border-radius:18px!important;background:var(--theme-light,#eff6ff)!important;color:var(--theme-primary,#2563eb)!important;display:flex!important;align-items:center!important;justify-content:center!important;font-size:24px!important;margin:0 auto 12px!important;}
        .ais-crud-global-<%=rnd%> .crud-process-title{text-align:center!important;font-size:18px!important;font-weight:900!important;color:#0f172a!important;margin-bottom:6px!important;}
        .ais-crud-global-<%=rnd%> .crud-process-message{text-align:center!important;color:#475569!important;font-size:13px!important;margin-bottom:14px!important;line-height:1.45!important;}
        .ais-crud-global-<%=rnd%> .crud-process-sub{text-align:center!important;color:#64748b!important;font-size:12px!important;margin-top:10px!important;}
        .ais-crud-global-<%=rnd%> .crud-empty{text-align:center!important;padding:34px 20px!important;color:#64748b!important;}
        .ais-crud-global-<%=rnd%> .crud-actions{display:flex!important;gap:8px!important;flex-wrap:wrap!important;align-items:center!important;}
        .ais-crud-global-<%=rnd%> textarea.crud-control{min-height:92px!important;line-height:1.5!important;resize:vertical!important;}
        .ais-crud-global-<%=rnd%> .crud-lock-info{border:1px dashed var(--theme-primary,#2563eb)!important;background:var(--theme-light,#eff6ff)!important;color:var(--theme-primary-dark,#1e40af)!important;border-radius:14px!important;padding:10px 12px!important;font-weight:700!important;font-size:12px!important;}
        .ais-crud-global-<%=rnd%> .crud-btn{border:0!important;border-radius:12px!important;padding:9px 13px!important;font-size:13px!important;font-weight:700!important;cursor:pointer!important;display:inline-flex!important;align-items:center!important;gap:7px!important;text-decoration:none!important;line-height:1.2!important;}
        .ais-crud-global-<%=rnd%> .crud-btn-primary{background:var(--theme-gradient,#2563eb)!important;color:var(--theme-text-on-primary,#fff)!important;}
        .ais-crud-global-<%=rnd%> .crud-btn-gray{background:#f1f5f9!important;color:#334155!important;border:1px solid #e2e8f0!important;}
        @media(max-width:768px){.ais-crud-global-<%=rnd%>.crud-modal-backdrop{padding:8px!important}.ais-crud-global-<%=rnd%> .crud-modal{margin:4px auto!important;max-height:calc(100vh - 16px)!important}.ais-crud-global-<%=rnd%> .crud-modal-body{max-height:calc(100vh - 120px)!important}.ais-crud-global-<%=rnd%> .crud-form-grid{grid-template-columns:1fr!important;}}



        /* ============================================================
           MODERN UI/UX ENHANCEMENT v24
           - Header lebih tegas dan modern
           - Row ganjil/genap dibedakan jelas
           - Table lebih nyaman dibaca
           - Tombol dan filter lebih kekinian
           ============================================================ */
        #crud_<%=rnd%>.ais-crud-shell{--crud-row-odd:#ffffff;--crud-row-even:#f8fbff;--crud-row-hover:var(--theme-light,rgba(37,99,235,.08));--crud-header-glass:rgba(255,255,255,.16);--crud-card-shadow:0 20px 52px rgba(15,23,42,.10);}
        #crud_<%=rnd%> .crud-card{border-radius:28px;border:1px solid rgba(148,163,184,.18);box-shadow:var(--crud-card-shadow);background:linear-gradient(180deg,#fff 0%,#f8fbff 100%);}
        #crud_<%=rnd%> .crud-hero{padding:24px 26px 23px;min-height:76px;background:var(--theme-gradient,linear-gradient(135deg,#123c7a,#2563eb));border-bottom:1px solid rgba(255,255,255,.14);}
        #crud_<%=rnd%> .crud-hero:before{content:"";position:absolute;inset:0;background:radial-gradient(circle at 18% -40%,rgba(255,255,255,.30),transparent 28%),radial-gradient(circle at 95% 8%,rgba(255,255,255,.18),transparent 26%),linear-gradient(135deg,rgba(255,255,255,.04),rgba(255,255,255,0));pointer-events:none;}
        #crud_<%=rnd%> .crud-hero:after{right:-46px;top:-70px;width:260px;height:260px;background:rgba(255,255,255,.13);box-shadow:-60px 80px 0 rgba(255,255,255,.06);}
        #crud_<%=rnd%> .crud-title{position:relative;z-index:2;align-items:center;}
        #crud_<%=rnd%> .crud-title h3{display:flex;align-items:center;gap:11px;font-size:22px;font-weight:900;letter-spacing:.35px;text-shadow:var(--theme-text-shadow,0 1px 2px rgba(0,0,0,.20));}
        #crud_<%=rnd%> .crud-title h3:before{content:"\f1c0";font-family:"Font Awesome 5 Free";font-weight:900;width:36px;height:36px;border-radius:14px;background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.22);display:inline-flex;align-items:center;justify-content:center;font-size:15px;box-shadow:inset 0 1px 0 rgba(255,255,255,.24),0 8px 18px rgba(0,0,0,.14);}
        #crud_<%=rnd%> .crud-subtitle{font-size:12.5px;font-weight:600;color:rgba(255,255,255,.86);letter-spacing:.02em;}
        #crud_<%=rnd%> .crud-actions{gap:9px;}
        #crud_<%=rnd%> .crud-btn{border-radius:14px;padding:10px 14px;font-size:13px;font-weight:800;box-shadow:0 1px 0 rgba(255,255,255,.18) inset;}
        #crud_<%=rnd%> .crud-btn-soft{background:rgba(255,255,255,.18);border:1px solid rgba(255,255,255,.34);backdrop-filter:blur(8px);}
        #crud_<%=rnd%> .crud-btn-primary{background:#fff;color:var(--theme-primary-dark,#1d4ed8);border:1px solid rgba(255,255,255,.55);box-shadow:0 10px 26px rgba(15,23,42,.18);}
        #crud_<%=rnd%> .crud-btn i{font-size:13px;}
        #crud_<%=rnd%> .crud-body{padding:22px;background:linear-gradient(180deg,#ffffff 0%,#f8fbff 55%,#f4f8ff 100%);}
        #crud_<%=rnd%> .crud-toolbar{margin-bottom:15px;}
        #crud_<%=rnd%> .crud-search-main input{height:46px;border-radius:18px;border:1px solid rgba(148,163,184,.35);background:rgba(255,255,255,.92);box-shadow:0 8px 22px rgba(15,23,42,.045);font-weight:600;transition:.18s;}
        #crud_<%=rnd%> .crud-search-main input:focus{background:#fff;border-color:var(--theme-primary,#2563eb);box-shadow:0 0 0 4px rgba(37,99,235,.10),0 10px 26px rgba(15,23,42,.08);}
        #crud_<%=rnd%> .crud-search-main span{font-size:17px;color:var(--theme-primary,#2563eb);}
        #crud_<%=rnd%> .crud-control{border-radius:14px;border:1px solid rgba(148,163,184,.35);background:#fff;font-weight:600;}
        #crud_<%=rnd%> .crud-filter-grid{background:linear-gradient(180deg,#ffffff,#f8fbff);border:1px solid rgba(148,163,184,.25);box-shadow:0 14px 28px rgba(15,23,42,.06);}
        #crud_<%=rnd%> .crud-table-wrap{border-radius:22px;border:1px solid rgba(148,163,184,.22);box-shadow:0 12px 30px rgba(15,23,42,.055);overflow:auto;background:#fff;}
        #crud_<%=rnd%> table.crud-table{font-size:13px;}
        #crud_<%=rnd%> .crud-table thead th{background:linear-gradient(180deg,#f8fbff 0%,#f1f6ff 100%);color:#334155;font-weight:900;border-bottom:1px solid rgba(148,163,184,.25);padding:14px 13px;letter-spacing:.06em;}
        #crud_<%=rnd%> .crud-table tbody tr:nth-child(odd){background:var(--crud-row-odd);}
        #crud_<%=rnd%> .crud-table tbody tr:nth-child(even){background:var(--crud-row-even);}
        #crud_<%=rnd%> .crud-table tbody tr{transition:background .16s,box-shadow .16s,transform .16s;}
        #crud_<%=rnd%> .crud-table tbody tr:hover{background:var(--crud-row-hover);box-shadow:inset 4px 0 0 var(--theme-primary,#2563eb);}
        #crud_<%=rnd%> .crud-table tbody td{padding:15px 13px;border-bottom:1px solid rgba(226,232,240,.75);line-height:1.55;color:#172033;}
        #crud_<%=rnd%> .crud-table tbody tr:last-child td{border-bottom:0;}
        #crud_<%=rnd%> .crud-badge{padding:5px 10px;font-weight:900;background:linear-gradient(180deg,#e0f2fe,#dbeafe);color:#075985;box-shadow:0 4px 12px rgba(14,165,233,.12);}
        #crud_<%=rnd%> .crud-badge.no{background:linear-gradient(180deg,#fee2e2,#fecaca);color:#b91c1c;}
        #crud_<%=rnd%> .crud-icon-btn{border-radius:13px;border:1px solid rgba(148,163,184,.28);background:#fff;box-shadow:0 6px 14px rgba(15,23,42,.045);transition:.16s;}
        #crud_<%=rnd%> .crud-icon-btn:hover{background:var(--theme-gradient,#2563eb);color:var(--theme-text-on-primary,#fff);border-color:transparent;transform:translateY(-1px);}
        #crud_<%=rnd%> .crud-footer{margin-top:16px;padding-top:12px;border-top:1px dashed rgba(148,163,184,.35);font-weight:700;}
        #crud_<%=rnd%> .crud-page{border-radius:12px;padding:8px 11px;font-weight:800;box-shadow:0 4px 10px rgba(15,23,42,.04);}
        #crud_<%=rnd%> .crud-page.active{box-shadow:0 10px 22px rgba(37,99,235,.22);}
        #crud_<%=rnd%> .crud-empty{background:linear-gradient(180deg,#fff,#f8fbff);border-radius:18px;margin:12px;color:#64748b;}
        #crud_<%=rnd%> .crud-modal{border-radius:28px;border:1px solid rgba(255,255,255,.18);}
        #crud_<%=rnd%> .crud-modal-head{padding:19px 22px;box-shadow:0 12px 26px rgba(15,23,42,.14);}
        #crud_<%=rnd%> .crud-step-card{border-radius:22px;box-shadow:0 12px 30px rgba(15,23,42,.055);}
        #crud_<%=rnd%> .crud-step-title{font-size:16px;}
        #crud_<%=rnd%> .crud-step-dot{background:var(--theme-light,#eff6ff);color:var(--theme-primary-dark,#1d4ed8);}


        #crud_<%=rnd%> .crud-view-tabs{display:flex;align-items:center;gap:10px;margin:0 0 16px 0;padding:7px;background:rgba(241,245,249,.78);border:1px solid rgba(148,163,184,.22);border-radius:18px;width:max-content;box-shadow:0 10px 24px rgba(15,23,42,.045);}
        #crud_<%=rnd%> .crud-view-tab{border:0;background:transparent;border-radius:13px;padding:10px 15px;font-size:13px;font-weight:900;color:#475569;display:inline-flex;align-items:center;gap:8px;transition:.16s;}
        #crud_<%=rnd%> .crud-view-tab:hover{background:#fff;color:var(--theme-primary-dark,#1d4ed8);box-shadow:0 8px 16px rgba(15,23,42,.05);}
        #crud_<%=rnd%> .crud-view-tab.active{background:var(--theme-gradient,#2563eb);color:var(--theme-text-on-primary,#fff);box-shadow:0 12px 28px rgba(37,99,235,.22);}
        #crud_<%=rnd%> .crud-stats-hero{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:0 0 18px 0;padding:18px 20px;border-radius:22px;background:linear-gradient(135deg,rgba(255,255,255,.96),rgba(239,246,255,.90));border:1px solid rgba(148,163,184,.20);box-shadow:0 14px 32px rgba(15,23,42,.06);}
        #crud_<%=rnd%> .crud-stats-hero h4{margin:0;font-size:18px;font-weight:950;color:#0f172a;letter-spacing:.01em;}
        #crud_<%=rnd%> .crud-stats-hero p{margin:3px 0 0 0;color:#64748b;font-size:12px;font-weight:700;}
        #crud_<%=rnd%> .crud-stats-cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:14px;margin-bottom:16px;}
        #crud_<%=rnd%> .crud-stat-card{background:#fff;border:1px solid rgba(148,163,184,.20);border-radius:22px;padding:17px;box-shadow:0 16px 34px rgba(15,23,42,.055);position:relative;overflow:hidden;}
        #crud_<%=rnd%> .crud-stat-card:after{content:"";position:absolute;right:-34px;top:-34px;width:92px;height:92px;border-radius:50%;background:var(--theme-light,rgba(37,99,235,.10));}
        #crud_<%=rnd%> .crud-stat-card .icon{width:42px;height:42px;border-radius:16px;display:flex;align-items:center;justify-content:center;background:var(--theme-gradient,#2563eb);color:var(--theme-text-on-primary,#fff);box-shadow:0 10px 24px rgba(15,23,42,.15);}
        #crud_<%=rnd%> .crud-stat-card .value{font-size:26px;font-weight:950;color:#0f172a;margin-top:12px;}
        #crud_<%=rnd%> .crud-stat-card .label{font-size:12px;font-weight:800;color:#64748b;text-transform:uppercase;letter-spacing:.06em;}
        #crud_<%=rnd%> .crud-stat-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(360px,1fr));gap:16px;}
        #crud_<%=rnd%> .crud-stat-panel{background:#fff;border:1px solid rgba(148,163,184,.20);border-radius:24px;box-shadow:0 16px 38px rgba(15,23,42,.06);overflow:hidden;}
        #crud_<%=rnd%> .crud-stat-panel-head{padding:16px 18px;background:linear-gradient(180deg,#f8fbff,#f1f6ff);border-bottom:1px solid rgba(148,163,184,.22);display:flex;align-items:center;justify-content:space-between;gap:12px;}
        #crud_<%=rnd%> .crud-stat-panel-head h5{margin:0;font-size:15px;font-weight:950;color:#0f172a;}
        #crud_<%=rnd%> .crud-stat-panel-head span{font-size:11px;font-weight:900;color:#64748b;background:#fff;border:1px solid rgba(148,163,184,.20);border-radius:999px;padding:5px 9px;}
        #crud_<%=rnd%> .crud-stat-panel-body{padding:16px;}
        #crud_<%=rnd%> .crud-stat-chart{height:220px;margin-bottom:12px;position:relative;}
        #crud_<%=rnd%> .crud-stat-table{width:100%;font-size:12px;border-collapse:collapse;}
        #crud_<%=rnd%> .crud-stat-table td{padding:9px 8px;border-bottom:1px dashed rgba(148,163,184,.25);font-weight:700;color:#334155;}
        #crud_<%=rnd%> .crud-stat-table td:last-child{text-align:right;color:#0f172a;font-weight:950;}
        #crud_<%=rnd%> .crud-stat-empty{padding:28px;border-radius:18px;background:#f8fafc;border:1px dashed rgba(148,163,184,.35);color:#64748b;text-align:center;font-weight:800;}
        #crud_<%=rnd%> .crud-stat-loading{padding:34px 28px;border-radius:22px;background:linear-gradient(135deg,rgba(255,255,255,.98),rgba(239,246,255,.92));border:1px solid rgba(148,163,184,.22);box-shadow:0 18px 45px rgba(15,23,42,.08);display:flex;align-items:center;gap:18px;margin:10px 0 18px 0;}
        #crud_<%=rnd%> .crud-stat-loading-icon{width:62px;height:62px;border-radius:20px;background:var(--theme-gradient,#2563eb);color:var(--theme-text-on-primary,#fff);display:flex;align-items:center;justify-content:center;font-size:28px;box-shadow:0 14px 30px rgba(37,99,235,.25);}
        #crud_<%=rnd%> .crud-stat-loading-title{font-size:17px;font-weight:950;color:#0f172a;margin-bottom:4px;}
        #crud_<%=rnd%> .crud-stat-loading-desc{font-size:13px;font-weight:700;color:#64748b;line-height:1.45;}
        #crud_<%=rnd%> .crud-stat-note{margin-bottom:12px;padding:9px 12px;border-radius:12px;background:#eff6ff;border:1px solid rgba(59,130,246,.20);color:#1e40af;font-size:12px;font-weight:800;}

        /* Override untuk modal yang dipindah ke document.body */
        .ais-crud-global-<%=rnd%>.crud-modal-backdrop{background:rgba(15,23,42,.72)!important;backdrop-filter:blur(5px)!important;}
        .ais-crud-global-<%=rnd%> .crud-modal{border-radius:28px!important;border:1px solid rgba(255,255,255,.18)!important;box-shadow:0 28px 100px rgba(0,0,0,.48)!important;}
        .ais-crud-global-<%=rnd%> .crud-modal-head{padding:19px 22px!important;background:var(--theme-gradient,linear-gradient(135deg,#123c7a,#2563eb))!important;box-shadow:0 12px 26px rgba(15,23,42,.14)!important;}
        .ais-crud-global-<%=rnd%> .crud-table tbody tr:nth-child(odd){background:#fff!important;}
        .ais-crud-global-<%=rnd%> .crud-table tbody tr:nth-child(even){background:#f8fbff!important;}
        .ais-crud-global-<%=rnd%> .crud-table tbody tr:hover{background:var(--theme-light,rgba(37,99,235,.08))!important;box-shadow:inset 4px 0 0 var(--theme-primary,#2563eb)!important;}
        .ais-crud-global-<%=rnd%> .crud-table tbody td{padding:15px 13px!important;line-height:1.55!important;}
        .ais-crud-global-<%=rnd%> .crud-table thead th{background:linear-gradient(180deg,#f8fbff 0%,#f1f6ff 100%)!important;color:#334155!important;font-weight:900!important;padding:14px 13px!important;letter-spacing:.06em!important;}
        .ais-crud-global-<%=rnd%> .crud-btn{border-radius:14px!important;font-weight:800!important;}
        .ais-crud-global-<%=rnd%> .crud-btn-primary{background:var(--theme-gradient,#2563eb)!important;color:var(--theme-text-on-primary,#fff)!important;box-shadow:0 10px 26px rgba(15,23,42,.18)!important;}

    </style>

    <div class="crud-card">
        <div class="crud-hero">
            <div class="crud-title">
                <div>
                    <h3 id="crudTitle_<%=rnd%>">Data</h3>
                    <div class="crud-subtitle" id="crudSubtitle_<%=rnd%>">Kelola data dengan tampilan ringkas, pencarian cepat, dan form bertahap.</div>
                </div>
                <div class="crud-actions">
                    <button type="button" id="crudDownloadBtn_<%=rnd%>" class="crud-btn crud-btn-soft" onclick="CRUD_<%=rnd%>.downloadExcel()" title="Download data Excel sesuai filter aktif"><i class="fas fa-file-download"></i> Download Excel</button>
                    <button type="button" id="crudUploadBtn_<%=rnd%>" class="crud-btn crud-btn-soft" onclick="CRUD_<%=rnd%>.chooseUploadExcel()" title="Upload Excel membutuhkan hak Tambah, Ubah, dan Hapus"><i class="fas fa-file-upload"></i> Upload Excel</button>
                    <button type="button" class="crud-btn crud-btn-soft" onclick="CRUD_<%=rnd%>.toggleFilter()"><i class="fas fa-filter"></i> Filter</button>
                    <button type="button" class="crud-btn crud-btn-soft" onclick="CRUD_<%=rnd%>.refresh()"><i class="fas fa-sync-alt"></i> Refresh</button>
                    <input type="file" id="crudUploadFile_<%=rnd%>" accept=".xlsx" style="display:none" onchange="CRUD_<%=rnd%>.uploadExcel(this)" />
                    <button type="button" id="crudAddBtn_<%=rnd%>" class="crud-btn crud-btn-primary" onclick="CRUD_<%=rnd%>.openForm('add')"><i class="fas fa-plus"></i> Tambah</button>
                </div>
            </div>
        </div>
        <div class="crud-body">
            <div id="crudAlert_<%=rnd%>" class="crud-alert"></div>
            <div id="crudContextInfo_<%=rnd%>" class="crud-lock-info" style="display:none;margin-bottom:12px;"></div>
            <div id="crudViewTabs_<%=rnd%>" class="crud-view-tabs" style="display:none;">
                <button type="button" id="crudViewTabData_<%=rnd%>" class="crud-view-tab active" onclick="CRUD_<%=rnd%>.switchMainView('data')"><i class="fas fa-table"></i> Data</button>
                <button type="button" id="crudViewTabStats_<%=rnd%>" class="crud-view-tab" onclick="CRUD_<%=rnd%>.switchMainView('stats')"><i class="fas fa-chart-pie"></i> Statistik</button>
            </div>
            <div id="crudDataView_<%=rnd%>">
            <div class="crud-toolbar">
                <div class="crud-search-main">
                    <span>⌕</span>
                    <input type="text" id="crudKeyword_<%=rnd%>" placeholder="Cari cepat..." onkeyup="CRUD_<%=rnd%>.keywordChanged(event)" />
                </div>
                <div class="crud-actions">
                    <select id="crudSize_<%=rnd%>" class="crud-control" style="width:120px" onchange="CRUD_<%=rnd%>.changeSize()">
                        <option value="10">10 / halaman</option>
                        <option value="20" selected>20 / halaman</option>
                        <option value="50">50 / halaman</option>
                        <option value="100">100 / halaman</option>
                    </select>
                </div>
            </div>
            <div id="crudFilters_<%=rnd%>" class="crud-filter-grid"></div>
            <div class="crud-table-wrap">
                <table class="crud-table">
                    <thead id="crudThead_<%=rnd%>"></thead>
                    <tbody id="crudTbody_<%=rnd%>"></tbody>
                </table>
            </div>
            <div class="crud-footer">
                <div id="crudInfo_<%=rnd%>">Memuat data...</div>
                <div id="crudPages_<%=rnd%>" class="crud-pages"></div>
            </div>
            </div>
            <div id="crudStatsView_<%=rnd%>" class="crud-stats-view" style="display:none;">
                <div class="crud-stats-hero">
                    <div>
                        <h4><i class="fas fa-chart-line"></i> Statistik Data</h4>
                        <p>Rekap otomatis berdasarkan relasi, field pilihan, dan tren tanggal perubahan data.</p>
                    </div>
                    <button type="button" class="crud-btn crud-btn-primary" onclick="CRUD_<%=rnd%>.loadStats(true)"><i class="fas fa-sync-alt"></i> Muat Ulang Statistik</button>
                </div>
                <div id="crudStatsContent_<%=rnd%>"><div class="crud-stat-empty"><i class="fas fa-chart-pie"></i> Klik tab Statistik untuk memuat rekap data.</div></div>
            </div>
        </div>
    </div>

    <div id="crudModal_<%=rnd%>" class="crud-modal-backdrop">
        <div class="crud-modal">
            <div class="crud-modal-head">
                <h4 id="crudModalTitle_<%=rnd%>">Form Data</h4>
                <button type="button" class="crud-close" onclick="CRUD_<%=rnd%>.closeForm()">×</button>
            </div>
            <div class="crud-modal-body" tabindex="-1">
                <div id="crudFormAlert_<%=rnd%>" class="crud-alert"></div>
                <input type="hidden" id="crudCurrentId_<%=rnd%>" />
                <div id="crudTabs_<%=rnd%>" class="crud-tabs"></div>
                <form id="crudForm_<%=rnd%>" onsubmit="CRUD_<%=rnd%>.save(event)">
                    <div id="crudFormArea_<%=rnd%>"></div>
                    <div class="crud-actions" style="justify-content:flex-end; margin-top:14px;">
                        <button type="button" class="crud-btn crud-btn-gray" onclick="CRUD_<%=rnd%>.closeForm()"><%=Common.getBahasaConfig("Batal")%></button>
                        <button type="submit" class="crud-btn crud-btn-primary" style="background:var(--theme-gradient,#2563eb);color:var(--theme-text-on-primary,#fff);"><%=Common.getBahasaConfig("Simpan Data")%></button>
                    </div>
                </form>
            </div>
        </div>
    </div>



    <div id="crudLookupModal_<%=rnd%>" class="crud-modal-backdrop">
        <div class="crud-modal" style="max-width:920px;">
            <div class="crud-modal-head">
                <h4 id="crudLookupTitle_<%=rnd%>">Pilih Data</h4>
                <button type="button" class="crud-close" onclick="CRUD_<%=rnd%>.closeLookupPopup()">×</button>
            </div>
            <div class="crud-modal-body">
                <div class="crud-toolbar">
                    <div class="crud-search-main" style="max-width:none;">
                        <span><i class="fas fa-search"></i></span>
                        <input type="text" id="crudLookupKeyword_<%=rnd%>" placeholder="Ketik minimal 1 huruf untuk mencari..." onkeyup="CRUD_<%=rnd%>.lookupPopupTyping(event)" />
                    </div>
                </div>
                <div id="crudLookupInfo_<%=rnd%>" class="crud-lock-info" style="display:none;margin-bottom:10px;"></div>
                <div class="crud-table-wrap">
                    <table class="crud-table crud-lookup-table">
                        <thead id="crudLookupThead_<%=rnd%>"></thead>
                        <tbody id="crudLookupTbody_<%=rnd%>"></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>



    <div id="crudProcessModal_<%=rnd%>" class="crud-modal-backdrop" data-crud-process="true">
        <div class="crud-modal crud-process-modal">
            <div class="crud-modal-body" style="padding:26px 24px;">
                <div class="crud-process-icon"><i id="crudProcessIcon_<%=rnd%>" class="fas fa-spinner fa-spin"></i></div>
                <div id="crudProcessTitle_<%=rnd%>" class="crud-process-title">Memproses Data</div>
                <div id="crudProcessMsg_<%=rnd%>" class="crud-process-message">Harap tunggu, proses sedang berjalan...</div>
                <div class="crud-progress-track"><div id="crudProcessBar_<%=rnd%>" class="crud-progress-bar"></div></div>
                <div id="crudProcessPct_<%=rnd%>" class="crud-process-sub">0%</div>
                <div class="crud-process-sub"><i class="fas fa-lock"></i> Menu lain dikunci sementara sampai proses selesai.</div>
            </div>
        </div>
    </div>

    <script type="application/json" id="crudConfigJson_<%=rnd%>"><%=configJson%></script>
    <script type="text/javascript">
    var CRUD_<%=rnd%> = (function(){
        var rnd = '<%=rnd%>';
        var apiUrl = '<%=apiUrl%>';
        var uploadUrl = '<%=uploadUrl%>';
        var rawConfigs = JSON.parse(document.getElementById('crudConfigJson_' + rnd).textContent || '[]');
        var meta = null;
        var page = 1;
        var size = 20;
        var sort = '';
        var order = '';
        var activeTab = 0;
        var currentData = {};
        var lookupTimers = {};
        var lookupPopupState = null;
        var lookupPopupOptions = [];
        var lookupPopupTimer = null;
        var statsLoaded = false;
        var statsCharts = {};
        var modalOpenCount = 0;
        var oldHtmlOverflow = '';
        var oldBodyOverflow = '';

        function $(id){ return document.getElementById(id); }
        function lockPageScroll(){
            try{
                if(modalOpenCount === 0){
                    oldHtmlOverflow = document.documentElement.style.overflow || '';
                    oldBodyOverflow = document.body.style.overflow || '';
                    document.documentElement.style.overflow = 'hidden';
                    document.body.style.overflow = 'hidden';
                }
                modalOpenCount++;
            }catch(e){}
        }
        function unlockPageScroll(){
            try{
                modalOpenCount--;
                if(modalOpenCount <= 0){
                    modalOpenCount = 0;
                    document.documentElement.style.overflow = oldHtmlOverflow;
                    document.body.style.overflow = oldBodyOverflow;
                }
            }catch(e){}
        }
        function ensureModalOnBody(id){
            var el = $(id); if(!el){ return null; }
            try{
                if(el.parentNode !== document.body){
                    if((' ' + el.className + ' ').indexOf(' ais-crud-global-' + rnd + ' ') < 0){
                        el.className += ' ais-crud-global-' + rnd;
                    }
                    document.body.appendChild(el);
                }
            }catch(e){}
            return el;
        }
        function showCrudModal(id, z){
            var el = ensureModalOnBody(id); if(!el){ return; }
            if(el.className.indexOf('show') < 0){ lockPageScroll(); }
            el.style.zIndex = z || '2147483000';
            if((' ' + el.className + ' ').indexOf(' ais-crud-global-' + rnd + ' ') < 0){ el.className += ' ais-crud-global-' + rnd; }
            if(el.className.indexOf('crud-modal-backdrop') < 0){ el.className += ' crud-modal-backdrop'; }
            if(el.className.indexOf('show') < 0){ el.className += ' show'; }
            var modal = el.querySelector ? el.querySelector('.crud-modal') : null;
            if(modal){ modal.style.zIndex = String(parseInt(el.style.zIndex || '2147483000', 10) + 1); }
            var body = el.querySelector ? el.querySelector('.crud-modal-body') : null;
            if(body){ try{ body.scrollTop = 0; body.focus(); }catch(e){} }
        }
        function hideCrudModal(id){
            var el = $(id); if(!el){ return; }
            if(el.className.indexOf('show') >= 0){ unlockPageScroll(); }
            el.className = 'crud-modal-backdrop ais-crud-global-' + rnd;
            el.style.zIndex = '';
        }
        function esc(v){ if(v === null || v === undefined || v === '') return ''; return String(v).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;'); }
        function defaultProfileImage(){ return '<%=request.getContextPath()%>/img/user_default.png'; }
        function previewProfileImage(url){ if(!url){ return; } try{ window.open(url, '_blank'); }catch(e){} }
        function enc(v){ return encodeURIComponent(v === null || v === undefined ? '' : v); }
        function labelize(field){ return field.replace(/([a-z])([A-Z])/g,'$1 $2').replace(/_/g,' ').replace(/^./, function(s){return s.toUpperCase();}); }
        function getFieldName(spec){ return (spec || '').split(';')[0]; }
        function getMeta(idx, field){ return meta && meta.configs[idx] && meta.configs[idx].fields[field] ? meta.configs[idx].fields[field] : {}; }
        function isRequired(idx, field){
            var req = (meta.configs[idx].required || []);
            for(var i=0;i<req.length;i++){ if(getFieldName(req[i]) === field){ return true; } }
            return false;
        }
        function locked(idx, field){ return meta && meta.configs[idx] && meta.configs[idx].lockedFields && meta.configs[idx].lockedFields[field] ? meta.configs[idx].lockedFields[field] : null; }
        function showContextInfo(){
            var info = (meta && meta.configs && meta.configs[0] && meta.configs[0].contextInfo) ? meta.configs[0].contextInfo : [];
            var el = $('crudContextInfo_' + rnd);
            if(el && info && info.length){ el.style.display='block'; el.innerHTML='<i class="fas fa-lock"></i> ' + esc(info.join(' • ')); }
        }
        function callApi(action, params, method){
            params = params || {};
            params.action = action;
            var url = apiUrl;
            var opt = { method: method || 'GET', credentials:'same-origin' };
            if(opt.method === 'GET'){
                var qs = [];
                for(var k in params){ if(params.hasOwnProperty(k)){ qs.push(enc(k)+'='+enc(params[k])); } }
                url += '&' + qs.join('&');
            } else {
                opt.headers = {'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'};
                var body = [];
                for(var b in params){ if(params.hasOwnProperty(b)){ body.push(enc(b)+'='+enc(params[b])); } }
                opt.body = body.join('&');
            }
            return fetch(url, opt).then(function(r){ return r.json(); });
        }
        function showAlert(targetId, msg, ok){
            var el = $(targetId); if(!el){ return; }
            el.className = 'crud-alert show ' + (ok ? 'ok' : 'err');
            el.innerHTML = esc(msg || '');
            if(ok){ setTimeout(function(){ el.className='crud-alert'; el.innerHTML=''; }, 2600); }
        }
        function applyToolbarPermissions(){
            var c = meta && meta.configs ? meta.configs[0] : {};
            var addBtn = $('crudAddBtn_' + rnd); if(addBtn){ addBtn.style.display = c.canCreate ? 'inline-flex' : 'none'; }
            var upBtn = $('crudUploadBtn_' + rnd); if(upBtn){ upBtn.style.display = c.canUpload ? 'inline-flex' : 'none'; }
            var downBtn = $('crudDownloadBtn_' + rnd); if(downBtn){ downBtn.style.display = (c.canDownload === false ? 'none' : 'inline-flex'); }
            var tabs = $('crudViewTabs_' + rnd); if(tabs){ tabs.style.display = c.canViewStatistics ? 'flex' : 'none'; }
        }
        function init(){
            callApi('meta', {}).then(function(j){
                if(j.status !== '00'){ showAlert('crudAlert_' + rnd, j.message, false); return; }
                meta = j;
                applyToolbarPermissions();
                showContextInfo();
                var title = meta.configs[0].dataTypeName || 'Data';
                $('crudTitle_' + rnd).innerHTML = 'Data ' + esc(title);
                buildHeader();
                buildFilters();
                buildFormShell();
                loadData();
            }).catch(function(e){ showAlert('crudAlert_' + rnd, e, false); });
        }
        function buildHeader(){
            var c = meta.configs[0];
            var cols = c.tableColumns || [];
            var labels = c.tableLabels || [];
            var h = '<tr>';
            if(c.hasProfileImage){ h += '<th class="crud-photo-col"><i class="fas fa-image"></i> Foto</th>'; }
            for(var i=0;i<cols.length;i++){
                var f = getFieldName(cols[i]);
                var lab = labels[i] || labelize(f);
                h += '<th class="crud-sort" onclick="CRUD_'+rnd+'.sortBy(\''+esc(f)+'\')">'+esc(lab)+' <span id="sort_'+rnd+'_'+esc(f)+'"></span></th>';
            }
            h += '<th style="text-align:right;width:112px"><%=Common.getBahasaConfig("Aksi")%></th></tr>';
            $('crudThead_' + rnd).innerHTML = h;
        }
        function buildFilters(){
            var c = meta.configs[0];
            var cols = c.searchCols || [];
            var labelsByField = {};
            for(var i=0;i<(c.tableColumns || []).length;i++){ labelsByField[getFieldName(c.tableColumns[i])] = (c.tableLabels || [])[i]; }
            var html = '';
            for(var x=0;x<cols.length;x++){
                var spec = cols[x]; var f = getFieldName(spec); var m = getMeta(0, f); var lab = labelsByField[f] || labelize(f);
                html += '<div class="crud-field"><label>'+esc(lab)+'</label>' + renderFilterInput(f, m) + '</div>';
            }
            $('crudFilters_' + rnd).innerHTML = html;
        }
        function renderFilterInput(f, m){
            var l = locked(0, f);
            if(l){
                return '<input type="hidden" id="f_'+rnd+'_'+esc(f)+'" value="'+esc(l.id)+'" />'
                    + '<div class="crud-lock-info"><i class="fas fa-lock"></i> '+esc(l.label)+'<br/><small>'+esc(l.message || 'Filter terkunci')+'</small></div>';
            }
            if(m.uiType === 'fk'){
                return renderLookupControl('filter', 0, f, m, false);
            }
            if(m.uiType === 'boolean'){
                return '<select id="f_'+rnd+'_'+esc(f)+'" class="crud-control" onchange="CRUD_'+rnd+'.refresh()"><option value="">Semua</option><option value="true">Ya</option><option value="false">Tidak</option></select>';
            }
            if(m.uiType === 'select'){
                var o = '<select id="f_'+rnd+'_'+esc(f)+'" class="crud-control" onchange="CRUD_'+rnd+'.refresh()"><option value="">Semua</option>';
                var opts = m.options || [];
                for(var i=0;i<opts.length;i++){ o += '<option value="'+esc(opts[i].id)+'">'+esc(opts[i].nama)+'</option>'; }
                return o + '</select>';
            }
            return '<input id="f_'+rnd+'_'+esc(f)+'" class="crud-control" type="text" placeholder="Filter..." onchange="CRUD_'+rnd+'.refresh()" />';
        }
        function collectFilterParams(){
            var c = meta.configs[0];
            var params = {page:page, size:size, sort:sort, order:order, keyword:$('crudKeyword_'+rnd).value || ''};
            var cols = c.searchCols || [];
            for(var i=0;i<cols.length;i++){
                var f = getFieldName(cols[i]); var el = $('f_'+rnd+'_'+f);
                if(el && el.value){ params['f_'+f] = el.value; }
            }
            return params;
        }
        function loadData(){
            $('crudTbody_' + rnd).innerHTML = '<tr><td colspan="99" class="crud-empty"><span class="crud-spinner"></span>Memuat data...</td></tr>';
            callApi('list', collectFilterParams()).then(function(j){
                if(j.status !== '00'){ showAlert('crudAlert_' + rnd, j.message, false); return; }
                renderRows(j.records || []);
                renderPages(j);
            }).catch(function(e){ showAlert('crudAlert_' + rnd, e, false); });
        }
        function renderRows(rows){
            var c = meta.configs[0];
            var cols = c.tableColumns || [];
            var html = '';
            if(rows.length === 0){ $('crudTbody_' + rnd).innerHTML = '<tr><td colspan="99" class="crud-empty">Belum ada data yang cocok.</td></tr>'; return; }
            for(var r=0;r<rows.length;r++){
                var row = rows[r];
                html += '<tr>';
                if(c.hasProfileImage){
                    var img = row._imageUrl || defaultProfileImage();
                    var big = row._imagePreviewUrl || img;
                    html += '<td class="crud-photo-cell"><img class="crud-photo-thumb" src="'+esc(img)+'" data-preview="'+esc(big)+'" onerror="this.onerror=null;this.src=\''+esc(defaultProfileImage())+'\';" onclick="CRUD_'+rnd+'.previewProfileImage(this.getAttribute(\'data-preview\'))" /></td>';
                }
                for(var i=0;i<cols.length;i++){
                    var f = getFieldName(cols[i]); var m = getMeta(0, f); var val = row[f];
                    html += '<td>'+formatCell(val, m, row, f)+'</td>';
                }
                var id = row._id || row.id;
                // Aksi dirangkai dahulu karena keduanya bergantung hak akses; menu
                // hanya tampil bila ada yang benar-benar dapat dipakai.
                var aksiBaris = [];
                if(c.canEdit){ aksiBaris.push({ ikon: 'fa-edit', label: 'Ubah', onclick: 'CRUD_'+rnd+'.openForm(\'edit\',\''+esc(id)+'\')' }); }
                if(c.canDelete){ aksiBaris.push({ ikon: 'far fa-trash-alt', label: 'Hapus', onclick: 'CRUD_'+rnd+'.remove(\''+esc(id)+'\')', merusak: true }); }
                html += '<td>' + aksiBarisMenu(aksiBaris) + '</td>';
                html += '</tr>';
            }
            $('crudTbody_' + rnd).innerHTML = html;
        }
        function formatCell(val, m, row, f){
            if(val === null || val === undefined || val === ''){ return '<span class="crud-cell-muted">-</span>'; }
            if(m.uiType === 'boolean'){
                var b = (val === true || val === 'true');
                return '<span class="crud-badge '+(b?'':'no')+'">'+(b?'Ya':'Tidak')+'</span>';
            }
            return esc(val);
        }
        function renderPages(j){
            var total = Number(j.total || 0), pages = Number(j.pages || 1);
            $('crudInfo_' + rnd).innerHTML = 'Total <b>'+total+'</b> data • Halaman '+j.page+' dari '+pages;
            var html = '';
            var start = Math.max(1, page - 2), end = Math.min(pages, page + 2);
            if(page > 1){ html += '<button class="crud-page" onclick="CRUD_'+rnd+'.goPage('+(page-1)+')">‹</button>'; }
            for(var i=start;i<=end;i++){ html += '<button class="crud-page '+(i===page?'active':'')+'" onclick="CRUD_'+rnd+'.goPage('+i+')">'+i+'</button>'; }
            if(page < pages){ html += '<button class="crud-page" onclick="CRUD_'+rnd+'.goPage('+(page+1)+')">›</button>'; }
            $('crudPages_' + rnd).innerHTML = html;
        }
        function buildFormShell(){
            var tabs = '', area = '';
            for(var i=0;i<meta.configs.length;i++){
                tabs += '<button type="button" id="tab_'+rnd+'_'+i+'" class="crud-tab '+(i===0?'active':'')+'" onclick="CRUD_'+rnd+'.showTab('+i+')">'+esc(meta.configs[i].tabTitle || meta.configs[i].dataTypeName)+'</button>';
                area += '<div id="tabArea_'+rnd+'_'+i+'" class="crud-step-card '+(i===0?'active':'')+'">'+renderConfigForm(i)+'</div>';
            }
            $('crudTabs_' + rnd).innerHTML = tabs;
            $('crudFormArea_' + rnd).innerHTML = area;
        }
        function renderConfigForm(idx){
            var c = meta.configs[idx];
            var formCols = c.formCols || []; var formLabels = c.formLabels || []; var html = '';
            html += '<input type="hidden" id="id_C'+idx+'_'+rnd+'" />';
            for(var s=0;s<formCols.length;s++){
                var title = (c.stepTitles || [])[s] || (s===0 ? c.dataTypeName : 'Bagian '+(s+1));
                html += '<div class="crud-step-title"><span class="crud-step-dot">'+(s+1)+'</span>'+esc(title)+'</div><div class="crud-form-grid">';
                var cols = formCols[s] || []; var labs = formLabels[s] || [];
                for(var i=0;i<cols.length;i++){
                    var spec = cols[i]; var f = getFieldName(spec); var m = getMeta(idx, f); var lab = labs[i] || labelize(f);
                    var wide = (m.uiType === 'textarea' || m.uiType === 'file') ? ' wide' : '';
                    html += '<div class="crud-field'+wide+'"><label>'+esc(lab)+(isRequired(idx,f)?' <span class="crud-required">*</span>':'')+'</label>'+renderInput(idx, f, m)+'</div>';
                }
                html += '</div>';
            }
            return html;
        }
        function renderInput(idx, f, m){
            var id = 'input_C'+idx+'_'+f+'_'+rnd;
            var l = locked(idx, f);
            if(l){ return '<input type="hidden" id="'+esc(id)+'" value="'+esc(l.id)+'" /><div class="crud-lock-info"><i class="fas fa-lock"></i> '+esc(l.label)+'<br/><small>'+esc(l.message || 'Data terkunci')+'</small></div>'; }
            if(m.uiType === 'file'){
                return '<div id="kolom_div_'+esc(f)+'_'+rnd+'" class="crud-control" style="min-height:64px;background:#f8fafc">Komponen upload akan dimuat saat form dibuka.</div>';
            }
            if(m.uiType === 'textarea'){
                return '<textarea id="'+esc(id)+'" class="crud-control" rows="4"></textarea>';
            }
            if(m.uiType === 'boolean'){
                return '<label class="crud-switch"><input id="'+esc(id)+'" type="checkbox" /> <span>Ya / Aktif</span></label>';
            }
            if(m.uiType === 'date'){
                return '<input id="'+esc(id)+'" class="crud-control" type="date" />';
            }
            if(m.uiType === 'number'){
                return '<input id="'+esc(id)+'" class="crud-control" type="number" />';
            }
            if(m.uiType === 'select'){
                var o = '<select id="'+esc(id)+'" class="crud-control"><option value=""></option>';
                var opts = m.options || [];
                for(var i=0;i<opts.length;i++){ o += '<option value="'+esc(opts[i].id)+'">'+esc(opts[i].nama)+'</option>'; }
                return o + '</select>';
            }
            if(m.uiType === 'fk'){
                return renderLookupControl('form', idx, f, m, false);
            }
            return '<input id="'+esc(id)+'" class="crud-control" type="text" />';
        }
        function lookupHiddenId(scope, idx, f){ return scope === 'filter' ? 'f_'+rnd+'_'+f : 'input_C'+idx+'_'+f+'_'+rnd; }
        function renderLookupControl(scope, idx, f, m, disabled){
            var hid = lookupHiddenId(scope, idx, f);
            var lab = 'label_' + hid;
            return '<div class="crud-banbox">'
                + '<input type="hidden" id="'+esc(hid)+'" />'
                + '<input id="'+esc(lab)+'" class="crud-control" type="text" readonly="readonly" placeholder="Klik untuk memilih..." onclick="CRUD_'+rnd+'.openLookupPopup(\''+scope+'\','+idx+',\''+esc(f)+'\')" />'
                + '<button type="button" class="crud-mini-btn" onclick="CRUD_'+rnd+'.openLookupPopup(\''+scope+'\','+idx+',\''+esc(f)+'\')"><i class="fas fa-search"></i></button>'
                + '<button type="button" class="crud-mini-btn" onclick="CRUD_'+rnd+'.clearLookup(\''+scope+'\','+idx+',\''+esc(f)+'\')"><i class="fas fa-times"></i></button>'
                + '</div>';
        }
        function clearLookup(scope, idx, f){
            var hid = lookupHiddenId(scope, idx, f); var lab = 'label_' + hid;
            if($(hid)) $(hid).value = ''; if($(lab)) $(lab).value = '';
            clearDependents(scope, idx, f);
            if(scope === 'filter') refresh();
        }
        function clearDependents(scope, idx, f){
            var lf = String(f).toLowerCase();
            if(lf === 'fakultas') clearLookupValue(scope, idx, 'jurusan');
            if(lf === 'yayasan') clearLookupValue(scope, idx, 'sekolah');
        }
        function clearLookupValue(scope, idx, f){
            var hid = lookupHiddenId(scope, idx, f); var lab = 'label_' + hid;
            if($(hid)) $(hid).value = ''; if($(lab)) $(lab).value = '';
        }
        function openLookupPopup(scope, idx, f){
            var m = getMeta(idx, f); if(!m || !m.optionClass){ return; }
            lookupPopupState = {scope:scope, idx:idx, field:f, meta:m};
            $('crudLookupTitle_' + rnd).innerHTML = 'Pilih ' + esc(labelize(f));
            $('crudLookupKeyword_' + rnd).value = '';
            showCrudModal('crudLookupModal_' + rnd, '2147483200');
            loadLookupPopup('');
            setTimeout(function(){ try{$('crudLookupKeyword_' + rnd).focus();}catch(e){} }, 100);
        }
        function closeLookupPopup(){ hideCrudModal('crudLookupModal_' + rnd); lookupPopupState = null; }
        function lookupPopupTyping(e){
            var q = $('crudLookupKeyword_' + rnd).value || '';
            if(lookupPopupTimer) clearTimeout(lookupPopupTimer);
            lookupPopupTimer = setTimeout(function(){ loadLookupPopup(q); }, 250);
        }
        function dependencyParams(scope, idx, f){
            var p = {}; var lf = String(f).toLowerCase();
            var fid = scope === 'filter' ? $('f_'+rnd+'_fakultas') : $('input_C'+idx+'_fakultas_'+rnd);
            var yid = scope === 'filter' ? $('f_'+rnd+'_yayasan') : $('input_C'+idx+'_yayasan_'+rnd);
            if(lf === 'jurusan' && fid && fid.value){ p.dep_fakultas = fid.value; }
            if(lf === 'sekolah' && yid && yid.value){ p.dep_yayasan = yid.value; }
            return p;
        }
        function loadLookupPopup(q){
            if(!lookupPopupState){ return; }
            $('crudLookupTbody_' + rnd).innerHTML = '<tr><td colspan="99" class="crud-empty"><span class="crud-spinner"></span>Memuat pilihan...</td></tr>';
            var params = {className: lookupPopupState.meta.optionClass, q:q || '', max:50};
            var dep = dependencyParams(lookupPopupState.scope, lookupPopupState.idx, lookupPopupState.field);
            for(var k in dep){ if(dep.hasOwnProperty(k)) params[k] = dep[k]; }
            callApi('options', params).then(function(j){
                if(j.status !== '00'){ return; }
                lookupPopupOptions = j.options || [];
                var cols = j.columns || []; var labels = j.columnLabels || [];
                var h = '<tr>';
                for(var c=0;c<cols.length;c++){ h += '<th>'+esc(labels[c] || labelize(cols[c]))+'</th>'; }
                h += '<th style="width:90px;text-align:right;"><%=Common.getBahasaConfig("Aksi")%></th></tr>';
                $('crudLookupThead_' + rnd).innerHTML = h;
                var info = j.contextInfo || []; var infEl = $('crudLookupInfo_' + rnd);
                if(info.length){ infEl.style.display='block'; infEl.innerHTML='<i class="fas fa-filter"></i> '+esc(info.join(' • ')); } else { infEl.style.display='none'; }
                var body = '';
                if(!lookupPopupOptions.length){ body = '<tr><td colspan="99" class="crud-empty">Tidak ada data yang cocok.</td></tr>'; }
                for(var i=0;i<lookupPopupOptions.length;i++){
                    var row = lookupPopupOptions[i].row || {};
                    body += '<tr ondblclick="CRUD_'+rnd+'.pickLookupPopupIndex('+i+')">';
                    for(var jx=0;jx<cols.length;jx++){ var cf = cols[jx]; body += '<td>'+formatCell(row[cf], {}, row, cf)+'</td>'; }
                    body += '<td style="text-align:right;"><button type="button" class="crud-btn crud-btn-primary" onclick="CRUD_'+rnd+'.pickLookupPopupIndex('+i+')"><i class="fas fa-check"></i> Pilih</button></td></tr>';
                }
                $('crudLookupTbody_' + rnd).innerHTML = body;
            });
        }
        function pickLookupPopupIndex(i){
            var opt = lookupPopupOptions[i]; if(!opt || !lookupPopupState){ return; }
            pickLookup(lookupPopupState.scope, lookupPopupState.idx, lookupPopupState.field, opt.id, opt.label || opt.nama || '');
            closeLookupPopup();
        }
        function openForm(mode, id){
            $('crudForm_' + rnd).reset();
            $('crudCurrentId_' + rnd).value = id || '';
            currentData = {};
            clearChildIds();
            applyLockedDefaults();
            showTab(0);
            $('crudModalTitle_' + rnd).innerHTML = (mode === 'edit' ? 'Ubah' : 'Tambah') + ' Data ' + esc(meta.configs[0].dataTypeName || '');
            if(mode === 'edit' && id){
                callApi('get', {id:id}).then(function(j){
                    if(j.status !== '00'){ showAlert('crudFormAlert_' + rnd, j.message, false); return; }
                    fillForm(j.models || []);
                    loadUploadComponents(id);
                    showCrudModal('crudModal_' + rnd, '2147483000');
                });
            } else {
                loadUploadComponents('');
                showCrudModal('crudModal_' + rnd, '2147483000');
            }
        }
        function applyLockedDefaults(){
            if(!meta || !meta.configs){ return; }
            for(var i=0;i<meta.configs.length;i++){
                var locks = meta.configs[i].lockedFields || {};
                for(var f in locks){ if(locks.hasOwnProperty(f)){
                    var id = 'input_C'+i+'_'+f+'_'+rnd; var el = $(id); if(el){ el.value = locks[f].id || ''; }
                    var lab = $('label_'+id); if(lab){ lab.value = locks[f].label || ''; }
                }}
            }
        }
        function clearChildIds(){ for(var i=0;i<meta.configs.length;i++){ var el=$('id_C'+i+'_'+rnd); if(el){el.value='';} } }
        function closeForm(){ hideCrudModal('crudModal_' + rnd); }
        function showTab(idx){
            activeTab = idx;
            for(var i=0;i<meta.configs.length;i++){
                $('tab_'+rnd+'_'+i).className = 'crud-tab '+(i===idx?'active':'');
                $('tabArea_'+rnd+'_'+i).className = 'crud-step-card '+(i===idx?'active':'');
            }
        }
        function fillForm(models){
            for(var i=0;i<models.length;i++){
                var idx = models[i].index; var data = models[i].data || {}; currentData[idx] = data;
                var idEl = $('id_C'+idx+'_'+rnd); if(idEl){ idEl.value = data._id || data.id || ''; }
                var c = meta.configs[idx];
                for(var f in c.fields){ if(c.fields.hasOwnProperty(f)){ fillField(idx, f, c.fields[f], data); } }
            }
        }
        function fillField(idx, f, m, data){
            var id = 'input_C'+idx+'_'+f+'_'+rnd; var el = $(id); if(!el){ return; }
            var l = locked(idx, f);
            if(l){ el.value = l.id || ''; var ll = $('label_'+id); if(ll){ ll.value = l.label || ''; } return; }
            if(m.uiType === 'boolean'){ el.checked = (data[f] === true || data[f] === 'true'); return; }
            if(m.uiType === 'date'){ el.value = data[f+'_iso'] || ''; return; }
            if(m.uiType === 'fk'){
                el.value = data[f+'_id'] || '';
                var lab = $('label_'+id); if(lab){ lab.value = data[f+'_nama'] || data[f] || ''; }
                return;
            }
            el.value = data[f] || '';
        }
        function save(ev){
            if(ev){ ev.preventDefault(); }
            var params = { id: $('crudCurrentId_' + rnd).value || '' };
            for(var i=0;i<meta.configs.length;i++){
                var idEl = $('id_C'+i+'_'+rnd); params['id_C'+i] = idEl ? idEl.value : '';
                var fields = meta.configs[i].fields;
                for(var f in fields){ if(fields.hasOwnProperty(f)){
                    var m = fields[f]; if(m.uiType === 'file'){ continue; }
                    var el = $('input_C'+i+'_'+f+'_'+rnd); if(!el){ continue; }
                    params['C'+i+'_'+f] = (m.uiType === 'boolean') ? (el.checked ? 'true' : 'false') : el.value;
                }}
            }
            callApi('save', params, 'POST').then(function(j){
                if(j.status !== '00'){ showAlert('crudFormAlert_' + rnd, j.message, false); return; }
                showAlert('crudAlert_' + rnd, j.message || 'Data berhasil disimpan', true);
                closeForm(); page = 1; loadData();
            }).catch(function(e){ showAlert('crudFormAlert_' + rnd, e, false); });
        }
        function remove(id){
            if(!confirm('Hapus data ini?')){ return; }
            callApi('delete', {id:id}, 'POST').then(function(j){
                if(j.status !== '00'){ showAlert('crudAlert_' + rnd, j.message, false); return; }
                showAlert('crudAlert_' + rnd, j.message || 'Data berhasil dihapus', true); loadData();
            });
        }
        function lookupTyping(idx, f, q){ openLookupPopup('form', idx, f); }
        function lookup(idx, f, q){ openLookupPopup('form', idx, f); }
        function pickLookup(scope, idx, f, idVal, label){
            var hid = lookupHiddenId(scope, idx, f); var lab = 'label_' + hid;
            if($(hid)) $(hid).value = idVal;
            if($(lab)) $(lab).value = label;
            clearDependents(scope, idx, f);
            if(scope === 'filter') refresh();
        }
        function loadUploadComponents(itemId){
            for(var i=0;i<meta.configs.length;i++){
                var fields = meta.configs[i].fields;
                for(var f in fields){ if(fields.hasOwnProperty(f) && fields[f].uiType === 'file'){
                    loadOneUpload(f, fields[f], itemId);
                }}
            }
        }
        function loadOneUpload(f, m, itemId){
            var target = $('kolom_div_'+f+'_'+rnd); if(!target){ return; }
            var type = m.customType || '{}'; var json = {};
            try{ json = JSON.parse(type); }catch(e){ json = {}; }
            var idVal = itemId ? '&id=' + enc(itemId) : '';
            var url = uploadUrl + '&clazz=' + enc(json.clazz || '') + '&jenis=' + enc(json.jenis || '') + idVal + '&col=' + enc(f) + '&rnd=' + enc(rnd);
            target.innerHTML = '<span class="crud-spinner"></span>Memuat upload...';
            fetch(url, {credentials:'same-origin'}).then(function(r){return r.text();}).then(function(html){
                target.innerHTML = '';
                var range = document.createRange(); range.selectNode(target);
                target.appendChild(range.createContextualFragment(html));
            }).catch(function(){ target.innerHTML = 'Gagal memuat komponen upload'; });
        }
        function buildQuery(params){
            var qs = [];
            for(var k in params){ if(params.hasOwnProperty(k)){ qs.push(enc(k)+'='+enc(params[k])); } }
            return qs.join('&');
        }
        function showProcessModal(title, message, percent, iconClass){
            percent = Math.max(0, Math.min(100, parseInt(percent || 0, 10)));
            var titleEl = $('crudProcessTitle_' + rnd), msgEl = $('crudProcessMsg_' + rnd), pctEl = $('crudProcessPct_' + rnd), barEl = $('crudProcessBar_' + rnd), iconEl = $('crudProcessIcon_' + rnd);
            if(titleEl){ titleEl.innerHTML = esc(title || 'Memproses Data'); }
            if(msgEl){ msgEl.innerHTML = esc(message || 'Harap tunggu, proses sedang berjalan...'); }
            if(pctEl){ pctEl.innerHTML = percent + '%'; }
            if(barEl){ barEl.style.width = percent + '%'; }
            if(iconEl){ iconEl.className = iconClass || 'fas fa-spinner fa-spin'; }
            showCrudModal('crudProcessModal_' + rnd, '2147483400');
        }
        function hideProcessModal(delay){
            setTimeout(function(){ hideCrudModal('crudProcessModal_' + rnd); }, typeof delay === 'number' ? delay : 900);
        }
        function setDownloadProgress(percent, message){
            showProcessModal('Memproses Download Excel', message || 'Memproses download...', percent, 'fas fa-spinner fa-spin');
        }
        function hideDownloadProgress(){ hideProcessModal(1200); }
        function setUploadProgress(percent, message, done, failed){
            showProcessModal(done ? (failed ? 'Upload Gagal' : 'Upload Selesai') : 'Memproses Upload Excel', message || 'Memproses upload...', percent, failed ? 'fas fa-exclamation-triangle' : (done ? 'fas fa-check-circle' : 'fas fa-spinner fa-spin'));
        }
        function pollDownloadProgress(jobId){
            callApi('downloadProgress', {jobId:jobId}).then(function(j){
                if(j.status !== '00'){
                    setDownloadProgress(100, j.message || 'Download gagal.');
                    showAlert('crudAlert_' + rnd, j.message || 'Download gagal.', false);
                    hideProcessModal(2200);
                    return;
                }
                setDownloadProgress(j.percent || 0, j.message || 'Memproses download...');
                if(j.statusJob === 'done'){
                    setDownloadProgress(100, 'Download siap. File akan mulai diunduh...');
                    if(j.downloadUrl){ window.location.href = j.downloadUrl; }
                    hideDownloadProgress();
                    return;
                }
                if(j.statusJob === 'error'){
                    showAlert('crudAlert_' + rnd, j.message || 'Download gagal.', false);
                    hideProcessModal(2200);
                    return;
                }
                setTimeout(function(){ pollDownloadProgress(jobId); }, 800);
            }).catch(function(e){
                showAlert('crudAlert_' + rnd, 'Gagal membaca progress download: ' + e, false);
                hideProcessModal(2200);
            });
        }
        function downloadExcel(){
            var params = collectFilterParams();
            setDownloadProgress(1, 'Menyiapkan download Excel...');
            callApi('download', params, 'POST').then(function(j){
                if(j.status !== '00'){
                    showAlert('crudAlert_' + rnd, j.message || 'Download gagal', false);
                    setDownloadProgress(100, j.message || 'Download gagal');
                    hideProcessModal(2200);
                    return;
                }
                pollDownloadProgress(j.jobId);
            }).catch(function(e){
                showAlert('crudAlert_' + rnd, 'Download gagal: ' + e, false);
                setDownloadProgress(100, 'Download gagal: ' + e);
                hideProcessModal(2200);
            });
        }
        function chooseUploadExcel(){
            var c = meta && meta.configs ? meta.configs[0] : {};
            if(!c.canUpload){ showAlert('crudAlert_' + rnd, 'Upload membutuhkan hak Tambah, Ubah, dan Hapus.', false); return; }
            var f = $('crudUploadFile_' + rnd); if(f){ f.value=''; f.click(); }
        }
        function uploadExcel(input){
            var file = input && input.files && input.files.length ? input.files[0] : null;
            if(!file){ return; }
            if(!/\.xlsx$/i.test(file.name || '')){ showAlert('crudAlert_' + rnd, 'File upload harus berformat .xlsx', false); return; }
            var reader = new FileReader();
            setUploadProgress(5, 'Membaca file Excel: ' + (file.name || ''), false, false);
            reader.onprogress = function(e){
                if(e.lengthComputable){
                    var pct = Math.min(30, Math.max(5, Math.round((e.loaded * 25 / e.total) + 5)));
                    setUploadProgress(pct, 'Membaca file Excel: ' + pct + '%', false, false);
                }
            };
            reader.onload = function(e){
                setUploadProgress(35, 'Mengirim dan memvalidasi file Excel...', false, false);
                callApi('uploadExcel', {fileName:file.name, fileData:e.target.result}, 'POST').then(function(j){
                    if(j.status !== '00'){
                        setUploadProgress(100, j.message || 'Upload gagal', true, true);
                        showAlert('crudAlert_' + rnd, j.message || 'Upload gagal', false);
                        hideProcessModal(2600);
                        return;
                    }
                    setUploadProgress(92, 'Upload berhasil. Memuat ulang data...', false, false);
                    var msg = j.message || 'Upload berhasil';
                    if(j.warnings && j.warnings.length){ msg += '<br/><small>'+esc(j.warnings.slice(0,5).join(' | '))+'</small>'; }
                    showAlert('crudAlert_' + rnd, msg, true);
                    loadData();
                    setUploadProgress(100, 'Upload selesai.', true, false);
                    hideProcessModal(1200);
                }).catch(function(err){
                    setUploadProgress(100, 'Upload gagal: ' + err, true, true);
                    showAlert('crudAlert_' + rnd, err, false);
                    hideProcessModal(2600);
                });
            };
            reader.onerror = function(){
                setUploadProgress(100, 'Gagal membaca file.', true, true);
                showAlert('crudAlert_' + rnd, 'Gagal membaca file.', false);
                hideProcessModal(2200);
            };
            reader.readAsDataURL(file);
        }

        function switchMainView(view){
            var dataEl = $('crudDataView_' + rnd), statEl = $('crudStatsView_' + rnd);
            var dataTab = $('crudViewTabData_' + rnd), statTab = $('crudViewTabStats_' + rnd);
            if(view === 'stats'){
                if(dataEl) dataEl.style.display = 'none';
                if(statEl) statEl.style.display = 'block';
                if(dataTab) dataTab.className = 'crud-view-tab';
                if(statTab) statTab.className = 'crud-view-tab active';
                loadStats(false);
            } else {
                if(dataEl) dataEl.style.display = 'block';
                if(statEl) statEl.style.display = 'none';
                if(dataTab) dataTab.className = 'crud-view-tab active';
                if(statTab) statTab.className = 'crud-view-tab';
            }
        }
        function renderStatsLoading(message){
            var target = $('crudStatsContent_' + rnd);
            if(!target){ return; }
            target.innerHTML = '<div class="crud-stat-loading animate__animated animate__fadeIn">' +
                '<div class="crud-stat-loading-icon"><i class="fas fa-chart-pie fa-spin"></i></div>' +
                '<div><div class="crud-stat-loading-title">Memproses statistik data</div>' +
                '<div class="crud-stat-loading-desc">'+esc(message || 'Menghitung rekap, grafik, dan tren data. Harap tunggu...')+'</div></div>' +
                '</div>';
        }

        function loadStats(force){
            var c = meta && meta.configs ? meta.configs[0] : {};
            if(!c.canViewStatistics){ showAlert('crudAlert_' + rnd, 'Anda tidak memiliki hak akses melihat statistik data.', false); return; }
            if(statsLoaded && !force){ return; }
            renderStatsLoading('Menyiapkan statistik data dan membaca filter aktif...');
            setStatsProgress(1, 'Menyiapkan statistik data...');
            callApi('stats', collectFilterParams(), 'POST').then(function(j){
                if(j.status !== '00'){
                    setStatsProgress(100, j.message || 'Gagal memuat statistik');
                    showAlert('crudAlert_' + rnd, j.message || 'Gagal memuat statistik', false);
                    hideProcessModal(2400);
                    return;
                }
                pollStatsProgress(j.jobId);
            }).catch(function(e){
                setStatsProgress(100, 'Gagal memuat statistik: ' + e);
                showAlert('crudAlert_' + rnd, 'Gagal memuat statistik: ' + e, false);
                hideProcessModal(2400);
            });
        }
        function setStatsProgress(percent, message){
            showProcessModal('Memproses Statistik Data', message || 'Menganalisis data...', percent, 'fas fa-chart-pie fa-spin');
        }
        function pollStatsProgress(jobId){
            callApi('statsProgress', {jobId:jobId}).then(function(j){
                if(j.status !== '00'){
                    setStatsProgress(100, j.message || 'Gagal memproses statistik');
                    hideProcessModal(2400);
                    return;
                }
                setStatsProgress(j.percent || 0, j.message || 'Menganalisis statistik...');
                if(j.statusJob === 'done'){
                    setStatsProgress(100, 'Statistik selesai diproses. Menyiapkan tampilan...');
                    renderStatistics(j.data || {});
                    statsLoaded = true;
                    hideProcessModal(900);
                    return;
                }
                if(j.statusJob === 'error'){
                    setStatsProgress(100, j.message || 'Gagal memproses statistik');
                    showAlert('crudAlert_' + rnd, j.message || 'Gagal memproses statistik', false);
                    hideProcessModal(2600);
                    return;
                }
                setTimeout(function(){ pollStatsProgress(jobId); }, 850);
            }).catch(function(e){
                setStatsProgress(100, 'Gagal membaca progress statistik: ' + e);
                hideProcessModal(2400);
            });
        }
        function renderStatistics(data){
            var target = $('crudStatsContent_' + rnd); if(!target){ return; }
            var html = '';
            var cards = data.cards || [];
            html += '<div class="crud-stats-cards">';
            for(var i=0;i<cards.length;i++){
                html += '<div class="crud-stat-card"><div class="icon"><i class="'+esc(cards[i].icon || 'fas fa-database')+'"></i></div><div class="value">'+esc(cards[i].value)+'</div><div class="label">'+esc(cards[i].label || '')+'</div></div>';
            }
            html += '</div>';
            var groups = data.groups || [];
            if(data.trend){ groups = [data.trend].concat(groups); }
            if(!groups.length){
                var note = data.statisticsNote || 'Belum ada field yang layak ditampilkan sebagai statistik. Field unik seperti nama, kode, keterangan, dan nomor unik otomatis dilewati.';
                html += '<div class="crud-stat-empty"><i class="fas fa-info-circle"></i> '+esc(note)+'</div>';
                target.innerHTML = html;
                return;
            }
            html += '<div class="crud-stat-grid">';
            for(var g=0;g<groups.length;g++){
                html += renderStatGroup(groups[g], g);
            }
            html += '</div>';
            target.innerHTML = html;
            setTimeout(function(){ drawAllStatsCharts(groups); }, 50);
        }
        function renderStatGroup(g, idx){
            var rows = g.rows || [];
            var canvasId = 'crudStatChart_' + rnd + '_' + idx;
            var html = '<div class="crud-stat-panel"><div class="crud-stat-panel-head"><h5><i class="fas fa-chart-bar"></i> '+esc(g.label || g.field || 'Statistik')+'</h5><span>'+esc(g.type || 'statistik')+'</span></div><div class="crud-stat-panel-body">';
            if(g.note){ html += '<div class="crud-stat-note"><i class="fas fa-info-circle"></i> '+esc(g.note)+'</div>'; }
            html += '<div class="crud-stat-chart"><canvas id="'+esc(canvasId)+'"></canvas></div>';
            html += '<table class="crud-stat-table">';
            for(var i=0;i<rows.length && i<12;i++){
                html += '<tr><td>'+esc(rows[i].label)+'</td><td>'+esc(rows[i].value)+'</td></tr>';
            }
            html += '</table></div></div>';
            return html;
        }
        function drawAllStatsCharts(groups){
            for(var i=0;i<groups.length;i++){
                drawStatsChart('crudStatChart_' + rnd + '_' + i, groups[i]);
            }
        }
        function drawStatsChart(canvasId, g){
            var el = $(canvasId); if(!el){ return; }
            var labels = g.chartLabels || [];
            var values = g.chartValues || [];
            if(!labels.length || !values.length){ return; }
            if(typeof Chart === 'undefined'){
                var parent = el.parentNode; if(!parent){ return; }
                var max = 1; for(var i=0;i<values.length;i++){ if(Number(values[i]) > max) max = Number(values[i]); }
                var html = '<div style="padding-top:8px;">';
                for(var j=0;j<labels.length;j++){
                    var w = Math.max(3, Math.round(Number(values[j]) * 100 / max));
                    html += '<div style="margin:8px 0;font-size:12px;font-weight:800;color:#334155;">'+esc(labels[j])+' <span style="float:right;">'+esc(values[j])+'</span><div style="height:8px;border-radius:99px;background:#e2e8f0;margin-top:5px;overflow:hidden;"><div style="height:8px;width:'+w+'%;background:var(--theme-gradient,#2563eb);"></div></div></div>';
                }
                html += '</div>'; parent.innerHTML = html; return;
            }
            try{ if(statsCharts[canvasId]){ statsCharts[canvasId].destroy(); } }catch(e){}
            try{
                statsCharts[canvasId] = new Chart(el.getContext('2d'), {
                    type: (g.field === 'tanggal_dirubah' ? 'line' : 'bar'),
                    data: { labels: labels, datasets: [{ label: g.label || 'Jumlah', data: values, borderWidth: 2, tension:.32, fill:true }] },
                    options: { responsive:true, maintainAspectRatio:false, plugins:{legend:{display:false}}, scales:{y:{beginAtZero:true,ticks:{precision:0}}, x:{ticks:{autoSkip:true,maxRotation:35,minRotation:0}}} }
                });
            }catch(e){}
        }
        function sortBy(f){
            if(sort === f){ order = (order === 'asc') ? 'desc' : 'asc'; } else { sort = f; order = 'asc'; }
            loadData();
        }
        function refresh(){ page = 1; loadData(); }
        function toggleFilter(){ var el = $('crudFilters_' + rnd); el.style.display = (el.style.display === 'grid') ? 'none' : 'grid'; }
        function keywordChanged(e){ if(e.keyCode === 13 || ($('crudKeyword_'+rnd).value || '').length === 0){ refresh(); } }
        function changeSize(){ size = Number($('crudSize_' + rnd).value || 20); refresh(); }
        function goPage(p){ page = p; loadData(); }

        setTimeout(init, 10);
        return { refresh:refresh, toggleFilter:toggleFilter, keywordChanged:keywordChanged, changeSize:changeSize, goPage:goPage,
            sortBy:sortBy, openForm:openForm, closeForm:closeForm, showTab:showTab, save:save, remove:remove,
            downloadExcel:downloadExcel, chooseUploadExcel:chooseUploadExcel, uploadExcel:uploadExcel,
            switchMainView:switchMainView, loadStats:loadStats,
            lookupTyping:lookupTyping, pickLookup:pickLookup, openLookupPopup:openLookupPopup, closeLookupPopup:closeLookupPopup,
            lookupPopupTyping:lookupPopupTyping, pickLookupPopupIndex:pickLookupPopupIndex, clearLookup:clearLookup,
            previewProfileImage:previewProfileImage };
    })();
    </script>
</div>
<% } %>
