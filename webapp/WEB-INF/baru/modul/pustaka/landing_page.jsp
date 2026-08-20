<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    String sParam = request.getParameter("s");
    String menuAwal = (sParam != null && !sParam.trim().isEmpty()) ? sParam : "katalog";
%>

<head>
    <link rel="stylesheet" href="<%=Common.ROOT%>/assets/library-modern/library.css">
    <style>
        .panel-bg-white-<%=rnd%> { background-color: rgba(255, 255, 255, 0.97) !important; }
    </style>
</head>

<jsp:include page="_header_perpustakaan.jsp">
    <jsp:param name="rnd" value="<%=rnd%>" />
</jsp:include>

<div class="container-fluid library-shell-modern mb-5 px-0" style="position: relative; z-index: 1; min-height: 60vh;">
    <div class="row g-4">
        <div class="col-lg-12" id="mainContentScroll<%=rnd%>">
            <div class="card border-0 panel-bg-white-<%=rnd%> p-0" style="overflow: hidden;">
                <div id="mainContentContainer<%=rnd%>">
                    <div class="p-5 text-center text-muted">
                        <div class="spinner-border text-primary mb-3"></div><br><%= Common.getBahasaConfig("Menyiapkan Konten Perpustakaan...") %>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div id="globalLoader<%=rnd%>" class="d-none align-items-center justify-content-center" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.6); z-index: 9999; backdrop-filter: blur(3px);">
    <div class="text-center text-white">
        <div class="spinner-border mb-3" style="width: 3rem; height: 3rem;" role="status"></div>
        <h5 class="fw-bold"><%= Common.getBahasaConfig("Mohon Tunggu Sebentar...") %></h5>
        <p class="small text-white-50"><%= Common.getBahasaConfig("Sistem sedang memproses permintaan Anda") %></p>
    </div>
</div>

<jsp:include page="_footer_perpustakaan.jsp">
    <jsp:param name="rnd" value="<%=rnd%>" />
</jsp:include>

<script>
const loadPageContent<%=rnd%> = async (url) => {
    const container = document.getElementById('mainContentContainer<%=rnd%>');
    showLoading<%=rnd%>(); 
    
    try {
        const response = await fetch(url + "&rnd=<%=rnd%>");
        if (response.ok) {
            container.innerHTML = await response.text();
            
            const scriptsArray = Array.from(container.getElementsByTagName('script'));
            for (let i = 0; i < scriptsArray.length; i++) {
                const oldScript = scriptsArray[i];
                const scriptNode = document.createElement('script');
                var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
                Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
                scriptNode.type = 'text/javascript';
                if (srcEff) {
                    scriptNode.src = srcEff;
                    document.body.appendChild(scriptNode);
                } else {
                    scriptNode.text = oldScript.innerHTML;
                    document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
                }
            }
        } else { throw new Error("Gagal"); }
    } catch (error) { 
        container.innerHTML = '<div class="p-5 text-center text-danger"><i class="fa fa-exclamation-triangle fa-2x mb-3 d-block"></i><%= Common.getBahasaConfig("Terjadi kesalahan pada saat memuat data halaman ini.") %></div>';
    } finally { hideLoading<%=rnd%>(); }
};

// Peta Routing Dinamis untuk Semua Menu
function panggilMenu<%=rnd%>(menuNama) {
    const mainSection = document.getElementById('mainContentScroll<%=rnd%>');
    if (mainSection) {
        const y = mainSection.getBoundingClientRect().top + window.pageYOffset - 90;
        window.scrollTo({top: y, behavior: 'smooth'});
    }

    const mobileNavEl = document.getElementById('navbarPerpus<%=rnd%>');
    if (mobileNavEl && mobileNavEl.classList.contains('show')) {
        try { bootstrap.Collapse.getInstance(mobileNavEl).hide(); } catch (e) {}
    }

    const baseURL = '<%=Common.ROOT%>/pustaka?hanya_tampil_jsp=true&p=pustaka&s=';
    const urlMap = {
        'katalog': baseURL + 'katalog',
        'populer': baseURL + 'populer',
        'sirkulasi_data': baseURL + 'sirkulasi',
        'kunjungan_data': baseURL + 'kunjungan',
        'dashboard': baseURL + 'dashboard',
        'beranda_anggota': baseURL + 'beranda_anggota',
        
        // Peta Informasi Profil & Layanan
        'sejarah': baseURL + '_informasi_pustaka&jenis=sejarah',
        'visi_misi': baseURL + '_informasi_pustaka&jenis=visi_misi',
        'struktur_organisasi': baseURL + '_informasi_pustaka&jenis=struktur_organisasi',
        'tata_tertib': baseURL + '_informasi_pustaka&jenis=tata_tertib',
        'sarpras': baseURL + '_informasi_pustaka&jenis=sarpras',
        'wifi': baseURL + '_informasi_pustaka&jenis=wifi',
        'ruang_baca': baseURL + '_informasi_pustaka&jenis=ruang_baca',
        'waktu_layanan_perpus': baseURL + '_informasi_pustaka&jenis=waktu_layanan_perpus',
        'waktu_layanan': baseURL + '_informasi_pustaka&jenis=waktu_layanan',
        'sirkulasi': baseURL + '_informasi_pustaka&jenis=sirkulasi',
        'peminjaman': baseURL + '_informasi_pustaka&jenis=peminjaman',
        'peminjaman_online': baseURL + '_informasi_pustaka&jenis=peminjaman_online',
        'pengembalian': baseURL + '_informasi_pustaka&jenis=pengembalian',
        'perpanjangan': baseURL + '_informasi_pustaka&jenis=perpanjangan',
        'referensi': baseURL + '_informasi_pustaka&jenis=referensi',
        'katalog_bersama': baseURL + '_informasi_pustaka&jenis=katalog_bersama',
        'katalog_nasional': baseURL + '_informasi_pustaka&jenis=katalog_nasional',
        'e_journal': baseURL + '_informasi_pustaka&jenis=e_journal',
        'panduan': baseURL + '_informasi_pustaka&jenis=panduan',
        'faq': baseURL + '_informasi_pustaka&jenis=faq'
    };

    if (urlMap[menuNama]) loadPageContent<%=rnd%>(urlMap[menuNama]);
}

const showLoading<%=rnd%> = () => { document.getElementById('globalLoader<%=rnd%>').classList.replace('d-none', 'd-flex'); };
const hideLoading<%=rnd%> = () => { document.getElementById('globalLoader<%=rnd%>').classList.replace('d-flex', 'd-none'); };

document.addEventListener("DOMContentLoaded", () => { panggilMenu<%=rnd%>('<%=menuAwal%>'); });
</script>
