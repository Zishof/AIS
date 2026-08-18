<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.FormatNilai"%>
<%@page import="ais.common.Common"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.json.JSONObject"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
String ppuParam = request.getParameter("ppu");
String random = Common.getGeneratedBarCode(7);
PertemuanPunyaUjian ppu = null;
Session mySession = null;

try {
    mySession = HibernateUtil.openSession();
    ppu = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, Long.parseLong(ppuParam));
    if (ppu == null) return;

    Pertemuan pertemuan = ppu.getPertemuan();
    boolean isObe = false;
    
    // Ekstraksi ID Perkuliahan untuk dikirim ke fungsi Cetak Laporan PDF nanti
    Long idPerkuliahan = null;
    
    if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
        idPerkuliahan = pertemuan.getPerkuliahan().getId();
        if (pertemuan.getPerkuliahan().getKurikulum() != null && 
            pertemuan.getPerkuliahan().getKurikulum().apakahObe(
                pertemuan.getPerkuliahan().getTahunAjaran(), 
                pertemuan.getPerkuliahan().getGanjilGenap())) {
            isObe = true;
        }
    }
%>

<div class="container-fluid p-3">
    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-header bg-light fw-bold">
            <i class="fas fa-cog me-2 text-primary"></i><%= Common.getBahasaConfig("Konfigurasi Bobot & Sinkronisasi") %>
        </div>
        <div class="card-body">
            
            <% if (pertemuan != null && pertemuan.getPerkuliahan() != null) { 
                List<FormatNilai> listFormat = Common.getFormatNilais(mySession, pertemuan.getPerkuliahan());
                boolean isLocked = pertemuan.getPerkuliahan().getDikunci() != null;
                
                if (isObe) { 
                    boolean adaCpmk = false;
                    for (FormatNilai fn : listFormat) if (fn.getNama() != null && fn.getNama().toLowerCase().contains("cpmk")) { adaCpmk = true; break; }
                    if (!adaCpmk) listFormat = Common.getFormatNilais(pertemuan.getPerkuliahan(), true);
                    
                    JSONObject jsonObj = new JSONObject(ppu.getFormatNilais() != null ? ppu.getFormatNilais() : "{}");
            %>
                <div class="alert alert-info small shadow-sm">
                    <i class="fas fa-info-circle me-2"></i><%= Common.getBahasaConfig("Pemetaan nilai berdasarkan Sub-CPMK. Masukkan nomor soal (contoh: 1,2,5) dan bobot masing-masing.") %>
                </div>

                <div class="table-responsive">
                    <table class="table table-bordered align-middle">
                        <thead class="table-dark small text-center">
                            <tr>
                                <th><%= Common.getBahasaConfig("Sub-CPMK") %></th>
                                <th><%= Common.getBahasaConfig("Nomor Soal") %></th>
                                <th><%= Common.getBahasaConfig("Bobot") %></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (FormatNilai fn : listFormat) { 
                                if (fn.getStatusPertemuan() == null) continue;
                                boolean checked = !jsonObj.isNull(fn.getId().toString());
                                String valSoal = checked ? jsonObj.getString(fn.getId().toString()) : "";
                                double valBobot = !jsonObj.isNull(fn.getId().toString() + "_bobot") ? jsonObj.getDouble(fn.getId().toString() + "_bobot") : 100.0;
                            %>
                            <tr>
                                <td width="40%">
                                    <div class="form-check">
                                        <input class="form-check-input chk-obe-<%=random%>" type="checkbox" value="<%= fn.getId() %>" id="chk_<%=fn.getId()%>" <%= checked ? "checked" : "" %> <%= isLocked ? "disabled" : "" %>>
                                        <label class="form-check-label fw-bold" for="chk_<%=fn.getId()%>">
                                            <%= fn.getNama() %> (<%= Common.numberFormat.get().format(fn.getPersen()) %>%)
                                        </label>
                                    </div>
                                </td>
                                <td width="40%">
                                    <input type="text" class="form-control form-control-sm input-soal-<%=random%>" data-id="<%=fn.getId()%>" placeholder="e.g. 1,2,3" value="<%= valSoal %>" <%= !checked || isLocked ? "disabled" : "" %>>
                                </td>
                                <td width="20%">
                                    <input type="number" class="form-control form-control-sm input-bobot-<%=random%>" data-id="<%=fn.getId()%>" value="<%= valBobot %>" <%= !checked || isLocked ? "disabled" : "" %>>
                                </td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>

            <% } else { %>
                <div class="row g-3 align-items-center">
                    <div class="col-md-6">
                        <label class="form-label fw-bold small"><%= Common.getBahasaConfig("Kategori Format Nilai") %></label>
                        <select class="form-select shadow-sm" id="selectFormat_<%=random%>" <%= isLocked ? "disabled" : "" %>>
                            <option value=""><%= Common.getBahasaConfig("Tidak Ada") %></option>
                            <% for (FormatNilai fn : listFormat) { 
                                if (fn.getStatusPertemuan() == null) continue;
                                boolean isSelected = ppu.getFormatNilai() != null && ppu.getFormatNilai().getId().equals(fn.getId());
                            %>
                            <option value="<%= fn.getId() %>" <%= isSelected ? "selected" : "" %>>
                                <%= fn.getNama() %> (<%= Common.numberFormat.get().format(fn.getPersen()) %>%)
                            </option>
                            <% } %>
                        </select>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label fw-bold small"><%= Common.getBahasaConfig("Bobot (%)") %></label>
                        <input type="number" class="form-control shadow-sm" id="inputProsentase_<%=random%>" value="<%= ppu.getProsentase() %>" <%= isLocked ? "disabled" : "" %>>
                    </div>
                </div>
                <% if (isLocked) { %>
                    <div class="text-danger small mt-2 fw-bold"><i class="fas fa-lock me-1"></i><%= Common.getBahasaConfig("Penilaian sudah dikunci.") %></div>
                <% } %>
            <% } 

            } else { %>
                <div class="row">
                    <div class="col-md-6">
                        <label class="form-label fw-bold small"><%= Common.getBahasaConfig("Bobot Penilaian") %></label>
                        <input type="number" class="form-control shadow-sm" id="inputProsentase_<%=random%>" value="<%= ppu.getProsentase() %>">
                    </div>
                </div>
            <% } %>

            <hr>
            <div class="d-flex justify-content-between gap-2 mt-4">
                <button type="button" class="btn btn-primary px-4 fw-bold shadow-sm" onclick="simpanFormatNilai_<%=random%>()">
                    <i class="fas fa-save me-2"></i><%= Common.getBahasaConfig("Simpan Perubahan") %>
                </button>
                
                <% if (pertemuan != null && pertemuan.getPerkuliahan() != null) { %>
                <button type="button" class="btn btn-success px-4 fw-bold shadow-sm" id="btnSync_<%=random%>" onclick="sinkronkanNilai_<%=random%>()">
                    <i class="fas fa-sync-alt me-2"></i><%= Common.getBahasaConfig("Sinkronkan Nilai ke KRS") %>
                </button>
                <% } %>
            </div>
        </div>
    </div>
</div>

<script>
    document.querySelectorAll('.chk-obe-<%=random%>').forEach(chk => {
        chk.addEventListener('change', function() {
            const row = this.closest('tr');
            row.querySelector('.input-soal-<%=random%>').disabled = !this.checked;
            row.querySelector('.input-bobot-<%=random%>').disabled = !this.checked;
        });
    });

    async function simpanFormatNilai_<%=random%>() {
        const formData = new URLSearchParams();
        formData.append('ppu', '<%= ppuParam %>');
        formData.append('action', 'save');
        formData.append('isObe', '<%= isObe %>');

        <% if (isObe) { %>
            let obeData = {};
            document.querySelectorAll('.chk-obe-<%=random%>:checked').forEach(chk => {
                const id = chk.value;
                const row = chk.closest('tr');
                const soal = row.querySelector('.input-soal-<%=random%>').value;
                const bobot = row.querySelector('.input-bobot-<%=random%>').value;
                obeData[id] = soal;
                obeData[id + "_bobot"] = parseFloat(bobot);
            });
            formData.append('formatNilaisJson', JSON.stringify(obeData));
        <% } else { %>
            const selectEl = document.getElementById('selectFormat_<%=random%>');
            formData.append('formatNilaiId', selectEl ? selectEl.value : '');
            formData.append('prosentase', document.getElementById('inputProsentase_<%=random%>').value);
        <% } %>

        try {
            const res = await fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_format_nilai_services', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData.toString()
            });
            const json = await res.json();
            if(json.status === 'success') {
                tampilkanToast(json.message, 'bg-success');
            } else {
                alert(json.message);
            }
        } catch(e) { alert('Error: ' + e); }
    }

    // =================================================================================
    // FUNGSI PEMANGGIL LAPORAN PDF
    // =================================================================================
    function cetakLaporanNilai_<%=random%>(idPerkuliahan) {
        if (typeof tampilkanToast === 'function') {
            tampilkanToast('<%= Common.getBahasaConfigJS("Sedang memproses dan menyusun laporan PDF. Harap tunggu...") %>', 'bg-info');
        }

        const formData = new URLSearchParams();
        formData.append('perkuliahanId', idPerkuliahan);

        fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_cetak_laporan_nilai', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(response => {
            if (!response.ok) throw new Error('Koneksi server gagal.');
            return response.json();
        })
        .then(data => {
            if (data.status === 'success') {
                tampilkanPdfDiPopup_<%=random%>(data.url);
            } else {
                alert('<%= Common.getBahasaConfigJS("Gagal mencetak: ") %>' + data.message);
            }
        })
        .catch(error => {
            console.error(error);
            alert('<%= Common.getBahasaConfigJS("Terjadi kesalahan koneksi saat memproses PDF.") %>');
        });
    }

    function tampilkanPdfDiPopup_<%=random%>(pdfUrl) {
        // Cek apakah modal view PDF sudah ada di DOM, jika belum buat elemennya secara dinamis
        if (!document.getElementById('modalPdfViewer_<%=random%>')) {
            const modalHtml = `
            <div class="modal fade" id="modalPdfViewer_<%=random%>" tabindex="-1" aria-hidden="true" style="z-index: 1060;">
                <div class="modal-dialog modal-xl modal-dialog-centered" style="height: 95vh;">
                    <div class="modal-content h-100 border-0 shadow-lg">
                        <div class="modal-header bg-dark text-white py-2">
                            <h6 class="modal-title fw-bold"><i class="fas fa-file-pdf text-danger me-2"></i> <%= Common.getBahasaConfig("Pratinjau Dokumen Laporan") %></h6>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body p-0 bg-secondary">
                            <iframe id="iframePdfViewer_<%=random%>" src="" style="width: 100%; height: 100%; border: none;"></iframe>
                        </div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }

        // Pasang URL PDF yang baru di-generate ke dalam iframe
        document.getElementById('iframePdfViewer_<%=random%>').src = pdfUrl;
        
        // Tampilkan Modalnya
        const modalInstance = new bootstrap.Modal(document.getElementById('modalPdfViewer_<%=random%>'));
        modalInstance.show();
    }


    // =================================================================================
    // FUNGSI SINKRONISASI NILAI UTAMA
    // =================================================================================
    function sinkronkanNilai_<%=random%>() {
        showConfirmModal('<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin menyinkronkan nilai ujian ini ke rekapitulasi nilai mahasiswa? Setelah sinkronisasi berhasil, laporan PDF akan langsung ditampilkan.") %>', async function() {
            const btn = document.getElementById('btnSync_<%=random%>');
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%= Common.getBahasaConfig("Memproses...") %>';

            try {
                const res = await fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_format_nilai_services&action=sync&ppu=<%=ppuParam%>');
                const json = await res.json();
                
                tampilkanToast(json.message, json.status === 'success' ? 'bg-success' : 'bg-danger');

                // Jika sinkronisasi ke KRS sukses, panggil fungsi pembuatan laporan PDF secara otomatis!
                if (json.status === 'success') {
                    <% if (idPerkuliahan != null) { %>
                        cetakLaporanNilai_<%=random%>('<%= idPerkuliahan %>');
                    <% } %>
                }

            } catch(e) { 
                alert('Error: ' + e); 
            } finally {
                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-sync-alt me-2"></i><%= Common.getBahasaConfig("Sinkronkan Nilai ke KRS") %>';
            }
        });
    }
</script>

<%
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_format_nilai.jsp:293");
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}
%>