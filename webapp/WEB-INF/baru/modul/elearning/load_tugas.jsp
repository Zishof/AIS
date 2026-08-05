<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="ais.database.model.TugasPertemuan"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Tugas"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        return;
    }

    String itemParam = request.getParameter("item");
    if (itemParam == null || itemParam.isEmpty()) return;

    for (String itemId : itemParam.split(",")) {
        String[] idData = itemId.split("_");
        if (idData.length < 2) continue;
        
        String idDataP = idData[0].trim();
        String clazzDataP = idData[1].trim();

        Class<?> clazz = clazzDataP.equalsIgnoreCase("Pertemuan") ? Pertemuan.class : TugasPertemuan.class;
        Tugas tugas = (Tugas) GeneralValueObject.ambilData(clazz, idDataP, true);

        if (tugas != null && tugas.getId() != null) {
            Pertemuan pertemuan = null;

            if (tugas instanceof Pertemuan) {
                pertemuan = (Pertemuan) tugas;
            } else if (tugas instanceof TugasPertemuan) {
                TugasPertemuan tp = (TugasPertemuan) tugas;
                pertemuan = tp.ambilPertemuan();
            }

            if (pertemuan == null) continue;

            List<Dosen> dosens = pertemuan.ambilDosen();
            List<Mahasiswa> mahasiswas = pertemuan.ambilMahasiswa();
            VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
            List<Long> swIdsTugas = (pembelajaran != null) ? pembelajaran.ambilSiswaById() : null;
            int totalPesertaTugas = (mahasiswas != null ? mahasiswas.size() : 0) + (swIdsTugas != null ? swIdsTugas.size() : 0);

            if(pembelajaran != null){
                // Format Tanggal
                String tglMulai = (tugas.getMulai() != null) ? SmartDateTimeUtil.getDayString(tugas.getMulai(), null) + " " + Common.dateFormat6.get().format(tugas.getMulai()) : "-";
                String jamMulai = (tugas.getMulai() != null) ? Common.timeFormat.get().format(tugas.getMulai()) + " " : "";
                
                String tglSelesai = (tugas.getSelesai() != null) ? SmartDateTimeUtil.getDayString(tugas.getSelesai(), null) + " " + Common.dateFormat6.get().format(tugas.getSelesai()) : "";
                String jamSelesai = (tugas.getSelesai() != null) ? Common.timeFormat.get().format(tugas.getSelesai()) + " " : "";

                String linkMahasiswas = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta&clazz=" + pembelajaran.getClass().getName() + "&id=" + pembelajaran.getId();
                
                // === TAMBAHAN VARIABEL UNTUK LINK MODAL TUGAS ===
                String linkTugas = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=tugas&pertemuan=" + pertemuan.getId();
                String jenisTugas = (tugas instanceof Pertemuan) ? "Pertemuan" : "TugasPertemuan";
                String escapedJudul = tugas.getJudultugas() != null ? tugas.getJudultugas().replace("'", "\\'") : Common.getBahasaConfig("Detail Tugas");
                // ================================================
%>

    <div class="card border-0 shadow-lg rounded-4 overflow-hidden mb-4 animate__animated animate__fadeInUp" data-id="<%=tugas.getId()%>">
        
        <div class="card-header bg-primary bg-gradient text-white p-3 border-0 d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center">
                <div class="bg-opacity-25 rounded-circle p-2 me-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width: 45px; height: 45px;">
                    <i class="fas fa-tasks fs-4"></i>
                </div>
                <div>
                    <h5 class="mb-0 fw-bold text-white"><%=tugas.getJudultugas() %></h5>
                    <small class="opacity-75"><i class="fas fa-book-reader me-1"></i>Tugas Pertemuan</small>
                </div>
            </div>
            <% if (tugas.getSyaratMengumpulkanTugas() != null) { %>
                <span class="badge bg-warning text-dark shadow-sm rounded-pill px-3 py-2">
                    <i class="fas fa-exclamation-circle me-1"></i> <%=tugas.getSyaratMengumpulkanTugas().getNama() %>
                </span>
            <% } %>
        </div>

        <div class="card-body p-4 bg-light bg-opacity-10">
            <div class="row g-4">
                
                <div class="col-lg-8 border-end-lg">
                    
                    <% if(pertemuan.getPerkuliahan() != null) { %>
                    <div class="mb-4">
                        <h6 class="fw-bold text-primary mb-1"><%=pertemuan.getPerkuliahan().getMatakuliah().getNama() %></h6>
                        <div class="d-flex flex-wrap gap-2 text-muted small">
                            <span><i class="fas fa-barcode me-1"></i><%=pertemuan.getPerkuliahan().getMatakuliah().getKode() %></span> • 
                            <span><i class="fas fa-door-open me-1"></i>Kelas <%=pertemuan.getPerkuliahan().getKelas() %></span> • 
                            <span><%=pertemuan.getPerkuliahan().getTahunAjaran() %></span>
                        </div>
                    </div>
                    <% } %>

                    <div class="d-flex flex-column flex-md-row gap-3">
                        <div class="flex-fill bg-white p-3 rounded-3 border shadow-sm">
                            <small class="text-uppercase text-muted fw-bold d-block mb-1" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Mulai") %></small>
                            <div class="d-flex align-items-center">
                                <i class="far fa-calendar-check text-success fs-4 me-3"></i>
                                <div style="line-height: 1.2;">
                                    <span class="fw-bold d-block text-dark"><%=tglMulai %></span>
                                    <span class="small text-muted"><%=jamMulai %></span>
                                </div>
                            </div>
                        </div>

                        <% if(!tglSelesai.isEmpty()) { %>
                        <div class="flex-fill bg-danger bg-opacity-10 p-3 rounded-3 border border-danger border-opacity-25">
                            <small class="text-uppercase text-danger fw-bold d-block mb-1" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Batas Akhir") %></small>
                            <div class="d-flex align-items-center">
                                <i class="far fa-calendar-times text-danger fs-4 me-3"></i>
                                <div style="line-height: 1.2;">
                                    <span class="fw-bold d-block text-danger"><%=tglSelesai %></span>
                                    <span class="small text-danger opacity-75"><%=jamSelesai %></span>
                                </div>
                            </div>
                        </div>
                        <% } %>
                    </div>
                </div>

                <div class="col-lg-4 d-flex flex-column justify-content-between">
                    
                    <div class="mb-4">
                        <small class="text-uppercase text-muted fw-bold mb-2 d-block" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Dosen Pengampu") %></small>
                        <div class="d-flex align-items-center">
                            <% for(Dosen dosen : dosens) { 
                                String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen));
                            %>
                                <img src="<%=url%>" 
                                     class="rounded-circle border border-2 border-white shadow-sm me-1" 
                                     style="width: 45px; height: 45px; object-fit: cover; cursor: pointer; transition: transform 0.2s;"
                                     onmouseover="this.style.transform='scale(1.1)'" 
                                     onmouseout="this.style.transform='scale(1)'"
                                     title="<%=dosen.getNama()%>" 
                                     data-bs-toggle="tooltip">
                            <% } %>
                            <div class="ms-2">
                                <span class="d-block small fw-bold text-dark"><%= dosens.isEmpty() ? "-" : dosens.get(0).getNama() %></span>
                                <% if(dosens.size() > 1) { %>
                                    <span class="d-block x-small text-muted">+<%=dosens.size()-1%> Lainnya</span>
                                <% } %>
                            </div>
                        </div>
                    </div>

                    <div class="d-grid gap-2">
                        <button class="btn btn-outline-primary btn-sm rounded-pill fw-bold" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Daftar Peserta") %>', '<%=linkMahasiswas%>');">
                            <i class="fas fa-users me-1"></i> <%=totalPesertaTugas %> <%=Common.getBahasaConfig("Peserta") %>
                        </button>

                        <% if (pertemuan.bolehUbahAbsenSaja(tbmuser)) { %>
                        <button class="btn btn-outline-success btn-sm rounded-pill fw-bold py-2"
                                onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Buat Tugas Baru") %>', '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=tugas_baru&jenis=Pertemuan&pertemuan=<%=pertemuan.getId()%>&contentModal='+cm, '75%', true, 'simpanDataHelper_'+cm+'();', cm);">
                            <i class="fas fa-plus me-1"></i> <%=Common.getBahasaConfig("Buat Tugas Baru") %>
                        </button>
                        <% } %>

                        <button class="btn btn-success btn-sm rounded-pill fw-bold shadow-sm py-2"
                                onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=escapedJudul%>', '<%=linkTugas%>&tugas=<%=tugas.getId()%>&jenis=<%=jenisTugas%>&contentModal='+cm, '85%', 'hidden', '', cm);">
                            <i class="fas fa-paper-plane me-1"></i> <%=Common.getBahasaConfig("Lihat Tugas") %>
                        </button>
                        </div>

                </div>
            </div>
        </div>
    </div>
    <%
            }
        } // End if tugas != null
    } // End Loop
%>