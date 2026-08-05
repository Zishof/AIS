<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.ui.util.SmartDateTimeUtil"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.database.model.PenjelasanBankSoal"%>
<%@page import="ais.action.master.SyaratUjianAction"%>
<%@page import="ais.database.model.Detailperkuliahan"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.SyaratUjian"%>
<%@page import="java.util.Date"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.ui.util.DiskusiUiHelper"%>
<%@page import="ais.database.model.BankSoalDetail"%>
<%@page import="ais.database.model.BankSoal"%>
<%@page import="java.util.HashSet"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.HasilUjianMahasiswaDetail"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.ui.util.MyHashMap"%>
<%@page import="java.util.Set"%>
<%@page import="ais.database.model.UjianPunyaSoal"%>
<%@page import="ais.action.master.helper.ProsesUjianHelper"%>
<%@page import="ais.ui.util.MyArrayList"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.HasilUjianMahasiswa"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="org.json.JSONArray" %>
<%@page import="org.json.JSONObject" %>
<%@page import="java.util.Collections" %>
<%@page import="java.util.ArrayList" %>
<%@page import="java.util.List" %>

<%!
    private static boolean isLampiranGambar(String nama, String url) {
        String s = ((nama == null ? "" : nama) + " " + (url == null ? "" : url)).toLowerCase();
        return s.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)(\\?.*)?$");
    }

    private static String renderLampiranSoal(LampiranLain lampiranLain, String label) throws Exception {
        if (lampiranLain == null) {
            return "";
        }
        String url = lampiranLain.createLinkUri();
        String nama = lampiranLain.getNama() == null || lampiranLain.getNama().trim().isEmpty()
                ? label
                : lampiranLain.getNama();
        String safeUrl = DiskusiUiHelper.escapeHtml(url);
        String safeNama = DiskusiUiHelper.escapeHtml(nama);
        String safeLabel = DiskusiUiHelper.escapeHtml(label);
        if (isLampiranGambar(nama, url)) {
            return "<div class=\"question-attachment mt-3\">"
                    + "<div class=\"small fw-bold text-secondary mb-2\"><i class=\"fas fa-paperclip me-1\"></i>"
                    + safeLabel + "</div>"
                    + "<img onclick=\"tampilGambar(this);\" src=\"" + safeUrl + "\""
                    + " class=\"question-attachment-img shadow-sm border border-2 border-white\""
                    + " title=\"" + safeNama + "\" alt=\"" + safeNama + "\">"
                    + "</div>";
        }
        return "<div class=\"question-attachment mt-3\">"
                + "<a class=\"btn btn-outline-primary btn-sm rounded-pill fw-bold\" href=\"" + safeUrl
                + "\" target=\"_blank\" rel=\"noopener noreferrer\">"
                + "<i class=\"fas fa-paperclip me-2\"></i>" + safeLabel + " - " + safeNama + "</a>"
                + "</div>";
    }
%>

<%
    // ==========================================
    // 1. OTENTIKASI & IDENTIFIKASI PESERTA
    // ==========================================
    Tbmuser tbmuser = Common.getCurrentUser(request);

    String biodataCalonMahasiswaParam = request.getParameter("biodataCalonMahasiswa");
    if(biodataCalonMahasiswaParam != null && !biodataCalonMahasiswaParam.trim().isEmpty()){
        BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, biodataCalonMahasiswaParam.trim(), true);
        if(biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null){
            tbmuser = new Tbmuser(biodataCalonMahasiswa);
        }
    }
    
    String mahasiswaParam = request.getParameter("mahasiswa");
    if(mahasiswaParam != null && !mahasiswaParam.trim().isEmpty()){
        Mahasiswa mahasiswa = (Mahasiswa) GeneralValueObject.ambilData(Mahasiswa.class, mahasiswaParam.trim(), true);
        if(mahasiswa != null && mahasiswa.getId() != null){
            tbmuser = new Tbmuser(mahasiswa);
        }
    }
    
    String siswaParam = request.getParameter("siswa");
    if(siswaParam != null && !siswaParam.trim().isEmpty()){
        Siswa siswa = (Siswa) GeneralValueObject.ambilData(Siswa.class, siswaParam.trim(), true);
        if(siswa != null && siswa.getId() != null){
            tbmuser = new Tbmuser(siswa);
        }
    }
    
    String calonSiswaParam = request.getParameter("calonSiswa");
    if(calonSiswaParam != null && !calonSiswaParam.trim().isEmpty()){
        CalonSiswa calonSiswa = (CalonSiswa) GeneralValueObject.ambilData(CalonSiswa.class, calonSiswaParam.trim(), true);
        if(calonSiswa != null && calonSiswa.getId() != null){
            tbmuser = new Tbmuser(calonSiswa);
        }
    }

    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return; 
    }

    String random = Common.getGeneratedBarCode(7);
    String ppuParam = request.getParameter("ppu");

    if (ppuParam == null || ppuParam.trim().isEmpty()) {
        out.println("<div class='alert alert-danger text-center mt-4 fw-bold shadow-sm'><i class='fas fa-exclamation-circle me-2'></i>" + Common.getBahasaConfig("Parameter sesi ujian tidak valid atau tidak ditemukan di dalam sistem.") + "</div>");
        return;
    }

    Boolean refreshData = request.getParameter("refresh") != null && request.getParameter("refresh").trim().equalsIgnoreCase("true");
    PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) GeneralValueObject.ambilData(PertemuanPunyaUjian.class, ppuParam.trim(), true);
    
    if (pertemuanPunyaUjian != null) {
        HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(
                pertemuanPunyaUjian, tbmuser.getMahasiswa(),
                tbmuser.getBiodataCalonMahasiswa(), tbmuser.getSiswa(), tbmuser.getCalonSiswa());
                
        // ==========================================
        // 2. MANAJEMEN WAKTU & STATUS UJIAN
        // ==========================================
        boolean hasMulai = (pertemuanPunyaUjian.getMulaiUjian() != null);
        boolean hasSelesai = (pertemuanPunyaUjian.getSampaiUjian() != null);
        
        long waktuSekarang = WaktuUtil.getDate().getTime();
        long waktuMulai = hasMulai ? pertemuanPunyaUjian.getMulaiUjian().getTime() : 0;
        long waktuSelesai = hasSelesai ? pertemuanPunyaUjian.getSampaiUjian().getTime() : 0;

        boolean isBelumMulai = hasMulai && (waktuSekarang < waktuMulai);
        boolean isSelesai = hasSelesai && (waktuSekarang > waktuSelesai);
        boolean isBerlangsung = !isBelumMulai && !isSelesai;

        // UI Config Vars
        String cardClass = "border-primary";
        String iconStatus = "fa-clock";
        String textStatus = Common.getBahasaConfig("Sedang Berlangsung");
        String textDesc = Common.getBahasaConfig("Silakan kerjakan ujian ini sebelum batas waktu berakhir.");
        String badgeColor = "bg-primary";
        
        if (isBelumMulai) {
            cardClass = "border-secondary";
            iconStatus = "fa-hourglass-start";
            textStatus = Common.getBahasaConfig("Belum Dimulai");
            textDesc = Common.getBahasaConfig("Tugas belum dibuka untuk pengerjaan.");
            badgeColor = "bg-secondary";
        } else if (isSelesai) {
            cardClass = "border-danger";
            iconStatus = "fa-lock";
            textStatus = Common.getBahasaConfig("Telah Selesai");
            textDesc = Common.getBahasaConfig("Batas waktu pengerjaan telah berakhir.");
            badgeColor = "bg-danger";
        }
        
        String strMulai = !hasMulai ? "-" : (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null) + " " + Common.dateFormat5.get().format(pertemuanPunyaUjian.getMulaiUjian()));
        String strSelesai = !hasSelesai ? "-" : (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null) + " " + Common.dateFormat5.get().format(pertemuanPunyaUjian.getSampaiUjian()));

        Integer batasMaksimal = pertemuanPunyaUjian.getJumlahBolehIkut() == null ? 0 : pertemuanPunyaUjian.getJumlahBolehIkut();
        Integer jumlahTelahIkut = (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getJumlahIkut() != null) ? hasilUjianMahasiswa.getJumlahIkut() : 0;
        
        if (hasilUjianMahasiswa != null && jumlahTelahIkut >= batasMaksimal) {
            String pesanBatas = Common.getBahasaConfig("Anda telah mengikuti ujian ini sebanyak {1} kali. Jumlah tersebut telah mencapai atau melewati batas maksimal yang diizinkan, yaitu {2} kali.")
                    .replace("{1}", jumlahTelahIkut.toString())
                    .replace("{2}", batasMaksimal.toString());
            out.println("<div class='alert alert-warning text-center mt-4 fw-bold shadow-sm'><i class='fas fa-exclamation-triangle me-2'></i>" + pesanBatas + "</div>");
            return;
        }
        
        // ==========================================
        // 3. PENGUJIAN SYARAT UJIAN (FILTER KETAT)
        // ==========================================
        StringBuilder syaratWarnings = new StringBuilder();
        if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getMahasiswa() != null
                && hasilUjianMahasiswa.getMahasiswa().getJurusan() != null
                && pertemuanPunyaUjian.getPertemuan() != null
                && pertemuanPunyaUjian.getPertemuan().getStatusPertemuan() != null
                && pertemuanPunyaUjian.getUjian() != null) {
            
            Session sessionSyarat = null;
            List<String> warnings = new ArrayList<String>();
            
            try {
                sessionSyarat = HibernateUtil.openSession();
                List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
                    sessionSyarat.createCriteria(SyaratUjian.class)
                        .add(Restrictions.or(Restrictions.isNull("fakultas"), Restrictions.eq("fakultas", hasilUjianMahasiswa.getMahasiswa().getJurusan().getFakultas())))
                        .add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", hasilUjianMahasiswa.getMahasiswa().getJurusan())))
                        .add(Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", hasilUjianMahasiswa.getMahasiswa().getProgram())))
                        .add(Restrictions.eq("statusPertemuan", pertemuanPunyaUjian.getPertemuan().getStatusPertemuan())),
                    SyaratUjian.class);
                    
                List<SyaratUjian> syaratUjiansGlobalUjian = ConstantValues.simpleList(
                        sessionSyarat.createCriteria(SyaratUjian.class).add(Restrictions.eq("berlakuUntukSemuaUjian", true)),
                        SyaratUjian.class);
                
                if (!syaratUjiansGlobalUjian.isEmpty()) {
                    syaratUjians.addAll(syaratUjiansGlobalUjian);
                }

                if (syaratUjians != null && !syaratUjians.isEmpty()) {
                    Detailperkuliahan detailperkuliahan = hasilUjianMahasiswa.getMahasiswa().ambilDetailperkuliahan(pertemuanPunyaUjian.getPertemuan().getPerkuliahan());
                    if (detailperkuliahan == null) {
                        hasilUjianMahasiswa.getMahasiswa().reInitDetailperkuliahan(sessionSyarat);
                        detailperkuliahan = hasilUjianMahasiswa.getMahasiswa().ambilDetailperkuliahan(pertemuanPunyaUjian.getPertemuan().getPerkuliahan());
                    }
                    
                    if (detailperkuliahan != null) {
                        for (SyaratUjian syaratUjian : syaratUjians) {
                            if (!SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, pertemuanPunyaUjian.getPertemuan().ambilVOPembelajaran(),
                                    hasilUjianMahasiswa.getMahasiswa(), detailperkuliahan.getSemester(), pertemuanPunyaUjian.getUjian().getNama(), warnings)) {
                                
                                String warningData = "";
                                for(String s : warnings) {
                                    warningData += warningData.isEmpty() ? s : "<br>" + s;
                                }
                                syaratWarnings.append(warningData);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:199");
            } finally {
                // Menutup Session secara aman di dalam blok finally
                if (sessionSyarat != null && sessionSyarat.isOpen()) {
                    try { sessionSyarat.clear(); sessionSyarat.disconnect(); sessionSyarat.close(); } catch(Exception ex) { ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:203"); }
                }
                HibernateUtil.closeSession();
            }
        }
%>

<% if (syaratWarnings.length() > 0) { %>
<div class="container py-3">
    <div class="alert border-0 shadow-sm rounded-4 p-4 mb-0"
         style="background:#fef2f2; border-left:5px solid #ef4444 !important;">
        <div class="d-flex align-items-start">
            <i class="fas fa-exclamation-triangle fs-2 text-danger me-3 mt-1 flex-shrink-0"></i>
            <div>
                <h6 class="fw-bold text-danger mb-2" style="font-size:1rem;">
                    &#9888; Perhatian: Ada Kewajiban/Syarat yang Belum Terpenuhi
                </h6>
                <div class="text-danger lh-lg"><%= syaratWarnings.toString() %></div>
                <div class="mt-2 text-muted small">
                    Anda tetap dapat mencoba mengikuti ujian, namun pastikan semua syarat dipenuhi
                    sebelum batas waktu berakhir.
                </div>
            </div>
        </div>
    </div>
</div>
<% } %>

<div id="intro-screen" class="container py-5">
    <div class="card shadow border-0 rounded-4 overflow-hidden <%= cardClass %>">
        
        <div class="card-header bg-primary text-white p-4 border-0">
            <h4 class="mb-0 fw-bold d-flex align-items-center">
                <i class="fas fa-clipboard-check me-3 fs-3"></i> 
                <%= pertemuanPunyaUjian.getUjian().getNama() %>
            </h4>
        </div>

        <div class="card-body p-4 p-md-5">
            <% if (pertemuanPunyaUjian.getUjian().getTatatertibUjian() != null && !pertemuanPunyaUjian.getUjian().getTatatertibUjian().trim().isEmpty()) { %>
                <div class="mb-4">
                    <h6 class="fw-bold text-secondary text-uppercase mb-3">
                        <i class="fas fa-file-alt me-2"></i> <%= Common.getBahasaConfig("Tata Tertib Ujian") %>
                    </h6>
                    <div class="p-4 bg-light rounded-3 border-start border-4 border-primary text-dark shadow-sm">
                        <%= pertemuanPunyaUjian.getUjian().getTatatertibUjian().replaceAll("font-size:9px;", "") %>
                    </div>
                </div>
            <% } %>

            <div class="alert alert-warning d-flex align-items-center mb-5 shadow-sm border-0 rounded-3" role="alert">
                <i class="fas fa-exclamation-triangle fs-3 me-4 text-warning"></i>
                <div class="lh-base">
                    <%= Common.getBahasaConfig("Ujian ini memiliki batas maksimal {1} kali percobaan. Saat ini Anda telah melakukan {2} percobaan. Pengecualian hanya berlaku dengan persetujuan dari pengawas ujian.")
                        .replace("{1}", "<strong>" + batasMaksimal.toString() + "</strong>")
                        .replace("{2}", "<strong>" + jumlahTelahIkut.toString() + "</strong>") %>
                </div>
            </div>
            
            <h6 class="fw-bold text-secondary text-uppercase mb-3">
                <i class="far fa-clock me-2"></i> <%= Common.getBahasaConfig("Jadwal Pelaksanaan") %>
            </h6>
            
            <div class="row g-3 mb-2">
                <div class="col-md-6">
                    <div class="d-flex align-items-center p-3 bg-light rounded-3 border shadow-sm hover-shadow">
                        <div class="bg-white p-3 rounded-circle shadow-sm me-3 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                            <i class="far fa-calendar-alt fs-4 <%= isBelumMulai ? "text-danger" : "text-primary" %>"></i>
                        </div>
                        <div>
                            <small class="text-muted d-block text-uppercase fw-bold mb-1" style="font-size: 0.75rem;">
                                <%= Common.getBahasaConfig("Waktu Mulai") %>
                            </small>
                            <span class="fw-bold fs-6 <%= isBelumMulai ? "text-danger" : "text-dark" %>">
                                <%= strMulai %>
                            </span>
                        </div>
                    </div>
                </div>
                
                <div class="col-md-6">
                    <div class="d-flex align-items-center p-3 bg-light rounded-3 border shadow-sm hover-shadow">
                        <div class="bg-white p-3 rounded-circle shadow-sm me-3 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                            <i class="fas fa-flag-checkered fs-4 <%= isSelesai ? "text-danger" : (isBerlangsung ? "text-primary" : "text-dark") %>"></i>
                        </div>
                        <div>
                            <small class="text-muted d-block text-uppercase fw-bold mb-1" style="font-size: 0.75rem;">
                                <%= Common.getBahasaConfig("Batas Waktu Akhir") %>
                            </small>
                            <span class="fw-bold fs-6 <%= isSelesai ? "text-danger" : (isBerlangsung ? "text-primary" : "text-dark") %>">
                                <%= strSelesai %>
                            </span>
                        </div>
                    </div>
                </div>
            </div>

        </div>
        
        <div class="card-footer bg-white p-4 text-end border-top">
            <% if (isBerlangsung) { %>
                <button type="button" class="btn btn-primary btn-lg px-5 fw-bold shadow hover-shadow" onclick="startExam<%=random%>()">
                    <i class="fas fa-play-circle me-2"></i> <%= Common.getBahasaConfig("Mulai Mengerjakan Ujian") %>
                </button>
            <% } else if (isBelumMulai) { %>
                <button class="btn btn-secondary btn-lg px-5 fw-bold disabled opacity-75" disabled>
                    <i class="fas fa-lock me-2"></i> <%= Common.getBahasaConfig("Ujian Belum Dimulai") %>
                </button>
            <% } else { %>
                <button class="btn btn-danger btn-lg px-5 fw-bold disabled opacity-75" disabled>
                    <i class="fas fa-times-circle me-2"></i> <%= Common.getBahasaConfig("Ujian Telah Berakhir") %>
                </button>
            <% } %>
        </div>
    </div>
</div>

<% 
// ==============================================================================
// 4. PENGAMBILAN & PENYUSUNAN SOAL UJIAN (HANYA JIKA SEDANG BERLANGSUNG)
// ==============================================================================
if (isBerlangsung) { 
    
    // Pembaruan Informasi Sesi Mahasiswa
    if (hasilUjianMahasiswa != null) {
        boolean perluUpdate = false;
        
        if (hasilUjianMahasiswa.getMulaiPada() == null && (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getPesertaKursus() != null || tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)) {
            hasilUjianMahasiswa.setMulaiPada(WaktuUtil.getDate());
            perluUpdate = true;
        }

        hasilUjianMahasiswa.setJumlahIkut(jumlahTelahIkut + 1); 
        hasilUjianMahasiswa.setLengkapiJawaban(false);
        
        Session sessionUpdateHum = null;
        try {
            sessionUpdateHum = HibernateUtil.openSession();
            sessionUpdateHum.getTransaction().begin();
            sessionUpdateHum.update(hasilUjianMahasiswa);
            sessionUpdateHum.getTransaction().commit();
        } catch (Exception e) {
            if (sessionUpdateHum != null && sessionUpdateHum.getTransaction().isActive()) {
                sessionUpdateHum.getTransaction().rollback();
            }
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:327");
        } finally {
            if (sessionUpdateHum != null && sessionUpdateHum.isOpen()) {
                try { sessionUpdateHum.clear(); sessionUpdateHum.disconnect(); sessionUpdateHum.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:330");}
            }
            HibernateUtil.closeSession();
        }
    }

    MyArrayList<Long> ujianPunyaSoals = null;
    try {
        if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getId() != null) {
            ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(pertemuanPunyaUjian.getJmlDitampilkan(), null, refreshData);
            if (ujianPunyaSoals.size() != pertemuanPunyaUjian.getJmlDitampilkan() && pertemuanPunyaUjian.getId() != null) {
                List<Long> ujianPunyaSoalsTemp = pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal(pertemuanPunyaUjian, false);
                ujianPunyaSoals = ProsesUjianHelper.randomPosisiton(ujianPunyaSoalsTemp, pertemuanPunyaUjian.getRandom(), null, pertemuanPunyaUjian.getJmlDitampilkan());
            }
        } else {
            List<Long> ujianPunyaSoalsTemp = pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal(pertemuanPunyaUjian, refreshData);
            ujianPunyaSoals = ProsesUjianHelper.randomPosisiton(ujianPunyaSoalsTemp, pertemuanPunyaUjian.getRandom(), null, pertemuanPunyaUjian.getJmlDitampilkan());
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:349");
    }
    
    if (ujianPunyaSoals == null || ujianPunyaSoals.isEmpty()) {
        out.println("<div class='alert alert-warning text-center mt-4 fw-bold shadow-sm'><i class='fas fa-info-circle me-2'></i>" + Common.getBahasaConfig("Soal ujian belum tersedia untuk sesi ini.") + "</div>");
        return;
    }
    
    MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = null;
    if (hasilUjianMahasiswa != null) {
        hasilUjianMahasiswaDetailsa = hasilUjianMahasiswa.ambilHasilUjianMahasiswaDetail(true, pertemuanPunyaUjian.getJmlDitampilkan(), null, ujianPunyaSoals);
        List<HasilUjianMahasiswaDetail> missingDetailsToSave = new ArrayList<HasilUjianMahasiswaDetail>();
        
        for (Long ujianPunyaSoalid : ujianPunyaSoals) {
            UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, ujianPunyaSoalid.toString());
            if (ujianPunyaSoal != null && ujianPunyaSoal.getBankSoal() != null) {
                BankSoal currentBankSoal = ujianPunyaSoal.getBankSoal();
                Set<Long> s = hasilUjianMahasiswaDetailsa.get(currentBankSoal.getId());
                
                if (s == null || s.isEmpty()) {
                    HasilUjianMahasiswaDetail myDetail = new HasilUjianMahasiswaDetail();
                    myDetail.setBankSoal(currentBankSoal);
                    myDetail.setHasilUjianMahasiswa(hasilUjianMahasiswa);
                    myDetail.setUjianPunyaSoal(ujianPunyaSoal);
                    myDetail.setNilai(currentBankSoal.getSkorDefault());
                    missingDetailsToSave.add(myDetail);
                }
            }
        }
    
        if (!missingDetailsToSave.isEmpty()) {
            Session batchSession = null;
            try {
                batchSession = HibernateUtil.openSession();
                batchSession.getTransaction().begin();
                for (HasilUjianMahasiswaDetail detail : missingDetailsToSave) {
                    batchSession.save(detail);
                    Set<Long> updatedSet = new HashSet<Long>();
                    updatedSet.add(detail.getId());
                    hasilUjianMahasiswaDetailsa.put(detail.getBankSoal().getId(), updatedSet);
                }
                batchSession.getTransaction().commit();
            } catch (Exception e) {
                if (batchSession != null && batchSession.getTransaction().isActive()) {
                    batchSession.getTransaction().rollback();
                }
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:395");
            } finally {
                if (batchSession != null && batchSession.isOpen()) {
                    try { batchSession.clear(); batchSession.disconnect(); batchSession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:398");}
                }
                HibernateUtil.closeSession();
            }
        }
    }
    
    // Penyusunan Data Soal untuk Antarmuka JS
    JSONArray examData = new JSONArray();
    for (Long ujianPunyaSoalid : ujianPunyaSoals) {
        UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, ujianPunyaSoalid.toString());
        if (ujianPunyaSoal != null && ujianPunyaSoal.getBankSoal() != null) {
            BankSoal currentBankSoal = ujianPunyaSoal.getBankSoal();
            JSONObject question = new JSONObject();
            question.put("id", ujianPunyaSoal.getId());
            String soalHtml = currentBankSoal.getSoal() == null ? "" : currentBankSoal.getSoal();
            String[] jenisLampiranSoal = new String[] { "Soal I", "Soal II", "Soal III", "Soal IV", "Soal V" };
            for (int lampiranIndex = 0; lampiranIndex < jenisLampiranSoal.length; lampiranIndex++) {
                try {
                    LampiranLain lampiranSoal = LampiranLain.ambil(currentBankSoal.getId(), jenisLampiranSoal[lampiranIndex]);
                    soalHtml += renderLampiranSoal(lampiranSoal, "Lampiran " + jenisLampiranSoal[lampiranIndex]);
                } catch (Exception e) {
                    ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:render-lampiran-soal");
                }
            }
            question.put("text", soalHtml);
            
            int skorBenar = currentBankSoal.getSkor() != null ? currentBankSoal.getSkor().intValue() : 0;
            int skorSalah = currentBankSoal.getSkorSalah() != null ? currentBankSoal.getSkorSalah().intValue() : 0;
            question.put("pointCorrect", skorBenar);
            question.put("pointWrong", skorSalah);  
            
            Long detailIdQuestion = null;
            if (hasilUjianMahasiswaDetailsa != null) {
                Set<Long> detailIds = hasilUjianMahasiswaDetailsa.get(currentBankSoal.getId());
                if (detailIds != null && !detailIds.isEmpty()) {
                    detailIdQuestion = detailIds.iterator().next();
                }
            }
            question.put("detailId", detailIdQuestion == null ? "" : detailIdQuestion);
            
            HasilUjianMahasiswaDetail cekDetail = null;
            if (hasilUjianMahasiswaDetailsa != null && detailIdQuestion != null) {
                cekDetail = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, detailIdQuestion.toString());
            }
            question.put("cekDetail", cekDetail == null ? "" : cekDetail.getId());
            question.put("koreksi", currentBankSoal.getJenisKoreksi());
            boolean hasAnswer = false; 

            // -- KOREKSI OTOMATIS (Pilihan Ganda) --
            if(PenjelasanBankSoal.KOREKSI_OTOMATIS.equals(currentBankSoal.getJenisKoreksi())) {
                JSONArray options = new JSONArray();
                List<JSONObject> optList = new ArrayList<JSONObject>();
                
                List<Long> bankSoalDetails = currentBankSoal.ambilBankSoalDetail(refreshData);
                if (bankSoalDetails != null) {
                    for (Long bankSoalDetailid : bankSoalDetails) {
                        BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
                        if (bankSoalDetail != null) {                       
                            JSONObject opt = new JSONObject();
                            opt.put("id", bankSoalDetail.getId());
                            opt.put("isCorrect", bankSoalDetail.getBetul()); 
                            
                            boolean isTerpilih = false;
                            if (cekDetail != null && cekDetail.getBankSoalDetail() != null && cekDetail.getBankSoalDetail().getId().equals(bankSoalDetail.getId())) {
                                isTerpilih = true;
                                hasAnswer = true;
                            }
                            opt.put("terpilih", isTerpilih);
                            
                            LampiranLain lampiranLain = LampiranLain.ambil(currentBankSoal.getId(), "Gambar_Jawaban_" + bankSoalDetail.getHuruf());
                            String jawaban = bankSoalDetail.getJawaban() != null ? bankSoalDetail.getJawaban() : "";
                            if (lampiranLain != null) {
                                jawaban += "<img onclick=\"tampilGambar(this);\" src=\"" + lampiranLain.createLinkUri() + "\" class=\"shadow-sm border border-2 border-white mt-2\" style=\"object-fit: cover; cursor: pointer; border-radius: 5px;\" title=\"" + lampiranLain.getNama() + "\" alt=\"Foto\">";
                            }
                            
                            opt.put("text", jawaban);
                            optList.add(opt);
                        }
                    }
                }
                
                Collections.shuffle(optList);
                for (JSONObject o : optList) options.put(o);
                question.put("options", options);
                
            } 
            // -- KOREKSI MANUAL (Esai) --
            else if(PenjelasanBankSoal.KOREKSI_MANUAL.equals(currentBankSoal.getJenisKoreksi())) {
                String savedAnswer = "";
                Long placeholderDetailId = 0L;
                
                List<Long> bankSoalDetails = currentBankSoal.ambilBankSoalDetail(refreshData);
                if (bankSoalDetails != null && !bankSoalDetails.isEmpty()) {
                    for (Long bankSoalDetailid : bankSoalDetails) {
                        BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
                        if (bankSoalDetail != null) {   
                             placeholderDetailId = bankSoalDetail.getId();
                             break; 
                        }
                    }
                }
                
                if (cekDetail != null && cekDetail.getJawaban() != null && !cekDetail.getJawaban().trim().isEmpty()) {
                    savedAnswer = cekDetail.getJawaban();
                }

                if (savedAnswer != null && !savedAnswer.trim().isEmpty()) {
                    hasAnswer = true;
                }

                question.put("answer", savedAnswer);
                question.put("bankSoalDetailId", placeholderDetailId);
            }
            
            question.put("hasAnswer", hasAnswer);
            examData.put(question);
        }
    }
    
    // ==============================================================================
    // 5. PERHITUNGAN WAKTU YANG AMAN (MENCEGAH NUMBER FORMAT EXCEPTION)
    // ==============================================================================
    String linkSimpan = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_update_ujian";
    String linkHasil = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=_hasil_ujian";
    
    Date lama = (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null) ? pertemuanPunyaUjian.getLama() : hasilUjianMahasiswa.getSisaWaktuPengerjaan();
    
    int jam = 0, menit = 30, detik = 0; // Waktu standar bawaan
    
    try {
        if (lama != null) {
            String waktuFormat = Common.timeFormat1.get().format(lama);
            if (waktuFormat.contains(":")) {
                String[] bagian = waktuFormat.split(":");
                if (bagian.length >= 3) {
                    jam = Integer.parseInt(bagian[0]);
                    menit = Integer.parseInt(bagian[1]);
                    detik = Integer.parseInt(bagian[2]);
                } else if (bagian.length == 2) {
                    menit = Integer.parseInt(bagian[0]);
                    detik = Integer.parseInt(bagian[1]);
                }
            }
        }
    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:533");
        // Abaikan galat pemformatan, sistem akan menggunakan waktu standar (00:30:00)
    }
    
    int totalDetikSisa = (jam * 3600) + (menit * 60) + detik;
    String formatSisaWaktu = String.format("%02d:%02d:%02d", jam, menit, detik);
    
    String placeholderUjian = Common.getBahasaConfig("Ketikkan jawaban Anda di sini...");
    Boolean blokirTangkap = pertemuanPunyaUjian.getAntiCurangBlokirTangkapLayar();
    boolean doBlokirTangkap = (blokirTangkap != null) ? blokirTangkap.booleanValue() : true;
%>

<% if (doBlokirTangkap) { %>
<script>
(function() {
    // Blokir PrintScreen / Screenshot (aktif sejak halaman intro, sebelum klik Mulai)
    document.addEventListener('keydown', function(e) {
        if (e.key === 'PrintScreen' || e.keyCode === 44) {
            try { document.execCommand('copy'); } catch(x) {}
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText('').catch(function() {});
            }
        }
    });
    document.addEventListener('keyup', function(e) {
        if (e.key === 'PrintScreen' || e.keyCode === 44) {
            try { document.execCommand('copy'); } catch(x) {}
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText('').catch(function() {});
            }
        }
    });
    // Nonaktifkan text selection untuk mencegah copy soal
    document.body.style.userSelect = 'none';
    document.body.style.webkitUserSelect = 'none';
    document.body.style.msUserSelect = 'none';
})();
</script>
<% } %>

<style>
    .question-rich-content {
        color: #1f2937;
        font-size: 1.1rem;
        line-height: 1.65;
        user-select: none;
        -webkit-user-select: none;
    }
    .question-rich-content p {
        margin-bottom: .75rem;
    }
    .question-rich-content table {
        width: auto;
        max-width: 100%;
        border-collapse: collapse;
        margin: .75rem 0;
        display: block;
        overflow-x: auto;
    }
    .question-rich-content th,
    .question-rich-content td {
        border: 1px solid #cbd5e1;
        padding: .45rem .6rem;
        vertical-align: top;
    }
    .question-rich-content img,
    .question-attachment-img {
        display: block;
        max-width: 100%;
        height: auto;
        object-fit: contain;
        border-radius: 8px;
        cursor: pointer;
    }
    .answer-rich-content img {
        max-width: min(100%, 280px);
        height: auto;
        object-fit: contain;
    }
</style>

<div id="exam-screen" class="container py-4 d-none">
    <div class="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
        <h5 class="mb-0 fw-bold text-dark"><i class="fas fa-file-signature me-2 text-primary"></i><%= Common.getBahasaConfig("Lembar Ujian") %></h5>
        <h5 class="text-danger fw-bold mb-0 shadow-sm px-3 py-2 bg-light border border-danger rounded-pill">
            <i class="fas fa-stopwatch me-2"></i> <%= Common.getBahasaConfig("Sisa Waktu") %> : <span id="timer-display"><%= formatSisaWaktu %></span>
        </h5>
    </div>

    <form id="examForm">
        <% 
            for (int i = 0; i < examData.length(); i++) {
                JSONObject q = examData.getJSONObject(i);
                String jawab = q.optString("answer", "");
                String detailId = q.optString("detailId", "");
                String cekDetail = q.optString("cekDetail", "");
                String soalId = q.optString("id","")+"";
                String bankSoalDetailId = q.optString("bankSoalDetailId","")+"";
                String name = "answer_"+i+"_"+random;
                String onchange = "saveAnswer"+random+"(document.getElementById('"+name+"'), "+i+");";
                String jenis = "jenis_ujian_"+(cekDetail == null || cekDetail.trim().isEmpty() ? "soal_"+soalId : "soal_diujikan_"+cekDetail );
        %>
        <div id="question-<%= i %>" class="question-page d-none animate__animated animate__fadeIn">
            <div class="card shadow-sm border-0 mb-4 rounded-4">
                <div class="card-body p-4">
                    <div class="question-rich-content"><%= q.getString("text") %></div>
                </div>
            </div>

            <div class="card shadow-sm border-0 mb-4 rounded-4 overflow-hidden">
                <div class="card-header bg-secondary text-white fw-bold py-3">
                    <i class="fas fa-pen-nib me-2"></i>
                    <%= PenjelasanBankSoal.KOREKSI_MANUAL.equals(q.getString("koreksi")) 
                        ? Common.getBahasaConfig("Silakan tuliskan jawaban Anda pada kolom di bawah ini:") 
                        : Common.getBahasaConfig("Silakan pilih salah satu jawaban yang paling tepat:") %>
                </div>
                
                <div class="card-body p-0">
                    <% if (PenjelasanBankSoal.KOREKSI_MANUAL.equals(q.getString("koreksi"))) { %>
                        <div class="p-4 bg-light">
                            <jsp:include page="/WEB-INF/baru/modul/common/text_area.jsp">
		                        <jsp:param name="placeholder" value="<%= placeholderUjian %>" />
		                        <jsp:param name="idtextarea" value="<%= name %>" />
		                        <jsp:param name="jenis" value="<%= jenis %>" />
		                        <jsp:param name="isi" value="<%= jawab %>" />
		                        <jsp:param name="namatextarea" value="<%= name %>" />
		                        <jsp:param name="data-detailid" value="<%= detailId %>" />
		                        <jsp:param name="data-ppu" value="<%= pertemuanPunyaUjian.getId() %>" />
		                        <jsp:param name="data-ujianpunyasoalid" value="<%= soalId %>" />
		                        <jsp:param name="data-banksoaldetailid" value="<%= bankSoalDetailId %>" />
		                        <jsp:param name="onchange" value="<%= onchange %>" />
		                        <jsp:param name="isGenerate" value="false" />
		                    </jsp:include>          
                        </div>
                    <% } else { %>
                        <div class="list-group list-group-flush">
					    <% 
					        JSONArray opts = q.optJSONArray("options");
					        if (opts != null) {
					            String detailIdStr = q.optString("detailId", "");
					            String soalIdStr = q.optString("id", "0");
					            String pointCorrectStr = q.optString("pointCorrect", "0");
					            String pointWrongStr = q.optString("pointWrong", "0");
					            String ppuIdStr = (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getId() != null) ? pertemuanPunyaUjian.getId().toString() : "";
					            
					            for (int j = 0; j < opts.length(); j++) { 
					                try{
					                	JSONObject opt = opts.getJSONObject(j);
						                String optIdStr = opt.optString("id", "0");
						                String isCorrectStr = opt.optString("isCorrect", "false");
						                String textStr = opt.optString("text", "");
						                String terpilihStr = opt.optString("terpilih", "false");
						                String checkedAttr = "true".equalsIgnoreCase(terpilihStr) ? "checked" : "";
									    %>
									    <label class="list-group-item list-group-item-action p-3 d-flex align-items-center cursor-pointer" style="transition: all 0.2s;">
									        <input class="form-check-input me-3 border-secondary" style="transform: scale(1.2);" type="radio" 
									               name="answer_<%= i %>" 
									               value="<%= optIdStr %>"
									               data-detailid="<%= detailIdStr %>"
									               data-ppu="<%= ppuIdStr %>"
									               data-banksoaldetailid="<%= optIdStr %>"
									               data-ujianpunyasoalid="<%= soalIdStr %>"
									               data-is-correct="<%= isCorrectStr %>"
									               data-point-correct="<%= pointCorrectStr %>"
									               data-point-wrong="<%= pointWrongStr %>"
									               <%= checkedAttr %>
									               onchange="saveAnswer<%=random%>(this, <%= i %>)">
									        <span class="fs-6 answer-rich-content"><%= textStr %></span>
									    </label>
									    <% 
					                }catch(Exception e){
					                	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/ujian.jsp:631");
					                }
					            }
					        } 
					    %>
					    </div>
                    <% } %>
                </div>
            </div>
        </div>
        <% } %>
    </form>

    <div class="card shadow-sm border-0 mt-3 rounded-4 overflow-hidden">
        <div class="card-body bg-light p-3 p-md-4 border-top">
		    <div class="row g-3 align-items-center">
		        
		        <div class="col-12 col-md-8">
		            <div class="row g-2">
		                <div class="col-6 col-md-auto d-grid">
		                    <button type="button" class="btn btn-outline-secondary rounded-pill px-4 py-2 fw-bold" id="btn-prev" onclick="navigate<%=random%>(-1)">
		                        <i class="fas fa-arrow-left me-2"></i> <%= Common.getBahasaConfig("Sebelumnya") %>
		                    </button>
		                </div>
		                <div class="col-6 col-md-auto d-grid">
		                    <button type="button" class="btn btn-primary rounded-pill px-4 py-2 fw-bold shadow-sm" id="btn-next" onclick="navigate<%=random%>(1)">
		                        <%= Common.getBahasaConfig("Selanjutnya") %> <i class="fas fa-arrow-right ms-2"></i>
		                    </button>
		                </div>
		            </div>
		        </div>
		        
		        <div class="col-12 col-md-4 d-grid d-md-flex justify-content-md-end">
		            <button type="button" class="btn btn-danger rounded-pill px-5 py-2 fw-bold shadow" onclick="finishExam<%=random%>()">
		                <i class="fas fa-check-double me-2"></i> <%= Common.getBahasaConfig("Akhiri Ujian") %>
		            </button>
		        </div>
		        
		    </div>
		</div>
  
        <div class="card-footer bg-white p-4">
            <div class="d-flex flex-wrap gap-2 justify-content-center">
                <% 
                for (int i = 0; i < examData.length(); i++) { 
                    JSONObject qObj = examData.getJSONObject(i);
                    boolean isAnswered = qObj.optBoolean("hasAnswer", false);
                    String badgeClass = isAnswered ? "btn-primary text-white" : "btn-outline-secondary";
                %>
                    <button type="button" id="nav-badge-<%= i %>" class="btn <%= badgeClass %> rounded-circle fw-bold shadow-sm" style="width: 42px; height: 42px; display: flex; align-items: center; justify-content: center; transition: all 0.2s;" onclick="goToQuestion<%=random%>(<%= i %>)"><%= (i + 1) %></button>
                <% } %>
            </div>
        </div>
    </div>
</div>

<div id="result-screen" class="container py-5 d-none text-center animate__animated animate__fadeInUp">
    <div class="row justify-content-center">
        <div class="col-md-6">
            <div class="card shadow-lg border-0 rounded-4 p-5">
                <i class="fas fa-trophy text-warning" style="font-size: 5rem;"></i>
                <h2 class="mt-4 fw-bold text-dark"><%= Common.getBahasaConfig("Ujian Telah Berakhir!") %></h2>
                
                <% if(pertemuanPunyaUjian.getLihatNilaiSetelahUjian()) { %>
                    <p class="text-muted fs-5"><%= Common.getBahasaConfig("Berikut adalah ringkasan hasil evaluasi ujian Anda.") %></p>
                    
                    <div class="bg-light p-4 rounded-4 mt-4 mb-4 shadow-sm border">
                        <h1 class="display-1 text-primary fw-bold mb-0" id="final-score">0</h1>
                        <span class="text-muted fw-bold text-uppercase" style="letter-spacing:1px;"><%= Common.getBahasaConfig("Skor Akhir") %></span>
                    </div>

                    <div class="d-flex justify-content-around text-muted mt-3">
                        <div class="p-3">
                            <span class="fw-bold fs-4 text-success" id="total-correct">0 / 0</span><br>
                            <small class="fw-bold text-uppercase"><%= Common.getBahasaConfig("Jawaban Benar") %></small>
                        </div>
                        <div class="p-3 border-start">
                            <span class="fw-bold fs-4 text-dark" id="total-questions">0</span><br>
                            <small class="fw-bold text-uppercase"><%= Common.getBahasaConfig("Total Soal") %></small>
                        </div>
                        <div style="display: none;">
                            <span class="fw-bold fs-5 text-info" id="score-obe">0</span><br><%= Common.getBahasaConfig("Nilai OBE") %>
                        </div>
                    </div>
                <% } else { %>
                    <div class="alert alert-success mt-4 shadow-sm">
                        <i class="fas fa-check-circle fs-4 mb-2 d-block text-success"></i>
                        <p class="text-dark fs-6 fw-bold mb-0"><%= Common.getBahasaConfig("Jawaban Anda telah berhasil disimpan dan saat ini sedang diproses oleh sistem. Terima kasih.") %></p>
                    </div>
                <% } %>
            </div>
        </div>
    </div>
</div>

<script>
    const totalQuestions<%=random%> = <%= examData.length() %>;
    let currentQuestion<%=random%> = 0;
    let timerInterval<%=random%>;
    let timeLeft<%=random%> = <%= totalDetikSisa %>;

    function showConfirmModal<%=random%>(message) {
        let existingModal = document.getElementById('modalKonfirmasi_<%=random%>');
        if (existingModal) { existingModal.remove(); }

        const modalHtml = `
            <div class="modal fade" id="modalKonfirmasi_<%=random%>" tabindex="-1" aria-hidden="true" style="z-index: 106000;">
                <div class="modal-dialog modal-dialog-centered modal-sm animate__animated animate__jackInTheBox animate__faster">
                    <div class="modal-content border-0 shadow-lg rounded-4">
                        <div class="modal-header border-0 pb-0">
                            <h5 class="modal-title fw-bold text-danger"><i class="fas fa-exclamation-circle me-2"></i><%=Common.getBahasaConfig("Konfirmasi Tindakan") %></h5>
                        </div>
                        <div class="modal-body text-center py-4">
                            <p class="mb-0 text-dark fw-bold" style="font-size:1.1rem;">` + message + `</p>
                        </div>
                        <div class="modal-footer border-0 pt-0 justify-content-center">
                            <button type="button" class="btn btn-light rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal") %></button>
                            <button type="button" class="btn btn-danger rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal" onclick="processFinish<%=random%>();"><i class="fas fa-check me-2"></i><%=Common.getBahasaConfig("Selesai") %></button>
                        </div>
                    </div>
                </div>
            </div>`;
            
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        
        let myModalElement = document.getElementById('modalKonfirmasi_<%=random%>');
        const myModal = new bootstrap.Modal(myModalElement);
        myModal.show();

        setTimeout(() => {
            const backdrops = document.querySelectorAll('.modal-backdrop');
            if (backdrops.length > 0) {
                backdrops[backdrops.length - 1].style.zIndex = '105999';
            }
        }, 105);
        
        myModalElement.addEventListener('hidden.bs.modal', function () {
            myModalElement.remove();
        });
    }

    // ==== FUNGSI SIMPAN AJAX ====
    function saveAnswer<%=random%>(element, index) {
        const isEssay = element.tagName.toLowerCase() === 'textarea';
        if (isEssay) {
            if(element.value.trim().length > 0) {
                updateProgress<%=random%>(index, true);
            } else {
                updateProgress<%=random%>(index, false);
            }
        } else {
            updateProgress<%=random%>(index, true);
        }

        const detailId = element.getAttribute('data-detailid');
        const ppu = element.getAttribute('data-ppu');
        const bankSoalDetailid = element.getAttribute('data-banksoaldetailid');
        const ujianPunyaSoalid = element.getAttribute('data-ujianpunyasoalid');
        const waktuStr = document.getElementById('timer-display').innerText;

        if (!detailId) {
            console.error('<%= Common.getBahasaConfigJS("Terjadi kesalahan teknis: ID Detail data ujian tidak ditemukan.") %>');
            return;
        }

        const formData = new URLSearchParams();
        formData.append('myHasilUjianMahasiswaDetailid', detailId);
        formData.append('ppu', ppu);
        formData.append('ujianPunyaSoalid', ujianPunyaSoalid);
        formData.append('waktu', waktuStr);
        
        if (isEssay) {
            formData.append('jawabanEsai', element.value);
            formData.append('jawaban', element.value);
            formData.append('bankSoalDetailid', bankSoalDetailid); 
        } else {
            formData.append('bankSoalDetailid', bankSoalDetailid);
        }

        fetch('<%=linkSimpan%>', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(response => response.json())
        .then(data => {
            if(data.status !== 'success') {
                console.error(data);
            }
        })
        .catch(error => {
            if (typeof tampilkanToast === "function") {
                tampilkanToast('<%= Common.getBahasaConfigJS("Gagal terhubung ke server saat mencoba menyimpan jawaban Anda.") %>', 'bg-danger text-white');
            } else {
                console.error('<%= Common.getBahasaConfigJS("Gagal terhubung ke server saat mencoba menyimpan jawaban Anda.") %>');
            }
        });
    }

    // ==== FUNGSI NAVIGASI & TIMER ====
    function startExam<%=random%>() {
        document.getElementById('intro-screen').classList.add('d-none');
        document.getElementById('exam-screen').classList.remove('d-none');
        goToQuestion<%=random%>(0);
        startTimer<%=random%>();
        // Anti-Curang (CBT) PER-UJIAN — REUSE ProsesUjianHelper.buildCbtAntiCheatScriptJsp (baca kolom ac_*
        // pada PertemuanPunyaUjian; DEFAULT non-aktif → menghasilkan string kosong). Dimulai di sini karena
        // klik "Mulai" = gesture pengguna (syarat fullscreen). Auto-selesai = processFinish saat batas tercapai.
        <%= ProsesUjianHelper.buildCbtAntiCheatScriptJsp(pertemuanPunyaUjian, "processFinish" + random + "()") %>
    }

    function startTimer<%=random%>() {
        const display = document.getElementById('timer-display');
        timerInterval<%=random%> = setInterval(function () {
            let hours   = Math.floor(timeLeft<%=random%> / 3600);
            let minutes = Math.floor((timeLeft<%=random%> % 3600) / 60);
            let seconds = timeLeft<%=random%> % 60;

            display.textContent = 
                (hours < 10 ? "0" + hours : hours) + ":" + 
                (minutes < 10 ? "0" + minutes : minutes) + ":" + 
                (seconds < 10 ? "0" + seconds : seconds);

            if (--timeLeft<%=random%> < 0) {
                clearInterval(timerInterval<%=random%>);
                if (typeof tampilkanToast === "function") {
                    tampilkanToast('<%= Common.getBahasaConfigJS("Waktu pengerjaan ujian telah habis!") %>', 'bg-danger text-white');
                } else {
                    alert('<%= Common.getBahasaConfigJS("Waktu pengerjaan ujian telah habis!") %>');
                }
                processFinish<%=random%>();
            }
        }, 1000);
    }

    function goToQuestion<%=random%>(index) {
        if (index < 0 || index >= totalQuestions<%=random%>) return;
        document.querySelectorAll('.question-page').forEach(page => page.classList.add('d-none'));
        document.getElementById('question-' + index).classList.remove('d-none');
        currentQuestion<%=random%> = index;
        document.getElementById('btn-prev').disabled = (currentQuestion<%=random%> === 0);
        document.getElementById('btn-next').disabled = (currentQuestion<%=random%> === totalQuestions<%=random%> - 1);
        updateProgress<%=random%>Badges();
    }

    function navigate<%=random%>(direction) { 
        goToQuestion<%=random%>(currentQuestion<%=random%> + direction);
    }

    function updateProgress<%=random%>(index, isFilled) {
        const badge = document.getElementById('nav-badge-' + index);
        if(badge) {
            if(isFilled) {
                badge.classList.remove('btn-outline-secondary');
                badge.classList.add('btn-primary', 'text-white');
            } else {
                badge.classList.remove('btn-primary', 'text-white');
                badge.classList.add('btn-outline-secondary');
            }
        }
    }

    function updateProgress<%=random%>Badges() {
        for(let i=0; i<totalQuestions<%=random%>; i++) {
            const badge = document.getElementById('nav-badge-' + i);
            if(badge) {
                badge.style.border = (i === currentQuestion<%=random%>) ? "3px solid #dc3545" : "";
            }
        }
    }

    function finishExam<%=random%>() {
        if (timeLeft<%=random%> > 0) {
            showConfirmModal<%=random%>(
                '<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin mengakhiri sesi ujian ini? Waktu pengerjaan Anda masih tersisa.") %>'
            );
        } else {
            processFinish<%=random%>();
        }
    }

    function processFinish<%=random%>() {
        try { if (window.__cbtStop) window.__cbtStop(); } catch (e) {} // hentikan pengawasan anti-curang.
        clearInterval(timerInterval<%=random%>);
        try {
           document.getElementById('exam-screen').classList.add('d-none');
        } catch(e) {}
        
        const formData = new URLSearchParams();
        formData.append('ppu', '<%= pertemuanPunyaUjian.getId() %>');

        fetch('<%=linkHasil%>', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(response => response.json())
        .then(data => {
            try {
                document.getElementById('result-screen').classList.remove('d-none');
            } catch(e) {}
             
            if(data.status === 'success') {
                try {
                    document.getElementById('final-score').innerText = data.nilai;
                    document.getElementById('total-correct').innerText = data.jawabanBenar + " / " + data.jawabanBenarMasimal;
                    document.getElementById('total-questions').innerText = data.jumlahSoal;
                    let scoreObe = document.getElementById('score-obe');
                    if (scoreObe) scoreObe.innerText = data.nilai_obe;
                } catch(e) {}
            } else {
                if (typeof tampilkanToast === "function") {
                    tampilkanToast(data.message || '<%= Common.getBahasaConfigJS("Terjadi kesalahan pada sistem saat memuat hasil ujian.") %>', 'bg-danger text-white');
                }
            }
        })
        .catch(error => {
            console.error(error);
            try { document.getElementById('result-screen').classList.remove('d-none'); } catch(e) {}
            if (typeof tampilkanToast === "function") {
                tampilkanToast('<%= Common.getBahasaConfigJS("Gagal terhubung ke server saat memproses hasil ujian.") %>', 'bg-danger text-white');
            }
        });
    }
</script>
<%
    }
}
%>
