<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="java.util.List"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. Validasi Pengguna & Parameter Awal
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return; 
    }
    String itemsParam = request.getParameter("item");
    boolean adaData = false;
%>

<div class="d-flex flex-column gap-4">
<%
    try {
        // Validasi parameter kosong untuk mencegah NullPointerException
        if (itemsParam != null && !itemsParam.trim().isEmpty()) {
            String[] items = itemsParam.split(",");
            
            for (String itemId : items) {
                if (itemId == null || itemId.trim().isEmpty()) continue;

                // 2. Pengambilan Data
                PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) GeneralValueObject.ambilData(PertemuanPunyaUjian.class, itemId.trim(), true);
                
                // Validasi Integritas Data (Safety layer)
                if (ppu == null || ppu.getId() == null || ppu.getPertemuan() == null || ppu.getUjian() == null) continue;
                
                adaData = true; // Tandai bahwa data valid ditemukan

                // 3. Persiapan Variabel Tampilan
                List<Dosen> dosens = ppu.getPertemuan().ambilDosen();
                List<Mahasiswa> mahasiswas = ppu.getPertemuan().ambilMahasiswa();
                VOPembelajaran pembelajaran = ppu.getPertemuan().ambilVOPembelajaran();
                List<Long> swIdsUjian = (pembelajaran != null) ? pembelajaran.ambilSiswaById() : null;

                boolean adaKuliah = ppu.getPertemuan().getPerkuliahan() != null;
                boolean bolehUbah = ppu.getPertemuan().bolehUbahAbsenSaja(tbmuser);
                int totalPeserta = (mahasiswas != null ? mahasiswas.size() : 0) + (swIdsUjian != null ? swIdsUjian.size() : 0);
                
                // Format Tanggal & Waktu dengan Null-Check
                String tglMulai = (ppu.getMulaiUjian() != null) 
                    ? SmartDateTimeUtil.getDayString(ppu.getMulaiUjian(), null) + " " + Common.dateFormat61.get().format(ppu.getMulaiUjian()) 
                    : "-";
                
                String tglSelesai = (ppu.getSampaiUjian() != null) 
                    ? SmartDateTimeUtil.getDayString(ppu.getSampaiUjian(), null) + " " + Common.dateFormat61.get().format(ppu.getSampaiUjian()) 
                    : "-";
                    
                // Kalkulasi Rentang Waktu dengan Aman (Mencegah NullPointerException)
                String rentangWaktu = "-";
                if (ppu.getMulaiUjian() != null && ppu.getSampaiUjian() != null) {
                    long selisihMilis = ppu.getSampaiUjian().getTime() - ppu.getMulaiUjian().getTime();
                    
                    if (selisihMilis > 0) {
                        long hari = selisihMilis / (24 * 60 * 60 * 1000);
                        long sisaMilis = selisihMilis % (24 * 60 * 60 * 1000);
                        long jam = sisaMilis / (60 * 60 * 1000);
                        sisaMilis = sisaMilis % (60 * 60 * 1000);
                        long menit = sisaMilis / (60 * 1000);

                        StringBuilder sbWaktu = new StringBuilder();
                        if (hari > 0) sbWaktu.append(hari).append(" ").append(Common.getBahasaConfig("Hari")).append(", ");
                        if (jam > 0 || hari > 0) sbWaktu.append(jam).append(" ").append(Common.getBahasaConfig("Jam")).append(", ");
                        sbWaktu.append(menit).append(" ").append(Common.getBahasaConfig("Menit"));
                        
                        rentangWaktu = sbWaktu.toString();
                    }
                }

                String durasiPengerjaan = (ppu.getLama() != null) ? Common.timeFormat1.get().format(ppu.getLama()) : "-";
                
                // Pembuatan Tautan (Links)
                String linkSoal = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=daftar_soal&ppu=" + ppu.getId();
                String linkUjian = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=ujian&ppu=" + ppu.getId();
                String linkPeserta = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta&clazz=" + pembelajaran.getClass().getName() + "&id=" + pembelajaran.getId();
%>

    <div class="card border-0 shadow-sm rounded-4 overflow-hidden animate__animated animate__fadeInUp" data-id="<%=ppu.getId()%>">
        <div class="card-header bg-primary bg-gradient text-white p-3 border-0 d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center">
                <div class="bg-opacity-25 bg-white rounded-circle p-2 me-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width: 45px; height: 45px;">
                    <i class="fas fa-file-signature fs-4 text-white"></i>
                </div>
                <h5 class="mb-0 fw-bold text-white"><%=ppu.getUjian().getNama() %></h5>
            </div>
            <span class="badge bg-white text-primary rounded-pill px-3 py-2 text-uppercase fw-bold shadow-sm">
                <%=ppu.getUjian().getJenis() %>
            </span>
        </div>

        <div class="card-body p-4">
            <div class="row g-4 align-items-start">
                
                <div class="col-lg-7 col-md-12 border-end-lg">
                    <div class="row g-3">
                        <div class="col-12">
                            <div class="d-flex align-items-start">
                                <div class="text-primary me-3"><i class="far fa-calendar-alt fa-lg mt-1"></i></div>
                                <div>
                                    <small class="text-uppercase text-muted fw-bold" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Waktu Mulai Pelaksanaan") %></small>
                                    <div class="fw-bold text-dark"><%=tglMulai %></div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-12">
                            <div class="d-flex align-items-start">
                                <div class="text-danger me-3"><i class="far fa-calendar-check fa-lg mt-1"></i></div>
                                <div>
                                    <small class="text-uppercase text-muted fw-bold" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Batas Waktu Pelaksanaan") %></small>
                                    <div class="fw-bold text-dark"><%=tglSelesai %></div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-12">
                            <div class="d-flex align-items-start">
                                <div class="text-warning me-3"><i class="fas fa-hourglass-half fa-lg mt-1"></i></div>
                                <div>
                                    <small class="text-uppercase text-muted fw-bold" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Rentang Waktu Akses Ujian") %></small>
                                    <div class="fw-bold text-dark"><%=rentangWaktu %></div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-12">
                            <div class="d-flex align-items-start">
                                <div class="text-info me-3"><i class="far fa-clock fa-lg mt-1"></i></div>
                                <div>
                                    <small class="text-uppercase text-muted fw-bold" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Durasi Pengerjaan") %></small>
                                    <div class="fw-bold text-dark"><%=durasiPengerjaan %></div>
                                    <div class="small text-secondary"><i class="fas fa-info-circle me-1"></i> <%=Common.getBahasaConfig("Batas waktu maksimal pengerjaan soal") %></div>
                                </div>
                            </div>
                        </div>
                        
                        <% if(adaKuliah) { %>
                        <div class="col-md-6 col-12">
                            <div class="d-flex align-items-start mt-2">
                                <div class="text-success me-3"><i class="fas fa-book fa-lg mt-1"></i></div>
                                <div>
                                    <small class="text-uppercase text-muted fw-bold" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Mata Kuliah") %></small>
                                    <div class="fw-bold text-dark text-truncate" style="max-width: 220px;" title="<%=ppu.getPertemuan().getPerkuliahan().getMatakuliah().getNama() %>">
                                        <%=ppu.getPertemuan().getPerkuliahan().getMatakuliah().getNama() %>
                                    </div>
                                    <div class="small text-muted"><%=ppu.getPertemuan().getPerkuliahan().getMatakuliah().getKode() %></div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-md-6 col-12">
                            <div class="d-flex align-items-start mt-2">
                                <div class="text-secondary me-3"><i class="fas fa-layer-group fa-lg mt-1"></i></div>
                                <div>
                                    <small class="text-uppercase text-muted fw-bold" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Kelas dan Tahun Ajaran") %></small>
                                    <div class="fw-bold text-dark">
                                        <%=Common.getBahasaConfig("Kelas") %> <%=ppu.getPertemuan().getPerkuliahan().getKelas() %>
                                    </div>
                                    <div class="small text-muted">
                                        <%=ppu.getPertemuan().getPerkuliahan().getTahunAjaran() %> (<%=ppu.getPertemuan().getPerkuliahan().getGanjilGenap() %>)
                                    </div>
                                </div>
                            </div>
                        </div>
                        <% } %>
                    </div>
                </div>

                <div class="col-lg-5 col-md-12 d-flex flex-column justify-content-between">
                    <div class="bg-light rounded-4 p-3 border border-light mb-4">
                        <small class="text-muted fw-bold d-block mb-2"><%=Common.getBahasaConfig("Dosen Pengampu") %>:</small>
                        <div class="d-flex align-items-center">
                            <div class="d-flex ps-2">
                                <% 
                                if (dosens != null && !dosens.isEmpty()) {
                                    StringBuilder dosenNames = new StringBuilder();
                                    for(Dosen d : dosens) {
                                        if(d == null) continue;
                                        String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(d));
                                        if(dosenNames.length() > 0) dosenNames.append(", ");
                                        dosenNames.append(d.getNama());
                                %>
                                <img src="<%=url %>" 
                                     class="rounded-circle border border-2 border-white shadow-sm" 
                                     style="width: 45px; height: 45px; object-fit: cover; margin-left: -12px; cursor: pointer;" 
                                     data-bs-toggle="tooltip" 
                                     title="<%=d.getNama() %>"
                                     onclick="tampilGambar(this)">
                                <% 
                                    } 
                                %>
                            </div>
                            <div class="ms-3 small text-muted text-truncate fst-italic" style="max-width: 150px;" title="<%=dosenNames.toString() %>">
                                <%=dosenNames.toString() %>
                            </div>
                            <% } else { %>
                                <span class="small text-muted fst-italic"><%=Common.getBahasaConfig("Tidak ada data dosen") %></span>
                            <% } %>
                        </div>
                    </div>

                    <div class="d-flex flex-wrap gap-2 justify-content-lg-end justify-content-start">
                        <button class="btn btn-outline-secondary btn-sm rounded-pill fw-bold px-3 d-flex align-items-center" 
                                onclick="loadModalContent('<%=Common.getBahasaConfigJS("Daftar Peserta") %>', '<%=linkPeserta%>');">
                            <i class="fas fa-users me-2"></i> <%=totalPeserta %> <%=Common.getBahasaConfig("Peserta") %>
                        </button>
                        
                        <% if(bolehUbah) { %>
                        <button class="btn btn-success btn-sm rounded-pill fw-bold px-3 shadow-sm d-flex align-items-center"
                                onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Buat Ujian Baru") %>', '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=ujian_baru&pertemuan=<%=ppu.getPertemuan().getId()%>&contentModal='+cm+'&afterSave='+encodeURIComponent(''), '75%', true, 'simpanDataHelper_'+cm+'();', cm);">
                            <i class="fas fa-plus me-2"></i> <%=Common.getBahasaConfig("Buat Ujian Baru") %>
                        </button>
                        <button class="btn btn-warning text-dark btn-sm rounded-pill fw-bold px-3 shadow-sm d-flex align-items-center"
                                onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Tinjau Soal") %>', '<%=linkSoal%>&contentModal='+cm+'&afterSave='+encodeURIComponent(''), '95%', 'hidden', '', cm);">
                            <i class="fas fa-eye me-2"></i> <%=Common.getBahasaConfig("Tinjau Soal") %>
                        </button>
                        <button class="btn btn-outline-dark btn-sm rounded-pill fw-bold px-3 shadow-sm d-flex align-items-center"
                                onclick="gandakanUjian(<%=ppu.getId()%>)">
                            <i class="fas fa-copy me-2"></i> <%=Common.getBahasaConfig("Gandakan") %>
                        </button>
                        <% } %>
                        
                        <button class="btn btn-primary btn-sm rounded-pill fw-bold px-4 shadow-sm d-flex align-items-center" 
                                onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Mulai Ujian") %>', '<%=linkUjian%>&contentModal='+cm+'&afterSave='+encodeURIComponent(''), '95%', 'hidden', '', cm);">
                            <i class="fas fa-edit me-2"></i> <%=Common.getBahasaConfig("Mulai Ujian") %>
                        </button>
                    </div>
                </div>
                
            </div>
        </div>
    </div>

<%
            } // Akhir iterasi items
        } // Akhir validasi itemsParam

        // 4. Kondisi Data Kosong
        if (!adaData) {
%>
        <jsp:include page="/WEB-INF/baru/componen/tidak_ketemu.jsp">
            <jsp:param name="pesan" value='<%=Common.getBahasaConfigJS("Tidak ditemukan data ujian untuk pertemuan atau kelas yang dipilih.") %>' />
        </jsp:include>
<%
        }

    } catch (Exception e) {
        System.out.println("Terjadi kesalahan saat memuat daftar ujian: " + e.getMessage());
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/load_ujian.jsp:257");
    } finally {
        // 5. Menutup Session Database
        // Catatan: Jika Anda menggunakan ORM standar seperti Hibernate dalam framework,
        // silakan panggil method penutup di sini. Contoh yang paling umum:
        try {
            // GeneralValueObject.closeSession(); // Uncomment atau ganti dengan utility penutup session Anda
        } catch (Exception ex) {
            System.out.println("Gagal menutup koneksi database: " + ex.getMessage());
        }
    }
%>
<script>
function gandakanUjian(ppuId) {
    if (!confirm('<%=Common.getBahasaConfigJS("Gandakan ujian beserta semua soalnya? Ujian baru akan dibuat sebagai tidak aktif.") %>')) return;
    fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_gandakan_ujian', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: 'ppu=' + ppuId
    })
    .then(function(r) { return r.json(); })
    .then(function(d) {
        if (d.status === 'success') { alert(d.message); location.reload(); }
        else alert('<%=Common.getBahasaConfigJS("Gagal") %>: ' + d.message);
    })
    .catch(function(e) { alert('Error: ' + e); });
}
</script>
</div>