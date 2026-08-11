<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%--
  Backoffice ringkas Pendaftaran Tenant (§15) -- di-forward HANYA dari
  PendaftaranTenantServlet mode=admin SETELAH gerbang privilege (root/role Administrator).
  Setiap aksi POST admin_* diperiksa ulang privilege + CSRF di server per-request.
  Akses: <ctx>/pendaftaran?mode=admin (buka saat sudah login sbg admin platform).
--%>
<%
	String csrfToken = (String) request.getAttribute("csrfToken");
	if (csrfToken == null) csrfToken = "";
%>
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>Backoffice Pendaftaran Tenant</title>
<style>
body{margin:0;font-family:'Segoe UI',system-ui,Arial,sans-serif;background:#f4f6fb;color:#111827}
.topbar{background:#083f8c;color:#fff;padding:12px 20px;font-weight:600}
.wrap{max-width:1180px;margin:0 auto;padding:16px 12px}
.kartu{background:#fff;border:1px solid #e5e7eb;border-radius:10px;padding:14px;margin-bottom:12px}
input,select{padding:8px 10px;border:1px solid #cbd5e1;border-radius:7px;font-size:.85rem}
button{border:0;border-radius:7px;padding:8px 14px;font-size:.8rem;font-weight:600;cursor:pointer;background:#0b5ed7;color:#fff}
button.abu{background:#eef2f7;color:#111827}
button.merah{background:#fee2e2;color:#b91c1c}
button.hijau{background:#dcfce7;color:#166534}
table{width:100%;border-collapse:collapse;font-size:.8rem}
th,td{border-bottom:1px solid #e5e7eb;padding:8px 6px;text-align:left;vertical-align:top}
th{background:#f8fafc;font-size:.72rem;text-transform:uppercase;color:#6b7280}
.badge{display:inline-block;border-radius:12px;padding:2px 9px;font-size:.7rem;font-weight:700;background:#e0e7ff;color:#3730a3;white-space:nowrap}
.aksi{display:flex;gap:4px;flex-wrap:wrap}
#hasilAksi{font-size:.82rem;margin-top:8px;min-height:1.2em}
#detailStep{font-size:.78rem;white-space:pre-wrap;background:#0f172a;color:#e2e8f0;border-radius:8px;padding:10px;display:none}
.tabelwrap{overflow-x:auto}
</style>
</head>
<body>
<div class="topbar">Backoffice Pendaftaran Tenant</div>
<div class="wrap">
	<div class="kartu">
		<div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">
			<input type="text" id="q" placeholder="Cari kode/nama/email/username" style="flex:1;min-width:200px">
			<select id="statusFilter">
				<option value="">Semua status</option>
				<option>EMAIL_VERIFICATION_PENDING</option><option>VERIFIED</option>
				<option>REVIEW_PENDING</option><option>PROVISIONING_QUEUED</option>
				<option>PROVISIONING</option><option>PROVISIONING_FAILED</option>
				<option>READY</option><option>ACTIVE</option>
				<option>REJECTED</option><option>CANCELLED</option><option>EXPIRED</option>
			</select>
			<button id="btnMuat">Muat</button>
		</div>
		<div id="hasilAksi" role="status"></div>
	</div>
	<div class="kartu tabelwrap">
		<table>
			<thead><tr>
				<th>Kode</th><th>Usaha / Email</th><th>Username</th><th>Jenis Usaha</th>
				<th>Status</th><th>Provisioning</th><th>Aksi</th>
			</tr></thead>
			<tbody id="isiTabel"><tr><td colspan="7">Klik "Muat".</td></tr></tbody>
		</table>
	</div>
	<div class="kartu"><pre id="detailStep"></pre></div>
</div>
<script>
(function(){
"use strict";
var URL_API = '<%=Common.ROOT%>/pendaftaran';
var CSRF = '<%=csrfToken%>';
function kirim(action, data){
	var body = new URLSearchParams(data || {});
	body.append('action', action); body.append('csrf', CSRF);
	return fetch(URL_API, { method:'POST', credentials:'include',
		headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
		body: body.toString() }).then(function(r){ return r.json(); });
}
function el(id){ return document.getElementById(id); }
function info(pesan, sukses){
	var e = el('hasilAksi');
	e.textContent = pesan || '';
	e.style.color = sukses ? '#166534' : '#b91c1c';
}
function aksi(nama, kode, butuhAlasan){
	var data = { kode: kode };
	if (butuhAlasan){
		var alasan = window.prompt('Alasan (wajib) untuk ' + nama + ' ' + kode + ':');
		if (alasan === null) return;
		data.reason = alasan;
	}
	kirim(nama, data).then(function(r){
		info(r.description || r.code, r.status === '00');
		muat();
	});
}
function tombol(label, kelas, onclick){
	var b = document.createElement('button');
	b.textContent = label; if (kelas) b.className = kelas;
	b.addEventListener('click', onclick);
	return b;
}
function muat(){
	kirim('admin_list', { q: el('q').value, statusFilter: el('statusFilter').value, halaman: '1' })
	.then(function(r){
		var tb = el('isiTabel'); tb.innerHTML = '';
		if (r.status !== '00'){ info(r.description || 'Gagal memuat', false); return; }
		var rows = r.data || [];
		if (!rows.length){ tb.innerHTML = '<tr><td colspan="7">Tidak ada data.</td></tr>'; return; }
		for (var i = 0; i < rows.length; i++){
			(function(d){
				var tr = document.createElement('tr');
				function td(isi){ var c = document.createElement('td'); c.textContent = isi || '-'; tr.appendChild(c); return c; }
				td(d.kode);
				td(d.namaUsaha + '\n' + d.email);
				td(d.username);
				td(d.jenisUsaha);
				var cStatus = document.createElement('td');
				var badge = document.createElement('span'); badge.className = 'badge';
				badge.textContent = d.statusPermohonan; cStatus.appendChild(badge);
				if (d.failureMessage){ var fm = document.createElement('div'); fm.textContent = d.failureMessage;
					fm.style.cssText = 'color:#b91c1c;font-size:.72rem;margin-top:3px'; cStatus.appendChild(fm); }
				tr.appendChild(cStatus);
				td((d.jobStatus || '-') + (d.jobStage ? (' @ ' + d.jobStage) : '')
					+ (d.tenantStatus ? (' | tenant ' + d.tenantStatus) : ''));
				var cAksi = document.createElement('td');
				var w = document.createElement('div'); w.className = 'aksi';
				w.appendChild(tombol('Step', 'abu', function(){
					kirim('admin_detail_provisioning', { kode: d.kode }).then(function(r2){
						var pre = el('detailStep'); pre.style.display = 'block';
						pre.textContent = JSON.stringify(r2, null, 2);
					});
				}));
				if (d.statusPermohonan === 'REVIEW_PENDING'){
					w.appendChild(tombol('Setujui', 'hijau', function(){ aksi('admin_approve', d.kode, true); }));
					w.appendChild(tombol('Tolak', 'merah', function(){ aksi('admin_reject', d.kode, true); }));
				}
				if (d.statusPermohonan === 'EMAIL_VERIFICATION_PENDING'){
					w.appendChild(tombol('Verifikasi Manual', 'hijau', function(){ aksi('admin_verify_manual', d.kode, true); }));
					w.appendChild(tombol('Tolak', 'merah', function(){ aksi('admin_reject', d.kode, true); }));
				}
				if (d.statusPermohonan === 'PROVISIONING_FAILED'){
					w.appendChild(tombol('Retry', '', function(){ aksi('admin_retry', d.kode, true); }));
				}
				if (['REJECTED','CANCELLED','EXPIRED'].indexOf(d.statusPermohonan) >= 0 && !d.tenantStatus){
					w.appendChild(tombol('Lepas Username', 'abu', function(){ aksi('admin_release_reservation', d.kode, true); }));
				}
				cAksi.appendChild(w); tr.appendChild(cAksi);
				tb.appendChild(tr);
			})(rows[i]);
		}
	});
}
el('btnMuat').addEventListener('click', muat);
muat();
})();
</script>
</body>
</html>
