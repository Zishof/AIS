<%-- BUILD 2026-06-29: dipaksa rekompilasi untuk cegah IncompatibleClassChangeError dari method PembayaranUtil yang berubah static->instance pada JSP ter-compile lama. WAJIB bersihkan work dir Tomcat saat deploy lalu restart. --%>
<%@page import="ais.action.master.helper.KegiatanPersistenceHelper"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.DetailBiaya"%>
<%@page import="ais.database.model.DetailKegiatan"%>
<%@page import="ais.database.model.PengaturanPembayaranBulanan"%>
<%@page import="ais.database.model.JadwalPembayaran"%>
<%@page import="ais.database.model.ItemBiaya"%>
<%@page import="ais.database.model.CicilanPembayaran"%>
<%@page import="ais.database.model.VOMahasiswa"%>
<%@page import="ais.database.model.DetailSettingBiaya"%>
<%@page import="ais.database.model.JenisDiskonMahasiswa"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.action.ws.util.PembayaranUtil"%>
<%@page import="ais.action.master.helper.PembayaranUtilHelper"%>
<%@page import="ais.action.report.CommonReportHelper"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.*"%>
<%@page import="java.util.concurrent.*"%>
<%@page import="java.util.concurrent.atomic.AtomicInteger"%>

<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:34");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi berakhir. Silakan muat ulang halaman.") + "\"}");
        return;
    }

    String action = request.getParameter("action");
    String personIdStr = request.getParameter("personId");
    String isMahasiswaStr = request.getParameter("isMahasiswa");
    String jkIdStr = request.getParameter("jkId");
    String smtMulaiStr = request.getParameter("smtMulai");
    String smtSampaiStr = request.getParameter("smtSampai");
    boolean isRefresh = "true".equalsIgnoreCase(request.getParameter("refresh"));

    if (personIdStr == null || personIdStr.trim().isEmpty()) {
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Identitas pelanggan tidak valid.") + "\"}");
        return;
    }

    long personId = Long.parseLong(personIdStr);
    boolean isMahasiswa = "true".equalsIgnoreCase(isMahasiswaStr);
    
    Session parentSession = null;
    ExecutorService executor = null;

    try {
        parentSession = HibernateUtil.openSession();
        
        VOMahasiswa mhsUtama = (VOMahasiswa) (isMahasiswa 
            ? ConstantValues.ambil(Mahasiswa.class.getName(), personId, true) 
            : ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), personId, true));
            
        if (mhsUtama == null) throw new Exception("Data mahasiswa tidak ditemukan di pangkalan data.");

        // =========================================================================
        // LOGIKA CETAK DOKUMEN SURAT (Diarahkan ke metode standar CommonReportHelper)
        // =========================================================================
        if ("CETAK_TAGIHAN".equals(action) || "CETAK_BEBAS_TUNGGAKAN".equals(action)) {
            // Karena CommonReportHelper memerlukan TreeMap semua, kita persiapkan Map Dummy (Hanya representasi)
            // Di lingkungan ZK, 'semua' diisi dari UI. Di JSP, kita panggil fungsi Report bawaan yang kompatibel.
            File filePdf = null;
            if ("CETAK_TAGIHAN".equals(action)) {
                //if (mhsUtama instanceof Mahasiswa) filePdf = CommonReportHelper.onLaporanTagihanFile((Mahasiswa) mhsUtama, new TreeMap());
                //else filePdf = CommonReportHelper.onLaporanTagihanFile((BiodataCalonMahasiswa) mhsUtama, new TreeMap());
            } else {
                //if (mhsUtama instanceof Mahasiswa) filePdf = CommonReportHelper.onLaporanBebasTunggakanFile((Mahasiswa) mhsUtama, new TreeMap());
            }
            
            if (filePdf != null && filePdf.exists()) {
                String pathPdf = request.getContextPath() + "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(filePdf.getName()), "UTF-8");
                JSONObject res = new JSONObject(); res.put("status", "00"); res.put("urlPdf", pathPdf);
                out.print(res.toString()); return;
            } else {
                throw new Exception("Dokumen gagal dihasilkan oleh Report Engine.");
            }
        }

        // =========================================================================
        // LOGIKA UTAMA: MEMUAT TAGIHAN PARALEL (MULTITHREADING MAX 50)
        // =========================================================================
        
        // Optimasi Memori: Ekstrak DetailKegiatan Sekali di Luar Loop
        final Collection<DetailKegiatan> detailKegiatansTempGlobal = new ArrayList<DetailKegiatan>();
        if (isRefresh) mhsUtama.reInitKegiatan(parentSession);
        
        if (mhsUtama instanceof BiodataCalonMahasiswa) {
            detailKegiatansTempGlobal.addAll(((BiodataCalonMahasiswa) mhsUtama).ambilDetailKegiatanSaja(isRefresh));
        } else {
            detailKegiatansTempGlobal.addAll(mhsUtama.ambilDetailKegiatanSaja(isRefresh));
            Mahasiswa objMhs = (Mahasiswa) mhsUtama;
            if (objMhs.getBiodataCalonMahasiswa() != null) {
                BiodataCalonMahasiswa bcm = (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), objMhs.getBiodataCalonMahasiswa());
                if (bcm != null) {
                    if (isRefresh) bcm.reInitKegiatan(parentSession);
                    detailKegiatansTempGlobal.addAll(bcm.ambilDetailKegiatanSaja(isRefresh));
                }
            }
        }

        // Menyusun Map Jenis Kegiatan yang akan diproses
        final Map<Long, JenisKegiatan> mapsJk = new HashMap<Long, JenisKegiatan>();
        if ("BELUM_LUNAS".equals(jkIdStr) || "SEMUA_TAGIHAN".equals(jkIdStr)) {
            List<JenisKegiatan> listAllJk = ConstantValues.simpleList(parentSession.createCriteria(JenisKegiatan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), JenisKegiatan.class);
            for(JenisKegiatan j : listAllJk) mapsJk.put(j.getId(), j);
        } else {
            JenisKegiatan jSpec = (JenisKegiatan) parentSession.get(JenisKegiatan.class, Long.parseLong(jkIdStr));
            if (jSpec != null) mapsJk.put(jSpec.getId(), jSpec);
        }

        final int sMulaiOpt = smtMulaiStr != null ? Integer.parseInt(smtMulaiStr) : 1;
        final int sSampaiOpt = smtSampaiStr != null ? Integer.parseInt(smtSampaiStr) : 1;

        executor = Executors.newFixedThreadPool(50);
        List<Callable<String>> tasks = new ArrayList<Callable<String>>();

        for (final JenisKegiatan jk : mapsJk.values()) {
            if (jk == null || jk.getId() == null || (jk.getAktif() != null && !jk.getAktif())) continue;

            int mulai = sMulaiOpt; int sampai = sSampaiOpt;
            if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(jk.getId())) {
                mulai = 0; sampai = 0;
            }

            for (int smt = mulai; smt <= sampai; smt++) {
                final int fSmt = smt;
                final VOMahasiswa fMhsUtama = mhsUtama;
                final boolean fRefresh = isRefresh;
                final boolean isOnlyBelumLunas = "BELUM_LUNAS".equals(jkIdStr);

                tasks.add(new Callable<String>() {
                    @Override
                    public String call() {
                        Session childSession = null;
                        StringBuilder htmlBuilder = new StringBuilder();
                        try {
                            childSession = HibernateUtil.openSession();
                            
                            VOMahasiswa mhsTarget = fMhsUtama;
                            if (fMhsUtama instanceof Mahasiswa) {
                                boolean isCalonAct = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(jk.getId()));
                                boolean isDaftarAct = (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId().equals(jk.getId()));
                                
                                if (isCalonAct || isDaftarAct) {
                                    Mahasiswa objMhs = (Mahasiswa) fMhsUtama;
                                    if (objMhs.getBiodataCalonMahasiswa() != null) {
                                        BiodataCalonMahasiswa bcm = (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), objMhs.getBiodataCalonMahasiswa());
                                        if (bcm != null) mhsTarget = bcm;
                                    }
                                }
                            }

                            List<CicilanPembayaran> cicilanPembayarans = (mhsTarget instanceof BiodataCalonMahasiswa) 
                                    ? ((BiodataCalonMahasiswa) mhsTarget).ambilCicilan(jk, fRefresh) 
                                    : mhsTarget.ambilCicilan(jk, fRefresh);

                            Collection detailBiayas;
                            if (mhsTarget instanceof Mahasiswa) {
                                detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa((Mahasiswa) mhsTarget, fSmt, jk, fRefresh);
                            } else {
                                detailBiayas = new ArrayList();
                                BiodataCalonMahasiswa calonMhs = (BiodataCalonMahasiswa) mhsTarget;
                                ais.database.model.Jurusan prodiLulus = calonMhs.getProdiLulus();
                                ais.database.model.Jurusan targetJur = (prodiLulus == null || prodiLulus.getId() == null) ? (calonMhs.getProdi1() == null ? calonMhs.getProdi2() : calonMhs.getProdi1()) : prodiLulus;
                                
                                if (jk.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) detailBiayas.addAll(PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calonMhs, jk, targetJur, fSmt, fRefresh));
                                else if (jk.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) detailBiayas.addAll(PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calonMhs, jk, targetJur, fRefresh));
                            }

                            int countPengaturanBulanan = (mhsTarget instanceof Mahasiswa)
                                    ? PembayaranUtilHelper.countBulanan(childSession, (Mahasiswa) mhsTarget, jk, fSmt, detailBiayas, fRefresh, false)
                                    : PembayaranUtilHelper.countBulanan(childSession, (BiodataCalonMahasiswa) mhsTarget, jk, fSmt, detailBiayas, fRefresh, false);
                            
                            if (countPengaturanBulanan > 0) {
                                detailBiayas = (mhsTarget instanceof Mahasiswa)
                                        ? PembayaranUtilHelper.getDetailBiayaMahasiswa((Mahasiswa) mhsTarget, fSmt, jk, "-1", true, fRefresh)
                                        : PembayaranUtil.getInstance().getPengaturanPembayaranSemua((BiodataCalonMahasiswa) mhsTarget, childSession, fSmt, jk, detailBiayas, fRefresh, false);
                            }

                            if (detailBiayas == null || detailBiayas.isEmpty()) return "";

                            Kegiatan kegiatan = mhsTarget.ambilKegiatans(fSmt, jk, fRefresh);
                            Collection<DetailKegiatan> detailKegiatans = KegiatanPersistenceHelper.ambilDetailKegiatan(detailKegiatansTempGlobal, kegiatan, fRefresh);  

                            // Parameter Jadwal Pembayaran
                            Integer tahunAngkatan = 0; String semesterMulaiNama = "";
                            ais.database.model.Jenjang jenjangObj = null; ais.database.model.JenisSeleksi seleksiObj = null;
                            String programMhs = ""; String identitasMhs = ""; ais.database.model.GelombangPendaftaran gelombangPendaftaran = null;

                            if (mhsTarget instanceof Mahasiswa) {
                                Mahasiswa m = (Mahasiswa) mhsTarget;
                                tahunAngkatan = m.getTahunangkatan() != null ? m.getTahunangkatan() : 0;
                                semesterMulaiNama = m.getSemesterMulai();
                                jenjangObj = (m.getJurusan() != null) ? m.getJurusan().getJenjang() : null;
                                seleksiObj = m.getJenisSeleksi(); programMhs = m.getProgram(); identitasMhs = m.getNim();
                                try { java.lang.reflect.Method mtd = m.getClass().getMethod("getGelombangPendaftaran"); gelombangPendaftaran = (ais.database.model.GelombangPendaftaran) mtd.invoke(m); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:211");}
                            } else {
                                BiodataCalonMahasiswa c = (BiodataCalonMahasiswa) mhsTarget;
                                tahunAngkatan = c.getTahun() != null ? c.getTahun() : 0;
                                semesterMulaiNama = c.getSemesterMulai();
                                jenjangObj = c.getJenjang(); seleksiObj = c.getJenisSeleksi(); programMhs = c.getProgram(); identitasMhs = c.getNoRegistrasi(); gelombangPendaftaran = c.getGelombangPendaftaran();
                            }

                            Integer smtMasuk = (mhsTarget instanceof Mahasiswa && ((Mahasiswa)mhsTarget).getPindahKeKampusIniMasukSemester() != null) ? ((Mahasiswa)mhsTarget).getPindahKeKampusIniMasukSemester() : 0;
                            Integer thnAkademikMulai = Common.getTahunAkademik(fSmt, tahunAngkatan, smtMasuk, semesterMulaiNama);
                            String taJadwalStr = thnAkademikMulai + "/" + (thnAkademikMulai + 1);

                            Date tanggalSekarang = WaktuUtil.getDate();
                            java.io.Serializable[] serials = PembayaranUtil.getInstance().getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
                                    tanggalSekarang, jk, jenjangObj, taJadwalStr, 
                                    ("Ganjil".equalsIgnoreCase(semesterMulaiNama) || ais.database.model.Perkuliahan.GANJIL.equalsIgnoreCase(semesterMulaiNama)),
                                    seleksiObj, programMhs, identitasMhs, gelombangPendaftaran);

                            JadwalPembayaran jadwal = (serials != null && serials.length > 0) ? (JadwalPembayaran) serials[0] : null;
                            if (jadwal == null && kegiatan != null) jadwal = kegiatan.getJadwalPembayaran();
                            JadwalPembayaran jdw = jadwal != null && jadwal.getKhususUntukNim() != null && jadwal.getKhususUntukNim().contains("," + identitasMhs + ",") ? jadwal : null;

                            // Pembentukan HTML Responsif (Bootstrap Card/Table)
                            StringBuilder rowsHtml = new StringBuilder();
                            Double grandTagihan = 0.0; Double grandDibayar = 0.0; Double grandBelumDibayar = 0.0;

                            for (Object o : detailBiayas) {
                                DetailBiaya tempdetailBiaya = null; PengaturanPembayaranBulanan pb = null;
                                if (o instanceof DetailBiaya) { tempdetailBiaya = (DetailBiaya) o; } 
                                else if (o instanceof PengaturanPembayaranBulanan) { pb = (PengaturanPembayaranBulanan) o; if (pb != null) tempdetailBiaya = pb.getDetailBiaya(); }

                                DetailKegiatan dk = kegiatan == null ? null : (pb != null ? kegiatan.ambilSatuDetailKegiatan(pb, detailKegiatans) : kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya));
                                if (dk != null && dk.getBukanTagihan() != null && dk.getBukanTagihan()) { continue; }

                                Double jumlah = 0.0; Double telahDibayar = 0.0; String namaItem = ""; String dendaHtml = "";
                                
                                if (pb != null) {
                                    namaItem = pb.getDetailBiaya().getItemBiaya().getNama() + ", Bulan " + pb.getNamaBulan();
                                    jumlah = (mhsTarget instanceof Mahasiswa) ? Kegiatan.ambilJumlahTagihan(dk, pb.getDetailBiaya(), kegiatan, (Mahasiswa) mhsTarget, fSmt, pb) : Kegiatan.ambilJumlahTagihan(dk, pb.getDetailBiaya(), kegiatan, fSmt, pb);
                                    if(jumlah == null) jumlah = 0.0;

                                    Date tanggalBayarItem = tanggalSekarang;
                                    if (cicilanPembayarans != null) {
                                        for (CicilanPembayaran cp : cicilanPembayarans) {
                                            try {
                                                if (cp.getItemBiaya() != null && cp.getItemBiaya().getId().equals(pb.getDetailBiaya().getItemBiaya().getId()) && pb.getDetailBiaya().getBayarKe().equals(cp.getBayarKe()) && cp.getKegiatan().getId().equals(kegiatan.getId())) {
                                                    if (cp.getPengaturanPembayaranBulanan() != null) {
                                                        PengaturanPembayaranBulanan p = cp.getPengaturanPembayaranBulanan();
                                                        if (p.getDetailBiaya().getItemBiaya().getId().equals(pb.getDetailBiaya().getItemBiaya().getId()) && p.getRealBulan().equals(pb.getRealBulan())) { tanggalBayarItem = cp.getTanggal(); }
                                                    }
                                                }
                                            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:262");}
                                        }
                                    }

                                    // LOGIKA DENDA KUSTOM VS SISTEM (PENGATURAN BULANAN)
                                    Double hasilDenda = dk != null && (dk.getBatalkanDenda() != null && dk.getBatalkanDenda() || jumlah.intValue() == 0) ? jumlah
                                            : dk != null && dk.getMenggunakanDendaCustom() != null && dk.getMenggunakanDendaCustom() ? jumlah
                                            : pb.checkDenda(jumlah, tanggalBayarItem, jdw, jadwal == null ? null : jadwal.getJenisKegiatan());

                                    if (dk != null && dk.getMenggunakanDendaCustom() != null && dk.getMenggunakanDendaCustom()) {
                                        pb.setInfoDenda(" Penambahan denda senilai " + Common.numberFormat.get().format(dk.getDendaCustom() != null ? dk.getDendaCustom() : 0.0) + ".");
                                    }
                                    
                                    Double kalkulasiDendaSistem = hasilDenda - jumlah;
                                    if (dk != null && (dk.getMenggunakanDendaCustom() == null || !dk.getMenggunakanDendaCustom())) { dk.setDendaCustom(kalkulasiDendaSistem); }
                                    if (kalkulasiDendaSistem > 0.0 || (dk != null && dk.getMenggunakanDendaCustom() != null && dk.getMenggunakanDendaCustom() && dk.getDendaCustom() != null && dk.getDendaCustom() > 0)) {
                                        Double valDendaRender = (dk != null && dk.getMenggunakanDendaCustom() != null && dk.getMenggunakanDendaCustom()) ? dk.getDendaCustom() : kalkulasiDendaSistem;
                                        dendaHtml = " <span class='badge bg-danger ms-2 shadow-sm' style='font-size:0.65rem;'><i class='fas fa-exclamation-circle me-1'></i>Denda: Rp " + Common.numberFormat.get().format(valDendaRender) + "</span>";
                                    }

                                    telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, pb, cicilanPembayarans);
                                    if(telahDibayar == null) telahDibayar = 0.0;

                                    if (jumlah.intValue() == 0 && telahDibayar.intValue() > 0) { jumlah = telahDibayar; }
                                    jumlah = hasilDenda.intValue() > jumlah.intValue() ? hasilDenda : jumlah;

                                    if (pb.getDetailBiaya() != null && pb.getDetailBiaya().getItemBiaya() != null && ItemBiaya.DIKALI_NILAI_MINUS.equals(pb.getDetailBiaya().getItemBiaya().getPenghitungan())) {
                                        jumlah = -Math.abs(jumlah); telahDibayar = -Math.abs(telahDibayar);
                                    }
                                } else if (tempdetailBiaya != null) {
                                    namaItem = tempdetailBiaya.getKeterangan() != null ? tempdetailBiaya.getKeterangan() : tempdetailBiaya.getItemBiaya().getNama();
                                    jumlah = Kegiatan.ambilJumlahTagihan(dk, kegiatan, tempdetailBiaya, fRefresh);
                                    if(jumlah == null) jumlah = 0.0;
                                    
                                    // LOGIKA DENDA KUSTOM VS SISTEM (DETAIL BIAYA)
                                    Double hasilDenda = dk != null && dk.getMenggunakanDendaCustom() != null && dk.getMenggunakanDendaCustom() ? jumlah
                                            : dk != null && (dk.getBatalkanDenda() != null && dk.getBatalkanDenda() || jumlah.intValue() == 0) ? jumlah
                                            : tempdetailBiaya.checkDenda(jumlah, tanggalSekarang, jdw, jadwal == null ? null : jadwal.getJenisKegiatan(), null);

                                    if (dk != null && dk.getMenggunakanDendaCustom() != null && dk.getMenggunakanDendaCustom()) {
                                        tempdetailBiaya.setInfoDenda(" Penambahan denda senilai " + Common.numberFormat.get().format(dk.getDendaCustom() != null ? dk.getDendaCustom() : 0.0) + ".");
                                    }
                                    
                                    Double kalkulasiDendaSistem = hasilDenda - jumlah;
                                    if (dk != null && (dk.getMenggunakanDendaCustom() == null || !dk.getMenggunakanDendaCustom())) { dk.setDendaCustom(kalkulasiDendaSistem); }
                                    if (kalkulasiDendaSistem > 0.0 || (dk != null && dk.getMenggunakanDendaCustom() != null && dk.getMenggunakanDendaCustom() && dk.getDendaCustom() != null && dk.getDendaCustom() > 0)) {
                                        Double valDendaRender = (dk != null && dk.getMenggunakanDendaCustom() != null && dk.getMenggunakanDendaCustom()) ? dk.getDendaCustom() : kalkulasiDendaSistem;
                                        dendaHtml = " <span class='badge bg-danger ms-2 shadow-sm' style='font-size:0.65rem;'><i class='fas fa-exclamation-circle me-1'></i>Denda: Rp " + Common.numberFormat.get().format(valDendaRender) + "</span>";
                                    }

                                    telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, tempdetailBiaya, cicilanPembayarans);
                                    if(telahDibayar == null) telahDibayar = 0.0;

                                    if (jumlah.intValue() == 0 && telahDibayar.intValue() > 0) { jumlah = telahDibayar; }
                                    jumlah = hasilDenda.intValue() > jumlah.intValue() ? hasilDenda : jumlah;

                                    if (tempdetailBiaya.getItemBiaya() != null && ItemBiaya.DIKALI_NILAI_MINUS.equals(tempdetailBiaya.getItemBiaya().getPenghitungan())) {
                                        jumlah = -Math.abs(jumlah); telahDibayar = -Math.abs(telahDibayar);
                                    }
                                }

                                Double belumDibayar = jumlah - telahDibayar;
                                int totalValid = jumlah.intValue() + belumDibayar.intValue() + telahDibayar.intValue();
                                if (totalValid == 0) continue;

                                Double persen = (jumlah > 0) ? (telahDibayar * 100.0) / jumlah : 0.0;
                                
                                String revBadges = "";
                                DetailBiaya refDb = pb != null ? pb.getDetailBiaya() : tempdetailBiaya;
                                if (pb == null && kegiatan != null && kegiatan.getMahasiswa() != null && refDb != null && refDb.getItemBiaya() != null
                                        && kegiatan.getMahasiswa().getKelompokMahasiswa() != null
                                        && kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtMulai() != null 
                                        && kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtMulai() <= kegiatan.getSemster()
                                        && kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtSampai() != null
                                        && kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtSampai() >= kegiatan.getSemster()
                                        && kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa() != null
                                        && !kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds().isEmpty()
                                        && kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds().contains(refDb.getItemBiaya().getId())) {
                                    revBadges += "<span class='badge bg-warning text-dark ms-1'>" + kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getNama() + "</span>";
                                } else if (dk != null && dk.adaDiskon()) {
                                    if(dk.getDiskonMahasiswaData() != null && dk.getDiskonMahasiswaData().getJenisDiskonMahasiswa() != null) revBadges += "<span class='badge bg-warning text-dark ms-1'>" + dk.getDiskonMahasiswaData().getJenisDiskonMahasiswa().getNama() + "</span>";
                                    if(dk.getDiskonMahasiswaData2() != null && dk.getDiskonMahasiswaData2().getJenisDiskonMahasiswa() != null) revBadges += "<span class='badge bg-warning text-dark ms-1'>" + dk.getDiskonMahasiswaData2().getJenisDiskonMahasiswa().getNama() + "</span>";
                                    if(dk.getDiskonMahasiswaData3() != null && dk.getDiskonMahasiswaData3().getJenisDiskonMahasiswa() != null) revBadges += "<span class='badge bg-warning text-dark ms-1'>" + dk.getDiskonMahasiswaData3().getJenisDiskonMahasiswa().getNama() + "</span>";
                                }

                                if (refDb != null && refDb.getDetailSettingBiaya() != null && refDb.getDetailSettingBiaya().getSettingBiaya() != null && refDb.getDetailSettingBiaya().getSettingBiaya().getJumlahPembayaran() > 1) {
                                    revBadges += "<span class='badge bg-info text-dark ms-1'>Cicilan " + refDb.getBayarKe() + "</span>";
                                }
                                
                                String tagHtml = "<td class='text-end fw-semibold'>" + Common.numberFormat.get().format(jumlah) + "</td>";
                                Double origNom = pb != null ? pb.getNominal() : (tempdetailBiaya != null ? tempdetailBiaya.getNilaiBiaya() : 0.0);
                                if (jumlah.intValue() < origNom.intValue()) {
                                    tagHtml = "<td class='text-end fw-semibold'><span class='text-muted text-decoration-line-through d-block' style='font-size:0.7rem;'>" + Common.numberFormat.get().format(origNom) + "</span>" + Common.numberFormat.get().format(jumlah) + "</td>";
                                }

                                rowsHtml.append("<tr>")
                                        .append("<td><div class='fw-bold text-dark'>").append(namaItem).append(dendaHtml).append(revBadges).append("</div></td>")
                                        .append(tagHtml)
                                        .append("<td class='text-end text-success'>").append(Common.numberFormat.get().format(telahDibayar)).append("</td>")
                                        .append("<td class='text-end text-danger fw-bold'>").append(Common.numberFormat.get().format(belumDibayar)).append("</td>")
                                        .append("<td class='text-end text-primary'>").append(Common.numberFormat.get().format(persen)).append("%</td>")
                                        .append("</tr>");
                                        
                                grandTagihan += jumlah; grandDibayar += telahDibayar; grandBelumDibayar += belumDibayar;
                            }

                            if (isOnlyBelumLunas && grandBelumDibayar.intValue() == 0) return "";
                            if (grandTagihan.intValue() == 0 && grandDibayar.intValue() == 0 && grandBelumDibayar.intValue() == 0) return "";

                            Double grandPersen = (grandTagihan > 0) ? (grandDibayar * 100.0) / grandTagihan : 0.0;
                            
                            htmlBuilder.append("<div class='card border-0 shadow-sm rounded-4 mb-4 overflow-hidden'>")
                                       .append("<div class='card-header bg-primary text-white py-3 px-4 d-flex justify-content-between align-items-center'>")
                                       .append("<h6 class='mb-0 fw-bold'><i class='fas fa-file-invoice-dollar me-2'></i>").append(jk.getNamaKegiatan()).append(" - Semester ").append(fSmt).append("</h6>")
                                       .append("</div>")
                                       .append("<div class='table-responsive bg-white'><table class='table table-hover mb-0' style='font-size:0.85rem;'>")
                                       .append("<thead class='table-light text-secondary'><tr><th>Deskripsi Item Biaya</th><th class='text-end'>Tagihan (Rp)</th><th class='text-end'>Dibayar (Rp)</th><th class='text-end'>Sisa (Rp)</th><th class='text-end'>Persen</th></tr></thead>")
                                       .append("<tbody>").append(rowsHtml.toString()).append("</tbody>")
                                       .append("<tfoot class='bg-light border-top border-2'><tr><td class='text-end fw-bold text-uppercase'>Total Akumulasi :</td>")
                                       .append("<td class='text-end fw-bolder text-dark'>").append(Common.numberFormat.get().format(grandTagihan)).append("</td>")
                                       .append("<td class='text-end fw-bolder text-success'>").append(Common.numberFormat.get().format(grandDibayar)).append("</td>")
                                       .append("<td class='text-end fw-bolder text-danger fs-6'>").append(Common.numberFormat.get().format(grandBelumDibayar)).append("</td>")
                                       .append("<td class='text-end fw-bolder text-primary'>").append(Common.numberFormat.get().format(grandPersen)).append("%</td>")
                                       .append("</tr></tfoot></table></div></div>");

                            return htmlBuilder.toString();
                        } catch (Exception e) {
                            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:389");
                            return "";
                        } finally {
                            if (childSession != null && childSession.isOpen()) {
                                try { childSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:393");}
                                try { childSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:394");}
                                try { childSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:395");}
                            }
                        }
                    }
                });
            }
        }

        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        StringBuilder finalHtml = new StringBuilder();
        for (Future<String> future : futures) {
            String chunk = future.get();
            if (chunk != null && !chunk.isEmpty()) finalHtml.append(chunk);
        }

        if (finalHtml.length() == 0) {
            finalHtml.append("<div class='alert alert-info text-center shadow-sm rounded-4 m-4 py-5 border-0'><i class='fas fa-box-open fa-3x mb-3 text-secondary opacity-50'></i><br><h6 class='fw-bold text-dark'>").append(Common.getBahasaConfig("Kewajiban Pembayaran Tidak Ditemukan")).append("</h6>").append(Common.getBahasaConfig("Belum ada data atau rincian pembayaran yang dapat ditampilkan pada kriteria ini.")).append("</div>");
        }

        JSONObject resObj = new JSONObject();
        resObj.put("status", "00");
        resObj.put("html", finalHtml.toString());
        out.print(resObj.toString());
        out.flush();

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:423");
        JSONObject resObj = new JSONObject();
        resObj.put("status", "error");
        resObj.put("message", e.getMessage());
        out.print(resObj.toString());
        out.flush();
    } finally {
        if (parentSession != null && parentSession.isOpen()) {
            try { parentSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:431");}
            try { parentSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:432");}
            try { parentSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:433");}
        }
        try { HibernateUtil.closeSession(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_service_informasi_pembayaran_mahasiswa.jsp:435");}
    }
%>