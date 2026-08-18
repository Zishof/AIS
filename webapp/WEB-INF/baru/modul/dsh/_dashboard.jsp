<%@page import="ais.common.Common"%>
<%
// Menangkap parameter 'rnd' yang dilempar dari halaman induk
String rnd = request.getParameter("rnd");
if (rnd == null || rnd.trim().isEmpty()) {
    rnd = Common.getGeneratedBarCode(7);
}

// 1. Tangkap parameter tampilkan_header dengan pengecekan null secara eksplisit
String paramHeader = request.getParameter("tampilkan_header");
boolean tampilkanHeader = false;

if (paramHeader != null && paramHeader.trim().equalsIgnoreCase("true")) {
    tampilkanHeader = true;
}

// Deklarasi variabel identitas Perguruan Tinggi
String judulNamaPerguruanTinggi = "";
String Telepon = "";
String Motto = "";
String Alamat1 = "";
String Email = "";
String logo_PerguruanTinggi = "";

// 2. Ambil data Perguruan Tinggi hanya jika header akan ditampilkan
if (tampilkanHeader) {
    try {
        judulNamaPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
        Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getTelepon();
        Motto = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getMotto();
        Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getAlamat1();
        Email = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getEmail();
        logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
    } catch (Exception e) {
        judulNamaPerguruanTinggi = "Perguruan Tinggi";
    }
}
%>

<%-- BLOK HEADER DINAMIS --%>
<% if (tampilkanHeader) { %>

<div class="page-bg-dashboard"></div>

<div class="hero-section-dashboard pt-3 pb-5 mb-4 text-center">
    <div class="container pb-4 pt-4">
        <% if (logo_PerguruanTinggi != null && !logo_PerguruanTinggi.trim().isEmpty()) { %>
            <img src="<%=logo_PerguruanTinggi%>" alt="Logo Institusi" class="img-fluid mb-3 animate__animated animate__fadeInDown logo-shadow-dashboard" style="max-height: 130px; object-fit: contain;">
        <% } %>

        <h2 class="fw-bold text-white text-uppercase tracking-wide-dashboard text-shadow-dashboard mb-1 animate__animated animate__fadeInDown">
            <%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %>
        </h2>
        
        <% if (Motto != null && !Motto.trim().isEmpty()) { %>
            <p class="text-white text-shadow-dashboard fst-italic mb-2 opacity-75">"<%= Motto %>"</p>
        <% } %>

        <div class="d-inline-block badge-dashboard rounded-pill px-4 py-2 mb-3 mt-2 animate__animated animate__zoomIn">
            <h3 class="fw-bold mb-0 text-white text-shadow-dashboard">
                <i class="fas fa-chart-line me-2 text-warning"></i><%= Common.getBahasaConfig("Sistem Informasi & Dashboard Statistik") %>
            </h3>
        </div>

        <p class="lead fw-normal mb-3 text-white opacity-75 mx-auto animate__animated animate__fadeInUp" style="max-width: 800px; font-size: 1.1rem;">
            <%= Common.getBahasaConfig("Selamat datang di panel dashboard statistik terpadu. Anda dapat melihat berbagai rekapitulasi data secara interaktif.") %>
        </p>

        <div class="d-flex justify-content-center gap-4 text-white-50 small mt-3 animate__animated animate__fadeInUp">
            <% if(Email != null && !Email.trim().isEmpty()) { %><span><i class="fas fa-envelope me-1 text-white"></i> <%= Email %></span><% } %>
            <% if(Telepon != null && !Telepon.trim().isEmpty()) { %><span><i class="fas fa-phone-alt me-1 text-white"></i> <%= Telepon %></span><% } %>
        </div>
    </div>
</div>
<% } %>
<%-- AKHIR BLOK HEADER DINAMIS --%>

<div class="container-fluid mt-4" style="position: relative; z-index: 3; min-height: 50vh;">
    <ul class="nav nav-tabs shadow-sm" id="mainTab<%=rnd%>" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active fw-bold" id="tab-matakuliah<%=rnd%>" data-bs-toggle="tab" data-bs-target="#content-matakuliah<%=rnd%>" type="button" role="tab" 
                data-url="<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmastermatakuliahzul&s=_statistik_matakuliah&baru=true&rnd=<%=rnd%>">
                <%= Common.getBahasaConfig("Mata Kuliah") %>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold" id="tab-kurikulum<%=rnd%>" data-bs-toggle="tab" data-bs-target="#content-kurikulum<%=rnd%>" type="button" role="tab" 
                data-url="<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmasterkurikulumzul&s=_statistik_kurikulum&baru=true&rnd=<%=rnd%>">
                <%= Common.getBahasaConfig("Kurikulum") %>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold" id="tab-dosen<%=rnd%>" data-bs-toggle="tab" data-bs-target="#content-dosen<%=rnd%>" type="button" role="tab" 
                data-url="<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmasterdosenzul&s=_statistik_dosen&baru=true&rnd=<%=rnd%>">
                <%= Common.getBahasaConfig("Dosen") %>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold" id="tab-mahasiswa<%=rnd%>" data-bs-toggle="tab" data-bs-target="#content-mahasiswa<%=rnd%>" type="button" role="tab" 
                data-url="<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmastermahasiswazul&s=_statistik_mahasiswa_aktif&baru=true&rnd=<%=rnd%>">
                <%= Common.getBahasaConfig("Mahasiswa") %>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold" id="tab-perkuliahan<%=rnd%>" data-bs-toggle="tab" data-bs-target="#content-perkuliahan<%=rnd%>" type="button" role="tab" 
                data-url="<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=pagesmasterperkuliahanzul&s=_statistik_perkuliahan&baru=true&rnd=<%=rnd%>">
                <%= Common.getBahasaConfig("Perkuliahan") %>
            </button>
        </li>
        
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold" id="tab-pertemuan<%=rnd%>" data-bs-toggle="tab" data-bs-target="#content-pertemuan<%=rnd%>" type="button" role="tab" 
                data-url="<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=elearning&s=_statistik_pertemuan&baru=true&rnd=<%=rnd%>">
                <%= Common.getBahasaConfig("Pertemuan") %>
            </button>
        </li>
        
        <li class="nav-item" role="presentation">
            <button class="nav-link fw-bold" id="tab-alumni<%=rnd%>" data-bs-toggle="tab" data-bs-target="#content-alumni<%=rnd%>" type="button" role="tab" 
                data-url="<%=Common.ROOT%>/dsh?hanya_tampil_jsp=true&p=alumni&s=_statistik_alumni&baru=true&rnd=<%=rnd%>">
                <%= Common.getBahasaConfig("Alumni") %>
            </button>
        </li>
    </ul>

    <div class="tab-content border-start border-end border-bottom p-4 bg-white shadow-sm" id="mainTabContent<%=rnd%>">
        <div class="tab-pane fade show active" id="content-matakuliah<%=rnd%>" role="tabpanel"></div>
        <div class="tab-pane fade" id="content-kurikulum<%=rnd%>" role="tabpanel"></div>
        <div class="tab-pane fade" id="content-dosen<%=rnd%>" role="tabpanel"></div>
        <div class="tab-pane fade" id="content-mahasiswa<%=rnd%>" role="tabpanel"></div>
        <div class="tab-pane fade" id="content-perkuliahan<%=rnd%>" role="tabpanel"></div>
        <div class="tab-pane fade" id="content-pertemuan<%=rnd%>" role="tabpanel"></div>
        <div class="tab-pane fade" id="content-alumni<%=rnd%>" role="tabpanel"></div>
    </div>
</div>

<%-- BLOK FOOTER DINAMIS --%>
<% if (tampilkanHeader) { %>
<footer class="footer-dashboard pt-5 pb-3">
    <div class="container text-center text-md-start">
        <div class="row text-center text-md-start">
            
            <div class="col-md-5 col-lg-5 col-xl-5 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold mb-3">
                    <i class="fas fa-university me-2"></i><%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %>
                </h5>
                <p class="small pe-md-4">
                    <%= Common.getBahasaConfig("Pusat Sistem Informasi dan Dashboard Statistik Terpadu. Kami berkomitmen untuk menyajikan data yang akurat, mutakhir, dan komprehensif.") %>
                </p>
                <% if (Motto != null && !Motto.trim().isEmpty()) { %>
                    <p class="fst-italic mt-2 mb-0">"<%= Motto %>"</p>
                <% } %>
            </div>

            <div class="col-md-3 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold mb-3"><%= Common.getBahasaConfig("Tautan Cepat") %></h5>
                <p class="mb-2"><a href="#" class="text-decoration-none small hover-text-white"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Beranda Utama") %></a></p>
                <p class="mb-2"><a href="#" class="text-decoration-none small hover-text-white"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Portal Akademik") %></a></p>
                <p class="mb-2"><a href="#" class="text-decoration-none small hover-text-white"><i class="fas fa-angle-right me-2"></i><%= Common.getBahasaConfig("Pusat Bantuan (Helpdesk)") %></a></p>
            </div>

            <div class="col-md-4 col-lg-3 col-xl-3 mx-auto mb-4">
                <h5 class="text-uppercase fw-bold mb-3"><%= Common.getBahasaConfig("Hubungi Kami") %></h5>
                <p class="small mb-2"><i class="fas fa-map-marker-alt me-2"></i> <%= Alamat1 != null && !Alamat1.trim().isEmpty() ? Alamat1 : "-" %></p>
                <p class="small mb-2"><i class="fas fa-envelope me-2"></i> <%= Email != null && !Email.trim().isEmpty() ? Email : "-" %></p>
                <p class="small mb-4"><i class="fas fa-phone-alt me-2"></i> <%= Telepon != null && !Telepon.trim().isEmpty() ? Telepon : "-" %></p>
                
                <div>
                    <a href="#" class="social-icon"><i class="fab fa-facebook-f"></i></a>
                    <a href="#" class="social-icon"><i class="fab fa-twitter"></i></a>
                    <a href="#" class="social-icon"><i class="fab fa-instagram"></i></a>
                    <a href="#" class="social-icon"><i class="fab fa-youtube"></i></a>
                </div>
            </div>
        </div>

        <hr class="mb-4 mt-2" style="border-color: rgba(255,255,255,0.2);">
        
        <div class="row align-items-center">
            <div class="col-md-12 col-lg-12 text-center">
                <p class="small mb-0">
                    &copy; <%= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) %> <strong><%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "" %></strong>. <%= Common.getBahasaConfig("Hak Cipta Dilindungi Undang-Undang.") %>
                </p>
            </div>
        </div>
    </div>
</footer>
<% } %>

<script>
document.addEventListener("DOMContentLoaded", function() {
    
    // Fungsi utama untuk menarik HTML dari tautan dan merendernya ke dalam area tab
    const memuatKontenTab<%=rnd%> = async (tombolTab) => {
        const targetPemilih = tombolTab.getAttribute('data-bs-target');
        const areaTarget = document.querySelector(targetPemilih);
        const tautanUrl = tombolTab.getAttribute('data-url');

        if (areaTarget.innerHTML.trim() === "") {
            areaTarget.innerHTML = '<div class="p-5 text-center text-muted"><div class="spinner-border text-primary mb-3"></div><br><%= Common.getBahasaConfig("Sedang mengkalkulasi dan menyiapkan data...") %></div>';
            
            try {
                const respons = await fetch(tautanUrl);
                
                if (respons.ok) {
                    areaTarget.innerHTML = await respons.text();
                    
                    const daftarSkrip = Array.from(areaTarget.getElementsByTagName('script'));
                    for (let i = 0; i < daftarSkrip.length; i++) {
                        const skripLama = daftarSkrip[i];
                        const simpulSkrip = document.createElement('script');
                        
                        Array.from(skripLama.attributes).forEach(atribut => { 
                            simpulSkrip.setAttribute(atribut.name, atribut.value); 
                        });
                        
                        if (skripLama.src) { 
                            simpulSkrip.src = skripLama.src; 
                            document.body.appendChild(simpulSkrip); 
                        } else { 
                            simpulSkrip.text = skripLama.innerHTML;
                            document.body.appendChild(simpulSkrip).parentNode.removeChild(simpulSkrip); 
                        }
                    }
                } else {
                    throw new Error("Gagal mengambil data dari peladen");
                }
            } catch (kesalahan) { 
                areaTarget.innerHTML = '<div class="p-5 text-center text-danger"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%= Common.getBahasaConfig("Terjadi kesalahan pada saat memuat data halaman ini.") %></div>';
            }
        }
    };

    const tombolTabAktif = document.querySelector('#mainTab<%=rnd%> .nav-link.active');
    if(tombolTabAktif) {
        memuatKontenTab<%=rnd%>(tombolTabAktif);
    }

    const elemenTab = document.querySelectorAll('#mainTab<%=rnd%> button[data-bs-toggle="tab"]');
    elemenTab.forEach(tombol => {
        tombol.addEventListener('show.bs.tab', function (acara) {
            memuatKontenTab<%=rnd%>(acara.target);
        });
    });
});
</script>