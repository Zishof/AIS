<%-- BUILD 2026-06-29: dipaksa rekompilasi. FIX IncompatibleClassChangeError "Expected static method
     ais.action.ws.util.PembayaranUtil.getDetailBiayaCalonMahasiswa(...)": JSP lama ter-compile saat method
     itu masih STATIC (invokestatic); method kini INSTANCE (singleton via PembayaranUtil.getInstance()).
     WAJIB bersihkan work dir Tomcat (.../work/Catalina/<host>/sat/...) lalu restart agar JSP dikompilasi ulang. --%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.VOMahasiswa"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.DetailKegiatan"%>
<%@page import="ais.database.model.DetailBiaya"%>
<%@page import="ais.database.model.ItemBiaya"%>
<%@page import="ais.database.model.CicilanPembayaran"%>
<%@page import="ais.database.model.JenisPembayaran"%>
<%@page import="ais.database.model.PengaturanPembayaranBulanan"%>
<%@page import="ais.database.model.JadwalPembayaran"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.action.ws.util.PembayaranUtil"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.action.report.CommonReportHelper"%>
<%@page import="ais.action.report.Report"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Collection"%>
<%@page import="java.util.Collections"%>

<%!
    private static void executeNativeUpdateTransaction(GeneralValueObject entity) {
        Session sessionUpdate = null;
        Transaction tx = null;
        try {
            sessionUpdate = HibernateUtil.openSession();
            tx = sessionUpdate.beginTransaction();
            Common.refreshUpdate(sessionUpdate, entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:55");}
            }
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:57");
        } finally {
            if (sessionUpdate != null && sessionUpdate.isOpen()) {
                try { sessionUpdate.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:60");}
                try { sessionUpdate.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:61");}
            }
            try { HibernateUtil.closeSession(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:63");}
        }
    }
%>

<%
    String action = request.getParameter("action");
    String idStr = request.getParameter("id");
    String jkIdStr = request.getParameter("jkId");
    String isMahasiswaStr = request.getParameter("isMahasiswa");
    String smtStr = request.getParameter("smt");
    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(6);
    String refreshStr = request.getParameter("refresh");
    boolean refresh = "true".equalsIgnoreCase(refreshStr);
    
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        return;
    }

    boolean isUserMhs = (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null);
    boolean editPriv = false;
    boolean addPriv = false;
    boolean deletePriv = false;
    boolean isAdminPriv = false;
    
    if (tbmuser != null && tbmuser.getUserId() != null) {
        editPriv = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
        deletePriv = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
        addPriv = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser);
        
        if (Common.getApakahAdminLain(tbmuser)) {
            editPriv = true;
            deletePriv = true;
            addPriv = true;
            isAdminPriv = true;
        }
    }

    boolean bolehMerubahCicilan = false;
    if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null) {
        try { bolehMerubahCicilan = tbmuser.hakAkses().getRoleId().trim().equalsIgnoreCase(Tbmrole.ADMINISTRATOR); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:104");}
    }

    if (isUserMhs) {
        bolehMerubahCicilan = false;
    } else {
        try {
            String admLain = Common.getKonfigurasi("admin_yang_bisa_menghapus_data_pembayaran_mahasiswa", "am").getNilai();
            if (admLain != null && !admLain.isEmpty()) {
                String[] aa = admLain.split(";");
                for (String a : aa) {
                    if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null) {
                        if (a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId())) { bolehMerubahCicilan = true; break; }
                    }
                }
            }
            if (!bolehMerubahCicilan) {
                admLain = Common.getKonfigurasi("admin_lain_bisa_menghapus_pembayaran_mahasiswa", "").getNilai();
                if (admLain != null && !admLain.isEmpty()) {
                    String[] aa = admLain.split(";");
                    for (String a : aa) {
                        if (a.trim().equalsIgnoreCase(tbmuser.getUserId())) { bolehMerubahCicilan = true; break; }
                    }
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:129");}
    }

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        
        // =========================================================================================
        // AKSI: CETAK SURAT TAGIHAN
        // =========================================================================================
        if ("CETAK_SURAT_TAGIHAN".equals(action)) {
            try {
                long id = Long.parseLong(idStr);
                boolean isMahasiswa = "true".equalsIgnoreCase(isMahasiswaStr);
                
                Mahasiswa mhs = null;
                BiodataCalonMahasiswa calon = null;
                
                if (isMahasiswa) {
                    mhs = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), id, true);
                } else {
                    calon = (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), id, true);
                }
                
                if (mhs == null && calon == null) {
                    mhs = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), id, true);
                    if (mhs == null) {
                        calon = (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), id, true);
                    }
                }
                
                if (mhs == null && calon == null) {
                    response.setStatus(404);
                    out.print(Common.getBahasaConfig("Data pelanggan tidak ditemukan."));
                    return;
                }
                
                String tanggalStr = request.getParameter("tanggalSurat");
                String tanggalJatuhTempoStr = request.getParameter("tanggalJatuhTempo");
                String nomor = request.getParameter("nomorSurat");
                String dendaStr = request.getParameter("denda");
                String[] tagihanTerpilih = request.getParameterValues("tagihanTerpilih");
                String caraBayarId = request.getParameter("caraBayar");
                Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
                
                Long personId = mhs != null ? mhs.getId() : calon.getId();
                String tahunAkademik = mhs != null 
                    ? (mhs.getTahunangkatan() != null ? String.valueOf(mhs.getTahunangkatan()) : "-") 
                    : (calon.getTahunAkademik() != null ? String.valueOf(calon.getTahunAkademik()) : "-");
                
                String namaPerson = mhs != null ? mhs.getNama() : calon.getNama();
                String nimPerson = mhs != null ? mhs.getNim() : (calon.getNoRegistrasi() != null ? calon.getNoRegistrasi() : (calon.getKode() != null ? calon.getKode() : "-"));

                parameters.put("biodata_id", personId);
                Date tanggal = ais.ui.util.WaktuUtil.getDate();
                if (tanggalStr != null && !tanggalStr.trim().isEmpty()) {
                    try { tanggal = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(tanggalStr); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:185");}
                }
                parameters.put("tanggal", tanggal);
                parameters.put("nomor", nomor != null ? nomor : "");
                parameters.put("tahunakademik", tahunAkademik);
                
                Date tanggalJatuhTempo = ais.ui.util.WaktuUtil.getDate();
                if (tanggalJatuhTempoStr != null && !tanggalJatuhTempoStr.trim().isEmpty()) {
                    try { tanggalJatuhTempo = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(tanggalJatuhTempoStr); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:193");}
                }
                parameters.put("tanggalJatuhTempo", tanggalJatuhTempo);
                
                if (caraBayarId != null && !caraBayarId.isEmpty()) {
                    JenisPembayaran jp = (JenisPembayaran) sess.get(JenisPembayaran.class, Long.parseLong(caraBayarId));
                    if (jp != null) {
                        if (jp.getBank() != null) parameters.put("rekening_pembayaran", " melalui Bank " + jp.getBank().getNama() + "</b>");
                        else parameters.put("rekening_pembayaran", " melalui  " + jp.getNama());
                    }
                }
                
                parameters.put("prosentaseDenda", dendaStr != null && !dendaStr.isEmpty() ? Double.parseDouble(dendaStr) : 0.0);
                parameters.put("nama", namaPerson != null ? namaPerson : "-");
                parameters.put("nim", nimPerson != null ? nimPerson : "-");
                parameters.put("semester", smtStr != null && !smtStr.trim().isEmpty() ? Integer.parseInt(smtStr) : 1);
                
                String namaKaprodi = "(......................)";
                String namaJurusan = "-";
                String namaFakultas = "-";
                
                ais.database.model.Jurusan jur = mhs != null ? mhs.getJurusan() : calon.ambilJurusan();
                if (jur != null) {
                    namaJurusan = jur.getNama() != null ? jur.getNama() : "-";
                    if (jur.getKaprodi() != null && jur.getKaprodi().getNama() != null) {
                        namaKaprodi = jur.getKaprodi().getNama();
                    }
                    if (jur.getFakultas() != null && jur.getFakultas().getNama() != null) {
                        namaFakultas = jur.getFakultas().getNama();
                    }
                }
                
                parameters.put("kaprodi", namaKaprodi);
                parameters.put("jurusan", namaJurusan);
                parameters.put("fakultas", namaFakultas);
                
                List<Map<String, Object>> newMaps = new ArrayList<Map<String, Object>>();
                StringBuilder tagihanDataBldr = new StringBuilder();
                if (tagihanTerpilih != null) {
                    for (String tag : tagihanTerpilih) {
                        String[] parts = tag.split("\\|");
                        if (parts.length == 2) {
                            Map<String, Object> map = new HashMap<String, Object>();
                            map.put("label", parts[0]);
                            map.put("nilai", Double.parseDouble(parts[1]));
                            newMaps.add(map);
                            if (tagihanDataBldr.length() > 0) tagihanDataBldr.append("; ");
                            tagihanDataBldr.append(parts[0]).append(", Rp ").append(Common.numberFormat.get().format(Double.parseDouble(parts[1])));
                        }
                    }
                }
                
                parameters.put("tagihanData", tagihanDataBldr.toString());
                parameters.put("maps", newMaps);
                
                String nimFile = nimPerson != null ? nimPerson : (mhs != null ? "NIM" : "NOREG");
                String namaFile = namaPerson != null ? namaPerson : "NAMA";
                parameters.put("file_laporan", URLEncoder.encode(nimFile + " " + namaFile + " Tagihan", "UTF-8"));
                parameters.put("jenis_tagihan", mhs != null ? "MAHASISWA" : "CALON MAHASISWA");
                
                List maps = null;
                File filePdf = Report.generatePDFReport("pdf", parameters, "Surat_Tagihan_Mahasiswa", WaktuUtil.getDate(), maps);
                
                if(filePdf != null && filePdf.exists()){
                    String pathPdf = "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(filePdf.getName()), "UTF-8");
                    out.print(Common.ROOT + pathPdf);
                } else {
                    response.setStatus(500);
                    out.print(Common.getBahasaConfig("Gagal membuat dokumen surat tagihan."));
                }
                return;
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:265");
                response.setStatus(500);
                out.print(Common.getBahasaConfig("Galat internal peladen: ") + e.getMessage());
                return;
            }
        }

        // =========================================================================================
        // AKSI: CETAK STRUK PEMBAYARAN PDF 
        // =========================================================================================
        if ("CETAK_STRUK".equals(action)) {
             try {
                long id = Long.parseLong(idStr);
                long jkId = Long.parseLong(jkIdStr);
                int smtInt = (smtStr != null && !smtStr.trim().isEmpty()) ? Integer.parseInt(smtStr) : 1;
                boolean isMahasiswa = "true".equalsIgnoreCase(isMahasiswaStr);
                
                VOMahasiswa personCetak = (VOMahasiswa) (isMahasiswa 
                                ? ConstantValues.ambil(Mahasiswa.class.getName(), id, true) 
                                : ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), id, true));
                                
                JenisKegiatan jkCetak = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(), jkId, true);
                
                Kegiatan kCetak = null;
                if (personCetak != null && jkCetak != null) {
                    kCetak = personCetak.ambilKegiatansRefresh(smtInt, jkCetak, true);
                }

                if (kCetak != null) {
                    File fileStruk = null;
                    if (isMahasiswa) {
                        fileStruk = CommonReportHelper.cetakBuktipembayaranMahasiswa(kCetak, true);
                    } else {
                        fileStruk = CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kCetak, true);
                    }
                    
                    if (fileStruk != null && fileStruk.exists()) {
                        String pathPdf = "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(fileStruk.getName()), "UTF-8");
                        out.print(Common.ROOT + pathPdf);
                    } else {
                        response.setStatus(500);
                        out.print("Gagal membuat dokumen struk pembayaran.");
                    }
                } else {
                    response.setStatus(404);
                    out.print("Data kegiatan pembayaran tidak ditemukan.");
                }
             } catch (Exception e) {
                 e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:313");
                 response.setStatus(500);
                 out.print(Common.getBahasaConfig("Galat internal peladen: ") + e.getMessage());
                 return;
             }
            return;
        }

        // =========================================================================================
        // AKSI: HAPUS DATA PEMBAYARAN
        // =========================================================================================
        if ("DELETE_CICILAN".equals(action) && deletePriv) {
            String idCicilanHapus = request.getParameter("idCicilan");
            if (idCicilanHapus != null && !idCicilanHapus.trim().isEmpty()) {
                CicilanPembayaran cicilanHapus = (CicilanPembayaran) sess.get(CicilanPembayaran.class, Long.parseLong(idCicilanHapus));
                if (cicilanHapus != null && cicilanHapus.getPostingHistory() == null && bolehMerubahCicilan) {
                    sess.delete(cicilanHapus);
                    sess.flush(); action = "LOAD_TAGIHAN";
                    out.print("<script>if(typeof tampilkanToast === 'function') tampilkanToast('"+Common.getBahasaConfigJS("Data pembayaran berhasil dibatalkan dan dihapus.")+"', 'bg-success text-white');</script>");
                }
            }
        }

        // =========================================================================================
        // AKSI: UBAH DATA PEMBAYARAN
        // =========================================================================================
        if ("UPDATE_CICILAN".equals(action) && editPriv) {
            String idCicilanUbah = request.getParameter("idCicilan");
            if (idCicilanUbah != null && !idCicilanUbah.trim().isEmpty()) {
                CicilanPembayaran cp = (CicilanPembayaran) sess.get(CicilanPembayaran.class, Long.parseLong(idCicilanUbah));
                if (cp != null && cp.getPostingHistory() == null && bolehMerubahCicilan) {
                    try {
                        double nilaiBaru = Double.parseDouble(request.getParameter("nilai") != null ? request.getParameter("nilai") : "0");
                        double dendaBaru = Double.parseDouble(request.getParameter("denda") != null ? request.getParameter("denda") : "0");
                        String tanggalStr = request.getParameter("tanggal");
                        String tanggalTagihanStr = request.getParameter("tanggalTagihan");
                        String idCaraBayar = request.getParameter("caraBayar");
                        String valItemBiaya = request.getParameter("itemBiaya");
                        String ketBaru = request.getParameter("keterangan");

                        cp.setNilai(nilaiBaru); cp.setNilaiDiubah(nilaiBaru); cp.setDenda(dendaBaru); cp.setKeterangan(ketBaru);
                        if (tanggalStr != null && !tanggalStr.trim().isEmpty()) {
                            Date tgl = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(tanggalStr);
                            cp.setTanggal(tgl);
                        }
                        if (tanggalTagihanStr != null && !tanggalTagihanStr.trim().isEmpty()) {
                            Date tglTagihan = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(tanggalTagihanStr);
                            try { cp.getClass().getMethod("setTanggalTagihan", Date.class).invoke(cp, tglTagihan); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:360");}
                        }
                        if (idCaraBayar != null && !idCaraBayar.trim().isEmpty()) {
                            JenisPembayaran jp = (JenisPembayaran) sess.get(JenisPembayaran.class, Long.parseLong(idCaraBayar));
                            if (jp != null) cp.setJenisPembayaran(jp);
                        }
                        if (valItemBiaya != null && !valItemBiaya.trim().isEmpty()) {
                            if (valItemBiaya.startsWith("PB_")) {
                                PengaturanPembayaranBulanan pb = (PengaturanPembayaranBulanan) sess.get(PengaturanPembayaranBulanan.class, Long.parseLong(valItemBiaya.replace("PB_", "")));
                                if (pb != null) { cp.setPengaturanPembayaranBulanan(pb); cp.setDetailBiaya(pb.getDetailBiaya()); cp.setItemBiaya(pb.getDetailBiaya() != null ? pb.getDetailBiaya().getItemBiaya() : null); }
                            } else if (valItemBiaya.startsWith("DB_")) {
                                DetailBiaya db = (DetailBiaya) sess.get(DetailBiaya.class, Long.parseLong(valItemBiaya.replace("DB_", "")));
                                if (db != null) { cp.setPengaturanPembayaranBulanan(null); cp.setDetailBiaya(db); cp.setItemBiaya(db.getItemBiaya()); }
                            }
                        }
                        Common.refreshUpdate(sess, cp);
                        sess.flush(); action = "LOAD_TAGIHAN";
                        out.print("<script>if(typeof tampilkanToast === 'function') tampilkanToast('"+Common.getBahasaConfigJS("Rincian riwayat pembayaran berhasil diperbarui.")+"', 'bg-success text-white');</script>");
                    } catch(Exception e) {
                        out.print("<script>if(typeof tampilkanToast === 'function') tampilkanToast('"+Common.getBahasaConfigJS("Gagal menyimpan pembaruan:")+" "+Common.jsEscape(e.getMessage())+"', 'bg-danger text-white');</script>");
                    }
                }
            }
        }

        // =========================================================================================
        // AKSI UTAMA: PEMUATAN TAGIHAN
        // =========================================================================================
        if ("LOAD_TAGIHAN".equals(action)) {
            if (idStr == null || idStr.trim().isEmpty() || jkIdStr == null || jkIdStr.trim().isEmpty()) {
                out.print("<div class='alert alert-warning shadow-sm rounded-4 border-0'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Identitas pelanggan atau jenis kegiatan tidak valid.") + "</div>");
                return;
            }

            long id = Long.parseLong(idStr);
            long jkId = Long.parseLong(jkIdStr);
            boolean isMahasiswa = "true".equalsIgnoreCase(isMahasiswaStr);

            VOMahasiswa person = (VOMahasiswa) (isMahasiswa 
                            ? ConstantValues.ambil(Mahasiswa.class.getName(), id, true) 
                            : ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), id, true));
            
            // PERBAIKAN: LAYER 2 - Cross-check fallback jika data tidak ketemu
            if (person == null) {
                if (!isMahasiswa) {
                    person = (VOMahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), id, true);
                    if (person != null) isMahasiswa = true; 
                } else {
                    person = (VOMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), id, true);
                    if (person != null) isMahasiswa = false;
                }
            }

            if (refresh && person != null) {
                if (!isMahasiswa) {
                    person.reInitKegiatan(sess);
                } else {
                    person.reInitKegiatan(sess);
                }
            }
            
            JenisKegiatan jk = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(), jkId, true);
            
            if (person == null || jk == null) {
                out.print("<div class='alert alert-danger shadow-sm rounded-4 border-0'><i class='fas fa-user-times me-2'></i>" + Common.getBahasaConfig("Data pelanggan tidak ditemukan pada pangkalan data.") + "</div>");
                return;
            }
            
            // PERBAIKAN: LAYER 3 - Smart Re-Routing untuk Mahasiswa yang mengakses Kegiatan PMB
            if (isMahasiswa && person instanceof Mahasiswa && jk != null) {
                boolean isCalonAct = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(jk.getId()));
                boolean isDaftarAct = (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId().equals(jk.getId()));
                
                if ((isCalonAct || isDaftarAct) && ((Mahasiswa)person).getBiodataCalonMahasiswa() != null) {
                    BiodataCalonMahasiswa bcm = (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), ((Mahasiswa)person).getBiodataCalonMahasiswa(), true);
                    if (bcm != null) {
                        person = bcm;
                        isMahasiswa = false;
                    }
                }
            }

            // EKSTRAKSI FOTO PELANGGAN
            Tbmuser dummyUser = new Tbmuser();
            if (person instanceof Mahasiswa) dummyUser.setMahasiswa((Mahasiswa) person);
            else if (person instanceof BiodataCalonMahasiswa) dummyUser.setBiodataCalonMahasiswa((BiodataCalonMahasiswa) person);
            
            String urlFotoDB = CommonMedia.getUrlFotoPengguna(dummyUser);
            String linkFoto = (urlFotoDB == null || urlFotoDB.trim().isEmpty()) 
                            ? request.getContextPath() + "/component/uiux/assets/img/team/avatar.png" 
                            : urlFotoDB;
            String nama = "-"; String identitas = "-"; String infoProdi = "-"; String tahunAkademik = "-";
            int smtInt = 1;
            try { 
                if (smtStr != null && !smtStr.trim().isEmpty()) { smtInt = Integer.parseInt(smtStr); } 
                else {
                    if (person instanceof Mahasiswa) {
                        Mahasiswa mhs = (Mahasiswa) person;
                        int tahunAngkatanMhs = mhs.getTahunangkatan() != null ? mhs.getTahunangkatan() : WaktuUtil.getCalendar().get(Calendar.YEAR);
                        String semesterMulaiStr = "Ganjil";
                        try {
                            ais.database.model.Konfigurasi konf = Common.getKonfigurasi("semester_mulai", "Ganjil");
                            if (konf != null && konf.getNilai() != null) semesterMulaiStr = konf.getNilai();
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:463");}
                        Integer smt = Common.getSemester(tahunAngkatanMhs, semesterMulaiStr, mhs.getPindahKeKampusIniMasukSemester(), mhs.getSemesterMulai());
                        smtInt = smt != null ? smt : 1;
                        if (smtInt == 0) smtInt = 1;
                    }
                }
            } catch (Exception e) { smtInt = 1; }
            
            String semesterDisplay = (smtStr != null && !smtStr.trim().isEmpty()) ? String.valueOf(smtInt) : Common.getBahasaConfig("Saat Ini") + " (" + smtInt + ")";
            PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
            List<Object> dataTagihanData = new ArrayList<Object>();
            Kegiatan kegiatanAktif = person.ambilKegiatansRefresh(smtInt, jk);

            // =========================================================================================
            // VALIDASI JADWAL PEMBAYARAN & DENDA
            // =========================================================================================
            Date tanggalJadwal = WaktuUtil.getDate();
            Integer tahunAngkatanMhs = 0;
            String semesterMulaiVal = "";
            ais.database.model.Jenjang jenjang = null;
            ais.database.model.JenisSeleksi jenisSeleksi = null;
            String jenisKuliah = "";
            String nimAtauNoReg = "";
            ais.database.model.GelombangPendaftaran gelombangPendaftaran = null;

            if (person instanceof Mahasiswa) {
                Mahasiswa mhsObj = (Mahasiswa) person;
                tahunAngkatanMhs = mhsObj.getTahunangkatan() != null ? mhsObj.getTahunangkatan() : WaktuUtil.getCalendar().get(Calendar.YEAR);
                semesterMulaiVal = mhsObj.getSemesterMulai() != null ? mhsObj.getSemesterMulai() : "";
                jenjang = mhsObj.getJurusan() != null ? mhsObj.getJurusan().getJenjang() : null;
                jenisSeleksi = mhsObj.getJenisSeleksi();
                jenisKuliah = mhsObj.getProgram();
                nimAtauNoReg = mhsObj.getNim();
                try { 
                    java.lang.reflect.Method m = mhsObj.getClass().getMethod("getGelombangPendaftaran");
                    gelombangPendaftaran = (ais.database.model.GelombangPendaftaran) m.invoke(mhsObj);
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:499");}
            } else if (person instanceof BiodataCalonMahasiswa) {
                BiodataCalonMahasiswa calonObj = (BiodataCalonMahasiswa) person;
                tahunAngkatanMhs = calonObj.getTahun() != null ? calonObj.getTahun() : WaktuUtil.getCalendar().get(Calendar.YEAR);
                semesterMulaiVal = calonObj.getSemesterMulai() != null ? calonObj.getSemesterMulai() : "";
                jenjang = calonObj.getJenjang();
                jenisSeleksi = calonObj.getJenisSeleksi();
                jenisKuliah = calonObj.getProgram();
                nimAtauNoReg = calonObj.getNoRegistrasi() != null ? calonObj.getNoRegistrasi() : (calonObj.getKode() != null ? calonObj.getKode() : "-");
                gelombangPendaftaran = calonObj.getGelombangPendaftaran();
            }

            Integer semesterMulaiJadwal = 0;
            Integer tahunAkademikMulai = Common.getTahunAkademik(smtInt, tahunAngkatanMhs, semesterMulaiJadwal, semesterMulaiVal);
            String tahunAkademikJadwal = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
            java.io.Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
                    tanggalJadwal, jk, jenjang, tahunAkademikJadwal,
                    "Ganjil".equalsIgnoreCase(semesterMulaiVal) || ais.database.model.Perkuliahan.GANJIL.equalsIgnoreCase(semesterMulaiVal),
                    jenisSeleksi, jenisKuliah, nimAtauNoReg, gelombangPendaftaran);
                    
            JadwalPembayaran jadwalPembayaran = null;
            if (serializables != null && serializables.length > 0) {
                jadwalPembayaran = (JadwalPembayaran) serializables[0];
            }

            if (jadwalPembayaran == null) {
                if (kegiatanAktif != null && kegiatanAktif.getJadwalPembayaran() != null) {
                    jadwalPembayaran = kegiatanAktif.getJadwalPembayaran();
                } else {
                    out.print("<script>if(typeof tampilkanToast === 'function') tampilkanToast('"+Common.getBahasaConfigJS("Jadwal pembayaran belum ada, sudah terlewat, atau belum mulai")+"', 'bg-danger text-white');</script>");
                    out.print("<div class='alert alert-danger shadow-sm rounded-4 border-0 m-4'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfigJS("Jadwal pembayaran belum ada, sudah terlewat, atau belum mulai") + "</div>");
                    return;
                }
            }

            // =========================================================================================

            Collection<DetailKegiatan> detailKegiatans = kegiatanAktif != null ? kegiatanAktif.ambilDetailKegiatan(refresh) : new ArrayList<DetailKegiatan>();
            Collection<CicilanPembayaran> cicilanPembayarans = person.ambilCicilanPembayaran(kegiatanAktif, person.ambilCicilan());

            if (person instanceof Mahasiswa) {
                Mahasiswa mhsObj = (Mahasiswa) person;
                nama = mhsObj.getNama() != null ? mhsObj.getNama() : "-";
                identitas = mhsObj.getNim() != null ? mhsObj.getNim() : "-";
                tahunAkademik = mhsObj.getTahunangkatan() != null ? String.valueOf(mhsObj.getTahunangkatan()) : "-";
                String prog = mhsObj.getProgram() != null ? mhsObj.getProgram() : "-";
                String jurName = (mhsObj.getJurusan() != null && mhsObj.getJurusan().getNama() != null) ? mhsObj.getJurusan().getNama() : "-";
                infoProdi = prog + " - " + jurName;
                
                Collection detailBiayas = pembayaranUtil.getDetailBiayaMahasiswa(mhsObj, smtInt, jk, refresh);
                int countPengaturanBulanan = pembayaranUtil.countBulanan(sess, mhsObj, jk, smtInt, detailBiayas, refresh, false);
                Collection biayaBulanan = null;
                if (countPengaturanBulanan > 0) biayaBulanan = pembayaranUtil.getDetailBiayaMahasiswa(mhsObj, smtInt, jk, "-1", true, refresh);
                Collection ooo = (biayaBulanan != null ? biayaBulanan : detailBiayas);
                dataTagihanData = new ArrayList<Object>(ooo);
                try { Collections.sort((List)dataTagihanData); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:554");}
            } 
            else if (person instanceof BiodataCalonMahasiswa) {
                BiodataCalonMahasiswa calon = (BiodataCalonMahasiswa) person;
                nama = calon.getNama() != null ? calon.getNama() : "-";
                identitas = nimAtauNoReg;
                tahunAkademik = calon.getTahunAkademik() != null ? String.valueOf(calon.getTahunAkademik()) : "-";
                String prog = calon.getProgram() != null ? calon.getProgram() : "-";
                String jurName = (calon.ambilJurusan() != null && calon.ambilJurusan().getNama() != null) ? calon.ambilJurusan().getNama() : "-";
                String gel = (calon.getGelombangPendaftaran() != null && calon.getGelombangPendaftaran().getNama() != null) ? calon.getGelombangPendaftaran().getNama() : "-";
                infoProdi = prog + " - " + jurName + " (" + Common.getBahasaConfig("Gelombang") + " " + gel + ")";
                
                Jurusan targetJurusan = calon.getProdiLulus() != null ? calon.getProdiLulus() : (calon.getProdi1() != null ? calon.getProdi1() : calon.getProdi2());
                List detailBiayas = new ArrayList();
                if (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && jk.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
                    detailBiayas.addAll(pembayaranUtil.getDetailBiayaCalonMahasiswa(calon, jk, targetJurusan, smtInt, refresh));
                } else if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && jk.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
                    detailBiayas.addAll(pembayaranUtil.getDetailBiayaCalonMahasiswa(calon, jk, targetJurusan, refresh));
                }

                int countPengaturanBulanan = pembayaranUtil.countBulanan(sess, calon, jk, smtInt, detailBiayas, refresh, false);
                Collection biayaBulanan = null;
                if (countPengaturanBulanan > 0) biayaBulanan = pembayaranUtil.getPengaturanPembayaranSemua(calon, sess, smtInt, jk, detailBiayas, refresh, false);
                Collection ooo = (biayaBulanan != null ? biayaBulanan : detailBiayas);
                dataTagihanData = new ArrayList<Object>(ooo);
                try { Collections.sort((List)dataTagihanData); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:579");}
            }

            StringBuilder sbItemBiaya = new StringBuilder();
            for (Object oo : dataTagihanData) {
                DetailBiaya tDetailBiaya = null;
                PengaturanPembayaranBulanan tPengaturan = null;
                if (oo instanceof DetailBiaya) tDetailBiaya = (DetailBiaya) oo;
                else if (oo instanceof PengaturanPembayaranBulanan) { tPengaturan = (PengaturanPembayaranBulanan) oo; if (tPengaturan != null) tDetailBiaya = tPengaturan.getDetailBiaya(); }
                
                String valOpt = "";
                String descOpt = "";
                if (tPengaturan != null) {
                    valOpt = "PB_" + tPengaturan.getId();
                    Double jml = tPengaturan.getNominal();
                    descOpt = tPengaturan.getKeterangan();
                    descOpt = (descOpt == null || descOpt.isEmpty() ? (tPengaturan.getDetailBiaya().getItemBiaya().getNama()) : descOpt) + ", " + tPengaturan.getNamaBulan() + ", Rp " + Common.numberFormat.get().format(jml);
                } else if (tDetailBiaya != null) {
                    valOpt = "DB_" + tDetailBiaya.getId();
                    descOpt = tDetailBiaya.getKeterangan();
                    descOpt = (descOpt == null || descOpt.isEmpty() ? (tDetailBiaya.getItemBiaya().getNama()) : descOpt) + ", Rp " + Common.numberFormat.get().format(tDetailBiaya.getNilaiBiaya());
                }
                if (!valOpt.isEmpty()) sbItemBiaya.append("<option value='").append(valOpt).append("'>").append(descOpt).append("</option>");
            }

            List<JenisPembayaran> listCaraBayar = ConstantValues.simpleList(sess.createCriteria(JenisPembayaran.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), JenisPembayaran.class);
            StringBuilder sbCaraBayar = new StringBuilder();
            for (JenisPembayaran jp : listCaraBayar) { sbCaraBayar.append("<option value='").append(jp.getId()).append("'>").append(jp.getNama()).append("</option>"); }

            List<Map<String, Object>> rincianTagihan = new ArrayList<Map<String, Object>>();
            Map<String, Double> nilais = new HashMap<String, Double>();
            for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
                if (cicilanPembayaran != null && cicilanPembayaran.getId() != null && cicilanPembayaran.getItemBiaya() != null) {
                    String key = cicilanPembayaran.getItemBiaya().getId() + "_" + cicilanPembayaran.getBayarKe();
                    if (nilais.containsKey(key)) nilais.put(key, nilais.get(key) + cicilanPembayaran.getNilai());
                    else nilais.put(key, cicilanPembayaran.getNilai());
                }
            }

            Double totalSisaKeseluruhan = 0.0;
            for (Object obj : dataTagihanData) {
                try {
                    String namaItem = "";
                    String idUnik = ""; boolean editNominal = false;
                    double tagihan = 0.0; double potongan = 0.0; double terbayar = 0.0;
                    double denda = 0.0; double sisa = 0.0;
                    
                    // PENENTUAN JADWAL SPESIFIK (JDW)
                    JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null && jadwalPembayaran.getKhususUntukNim().contains("," + nimAtauNoReg + ",") ? jadwalPembayaran : null;

                    if (obj instanceof DetailBiaya) {
                        DetailBiaya db = (DetailBiaya) obj;
                        idUnik = "DB_" + db.getId(); ItemBiaya ib = db.getItemBiaya();
                        DetailKegiatan dk = null;
                        if (detailKegiatans != null) {
                            for (DetailKegiatan d : detailKegiatans) { if (d.getDetailBiaya() != null && d.getDetailBiaya().getId().equals(db.getId()) && d.getPengaturanPembayaranBulanan() == null) { dk = d; break; } }
                        }
                        if (ib != null) {
                            namaItem = ib.getNama();
                            if (!ib.getKode().isEmpty()) namaItem = ib.getKode()+ " " + namaItem;
                            if (dk != null && dk.getUraian() != null) namaItem += " " + dk.getUraian();
                            Double jml = Kegiatan.ambilJumlahTagihan(dk, kegiatanAktif, db, refresh); 
                            if(jml == null) jml = 0.0;
                            // LOGIKA KALKULASI DENDA SESUAI RENDERER
                            Double hasilDenda = dk != null && dk.getMenggunakanDendaCustom() ? jml
                                    : dk != null && (dk.getBatalkanDenda() || jml.intValue() == 0) ? jml
                                    : db.checkDenda(jml, WaktuUtil.getDate(), jdw, jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan(), null);
                            if (dk != null && dk.getMenggunakanDendaCustom()) {
                                db.setInfoDenda(" Penambahan denda senilai " + Common.numberFormat.get().format(dk.getDendaCustom() != null ? dk.getDendaCustom() : 0.0) + ".");
                            }

                            Double nilaiDenda = hasilDenda - jml;
                            if (dk != null && !dk.getMenggunakanDendaCustom()) {
                                dk.setDendaCustom(nilaiDenda);
                                denda = nilaiDenda;
                            } else if (dk != null && dk.getMenggunakanDendaCustom()) {
                                denda = dk.getDendaCustom() != null ? dk.getDendaCustom() : 0.0;
                            }
                            
                            tagihan = jml + denda;
                            potongan = dk != null && dk.getDiskon() != null ? dk.getDiskon() : 0.0;
                            String key = ib.getId() + "_" + db.getBayarKe(); Double bayar = nilais.get(key);
                            terbayar = bayar != null ? bayar : 0.0;
                            
                            /* OVERRIDE Tagihan Default: item dari SettingBiaya "Gunakan Nilai Tagihan
                               Default" (Tagihan Default = Ya) WAJIB dibayar penuh - bukan angsuran,
                               nominal tidak boleh diubah, apa pun flag mencicil pada ItemBiaya.
                               Aturan terpusat di PembayaranUtil.bolehDiangsur (dipakai juga wizard ZK). */
                            editNominal = ais.action.ws.util.PembayaranUtil.bolehDiangsur(db, !isUserMhs);
                        }
                    } else if (obj instanceof PengaturanPembayaranBulanan) {
                        PengaturanPembayaranBulanan pb = (PengaturanPembayaranBulanan) obj;
                        idUnik = "PB_" + pb.getId(); DetailBiaya db = pb.getDetailBiaya(); ItemBiaya ib = db != null ? db.getItemBiaya() : null;
                        DetailKegiatan dk = null;
                        if (detailKegiatans != null) {
                            for (DetailKegiatan d : detailKegiatans) { if (d.getPengaturanPembayaranBulanan() != null && d.getPengaturanPembayaranBulanan().getId().equals(pb.getId())) { dk = d; break; } }
                        }
                        if (ib != null) {
                            namaItem = ib.getNama();
                            if (pb.getKeterangan() != null && !pb.getKeterangan().isEmpty()) namaItem += " - " + pb.getKeterangan();
                            else if (pb.getNamaBulan() != null) namaItem += " - " + pb.getNamaBulan();
                            if (dk != null && dk.getUraian() != null) namaItem += " " + dk.getUraian();
                            Double jml = Kegiatan.ambilJumlahTagihan(dk, db, kegiatanAktif, (person instanceof Mahasiswa ? (Mahasiswa)person : null), smtInt, pb);
                            if(jml == null) jml = 0.0;

                            // LOGIKA KALKULASI DENDA BULANAN SESUAI RENDERER
                            Double hasilDenda = dk != null && (dk.getBatalkanDenda() || jml.intValue() == 0) ? jml
                                    : dk != null && dk.getMenggunakanDendaCustom() ? jml
                                    : pb.checkDenda(jml, WaktuUtil.getDate(), jdw, jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan());
                            if (dk != null && dk.getMenggunakanDendaCustom()) {
                                pb.setInfoDenda(" Penambahan denda senilai " + Common.numberFormat.get().format(dk.getDendaCustom() != null ? dk.getDendaCustom() : 0.0) + ".");
                            }

                            Double nilaiDenda = hasilDenda - jml;
                            if (dk != null && !dk.getMenggunakanDendaCustom()) {
                                dk.setDendaCustom(nilaiDenda);
                                denda = nilaiDenda;
                            } else if (dk != null && dk.getMenggunakanDendaCustom()) {
                                denda = dk.getDendaCustom() != null ? dk.getDendaCustom() : 0.0;
                            }
                            
                            tagihan = jml + denda;
                            potongan = dk != null && dk.getDiskon() != null ? dk.getDiskon() : 0.0;
                            Number sumCicilan = VOMahasiswa.hitungTotalCicilan(kegiatanAktif, pb, cicilanPembayarans);
                            terbayar = sumCicilan != null ? sumCicilan.doubleValue() : 0.0;
                            
                            /* OVERRIDE Tagihan Default: item dari SettingBiaya "Gunakan Nilai Tagihan
                               Default" (Tagihan Default = Ya) WAJIB dibayar penuh - bukan angsuran,
                               nominal tidak boleh diubah, apa pun flag mencicil pada ItemBiaya.
                               Aturan terpusat di PembayaranUtil.bolehDiangsur (dipakai juga wizard ZK). */
                            editNominal = ais.action.ws.util.PembayaranUtil.bolehDiangsur(db, !isUserMhs);
                        }
                    }
                    
                    sisa = tagihan - potongan - terbayar;
                    totalSisaKeseluruhan += sisa;
                    
                    if (!namaItem.isEmpty() && sisa > 0) {
                        Map<String, Object> mapTagihan = new HashMap<String, Object>();
                        // TAMPILKAN NOMINAL DENDA DI TABEL
                        if (denda > 0.0) {
                            namaItem += " <span class='badge bg-danger ms-2 px-2 py-1 shadow-sm' style='font-size: 0.7rem; vertical-align: text-bottom;'><i class='fas fa-exclamation-circle me-1'></i>" + Common.getBahasaConfig("Denda: Rp") + " " + Common.numberFormat.get().format(denda) + "</span>";
                        }
                        
                        mapTagihan.put("idUnik", idUnik);
                        mapTagihan.put("editNominal", editNominal);
                        mapTagihan.put("namaItem", namaItem);
                        mapTagihan.put("tagihan", tagihan); mapTagihan.put("potongan", potongan); mapTagihan.put("terbayar", terbayar); mapTagihan.put("sisa", sisa);
                        rincianTagihan.add(mapTagihan);
                    }
                } catch (Exception ex) { ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:729"); }
            }
            

            Double telahDibayarKeseluruhan = 0.0;
            if (cicilanPembayarans != null) {
                for (CicilanPembayaran cp : cicilanPembayarans) {
                    if (cp.getId() != null && cp.getNilai() != null) {
                        telahDibayarKeseluruhan += cp.getNilai();
                    }
                }
            }

            try {
                if (kegiatanAktif != null && kegiatanAktif.getId() != null && kegiatanAktif.getJenisKegiatan() != null) {
                    Double amountDbs = kegiatanAktif.getAmount() != null ? kegiatanAktif.getAmount() : 0.0;
                    Double amountTerhutangDbs = kegiatanAktif.getAmountTerhutang() != null ? kegiatanAktif.getAmountTerhutang() : 0.0;
                    if (amountDbs.intValue() != telahDibayarKeseluruhan.intValue() || amountTerhutangDbs.intValue() != totalSisaKeseluruhan.intValue()) {
                        kegiatanAktif.setTanggal(WaktuUtil.getDate());
                        kegiatanAktif.setAmount(telahDibayarKeseluruhan);
                        kegiatanAktif.setAmountTerhutang(totalSisaKeseluruhan);
                        executeNativeUpdateTransaction((GeneralValueObject) kegiatanAktif);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:754");
            }
            %>

            <style>
                .card-header-<%=rnd%> { border-bottom: 3px solid rgba(0,0,0,0.05); }
                .tbl-kewajiban-<%=rnd%> th { font-size: 0.8rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
                .tbl-kewajiban-<%=rnd%> td { font-size: 0.85rem; vertical-align: middle; }
                .hover-elevate-<%=rnd%> { transition: transform 0.2s ease, box-shadow 0.2s ease; }
                .hover-elevate-<%=rnd%>:hover { transform: translateY(-2px); box-shadow: 0 0.25rem 0.5rem rgba(0,0,0,.05); }
                .btn-aksi-<%=rnd%> { width: 32px; height: 32px; display: inline-flex; align-items: center; justify-content: center; }
                @media (max-width: 575.98px) {
                    .info-prodi-text-<%=rnd%> { font-size: 0.9rem !important; }
                    .lbl-total-<%=rnd%> { font-size: 0.8rem !important; }
                }
            </style>

            <div class="row g-4 animate__animated animate__fadeInUp">
                
                <div class="col-lg-7">
                    <div class="card border-0 shadow-sm rounded-4 h-100">
                        <div class="card-header bg-primary text-white p-3 card-header-<%=rnd%> d-flex justify-content-between align-items-center">
                            <div class="d-flex align-items-center gap-3">
                                <div class="bg-white bg-opacity-25 rounded-circle d-flex align-items-center justify-content-center" style="width: 45px; height: 45px;">
                                    <i class="fas fa-file-invoice-dollar fs-4"></i>
                                </div>
                                <div>
                                    <h6 class="mb-0 fw-bold"><%= Common.getBahasaConfig("Rincian Kewajiban Pembayaran") %></h6>
                                    <span class="badge bg-light text-primary border rounded-pill mt-1" style="font-size: 0.7rem;">
                                        <i class="fas fa-tag me-1"></i><%= jk.getNama() %>
                                    </span>
                                </div>
                            </div>
                            <div class="text-end d-none d-sm-flex align-items-center gap-3">
                                <div class="text-end">
                                    <h6 class="mb-0 fw-bold fs-5"><%= identitas %></h6>
                                    <span class="text-white-50 small"><i class="fas fa-user-circle me-1"></i><%= nama %></span>
                                </div>
                                <img src="<%=linkFoto%>" alt="<%=Common.getBahasaConfig("Foto Pelanggan")%>" class="rounded-circle border border-2 border-white shadow-sm" style="width: 50px; height: 50px; object-fit: cover;">
                            </div>
                        </div>

                        <div class="card-body p-3 bg-light">
                            <div class="d-block d-sm-none text-center bg-white p-3 rounded-3 mb-3 shadow-sm border border-light">
                                <img src="<%=linkFoto%>" alt="<%=Common.getBahasaConfig("Foto Pelanggan")%>" class="rounded-circle border border-3 border-light shadow-sm mb-2" style="width: 70px; height: 70px; object-fit: cover;">
                                <h6 class="fw-bold mb-0 text-dark"><%= identitas %></h6>
                                <small class="text-secondary"><%= nama %></small>
                            </div>

                            <div class="bg-white p-3 rounded-3 shadow-sm mb-3 border-start border-4 border-info d-flex align-items-center hover-elevate-<%=rnd%>">
                                <div class="bg-info bg-opacity-10 rounded-circle text-info me-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width: 45px; height: 45px;">
                                    <i class="fas fa-university fs-5"></i>
                                </div>
                                <div>
                                    <small class="text-secondary fw-bold" style="font-size: 0.65rem; letter-spacing: 0.5px;"><%= Common.getBahasaConfig("PROGRAM STUDI & INFORMASI") %></small>
                                    <div class="fw-bold text-dark info-prodi-text-<%=rnd%> mt-1 text-capitalize d-flex flex-wrap align-items-center gap-2">
                                        <%= infoProdi %>
                                        <span class="badge bg-primary"><i class="fas fa-layer-group me-1"></i><%= Common.getBahasaConfig("Semester") %> <%= semesterDisplay %></span>
                                    </div>
                                </div>
                            </div>

                            <div class="table-responsive rounded-3 border border-light bg-white shadow-sm">
                                <table class="table table-hover mb-0 tbl-kewajiban-<%=rnd%>" style="width: 100%;">
                                    <thead class="table-dark">
                                        <tr>
                                            <th class="text-center" style="width: 40px; min-width: 40px;">
                                                <input class="form-check-input" type="checkbox" id="chkAllBayar<%=rnd%>" onclick="window.toggleSemuaBayar<%=rnd%>(this)">
                                            </th>
                                            <th><%= Common.getBahasaConfig("Deskripsi Item Biaya") %></th>
                                            <th class="text-end d-none d-md-table-cell"><%= Common.getBahasaConfig("Tagihan (Rp)") %></th>
                                            <th class="text-end d-none d-lg-table-cell"><%= Common.getBahasaConfig("Potongan/Terbayar") %></th>
                                            <th class="text-end text-warning" style="min-width: 130px;"><%= Common.getBahasaConfig("Nominal Dibayar") %></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <% if(rincianTagihan.isEmpty()) { %>
                                        <tr>
                                            <td colspan="5" class="text-center py-4 text-muted">
                                                <i class="fas fa-check-circle fa-2x text-success opacity-50 mb-2 d-block"></i>
                                                <span class="fw-bold"><%= Common.getBahasaConfig("Tidak Ada Kewajiban Pembayaran Aktif") %></span>
                                                <div class="small mt-1"><%= Common.getBahasaConfig("Pelanggan ini tidak memiliki sisa kewajiban yang harus dilunasi pada periode ini.") %></div>
                                            </td>
                                        </tr>
                                        <% } else {
                                            for(Map<String, Object> map : rincianTagihan) { 
                                                String uid = (String) map.get("idUnik");
                                                double sisaVal = (Double) map.get("sisa");
                                                boolean isEdit = (Boolean) map.get("editNominal");
                                                String jsSafeNamaItem = map.get("namaItem").toString().replace("'", "\\'").replace("\"", "&quot;");
                                        %>
                                        <tr>
                                            <td class="text-center">
                                                <input class="form-check-input chk-item-bayar<%=rnd%>" type="checkbox" value="<%=uid%>" onchange="window.onCheckboxChange<%=rnd%>('<%=uid%>', this)">
                                            </td>
                                            <td><div class="fw-bold text-dark"><%= map.get("namaItem") %></div></td>
                                            <td class="text-end text-secondary fw-semibold d-none d-md-table-cell"><%= String.format("%,.0f", map.get("tagihan")) %></td>
                                            <td class="text-end d-none d-lg-table-cell">
                                                <div class="text-success" style="font-size: 0.75rem;">- <%= String.format("%,.0f", map.get("potongan")) %></div>
                                                <div class="text-info" style="font-size: 0.75rem;">- <%= String.format("%,.0f", map.get("terbayar")) %></div>
                                            </td>
                                            <td class="text-end px-2">
                                                <div class="d-flex align-items-center justify-content-end gap-2">
                                                    <span class="fw-bold text-danger" id="lbl_nom_<%=uid%>"><%= String.format("%,.0f", sisaVal) %></span>
                                                    <input type="hidden" id="val_nom_<%=uid%>" value="<%= sisaVal %>" data-max="<%= sisaVal %>">
                                                    <% if (isEdit) { %>
                                                    <button type="button" id="btn_edit_<%=uid%>" style="display: none;" class="btn btn-sm btn-light text-primary rounded-circle border btn-aksi-<%=rnd%>" onclick="window.bukaPopupCicilan<%=rnd%>('<%=uid%>', '<%=jsSafeNamaItem%>')" title="<%=Common.getBahasaConfig("Ubah Nominal Pembayaran")%>">
                                                        <i class="fas fa-pencil-alt" style="font-size: 0.7rem;"></i>
                                                    </button>
                                                    <% } %>
                                                </div>
                                            </td>
                                        </tr>
                                        <% } } %>
                                    </tbody>
                                    <% if(!rincianTagihan.isEmpty()) { %>
                                    <tfoot class="bg-light">
                                        <tr>
                                            <td colspan="5" class="text-end p-3">
                                                <span class="fw-bold text-dark text-uppercase me-2" style="font-size: 0.8rem;"><%= Common.getBahasaConfig("Total Rencana Pembayaran :") %></span>
                                                <span class="fw-bold fs-7 text-danger m-0" id="grandTotalDisplay<%=rnd%>">Rp 0</span>
                                            </td>
                                        </tr>
                                    </tfoot>
                                    <% } %>
                                </table>
                            </div>
                        </div>

                        <div class="card-footer bg-white border-top p-3">
                            <div class="d-flex flex-column flex-sm-row justify-content-between align-items-center gap-3">
                                <small class="text-muted text-center text-sm-start"><i class="fas fa-shield-alt text-success me-1"></i> <%= Common.getBahasaConfig("Sistem Terenkripsi & Aman.") %></small>
                                <div class="d-flex gap-2 w-100 w-sm-auto">
                                    <button class="btn btn-info text-white fw-bold btn-sm px-3 flex-grow-1 flex-sm-grow-0 shadow-sm" onclick="window.loadTagihanMhs<%=rnd%>(true)">
                                        <i class="fas fa-sync-alt me-1"></i> <%= Common.getBahasaConfig("Segarkan") %>
                                    </button>
                                    <button class="btn btn-success fw-bold btn-sm px-4 flex-grow-1 flex-sm-grow-0 shadow-sm" onclick="window.prosesLanjutBayar<%=rnd%>()" <%= rincianTagihan.isEmpty() ? "disabled" : "" %>>
                                        <i class="fas fa-credit-card me-1"></i> <%= Common.getBahasaConfig("Lanjutkan Pembayaran") %>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col-lg-5">
                    <div class="card border-0 shadow-sm rounded-4 h-100">
                        <div class="card-header bg-success text-white p-3 card-header-<%=rnd%> d-flex justify-content-between align-items-center">
                            <div class="d-flex align-items-center gap-3">
                                <div class="bg-white bg-opacity-25 rounded-circle d-flex align-items-center justify-content-center" style="width: 45px; height: 45px;">
                                    <i class="fas fa-history fs-4"></i>
                                </div>
                                <div>
                                    <h6 class="mb-0 fw-bold"><%= Common.getBahasaConfig("Riwayat Pembayaran") %></h6>
                                    <span class="badge bg-light text-success border rounded-pill mt-1" style="font-size: 0.7rem;">
                                        <i class="fas fa-check-circle me-1"></i><%= Common.getBahasaConfig("Transaksi Lunas") %>
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div class="card-body p-0 bg-white">
                            <div class="table-responsive rounded-bottom">
                                <table class="table table-hover mb-0 tbl-kewajiban-<%=rnd%>" style="width: 100%;">
                                    <thead class="table-light text-secondary border-bottom border-success">
                                        <tr>
                                            <th class="text-center" style="width: 45px;"><%= Common.getBahasaConfig("Thp") %></th>
                                            <th><%= Common.getBahasaConfig("Keterangan & Waktu") %></th>
                                            <th class="text-end px-3"><%= Common.getBahasaConfig("Nilai Dibayar") %></th>
                                            <% if (editPriv || deletePriv) { %><th class="text-center" style="width: 80px;"><%= Common.getBahasaConfig("Aksi") %></th><% } %>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <% 
                                        double totalRiwayatDibayar = 0.0;
                                        boolean adaRiwayat = false;
                                        if (cicilanPembayarans != null && !cicilanPembayarans.isEmpty()) {
                                            for (CicilanPembayaran cp : cicilanPembayarans) {
                                                if (cp.getId() != null) {
                                                    adaRiwayat = true;
                                                    String descRiwayat = ""; String dataValItem = "";
                                                    if (cp.getPengaturanPembayaranBulanan() != null) {
                                                        descRiwayat = cp.getPengaturanPembayaranBulanan().getDetailBiaya().getItemBiaya().getNama() + " (" + cp.getPengaturanPembayaranBulanan().getNamaBulan() + ")";
                                                        dataValItem = "PB_" + cp.getPengaturanPembayaranBulanan().getId();
                                                    } else if (cp.getItemBiaya() != null) {
                                                        descRiwayat = cp.getItemBiaya().getNama();
                                                        if (cp.getDetailBiaya() != null) dataValItem = "DB_" + cp.getDetailBiaya().getId();
                                                    } else { descRiwayat = cp.getKeterangan() != null ? cp.getKeterangan() : "-"; }

                                                    String caraBayar = cp.getJenisPembayaran() != null ? cp.getJenisPembayaran().getNama() : "Tunai";
                                                    String idCaraBayar = cp.getJenisPembayaran() != null ? String.valueOf(cp.getJenisPembayaran().getId()) : "";
                                                    
                                                    String tglBayarStr = cp.getTanggal() != null ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(cp.getTanggal()) : new java.text.SimpleDateFormat("yyyy-MM-dd").format(WaktuUtil.getDate());
                                                    String tglBayarTampil = cp.getTanggal() != null ? Common.dateFormat41.get().format(cp.getTanggal()) : "-";
                                                    
                                                    String tglTagihanStr = "";
                                                    try {
                                                        Date tglT = (Date) cp.getClass().getMethod("getTanggalTagihan").invoke(cp);
                                                        tglTagihanStr = tglT != null ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(tglT) : new java.text.SimpleDateFormat("yyyy-MM-dd").format(WaktuUtil.getDate());
                                                    } catch(Exception e) {
                                                        tglTagihanStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(WaktuUtil.getDate());
                                                    }

                                                    Double nilaiBayar = cp.getNilai() != null ? cp.getNilai() : 0.0;
                                                    Double dendaBayar = cp.getDenda() != null ? cp.getDenda() : 0.0;
                                                    String ketBayar = cp.getKeterangan() != null ? cp.getKeterangan().replace("\"", "&quot;") : "";
                                                    totalRiwayatDibayar += nilaiBayar;
                                           
                                                    boolean bolehUbahAtauHapus = (cp.getPostingHistory() == null && bolehMerubahCicilan && cp.getId() != null);
                                                    boolean showEdit = editPriv && bolehUbahAtauHapus;
                                                    boolean showDelete = deletePriv && bolehUbahAtauHapus;
                                        %>
                                        <tr>
                                            <td class="text-center align-middle">
                                                <div class="bg-success text-white fw-bold rounded-circle d-inline-flex align-items-center justify-content-center shadow-sm" style="width: 25px; height: 25px; font-size: 0.75rem;">
                                                    <%= cp.getKe() != null ? cp.getKe() : "-" %>
                                                </div>
                                            </td>
                                            <td>
                                                <div class="fw-bold text-dark"><%= descRiwayat %></div>
                                                <div class="text-muted d-flex flex-wrap align-items-center gap-2 mt-1" style="font-size: 0.7rem;">
                                                    <span><i class="far fa-calendar-alt me-1"></i><%= tglBayarTampil %></span>
                                                    <span class="badge bg-light text-dark border px-2 py-0"><i class="fas fa-wallet text-secondary me-1"></i><%= caraBayar %></span>
                                                </div>
                                            </td>
                                            <td class="text-end align-middle fw-bold text-success fs-8 px-3"><%= String.format("%,.0f", nilaiBayar) %></td>
                                            <% if (editPriv || deletePriv) { %>
                                            <td class="text-center align-middle">
                                                <div class="d-flex justify-content-center gap-1">
                                                    <% if (showEdit) { %>
                                                    <button type="button" class="btn btn-outline-primary btn-aksi-<%=rnd%> rounded-circle" data-id="<%=cp.getId()%>" data-tgl="<%=tglBayarStr%>" data-tgltagihan="<%=tglTagihanStr%>" data-nilai="<%=nilaiBayar%>" data-denda="<%=dendaBayar%>" data-cara="<%=idCaraBayar%>" data-item="<%=dataValItem%>" data-ket="<%=ketBayar%>" onclick="window.editRiwayat<%=rnd%>(this)" title="<%= Common.getBahasaConfig("Ubah Riwayat") %>"><i class="fas fa-pencil-alt" style="font-size: 0.75rem;"></i></button>
                                                    <% } %>
                                                    <% if (showDelete) { %>
                                                    <button type="button" class="btn btn-outline-danger btn-aksi-<%=rnd%> rounded-circle" onclick="window.hapusRiwayat<%=rnd%>('<%=cp.getId()%>')" title="<%= Common.getBahasaConfig("Hapus Riwayat") %>"><i class="fas fa-trash-alt" style="font-size: 0.75rem;"></i></button>
                                                    <% } %>
                                                </div>
                                            </td>
                                            <% } %>
                                        </tr>
                                        <% } } } 
                                        if (!adaRiwayat) { int colspanValue = (editPriv || deletePriv) ? 4 : 3; %>
                                        <tr>
                                            <td colspan="<%=colspanValue%>" class="text-center py-5 text-muted">
                                                <i class="fas fa-folder-open fa-2x text-secondary opacity-25 mb-2 d-block"></i>
                                                <span class="fw-bold d-block"><%= Common.getBahasaConfig("Tidak Ada Riwayat Pembayaran") %></span>
                                                <small class="d-block mt-1"><%= Common.getBahasaConfig("Belum ada data pembayaran yang tercatat.") %></small>
                                            </td>
                                        </tr>
                                        <% } %>
                                    </tbody>
                                    <% if (adaRiwayat || (rincianTagihan != null && !rincianTagihan.isEmpty())) { 
                                        int colspanFoot = (editPriv || deletePriv) ? 4 : 3;
                                    %>
                                    <tfoot class="bg-light border-top border-2">
                                        <tr>
                                            <td colspan="<%=colspanFoot%>" class="p-3">
                                                <div class="row align-items-center">
                                                    <div class="col-md-6 mb-3 mb-md-0">
                                                        <div class="d-flex flex-column align-items-start gap-2">
                                                            <% if(adaRiwayat) { %>
                                                            <button type="button" class="btn btn-sm btn-outline-success rounded-pill fw-bold px-3 shadow-sm w-100 w-md-auto text-start text-md-center" onclick="window.cetakStrukGlobal<%=rnd%>()">
                                                                <i class="fas fa-print me-1"></i><%= Common.getBahasaConfig("Cetak Struk Pembayaran") %>
                                                            </button>
                                                            <% } %>
                                                            <% if(rincianTagihan != null && !rincianTagihan.isEmpty()) { %>
                                                            <button type="button" class="btn btn-sm btn-outline-primary rounded-pill fw-bold px-3 shadow-sm w-100 w-md-auto text-start text-md-center" onclick="window.bukaModalSuratTagihan<%=rnd%>()">
                                                                <i class="fas fa-envelope-open-text me-1"></i><%= Common.getBahasaConfig("Cetak Surat Tagihan") %>
                                                            </button>
                                                            <% } %>
                                                        </div>
                                                    </div>
                                                    <div class="col-md-6 text-md-end text-start">
                                                        <div class="d-inline-flex flex-column flex-sm-row align-items-sm-center justify-content-md-end gap-2">
                                                            <span class="fw-bold text-dark text-uppercase" style="font-size: 0.75rem; letter-spacing: 0.5px;"><%= Common.getBahasaConfig("Total Keseluruhan :") %></span>
                                                            <span class="fw-bolder text-success" style="font-size: 1rem; line-height: 1;">Rp <%= String.format("%,.0f", totalRiwayatDibayar) %></span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                    </tfoot>
                                    <% } %>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>

            </div>

            <div id="data-select-edit-<%=rnd%>" style="display:none;">
                <select id="optCaraBayar_<%=rnd%>"><%=sbCaraBayar.toString()%></select>
                <select id="optItemBiaya_<%=rnd%>"><%=sbItemBiaya.toString()%></select>
            </div>

            <div id="htmlModalSuratTagihan<%=rnd%>" style="display:none;">
                <form id="formSuratTagihan<%=rnd%>">
                    <div class="mb-3">
                        <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Surat")%></label>
                        <input type="date" class="form-control rounded-3" name="tanggalSurat" value="<%=new java.text.SimpleDateFormat("yyyy-MM-dd").format(WaktuUtil.getDate())%>" required>
                    </div>
                    <% 
                        Calendar cal = WaktuUtil.getCalendar();
                        cal.add(Calendar.MONTH, 1);
                        String defaultJatuhTempo = new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
                    %>
                    <div class="mb-3">
                        <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Jatuh Tempo")%></label>
                        <input type="date" class="form-control rounded-3" name="tanggalJatuhTempo" value="<%=defaultJatuhTempo%>" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Nomor Surat")%></label>
                        <input type="text" class="form-control rounded-3" name="nomorSurat" placeholder="<%=Common.getBahasaConfig("Otomatis/Sesuai format")%>">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Cara Pembayaran")%></label>
                        <select class="form-select rounded-3" name="caraBayar">
                            <option value=""><%=Common.getBahasaConfig("== Tidak menggunakan cara pembayaran ==")%></option>
                            <%=sbCaraBayar.toString()%>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Persentase Denda")%></label>
                        <input type="number" class="form-control rounded-3" name="denda" value="0.0" min="0" step="any">
                    </div>
                    <div class="mb-0">
                        <label class="form-label fw-bold text-secondary mb-2"><%=Common.getBahasaConfig("Pilih Tagihan yang Akan Dicetak")%></label>
                        <div class="p-3 border rounded-3 bg-white" style="max-height: 180px; overflow-y: auto;">
                            <% for(Map<String, Object> map : rincianTagihan) { 
                                String labelSafe = map.get("namaItem").toString().replaceAll("<[^>]*>", "").replace("\"", "&quot;");
                            %>
                                <div class="form-check mb-2">
                                    <input class="form-check-input" type="checkbox" name="tagihanTerpilih" value="<%=labelSafe%>|<%=map.get("sisa")%>" checked>
                                    <label class="form-check-label fw-semibold text-dark"><%=labelSafe%> <span class="text-danger">(Rp <%=String.format("%,.0f", map.get("sisa"))%>)</span></label>
                                </div>
                            <% } %>
                        </div>
                    </div>
                </form>
            </div>

            <script>
                window.formatRupiah<%=rnd%> = function(angka) { return new Intl.NumberFormat('id-ID').format(angka); };

                window.kalkulasiTotalBayar<%=rnd%> = function() {
                    const checkboxes = document.querySelectorAll('.chk-item-bayar<%=rnd%>');
                    let total = 0;
                    checkboxes.forEach(function(chk) { if (chk.checked) { const valInput = document.getElementById('val_nom_' + chk.value); if (valInput) total += parseFloat(valInput.value || 0); } });
                    const checkAllBtn = document.getElementById('chkAllBayar<%=rnd%>');
                    if (checkAllBtn) checkAllBtn.checked = document.querySelectorAll('.chk-item-bayar<%=rnd%>:checked').length === checkboxes.length && checkboxes.length > 0;
                    document.getElementById('grandTotalDisplay<%=rnd%>').innerHTML = "Rp " + window.formatRupiah<%=rnd%>(total);
                };

                window.onCheckboxChange<%=rnd%> = function(uid, chkEl) {
                    const btnEdit = document.getElementById('btn_edit_' + uid);
                    if (btnEdit) btnEdit.style.display = chkEl.checked ? 'inline-block' : 'none';
                    window.kalkulasiTotalBayar<%=rnd%>();
                };
                
                window.toggleSemuaBayar<%=rnd%> = function(source) {
                    document.querySelectorAll('.chk-item-bayar<%=rnd%>').forEach(function(chk) {
                        chk.checked = source.checked; const uid = chk.value; const btnEdit = document.getElementById('btn_edit_' + uid);
                        if (btnEdit) btnEdit.style.display = chk.checked ? 'inline-block' : 'none';
                    });
                    window.kalkulasiTotalBayar<%=rnd%>();
                };

                // --- MODAL UBAH NOMINAL PEMBAYARAN (CHECKOUT) ---
                window.bukaPopupCicilan<%=rnd%> = function(uid, namaItem) {
                    const inputEl = document.getElementById('val_nom_' + uid);
                    const currentVal = parseFloat(inputEl.value);
                    const maxVal = parseFloat(inputEl.getAttribute('data-max'));
                    const modalId = 'modalEditNominal<%=rnd%>';
                    
                    const existingModal = document.getElementById(modalId);
                    if (existingModal) { existingModal.remove(); document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); }

                    const modalHtml = '<div class="modal fade animate__animated animate__fadeIn" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                                      '  <div class="modal-dialog modal-dialog-centered">' +
                                      '    <div class="modal-content border-0 shadow-lg rounded-4">' +
                                      '      <form id="formEditNominal' + '<%=rnd%>' + '">' +
                                      '          <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                                      '            <h5 class="modal-title fw-bold"><i class="fas fa-edit me-2"></i><%=Common.getBahasaConfig("Ubah Nominal Pembayaran")%></h5>' +
                                      '            <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                                      '          </div>' +
                                      '          <div class="modal-body p-4 bg-light">' +
                                      '            <div class="mb-3">' +
                                      '                <label class="form-label fw-bold text-secondary small"><%=Common.getBahasaConfig("Deskripsi Item Biaya")%></label>' +
                                      '                <div class="p-3 bg-white border rounded-3 fw-bold text-dark">' + namaItem + '</div>' +
                                      '            </div>' +
                                      '            <div class="mb-3">' +
                                      '                <label class="form-label fw-bold text-secondary small"><%=Common.getBahasaConfig("Masukkan Nominal Baru (Rp)")%></label>' +
                                      '                <input type="number" class="form-control form-control-lg rounded-3 fw-bold text-primary" id="inputNominalModal' + '<%=rnd%>' + '" value="' + currentVal + '" min="1" max="' + maxVal + '" step="any" required>' +
                                      '                <div class="form-text text-danger mt-2"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Maksimal Sisa Tagihan:")%> Rp ' + window.formatRupiah<%=rnd%>(maxVal) + '</div>' +
                                      '            </div>' +
                                      '          </div>' +
                                      '          <div class="modal-footer bg-white border-top p-3 d-flex justify-content-between">' +
                                      '            <button type="button" class="btn btn-light rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>' +
                                      '            <button type="submit" class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm"><i class="fas fa-check me-2"></i><%=Common.getBahasaConfig("Terapkan")%></button>' +
                                      '          </div>' +
                                      '      </form>' +
                                      '    </div>' +
                                      '  </div>' +
                                      '</div>';
                    
                    document.body.insertAdjacentHTML('beforeend', modalHtml);
                    const modalEl = document.getElementById(modalId);
                    const modalObj = new bootstrap.Modal(modalEl);
                    modalObj.show();

                    modalEl.addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
                    modalEl.addEventListener('hidden.bs.modal', function () { modalEl.remove(); });

                    document.getElementById('formEditNominal<%=rnd%>').addEventListener('submit', function(e) {
                        e.preventDefault();
                        const inputNominal = document.getElementById('inputNominalModal<%=rnd%>').value;
                        let val = parseFloat(inputNominal);

                        if (isNaN(val) || val <= 0) {
                            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Nominal yang dimasukkan tidak valid atau harus lebih dari nol.")%>', 'bg-warning text-dark');
                            return;
                        }
                        if (val > maxVal) {
                            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Nominal tidak boleh melebihi sisa kewajiban pembayaran.")%>', 'bg-warning text-dark');
                            return;
                        }

                        inputEl.value = val;
                        document.getElementById('lbl_nom_' + uid).innerText = window.formatRupiah<%=rnd%>(val);
                        
                        const chkbox = document.querySelector('.chk-item-bayar<%=rnd%>[value="'+uid+'"]');
                        if(chkbox && !chkbox.checked) {
                            chkbox.checked = true;
                            const btnEditInline = document.getElementById('btn_edit_' + uid);
                            if (btnEditInline) btnEditInline.style.display = 'inline-block';
                        }

                        window.kalkulasiTotalBayar<%=rnd%>();
                        modalObj.hide();
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Nominal berhasil diperbarui pada daftar.")%>', 'bg-success text-white');
                    });
                };
                
                window.prosesLanjutBayar<%=rnd%> = function() {
                    const checkedItems = document.querySelectorAll('.chk-item-bayar<%=rnd%>:checked');
                    if(checkedItems.length === 0) {
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Harap centang setidaknya satu rincian biaya yang akan dilunasi.")%>', 'bg-warning text-dark');
                        return;
                    }

                    let payloadArr = [];
                    checkedItems.forEach(function(chk) {
                        const idUnik = chk.value; const nominal = document.getElementById('val_nom_' + idUnik).value;
                        payloadArr.push(idUnik + "|" + nominal);
                    });
                    
                    const payloadStr = payloadArr.join(",");
                    const urlLanjut = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_lanjut_bayar&id=<%=idStr%>&jkId=<%=jkIdStr%>&isMahasiswa=<%=isMahasiswaStr%>&smt=<%=smtInt%>&payload=' + encodeURIComponent(payloadStr);
                    
                    if (typeof showConfirmModal === 'function') {
                        showConfirmModal('<%=Common.getBahasaConfigJS("Sistem akan menyimpan dan menyiapkan saluran pembayaran ini. Lanjutkan proses?")%>', function() { window.tampilkanModalCheckout<%=rnd%>(urlLanjut); });
                    } else {
                        if (confirm('<%=Common.getBahasaConfigJS("Sistem akan menyiapkan saluran pembayaran ini. Lanjutkan?")%>')) { window.tampilkanModalCheckout<%=rnd%>(urlLanjut); }
                    }
                };
                
                window.tampilkanModalCheckout<%=rnd%> = function(urlFetch) {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Memuat rincian gerbang pembayaran...")%>', 'bg-info text-white');
                    const modalId = 'modalCheckout<%=rnd%>';
                    const existingModal = document.getElementById(modalId);
                    if (existingModal) { existingModal.remove(); document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); }

                    const modalHtml = '<div class="modal fade animate__animated animate__fadeIn" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                                      '  <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                                      '    <div class="modal-content border-0 shadow-lg rounded-4">' +
                                      '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                                      '        <h5 class="modal-title fw-bold"><i class="fas fa-shopping-cart me-2"></i> <%=Common.getBahasaConfig("Penyelesaian Transaksi Pembayaran")%></h5>' +
                                      '        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                                      '      </div>' +
                                      '      <div class="modal-body p-0 bg-light" id="modalCheckoutBody' + '<%=rnd%>' + '">' +
                                      '        <div class="text-center py-5">' +
                                      '            <div class="spinner-border text-primary" style="width: 3rem; height: 3rem;" role="status"></div>' +
                                      '            <h6 class="mt-3 text-secondary fw-bold"><%=Common.getBahasaConfig("Mengambil data saluran pembayaran...")%></h6>' +
                                      '        </div>' +
                                      '      </div>' +
                                      '    </div>' +
                                      '  </div>' +
                                      '</div>';

                    document.body.insertAdjacentHTML('beforeend', modalHtml);
                    
                    const modalEl = document.getElementById(modalId);
                    const modalObj = new bootstrap.Modal(modalEl);
                    modalObj.show();
                    
                    modalEl.addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
                    modalEl.addEventListener('hidden.bs.modal', function () { modalEl.remove(); });

                    fetch(urlFetch).then(res => res.text()).then(html => {
                        const bodyEl = document.getElementById('modalCheckoutBody<%=rnd%>');
                        if(bodyEl) {
                            bodyEl.innerHTML = html;
                            bodyEl.querySelectorAll('script').forEach(s => { const ns = document.createElement('script'); if (s.src) ns.src = s.src; else ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); });
                        }
                    }).catch(err => {
                        const bodyEl = document.getElementById('modalCheckoutBody<%=rnd%>');
                        if(bodyEl) bodyEl.innerHTML = '<div class="alert alert-danger m-4 shadow-sm rounded-4"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal memuat saluran pembayaran. Silakan periksa koneksi atau coba lagi.")%></div>';
                    });
                };
                
                // --- MANAJEMEN CETAK SURAT TAGIHAN ---
                window.bukaModalSuratTagihan<%=rnd%> = function() {
                    const modalId = 'modalSuratTagihan<%=rnd%>';
                    const existingModal = document.getElementById(modalId);
                    if (existingModal) { existingModal.remove(); document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); }

                    const formContent = document.getElementById('htmlModalSuratTagihan<%=rnd%>').innerHTML;
                    const modalHtml = '<div class="modal fade animate__animated animate__fadeIn" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                                      '  <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">' +
                                      '    <div class="modal-content border-0 shadow-lg rounded-4">' +
                                      '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                                      '        <h5 class="modal-title fw-bold"><i class="fas fa-file-invoice me-2"></i> <%= Common.getBahasaConfig("Cetak Surat Tagihan") %></h5>' +
                                      '        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                                      '      </div>' +
                                      '      <div class="modal-body p-4 bg-light" id="bodySuratTagihan' + '<%=rnd%>' + '">' +
                                      formContent +
                                      '      </div>' +
                                      '      <div class="modal-footer border-0 bg-white d-flex justify-content-between py-3">' +
                                      '        <button type="button" class="btn btn-light rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Batal") %></button>' +
                                      '        <button type="button" class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" onclick="window.submitSuratTagihan<%=rnd%>()"><i class="fas fa-print me-2"></i><%= Common.getBahasaConfig("Tampilkan Tagihan") %></button>' +
                                      '      </div>' +
                                      '    </div>' +
                                      '  </div>' +
                                      '</div>';
                    
                    document.body.insertAdjacentHTML('beforeend', modalHtml);
                    const modalEl = document.getElementById(modalId);
                    const modalObj = new bootstrap.Modal(modalEl);
                    modalObj.show();
                    
                    modalEl.addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
                    modalEl.addEventListener('hidden.bs.modal', function () { modalEl.remove(); });
                };

                window.submitSuratTagihan<%=rnd%> = function() {
                    const form = document.querySelector('#modalSuratTagihan<%=rnd%> form');
                    if(!form) return;
                    
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Memproses surat tagihan, mohon tunggu...")%>', 'bg-info text-white');
                    
                    const formData = new URLSearchParams(new FormData(form));
                    const urlCetak = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=pembayaran_online_mhs_services&action=CETAK_SURAT_TAGIHAN&id=<%=idStr%>&jkId=<%=jkIdStr%>&isMahasiswa=<%=isMahasiswaStr%>&smt=<%=smtInt%>';
                    
                    fetch(urlCetak, { method: 'POST', body: formData })
                        .then(response => {
                            if (!response.ok) throw new Error("Gagal");
                            return response.text();
                        })
                        .then(urlPdf => {
                            const modalEl = document.getElementById('modalSuratTagihan<%=rnd%>');
                            if(modalEl) bootstrap.Modal.getInstance(modalEl).hide();
                            
                            window.tampilkanPdfSuratTagihan<%=rnd%>(urlPdf);
                        })
                        .catch(err => {
                            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi galat saat membuat dokumen surat tagihan.")%>', 'bg-danger text-white');
                        });
                };

                window.tampilkanPdfSuratTagihan<%=rnd%> = function(urlPdf) {
                    const modalId = 'modalKwitansi<%=rnd%>';
                    const existingModal = document.getElementById(modalId);
                    if (existingModal) { existingModal.remove(); document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); }

                    const modalHtml = '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                                      '  <div class="modal-dialog modal-xl modal-dialog-centered">' +
                                      '    <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">' +
                                      '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                                      '        <h5 class="modal-title fw-bold"><i class="fas fa-file-pdf me-2"></i> <%= Common.getBahasaConfig("Dokumen Surat Tagihan") %></h5>' +
                                      '        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                                      '      </div>' +
                                      '      <div class="modal-body p-0 bg-light">' +
                                      '        <iframe src="' + urlPdf + '" width="100%" height="600px" style="border: none;"></iframe>' +
                                      '      </div>' +
                                      '      <div class="modal-footer border-0 bg-white d-flex justify-content-center py-3">' +
                                      '        <button type="button" class="btn btn-secondary px-5 py-2 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Tutup") %></button>' +
                                      '      </div>' +
                                      '    </div>' +
                                      '  </div>' +
                                      '</div>';
                                      
                    document.body.insertAdjacentHTML('beforeend', modalHtml);
                    const modalObj = new bootstrap.Modal(document.getElementById(modalId));
                    modalObj.show();
                    
                    document.getElementById(modalId).addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
                    document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
                };

                // --- MANAJEMEN RIWAYAT (CETAK STRUK, HAPUS & EDIT) ---
                window.cetakStrukGlobal<%=rnd%> = function() {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Mempersiapkan dokumen struk...")%>', 'bg-info text-white');
                    const urlCetak = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=pembayaran_online_mhs_services&action=CETAK_STRUK&id=<%=idStr%>&jkId=<%=jkIdStr%>&isMahasiswa=<%=isMahasiswaStr%>&smt=<%=smtInt%>';
                    console.log("urlCetak ", urlCetak);
                    fetch(urlCetak).then(response => {
                        if (!response.ok) throw new Error("Gagal");
                        return response.text();
                    }).then(urlPdf => {
                        const modalId = 'modalKwitansi<%=rnd%>';
                        const existingModal = document.getElementById(modalId);
                        if (existingModal) { existingModal.remove(); document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); }

                        const modalHtml = '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                                          '  <div class="modal-dialog modal-xl modal-dialog-centered">' +
                                          '    <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">' +
                                          '      <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                                          '        <h5 class="modal-title fw-bold"><i class="fas fa-file-pdf me-2"></i> <%= Common.getBahasaConfig("Dokumen Kuitansi Pembayaran") %></h5>' +
                                          '        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                                          '      </div>' +
                                          '      <div class="modal-body p-0 bg-light">' +
                                          '        <iframe src="' + urlPdf + '" width="100%" height="600px" style="border: none;"></iframe>' +
                                          '      </div>' +
                                          '      <div class="modal-footer border-0 bg-white d-flex justify-content-center py-3">' +
                                          '        <button type="button" class="btn btn-secondary px-5 py-2 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Tutup") %></button>' +
                                          '      </div>' +
                                          '    </div>' +
                                          '  </div>' +
                                          '</div>';
                                          
                        document.body.insertAdjacentHTML('beforeend', modalHtml);
                        const modalObj = new bootstrap.Modal(document.getElementById(modalId));
                        modalObj.show();
                        
                        document.getElementById(modalId).addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
                        document.getElementById(modalId).addEventListener('hidden.bs.modal', function () { this.remove(); });
                    }).catch(err => {
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi galat saat membuat dokumen struk.")%>', 'bg-danger text-white');
                    });
                };

                window.hapusRiwayat<%=rnd%> = function(idCicilan) {
                	prosesHapusRiwayat<%=rnd%>(idCicilan);
                };

                function prosesHapusRiwayat<%=rnd%>(idCicilan) {
                    if (typeof prosesDeleteData === 'function') {
                        prosesDeleteData('ais.database.model.CicilanPembayaran', idCicilan, function() {
                            if(typeof window.loadTagihanMhs<%=rnd%> === 'function') { 
                                window.loadTagihanMhs<%=rnd%>(true); 
                            } else {
                                window.location.reload(); 
                            }
                        });
                    } else {
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Sedang menghapus data pembayaran...")%>', 'bg-info text-white');
                        const urlDelete = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=pembayaran_online_mhs_services&action=DELETE_CICILAN&id=<%=idStr%>&jkId=<%=jkIdStr%>&isMahasiswa=<%=isMahasiswaStr%>&smt=<%=smtInt%>&idCicilan=' + idCicilan;
                        
                        fetch(urlDelete).then(response => response.text()).then(html => {
                            const tempDiv = document.createElement('div'); tempDiv.innerHTML = html;
                            tempDiv.querySelectorAll('script').forEach(s => { const ns = document.createElement('script'); ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); });
                            if(typeof window.loadTagihanMhs<%=rnd%> === 'function') { window.loadTagihanMhs<%=rnd%>(true); } else window.location.reload();
                        }).catch(err => {
                            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menghubungi peladen.")%>', 'bg-danger text-white');
                        });
                    }
                }
                
                // MODAL UBAH RIWAYAT SECARA DINAMIS
                window.editRiwayat<%=rnd%> = function(btn) {
                    const ds = btn.dataset;
                    const modalId = 'modalEditRiwayat<%=rnd%>';
                    const existingModal = document.getElementById(modalId);
                    if (existingModal) { existingModal.remove(); document.querySelectorAll('.modal-backdrop').forEach(b => b.remove()); }
                    
                    const optsCaraBayar = document.getElementById('optCaraBayar_<%=rnd%>').innerHTML;
                    const optsItemBiaya = document.getElementById('optItemBiaya_<%=rnd%>').innerHTML;

                    const modalHtml = '<div class="modal fade animate__animated animate__fadeIn" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">' +
                                      '  <div class="modal-dialog modal-dialog-centered">' +
                                      '    <div class="modal-content border-0 shadow-lg rounded-4">' +
                                      '      <form id="formEditRiwayat' + '<%=rnd%>' + '">' +
                                      '          <div class="modal-header bg-gradient bg-primary text-white border-0 py-3">' +
                                      '            <h5 class="modal-title fw-bold"><i class="fas fa-edit me-2"></i> <%=Common.getBahasaConfig("Ubah Rincian Riwayat Pembayaran")%></h5>' +
                                      '            <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal"></button>' +
                                      '          </div>' +
                                      '          <div class="modal-body p-4 bg-light">' +
                                      '            <input type="hidden" name="idCicilan" value="' + ds.id + '">' +
                                      '            <div class="row">' +
                                      '                <div class="col-6 mb-3">' +
                                      '                    <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Bayar")%></label>' +
                                      '                    <input type="date" class="form-control rounded-3" name="tanggal" value="' + ds.tgl + '" required>' +
                                      '                </div>' +
                                      '                <div class="col-6 mb-3">' +
                                      '                    <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Tagihan")%></label>' +
                                      '                    <input type="date" class="form-control rounded-3" name="tanggalTagihan" value="' + ds.tgltagihan + '">' +
                                      '                </div>' +
                                      '            </div>' +
                                      '            <div class="mb-3">' +
                                      '                <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Item Biaya")%></label>' +
                                      '                <select class="form-select rounded-3" name="itemBiaya" required>' + optsItemBiaya + '</select>' +
                                      '            </div>' +
                                      '            <div class="mb-3">' +
                                      '                <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Cara Bayar")%></label>' +
                                      '                <select class="form-select rounded-3" name="caraBayar" required>' + optsCaraBayar + '</select>' +
                                      '            </div>' +
                                      '            <div class="row">' +
                                      '                <div class="col-6 mb-3">' +
                                      '                    <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Nilai Bayar")%></label>' +
                                      '                    <input type="number" class="form-control rounded-3 fw-bold text-success" name="nilai" value="' + ds.nilai + '" min="0" step="any" required>' +
                                      '                </div>' +
                                      '                <div class="col-6 mb-3">' +
                                      '                    <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Denda")%></label>' +
                                      '                    <input type="number" class="form-control rounded-3 fw-bold text-danger" name="denda" value="' + ds.denda + '" min="0" step="any">' +
                                      '                </div>' +
                                      '            </div>' +
                                      '            <div class="mb-0">' +
                                      '                <label class="form-label fw-bold text-secondary"><%=Common.getBahasaConfig("Keterangan Tambahan")%></label>' +
                                      '                <textarea class="form-control rounded-3" name="keterangan" rows="3">' + ds.ket + '</textarea>' +
                                      '            </div>' +
                                      '          </div>' +
                                      '          <div class="modal-footer bg-white border-top p-3 d-flex justify-content-between">' +
                                      '            <button type="button" class="btn btn-light rounded-pill px-4 fw-bold shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>' +
                                      '            <button type="submit" class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Perubahan")%></button>' +
                                      '          </div>' +
                                      '      </form>' +
                                      '    </div>' +
                                      '  </div>' +
                                      '</div>';
                    
                    document.body.insertAdjacentHTML('beforeend', modalHtml);
                    
                    const formEdit = document.getElementById('formEditRiwayat<%=rnd%>');
                    formEdit.elements['caraBayar'].value = ds.cara;
                    formEdit.elements['itemBiaya'].value = ds.item;

                    const modalEl = document.getElementById(modalId);
                    const modalObj = new bootstrap.Modal(modalEl);
                    modalObj.show();

                    modalEl.addEventListener('hide.bs.modal', function() { if(document.activeElement) document.activeElement.blur(); });
                    modalEl.addEventListener('hidden.bs.modal', function () { modalEl.remove(); });
                    
                    formEdit.addEventListener('submit', function(e) {
                        e.preventDefault();
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Menyimpan pembaruan data...")%>', 'bg-info text-white');
                        
                        const formData = new URLSearchParams(new FormData(formEdit));
                        const urlUpdate = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=pembayaran_online_mhs_services&action=UPDATE_CICILAN&id=<%=idStr%>&jkId=<%=jkIdStr%>&isMahasiswa=<%=isMahasiswaStr%>&smt=<%=smtInt%>';
                        
                        fetch(urlUpdate, { method: 'POST', body: formData }).then(res => res.text()).then(html => {
                            modalObj.hide();
                            const tempDiv = document.createElement('div'); tempDiv.innerHTML = html;
                            tempDiv.querySelectorAll('script').forEach(s => { const ns = document.createElement('script'); ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); });

                            if(typeof window.loadTagihanMhs<%=rnd%> === 'function') { window.loadTagihanMhs<%=rnd%>(true); } else { window.location.reload(); }
                        }).catch(err => {
                            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menyimpan perubahan ke peladen.")%>', 'bg-danger text-white');
                        });
                    });
                };
            </script>
            <%
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:1518");
        out.print("<div class='alert alert-danger shadow-sm rounded-4 border-0 m-4'><i class='fas fa-exclamation-circle me-2'></i>" + Common.getBahasaConfig("Terjadi galat sistem secara teknis saat memuat data: ") + e.getMessage() + "</div>");
    } finally {
        if (sess != null && sess.isOpen()) {
            sess.disconnect();
            sess.close();
        }
        try { HibernateUtil.closeSession(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/pembayaran_online_mhs_services.jsp:1525");}
    }
%>