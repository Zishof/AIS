<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="java.util.*"%>
<%@ page import="ais.common.*"%>
<%@ page import="ais.database.model.*"%>
<%@ page import="ais.database.model.streaming.*"%>
<%@ page import="ais.database.model.file.*"%>
<%@ page import="ais.ui.util.SmartDateTimeUtil"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<%
    // ====================================================================================
    // 1. VALIDASI KEAMANAN & INISIALISASI PENGGUNA
    // ====================================================================================
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        return;
    }

    String itemId = request.getParameter("pertemuan");
    Boolean refreshData = request.getParameter("refresh") == null ? false : request.getParameter("refresh").trim().equalsIgnoreCase("true");
    
    if (itemId == null || itemId.trim().isEmpty()) {
        return;
    }

    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, itemId.trim(), true);

    if (pertemuan == null || pertemuan.getId() == null || !pertemuan.getAktif()) {
        return;
    }
    
    // ====================================================================================
    // 2. PERBAIKAN LOGIKA SESI HIBERNATE (Menghindari Kebocoran Memori / Deadlock)
    // ====================================================================================
    if (pertemuan.getPertemuanKe() != null && pertemuan.getPertemuanKe().equals(0)) {
        Session sess = null;
        try {
            sess = HibernateUtil.openSession();
            if (pertemuan.ambilVOPembelajaran() != null) {
                pertemuan.ambilVOPembelajaran().reInitPertemuan(sess);
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/pertemuan.jsp:47");
        } finally {
            ais.common.ElearningSessionUtil.closeQuietly(sess);
            HibernateUtil.closeSessionQuietly(sess);
        }
    }

    if (refreshData) {
        pertemuan.refreshData();
    }
    
    // ====================================================================================
    // 3. PERSIAPAN DATA (Memisahkan Logika dengan Tampilan Antarmuka)
    // ====================================================================================
    boolean bolehUbah = pertemuan.bolehUbahAbsenSaja(tbmuser);
    boolean isStudentRole = (tbmuser.getMahasiswa() != null) || (tbmuser.getSiswa() != null) || (tbmuser.getBiodataCalonMahasiswa() != null) || (tbmuser.getCalonSiswa() != null);

    // -- Pembuatan Identitas & Tautan Panggilan
    StringBuilder reqIdBuilder = new StringBuilder();
    try {
        if (pertemuan.ambilVOPembelajaran() != null && pertemuan.ambilVOPembelajaran().ambilPertemuan() != null) {
            for (Long idP : pertemuan.ambilVOPembelajaran().ambilPertemuan().values()) {
                if (reqIdBuilder.length() > 0) reqIdBuilder.append(",");
                reqIdBuilder.append(idP);
            }
        }
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/pertemuan.jsp:73"); }

    String linkAgenda = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_timeline_perkuliahan&tab=true&item=" + reqIdBuilder.toString();
    String linkAbsen = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta_absen&pertemuan=" + pertemuan.getId();
    String linkPengajuanIzinDanSakit = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_pengajuan_peserta_absen_izin_dan_sakit&pertemuan=" + pertemuan.getId();
    String linkUbah = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=ubah_pertemuan&pertemuan=" + pertemuan.getId();
    String linkTugas = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=tugas&pertemuan=" + pertemuan.getId();
    String linkTugasBaru = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=tugas_baru&pertemuan=" + pertemuan.getId();
    String linkUjianBaru = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Fujian&s=ujian_baru&pertemuan=" + pertemuan.getId();
   
    // -- Format Penanggalan & Waktu
    String tglDisplay = (pertemuan.getTanggal() == null) ? "-" : (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai()) + " " + Common.dateFormat6.get().format(pertemuan.getTanggal()));
    String waktuDisplay = (pertemuan.getWaktuMulai() == null) ? "" : pertemuan.getWaktuMulai() + " - " + (pertemuan.getWaktuSelesai() != null ? pertemuan.getWaktuSelesai() : "");
    
    // -- Format Teks Paragraf ke HTML
    String[] textFields = {
        pertemuan.getBukuRujukan1(), 
        pertemuan.getMetodePembelajaran(), 
        pertemuan.getBukuRujukan2(), 
        pertemuan.getCatatan(), 
        pertemuan.getTugasDanPenilaian(), 
        pertemuan.getPengalamanBelajar()
    };
    String[] formattedTexts = new String[6]; 
    
    for (int i = 0; i < textFields.length; i++) {
        String txt = textFields[i];
        if (txt != null && !txt.trim().isEmpty()) {
            List<String> urls = Common.getUrls(txt);
            txt = txt.replaceAll("\n", "<br>");
            for (String url : urls) {
                txt = StringUtils.replace(txt, url, "<a href='" + url + "' target='_blank' class='text-decoration-none text-primary fw-bold'><i class='fas fa-external-link-alt me-1'></i>" + url + "</a>");
            }
            formattedTexts[i] = txt;
        } else {
            formattedTexts[i] = ""; // Berikan nilai kosong sebagai standar
        }
    }
    
    // -- Perhitungan Status Kehadiran
    Map<String, Integer> statuses = pertemuan.hitungStatus();
    int cp = 0;
    if (statuses != null) {
        for (String s : statuses.keySet()) {
            if (s != null && !s.trim().isEmpty() && !s.trim().equals("-")) {
                cp += statuses.get(s); 
            }
        }
    }
    
    int p = pertemuan.ambilJumlahPengajuanIzinTidakMasukPerkuliahan();
    
    // -- Daftar Peserta & Pengajar
    List<Dosen> dosens = pertemuan.ambilDosen() != null ? pertemuan.ambilDosen() : new ArrayList<Dosen>();
    VOPembelajaran vopForPeserta = pertemuan.ambilVOPembelajaran();
    List<Long> allMhsIds = (vopForPeserta != null) ? vopForPeserta.ambilMahasiswaById() : new ArrayList<Long>();
    List<Long> allSwIds  = (vopForPeserta != null) ? vopForPeserta.ambilSiswaById()     : new ArrayList<Long>();
    int totalPeserta = (allMhsIds != null ? allMhsIds.size() : 0) + (allSwIds != null ? allSwIds.size() : 0);
    // Gunakan VOPembelajaran (Perkuliahan/JadwalPelajaran) untuk peserta service, bukan Pertemuan
    String pesertaVopId    = (vopForPeserta != null && vopForPeserta.getId() != null) ? vopForPeserta.getId().toString() : pertemuan.getId().toString();
    String pesertaVopJenis = (vopForPeserta != null) ? vopForPeserta.getClass().getSimpleName() : "Pertemuan";
    TreeMap<String, String> dAkses = pertemuan.ambilData("akses", null);

    int belum = Math.max(0, totalPeserta - cp);
    
    // -- Statistik Lampiran & Interaksi
    Number audioNum = pertemuan.ambilJumlahAudioPertemuan();
    Number videoNum = pertemuan.ambilJumlahVideoPertemuan();
    Number fileNum = pertemuan.ambilJumlahPertemuanFileContent();
    Number diskusiNum = pertemuan.ambilJumlahPertemuanPunyaDiskusi();
    
    int countAudio = (audioNum != null) ? audioNum.intValue() : 0;
    int countVideo = (videoNum != null) ? videoNum.intValue() : 0;
    int countFile  = (fileNum != null) ? fileNum.intValue() : 0;
    int countDiskusi = (diskusiNum != null) ? diskusiNum.intValue() : 0;
    
    // -- Logika Pengelolaan Ujian
    TreeMap<Long, PertemuanPunyaUjian> ujianData = pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser);
    StringBuilder ujianIdBuilder = new StringBuilder();
    if (ujianData != null) {
        for (PertemuanPunyaUjian idP : ujianData.values()) {
            if (ujianIdBuilder.length() > 0) ujianIdBuilder.append(",");
            ujianIdBuilder.append(idP.getId());
        }
    }
    String linkUjian = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_ujian&item=" + ujianIdBuilder.toString();
    int countUjian = ujianData != null ? ujianData.size() : 0;
    
    // -- Tautan Diskusi
    String linkDiskusi = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=_diskusi&idPertemuan=" + pertemuan.getId();
    
    // -- Sintesis Konten Lampiran (Materi/Media HTML)
    StringBuilder mediaHtml = new StringBuilder();
    TreeMap<Long, PertemuanFileContent> files = pertemuan.ambilPertemuanFileContentTotal();
    if (files != null) {
        for (PertemuanFileContent pfc : files.values()) {
            String u = pfc.getLink();
            String url = (u != null) ? u : pfc.createLinkUri();
            String mime = pfc.getFileMimeType();
            
            // Lewati jika merupakan buku cetak fisik
            if ((mime != null && mime.toLowerCase().contains("book")) || pfc.getLokasiFisik() != null) continue;
            
            mediaHtml.append("<div class='card mb-3 border border-light bg-light p-2 rounded-3 shadow-sm'>");
            if (pfc.getGdrive() != null) {
                mediaHtml.append("<div class='ratio ratio-16x9 rounded overflow-hidden'><iframe src='https://drive.google.com/file/d/").append(pfc.getGdrive()).append("/preview' allowfullscreen></iframe></div>");
            } else if (pfc.merupakanGambar()) {
                mediaHtml.append("<img src='").append(pfc.createLinkUri()).append("' class='img-fluid rounded shadow-sm' style='cursor:pointer;' onclick='tampilGambar(this)' alt='").append(pfc.getNama()).append("'>");
            } else if (pfc.getNama() != null && pfc.getNama().toLowerCase().trim().endsWith(".mp3")) {
                mediaHtml.append("<audio controls class='w-100 mt-2'><source src='").append(u).append("' type='audio/mpeg'>").append(Common.getBahasaConfig("Peramban Anda tidak mendukung pemutar audio.")).append("</audio>");
            } else if (u != null && u.contains("drive.google.com/drive/folders")) {
                String folderId = u.replace("https://drive.google.com/drive/folders", "").split("&")[0].split("/")[0].split("\\?")[0];
                mediaHtml.append("<div class='ratio ratio-16x9 rounded overflow-hidden'><iframe src='https://drive.google.com/embeddedfolderview?id=").append(folderId).append("#list'></iframe></div>");
            } else if (url != null) {
                mediaHtml.append("<div class='ratio ratio-16x9 rounded overflow-hidden'><iframe src='https://drive.google.com/file/d/").append(pfc.getGdrive()).append("/preview'></iframe></div>");
            }
            mediaHtml.append("<small class='text-secondary d-block text-center mt-2 fw-semibold'>").append(pfc.getKeterangan() != null ? pfc.getKeterangan() : pfc.getNama()).append("</small>");
            mediaHtml.append("</div>");
        }
    }
    
    // Koleksi Audio & Video Tambahan
    TreeMap<Long, AudioPertemuan> audios = pertemuan.ambilAudioPertemuanTotal();
    if (audios != null) {
        for (AudioPertemuan ap : audios.values()) {
            mediaHtml.append("<div class='card mb-3 p-3 border-0 bg-light shadow-sm rounded-3'><audio controls class='w-100'><source src='").append(ap.createLinkUri(false)).append("' type='audio/mpeg'></audio>");
            mediaHtml.append("<small class='text-center text-secondary fw-semibold mt-2'>").append(ap.getKeteranganTambahan()).append("</small></div>");
        }
    }
    TreeMap<Long, VideoPertemuan> videos = pertemuan.ambilVideoPertemuanTotal();
    if (videos != null) {
        for (VideoPertemuan vp : videos.values()) {
            mediaHtml.append("<div class='mb-4 rounded-3 overflow-hidden shadow-sm'>").append(YouTubeHelper.convertoToEmbed(vp.createLinkUri(false)));
            mediaHtml.append("<small class='d-block text-center text-secondary bg-light py-2 fw-semibold'>").append(vp.getKeteranganTambahan()).append("</small></div>");
        }
    }
    StringBuilder audioIdBuilder = new StringBuilder();
    if (audios != null) {
        for (Long aId : audios.keySet()) {
            if (audioIdBuilder.length() > 0) audioIdBuilder.append(",");
            audioIdBuilder.append(aId);
        }
    }
    StringBuilder videoIdBuilder = new StringBuilder();
    if (videos != null) {
        for (Long vId : videos.keySet()) {
            if (videoIdBuilder.length() > 0) videoIdBuilder.append(",");
            videoIdBuilder.append(vId);
        }
    }
    String linkAudio = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_audio&item=" + audioIdBuilder.toString();
    String linkVideo = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=load_video&item=" + videoIdBuilder.toString();
    String zoomLink = pertemuan.getZoomLink();
%> 

<style>
    .hover-shadow-sm { transition: box-shadow 0.3s ease; }
    .hover-shadow-sm:hover { box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15) !important; }
    .btn-soft-primary { background-color: rgba(13, 110, 253, 0.1); color: #0d6efd; transition: all 0.2s; }
    .btn-soft-primary:hover { background-color: #0d6efd; color: #fff; }
</style>

<div class="card border-0 shadow-lg rounded-4 overflow-hidden mb-4 animate__animated animate__fadeInUp" data-id="<%=pertemuan.getId()%>">
    
    <div class="card-header bg-primary text-dark bg-gradient text-white p-3 border-0 d-flex justify-content-between align-items-center">
        <div class="d-flex align-items-center">
             <div class="bg-white bg-opacity-25 rounded-circle p-2 me-3 d-flex align-items-center justify-content-center flex-shrink-0 shadow-sm" style="width: 50px; height: 50px;">
                    <i class="fas fa-chalkboard-teacher fs-4 text-white"></i>
            </div>
            
            <% if (pertemuan.getPerkuliahan() != null) { %>
            <div>
                <h4 class="fw-bold mb-1 text-white"><%=pertemuan.getPerkuliahan().getMatakuliah().getKode()%> - <%=pertemuan.getPerkuliahan().getMatakuliah().getNama()%></h4>
                <div class="d-flex flex-wrap gap-3 small opacity-75 mt-2 fw-semibold">
                    <span><i class="fas fa-check-square me-1"></i> <%=Common.getBahasaConfig("Pertemuan ke-")%> <%=pertemuan.getPertemuanKe()%></span>
                    <span><i class="far fa-calendar-alt me-1"></i> <%=tglDisplay%></span>
                    <span><i class="far fa-clock me-1"></i> <%=waktuDisplay%></span>
                    <span><i class="fas fa-map-marker-alt me-1"></i> <%=pertemuan.getRuang() == null ? "-" : pertemuan.getRuang().getNama()%></span>
                </div>
                <div class="small opacity-75 mt-1">
                    <span class="me-3"><i class="fas fa-university me-1"></i> SMT <%=pertemuan.getPerkuliahan().getSemester()%> <%=pertemuan.getPerkuliahan().getKelas()%></span>
                    <span>TA <%=pertemuan.getPerkuliahan().getTahunAjaran()%> (<%=pertemuan.getPerkuliahan().getGanjilGenap()%>)</span>
                </div>
            </div>
            <% } else if (pertemuan.getJadwalPelajaran() != null) { %>
            <div>
                <h4 class="fw-bold mb-1 text-white"><%=(pertemuan.getJadwalPelajaran().getMatapelajaran() == null ? "" : pertemuan.getJadwalPelajaran().getMatapelajaran().getNama())%></h4>
                <div class="d-flex flex-wrap gap-3 small opacity-75 mt-2 fw-semibold">
                    <span><i class="fas fa-check-square me-1"></i> <%=Common.getBahasaConfig("Pertemuan ke-")%> <%=pertemuan.getPertemuanKe()%></span>
                    <span><i class="far fa-calendar-alt me-1"></i> <%=tglDisplay%></span>
                    <span><i class="far fa-clock me-1"></i> <%=waktuDisplay%></span>
                    <span><i class="fas fa-map-marker-alt me-1"></i> <%=pertemuan.getRuang() == null ? "-" : pertemuan.getRuang().getNama()%></span>
                </div>
                <div class="small opacity-75 mt-1">
                    <span class="me-3"><i class="fas fa-university me-1"></i> <%=pertemuan.getJadwalPelajaran().getKelas().getNama()%></span>
                    <span>TA <%=pertemuan.getJadwalPelajaran().getTahunAjaran()%> (<%=pertemuan.getJadwalPelajaran().getSemester().equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP%>)</span>
                </div>
            </div>
            <% } else { %>
            <div>
                <h4 class="fw-bold mb-1 text-white"><%=pertemuan.info()%></h4>
            </div>
           <% } %>
        </div>
    </div>

    <div class="card-body bg-white p-4">
        
        <h6 class="text-uppercase text-secondary fw-bold small mb-3 border-bottom pb-2">
            <i class="fas fa-chalkboard-teacher me-1 text-primary"></i> <%= dosens.size() > 1 ? Common.getBahasaConfig("Tim Pengajar") : Common.getBahasaConfig("Pengajar") %>
        </h6>
        <div class="d-flex flex-wrap gap-3 mb-4">
            <% for (Dosen dosen : dosens) { 
                String foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen));
            %>
            <div class="d-flex align-items-center bg-light rounded-pill pe-3 shadow-sm border hover-shadow-sm">
                <img src="<%=foto%>" class="rounded-circle me-2 border border-2 border-white shadow-sm" style="width: 45px; height: 45px; object-fit: cover; cursor: pointer;" onclick="tampilGambar(this)" alt="<%=dosen.getNama()%>" onerror="this.src='<%=Common.ROOT%>/images/default_user.png'">
                <div>
                    <div class="fw-bold text-dark" style="font-size: 0.85rem; line-height: 1.2;"><%=dosen.getNama()%></div>
                    <div class="text-muted" style="font-size: 0.7rem;"><%=dosen.getNidn()%></div>
                </div>
            </div>
            <% } %>
        </div>

        <div class="card border border-light bg-light rounded-4 p-4 mb-4 shadow-sm">
            <div class="row g-4">
                <div class="col-md-6 border-end-md">
                    <div class="mb-3">
                        <small class="fw-bold text-primary text-uppercase d-block mb-1"><i class="fas fa-bullseye me-1"></i> <%=Common.getBahasaConfig("Tujuan Akhir Pembelajaran")%></small>
                        <p class="small text-dark mb-0 fw-semibold"><%=pertemuan.getTopik() == null || pertemuan.getTopik().trim().isEmpty() ? "-" : pertemuan.getTopik()%></p>
                    </div>
                    <div class="mb-3">
                        <small class="fw-bold text-primary text-uppercase d-block mb-1"><i class="fas fa-book-open me-1"></i> <%=Common.getBahasaConfig("Bahan Kajian")%></small>
                        <p class="small text-secondary mb-0"><%=formattedTexts[0].isEmpty() ? "-" : formattedTexts[0]%></p>
                    </div>
                     <div>
                        <small class="fw-bold text-primary text-uppercase d-block mb-1"><i class="fas fa-link me-1"></i> <%=Common.getBahasaConfig("Referensi Rujukan")%></small>
                        <p class="small text-secondary mb-0"><%=formattedTexts[2].isEmpty() ? "-" : formattedTexts[2]%></p>
                    </div>
                </div>
                <div class="col-md-6 ps-md-4">
                    <div class="mb-3">
                        <small class="fw-bold text-primary text-uppercase d-block mb-1"><i class="fas fa-tasks me-1"></i> <%=Common.getBahasaConfig("Metode Pembelajaran")%></small>
                        <p class="small text-secondary mb-0"><%=formattedTexts[1].isEmpty() ? "-" : formattedTexts[1]%></p>
                    </div>
                    <div class="mb-3">
                        <small class="fw-bold text-primary text-uppercase d-block mb-1"><i class="fas fa-clipboard-check me-1"></i> <%=Common.getBahasaConfig("Tugas & Kriteria Penilaian")%></small>
                        <p class="small text-secondary mb-0"><%=formattedTexts[4].isEmpty() ? "-" : formattedTexts[4]%></p>
                    </div>
                    <div>
                        <small class="fw-bold text-primary text-uppercase d-block mb-1"><i class="fas fa-running me-1"></i> <%=Common.getBahasaConfig("Pengalaman Belajar")%></small>
                        <p class="small text-secondary mb-0"><%=formattedTexts[5].isEmpty() ? "-" : formattedTexts[5]%></p>
                    </div>
                </div>
            </div>
            
            <% if (!formattedTexts[3].isEmpty()) { %>
            <div class="alert alert-warning border-0 shadow-sm mt-4 mb-0 d-flex align-items-start rounded-3">
                <i class="fas fa-exclamation-triangle mt-1 me-2 text-warning fs-5"></i>
                <div>
                    <strong class="d-block mb-1 text-warning-emphasis"><%=Common.getBahasaConfig("Catatan Khusus")%>:</strong>
                    <span class="small text-dark"><%=formattedTexts[3]%></span>
                </div>
            </div>
            <% } %>
        </div>

        <% if (countFile > 0 || mediaHtml.length() > 0) { %>
        <div class="mb-4">
             <button class="btn btn-soft-primary fw-bold w-100 d-flex justify-content-between align-items-center py-2 rounded-3 border-0 shadow-sm" type="button" data-bs-toggle="collapse" data-bs-target="#materi-<%=pertemuan.getId()%>">
                <span><i class="fas fa-folder-open me-2 fs-5 align-middle"></i> <span class="align-middle"><%=Common.getBahasaConfig("Materi Pembelajaran Tambahan")%></span></span>
                <span class="badge bg-primary rounded-pill px-3 py-2"><%=countFile%> <%=Common.getBahasaConfig("Berkas")%></span>
            </button>
            <div class="collapse mt-3" id="materi-<%=pertemuan.getId()%>">
                <%= mediaHtml.toString() %>
            </div>
        </div>
        <% } %>

        <div class="mb-4">
             <button class="btn btn-light border w-100 d-flex justify-content-between align-items-center py-2 rounded-3 shadow-sm fw-bold text-secondary hover-shadow-sm" type="button" data-bs-toggle="collapse" data-bs-target="#peserta-<%=pertemuan.getId()%>">
                <span><i class="fas fa-users me-2 fs-5 align-middle text-primary"></i> <span class="align-middle"><%=Common.getBahasaConfig("Daftar Peserta Kelas")%></span></span>
                <span class="badge bg-secondary rounded-pill px-3 py-2"><%=totalPeserta%> <%=Common.getBahasaConfig("Peserta")%></span>
            </button>
            <div class="collapse mt-3" id="peserta-<%=pertemuan.getId()%>">
                <div id="peserta-list-<%=pertemuan.getId()%>" class="d-flex flex-wrap gap-2 p-3 bg-light rounded-3 border shadow-inner">
                    <div class="text-muted w-100 text-center py-2 small"><div class="spinner-border spinner-border-sm text-primary me-1"></div> <%=Common.getBahasaConfig("Memuat data peserta")%>...</div>
                </div>
            </div>
        </div>

        <hr class="text-muted opacity-25 my-4">

        <h6 class="text-uppercase text-secondary fw-bold small mb-3 border-bottom pb-2">
            <i class="fas fa-cogs me-1 text-primary"></i> <%=Common.getBahasaConfig("Aktivitas & Menu Kendali")%>
        </h6>
        
        <div class="d-flex flex-wrap gap-2 align-items-center">
            
            <button class="btn btn-sm btn-outline-dark rounded-pill fw-semibold px-3 shadow-sm hover-shadow-sm" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Pengaturan Pertemuan") %>', '<%=linkUbah%>&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);'), '85%', 'hidden', '', cm);">
            	<i class="far fa-edit me-1 text-primary"></i> <%= bolehUbah ? Common.getBahasaConfig("Ubah Pertemuan") : Common.getBahasaConfig("Lihat Catatan") %>
            </button>
            
            <button class="btn btn-sm btn-outline-info text-dark rounded-pill fw-semibold px-3 shadow-sm hover-shadow-sm" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Agenda Pertemuan")%>', '<%=linkAgenda%>');">
                <i class="far fa-calendar-check me-1 text-info"></i> <%=Common.getBahasaConfig("Agenda")%>
            </button>
            
            <div class="btn-group btn-group-sm shadow-sm rounded-pill" role="group">
                <button type="button" class="btn btn-light border fw-bold text-dark px-3" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Rincian Kehadiran")%>', '<%=linkAbsen%>');">
                    <i class="fas fa-clipboard-list me-1 text-primary"></i> <%=Common.getBahasaConfig("Presensi")%>
                </button>
                <button type="button" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Daftar Hadir")%>', '<%=linkAbsen%>&filter_status=<%=ConstantValues.MASUK.getId()%>');" class="btn btn-light border text-success fw-bold px-3" title="<%=Common.getBahasaConfig("Hadir")%>"><%= statuses.get("M") == null ? 0 : statuses.get("M") %> <i class="fas fa-check-circle ms-1"></i></button>
                <button type="button" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Daftar Izin")%>', '<%=linkAbsen%>&filter_status=<%=ConstantValues.IZIN.getId()%>');" class="btn btn-light border text-info fw-bold px-3" title="<%=Common.getBahasaConfig("Izin")%>"><%= statuses.get("I") == null ? 0 : statuses.get("I") %> <i class="far fa-envelope ms-1"></i></button>
                <button type="button" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Daftar Sakit")%>', '<%=linkAbsen%>&filter_status=<%=ConstantValues.SAKIT.getId()%>');" class="btn btn-light border text-warning fw-bold px-3" title="<%=Common.getBahasaConfig("Sakit")%>"><%= statuses.get("S") == null ? 0 : statuses.get("S") %> <i class="fas fa-user-injured ms-1"></i></button>
                <button type="button" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Daftar Alpha")%>', '<%=linkAbsen%>&filter_status=<%=ConstantValues.TIDAK_ADA_ALASAN.getId()%>');" class="btn btn-light border text-danger fw-bold px-3" title="<%=Common.getBahasaConfig("Alpha (Tanpa Keterangan)")%>"><%= statuses.get("A") == null ? 0 : statuses.get("A") %> <i class="fas fa-times-circle ms-1"></i></button>
                <button type="button" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Daftar Belum Presensi")%>', '<%=linkAbsen%>&filter_status=<%=ConstantValues.BELUM_ABSEN.getId()%>');" class="btn btn-light border text-secondary fw-bold px-3" title="<%=Common.getBahasaConfig("Belum Memilih Status")%>"><%= belum %> <i class="fas fa-user-clock ms-1"></i></button>
                <button type="button" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Pengajuan Izin atau Sakit") %>', '<%=linkPengajuanIzinDanSakit%>&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);'), '75%', null, '', cm);" class="btn btn-light border text-dark fw-bold px-3" title="<%=Common.getBahasaConfig("Daftar Pengajuan")%>"><%= p %> <i class="far fa-paper-plane ms-1"></i></button>
            </div>
            
            <button class="btn btn-sm btn-outline-danger position-relative rounded-pill fw-semibold px-3 shadow-sm hover-shadow-sm" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Daftar Ujian")%>', '<%=linkUjian%>');">
                <i class="fas fa-file-signature me-1"></i> <%=Common.getBahasaConfig("Ujian")%>
                <% if(countUjian > 0) { %><span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light"><%=countUjian%></span><% } %>
            </button>

            <% if (bolehUbah && !isStudentRole) { %>
            <button class="btn btn-sm btn-outline-danger border border-danger border-2 rounded-pill fw-bold px-3 shadow-sm hover-shadow-sm" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Buat Ujian Baru") %>', '<%=linkUjianBaru%>&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);'), '75%', true, 'simpanDataHelper_'+cm+'();', cm);">
                <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Ujian Baru")%>
            </button>
            <% } %>

            <button class="btn btn-sm btn-outline-primary position-relative rounded-pill fw-semibold px-3 shadow-sm hover-shadow-sm" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ruang Diskusi Interaktif") %>', '<%=linkDiskusi%>', '85%', 'hidden', '', cm);">
                <i class="far fa-comments me-1"></i> <%=Common.getBahasaConfig("Forum Diskusi")%>
                <% if(countDiskusi > 0) { %><span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light"><%=countDiskusi%></span><% } %>
            </button>

            <% if(countAudio > 0) { %>
            <button class="btn btn-sm btn-outline-secondary position-relative rounded-pill fw-semibold px-3 shadow-sm hover-shadow-sm" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Audio Tambahan")%>', '<%=linkAudio%>')">
                <i class="far fa-file-audio me-1"></i> <%=Common.getBahasaConfig("Audio Tambahan")%>
                <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light"><%=countAudio%></span>
            </button>
            <% } %>

            <% if(countVideo > 0) { %>
            <button class="btn btn-sm btn-outline-secondary position-relative rounded-pill fw-semibold px-3 shadow-sm hover-shadow-sm" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Video Tambahan")%>', '<%=linkVideo%>')">
                <i class="far fa-file-video me-1"></i> <%=Common.getBahasaConfig("Video Tambahan")%>
                <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light"><%=countVideo%></span>
            </button>
            <% } %>

            <% 
            boolean adaTugas = false;
            
            // 1. Tugas Pertemuan Utama
            if (pertemuan.getJudultugas() != null && !pertemuan.getJudultugas().trim().isEmpty()) {
                 int tCount = pertemuan.ambilJumlahTugasFileContent();
                 adaTugas = true;
            %>
                 <button class="btn btn-sm btn-warning text-dark position-relative rounded-pill fw-bold px-3 shadow-sm hover-shadow-sm" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ubah Penugasan Mandiri") %>', '<%=linkTugas%>&tugas=<%=pertemuan.getId() %>&jenis=Pertemuan&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);'), '85%', 'hidden', '', cm);">
                     <i class="fas fa-tasks me-1"></i> <%=pertemuan.getJudultugas()%>
                     <% if(tCount > 0){ %><span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light"><%=tCount%></span><% } %>
                 </button>
            <% 
            } 
            
            // 2. Tugas Anak (TugasPertemuan)
            TreeMap<Long, TugasPertemuan> tgs = pertemuan.ambilTugasPertemuanTotal();
            if (tgs != null) {
                for (TugasPertemuan tp : tgs.values()) {
                     if (tp.getJudultugas() != null && !tp.getJudultugas().trim().isEmpty()) {
                        int tCount = tp.ambilJumlahTugasFileContent();
                        adaTugas = true;
            %>
                     <button class="btn btn-sm btn-warning text-dark position-relative rounded-pill fw-bold px-3 shadow-sm hover-shadow-sm" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ubah Penugasan Tambahan") %>', '<%=linkTugas%>&tugas=<%=tp.getId() %>&jenis=TugasPertemuan&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);'), '85%', 'hidden', '', cm);">
                         <i class="fas fa-tasks me-1"></i> <%=tp.getJudultugas()%>
                         <% if(tCount > 0){ %><span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light"><%=tCount%></span><% } %>
                     </button>
            <%       
                     }
                } 
            }
            
            // 3. Tugas Kelompok
            TreeMap<Long, TugasKelompok> tgk = pertemuan.ambilTugasKelompokTotal();
            if (tgk != null) {
                for (TugasKelompok tk : tgk.values()) {
                     if (tk.getJudultugas() != null && !tk.getJudultugas().trim().isEmpty()) {
                        int tCount = tk.ambilJumlahTugasFileContent();
            %>
                     <button class="btn btn-sm btn-warning text-dark position-relative rounded-pill fw-bold px-3 shadow-sm hover-shadow-sm" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ubah Tugas Kelompok") %>', '<%=linkTugas%>&tugas=<%=tk.getId() %>&jenis=TugasKelompok&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);'), '85%', 'hidden', '', cm);">
                         <i class="fas fa-users-cog me-1"></i> <%=tk.getJudultugas()%>
                         <% if(tCount > 0){ %><span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light"><%=tCount%></span><% } %>
                     </button>
            <%       }
                 } 
            }
            
            // Tombol Penambahan Tugas Baru (Khusus Dosen/Admin)
            if (bolehUbah) {
            %>
                <div class="btn-group btn-group-sm ms-1 shadow-sm" role="group">
                    <button type="button" class="btn btn-sm btn-outline-success border border-success border-2 fw-bold px-3 hover-shadow-sm" onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Tugas Individu") %>', '<%=linkTugasBaru%>&jenis=<%=adaTugas ? TugasPertemuan.class.getSimpleName() : Pertemuan.class.getSimpleName() %>&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);'), '75%', true, 'simpanDataHelper_'+cm+'();', cm);">
                        <i class="fas fa-plus-circle me-1"></i><%=Common.getBahasaConfig("Buat Tugas Baru")%>
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-success border border-success border-2 dropdown-toggle dropdown-toggle-split px-2 hover-shadow-sm" data-bs-toggle="dropdown" aria-expanded="false">
                        <span class="visually-hidden">Toggle Dropdown</span>
                    </button>
                    <ul class="dropdown-menu shadow-sm">
                        <li>
                            <a class="dropdown-item" href="#" onclick="event.preventDefault(); var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Tugas Individu") %>', '<%=linkTugasBaru%>&jenis=<%=adaTugas ? TugasPertemuan.class.getSimpleName() : Pertemuan.class.getSimpleName() %>&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);'), '75%', true, 'simpanDataHelper_'+cm+'();', cm); return false;">
                                <i class="fas fa-user me-2 text-success"></i><%=Common.getBahasaConfig("Tugas Individu")%>
                            </a>
                        </li>
                        <li>
                            <a class="dropdown-item" href="#" onclick="event.preventDefault(); var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Tugas Kelompok") %>', '<%=linkTugasBaru%>&jenis=TugasKelompok&contentModal='+cm+'&afterSave='+encodeURIComponent('generatePertemuanHTML(<%=pertemuan.getId()%>);'), '75%', true, 'simpanDataHelper_'+cm+'();', cm); return false;">
                                <i class="fas fa-users me-2 text-primary"></i><%=Common.getBahasaConfig("Tugas Kelompok")%>
                            </a>
                        </li>
                    </ul>
                </div>
            <% } %>

            <% if(zoomLink != null && !zoomLink.trim().isEmpty()) { %>
            <button class="btn btn-sm btn-primary ms-auto rounded-pill fw-bold px-4 shadow-sm hover-shadow-sm" onclick="window.open('<%=zoomLink%>', '_blank')">
                <i class="fas fa-video me-2"></i> <%=Common.getBahasaConfig("Gabung Pertemuan Daring")%>
            </button>
            <% } %>
        </div>
        
        <div class="d-flex justify-content-between align-items-center mt-4 pt-3 border-top small">
            <span class="text-muted fw-semibold"><i class="far fa-eye me-1 text-primary"></i> <%=Common.numberFormat.get().format(dAkses != null ? dAkses.size() : 0)%> <%=Common.getBahasaConfig("Jumlah Kunjungan / Akses")%></span>
            
            <div class="btn-group btn-group-sm">
                <button class="btn btn-light border text-dark fw-semibold px-3 shadow-sm hover-shadow-sm" onclick="tampilQrPertemuan<%=pertemuan.getId()%>()"><i class="fas fa-qrcode me-1 text-secondary"></i> <%=Common.getBahasaConfig("Tampilkan QR Code")%></button>
                <button class="btn btn-light border text-dark fw-semibold px-3 shadow-sm hover-shadow-sm" onclick="loadModalContent('<%=Common.getBahasaConfigJS("Evaluasi dan Penilaian")%>', '<%=linkAbsen%>&defaultTab=hasil')"><i class="fas fa-star-half-alt me-1 text-warning"></i> <%=Common.getBahasaConfig("Evaluasi / Penilaian")%></button>
            </div>
        </div>

    </div>
</div>

<!-- ═══════════════ MODAL QR CODE PERTEMUAN <%=pertemuan.getId()%> ═══════════════ -->
<div class="modal fade" id="modalQrPtm<%=pertemuan.getId()%>" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered" style="max-width:360px;">
    <div class="modal-content border-0 shadow-lg rounded-4">
      <div class="modal-header border-0 pb-0">
        <h6 class="modal-title fw-bold"><i class="fas fa-qrcode me-2 text-primary"></i><%=Common.getBahasaConfig("QR Code Presensi")%></h6>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body text-center py-3">
        <p class="text-muted small mb-3"><%=Common.getBahasaConfig("Tunjukkan QR code ini kepada peserta untuk konfirmasi kehadiran.")%></p>
        <div id="qrWrap<%=pertemuan.getId()%>" class="d-inline-block bg-white p-3 border rounded-3 shadow-sm mb-3"></div>
        <div class="small text-muted fw-semibold"><%=pertemuan.getTopik() != null ? pertemuan.getTopik() : "Pertemuan" %></div>
        <div class="small text-secondary">ID: <%=pertemuan.getId()%></div>
      </div>
    </div>
  </div>
</div>

<script>
/* ── Lazy-load daftar peserta kelas ───────────────────────────── */
(function() {
    var collapseEl = document.getElementById('peserta-<%=pertemuan.getId()%>');
    if (!collapseEl) return;
    collapseEl.addEventListener('show.bs.collapse', function onShow() {
        collapseEl.removeEventListener('show.bs.collapse', onShow);
        var el = document.getElementById('peserta-list-<%=pertemuan.getId()%>');
        if (!el) return;
        var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=_peserta_service&id=<%=pesertaVopId%>&jenis=<%=pesertaVopJenis%>';
        fetch(url)
            .then(function(r){ return r.json(); })
            .then(function(arr) {
                if (!arr || arr.length === 0) {
                    el.innerHTML = '<p class="text-muted text-center w-100 small py-2"><%=Common.getBahasaConfig("Belum ada peserta terdaftar")%>.</p>';
                    return;
                }
                var html = '';
                for (var i = 0; i < arr.length; i++) {
                    var m = arr[i];
                    html += '<div class="d-flex align-items-center bg-white border rounded-pill pe-3 shadow-sm" style="width:fit-content;">';
                    html += '<img src="' + m.foto + '" class="rounded-circle me-2 border" style="width:35px;height:35px;object-fit:cover;" onerror="this.src=\'<%=Common.ROOT%>/images/default_user.png\'">';
                    html += '<div class="py-1"><small class="d-block fw-bold text-dark" style="font-size:.75rem;">' + m.nama + '</small>';
                    html += '<small class="text-muted" style="font-size:.7rem;">' + m.nim + '</small></div></div>';
                }
                el.innerHTML = html;
            })
            .catch(function(e) { el.innerHTML = '<p class="text-danger small text-center w-100 py-2">Gagal memuat peserta: ' + e.message + '</p>'; });
    });
})();

/* ── Tampilkan QR Code Pertemuan ──────────────────────────────── */
function tampilQrPertemuan<%=pertemuan.getId()%>() {
    var wrap = document.getElementById('qrWrap<%=pertemuan.getId()%>');
    if (!wrap) return;
    wrap.innerHTML = '';
    var url = '<%=Common.ROOT%>/baru?_mob=1&p=elearning&s=load_ringkasan&pertemuan=<%=pertemuan.getId()%>';
    if (typeof QRCode !== 'undefined') {
        new QRCode(wrap, { text: url, width: 200, height: 200, correctLevel: QRCode.CorrectLevel.M });
    } else {
        wrap.innerHTML = '<div class="text-danger small p-2"><%=Common.getBahasaConfig("Library QR Code belum dimuat")%></div>';
    }
    var modal = new bootstrap.Modal(document.getElementById('modalQrPtm<%=pertemuan.getId()%>'));
    modal.show();
    document.getElementById('modalQrPtm<%=pertemuan.getId()%>').addEventListener('hidden.bs.modal', function() {
        if (typeof generatePertemuanHTML === 'function') generatePertemuanHTML(<%=pertemuan.getId()%>);
    }, { once: true });
}

/* ── Reload pertemuan ini saat modal apapun di dalamnya ditutup ── */
(function() {
    var card = document.querySelector('.card[data-id="<%=pertemuan.getId()%>"]');
    if (!card) return;
    card.addEventListener('click', function(e) {
        if (!e.target.closest('button[onclick]')) return;
        /* Cari modal yang baru saja dibuka (50ms cukup, injeksi modal sinkron) */
        setTimeout(function() {
            var modals = document.querySelectorAll('.modal:not(#modalQrPtm<%=pertemuan.getId()%>)');
            for (var i = modals.length - 1; i >= 0; i--) {
                var dlg = modals[i].querySelector('.modal-dialog');
                if (dlg && !dlg._cleanup) {
                    dlg._cleanup = (function() {
                        return function() {
                            if (typeof generatePertemuanHTML === 'function') generatePertemuanHTML(<%=pertemuan.getId()%>);
                        };
                    })();
                    break;
                }
            }
        }, 50);
    });
})();
</script>