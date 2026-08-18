<%@page import="ais.database.model.NamaTugasKelompokPunyaMahasiswa"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.NamaTugasKelompok"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.TugasKelompok"%>
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

    for (String itemIdRaw : itemParam.split(",")) {
        String itemId = itemIdRaw.contains("_") ? itemIdRaw.split("_")[0].trim() : itemIdRaw.trim();
        
        TugasKelompok tugas = (TugasKelompok) GeneralValueObject.ambilData(TugasKelompok.class, itemId, true); 
        
        if (tugas != null && tugas.getId() != null) {
            Perkuliahan perkuliahan = tugas.getPerkuliahan();
            // KE-19: tugas.getPerkuliahan() bisa null (data tak konsisten / relasi terhapus).
            // Lewati item ini saja, jangan crash seluruh render (item lain tetap tampil).
            if (perkuliahan == null) {
                continue;
            }
            List<Dosen> dosens = perkuliahan.populateDosenBuNama();
            List<Mahasiswa> mahasiswas = perkuliahan.ambilMahasiswa();
            
            // 1. Fetch Daftar Kelompok
            List<NamaTugasKelompok> namaTugasKelompoks = null;
            Session sessionInfo = null;
            try {
                sessionInfo = HibernateUtil.openSession();
                namaTugasKelompoks = sessionInfo.createCriteria(NamaTugasKelompok.class)
                        .addOrder(Order.asc("nama"))
                        .add(Restrictions.eq("tugasKelompok", tugas))
                        .list();
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/load_tugas_kelompok.jsp:57");
            } finally {
                ais.common.ElearningSessionUtil.closeQuietly(sessionInfo);
            }
            
            // Format Tanggal
            String tglMulai = (tugas.getMulai() != null) ? SmartDateTimeUtil.getDayString(tugas.getMulai(), null) + " " + Common.dateFormat6.get().format(tugas.getMulai()) : "-";
            String jamMulai = (tugas.getMulai() != null) ? Common.timeFormat.get().format(tugas.getMulai()) : "";
            String tglSelesai = (tugas.getSelesai() != null) ? SmartDateTimeUtil.getDayString(tugas.getSelesai(), null) + " " + Common.dateFormat6.get().format(tugas.getSelesai()) : "-";
            String jamSelesai = (tugas.getSelesai() != null) ? Common.timeFormat.get().format(tugas.getSelesai()) : "";
            
            String linkMahasiswas = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta&clazz=" + Perkuliahan.class.getName() + "&id=" + perkuliahan.getId();

            // Pertemuan induk tugas kelompok (untuk tombol "Detail Tugas" → halaman kelola pertemuan)
            ais.database.model.Pertemuan pertemuanKlp = tugas.ambilPertemuan();
            Long idPertemuanKlp = (pertemuanKlp != null) ? pertemuanKlp.getId() : null;
            String linkKelolaKlp = (idPertemuanKlp != null)
                ? Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta_absen&pertemuan=" + idPertemuanKlp
                : "";
%>

    <div class="card border-0 shadow-lg rounded-4 overflow-hidden mb-5 animate__animated animate__fadeInUp" data-id="<%=tugas.getId()%>">
        
        <div class="card-header bg-primary bg-gradient text-white p-3 border-0 d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center">
                <div class="bg-opacity-25 rounded-circle p-2 me-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width: 45px; height: 45px;">
                    <i class="fas fa-users-cog fs-4"></i>
                </div>
                <div>
                    <h5 class="mb-0 fw-bold text-white"><%=tugas.getJudul() %></h5>
                    <small class="opacity-75"><i class="fas fa-layer-group me-1"></i><%=Common.getBahasaConfig("Total Kelompok") %>: <b><%= (namaTugasKelompoks != null ? namaTugasKelompoks.size() : 0) %></b></small>
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
                    
                    <div class="mb-4">
                         <h6 class="fw-bold text-primary mb-1"><%=perkuliahan.getMatakuliah().getNama() %></h6>
                         <div class="d-flex flex-wrap gap-2 text-muted small">
                             <span><i class="fas fa-barcode me-1"></i><%=perkuliahan.getMatakuliah().getKode() %></span> • 
                             <span><i class="fas fa-door-open me-1"></i>Kelas <%=perkuliahan.getKelas() %></span> • 
                             <span><%=perkuliahan.getTahunAjaran() %> (<%=perkuliahan.getGanjilGenap() %>)</span>
                         </div>
                    </div>

                    <div class="d-flex flex-column flex-md-row gap-3 mb-4">
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

                        <% if(tugas.getSelesai() != null) { %>
                        <div class="flex-fill bg-danger bg-opacity-10 p-3 rounded-3 border border-danger border-opacity-25">
                            <small class="text-uppercase text-danger fw-bold d-block mb-1" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Batas Akhir") %></small>
                            <div class="d-flex align-items-center">
                                <i class="far fa-calendar-times text-danger fs-4 me-3"></i>
                                <div style="line-height: 1.2;">
                                    <span class="fw-bold d-block text-danger"><%=tglSelesai %></span>
                                    <span class="small text-danger opacity-75"><%=jamSelesai %> WIB</span>
                                </div>
                            </div>
                        </div>
                        <% } %>
                    </div>

                    <% if(namaTugasKelompoks != null && !namaTugasKelompoks.isEmpty()) { %>
                    <div class="mt-4">
                        <h6 class="text-uppercase text-muted fw-bold mb-3 small"><i class="fas fa-th-list me-2"></i><%=Common.getBahasaConfig("Daftar Kelompok & Anggota") %></h6>
                        
                        <div class="accordion" id="accordionTugas<%=tugas.getId()%>">
                            <% 
                            for(NamaTugasKelompok ntk : namaTugasKelompoks) { 
                                List<Mahasiswa> listMhs = new ArrayList<Mahasiswa>();
                                List<Siswa> listSiswa = new ArrayList<Siswa>();
                                boolean isSiswa = (ntk.getTugasKelompok().getJadwalPelajaran() != null);
                                int jumlahAnggota = 0;

                                Session sessionAnggota = null;
                                try {
                                    sessionAnggota = HibernateUtil.openSession();
                                    
                                    if (isSiswa) {
                                        // Fetch Siswa
                                        List<NamaTugasKelompokPunyaMahasiswa> relations = sessionAnggota.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
                                        	.add(Restrictions.isNotNull("siswa"))
                                            .add(Restrictions.eq("namaTugasKelompok", ntk))
                                            .list();
                                        for(NamaTugasKelompokPunyaMahasiswa r : relations) {
                                            if(r.getSiswa() != null) listSiswa.add(r.getSiswa());
                                        }
                                        jumlahAnggota = listSiswa.size();
                                    } else {
                                        // Fetch Mahasiswa
                                        List<NamaTugasKelompokPunyaMahasiswa> relations = sessionAnggota.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
                                        	.add(Restrictions.isNotNull("mahasiswa"))
                                            .add(Restrictions.eq("namaTugasKelompok", ntk))
                                            .list();
                                        for(NamaTugasKelompokPunyaMahasiswa r : relations) {
                                            if(r.getMahasiswa() != null) listMhs.add(r.getMahasiswa());
                                        }
                                        jumlahAnggota = listMhs.size();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/load_tugas_kelompok.jsp:175");
                                } finally {
                                    ais.common.ElearningSessionUtil.closeQuietly(sessionAnggota);
                                }
                                
                                String collapseId = "collapse_" + ntk.getId();
                            %>
                            
                            <div class="accordion-item border-0 mb-2 shadow-sm rounded overflow-hidden">
                                <h2 class="accordion-header">
                                    <button class="accordion-button collapsed fw-bold text-dark bg-white" type="button" data-bs-toggle="collapse" data-bs-target="#<%=collapseId%>" aria-expanded="false">
                                        <div class="d-flex align-items-center w-100 me-3">
                                            <div class="me-auto">
                                                <i class="fas fa-users text-primary me-2"></i> <%= ntk.getNama() %>
                                            </div>
                                            <span class="badge bg-primary rounded-pill"><%= jumlahAnggota %> <%=Common.getBahasaConfig("Anggota") %></span>
                                        </div>
                                    </button>
                                </h2>
                                <div id="<%=collapseId%>" class="accordion-collapse collapse" data-bs-parent="#accordionTugas<%=tugas.getId()%>">
                                    <div class="accordion-body bg-light p-2">
                                        <% if (jumlahAnggota > 0) { %>
                                            <div class="list-group list-group-flush rounded-3">
                                                <% 
                                                if(isSiswa) {
                                                    for(Siswa s : listSiswa){
                                                        String urlFoto = CommonMedia.getUrlFotoPengguna(new Tbmuser(s));
                                                %>
                                                    <div class="list-group-item d-flex align-items-center bg-white">
                                                        <img src="<%=urlFoto%>" onclick="tampilGambar(this)" class="rounded-circle me-3 border" style="width: 35px; height: 35px; object-fit: cover; cursor:pointer;">
                                                        <div>
                                                            <div class="fw-bold text-dark small"><%=s.getNama()%></div>
                                                            <small class="text-muted" style="font-size: 0.7rem;"><%=s.getNomorInduk() == null ? "-" : s.getNomorInduk()%></small>
                                                        </div>
                                                    </div>
                                                <% 
                                                    } 
                                                } else { 
                                                    for(Mahasiswa m : listMhs){
                                                        String urlFoto = CommonMedia.getUrlFotoPengguna(new Tbmuser(m));
                                                %>
                                                    <div class="list-group-item d-flex align-items-center bg-white">
                                                        <img src="<%=urlFoto%>" onclick="tampilGambar(this)" class="rounded-circle me-3 border" style="width: 35px; height: 35px; object-fit: cover; cursor:pointer;">
                                                        <div>
                                                            <div class="fw-bold text-dark small"><%=m.getNama()%></div>
                                                            <small class="text-muted" style="font-size: 0.7rem;"><%=m.getNim() == null ? "-" : m.getNim()%></small>
                                                        </div>
                                                    </div>
                                                <% 
                                                    } 
                                                } 
                                                %>
                                            </div>
                                        <% } else { %>
                                            <div class="text-center text-muted small py-2 fst-italic">
                                                <%=Common.getBahasaConfig("Belum ada anggota di kelompok ini") %>
                                            </div>
                                        <% } %>
                                    </div>
                                </div>
                            </div>
                            
                            <% } %>
                        </div>
                    </div>
                    <% } else { %>
                        <div class="alert alert-warning border-0 bg-warning bg-opacity-10 text-dark small">
                            <i class="fas fa-info-circle me-1"></i> <%=Common.getBahasaConfig("Belum ada pembagian kelompok.") %>
                        </div>
                    <% } %>

                </div>

                <div class="col-lg-4 d-flex flex-column">
                    <div class="mb-4 bg-white p-3 rounded-4 border shadow-sm">
                        <small class="text-uppercase text-muted fw-bold mb-3 d-block border-bottom pb-2" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Dosen Pengampu") %></small>
                        <div class="d-flex flex-column gap-2">
                            <% for(Dosen dosen : dosens) { 
                                String urlFoto = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen));
                            %>
                            <div class="d-flex align-items-center">
                                <img onclick="tampilGambar(this)" 
                                     src="<%=urlFoto%>" 
                                     class="rounded-circle border shadow-sm me-2" 
                                     style="width: 35px; height: 35px; object-fit: cover; cursor: pointer;">
                                <span class="small fw-bold text-dark"><%=dosen.getNama()%></span>
                            </div>
                            <% } %>
                        </div>
                    </div>

                    <div class="mt-auto d-grid gap-2">
                        <button class="btn btn-outline-primary btn-sm rounded-pill fw-bold" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Daftar Mahasiswa") %>', '<%=linkMahasiswas%>');">
                            <i class="fas fa-users me-1"></i> <%=mahasiswas.size() %> <%=Common.getBahasaConfig("Peserta Kelas") %>
                        </button>
                        <% if (idPertemuanKlp != null) { %>
                        <button class="btn btn-primary btn-sm rounded-pill fw-bold shadow-sm py-2"
                                onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Detail & Kelola Tugas Kelompok") %>', '<%=linkKelolaKlp%>&contentModal='+cm, '90%', null, '', cm);">
                            <i class="fas fa-eye me-1"></i> <%=Common.getBahasaConfig("Detail Tugas") %>
                        </button>
                        <% } %>
                        <% if (pertemuanKlp != null && pertemuanKlp.bolehUbahAbsenSaja(tbmuser)) { %>
                        <button class="btn btn-outline-dark btn-sm rounded-pill fw-bold"
                                onclick="window.open('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=download_semua&idTugas=<%=tugas.getId()%>&jenis=TugasKelompok', '_blank');">
                            <i class="fas fa-download me-1"></i> <%=Common.getBahasaConfig("Download Semua") %>
                        </button>
                        <% } %>
                    </div>
                </div>
            </div>
        </div>
    </div>

<%
        } // End if tugas != null
    } // End loop
%>