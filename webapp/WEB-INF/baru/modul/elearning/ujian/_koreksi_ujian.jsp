<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="org.zkoss.zul.Label"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.*"%>
<%@page import="org.jsoup.Jsoup"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
String idHasilParam = request.getParameter("idHasil");
String random = Common.getGeneratedBarCode(7);

if (idHasilParam == null || idHasilParam.trim().isEmpty()) {
    out.println("<div class='alert alert-danger shadow-sm fw-bold'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Parameter identitas rekaman ujian tidak lengkap.") + "</div>");
    return;
}

Session mySession = null;
HasilUjianMahasiswa hum = null;

try {
    mySession = HibernateUtil.openSession();
    hum = (HasilUjianMahasiswa) mySession.get(HasilUjianMahasiswa.class, Long.parseLong(idHasilParam));
    if (hum == null) {
        out.println("<div class='alert alert-warning shadow-sm fw-bold'><i class='fas fa-info-circle me-2'></i>" + Common.getBahasaConfig("Dokumen hasil ujian peserta tidak ditemukan di dalam peladen.") + "</div>");
        return;
    }

    PertemuanPunyaUjian ppu = hum.getPertemuanPunyaUjian();
    ais.ui.util.MyArrayList<Long> ujianPunyaSoals = hum.ambilUjianPunyaSoals(ppu.getJmlDitampilkan(), new Label(), true);
    Map<Long, Set<Long>> humDetails = hum.ambilHasilUjianMahasiswaDetail(ppu.getJmlDitampilkan(), ujianPunyaSoals, false);
    
    // --- PENANGANAN FOTO & NAMA PESERTA ---
    String namaPeserta = Common.getBahasaConfig("Peserta Tanpa Nama");
    String urlFoto = Common.ROOT + "/images/default_user.png";
    
    if (hum.getMahasiswa() != null) {
        namaPeserta = hum.getMahasiswa().getNama();
        urlFoto = CommonMedia.getUrlFotoPengguna(new Tbmuser(hum.getMahasiswa()));
    } else if (hum.getBiodataCalonMahasiswa() != null) {
        namaPeserta = hum.getBiodataCalonMahasiswa().getNama();
    }
    
    // --- PENGECEKAN STATUS OBE & FORMAT NILAI ---
    boolean isObe = false;
    List<FormatNilai> formatNilais = new ArrayList<FormatNilai>();
    if (ppu.getPertemuan() != null && ppu.getPertemuan().getPerkuliahan() != null) {
        if (ppu.getPertemuan().getPerkuliahan().getKurikulum() != null && 
            ppu.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
                ppu.getPertemuan().getPerkuliahan().getTahunAjaran(), 
                ppu.getPertemuan().getPerkuliahan().getGanjilGenap())) {
            isObe = true;
            formatNilais = Common.getFormatNilais(mySession, ppu.getPertemuan().getPerkuliahan());
        }
    }
%>

<div class="container-fluid py-3 bg-light rounded-3">
    <div class="card border-0 shadow-sm rounded-4 mb-4 bg-primary bg-gradient text-white">
        <div class="card-body p-4 d-flex flex-column flex-md-row justify-content-between align-items-md-center">
            
            <div class="d-flex align-items-center mb-3 mb-md-0">
                <img src="<%= urlFoto %>" class="rounded-circle shadow-sm me-4 border border-3 border-white" style="width: 70px; height: 70px; object-fit: cover; background-color: #fff;" alt="Foto Peserta">
                <div>
                    <h5 class="fw-bold mb-2"><%= namaPeserta %></h5>
                    <span class="badge bg-white text-dark shadow-sm px-3 py-2 rounded-pill fw-bold">
                        <i class="far fa-file-alt text-primary me-2"></i><%= Common.getBahasaConfig("Lembar Jawab Evaluasi") %>
                    </span>
                </div>
            </div>
            
            <div class="text-md-end bg-white bg-opacity-25 rounded-4 p-3 border border-white border-opacity-50">
                <div class="text-uppercase fw-bold text-white-50" style="font-size: 0.75rem; letter-spacing: 1px;"><%= Common.getBahasaConfig("Akumulasi Nilai Sementara") %></div>
                <div class="fs-1 fw-bold lh-1 mt-1"><%= Common.numberFormat.get().format(hum.getNilai() != null ? hum.getNilai() : 0.0) %></div>
                
                <% if (isObe) { 
                    JSONObject humObe = new JSONObject(hum.getNilaiObe() != null && hum.getNilaiObe().trim().startsWith("{") ? hum.getNilaiObe() : "{}");
                %>
                    <div class="d-flex flex-wrap justify-content-md-end gap-2 mt-3 border-top border-white border-opacity-25 pt-3">
                        <% for (FormatNilai fn : formatNilais) { 
                            if (fn.getStatusPertemuan() != null) {
                                Double score = humObe.isNull(fn.getId().toString()) ? 0.0 : humObe.getDouble(fn.getId().toString());
                                Double max = humObe.isNull(fn.getId().toString() + "_max") ? 0.0 : humObe.getDouble(fn.getId().toString() + "_max");
                                String displayScore = max > 0 ? Common.numberFormat.get().format((score * 100.0) / max) : "0";
                        %>
                            <span class="badge bg-white text-primary shadow-sm px-2 py-1" style="font-size: 0.75rem;" title="<%= fn.getNama() %>">
                                <%= fn.getNama() %> : <b class="ms-1 fs-6"><%= displayScore %></b>
                            </span>
                        <%  } 
                        } %>
                    </div>
                <% } %>
            </div>
            
        </div>
    </div>

    <% java.util.List<String> aiEsaiIds = new java.util.ArrayList<String>();
       java.util.List<String> aiEsaiMaks = new java.util.ArrayList<String>(); %>

    <div class="d-flex justify-content-end mb-3">
        <button type="button" id="btnKoreksiSemuaAi<%=random%>" class="btn fw-bold text-white shadow-sm" style="background-color:#16a34a;" onclick="koreksiSemuaEsaiAi_<%=random%>()">
            <i class="fas fa-magic me-2"></i><%= Common.getBahasaConfig("Koreksi Semua Esai via AI") %>
        </button>
    </div>
    <div id="progresKoreksiAi<%=random%>" class="alert alert-info d-none small mb-3"></div>

    <div class="row g-4">
        <%
        for (Long upsId : ujianPunyaSoals) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, upsId.toString());
            if (ups == null) continue;
            
            BankSoal bs = ups.getBankSoal();
            boolean isPG = BankSoal.PILIHAN_GANDA.equals(bs.getJenis());
            
            Set<Long> ansSet = humDetails.get(bs.getId());
            HasilUjianMahasiswaDetail humd = null;
            if (ansSet != null && !ansSet.isEmpty()) {
                humd = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, ansSet.iterator().next().toString());
            }
            
            String humdId = humd != null ? humd.getId().toString() : "";
            if (!isPG && humd != null) { aiEsaiIds.add(humdId); aiEsaiMaks.add(String.valueOf(bs.getSkor() != null ? bs.getSkor() : 0.0)); }
        %>
        <div class="col-12">
            <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
                <div class="card-header bg-white border-bottom border-2 pt-4 pb-3 px-4 d-flex justify-content-between align-items-center">
                    <h6 class="fw-bold mb-0 text-dark">
                        <span class="bg-primary text-white rounded-circle d-inline-flex justify-content-center align-items-center me-2 shadow-sm" style="width: 28px; height: 28px; font-size: 0.85rem;"><%= ups.getNomorUrut() %></span> 
                        <%= Common.getBahasaConfig("Rincian Pertanyaan") %>
                    </h6>
                    <span class="badge bg-light text-secondary border border-secondary-subtle px-3 py-2 rounded-pill shadow-sm">
                        <i class="fas fa-weight-hanging me-2"></i><%= Common.getBahasaConfig("Bobot Maksimal:") %> <%= Common.numberFormat.get().format(bs.getSkor()) %>
                    </span>
                </div>
                
                <div class="card-body p-4">
                    <div class="mb-4 pb-4 border-bottom border-secondary-subtle border-dashed">
                        <div class="text-dark fs-6" style="line-height: 1.7;"><%= bs.getSoal() %></div>
                    </div>

                    <div class="row g-4">
                        <div class="col-lg-7 border-end-lg">
                            <h6 class="fw-bold text-muted small text-uppercase mb-3">
                                <i class="fas fa-comment-dots text-primary me-2"></i><%= Common.getBahasaConfig("Tanggapan Peserta Ujian:") %>
                            </h6>
                            
                            <% if (humd == null) { %>
                                <div class="alert alert-danger bg-opacity-10 border-danger border-dashed text-danger fw-bold d-flex align-items-center rounded-3 p-4">
                                    <i class="fas fa-exclamation-triangle fs-3 me-3 opacity-75"></i> 
                                    <div>
                                        <span class="d-block fs-5"><%= Common.getBahasaConfig("Lembar Kosong") %></span>
                                        <span class="small fw-normal text-dark"><%= Common.getBahasaConfig("Peserta melewati dan tidak merekam tanggapan pada butir pertanyaan ini.") %></span>
                                    </div>
                                </div>
                            <% } else { %>
                                
                                <% if (isPG) { 
                                    BankSoalDetail bsd = humd.getBankSoalDetail();
                                    boolean isBenar = bsd != null && bsd.getBetul();
                                %>
                                    <div class="p-4 bg-light rounded-4 border border-<%= isBenar ? "success" : "danger" %>-subtle position-relative overflow-hidden shadow-sm">
                                        <div class="position-absolute top-0 start-0 w-100" style="height: 4px; background-color: <%= isBenar ? "#198754" : "#dc3545" %>;"></div>
                                        <div class="d-flex justify-content-between align-items-start mb-3">
                                            <span class="badge bg-<%= isBenar ? "success" : "danger" %> shadow-sm px-3 py-2 rounded-pill fs-6">
                                                <i class="fas fa-<%= isBenar ? "check" : "times" %>-circle me-2"></i><%= Common.getBahasaConfig(isBenar ? "Tepat / Valid" : "Keliru / Salah") %>
                                            </span>
                                            <span class="text-muted fw-bold small bg-white px-3 py-2 rounded-pill shadow-sm border"><%= Common.getBahasaConfig("Skor Evaluasi:") %> <%= Common.numberFormat.get().format(isBenar ? bs.getSkor() : bs.getSkorSalah()) %></span>
                                        </div>
                                        <div class="fs-6 text-dark mt-3" style="line-height: 1.6;">
                                            <b class="text-primary fs-5 me-2"><%= bsd != null ? bsd.getHuruf() + "." : "-" %></b> 
                                            <%= bsd != null ? bsd.getJawaban() : (humd.getJawaban().isEmpty() ? Common.getBahasaConfig("Tidak Terekam") : humd.getJawaban()) %>
                                        </div>
                                    </div>
                                <% } else { %>
                                    <div class="p-4 bg-light rounded-4 border border-secondary-subtle shadow-sm">
                                        <div class="text-dark" style="white-space: pre-wrap; line-height: 1.6; font-size: 0.95rem;"><%= humd.getJawaban() %></div>
                                    </div>
                                <% } %>

                            <% } %>
                        </div>
                        
                        <div class="col-lg-5 ps-lg-4">
                            <h6 class="fw-bold text-muted small text-uppercase mb-3">
                                <i class="fas fa-chalkboard-teacher text-success me-2"></i><%= Common.getBahasaConfig("Bilik Evaluasi Dosen:") %>
                            </h6>
                            
                            <% if (humd != null) { %>
                                
                                <% if (!isPG) { %>
                                    <div class="mb-4 bg-white p-3 rounded-4 shadow-sm border border-primary-subtle">
                                        <label class="form-label small fw-bold text-primary mb-3 d-block">
                                            <i class="fas fa-star text-warning me-2"></i><%= Common.getBahasaConfig("Beri Skor Manual Uraian") %>
                                        </label>
                                        <div class="input-group input-group-lg shadow-sm rounded-pill overflow-hidden">
                                            <input type="number" step="0.01" max="<%= bs.getSkor() %>" id="nilai_<%= humdId %>" class="form-control fw-bold text-center text-primary fs-4 border-0 bg-light" value="<%= humd.getNilai() != null ? humd.getNilai() : 0 %>" onchange="simpanKoreksi_<%=random%>('<%= humdId %>', <%= bs.getSkor() %>)">
                                            <span class="input-group-text bg-white border-0 fw-bold text-muted px-4">/ <%= Common.numberFormat.get().format(bs.getSkor()) %></span>
                                        </div>
                                        <button type="button" id="btnAiKoreksi_<%= humdId %>" class="btn btn-sm w-100 mt-3 fw-bold text-white shadow-sm" style="background-color:#16a34a;" onclick="koreksiSatuAi_<%=random%>('<%= humdId %>', <%= bs.getSkor() %>, false)">
                                            <i class="fas fa-magic me-2"></i><%= Common.getBahasaConfig("Koreksi via AI") %>
                                        </button>
                                    </div>
                                <% } else { %>
                                    <div class="mb-3">
                                        <button type="button" id="btnAiKoreksi_<%= humdId %>" class="btn btn-sm w-100 fw-bold shadow-sm" style="border:1px solid #16a34a;color:#16a34a;background:#fff;" onclick="koreksiSatuAi_<%=random%>('<%= humdId %>', <%= bs.getSkor() %>, true)">
                                            <i class="fas fa-comment-dots me-2"></i><%= Common.getBahasaConfig("Beri Umpan Balik via AI") %>
                                        </button>
                                    </div>
                                <% } %>

                                <div class="mb-2 position-relative">
                                    <label class="form-label small fw-bold text-dark"><i class="fas fa-pen-nib text-secondary me-2"></i><%= Common.getBahasaConfig("Beri Ulasan / Catatan Khusus:") %></label>
                                    <textarea id="catatan_<%= humdId %>" class="form-control bg-light border-secondary-subtle rounded-4 shadow-sm p-3" rows="4" placeholder="<%= Common.getBahasaConfig("Tuliskan deskripsi koreksi, masukan, atau alasan pemberian nilai untuk diketahui oleh mahasiswa yang bersangkutan...") %>" onchange="simpanKoreksi_<%=random%>('<%= humdId %>', <%= bs.getSkor() %>)"><%= humd.getKoreksi() != null ? humd.getKoreksi() : "" %></textarea>
                                </div>
                                <div id="status_<%= humdId %>" class="text-success small fw-bold mt-2" style="display: none; transition: opacity 0.5s;">
                                    <i class="fas fa-check-circle me-1"></i><%= Common.getBahasaConfig("Perubahan berhasil direkam secara otomatis oleh sistem.") %>
                                </div>
                            
                            <% } else { %>
                                <div class="d-flex align-items-center justify-content-center h-100 bg-light rounded-4 border border-dashed p-4 text-center">
                                    <div>
                                        <i class="fas fa-ban fs-2 text-muted mb-3 opacity-50"></i>
                                        <p class="text-muted small fw-bold mb-0"><%= Common.getBahasaConfig("Formulir penilaian dinonaktifkan secara otomatis karena peserta ujian tidak menyerahkan jawaban untuk butir pertanyaan ini.") %></p>
                                    </div>
                                </div>
                            <% } %>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <% } %>
    </div>
</div>

<style>
    .border-dashed { border-style: dashed !important; }
    .border-end-lg { border-right: 1px solid #dee2e6 !important; }
    @media (max-width: 991.98px) { 
        .border-end-lg { border-right: none !important; border-bottom: 1px solid #dee2e6 !important; padding-bottom: 1.5rem !important; margin-bottom: 1.5rem !important; } 
    }
</style>

<script>
    function simpanKoreksi_<%=random%>(idDetail, maxSkor) {
        const inputNilai = document.getElementById('nilai_' + idDetail);
        const inputCatatan = document.getElementById('catatan_' + idDetail);
        const statusEl = document.getElementById('status_' + idDetail);
        
        let nilaiVal = inputNilai ? parseFloat(inputNilai.value) : null;
        let catatanVal = inputCatatan ? inputCatatan.value : '';

        if (inputNilai && nilaiVal > maxSkor) {
            alert('<%= Common.getBahasaConfigJS("Peringatan Mutlak: Anda tidak diperkenankan memberikan skor melampaui bobot maksimal yang telah ditetapkan (") %>' + maxSkor + ').');
            inputNilai.value = maxSkor;
            nilaiVal = maxSkor;
        }

        const formData = new URLSearchParams();
        formData.append('idDetail', idDetail);
        formData.append('koreksi', catatanVal);
        if(inputNilai) formData.append('nilai', nilaiVal);

        statusEl.style.display = 'none';

        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_koreksi_ujian_services', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
            if(data.status === 'success') {
                statusEl.style.display = 'block';
                statusEl.style.opacity = '1';
                setTimeout(() => { 
                    statusEl.style.opacity = '0'; 
                    setTimeout(() => { statusEl.style.display = 'none'; }, 500);
                }, 3000);
            } else {
                alert('<%= Common.getBahasaConfigJS("Peladen mengalami kendala dalam merekam data koreksi: ") %>' + data.message);
            }
        })
        .catch(err => {
            console.error(err);
            alert('<%= Common.getBahasaConfigJS("Sistem gagal mengirimkan catatan koreksi ke peladen utama. Mohon periksa koneksi jaringan Anda.") %>');
        });
    }

    // ================= KOREKSI OTOMATIS via AI =================
    var aiEsaiList_<%=random%> = [<% for (int _i = 0; _i < aiEsaiIds.size(); _i++) { %><%= _i > 0 ? "," : "" %>{id:'<%= aiEsaiIds.get(_i) %>',maks:<%= aiEsaiMaks.get(_i) %>}<% } %>];

    function ekstrakObjAi_<%=random%>(teks) {
        if (!teks) return null;
        var s = teks.trim();
        var f = s.indexOf('```');
        if (f >= 0) { var nl = s.indexOf('\n', f); var fa = s.lastIndexOf('```');
            if (nl >= 0 && fa > nl) s = s.substring(nl + 1, fa).trim(); else if (nl >= 0) s = s.substring(nl + 1).trim(); }
        var depth = 0, mulai = -1, ds = false, esc = false;
        for (var i = 0; i < s.length; i++) { var c = s[i];
            if (ds) { if (esc) esc = false; else if (c === '\\') esc = true; else if (c === '"') ds = false; continue; }
            if (c === '"') ds = true;
            else if (c === '{') { if (depth === 0) mulai = i; depth++; }
            else if (c === '}') { if (depth > 0) { depth--; if (depth === 0 && mulai >= 0) { try { return JSON.parse(s.substring(mulai, i + 1)); } catch (e) {} mulai = -1; } } }
        }
        try { return JSON.parse(s); } catch (e) { return null; }
    }

    function koreksiSatuAi_<%=random%>(humdId, maks, isPg) {
        return _koreksiAiInternal_<%=random%>(humdId, maks, isPg, true);
    }

    function _koreksiAiInternal_<%=random%>(humdId, maks, isPg, tampilkanError) {
        return new Promise(function(resolve) {
            var btn = document.getElementById('btnAiKoreksi_' + humdId);
            var teksAsli = btn ? btn.innerHTML : '';
            if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%= Common.getBahasaConfig("Memproses...") %>'; }
            var bodyP = new URLSearchParams(); bodyP.append('idDetail', humdId);
            fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_prompt_koreksi_ai', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: bodyP.toString() })
            .then(function(r){ return r.json(); })
            .then(function(dp){
                if (!dp || dp.status !== 'success' || !dp.prompt) throw new Error(dp && dp.message ? dp.message : '<%= Common.getBahasaConfigJS("Gagal menyiapkan prompt koreksi.") %>');
                return fetch('<%=Common.ROOT%>/Ai', { method:'POST', headers:{'Content-Type':'application/json; charset=UTF-8'}, body: JSON.stringify({ instruksi: dp.prompt, prompt: dp.prompt, maxTokens: 900, temperature: 0.3, source:'koreksi_ai' }) }).then(function(r){ return r.json(); });
            })
            .then(function(da){
                var teks = (da && (da.text || da.response || da.message)) ? (da.text || da.response || da.message) : '';
                if (!da || da.success === false || !teks) throw new Error(da && (da.error || da.raw) ? (da.error || da.raw) : '<%= Common.getBahasaConfigJS("Respons AI kosong.") %>');
                var obj = ekstrakObjAi_<%=random%>(teks);
                if (!obj) throw new Error('<%= Common.getBahasaConfigJS("Format respons AI tidak dikenali.") %>');
                var inputCat = document.getElementById('catatan_' + humdId);
                if (inputCat && obj.koreksi != null) inputCat.value = obj.koreksi;
                if (!isPg) {
                    var inputNil = document.getElementById('nilai_' + humdId);
                    if (inputNil && obj.skor != null) { var sk = parseFloat(obj.skor); if (isNaN(sk)) sk = 0; if (sk > maks) sk = maks; if (sk < 0) sk = 0; inputNil.value = sk; }
                }
                simpanKoreksi_<%=random%>(humdId, maks);
                if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fas fa-check me-2"></i><%= Common.getBahasaConfig("Terkoreksi") %>'; }
                resolve(true);
            })
            .catch(function(err){ console.error(err); if (tampilkanError) alert('AI: ' + (err && err.message ? err.message : err)); if (btn) { btn.disabled = false; btn.innerHTML = teksAsli; } resolve(false); });
        });
    }

    function koreksiSemuaEsaiAi_<%=random%>() {
        var list = aiEsaiList_<%=random%>;
        var prog = document.getElementById('progresKoreksiAi<%=random%>');
        var btn = document.getElementById('btnKoreksiSemuaAi<%=random%>');
        if (!list || list.length === 0) { if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Tidak ada soal esai untuk dikoreksi.") %>', 'bg-warning'); return; }
        if (btn) btn.disabled = true;
        if (prog) prog.className = 'alert alert-info small mb-3';
        var i = 0, ok = 0;
        function next() {
            if (i >= list.length) {
                if (prog) { prog.className = 'alert alert-success small mb-3'; prog.innerHTML = '<i class="fas fa-check-circle me-2"></i>' + ok + ' / ' + list.length + ' <%= Common.getBahasaConfig("esai berhasil dikoreksi via AI.") %>'; }
                if (btn) btn.disabled = false;
                if (typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Koreksi AI selesai.") %> (' + ok + '/' + list.length + ')', 'bg-success');
                return;
            }
            var item = list[i];
            if (prog) prog.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%= Common.getBahasaConfig("Mengoreksi esai") %> ' + (i + 1) + ' / ' + list.length + '...';
            _koreksiAiInternal_<%=random%>(item.id, item.maks, false, false).then(function(res){ if (res) ok++; i++; setTimeout(next, 300); });
        }
        next();
    }
</script>

<%
} catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_koreksi_ujian.jsp:277"); } 
finally { ais.common.ElearningSessionUtil.closeQuietly(mySession); }
%>