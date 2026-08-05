<%@page import="ais.database.model.sekolah.Matapelajaran"%>
<%@page import="ais.database.model.sekolah.JadwalPelajaran"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="java.util.List"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // --- Inisialisasi Variable Default ---
    String wkt = "-";
    String tgl = "-";
    String kodeMK = "";
    String namaMK = "";
    String ta = "-";
    String smt = "-";
    String kelas = "-";
    String semAngka = "";
    String ruang = "-";
    String topik = "-";
    String catat = "";
    String listPengajar = "-";
    String pertemuanKe = "-";
    boolean hasCatatan = false;
    
    // Logic tetap dipertahankan seperti aslinya
    try {
        String reqId = request.getParameter("pertemuan");
        if (reqId != null && !reqId.isEmpty()) {
            Pertemuan p = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, reqId, true);

            if (p != null) {
                if (p.getWaktuMulai() != null && p.getWaktuSelesai() != null) {
                    wkt = p.getWaktuMulai() + " - " + p.getWaktuSelesai();
                }
                
                if (p.getTanggal() != null) {
                    tgl = SmartDateTimeUtil.getDayString(p.getTanggal(), p.getWaktuMulai()) + " " + Common.dateFormat6.get().format(p.getTanggal());
                }

                pertemuanKe = (p.getPertemuanKe() != null) ? p.getPertemuanKe().toString() : "-";
                topik = (p.getTopik() != null) ? p.getTopik() : "-";
                if (p.getRuang() != null && p.getRuang().getNama() != null) {
                    ruang = p.getRuang().getNama();
                }

                Perkuliahan perkuliahan = p.getPerkuliahan();
                JadwalPelajaran jadwalPelajaran = p.getJadwalPelajaran();
                if (perkuliahan != null) {
                    Matakuliah mk = perkuliahan.getMatakuliah();
                    kodeMK = (mk != null) ? mk.getKode() : "";
                    namaMK = (mk != null) ? mk.getNama() : p.info();
                    ta = (perkuliahan.getTahunAjaran() != null) ? perkuliahan.getTahunAjaran() : "-";
                    smt = (perkuliahan.getGanjilGenap() != null) ? perkuliahan.getGanjilGenap() : "-";
                    kelas = (perkuliahan.getKelas() != null) ? perkuliahan.getKelas() : "-";
                    semAngka = (perkuliahan.getSemester() != null) ? perkuliahan.getSemester().toString() : "";
                } else if (jadwalPelajaran != null) {
                    Matapelajaran mk = jadwalPelajaran.getMatapelajaran();
                    kodeMK = (mk != null) ? mk.getKode() : "";
                    namaMK = (mk != null) ? mk.getNama() : p.info();
                    ta = (jadwalPelajaran.getTahunAjaran() != null) ? jadwalPelajaran.getTahunAjaran() : "-";
                    smt = (jadwalPelajaran.getSemester() != null) ? (jadwalPelajaran.getSemester().equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP) : "-";
                    kelas = (jadwalPelajaran.getKelas() != null) ? jadwalPelajaran.getKelas().getNama() : "-";
                    semAngka = (jadwalPelajaran.getSekolah() != null) ? jadwalPelajaran.getSekolah().getNama() : "";
                } else {
                    namaMK = p.info();
                }
                
                StringBuilder sbPengajar = new StringBuilder();
                List<Dosen> dosens = p.ambilDosen();
                if (dosens != null && !dosens.isEmpty()) {
                    for (Dosen d : dosens) {
                        if (d.getNama() != null) {
                            if (sbPengajar.length() > 0) sbPengajar.append(", ");
                            sbPengajar.append(d.getNama());
                        }
                    }
                }
                List<Guru> gurus = p.ambilGuru();
                if (gurus != null && !gurus.isEmpty()) {
                    for (Guru g : gurus) {
                        if (g.getNama() != null) {
                            if (sbPengajar.length() > 0) sbPengajar.append(", ");
                            sbPengajar.append(g.getNama());
                        }
                    }
                }
                if (sbPengajar.length() > 0) listPengajar = sbPengajar.toString();

                catat = p.getCatatan();
                hasCatatan = (catat != null && !catat.trim().isEmpty());
                if (hasCatatan) {
                    String linkText = Common.getBahasaConfig("Buka Tautan");
                    catat = catat.replaceAll("(https?://\\S+)", "<a target='_blank' class='text-decoration-underline fw-bold' href=\"$1\">" + linkText + " <i class='fas fa-external-link-alt small'></i></a>");
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/_info_kehadiran_pertemuan_horizontal.jsp:105");
    }
    
    if (kodeMK.isEmpty() && namaMK.isEmpty()) return;
%>

<div class="card shadow-sm border-0 rounded-4 overflow-hidden mb-4">
    <div class="row g-0">
        
        <div class="col-md-3 bg-primary bg-gradient text-white d-flex flex-column justify-content-center align-items-center p-4 text-center position-relative">
            <div class="position-absolute top-0 start-0 w-100 h-100" style="background-image: radial-gradient(circle at top right, rgba(255,255,255,0.1) 0%, transparent 40%); pointer-events: none;"></div>
            
            <div class="mb-3 position-relative">
                <span class="badge bg-white text-primary rounded-pill px-3 py-2 shadow-sm border border-light" style="font-size: 0.85rem;">
                    <%=Common.getBahasaConfig("Pertemuan") %> #<span class="fw-bold"><%=pertemuanKe %></span>
                </span>
            </div>
            
            <h5 class="fw-bold mb-1 text-white"><%=tgl %></h5>
            <div class="d-flex align-items-center justify-content-center bg-white bg-opacity-10 rounded-3 px-3 py-1 mt-2">
                <i class="far fa-clock me-2"></i> 
                <span class="fw-semibold"><%=wkt %></span>
            </div>
        </div>

        <div class="col-md-9 bg-white">
            <div class="card-body p-4">
                
                <div class="d-flex flex-wrap justify-content-between align-items-start mb-4 border-bottom pb-3">
                    <div>
                        <span class="text-primary fw-bold text-uppercase small letter-spacing-1"><%=kodeMK %></span>
                        <h4 class="card-title fw-bold text-dark mb-0 mt-1"><%=namaMK %></h4>
                        <div class="text-muted small mt-1">
                            <%=ta %> <span class="mx-1">&bull;</span> Semester <%=smt %> (<%=semAngka %>)
                        </div>
                    </div>
                    <div class="mt-2 mt-md-0">
                        <div class="d-flex align-items-center">
                            <span class="badge bg-info-subtle text-info-emphasis border border-info-subtle rounded-pill px-3 py-2">
                                <i class="fas fa-users me-1"></i> Kelas <%=kelas %>
                            </span>
                        </div>
                    </div>
                </div>

                <div class="row g-4 mb-4">
                    <div class="col-md-7">
                        <div class="d-flex">
                            <div class="flex-shrink-0">
                                <div class="bg-success-subtle text-success rounded-circle p-2 d-flex align-items-center justify-content-center" style="width: 40px; height: 40px;">
                                    <i class="fas fa-chalkboard-teacher"></i>
                                </div>
                            </div>
                            <div class="flex-grow-1 ms-3">
                                <small class="text-uppercase text-muted fw-bold" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Pengajar") %></small>
                                <div class="fw-semibold text-dark"><%=listPengajar %></div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-5">
                         <div class="d-flex">
                            <div class="flex-shrink-0">
                                <div class="bg-warning-subtle text-warning-emphasis rounded-circle p-2 d-flex align-items-center justify-content-center" style="width: 40px; height: 40px;">
                                    <i class="fas fa-map-marker-alt"></i>
                                </div>
                            </div>
                            <div class="flex-grow-1 ms-3">
                                <small class="text-uppercase text-muted fw-bold" style="font-size: 0.7rem;"><%=Common.getBahasaConfig("Ruangan") %></small>
                                <div class="fw-semibold text-dark"><%=ruang %></div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="bg-light rounded-3 p-3 border-start border-4 border-primary mb-3">
                    <h6 class="fw-bold text-primary mb-1" style="font-size: 0.8rem; text-transform: uppercase;">
                        <%=Common.getBahasaConfig("Topik / Bahasan") %>
                    </h6>
                    <div class="text-dark"><%=topik %></div>
                </div>

                <% if(hasCatatan) { %>
                <div class="alert alert-warning border-0 bg-warning-subtle text-warning-emphasis d-flex align-items-start mb-0 rounded-3">
                    <i class="far fa-sticky-note mt-1 me-3 fa-lg"></i>
                    <div>
                        <div class="fw-bold mb-1"><%=Common.getBahasaConfig("Catatan") %></div>
                        <div class="small text-break"><%=catat %></div>
                    </div>
                </div>
                <% } %>

            </div>
        </div>
    </div>
</div>