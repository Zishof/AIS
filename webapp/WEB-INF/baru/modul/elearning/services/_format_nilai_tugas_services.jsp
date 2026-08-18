<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="org.hibernate.Session"%>
<%@ page import="org.hibernate.criterion.Restrictions"%>
<%@ page import="org.hibernate.criterion.Projections"%>
<%@ page import="org.hibernate.criterion.Order"%>
<%@ page import="ais.database.hibernate.HibernateUtil"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.ConstantValues"%>
<%@ page import="ais.database.model.*"%>
<%@ page import="ais.database.model.sekolah.*"%>

<%
    Session sess = null;
    try {
        // Buka Session Database Native
        sess = HibernateUtil.openSession();

        // Siapkan ID Modal / RND untuk mengunci Scope UI
        String contentModal = request.getParameter("contentModal");
        String rnd = contentModal != null && !contentModal.trim().isEmpty() ? contentModal : Common.getGeneratedBarCode(7);
        
        // Script penutup modal fallback
        String scriptCloseModal = "if(typeof closeModal === 'function'){closeModal();}else if(typeof window.parent !== 'undefined' && typeof window.parent.closeModal === 'function'){window.parent.closeModal();}else if(typeof $ !== 'undefined'){$('#wrapper-format-nilai-"+rnd+"').closest('.modal').modal('hide');}";

        // =========================================================================================
        // 1. BLOK PROSES POST AJAX (SIMPAN / SYNC)
        // =========================================================================================
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String action = request.getParameter("action");
            String idTugasPost = request.getParameter("idTugas");
            String jenisPost = request.getParameter("jenisTugas");
            
            // --- BLOK A: AKSI SINKRONISASI NILAI (MENGEMBALIKAN JSON) ---
            if ("sync".equals(action)) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                JSONObject resp = new JSONObject();
                
                if (idTugasPost == null || idTugasPost.trim().isEmpty()) {
                    resp.put("status", "error").put("message", Common.getBahasaConfig("ID Tugas tidak ditemukan."));
                    out.print(resp.toString());
                    return;
                }
                
                Tugas tugasPost = null;
                Long parsedId = Long.parseLong(idTugasPost);
                if ("TugasPertemuan".equalsIgnoreCase(jenisPost)) {
                    tugasPost = (Tugas) sess.get(TugasPertemuan.class, parsedId);
                } else if ("TugasKelompok".equalsIgnoreCase(jenisPost)) {
                    tugasPost = (Tugas) sess.get(TugasKelompok.class, parsedId);
                } else {
                    tugasPost = (Tugas) sess.get(Pertemuan.class, parsedId);
                }
                
                if (tugasPost != null) {
                    Perkuliahan pKuliah = null;
                    if (tugasPost instanceof Pertemuan) {
                        pKuliah = ((Pertemuan) tugasPost).getPerkuliahan();
                    } else if (tugasPost instanceof TugasPertemuan && ((TugasPertemuan) tugasPost).ambilPertemuan() != null) {
                        pKuliah = ((TugasPertemuan) tugasPost).ambilPertemuan().getPerkuliahan();
                    } else if (tugasPost instanceof TugasKelompok && ((TugasKelompok) tugasPost).ambilPertemuan() != null) {
                        pKuliah = ((TugasKelompok) tugasPost).ambilPertemuan().getPerkuliahan();
                    }
                    
                    if (pKuliah != null) {
                        boolean isObe = false;
                        if (pKuliah.getKurikulum() != null && pKuliah.getKurikulum().apakahObe(pKuliah.getTahunAjaran(), pKuliah.getGanjilGenap())) {
                            isObe = true;
                        }

                        // Trigger Fungsi Grading Helper
                        if (isObe) {
                            ais.common.WebGradingHelper.sinkronObe(pKuliah, tugasPost.getFormatNilais(), Common.getCurrentUser(request));
                        } else {
                            ais.common.WebGradingHelper.sinkronNonObe(pKuliah, tugasPost.getFormatNilai(), Common.getCurrentUser(request));
                        }
                        
                        resp.put("status", "success").put("message", Common.getBahasaConfig("Nilai berhasil dikalkulasi dan disinkronkan ke rekap KRS Mahasiswa."));
                    } else {
                        resp.put("status", "error").put("message", Common.getBahasaConfig("Perkuliahan tidak ditemukan. Singkronisasi dibatalkan."));
                    }
                } else {
                    resp.put("status", "error").put("message", Common.getBahasaConfig("Data tugas tidak ditemukan."));
                }
                
                out.print(resp.toString());
                return; // Return karena ini adalah handler API JSON
            }
            
            // --- BLOK B: AKSI SIMPAN FORMAT NILAI (MENGEMBALIKAN SCRIPT JS) ---
            if (idTugasPost == null || idTugasPost.trim().isEmpty()) {
                out.print("<script>if(typeof tampilkanToast === 'function') { tampilkanToast('"+Common.getBahasaConfigJS("Gagal menyimpan: Parameter ID Tugas kosong.")+"', 'bg-danger'); } else { alert('"+Common.getBahasaConfigJS("Gagal menyimpan: Parameter ID Tugas kosong.")+"'); } " + scriptCloseModal + "</script>");
                return;
            }
            
            Tugas tugasPost = null;
            Long parsedId = Long.parseLong(idTugasPost);
            
            if ("TugasPertemuan".equalsIgnoreCase(jenisPost)) {
                tugasPost = (Tugas) sess.get(TugasPertemuan.class, parsedId);
            } else if ("TugasKelompok".equalsIgnoreCase(jenisPost)) {
                tugasPost = (Tugas) sess.get(TugasKelompok.class, parsedId);
            } else {
                tugasPost = (Tugas) sess.get(Pertemuan.class, parsedId);
            }
            
            if (tugasPost != null) {
                Perkuliahan pKuliah = null;
                if (tugasPost instanceof Pertemuan) {
                    pKuliah = ((Pertemuan) tugasPost).getPerkuliahan();
                } else if (tugasPost instanceof TugasPertemuan && ((TugasPertemuan) tugasPost).ambilPertemuan() != null) {
                    pKuliah = ((TugasPertemuan) tugasPost).ambilPertemuan().getPerkuliahan();
                } else if (tugasPost instanceof TugasKelompok && ((TugasKelompok) tugasPost).ambilPertemuan() != null) {
                    pKuliah = ((TugasKelompok) tugasPost).ambilPertemuan().getPerkuliahan();
                }
                
                if (pKuliah != null) {
                    boolean isObe = (pKuliah.getKurikulum() != null && pKuliah.getKurikulum().apakahObe(pKuliah.getTahunAjaran(), pKuliah.getGanjilGenap()));
                    
                    if (isObe) {
                        String jsonStr = tugasPost.getFormatNilais();
                        JSONObject jsonObject = (jsonStr != null && !jsonStr.trim().isEmpty()) ? new JSONObject(jsonStr) : new JSONObject();
                        List<FormatNilai> fns = Common.getFormatNilais(sess, pKuliah);
                        
                        for (FormatNilai fn : fns) {
                            if (fn.getStatusPertemuan() != null) {
                                String chk = request.getParameter("chkFormatNilai_" + fn.getId());
                                if (chk != null && !chk.isEmpty()) {
                                    String bobotStr = request.getParameter("bobot_" + fn.getId());
                                    double bobot = (bobotStr != null && !bobotStr.trim().isEmpty()) ? Double.parseDouble(bobotStr) : 100.0;
                                    jsonObject.put(fn.getId().toString(), bobot);
                                } else {
                                    jsonObject.remove(fn.getId().toString());
                                }
                            }
                        }
                        tugasPost.setFormatNilais(jsonObject.toString());
                    } else {
                        String fnIdStr = request.getParameter("formatNilaiId");
                        if (fnIdStr != null && !fnIdStr.trim().isEmpty()) {
                            FormatNilai fn = (FormatNilai) sess.get(FormatNilai.class, Long.parseLong(fnIdStr));
                            tugasPost.setFormatNilai(fn);
                            String prosentaseStr = request.getParameter("prosentaseNilai");
                            double prosentase = (prosentaseStr != null && !prosentaseStr.trim().isEmpty()) ? Double.parseDouble(prosentaseStr) : 100.0;
                            tugasPost.setProsentase(prosentase);
                        } else {
                            tugasPost.setFormatNilai(null);
                            tugasPost.setProsentase(null);
                        }
                    }
                } else {
                   String jenisItemId = request.getParameter("jenisItemPenilaianId");
                   if (jenisItemId != null && !jenisItemId.trim().isEmpty()) {
                       String[] parts = jenisItemId.split("_");
                       if(parts.length >= 3) {
                           JenisItemPenilaianSiswa jips = (JenisItemPenilaianSiswa) sess.get(JenisItemPenilaianSiswa.class, Long.parseLong(parts[0]));
                           GrupPenilaian gp = (GrupPenilaian) sess.get(GrupPenilaian.class, Long.parseLong(parts[1]));
                           GrupKategoriItemPenilaianSiswa gkp = (GrupKategoriItemPenilaianSiswa) sess.get(GrupKategoriItemPenilaianSiswa.class, Long.parseLong(parts[2]));
                           tugasPost.setJenisItemPenilaianSiswa(jips);
                           tugasPost.setGrupPenilaian(gp);
                           tugasPost.setGrupKategoriItemPenilaianSiswa(gkp);
                       }
                   } else {
                       tugasPost.setJenisItemPenilaianSiswa(null);
                       tugasPost.setGrupPenilaian(null);
                       tugasPost.setGrupKategoriItemPenilaianSiswa(null);
                   }
                }
                
                Common.refreshUpdate(sess, tugasPost);
                out.print("<script>if(typeof tampilkanToast === 'function') { tampilkanToast('"+Common.getBahasaConfigJS("Format nilai berhasil disimpan.")+"', 'bg-success'); } else { alert('"+Common.getBahasaConfigJS("Format nilai berhasil disimpan.")+"'); } " + scriptCloseModal + "</script>");
            }
            return;
        }

        // =========================================================================================
        // 2. BLOK TAMPILAN ANTARMUKA (GET)
        // =========================================================================================
        String idTugasStr = request.getParameter("idTugas");
        String jenis = request.getParameter("jenis");
        
        if (idTugasStr == null || idTugasStr.trim().isEmpty()) {
            out.println("<div class='alert alert-danger m-3'><i class='fas fa-exclamation-triangle'></i> " + Common.getBahasaConfig("Parameter referensi tugas tidak valid.") + "</div>");
            return;
        }
        
        Long idTugas = Long.parseLong(idTugasStr);
        Tugas tugas = null;

        if ("TugasPertemuan".equalsIgnoreCase(jenis)) {
            tugas = (Tugas) sess.get(TugasPertemuan.class, idTugas);
        } else if ("TugasKelompok".equalsIgnoreCase(jenis)) {
            tugas = (Tugas) sess.get(TugasKelompok.class, idTugas);
        } else {
            tugas = (Tugas) sess.get(Pertemuan.class, idTugas);
        }

        if (tugas == null) {
            out.println("<div class='alert alert-danger m-3'><i class='fas fa-exclamation-triangle'></i> " + Common.getBahasaConfig("Data tugas tidak ditemukan di dalam sistem.") + "</div>");
            return;
        }
        
        Perkuliahan perkuliahan = null;
        JadwalPelajaran jadwalPelajaran = null;
        
        if (tugas instanceof Pertemuan) {
            perkuliahan = ((Pertemuan) tugas).getPerkuliahan();
            jadwalPelajaran = ((Pertemuan) tugas).getJadwalPelajaran();
        } else if (tugas instanceof TugasPertemuan) {
            Pertemuan pp = ((TugasPertemuan) tugas).ambilPertemuan();
            if(pp != null) {
                perkuliahan = pp.getPerkuliahan();
                jadwalPelajaran = pp.getJadwalPelajaran();
            }
        } else if (tugas instanceof TugasKelompok) {
            Pertemuan pp = ((TugasKelompok) tugas).ambilPertemuan();
            if(pp != null) {
                perkuliahan = pp.getPerkuliahan();
                jadwalPelajaran = pp.getJadwalPelajaran();
            }
        }
%>

<div class="container-fluid py-3" id="wrapper-format-nilai-<%=rnd%>">
    <form id="formFormatNilai_<%=rnd%>" onsubmit="submitFormatNilai<%=rnd%>(event)">
        <input type="hidden" name="idTugas" value="<%= idTugas %>" />
        <input type="hidden" name="jenisTugas" value="<%= jenis %>" />
        
        <div class="alert alert-info border-0 shadow-sm rounded-3 d-flex align-items-center mb-4 p-3 bg-gradient">
            <i class="fas fa-info-circle fa-2x text-info me-3"></i> 
            <div>
                <strong class="text-dark"><%= Common.getBahasaConfig("Pengaturan Integrasi Penilaian") %></strong><br>
                <small class="text-secondary"><%= Common.getBahasaConfig("Nilai yang diberikan pada tugas ini akan terintegrasi langsung dengan master format nilai atau rapor yang Anda tentukan di bawah ini.") %></small>
            </div>
        </div>

        <div class="card shadow-sm border-0 mb-4 rounded-4 overflow-hidden">
            <div class="card-header bg-white border-bottom pt-3 pb-2">
                <h6 class="fw-bold text-primary mb-0">
                    <i class="fas fa-sliders-h me-2"></i><%= Common.getBahasaConfig("Konfigurasi Format Penilaian") %>
                </h6>
            </div>
            <div class="card-body p-4 bg-light bg-opacity-50">
                
        <% if (perkuliahan != null) { 
            List<FormatNilai> formatNilais = Common.getFormatNilais(sess, perkuliahan);
            boolean isObe = (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap()));
            boolean isLocked = perkuliahan.getDikunci() != null;
            
            if (isObe) {
                if (!formatNilais.isEmpty()) {
                    boolean sudahadasubCpmk = false;
                    for (FormatNilai nilai : formatNilais) {
                        if (nilai.getNama().toLowerCase().contains("cpmk")) { sudahadasubCpmk = true; break; }
                    }
                    if (!sudahadasubCpmk) { formatNilais = Common.getFormatNilais(perkuliahan, true); }
                }

                String jsonStr = tugas.getFormatNilais();
                JSONObject jsonObe = (jsonStr != null && !jsonStr.trim().isEmpty()) ? new JSONObject(jsonStr) : new JSONObject();
        %>
                <div class="mb-3">
                    <label class="form-label fw-bold text-dark"><i class="far fa-check-square me-2 text-primary"></i><%= Common.getBahasaConfig("Penilaian Berbasis OBE (Pilih Sub-CPMK Terkait)") %></label>
                    <p class="text-muted small mb-3"><%= Common.getBahasaConfig("Tandai Sub-CPMK mana saja yang akan dievaluasi melalui tugas ini, lalu tentukan proporsi bobotnya.") %></p>
                </div>

                <div class="table-responsive rounded-3 border shadow-sm">
                    <table class="table table-hover align-middle mb-0 bg-white">
                        <thead class="bg-primary text-white">
                            <tr>
                                <th class="text-center" width="8%"><i class="fas fa-check"></i></th>
                                <th><%= Common.getBahasaConfig("Format Nilai (Sub-CPMK)") %></th>
                                <th width="25%"><%= Common.getBahasaConfig("Bobot Tugas (%)") %></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% 
                            boolean adaDataObe = false;
                            for (FormatNilai nilai : formatNilais) { 
                                 if (nilai.getStatusPertemuan() != null) { 
                                     adaDataObe = true;
                                     boolean isChecked = !jsonObe.isNull(nilai.getId().toString());
                                     double bobotVal = isChecked ? jsonObe.getDouble(nilai.getId().toString()) : 100.0;
                            %>
                            <tr>
                                <td class="text-center">
                                    <div class="form-check form-switch d-flex justify-content-center">
                                        <input class="form-check-input fs-5 cursor-pointer chk-obe-<%=rnd%>" type="checkbox" 
                                               name="chkFormatNilai_<%= nilai.getId() %>" 
                                               id="chk_<%= nilai.getId() %>" 
                                               value="<%= nilai.getId() %>" 
                                               <%= isChecked ? "checked" : "" %> 
                                               <%= isLocked ? "disabled" : "" %>
                                               onchange="toggleBobot<%=rnd%>('<%= nilai.getId() %>')">
                                    </div>
                                </td>
                                <td>
                                    <label for="chk_<%= nilai.getId() %>" class="mb-0 cursor-pointer d-block fw-semibold text-dark">
                                        <%= nilai.getNama() %> 
                                        <span class="badge bg-info text-dark ms-2 shadow-sm"><%= Common.getBahasaConfig("Bobot Master:") %> <%= Common.numberFormat.get().format(nilai.getPersen()) %>%</span>
                                    </label>
                                </td>
                                <td>
                                    <div class="input-group input-group-sm">
                                        <span class="input-group-text bg-light text-muted"><i class="fas fa-balance-scale"></i></span>
                                        <input type="number" step="0.1" min="0" max="100" 
                                               class="form-control text-center fw-bold input-bobot-<%=rnd%>" 
                                               id="bobot_<%= nilai.getId() %>" 
                                               name="bobot_<%= nilai.getId() %>" 
                                               value="<%= bobotVal %>" 
                                               <%= !isChecked || isLocked ? "readonly" : "" %> />
                                        <span class="input-group-text bg-light text-muted">%</span>
                                    </div>
                                </td>
                            </tr>
                            <%   } 
                               } 
                               if(!adaDataObe) {
                            %>
                                <tr><td colspan="3" class="text-center text-muted fst-italic py-4"><i class="fas fa-folder-open fa-2x mb-2 d-block opacity-50"></i><%= Common.getBahasaConfig("Data Sub-CPMK belum dikonfigurasi pada Master Perkuliahan.") %></td></tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
        <%  } else { 
                // NON-OBE
        %>
                <div class="mb-3">
                    <label class="form-label fw-bold text-dark"><i class="fas fa-list-ul me-2 text-primary"></i><%= Common.getBahasaConfig("Penilaian Konvensional (Non-OBE)") %></label>
                </div>
                <div class="row align-items-end bg-white p-3 rounded-3 border shadow-sm mx-0">
                    <div class="col-md-8 mb-3 mb-md-0">
                        <label class="form-label fw-bold text-secondary small text-uppercase"><%= Common.getBahasaConfig("Kategori Nilai") %> <span class="text-danger">*</span></label>
                        <select class="form-select form-select-lg border-primary" name="formatNilaiId" id="cbFormatNilai_<%=rnd%>" <%= isLocked ? "disabled" : "" %> onchange="cekFormatTunggal<%=rnd%>()">
                            <option value=""><%= Common.getBahasaConfig("-- Tidak Ada / Jangan Masukkan ke Rekapitulasi --") %></option>
                            <% for (FormatNilai nilai : formatNilais) { 
                                 if (nilai.getStatusPertemuan() != null) { 
                                     String sel = (tugas.getFormatNilai() != null && tugas.getFormatNilai().getId().equals(nilai.getId())) ? "selected" : "";
                            %>
                                <option value="<%= nilai.getId() %>" <%= sel %>><%= nilai.getNama() %> (<%= Common.getBahasaConfig("Bobot Master:") %> <%= Common.numberFormat.get().format(nilai.getPersen()) %>%)</option>
                            <%   } 
                               } %>
                        </select>
                    </div>
                    <div class="col-md-4" id="divProsentase_<%=rnd%>" style="<%= tugas.getFormatNilai() == null ? "display:none;" : "" %>">
                        <label class="form-label fw-bold text-secondary small text-uppercase"><%= Common.getBahasaConfig("Proporsi Nilai Tugas") %></label>
                        <div class="input-group input-group-lg">
                            <input type="number" step="0.1" class="form-control fw-bold text-center" name="prosentaseNilai" value="<%= tugas.getProsentase() != null ? tugas.getProsentase() : "100.0" %>" <%= isLocked ? "readonly" : "" %> />
                            <span class="input-group-text bg-light">%</span>
                        </div>
                    </div>
                </div>
        <%  } %>

                <% if(isLocked) { %>
                    <div class="alert alert-warning py-3 mt-4 border-0 shadow-sm d-flex align-items-center">
                        <i class="fas fa-lock fa-2x text-warning me-3"></i> 
                        <div>
                            <strong><%= Common.getBahasaConfig("Pemberitahuan Sistem") %></strong><br>
                            <span class="small"><%= Common.getBahasaConfig("Sistem penilaian telah dikunci oleh Administrator atau Program Studi. Anda tidak lagi diizinkan untuk mengubah struktur format nilai.") %></span>
                        </div>
                    </div>
                <% } %>

        <% } else if (jadwalPelajaran != null) { 
            boolean isLocked = jadwalPelajaran.getDikunci() != null;
            KelasSiswa kelasSiswa = jadwalPelajaran.getKelas();
            JenisPenilaian jenisPenilaian = jadwalPelajaran.getMatapelajaran() != null ? jadwalPelajaran.getMatapelajaran().getJenisPenilaian() : null;
            if (jadwalPelajaran.getKurikulumPunyaMatapelajaran() != null && jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah() != null && jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah().getJenisPenilaian() != null) {
                jenisPenilaian = jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah().getJenisPenilaian();
            }

            List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(sess.createCriteria(DetailJenisPenilaian.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
                    .setProjection(Projections.groupProperty("grupPenilaian.id")), GrupPenilaian.class, false);

            List<JenisItemPenilaianSiswa> validItems = new ArrayList<JenisItemPenilaianSiswa>();
            Map<Long, GrupPenilaian> mapGp = new HashMap<>();
            Map<Long, GrupKategoriItemPenilaianSiswa> mapGkp = new HashMap<>();

            for (GrupPenilaian grupPenilaian : grupPenilaians) {
                if (grupPenilaian != null && kelasSiswa != null && kelasSiswa.getTingkat() > 0 && grupPenilaian.getKhususTingkat() != null && !grupPenilaian.getKhususTingkat().equals(kelasSiswa.getTingkat())) { continue; }
                
                List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues.simpleList(sess.createCriteria(DetailGrupPenilaian.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa"))
                        .setProjection(Projections.groupProperty("grupKategoriItemPenilaianSiswa.id"))
                        .add(Restrictions.eq("grupPenilaian", grupPenilaian)), GrupKategoriItemPenilaianSiswa.class, false);

                if (!grupKategoriItemPenilaianSiswas.isEmpty()) {
                    Collections.sort(grupKategoriItemPenilaianSiswas);
                    for (GrupKategoriItemPenilaianSiswa gKips : grupKategoriItemPenilaianSiswas) {
                        if (gKips != null && kelasSiswa != null && kelasSiswa.getTingkat() > 0 && gKips.getKhususTingkat() != null && !gKips.getKhususTingkat().equals(kelasSiswa.getTingkat())) { continue; }
                        
                        List<KategoriItemPenilaianSiswa> kipsId = ConstantValues.simpleList(sess.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
                                .add(Restrictions.eq("grupKategoriItemPenilaianSiswa", gKips))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .setProjection(Projections.groupProperty("kategoriItemPenilaianSiswa.id")), KategoriItemPenilaianSiswa.class, false);
                        
                        if(!kipsId.isEmpty()){
                            List<JenisItemPenilaianSiswa> jips = ConstantValues.simpleList(sess.createCriteria(JenisItemPenilaianSiswa.class)
                                    .createAlias("kategoriItemPenilaianSiswa", "kategoriItemPenilaianSiswa")
                                    .addOrder(Order.asc("kategoriItemPenilaianSiswa.kode"))
                                    .addOrder(Order.asc("nomorUrut"))
                                    .add(Restrictions.in("kategoriItemPenilaianSiswa", kipsId))
                                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), JenisItemPenilaianSiswa.class);

                            for (JenisItemPenilaianSiswa jp : jips) {
                                if (jp.getTipeDataInputan().equals(JenisItemPenilaianSiswa.ANGKA) || jp.getTipeDataInputan().equals(JenisItemPenilaianSiswa.TEXT_ANGKA)) {
                                    validItems.add(jp);
                                    mapGp.put(jp.getId(), grupPenilaian);
                                    mapGkp.put(jp.getId(), gKips);
                                }
                            }
                        }
                    }
                }
            }
        %>
            <div class="mb-3">
                <label class="form-label fw-bold text-dark"><i class="fas fa-graduation-cap me-2 text-primary"></i><%= Common.getBahasaConfig("Penilaian Rapor Sekolah") %></label>
                <p class="text-muted small mb-3"><%= Common.getBahasaConfig("Pilih komponen nilai rapor yang relevan untuk menampung hasil evaluasi tugas ini.") %></p>
            </div>
            
            <div class="bg-white p-4 rounded-3 border shadow-sm">
                <label class="form-label fw-bold text-secondary small text-uppercase"><%= Common.getBahasaConfig("Kategori / Komponen Rapor") %></label>
                <select class="form-select form-select-lg border-primary mb-2" name="jenisItemPenilaianId" <%= isLocked ? "disabled" : "" %>>
                    <option value=""><%= Common.getBahasaConfig("-- Tidak Ada / Jangan Masukkan ke Rapor --") %></option>
                    <% for(JenisItemPenilaianSiswa item : validItems) { 
                        GrupPenilaian gp = mapGp.get(item.getId());
                        GrupKategoriItemPenilaianSiswa gkp = mapGkp.get(item.getId());
                        String desc = gkp.getNama() + " (" + gp.getNama() + ")";
                        String sel = (tugas.getJenisItemPenilaianSiswa() != null && tugas.getJenisItemPenilaianSiswa().getId().equals(item.getId())) ? "selected" : "";
                    %>
                        <option value="<%= item.getId() %>_<%= gp.getId() %>_<%= gkp.getId() %>" <%= sel %>>
                            <%= item.getNama() %> (<%= item.getKode() %>)  —  [ <%= desc %> ]
                        </option>
                    <% } %>
                </select>
                <div class="form-text text-muted fst-italic">
                    <i class="fas fa-info-circle me-1"></i><%= Common.getBahasaConfig("Struktur penamaan: Nama Item (Kode Item) — [ Kategori Grup ]") %>
                </div>
            </div>

            <% if(isLocked) { %>
                <div class="alert alert-warning py-3 mt-4 border-0 shadow-sm d-flex align-items-center">
                    <i class="fas fa-lock fa-2x text-warning me-3"></i> 
                    <div>
                        <strong><%= Common.getBahasaConfig("Pemberitahuan Sistem") %></strong><br>
                        <span class="small"><%= Common.getBahasaConfig("Sistem penilaian telah dikunci. Anda tidak lagi diizinkan untuk mengubah konfigurasi nilai rapor.") %></span>
                    </div>
                </div>
            <% } %>

        <% } %>
        
            </div>
        </div>

        <div class="d-flex justify-content-end gap-2 px-1 pb-2">
            <button type="button" class="btn btn-light border fw-bold text-secondary px-4 rounded-pill shadow-sm" onclick="tutupModalAman<%=rnd%>()">
                <i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Batalkan") %>
            </button>
            <button type="submit" class="btn btn-success fw-bold px-5 rounded-pill shadow-sm" id="btnSimpan_<%=rnd%>">
                <i class="fas fa-save me-2"></i><%= Common.getBahasaConfig("Simpan Pengaturan") %>
            </button>
        </div>
    </form>
</div>

<script>
    function tutupModalAman<%=rnd%>() {
        if (typeof closeModal === 'function') {
            closeModal();
        } else if (typeof window.parent !== 'undefined' && typeof window.parent.closeModal === 'function') {
            window.parent.closeModal();
        } else if (typeof $ !== 'undefined') {
            $('#wrapper-format-nilai-<%=rnd%>').closest('.modal').modal('hide');
        }
    }

    function toggleBobot<%=rnd%>(id) {
        var chk = document.getElementById("chk_" + id);
        var bobot = document.getElementById("bobot_" + id);
        if(chk && bobot) {
            if(chk.checked) {
                bobot.removeAttribute("readonly");
                bobot.classList.add("bg-white");
                bobot.classList.remove("bg-light");
            } else {
                bobot.setAttribute("readonly", "readonly");
                bobot.classList.add("bg-light");
                bobot.classList.remove("bg-white");
            }
        }
    }

    function cekFormatTunggal<%=rnd%>() {
        var cb = document.getElementById("cbFormatNilai_<%=rnd%>");
        var divP = document.getElementById("divProsentase_<%=rnd%>");
        if(cb && divP) {
            if(cb.value !== "") {
                divP.style.display = "block";
            } else {
                divP.style.display = "none";
            }
        }
    }

    function submitFormatNilai<%=rnd%>(e) {
        e.preventDefault();
        var form = document.getElementById("formFormatNilai_<%=rnd%>");
        
        var searchParams = new URLSearchParams();
        var formData = new FormData(form);
        for (var pair of formData.entries()) {
            searchParams.append(pair[0], pair[1]);
        }

        var btn = document.getElementById("btnSimpan_<%=rnd%>");
        var originalText = btn.innerHTML;
        
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> <%= Common.getBahasaConfig("Menyimpan Data...") %>';
        btn.disabled = true;

        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_format_nilai_tugas_services', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded' 
            },
            body: searchParams.toString()
        })
        .then(function(response){ return response.text(); })
        .then(function(htmlStr) {
            var tempDiv = document.createElement('div');
            tempDiv.innerHTML = htmlStr;
            var scripts = tempDiv.getElementsByTagName('script');
            for (var i = 0; i < scripts.length; i++) {
                eval(scripts[i].innerText);
            }
        })
        .catch(function(error) {
            if(typeof tampilkanToast === 'function') {
                 tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi kesalahan pada sistem saat menyimpan data.") %>', 'bg-danger');
            } else {
                 alert('<%= Common.getBahasaConfigJS("Terjadi kesalahan pada sistem saat menyimpan data.") %>');
            }
        })
        .finally(function() {
            if(document.getElementById("btnSimpan_<%=rnd%>")) {
                btn.innerHTML = originalText;
                btn.disabled = false;
            }
        });
    }

    cekFormatTunggal<%=rnd%>();
</script>

<%
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_format_nilai_tugas_services.jsp:565");
        out.println("<div class='alert alert-danger m-3'><i class='fas fa-exclamation-triangle'></i> Terjadi kesalahan internal: " + e.getMessage() + "</div>");
    } finally {
        try { if (sess != null) sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_format_nilai_tugas_services.jsp:568");}
        try { if (sess != null) sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_format_nilai_tugas_services.jsp:569");}
        HibernateUtil.closeSessionQuietly(sess);
    }
%>