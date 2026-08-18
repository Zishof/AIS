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
    
    // Variable Session/Koneksi (Sesuaikan dengan framework database Anda jika perlu deklarasi manual)
    // Contoh: Session dbSession = null; 

    try {
        // --- 1. VALIDASI INPUT ---
        String reqId = request.getParameter("pertemuan");
        if (reqId != null && !reqId.isEmpty()) {

            // --- 2. PENGAMBILAN DATA UTAMA ---
            Pertemuan p = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, reqId, true);

            if (p != null) {
                // --- 3. LOGIC & FORMATTING ---
                
                // Format Tanggal & Waktu
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

                // Data Perkuliahan (Deep Null Checking)
                Perkuliahan perkuliahan = p.getPerkuliahan();
                JadwalPelajaran jadwalPelajaran = p.getJadwalPelajaran();
                if (perkuliahan != null) {
                    Matakuliah mk = perkuliahan.getMatakuliah();
                    kodeMK = (mk != null) ? mk.getKode() : "";
                    namaMK = (mk != null) ? mk.getNama() : p.info(); // Fallback ke p.info() jika MK null
                    
                    ta = (perkuliahan.getTahunAjaran() != null) ? perkuliahan.getTahunAjaran() : "-";
                    smt = (perkuliahan.getGanjilGenap() != null) ? perkuliahan.getGanjilGenap() : "-";
                    kelas = (perkuliahan.getKelas() != null) ? perkuliahan.getKelas() : "-";
                    semAngka = (perkuliahan.getSemester() != null) ? perkuliahan.getSemester().toString() : "";
                } else if (jadwalPelajaran != null) {
                    Matapelajaran mk = jadwalPelajaran.getMatapelajaran();
                    kodeMK = (mk != null) ? mk.getKode() : "";
                    namaMK = (mk != null) ? mk.getNama() : p.info(); // Fallback ke p.info() jika MK null
                    
                    ta = (jadwalPelajaran.getTahunAjaran() != null) ? jadwalPelajaran.getTahunAjaran() : "-";
                    smt = (jadwalPelajaran.getSemester() != null) ? (jadwalPelajaran.getSemester().equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP) : "-";
                    kelas = (jadwalPelajaran.getKelas() != null) ? jadwalPelajaran.getKelas().getNama() : "-";
                    semAngka = (jadwalPelajaran.getSekolah() != null) ? jadwalPelajaran.getSekolah().getNama() : "";
                } else {
                    namaMK = p.info();
                }
                
                
                

                // --- 4. LOGIKA PENGAMBILAN DOSEN & GURU ---
                // Menggunakan StringBuilder untuk efisiensi memori
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
                            if (sbPengajar.length() > 0) sbPengajar.append(", "); // Kom pemisah jika sudah ada dosen
                            sbPengajar.append(g.getNama());
                        }
                    }
                }
                
                if (sbPengajar.length() > 0) {
                    listPengajar = sbPengajar.toString();
                }

                // --- 5. FORMAT CATATAN ---
                catat = p.getCatatan();
                hasCatatan = (catat != null && !catat.trim().isEmpty());
                if (hasCatatan) {
                    String linkText = Common.getBahasaConfig("Buka Tautan");
                    catat = catat.replaceAll("(https?://\\S+)", "<a target='_blank' class='text-decoration-underline fw-bold' href=\"$1\">" + linkText + " <i class='fas fa-external-link-alt small'></i></a>");
                }
            }
        }
    } catch (Exception e) {
        // Error handling silent atau log ke console server
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/_info_kehadiran_pertemuan.jsp:128");
    } finally {
        // --- 6. TUTUP SESSION / CLEANUP ---
        // Jika framework Anda membutuhkan penutupan session manual, lakukan di sini.
        // Contoh:
        // if (dbSession != null && dbSession.isOpen()) dbSession.close();
    }
    
    // Jika data tidak ditemukan setelah logic berjalan, return agar tidak merender HTML kosong/rusak
    if (kodeMK.isEmpty() && namaMK.isEmpty()) return;
%>

<div class="card shadow-sm border-0 rounded-4 overflow-hidden mb-4">
    
    <div class="card-header bg-primary bg-gradient text-white p-3 border-0">
        <div class="d-flex align-items-center">
            <div class="bg-white bg-opacity-25 rounded-circle p-2 me-3 d-flex align-items-center justify-content-center" style="width: 45px; height: 45px;">
                <i class="fas fa-book-open fa-lg"></i>
            </div>
            <div>
                <small class="text-white-50 text-uppercase fw-bold" style="font-size: 0.75rem;"><%=kodeMK %></small>
                <h5 class="mb-0 fw-bold text-white"><%=namaMK %></h5>
                <small class="opacity-75">
                    <%=Common.getBahasaConfig("Pertemuan ke-") %> 
                    <span class="badge bg-white text-primary rounded-pill"><%=pertemuanKe %></span>
                </small>
            </div>
        </div>
    </div>

    <div class="card-body p-0">
        <div class="p-3 bg-info-subtle border-bottom border-light">
            <div class="row align-items-center">
                <div class="col-md-5 mb-2 mb-md-0">
                    <div class="d-flex align-items-center text-info-emphasis">
                        <i class="far fa-calendar-alt fa-2x me-3 opacity-50"></i>
                        <div>
                            <small class="text-uppercase fw-bold opacity-75"><%=Common.getBahasaConfig("Waktu Pelaksanaan") %></small>
                            <div class="fw-bold"><%=tgl %></div>
                            <small><i class="far fa-clock me-1"></i> <%=wkt %></small>
                        </div>
                    </div>
                </div>
                <div class="col-md-7 border-start-md border-info-subtle">
                    <small class="text-uppercase fw-bold text-info-emphasis opacity-75"><%=Common.getBahasaConfig("Topik / Bahasan") %></small>
                    <div class="text-dark fw-semibold"><%=topik %></div>
                </div>
            </div>
        </div>

        <div class="p-3 bg-success-subtle border-bottom border-light">
            <div class="row gy-3">
                <div class="col-6 col-md-3">
                    <small class="text-muted d-block text-uppercase" style="font-size: 0.7rem;"><i class="fas fa-map-marker-alt me-1"></i> <%=Common.getBahasaConfig("Ruangan") %></small>
                    <span class="fw-bold text-success-emphasis"><%=ruang %></span>
                </div>
                <div class="col-6 col-md-3">
                    <small class="text-muted d-block text-uppercase" style="font-size: 0.7rem;"><i class="fas fa-graduation-cap me-1"></i> <%=Common.getBahasaConfig("Tahun Ajaran") %></small>
                    <span class="text-dark"><%=ta %> (<%=smt %>)</span>
                </div>
                <div class="col-6 col-md-3">
                    <small class="text-muted d-block text-uppercase" style="font-size: 0.7rem;"><i class="fas fa-users me-1"></i> <%=Common.getBahasaConfig("Kelas") %></small>
                    <span class="badge bg-success text-white rounded-pill px-2">Sem. <%=semAngka %></span> 
                    <span class="fw-bold ms-1 text-dark"><%=kelas %></span>
                </div>
                 <div class="col-6 col-md-3">
                    <small class="text-muted d-block text-uppercase" style="font-size: 0.7rem;"><i class="fas fa-chalkboard-teacher me-1"></i> <%=Common.getBahasaConfig("Pengajar") %></small>
                    <span class="fw-bold text-dark" style="font-size: 0.9rem;"><%=listPengajar %></span>
                </div>
            </div>
        </div>

        <% if(hasCatatan) { %>
        <div class="p-3 bg-warning-subtle">
            <div class="d-flex align-items-start">
                <i class="far fa-sticky-note text-warning-emphasis mt-1 me-3 fa-lg"></i>
                <div class="flex-grow-1">
                    <h6 class="fw-bold text-warning-emphasis mb-1"><%=Common.getBahasaConfig("Catatan") %></h6>
                    <div class="bg-white bg-opacity-50 p-2 rounded border border-warning-subtle text-dark small" style="border-left: 4px solid #ffc107 !important;">
                        <%=catat %>
                    </div>
                </div>
            </div>
        </div>
        <% } %>

    </div>
</div>