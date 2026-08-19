<%@page import="ais.common.ConstantValues"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common" %>
<%@ page import="ais.database.model.library.*" %>
<%@ page import="ais.database.model.sekolah.Sekolah" %>
<%@ page import="ais.database.model.sekolah.Yayasan" %>
<%@ page import="ais.database.model.Fakultas" %>
<%@ page import="ais.database.model.Jurusan" %>
<%@ page import="ais.database.model.Tbmuser" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.hibernate.Session" %>
<%@ page import="org.hibernate.criterion.Order" %>
<%@ page import="org.hibernate.criterion.Restrictions" %>
<%@ page import="ais.database.hibernate.HibernateUtil" %>
<%
String rnd = Common.getGeneratedBarCode(7);
String linkDetail = Common.ROOT + "/pustaka?hanya_tampil_jsp=true&p=pustaka&s=_item_rinci";

// =========================================================================
// LOGIKA LOAD MASTER STRUKTUR ORGANISASI (MENAMPILKAN SEMUA DATA AKTIF)
// =========================================================================
Tbmuser tbmuser = Common.getCurrentUser(request);
boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
boolean pt = ptYa[0];
boolean ya = ptYa[1];

List<Sekolah> listSekolahRyg = new ArrayList<Sekolah>();
List<Yayasan> listYayasanRyg = new ArrayList<Yayasan>();
List<Fakultas> listFakultasRyg = new ArrayList<Fakultas>();
List<Jurusan> listJurusanRyg = new ArrayList<Jurusan>();

Session sessFilterRyg = null;
try {
    sessFilterRyg = HibernateUtil.openSession();
    
    // Menarik seluruh data master Yayasan dan Sekolah yang aktif
    if (ya) {
        org.hibernate.Criteria cYay = sessFilterRyg.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
        listYayasanRyg = ConstantValues.simpleList(cYay, Yayasan.class);

        org.hibernate.Criteria cSek = sessFilterRyg.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
        listSekolahRyg = ConstantValues.simpleList(cSek, Sekolah.class);
    }
    
    // Menarik seluruh data master Fakultas dan Jurusan yang aktif
    if (pt) {
        org.hibernate.Criteria cFak = sessFilterRyg.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
        listFakultasRyg = ConstantValues.simpleList(cFak, Fakultas.class);

        org.hibernate.Criteria cJur = sessFilterRyg.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
        listJurusanRyg = ConstantValues.simpleList(cJur, Jurusan.class);
    }
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pustaka/populer.jsp:54");
    // Diabaikan untuk mencegah terhentinya render halaman jika terjadi galat koneksi
} finally {
    if(sessFilterRyg != null && sessFilterRyg.isOpen()) {
        sessFilterRyg.disconnect();
        sessFilterRyg.close();
    }
}
// =========================================================================
%>

<style>
    /* Desain Dasar Halaman */
    .main-body-bg<%=rnd%> { background-color: #f8f9fa; position: relative; }
    .main-body-bg<%=rnd%>::before { 
        content: ""; position: fixed; top: 0; left: 0; right: 0; bottom: 0; 
        background-image: url('<%=Common.ROOT%>/img/buku.jpeg'); 
        background-size: cover; background-position: center; opacity: 0.25; z-index: -10; pointer-events: none; 
    }

    /* Estetika Kartu Pustaka */
    .book-card<%=rnd%> { transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); background: #fff; border: 1px solid rgba(0,0,0,0.05); }
    .book-card<%=rnd%>:hover { transform: translateY(-7px); box-shadow: 0 1.5rem 2.5rem rgba(0,0,0,0.1)!important; border-color: transparent; }
    .book-cover<%=rnd%> { object-fit: contain; height: 220px; width: 100%; padding: 15px; background: linear-gradient(to bottom, #f8f9fa, #ffffff); }
    
    /* Bagian Header/Hero */
    .hero-section<%=rnd%> { 
        background-image: url('<%=Common.ROOT%>/img/buku.jpeg'); background-size: cover; background-position: center; background-attachment: fixed; 
        padding: 5rem 0 4rem 0; border-radius: 0 0 2.5rem 2.5rem; position: relative; overflow: hidden;
    }
    @media (max-width: 767.98px) { .hero-section<%=rnd%> { background-attachment: scroll; padding: 3rem 0 2rem 0; } }
    .hero-section<%=rnd%>::before { content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); z-index: 1; }
    .hero-section<%=rnd%>::after { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 50px; background: linear-gradient(to top, rgba(0,0,0,0.3), transparent); z-index: 1; }
    .hero-section<%=rnd%> .container { position: relative; z-index: 2; }
    .text-light-shadow<%=rnd%> { color: #ffffff !important; text-shadow: 2px 2px 6px rgba(0, 0, 0, 0.8), -1px -1px 4px rgba(0, 0, 0, 0.6); font-weight: 800; }
    
    /* Panel Pencarian */
    .search-wrapper<%=rnd%> { position: relative; overflow: hidden; background-color: #fff; }
    .search-wrapper<%=rnd%>::before { 
        content: ""; position: absolute; top: 0; left: 0; right: 0; bottom: 0; 
        background-image: url('<%=Common.ROOT%>/img/buku.jpeg'); background-size: cover; background-position: center; opacity: 0.08; z-index: 1; 
    }
    .search-content-inner<%=rnd%> { position: relative; z-index: 2; background: rgba(255, 255, 255, 0.95); border-radius: 1.5rem; box-shadow: 0 8px 25px rgba(0,0,0,0.08); padding: 2.5rem; }
    
    /* Pengaturan Form Input agar tampak modern */
    .input-group-text.bg-white { border-right: none; }
    .form-control.border-start-0 { border-left: none; }
    .form-control.border-start-0:focus { box-shadow: none; border-color: #ced4da; }
    .form-select.border-start-0 { border-left: none; }
    .form-select.border-start-0:focus { box-shadow: none; border-color: #ced4da; }
    
    /* Teks Elipsis */
    .line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .text-truncate-custom { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; }
    #modalDetail<%=rnd%> .modal-dialog { max-width: 90% !important; }
</style>

<div class="hero-section<%=rnd%> text-center shadow mb-5">
    <div class="container">
        <h1 class="display-5 mb-3 text-light-shadow<%=rnd%>"><i class="fa fa-fire me-3 text-warning"></i><%=Common.getBahasaConfig("Koleksi Pustaka Populer")%></h1>
        <p class="lead mb-5 text-light-shadow<%=rnd%>" style="font-weight: 600;"><%=Common.getBahasaConfig("Eksplorasi literatur dan bahan bacaan yang paling diminati serta sering dipinjam oleh pemustaka.")%></p>
    </div>
</div>

<div class="container mb-5">
    <div class="card shadow-sm border-0 rounded-4 mb-5 search-wrapper<%=rnd%>">
        <div class="search-content-inner<%=rnd%>">
            <h5 class="fw-bold mb-4 text-primary border-bottom pb-3"><i class="fa fa-search me-2"></i><%=Common.getBahasaConfig("Parameter Pencarian Koleksi")%></h5>
            <form id="formPencarian<%=rnd%>" onsubmit="event.preventDefault(); loadBuku<%=rnd%>(1);">
                
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Judul / Keterangan Pustaka")%></label>
                        <div class="input-group shadow-sm rounded-3">
                            <span class="input-group-text bg-white text-primary"><i class="fa fa-book"></i></span>
                            <input type="text" id="filterJudul<%=rnd%>" class="form-control border-start-0 py-2" placeholder="<%=Common.getBahasaConfig("Ketikkan judul atau kata kunci referensi...")%>">
                        </div>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor ISBN")%></label>
                        <div class="input-group shadow-sm rounded-3">
                            <span class="input-group-text bg-white text-muted"><i class="fa fa-barcode"></i></span>
                            <input type="text" id="filterIsbn<%=rnd%>" class="form-control border-start-0 py-2" placeholder="<%=Common.getBahasaConfig("Contoh: 978-...")%>">
                        </div>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor ISSN")%></label>
                        <div class="input-group shadow-sm rounded-3">
                            <span class="input-group-text bg-white text-muted"><i class="fa fa-qrcode"></i></span>
                            <input type="text" id="filterIssn<%=rnd%>" class="form-control border-start-0 py-2" placeholder="<%=Common.getBahasaConfig("Contoh: 1234-...")%>">
                        </div>
                    </div>
                </div>

                <div class="text-center mt-4 mb-2">
                    <button class="btn btn-sm btn-outline-secondary rounded-pill px-4 py-2 fw-bold shadow-sm transition-all" type="button" onclick="toggleAdvancedSearch<%=rnd%>(this)">
                        <i class="fa fa-sliders me-2"></i> <span id="textToggle<%=rnd%>"><%=Common.getBahasaConfig("Tampilkan Pencarian Lanjutan")%></span>
                    </button>
                </div>

                <div id="advancedSearch<%=rnd%>" style="display: none;" data-expanded="false" class="mt-3">
                    <div class="card card-body bg-light border-0 shadow-sm rounded-4 p-4">
                        <h6 class="fw-bold text-secondary mb-3"><i class="fa fa-filter me-2"></i><%=Common.getBahasaConfig("Filter Spesifik")%></h6>
                        <div class="row g-3">
                            
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kategori / Subjek")%></label>
                                <div class="input-group bg-white rounded-3 shadow-sm">
                                    <span class="input-group-text bg-white border-0 text-muted"><i class="fa fa-tags"></i></span>
                                    <input type="text" id="filterKategori<%=rnd%>" class="form-control border-0" placeholder="<%=Common.getBahasaConfig("Ketikkan kategori...")%>">
                                </div>
                            </div>

                            <% if (ya) { %>
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Yayasan")%></label>
                                    <div class="input-group bg-white rounded-3 shadow-sm">
                                        <span class="input-group-text bg-white border-0 text-info"><i class="fa fa-building"></i></span>
                                        <select id="filterYayasan<%=rnd%>" class="form-select border-0 fw-medium">
                                            <option value=""><%=Common.getBahasaConfig("-- Seluruh Yayasan --")%></option>
                                            <% for(Yayasan y : listYayasanRyg) { %><option value="<%=y.getId()%>"><%=y.getNama()%></option><% } %>
                                        </select>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Sekolah")%></label>
                                    <div class="input-group bg-white rounded-3 shadow-sm">
                                        <span class="input-group-text bg-white border-0 text-primary"><i class="fa fa-institution"></i></span>
                                        <select id="filterSekolah<%=rnd%>" class="form-select border-0 fw-medium">
                                            <option value=""><%=Common.getBahasaConfig("-- Seluruh Sekolah --")%></option>
                                            <% for(Sekolah s : listSekolahRyg) { %><option value="<%=s.getId()%>"><%=s.getNama()%></option><% } %>
                                        </select>
                                    </div>
                                </div>
                            <% } %>

                            <% if (pt) { %>
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Fakultas")%></label>
                                    <div class="input-group bg-white rounded-3 shadow-sm">
                                        <span class="input-group-text bg-white border-0 text-info"><i class="fa fa-university"></i></span>
                                        <select id="filterFakultas<%=rnd%>" class="form-select border-0 fw-medium">
                                            <option value=""><%=Common.getBahasaConfig("-- Seluruh Fakultas --")%></option>
                                            <% for(Fakultas f : listFakultasRyg) { %><option value="<%=f.getId()%>"><%=f.getNama()%></option><% } %>
                                        </select>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Program Studi / Jurusan")%></label>
                                    <div class="input-group bg-white rounded-3 shadow-sm">
                                        <span class="input-group-text bg-white border-0 text-primary"><i class="fa fa-graduation-cap"></i></span>
                                        <select id="filterJurusan<%=rnd%>" class="form-select border-0 fw-medium">
                                            <option value=""><%=Common.getBahasaConfig("-- Seluruh Program Studi --")%></option>
                                            <% for(Jurusan j : listJurusanRyg) { %><option value="<%=j.getId()%>"><%=j.getNama()%></option><% } %>
                                        </select>
                                    </div>
                                </div>
                            <% } %>
                            
                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Lokasi Perpustakaan")%></label>
                                <div class="input-group bg-white rounded-3 shadow-sm">
                                    <span class="input-group-text bg-white border-0 text-danger"><i class="fa fa-map-marker"></i></span>
                                    <select id="filterPerpustakaan<%=rnd%>" class="form-select border-0 fw-medium">
                                        <option value=""><%=Common.getBahasaConfig("-- Seluruh Lokasi --")%></option>
                                    </select>
                                </div>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Jenis Referensi")%></label>
                                <div class="input-group bg-white rounded-3 shadow-sm">
                                    <span class="input-group-text bg-white border-0 text-success"><i class="fa fa-tags"></i></span>
                                    <select id="filterJenis<%=rnd%>" class="form-select border-0 fw-medium">
                                        <option value=""><%=Common.getBahasaConfig("-- Seluruh Jenis --")%></option>
                                    </select>
                                </div>
                            </div>

                            <div class="col-md-4">
                                <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tipe Kepustakaan")%></label>
                                <div class="input-group bg-white rounded-3 shadow-sm">
                                    <span class="input-group-text bg-white border-0 text-warning"><i class="fa fa-bookmark"></i></span>
                                    <select id="filterTipe<%=rnd%>" class="form-select border-0 fw-medium">
                                        <option value=""><%=Common.getBahasaConfig("-- Seluruh Tipe --")%></option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="d-flex align-items-center justify-content-end gap-3 mt-4 pt-3 border-top">
                    <button type="button" class="btn btn-light px-4 py-2 rounded-pill fw-bold text-secondary shadow-sm" onclick="resetFilter<%=rnd%>()">
                        <i class="fa fa-undo me-2"></i><%=Common.getBahasaConfig("Atur Ulang Kriteria")%>
                    </button>
                    <button type="submit" class="btn btn-primary px-5 py-2 rounded-pill shadow-sm fw-bold">
                        <i class="fa fa-search me-2"></i><%=Common.getBahasaConfig("Cari Pustaka")%>
                    </button>
                </div>
            </form>
        </div>
    </div>

    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-end mb-4 border-bottom pb-3">
        <h4 class="fw-bold text-dark m-0 mb-2 mb-md-0"><i class="fa fa-star text-warning me-2"></i><%=Common.getBahasaConfig("Koleksi Pustaka Populer")%></h4>
        <div id="infoHasil<%=rnd%>" class="text-muted small fw-medium bg-white px-3 py-1 rounded-pill border"><%=Common.getBahasaConfig("Sistem sedang menyiapkan data pustaka...")%></div>
    </div>

    <div class="row row-cols-1 row-cols-md-2 row-cols-xl-4 g-4 main-body-bg<%=rnd%>" id="gridBuku<%=rnd%>"></div>

    <nav class="mt-5 pt-3" aria-label="<%=Common.getBahasaConfig("Navigasi Halaman Data")%>">
        <ul class="pagination pagination-lg justify-content-center shadow-sm rounded-pill bg-white d-inline-flex px-2 py-2" id="paginationBuku<%=rnd%>"></ul>
    </nav>
</div>

<script>
    const limitPerPage<%=rnd%> = 12;
    let currentPageGlobal<%=rnd%> = 1;
    const apiUrl<%=rnd%> = '<%=Common.ROOT%>/Data';

    const txtSedangMemuat<%=rnd%> = '<%=Common.getBahasaConfigJS("Sistem sedang memuat data pustaka...")%>';
    const txtGagalMemuat<%=rnd%> = '<%=Common.getBahasaConfigJS("Terjadi kendala saat menarik data dari peladen. Silakan muat ulang halaman.")%>';
    const txtTidakDitemukan<%=rnd%> = '<%=Common.getBahasaConfigJS("Mohon maaf, tidak ditemukan pustaka yang sesuai dengan kriteria pencarian Anda.")%>';
    const txtTanpaJudul<%=rnd%> = '<%=Common.getBahasaConfigJS("Tanpa Judul")%>';
    const txtTidakDiketahui<%=rnd%> = '<%=Common.getBahasaConfigJS("Data Tidak Tersedia")%>';
    const txtSebelumnya<%=rnd%> = '<%=Common.getBahasaConfigJS("Sebelumnya")%>';
    const txtSelanjutnya<%=rnd%> = '<%=Common.getBahasaConfigJS("Selanjutnya")%>';

    // Fungsi Toggle Ekstra Aman berbasis data-attribute (Anti-glitch pada AJAX Lazy Load)
    function toggleAdvancedSearch<%=rnd%>(btnElement) {
        const panel = document.getElementById('advancedSearch<%=rnd%>');
        const textSpan = document.getElementById('textToggle<%=rnd%>');
        const isExpanded = panel.getAttribute('data-expanded') === 'true';
        
        if (!isExpanded) {
            if (typeof window.jQuery !== 'undefined') { window.jQuery(panel).slideDown('fast'); } else { panel.style.display = 'block'; }
            panel.setAttribute('data-expanded', 'true');
            textSpan.innerHTML = '<%=Common.getBahasaConfigJS("Sembunyikan Pencarian Lanjutan")%>';
            btnElement.className = 'btn btn-sm btn-secondary rounded-pill px-4 py-2 fw-bold shadow-sm transition-all text-white';
        } else {
            if (typeof window.jQuery !== 'undefined') { window.jQuery(panel).slideUp('fast', function(){ panel.style.display = 'none'; }); } else { panel.style.display = 'none'; }
            panel.setAttribute('data-expanded', 'false');
            textSpan.innerHTML = '<%=Common.getBahasaConfigJS("Tampilkan Pencarian Lanjutan")%>';
            btnElement.className = 'btn btn-sm btn-outline-secondary rounded-pill px-4 py-2 fw-bold shadow-sm transition-all';
        }
    }

    const fetchData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch(apiUrl<%=rnd%>, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sql: sql, action: "sql", tanpaLogin:"true" }) });
            const result = await res.json(); return result.data || [];
        } catch (e) { console.error(e); return null; }
    };

    // Fungsi Pengambilan Data Master secara Paralel untuk Filter (Perpustakaan, Jenis, dan Tipe)
    async function loadMasterData<%=rnd%>() {
        try {
            const reqPerpus = { "action": "daftar", "tanpaLogin":"true", "class": "<%=ais.database.model.library.Perpustakaan.class.getName()%>", "where1": "(aktif is null or aktif = true)", "max": 100, "order1": "asc", "sort1": "nama" };
            const pPerpus = fetch(apiUrl<%=rnd%>, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqPerpus) }).then(res => res.json());

            const reqJenis = { "action": "daftar", "tanpaLogin":"true", "class": "<%=ais.database.model.library.JenisItem.class.getName()%>", "max": 100, "order1": "asc", "sort1": "nama" };
            const pJenis = fetch(apiUrl<%=rnd%>, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqJenis) }).then(res => res.json());

            const reqTipe = { "action": "daftar", "tanpaLogin":"true", "class": "<%=ais.database.model.library.TipeItem.class.getName()%>", "max": 100, "order1": "asc", "sort1": "nama" };
            const pTipe = fetch(apiUrl<%=rnd%>, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(reqTipe) }).then(res => res.json());

            const [dataPerpus, dataJenis, dataTipe] = await Promise.all([pPerpus, pJenis, pTipe]);

            const selectPerpus = document.getElementById("filterPerpustakaan<%=rnd%>");
            if (dataPerpus && dataPerpus.data) {
                dataPerpus.data.forEach(p => selectPerpus.appendChild(new Option(p.nama, p.id)));
                if (dataPerpus.data.length === 1) selectPerpus.value = dataPerpus.data[0].id;
            }

            const selectJenis = document.getElementById("filterJenis<%=rnd%>");
            if (dataJenis && dataJenis.data) {
                dataJenis.data.forEach(p => selectJenis.appendChild(new Option(p.nama, p.id)));
                if (dataJenis.data.length === 1) selectJenis.value = dataJenis.data[0].id;
            }

            const selectTipe = document.getElementById("filterTipe<%=rnd%>");
            if (dataTipe && dataTipe.data) {
                dataTipe.data.forEach(p => selectTipe.appendChild(new Option(p.nama, p.id)));
                if (dataTipe.data.length === 1) selectTipe.value = dataTipe.data[0].id;
            }
        } catch (error) { 
            console.error("Gagal memuat referensi parameter pencarian:", error);
        }
    }

    // Fungsi Penyusunan SQL Filter berdasarkan form yang diinput
    function buildFilterSQL<%=rnd%>() {
        let condition = " WHERE 1=1 AND a.qty > 0 AND a.kode_transaksi = 6 AND (b.aktif IS NULL OR b.aktif = true) AND (b.status_terbit_item IS NULL OR b.status_terbit_item IN (SELECT id FROM library.status_terbit_item WHERE lower(nama) IN ('terbit', 'publish', 'disetujui'))) ";
        
        // Membaca Nilai Pencarian Dasar
        const elJudul = document.getElementById("filterJudul<%=rnd%>");
        const elIsbn = document.getElementById("filterIsbn<%=rnd%>");
        const elIssn = document.getElementById("filterIssn<%=rnd%>");
        const judul = elJudul ? elJudul.value.trim().replace(/'/g, "''") : "";
        const isbn = elIsbn ? elIsbn.value.trim().replace(/'/g, "''") : "";
        const issn = elIssn ? elIssn.value.trim().replace(/'/g, "''") : "";
        
        // Membaca Nilai Pencarian Lanjutan
        const elKategori = document.getElementById("filterKategori<%=rnd%>");
        const kategori = elKategori ? elKategori.value.trim().replace(/'/g, "''") : "";
        
        const perpustakaanId = document.getElementById("filterPerpustakaan<%=rnd%>")?.value;
        const jenisId = document.getElementById("filterJenis<%=rnd%>")?.value;
        const tipeId = document.getElementById("filterTipe<%=rnd%>")?.value;

        const yayasanId = document.getElementById("filterYayasan<%=rnd%>")?.value;
        const sekolahId = document.getElementById("filterSekolah<%=rnd%>")?.value;
        const fakultasId = document.getElementById("filterFakultas<%=rnd%>")?.value;
        const jurusanId = document.getElementById("filterJurusan<%=rnd%>")?.value;

        // Implementasi SQL 
        if (judul) condition += " AND (b.nama ILIKE '%" + judul + "%' OR b.keterangan ILIKE '%" + judul + "%') ";
        if (isbn) condition += " AND (b.isbn ILIKE '%" + isbn + "%' OR b.isbn10 ILIKE '%" + isbn + "%') ";
        if (issn) condition += " AND b.issn ILIKE '%" + issn + "%' ";
        
        if (kategori) condition += " AND b.kategories ILIKE '%" + kategori + "%' ";
        if (perpustakaanId) condition += " AND a.perpustakaan = " + perpustakaanId + " ";
        if (jenisId) condition += " AND b.jenis_item = " + jenisId + " ";
        if (tipeId) condition += " AND b.tipe_item = " + tipeId + " ";
        
        if (yayasanId) condition += " AND b.sekolah IN (SELECT id FROM sekolah WHERE yayasan = " + yayasanId + ") ";
        if (sekolahId) condition += " AND b.sekolah = " + sekolahId + " ";
        if (fakultasId) condition += " AND b.jurusan IN (SELECT id FROM jurusan WHERE fakultas = " + fakultasId + ") ";
        if (jurusanId) condition += " AND b.jurusan = " + jurusanId + " ";

        return condition;
    }

    function getBookImage<%=rnd%>(item) {
        if (item.image_url && item.image_url.trim() !== "" && item.image_url.trim().startsWith("http")) { return item.image_url; } 
        else { const host = "<%=Common.ROOT%>"; return host + "/AmbilMedia?id=" + (item.id || "") + "&name=nama&foto=foto&clazz=ais.database.model.file.FotoGambarItem&property=item&height=152&width=114"; }
    }

    async function loadBuku<%=rnd%>(currentPage) {
        currentPage = parseInt(currentPage, 10) || 1;
        currentPageGlobal<%=rnd%> = currentPage;
        
        const grid = document.getElementById("gridBuku<%=rnd%>");
        const infoText = document.getElementById("infoHasil<%=rnd%>");
        if (grid) grid.innerHTML = "<div class='col-12 text-center py-5'><div class='spinner-border text-primary' role='status' style='width: 3rem; height: 3rem;'></div><h5 class='mt-4 text-muted fw-bold'>" + txtSedangMemuat<%=rnd%> + "</h5></div>";
        const paginationEl = document.getElementById("paginationBuku<%=rnd%>");
        if (paginationEl) paginationEl.innerHTML = ""; 
        if (infoText) infoText.innerHTML = "";

        const baseCondition = buildFilterSQL<%=rnd%>();
        const sqlFrom = " FROM library.detail_transaksi a INNER JOIN library.item b ON (a.item = b.id) " + baseCondition;
        const sqlCount = "SELECT COUNT(DISTINCT a.item) AS total_data" + sqlFrom;
        const offset = (currentPage - 1) * limitPerPage<%=rnd%>;
        const sqlData = "SELECT b.id, b.nama, b.pengarangs, b.kategories as tema, b.image_url" + sqlFrom + " GROUP BY b.id, b.nama, b.pengarangs, b.kategories, b.image_url ORDER BY count(a.id) DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset;

        const countResult = await fetchData<%=rnd%>(sqlCount);
        const dataResult = await fetchData<%=rnd%>(sqlData);

        if (!countResult || !dataResult) {
            if (grid) grid.innerHTML = "<div class='col-12 text-center text-danger py-5'><div class='bg-danger bg-opacity-10 p-4 rounded-circle d-inline-block mb-3'><i class='fa fa-exclamation-triangle fa-3x text-danger'></i></div><h5 class='fw-bold'>" + txtGagalMemuat<%=rnd%> + "</h5></div>";
            return;
        }

        let totalRecords = 0;
        if (countResult.length > 0) {
            let row = countResult[0];
            let val = row.total_data || row.TOTAL_DATA || (Array.isArray(row) ? row[0] : Object.values(row)[0]);
            totalRecords = parseInt(val, 10) || 0;
        }

        let bukuList = [];
        if (dataResult.length > 0) {
            bukuList = dataResult.map(row => {
                if(Array.isArray(row)) {
                    return { id: row[0], nama: row[1], pengarangs: row[2], tema: row[3], image_url: row[4] };
                } else {
                    return { 
                        id: row.id || row.ID, 
                        nama: row.nama || row.NAMA, 
                        pengarangs: row.pengarangs || row.PENGARANGS, 
                        tema: row.tema || row.TEMA || row.kategories || row.KATEGORIES,
                        image_url: row.image_url || row.IMAGE_URL 
                    };
                }
            });
        }
        
        renderBuku<%=rnd%>(bukuList); 
        updateInfoHasil<%=rnd%>(totalRecords, dataResult.length, currentPage);
        renderPagination<%=rnd%>(totalRecords, currentPage);
    }

    function renderBuku<%=rnd%>(bukuList) {
        const grid = document.getElementById("gridBuku<%=rnd%>");
        if (!grid) return;
        grid.innerHTML = "";

        if (bukuList.length === 0) { 
            grid.innerHTML = "<div class='col-12 text-center py-5'><i class='fa fa-folder-open-o fa-5x text-secondary opacity-50 mb-4'></i><h4 class='text-secondary fw-bold'>" + txtTidakDitemukan<%=rnd%> + "</h4></div>";
            return; 
        }

        let htmlContent = "";
        bukuList.forEach(item => {
            const imgSrc = getBookImage<%=rnd%>(item);
            const judul = item.nama || txtTanpaJudul<%=rnd%>;
            const pengarang = (item.pengarangs && item.pengarangs.trim() !== "") ? item.pengarangs : "-";
            const tema = (item.tema && item.tema.trim() !== "") ? item.tema : "-";
            const detailHref = '<%=Common.ROOT%>/pustaka?id=' + item.id;

            htmlContent += 
                "<div class='col'>" +
                    "<a href='" + detailHref + "' class='card h-100 book-card<%=rnd%> rounded-4 overflow-hidden text-decoration-none' onclick='event.preventDefault(); tampilkanDetail<%=rnd%>(\"" + item.id + "\")'>" +
                        "<div class='position-absolute top-0 end-0 p-2'><span class='badge bg-danger shadow-sm'><i class='fa fa-fire me-1'></i>Populer</span></div>" +
                        "<img src='" + imgSrc + "' class='card-img-top book-cover<%=rnd%>' alt='" + judul + "' onerror=\"this.src='https://via.placeholder.com/114x152?text=No+Cover'\">" +
                        "<div class='card-body d-flex flex-column p-4'>" +
                            "<h6 class='card-title fw-bold text-dark mb-3 line-clamp-2' title='" + judul + "'>" + judul + "</h6>" +
                            "<div class='mt-auto border-top pt-3'>" +
                                "<div class='small text-secondary mb-2 text-truncate-custom' title='" + pengarang + "'><i class='fa fa-user text-primary me-2 w-15px'></i>" + pengarang + "</div>" +
                                "<div class='small text-secondary mb-0 text-truncate-custom' title='" + tema + "'><i class='fa fa-tag text-primary me-2 w-15px'></i>" + tema + "</div>" +
                            "</div>" +
                        "</div>" +
                    "</a>" +
                "</div>";
        });
        grid.innerHTML = htmlContent;
    }

    async function tampilkanDetail<%=rnd%>(itemId) {
        const modalId = "modalDetail<%=rnd%>";
        const existingModal = document.getElementById(modalId);
        if (existingModal) existingModal.remove();

        try {
            const response = await fetch('<%=linkDetail%>&id=' + itemId + '&modalId=' + modalId);
            if (!response.ok) throw new Error("Gagal load halaman rincian");
            let modalHtml = await response.text();
            modalHtml = modalHtml.replace(/http:\/\/fonts\.googleapis\.com/g, 'https://fonts.googleapis.com');
            
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            const modalElement = document.getElementById(modalId);
            
            if (!modalElement) throw new Error("Elemen rincian buku tidak ditemukan.");
            
            if (typeof bootstrap !== 'undefined') {
                const modalInstance = new bootstrap.Modal(modalElement);
                modalInstance.show();
                modalElement.addEventListener('hidden.bs.modal', () => modalElement.remove());
            } else if (typeof window.jQuery !== 'undefined' && typeof window.jQuery(modalElement).modal === 'function') {
                window.jQuery(modalElement).modal('show');
                window.jQuery(modalElement).on('hidden.bs.modal', () => modalElement.remove());
            } else {
                modalElement.classList.add('show');
                modalElement.style.display = 'block';
                modalElement.style.backgroundColor = 'rgba(0,0,0,0.6)';
                modalElement.style.overflowY = 'auto';
                document.body.style.overflow = 'hidden';
                modalElement.querySelectorAll('[data-bs-dismiss="modal"], [data-dismiss="modal"], .btn-close').forEach(btn => {
                    btn.addEventListener('click', (e) => {
                        e.preventDefault(); modalElement.style.display = 'none'; modalElement.classList.remove('show'); document.body.style.overflow = 'auto'; modalElement.remove(); 
                    });
                });
            }
        } catch (error) {
            console.error(error);
            if(typeof tampilkanToast === 'function') { 
                tampilkanToast('<%=Common.getBahasaConfigJS("Gagal memuat rincian pustaka dari peladen. Silakan coba lagi.")%>', 'bg-danger'); 
            }
        }
    }

    function renderPagination<%=rnd%>(totalRecords, currentPage) {
        const totalPages = Math.ceil(totalRecords / limitPerPage<%=rnd%>);
        const paginationEl = document.getElementById("paginationBuku<%=rnd%>"); 
        if (!paginationEl) return;
        
        paginationEl.innerHTML = "";
        if (totalPages <= 1) return;
        
        let html = "<li class='page-item " + (currentPage === 1 ? "disabled" : "") + "'><a class='page-link border-0 text-secondary fw-bold px-3 rounded-pill mx-1' href='#' onclick='event.preventDefault(); if(" + currentPage + " > 1) loadBuku<%=rnd%>(" + (currentPage - 1) + ")'><i class='fa fa-chevron-left me-2'></i>" + txtSebelumnya<%=rnd%> + "</a></li>";
        
        let startPage = Math.max(1, currentPage - 2); 
        let endPage = Math.min(totalPages, currentPage + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            let activeClass = (i === currentPage) ? "active shadow-sm" : ""; 
            let btnStyle = (i === currentPage) ? "background-color: #0d6efd; color: white;" : "color: #6c757d;";
            html += "<li class='page-item mx-1 " + activeClass + " rounded-circle'><a class='page-link border-0 rounded-circle fw-bold' style='width: 45px; height: 45px; display: flex; align-items: center; justify-content: center; " + btnStyle + "' href='#' onclick='event.preventDefault(); loadBuku<%=rnd%>(" + i + ")'>" + i + "</a></li>";
        }
        
        html += "<li class='page-item " + (currentPage === totalPages ? "disabled" : "") + "'><a class='page-link border-0 text-secondary fw-bold px-3 rounded-pill mx-1' href='#' onclick='event.preventDefault(); if(" + currentPage + " < " + totalPages + ") loadBuku<%=rnd%>(" + (currentPage + 1) + ")'>" + txtSelanjutnya<%=rnd%> + "<i class='fa fa-chevron-right ms-2'></i></a></li>";
        
        paginationEl.innerHTML = html;
    }

    function updateInfoHasil<%=rnd%>(totalRecords, currentCount, currentPage) {
        const infoText = document.getElementById("infoHasil<%=rnd%>");
        if (!infoText) return;
        
        if (totalRecords > 0) {
            let start = ((currentPage - 1) * limitPerPage<%=rnd%>) + 1;
            let end = start + currentCount - 1;
            infoText.innerHTML = '<%=Common.getBahasaConfigJS("Menampilkan referensi")%>' + " <span class='text-primary fw-bold mx-1'>" + start + "-" + end + "</span> " + '<%=Common.getBahasaConfigJS("dari total keseluruhan")%>' + " <span class='text-primary fw-bold mx-1'>" + totalRecords + "</span> " + '<%=Common.getBahasaConfigJS("koleksi populer")%>' + ".";
        } else {
            infoText.innerHTML = "";
        }
    }

    function resetFilter<%=rnd%>() { 
        const form = document.getElementById("formPencarian<%=rnd%>");
        if(form) { 
            form.reset(); 
            loadMasterData<%=rnd%>().then(() => loadBuku<%=rnd%>(1)); 
        } 
        else { 
            loadBuku<%=rnd%>(1); 
        }
    }

    // Eksekusi Pemuatan Data Pertama Kali
    loadMasterData<%=rnd%>().then(() => { loadBuku<%=rnd%>(1); });
</script>
