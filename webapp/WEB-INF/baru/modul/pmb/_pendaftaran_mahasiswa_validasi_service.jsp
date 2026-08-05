<%@page import="java.io.BufferedReader"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.file.FileFotoLain"%>
<%@page import="ais.database.model.file.FotoBiodataCalonMahasiswa"%>
<%@page import="ais.common.Common"%>
<%@page import="org.joda.time.Years"%>
<%@page import="org.joda.time.DateTime"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
    // =========================================================================
    // HELPER METHODS: Pengganti optLong/optInt untuk kompatibilitas JSON lawas
    // =========================================================================
    private long parseLongSafe(JSONObject obj, String key) {
        try { return obj.has(key) && !obj.isNull(key) && !obj.get(key).toString().trim().isEmpty() ? Long.parseLong(obj.get(key).toString().trim()) : 0L; } 
        catch (Exception e) { return 0L; }
    }
    private int parseIntSafe(JSONObject obj, String key) {
        try { return obj.has(key) && !obj.isNull(key) && !obj.get(key).toString().trim().isEmpty() ? Integer.parseInt(obj.get(key).toString().trim()) : 0; } 
        catch (Exception e) { return 0; }
    }
    private boolean parseBoolSafe(JSONObject obj, String key) {
        try { 
            if (!obj.has(key) || obj.isNull(key)) return false;
            String v = obj.get(key).toString().trim().toLowerCase();
            return v.equals("true") || v.equals("1");
        } catch (Exception e) { return false; }
    }
    private String parseStringSafe(JSONObject obj, String key) {
        try { return obj.has(key) && !obj.isNull(key) ? obj.get(key).toString().trim() : ""; } 
        catch (Exception e) { return ""; }
    }
%>

<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    Session hibSession = null;
    BiodataCalonMahasiswa biodataCalonMahasiswaLogin = Common.isLogin(request);
    try {
        hibSession = HibernateUtil.openSession();

        // Mengambil payload JSON dari Request Body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) { sb.append(line); }
        if (sb.length() == 0) {
            out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Data pendaftaran belum dikirim oleh browser.")));
            return;
        }
        
        JSONObject payload = new JSONObject(sb.toString());
        JSONObject data = payload.has("data") && !payload.isNull("data") ? payload.getJSONObject("data") : payload;

        Long currentId = parseLongSafe(data, "id");

        // =========================================================================
        // 1. VALIDASI WAJIB DASAR (TAHUN, GELOMBANG, SELEKSI)
        // =========================================================================
        String tahunAkademik = parseStringSafe(data, "tahunAkademik");
        if (tahunAkademik.isEmpty()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Tahun Akademik harus diisi."))); return; }
        
        long gelId = parseLongSafe(data, "gelombangPendaftaran");
        if (gelId == 0) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Gelombang Pendaftaran harus dipilih."))); return; }
        
        long jsId = parseLongSafe(data, "jenisSeleksi");
        if (jsId == 0) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Jenis Seleksi harus dipilih."))); return; }

        GelombangPendaftaran gel = (GelombangPendaftaran) hibSession.get(GelombangPendaftaran.class, gelId);

        // =========================================================================
        // 2. VALIDASI NISN & NOMOR IDENTITAS (KTP)
        // =========================================================================
        String nisn = parseStringSafe(data, "nisn");
        if (!nisn.isEmpty() && nisn.length() != 10) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("NISN harus terdiri dari 10 digit angka."))); return; }

        long jkiId = parseLongSafe(data, "jenisKartuIdentitas");
        if (jkiId > 0) {
            JenisKartuIdentitasMahasiswaBaru jki = (JenisKartuIdentitasMahasiswaBaru) hibSession.get(JenisKartuIdentitasMahasiswaBaru.class, jkiId);
            if (jki != null && jki.getNama() != null && jki.getNama().toLowerCase().contains("ktp")) {
                String nik = parseStringSafe(data, "noIdentitas");
                if (!nik.isEmpty() && nik.length() != 16) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Nomor Identitas (NIK KTP) harus terdiri dari 16 digit angka."))); return; }
            }
        }

        // =========================================================================
        // 3. VALIDASI PEMBATASAN UMUR
        // =========================================================================
        String tglLahirStr = parseStringSafe(data, "tanggalLahir");
        if (!tglLahirStr.isEmpty() && gel != null) {
            Date tglLahir = new SimpleDateFormat("yyyy-MM-dd").parse(tglLahirStr);
            int umurCalon = Years.yearsBetween(new DateTime(tglLahir), new DateTime(WaktuUtil.getDate())).getYears();

            if (gel.getDibatasiUmur() != null && gel.getDibatasiUmur()) {
                int maxU = gel.getUmurmaksimal() != null ? gel.getUmurmaksimal() : 99;
                int minU = gel.getUmurminimal() != null ? gel.getUmurminimal() : 0;
                
                if (umurCalon > maxU) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Umur maksimal pendaftaran adalah") + " " + maxU + " " + Common.getBahasaConfig("tahun, sedangkan umur Anda") + " " + umurCalon + " " + Common.getBahasaConfig("tahun."))); return; }
                if (umurCalon < minU) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Umur minimal pendaftaran adalah") + " " + minU + " " + Common.getBahasaConfig("tahun, sedangkan umur Anda") + " " + umurCalon + " " + Common.getBahasaConfig("tahun."))); return; }
            } else if (Common.getKonfigurasi("umur_calon_mahasiswa_dibatasi", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                int maxGlobal = Integer.parseInt(Common.getKonfigurasi("nilai_umur_calon_mahasiswa_dibatasi", "27").getNilai().trim());
                if (umurCalon > maxGlobal) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Batas maksimal umur pendaftaran adalah") + " " + maxGlobal + " " + Common.getBahasaConfig("tahun, sedangkan umur Anda") + " " + umurCalon + " " + Common.getBahasaConfig("tahun."))); return; }
            }
        }

        // =========================================================================
        // 4. VALIDASI KETERSEDIAAN USERNAME & FORMAT EMAIL
        // =========================================================================
        String reqUsername = parseStringSafe(data, "username");
        if (!reqUsername.isEmpty()) {
            boolean isUsernameTerpakai = Common.checkUsername(reqUsername, "", currentId > 0 ? currentId : null);
            if (isUsernameTerpakai) {
                out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Username yang Anda masukkan telah terpakai, silakan pilih username yang lain.")));
                return;
            }
        }

        String email = parseStringSafe(data, "email");
        if (!email.isEmpty() && !Common.isValidEmailAddress(email)) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Format alamat email yang Anda masukkan tidak valid."))); return; }

        // =========================================================================
        // 5. VALIDASI PROGRAM STUDI & PAKET KULIAH
        // =========================================================================
        long pktId = parseLongSafe(data, "paket");
        if (pktId == 0) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Paket Pendaftaran harus dipilih."))); return; }

        Paket pkt = (Paket) hibSession.get(Paket.class, pktId);
        if (pkt != null) {
            long p1 = parseLongSafe(data, "prodi1");
            long p2 = parseLongSafe(data, "prodi2");
            long p3 = parseLongSafe(data, "prodi3");
            long p4 = parseLongSafe(data, "prodi4");
            long p5 = parseLongSafe(data, "prodi5");

            if (p1 == 0) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Program Studi Pilihan ke-1 harus diisi."))); return; }
            if (p2 == 0 && pkt.getJumlahProdiYgBolehDiambil() > 1) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Program Studi Pilihan ke-2 harus diisi."))); return; }
            if (p3 == 0 && pkt.getJumlahProdiYgBolehDiambil() > 2) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Program Studi Pilihan ke-3 harus diisi."))); return; }
            if (p4 == 0 && pkt.getJumlahProdiYgBolehDiambil() > 3) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Program Studi Pilihan ke-4 harus diisi."))); return; }
            if (p5 == 0 && pkt.getJumlahProdiYgBolehDiambil() > 4) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Program Studi Pilihan ke-5 harus diisi."))); return; }

            List<Jurusan> jurusans = new ArrayList<Jurusan>();
            List<Long> prodiListId = new ArrayList<Long>();
            long[] prodisArray = {p1, p2, p3, p4, p5};
            
            for (long pId : prodisArray) {
                if (pId > 0) {
                    Jurusan j = (Jurusan) hibSession.get(Jurusan.class, pId);
                    if (j != null) {
                        jurusans.add(j);
                        // Pengecekan Duplikasi Pilihan Prodi
                        if (!pkt.getBisaMemilihPilihanYangSama() && prodiListId.contains(pId)) {
                            out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Program Studi") + " " + j.getNama() + " " + Common.getBahasaConfig("tidak dapat dipilih lebih dari satu kali.")));
                            return;
                        }
                        prodiListId.add(pId);
                    }
                }
            }

            // Pengecekan Kombinasi Paket Prodi
            if (jurusans.size() > 1 && !PersyaratanPilihanPaket.checkKombinasiPaket(pkt, jurusans)) {
                out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Untuk pilihan paket") + " \"" + pkt.getNama() + "\", " + Common.getBahasaConfig("kombinasi pilihan program studi tidak diizinkan. Silakan periksa kembali.")));
                return;
            }
        }

        // =========================================================================
        // 6. VALIDASI PENDIDIKAN ASAL & MAHASISWA PINDAHAN
        // =========================================================================
        if (parseLongSafe(data, "jenisSekolah") == 0) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Jenis Pendidikan Asal harus dipilih."))); return; }
        if (parseLongSafe(data, "jurusanSekolah") == 0) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Jurusan Pendidikan Asal harus dipilih."))); return; }
        if (parseStringSafe(data, "program").isEmpty()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Program Kuliah harus dipilih."))); return; }

        if (parseBoolSafe(data, "merupakanPindahan")) {
            if (parseStringSafe(data, "pindahanDariKampus").isEmpty()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Nama kampus sebelum pindah wajib diisi."))); return; }
            if (parseStringSafe(data, "pindahanDariProdi").isEmpty()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Nama program studi sebelum pindah wajib diisi."))); return; }
            if (parseStringSafe(data, "nimLamaSebelumPindah").isEmpty()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("NIM/NPM kampus sebelum pindah wajib diisi."))); return; }
            if (parseIntSafe(data, "pindahDariKampusLamaDiSemester") <= 0) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Semester terakhir di kampus asal wajib diisi."))); return; }
            
            if (parseStringSafe(data, "keteranganPindah").isEmpty() && data.has("keteranganPindah")) { 
                out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Untuk calon mahasiswa pindahan, keterangan atau alasan pindah wajib diisi."))); 
                return; 
            }
        }

        // =========================================================================
        // 7. VALIDASI SUMBER INFORMASI PMB
        // =========================================================================
        boolean tampilkanInfoPMB = Common.getKonfigurasi("tampilkan_info_sekolah_dari_mana_pada_pmb", Konfigurasi.TIDAK_AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);
        if (tampilkanInfoPMB) {
            String info = parseStringSafe(data, "infoKampusDariMana");
            if (info.isEmpty()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Sumber informasi pendaftaran wajib dipilih."))); return; }
            
            if (info.toLowerCase().contains(";teman;") || info.toLowerCase().contains(";kawan;")) {
                if (parseStringSafe(data, "namaTemanInfoKampusDariMana").isEmpty()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Nama Teman/Kawan yang merekomendasikan wajib diisi."))); return; }
            }
            if (info.toLowerCase().contains("dosen") || info.toLowerCase().contains("karyawan")) {
                if (parseStringSafe(data, "dariNamaDosenKaryawan").isEmpty()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Nama Dosen/Karyawan yang merekomendasikan wajib diisi."))); return; }
            }
            if (info.toLowerCase().contains(";lain;") || info.toLowerCase().contains(";lainnya;") || info.toLowerCase().contains(";lain-lain;")) {
                if (parseStringSafe(data, "keteranganInfoKampusDariMana").isEmpty()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Keterangan sumber informasi lainnya wajib diisi."))); return; }
            }
        }

        // =========================================================================
        // 8. VALIDASI PERNYATAAN & KUOTA AFILIASI
        // =========================================================================
        if (!parseBoolSafe(data, "pernyataan")) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Pernyataan Persetujuan pendaftaran wajib dicentang."))); return; }

        long afilId = parseLongSafe(data, "afiliasiCalonMahasiswa");
        if (afilId > 0) {
            AfiliasiCalonMahasiswa afil = (AfiliasiCalonMahasiswa) hibSession.get(AfiliasiCalonMahasiswa.class, afilId);
            if (afil != null && afil.getKuotaDaftar() != null) {
                Number countAfil = (Number) hibSession.createCriteria(BiodataCalonMahasiswa.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(currentId > 0 ? Restrictions.ne("id", currentId) : Restrictions.sqlRestriction("true"))
                    .add(Restrictions.eq("afiliasiCalonMahasiswa", afil))
                    .setProjection(Projections.rowCount())
                    .uniqueResult();
                
                int jmlAfil = countAfil == null ? 0 : countAfil.intValue();
                if (jmlAfil >= afil.getKuotaDaftar()) { out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Kuota pendaftaran untuk afiliasi/referensi") + " \"" + afil.getNama() + "\" " + Common.getBahasaConfig("telah penuh."))); return; }
            }
        }

        // =========================================================================
        // 9. VALIDASI DUPLIKASI DATA (DOUBLE REGISTER)
        // =========================================================================
        JenisSeleksi js = (JenisSeleksi) hibSession.get(JenisSeleksi.class, jsId);
        String nama = parseStringSafe(data, "nama");
        String namaIbu = parseStringSafe(data, "namaIbu");

        if (!nama.isEmpty() && !tglLahirStr.isEmpty() && !namaIbu.isEmpty()) {
            Date tglLahir = new SimpleDateFormat("yyyy-MM-dd").parse(tglLahirStr);
            
            Number countDuplicate = (Number) hibSession.createCriteria(BiodataCalonMahasiswa.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(js != null && gel != null && !Boolean.TRUE.equals(gel.getTidakBolehMendaftarMhsYgSama()) ? Restrictions.eq("jenisSeleksi", js) : Restrictions.sqlRestriction("true"))
                .add(gel != null && !Boolean.TRUE.equals(gel.getTidakBolehMendaftarMhsYgSama()) ? Restrictions.eq("gelombangPendaftaran", gel) : Restrictions.sqlRestriction("true"))
                .add(Restrictions.eq("tahunAkademik", tahunAkademik))
                .add(Restrictions.ilike("nama", nama, MatchMode.EXACT))
                .add(Restrictions.ilike("namaIbu", namaIbu, MatchMode.EXACT))
                .add(currentId > 0 ? Restrictions.ne("id", currentId) : Restrictions.sqlRestriction("true"))
                .add(Restrictions.eq("tanggalLahir", tglLahir))
                .setProjection(Projections.rowCount())
                .uniqueResult();

            if (countDuplicate != null && countDuplicate.intValue() > 0) {
                out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Data pendaftaran dengan Nama") + " " + nama + ", " + Common.getBahasaConfig("Tanggal Lahir") + " " + tglLahirStr + ", " + Common.getBahasaConfig("dan Nama Ibu") + " " + namaIbu + " " + Common.getBahasaConfig("telah terdaftar sebelumnya.")));
                return;
            }
        }

        // =========================================================================
        // 10. VALIDASI FOTO WAJIB UPLOAD
        // =========================================================================
        if (gel != null && Boolean.TRUE.equals(gel.getFotoWajibDiuplad())) {
            long idUploadFoto = parseLongSafe(data, "idUploadFoto");
            FileFotoLain fileFotoLain = null;
            if (idUploadFoto != 0) {
                fileFotoLain = FileFotoLain.ambil(false, idUploadFoto, FotoBiodataCalonMahasiswa.DEFAULT_JENIS, FotoBiodataCalonMahasiswa.class, true);
            }
            if (fileFotoLain == null || fileFotoLain.getId() == null) {
                out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Pas Foto wajib diunggah sesuai dengan persyaratan gelombang pendaftaran ini.")));
                return;
            }
        }

        if(biodataCalonMahasiswaLogin == null || biodataCalonMahasiswaLogin.getId() == null){
	        // =========================================================================
	        // 11. VALIDASI KELENGKAPAN DOKUMEN (KHUSUS MODE EDIT SEBELUM SIMPAN)
	        // =========================================================================
	        if (currentId > 0 && gel != null && Boolean.TRUE.equals(gel.getDokumenHarusDiverivikasiSebelumBisaSimpan())) {
	            BiodataCalonMahasiswa camaEksisting = (BiodataCalonMahasiswa) hibSession.get(BiodataCalonMahasiswa.class, currentId);
	            if (camaEksisting != null) {
	                if (!ais.action.master.pmb.BiodataCalonMahasiswaAction.lengkap(camaEksisting)) {
	                    out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Dokumen persyaratan Anda belum lengkap atau belum diverifikasi. Anda tidak dapat memperbarui profil saat ini.")));
	                    return;
	                }
	            }
	        }
        }

        // JIKA SEMUA VALIDASI LOLOS
        out.print(new JSONObject().put("status", "success"));

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa_validasi_service.jsp:300");
        out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Terjadi kesalahan sistem saat memvalidasi data: ") + e.getMessage()));
    } finally {
        if (hibSession != null) {
            try { hibSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa_validasi_service.jsp:304");}
            try { hibSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa_validasi_service.jsp:305");}
            try { hibSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_pendaftaran_mahasiswa_validasi_service.jsp:306");}
        }
    }
%>