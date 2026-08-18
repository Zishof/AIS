<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.Ujian"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
String ppuParam = request.getParameter("ppu");
String random = Common.getGeneratedBarCode(7);

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || ppuParam == null || ppuParam.trim().isEmpty()) {
    out.println("<div class='alert alert-danger shadow-sm fw-bold'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Sesi Anda telah berakhir atau parameter tidak valid.") + "</div>");
    return;
}

Session mySession = null;
PertemuanPunyaUjian ppu = null;
boolean isObe = false;
try {
    mySession = HibernateUtil.openSession();
    ppu = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, Long.parseLong(ppuParam.trim()));
    
    if (ppu != null && ppu.getPertemuan() != null && ppu.getPertemuan().getPerkuliahan() != null) {
        if (ppu.getPertemuan().getPerkuliahan().getKurikulum() != null && 
            ppu.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
                ppu.getPertemuan().getPerkuliahan().getTahunAjaran(), 
                ppu.getPertemuan().getPerkuliahan().getGanjilGenap())) {
            isObe = true;
        }
    }
} catch(Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_lihat_hasil_ujian.jsp:34"); } 
finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

if (ppu == null || ppu.getUjian() == null) {
    out.println("<div class='alert alert-warning shadow-sm fw-bold'><i class='fas fa-info-circle me-2'></i>" + Common.getBahasaConfig("Data evaluasi ujian tidak ditemukan di dalam sistem.") + "</div>");
    return;
}

boolean isPG = ais.database.model.BankSoal.PILIHAN_GANDA.equals(ppu.getUjian().getJenis());
%>

<div class="container-fluid p-0 bg-white">
    <div class="d-flex flex-column flex-lg-row justify-content-between align-items-center bg-light p-3 border-bottom shadow-sm">
        <div class="d-flex flex-wrap gap-2 mb-2 mb-lg-0">
            <button class="btn btn-sm btn-primary fw-bold rounded-pill shadow-sm px-3" onclick="muatDataHasilUjian_<%=random%>(true)">
                <i class="fas fa-sync-alt me-1"></i> <%= Common.getBahasaConfig("Segarkan Data") %>
            </button>
            
            <button id="btnHitungUlangSemua_<%=random%>" class="btn btn-sm btn-outline-danger fw-bold rounded-pill shadow-sm px-3" onclick="hitungUlangSemua_<%=random%>()">
                <i class="fas fa-calculator me-1"></i> <%= Common.getBahasaConfig("Kalkulasi Ulang Massal") %>
            </button>

            <button class="btn btn-sm btn-outline-success fw-bold rounded-pill shadow-sm px-3" onclick="downloadHasilExcel_<%=random%>()">
                <i class="fas fa-file-excel me-1"></i> <%= Common.getBahasaConfig("Ekspor Rekap Excel") %>
            </button>

            <button class="btn btn-sm btn-outline-dark fw-bold rounded-pill shadow-sm px-3" onclick="window.open('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=download_semua&ppu=<%=ppu.getId()%>', '_blank');">
                <i class="fas fa-download me-1"></i> <%= Common.getBahasaConfig("Unduh Semua Jawaban") %>
            </button>
            
            <% if(isPG) { %>
                <button class="btn btn-sm btn-outline-secondary fw-bold rounded-pill shadow-sm px-3" onclick="analisisButirSoal_<%=random%>()">
                    <i class="fas fa-chart-bar me-1"></i> <%= Common.getBahasaConfig("Analisis Butir Soal") %>
                </button>
                <% if(isObe) { %>
                    <button class="btn btn-sm btn-outline-primary fw-bold rounded-pill shadow-sm px-3" onclick="analisisButirSoalObe_<%=random%>()">
                        <i class="fas fa-project-diagram me-1"></i> <%= Common.getBahasaConfig("Analisis Soal Berbasis OBE") %>
                    </button>
                <% } %>
            <% } %>
        </div>
        
        <div class="input-group w-100 w-lg-25 shadow-sm rounded-pill overflow-hidden">
            <input type="text" id="pencarianPeserta_<%=random%>" class="form-control form-control-sm border-0 bg-white ps-3" placeholder="<%= Common.getBahasaConfig("Pencarian Nama atau NIM/No.Reg...") %>" onkeypress="if(event.key === 'Enter') muatDataHasilUjian_<%=random%>(false)">
            <button class="btn btn-sm btn-secondary border-0 px-3" onclick="muatDataHasilUjian_<%=random%>(false)">
                <i class="fas fa-search"></i>
            </button>
        </div>
    </div>

    <div class="row g-0">
        <div class="col-md-8 border-end" style="height: 75vh; overflow-y: auto;">
            <table class="table table-hover table-striped align-middle mb-0">
                <thead class="table-dark sticky-top" style="z-index: 10;">
                    <tr class="text-center small text-uppercase">
                        <th width="5%" class="py-3"><%= Common.getBahasaConfig("No.") %></th>
                        <th width="35%" class="text-start py-3"><%= Common.getBahasaConfig("Identitas Peserta") %></th>
                        <th width="15%" class="py-3"><%= Common.getBahasaConfig("Durasi Waktu") %></th>
                        <th width="15%" class="py-3"><%= Common.getBahasaConfig("Progres") %></th>
                        <th width="20%" class="py-3"><%= Common.getBahasaConfig("Perolehan Nilai") %></th>
                        <th width="10%" class="py-3"><%= Common.getBahasaConfig("Tindakan") %></th>
                    </tr>
                </thead>
                <tbody id="tbodyHasil_<%=random%>">
                    <tr>
                        <td colspan="6" class="text-center py-5 text-muted">
                            <div class="spinner-border text-primary mb-3" style="width: 2.5rem; height: 2.5rem;"></div>
                            <br/><b class="text-dark fs-6"><%= Common.getBahasaConfig("Menyiapkan antarmuka dan memuat data...") %></b>
                            <br/><small><%= Common.getBahasaConfig("Mohon tunggu beberapa saat.") %></small>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="col-md-4 bg-light" style="height: 75vh; overflow-y: auto;">
            <div class="p-4">
                <h6 class="fw-bold border-bottom border-2 border-primary pb-2 mb-4 text-dark text-uppercase">
                    <i class="fas fa-chart-pie text-primary me-2"></i><%= Common.getBahasaConfig("Ringkasan Eksekutif") %>
                </h6>
                
                <div id="wadahStatistik_<%=random%>" class="text-center text-muted py-5">
                    <div class="spinner-border spinner-border-sm text-secondary mb-2"></div>
                    <br/><small><%= Common.getBahasaConfig("Sedang mengkalkulasi statistik peserta...") %></small>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    function muatDataHasilUjian_<%=random%>(forceRefresh) {
        const tbody = document.getElementById('tbodyHasil_<%=random%>');
        const wadahStat = document.getElementById('wadahStatistik_<%=random%>');
        const kataKunci = document.getElementById('pencarianPeserta_<%=random%>').value.trim();

        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5"><div class="spinner-border text-primary mb-3" style="width: 2.5rem; height: 2.5rem;"></div><br/><b class="text-dark fs-6"><%= Common.getBahasaConfig("Memproses perhitungan nilai dan mengambil rekaman data...") %></b><br/><small class="text-muted"><%= Common.getBahasaConfig("Proses ini dapat memakan waktu bergantung pada volume peserta ujian.") %></small></td></tr>';
        wadahStat.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-secondary mb-3"></div><br/><small class="text-muted fw-bold"><%= Common.getBahasaConfig("Menyusun data statistik...") %></small></div>';

        const formData = new URLSearchParams();
        formData.append('ppu', '<%=ppuParam%>');
        formData.append('keyword', kataKunci);
        formData.append('refresh', forceRefresh ? 'true' : 'false');

        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_lihat_hasil_ujian_services', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
            if(data.status === 'success') {
                renderTabelHasil_<%=random%>(data.peserta);
                renderStatistik_<%=random%>(data.statistik);
            } else {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-danger fw-bold"><i class="fas fa-exclamation-triangle fs-1 d-block mb-3"></i>' + data.message + '</td></tr>';
                wadahStat.innerHTML = '<div class="text-center py-4 text-danger fw-bold"><i class="fas fa-times-circle fs-2 d-block mb-2"></i> <%= Common.getBahasaConfig("Gagal memuat statistik.") %></div>';
            }
        })
        .catch(err => {
            console.error(err);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-danger fw-bold"><i class="fas fa-wifi fs-1 d-block mb-3"></i><%= Common.getBahasaConfig("Gagal terhubung dengan peladen utama.") %></td></tr>';
        });
    }

    function renderTabelHasil_<%=random%>(listPeserta) {
        const tbody = document.getElementById('tbodyHasil_<%=random%>');
        if(!listPeserta || listPeserta.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-5 text-muted fw-bold"><i class="fas fa-folder-open fs-1 d-block mb-3 opacity-50"></i><%= Common.getBahasaConfig("Tidak ada rekaman peserta ujian yang cocok ditemukan.") %></td></tr>';
            return;
        }

        let html = '';
        listPeserta.forEach((p, idx) => {
            let bgClass = '';
            if (p.persenSelesai === 100) bgClass = 'bg-success bg-opacity-10';
            else if (p.persenSelesai > 0) bgClass = 'bg-warning bg-opacity-10';

            let btnHitungUlang = `
                <button class="btn btn-sm btn-outline-secondary w-100 fw-bold border-secondary-subtle shadow-sm mt-2 rounded-pill" style="font-size: 0.7rem;" onclick="hitungUlangNilai_<%=random%>(event, '`+p.idHasil+`')" title="<%= Common.getBahasaConfig("Kalkulasi Ulang Nilai Peserta Ini") %>">
                    <i class="fas fa-sync-alt me-1"></i> <%= Common.getBahasaConfig("Kalkulasi") %>
                </button>
            `;

            let nilaiHtml = '';
            if (p.isPG) {
                if (p.isObe) {
                    if (p.obeScores && p.obeScores.length > 0) {
                        nilaiHtml = '<div class="d-flex flex-column gap-1 text-start align-items-center">';
                        p.obeScores.forEach(os => {
                            nilaiHtml += '<span class="badge bg-white text-primary border border-primary-subtle text-wrap shadow-sm w-100" style="font-size:0.7rem;">'+os.nama+' : '+os.nilaiStr+'</span>';
                        });
                        nilaiHtml += '</div>';
                    } else {
                        nilaiHtml = '<span class="text-muted small fst-italic"><i class="fas fa-clock me-1"></i><%= Common.getBahasaConfig("Menunggu Kalkulasi") %></span>';
                    }
                    nilaiHtml += btnHitungUlang;
                } else {
                    nilaiHtml = '<span class="text-success fw-bold fs-5">'+p.nilaiAkhir+'</span>' + btnHitungUlang;
                }
            } else {
                if (p.isObe) {
                    let obeInputs = '<div class="d-flex flex-column gap-2 mb-2">';
                    if(p.obeScores && p.obeScores.length > 0) {
                        p.obeScores.forEach(os => {
                            let shortName = os.nama.length > 10 ? os.nama.substring(0,10)+'..' : os.nama;
                            obeInputs += `
                            <div class="input-group input-group-sm shadow-sm rounded-pill overflow-hidden">
                                <span class="input-group-text bg-light text-muted fw-bold border-0" style="font-size:0.65rem; width: 80px;" title="`+os.nama+`">`+shortName+`</span>
                                <input type="number" step="0.01" class="form-control text-center fw-bold input-obe-`+p.idHasil+` border-0 bg-white" data-fnid="`+os.id+`" value="`+os.nilaiDapat+`">
                            </div>`;
                        });
                    } else {
                        obeInputs += '<span class="text-muted small"><i class="fas fa-exclamation-triangle me-1"></i><%= Common.getBahasaConfig("Format CPMK belum tersedia") %></span>';
                    }
                    obeInputs += '</div>';
                    
                    nilaiHtml = obeInputs + `
                        <button class="btn btn-sm btn-primary w-100 fw-bold shadow-sm rounded-pill mb-1" style="font-size: 0.7rem;" onclick="simpanNilaiManualObe_<%=random%>('`+p.idHasil+`', this)">
                            <i class="fas fa-save me-1"></i> <%= Common.getBahasaConfig("Simpan Skor") %>
                        </button>
                    ` + btnHitungUlang;
                } else {
                    nilaiHtml = `
                        <div class="input-group input-group-sm mb-2 shadow-sm rounded-pill overflow-hidden">
                            <input type="number" step="0.01" class="form-control text-center fw-bold border-0 bg-white" id="inputNilai_`+p.idHasil+`" value="`+p.nilaiAkhir+`" onchange="simpanNilaiManual_<%=random%>('`+p.idHasil+`', this.value)">
                        </div>
                    ` + btnHitungUlang;
                }
            }

            html += `
                <tr class="`+bgClass+`">
                    <td class="text-center fw-bold text-muted">`+(idx+1)+`</td>
                    <td>
                        <div class="d-flex align-items-center gap-3">
                            <img src="`+p.foto+`" class="rounded-circle shadow-sm border border-2 border-white" style="width:45px; height:45px; object-fit:cover;">
                            <div>
                                <div class="fw-bold text-dark" style="font-size: 0.95rem;">`+p.nama+`</div>
                                <div class="text-secondary" style="font-size: 0.8rem;"><i class="far fa-id-card me-1"></i>`+p.nim+`</div>
                            </div>
                        </div>
                    </td>
                    <td class="small">
                        <div class="text-success fw-bold mb-1"><i class="far fa-play-circle me-1"></i>`+p.waktuMulai+`</div>
                        <div class="text-danger fw-bold"><i class="far fa-stop-circle me-1"></i>`+p.waktuSelesai+`</div>
                    </td>
                    <td class="text-center">
                        <div class="fw-bold text-dark mb-1">`+p.soalTerjawab+` / `+p.totalSoal+` <%= Common.getBahasaConfig("Soal") %></div>
                        <div class="progress shadow-sm border border-secondary-subtle rounded-pill" style="height: 6px;">
                            <div class="progress-bar bg-`+(p.persenSelesai===100?'success':'warning')+`" style="width: `+p.persenSelesai+`%;"></div>
                        </div>
                    </td>
                    <td class="text-center align-middle">
                        `+nilaiHtml+`
                    </td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-outline-info text-dark fw-bold shadow-sm rounded-pill w-100" onclick="lihatDetailKoreksi_<%=random%>('`+p.idHasil+`')" title="<%= Common.getBahasaConfig("Buka Lembar Jawaban Peserta") %>">
                            <i class="fas fa-user-edit me-1"></i> <%= Common.getBahasaConfig("Koreksi") %>
                        </button>
                    </td>
                </tr>
            `;
        });
        tbody.innerHTML = html;
    }

    function renderStatistik_<%=random%>(stat) {
        const wadah = document.getElementById('wadahStatistik_<%=random%>');
        if(!stat) return;

        let html = `
            <div class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-4">
                    <h6 class="fw-bold text-secondary text-uppercase mb-3" style="font-size: 0.8rem;">
                        <i class="fas fa-users me-2"></i><%= Common.getBahasaConfig("Keterlibatan Peserta") %>
                    </h6>
                    <div class="d-flex justify-content-between mb-2 small fw-bold text-dark">
                        <span><i class="fas fa-check text-success me-1"></i><%= Common.getBahasaConfig("Hadir:") %> `+stat.jmlIkut+`</span>
                        <span><i class="fas fa-times text-danger me-1"></i><%= Common.getBahasaConfig("Absen:") %> `+stat.jmlBelumIkut+`</span>
                    </div>
                    <div class="progress rounded-pill shadow-sm mb-3" style="height: 12px;">
                        <div class="progress-bar bg-success progress-bar-striped" style="width: `+stat.persenIkut+`%;"></div>
                        <div class="progress-bar bg-light border" style="width: `+stat.persenBelumIkut+`%;"></div>
                    </div>
                    <div class="text-center mt-3 fw-bold text-primary fs-5 border-top pt-3">
                        <%= Common.getBahasaConfig("Total") %> `+stat.jmlPeserta+` <%= Common.getBahasaConfig("Peserta") %>
                    </div>
                </div>
            </div>

            <div class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-4">
                    <h6 class="fw-bold text-secondary text-uppercase mb-3" style="font-size: 0.8rem;">
                        <i class="fas fa-tasks me-2"></i><%= Common.getBahasaConfig("Penyelesaian Butir Soal") %>
                    </h6>
                    <div class="d-flex justify-content-between mb-2 small fw-bold text-dark">
                        <span><i class="fas fa-edit text-primary me-1"></i><%= Common.getBahasaConfig("Terjawab:") %> `+stat.soalTerjawab+`</span>
                        <span><i class="fas fa-minus-circle text-warning me-1"></i><%= Common.getBahasaConfig("Kosong:") %> `+stat.soalKosong+`</span>
                    </div>
                    <div class="progress rounded-pill shadow-sm mb-3" style="height: 12px;">
                        <div class="progress-bar bg-primary progress-bar-striped" style="width: `+stat.persenTerjawab+`%;"></div>
                        <div class="progress-bar bg-light border" style="width: `+stat.persenKosong+`%;"></div>
                    </div>
                    <div class="text-center mt-3 fw-bold text-primary fs-5 border-top pt-3">
                        <%= Common.getBahasaConfig("Total") %> `+stat.totalSoalBeredar+` <%= Common.getBahasaConfig("Soal") %>
                    </div>
                </div>
            </div>
        `;
        wadah.innerHTML = html;
    }

    function simpanNilaiManual_<%=random%>(idHasil, nilaiVal) {
        const formData = new URLSearchParams();
        formData.append('action', 'save_manual');
        formData.append('idHasil', idHasil);
        formData.append('nilai', nilaiVal);
        formData.append('isObe', 'false');
        formData.append('ppu', '<%=ppuParam%>');

        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_lihat_hasil_ujian_services', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
            if(data.status === 'success') {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Nilai manual berhasil diperbarui secara permanen.") %>', 'bg-success');
            } else {
                alert('<%= Common.getBahasaConfigJS("Sistem gagal merekam perubahan nilai: ") %>' + data.message);
            }
        });
    }

    function simpanNilaiManualObe_<%=random%>(idHasil, btnElement) {
        const inputs = document.querySelectorAll('.input-obe-' + idHasil);
        let obeData = {};
        inputs.forEach(inp => {
            obeData[inp.getAttribute('data-fnid')] = parseFloat(inp.value) || 0;
        });
        
        const btnAsli = btnElement.innerHTML;
        btnElement.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        btnElement.disabled = true;

        const formData = new URLSearchParams();
        formData.append('action', 'save_manual');
        formData.append('idHasil', idHasil);
        formData.append('isObe', 'true');
        formData.append('obeData', JSON.stringify(obeData));
        formData.append('ppu', '<%=ppuParam%>');

        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_lihat_hasil_ujian_services', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
            if(data.status === 'success') {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Skor berbasis Sub-CPMK berhasil disimpan.") %>', 'bg-success');
                muatDataHasilUjian_<%=random%>(false);
            } else {
                alert('<%= Common.getBahasaConfigJS("Sistem gagal merekam perolehan OBE: ") %>' + data.message);
                btnElement.innerHTML = btnAsli;
                btnElement.disabled = false;
            }
        });
    }

    function hitungUlangNilai_<%=random%>(event, idHasil) {
        const btn = event.currentTarget;
        const originalHtml = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        btn.disabled = true;

        const formData = new URLSearchParams();
        formData.append('idHasil', idHasil);

        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_hitung_ulang_ujian_services', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
            btn.innerHTML = originalHtml;
            btn.disabled = false;
            
            if(data.status === 'success') {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Kalkulasi ulang nilai peserta berhasil dijalankan.") %>', 'bg-success');
                muatDataHasilUjian_<%=random%>(false);
            } else if(data.status === 'warning') {
                alert('<%= Common.getBahasaConfigJS("Peringatan Sistem: ") %>' + data.message);
            } else {
                alert('<%= Common.getBahasaConfigJS("Kegagalan Proses: ") %>' + data.message);
            }
        })
        .catch(err => {
            console.error(err);
            btn.innerHTML = originalHtml;
            btn.disabled = false;
            alert('<%= Common.getBahasaConfigJS("Sistem gagal menghubungi peladen kalkulasi utama.") %>');
        });
    }

    function hitungUlangSemua_<%=random%>() {
        showConfirmModal('<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin menghitung ulang nilai SELURUH peserta ujian secara massal? Proses ini akan memakan waktu beberapa saat tergantung pada jumlah peserta.") %>', function() {
            
            const btn = document.getElementById('btnHitungUlangSemua_<%=random%>');
            const originalHtml = btn.innerHTML;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%= Common.getBahasaConfig("Mengkalkulasi Massal...") %>';
            btn.disabled = true;

            const formData = new URLSearchParams();
            formData.append('ppu', '<%=ppuParam%>');

            fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_hitung_ulang_semua_ujian_services', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData.toString()
            })
            .then(res => res.json())
            .then(data => {
                btn.innerHTML = originalHtml;
                btn.disabled = false;
                
                if(data.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Kalkulasi massal berhasil diselesaikan.") %>', 'bg-success');
                    muatDataHasilUjian_<%=random%>(false);
                } else {
                    alert('<%= Common.getBahasaConfigJS("Terdapat hambatan teknis: ") %>' + data.message);
                }
            })
            .catch(err => {
                console.error(err);
                btn.innerHTML = originalHtml;
                btn.disabled = false;
                alert('<%= Common.getBahasaConfigJS("Terjadi pemutusan koneksi saat melakukan kalkulasi massal.") %>');
            });
        });
    }

    function analisisButirSoal_<%=random%>() {
        const modalId = 'modalAnalisis_<%=random%>';
        const bodyId = 'bodyAnalisis_<%=random%>';

        if (!document.getElementById(modalId)) {
            const modalHtml = `
            <div class="modal fade" id="`+modalId+`" tabindex="-1" aria-hidden="true" style="z-index: 9999;">
                <div class="modal-dialog modal-fullscreen modal-dialog-scrollable">
                    <div class="modal-content border-0">
                        <div class="modal-header bg-dark bg-gradient text-white py-3">
                            <h5 class="modal-title fw-bold"><i class="fas fa-chart-line me-2 text-info"></i> <%= Common.getBahasaConfig("Laporan Analisis Butir Soal Evaluasi") %></h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body bg-light p-4" id="`+bodyId+`"></div>
                        <div class="modal-footer bg-white border-top shadow-sm">
                            <button type="button" class="btn btn-outline-secondary fw-bold px-4 rounded-pill" data-bs-dismiss="modal"><i class="fas fa-times-circle me-1"></i> <%= Common.getBahasaConfig("Tutup Layar") %></button>
                            <button type="button" class="btn btn-primary fw-bold px-4 rounded-pill shadow-sm" onclick="cetakAnalisisPdf_<%=random%>('bodyAnalisis_<%=random%>')"><i class="fas fa-print me-1"></i> <%= Common.getBahasaConfig("Cetak Dokumen (PDF)") %></button>
                            <button type="button" class="btn btn-success fw-bold px-4 rounded-pill shadow-sm" id="btnDownloadExcelAnalisis_<%=random%>" onclick="downloadAnalisisExcel_<%=random%>('_analisis_butir_soal_services', 'btnDownloadExcelAnalisis_<%=random%>')"><i class="fas fa-file-excel me-1"></i> <%= Common.getBahasaConfig("Unduh Berkas Excel") %></button>
                        </div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }

        const modalInstance = new bootstrap.Modal(document.getElementById(modalId));
        modalInstance.show();
        document.getElementById(bodyId).innerHTML = '<div class="text-center py-5 mt-5"><div class="spinner-border text-primary mb-3" style="width: 3.5rem; height: 3.5rem;"></div><h4 class="fw-bold text-dark"><%= Common.getBahasaConfig("Membangun Tabel Analisis Kualitas Soal...") %></h4><p class="text-muted"><%= Common.getBahasaConfig("Mohon bersabar, proses pemetaan jawaban peserta sedang berlangsung.") %></p></div>';

        const formData = new URLSearchParams();
        formData.append('ppu', '<%=ppuParam%>');
        formData.append('action', 'view');
        
        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_analisis_butir_soal_services', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        }).then(res => res.json()).then(data => {
            if(data.status === 'success') { document.getElementById(bodyId).innerHTML = data.html; } 
            else { document.getElementById(bodyId).innerHTML = '<div class="alert alert-danger shadow-sm fw-bold fs-5 text-center mt-5"><i class="fas fa-exclamation-triangle d-block fs-1 mb-3"></i>' + data.message + '</div>'; }
        });
    }

    function analisisButirSoalObe_<%=random%>() {
        const modalId = 'modalAnalisisObe_<%=random%>';
        const bodyId = 'bodyAnalisisObe_<%=random%>';

        if (!document.getElementById(modalId)) {
            const modalHtml = `
            <div class="modal fade" id="`+modalId+`" tabindex="-1" aria-hidden="true" style="z-index: 9999;">
                <div class="modal-dialog modal-fullscreen modal-dialog-scrollable">
                    <div class="modal-content border-0">
                        <div class="modal-header bg-primary bg-gradient text-white py-3 shadow-sm">
                            <h5 class="modal-title fw-bold"><i class="fas fa-project-diagram me-2 text-warning"></i> <%= Common.getBahasaConfig("Analisis Pemetaan Soal Berbasis Sub-CPMK (OBE)") %></h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body bg-light p-4" id="`+bodyId+`"></div>
                        <div class="modal-footer bg-white border-top shadow-sm">
                            <button type="button" class="btn btn-outline-secondary fw-bold px-4 rounded-pill" data-bs-dismiss="modal"><i class="fas fa-times-circle me-1"></i> <%= Common.getBahasaConfig("Tutup Layar") %></button>
                            <button type="button" class="btn btn-primary fw-bold px-4 rounded-pill shadow-sm" onclick="cetakAnalisisPdf_<%=random%>('bodyAnalisisObe_<%=random%>')"><i class="fas fa-print me-1"></i> <%= Common.getBahasaConfig("Cetak Dokumen (PDF)") %></button>
                            <button type="button" class="btn btn-success fw-bold px-4 rounded-pill shadow-sm" id="btnDownloadExcelAnalisisObe_<%=random%>" onclick="downloadAnalisisExcel_<%=random%>('_analisis_soal_obe_services', 'btnDownloadExcelAnalisisObe_<%=random%>')"><i class="fas fa-file-excel me-1"></i> <%= Common.getBahasaConfig("Unduh Berkas Excel") %></button>
                        </div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }

        const modalInstance = new bootstrap.Modal(document.getElementById(modalId));
        modalInstance.show();
        document.getElementById(bodyId).innerHTML = '<div class="text-center py-5 mt-5"><div class="spinner-border text-primary mb-3" style="width: 3.5rem; height: 3.5rem;"></div><h4 class="fw-bold text-dark"><%= Common.getBahasaConfig("Menyelaraskan Matriks Soal Terhadap Capaian Pembelajaran...") %></h4><p class="text-muted"><%= Common.getBahasaConfig("Mohon tunggu, proses kalkulasi OBE sedang berjalan.") %></p></div>';

        const formData = new URLSearchParams();
        formData.append('ppu', '<%=ppuParam%>');
        formData.append('action', 'view');
        
        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_analisis_soal_obe_services', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        }).then(res => res.json()).then(data => {
            if(data.status === 'success') { document.getElementById(bodyId).innerHTML = data.html; } 
            else { document.getElementById(bodyId).innerHTML = '<div class="alert alert-danger shadow-sm fw-bold fs-5 text-center mt-5"><i class="fas fa-exclamation-triangle d-block fs-1 mb-3"></i>' + data.message + '</div>'; }
        });
    }

    function downloadAnalisisExcel_<%=random%>(serviceName, btnId) {
        const btn = document.getElementById(btnId);
        const originalText = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%= Common.getBahasaConfig("Mempersiapkan Berkas...") %>';
        btn.disabled = true;
        
        const formData = new URLSearchParams();
        formData.append('ppu', '<%=ppuParam%>');
        formData.append('action', 'excel');
        
        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=' + serviceName, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        }).then(res => res.json()).then(data => {
            btn.innerHTML = originalText;
            btn.disabled = false;
            if(data.status === 'success') {
                window.location.href = data.url; 
            } else { alert('<%= Common.getBahasaConfigJS("Sistem gagal menghasilkan dokumen Excel: ") %>' + data.message); }
        }).catch(err => {
            btn.innerHTML = originalText;
            btn.disabled = false;
            alert('<%= Common.getBahasaConfigJS("Koneksi server terganggu saat mengunduh dokumen Excel.") %>');
        });
    }

    function cetakAnalisisPdf_<%=random%>(bodyId) {
        const printContent = document.getElementById(bodyId).innerHTML;
        const win = window.open('', '', 'width=1200,height=800');
        win.document.write('<html><head><title><%= Common.getBahasaConfig("Pencetakan Laporan Evaluasi") %></title>');
        win.document.write('<style>body { font-family: "Segoe UI", Roboto, "Helvetica Neue", sans-serif; padding: 20px; } table { width: 100%; border-collapse: collapse; font-size: 11px; text-align: center; } th, td { border: 1px solid #dee2e6; padding: 6px; } .table-dark { background-color: #343a40; color: white; font-weight: bold; text-transform: uppercase; } .table-secondary { background-color: #e9ecef; font-weight: bold; } h3 { text-align: center; font-size: 18px; margin-bottom: 25px; text-transform: uppercase; letter-spacing: 1px; color: #212529; } .bg-success { background-color: #d1e7dd !important; } .bg-danger { background-color: #f8d7da !important; }</style>');
        win.document.write('</head><body>');
        win.document.write('<h3><%= Common.getBahasaConfig("Dokumen Resmi Laporan Analisis Butir Soal Kelas") %></h3>');
        win.document.write(printContent);
        win.document.write('</body></html>');
        win.document.close();
        setTimeout(() => { win.print(); }, 1200);
    }

    function lihatDetailKoreksi_<%=random%>(idHasilUjian) {
        var cm = 'cm_' + (++variable);
        loadModalContentCustomSimpan(
            '<%= Common.getBahasaConfigJS("Formulir Koreksi Lembar Jawaban Peserta") %>', 
            '<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_koreksi_ujian&idHasil=' + idHasilUjian + '&contentModal=' + cm, 
            '95%', 'hidden', '', cm
        );
    }

    function downloadHasilExcel_<%=random%>() {
        if(typeof tampilkanToast === 'function') tampilkanToast('<%= Common.getBahasaConfigJS("Sedang menyusun dan merangkum rekapitulasi nilai ke format Excel...") %>', 'bg-info');
        
        const formData = new URLSearchParams();
        formData.append('ppu', '<%=ppuParam%>');
        
        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_export_ujian_ke_excel_services', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
            if(data.status === 'success') {
                window.location.href = data.url; 
            } else {
                alert('<%= Common.getBahasaConfigJS("Kendala Teknis: ") %>' + data.message);
            }
        })
        .catch(err => {
            console.error(err);
            alert('<%= Common.getBahasaConfigJS("Terjadi kesalahan jaringan pada saat merakit berkas Excel.") %>');
        });
    }

    setTimeout(function() { muatDataHasilUjian_<%=random%>(false); }, 300);
</script>