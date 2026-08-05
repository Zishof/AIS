<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*"%>
<%@ page import="org.hibernate.*"%>
<%@ page import="org.hibernate.criterion.*"%>
<%@ page import="ais.database.hibernate.HibernateUtil"%>
<%@ page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@ page import="ais.database.model.*"%>
<%@ page import="ais.database.model.file.PertemuanFileContent"%>
<%@ page import="ais.database.model.streaming.AudioPertemuan"%>
<%@ page import="ais.database.model.streaming.VideoPertemuan"%>
<%@ page import="ais.common.*"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if(tbmuser == null || tbmuser.getUserId() == null){
        return;
    }

    String paramItem = request.getParameter("item");
    if(paramItem == null || paramItem.isEmpty()){
        return;
    }

    Session sess = null;
    Session sessStream = null;
    try {
        sess = HibernateUtil.openSession();
        try { sessStream = StreamingHibernateUtil.openSession(); } catch (Exception exStr) { ais.common.ErrorAuditUtil.record(exStr, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:28");}

        out.print("<style>");
        out.print(".stat-box-reload { background-color: #f8f9fa; border: 1px solid #edf2f7; border-radius: 6px; padding: 8px 4px; text-align: center; height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; transition: all 0.2s; }");
        out.print(".stat-box-reload:hover { background-color: #e9ecef; border-color: #dee2e6; }");
        out.print(".stat-box-reload i { font-size: 14px; margin-bottom: 4px; }");
        out.print(".stat-label-reload { font-size: 9.5px; font-weight: 600; color: #555; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; }");
        out.print(".card-ringkasan-reload { transition: all 0.3s ease; border: 1px solid #eef2f5 !important; }");
        out.print(".card-ringkasan-reload:hover { box-shadow: 0 .5rem 1rem rgba(0,0,0,.1)!important; transform: translateY(-2px); }");
        out.print("</style>");

        for(String itemId : paramItem.split(",")){
            Perkuliahan p = (Perkuliahan) sess.get(Perkuliahan.class, Long.parseLong(itemId.trim()));
            if(p != null) {

                // ── 1. Data Utama ──────────────────────────────────────────────────────────
                String title = p.getMatakuliah() != null ? p.getMatakuliah().getNama() : "Tanpa Matakuliah";
                String sub1 = (p.getMatakuliah() != null ? p.getMatakuliah().getSks() + " SKS" : "0 SKS") + " - " + (p.getMatakuliah() != null ? p.getMatakuliah().getKode() : "");
                String sub2 = (p.getHari() != null ? p.getHari() : "") + " | " + (p.getWaktuMulai() != null ? p.getWaktuMulai() : "00.00") + "-" + (p.getWaktuSelesai() != null ? p.getWaktuSelesai() : "00.00") + " | " + (p.getRuang() != null ? p.getRuang().getNama() : "");
                String sub3 = "TA: " + (p.getTahunAjaran() != null ? p.getTahunAjaran() : "") + " | Smt: " + p.getSemester() + (p.getKelas() != null ? " | Kls: " + p.getKelas() : "");

                // ── 2. Data Pengajar ───────────────────────────────────────────────────────
                String pengajarNama = Common.getBahasaConfig("Belum Ada Keterangan");
                String pengajarFoto = Common.ROOT + "/images/default_user.png";
                List<Long> dIds = p.populateDosenBuId();
                int dCount = 0;
                if (dIds != null && !dIds.isEmpty()) {
                    Dosen dd = (Dosen) sess.get(Dosen.class, dIds.get(0));
                    if (dd != null) {
                        pengajarNama = dd.getNama();
                        try {
                            String foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(dd));
                            if(foto != null && !foto.trim().isEmpty()){
                                pengajarFoto = (foto.startsWith("http") || foto.startsWith("data:") || foto.startsWith(Common.ROOT)) ? foto : Common.ROOT + (foto.startsWith("/") ? foto : "/" + foto);
                            }
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:63");}
                    }
                    dCount = dIds.size();
                }
                if (dCount > 1) pengajarNama += " (+" + (dCount - 1) + " " + Common.getBahasaConfig("Lainnya") + ")";

                // ── 3. Statistik Dasar ─────────────────────────────────────────────────────
                int mhsCount = 0;
                List<Long> mIds = p.ambilMahasiswaById();
                List<Long> sIds = p.ambilSiswaById();
                mhsCount = (mIds != null ? mIds.size() : 0) + (sIds != null ? sIds.size() : 0);
                int pertCount = p.ambilJumlahPertemuan() != null ? p.ambilJumlahPertemuan() : 0;

                boolean isObe = false;
                String idKur = "";
                if(p.getKurikulum() != null) isObe = p.getKurikulum().apakahObe(p.getTahunAjaran(), p.getGanjilGenap());
                if(p.ambilKurikulumPunyaMatakuliah() != null) idKur = p.ambilKurikulumPunyaMatakuliah().getId().toString();

                String propName = "perkuliahan";
                String jenisVop = "Perkuliahan";

                // ── 4. Ujian, Tugas, Tugas Kelompok ───────────────────────────────────────
                long cUjian = 0, cTugas = 0, cTugasKlp = 0;
                try {
                    Long lUjian = (Long) sess.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "pt").add(Restrictions.eq("pt.perkuliahan", p)).setProjection(Projections.rowCount()).uniqueResult();
                    if (lUjian != null) cUjian = lUjian;
                    Long lTugas1 = (Long) sess.createCriteria(Pertemuan.class).add(Restrictions.eq("perkuliahan", p)).add(Restrictions.isNotNull("judultugas")).add(Restrictions.ne("judultugas", "")).setProjection(Projections.rowCount()).uniqueResult();
                    Long lTugas2 = (Long) sess.createCriteria(TugasPertemuan.class).createAlias("pertemuanData", "pt").add(Restrictions.eq("pt.perkuliahan", p)).add(Restrictions.isNotNull("judultugas")).add(Restrictions.ne("judultugas", "")).setProjection(Projections.rowCount()).uniqueResult();
                    cTugas = (lTugas1 != null ? lTugas1 : 0) + (lTugas2 != null ? lTugas2 : 0);
                    Long lTugasKlp = (Long) sess.createCriteria(TugasKelompok.class).createAlias("pertemuanData", "pt").add(Restrictions.eq("pt.perkuliahan", p)).setProjection(Projections.rowCount()).uniqueResult();
                    if (lTugasKlp != null) cTugasKlp = lTugasKlp;
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:94");}

                // ── 5. Diskusi (PertemuanPunyaDiskusi → pertemuan.perkuliahan) ─────────────
                long cDiskusi = 0;
                try {
                    Long lDiskusi = (Long) sess.createCriteria(PertemuanPunyaDiskusi.class)
                        .createAlias("pertemuan", "pJoin")
                        .add(Restrictions.eq("pJoin.perkuliahan", p))
                        .setProjection(Projections.rowCount())
                        .uniqueResult();
                    if (lDiskusi != null) cDiskusi = lDiskusi;
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:105");}

                // ── 6. Sesuai RPS (Pertemuan.sesuai = true) ───────────────────────────────
                long cSesuaiRps = 0;
                try {
                    Long lSesuai = (Long) sess.createCriteria(Pertemuan.class)
                        .add(Restrictions.eq("perkuliahan", p))
                        .add(Restrictions.eq("sesuai", Boolean.TRUE))
                        .setProjection(Projections.rowCount())
                        .uniqueResult();
                    if (lSesuai != null) cSesuaiRps = lSesuai;
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:116");}

                // ── 7. Buku Referensi (Pertemuan dgn bukuRujukan1/2 terisi) ───────────────
                long cBukuRef = 0;
                try {
                    Long lBukuRef = (Long) sess.createCriteria(Pertemuan.class)
                        .add(Restrictions.eq("perkuliahan", p))
                        .add(Restrictions.or(
                            Restrictions.and(Restrictions.isNotNull("bukuRujukan1"), Restrictions.ne("bukuRujukan1", "")),
                            Restrictions.and(Restrictions.isNotNull("bukuRujukan2"), Restrictions.ne("bukuRujukan2", ""))
                        ))
                        .setProjection(Projections.rowCount())
                        .uniqueResult();
                    if (lBukuRef != null) cBukuRef = lBukuRef;
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:130");}

                // ── 8. Buku Ajar (MatakuliahPunyaBukuBahanAjar) ───────────────────────────
                long cBukuAjar = 0;
                try {
                    if (p.getMatakuliah() != null) {
                        Long lBukuAjar = (Long) sess.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
                            .add(Restrictions.eq("matakuliah", p.getMatakuliah()))
                            .setProjection(Projections.rowCount())
                            .uniqueResult();
                        if (lBukuAjar != null) cBukuAjar = lBukuAjar;
                    }
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:142");}

                // ── 9. Audio, Video, Materi (streaming DB via pertemuan IDs) ──────────────
                long cAudio = 0, cVideo = 0, cMateri = 0;
                if (sessStream != null) {
                    List<Long> pertIds = null;
                    try {
                        pertIds = (List<Long>) sess.createCriteria(Pertemuan.class)
                            .add(Restrictions.eq("perkuliahan", p))
                            .setProjection(Projections.property("id"))
                            .list();
                    } catch (Exception exP) { ais.common.ErrorAuditUtil.record(exP, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:153");}

                    if (pertIds != null && !pertIds.isEmpty()) {
                        try {
                            Long lAudio = (Long) sessStream.createCriteria(AudioPertemuan.class)
                                .add(Restrictions.in("pertemuan", pertIds))
                                .setProjection(Projections.rowCount())
                                .uniqueResult();
                            if (lAudio != null) cAudio = lAudio;
                        } catch (Exception exA) { ais.common.ErrorAuditUtil.record(exA, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:162");}
                        try {
                            Long lVideo = (Long) sessStream.createCriteria(VideoPertemuan.class)
                                .add(Restrictions.in("pertemuan", pertIds))
                                .setProjection(Projections.rowCount())
                                .uniqueResult();
                            if (lVideo != null) cVideo = lVideo;
                        } catch (Exception exV) { ais.common.ErrorAuditUtil.record(exV, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:169");}
                        try {
                            Long lMateri = (Long) sessStream.createCriteria(PertemuanFileContent.class)
                                .add(Restrictions.in("pertemuan", pertIds))
                                .setProjection(Projections.rowCount())
                                .uniqueResult();
                            if (lMateri != null) cMateri = lMateri;
                        } catch (Exception exM) { ais.common.ErrorAuditUtil.record(exM, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:176");}
                    }
                }

                String safeTitle = title.replace("'", "%27").replace("\"", "&quot;");
                String vopParams = p.getId() + ", '" + jenisVop + "', '" + propName + "', '" + safeTitle + "', " + (isObe ? "true" : "false") + ", '" + idKur + "'";
                String jsCall = "var fn = Object.keys(window).find(k => k.startsWith('bukaModalDetail')); if(fn && typeof window[fn] === 'function') { window[fn](";
%>
        <div class="card mb-3 shadow-sm border-0 card-ringkasan-reload">
          <div class="card-body p-3">
             <div class="row m-0">
                <div class="col-md-2 col-lg-2 text-center mb-3 mb-md-0 d-flex flex-column align-items-center justify-content-center border-right-md py-1">
                   <img src="<%=pengajarFoto%>" class="rounded-circle mb-2 shadow-sm border" width="65" height="65" style="object-fit:cover; background-color:#fff; cursor: pointer;" onclick="tampilGambar(this)" alt="<%=pengajarNama%>" onerror="this.src='<%=Common.ROOT%>/images/default_user.png'">
                   <div style="font-size:12px; font-weight:bold; color:#333; line-height:1.2; word-break:break-word;" title="<%=pengajarNama%>"><%=pengajarNama%></div>
                   <div style="font-size:10px; color:#777; margin-top:3px; text-transform:uppercase;"><%=Common.getBahasaConfig("Pengajar Utama")%></div>
                </div>

                <div class="col-md-5 col-lg-5 mb-3 mb-md-0 px-md-3 py-1">
                   <h6 class="font-weight-bold text-primary mb-2" style="line-height:1.4;" title="<%=title%>"><%=title%></h6>
                   <div class="mb-3">
                       <% for(String part : sub1.split(" - ")) { if(!part.trim().isEmpty()) { %>
                           <span class="badge badge-light border text-dark mr-1 mb-1 px-2 py-1"><%=part.trim()%></span>
                       <% } } %>
                       <% for(String part : sub3.split("\\|")) { if(!part.trim().isEmpty()) { %>
                           <span class="badge badge-light border text-dark mr-1 mb-1 px-2 py-1"><%=part.trim()%></span>
                       <% } } %>
                   </div>
                   <ul class="list-unstyled mb-0" style="font-size: 12.5px; color: #555;">
                       <% if(sub2 != null && !sub2.trim().isEmpty()) { %>
                           <li class="mb-1"><i class="fas fa-check text-success mr-2"></i><%=sub2%></li>
                       <% } %>
                   </ul>
                </div>

                <div class="col-md-5 col-lg-5 py-1">
                   <div class="row no-gutters h-100">
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload" style="cursor:pointer;" onclick="<%=jsCall%>'peserta', <%=vopParams%>); }">
                               <i class="fas fa-users text-info"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Peserta")%> (<%=mhsCount%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload" style="cursor:pointer;" onclick="<%=jsCall%>'pertemuan', <%=vopParams%>); }">
                               <i class="fas fa-calendar-alt text-danger"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Pertemuan")%> (<%=pertCount%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload">
                               <i class="fas fa-check-circle text-warning"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Sesuai RPS")%> (<%=cSesuaiRps%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload">
                               <i class="fas fa-comments text-primary"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Diskusi")%> (<%=cDiskusi%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload">
                               <i class="fas fa-file-alt text-info"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Materi")%> (<%=cMateri%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload" style="cursor:pointer;" onclick="<%=jsCall%>'tugas', <%=vopParams%>); }">
                               <i class="fas fa-clipboard-list text-primary"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Tugas")%> (<%=cTugas%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload" style="cursor:pointer;" onclick="<%=jsCall%>'ujian', <%=vopParams%>); }">
                               <i class="fas fa-check-square text-success"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Ujian")%> (<%=cUjian%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload" style="cursor:pointer;" onclick="<%=jsCall%>'tugas_kelompok', <%=vopParams%>); }">
                               <i class="fas fa-users-cog text-info"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Tgs Kelompok")%> (<%=cTugasKlp%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload">
                               <i class="fas fa-microphone text-danger"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Audio")%> (<%=cAudio%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload">
                               <i class="fas fa-video text-primary"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Video")%> (<%=cVideo%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload">
                               <i class="fas fa-book text-warning"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Buku Ref.")%> (<%=cBukuRef%>)</div>
                           </div>
                       </div>
                       <div class="col-4 col-sm-3 col-md-3 col-lg-3 p-1">
                           <div class="stat-box-reload">
                               <i class="fas fa-book-open text-success"></i>
                               <div class="stat-label-reload"><%=Common.getBahasaConfig("Buku Ajar")%> (<%=cBukuAjar%>)</div>
                           </div>
                       </div>
                   </div>
                </div>
             </div>
          </div>
        </div>
<%
            }
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/load_ringkasan.jsp:293");
    } finally {
        ais.common.ElearningSessionUtil.closeQuietly(sess);
        HibernateUtil.closeSessionQuietly(sess);
        StreamingHibernateUtil.closeSessionQuietly(sessStream);
    }
%>
