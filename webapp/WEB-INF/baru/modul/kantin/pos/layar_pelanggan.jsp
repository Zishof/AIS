<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%--
  Layar Pelanggan — layar KEDUA untuk mesin POS dual-monitor. Dibuka lewat window.open() dari POS
  (versi JSP: _pos.jsp; versi ZKoss: PosKantinAction) sebagai jendela browser TERPISAH yang bisa
  digeser/dipindah operator ke monitor kedua. Halaman ini SAMA untuk kedua versi POS — komunikasi
  murni client-side lewat BroadcastChannel (tanpa polling server), diberi nama via parameter "ch"
  yang HARUS sama persis dengan channel yang dibuat sisi kasir.

  Pesan yang diterima (dari kasir -> layar ini), field "tipe":
    keranjang  : {items:[{nama,harga,jumlah,diskon}], subtotal, totalDiskon, totalCashback,
                  grandTotal, namaToko, namaMember}  -> tampil belanja real-time
    qris       : {qrString, totalBayar}               -> render ULANG QR yang sama di layar ini
    batal_qris : {}                                    -> kembali ke layar belanja
    sukses     : {totalBayar, uangTunai, kembalian, namaToko} -> ucapan terima kasih + survey
    reset      : {}                                    -> kembali ke layar sambutan

  Pesan yang DIKIRIM balik (layar ini -> kasir): {tipe:'survey', rating, catatan} — kasir cukup
  menampilkannya sbg info (opsional); penyimpanan rating dilakukan LANGSUNG oleh layar ini via
  survey_kepuasan_service.jsp (jendela ini berbagi cookie sesi dgn kasir, origin sama).
--%>
<%
Tbmuser uLP = Common.getCurrentUser(request);
if (uLP == null || uLP.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    out.print("<div style='text-align:center;padding:60px;font-family:sans-serif;font-size:20px;'>" + Common.getBahasaConfig("Akses ditolak. Sesi Anda telah berakhir.") + "</div>");
    return;
}
String rndLp = Common.getGeneratedBarCode(7);
String channelLp = request.getParameter("ch");
if (channelLp == null) channelLp = "";

String namaTokoAwal = "";
String tokoIdParam = request.getParameter("toko");
if (tokoIdParam != null && tokoIdParam.trim().length() > 0) {
    try {
        Toko t = (Toko) ais.common.ConstantValues.ambil(Toko.class.getName(), Long.valueOf(tokoIdParam.trim()));
        if (t != null && t.getNama() != null) namaTokoAwal = t.getNama();
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/pos/layar_pelanggan.jsp:41"); /* abaikan, tampil default */ }
}

PerguruanTinggi ptLp = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
String namaPtLp = ptLp != null && ptLp.getNama() != null ? ptLp.getNama() : "";
String logoPtLp = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
%>
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><%=Common.getBahasaConfig("Layar Pelanggan")%></title>
<script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
<style>
    * { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; height: 100%; width: 100%; overflow: hidden; background: #0b1220; font-family: "Segoe UI", Roboto, Arial, sans-serif; color: #fff; }
    .lp-panel { position: fixed; inset: 0; display: none; flex-direction: column; align-items: center; justify-content: center; padding: 4vh 5vw; text-align: center; }
    .lp-panel.lp-show { display: flex; }
    .lp-fsbtn { position: fixed; top: 16px; right: 16px; z-index: 999; background: rgba(255,255,255,.12); color: #fff; border: 1px solid rgba(255,255,255,.3); border-radius: 999px; padding: 8px 16px; font-size: 14px; cursor: pointer; }
    .lp-fsbtn:hover { background: rgba(255,255,255,.22); }

    /* IDLE / sambutan */
    #lpIdle<%=rndLp%> { background: radial-gradient(circle at 50% 30%, #16233f 0%, #0b1220 70%); }
    .lp-logo { max-height: 14vh; margin-bottom: 3vh; filter: drop-shadow(0 6px 14px rgba(0,0,0,.4)); }
    .lp-toko-nama { font-size: clamp(28px, 4vw, 56px); font-weight: 800; margin-bottom: 1vh; }
    .lp-welcome { font-size: clamp(20px, 2.4vw, 32px); opacity: .85; margin-bottom: 3vh; }
    .lp-jam { font-size: clamp(16px, 1.6vw, 22px); opacity: .55; letter-spacing: .05em; }

    /* KERANJANG */
    #lpKeranjang<%=rndLp%> { background: #0f1830; align-items: stretch; justify-content: flex-start; padding: 3vh 6vw; }
    .lp-kr-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 2vh; border-bottom: 2px solid rgba(255,255,255,.15); padding-bottom: 1.5vh; }
    .lp-kr-head h2 { margin: 0; font-size: clamp(22px, 2.6vw, 34px); }
    .lp-kr-head span { font-size: clamp(14px, 1.4vw, 18px); opacity: .6; }
    #lpKrItems<%=rndLp%> { flex: 1 1 auto; overflow-y: auto; text-align: left; }
    .lp-kr-row { display: flex; justify-content: space-between; align-items: center; padding: 1.4vh 0; border-bottom: 1px dashed rgba(255,255,255,.12); font-size: clamp(16px, 1.7vw, 22px); }
    .lp-kr-row .nm { font-weight: 600; }
    .lp-kr-row .qh { opacity: .6; font-size: .8em; }
    .lp-kr-row .tt { font-weight: 700; color: #7dd3fc; }
    .lp-kr-foot { border-top: 2px solid rgba(255,255,255,.15); margin-top: 1.5vh; padding-top: 2vh; }
    .lp-kr-line { display: flex; justify-content: space-between; font-size: clamp(15px, 1.5vw, 20px); opacity: .75; margin-bottom: .6vh; }
    .lp-kr-total { display: flex; justify-content: space-between; align-items: center; margin-top: 1.2vh; }
    .lp-kr-total .lbl { font-size: clamp(20px, 2.2vw, 30px); font-weight: 700; }
    .lp-kr-total .val { font-size: clamp(34px, 5vw, 64px); font-weight: 900; color: #4ade80; }
    .lp-kr-empty { flex: 1; display: flex; align-items: center; justify-content: center; opacity: .4; font-size: clamp(18px, 2vw, 26px); }

    /* QRIS */
    #lpQris<%=rndLp%> { background: #0f1830; }
    #lpQrisWrap<%=rndLp%> { background: #fff; padding: 20px; border-radius: 20px; display: inline-block; box-shadow: 0 10px 30px rgba(0,0,0,.4); }
    .lp-qris-total { font-size: clamp(30px, 4vw, 52px); font-weight: 900; color: #4ade80; margin-top: 3vh; }
    .lp-qris-hint { font-size: clamp(16px, 1.8vw, 24px); opacity: .8; margin-top: 1.5vh; max-width: 60vw; }

    /* SUKSES */
    #lpSukses<%=rndLp%> { background: radial-gradient(circle at 50% 30%, #0f3a24 0%, #0b1220 70%); }
    .lp-check { font-size: clamp(60px, 9vw, 130px); line-height: 1; margin-bottom: 1vh; }
    .lp-sukses-judul { font-size: clamp(28px, 4vw, 52px); font-weight: 800; margin-bottom: 2vh; }
    .lp-sukses-rincian { font-size: clamp(16px, 1.8vw, 24px); opacity: .85; line-height: 1.8; }
    .lp-sukses-rincian b { color: #4ade80; }

    /* SURVEY */
    #lpSurvey<%=rndLp%> { background: #0f1830; }
    .lp-survey-judul { font-size: clamp(24px, 3vw, 40px); font-weight: 800; margin-bottom: 4vh; }
    .lp-stars { display: flex; gap: 3vw; margin-bottom: 3vh; }
    .lp-star { font-size: clamp(48px, 7vw, 90px); cursor: pointer; opacity: .3; transition: transform .15s, opacity .15s; user-select: none; }
    .lp-star.lp-active { opacity: 1; transform: scale(1.15); }
    .lp-survey-skip { margin-top: 2vh; opacity: .5; font-size: clamp(14px, 1.4vw, 18px); cursor: pointer; text-decoration: underline; }
    .lp-survey-done { font-size: clamp(22px, 2.6vw, 36px); font-weight: 700; color: #4ade80; }

    /* PIN */
    #lpPin<%=rndLp%> { background: radial-gradient(circle at 50% 25%, #1a1030 0%, #0b1220 70%); }
    .lp-pin-judul { font-size: clamp(20px, 2.4vw, 30px); font-weight: 700; margin-bottom: .6vh; }
    .lp-pin-nama { font-size: clamp(16px, 1.6vw, 22px); opacity: .7; margin-bottom: 3vh; }
    .lp-pin-dots { font-size: clamp(28px, 3.2vw, 44px); letter-spacing: .3em; min-height: 1.4em; margin-bottom: 2vh; font-weight: 700; color: #a78bfa; }
    .lp-pin-error { color: #f87171; font-size: clamp(14px, 1.5vw, 19px); min-height: 1.6em; margin-bottom: 1vh; }
    .lp-pin-pad { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.4vh 1.4vw; width: min(70vw, 340px); }
    .lp-pin-key { background: rgba(255,255,255,.08); border: 1px solid rgba(255,255,255,.15); border-radius: 14px; color: #fff; font-size: clamp(22px, 2.6vw, 30px); font-weight: 700; padding: 2vh 0; cursor: pointer; user-select: none; }
    .lp-pin-key:active { background: rgba(255,255,255,.22); }
    .lp-pin-key.lp-pin-ok { background: #16a34a; border-color: #16a34a; }
    .lp-pin-key.lp-pin-del { background: rgba(248,113,113,.18); border-color: rgba(248,113,113,.4); }
    .lp-pin-cancel { margin-top: 2.5vh; opacity: .5; font-size: clamp(14px, 1.4vw, 18px); cursor: pointer; text-decoration: underline; }
    .lp-pin-ok-msg { font-size: clamp(24px, 2.8vw, 38px); font-weight: 800; color: #4ade80; }
</style>
</head>
<body>

<button class="lp-fsbtn" id="lpFsBtn<%=rndLp%>" onclick="lpToggleFullscreen<%=rndLp%>()"><%=Common.getBahasaConfig("Layar Penuh")%></button>

<!-- IDLE -->
<div class="lp-panel lp-show" id="lpIdle<%=rndLp%>">
    <% if (logoPtLp != null && logoPtLp.trim().length() > 0) { %>
    <img class="lp-logo" src="<%=logoPtLp%>" onerror="this.style.display='none';" alt="">
    <% } %>
    <div class="lp-toko-nama" id="lpIdleToko<%=rndLp%>"><%=namaTokoAwal.length() > 0 ? namaTokoAwal : namaPtLp%></div>
    <div class="lp-welcome"><%=Common.getBahasaConfig("Selamat Datang!")%></div>
    <div class="lp-jam" id="lpJam<%=rndLp%>"></div>
</div>

<!-- KERANJANG -->
<div class="lp-panel" id="lpKeranjang<%=rndLp%>">
    <div class="lp-kr-head">
        <h2 id="lpKrToko<%=rndLp%>"><%=Common.getBahasaConfig("Belanja Anda")%></h2>
        <span id="lpKrMember<%=rndLp%>"></span>
    </div>
    <div id="lpKrItems<%=rndLp%>"></div>
    <div class="lp-kr-foot">
        <div class="lp-kr-line"><span><%=Common.getBahasaConfig("Subtotal")%></span><span id="lpKrSub<%=rndLp%>">Rp 0</span></div>
        <div class="lp-kr-line"><span><%=Common.getBahasaConfig("Diskon")%></span><span id="lpKrDisk<%=rndLp%>">- Rp 0</span></div>
        <div class="lp-kr-line" id="lpKrPajakRow<%=rndLp%>" style="display:none;"><span><%=Common.getBahasaConfig("Pajak")%></span><span id="lpKrPajak<%=rndLp%>">Rp 0</span></div>
        <div class="lp-kr-total"><span class="lbl"><%=Common.getBahasaConfig("Total Bayar")%></span><span class="val" id="lpKrTotal<%=rndLp%>">Rp 0</span></div>
    </div>
</div>

<!-- QRIS -->
<div class="lp-panel" id="lpQris<%=rndLp%>">
    <div style="font-size:clamp(18px,2vw,26px);font-weight:700;margin-bottom:2vh;"><%=Common.getBahasaConfig("Pindai untuk Membayar")%></div>
    <div id="lpQrisWrap<%=rndLp%>"></div>
    <div class="lp-qris-total" id="lpQrisTotal<%=rndLp%>">Rp 0</div>
    <div class="lp-qris-hint"><%=Common.getBahasaConfig("Buka aplikasi e-wallet/mobile banking Anda, lalu pindai kode QR di atas.")%></div>
</div>

<!-- SUKSES -->
<div class="lp-panel" id="lpSukses<%=rndLp%>">
    <div class="lp-check">&#10004;</div>
    <div class="lp-sukses-judul"><%=Common.getBahasaConfig("Terima Kasih!")%></div>
    <div class="lp-sukses-rincian" id="lpSuksesRincian<%=rndLp%>"></div>
</div>

<!-- SURVEY -->
<div class="lp-panel" id="lpSurvey<%=rndLp%>">
    <div class="lp-survey-judul" id="lpSurveyJudul<%=rndLp%>"><%=Common.getBahasaConfig("Bagaimana pelayanan kami hari ini?")%></div>
    <div class="lp-stars" id="lpStars<%=rndLp%>"></div>
    <div class="lp-survey-skip" id="lpSurveySkip<%=rndLp%>" onclick="lpKembaliIdle<%=rndLp%>()"><%=Common.getBahasaConfig("Lewati")%></div>
</div>

<!-- PIN -->
<div class="lp-panel" id="lpPin<%=rndLp%>">
    <div id="lpPinBody<%=rndLp%>">
        <div class="lp-pin-judul"><%=Common.getBahasaConfig("Masukkan PIN Anda")%></div>
        <div class="lp-pin-nama" id="lpPinNama<%=rndLp%>"></div>
        <div class="lp-pin-dots" id="lpPinDots<%=rndLp%>">&ndash;</div>
        <div class="lp-pin-error" id="lpPinError<%=rndLp%>"></div>
        <div class="lp-pin-pad" id="lpPinPad<%=rndLp%>"></div>
        <div class="lp-pin-cancel" id="lpPinCancel<%=rndLp%>"><%=Common.getBahasaConfig("Batalkan Transaksi")%></div>
    </div>
</div>

<script>
(function(){
    var CH = "<%=channelLp%>";
    var TOKO_ID = "<%=tokoIdParam == null ? "" : tokoIdParam.trim()%>";
    var SVC_SURVEY = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kantin%2Fpos&s=survey_kepuasan_service";
    var SVC_PIN = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=kantin%2Fpos&s=verifikasi_pin_service";
    var resetTimer = null;

    var formatRp = function(n){ return new Intl.NumberFormat('id-ID', { style:'currency', currency:'IDR', minimumFractionDigits:0 }).format(n||0); };
    var esc = function(s){ return (s==null?"":String(s)).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); };
    var $ = function(id){ return document.getElementById(id); };

    // ---------- Channel ----------
    var channel = null;
    try { if (CH && 'BroadcastChannel' in window) channel = new BroadcastChannel(CH); } catch(e) { channel = null; }

    function tampilPanel(id){
        ['lpIdle<%=rndLp%>','lpKeranjang<%=rndLp%>','lpQris<%=rndLp%>','lpSukses<%=rndLp%>','lpSurvey<%=rndLp%>','lpPin<%=rndLp%>'].forEach(function(pid){
            var el = $(pid); if (el) el.classList.toggle('lp-show', pid === id);
        });
    }

    function clearResetTimer(){ if (resetTimer) { clearTimeout(resetTimer); resetTimer = null; } }

    // ---------- KERANJANG ----------
    function tampilKeranjang(d){
        clearResetTimer();
        $('lpKrToko<%=rndLp%>').textContent = d.namaToko || "<%=Common.getBahasaConfig("Belanja Anda")%>";
        $('lpKrMember<%=rndLp%>').textContent = d.namaMember ? ("<%=Common.getBahasaConfig("Member")%>: " + d.namaMember) : "";
        var items = d.items || [];
        if (!items.length) {
            $('lpKrItems<%=rndLp%>').innerHTML = '<div class="lp-kr-empty"><%=Common.getBahasaConfig("Keranjang masih kosong")%></div>';
        } else {
            var html = '';
            items.forEach(function(it){
                var tot = (Number(it.harga)||0) * (Number(it.jumlah)||0) - (Number(it.diskon)||0);
                html += '<div class="lp-kr-row"><div class="nm">' + esc(it.nama) + '<br><span class="qh">' + (it.jumlah||0) + ' x ' + formatRp(it.harga) + '</span></div><div class="tt">' + formatRp(tot) + '</div></div>';
            });
            $('lpKrItems<%=rndLp%>').innerHTML = html;
        }
        $('lpKrSub<%=rndLp%>').textContent = formatRp(d.subtotal);
        $('lpKrDisk<%=rndLp%>').textContent = "- " + formatRp(d.totalDiskon);
        var rowPajak = $('lpKrPajakRow<%=rndLp%>');
        if (d.totalPajak > 0) {
            rowPajak.style.display = '';
            $('lpKrPajak<%=rndLp%>').textContent = formatRp(d.totalPajak);
        } else if (rowPajak) {
            rowPajak.style.display = 'none';
        }
        $('lpKrTotal<%=rndLp%>').textContent = formatRp(d.grandTotal);
        tampilPanel('lpKeranjang<%=rndLp%>');
    }

    // ---------- QRIS ----------
    function tampilQris(d){
        clearResetTimer();
        var wrap = $('lpQrisWrap<%=rndLp%>');
        wrap.innerHTML = '';
        try {
            if (window.QRCode) {
                new QRCode(wrap, { text: d.qrString || '', width: 320, height: 320, colorDark: '#000000', colorLight: '#ffffff', correctLevel: QRCode.CorrectLevel.H });
            }
        } catch(e) {}
        $('lpQrisTotal<%=rndLp%>').textContent = formatRp(d.totalBayar);
        tampilPanel('lpQris<%=rndLp%>');
    }

    // ---------- PIN (verifikasi pembeli, hanya bila jenis anggota "Wajib PIN") ----------
    var pinBuffer = '';
    var pinMemberId = null;

    function pinDotsRender(){
        $('lpPinDots<%=rndLp%>').textContent = pinBuffer.length ? pinBuffer.split('').map(function(){ return '●'; }).join(' ') : '–';
    }

    function pinBuatPad(){
        var pad = $('lpPinPad<%=rndLp%>');
        pad.innerHTML = '';
        var keys = ['1','2','3','4','5','6','7','8','9','del','0','ok'];
        keys.forEach(function(k){
            var b = document.createElement('div');
            if (k === 'del') { b.className = 'lp-pin-key lp-pin-del'; b.textContent = '⌫'; b.onclick = pinHapus; }
            else if (k === 'ok') { b.className = 'lp-pin-key lp-pin-ok'; b.textContent = '✓'; b.onclick = pinSubmit; }
            else { b.className = 'lp-pin-key'; b.textContent = k; b.onclick = (function(v){ return function(){ pinTekan(v); }; })(k); }
            pad.appendChild(b);
        });
    }

    function pinTekan(d){ if (pinBuffer.length < 8) { pinBuffer += d; pinDotsRender(); } }
    function pinHapus(){ pinBuffer = pinBuffer.slice(0, -1); pinDotsRender(); }

    function pinBatal(){
        if (channel) { try { channel.postMessage({ tipe: 'pin_hasil', ok: false, batal: true }); } catch(e) {} }
        pinBuffer = ''; pinMemberId = null;
        window['lpKembaliIdle<%=rndLp%>']();
    }

    function pinSubmit(){
        if (!pinBuffer || !pinMemberId) return;
        var body = 'memberId=' + encodeURIComponent(pinMemberId) + '&pin=' + encodeURIComponent(pinBuffer);
        fetch(SVC_PIN, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body })
            .then(function(r){ return r.json(); })
            .then(function(j){
                if (j.status === '00' && j.ok) {
                    $('lpPinBody<%=rndLp%>').innerHTML = '<div class="lp-pin-ok-msg">&#10004; <%=Common.getBahasaConfig("PIN Benar! Memproses transaksi...")%></div>';
                    if (channel) { try { channel.postMessage({ tipe: 'pin_hasil', ok: true }); } catch(e) {} }
                    pinBuffer = ''; pinMemberId = null;
                    setTimeout(function(){ tampilPanel('lpKeranjang<%=rndLp%>'); }, 1400);
                } else {
                    $('lpPinError<%=rndLp%>').textContent = "<%=Common.getBahasaConfig("PIN salah, silakan coba lagi.")%>";
                    pinBuffer = ''; pinDotsRender();
                }
            })
            .catch(function(){ $('lpPinError<%=rndLp%>').textContent = "<%=Common.getBahasaConfig("Gagal menghubungi server, coba lagi.")%>"; });
    }

    function tampilPin(d){
        clearResetTimer();
        pinBuffer = '';
        pinMemberId = d.memberId;
        $('lpPinBody<%=rndLp%>').innerHTML = '<div class="lp-pin-judul"><%=Common.getBahasaConfig("Masukkan PIN Anda")%></div>'
            + '<div class="lp-pin-nama">' + esc(d.memberNama || '') + '</div>'
            + '<div class="lp-pin-dots" id="lpPinDots<%=rndLp%>">&ndash;</div>'
            + '<div class="lp-pin-error" id="lpPinError<%=rndLp%>"></div>'
            + '<div class="lp-pin-pad" id="lpPinPad<%=rndLp%>"></div>'
            + '<div class="lp-pin-cancel" id="lpPinCancel<%=rndLp%>"><%=Common.getBahasaConfig("Batalkan Transaksi")%></div>';
        pinBuatPad();
        pinDotsRender();
        $('lpPinCancel<%=rndLp%>').onclick = pinBatal;
        tampilPanel('lpPin<%=rndLp%>');
    }

    // ---------- SUKSES + SURVEY ----------
    function tampilSukses(d){
        clearResetTimer();
        var rincian = '<%=Common.getBahasaConfigJS("Total Dibayar")%>: <b>' + formatRp(d.totalBayar) + '</b>';
        if (d.uangTunai && Number(d.uangTunai) > 0) {
            rincian += '<br><%=Common.getBahasaConfig("Uang Diterima")%>: ' + formatRp(d.uangTunai);
            rincian += '<br><%=Common.getBahasaConfig("Kembalian")%>: ' + formatRp(d.kembalian);
        }
        $('lpSuksesRincian<%=rndLp%>').innerHTML = rincian;
        if (d.namaToko) $('lpIdleToko<%=rndLp%>').textContent = d.namaToko;
        tampilPanel('lpSukses<%=rndLp%>');
        resetTimer = setTimeout(function(){ tampilSurvey(); }, 3500);
    }

    function buatBintang(){
        var wrap = $('lpStars<%=rndLp%>');
        wrap.innerHTML = '';
        for (var i = 1; i <= 5; i++) {
            var s = document.createElement('span');
            s.className = 'lp-star'; s.textContent = '★'; s.setAttribute('data-v', i);
            s.onclick = (function(v){ return function(){ pilihRating(v); }; })(i);
            wrap.appendChild(s);
        }
    }

    function pilihRating(v){
        document.querySelectorAll('#lpStars<%=rndLp%> .lp-star').forEach(function(el){
            el.classList.toggle('lp-active', Number(el.getAttribute('data-v')) <= v);
        });
        setTimeout(function(){ kirimSurvey(v); }, 350);
    }

    function kirimSurvey(rating){
        $('lpSurveyJudul<%=rndLp%>').innerHTML = '<div class="lp-survey-done"><%=Common.getBahasaConfig("Terima kasih atas penilaian Anda!")%></div>';
        $('lpSurveySkip<%=rndLp%>').style.display = 'none';
        if (channel) { try { channel.postMessage({ tipe: 'survey', rating: rating }); } catch(e) {} }
        var body = 'aksi=simpan&rating=' + encodeURIComponent(rating) + (TOKO_ID ? ('&toko=' + encodeURIComponent(TOKO_ID)) : '');
        fetch(SVC_SURVEY, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body }).catch(function(){});
        resetTimer = setTimeout(function(){ lpKembaliIdle(); }, 2500);
    }

    function tampilSurvey(){
        clearResetTimer();
        $('lpSurveyJudul<%=rndLp%>').textContent = "<%=Common.getBahasaConfig("Bagaimana pelayanan kami hari ini?")%>";
        $('lpSurveySkip<%=rndLp%>').style.display = '';
        buatBintang();
        tampilPanel('lpSurvey<%=rndLp%>');
        resetTimer = setTimeout(function(){ lpKembaliIdle(); }, 15000);
    }

    window['lpKembaliIdle<%=rndLp%>'] = function(){
        clearResetTimer();
        tampilPanel('lpIdle<%=rndLp%>');
    };

    // ---------- Terima pesan dari kasir ----------
    if (channel) {
        channel.onmessage = function(ev){
            var d = ev.data || {};
            if (d.tipe === 'keranjang') tampilKeranjang(d);
            else if (d.tipe === 'qris') tampilQris(d);
            else if (d.tipe === 'batal_qris') { clearResetTimer(); tampilPanel('lpKeranjang<%=rndLp%>'); }
            else if (d.tipe === 'minta_pin') tampilPin(d);
            else if (d.tipe === 'sukses') tampilSukses(d);
            else if (d.tipe === 'reset') window['lpKembaliIdle<%=rndLp%>']();
        };
        // Minta status terkini begitu layar ini terbuka (mis. dibuka ulang di tengah transaksi).
        try { channel.postMessage({ tipe: 'minta_status' }); } catch(e) {}
    }

    // ---------- Jam berjalan (layar sambutan) ----------
    function updateJam(){
        var el = $('lpJam<%=rndLp%>'); if (!el) return;
        var now = new Date();
        var pad = function(n){ return n.toString().padStart(2,'0'); };
        var hari = ['Minggu','Senin','Selasa','Rabu','Kamis','Jumat','Sabtu'][now.getDay()];
        var bln = ['Januari','Februari','Maret','April','Mei','Juni','Juli','Agustus','September','Oktober','November','Desember'][now.getMonth()];
        el.textContent = hari + ', ' + now.getDate() + ' ' + bln + ' ' + now.getFullYear() + '  ' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());
    }
    updateJam(); setInterval(updateJam, 1000);

    // ---------- Layar penuh ----------
    window['lpToggleFullscreen<%=rndLp%>'] = function(){
        if (!document.fullscreenElement) { (document.documentElement.requestFullscreen || function(){}).call(document.documentElement); }
        else { (document.exitFullscreen || function(){}).call(document); }
    };
})();
</script>
</body>
</html>
