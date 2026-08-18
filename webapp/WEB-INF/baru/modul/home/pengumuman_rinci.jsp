<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.file.FileFoto"%>
<%@page import="ais.database.model.file.LampiranPengumumanAkademis"%>
<%@page import="ais.database.model.DiskusiPengumumanAkademis"%>
<%@page import="ais.database.model.PengumumanAkademis"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.jsoup.Jsoup"%>
<%@page import="java.io.File"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.TreeSet"%>
<%@page import="java.text.SimpleDateFormat"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. INITIALIZATION & PARAMETER PARSING
    String idParam = request.getParameter("id");
    Long id = (idParam == null || idParam.trim().isEmpty()) ? -1L : Long.parseLong(idParam.trim());
    
    Tbmuser tbmuser = Common.getCurrentUser(request);
    PengumumanAkademis pengumuman = (PengumumanAkademis) ConstantValues.ambil(PengumumanAkademis.class.getName(), id);

    // Jika data tidak ditemukan, stop proses
    if (pengumuman == null) {
        out.println("<div class='alert alert-danger'>Data pengumuman tidak ditemukan.</div>");
        return;
    }
    
    String via = request.getParameter("via") == null || request.getParameter("via").trim().isEmpty() ? "baru" : request.getParameter("via");

    // 2. ACTION HANDLING (DELETE / ADD COMMENT)
    String hapusId = request.getParameter("hapus");
    String komentarBaru = request.getParameter("tulisKomentar");
    
    Session dbSession = null;
    try {
        dbSession = HibernateUtil.openSession();

        // --- Logic Hapus Komentar ---
        if (hapusId != null && !hapusId.trim().isEmpty()) {
            try {
                Long idHapus = Long.parseLong(hapusId.trim());
                DiskusiPengumumanAkademis diskusi = (DiskusiPengumumanAkademis) dbSession.createCriteria(DiskusiPengumumanAkademis.class)
                        .add(Restrictions.idEq(idHapus)).uniqueResult();

                if (diskusi != null && tbmuser != null) {
                    // Cek Kepemilikan (Ownership Check)
                    boolean isOwner = false;
                    if (diskusi.getTbmuser() != null && tbmuser.getUserId().equals(diskusi.getTbmuser().getUserId())) {
                        isOwner = true;
                    } else if (diskusi.getMahasiswa() != null && tbmuser.getMahasiswa() != null 
                            && diskusi.getMahasiswa().getId().equals(tbmuser.getMahasiswa().getId())) {
                        isOwner = true;
                    }

                    if (isOwner) {
                        dbSession.beginTransaction();
                        dbSession.delete(diskusi);
                        dbSession.getTransaction().commit();
                    }
                }
            } catch (Exception e) {
                if (dbSession.getTransaction().isActive()) dbSession.getTransaction().rollback();
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/home/pengumuman_rinci.jsp:73");
            }
        } 
        // --- Logic Tambah Komentar ---
        else if (komentarBaru != null && !komentarBaru.trim().isEmpty() && tbmuser != null) {
            try {
                // Tentukan Label "Oleh"
                String labelOleh = tbmuser.getUserId(); // Default
                if (tbmuser.getMahasiswa() != null) labelOleh = tbmuser.getMahasiswa().getNim() + " - " + tbmuser.getMahasiswa().getNama() + " (Mahasiswa)";
                else if (tbmuser.ambilDosen() != null) labelOleh = tbmuser.ambilDosen().getNama() + " (Dosen)";
                else if (tbmuser.getSiswa() != null) labelOleh = tbmuser.getSiswa().getNama() + " (Siswa)";
                else if (tbmuser.ambilGuru() != null) labelOleh = tbmuser.ambilGuru().getNama() + " (Guru)";
                else if (tbmuser.hakAkses() != null) labelOleh = tbmuser.getUserNama() + " (" + tbmuser.hakAkses().getRoleName() + ")";

                DiskusiPengumumanAkademis newDiskusi = new DiskusiPengumumanAkademis();
                newDiskusi.setTanggal(ais.ui.util.WaktuUtil.getDate());
                newDiskusi.setOleh(labelOleh);
                newDiskusi.setPengguna(labelOleh);
                newDiskusi.setCatatan(komentarBaru);
                newDiskusi.setPengumumanAkademis(pengumuman);
                newDiskusi.setTbmuser(tbmuser);
                newDiskusi.setMahasiswa(tbmuser.getMahasiswa());
                newDiskusi.setDosen(tbmuser.ambilDosen());
                newDiskusi.setSiswa(tbmuser.getSiswa());

                dbSession.beginTransaction();
                dbSession.save(newDiskusi);
                dbSession.getTransaction().commit();
                
                // Update status (jika ada method ini)
                try { pengumuman.belum("diskusi"); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/pengumuman_rinci.jsp:103");}
                
            } catch (Exception e) {
                if (dbSession.getTransaction().isActive()) dbSession.getTransaction().rollback();
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/home/pengumuman_rinci.jsp:107");
            }
        }
    } finally {
        if (dbSession != null && dbSession.isOpen()) {
            dbSession.close();
        }
    }

    // 3. PREPARE DATA FOR VIEW
    String currentLang = (String) session.getAttribute("current_lang");
    if (currentLang == null) currentLang = Tbmuser.INDONESIA;

    // Non-Indonesia: terjemahkan otomatis dari teks Indonesia via TRANSLATER INTERNAL (mengikuti bahasa
    // aktif: English/Arab/Mandarin) — TIDAK disimpan ke DB. Konten pakai penerjemah SADAR-TAG.
    boolean _isIndoR = currentLang == null || currentLang.equals(Tbmuser.INDONESIA);
    String judul;
    String isi;
    if (_isIndoR) {
        judul = pengumuman.getJudul();
        isi = pengumuman.getCatatan();
    } else {
        judul = ais.common.Common.terjemahDinamis(pengumuman.getJudul());
        isi = ais.common.Common.terjemahDinamisHtml(pengumuman.getCatatan());
    }
    judul = (judul == null) ? "" : judul;
    isi = (isi == null) ? "" : isi;

    // Ambil Lampiran
    List<LampiranPengumumanAkademis> listLampiran = new ArrayList<LampiranPengumumanAkademis>();
    Session viewSession = HibernateUtil.openSession();
    try {
        listLampiran = viewSession.createCriteria(LampiranPengumumanAkademis.class)
                .add(Restrictions.eq("pengumumanAkademis", pengumuman))
                .addOrder(Order.desc("id")).list();
    } finally {
        viewSession.close();
    }

    // Ambil Diskusi
    TreeSet<Long> diskusiIds = pengumuman.ambilDiskusiPengumumanAkademisTotal(true, null, false);
    String userPhotoUrl = CommonMedia.getUrlFotoPengguna(tbmuser, 90, 80);
%>

<div class="card shadow-sm mb-4 animate__animated animate__fadeInUp">
    <div class="card-header bg-light py-3 border-bottom">
        <h5 class="mb-0 text-primary"><%=judul %></h5>
    </div>

    <div class="card-body">
        <div class="mb-3">
            <p class="mb-2"><%=isi %></p>
        </div>

        <% if (listLampiran != null && !listLampiran.isEmpty()) { %>
            <div class="border-top pt-3 mt-3">
                <h6 class="small text-muted mb-2">Lampiran:</h6>
                <% 
                for(LampiranPengumumanAkademis lampiran : listLampiran){
                    File file = ((FileFoto) lampiran).ambilFile();
                    if(file != null) {
                        if(file.getName().toLowerCase().endsWith("pdf")){
                            String linkPdf = LampiranLain.ambilLinkLampiranLain(file);
                %>
                            <div class="mb-2">
                                <jsp:include page="/WEB-INF/baru/componen/pdf2.jsp">
                                    <jsp:param name="pdf_url" value="<%=linkPdf%>" />
                                    <jsp:param name="download" value="true" />
                                </jsp:include>
                            </div>
                <%      
                        } else {
                            String previewImg = CommonMedia.preview(lampiran);
                            if(!previewImg.isEmpty()){ 
                %>
                                <div class="mb-2"><%=previewImg %></div>
                <% 
                            }
                        }
                    }
                } 
                %>
            </div>
        <% } %>
    </div>

    <% if(!pengumuman.getKomentarDitutup()) { %>
    <div class="card-footer bg-light pt-3">
        
        <form id="formKomentar" action="<%=request.getContextPath()+"/"+via+"?"+request.getQueryString() %>" method="post" class="d-flex align-items-center mb-4">
            <input type="hidden" name="id" value="<%=id%>"/>  
            
            <div class="flex-shrink-0 me-2">
                <img class="rounded-circle shadow-sm" 
                     src="<%=userPhotoUrl %>" 
                     alt="User" 
                     style="width: 40px; height: 40px; object-fit: cover; cursor: pointer;"
                     onclick="tampilGambar(this)">
            </div>
            
            <div class="flex-grow-1 d-flex">
                <input id="tulisKomentar" name="tulisKomentar" 
                       class="form-control rounded-pill me-2" 
                       type="text" 
                       placeholder="<%=Common.getBahasaConfig("Tulis komentar...")%>" 
                       autocomplete="off" />
                       
                <button type="submit" class="btn btn-primary rounded-circle shadow-sm" 
                        style="width: 40px; height: 40px; padding: 0;"
                        title="Kirim Komentar">
                    <i class="fas fa-paper-plane"></i>
                </button>
            </div>
        </form>
        
        <script>
            document.getElementById('formKomentar').addEventListener('submit', function(e) {
                var inputKomentar = document.getElementById('tulisKomentar');
                if (!inputKomentar.value.trim()) {
                    e.preventDefault(); // Mencegah form terkirim
                    tampilkanToast("Maaf, komentar tidak boleh kosong!", 'bg-danger');
                    inputKomentar.focus();
                }
            });
        </script>

        <div class="comment-list">
            <%
            for(Long diskusiId : diskusiIds) {
                DiskusiPengumumanAkademis diskusi = (DiskusiPengumumanAkademis) DiskusiPengumumanAkademis.ambilData(DiskusiPengumumanAkademis.class, diskusiId.toString());
                
                if(diskusi != null) {
                	
                	try{
                	
                    // Logic Nama Penulis
                    String namaPenulis = diskusi.getOleh();
                    if (diskusi.getBiodataCalonMahasiswa() != null) namaPenulis = diskusi.getBiodataCalonMahasiswa().getNama() + " (Calon Mhs)";
                    else if (diskusi.getMahasiswa() != null) namaPenulis = diskusi.getMahasiswa().getNama() + " (Mhs)";
                    else if (diskusi.getDosen() != null) namaPenulis = diskusi.getDosen().getNama() + " (Dosen)";
                    else if (diskusi.getSiswa() != null) namaPenulis = diskusi.getSiswa().getNama() + " (Siswa)";
                    else if (diskusi.getGuru() != null) namaPenulis = diskusi.getGuru().getNama() + " (Guru)";
                    else if (diskusi.getTbmuser() != null) namaPenulis = diskusi.getTbmuser().getUserNama();
                    
                    // Logic Foto Penulis
                    Tbmuser userKomentar = diskusi.getTbmuser();
                    if(diskusi.getMahasiswa() != null) userKomentar = new Tbmuser(diskusi.getMahasiswa());
                    else if(diskusi.getSiswa() != null) userKomentar = new Tbmuser(diskusi.getSiswa());
                    String fotoPenulis = CommonMedia.getUrlFotoPengguna(userKomentar, 90, 80);

                    // Logic Isi Komentar
                    String isiDiskusi = diskusi.getCatatan();
                    try {
                        isiDiskusi = Jsoup.parse(isiDiskusi).text();
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/pengumuman_rinci.jsp:261");}

                    // Logic Hapus
                    boolean canDelete = false;
                    if (tbmuser != null) {
                        if (diskusi.getTbmuser() != null && tbmuser.getUserId().equals(diskusi.getTbmuser().getUserId())) canDelete = true;
                        else if (diskusi.getMahasiswa() != null && tbmuser.getMahasiswa() != null && diskusi.getMahasiswa().getId().equals(tbmuser.getMahasiswa().getId())) canDelete = true;
                    }
            %>
            <div class="d-flex mb-3">
                <div class="flex-shrink-0 me-2">
                    <img class="rounded-circle border" 
                         src="<%=fotoPenulis %>" 
                         alt="Avatar" 
                         style="width: 40px; height: 40px; object-fit: cover; cursor: pointer;"
                         onclick="tampilGambar(this)">
                </div>
                <div class="flex-grow-1">
                    <div class="bg-white border rounded-3 p-2 shadow-sm">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <a href="#!" class="fw-bold text-decoration-none small text-dark"><%=namaPenulis %></a>
                            <small class="text-muted" style="font-size: 0.75rem;">
                                <%=Common.dateFormat.get().format(diskusi.getTanggal()) %>
                            </small>
                        </div>
                        <p class="mb-0 small text-secondary"><%=isiDiskusi %></p>
                    </div>
                    <% if(canDelete){ %>
                        <div class="px-2 mt-1">
                            <a class="text-danger small text-decoration-none" 
                               onclick="return confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data ini?") %>');" 
                               href="<%=request.getContextPath()%>/<%=via%>?p=home&s=pengumuman_rinci&id=<%=pengumuman.getId()%>&hapus=<%=diskusi.getId()%>">
                               <i class="fas fa-trash-alt me-1"></i>Hapus
                            </a>
                        </div>
                    <% } %>
                </div>
            </div>
            <%  }catch(Exception e){
            	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/home/pengumuman_rinci.jsp:300");
            }
                }
            } %>
        </div>
    </div>
    <% } %>
</div>