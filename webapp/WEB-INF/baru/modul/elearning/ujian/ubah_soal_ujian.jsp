<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.BankSoal"%>
<%@page import="ais.database.model.BankSoalDetail"%>
<%@page import="ais.database.model.UjianPunyaSoal"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.PenjelasanBankSoal"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
// 1. PENGECEKAN KEAMANAN (SESI PENGGUNA)
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    return; 
}

String random = Common.getGeneratedBarCode(7);
String ppuParam = request.getParameter("ppu");
String idSoalParam = request.getParameter("idSoal");
String afterSave = request.getParameter("afterSave") == null ? "" : request.getParameter("afterSave");

if (ppuParam == null || ppuParam.trim().isEmpty()) {
    out.println("<div class='alert alert-danger text-center mt-4 shadow-sm fw-bold'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Parameter sesi ujian tidak valid atau tidak ditemukan di dalam sistem.") + "</div>");
    return;
}

// 2. PERSIAPAN VARIABEL DAN PENGAMBILAN DATA
PertemuanPunyaUjian pertemuanPunyaUjian = null;
UjianPunyaSoal ujianPunyaSoal = null;
BankSoal bankSoal = null;
List<BankSoalDetail> listOpsiJawaban = new ArrayList<BankSoalDetail>();
Session mySession = null;
boolean isDataError = false;

try {
    mySession = HibernateUtil.openSession();
    Long idPertemuan = Long.parseLong(ppuParam.trim());
    pertemuanPunyaUjian = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, idPertemuan);

    // Mode Edit: Jika ID Soal tersedia dan valid
    if (idSoalParam != null && !idSoalParam.trim().isEmpty()) {
        Long idSoal = Long.parseLong(idSoalParam.trim());
        ujianPunyaSoal = (UjianPunyaSoal) mySession.get(UjianPunyaSoal.class, idSoal);
        bankSoal = ujianPunyaSoal != null ? ujianPunyaSoal.getBankSoal() : null;

        // Mencegah LazyInitializationException dengan mengambil opsi secara eksplisit
        if (bankSoal != null && pertemuanPunyaUjian != null && pertemuanPunyaUjian.getUjian() != null && 
            PenjelasanBankSoal.KOREKSI_OTOMATIS.equals(pertemuanPunyaUjian.getUjian().getJenisKoreksi())) {
            
            List<Long> detailIds = bankSoal.ambilBankSoalDetail(false);
            if (detailIds != null) {
                for (Long idOpsi : detailIds) {
                    BankSoalDetail bsd = (BankSoalDetail) mySession.get(BankSoalDetail.class, idOpsi);
                    if (bsd != null) listOpsiJawaban.add(bsd);
                }
            }
        }
    }
} catch (NumberFormatException e) {
    isDataError = true;
} catch (Exception e) {
    isDataError = true;
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/ubah_soal_ujian.jsp:69");
} finally {
    if (mySession != null) {
        try {
            if (mySession.isOpen()) {
                try { mySession.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/ubah_soal_ujian.jsp:74");}
            try { mySession.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/ubah_soal_ujian.jsp:75");}
            mySession.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/ubah_soal_ujian.jsp:79");
        }
    }
}

if (isDataError || pertemuanPunyaUjian == null || pertemuanPunyaUjian.getUjian() == null) {
    out.println("<div class='alert alert-danger text-center mt-4 shadow-sm fw-bold'><i class='fas fa-times-circle me-2'></i>" + Common.getBahasaConfig("Data informasi ujian tidak ditemukan atau terjadi kesalahan dalam pengambilan data.") + "</div>");
    return;
}

// 4. EKSTRAKSI INFORMASI UJIAN
String jenisKoreksi = pertemuanPunyaUjian.getUjian().getJenisKoreksi() != null ? pertemuanPunyaUjian.getUjian().getJenisKoreksi() : PenjelasanBankSoal.KOREKSI_OTOMATIS;
String labelJenisKoreksi = PenjelasanBankSoal.KOREKSI_MANUAL.equals(jenisKoreksi) ?
        Common.getBahasaConfig("Esai / Uraian Bebas (Koreksi Manual)") : 
        Common.getBahasaConfig("Pilihan Ganda (Koreksi Otomatis)");
        
String namaUjian = pertemuanPunyaUjian.getUjian().getNama() != null ? pertemuanPunyaUjian.getUjian().getNama() : "-";
String keterangan = pertemuanPunyaUjian.getKeterangan() != null && !pertemuanPunyaUjian.getKeterangan().trim().isEmpty() ?
        pertemuanPunyaUjian.getKeterangan() : Common.getBahasaConfig("Tidak ada deskripsi atau keterangan tambahan untuk ujian ini.");
%>

<style>
    /* Merapikan jarak komponen upload agar pas di dalam baris opsi */
    .upload-wrapper-opsi .form-group { margin-bottom: 0 !important; }
    .upload-wrapper-opsi .input-group { margin-bottom: 0 !important; }
</style>

<div class="container-fluid py-4">
    
    <div class="card mb-4 border-0 shadow-sm bg-white rounded-4 overflow-hidden">
        <div class="bg-primary bg-gradient text-white p-3 d-flex align-items-center">
            <i class="fas fa-info-circle fs-4 me-3"></i>
            <h5 class="fw-bold mb-0"><%= Common.getBahasaConfig("Informasi Rincian Ujian") %></h5>
        </div>
        <div class="card-body p-4">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <small class="text-muted text-uppercase fw-bold"><i class="fas fa-book-open me-2"></i><%= Common.getBahasaConfig("Mata Ujian / Tugas") %></small>
                    <h4 class="fw-bold text-dark mt-2 mb-2"><%= namaUjian %></h4>
                    <p class="text-secondary mb-0" style="font-size: 0.95rem; line-height: 1.5;"><%= keterangan %></p>
                </div>
                <div class="col-md-4 text-md-end mt-4 mt-md-0">
                    <div class="d-inline-flex flex-column text-start bg-light p-3 rounded-3 border border-secondary-subtle shadow-sm">
                        <small class="text-uppercase fw-bold text-muted mb-1" style="font-size: 0.75rem;">
                            <i class="fas fa-tasks me-1 text-primary"></i><%= Common.getBahasaConfig("Jenis Evaluasi") %>
                        </small>
                        <span class="fw-bold text-dark fs-6"><%= labelJenisKoreksi %></span>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="card shadow-sm border-0 rounded-4" id="cardFormSoal<%=random%>">
        <div class="card-header bg-white border-bottom pt-4 pb-3 d-flex justify-content-between align-items-center">
            <h5 class="fw-bold text-dark mb-0">
                <i class="<%= bankSoal != null ? "fas fa-edit" : "fas fa-plus-circle" %> me-2 text-primary"></i>
                <%= bankSoal != null ? Common.getBahasaConfig("Formulir Perubahan Data Soal") : Common.getBahasaConfig("Formulir Pembuatan Soal Baru") %>
            </h5>
            <% if(bankSoal != null) { %>
                <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2 rounded-pill shadow-sm">
                    <i class="fas fa-tag me-1"></i> <%= Common.getBahasaConfig("Nomor Identitas Soal:") %> <%= ujianPunyaSoal.getId() %>
                </span>
            <% } %>
        </div>
        
        <div class="card-body px-4 py-4 bg-white">
            <form id="formSoal<%=random%>">
                <input type="hidden" id="idSoal<%=random%>" name="idSoal" value="<%= ujianPunyaSoal != null ? ujianPunyaSoal.getId() : "" %>">
                <input type="hidden" id="jenisKoreksi<%=random%>" name="jenisKoreksi" value="<%= jenisKoreksi %>">
                
                <div class="mb-5">
                    <label class="form-label fw-bold text-dark fs-6 mb-3">
                        <i class="fas fa-question-circle text-primary me-2"></i><%= Common.getBahasaConfig("Uraian Pertanyaan Soal") %> <span class="text-danger">*</span>
                    </label>
                    <%
                    String placeholderUjian = Common.getBahasaConfig("Tuliskan uraian pertanyaan secara jelas dan terperinci di sini...");
                    String jenis = "teksSoal_" + random;
                    String jawab = bankSoal != null ? bankSoal.getSoal() : "";
                    String idteksSoal = "teksSoal" + random;
                    %>
                    <div class="border rounded-3 shadow-sm p-1">
                        <jsp:include page="/WEB-INF/baru/modul/common/text_area.jsp">
                            <jsp:param name="placeholder" value="<%= placeholderUjian %>" />
                            <jsp:param name="idtextarea" value="<%=idteksSoal %>" />
                            <jsp:param name="jenis" value="<%= jenis %>" />
                            <jsp:param name="isi" value="<%= jawab %>" />
                            <jsp:param name="namatextarea" value="<%=idteksSoal %>" />
                        </jsp:include>
                    </div>
                </div>

                <div id="areaPilihanGanda<%=random%>" class="p-4 bg-light rounded-4 border border-secondary-subtle <%= PenjelasanBankSoal.KOREKSI_OTOMATIS.equals(jenisKoreksi) ? "" : "d-none" %>">
                    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 border-bottom pb-3">
                        <div class="mb-3 mb-md-0">
                            <h6 class="fw-bold text-primary mb-2 fs-5"><i class="fas fa-list-ul me-2"></i><%= Common.getBahasaConfig("Daftar Opsi Jawaban") %></h6>
                            <small class="text-muted"><%= Common.getBahasaConfig("Tuliskan opsi jawaban pada kolom yang tersedia. Tandai salah satu opsi yang paling benar sebagai kunci jawaban utama.") %></small>
                        </div>
                        <button type="button" class="btn btn-sm btn-primary fw-bold rounded-pill px-4 py-2 shadow-sm d-flex align-items-center" onclick="tambahOpsiJawaban<%=random%>()">
                            <i class="fas fa-plus me-2"></i> <%= Common.getBahasaConfig("Tambahkan Opsi Baru") %>
                        </button>
                    </div>
                    
                    <div id="wadahOpsi<%=random%>" class="d-flex flex-column">
                        <%
                        int existingOptionsCount = listOpsiJawaban.size();
                        for (int i = 0; i < existingOptionsCount; i++) {
                            BankSoalDetail bsd = listOpsiJawaban.get(i);
                            String idOpsi = "opsi_" + (i + 1);
                            boolean isBenar = bsd.getBetul() != null && bsd.getBetul();
                            String isiOpsi = bsd.getJawaban() != null ? bsd.getJawaban().replaceAll("<[^>]*>", "").trim() : "";
                            
                            // Ambil Huruf atau kalkulasi otomatis
                            String huruf = bsd.getHuruf() != null ? bsd.getHuruf() : String.valueOf((char)(65 + i));
                        %>
                        <div class="d-flex align-items-start shadow-sm bg-white border border-secondary-subtle rounded-3 p-3 mb-3 baris-opsi" id="<%=idOpsi%>" data-huruf="<%=huruf%>" style="transition: all 0.3s ease;">
                            
                            <div class="d-flex flex-column align-items-center justify-content-center pe-3 me-3 border-end h-100" style="min-height: 80px; min-width: 70px;">
                                <div class="bg-primary text-white fw-bold rounded-circle d-flex align-items-center justify-content-center shadow-sm bulatan-huruf mb-2" style="width: 32px; height: 32px; font-size: 1.1rem;">
                                    <%= huruf %>
                                </div>
                                <div class="d-flex flex-column align-items-center" title="<%= Common.getBahasaConfig("Tandai sebagai kunci jawaban yang benar") %>">
                                    <span class="text-muted fw-bold mb-1" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Kunci") %></span>
                                    <input class="form-check-input mt-0 radio-kunci-jawaban shadow-sm" type="radio" name="kunciJawabanUtama<%=random%>" value="<%=idOpsi%>" <%= isBenar ? "checked" : "" %> style="cursor: pointer; transform: scale(1.3);">
                                </div>
                            </div>
                            
                            <div class="flex-grow-1 pe-3 d-flex flex-column justify-content-center">
                                <input type="hidden" name="hurufOpsi[]" class="input-huruf-opsi" value="<%= huruf %>">
                                <textarea class="form-control input-opsi-textarea bg-light shadow-sm mb-2" rows="2" style="resize: none; border-radius: 8px;" placeholder="<%= Common.getBahasaConfig("Ketikkan uraian jawaban di sini...") %>"><%= isiOpsi.replace("</textarea>", "&lt;/textarea&gt;") %></textarea>
                                
                                <% if (bankSoal != null && bankSoal.getId() != null) { %>
                                    <div class="w-100 mt-1 upload-wrapper-opsi">
                                        <jsp:include page="/WEB-INF/baru/modul/common/upload_component.jsp">
                                            <jsp:param name="clazz" value="<%= ais.database.model.file.LampiranLain.class.getName() %>" />
                                            <jsp:param name="jenis" value="<%= \"Gambar_Jawaban_\" + huruf %>" />
                                            <jsp:param name="id" value="<%= bankSoal.getId().toString() %>" />
                                            <jsp:param name="col" value="<%= \"lampiran_\" + random + \"_\" + huruf %>" />
                                            <jsp:param name="tampilUpload" value="true" />
                                            <jsp:param name="loadPreview" value="true" />
                                            <jsp:param name="rnd" value="<%= random + \"_\" + huruf %>" />
                                            <jsp:param name="label" value="Lampiran Opsi" />
                                        </jsp:include>
                                    </div>
                                <% } else { %>
                                    <div class="text-muted small fst-italic mt-1">
                                        <i class="fas fa-info-circle me-1"></i><%= Common.getBahasaConfig("Simpan soal terlebih dahulu untuk dapat mengunggah lampiran gambar/file.") %>
                                    </div>
                                <% } %>
                            </div>
                            
                            <div class="d-flex flex-column px-2 border-start justify-content-center h-100" style="min-height: 80px;">
                                <button class="btn btn-outline-danger btn-sm fw-bold shadow-sm rounded-pill px-3 py-2" type="button" onclick="hapusOpsiJawaban<%=random%>('<%=idOpsi%>')" title="<%= Common.getBahasaConfig("Hapus opsi jawaban ini secara permanen") %>">
                                    <i class="fas fa-trash-alt"></i>
                                </button>
                            </div>
                        </div>
                        <% } %>
                    </div>
                </div>
            </form>
        </div>
        
        <div class="card-footer bg-light border-top p-4 d-flex justify-content-between align-items-center rounded-bottom-4">
            <button type="button" class="btn btn-outline-secondary px-4 py-2 rounded-pill fw-bold shadow-sm" onclick="kembaliAtauBatal<%=random%>()">
                <i class="fas fa-arrow-circle-left me-2"></i> <%= Common.getBahasaConfig("Kembali ke Sebelumnya") %>
            </button>
            <button type="button" class="btn btn-success px-5 py-2 rounded-pill fw-bold shadow" id="btnSimpanSoal<%=random%>" onclick="simpanDataSoal<%=random%>()">
                <i class="fas fa-save me-2"></i> <%= Common.getBahasaConfig("Simpan Data Soal") %>
            </button>
        </div>
    </div>
</div>

<script>
    const KOREKSI_OTOMATIS<%=random%> = '<%= PenjelasanBankSoal.KOREKSI_OTOMATIS %>';
    let hitungOpsi<%=random%> = <%= existingOptionsCount %>;

    document.addEventListener("DOMContentLoaded", function() {
        // OTOMATISASI FORM BARU (Minimal 2 Pilihan)
        <% if (bankSoal == null && PenjelasanBankSoal.KOREKSI_OTOMATIS.equals(jenisKoreksi)) { %>
            if (hitungOpsi<%=random%> === 0) {
                tambahOpsiJawaban<%=random%>();
                tambahOpsiJawaban<%=random%>();
            }
        <% } %>
    });

    // ==========================================
    // MANAJEMEN PENAMBAHAN/PENGHAPUSAN OPSI & HURUF
    // ==========================================
    function recalculateHurufOpsi<%=random%>() {
        const wadah = document.getElementById('wadahOpsi<%=random%>');
        const barisOpsis = wadah.querySelectorAll('.baris-opsi');
        let index = 0;
        
        barisOpsis.forEach(baris => {
            const hurufBaru = String.fromCharCode(65 + (index % 26)); 
            baris.setAttribute('data-huruf', hurufBaru);
            
            const circle = baris.querySelector('.bulatan-huruf');
            if (circle) circle.innerText = hurufBaru;
            
            const inputHidden = baris.querySelector('.input-huruf-opsi');
            if (inputHidden) inputHidden.value = hurufBaru;
            
            index++;
        });
        
        hitungOpsi<%=random%> = index;
    }

    function tambahOpsiJawaban<%=random%>() {
        hitungOpsi<%=random%>++;
        const idOpsi = 'opsi_' + hitungOpsi<%=random%>;
        const hurufOpsi = String.fromCharCode(65 + ((hitungOpsi<%=random%> - 1) % 26));

        const htmlOpsi = `
            <div class="d-flex align-items-start shadow-sm bg-white border border-secondary-subtle rounded-3 p-3 mb-3 baris-opsi" id="`+idOpsi+`" data-huruf="`+hurufOpsi+`" style="transition: all 0.3s ease;">
                
                <div class="d-flex flex-column align-items-center justify-content-center pe-3 me-3 border-end h-100" style="min-height: 80px; min-width: 70px;">
                    <div class="bg-primary text-white fw-bold rounded-circle d-flex align-items-center justify-content-center shadow-sm bulatan-huruf mb-2" style="width: 32px; height: 32px; font-size: 1.1rem;">
                        `+hurufOpsi+`
                    </div>
                    <div class="d-flex flex-column align-items-center" title="<%= Common.getBahasaConfig("Tandai sebagai kunci jawaban yang benar") %>">
                        <span class="text-muted fw-bold mb-1" style="font-size: 0.75rem;"><%= Common.getBahasaConfig("Kunci") %></span>
                        <input class="form-check-input mt-0 radio-kunci-jawaban shadow-sm" type="radio" name="kunciJawabanUtama<%=random%>" value="`+idOpsi+`" style="cursor: pointer; transform: scale(1.3);">
                    </div>
                </div>
                
                <div class="flex-grow-1 pe-3 d-flex flex-column justify-content-center">
                    <input type="hidden" name="hurufOpsi[]" class="input-huruf-opsi" value="`+hurufOpsi+`">
                    <textarea class="form-control input-opsi-textarea bg-light shadow-sm mb-2" rows="2" style="resize: none; border-radius: 8px;" placeholder="<%= Common.getBahasaConfig("Ketikkan uraian jawaban di sini...") %>"></textarea>
                    
                    <div class="text-muted small fst-italic mt-1 info-upload-dynamic">
                        <i class="fas fa-info-circle me-1"></i><%= Common.getBahasaConfig("Simpan soal terlebih dahulu untuk dapat mengunggah lampiran gambar/file pada opsi baru ini.") %>
                    </div>
                </div>
                
                <div class="d-flex flex-column px-2 border-start justify-content-center h-100" style="min-height: 80px;">
                    <button class="btn btn-outline-danger btn-sm fw-bold shadow-sm rounded-pill px-3 py-2" type="button" onclick="hapusOpsiJawaban<%=random%>('`+idOpsi+`')" title="<%= Common.getBahasaConfig("Hapus opsi jawaban ini secara permanen") %>">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </div>
            </div>
        `;
        
        document.getElementById('wadahOpsi<%=random%>').insertAdjacentHTML('beforeend', htmlOpsi);
        recalculateHurufOpsi<%=random%>();
    }

    function hapusOpsiJawaban<%=random%>(idOpsi) {
        const wadah = document.getElementById('wadahOpsi<%=random%>');
        const jumlahOpsiSaatIni = wadah.querySelectorAll('.baris-opsi').length;

        if (jumlahOpsiSaatIni <= 2) {
            if(typeof tampilkanToast === "function") {
                tampilkanToast('<%= Common.getBahasaConfigJS("Sistem mewajibkan ketersediaan minimal 2 (dua) opsi jawaban untuk jenis soal pilihan ganda.") %>', 'bg-danger');
            } else {
                alert('<%= Common.getBahasaConfigJS("Sistem mewajibkan ketersediaan minimal 2 (dua) opsi jawaban untuk jenis soal pilihan ganda.") %>');
            }
            return;
        }

        const elemenHapus = document.getElementById(idOpsi);
        if (elemenHapus) {
            elemenHapus.remove();
            recalculateHurufOpsi<%=random%>();
        }
    }

    // ==========================================
    // NAVIGASI PEMBATALAN
    // ==========================================
    function kembaliAtauBatal<%=random%>() {
        const formCard = document.getElementById('cardFormSoal<%=random%>');
        if (formCard) {
            const modalElement = $(formCard).closest('.modal');
            if (modalElement.length > 0) {
                modalElement.modal('hide');
            } else {
                window.history.back();
            }
        }
    }

    // ==========================================
    // PENYIMPANAN DATA KE BACKEND (AJAX)
    // ==========================================
    function simpanDataSoal<%=random%>() {
        const idSoal = document.getElementById('idSoal<%=random%>').value;
        const jenisKoreksi = document.getElementById('jenisKoreksi<%=random%>').value;
        
        let teksSoal = document.getElementById('teksSoal<%=random%>').value;
        if (typeof CKEDITOR !== 'undefined' && CKEDITOR.instances['teksSoal<%=random%>']) {
            teksSoal = CKEDITOR.instances['teksSoal<%=random%>'].getData();
        }

        if (teksSoal.trim() === "" || teksSoal === "<p><br></p>") {
            if(typeof tampilkanToast === "function") {
                tampilkanToast('<%= Common.getBahasaConfigJS("Mohon lengkapi uraian pertanyaan soal terlebih dahulu sebelum melanjutkan proses penyimpanan.") %>', 'bg-danger');
            } else {
                alert('<%= Common.getBahasaConfigJS("Mohon lengkapi uraian pertanyaan soal terlebih dahulu sebelum melanjutkan proses penyimpanan.") %>');
            }
            return;
        }

        const formData = new URLSearchParams();
        formData.append('idSoal', idSoal);
        formData.append('jenisKoreksi', jenisKoreksi);
        formData.append('teksSoal', teksSoal);
        formData.append('ppu', '<%=ppuParam%>');

        if (jenisKoreksi === KOREKSI_OTOMATIS<%=random%>) {
            const barisOpsis = document.querySelectorAll('.baris-opsi');
            if (barisOpsis.length < 2) {
                if(typeof tampilkanToast === "function") {
                    tampilkanToast('<%= Common.getBahasaConfigJS("Sistem mewajibkan ketersediaan minimal 2 (dua) opsi jawaban untuk jenis soal pilihan ganda.") %>', 'bg-danger');
                } else {
                    alert('<%= Common.getBahasaConfigJS("Sistem mewajibkan ketersediaan minimal 2 (dua) opsi jawaban untuk jenis soal pilihan ganda.") %>');
                }
                return;
            }

            let adaKunciJawaban = false;
            let indexOpsi = 0;

            for (const baris of barisOpsis) {
                const textareaOpsi = baris.querySelector('.input-opsi-textarea');
                const radioKunci = baris.querySelector('.radio-kunci-jawaban');
                const inputHuruf = baris.querySelector('.input-huruf-opsi');
                
                const nilaiOpsi = textareaOpsi.value.trim();
                const nilaiHuruf = inputHuruf ? inputHuruf.value : String.fromCharCode(65 + indexOpsi);

                if (nilaiOpsi === "") {
                    if(typeof tampilkanToast === "function") {
                        tampilkanToast('<%= Common.getBahasaConfigJS("Terdapat opsi jawaban yang belum terisi. Mohon lengkapi seluruh opsi atau hapus opsi yang tidak diperlukan.") %>', 'bg-danger');
                    } else {
                        alert('<%= Common.getBahasaConfigJS("Terdapat opsi jawaban yang belum terisi. Mohon lengkapi seluruh opsi atau hapus opsi yang tidak diperlukan.") %>');
                    }
                    return;
                }

                formData.append('opsiJawaban[]', nilaiOpsi);
                formData.append('hurufOpsi[]', nilaiHuruf);
                
                if (radioKunci.checked) {
                    adaKunciJawaban = true;
                    formData.append('indeksKunciJawaban', indexOpsi); 
                }
                indexOpsi++;
            }

            if (!adaKunciJawaban) {
                if(typeof tampilkanToast === "function") {
                    tampilkanToast('<%= Common.getBahasaConfigJS("Kunci jawaban utama belum ditentukan. Silakan tandai salah satu opsi yang paling tepat sebagai jawaban yang benar.") %>', 'bg-danger');
                } else {
                    alert('<%= Common.getBahasaConfigJS("Kunci jawaban utama belum ditentukan. Silakan tandai salah satu opsi yang paling tepat sebagai jawaban yang benar.") %>');
                }
                return;
            }
        }

        const btnSimpan = document.getElementById('btnSimpanSoal<%=random%>');
        const teksAsliBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span><%= Common.getBahasaConfig("Sedang Menyimpan...") %>`;
        btnSimpan.disabled = true;

        fetch('<%= Common.ROOT %>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_simpan_soal', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(response => {
            if (!response.ok) throw new Error('<%= Common.getBahasaConfigJS("Respons jaringan server tidak valid.") %>');
            return response.json();
        })
        .then(data => {
            btnSimpan.innerHTML = teksAsliBtn;
            btnSimpan.disabled = false;

            if (data.status === 'success') {
                if (typeof tampilkanToast === "function") {
                    tampilkanToast(data.message || '<%= Common.getBahasaConfigJS("Data soal berhasil disimpan secara permanen ke dalam sistem.") %>', 'bg-success');
                } else {
                    alert(data.message || '<%= Common.getBahasaConfigJS("Data soal berhasil disimpan secara permanen ke dalam sistem.") %>');
                }
                
                <%=afterSave%>
                
                // Menutup popup secara otomatis (Baik Tambah Baru maupun Edit)
                kembaliAtauBatal<%=random%>();

            } else {
                if (typeof tampilkanToast === "function") {
                    tampilkanToast(data.message || '<%= Common.getBahasaConfigJS("Terjadi masalah sistem saat memproses rekam data penyimpanan.") %>', 'bg-danger');
                } else {
                    alert(data.message || '<%= Common.getBahasaConfigJS("Terjadi masalah sistem saat memproses rekam data penyimpanan.") %>');
                }
            }
        })
        .catch(error => {
            btnSimpan.innerHTML = teksAsliBtn;
            btnSimpan.disabled = false;
            if (typeof tampilkanToast === "function") {
                tampilkanToast('<%= Common.getBahasaConfigJS("Sistem gagal menghubungi peladen server utama. Silakan evaluasi koneksi lalu coba kembali nanti.") %>', 'bg-danger');
            } else {
                alert('<%= Common.getBahasaConfigJS("Sistem gagal menghubungi peladen server utama. Silakan evaluasi koneksi lalu coba kembali nanti.") %>');
            }
        });
    }
</script>