<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="ais.common.Common"%>
<%--
  Wizard publik Pendaftaran Tenant Baru -- di-forward HANYA dari PendaftaranTenantServlet
  (akses langsung pendaftaran_tenant.jsp dipantulkan FilterJSP ke "/"). Tiga tampilan dalam
  satu halaman sesuai atribut modeHalaman: wizard (8 langkah), status, verifikasi.
  Seluruh AJAX POST ke Common.ROOT + "/pendaftaran" dgn token CSRF yang dibagikan servlet.
  Data katalog jenis usaha/paket berasal dari database (atribut katalogJson) -- TIDAK di-hard-code.
--%>
<%
	String modeHalaman = (String) request.getAttribute("modeHalaman");
	if (modeHalaman == null) modeHalaman = "wizard";
	String katalogJson = (String) request.getAttribute("katalogJson");
	if (katalogJson == null) katalogJson = "{}";
	String csrfToken = (String) request.getAttribute("csrfToken");
	if (csrfToken == null) csrfToken = "";
	String formInstanceId = (String) request.getAttribute("formInstanceId");
	if (formInstanceId == null) formInstanceId = "";
	String idempotencyKey = (String) request.getAttribute("idempotencyKey");
	if (idempotencyKey == null) idempotencyKey = "";
	String kodeStatus = (String) request.getAttribute("kodeStatus");
	if (kodeStatus == null) kodeStatus = "";
	String hasilVerifikasi = (String) request.getAttribute("hasilVerifikasi");
	if (hasilVerifikasi == null) hasilVerifikasi = "null";
%>
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>Pendaftaran Tenant Baru - eBisnis</title>
<style>
:root{--biru:#0b5ed7;--biru-tua:#083f8c;--abu:#6b7280;--garis:#e5e7eb;--merah:#dc2626;--hijau:#16a34a;--latar:#f4f6fb}
*{box-sizing:border-box}
body{margin:0;font-family:'Segoe UI',system-ui,-apple-system,Arial,sans-serif;background:var(--latar);color:#111827}
.topbar{background:linear-gradient(120deg,var(--biru-tua),var(--biru));color:#fff;padding:14px 20px}
.topbar h1{margin:0;font-size:1.05rem;font-weight:600}
.topbar small{opacity:.85}
.wrap{max-width:880px;margin:0 auto;padding:18px 14px 90px}
.kartu{background:#fff;border:1px solid var(--garis);border-radius:12px;padding:18px;margin-bottom:14px;box-shadow:0 1px 2px rgba(16,24,40,.04)}
.stepper{display:flex;gap:6px;flex-wrap:wrap;margin-bottom:14px}
.stepper .s{flex:1;min-width:72px;text-align:center;font-size:.68rem;color:var(--abu);padding:6px 2px;border-top:3px solid var(--garis)}
.stepper .s.aktif{color:var(--biru);border-top-color:var(--biru);font-weight:600}
.stepper .s.beres{color:var(--hijau);border-top-color:var(--hijau)}
h2.judul{font-size:1.05rem;margin:0 0 4px}
p.sub{margin:0 0 14px;color:var(--abu);font-size:.85rem}
label{display:block;font-size:.8rem;font-weight:600;margin:10px 0 4px}
label .ops{color:var(--abu);font-weight:400}
input[type=text],input[type=email],input[type=password],input[type=tel],select,textarea{width:100%;padding:9px 11px;border:1px solid #cbd5e1;border-radius:8px;font-size:.9rem;background:#fff}
input:focus,select:focus,textarea:focus{outline:2px solid var(--biru);outline-offset:1px;border-color:var(--biru)}
.baris{display:grid;grid-template-columns:1fr 1fr;gap:0 14px}
@media(max-width:640px){.baris{grid-template-columns:1fr}}
.err{color:var(--merah);font-size:.75rem;margin-top:3px;min-height:1em}
.kartu-jenis{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:10px}
.jenis{border:1.5px solid var(--garis);border-radius:10px;padding:12px;cursor:pointer;display:block}
.jenis input{margin-right:8px}
.jenis.dipilih{border-color:var(--biru);background:#eff6ff}
.jenis b{font-size:.87rem}
.jenis .desk{font-size:.74rem;color:var(--abu);margin:4px 0}
.jenis .modul{font-size:.68rem;color:#334155;background:#f1f5f9;border-radius:6px;padding:4px 6px}
.paket{display:grid;grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:10px}
.paket label.p{border:1.5px solid var(--garis);border-radius:10px;padding:12px;cursor:pointer;font-weight:400;margin:0}
.paket label.p.dipilih{border-color:var(--biru);background:#eff6ff}
.paket b{display:block;font-size:.85rem}
.paket .harga{color:var(--biru);font-weight:700;font-size:.95rem;margin-top:4px}
.navbawah{position:fixed;left:0;right:0;bottom:0;background:#fff;border-top:1px solid var(--garis);padding:10px 14px;display:flex;gap:10px;justify-content:space-between;z-index:5}
.navbawah .dalam{max-width:880px;margin:0 auto;display:flex;gap:10px;justify-content:space-between;width:100%}
button{border:0;border-radius:8px;padding:11px 20px;font-size:.9rem;font-weight:600;cursor:pointer}
button:disabled{opacity:.55;cursor:not-allowed}
.btn-utama{background:var(--biru);color:#fff}
.btn-polos{background:#eef2f7;color:#111827}
.btn-bahaya{background:#fee2e2;color:var(--merah)}
.status-user{font-size:.78rem;margin-top:5px;min-height:1.1em}
.status-user.ok{color:var(--hijau)} .status-user.tidak{color:var(--merah)} .status-user.cek{color:var(--abu)}
.ring{border:1px solid var(--garis);border-radius:8px;padding:10px 12px;margin-bottom:8px}
.ring b{font-size:.8rem;color:var(--abu);display:block}
.ring span{font-size:.88rem;word-break:break-word}
.banner{border-radius:10px;padding:12px 14px;margin-bottom:12px;font-size:.87rem}
.banner.sukses{background:#dcfce7;color:#14532d}
.banner.error{background:#fee2e2;color:#7f1d1d}
.badge{display:inline-block;border-radius:20px;padding:3px 12px;font-size:.75rem;font-weight:700;background:#e0e7ff;color:#3730a3}
.langkah-status{display:flex;flex-direction:column;gap:6px;margin:12px 0}
.langkah-status .ls{display:flex;align-items:center;gap:8px;font-size:.85rem;color:var(--abu)}
.langkah-status .ls.b{color:var(--hijau);font-weight:600}
.langkah-status .ls.a{color:var(--biru);font-weight:600}
.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0)}
.hp-field{position:absolute;left:-9999px;top:-9999px}
a{color:var(--biru)}
</style>
</head>
<body>
<div class="topbar">
	<h1>Pendaftaran Tenant Baru</h1>
	<small>eBisnis Platform &middot; coba gratis <span id="topTrialHari">30</span> hari</small>
</div>
<div class="wrap">

<!-- ================= WIZARD ================= -->
<div id="viewWizard" hidden>
	<div class="stepper" id="stepper" aria-hidden="true"></div>
	<div class="banner error" id="bannerError" hidden role="alert"></div>

	<form id="formWizard" autocomplete="on" novalidate>
		<input type="hidden" name="csrf" value="<%=csrfToken%>">
		<input type="hidden" name="formInstanceId" value="<%=formInstanceId%>">
		<input type="hidden" name="idempotencyKey" value="<%=idempotencyKey%>">
		<input type="hidden" name="termsVersion" id="termsVersion" value="">
		<input type="hidden" name="privacyVersion" id="privacyVersion" value="">
		<!-- honeypot: manusia tidak melihat/mengisi field ini -->
		<div class="hp-field" aria-hidden="true"><input type="text" name="website_hp" tabindex="-1" autocomplete="off"></div>

		<!-- Langkah 1: Identitas Usaha -->
		<section class="kartu langkah" data-langkah="1">
			<h2 class="judul">Identitas Usaha/Instansi</h2>
			<p class="sub">Data utama usaha Anda. Kolom bertanda <span class="ops">(opsional)</span> boleh dilewati.</p>
			<label for="namaUsaha">Nama usaha/instansi</label>
			<input type="text" id="namaUsaha" name="namaUsaha" maxlength="255" required aria-describedby="e-namaUsaha">
			<div class="err" id="e-namaUsaha"></div>
			<div class="baris">
				<div><label for="legalName">Nama legal perusahaan/yayasan <span class="ops">(opsional)</span></label>
					<input type="text" id="legalName" name="legalName" maxlength="255"></div>
				<div><label for="tradeName">Nama dagang/brand awal <span class="ops">(opsional)</span></label>
					<input type="text" id="tradeName" name="tradeName" maxlength="255"></div>
			</div>
			<div class="baris">
				<div><label for="bentukUsaha">Bentuk usaha <span class="ops">(opsional)</span></label>
					<select id="bentukUsaha" name="bentukUsaha">
						<option value="">- Pilih -</option><option>Perorangan</option><option>CV</option>
						<option>PT</option><option>Yayasan</option><option>Koperasi</option><option>Lainnya</option>
					</select></div>
				<div><label for="telpUsaha">Nomor telepon usaha <span class="ops">(opsional)</span></label>
					<input type="tel" id="telpUsaha" name="telpUsaha" maxlength="30"></div>
			</div>
			<div class="baris">
				<div><label for="nib">NIB <span class="ops">(opsional)</span></label>
					<input type="text" id="nib" name="nib" maxlength="50"></div>
				<div><label for="npwp">NPWP <span class="ops">(opsional)</span></label>
					<input type="text" id="npwp" name="npwp" maxlength="50"></div>
			</div>
			<div class="baris">
				<div><label for="emailUsaha">Email usaha <span class="ops">(opsional)</span></label>
					<input type="email" id="emailUsaha" name="emailUsaha" maxlength="255"></div>
				<div><label for="website">Website <span class="ops">(opsional)</span></label>
					<input type="text" id="website" name="website" maxlength="255" placeholder="https://"></div>
			</div>
			<label for="deskripsi">Deskripsi singkat <span class="ops">(opsional)</span></label>
			<textarea id="deskripsi" name="deskripsi" rows="2" maxlength="500"></textarea>
		</section>

		<!-- Langkah 2: Lokasi -->
		<section class="kartu langkah" data-langkah="2" hidden>
			<h2 class="judul">Lokasi</h2>
			<p class="sub">Alamat operasional utama.</p>
			<div class="baris">
				<div><label for="negara">Negara</label>
					<input type="text" id="negara" name="negara" value="Indonesia" maxlength="100"></div>
				<div><label for="provinsi">Provinsi</label>
					<input type="text" id="provinsi" name="provinsi" maxlength="100"></div>
			</div>
			<div class="baris">
				<div><label for="kotaKabupaten">Kota/Kabupaten</label>
					<input type="text" id="kotaKabupaten" name="kotaKabupaten" maxlength="100"></div>
				<div><label for="kecamatan">Kecamatan</label>
					<input type="text" id="kecamatan" name="kecamatan" maxlength="100"></div>
			</div>
			<div class="baris">
				<div><label for="kelurahan">Kelurahan/Desa <span class="ops">(opsional)</span></label>
					<input type="text" id="kelurahan" name="kelurahan" maxlength="100"></div>
				<div><label for="kodePos">Kode pos <span class="ops">(opsional)</span></label>
					<input type="text" id="kodePos" name="kodePos" maxlength="20"></div>
			</div>
			<label for="alamat">Alamat lengkap</label>
			<textarea id="alamat" name="alamat" rows="2" maxlength="500"></textarea>
			<label for="timezone">Zona waktu</label>
			<select id="timezone" name="timezone">
				<option value="Asia/Jakarta">WIB - Asia/Jakarta</option>
				<option value="Asia/Makassar">WITA - Asia/Makassar</option>
				<option value="Asia/Jayapura">WIT - Asia/Jayapura</option>
			</select>
		</section>

		<!-- Langkah 3: Pemilik/PIC -->
		<section class="kartu langkah" data-langkah="3" hidden>
			<h2 class="judul">Pemilik / Penanggung Jawab (PIC)</h2>
			<p class="sub">Kontak yang dapat kami hubungi. Dokumen identitas TIDAK diminta pada tahap ini.</p>
			<div class="baris">
				<div><label for="picNama">Nama lengkap pemilik/PIC</label>
					<input type="text" id="picNama" name="picNama" maxlength="255" aria-describedby="e-picNama">
					<div class="err" id="e-picNama"></div></div>
				<div><label for="picJabatan">Jabatan <span class="ops">(opsional)</span></label>
					<input type="text" id="picJabatan" name="picJabatan" maxlength="100"></div>
			</div>
			<div class="baris">
				<div><label for="picEmail">Email PIC <span class="ops">(opsional)</span></label>
					<input type="email" id="picEmail" name="picEmail" maxlength="255"></div>
				<div><label for="picTelp">Telepon/WhatsApp PIC <span class="ops">(opsional)</span></label>
					<input type="tel" id="picTelp" name="picTelp" maxlength="30"></div>
			</div>
			<label>Kanal verifikasi</label>
			<p class="sub" style="margin-bottom:0">Verifikasi dilakukan melalui <b>email login</b> Anda (langkah 7).</p>
		</section>

		<!-- Langkah 4: Jenis Usaha (multi-select, dari database) -->
		<section class="kartu langkah" data-langkah="4" hidden>
			<h2 class="judul">Jenis Usaha/Instansi</h2>
			<p class="sub">Pilih satu atau lebih. Pilihan menentukan modul awal yang disiapkan (bukan izin pengguna).</p>
			<div class="kartu-jenis" id="wadahJenis" role="group" aria-label="Pilihan jenis usaha"></div>
			<div class="err" id="e-jenisUsahaIds"></div>
			<div id="wadahLainnya" hidden>
				<label for="jenisUsahaLainnya">Keterangan jenis usaha lainnya</label>
				<input type="text" id="jenisUsahaLainnya" name="jenisUsahaLainnya" maxlength="500" aria-describedby="e-jenisUsahaLainnya">
				<div class="err" id="e-jenisUsahaLainnya"></div>
			</div>
		</section>

		<!-- Langkah 5: Username Tenant -->
		<section class="kartu langkah" data-langkah="5" hidden>
			<h2 class="judul">Username Tenant &amp; Domain</h2>
			<p class="sub">Username menjadi alamat workspace Anda dan TIDAK dapat diubah setelah tenant aktif.</p>
			<label for="desiredUsername">Username tenant</label>
			<input type="text" id="desiredUsername" name="desiredUsername" maxlength="31"
				autocapitalize="none" autocomplete="off" spellcheck="false"
				aria-describedby="statusUsername e-desiredUsername" placeholder="mis. tokoberkah">
			<div class="status-user cek" id="statusUsername" aria-live="polite"></div>
			<div class="err" id="e-desiredUsername"></div>
			<div class="ring" style="margin-top:8px"><b>Pratinjau alamat</b><span id="previewDomain">-</span></div>
			<label for="customDomain">Custom domain <span class="ops">(opsional, diverifikasi setelah aktivasi)</span></label>
			<input type="text" id="customDomain" name="customDomain" maxlength="255" placeholder="mis. toko-anda.co.id">
		</section>

		<!-- Langkah 6: Paket & Trial (dari database) -->
		<section class="kartu langkah" data-langkah="6" hidden>
			<h2 class="judul">Paket &amp; Masa Coba</h2>
			<p class="sub">Masa coba gratis dimulai saat tenant Anda SIAP (bukan hari ini). Paket dapat diubah kapan saja.</p>
			<div class="paket" id="wadahPaket" role="radiogroup" aria-label="Pilihan paket"></div>
		</section>

		<!-- Langkah 7: Akun & Persetujuan -->
		<section class="kartu langkah" data-langkah="7" hidden>
			<h2 class="judul">Akun &amp; Persetujuan</h2>
			<p class="sub">Email ini menjadi akun masuk (login) Anda.</p>
			<label for="emailLogin">Email login</label>
			<input type="email" id="emailLogin" name="emailLogin" maxlength="255" required aria-describedby="e-emailLogin">
			<div class="err" id="e-emailLogin"></div>
			<div class="baris">
				<div><label for="password">Password <span class="ops">(min <span id="labelPassMin">10</span> karakter)</span></label>
					<input type="password" id="password" name="password" maxlength="200" aria-describedby="e-password">
					<div class="err" id="e-password"></div></div>
				<div><label for="konfirmasiPassword">Konfirmasi password</label>
					<input type="password" id="konfirmasiPassword" name="konfirmasiPassword" maxlength="200"></div>
			</div>
			<label style="font-weight:400"><input type="checkbox" id="setujuTerms" name="setujuTerms" value="1">
				Saya menyetujui <a href="#" onclick="return false" title="Syarat dan Ketentuan">Syarat &amp; Ketentuan</a>
				(versi <span id="labelTermsVersion">-</span>)</label>
			<div class="err" id="e-setujuTerms"></div>
			<label style="font-weight:400"><input type="checkbox" id="setujuPrivacy" name="setujuPrivacy" value="1">
				Saya menyetujui <a href="#" onclick="return false" title="Kebijakan Privasi">Kebijakan Privasi</a>
				(versi <span id="labelPrivacyVersion">-</span>)</label>
			<div class="err" id="e-setujuPrivacy"></div>
			<label style="font-weight:400"><input type="checkbox" id="setujuMarketing" name="setujuMarketing" value="1">
				Saya bersedia menerima informasi produk <span class="ops">(opsional)</span></label>
		</section>

		<!-- Langkah 8: Review -->
		<section class="kartu langkah" data-langkah="8" hidden>
			<h2 class="judul">Tinjau &amp; Kirim</h2>
			<p class="sub">Periksa kembali sebelum mengirim pendaftaran.</p>
			<div id="wadahReview"></div>
		</section>
	</form>

	<div class="navbawah"><div class="dalam">
		<button type="button" class="btn-polos" id="btnKembali">Kembali</button>
		<div style="display:flex;gap:10px">
			<button type="button" class="btn-utama" id="btnLanjut">Lanjut</button>
			<button type="button" class="btn-utama" id="btnKirim" hidden>Kirim Pendaftaran</button>
		</div>
	</div></div>
</div>

<!-- ================= STATUS ================= -->
<div id="viewStatus" hidden>
	<div class="kartu">
		<h2 class="judul">Status Pendaftaran</h2>
		<p class="sub">Masukkan/lihat nomor pendaftaran Anda.</p>
		<label for="inputKode">Nomor pendaftaran</label>
		<div style="display:flex;gap:8px">
			<input type="text" id="inputKode" maxlength="40" placeholder="REG-2026-000001" style="flex:1">
			<button type="button" class="btn-utama" id="btnCekStatus">Cek Status</button>
		</div>
	</div>
	<div class="kartu" id="kartuStatus" hidden>
		<div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
			<h2 class="judul" id="stNama">-</h2><span class="badge" id="stBadge">-</span>
		</div>
		<div class="ring"><b>Nomor pendaftaran</b><span id="stKode">-</span></div>
		<div class="ring"><b>Username tenant</b><span id="stUsername">-</span></div>
		<div class="ring"><b>Jenis usaha dipilih</b><span id="stJenis">-</span></div>
		<div class="ring"><b>Email (verifikasi)</b><span id="stEmail">-</span></div>
		<div class="langkah-status" id="stTahapan"></div>
		<div class="banner error" id="stGagal" hidden></div>
		<div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:8px">
			<button type="button" class="btn-polos" id="btnResend" hidden>Kirim Ulang Email Verifikasi</button>
			<button type="button" class="btn-bahaya" id="btnBatal" hidden>Batalkan Pendaftaran</button>
		</div>
		<p class="sub" style="margin-top:12px">Butuh bantuan? Hubungi dukungan kami melalui email yang tertera di situs utama.</p>
	</div>
</div>

<!-- ================= HASIL VERIFIKASI ================= -->
<div id="viewVerifikasi" hidden>
	<div class="kartu" id="kartuVerifikasi"></div>
</div>

</div><!-- /wrap -->
<script>
(function(){
"use strict";
var CTX = '<%=Common.ROOT%>';
var URL_API = CTX + '/pendaftaran';
var MODE = '<%=modeHalaman%>';
var KATALOG = <%=katalogJson%>;
var HASIL_VERIF = <%=hasilVerifikasi%>;
var KODE_AWAL = '<%=kodeStatus%>';
var CSRF = document.querySelector('input[name=csrf]') ? document.querySelector('input[name=csrf]').value : '';

function kirim(action, data){
	var body = new URLSearchParams(data || {});
	body.append('action', action);
	if (!body.has('csrf')) body.append('csrf', CSRF);
	return fetch(URL_API, { method:'POST', credentials:'include',
		headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
		body: body.toString() }).then(function(r){ return r.json(); });
}
function el(id){ return document.getElementById(id); }
function teks(id, isi){ var e = el(id); if (e) e.textContent = isi == null ? '-' : String(isi); }

/* ===================== WIZARD ===================== */
var LANGKAH_LABEL = ['Usaha','Lokasi','PIC','Jenis Usaha','Username','Paket','Akun','Tinjau'];
var langkahAktif = 1, TOTAL = 8;
var usernameTersedia = false, cekUsernameTimer = null;

function initWizard(){
	el('viewWizard').hidden = false;
	teks('topTrialHari', KATALOG.trialHari || 30);
	teks('labelPassMin', KATALOG.passwordMin || 10);
	teks('labelTermsVersion', KATALOG.termsVersion || '-');
	teks('labelPrivacyVersion', KATALOG.privacyVersion || '-');
	el('termsVersion').value = KATALOG.termsVersion || '';
	el('privacyVersion').value = KATALOG.privacyVersion || '';
	renderStepper(); renderJenis(); renderPaket(); tampilkanLangkah(1);

	el('btnLanjut').addEventListener('click', lanjut);
	el('btnKembali').addEventListener('click', kembali);
	el('btnKirim').addEventListener('click', submitPendaftaran);
	el('desiredUsername').addEventListener('input', onUsernameInput);
}
function renderStepper(){
	var w = el('stepper'); w.innerHTML = '';
	for (var i = 1; i <= TOTAL; i++){
		var d = document.createElement('div');
		d.className = 's' + (i === langkahAktif ? ' aktif' : (i < langkahAktif ? ' beres' : ''));
		d.textContent = i + '. ' + LANGKAH_LABEL[i-1];
		w.appendChild(d);
	}
}
function renderJenis(){
	var w = el('wadahJenis'); w.innerHTML = '';
	var daftar = (KATALOG.jenisUsaha || []);
	for (var i = 0; i < daftar.length; i++){
		(function(j){
			var lab = document.createElement('label'); lab.className = 'jenis';
			var cb = document.createElement('input');
			cb.type = 'checkbox'; cb.value = j.id; cb.setAttribute('data-code', j.code);
			var b = document.createElement('b'); b.textContent = j.nama;
			var desk = document.createElement('div'); desk.className = 'desk'; desk.textContent = j.deskripsi || '';
			var modul = document.createElement('div'); modul.className = 'modul';
			modul.textContent = 'Modul: ' + (j.modules && j.modules.length ? j.modules.join(', ') : '-');
			lab.appendChild(cb); lab.appendChild(b); lab.appendChild(desk); lab.appendChild(modul);
			cb.addEventListener('change', function(){
				lab.className = 'jenis' + (cb.checked ? ' dipilih' : '');
				var adaLainnya = !!document.querySelector('#wadahJenis input[data-code=LAINNYA]:checked');
				el('wadahLainnya').hidden = !adaLainnya;
			});
			w.appendChild(lab);
		})(daftar[i]);
	}
}
function renderPaket(){
	var w = el('wadahPaket'); w.innerHTML = '';
	var daftar = (KATALOG.paket || []);
	var format = new Intl.NumberFormat('id-ID');
	for (var i = 0; i < daftar.length; i++){
		(function(p, idx){
			var lab = document.createElement('label'); lab.className = 'p' + (idx === 0 ? ' dipilih' : '');
			lab.classList.add('p');
			var rb = document.createElement('input');
			rb.type = 'radio'; rb.name = 'planCode'; rb.value = p.code; rb.checked = idx === 0;
			var b = document.createElement('b'); b.textContent = ' ' + p.nama;
			var h = document.createElement('div'); h.className = 'harga';
			h.textContent = 'Rp' + format.format(p.hargaBulanan || 0) + '/bulan';
			var m = document.createElement('div'); m.className = 'desk';
			m.style.cssText = 'font-size:.72rem;color:#6b7280';
			m.textContent = 'Mulai Trial ' + (KATALOG.trialHari || 30) + ' hari';
			lab.appendChild(rb); lab.appendChild(b); lab.appendChild(h); lab.appendChild(m);
			rb.addEventListener('change', function(){
				var semua = w.querySelectorAll('label.p');
				for (var k = 0; k < semua.length; k++) semua[k].classList.remove('dipilih');
				lab.classList.add('dipilih');
			});
			w.appendChild(lab);
		})(daftar[i], i);
	}
}
function tampilkanLangkah(n){
	langkahAktif = n;
	var secs = document.querySelectorAll('.langkah');
	for (var i = 0; i < secs.length; i++) secs[i].hidden = Number(secs[i].getAttribute('data-langkah')) !== n;
	el('btnKembali').disabled = n === 1;
	el('btnLanjut').hidden = n === TOTAL;
	el('btnKirim').hidden = n !== TOTAL;
	el('bannerError').hidden = true;
	renderStepper();
	if (n === TOTAL) renderReview();
	window.scrollTo(0, 0);
}
function setErr(id, pesan){ var e = el('e-' + id); if (e) e.textContent = pesan || ''; }
function nilai(id){ var e = el(id); return e ? e.value.trim() : ''; }
function validasiLangkah(n){
	var oke = true, fokus = null;
	if (n === 1){
		setErr('namaUsaha', '');
		if (!nilai('namaUsaha')){ setErr('namaUsaha', 'Nama usaha/instansi wajib diisi.'); oke = false; fokus = 'namaUsaha'; }
	}
	if (n === 4){
		setErr('jenisUsahaIds', ''); setErr('jenisUsahaLainnya', '');
		var dipilih = document.querySelectorAll('#wadahJenis input:checked');
		if (!dipilih.length){ setErr('jenisUsahaIds', 'Pilih minimal satu jenis usaha.'); oke = false; }
		var lainnya = document.querySelector('#wadahJenis input[data-code=LAINNYA]:checked');
		if (lainnya && !nilai('jenisUsahaLainnya')){
			setErr('jenisUsahaLainnya', 'Jelaskan jenis usaha Anda.'); oke = false; fokus = 'jenisUsahaLainnya';
		}
	}
	if (n === 5){
		setErr('desiredUsername', '');
		var u = nilai('desiredUsername').toLowerCase();
		if (!/^[a-z][a-z0-9_]{2,30}$/.test(u)){
			setErr('desiredUsername', 'Gunakan 3-31 karakter: huruf kecil di awal, lalu huruf/angka/garis bawah.');
			oke = false; fokus = 'desiredUsername';
		} else if (!usernameTersedia){
			setErr('desiredUsername', 'Tunggu pemeriksaan ketersediaan username selesai (harus Tersedia).');
			oke = false; fokus = 'desiredUsername';
		}
	}
	if (n === 7){
		setErr('emailLogin', ''); setErr('password', ''); setErr('setujuTerms', ''); setErr('setujuPrivacy', '');
		var email = nilai('emailLogin').toLowerCase();
		if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)){ setErr('emailLogin', 'Alamat email valid wajib diisi.'); oke = false; fokus = fokus || 'emailLogin'; }
		var min = KATALOG.passwordMin || 10;
		if (nilai('password').length < min){ setErr('password', 'Password minimal ' + min + ' karakter.'); oke = false; fokus = fokus || 'password'; }
		else if (el('password').value !== el('konfirmasiPassword').value){ setErr('password', 'Konfirmasi password tidak sama.'); oke = false; fokus = fokus || 'password'; }
		if (!el('setujuTerms').checked){ setErr('setujuTerms', 'Wajib menyetujui Syarat & Ketentuan.'); oke = false; }
		if (!el('setujuPrivacy').checked){ setErr('setujuPrivacy', 'Wajib menyetujui Kebijakan Privasi.'); oke = false; }
	}
	if (fokus && el(fokus)) el(fokus).focus();
	return oke;
}
function lanjut(){ if (validasiLangkah(langkahAktif)) tampilkanLangkah(langkahAktif + 1); }
function kembali(){ if (langkahAktif > 1) tampilkanLangkah(langkahAktif - 1); }

function onUsernameInput(){
	usernameTersedia = false;
	var s = el('statusUsername');
	var u = nilai('desiredUsername').toLowerCase();
	el('previewDomain').textContent = u ? (u + '.' + (KATALOG.subdomainBase || 'ebisnis.id')) : '-';
	if (cekUsernameTimer) clearTimeout(cekUsernameTimer);
	if (!/^[a-z][a-z0-9_]{2,30}$/.test(u)){ s.className = 'status-user cek'; s.textContent = ''; return; }
	s.className = 'status-user cek'; s.textContent = 'Sedang diperiksa...';
	cekUsernameTimer = setTimeout(function(){
		kirim('check_username', { username: u }).then(function(r){
			if (nilai('desiredUsername').toLowerCase() !== u) return; // sudah berubah
			if (r.tersedia){ usernameTersedia = true; s.className = 'status-user ok'; s.textContent = 'Tersedia'; }
			else { s.className = 'status-user tidak'; s.textContent = r.description || 'Tidak tersedia'; }
		}).catch(function(){ s.className = 'status-user cek'; s.textContent = 'Gagal memeriksa. Coba lagi.'; });
	}, 400);
}
function tambahReview(w, label, isi){
	var d = document.createElement('div'); d.className = 'ring';
	var b = document.createElement('b'); b.textContent = label;
	var s = document.createElement('span'); s.textContent = isi || '-';
	d.appendChild(b); d.appendChild(s); w.appendChild(d);
}
function renderReview(){
	var w = el('wadahReview'); w.innerHTML = '';
	tambahReview(w, 'Nama usaha', nilai('namaUsaha'));
	tambahReview(w, 'Lokasi', [nilai('kotaKabupaten'), nilai('provinsi'), nilai('negara')].filter(Boolean).join(', '));
	tambahReview(w, 'PIC', nilai('picNama'));
	var jenis = [];
	var cb = document.querySelectorAll('#wadahJenis input:checked');
	for (var i = 0; i < cb.length; i++) jenis.push(cb[i].parentNode.querySelector('b').textContent);
	tambahReview(w, 'Jenis usaha', jenis.join(', '));
	tambahReview(w, 'Username tenant', nilai('desiredUsername').toLowerCase() + '  (' + el('previewDomain').textContent + ')');
	var paket = document.querySelector('#wadahPaket input:checked');
	tambahReview(w, 'Paket', paket ? paket.parentNode.querySelector('b').textContent : '-');
	tambahReview(w, 'Email login', nilai('emailLogin').toLowerCase());
	tambahReview(w, 'Masa coba', (KATALOG.trialHari || 30) + ' hari, dimulai saat tenant SIAP');
}
function submitPendaftaran(){
	// Validasi ulang seluruh langkah kunci sebelum kirim.
	var urutan = [1, 4, 5, 7];
	for (var i = 0; i < urutan.length; i++){
		if (!validasiLangkah(urutan[i])){ tampilkanLangkah(urutan[i]); return; }
	}
	var tombol = el('btnKirim'); tombol.disabled = true; tombol.textContent = 'Mengirim...';
	var data = {};
	var form = el('formWizard');
	var inputs = form.querySelectorAll('input,select,textarea');
	for (var i = 0; i < inputs.length; i++){
		var f = inputs[i];
		if (!f.name) continue;
		if (f.type === 'checkbox'){ if (f.checked) data[f.name] = f.value; }
		else if (f.type === 'radio'){ if (f.checked) data[f.name] = f.value; }
		else data[f.name] = f.value;
	}
	var ids = [];
	var cb = document.querySelectorAll('#wadahJenis input:checked');
	for (var i = 0; i < cb.length; i++) ids.push(cb[i].value);
	data.jenisUsahaIds = ids.join(',');
	data.desiredUsername = (data.desiredUsername || '').toLowerCase();
	data.emailLogin = (data.emailLogin || '').toLowerCase();

	kirim('submit_registration', data).then(function(r){
		if (r.status === '00'){
			window.location.href = r.redirect || (URL_API + '?mode=status&kode=' + encodeURIComponent(r.registrationCode));
			return;
		}
		tombol.disabled = false; tombol.textContent = 'Kirim Pendaftaran';
		var banner = el('bannerError');
		banner.hidden = false; banner.textContent = r.description || 'Pendaftaran gagal. Periksa isian Anda.';
		if (r.fieldErrors){
			var pertama = null;
			for (var k in r.fieldErrors){
				setErr(k, r.fieldErrors[k]);
				if (!pertama) pertama = k;
			}
			var kembaliKe = { namaUsaha:1, jenisUsahaIds:4, jenisUsahaLainnya:4, desiredUsername:5,
				emailLogin:7, password:7, setujuTerms:7, setujuPrivacy:7 };
			if (pertama && kembaliKe[pertama]){ tampilkanLangkah(kembaliKe[pertama]); banner.hidden = false; }
		}
	}).catch(function(){
		tombol.disabled = false; tombol.textContent = 'Kirim Pendaftaran';
		var banner = el('bannerError');
		banner.hidden = false; banner.textContent = 'Koneksi terputus. Coba lagi (pendaftaran tidak akan dobel).';
	});
}

/* ===================== STATUS ===================== */
var URUTAN_TAHAP = [
	['EMAIL_VERIFICATION_PENDING', 'Verifikasi email'],
	['VERIFIED', 'Email terverifikasi'],
	['PROVISIONING', 'Penyiapan tenant'],
	['READY', 'Tenant siap'],
	['ACTIVE', 'Aktif']
];
function initStatus(kode){
	el('viewStatus').hidden = false;
	if (kode){ el('inputKode').value = kode; muatStatus(kode); }
	el('btnCekStatus').addEventListener('click', function(){ muatStatus(el('inputKode').value.trim()); });
	el('btnResend').addEventListener('click', function(){
		var k = el('inputKode').value.trim();
		el('btnResend').disabled = true;
		kirim('resend_verification', { kode: k }).then(function(r){
			alert(r.description || '-'); el('btnResend').disabled = false;
		});
	});
	el('btnBatal').addEventListener('click', function(){
		var k = el('inputKode').value.trim();
		if (!window.confirm('Batalkan pendaftaran ' + k + '?')) return;
		kirim('cancel_draft', { kode: k }).then(function(r){
			alert(r.description || '-'); muatStatus(k);
		});
	});
}
function muatStatus(kode){
	if (!kode) return;
	kirim('get_status', { kode: kode }).then(function(r){
		var kartu = el('kartuStatus');
		if (r.status !== '00'){ kartu.hidden = false; teks('stNama', r.description || 'Tidak ditemukan');
			teks('stBadge', '-'); el('stTahapan').innerHTML = ''; el('btnResend').hidden = true; el('btnBatal').hidden = true; return; }
		kartu.hidden = false;
		teks('stNama', r.namaUsaha); teks('stBadge', r.statusPermohonan);
		teks('stKode', r.registrationCode);
		teks('stUsername', r.username + '  (' + (r.previewDomain || '-') + ')');
		teks('stJenis', (r.jenisUsaha || []).join(', '));
		teks('stEmail', r.emailMasked);
		var w = el('stTahapan'); w.innerHTML = '';
		var posisi = r.statusPermohonan;
		var lewat = true;
		for (var i = 0; i < URUTAN_TAHAP.length; i++){
			var d = document.createElement('div');
			var kelas = 'ls';
			if (URUTAN_TAHAP[i][0] === posisi){ kelas += ' a'; lewat = false; }
			else if (lewat && posisi !== 'CANCELLED' && posisi !== 'REJECTED') kelas += ' b';
			d.className = kelas;
			d.textContent = (kelas.indexOf(' b') > 0 ? '✓ ' : (kelas.indexOf(' a') > 0 ? '▶ ' : '○ ')) + URUTAN_TAHAP[i][1];
			w.appendChild(d);
		}
		var gagal = el('stGagal');
		if (r.failureMessage){ gagal.hidden = false; gagal.textContent = r.failureMessage; } else gagal.hidden = true;
		el('btnResend').hidden = r.statusPermohonan !== 'EMAIL_VERIFICATION_PENDING';
		el('btnBatal').hidden = ['DRAFT','SUBMITTED','EMAIL_VERIFICATION_PENDING'].indexOf(r.statusPermohonan) < 0;
	});
}

/* ===================== VERIFIKASI ===================== */
function initVerifikasi(){
	el('viewVerifikasi').hidden = false;
	var kartu = el('kartuVerifikasi');
	var r = HASIL_VERIF || {};
	var sukses = r.status === '00';
	var h = document.createElement('h2'); h.className = 'judul';
	h.textContent = sukses ? 'Email Berhasil Diverifikasi' : 'Verifikasi Gagal';
	var ban = document.createElement('div'); ban.className = 'banner ' + (sukses ? 'sukses' : 'error');
	ban.textContent = r.description || '-';
	kartu.appendChild(h); kartu.appendChild(ban);
	if (r.registrationCode){
		var a = document.createElement('a');
		a.href = URL_API + '?mode=status&kode=' + encodeURIComponent(r.registrationCode);
		a.textContent = 'Lihat status pendaftaran ' + r.registrationCode;
		kartu.appendChild(a);
	} else {
		var a2 = document.createElement('a');
		a2.href = URL_API + '?mode=status';
		a2.textContent = 'Buka halaman status';
		kartu.appendChild(a2);
	}
}

if (MODE === 'status') initStatus(KODE_AWAL);
else if (MODE === 'verifikasi') initVerifikasi();
else initWizard();
})();
</script>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
</html>
