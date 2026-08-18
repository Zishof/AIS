<%@ page trimDirectiveWhitespaces="true" language="java" contentType="application/zip" pageEncoding="UTF-8" import="java.util.*, java.io.*, java.util.zip.*, org.hibernate.Session, org.hibernate.criterion.Restrictions, ais.database.hibernate.HibernateUtil, ais.common.Common, ais.common.ConstantValues, ais.database.model.GeneralValueObject, ais.database.model.Tbmuser, ais.database.model.PertemuanPunyaUjian, ais.database.model.HasilUjianMahasiswa, ais.database.model.HasilUjianMahasiswaDetail, ais.database.model.UjianPunyaSoal, ais.database.model.BankSoal, ais.database.model.BankSoalDetail, ais.database.model.Mahasiswa, ais.database.model.BiodataCalonMahasiswa, ais.database.model.sekolah.Siswa, ais.ui.util.MyArrayList" %><%!
    // Escape minimal untuk teks jawaban peserta (soal dibiarkan apa adanya krn bisa mengandung HTML/gambar)
    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
%><%
    // ============================================================================
    // DOWNLOAD SEMUA lembar jawaban ujian → 1 ZIP (1 berkas HTML per peserta).
    // Jawaban ujian berupa TEKS (bukan berkas), jadi arsip berisi lembar jawaban.
    // Hanya untuk pengajar/admin. Output BINER via response.getOutputStream().
    // ============================================================================
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED); return; }

    String ppuParam = request.getParameter("ppu");
    if (ppuParam == null || ppuParam.trim().isEmpty()) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter ppu tidak ada"); return; }

    PertemuanPunyaUjian ppu = null;
    try { ppu = (PertemuanPunyaUjian) GeneralValueObject.ambilData(PertemuanPunyaUjian.class, ppuParam.trim(), true); } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/download_semua.jsp:20"); }
    if (ppu == null || ppu.getUjian() == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND, "Data ujian tidak ditemukan"); return; }

    // Guard peran: hanya pengajar/admin
    boolean bolehUnduh = (ppu.getPertemuan() != null) && ppu.getPertemuan().bolehUbahAbsenSaja(tbmuser);
    if (!bolehUnduh) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tidak memiliki hak akses"); return; }

    int jmlDitampilkan = ppu.getJmlDitampilkan() != null ? ppu.getJmlDitampilkan().intValue() : 0;
    String judulUjian = ppu.getUjian().getKeterangan() != null ? ppu.getUjian().getKeterangan() : "Ujian";

    // Kumpulkan lembar jawaban di memori dulu (biar validasi/DB rampung sebelum output biner)
    List<String[]> lembar = new ArrayList<String[]>(); // [namaEntri, htmlIsi]
    Session mySession = null;
    try {
        mySession = HibernateUtil.openSession();
        List<HasilUjianMahasiswa> daftar = mySession.createCriteria(HasilUjianMahasiswa.class)
                .add(Restrictions.eq("pertemuanPunyaUjian", ppu))
                .add(Restrictions.isNotNull("keyhasil"))
                .list();
        if (daftar != null) {
            for (HasilUjianMahasiswa hum : daftar) {
                try {
                    // Label peserta
                    String ident = "", nama = "";
                    if (hum.getMahasiswa() != null) { ident = hum.getMahasiswa().getNim() != null ? hum.getMahasiswa().getNim() : ""; nama = hum.getMahasiswa().getNama() != null ? hum.getMahasiswa().getNama() : ""; }
                    else if (hum.getSiswa() != null) { ident = hum.getSiswa().getNomorInduk() != null ? hum.getSiswa().getNomorInduk() : ""; nama = hum.getSiswa().getNama() != null ? hum.getSiswa().getNama() : ""; }
                    else if (hum.getBiodataCalonMahasiswa() != null) { ident = hum.getBiodataCalonMahasiswa().getNoRegistrasi() != null ? hum.getBiodataCalonMahasiswa().getNoRegistrasi() : ""; nama = hum.getBiodataCalonMahasiswa().getNama() != null ? hum.getBiodataCalonMahasiswa().getNama() : ""; }
                    String label = (ident + "_" + nama).trim();
                    if (label.equals("_") || label.isEmpty()) label = "peserta_" + hum.getId();

                    // Ambil soal & jawaban peserta (refresh=false: hanya baca, tidak mengubah data)
                    MyArrayList<Long> ujianPunyaSoals = hum.ambilUjianPunyaSoals(jmlDitampilkan, null, false);
                    Map<Long, Set<Long>> detailMap = hum.ambilHasilUjianMahasiswaDetail(jmlDitampilkan, ujianPunyaSoals, false);

                    StringBuilder html = new StringBuilder();
                    html.append("<!doctype html><html><head><meta charset='UTF-8'><title>").append(escHtml(label)).append("</title>");
                    html.append("<style>body{font-family:Arial,sans-serif;font-size:13px;color:#222;margin:24px;}h2{margin:0 0 4px;}h3{color:#555;font-weight:normal;margin:0 0 16px;}.soal{border:1px solid #ddd;border-radius:6px;padding:12px 14px;margin-bottom:12px;}.q{font-weight:bold;margin-bottom:6px;}.a{background:#f6f8fa;border-left:3px solid #0d6efd;padding:8px 10px;border-radius:4px;white-space:pre-wrap;}.meta{color:#666;font-size:12px;margin-top:6px;}</style></head><body>");
                    html.append("<h2>").append(escHtml(nama.isEmpty() ? label : nama)).append("</h2>");
                    html.append("<h3>").append(escHtml(ident)).append(" &middot; ").append(escHtml(judulUjian));
                    if (hum.getNilai() != null) html.append(" &middot; Nilai: <b>").append(Common.numberFormat.get().format(hum.getNilai())).append("</b>");
                    html.append("</h3>");

                    int no = 1;
                    if (ujianPunyaSoals != null) {
                        for (Long upsId : ujianPunyaSoals) {
                            try {
                                UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, upsId.toString());
                                if (ups == null || ups.getBankSoal() == null) continue;
                                BankSoal bs = ups.getBankSoal();
                                String soal = bs.getSoal() != null ? bs.getSoal() : "-";

                                // Jawaban peserta untuk soal ini
                                StringBuilder jwb = new StringBuilder();
                                Double nilaiSoal = null;
                                Set<Long> detIds = (detailMap != null) ? detailMap.get(bs.getId()) : null;
                                if (detIds != null) {
                                    for (Long did : detIds) {
                                        try {
                                            HasilUjianMahasiswaDetail d = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, did.toString());
                                            if (d == null) continue;
                                            String isi = "";
                                            if (d.getBankSoalDetail() != null && d.getBankSoalDetail().getJawaban() != null) isi = d.getBankSoalDetail().getJawaban();
                                            else if (d.getJawaban() != null) isi = d.getJawaban();
                                            if (isi != null && !isi.trim().isEmpty()) { if (jwb.length() > 0) jwb.append("\n"); jwb.append(isi.trim()); }
                                            if (d.getNilai() != null) nilaiSoal = (nilaiSoal == null ? 0.0 : nilaiSoal) + d.getNilai();
                                        } catch (Exception exd) { ais.common.ErrorAuditUtil.record(exd, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/download_semua.jsp:85");}
                                    }
                                }
                                html.append("<div class='soal'><div class='q'>").append(no).append(". ").append(soal).append("</div>");
                                html.append("<div class='a'>").append(jwb.length() > 0 ? escHtml(jwb.toString()) : "<i>(Tidak dijawab)</i>").append("</div>");
                                if (nilaiSoal != null) html.append("<div class='meta'>Nilai soal: ").append(Common.numberFormat.get().format(nilaiSoal)).append("</div>");
                                html.append("</div>");
                                no++;
                            } catch (Exception exq) { ais.common.ErrorAuditUtil.record(exq, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/download_semua.jsp:93");}
                        }
                    }
                    html.append("</body></html>");

                    String baseName = label.replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replaceAll("\\s+", "_");
                    if (baseName.isEmpty()) baseName = "peserta_" + hum.getId();
                    lembar.add(new String[]{ baseName + ".html", html.toString() });
                } catch (Exception exHum) { exHum.printStackTrace(); ais.common.ErrorAuditUtil.record(exHum, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/download_semua.jsp:101"); }
            }
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/download_semua.jsp:105");
    } finally {
        ais.common.ElearningSessionUtil.closeQuietly(mySession);
        try { HibernateUtil.closeSessionQuietly(mySession); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/download_semua.jsp:108");}
    }

    // Output biner ZIP (validasi & DB sudah selesai)
    String namaZip = ("Ujian_" + judulUjian).replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replaceAll("\\s+", "_");
    if (namaZip.isEmpty()) namaZip = "Ujian";
    try { response.reset(); } catch (Exception exReset) { ais.common.ErrorAuditUtil.record(exReset, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/download_semua.jsp:114");}
    response.setContentType("application/zip");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + namaZip + ".zip\"");

    OutputStream os = response.getOutputStream();
    ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
    Set<String> usedNames = new HashSet<String>();
    try {
        if (!lembar.isEmpty()) {
            for (String[] item : lembar) {
                String finalName = item[0]; int dup = 1;
                while (usedNames.contains(finalName)) {
                    finalName = item[0].replaceAll("\\.html$", "") + "_" + (dup++) + ".html";
                }
                usedNames.add(finalName);
                zos.putNextEntry(new ZipEntry(finalName));
                zos.write(item[1].getBytes("UTF-8"));
                zos.closeEntry();
            }
        } else {
            zos.putNextEntry(new ZipEntry("INFO.txt"));
            zos.write("Belum ada peserta yang menyelesaikan ujian ini.".getBytes("UTF-8"));
            zos.closeEntry();
        }
        zos.finish();
        zos.flush();
    } finally {
        try { zos.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/download_semua.jsp:141");}
    }
%>
