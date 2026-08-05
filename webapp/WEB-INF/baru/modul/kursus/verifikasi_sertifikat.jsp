<%
    boolean valid = Boolean.TRUE.equals(request.getAttribute("valid"));
    String kode = (String) request.getAttribute("kode");
%>
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex,nofollow">
<title>Verifikasi Sertifikat Kursus</title>
<style>
    * { box-sizing: border-box; }
    body { margin: 0; font-family: -apple-system, "Segoe UI", Roboto, Arial, sans-serif; background: #f1f5f9; color: #0f172a; padding: 32px 16px; }
    .kr-vf-card { max-width: 560px; margin: 0 auto; background: #fff; border-radius: 1.25rem; box-shadow: 0 12px 32px rgba(15,23,42,.10); overflow: hidden; }
    .kr-vf-head { padding: 28px 28px 20px; text-align: center; color: #fff; background: linear-gradient(120deg,#3730a3 0%,#4f46e5 45%,#7c3aed 100%); }
    .kr-vf-head.kr-vf-invalid { background: linear-gradient(120deg,#7f1d1d,#dc2626); }
    .kr-vf-head i { font-size: 2.5rem; margin-bottom: 8px; display: block; }
    .kr-vf-head h1 { font-size: 1.15rem; margin: 0; font-weight: 700; }
    .kr-vf-body { padding: 24px 28px 28px; }
    .kr-vf-row { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; border-bottom: 1px solid rgba(15,23,42,.08); }
    .kr-vf-row:last-child { border-bottom: none; }
    .kr-vf-label { color: #64748b; font-size: .85rem; }
    .kr-vf-value { font-weight: 600; text-align: right; }
    .kr-vf-badge { display: inline-block; padding: 4px 12px; border-radius: 999px; font-size: .8rem; font-weight: 700; }
    .kr-vf-badge-ok { background: #ecfdf5; color: #16a34a; }
    .kr-vf-badge-bad { background: #fef2f2; color: #dc2626; }
    .kr-vf-foot { text-align: center; padding: 16px 28px 26px; color: #94a3b8; font-size: .78rem; }
</style>
</head>
<body>
    <div class="kr-vf-card">
        <% if (valid) { %>
        <div class="kr-vf-head">
            <i class="fas fa-certificate">&#127942;</i>
            <h1>Sertifikat Terverifikasi</h1>
        </div>
        <div class="kr-vf-body">
            <div class="kr-vf-row"><span class="kr-vf-label">Nomor Sertifikat</span><span class="kr-vf-value"><%= esc((String) request.getAttribute("nomorSertifikat")) %></span></div>
            <div class="kr-vf-row"><span class="kr-vf-label">Nama Peserta</span><span class="kr-vf-value"><%= esc((String) request.getAttribute("namaPeserta")) %></span></div>
            <div class="kr-vf-row"><span class="kr-vf-label">Kursus</span><span class="kr-vf-value"><%= esc((String) request.getAttribute("namaKursus")) %></span></div>
            <div class="kr-vf-row"><span class="kr-vf-label">Instruktur</span><span class="kr-vf-value"><%= esc((String) request.getAttribute("namaInstruktur")) %></span></div>
            <div class="kr-vf-row"><span class="kr-vf-label">Institusi Penerbit</span><span class="kr-vf-value"><%= esc((String) request.getAttribute("namaInstitusi")) %></span></div>
            <div class="kr-vf-row"><span class="kr-vf-label">Tanggal Terbit</span><span class="kr-vf-value"><%= esc((String) request.getAttribute("tanggalTerbit")) %></span></div>
            <% String st = (String) request.getAttribute("statusSertifikat"); boolean aktif = "Aktif".equals(st); %>
            <div class="kr-vf-row"><span class="kr-vf-label">Status</span><span class="kr-vf-badge <%= aktif ? "kr-vf-badge-ok" : "kr-vf-badge-bad" %>"><%= aktif ? "Valid" : "Dicabut" %></span></div>
            <div style="text-align:center;margin-top:18px;">
                <img src="<%= request.getContextPath() %>/VerifikasiSertifikatKursus?qr=1&amp;kode=<%= java.net.URLEncoder.encode(kode, "UTF-8") %>"
                     alt="QR Verifikasi" width="140" height="140" style="border:1px solid rgba(15,23,42,.08);border-radius:.75rem;">
            </div>
        </div>
        <% } else { %>
        <div class="kr-vf-head kr-vf-invalid">
            <i>&#10060;</i>
            <h1>Sertifikat Tidak Ditemukan</h1>
        </div>
        <div class="kr-vf-body">
            <p style="text-align:center;color:#64748b;margin:0;">Kode verifikasi tidak valid atau sertifikat tidak ditemukan. Pastikan tautan/QR yang dipindai sudah benar.</p>
        </div>
        <% } %>
        <div class="kr-vf-foot">Halaman verifikasi publik &mdash; hanya menampilkan informasi keabsahan sertifikat.</div>
    </div>
</body>
</html>
<%!
    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
%>
