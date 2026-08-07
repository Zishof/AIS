<%@page import="ais.action.master.pmb.VerifikasiPMBHelper"%>
<%@page import="ais.action.master.helper.KegiatanHelper"%>
<%@page import="ais.action.report.Report"%>
<%@page import="java.util.*"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPMB"%>
<%@page import="ais.common.CommonEmail"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.GelombangPendaftaran"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.RuangPaketPMB"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.action.master.KonfigurasiTampilanBiodataCalonMahasiswaAction"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%!
private boolean containsIgnoreCaseKartuUjianPmb(String text, String pattern) {
    return text != null && pattern != null && text.toLowerCase().indexOf(pattern.toLowerCase()) >= 0;
}

private boolean isPendaftaranGratisKartuUjianPmb(BiodataCalonMahasiswa cama) {
    if (cama == null) {
        return false;
    }
    String namaPaket = cama.getPaket() == null ? "" : cama.getPaket().getNama();
    String namaSeleksi = cama.getJenisSeleksi() == null ? "" : cama.getJenisSeleksi().getNama();
    String namaGelombang = cama.getGelombangPendaftaran() == null ? "" : cama.getGelombangPendaftaran().getNama();
    String gabungan = namaPaket + " " + namaSeleksi + " " + namaGelombang;
    return containsIgnoreCaseKartuUjianPmb(gabungan, "kip")
            || containsIgnoreCaseKartuUjianPmb(gabungan, "beasiswa")
            || containsIgnoreCaseKartuUjianPmb(gabungan, "gratis")
            || containsIgnoreCaseKartuUjianPmb(gabungan, "bebas");
}
%>

<%
    // =========================================================================
    // API PEMBUAT KARTU UJIAN (JSON ENDPOINT)
    // =========================================================================
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    
    String idParam = request.getParameter("id");
    Tbmuser tbmuser = Common.getCurrentUser(request);

    // 1. Validasi Parameter ID
    if (idParam == null || idParam.trim().isEmpty()) {
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("ID Calon Mahasiswa tidak valid.") + "\"}");
        return;
    }
    
    BiodataCalonMahasiswa cama = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, idParam.trim(), true);
    if (cama == null) {
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data calon mahasiswa tidak ditemukan di dalam sistem.") + "\"}");
        return;
    }
    
    Session sessionLocal = null;
    Transaction tx = null;
    
    try {
        // =====================================================================
        // MEMBUKA SESI DATABASE UNTUK VALIDASI DAN GENERATE PDF
        // =====================================================================
        sessionLocal = HibernateUtil.openSession();
        tx = sessionLocal.beginTransaction();
        
        // Re-attach object ke session agar relasi (Lazy Load) terbaca sempurna
        cama = (BiodataCalonMahasiswa) sessionLocal.get(BiodataCalonMahasiswa.class, cama.getId());
        GelombangPendaftaran gelombang = cama == null ? null : cama.getGelombangPendaftaran();
        if (gelombang == null) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Gelombang pendaftaran belum terhubung dengan data calon mahasiswa.") + "\"}");
            return;
        }
        
        // 2. VALIDASI VERIFIKASI DOKUMEN
        if (gelombang != null && gelombang.getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian() != null && gelombang.getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian()) {
            if (!VerifikasiPMBHelper.checkVerifikasi(cama)) {
                out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Dokumen persyaratan Anda belum lengkap atau belum diverifikasi oleh panitia penerimaan.") + "\"}");
                tx.rollback(); return;
            }
        }
        
        // 3. VALIDASI KELENGKAPAN BIODATA (Hanya berlaku untuk Non-Admin)
        if (!Common.getApakahAdminLain(tbmuser)) {
            List<String> daftarWajibDiisi = KonfigurasiTampilanBiodataCalonMahasiswaAction.dataYangWajibDiisi(tbmuser);
            if (daftarWajibDiisi != null) {
                for (String key : daftarWajibDiisi) {
                    if (Common.checkIsNull(BiodataCalonMahasiswa.class, cama, key)) {
                        String msg = Common.getBahasaConfig("Biodata Anda belum lengkap. Kolom") + " [" + KonfigurasiTampilanBiodataCalonMahasiswaAction.keyDesc(key) + "] " + Common.getBahasaConfig("masih kosong.");
                        out.print("{\"status\":\"error\", \"message\":\"" + msg + "\"}");
                        tx.rollback(); return;
                    }
                }
            }
        }
        
        // 4. VALIDASI PEMBAYARAN REGISTRASI
        if (gelombang != null && gelombang.getHarusBayarSebelumBisaLogin() != null && gelombang.getHarusBayarSebelumBisaLogin()) {
            Kegiatan kegiatan = cama.getPembayaranRegistrasi();
            if (kegiatan == null || kegiatan.getId() == null) {
                kegiatan = KegiatanHelper.checkKegiatanCalonMahasiswa(ConstantValues.PENDAFTARAN_CALON_MAHASISWA,
                        cama, 0, cama.getTahunAkademik(), true, false, null, sessionLocal);
                cama.setPembayaranRegistrasi(kegiatan);
            }
            boolean pendaftaranGratis = isPendaftaranGratisKartuUjianPmb(cama);
            boolean tagihanNol = kegiatan != null && (kegiatan.getAmount() + kegiatan.getAmountTerhutang()) < 0.01;
            Double persenLunas = kegiatan == null ? null : kegiatan.getPersentaseLunas();
            boolean pembayaranLunas = kegiatan != null
                    && (Boolean.TRUE.equals(kegiatan.getLunas())
                            || (persenLunas != null && persenLunas >= 99.0)
                            || tagihanNol);
            if (!pendaftaranGratis && !pembayaranLunas) {
                String infoBelumBayar = Common.getKonfigurasi("infoBelumbayarSaatProsescalonMahasiswa", "Calon Mahasiswa dengan nomor pendaftaran [noreg] belum dapat diproses karena belum melakukan pelunasan pembayaran.").getNilai();
                infoBelumBayar = StringUtils.replace(infoBelumBayar, "[noreg]", cama.getNoRegistrasi());
                out.print("{\"status\":\"error\", \"message\":\"" + infoBelumBayar + "\"}");
                tx.rollback(); return;
            }
        }
        
        // =====================================================================
        // 5. GENERATE ATAU AMBIL NOMOR UJIAN
        // =====================================================================
        String noUjian = cama.getNoUjian();
        if (noUjian == null || noUjian.trim().isEmpty()) {
            List<String> warnings = new ArrayList<String>();
            noUjian = CommonPMB.generateNoUjian(tbmuser, cama, warnings);
            
            // Menghentikan proses dan menampilkan peringatan jika ada kendala generate no ujian
            if (!warnings.isEmpty()) {
                StringBuilder sbWarning = new StringBuilder();
                for (String w : warnings) {
                    if (sbWarning.length() > 0) {
                        sbWarning.append(" ");
                    }
                    // Membersihkan tanda kutip agar tidak merusak struktur JSON
                    sbWarning.append(w.replace("\"", "&quot;"));
                }
                out.print("{\"status\":\"error\", \"message\":\"" + sbWarning.toString() + "\"}");
                tx.rollback(); 
                return;
            }
        }
        
        if (noUjian == null || noUjian.trim().isEmpty()) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sistem gagal menghasilkan Nomor Ujian secara otomatis. Silakan hubungi panitia.") + "\"}");
            tx.rollback(); 
            return;
        }
        
        // 6. DAPATKAN RUANG UJIAN (ALOKASI)
        RuangPaketPMB ruangPaketPMB = (RuangPaketPMB) sessionLocal.createCriteria(RuangPaketPMB.class)
                .add(Restrictions.eq("biodataCalonMahasiswa", cama)).setMaxResults(1).uniqueResult();
                
        if (ruangPaketPMB == null && cama.getNoUjian() != null && !cama.getNoUjian().trim().isEmpty()) {
            ruangPaketPMB = CommonPMB.dapatkanRuangUjian(cama);
        }
        
        if (ruangPaketPMB == null) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Anda belum mendapatkan alokasi lokasi/ruang ujian. Harap muat ulang halaman atau hubungi panitia.") + "\"}");
            tx.rollback(); return;
        }
        
        // =====================================================================
        // 7. TANDAI STATUS TELAH CETAK KARTU KE DATABASE (VIA MANUAL QUERY)
        // =====================================================================
        cama.put("1", "setCetakKartu");
        cama.setCetakKartu(1);
        cama.setNoUjian(noUjian);
        
     // Simpan variabel ke dalam variabel final agar bisa diakses di dalam Thread (Anonymous Class)
        final String fNoUjian = noUjian;
        final Long fIdCama = cama.getId();

        new Thread(new Runnable() {
            public void run() {
                Session threadSession = null;
                Transaction threadTx = null;
                try {
                    // Membuka session baru khusus untuk thread ini
                    threadSession = HibernateUtil.getSessionFactory().openSession();
                    threadTx = threadSession.beginTransaction();

                    String hqlUpdate = "UPDATE BiodataCalonMahasiswa SET cetakKartu = 1, noUjian = :noUjian WHERE id = :idCama";
                    threadSession.createQuery(hqlUpdate)
                                .setParameter("noUjian", fNoUjian)
                                .setParameter("idCama", fIdCama)
                                .executeUpdate();

                    threadTx.commit();
                    // System.out.println("Update no_ujian berhasil di latar belakang untuk ID: " + fIdCama);
                } catch (Exception e) {
                    if (threadTx != null) threadTx.rollback();
                    System.err.println("Gagal update no_ujian di background thread: " + e.getMessage());
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:172");
                } finally {
                    if (threadSession != null) {
                        try { threadSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:175");}
                        try { threadSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:176");}
                        try { threadSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:177");}
                    }
                }
            }
        }).start();

        // =====================================================================
        // 8. GENERATE PDF KARTU UJIAN MENGGUNAKAN JASPER REPORTS
        // =====================================================================
        Map parameters = ais.common.HashMapGenerator.getRand();
        Common.insertProperty(BiodataCalonMahasiswa.class, cama, parameters, "pmb");
        parameters.put("biodata_id", cama.getId());
        parameters.put("nomorUjian", noUjian); 
        
        cama.putPhoto(parameters);

        // Memanggil Engine Pembuat Laporan PDF Bawaan Sistem
        // Endpoint ini mengembalikan JSON berisi URL PDF, bukan download ZK langsung.
        // Jangan pakai mode download=true karena Report akan memanggil Filedownload.save()
        // yang hanya valid pada konteks ZK dan menyebabkan NPE saat dipanggil dari JSP.
        File filePDF = Report.generateDownloadReport(Report.PDF, parameters, "KartuUjianSpmbMandiri", null,
                ais.ui.util.WaktuUtil.getDate(), Common.locale, false);
        
        // Mengirim Email Notifikasi (Opsional, dibungkus try-catch agar jika gagal email, cetak PDF tetap berjalan)
        try {
            CommonEmail.infoDaftarUjianMahasiswa(cama, new File[] { filePDF });
        } catch (Exception emailEx) { ais.common.ErrorAuditUtil.record(emailEx, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:199");
            // Mengabaikan pesan kegagalan email demi kelancaran proses utama
        }

        // Komit Transaksi Database
        tx.commit();
        
        // =====================================================================
        // 9. MENGEMBALIKAN URL PDF TERENKRIPSI KE FRONT-END
        // =====================================================================
        String urlPDF = Common.ROOT + "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(filePDF.getName()), "UTF-8");
        
        out.print("{\"status\":\"success\", \"url\":\"" + urlPDF + "\"}");
        
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:215");
        if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:216");} }
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Terjadi kesalahan internal peladen saat memproses dan membuat dokumen PDF kartu ujian.") + "\"}");
    } finally {
        if (sessionLocal != null) {
            try { sessionLocal.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:221");}
            try { sessionLocal.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:222");}
            try { sessionLocal.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_cetak_kartu_ujian.jsp:223");}
        }
    }
%>
