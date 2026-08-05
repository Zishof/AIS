<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    out.print("{\"status\":\"error\", \"message\":\""
    + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
    return;
}

Mahasiswa currentMahasiswa = tbmuser.getMahasiswa();
Dosen currentDosen = tbmuser.ambilDosen();
Siswa currentSiswa = tbmuser.getSiswa();
Guru currentGuru = tbmuser.ambilGuru();
Pegawai currentPegawai = tbmuser.ambilPegawai();

boolean admin = currentMahasiswa == null && currentDosen == null && currentSiswa == null && currentGuru == null;
boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
boolean pt = ptYa[0];
boolean ya = ptYa[1];

// Build baseWhere for role-scoped SQL query (will be formatted per-tipe in JS)
String baseWhereMhs = " WHERE p.id IS NOT NULL ";
if (currentMahasiswa != null) {
    baseWhereMhs += " AND p.mahasiswa = " + currentMahasiswa.getId();
} else if (tbmuser.getJurusan() != null) {
    baseWhereMhs += " AND p.jurusan = " + tbmuser.getJurusan().getId();
} else if (tbmuser.getFakultas() != null) {
    baseWhereMhs += " AND p.fakultas = " + tbmuser.getFakultas().getId();
}

String baseWhereDosen = " WHERE p.id IS NOT NULL ";
if (currentDosen != null) {
    baseWhereDosen += " AND p.dosen = " + currentDosen.getId();
}

String baseWhereSiswa = " WHERE p.id IS NOT NULL ";
if (currentSiswa != null) {
    baseWhereSiswa += " AND p.siswa = " + currentSiswa.getId();
} else if (tbmuser.getSekolah() != null) {
    baseWhereSiswa += " AND p.sekolah = " + tbmuser.getSekolah().getId();
} else if (tbmuser.getYayasan() != null) {
    baseWhereSiswa += " AND p.yayasan = " + tbmuser.getYayasan().getId();
}

String baseWherePegawai = " WHERE p.id IS NOT NULL ";
if (currentPegawai != null) {
    baseWherePegawai += " AND p.pegawai = " + currentPegawai.getId();
}

// Default tipe shown
String defaultTipe = "mahasiswa";
if (currentSiswa != null || (!pt && ya)) defaultTipe = "siswa";
else if (currentDosen != null || currentGuru != null) defaultTipe = "dosen";
else if (admin && !pt && !ya) defaultTipe = "pegawai";

String rnd = Common.getGeneratedBarCode(7);

String lblBelumAda  = Common.getBahasaConfig("Belum ada catatan yang tersedia");
String lblKeterangan= Common.getBahasaConfig("Catatan akan muncul dari kolom Keterangan pada data prestasi yang telah diisi.");
String lblMemuat    = Common.getBahasaConfig("Memuat catatan...");
String lblRefresh   = Common.getBahasaConfig("Refresh");
String lblCari      = Common.getBahasaConfig("Cari nama prestasi atau catatan...");
String lblSemua     = Common.getBahasaConfig("Semua");
String lblStatus    = Common.getBahasaConfig("Status");
String lblData      = Common.getBahasaConfig("Data");
String lblTanggal   = Common.getBahasaConfig("Tanggal");
String lblOleh      = Common.getBahasaConfig("Oleh");
%>

<div class="animate__animated animate__fadeInUp">
<div class="card border-0 shadow-sm rounded-4 border-top border-info border-4">
    <div class="card-header bg-white border-0 pt-4 pb-3 px-4">
        <div class="d-flex align-items-start justify-content-between flex-wrap gap-2">
            <div>
                <h5 class="fw-bold text-dark mb-1">
                    <i class="fas fa-sticky-note text-info me-2"></i><%=Common.getBahasaConfig("Catatan Prestasi & Kegiatan")%>
                </h5>
                <p class="text-muted small mb-0">
                    <%=Common.getBahasaConfig("Ringkasan catatan dan keterangan dari seluruh data prestasi yang telah diisi.")%>
                </p>
            </div>
            <% if (admin) { %>
            <div class="d-flex align-items-center gap-2 flex-wrap">
                <label class="fw-semibold small text-muted mb-0"><%=lblData%>:</label>
                <select class="form-select form-select-sm" id="pilihTipe_<%=rnd%>" style="width:auto;" onchange="muat<%=rnd%>()">
                    <% if (pt) { %>
                    <option value="mahasiswa" <%= "mahasiswa".equals(defaultTipe) ? "selected" : "" %>><%=Common.getBahasaConfig("Mahasiswa")%></option>
                    <% } %>
                    <% if (ya) { %>
                    <option value="siswa" <%= "siswa".equals(defaultTipe) ? "selected" : "" %>><%=Common.getBahasaConfig("Siswa")%></option>
                    <% } %>
                    <% if (pt) { %>
                    <option value="dosen" <%= "dosen".equals(defaultTipe) ? "selected" : "" %>><%=Common.getBahasaConfig("Dosen")%></option>
                    <% } %>
                    <option value="pegawai" <%= "pegawai".equals(defaultTipe) ? "selected" : "" %>><%=Common.getBahasaConfig("Pegawai")%></option>
                </select>
            </div>
            <% } %>
        </div>
    </div>

    <div class="card-body px-4 pt-0 pb-4">
        <!-- Toolbar -->
        <div class="d-flex align-items-center gap-2 flex-wrap mb-3">
            <div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;">
                <span class="input-group-text bg-white border-end-0">
                    <i class="fas fa-search text-muted"></i>
                </span>
                <input type="text" class="form-control border-start-0 ps-0 bg-white fw-medium"
                    id="cariCatatan_<%=rnd%>"
                    placeholder="<%=lblCari%>"
                    oninput="filterLokal<%=rnd%>(this.value)">
                <button class="btn btn-outline-secondary btn-sm" onclick="document.getElementById('cariCatatan_<%=rnd%>').value='';filterLokal<%=rnd%>('');">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <select class="form-select form-select-sm" id="filterStatus_<%=rnd%>" style="width:auto;" onchange="muat<%=rnd%>()">
                <option value=""><%=lblSemua%> <%=lblStatus%></option>
                <option value="Disetujui"><%=Common.getBahasaConfig("Disetujui")%></option>
                <option value="Sedang diproses"><%=Common.getBahasaConfig("Sedang Diproses")%></option>
                <option value="Belum diproses"><%=Common.getBahasaConfig("Belum Diproses")%></option>
                <option value="Ditolak"><%=Common.getBahasaConfig("Ditolak")%></option>
            </select>
            <button class="btn btn-outline-primary btn-sm fw-semibold" onclick="muat<%=rnd%>()">
                <i class="fas fa-sync me-1"></i><%=lblRefresh%>
            </button>
            <span class="text-muted small ms-auto" id="jumlahCatatan_<%=rnd%>"></span>
        </div>

        <!-- Notes container -->
        <div id="catatanKonten_<%=rnd%>">
            <div class="text-center py-5">
                <div class="spinner-border text-info spinner-border-sm"></div>
                <p class="text-muted small mt-2"><%=lblMemuat%></p>
            </div>
        </div>

        <!-- Empty state -->
        <div id="catatanKosong_<%=rnd%>" class="text-center py-5 d-none">
            <i class="fas fa-inbox fa-3x text-muted mb-3 d-block"></i>
            <p class="text-muted fw-semibold mb-1"><%=lblBelumAda%></p>
            <small class="text-muted"><%=lblKeterangan%></small>
        </div>
    </div>
</div>
</div>

<script>
(function(){
    var ROOT = '<%=Common.ROOT%>';
    var defaultTipe = '<%=defaultTipe%>';
    var isAdmin = <%=admin%>;

    // SQL baseWhere strings injected from server
    var baseWhereMhs     = '<%=baseWhereMhs.replace("'", "\\'")%>';
    var baseWhereDosen   = '<%=baseWhereDosen.replace("'", "\\'")%>';
    var baseWhereSiswa   = '<%=baseWhereSiswa.replace("'", "\\'")%>';
    var baseWherePegawai = '<%=baseWherePegawai.replace("'", "\\'")%>';

    var lblOleh    = '<%=lblOleh%>';
    var lblTanggal = '<%=lblTanggal%>';

    async function fetchSql(sql) {
        try {
            var res = await fetch(ROOT + '/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'sql', sql: sql })
            });
            var result = await res.json();
            return result.data || [];
        } catch(e) {
            console.error('Catatan SQL error:', e);
            return [];
        }
    }

    function badgeStatus(st) {
        var map = { 'Disetujui': 'bg-success', 'Ditolak': 'bg-danger', 'Sedang diproses': 'bg-info text-dark', 'Belum diproses': 'bg-warning text-dark' };
        return '<span class="badge ' + (map[st] || 'bg-secondary') + ' rounded-pill">' + (st || '-') + '</span>';
    }

    function fmtTgl(val) {
        if (!val) return '-';
        var d = new Date(val);
        return isNaN(d.getTime()) ? val : d.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' });
    }

    function renderNotes(rows) {
        var konten = document.getElementById('catatanKonten_<%=rnd%>');
        var kosong = document.getElementById('catatanKosong_<%=rnd%>');
        var jml    = document.getElementById('jumlahCatatan_<%=rnd%>');

        if (!rows || rows.length === 0) {
            konten.innerHTML = '';
            kosong.classList.remove('d-none');
            if (jml) jml.textContent = '';
            return;
        }
        kosong.classList.add('d-none');
        if (jml) jml.textContent = rows.length + ' <%=Common.getBahasaConfig("catatan")%>';

        var html = '<div class="row g-3">';
        rows.forEach(function(d) {
            var nama     = d.namaprestasi || d.nama_prestasi || d.judul || '-';
            var ket      = d.keterangan   || '';
            var tgl      = fmtTgl(d.tanggalmulai || d.tanggal_mulai);
            var status   = d.status       || '';
            var pemilik  = d.nama_pemilik || d.nama || '';
            var kategori = d.kategori     || '';
            var cabang   = d.cabang       || '';

            html += '<div class="col-12 col-md-6 col-xl-4 catatan-item"' +
                    ' data-cari="' + (nama + ' ' + ket).toLowerCase().replace(/"/g, '') + '">';
            html += '<div class="card border-0 shadow-sm rounded-3 h-100" style="border-left:4px solid #0dcaf0!important;">';
            html += '<div class="card-body p-3">';

            // Header: nama + status
            html += '<div class="d-flex align-items-start justify-content-between gap-2 mb-2">';
            html += '<div class="fw-semibold text-dark" style="font-size:.9rem;line-height:1.3;">' + nama + '</div>';
            html += badgeStatus(status);
            html += '</div>';

            // Keterangan (notes text)
            html += '<p class="text-muted mb-3 fst-italic" style="font-size:.85rem;white-space:pre-line;">&ldquo;' + ket + '&rdquo;</p>';

            // Meta: kategori, cabang
            if (kategori || cabang) {
                html += '<div class="d-flex flex-wrap gap-1 mb-2">';
                if (kategori) html += '<span class="badge bg-light text-dark border" style="font-size:.72rem;"><i class="fas fa-tag me-1 text-info"></i>' + kategori + '</span>';
                if (cabang)   html += '<span class="badge bg-light text-dark border" style="font-size:.72rem;"><i class="fas fa-sitemap me-1 text-secondary"></i>' + cabang + '</span>';
                html += '</div>';
            }

            // Footer: pemilik + tanggal
            html += '<div class="d-flex justify-content-between align-items-center mt-2 pt-2 border-top flex-wrap gap-1">';
            if (pemilik) {
                html += '<small class="text-muted"><i class="fas fa-user me-1 text-info"></i>' + lblOleh + ': <strong>' + pemilik + '</strong></small>';
            } else {
                html += '<span></span>';
            }
            html += '<small class="text-muted"><i class="far fa-calendar me-1"></i>' + tgl + '</small>';
            html += '</div>';

            html += '</div></div></div>';
        });
        html += '</div>';
        konten.innerHTML = html;
    }

    function buildSql(tipe, statusFilter) {
        var addStatus = statusFilter ? " AND p.status = '" + statusFilter.replace(/'/g, "''") + "' " : '';
        var addKet    = " AND p.keterangan IS NOT NULL AND trim(p.keterangan) <> '' ";
        var limit     = ' LIMIT 200 ';
        var sql = '';

        if (tipe === 'mahasiswa') {
            sql = "SELECT m.nama as nama_pemilik, m.nim, p.namaprestasi, p.keterangan, p.status, p.tanggalmulai, " +
                  " k.nama as kategori, c.nama as cabang " +
                  " FROM public.prestasi_mahasiswa p " +
                  " LEFT JOIN public.mahasiswa m ON p.mahasiswa = m.id " +
                  " LEFT JOIN public.kategori_prestasi_mahasiswa k ON p.kategori_prestasi_mahasiswa = k.id " +
                  " LEFT JOIN public.cabang_prestasi_mahasiswa c ON p.cabang_prestasi_mahasiswa = c.id " +
                  baseWhereMhs + addKet + addStatus +
                  " ORDER BY p.tanggalmulai DESC " + limit;
        } else if (tipe === 'siswa') {
            sql = "SELECT sw.nama as nama_pemilik, p.namaprestasi, p.keterangan, p.status, p.tanggalmulai, " +
                  " k.nama as kategori, c.nama as cabang " +
                  " FROM public.prestasi_siswa p " +
                  " LEFT JOIN public.siswa sw ON p.siswa = sw.id " +
                  " LEFT JOIN public.kategori_prestasi_siswa k ON p.kategori_prestasi_siswa = k.id " +
                  " LEFT JOIN public.cabang_prestasi_siswa c ON p.cabang_prestasi_siswa = c.id " +
                  baseWhereSiswa + addKet + addStatus +
                  " ORDER BY p.tanggalmulai DESC " + limit;
        } else if (tipe === 'dosen') {
            sql = "SELECT d.nama as nama_pemilik, d.nidn, p.namaprestasi, p.keterangan, p.status, p.tanggalmulai, " +
                  " k.nama as kategori, c.nama as cabang " +
                  " FROM public.prestasi_dosen p " +
                  " LEFT JOIN public.dosen d ON p.dosen = d.id " +
                  " LEFT JOIN public.kategori_prestasi_dosen k ON p.kategori_prestasi_dosen = k.id " +
                  " LEFT JOIN public.cabang_prestasi_dosen c ON p.cabang_prestasi_dosen = c.id " +
                  baseWhereDosen + addKet + addStatus +
                  " ORDER BY p.tanggalmulai DESC " + limit;
        } else if (tipe === 'pegawai') {
            sql = "SELECT pg.nama as nama_pemilik, p.namaprestasi, p.keterangan, p.status, p.tanggalmulai, " +
                  " k.nama as kategori, c.nama as cabang " +
                  " FROM public.prestasi_pegawai p " +
                  " LEFT JOIN public.pegawai pg ON p.pegawai = pg.id " +
                  " LEFT JOIN public.kategori_prestasi_pegawai k ON p.kategori_prestasi_pegawai = k.id " +
                  " LEFT JOIN public.cabang_prestasi_pegawai c ON p.cabang_prestasi_pegawai = c.id " +
                  baseWherePegawai + addKet + addStatus +
                  " ORDER BY p.tanggalmulai DESC " + limit;
        }
        return sql;
    }

    window['filterLokal<%=rnd%>'] = function(q) {
        q = (q || '').toLowerCase();
        var items = document.querySelectorAll('#catatanKonten_<%=rnd%> .catatan-item');
        items.forEach(function(el) {
            var cari = (el.getAttribute('data-cari') || '');
            el.style.display = (!q || cari.includes(q)) ? '' : 'none';
        });
    };

    window['muat<%=rnd%>'] = async function() {
        var konten = document.getElementById('catatanKonten_<%=rnd%>');
        var kosong = document.getElementById('catatanKosong_<%=rnd%>');
        var jml    = document.getElementById('jumlahCatatan_<%=rnd%>');

        konten.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-info spinner-border-sm"></div>' +
                           '<p class="text-muted small mt-2"><%=lblMemuat%></p></div>';
        kosong.classList.add('d-none');
        if (jml) jml.textContent = '';

        var tipe = defaultTipe;
        if (isAdmin) {
            var sel = document.getElementById('pilihTipe_<%=rnd%>');
            if (sel) tipe = sel.value;
        }
        var statusFilter = (document.getElementById('filterStatus_<%=rnd%>') || {}).value || '';
        var sql = buildSql(tipe, statusFilter);
        if (!sql) return;

        var rows = await fetchSql(sql);
        renderNotes(rows);
    };

    document.addEventListener('DOMContentLoaded', function() {
        window['muat<%=rnd%>']();
    });
})();
</script>
