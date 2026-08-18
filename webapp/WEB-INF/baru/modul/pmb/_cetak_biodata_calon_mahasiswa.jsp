<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.GelombangPendaftaran"%>
<%@page import="ais.database.model.JenisSeleksi"%>
<%@page import="ais.database.model.VerifikasiKelengkapanCalonMahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas"%>
<%@page import="ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran"%>
<%@page import="ais.database.model.MatapelajaranSekolah"%>
<%@page import="ais.database.model.PaketPunyaMatapelajaran"%>
<%@page import="ais.database.model.CommonVO"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.file.LampiranLainBiodataCalonMahasiswa"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
    // =========================================================================
    // METHOD PEMBANTU (HELPER) UNTUK EFISIENSI STRUKTUR DATA MAP
    // =========================================================================
    private void addData(List<Map<String, String>> maps, String group, String label, String value) {
        if (value != null && !value.trim().isEmpty() && !value.trim().equalsIgnoreCase("null")) {
            Map<String, String> map = new LinkedHashMap<String, String>();
            map.put("grup", group);
            map.put("label", label);
            map.put("nilai", value);
            maps.add(map);
        }
    }

    private void addDataUrl(List<Map<String, String>> maps, String group, String label, String value, String url) {
        if ((value != null && !value.trim().isEmpty() && !value.trim().equalsIgnoreCase("null")) || (url != null && !url.trim().isEmpty())) {
            Map<String, String> map = new LinkedHashMap<String, String>();
            map.put("grup", group);
            map.put("label", label);
            map.put("nilai", value == null ? "" : value);
            if (url != null && !url.trim().isEmpty()) {
                map.put("url", url);
            }
            maps.add(map);
        }
    }
%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    String idParam = request.getParameter("id");
    String renderHtml = request.getParameter("render_html");

    // =========================================================================
    // FASE 0: MODE JSON UNTUK AJAX CALL (SUKSES_LOGIN.JSP)
    // =========================================================================
    if (renderHtml == null || !renderHtml.equals("true")) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        if (idParam == null || idParam.trim().isEmpty()) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("ID Calon Mahasiswa tidak valid.") + "\"}");
        } else {
            String urlCetak = Common.ROOT + "/pmb?hanya_tampil_jsp=true&p=pmb&s=_cetak_biodata_calon_mahasiswa&id=" + idParam.trim() + "&render_html=true";
            out.print("{\"status\":\"success\", \"url\":\"" + urlCetak + "\"}");
        }
        return;
    }

    // =========================================================================
    // FASE 1 & 2: MODE HTML - EKSTRAKSI DATA BERDASARKAN LOGIKA JAVA ASLI
    // =========================================================================
    BiodataCalonMahasiswa calonMahasiswa = null;
    if (idParam != null && !idParam.trim().isEmpty()) {
        calonMahasiswa = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, idParam.trim(), true);
    }

    if (calonMahasiswa == null) {
        out.print("<div style='padding: 50px; text-align: center; font-family: sans-serif; color: red;'><h3>" + Common.getBahasaConfig("Data Calon Mahasiswa Tidak Ditemukan") + "</h3></div>");
        return;
    }

    List<Map<String, String>> maps = new ArrayList<Map<String, String>>();
    
    // Grup "" (Data Registrasi)
    String grupUtama = "Data Registrasi Utama";
    addData(maps, grupUtama, "Nomor Pendaftaran", calonMahasiswa.getNoRegistrasi());
    if (calonMahasiswa.getNoUjian() != null) addData(maps, grupUtama, "Nomor Ujian", calonMahasiswa.getNoUjian());
    addData(maps, grupUtama, "Tanggal Pendaftaran", calonMahasiswa.getTanggalPendaftaran() == null ? "" : Common.dateFormat6.get().format(calonMahasiswa.getTanggalPendaftaran()));
    addData(maps, grupUtama, "Tahun Akademik", calonMahasiswa.getTahunAkademik());
    addData(maps, grupUtama, "Gelombang Pendaftaran", calonMahasiswa.getGelombangPendaftaran() == null ? "" : calonMahasiswa.getGelombangPendaftaran().getNama());
    addData(maps, grupUtama, "Jenis Seleksi", (calonMahasiswa.getGelombangPendaftaran() == null || calonMahasiswa.getGelombangPendaftaran().getJenisSeleksi() == null) ? "" : calonMahasiswa.getGelombangPendaftaran().getJenisSeleksi().getNama());

    // Grup "I. Data Calon Mahasiswa"
    String grup1 = "I. Data Identitas Diri";
    addData(maps, grup1, "Nama Lengkap", calonMahasiswa.getNama());
    addData(maps, grup1, "Jenis Kartu Identitas", calonMahasiswa.getJenisKartuIdentitas() == null ? "" : calonMahasiswa.getJenisKartuIdentitas().getNama());
    addData(maps, grup1, "No Kartu Identitas", calonMahasiswa.getNoIdentitas());
    addData(maps, grup1, "Nomor Induk Siswa Nasional (NISN)", calonMahasiswa.getNisn());
    addData(maps, grup1, "Tempat Lahir", calonMahasiswa.getTempatLahir());
    addData(maps, grup1, "Tanggal Lahir", calonMahasiswa.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(calonMahasiswa.getTanggalLahir()));
    addData(maps, grup1, "Email", calonMahasiswa.getEmail());
    addData(maps, grup1, "Jenis Kelamin", calonMahasiswa.getJenisKelamin());
    
    Integer statusNikah = calonMahasiswa.getStatusNikah();
    String strNikah = (statusNikah == null) ? "" : (statusNikah.equals(0) ? "Belum Nikah" : statusNikah.equals(1) ? "Nikah" : statusNikah.equals(2) ? "Janda" : "Duda");
    addData(maps, grup1, "Status Perkawinan", strNikah);
    addData(maps, grup1, "Agama", calonMahasiswa.getAgama() == null ? "" : calonMahasiswa.getAgama().getNama());
    addData(maps, grup1, "Kewarganegaraan", calonMahasiswa.getKewarganegaraan());
    addData(maps, grup1, "Asal Negara", calonMahasiswa.getAsalNegara() == null ? "" : calonMahasiswa.getAsalNegara().getNamaNegara());
    addData(maps, grup1, "Alamat Rumah", calonMahasiswa.getAlamat());
    addData(maps, grup1, "Dusun / Kampung", calonMahasiswa.getDusunCalon());
    addData(maps, grup1, "RT / RW", (calonMahasiswa.getRt() != null ? calonMahasiswa.getRt() : "-") + " / " + (calonMahasiswa.getRw() != null ? calonMahasiswa.getRw() : "-"));
    addData(maps, grup1, "Kelurahan / Desa", calonMahasiswa.getKelurahanCalon());
    addData(maps, grup1, "Kecamatan", calonMahasiswa.getKecamatanCalon() == null ? "" : calonMahasiswa.getKecamatanCalon().getNama());
    addData(maps, grup1, "Kota / Kabupaten", calonMahasiswa.getKotaCalon() == null ? "" : calonMahasiswa.getKotaCalon().getNama());
    addData(maps, grup1, "Provinsi", calonMahasiswa.getPropinsiCalon() == null ? "" : calonMahasiswa.getPropinsiCalon().getNama());
    addData(maps, grup1, "Kode Pos", calonMahasiswa.getKodePos());
    addData(maps, grup1, "Nomor Handphone / WA", calonMahasiswa.getHp());
    addData(maps, grup1, "Telepon Rumah", calonMahasiswa.getTeleponRumah());

    // Grup "II. Data Pendidikan Asal"
    String grup2 = "II. Data Pendidikan Asal";
    addData(maps, grup2, "Jenis Pendidikan Sebelumnya", calonMahasiswa.getJenisSekolah() == null ? "" : calonMahasiswa.getJenisSekolah().getNama());
    String jurLain = (calonMahasiswa.getJurusanSekolahLain() == null || calonMahasiswa.getJurusanSekolahLain().trim().isEmpty()) ? "" : " " + calonMahasiswa.getJurusanSekolahLain();
    addData(maps, grup2, "Nama Jurusan Pendidikan Asal", (calonMahasiswa.getJurusanSekolah() == null ? "" : calonMahasiswa.getJurusanSekolah().getNama()) + jurLain);
    addData(maps, grup2, "Akreditasi Pendidikan Sebelumnya", calonMahasiswa.getAkreditasiSekolah());
    addData(maps, grup2, "Nama Institusi / Sekolah Asal", calonMahasiswa.getAsalSma());
    addData(maps, grup2, "Alamat Pendidikan Sebelumnya", calonMahasiswa.getAlamatAsalSma());
    addData(maps, grup2, "Kecamatan Pendidikan", calonMahasiswa.getKecamatanSekolah() == null ? "" : calonMahasiswa.getKecamatanSekolah().getNama());
    addData(maps, grup2, "Kota / Kabupaten Pendidikan", calonMahasiswa.getKotaSekolah() == null ? "" : calonMahasiswa.getKotaSekolah().getNama());
    addData(maps, grup2, "Provinsi Pendidikan", calonMahasiswa.getPropinsiSekolah() == null ? "" : calonMahasiswa.getPropinsiSekolah().getNama());
    addData(maps, grup2, "Tahun Kelulusan", calonMahasiswa.getTahunKelulusan() != null ? String.valueOf(calonMahasiswa.getTahunKelulusan()) : "");

    // Grup "III. Data Orang Tua/Wali"
    String grup3 = "III. Data Keluarga (Orang Tua / Wali)";
    addData(maps, grup3, "Nama Ayah", calonMahasiswa.getNamaAyah());
    addData(maps, grup3, "Pendidikan Ayah", calonMahasiswa.getPendidikanOrtu() == null ? "" : calonMahasiswa.getPendidikanOrtu().getNama());
    addData(maps, grup3, "Pekerjaan Ayah", calonMahasiswa.getPekerjaanAyah() == null ? "" : calonMahasiswa.getPekerjaanAyah().getNama());
    addData(maps, grup3, "Pendapatan Ayah", calonMahasiswa.getPendapatanOrtu() == null ? "" : "Rp " + Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtu().getMulaiDari()) + " - Rp " + Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtu().getSampai()));
    
    addData(maps, grup3, "Nama Ibu", calonMahasiswa.getNamaIbu());
    addData(maps, grup3, "Pendidikan Ibu", calonMahasiswa.getPendidikanOrtuIbu() == null ? "" : calonMahasiswa.getPendidikanOrtuIbu().getNama());
    addData(maps, grup3, "Pekerjaan Ibu", calonMahasiswa.getPekerjaanAyahIbu() == null ? "" : calonMahasiswa.getPekerjaanAyahIbu().getNama());
    addData(maps, grup3, "Pendapatan Ibu", calonMahasiswa.getPendapatanOrtuIbu() == null ? "" : "Rp " + Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtuIbu().getMulaiDari()) + " - Rp " + Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtuIbu().getSampai()));
    
    addData(maps, grup3, "Nama Wali", calonMahasiswa.getNamaWali());
    addData(maps, grup3, "Pendidikan Wali", calonMahasiswa.getPendidikanOrtuWali() == null ? "" : calonMahasiswa.getPendidikanOrtuWali().getNama());
    addData(maps, grup3, "Pekerjaan Wali", calonMahasiswa.getPekerjaanAyahWali() == null ? "" : calonMahasiswa.getPekerjaanAyahWali().getNama());
    addData(maps, grup3, "Pendapatan Wali", calonMahasiswa.getPendapatanOrtuWali() == null ? "" : "Rp " + Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtuWali().getMulaiDari()) + " - Rp " + Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtuWali().getSampai()));
    
    addData(maps, grup3, "Alamat Lengkap Orang Tua / Wali", calonMahasiswa.getAlamatOrtu());
    addData(maps, grup3, "RT / RW", (calonMahasiswa.getRtOrtu() != null ? calonMahasiswa.getRtOrtu() : "-") + " / " + (calonMahasiswa.getRwOrtu() != null ? calonMahasiswa.getRwOrtu() : "-"));
    addData(maps, grup3, "Kelurahan Orang Tua", calonMahasiswa.getKelurahanOrtu());
    addData(maps, grup3, "Kecamatan Orang Tua", calonMahasiswa.getKecamatanOrtu() == null ? "" : calonMahasiswa.getKecamatanOrtu().getNama());
    addData(maps, grup3, "Kota / Kabupaten Orang Tua", calonMahasiswa.getKotaOrtu() == null ? "" : calonMahasiswa.getKotaOrtu().getNama());
    addData(maps, grup3, "Provinsi Orang Tua", calonMahasiswa.getPropinsiOrtu() == null ? "" : calonMahasiswa.getPropinsiOrtu().getNama());
    addData(maps, grup3, "Nomor Telp / Handphone", calonMahasiswa.getNoTelpOrtu());

    // Grup "IV & V. Pilihan Akademik"
    String grup5 = "IV. Pilihan Program Studi & Paket";
    addData(maps, grup5, "Pilihan Paket", calonMahasiswa.getPaket() == null ? "" : calonMahasiswa.getPaket().getNama());
    if (calonMahasiswa.getProdi1() != null) addData(maps, grup5, "Pilihan Prodi Ke-1", calonMahasiswa.getProdi1().getNama());
    if (calonMahasiswa.getProdi2() != null) addData(maps, grup5, "Pilihan Prodi Ke-2", calonMahasiswa.getProdi2().getNama());
    if (calonMahasiswa.getProdi3() != null) addData(maps, grup5, "Pilihan Prodi Ke-3", calonMahasiswa.getProdi3().getNama());
    if (calonMahasiswa.getProdi4() != null) addData(maps, grup5, "Pilihan Prodi Ke-4", calonMahasiswa.getProdi4().getNama());
    if (calonMahasiswa.getProdi5() != null) addData(maps, grup5, "Pilihan Prodi Ke-5", calonMahasiswa.getProdi5().getNama());
    if (calonMahasiswa.getProdiLulus() != null) addData(maps, grup5, "Status Diterima di Prodi", calonMahasiswa.getProdiLulus().getNama());
    
    if (calonMahasiswa.getMahasiswa() == null) {
        addData(maps, grup5, "Program Perkuliahan", calonMahasiswa.getProgram());
    } else {
        addData(maps, grup5, "Program Perkuliahan", calonMahasiswa.getMahasiswa().getProgramBaru() == null ? calonMahasiswa.getMahasiswa().getProgram() : calonMahasiswa.getMahasiswa().getProgramBaru().getNamaBaru());
    }

    // Grup "VI. Pindahan"
    String grup6 = "V. Status Mahasiswa Pindahan";
    addData(maps, grup6, "Merupakan Mahasiswa Pindahan", (calonMahasiswa.getMerupakanPindahan() != null && calonMahasiswa.getMerupakanPindahan()) ? "Ya" : "Tidak");
    if (calonMahasiswa.getMerupakanPindahan() != null && calonMahasiswa.getMerupakanPindahan()) {
        addData(maps, grup6, "Nama Kampus Asal", calonMahasiswa.getPindahanDariKampus());
        addData(maps, grup6, "Program Studi Asal", calonMahasiswa.getPindahanDariProdi());
        addData(maps, grup6, "NIM Asal", calonMahasiswa.getNimLamaSebelumPindah());
        addData(maps, grup6, "Semester Terakhir di Kampus Asal", calonMahasiswa.getPindahDariKampusLamaDiSemester() != null ? String.valueOf(calonMahasiswa.getPindahDariKampusLamaDiSemester()) : "");
        addData(maps, grup6, "Alasan Pindah", calonMahasiswa.getKeteranganPindah());
    }

    // Grup "Info Kampus"
    String grupInfo = "VI. Informasi Tambahan";
    StringBuilder infoBuilder = new StringBuilder();
    if (calonMahasiswa.getInfoKampusDariMana() != null) {
        for (String s : calonMahasiswa.getInfoKampusDariMana().split(";")) {
            if(infoBuilder.length() > 0) infoBuilder.append(", ");
            infoBuilder.append(s);
        }
    }
    addData(maps, grupInfo, "Sumber Informasi Pendaftaran", infoBuilder.toString());
    addData(maps, grupInfo, "Nama Referensi (Teman/Mahasiswa)", calonMahasiswa.getNamaTemanInfoKampusDariMana());
    addData(maps, grupInfo, "Keterangan Informasi", calonMahasiswa.getKeteranganInfoKampusDariMana());

    // Parameter Tambahan Dinamis
    for (CommonVO commonVO : calonMahasiswa.ambilDataParameterTambahan()) {
        String lbl = commonVO.getName();
        String url = commonVO.getName2();
        String val = commonVO.getName1();
        try {
            if (val != null) {
                String[] d = StringUtils.split(val, ":");
                if (d.length > 1 && Common.isNumber(d[1].trim())) {
                    val = d[0];
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_biodata_calon_mahasiswa.jsp:220");}

        if ((val != null && !val.trim().isEmpty() && !val.trim().equalsIgnoreCase("null")) || (url != null && !url.trim().isEmpty())) {
            String[] param = (lbl != null) ? lbl.split("->") : new String[]{""};
            String groupParam = param[0];
            String labelParam = param.length > 1 ? param[1] : "";
            addDataUrl(maps, "VII. " + groupParam, labelParam, val, url);
        }
    }

    // =========================================================================
    // FASE 3: KONEKSI DATABASE UNTUK BERKAS DAN VERIFIKASI
    // =========================================================================
    Session sessionLocal = null;
    Transaction tx = null;
    String urlFotoProfil = Common.ROOT + "/img/default-avatar.png";
    
    try {
        sessionLocal = HibernateUtil.getSessionFactory().openSession();
        tx = sessionLocal.beginTransaction();

        // Mengambil Foto Menggunakan Fungsi Utilitas (Efisien)
        try {
            String tempUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(calonMahasiswa));
            if (tempUrl != null && !tempUrl.trim().isEmpty()) {
                urlFotoProfil = tempUrl;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_biodata_calon_mahasiswa.jsp:247");}

        // Mengambil Data Lampiran Lainnya
        String[] jenisLampirans = {
            LampiranLainBiodataCalonMahasiswa.IJAZAH, LampiranLainBiodataCalonMahasiswa.TRANSKRIP_NILAI,
            LampiranLainBiodataCalonMahasiswa.KTP, LampiranLainBiodataCalonMahasiswa.LAMPIRAN_1,
            LampiranLainBiodataCalonMahasiswa.LAMPIRAN_2, LampiranLainBiodataCalonMahasiswa.LAMPIRAN_3,
            LampiranLainBiodataCalonMahasiswa.LAMPIRAN_4, LampiranLainBiodataCalonMahasiswa.LAMPIRAN_5,
            LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN, LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG
        };

        for (String jenis : jenisLampirans) {
            Long fileId = (Long) sessionLocal.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
                    .setProjection(Projections.property("id"))
                    .add(Restrictions.eq("jenis", jenis))
                    .add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa.getId()))
                    .setMaxResults(1).uniqueResult();

            if (fileId != null) {
                String fileUrl = CommonMedia.getFile(fileId, LampiranLainBiodataCalonMahasiswa.class.getName());
                addDataUrl(maps, "VIII. Dokumen / Lampiran Asli", jenis, Common.getBahasaConfig("Sudah Diunggah"), fileUrl);
            }
        }

        // Ambil Data Verifikasi Berkas
        if (calonMahasiswa.getGelombangPendaftaran() != null) {
            GelombangPendaftaran gel = (GelombangPendaftaran) sessionLocal.get(GelombangPendaftaran.class, calonMahasiswa.getGelombangPendaftaran().getId());
            if (gel != null) {
                Set<VerifikasiKelengkapanCalonMahasiswa> verifikasiTemp = gel.getVerifikasiKelengkapanCalonMahasiswas();
                JenisSeleksi jenisSeleksi = calonMahasiswa.getJenisSeleksi();
                if (jenisSeleksi != null) {
                    jenisSeleksi = (JenisSeleksi) sessionLocal.get(JenisSeleksi.class, jenisSeleksi.getId());
                    if (jenisSeleksi != null && !jenisSeleksi.getVerifikasiKelengkapanCalonMahasiswas().isEmpty()) {
                        verifikasiTemp = jenisSeleksi.getVerifikasiKelengkapanCalonMahasiswas();
                    }
                }

                List<VerifikasiKelengkapanCalonMahasiswa> verifikasiList = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>(verifikasiTemp);
                try { Collections.sort(verifikasiList); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_biodata_calon_mahasiswa.jsp:285");}

                for (VerifikasiKelengkapanCalonMahasiswa v : verifikasiList) {
                    if (v.getAktif()) {
                        BiodataCalonMahasiswaPunyaVerifikasiBerkas berkas = (BiodataCalonMahasiswaPunyaVerifikasiBerkas) sessionLocal
                                .createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
                                .add(Restrictions.eq("verifikasiKelengkapanCalonMahasiswa", v))
                                .add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).setMaxResults(1).uniqueResult();

                        if (berkas == null) {
                            berkas = new BiodataCalonMahasiswaPunyaVerifikasiBerkas();
                            berkas.setBiodataCalonMahasiswa(calonMahasiswa);
                            berkas.setVerifikasiKelengkapanCalonMahasiswa(v);
                            sessionLocal.saveOrUpdate(berkas);
                        }

                        String keterangan = (berkas.getKeterangan() == null || berkas.getKeterangan().isEmpty()) ? "" : " (Catatan: " + berkas.getKeterangan() + ")";
                        String nilaiBerkas = (berkas.getVerified() != null && berkas.getVerified() ? "Valid/Telah sesuai" : "Belum Diverifikasi") + keterangan;

                        LampiranLain lampiranLain = LampiranLain.ambil(berkas.getId(), BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName());
                        String lampUrl = null;
                        if (lampiranLain != null) {
                            lampUrl = lampiranLain.getGdrive() != null ? lampiranLain.forwardGDriveUrl() : CommonMedia.getFile(lampiranLain.getId(), LampiranLain.class.getName());
                        }

                        addDataUrl(maps, "IX. Verifikasi Kelengkapan Berkas", berkas.getVerifikasiKelengkapanCalonMahasiswa().getNama(), nilaiBerkas, lampUrl);
                    }
                }
            }
        }

        // Ambil Data Verifikasi Nilai Rapor
        if (calonMahasiswa.getPaket() != null) {
            @SuppressWarnings("unchecked")
            List<MatapelajaranSekolah> matapelajaranSekolahs = sessionLocal.createCriteria(PaketPunyaMatapelajaran.class)
                    .setProjection(Projections.property("matapelajaranSekolah"))
                    .createAlias("matapelajaranSekolah", "matapelajaranSekolah")
                    .add(Restrictions.eq("paket", calonMahasiswa.getPaket()))
                    .add(Restrictions.eq("matapelajaranSekolah.aktif", true))
                    .addOrder(Order.asc("matapelajaranSekolah.nama")).list();

            for (MatapelajaranSekolah mps : matapelajaranSekolahs) {
                BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran berkasNilai = (BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran) sessionLocal
                        .createCriteria(BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran.class)
                        .add(Restrictions.eq("matapelajaranSekolah", mps))
                        .add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).setMaxResults(1).uniqueResult();

                if (berkasNilai == null) {
                    berkasNilai = new BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran();
                    berkasNilai.setBiodataCalonMahasiswa(calonMahasiswa);
                    berkasNilai.setMatapelajaranSekolah(mps);
                    sessionLocal.saveOrUpdate(berkasNilai);
                }

                StringBuilder nilaiBuilder = new StringBuilder();
                if (calonMahasiswa.getPaket().getKelasVerifikasiRapor() != null) {
                    for (String nilaikelas : calonMahasiswa.getPaket().getKelasVerifikasiRapor().split(";")) {
                        if (!nilaikelas.trim().isEmpty() && berkasNilai.ambilNilai(nilaikelas.trim()) > 0.1 && berkasNilai.ambilVerifikasi(nilaikelas.trim())) {
                            String[] ca = StringUtils.split(nilaikelas, ":");
                            String kel = ca.length > 0 ? ca[0] : "";
                            String sem = ca.length > 1 ? ca[1] : "";
                            String s = "Kls:" + kel + (sem.isEmpty() ? "" : ", Smt:" + sem) + " = " + Common.numberFormat.get().format(berkasNilai.ambilNilai(nilaikelas.trim()));
                            if (nilaiBuilder.length() > 0) nilaiBuilder.append(", ");
                            nilaiBuilder.append(s);
                        }
                    }
                }

                String ketNilai = berkasNilai.getKeterangan() == null ? "" : berkasNilai.getKeterangan();
                String nilaiAkhir = nilaiBuilder.toString();
                if (!ketNilai.isEmpty()) {
                    nilaiAkhir += (nilaiAkhir.trim().isEmpty() ? "" : " (Catatan: ") + ketNilai + ")";
                }

                if(!nilaiAkhir.trim().isEmpty()) {
                    addData(maps, "X. Verifikasi Nilai Rapor", berkasNilai.getMatapelajaranSekolah().getNama(), nilaiAkhir);
                }
            }
        }

        tx.commit();
    } catch (Exception e) {
        if (tx != null && tx.isActive()) {
            try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_biodata_calon_mahasiswa.jsp:368");}
        }
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_biodata_calon_mahasiswa.jsp:370");
    } finally {
        if (sessionLocal != null) {
            try { sessionLocal.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_biodata_calon_mahasiswa.jsp:373");}
            try { sessionLocal.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_biodata_calon_mahasiswa.jsp:374");}
            try { sessionLocal.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_biodata_calon_mahasiswa.jsp:375");}
        }
    }

    String judulNamaPerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request).getNama();
    String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
%>

<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= Common.getBahasaConfig("Biodata Calon Mahasiswa") %> - <%= calonMahasiswa.getNama() %></title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        body { background-color: #f8f9fa; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333; }
        .print-container { max-width: 900px; margin: 0 auto; background: #fff; padding: 40px; box-shadow: 0 5px 15px rgba(0,0,0,0.05); }
        .kop-surat { border-bottom: 3px solid #0d6efd; padding-bottom: 15px; margin-bottom: 30px; display: flex; align-items: center; }
        .kop-surat img { max-height: 80px; margin-right: 20px; }
        .kop-surat .judul { flex-grow: 1; text-align: center; }
        .kop-surat h4 { font-weight: 800; margin: 0; color: #0d6efd; letter-spacing: 1px; text-transform: uppercase; }
        .kop-surat p { margin: 0; font-size: 0.9rem; color: #6c757d; }
        
        .foto-profil { width: 120px; height: 160px; object-fit: cover; border: 3px solid #dee2e6; padding: 3px; border-radius: 8px; }
        
        .section-title { background-color: #f1f3f5; color: #495057; font-weight: 700; padding: 8px 15px; border-left: 4px solid #0d6efd; margin-top: 25px; margin-bottom: 15px; font-size: 1.1rem; }
        
        .data-table { width: 100%; font-size: 0.95rem; }
        .data-table td { padding: 6px 10px; vertical-align: top; border-bottom: 1px dashed #e9ecef; }
        .data-table tr:last-child td { border-bottom: none; }
        .data-table .col-label { width: 35%; font-weight: 600; color: #555; }
        .data-table .col-separator { width: 2%; text-align: center; }
        .data-table .col-value { width: 63%; color: #212529; }
        
        .btn-print-float { position: fixed; bottom: 30px; right: 30px; z-index: 1000; border-radius: 50px; padding: 12px 25px; box-shadow: 0 4px 10px rgba(0,0,0,0.3); font-weight: bold; transition: transform 0.2s; }
        .btn-print-float:hover { transform: scale(1.05); }

        @media print {
            body { background-color: #fff; }
            .print-container { box-shadow: none; padding: 0; max-width: 100%; margin: 0; }
            .btn-print-float { display: none !important; }
            .section-title { background-color: transparent !important; color: #000; border-bottom: 2px solid #000; border-left: none; padding-left: 0; }
            .data-table td { border-bottom: 1px solid #eee; padding: 4px 0; }
        }
    </style>
</head>
<body>

    <button onclick="window.print()" class="btn btn-primary btn-print-float d-print-none">
        <i class="fas fa-print me-2"></i><%= Common.getBahasaConfig("Cetak Dokumen") %>
    </button>

    <div class="print-container">
        
        <div class="kop-surat">
            <% if (logo_PerguruanTinggi != null && !logo_PerguruanTinggi.isEmpty()) { %>
                <img src="<%= logo_PerguruanTinggi %>" alt="Logo">
            <% } %>
            <div class="judul">
                <h4><%= judulNamaPerguruanTinggi != null ? judulNamaPerguruanTinggi : "KAMPUS" %></h4>
                <p><%= Common.getBahasaConfig("BIODATA LENGKAP CALON MAHASISWA BARU") %></p>
            </div>
            <div style="width: 80px;"></div> 
        </div>

        <div class="row align-items-center mb-4">
            <div class="col-auto text-center">
                <img src="<%= urlFotoProfil %>" alt="Foto" class="foto-profil shadow-sm">
            </div>
            <div class="col">
                <h3 class="fw-bold text-dark mb-1"><%= calonMahasiswa.getNama().toUpperCase() %></h3>
                <h5 class="text-primary mb-3"><i class="fas fa-hashtag me-2"></i><%= calonMahasiswa.getNoRegistrasi() %></h5>
                
                <table class="data-table" style="font-size: 0.9rem;">
                    <% 
                    // Menampilkan Grup Utama di samping foto
                    for (Map<String, String> row : maps) {
                        if (row.get("grup").equals("Data Registrasi Utama")) {
                    %>
                        <tr>
                            <td class="col-label text-muted" style="width: 40%;"><%= Common.getBahasaConfig(row.get("label")) %></td>
                            <td class="col-separator">:</td>
                            <td class="col-value fw-semibold"><%= row.get("nilai") %></td>
                        </tr>
                    <%  }
                    } %>
                </table>
            </div>
        </div>

        <%
        String currentGrup = "";
        boolean isTableOpen = false;

        for (Map<String, String> row : maps) {
            String grup = row.get("grup");
            
            // Lewati grup utama karena sudah dirender di atas
            if (grup.equals("Data Registrasi Utama")) continue;

            if (!grup.equals(currentGrup)) {
                if (isTableOpen) {
                    out.print("</tbody></table></div>");
                }
                currentGrup = grup;
                isTableOpen = true;
                
                out.print("<div class='section-title'>" + Common.getBahasaConfig(grup) + "</div>");
                out.print("<div class='table-responsive'><table class='data-table'><tbody>");
            }

            String label = Common.getBahasaConfig(row.get("label"));
            String nilai = row.get("nilai");
            String urlLink = row.get("url");

            out.print("<tr>");
            out.print("<td class='col-label'>" + label + "</td>");
            out.print("<td class='col-separator'>:</td>");
            
            if (urlLink != null && !urlLink.isEmpty()) {
                out.print("<td class='col-value'>" + nilai + " <a href='" + urlLink + "' target='_blank' class='badge bg-primary text-decoration-none ms-2 d-print-none'><i class='fas fa-up-right-from-square me-1'></i>" + Common.getBahasaConfig("Buka Berkas") + "</a></td>");
            } else {
                out.print("<td class='col-value'>" + nilai + "</td>");
            }
            out.print("</tr>");
        }

        if (isTableOpen) {
            out.print("</tbody></table></div>");
        }
        %>

        <div class="row mt-5 pt-4">
            <div class="col-7"></div>
            <div class="col-5 text-center">
                <p class="mb-5"><%= Common.getBahasaConfig("Dicetak pada tanggal") %>: <br> <strong><%= Common.dateFormat1.get().format(new Date()) %></strong></p>
                <p class="mt-5 pt-4 border-top border-dark d-inline-block px-4">
                    <strong><%= calonMahasiswa.getNama().toUpperCase() %></strong><br>
                    <small class="text-muted"><%= Common.getBahasaConfig("Tanda Tangan Calon Mahasiswa") %></small>
                </p>
            </div>
        </div>

    </div>
</body>
</html>