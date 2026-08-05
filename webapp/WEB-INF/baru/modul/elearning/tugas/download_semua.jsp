<%@ page trimDirectiveWhitespaces="true" language="java" contentType="application/zip" pageEncoding="UTF-8" import="java.util.*, java.io.*, java.util.zip.*, org.apache.commons.io.FilenameUtils, ais.common.Common, ais.common.ConstantValues, ais.database.model.GeneralValueObject, ais.database.model.Tbmuser, ais.database.model.Tugas, ais.database.model.TugasPertemuan, ais.database.model.TugasKelompok, ais.database.model.Pertemuan, ais.database.model.Mahasiswa, ais.database.model.BiodataCalonMahasiswa, ais.database.model.sekolah.Siswa, ais.database.model.file.TugasFileContent" %><%
    // ============================================================================
    // DOWNLOAD SEMUA berkas pengumpulan tugas (individu/pertemuan & kelompok) → 1 ZIP.
    // Nama tiap entri = {identitas}_{nama peserta}.{ekstensi}. Hanya untuk pengajar/admin.
    // Output BINER: seluruh berkas ditulis via response.getOutputStream() (tanpa JspWriter).
    // ============================================================================
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED); return; }

    String idTugasStr = request.getParameter("idTugas");
    String jenis = request.getParameter("jenis"); // Pertemuan | TugasPertemuan | TugasKelompok
    if (idTugasStr == null || idTugasStr.trim().isEmpty()) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter idTugas tidak ada"); return; }

    // 1. Muat objek tugas sesuai jenis
    Tugas tugas = null;
    try {
        if ("TugasKelompok".equalsIgnoreCase(jenis)) {
            tugas = (Tugas) GeneralValueObject.ambilData(TugasKelompok.class, idTugasStr.trim(), true);
        } else if ("TugasPertemuan".equalsIgnoreCase(jenis)) {
            tugas = (Tugas) GeneralValueObject.ambilData(TugasPertemuan.class, idTugasStr.trim(), true);
        } else {
            tugas = (Tugas) GeneralValueObject.ambilData(Pertemuan.class, idTugasStr.trim(), true);
        }
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas/download_semua.jsp:24"); }
    if (tugas == null || tugas.getId() == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tugas tidak ditemukan"); return; }

    // 2. Guard peran: hanya pengajar/admin (peserta didik ditolak)
    Pertemuan pertemuanTgs = null;
    if (tugas instanceof Pertemuan) pertemuanTgs = (Pertemuan) tugas;
    else if (tugas instanceof TugasPertemuan) pertemuanTgs = ((TugasPertemuan) tugas).ambilPertemuan();
    else if (tugas instanceof TugasKelompok) pertemuanTgs = ((TugasKelompok) tugas).ambilPertemuan();
    boolean bolehUnduh = (pertemuanTgs != null) && pertemuanTgs.bolehUbahAbsenSaja(tbmuser);
    if (!bolehUnduh) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tidak memiliki hak akses"); return; }

    // 3. Ambil seluruh berkas pengumpulan (pola sama tugas_peserta.jsp)
    TreeMap<Long, TugasFileContent> map = null;
    try {
        map = tugas.ambilTugasFileContentTotal(new TreeMap<Long, TugasFileContent>(), "", null, 1000);
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas/download_semua.jsp:39"); }

    // 4. Siapkan header ZIP (validasi sudah selesai; setelah ini output biner).
    // response.reset() = cara kanonik mengalihkan JSP ke output biner: bersihkan buffer
    // JspWriter + header sebelumnya sehingga response.getOutputStream() aman dipanggil.
    String judul = tugas.getJudultugas() != null ? tugas.getJudultugas() : "Tugas";
    String namaZip = ("Tugas_" + judul).replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replaceAll("\\s+", "_");
    if (namaZip.isEmpty()) namaZip = "Tugas";
    try { response.reset(); } catch (Exception exReset) { ais.common.ErrorAuditUtil.record(exReset, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/tugas/download_semua.jsp:47");}
    response.setContentType("application/zip");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + namaZip + ".zip\"");

    OutputStream os = response.getOutputStream();
    ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
    Set<String> usedNames = new HashSet<String>();
    int jumlah = 0;
    try {
        if (map != null && !map.isEmpty()) {
            for (TugasFileContent tfc : map.values()) {
                if (tfc == null) continue;
                try {
                    // Label peserta
                    String ident = "", nama = "";
                    if (tfc.getMahasiswa() != null && tfc.getMahasiswa() > 0L) {
                        Mahasiswa m = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), tfc.getMahasiswa());
                        if (m != null) { ident = m.getNim() != null ? m.getNim() : ""; nama = m.getNama() != null ? m.getNama() : ""; }
                    } else if (tfc.getSiswa() != null && tfc.getSiswa() > 0L) {
                        Siswa s = (Siswa) ConstantValues.ambil(Siswa.class.getName(), tfc.getSiswa());
                        if (s != null) { ident = s.getNomorInduk() != null ? s.getNomorInduk() : ""; nama = s.getNama() != null ? s.getNama() : ""; }
                    } else if (tfc.getBiodataCalonMahasiswa() != null && tfc.getBiodataCalonMahasiswa() > 0L) {
                        BiodataCalonMahasiswa c = (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), tfc.getBiodataCalonMahasiswa());
                        if (c != null) { ident = c.getNoRegistrasi() != null ? c.getNoRegistrasi() : ""; nama = c.getNama() != null ? c.getNama() : ""; }
                    }
                    String label = (ident + "_" + nama).trim();
                    if (label.equals("_") || label.isEmpty()) label = "peserta_" + tfc.getId();

                    // Materialisasi berkas (dari blob DB streaming / gdrive → File on disk)
                    File f = tfc.ambilFile();
                    if (f == null || !f.exists() || f.length() <= 0L) continue;

                    String ext = FilenameUtils.getExtension(tfc.getNama() != null ? tfc.getNama() : f.getName());
                    String baseName = label.replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replaceAll("\\s+", "_");
                    if (baseName.isEmpty()) baseName = "peserta_" + tfc.getId();
                    String entryName = baseName + (ext != null && !ext.isEmpty() ? "." + ext : "");
                    // Cegah nama duplikat
                    String finalName = entryName; int dup = 1;
                    while (usedNames.contains(finalName)) {
                        finalName = baseName + "_" + (dup++) + (ext != null && !ext.isEmpty() ? "." + ext : "");
                    }
                    usedNames.add(finalName);

                    zos.putNextEntry(new ZipEntry(finalName));
                    FileInputStream fis = new FileInputStream(f);
                    try {
                        byte[] buf = new byte[8192]; int n;
                        while ((n = fis.read(buf)) != -1) { zos.write(buf, 0, n); }
                    } finally { try { fis.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/tugas/download_semua.jsp:95");} }
                    zos.closeEntry();
                    jumlah++;
                } catch (Exception exItem) {
                    exItem.printStackTrace(); ais.common.ErrorAuditUtil.record(exItem, "auto-audit webapp/WEB-INF/baru/modul/elearning/tugas/download_semua.jsp:99"); // lewati berkas bermasalah, lanjutkan sisanya
                }
            }
        }
        if (jumlah == 0) {
            // ZIP tidak kosong-membingungkan: sisipkan catatan
            zos.putNextEntry(new ZipEntry("INFO.txt"));
            String info = "Belum ada berkas pengumpulan tugas untuk sesi ini.";
            zos.write(info.getBytes("UTF-8"));
            zos.closeEntry();
        }
        zos.finish();
        zos.flush();
    } finally {
        try { zos.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/tugas/download_semua.jsp:113");}
    }
%>
