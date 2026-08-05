<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    boolean isLogin = (tbmuser != null && tbmuser.getUserId() != null);
    String serviceUrl = Common.ROOT + "/krrs?hanya_tampil_jsp=true&p=kursus&s=_kursus_service";
    String enrollmentId = request.getParameter("enrollmentId");
    if (enrollmentId == null) enrollmentId = "";
%>
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex,nofollow">
<title>Sertifikat Kursus</title>
<style>
    * { box-sizing: border-box; }
    body { margin: 0; font-family: Georgia, "Times New Roman", serif; background: #e2e8f0; padding: 32px 16px; }
    .kr-sf-toolbar { max-width: 900px; margin: 0 auto 16px; text-align: right; }
    .kr-sf-toolbar button {
        border: none; border-radius: 999px; padding: 10px 22px; font-weight: 700; cursor: pointer;
        background: #4f46e5; color: #fff; font-family: -apple-system, "Segoe UI", Roboto, Arial, sans-serif;
    }
    .kr-sf-cert {
        max-width: 900px; margin: 0 auto; background: #fff; padding: 56px 64px; border: 10px solid #4f46e5;
        outline: 2px solid #c7d2fe; outline-offset: -22px; text-align: center; position: relative;
    }
    .kr-sf-cert h2 { font-size: .95rem; letter-spacing: 4px; color: #64748b; text-transform: uppercase; margin: 0 0 4px; font-family: -apple-system, "Segoe UI", Roboto, Arial, sans-serif; }
    .kr-sf-cert h1 { font-size: 2.4rem; margin: 0 0 26px; color: #1e1b4b; }
    .kr-sf-cert .kr-sf-nama { font-size: 2rem; color: #4f46e5; margin: 18px 0; font-weight: bold; border-bottom: 2px solid #c7d2fe; display: inline-block; padding-bottom: 8px; }
    .kr-sf-cert .kr-sf-sub { color: #334155; font-size: 1.05rem; margin: 6px 0; }
    .kr-sf-cert .kr-sf-kursus { font-size: 1.4rem; font-weight: bold; color: #1e1b4b; margin: 14px 0 22px; }
    .kr-sf-meta { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 46px; font-family: -apple-system, "Segoe UI", Roboto, Arial, sans-serif; }
    .kr-sf-meta .kr-sf-col { text-align: left; font-size: .82rem; color: #475569; }
    .kr-sf-meta .kr-sf-col b { display: block; color: #0f172a; font-size: .9rem; }
    .kr-sf-qr { text-align: center; }
    .kr-sf-qr img { width: 90px; height: 90px; }
    .kr-sf-qr div { font-size: .68rem; color: #94a3b8; margin-top: 4px; font-family: -apple-system, "Segoe UI", Roboto, Arial, sans-serif; }
    .kr-sf-empty { max-width: 640px; margin: 60px auto; text-align: center; color: #475569; font-family: -apple-system, "Segoe UI", Roboto, Arial, sans-serif; }
    @media print {
        body { background: #fff; padding: 0; }
        .kr-sf-toolbar { display: none; }
        .kr-sf-cert { border-width: 6px; box-shadow: none; }
    }
</style>
</head>
<body>
<% if (!isLogin) { %>
    <div class="kr-sf-empty"><h3>Sesi login berakhir</h3><p>Silakan login kembali untuk melihat sertifikat Anda.</p></div>
<% } else { %>
    <div class="kr-sf-toolbar no-print">
        <button onclick="window.print()">Cetak / Unduh PDF</button>
    </div>
    <div id="krSfRoot"></div>
    <script>
        (function () {
            var enrollmentId = "<%= enrollmentId.replace("\"", "") %>";
            var serviceUrl = "<%= serviceUrl %>";
            var root = document.getElementById("krSfRoot");
            function esc(s) {
                return (s == null ? "" : String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");
            }
            fetch(serviceUrl, {
                method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams({ action: "get_sertifikat", enrollmentId: enrollmentId })
            }).then(function (r) { return r.json(); }).then(function (r) {
                if (r.status !== "success") {
                    root.innerHTML = '<div class="kr-sf-empty"><h3>Sertifikat belum tersedia</h3><p>' + esc(r.message || "Selesaikan seluruh materi kursus untuk mendapatkan sertifikat.") + '</p></div>';
                    return;
                }
                var d = r.data;
                var qrUrl = "<%=request.getContextPath()%>/VerifikasiSertifikatKursus?qr=1&kode=" + encodeURIComponent(d.kodeVerifikasi);
                var verifUrl = "<%=request.getContextPath()%>/VerifikasiSertifikatKursus?kode=" + encodeURIComponent(d.kodeVerifikasi);
                root.innerHTML =
                    '<div class="kr-sf-cert">' +
                        '<h2>Sertifikat Kelulusan</h2>' +
                        '<h1>Certificate of Completion</h1>' +
                        '<div class="kr-sf-sub">Dengan ini menyatakan bahwa</div>' +
                        '<div class="kr-sf-nama">' + esc(d.namaPeserta) + '</div>' +
                        '<div class="kr-sf-sub">telah berhasil menyelesaikan kursus</div>' +
                        '<div class="kr-sf-kursus">' + esc(d.namaKursus) + '</div>' +
                        (d.namaInstruktur ? '<div class="kr-sf-sub">diampu oleh ' + esc(d.namaInstruktur) + '</div>' : '') +
                        '<div class="kr-sf-meta">' +
                            '<div class="kr-sf-col">' +
                                '<span>Nomor Sertifikat</span><b>' + esc(d.nomorSertifikat) + '</b>' +
                                '<span>Tanggal Terbit</span><b>' + esc(d.tanggalTerbit) + '</b>' +
                                (d.nilaiAkhir != null ? '<span>Nilai Akhir</span><b>' + Number(d.nilaiAkhir).toFixed(1) + '</b>' : '') +
                                '<span>Institusi</span><b>' + esc(d.namaInstitusi) + '</b>' +
                            '</div>' +
                            '<div class="kr-sf-qr"><img src="' + qrUrl + '" alt="QR Verifikasi"><div>Pindai untuk verifikasi<br>' + esc(verifUrl) + '</div></div>' +
                        '</div>' +
                    '</div>';
            }).catch(function () {
                root.innerHTML = '<div class="kr-sf-empty"><h3>Gagal memuat sertifikat</h3><p>Terjadi kesalahan koneksi.</p></div>';
            });
        })();
    </script>
<% } %>
</body>
</html>
