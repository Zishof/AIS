<%-- BUILD 2026-06-29: dipaksa rekompilasi untuk cegah IncompatibleClassChangeError dari method PembayaranUtil yang berubah static->instance pada JSP ter-compile lama. WAJIB bersihkan work dir Tomcat saat deploy lalu restart. --%>
<%@page import="ais.action.master.sekolah.util.DepositHelper"%>
<%@page import="ais.action.master.sekolah.util.PembayaranSiswaUtil"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.ui.util.MyCheckboxConfig"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.JenisPembayaran"%>
<%@page import="ais.action.servlet.Wa"%>
<%@page import="ais.delivery.email.sender.MailSender"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.*"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="ais.common.*"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.sekolah.*"%>
<%@page import="ais.database.model.file.FileFotoLain"%>
<%@page import="ais.database.model.file.FotoSiswa"%>
<%@page import="ais.database.model.file.FotoCalonSiswa"%>
<%@page import="ais.action.master.sekolah.util.SekolahUtil"%>
<%@page import="ais.action.ws.util.PembayaranUtil"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.action.master.helper.virtualaccount.*"%>
<%@page import="ais.database.model.BankHost"%>
<%@page import="ais.database.model.VirtualAccountBank"%>
<%@page import="ais.action.master.sekolah.helper.DetailTagihanSiswaHelper"%>
<%@page import="ais.action.master.sekolah.helper.DetailTagihanCalonSiswaHelper"%>
<%@page import="ais.action.master.sekolah.helper.TagihanUtil"%>
<%@page import="ais.action.master.sekolah.helper.TagihanUtilCalonSiswa"%>
<%@page import="org.zkoss.zul.Rows"%>
<%@page import="org.zkoss.zul.Row"%>
<%@page import="org.zkoss.zul.Doublebox"%>

<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    JSONObject jsonResponse = new JSONObject();

    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        jsonResponse.put("status", "error");
        jsonResponse.put("message", Common.getBahasaConfig("Akses Ditolak. Sesi Anda telah habis."));
        out.print(jsonResponse.toString());
        return;
    }

    StringBuilder sb = new StringBuilder();
    BufferedReader reader = request.getReader();
    String line;
    while ((line = reader.readLine()) != null) {
        sb.append(line);
    }
    
    JSONObject payload = new JSONObject();
    try {
        if(sb.length() > 0) payload = new JSONObject(sb.toString());
    } catch(Exception e) {
        jsonResponse.put("status", "error").put("message", "Invalid Request Payload");
        out.print(jsonResponse.toString());
        return;
    }

    String action = payload.optString("action", "");
    Session sessionDB = null;
    
    Boolean khususUntukSiswa = request.getParameter("khususUntukSiswa") == null ? true : "true".equalsIgnoreCase(request.getParameter("khususUntukSiswa"));
    if (payload.has("khususUntukSiswa")) {
        khususUntukSiswa = payload.optBoolean("khususUntukSiswa");
    }

    try {
        if ("loadTagihan".equals(action)) {
            Long idSiswa = payload.optLong("idSiswa", 0);
            Integer bulan = payload.optInt("bulan", 1);
            Integer tahun = payload.optInt("tahun", 2024);
            boolean isBukanTagihan = payload.optBoolean("bukanTagihan", false);
            boolean isRefresh = payload.optBoolean("refresh", false);
            sessionDB = HibernateUtil.openSession();
            
            Siswa siswa = khususUntukSiswa ? (Siswa) ConstantValues.ambil(Siswa.class.getName(), idSiswa, true) : null;
            CalonSiswa calonSiswa = !khususUntukSiswa ? (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), idSiswa, true) : null;
            Sekolah sekolah = siswa != null ? siswa.getSekolah() : (calonSiswa != null ? calonSiswa.getSekolah() : null);

            Double tabungan = DepositHelper.hitungDeposit(siswa, calonSiswa);
            
            List<PengaturanBiaya> pBiayas = ConstantValues.simpleList(PengaturanBiaya
					.terapkanFilterPembayaran(sessionDB.createCriteria(PengaturanBiaya.class), siswa, calonSiswa)
                    .addOrder(Order.desc("id")).addOrder(Order.desc("jenisBiayaSekolah.periode")).addOrder(Order.asc("jenisBiayaSekolah.nama")), PengaturanBiaya.class);
            JSONArray tagihanArr = new JSONArray();

            for (PengaturanBiaya pb : pBiayas) {
                JenisBiayaSekolah jbs = pb.getJenisBiayaSekolah();
                if (pb.getAktif() && ((siswa != null && !jbs.getGunakanCalonSiswa() && DetailTagihanSiswaHelper.apakahAda(pb, siswa))
                        || (calonSiswa != null && jbs.getGunakanCalonSiswa() && DetailTagihanCalonSiswaHelper.apakahAda(pb, calonSiswa)))) {
                    
                    List<Tagihan> listTagihanLokal = jbs.getGunakanCalonSiswa()
                            ? TagihanUtilCalonSiswa.getTagihan(pb.getJenisBiayaSekolah(), pb, calonSiswa, bulan, tahun, isRefresh)
                            : TagihanUtil.getTagihan(pb.getJenisBiayaSekolah(), pb, siswa, bulan, tahun, isRefresh);
                    if (!listTagihanLokal.isEmpty()) {
                        boolean ada = false;
                        for (Tagihan tagihan : listTagihanLokal) {
                            if (((tagihan.getAktif() && (isBukanTagihan || !tagihan.ambilBukanTagihanData()))
                                    && !tagihan.getNominalBiaya().getBukanTagihan())
                                    && tagihan.getPembayaranSiswaDetail() == null) {
                                if (tagihan.getNominalBiaya().getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran() || tagihan.getNominal() > 0.1) {
                                    ada = true;
                                    break;
                                }
                            }
                        }

                        if (ada) {
                             for (Tagihan tagihan : listTagihanLokal) {
                                if (((tagihan.getAktif() && (isBukanTagihan || !tagihan.ambilBukanTagihanData()))
                                        && !tagihan.getNominalBiaya().getBukanTagihan())
                                        && tagihan.getPembayaranSiswaDetail() == null) {
                                    
                                    if (jbs != null && jbs.getPeriode().equalsIgnoreCase("Bulanan")) {
                                        if (tagihan.getPengaturanBiaya().getBulanMulai() != null && tagihan.getTahunbulan() < tagihan.getPengaturanBiaya().getBulanMulai()) continue;
                                        if (tagihan.getPengaturanBiaya().getBulanSampai() != null && tagihan.getTahunbulan() > tagihan.getPengaturanBiaya().getBulanSampai()) break;
                                    }

                                    JSONObject obj = new JSONObject();
                                    obj.put("id", tagihan.getId());
                                    obj.put("group", pb.toString());
                                    
                                    String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama() + (tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")" : "");
                                    if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode().equals("Harian")) {
                                        Calendar c = WaktuUtil.getCalendar();
                                        c.set(Calendar.DAY_OF_MONTH, tagihan.getBayarKe());
                                        c.set(Calendar.MONTH, Integer.parseInt((tagihan.getNominalBiaya().getTahunbulan() + "").substring(4)) - 1);
                                        c.set(Calendar.YEAR, Integer.parseInt((tagihan.getNominalBiaya().getTahunbulan() + "").substring(0, 4)));
                                        ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama() + " (" + Common.dateFormat41.get().format(c.getTime()) + ")";
                                    }
                                    
                                    obj.put("namaBiaya", ket);
                                    obj.put("periodeStr", (tagihan.getBulan() != null ? Common.BULAN[tagihan.getBulan()-1] + " " : "") + (tagihan.getTahun() != null ? tagihan.getTahun() : ""));
                                    obj.put("tahunBulanInt", tagihan.getTahunbulan() == null ? 0 : tagihan.getTahunbulan());
                                    obj.put("nominal", tagihan.getNominal());
                                    obj.put("denda", tagihan.getDenda() != null ? tagihan.getDenda() : 0.0);
                                    obj.put("diskon", tagihan.getDiskon() != null ? tagihan.getDiskon() : 0.0);
                                    obj.put("totalTampil", (tagihan.getNominal() + tagihan.getDenda()) - tagihan.getDiskon());
                                    
                                    obj.put("bisaUbahNominal", tagihan.getNominalBiaya().getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran());
                                    obj.put("wajibPilih", tagihan.getItemBiayaSekolah().getWajibPilih() != null ? tagihan.getItemBiayaSekolah().getWajibPilih() : false);
                                    obj.put("wajibPilihJikaBulanDipilih", tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih() != null ? tagihan.getItemBiayaSekolah().getWajibPilihJikaBulanDipilih() : false);
                                    
                                    obj.put("wajibDibayarSebelumnya", tagihan.getPengaturanBiaya().getWajibDibayarSebelumnya() != null ? tagihan.getPengaturanBiaya().getWajibDibayarSebelumnya() : "");
                                    obj.put("isTerakumulasiBulanan", tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getPilihanItemBiayaTerakumulasiBulanan());
                                    obj.put("idJenisBiaya", tagihan.getItemBiayaSekolah().getId());
                                    obj.put("dibayarSebanyak", tagihan.getNominalBiaya().getDibayarSebayak() != null ? tagihan.getNominalBiaya().getDibayarSebayak() : 1);
                                    obj.put("bayarKe", tagihan.getBayarKe() != null ? tagihan.getBayarKe() : 1);
                                    obj.put("periode", tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode());
                                    obj.put("tahunAjaran", tagihan.getTahunAjaran() != null ? tagihan.getTahunAjaran() : "");
                                    Long harusBayarId = null;
                                    if(tagihan.getItemBiayaSekolah().getHarusBayar() != null) {
                                        harusBayarId = tagihan.getItemBiayaSekolah().getHarusBayar().getId();
                                    }
                                    obj.put("harusBayarId", harusBayarId);
                                    obj.put("va", tagihan.getVa() != null ? tagihan.getVa() : "");
                                    obj.put("tglDeadline", tagihan.getTanggalDeadline() != null ? Common.dateFormat4.get().format(tagihan.getTanggalDeadline()) : "");
                                    boolean bisaDiangsur = tagihan.getNominalBiaya().getItemBiayaSekolah().getBolehDiangsur() 
                                            && tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getBolehAngsurBerapapun()
                                            && tagihan.getNominalBiaya().getDibayarSebayak().intValue() == tagihan.getBayarKe().intValue() 
                                            && !tagihan.getItemBiayaSekolah().getWajibPilih() 
                                            && !tagihan.getNominalBiaya().getItemBiayaSekolah().getAngsuranSeragam() 
                                            && tagihan.getKunci() == null && tagihan.getPengaturanBiaya().getKunci() == null;
                                    boolean bisaHapusAngsuran = tagihan.getNominalBiaya().getDibayarSebayak().intValue() > 1 
                                            && !tagihan.getItemBiayaSekolah().getWajibPilih() 
                                            && !tagihan.getNominalBiaya().getItemBiayaSekolah().getAngsuranSeragam()
                                            && tagihan.getKunci() == null && tagihan.getPengaturanBiaya().getKunci() == null;
                                    obj.put("bisaDiangsur", bisaDiangsur);
                                    obj.put("bisaHapusAngsuran", bisaHapusAngsuran);
                                    obj.put("maksAngsuran", (tagihan.getNominal() + tagihan.getDenda()) - tagihan.ambilDiskonTanpaDikonBayarSatuKali());

                                    tagihanArr.put(obj);
                                }
                            }
                        }
                    }
                }
            
            }

            JSONArray metodeArr = new JSONArray();
            if(Common.getApakahAdmin() && sekolah != null) {
                List<AkunPembayaranSiswa> akunTunai = sessionDB.createCriteria(AkunPembayaranSiswa.class)
                    .add(Restrictions.eq("sekolah", sekolah)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.or(Restrictions.eq("manual", true), Restrictions.eq("dariTabungan", true))).addOrder(Order.asc("nama")).list();
                for(AkunPembayaranSiswa ak : akunTunai) {
                    JSONObject m = new JSONObject();
                    m.put("id", ak.getId()); m.put("nama", "Tunai " + ak.getNama()); m.put("type", "TUNAI"); 
                    m.put("fee", Double.parseDouble(Common.getKonfigurasi(ak.getId() + "_biaya_administrasi", "0.0").getNilai()));
                    metodeArr.put(m);
                }
            }
            
            JSONArray jpArr = new JSONArray();
            ais.database.model.rab.SatuanKerja sk = Common.getSatuanKerja();
            List<JenisPembayaran> listJp = sessionDB.createCriteria(JenisPembayaran.class)
                    .add(sk == null ? Restrictions.sqlRestriction("true") : Restrictions.or(Restrictions.isNull("satuanKerja"), Restrictions.eq("satuanKerja", sk)))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")).list();
            for(JenisPembayaran jp : listJp) {
                JSONObject objJp = new JSONObject();
                objJp.put("id", jp.getId());
                objJp.put("nama", jp.getNama() + (jp.getBank() != null ? " (" + jp.getBank().getNama() + ")" : ""));
                jpArr.put(objJp);
            }
            
            String[] banks = {"bri", "bni", "bsi", "bank_btn"};
            int[] bankType = {1, 2, 3, 4};
            for(int i=0; i<banks.length; i++) {
                boolean aktif = Common.getKonfigurasi("aktifkan_pembayaran_via_" + banks[i], Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);
                if(i > 0) aktif = aktif && Common.getKonfigurasi("aktifkan_pembayaran_via_" + banks[i] + "_sekolah_" + (sekolah == null ? "" : sekolah.getId()), Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF);
                if(aktif) {
                    JSONObject mo = new JSONObject();
                    mo.put("id", "BANK_" + bankType[i]); mo.put("nama", "Via " + banks[i].toUpperCase().replace("BANK_", "")); mo.put("type", "DIRECT_BANK_" + bankType[i]);
                    mo.put("fee", Double.parseDouble(Common.getKonfigurasi((i == 3 ? "btn" : banks[i]) + "_biaya_administrasi", "0.0").getNilai()));
                    metodeArr.put(mo);
                }
            }

            if(Common.getKonfigurasi("aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                JSONObject mo = new JSONObject();
                mo.put("id", "ONLINE"); mo.put("nama", "Online Payment"); mo.put("type", "GATEWAY"); mo.put("fee", Double.parseDouble(Common.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai()));
                metodeArr.put(mo);
            }

            jsonResponse.put("status", "00");
            jsonResponse.put("tagihan", tagihanArr);
            jsonResponse.put("jenisPembayaran", jpArr);
            jsonResponse.put("tabungan", tabungan);
            jsonResponse.put("metodeBayar", metodeArr);

        } else if ("loadDasbor".equals(action)) {
            sessionDB = HibernateUtil.openSession();
            Long idSiswa = payload.optLong("idSiswa", 0);
            Siswa siswa = khususUntukSiswa ? (Siswa) ConstantValues.ambil(Siswa.class.getName(), idSiswa, true) : null;
            CalonSiswa calonSiswa = !khususUntukSiswa ? (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), idSiswa, true) : null;

            Double totalBayar = 0.0;
            JSONArray metodeChartArr = new JSONArray();
            try {
                String colPelanggan = khususUntukSiswa ? "siswa" : "calonSiswa";
                Object objPelanggan = khususUntukSiswa ? siswa : calonSiswa;
                String hqlTotal = "SELECT SUM(p.nominal) FROM PembayaranSiswa p WHERE p." + colPelanggan + " = :pelanggan";
                Number sumTotal = (Number) sessionDB.createQuery(hqlTotal).setParameter("pelanggan", objPelanggan).uniqueResult();
                if (sumTotal != null) totalBayar = sumTotal.doubleValue();
                String hqlMetode = "SELECT a.nama, SUM(p.nominal) FROM PembayaranSiswa p LEFT JOIN p.akunPembayaranSiswa a WHERE p."
                        + colPelanggan + " = :pelanggan GROUP BY a.nama";
                List<Object[]> listMetode = sessionDB.createQuery(hqlMetode).setParameter("pelanggan", objPelanggan).list();
                for (Object[] row : listMetode) {
                    JSONObject obj = new JSONObject();
                    obj.put("label", row[0] != null ? row[0].toString() : "Lainnya");
                    obj.put("val", row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
                    metodeChartArr.put(obj);
                }
            } catch(Exception ex) {
                System.out.println("Gagal load statistik dasbor HQL: " + ex.getMessage());
            }

            jsonResponse.put("status", "00");
            jsonResponse.put("totalBayar", totalBayar);
            jsonResponse.put("komposisiMetode", metodeChartArr);

        // =========================================================================
        // PERUBAHAN LOGIKA ACTION loadRiwayat: MENGGUNAKAN PembayaranSiswaDetail
        // =========================================================================
        } else if ("loadRiwayat".equals(action)) {
            sessionDB = HibernateUtil.openSession();
            Long idSiswa = payload.optLong("idSiswa", 0);
            Siswa siswa = khususUntukSiswa ? (Siswa) ConstantValues.ambil(Siswa.class.getName(), idSiswa, true) : null;
            CalonSiswa calonSiswa = !khususUntukSiswa ? (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), idSiswa, true) : null;
            boolean isBukanTagihan = payload.optBoolean("bukanTagihan", false);
            
            Date startDate = null;
            Date endDate = null;
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                if (payload.has("riwayatStartDate") && !payload.optString("riwayatStartDate").isEmpty()) {
                    startDate = sdf.parse(payload.optString("riwayatStartDate"));
                } else {
                    Calendar cal3 = Calendar.getInstance();
                    cal3.add(Calendar.YEAR, -3);
                    startDate = cal3.getTime();
                }
                
                if (payload.has("riwayatEndDate") && !payload.optString("riwayatEndDate").isEmpty()) {
                    endDate = sdf.parse(payload.optString("riwayatEndDate"));
                } else {
                    endDate = new Date();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayar/_pembayaran_online_services.jsp:299");}
            
            Integer rBulan = payload.optInt("riwayatBulan", 0);
            Integer rTahun = payload.optInt("riwayatTahun", 0);
            String rKeyword = payload.optString("riwayatKeyword", "");

            Criteria critHist = sessionDB.createCriteria(PembayaranSiswaDetail.class, "psd")
                    .createAlias("psd.itemBiayaSekolah", "itemBiayaSekolah")
                    .createAlias("psd.tagihan", "tagihan", Criteria.LEFT_JOIN)
                    .createAlias("tagihan.pengaturanBiaya", "pengaturanBiaya", Criteria.LEFT_JOIN)
                    .createAlias("psd.pembayaranSiswa", "pembayaranSiswa")
                    .addOrder(Order.desc("pengaturanBiaya.id"))
                    .addOrder(Order.asc("itemBiayaSekolah.nama"))
                    .addOrder(Order.asc("tagihan.tahunbulan"))
                    .addOrder(Order.asc("tagihan.bayarKe"))
                    .addOrder(Order.desc("psd.id"))
                    .add(siswa != null ? Restrictions.eq("pembayaranSiswa.siswa", siswa) : Restrictions.eq("pembayaranSiswa.calonSiswa", calonSiswa));

            if (startDate != null) critHist.add(Restrictions.ge("pembayaranSiswa.tanggal", startDate));
            if (endDate != null) {
                Calendar cEnd = Calendar.getInstance();
                cEnd.setTime(endDate);
                cEnd.set(Calendar.HOUR_OF_DAY, 23); cEnd.set(Calendar.MINUTE, 59); cEnd.set(Calendar.SECOND, 59);
                critHist.add(Restrictions.le("pembayaranSiswa.tanggal", cEnd.getTime()));
            }
            if (rBulan > 0) critHist.add(Restrictions.eq("tagihan.bulan", rBulan));
            if (rTahun > 0) critHist.add(Restrictions.eq("tagihan.tahun", rTahun));
            if (!rKeyword.isEmpty()) {
                critHist.add(Restrictions.or(
                    Restrictions.ilike("itemBiayaSekolah.nama", "%" + rKeyword + "%"),
                    Restrictions.ilike("itemBiayaSekolah.kode", "%" + rKeyword + "%")
                ));
            }

            List<PembayaranSiswaDetail> listTagihanHist = critHist.list();
            JSONArray riwayatArr = new JSONArray();
            boolean bolehEdit = (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) ||
                    Common.getApakahAdmin();

            for (PembayaranSiswaDetail pDetail : listTagihanHist) {
                Tagihan tagihan = pDetail.getTagihan();
                if (tagihan != null) {
                    if (((tagihan.getAktif() && (isBukanTagihan || !tagihan.ambilBukanTagihanData())) && !tagihan.getNominalBiaya().getBukanTagihan())) {
                        PembayaranSiswa pemb = pDetail.getPembayaranSiswa();
                        JSONObject rObj = new JSONObject();
                        // Menggunakan PengaturanBiaya toString() sebagai ID Group untuk UI Header
                        rObj.put("groupId", tagihan.getPengaturanBiaya().toString());
                        rObj.put("waktu", Common.dateFormat3.get().format(pemb.getTanggal()));
                        rObj.put("bln", (tagihan.getBulan() == null ? "" : Common.BULAN[tagihan.getBulan() - 1] + " ") + (tagihan.getTahun() == null ? "" : tagihan.getTahun()));
                        
                        String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama() + (tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")" : "");
                        if (tagihan.getDenda() != null && tagihan.getDenda() > 0.01) ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
                        if (tagihan.getTanggalDeadline() != null) ket += ", Deadline " + Common.dateFormat4.get().format(tagihan.getTanggalDeadline());
                        if (tagihan.getDiskonSiswa() != null && tagihan.getDiskonSiswa().getMemotongTagihan()) ket += " - " + tagihan.getDiskonSiswa().getNama();
                        try {
                            ais.database.model.sekolah.PengaturanBiayaItemBiaya pbItem = tagihan.getNominalBiaya().getPengaturanBiayaItemBiaya();
                            if ((pbItem == null || tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? 0.0 : pbItem.getDiskonBiaya()) > 0.1) {
                                ket += ", Diskon dibayar lunas 1x";
                            }
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayar/_pembayaran_online_services.jsp:358");}
                        
                        rObj.put("item", ket);
                        rObj.put("via", pemb.getAkunPembayaranSiswa() == null ? "" : pemb.getAkunPembayaranSiswa().getNama());
                        
                        Double n = pDetail.ambilNominal();
                        Double denda = tagihan.getDenda() != null ? tagihan.getDenda() : 0.0;
                        Double diskon = tagihan.getDiskon() != null ? tagihan.getDiskon() : 0.0;
                        Double n1 = (n + diskon + denda);
                        
                        rObj.put("nominal", n);
                        rObj.put("nominalCoret", (diskon > 0.01 && n1 > n) ? n1 : 0.0);
                        rObj.put("idPembayaran", pemb.getId());
                        
                        boolean bisaEdit = pemb.getVirtualAccountBank() == null && bolehEdit;
                        rObj.put("bisaEdit", bisaEdit);
                        
                        riwayatArr.put(rObj);
                    }
                }
            }
            jsonResponse.put("status", "00");
            jsonResponse.put("riwayat", riwayatArr);

        } else if ("loadDashboardGlobal".equals(action)) {
            sessionDB = HibernateUtil.openSession();
            Long idYayasan = payload.optLong("idYayasan", 0);
            Long idSekolah = payload.optLong("idSekolah", 0);
            boolean isSiswa = payload.optBoolean("isSiswa", true);
            String keywordPelanggan = payload.optString("keywordPelanggan", "");
            String keywordItem = payload.optString("keywordItem", "");
            String startDateStr = payload.optString("startDate", "");
            String endDateStr = payload.optString("endDate", "");

            Tbmuser currentUser = Common.getCurrentUser(request);
            StringBuilder whereClause = new StringBuilder();
            Map<String, Object> params = new HashMap<String, Object>();
            if (currentUser.ambilSekolah() != null) {
                whereClause.append("AND sek.id = :idSekolah ");
                params.put("idSekolah", currentUser.ambilSekolah().getId());
            } else if (currentUser.ambilYayasan() != null) {
                whereClause.append("AND sek.yayasan.id = :idYayasan ");
                params.put("idYayasan", currentUser.ambilYayasan().getId());
            } else {
                if (idSekolah > 0) { whereClause.append("AND sek.id = :idSekolah ");
                params.put("idSekolah", idSekolah); }
                else if (idYayasan > 0) { whereClause.append("AND sek.yayasan.id = :idYayasan ");
                params.put("idYayasan", idYayasan); }
            }

            if (!keywordPelanggan.isEmpty()) {
                if (isSiswa) whereClause.append("AND (lower(s.nama) LIKE :kw OR lower(s.nomorInduk) LIKE :kw) ");
                else whereClause.append("AND (lower(cs.nama) LIKE :kw OR lower(cs.noRegistrasi) LIKE :kw) ");
                params.put("kw", "%" + keywordPelanggan.toLowerCase() + "%");
            }
            if (!keywordItem.isEmpty()) {
                whereClause.append("AND (lower(ibs.nama) LIKE :kwItem OR lower(ibs.kode) LIKE :kwItem) ");
                params.put("kwItem", "%" + keywordItem.toLowerCase() + "%");
            }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            if (!startDateStr.isEmpty()) {
                whereClause.append("AND ps.tanggal >= :startDate ");
                params.put("startDate", sdf.parse(startDateStr));
            }
            if (!endDateStr.isEmpty()) {
                whereClause.append("AND ps.tanggal <= :endDate ");
                Calendar cEnd = Calendar.getInstance(); 
                cEnd.setTime(sdf.parse(endDateStr));
                cEnd.set(Calendar.HOUR_OF_DAY, 23); cEnd.set(Calendar.MINUTE, 59); cEnd.set(Calendar.SECOND, 59);
                params.put("endDate", cEnd.getTime());
            }

            String joinClause = "FROM PembayaranSiswaDetail psd JOIN psd.pembayaranSiswa ps JOIN psd.tagihan tag JOIN tag.itemBiayaSekolah ibs LEFT JOIN ps.akunPembayaranSiswa akun ";
            if (isSiswa) joinClause += "JOIN ps.siswa s LEFT JOIN s.sekolah sek ";
            else joinClause += "JOIN ps.calonSiswa cs LEFT JOIN cs.sekolah sek ";
            String hqlTotal = "SELECT SUM(psd.nominal) " + joinClause + " WHERE 1=1 " + whereClause.toString();
            org.hibernate.Query qTotal = sessionDB.createQuery(hqlTotal);
            for(Map.Entry<String, Object> entry : params.entrySet()) qTotal.setParameter(entry.getKey(), entry.getValue());
            Number totalSumObj = (Number) qTotal.uniqueResult();
            Double totalNominal = totalSumObj != null ? totalSumObj.doubleValue() : 0.0;

            String hqlRaw = "SELECT ps.tanggal, psd.nominal, akun.nama, sek.nama, ibs.nama " + joinClause + " WHERE 1=1 " + whereClause.toString() + " ORDER BY ps.tanggal ASC";
            org.hibernate.Query qRaw = sessionDB.createQuery(hqlRaw);
            for(Map.Entry<String, Object> entry : params.entrySet()) qRaw.setParameter(entry.getKey(), entry.getValue());
            List<Object[]> rawData = qRaw.list();
            Map<String, Double> mapMetode = new HashMap<>();
            Map<String, Double> mapTrend = new TreeMap<>();
            Map<String, Double> mapSekolah = new HashMap<>();
            java.text.SimpleDateFormat sdfTrend = new java.text.SimpleDateFormat("yyyy-MM"); 
            
            JSONArray exportArr = new JSONArray();
            for (Object[] row : rawData) {
                Date tgl = (Date) row[0];
                Double nom = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                String mtd = row[2] != null ? row[2].toString() : "Lainnya";
                String skl = row[3] != null ? row[3].toString() : "Tanpa Sekolah";
                String itm = row[4] != null ? row[4].toString() : "-";
                
                String trndStr = sdfTrend.format(tgl);
                mapMetode.put(mtd, mapMetode.getOrDefault(mtd, 0.0) + nom);
                mapTrend.put(trndStr, mapTrend.getOrDefault(trndStr, 0.0) + nom);
                mapSekolah.put(skl, mapSekolah.getOrDefault(skl, 0.0) + nom);

                JSONObject exObj = new JSONObject();
                exObj.put("Tanggal", Common.dateFormat3.get().format(tgl));
                exObj.put("Nominal", nom);
                exObj.put("Metode", mtd);
                exObj.put("Sekolah", skl);
                exObj.put("Item Biaya", itm);
                exportArr.put(exObj);
            }

            JSONArray arrMetode = new JSONArray();
            for(Map.Entry<String, Double> entry : mapMetode.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("label", entry.getKey()); o.put("val", entry.getValue()); arrMetode.put(o);
            }

            JSONArray arrTrend = new JSONArray();
            for(Map.Entry<String, Double> entry : mapTrend.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("label", entry.getKey()); o.put("val", entry.getValue()); arrTrend.put(o);
            }
            
            JSONArray arrSekolah = new JSONArray();
            for(Map.Entry<String, Double> entry : mapSekolah.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("label", entry.getKey()); o.put("val", entry.getValue()); arrSekolah.put(o);
            }

            jsonResponse.put("status", "00");
            jsonResponse.put("totalNominal", totalNominal);
            jsonResponse.put("chartMetode", arrMetode);
            jsonResponse.put("chartTrend", arrTrend);
            jsonResponse.put("chartSekolah", arrSekolah);
            jsonResponse.put("exportData", exportArr);
            out.print(jsonResponse.toString());
            return;

        // =========================================================================
        // PERUBAHAN LOGIKA ACTION loadRiwayatGlobal: DISAMAKAN DENGAN loadRiwayat
        // =========================================================================
        } else if ("loadRiwayatGlobal".equals(action)) {
            sessionDB = HibernateUtil.openSession();
            Long idYayasan = payload.optLong("idYayasan", 0);
            Long idSekolah = payload.optLong("idSekolah", 0);
            boolean isSiswa = payload.optBoolean("isSiswa", true);
            String keywordPelanggan = payload.optString("keywordPelanggan", "");
            String keywordItem = payload.optString("keywordItem", "");
            String startDateStr = payload.optString("startDate", "");
            String endDateStr = payload.optString("endDate", "");
            int rBulan = payload.optInt("bulan", 0);
            int rTahun = payload.optInt("tahun", 0);
            int pageNo = payload.optInt("page", 1);
            int limit = 15;
            int start = (pageNo - 1) * limit;

            Tbmuser currentUser = Common.getCurrentUser(request);
            StringBuilder hql = new StringBuilder("SELECT DISTINCT ps.id FROM PembayaranSiswaDetail psd JOIN psd.pembayaranSiswa ps JOIN psd.tagihan tag JOIN tag.itemBiayaSekolah ibs ");
            if (isSiswa) hql.append("JOIN ps.siswa s LEFT JOIN s.sekolah sek ");
            else hql.append("JOIN ps.calonSiswa cs LEFT JOIN cs.sekolah sek ");
            hql.append("WHERE 1=1 ");

            Map<String, Object> params = new HashMap<String, Object>();
            if (currentUser.ambilSekolah() != null) {
                hql.append("AND sek.id = :idSekolah ");
                params.put("idSekolah", currentUser.ambilSekolah().getId());
            } else if (currentUser.ambilYayasan() != null) {
                hql.append("AND sek.yayasan.id = :idYayasan ");
                params.put("idYayasan", currentUser.ambilYayasan().getId());
            } else {
                if (idSekolah > 0) { hql.append("AND sek.id = :idSekolah ");
                params.put("idSekolah", idSekolah); }
                else if (idYayasan > 0) { hql.append("AND sek.yayasan.id = :idYayasan ");
                params.put("idYayasan", idYayasan); }
            }

            if (!keywordPelanggan.isEmpty()) {
                if (isSiswa) hql.append("AND (lower(s.nama) LIKE :kw OR lower(s.nomorInduk) LIKE :kw) ");
                else hql.append("AND (lower(cs.nama) LIKE :kw OR lower(cs.noRegistrasi) LIKE :kw) ");
                params.put("kw", "%" + keywordPelanggan.toLowerCase() + "%");
            }
            if (!keywordItem.isEmpty()) {
                hql.append("AND (lower(ibs.nama) LIKE :kwItem OR lower(ibs.kode) LIKE :kwItem) ");
                params.put("kwItem", "%" + keywordItem.toLowerCase() + "%");
            }
            if (rBulan > 0) { hql.append("AND tag.bulan = :bulan ");
            params.put("bulan", rBulan); }
            if (rTahun > 0) { hql.append("AND tag.tahun = :tahun ");
            params.put("tahun", rTahun); }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            if (!startDateStr.isEmpty()) {
                hql.append("AND ps.tanggal >= :startDate ");
                params.put("startDate", sdf.parse(startDateStr));
            }
            if (!endDateStr.isEmpty()) {
                hql.append("AND ps.tanggal <= :endDate ");
                Calendar cEnd = Calendar.getInstance(); 
                cEnd.setTime(sdf.parse(endDateStr));
                cEnd.set(Calendar.HOUR_OF_DAY, 23); cEnd.set(Calendar.MINUTE, 59); cEnd.set(Calendar.SECOND, 59);
                params.put("endDate", cEnd.getTime());
            }

            String hqlCount = hql.toString().replace("SELECT DISTINCT ps.id", "SELECT COUNT(DISTINCT ps.id)");
            hql.append("ORDER BY ps.id DESC");

            org.hibernate.Query qId = sessionDB.createQuery(hql.toString());
            org.hibernate.Query qCount = sessionDB.createQuery(hqlCount);
            for(Map.Entry<String, Object> entry : params.entrySet()) {
                qId.setParameter(entry.getKey(), entry.getValue());
                qCount.setParameter(entry.getKey(), entry.getValue());
            }

            long totalCount = ((Number) qCount.uniqueResult()).longValue();
            
            qId.setFirstResult(start);
            qId.setMaxResults(limit);
            List<Long> psIds = qId.list();

            JSONArray dataArr = new JSONArray();
            if (!psIds.isEmpty()) {
                List<PembayaranSiswaDetail> details = sessionDB.createCriteria(PembayaranSiswaDetail.class, "psd")
                    .createAlias("psd.pembayaranSiswa", "ps")
                    .createAlias("psd.itemBiayaSekolah", "itemBiayaSekolah")
                    .createAlias("psd.tagihan", "tagihan", Criteria.LEFT_JOIN)
                    .createAlias("tagihan.pengaturanBiaya", "pengaturanBiaya", Criteria.LEFT_JOIN)
                    .createAlias("ps.siswa", "s", Criteria.LEFT_JOIN)
                    .createAlias("s.sekolah", "sek_s", Criteria.LEFT_JOIN)
                    .createAlias("ps.calonSiswa", "cs", Criteria.LEFT_JOIN)
                    .createAlias("cs.sekolah", "sek_cs", Criteria.LEFT_JOIN)
                    .add(Restrictions.in("ps.id", psIds))
                    .addOrder(Order.desc("ps.id"))
                    // Sort order inner match 
                    .addOrder(Order.desc("pengaturanBiaya.id"))
                    .addOrder(Order.asc("itemBiayaSekolah.nama"))
                    .addOrder(Order.asc("tagihan.tahunbulan"))
                    .addOrder(Order.asc("tagihan.bayarKe"))
                    .addOrder(Order.desc("psd.id"))
                    .list();
                    
                boolean bolehEdit = (currentUser != null && currentUser.getMahasiswa() == null && currentUser.getSiswa() == null && currentUser.getCalonSiswa() == null) || Common.getApakahAdmin();
                for (PembayaranSiswaDetail psd : details) {
                    PembayaranSiswa ps = psd.getPembayaranSiswa();
                    Tagihan tag = psd.getTagihan();
                    Siswa s = ps.getSiswa();
                    CalonSiswa cs = ps.getCalonSiswa();

                    JSONObject rObj = new JSONObject();
                    rObj.put("groupId", ps.getId().toString()); // Tetap grouping per struk ID Pembayaran untuk UI Global
                    rObj.put("waktu", Common.dateFormat3.get().format(ps.getTanggal()));
                    rObj.put("via", ps.getAkunPembayaranSiswa() != null ? ps.getAkunPembayaranSiswa().getNama() : "Online");
                    rObj.put("idPembayaran", ps.getId());

                    String nis = s != null ? s.getNomorInduk() : (cs != null ? cs.getNoRegistrasi() : "-");
                    String nama = s != null ? s.getNama() : (cs != null ? cs.getNama() : "-");
                    Sekolah sk = s != null ? s.getSekolah() : (cs != null ? cs.getSekolah() : null);
                    String namaSekolah = sk != null ? sk.getNama() : "-";
                    String fotoUrl = Common.ROOT + "/img/no_avatar.png";
                    try {
                        if(s != null) {
                            FileFotoLain fileFotoLain = FileFotoLain.ambil(false, s.getId(), FotoSiswa.DEFAULT_JENIS, FotoSiswa.class);
                            fotoUrl = fileFotoLain.createLinkUri();
                        } else if (cs != null) {
                             FileFotoLain fileFotoLain = FileFotoLain.ambil(false, cs.getId(), FotoCalonSiswa.DEFAULT_JENIS, FotoCalonSiswa.class);
                            fotoUrl = fileFotoLain.createLinkUri();
                        }
                    } catch (Exception ef) { ais.common.ErrorAuditUtil.record(ef, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayar/_pembayaran_online_services.jsp:626");}

                    rObj.put("nis", nis);
                    rObj.put("namaSiswa", nama);
                    rObj.put("sekolah", namaSekolah);
                    rObj.put("foto", fotoUrl);

                    String blnStr = (tag.getBulan() == null ? "" : Common.BULAN[tag.getBulan() - 1] + " ") + (tag.getTahun() == null ? "" : tag.getTahun());
                    rObj.put("bln", blnStr);

                    String ket = tag.getNominalBiaya().getItemBiayaSekolah().getNama() + (tag.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tag.getBayarKe() + ")" : "");
                    if (tag.getDenda() != null && tag.getDenda() > 0.01) ket += ", Denda " + Common.numberFormat.get().format(tag.getDenda());
                    if (tag.getTanggalDeadline() != null) ket += ", Deadline " + Common.dateFormat4.get().format(tag.getTanggalDeadline());
                    if (tag.getDiskonSiswa() != null && tag.getDiskonSiswa().getMemotongTagihan()) ket += " - " + tag.getDiskonSiswa().getNama();
                    try {
                        ais.database.model.sekolah.PengaturanBiayaItemBiaya pbItem = tag.getNominalBiaya().getPengaturanBiayaItemBiaya();
                        if ((pbItem == null || tag.getNominalBiaya().getDibayarSebayak() > 1 ? 0.0 : pbItem.getDiskonBiaya()) > 0.1) {
                            ket += ", Diskon dibayar lunas 1x";
                        }
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayar/_pembayaran_online_services.jsp:645");}
                    
                    rObj.put("item", ket);
                    rObj.put("pengaturanBiaya", tag.getPengaturanBiaya().toString());
                    
                    Double n = psd.getNominal();
                    Double denda = tag.getDenda() != null ? tag.getDenda() : 0.0;
                    Double diskon = tag.getDiskon() != null ? tag.getDiskon() : 0.0;
                    Double n1 = (n + diskon + denda);
                    
                    rObj.put("nominal", n);
                    rObj.put("nominalCoret", (diskon > 0.01 && n1 > n) ? n1 : 0.0);
                    rObj.put("bisaEdit", ps.getVirtualAccountBank() == null && bolehEdit);

                    dataArr.put(rObj);
                }
            }

            jsonResponse.put("status", "00");
            jsonResponse.put("data", dataArr);
            jsonResponse.put("total", totalCount);
            jsonResponse.put("totalPages", Math.ceil((double) totalCount / limit));

        } else if ("updateNominal".equals(action)) {
//... sisa kode (simpanAngsuran, hapusAngsuran, dll) tetap mengikuti versi sebelumnya tanpa perubahan.
            sessionDB = HibernateUtil.openSession();
            sessionDB.getTransaction().begin();
            Tagihan t = (Tagihan) ConstantValues.ambil(Tagihan.class.getName(), payload.optLong("idTagihan"), true);
            NominalBiaya nb = t.getNominalBiaya();
            nb.setNominal(payload.optDouble("nominal"));
            Common.refreshUpdate(sessionDB, nb);
            sessionDB.getTransaction().commit();
            jsonResponse.put("status", "00");
        } else if ("simpanAngsuran".equals(action)) {
            sessionDB = HibernateUtil.openSession();
            sessionDB.getTransaction().begin();
            Tagihan t = (Tagihan) ConstantValues.ambil(Tagihan.class.getName(), payload.optLong("idTagihan"), true);
            Double bayar = payload.optDouble("nominalBayar");
            String info = payload.optString("informasi");
            
            NominalBiaya nb = t.getNominalBiaya();
            int jml = nb.getDibayarSebayak() != null ? nb.getDibayarSebayak() : 1;
            Double diskon = t.ambilDiskonTanpaDikonBayarSatuKali();
            Double tagihanPenuh = (t.getNominal() + t.getDenda()) - diskon;
            Double sisa = tagihanPenuh - bayar;

            nb.setDibayarSebayakManual(jml + 1);
            nb.setDibayarSebayak(jml + 1);
            Common.refreshUpdate(sessionDB, nb);

            t.setDiskonManual(diskon);
            t.setNominal(bayar + diskon);
            t.setDibayarManual(bayar + diskon);
            t.setAktifkanmanual(true);
            Common.refreshUpdate(sessionDB, t);
            if(sisa > 0.1) {
                Tagihan tBaru = new Tagihan();
                tBaru.setNominalBiaya(nb);
                tBaru.setBulan(nb.getPengaturanBiaya().getJenisBiayaSekolah().getUntukBulan());
                tBaru.setTahun(nb.getPengaturanBiaya().getJenisBiayaSekolah().getUntukTahun());
                tBaru.setSiswa(nb.getSiswa());
                tBaru.setCalonSiswa(nb.getCalonSiswa());
                tBaru.setItemBiayaSekolah(t.getItemBiayaSekolah());
                tBaru.setBayarKe(jml + 1);
                tBaru.setNominal(sisa);
                tBaru.setAktifkanmanual(true);
                tBaru.setInformasi(info);
                sessionDB.save(tBaru);
            }
            sessionDB.getTransaction().commit();
            jsonResponse.put("status", "00");
        } else if ("hapusAngsuran".equals(action)) {
            sessionDB = HibernateUtil.openSession();
            sessionDB.getTransaction().begin();
            Tagihan t = (Tagihan) ConstantValues.ambil(Tagihan.class.getName(), payload.optLong("idTagihan"), true);
            NominalBiaya nb = t.getNominalBiaya();

            Tagihan tAkhir = (Tagihan) sessionDB.createCriteria(Tagihan.class).add(Restrictions.eq("nominalBiaya", nb)).add(Restrictions.gt("bayarKe", 1)).addOrder(Order.desc("bayarKe")).setMaxResults(1).uniqueResult();
            if(tAkhir != null && !tAkhir.getId().equals(t.getId())) {
                tAkhir.setNominal(tAkhir.getNominal() + t.getNominal());
                Common.refreshUpdate(sessionDB, tAkhir);
            }
            Common.refreshDelete(sessionDB, t);
            List<Tagihan> tList = sessionDB.createCriteria(Tagihan.class).add(Restrictions.gt("nominal", 0.1)).add(Restrictions.eq("nominalBiaya", nb)).addOrder(Order.asc("id")).list();
            int index = 1;
            for (Tagihan tData : tList) {
                tData.setBayarKe(index++);
                Common.refreshUpdate(sessionDB, tData);
            }
            nb.setDibayarSebayakManual(index - 1); nb.setDibayarSebayak(index - 1);
            Common.refreshUpdate(sessionDB, nb);

            sessionDB.getTransaction().commit();
            jsonResponse.put("status", "00");

        } else if ("refreshDataSiswa".equals(action)) {
            sessionDB = HibernateUtil.openSession();
            Long idSiswa = payload.optLong("idSiswa");
            
            Siswa s = khususUntukSiswa ? (Siswa) ConstantValues.ambil(Siswa.class.getName(), idSiswa, true) : null;
            CalonSiswa cs = !khususUntukSiswa ? (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), idSiswa, true) : null;
            
   
            jsonResponse.put("status", "00");
        } else if ("cetakStrukUlang".equals(action)) {
            Long idPembayaran = payload.optLong("idPembayaran");
            PembayaranSiswa pemb = (PembayaranSiswa) ConstantValues.ambil(PembayaranSiswa.class.getName(), idPembayaran, true);
            if (pemb != null) {
                PembayaranSiswaUtil.cetakStruk(pemb);
                return; 
            } else {
                jsonResponse.put("status", "error").put("message", Common.getBahasaConfig("Data riwayat pembayaran tidak ditemukan."));
            }

        } else if ("prosesSuratTagihan".equals(action)) {
            sessionDB = HibernateUtil.openSession();
            Siswa siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(), payload.optLong("idSiswa"), true);
            JSONArray ids = payload.optJSONArray("tagihanIds");
            if (siswa != null && ids != null && ids.length() > 0) {
                Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
                parameters.put("biodata_id", siswa.getId());
                
                java.text.SimpleDateFormat sdfHtml = new java.text.SimpleDateFormat("yyyy-MM-dd");
                
                String tanggalSurat = payload.optString("tanggalSurat");
                if(!tanggalSurat.isEmpty()) parameters.put("tanggal", sdfHtml.parse(tanggalSurat));
                else parameters.put("tanggal", WaktuUtil.getDate());
                String tanggalJatuhTempo = payload.optString("tanggalJatuhTempo");
                if(!tanggalJatuhTempo.isEmpty()) parameters.put("tanggalJatuhTempo", sdfHtml.parse(tanggalJatuhTempo));
                else parameters.put("tanggalJatuhTempo", WaktuUtil.getDate());
                
                parameters.put("nomor", payload.optString("nomorSurat"));
                
                Long caraBayarId = payload.optLong("caraPembayaranId", 0);
                if (caraBayarId > 0) {
                    JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) ConstantValues.ambil(JenisPembayaran.class.getName(), caraBayarId, true);
                    if (pilihanJenisPembayaran != null) {
                        if (pilihanJenisPembayaran.getBank() != null) {
                            parameters.put("rekening_pembayaran", " melalui Bank " + pilihanJenisPembayaran.getBank().getNama() + "</b>");
                        } else {
                            parameters.put("rekening_pembayaran", " melalui  " + pilihanJenisPembayaran.getNama());
                        }
                    }
                } else {
                    parameters.put("rekening_pembayaran", "");
                }
                
                parameters.put("catatan", payload.optString("catatan"));
                parameters.put("prosentaseDenda", payload.optDouble("denda"));
                parameters.put("nama", siswa.getNama());
                parameters.put("nim", siswa.getNim());
                parameters.put("tahun", payload.optInt("tahun"));
                parameters.put("bulan", payload.optInt("bulan"));
                parameters.put("jenis_tagihan", "SISWA");
                
                String namaKepala = "(......................)";
                String namaSekolah = "";
                String namaYayasan = "";
                if (siswa.getSekolah() != null) {
                    namaSekolah = siswa.getSekolah().getNama() == null ? "" : siswa.getSekolah().getNama();
                    if (siswa.getSekolah().getNamaKepalaSekolah() != null && !siswa.getSekolah().getNamaKepalaSekolah().trim().isEmpty()) {
                        namaKepala = siswa.getSekolah().getNamaKepalaSekolah();
                    }
                    if (siswa.getSekolah().getYayasan() != null && siswa.getSekolah().getYayasan().getNama() != null) {
                        namaYayasan = siswa.getSekolah().getYayasan().getNama();
                    }
                }
                parameters.put("kepala_sekolah", namaKepala);
                parameters.put("kaprodi", namaKepala);
                parameters.put("sekolah", namaSekolah);
                parameters.put("yayasan", namaYayasan);
                parameters.put("fakultas", namaSekolah);
                parameters.put("jurusan", namaSekolah);

                StringBuilder sbTagihanData = new StringBuilder();
                List<Map<String, Object>> newMaps = new ArrayList<Map<String, Object>>();
                for(int i=0; i<ids.length(); i++) {
                    Tagihan tag = (Tagihan) ConstantValues.ambil(Tagihan.class.getName(), ids.getLong(i), true);
                    if(tag != null) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("tagihan", tag);
                        Double n = (tag.getNominal() + (tag.getDenda() != null ? tag.getDenda() : 0.0)) - (tag.getDiskon() != null ? tag.getDiskon() : 0.0);
                        map.put("nilai", n);
                        String lbl = tag.getNominalBiaya().getItemBiayaSekolah().getNama() + (tag.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tag.getBayarKe() + ")" : "");
                        map.put("label", lbl);
                        map.put("jenis_tagihan_id", tag.getPengaturanBiaya().getId());
                        map.put("jenis_tagihan", tag.getPengaturanBiaya().toString());
                        newMaps.add(map);
                        
                        if (sbTagihanData.length() > 0) sbTagihanData.append("; ");
                        sbTagihanData.append(lbl).append(", ").append(Common.numberFormat.get().format(n));
                    }
                }
                
                parameters.put("tagihanData", sbTagihanData.toString());
                parameters.put("maps", newMaps);

                boolean kirimWA = payload.optBoolean("kirimWA", false);
                
                File file = ais.action.report.Report.generateFileReportCore("pdf", parameters, "Surat_Tagihan_Siswa", WaktuUtil.getDate(), null, Common.getGeneratedBarCode(), Common.locale);
                if (!kirimWA) {
                    String encryptedName = Common.desEncrypter.get().encrypt(file.getName());
                    String pathUri = "/pdf?p=" + URLEncoder.encode(encryptedName, "UTF-8");
                    String urlPdf = Common.ROOT + pathUri;
                    
                    String hostProtocol = Common.getRequestHostWithProtocol();
                    if (!hostProtocol.toLowerCase().contains("localhost") && !hostProtocol.toLowerCase().contains("127.0.0.1")
                            && Common.getKonfigurasi("gunakan_google_view_saat_mobile", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF)
                            && (Common.isAsliMobile() || Common.getKonfigurasi("gunakan_google_view_saat_tampilkan_pdf", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF))) {
                        
                        String fullPath = hostProtocol + Common.ROOT + pathUri;
                        urlPdf = "https://docs.google.com/gview?embedded=true&url=" + URLEncoder.encode(fullPath, "UTF-8");
                    }

                    String modalId = "modalPdf_" + System.currentTimeMillis();
                    StringBuilder sbHtm = new StringBuilder();
                    sbHtm.append("<script>");
                    sbHtm.append("var modalHtml = '");
                    sbHtm.append("<div class=\"modal fade\" id=\"").append(modalId).append("\" tabindex=\"-1\" aria-hidden=\"true\" style=\"z-index: 106000;\">");
                    sbHtm.append("<div class=\"modal-dialog modal-xl modal-dialog-centered\">");
                    sbHtm.append("<div class=\"modal-content shadow-lg border-0 rounded-4\">");
                    sbHtm.append("<div class=\"modal-header border-bottom-0 bg-light\">");
                    sbHtm.append("<h5 class=\"modal-title fw-bold text-dark\"><i class=\"fas fa-file-pdf text-danger me-2\"></i>Surat Tagihan Pelanggan</h5>");
                    sbHtm.append("<button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"modal\" aria-label=\"Close\"></button>");
                    sbHtm.append("</div>");
                    sbHtm.append("<div class=\"modal-body p-0\" style=\"height: 80vh; min-height: 500px; background: #525659;\">");
                    sbHtm.append("<iframe src=\"").append(urlPdf).append("\" style=\"width: 100%; height: 100%; border: none;\"></iframe>");
                    sbHtm.append("</div>");
                    sbHtm.append("<div class=\"modal-footer border-top-0 bg-light\">");
                    sbHtm.append("<button type=\"button\" class=\"btn btn-secondary px-4 rounded-pill\" data-bs-dismiss=\"modal\">Tutup</button>");
                    sbHtm.append("</div>");
                    sbHtm.append("</div></div></div>';");
                    
                    sbHtm.append("document.body.insertAdjacentHTML('beforeend', modalHtml);");
                    sbHtm.append("var myModalEl = document.getElementById('").append(modalId).append("');");
                    sbHtm.append("var myModal = new bootstrap.Modal(myModalEl);");
                    sbHtm.append("myModal.show();");
                    sbHtm.append("myModalEl.addEventListener('hidden.bs.modal', function () { myModalEl.remove(); });");
                    sbHtm.append("</script>");

                    response.setContentType("text/html;charset=UTF-8");
                    response.getWriter().print(sbHtm.toString());
                    response.getWriter().flush();
                    return;
                } else {
                    String waktuStr = "Pagi";
                    int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    if (jam >= 10 && jam < 15) waktuStr = "Siang";
                    else if (jam >= 15 && jam < 18) waktuStr = "Sore";
                    else if (jam >= 18 && jam <= 24) waktuStr = "Malam";
                    String subject = "Tagihan siswa atas nama " + siswa.getNama() + " (" + siswa.getNomorInduk() + ")";
                    String body = "Selamat " + waktuStr + ",<br><br>Yth. Bapak/Ibu Wali Murid dari <b>" + siswa.getNama()
                            + "</b>,<br>Kami dari pihak sekolah *" + namaSekolah
                            + "* menyampaikan informasi mengenai tagihan yang perlu diselesaikan atas nama <b>"
                            + siswa.getNama() + "</b>. Rincian lengkap mengenai tagihan terlampir pada file terlampir. Kami harap Bapak/Ibu dapat melakukan pembayaran sesuai dengan batas waktu yang tertera pada file. Jika ada pertanyaan terkait tagihan atau pembayaran, silakan menghubungi kami pihak sekolah, kami siap membantu.<br><br><b>Terima kasih atas perhatian dan kerja samanya.</b>";
                    String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
                    org.json.JSONArray userIds = new org.json.JSONArray();
                    userIds.put(siswa.getNomorIndukNasional() == null || siswa.getNomorIndukNasional().trim().isEmpty() ? siswa.getNomorInduk() : siswa.getNomorIndukNasional());
                    MailSender.sendMailLampiranTagihan(userIds, subject, body, sender, siswa.getAlamatEmail(), null, false, siswa, true, file);
                    
                    String url = Common.getRequestHostWithProtocolSimple() + file.getAbsolutePath().split("webapps")[1];
                    Set<String> forms = siswa.ambilTelp();
                    if (forms != null) {
                        for (String from : forms) {
                            if (from != null && !from.trim().isEmpty() && !(from.trim().equals("00000000000000000000") || from.trim().equals("000000000"))) {
                                String bodyWA = "Selamat " + waktuStr + ",\n\nYth. Bapak/Ibu Wali Murid atas nama *" + siswa.getNama()
                                        + "*,\n\nKami dari pihak sekolah *" + namaSekolah
                                        + "* menyampaikan informasi mengenai tagihan yang perlu diselesaikan, rincian lengkap mengenai tagihan terlampir pada file terlampir. Kami harap Bapak/Ibu dapat melakukan pembayaran sesuai dengan batas waktu yang tertera pada file. Jika ada pertanyaan terkait tagihan atau pembayaran, silakan menghubungi kami pihak sekolah, kami siap membantu.\n\n*Terima kasih atas perhatian dan kerja samanya.*";
                                String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal", "*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n").getNilai();
                                Wa.kirimWaViaUltramsg(from, dawal + bodyWA, "Tagihan_Pembayaran.pdf", url, Wa.buatProfile(siswa.getSekolah(), null));
                            }
                        }
                    }
                    jsonResponse.put("status", "00");
                }
            } else {
                jsonResponse.put("status", "error").put("message", Common.getBahasaConfig("Pelanggan (Siswa) tidak ditemukan atau parameter tagihan tidak valid."));
            }

        } else if ("prosesBayar".equals(action)) {
            String typeMetode = payload.optString("typeMetode");
            JSONArray ids = payload.optJSONArray("tagihanIds");
            boolean pakaiTabungan = payload.optBoolean("pakaiTabungan");
            Double depositTersedia = 0.0; 
            sessionDB = HibernateUtil.openSession();
            
            Siswa siswa = khususUntukSiswa ? (Siswa) ConstantValues.ambil(Siswa.class.getName(), payload.optLong("idSiswa"), true) : null;
            CalonSiswa calonSiswa = !khususUntukSiswa ? (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), payload.optLong("idSiswa"), true) : null;
            Sekolah sek = siswa != null ? siswa.getSekolah() : (calonSiswa != null ? calonSiswa.getSekolah() : null);
            List<Tagihan> listTagihanBayar = new ArrayList<Tagihan>();
            Double totalDb = 0.0;
            for(int i=0; i<ids.length(); i++) {
                Tagihan th = (Tagihan) ConstantValues.ambil(Tagihan.class.getName(), ids.getLong(i), true);
                listTagihanBayar.add(th);
                totalDb += (th.getNominal() + th.getDenda()) - th.getDiskon();
            }

            if(pakaiTabungan) depositTersedia = DepositHelper.hitungDeposit(siswa, calonSiswa);
            if("TUNAI".equals(typeMetode)) {
                AkunPembayaranSiswa akun = (AkunPembayaranSiswa) ConstantValues.ambil(AkunPembayaranSiswa.class.getName(), payload.optLong("idMetode"), true);
                Rows dummyRows = new Rows();
                try {
                    for(Tagihan th : listTagihanBayar) {
                        Row r = new Row();
                        r.setAttribute("tagihan", th);
                        MyCheckboxConfig chk = new MyCheckboxConfig();
                        chk.setChecked(true);
                        r.setAttribute("pilih", chk);
                        Doublebox db = new Doublebox(th.getNominal());
                        r.setAttribute("nominal", db);
                        dummyRows.appendChild(r);
                    }
                } catch(Exception e) {
                    System.out.println("Peringatan: Gagal melakukan instansiasi Mock Rows ZK: " + e.getMessage());
                }

                ais.database.model.sekolah.PembayaranSiswa pemb = ais.common.TunaiSiswaCommon.onSave(siswa, calonSiswa, listTagihanBayar, depositTersedia, pakaiTabungan ? depositTersedia : null, tbmuser.getUserNama(), akun, dummyRows, WaktuUtil.getDate());
                PembayaranSiswaUtil.cetakStruk(pemb);
                return;

            } else if(typeMetode.contains("DIRECT_BANK_")) {
                int tipe = Integer.parseInt(typeMetode.replace("DIRECT_BANK_", ""));
                if(tipe == 1) ais.common.BriCommon.onSaveBri(siswa, calonSiswa, listTagihanBayar, totalDb, true, depositTersedia);
                else if (tipe == 2) ais.common.BniCommon.onSaveBni(siswa, calonSiswa, listTagihanBayar, totalDb, true, depositTersedia);
                else if (tipe == 3) ais.common.BsiCommon.onSaveBsi(siswa, calonSiswa, listTagihanBayar, totalDb, true, depositTersedia);
                jsonResponse.put("status", "00").put("tipe", "STRUK");
            } else if("GATEWAY".equals(typeMetode)) {
                AkunPembayaranSiswa akun = (AkunPembayaranSiswa) sessionDB.createCriteria(AkunPembayaranSiswa.class).add(Restrictions.eq("sekolah", sek)).add(Restrictions.eq("manual", false)).setMaxResults(1).uniqueResult();
                BankHost bh = PembayaranUtil.getInstance().getBankHost(Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
                Map<String, Object> paramMap = new HashMap<>(); paramMap.put("online", true);
                Double adminFee = payload.optDouble("fee");
                VirtualAccountBank va = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa, listTagihanBayar, paramMap, adminFee, pakaiTabungan ? depositTersedia : null, null, bh, akun, sek);
                if(va != null && va.getLink() != null && !va.getLink().trim().isEmpty()) {
                    jsonResponse.put("status", "00").put("tipe", "POPUP_URL").put("url", va.getLink());
                } else if(va != null && va.getKode() != null && !va.getKode().trim().isEmpty()) {
                    Double total = va.getTotal() != null ? va.getTotal() : 0.0;
                    String terbilang = ais.common.IndonesianNumberToWords.convert((long) (total + adminFee));
                    
                    String qrUrl = "";
                    try {
                        java.io.File fBarcode = new java.io.File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + va.getId() + ".png");
                        ais.common.BarcodeCommon.generateCRCode(va.getKode(), fBarcode);
                        qrUrl = Common.getRequestHostWithProtocol() + "/report/" + fBarcode.getName();
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayar/_pembayaran_online_services.jsp:988");}

                    JSONObject vaData = new JSONObject();
                    vaData.put("kode", va.getKode());
                    vaData.put("nama", siswa != null ? siswa.getNama() : calonSiswa.getNama());
                    vaData.put("nominal", total);
                    vaData.put("biayaAdministrasi", adminFee);
                    vaData.put("biayaTotal", total + adminFee);
                    vaData.put("terbilang", terbilang);
                    vaData.put("kadaluarsa", va.getKadaluarsa() != null ? Common.dateFormat.get().format(va.getKadaluarsa()) : "-");
                    vaData.put("qr", qrUrl);
                    
                    jsonResponse.put("status", "00").put("tipe", "SHOW_VA_INFO").put("vaData", vaData);
                } else {
                    jsonResponse.put("status", "error").put("message", Common.getBahasaConfig("Sistem gagal mendapatkan tautan rute pembayaran online dari perantara bank."));
                }
            }
        }
        
    } catch (Exception e) {
        if (sessionDB != null && sessionDB.getTransaction() != null && sessionDB.getTransaction().isActive()) {
            sessionDB.getTransaction().rollback();
        }
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayar/_pembayaran_online_services.jsp:1011");
        jsonResponse.put("status", "error");
        jsonResponse.put("message", Common.getBahasaConfig("Terjadi galat pada peladen: ") + e.getMessage());
    } finally {
        try {
            if (sessionDB != null && sessionDB.isOpen()) {
                sessionDB.disconnect();
                sessionDB.close();
            }
            HibernateUtil.closeSessionQuietly(sessionDB);
        } catch (Exception ex) {
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit webapp/WEB-INF/baru/modul/bayar/_pembayaran_online_services.jsp:1022");
        }
    }

    out.print(jsonResponse.toString());
%>