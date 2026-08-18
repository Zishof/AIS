<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="org.hibernate.Session"%>
<%@ page import="org.hibernate.criterion.*"%>
<%@ page import="ais.database.hibernate.HibernateUtil"%>
<%@ page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.database.model.*"%>
<%@ page import="ais.database.model.file.*"%>
<%@ page import="ais.database.model.streaming.*"%>
<%@ page import="org.json.*"%>

<%
    // Hanya menerima metode komunikasi POST demi keamanan API
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return;
    }
    
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        out.print("{\"status\":\"error\", \"msg\":\"" + Common.getBahasaConfig("Sesi otentikasi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
        return;
    }

    // ==============================================================================
    // PENANGKAPAN PARAMETER PENYARING (FILTER) DARI ANTARMUKA
    // ==============================================================================
    String kategori = request.getParameter("kategori");
    String kataKunci = request.getParameter("q_cari");
    String q_ta = request.getParameter("q_ta");
    String q_smt = request.getParameter("q_smt");
    String q_fakultas = request.getParameter("q_fakultas");
    String q_jurusan = request.getParameter("q_jurusan");
    String q_yayasan = request.getParameter("q_yayasan");
    String q_sekolah = request.getParameter("q_sekolah");
    boolean refresh = "true".equalsIgnoreCase(request.getParameter("refresh"));

    if (kategori == null || kategori.trim().isEmpty()) {
        kategori = "perkuliahan"; 
    }

    // Identifikasi Peran Sivitas Akademika
    boolean isMhs = tbmuser.getMahasiswa() != null;
    boolean isSsw = tbmuser.getSiswa() != null;
    boolean isDsn = tbmuser.ambilDosen() != null;
    boolean isGru = tbmuser.ambilGuru() != null;
    
    String myId = "";
    if (isMhs) myId = tbmuser.getMahasiswa().getId().toString();
    else if (isSsw) myId = tbmuser.getSiswa().getId().toString();
    else if (isDsn) myId = tbmuser.ambilDosen().getId().toString();
    else if (isGru) myId = tbmuser.ambilGuru().getId().toString();

    Session sess = null;
    Session sessionmy = null;
    JSONObject responseJson = new JSONObject();

    try {
        sess = HibernateUtil.openSession();
        sessionmy = StreamingHibernateUtil.getInstance().openSession();

        // ==============================================================================
        // LOGIKA KRITERIA PENYARINGAN PERTEMUAN BERDASARKAN KATEGORI & INSTITUSI
        // ==============================================================================
        org.hibernate.Criteria cp = sess.createCriteria(Pertemuan.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            
        if (kataKunci != null && !kataKunci.trim().isEmpty()) {
            cp.add(Restrictions.ilike("topik", kataKunci.trim(), MatchMode.ANYWHERE));
        }

        if ("perkuliahan".equals(kategori) || "tugas_akhir".equals(kategori) || "skripsi".equals(kategori) || "kkn".equals(kategori) || "pkl".equals(kategori)) {
            cp.createAlias("perkuliahan", "prk");
            if (q_ta != null && !q_ta.isEmpty()) cp.add(Restrictions.eq("prk.tahunAjaran", q_ta));
            if (q_smt != null && !q_smt.isEmpty()) cp.add(Restrictions.eq("prk.ganjilGenap", q_smt));
            
            // Filter Institusi Tinggi (Fakultas & Jurusan)
            if (q_fakultas != null && !q_fakultas.isEmpty()) {
                cp.createAlias("prk.matakuliah", "mk").createAlias("mk.jurusan", "jur").createAlias("jur.fakultas", "fak");
                cp.add(Restrictions.eq("fak.id", Long.parseLong(q_fakultas)));
            }
            if (q_jurusan != null && !q_jurusan.isEmpty()) {
                if (q_fakultas == null || q_fakultas.isEmpty()) cp.createAlias("prk.matakuliah", "mk").createAlias("mk.jurusan", "jur");
                cp.add(Restrictions.eq("jur.id", Long.parseLong(q_jurusan)));
            }
            
        } else if ("jadwal_pelajaran".equals(kategori) || "les".equals(kategori)) {
            cp.createAlias("jadwalPelajaran", "jp");
            if (q_ta != null && !q_ta.isEmpty()) cp.add(Restrictions.eq("jp.tahunAjaran", q_ta));
            if (q_smt != null && !q_smt.isEmpty()) {
                int smtInt = 1;
                if (q_smt.equalsIgnoreCase("Genap") || q_smt.equalsIgnoreCase(String.valueOf(Perkuliahan.GENAP))) smtInt = 2;
                else if (q_smt.equalsIgnoreCase("Pendek") || q_smt.equalsIgnoreCase(String.valueOf(Perkuliahan.SP))) smtInt = 3;
                cp.add(Restrictions.eq("jp.semester", smtInt));
            }
            
            // Filter Institusi Dasar/Menengah (Yayasan & Sekolah)
            if (q_yayasan != null && !q_yayasan.isEmpty()) {
                cp.createAlias("jp.sekolah", "sek").createAlias("sek.yayasan", "yay");
                cp.add(Restrictions.eq("yay.id", Long.parseLong(q_yayasan)));
            }
            if (q_sekolah != null && !q_sekolah.isEmpty()) {
                if (q_yayasan == null || q_yayasan.isEmpty()) cp.createAlias("jp.sekolah", "sek");
                cp.add(Restrictions.eq("sek.id", Long.parseLong(q_sekolah)));
            }
        }

        // Penyaringan Akses Menggunakan Kolom Terindeks Sivitas
        if (isMhs) cp.add(Restrictions.like("mahasiswas", "%," + myId + ",%"));
        else if (isDsn) cp.add(Restrictions.like("dosens", "%," + myId + ",%"));
        else if (isSsw) cp.add(Restrictions.like("siswas", "%," + myId + ",%"));
        else if (isGru) cp.add(Restrictions.like("gurus", "%," + myId + ",%"));

        // ==============================================================================
        // SOLUSI OPTIMASI BEBAN MEMORI (MENCEGAH ERROR OUT-OF-MEMORY PADA RATUSAN RIBU DATA)
        // ==============================================================================
        cp.setMaxResults(300); // Membatasi sistem hanya mengambil 300 riwayat terbaru
        
        @SuppressWarnings("unchecked")
        List<Pertemuan> listPertemuan = cp.addOrder(Order.desc("tanggal")).list();

        JSONArray jPertemuan = new JSONArray(); 
        JSONArray jMateri = new JSONArray();
        JSONArray jTugas = new JSONArray(); 
        JSONArray jUjian = new JSONArray();
        JSONArray jVideo = new JSONArray(); 
        JSONArray jAudio = new JSONArray();

        for (Pertemuan p : listPertemuan) {
            int totalMhs = p.ambilMahasiswa() != null ? p.ambilMahasiswa().size() : 0;
            int totalSsw = p.ambilSiswa() != null ? p.ambilSiswa().size() : 0;
            int totalPeserta = totalMhs + totalSsw;
            
            // Mengambil Informasi Ringkas Pembelajaran (Mata Kuliah / Kelas)
            String informasiVo = "";
            try {
                if (p.ambilVOPembelajaran() != null) {
                    informasiVo = p.ambilVOPembelajaran().infoSimple();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_dasbor_service.jsp:144");}

            String tglStr = p.getTanggal() != null ? Common.dateFormat6.get().format(p.getTanggal()) : "-";
            String wktStr = p.getWaktuMulai() != null ? p.getWaktuMulai() : "";
            String infoWaktu = tglStr + (wktStr.isEmpty() ? "" : " (" + wktStr + ")");
            
            // Penggabungan Informasi Meta
            String infoGabungan = informasiVo;
            if (!informasiVo.isEmpty() && !infoWaktu.equals("-")) {
                infoGabungan += " • " + infoWaktu;
            } else if (informasiVo.isEmpty()) {
                infoGabungan = infoWaktu;
            }

            // --- A. REKAPITULASI PERTEMUAN ---
            JSONObject oP = new JSONObject();
            oP.put("nama", p.getTopik() != null && !p.getTopik().isEmpty() ? p.getTopik() : Common.getBahasaConfig("Pertemuan Ke-") + p.getPertemuanKe());
            oP.put("info", infoGabungan);
            int doneP = p.ambilData("pertemuan", null).size();
            oP.put("done", doneP);
            oP.put("notDone", Math.max(0, totalPeserta - doneP));
            oP.put("total", totalPeserta);
            oP.put("myStatus", p.ambilData("pertemuan", myId).size() > 0 ? Common.getBahasaConfig("Sudah Akses") : Common.getBahasaConfig("Belum Akses"));
            jPertemuan.put(oP);

            // --- B. REKAPITULASI MATERI ---
            @SuppressWarnings("unchecked")
            List<PertemuanFileContent> materiList = sessionmy.createCriteria(PertemuanFileContent.class).add(Restrictions.eq("pertemuan", p.getId())).list();
            for (PertemuanFileContent m : materiList) {
                JSONObject oM = new JSONObject();
                oM.put("nama", m.getNama());
                oM.put("info", infoGabungan);
                
                String urlM = m.getLink() != null ? m.getLink() : m.createLinkUri();
                oM.put("url", urlM != null ? urlM : "");
                
                int doneM = p.ambilData("bahan_perkulaiahan_" + m.getId(), null).size();
                oM.put("done", doneM);
                oM.put("notDone", Math.max(0, totalPeserta - doneM));
                oM.put("total", totalPeserta);
                oM.put("myStatus", p.ambilData("bahan_perkulaiahan_" + m.getId(), myId).size() > 0 ? Common.getBahasaConfig("Sudah Akses") : Common.getBahasaConfig("Belum Akses"));
                jMateri.put(oM);
            }

            // --- C. REKAPITULASI TUGAS ---
            if (p.getJudultugas() != null && !p.getJudultugas().trim().isEmpty()) {
                JSONObject oT = new JSONObject();
                oT.put("nama", p.getJudultugas());
                oT.put("info", infoGabungan);
                Number numT = p.ambilJumlahTugasFileContent(refresh);
                int doneT = numT != null ? numT.intValue() : 0;
                oT.put("done", doneT);
                oT.put("notDone", Math.max(0, totalPeserta - doneT));
                oT.put("total", totalPeserta);
                oT.put("myStatus", p.ambilData("tugas", myId).size() > 0 ? Common.getBahasaConfig("Sudah Mengerjakan") : Common.getBahasaConfig("Belum Mengerjakan"));
                jTugas.put(oT);
            }
            
            @SuppressWarnings("unchecked")
            List<TugasPertemuan> tpList = sess.createCriteria(TugasPertemuan.class).add(Restrictions.eq("pertemuanData", p)).list();
            for (TugasPertemuan tp : tpList) {
                JSONObject oT = new JSONObject();
                oT.put("nama", tp.getJudultugas());
                oT.put("info", infoGabungan);
                Number numT = tp.ambilJumlahTugasFileContent(refresh);
                int doneT = numT != null ? numT.intValue() : 0;
                oT.put("done", doneT);
                oT.put("notDone", Math.max(0, totalPeserta - doneT));
                oT.put("total", totalPeserta);
                oT.put("myStatus", tp.ambilData("tugas", myId).size() > 0 ? Common.getBahasaConfig("Sudah Mengerjakan") : Common.getBahasaConfig("Belum Mengerjakan"));
                jTugas.put(oT);
            }

            @SuppressWarnings("unchecked")
            List<TugasKelompok> tkList = sess.createCriteria(TugasKelompok.class).add(Restrictions.eq("pertemuanData", p)).list();
            for (TugasKelompok tk : tkList) {
                JSONObject oT = new JSONObject();
                oT.put("nama", tk.getJudultugas());
                oT.put("info", infoGabungan);
                Number numT = tk.ambilJumlahTugasFileContent(refresh);
                int doneT = numT != null ? numT.intValue() : 0;
                oT.put("done", doneT);
                oT.put("notDone", Math.max(0, totalPeserta - doneT));
                oT.put("total", totalPeserta);
                oT.put("myStatus", tk.ambilData("tugas", myId).size() > 0 ? Common.getBahasaConfig("Sudah Mengerjakan") : Common.getBahasaConfig("Belum Mengerjakan"));
                jTugas.put(oT);
            }

            // --- D. REKAPITULASI UJIAN ---
            @SuppressWarnings("unchecked")
            List<PertemuanPunyaUjian> ujianList = sess.createCriteria(PertemuanPunyaUjian.class).add(Restrictions.eq("pertemuan", p)).list();
            for (PertemuanPunyaUjian u : ujianList) {
                JSONObject oU = new JSONObject();
                oU.put("nama", u.getNama());
                oU.put("info", infoGabungan);
                Number numU = u.ambilJumlahHasilUjianMahasiswaTelahIkut(refresh);
                int doneU = numU != null ? numU.intValue() : 0;
                oU.put("done", doneU);
                oU.put("notDone", Math.max(0, totalPeserta - doneU));
                oU.put("total", totalPeserta);
                oU.put("myStatus", u.ambilData("ujian", myId).size() > 0 ? Common.getBahasaConfig("Sudah Mengerjakan") : Common.getBahasaConfig("Belum Mengerjakan"));
                jUjian.put(oU);
            }

            // --- E. REKAPITULASI VIDEO ---
            @SuppressWarnings("unchecked")
            List<VideoPertemuan> videoList = sessionmy.createCriteria(VideoPertemuan.class).add(Restrictions.eq("pertemuan", p.getId())).list();
            for (VideoPertemuan v : videoList) {
                JSONObject oV = new JSONObject();
                oV.put("nama", v.getNama());
                oV.put("info", infoGabungan);
                
                String urlV = v.getLink() != null ? v.getLink() : v.createLinkUri(false);
                oV.put("url", urlV != null ? urlV : "");
                
                int doneV = p.ambilData("video_" + v.getId(), null).size();
                oV.put("done", doneV);
                oV.put("notDone", Math.max(0, totalPeserta - doneV));
                oV.put("total", totalPeserta);
                oV.put("myStatus", p.ambilData("video_" + v.getId(), myId).size() > 0 ? Common.getBahasaConfig("Sudah Akses") : Common.getBahasaConfig("Belum Akses"));
                jVideo.put(oV);
            }

            // --- F. REKAPITULASI AUDIO ---
            @SuppressWarnings("unchecked")
            List<AudioPertemuan> audioList = sessionmy.createCriteria(AudioPertemuan.class).add(Restrictions.eq("pertemuan", p.getId())).list();
            for (AudioPertemuan a : audioList) {
                JSONObject oA = new JSONObject();
                oA.put("nama", a.getNama());
                oA.put("info", infoGabungan);
                
                String urlA = a.getLink() != null ? a.getLink() : a.createLinkUri(false);
                oA.put("url", urlA != null ? urlA : "");
                
                int doneA = p.ambilData("audio_" + a.getId(), null).size();
                oA.put("done", doneA);
                oA.put("notDone", Math.max(0, totalPeserta - doneA));
                oA.put("total", totalPeserta);
                oA.put("myStatus", p.ambilData("audio_" + a.getId(), myId).size() > 0 ? Common.getBahasaConfig("Sudah Akses") : Common.getBahasaConfig("Belum Akses"));
                jAudio.put(oA);
            }
        }

        responseJson.put("status", "ok");
        JSONObject rekap = new JSONObject();
        rekap.put("pertemuan", jPertemuan); 
        rekap.put("materi", jMateri);
        rekap.put("tugas", jTugas); 
        rekap.put("ujian", jUjian);
        rekap.put("video", jVideo); 
        rekap.put("audio", jAudio);
        responseJson.put("rekap", rekap);

    } catch(Exception e) {
        if (sess != null && sess.getTransaction() != null && sess.getTransaction().isActive()) sess.getTransaction().rollback();
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_dasbor_service.jsp:299");
        responseJson.put("status", "error");
        responseJson.put("msg", Common.getBahasaConfig("Terjadi kesalahan internal peladen saat menyusun data statistik."));
    } finally {
        // ==============================================================================
        // PENUTUPAN SESI SECARA KETAT UNTUK MENCEGAH KEBOCORAN KONEKSI/MEMORI
        // ==============================================================================
        if (sessionmy != null) { 
            try { sessionmy.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_dasbor_service.jsp:307");}
            try { sessionmy.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_dasbor_service.jsp:308");}
            ais.common.ElearningSessionUtil.closeQuietly(sessionmy); 
        }
        if (sess != null) { 
            try { sess.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_dasbor_service.jsp:312");}
            try { sess.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_dasbor_service.jsp:313");}
            ais.common.ElearningSessionUtil.closeQuietly(sess); 
        }
        HibernateUtil.closeSessionQuietly(sess);
    }
    
    out.print(responseJson.toString());
%>
