<%-- BUILD 2026-06-29: dipaksa rekompilasi untuk cegah IncompatibleClassChangeError dari method PembayaranUtil yang berubah static->instance pada JSP ter-compile lama. WAJIB bersihkan work dir Tomcat saat deploy lalu restart. --%>
<%@page import="ais.database.model.ItemBiaya"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Calendar"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.DetailBiaya"%>
<%@page import="ais.database.model.PengaturanPembayaranBulanan"%>
<%@page import="ais.database.model.JadwalPembayaran"%>
<%@page import="ais.database.model.VirtualAccountBank"%>
<%@page import="ais.database.model.BankHost"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.CicilanPembayaran"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankOnline"%>
<%@page import="ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankOnline"%>
<%@page import="ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankOnline"%>
<%@page import="ais.action.ws.util.PembayaranUtil"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.IndonesianNumberToWords"%>
<%@page import="ais.common.BarcodeCommon"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.zkoss.zul.Grid"%>
<%@page import="org.zkoss.zul.Rows"%>
<%@page import="org.zkoss.zul.Row"%>
<%@page import="org.zkoss.zul.Combobox"%>
<%@page import="org.zkoss.zul.Comboitem"%>
<%@page import="ais.ui.util.MyDoublebox"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collection"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.io.File"%>

<%
    System.out.println("\n[PAYMENT DEBUG] === MULAI PROSES _lanjut_bayar_services.jsp ===");
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        System.out.println("[PAYMENT DEBUG] ERROR: Sesi pengguna berakhir.");
        response.sendRedirect(Common.ROOT + "/login.jsp");
        return;
    }

    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(5);
    String gateway = request.getParameter("gateway");
    String idStr = request.getParameter("id");
    String jkIdStr = request.getParameter("jkId");
    String isMahasiswaStr = request.getParameter("isMahasiswa");
    String smtStr = request.getParameter("smt");
    String payload = request.getParameter("payload");
    
    System.out.println("[PAYMENT DEBUG] Gateway Terpilih: " + gateway);
    System.out.println("[PAYMENT DEBUG] Parameter ID Person: " + idStr + ", IsMahasiswa: " + isMahasiswaStr);

    if (payload == null || payload.trim().isEmpty() || gateway == null || gateway.trim().isEmpty()) {
        System.out.println("[PAYMENT DEBUG] ERROR: Payload atau gateway kosong.");
        out.print("<div class='alert alert-danger m-4 shadow-sm rounded-4'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Parameter pembayaran tidak lengkap atau tidak sah.") + "</div>");
        return;
    }

    Session sess = null;
    VirtualAccountBank va = null;
    String popupUrlPrefix = null;
    String redirectLink = null;
    Double biayaAdministrasi = 0.0;
    boolean isExternalLink = false;
    
    try {
        sess = HibernateUtil.openSession();
        long id = Long.parseLong(idStr);
        long jkId = Long.parseLong(jkIdStr);
        boolean isMahasiswa = "true".equalsIgnoreCase(isMahasiswaStr);
        int smtInt = (smtStr != null && !smtStr.isEmpty()) ? Integer.parseInt(smtStr) : 1;

        Object person = isMahasiswa 
                        ? ConstantValues.ambil(Mahasiswa.class.getName(), id, true) 
                        : ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), id, true);

        if (person == null) {
            System.out.println("[PAYMENT DEBUG] ERROR: Entitas Mahasiswa / Calon Mahasiswa Tidak Ditemukan!");
            out.print("<div class='alert alert-danger m-4 shadow-sm rounded-4'><i class='fas fa-user-times me-2'></i>" + Common.getBahasaConfig("Data pelanggan tidak ditemukan di pangkalan data.") + "</div>");
            return;
        }

        String namaPelanggan = isMahasiswa ? ((Mahasiswa)person).getNama() : ((BiodataCalonMahasiswa)person).getNama();
        String identitasPlg = isMahasiswa ? ((Mahasiswa)person).getNim() : ((BiodataCalonMahasiswa)person).getNoRegistrasi();

        System.out.println("[PAYMENT DEBUG] Entitas Ditemukan: " + namaPelanggan + " (" + identitasPlg + ")");

        // ====================================================================
        // 1. LOGIKA PENGAMBILAN JADWAL & IDENTITAS
        // ====================================================================
        JadwalPembayaran jadwal = null;
        JenisKegiatan jenisKegiatan = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(), jkId, true);
        Date tanggalSekarang = WaktuUtil.getDate();

        /* GATE AKTIVASI GATEWAY (server-side): dulu gating on/off hanya di lapisan tampilan
           (_lanjut_bayar.jsp) sehingga URL services bisa dipanggil langsung dengan gateway
           yang konfigurasi-nya mati. Kini divalidasi ulang di sini memakai katalog terpusat
           (sumber aturan yang sama dengan tombol). Id "finpay" pada services adalah id
           legacy untuk katalog "bank_finpay"; kunci lama aktifkan_pembayaran_via_finpay
           tetap diterima demi kompatibilitas instansi lama. */
        {
            String idKatalogGw = "finpay".equals(gateway) ? "bank_finpay" : gateway;
            ais.action.master.helper.PembayaranGatewayKatalog.Gateway gwDef =
                    ais.action.master.helper.PembayaranGatewayKatalog.cari(idKatalogGw);
            boolean gwBoleh = gwDef != null
                    && ais.action.master.helper.PembayaranGatewayKatalog.tampil(gwDef, jenisKegiatan);
            if (!gwBoleh && "bank_finpay".equals(idKatalogGw)) {
                gwBoleh = ais.action.master.helper.PembayaranGatewayKatalog.tampil(
                        ais.action.master.helper.PembayaranGatewayKatalog.cari("finpay"), jenisKegiatan);
            }
            if (!gwBoleh) {
                System.out.println("[PAYMENT DEBUG] DITOLAK: gateway '" + gateway + "' tidak aktif menurut konfigurasi.");
                out.print("<div class='alert alert-warning m-4 shadow-sm rounded-4'><i class='fas fa-ban me-2'></i>"
                    + Common.getBahasaConfig("Saluran pembayaran ini tidak aktif. Silakan pilih saluran lain atau hubungi administrator.") + "</div>");
                return;
            }
        }
        
        Integer tahunAngkatan = 0;
        Integer semesterMulaiMasuk = 0;
        String semesterMulaiNama = "";
        ais.database.model.Jenjang jenjangObj = null;
        ais.database.model.JenisSeleksi seleksiObj = null;
        String programMhs = "";
        String identitasMhs = "";
        Kegiatan kegiatanAktif = null;
        ais.database.model.GelombangPendaftaran gelombangPendaftaran = null;

        if (isMahasiswa) {
            Mahasiswa m = (Mahasiswa) person;
            tahunAngkatan = m.getTahunangkatan() != null ? m.getTahunangkatan() : 0;
            semesterMulaiMasuk = m.getPindahKeKampusIniMasukSemester() != null ? m.getPindahKeKampusIniMasukSemester() : 0;
            semesterMulaiNama = m.getSemesterMulai();
            jenjangObj = (m.getJurusan() != null) ? m.getJurusan().getJenjang() : null;
            seleksiObj = m.getJenisSeleksi();
            programMhs = m.getProgram();
            identitasMhs = m.getNim();
            kegiatanAktif = m.ambilKegiatansRefresh(smtInt, jenisKegiatan, true);
            try { java.lang.reflect.Method mtd = m.getClass().getMethod("getGelombangPendaftaran"); gelombangPendaftaran = (ais.database.model.GelombangPendaftaran) mtd.invoke(m); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar_services.jsp:155");}
        } else {
            BiodataCalonMahasiswa c = (BiodataCalonMahasiswa) person;
            tahunAngkatan = c.getTahun() != null ? c.getTahun() : 0;
            semesterMulaiMasuk = 0;
            semesterMulaiNama = c.getSemesterMulai();
            jenjangObj = c.getJenjang();
            seleksiObj = c.getJenisSeleksi();
            programMhs = c.getProgram();
            identitasMhs = c.getNoRegistrasi();
            kegiatanAktif = c.ambilKegiatansRefresh(smtInt, jenisKegiatan, true);
            gelombangPendaftaran = c.getGelombangPendaftaran();
        }

        Integer tahunAkademikMulai = Common.getTahunAkademik(smtInt, tahunAngkatan, semesterMulaiMasuk, semesterMulaiNama);
        String tahunAkademikStr = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

        java.io.Serializable[] serializables = PembayaranUtil.getInstance().getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
                tanggalSekarang, jenisKegiatan, jenjangObj, tahunAkademikStr, (smtInt % 2 != 0),
                seleksiObj, programMhs, identitasMhs, gelombangPendaftaran);

        jadwal = (serializables != null && serializables.length > 0) ? (JadwalPembayaran) serializables[0] : null;

        if (jadwal == null && kegiatanAktif != null) {
            jadwal = kegiatanAktif.getJadwalPembayaran();
        }

        if (jadwal == null) {
            System.out.println("[PAYMENT DEBUG] ERROR: Jadwal Pembayaran Belum Mulai/Tidak Ditemukan!");
            out.print("<script>if(typeof tampilkanToast === 'function') tampilkanToast('"+Common.getBahasaConfigJS("Jadwal pembayaran belum ada, sudah terlewat, atau belum mulai")+"', 'bg-danger text-white');</script>");
            out.print("<div class='alert alert-danger m-4 shadow-sm rounded-4 text-center'>");
            out.print("<i class='fas fa-calendar-times fa-2x mb-3 text-danger'></i><br>");
            out.print("<b>" + Common.getBahasaConfig("Jadwal Tidak Ditemukan") + "</b><br>");
            out.print(Common.getBahasaConfig("Jadwal pembayaran belum tersedia untuk periode ini atau Anda berada di luar masa pembayaran."));
            out.print("</div>");
            return;
        }
        
        Collection<ais.database.model.DetailKegiatan> detailKegiatans = kegiatanAktif != null ? kegiatanAktif.ambilDetailKegiatan(false) : new ArrayList<ais.database.model.DetailKegiatan>();
        JadwalPembayaran jdw = jadwal != null && jadwal.getKhususUntukNim() != null && jadwal.getKhususUntukNim().contains("," + identitasMhs + ",") ? jadwal : null;

        // ====================================================================
        // 2. MENGURAI PAYLOAD & MERAKIT DETAIL BIAYA BERSAMA DENDA (DEEP MOCKING GRID ZK)
        // ====================================================================
        List<DetailBiaya> detailBiayasToPay = new ArrayList<DetailBiaya>();
        JSONArray itemsSmartlink = new JSONArray();
        Double totalTagihanTanpaAdmin = 0.0;
        
        Grid gridCicilanJava = new Grid();
        Rows rows = new Rows();
        gridCicilanJava.appendChild(rows);
        
        String[] itemsPayload = payload.split(",");
        System.out.println("[PAYMENT DEBUG] Mengurai Payload... Jumlah Item: " + itemsPayload.length);
        
        for (String item : itemsPayload) {
            String[] parts = item.split("\\|");
            if (parts.length == 2) {
                try {
                    String idUnik = parts[0].trim();
                    double nominal = Double.parseDouble(parts[1].trim());
                    
                    if (nominal > 0) {
                        DetailBiaya db = null;
                        ais.database.model.DetailKegiatan dkSesuai = null;
                        
                        if (idUnik.startsWith("DB_")) {
                            long dbId = Long.parseLong(idUnik.replace("DB_", ""));
                            db = (DetailBiaya) ConstantValues.ambil(DetailBiaya.class.getName(), dbId, true);
                            
                            if (db != null) {
                                for(ais.database.model.DetailKegiatan dk : detailKegiatans) {
                                    if(dk.getDetailBiaya() != null && dk.getDetailBiaya().getId().equals(db.getId()) && dk.getPengaturanPembayaranBulanan() == null) { dkSesuai = dk; break; }
                                }
                                Double jmlTagihan = Kegiatan.ambilJumlahTagihan(dkSesuai, kegiatanAktif, db, false);
                                if(jmlTagihan == null) jmlTagihan = 0.0;

                                Double hasilDenda = dkSesuai != null && dkSesuai.getMenggunakanDendaCustom() ? jmlTagihan
                                    : dkSesuai != null && (dkSesuai.getBatalkanDenda() || jmlTagihan.intValue() == 0) ? jmlTagihan
                                    : db.checkDenda(jmlTagihan, tanggalSekarang, jdw, jadwal == null ? null : jadwal.getJenisKegiatan(), null);

                                if (dkSesuai != null && dkSesuai.getMenggunakanDendaCustom()) {
                                    db.setInfoDenda(" Penambahan denda senilai " + Common.numberFormat.get().format(dkSesuai.getDendaCustom() != null ? dkSesuai.getDendaCustom() : 0.0) + ".");
                                }

                                Double nilaiDenda = hasilDenda - jmlTagihan;
                                if (dkSesuai != null && !dkSesuai.getMenggunakanDendaCustom()) {
                                    dkSesuai.setDendaCustom(nilaiDenda);
                                }
                                
                                if (db.getItemBiaya() != null) {
                                    if (db.getItemBiaya().getKode() == null) db.getItemBiaya().setKode(" ");
                                    if (db.getItemBiaya().getNama() == null) db.getItemBiaya().setNama(" ");
                                }
                                
                                Row mockRow = new Row();
                                MyDoublebox dbox = new MyDoublebox();
                                dbox.setValue(nominal);
                                mockRow.setAttribute("jumlahCicilan", dbox); 
                                
                                CicilanPembayaran mockCp = new CicilanPembayaran();
                                mockCp.setId(null); 
                                mockCp.setDetailBiaya(db);
                                mockRow.setAttribute("cicilanPembayaran", mockCp); 
                                mockRow.setAttribute("detailBiaya", db);
                                
                                Combobox cb = new Combobox();
                                Comboitem cItem = new Comboitem();
                                cItem.setValue(db);
                                cb.appendChild(cItem);
                                cb.setSelectedItem(cItem);
                                mockRow.setAttribute("itemBiaya", cb); 
                                
                                rows.appendChild(mockRow);
                            }
                        } else if (idUnik.startsWith("PB_")) {
                            long pbId = Long.parseLong(idUnik.replace("PB_", ""));
                            PengaturanPembayaranBulanan pb = (PengaturanPembayaranBulanan) ConstantValues.ambil(PengaturanPembayaranBulanan.class.getName(), pbId, true);
                            
                            if (pb != null) {
                                db = pb.getDetailBiaya();
                                for(ais.database.model.DetailKegiatan dk : detailKegiatans) {
                                    if(dk.getPengaturanPembayaranBulanan() != null && dk.getPengaturanPembayaranBulanan().getId().equals(pb.getId())) { dkSesuai = dk; break; }
                                }
                                Double jmlTagihan = Kegiatan.ambilJumlahTagihan(dkSesuai, db, kegiatanAktif, isMahasiswa ? (Mahasiswa)person : null, smtInt, pb);
                                if(jmlTagihan == null) jmlTagihan = 0.0;

                                Double hasilDenda = dkSesuai != null && (dkSesuai.getBatalkanDenda() || jmlTagihan.intValue() == 0) ? jmlTagihan
                                    : dkSesuai != null && dkSesuai.getMenggunakanDendaCustom() ? jmlTagihan
                                    : pb.checkDenda(jmlTagihan, tanggalSekarang, jdw, jadwal == null ? null : jadwal.getJenisKegiatan());

                                if (dkSesuai != null && dkSesuai.getMenggunakanDendaCustom()) {
                                    pb.setInfoDenda(" Penambahan denda senilai " + Common.numberFormat.get().format(dkSesuai.getDendaCustom() != null ? dkSesuai.getDendaCustom() : 0.0) + ".");
                                }

                                Double nilaiDenda = hasilDenda - jmlTagihan;
                                if (dkSesuai != null && !dkSesuai.getMenggunakanDendaCustom()) {
                                    dkSesuai.setDendaCustom(nilaiDenda);
                                }
                                
                                if (pb.getDetailBiaya() != null && pb.getDetailBiaya().getItemBiaya() != null) {
                                    if (pb.getDetailBiaya().getItemBiaya().getKode() == null) pb.getDetailBiaya().getItemBiaya().setKode(" ");
                                    if (pb.getDetailBiaya().getItemBiaya().getNama() == null) pb.getDetailBiaya().getItemBiaya().setNama(" ");
                                }
                                
                                Row mockRow = new Row();
                                MyDoublebox dbox = new MyDoublebox();
                                dbox.setValue(nominal);
                                mockRow.setAttribute("jumlahCicilan", dbox); 
                                
                                CicilanPembayaran mockCp = new CicilanPembayaran();
                                mockCp.setId(null); 
                                mockCp.setPengaturanPembayaranBulanan(pb);
                                mockRow.setAttribute("cicilanPembayaran", mockCp); 
                                mockRow.setAttribute("detailBiaya", db);
                                
                                rows.appendChild(mockRow);
                            }
                        }

                        if (db != null) {
                            db.setNilaiBiayaBaru(nominal);
                            detailBiayasToPay.add(db);

                            JSONObject itemJson = new JSONObject();
                            String desc = db.getItemBiaya() != null ? db.getItemBiaya().getNama() : "Pembayaran Terpadu";
                            if (desc.length() > 255) desc = desc.substring(0, 255);

                            itemJson.put("name", desc);
                            itemJson.put("amount", (int) nominal);
                            itemJson.put("qty", 1);
                            itemsSmartlink.put(itemJson);
                            
                            totalTagihanTanpaAdmin += nominal;
                        }
                    }
                } catch (Exception e) { System.out.println("[PAYMENT DEBUG] WARN: Gagal Parsing Item Payload: " + item); }
            }
        }

        if (detailBiayasToPay.isEmpty()) {
            System.out.println("[PAYMENT DEBUG] ERROR: Tidak ada Detail Biaya yang Sah / Nominal = 0.");
            out.print("<div class='alert alert-danger m-4 shadow-sm rounded-4'><i class='fas fa-receipt me-2'></i>" + Common.getBahasaConfig("Kewajiban pembayaran tidak dapat divalidasi. Pastikan nominal lebih dari nol.") + "</div>");
            return;
        }

        System.out.println("[PAYMENT DEBUG] Total Biaya Tanpa Admin: " + totalTagihanTanpaAdmin);

        // ====================================================================
        // 2.5 MEMBANGUN STRING CICILAN, DETAIL BIAYA & KETERANGAN UNTUK SET_CICILAN
        // ====================================================================
        StringBuilder cicilan = new StringBuilder();
        StringBuilder ket = new StringBuilder();
        StringBuilder pemb = new StringBuilder();
        StringBuilder keteranganSimpleBanget = new StringBuilder();
        
        if (gridCicilanJava != null) {
            List<Row> mycicilanrows = gridCicilanJava.getRows().getChildren();
            for (Row rowMock : mycicilanrows) {
                MyDoublebox jumlahCicilan = (MyDoublebox) rowMock.getAttribute("jumlahCicilan");

                if (jumlahCicilan != null && jumlahCicilan.getValue() != null && jumlahCicilan.getValue().intValue() != 0) {
                    CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) rowMock.getAttribute("cicilanPembayaran");

                    if (cicilanPembayaranSebelumnya != null && cicilanPembayaranSebelumnya.getId() == null) {
                        try {
                            PengaturanPembayaranBulanan biaya = cicilanPembayaranSebelumnya.getPengaturanPembayaranBulanan();
                            Double nilai = jumlahCicilan.getValue();

                            if (biaya != null) {
                                String descSimpleBanget = String.valueOf(biaya.getId());
                                if (keteranganSimpleBanget.length() > 0) keteranganSimpleBanget.append(";");
                                keteranganSimpleBanget.append(descSimpleBanget);

                                if (cicilan.length() > 0) cicilan.append(",");
                                cicilan.append("Bulanan-").append(biaya.getId()).append("-").append(nilai);

                                Double hasilDenda = biaya.checkDenda(nilai, tanggalSekarang, jdw, jadwal == null ? null : jadwal.getJenisKegiatan());

                                String desc = biaya.getKeterangan();
                                if (desc == null) desc = "";
                                desc = (desc.isEmpty() ? (biaya.getDetailBiaya().getItemBiaya().getNama()) : desc)
                                        + ", Rp. " + Common.numberFormat.get().format(nilai)
                                        + (hasilDenda.intValue() > nilai.intValue() ? biaya.getInfoDenda() : "");

                                if (ket.length() > 0) ket.append(",");
                                ket.append(biaya.getDetailBiaya().getItemBiaya().getNama());

                                pemb.append(biaya.getDetailBiaya().getItemBiaya().getKode().trim()).append(",").append(desc).append(";");
                            } else {
                                Combobox myItemBiaya = (Combobox) rowMock.getAttribute("itemBiaya");
                                ItemBiaya itemBiaya = null;
                                DetailBiaya detailBiaya = (DetailBiaya) (myItemBiaya == null || myItemBiaya.getSelectedItem() == null ? null : myItemBiaya.getSelectedItem().getValue());

                                if (rowMock.getAttribute("detailBiaya") != null) {
                                    detailBiaya = (DetailBiaya) rowMock.getAttribute("detailBiaya");
                                    itemBiaya = detailBiaya.getItemBiaya();
                                } else if (cicilanPembayaranSebelumnya.getItemBiaya() != null && cicilanPembayaranSebelumnya.getItemBiaya().getId() != null) {
                                    itemBiaya = cicilanPembayaranSebelumnya.getItemBiaya();
                                } else if (detailBiaya != null) {
                                    itemBiaya = detailBiaya.getItemBiaya();
                                }

                                if (itemBiaya != null && detailBiaya != null) {
                                    String descSimpleBanget = String.valueOf(itemBiaya.getId());
                                    if (keteranganSimpleBanget.length() > 0) keteranganSimpleBanget.append(";");
                                    keteranganSimpleBanget.append(descSimpleBanget);

                                    if (cicilan.length() > 0) cicilan.append(",");
                                    cicilan.append("Item-").append(itemBiaya.getId()).append("-").append(nilai)
                                            .append("-").append(detailBiaya.getBayarKe()).append("-")
                                            .append(detailBiaya.getId());

                                    String desc = itemBiaya.getNama() + ", Rp. " + Common.numberFormat.get().format(nilai);

                                    if (ket.length() > 0) ket.append(",");
                                    ket.append(itemBiaya.getNama());

                                    pemb.append(itemBiaya.getKode().trim()).append(",").append(desc).append(";");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar_services.jsp:417");
                        }
                    }
                }
            }
        }
        
        String cicilanStr = cicilan.toString();
        String pembStr = pemb.toString();
        
        String detailbiayaStr = "";
        for(DetailBiaya dx : detailBiayasToPay) {
            detailbiayaStr += (detailbiayaStr.isEmpty() ? dx.getId() : "," + dx.getId());
        }

        // ====================================================================
        // 3. KONFIGURASI BANK ONLINE SERIES & PENYESUAIAN OVERRIDE
        // ====================================================================
        String bankGatewayId = gateway;
        
        if ("online".equals(bankGatewayId)) {
            boolean via_bank_online_smartlink = Common.getKonfigurasi("aktifkan_va_e_smartlink", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            if (via_bank_online_smartlink) {
                bankGatewayId = "smartlink";
                System.out.println("[PAYMENT DEBUG] OVERRIDE Gateway: online -> smartlink");
            }
        }

        String bankHostConfig = "";
        String adminFeeConfig = "";
        String prefixKodeLainConfig = "";
        String basePathVA = "/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_tampil_va";

        if ("smartlink".equals(bankGatewayId)) {
            bankHostConfig = "online_bank_host_ip"; adminFeeConfig = "online_smartlink_biaya_administrasi";
            popupUrlPrefix = basePathVA; prefixKodeLainConfig = "prefix_kode_bank_lain_online";
        } else if ("maja".equals(bankGatewayId)) {
            bankHostConfig = "maja_bank_host_ip";
            adminFeeConfig = "maja_biaya_administrasi"; popupUrlPrefix = basePathVA; prefixKodeLainConfig = "prefix_kode_bank_lain_maja";
        } else if ("qris".equals(bankGatewayId)) {
            bankHostConfig = "qris_bank_host_ip";
            adminFeeConfig = "qris_biaya_administrasi"; popupUrlPrefix = basePathVA; prefixKodeLainConfig = null;
        } else if ("finpay".equals(bankGatewayId)) {
            bankHostConfig = "finpay_bank_host_ip";
            adminFeeConfig = "finpay_biaya_administrasi"; popupUrlPrefix = null; prefixKodeLainConfig = null;
        } else if ("flip".equals(bankGatewayId)) {
            bankHostConfig = "flip_bank_host_ip";
            adminFeeConfig = "flip_biaya_administrasi"; popupUrlPrefix = null; prefixKodeLainConfig = null;
        } else if ("otto".equals(bankGatewayId)) {
            bankHostConfig = "otto_bank_host_ip";
            adminFeeConfig = "otto_biaya_administrasi"; popupUrlPrefix = null; prefixKodeLainConfig = null;
        } else if ("briva".equals(bankGatewayId)) {
            bankHostConfig = "briva_bank_host_ip";
            adminFeeConfig = "briva_biaya_administrasi"; popupUrlPrefix = basePathVA; prefixKodeLainConfig = null;
        } else if ("online_2".equals(bankGatewayId)) {
            bankHostConfig = "online_2_bank_host_ip";
            adminFeeConfig = "online_biaya_administrasi_2"; popupUrlPrefix = basePathVA; prefixKodeLainConfig = "prefix_kode_bank_lain_online_2";
        } else if ("bankaltimtara".equals(bankGatewayId)) {
            bankHostConfig = "";
            adminFeeConfig = "bankaltimtara_biaya_administrasi"; popupUrlPrefix = basePathVA; prefixKodeLainConfig = null;
        } else if ("online".equals(bankGatewayId) || "bsi".equals(bankGatewayId)) {
            /* "bsi" pada deployment seri online memakai host online yang sama
               (prefix kode bank lain yang membedakan kanalnya). */
            bankGatewayId = "online";
            bankHostConfig = "online_bank_host_ip"; adminFeeConfig = "online_biaya_administrasi"; popupUrlPrefix = basePathVA; prefixKodeLainConfig = "prefix_kode_bank_lain_online";
        } else {
            /* PERBAIKAN BUG VA SALAH BANK:
               Dulu semua gateway yang tidak dikenal (bni, doku, ipaymu, faspay,
               jatelindo, cimb, bri, ntt, btn, bjb) diam-diam dipaksa menjadi
               "online" sehingga klik "BAYAR VIA BNI" malah menerbitkan VA host
               online (BSI/Nagari). Gateway tersebut memakai alur legacy
               tersendiri (mis. BNI e-collection via BniRequestAction) yang belum
               diporting ke checkout tampilan baru, jadi hentikan dengan pesan
               jelas daripada menerbitkan VA bank yang salah. */
            System.out.println("[PAYMENT DEBUG] Gateway '" + bankGatewayId + "' belum didukung checkout tampilan baru.");
            out.print("<div class='alert alert-warning m-4 shadow-sm rounded-4'><i class='fas fa-exclamation-triangle me-2'></i>"
                + Common.getBahasaConfig("Saluran pembayaran ini belum tersedia di tampilan baru. Silakan gunakan tampilan lama (ZK) untuk membayar melalui saluran tersebut, atau pilih saluran lain.") + "</div>");
            return;
        }

        try {
            biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi(adminFeeConfig, "0.0").getNilai());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar_services.jsp:499");}

        String baseHostUrl = Common.getKonfigurasi(bankHostConfig, "").getNilai();
        System.out.println("[PAYMENT DEBUG] Base Host URL Bank: " + baseHostUrl);

        PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
        BankHost bankHost = pembayaranUtil.getBankHost(baseHostUrl, "Bank Host");

        Map param = new HashMap();
        param.put("tahunAkademik", isMahasiswa ? String.valueOf(((Mahasiswa)person).getTahunangkatan()) : String.valueOf(((BiodataCalonMahasiswa)person).getTahunAkademik()));

        /* PERBAIKAN BUG TAGIHAN 2x LIPAT:
           param "cicilan" TIDAK boleh diisi di sini. Semua class Download*BankOnline
           membangun ulang string cicilan dari gridCicilan/detailBiayas; bila param
           sudah berisi item yang sama, token tercatat dua kali pada VA. String
           cicilan VA dipakai PembayaranAction untuk menghitung ulang total saat
           nasabah CEK TAGIHAN dari bank (inquiry) lalu menimpa nominal VA via
           updateTotal() - itulah sebabnya tagihan berubah jadi 2x lipat setelah
           sekadar dicek dari aplikasi bank lain/mobile. (cicilanStr tetap dihitung
           di atas hanya untuk kebutuhan log/diagnosa.) */
        
        if ("smartlink".equals(bankGatewayId)) param.put("smartlink", true);
        else if ("maja".equals(bankGatewayId)) param.put("maja", true);
        else if ("qris".equals(bankGatewayId)) param.put("qris", true);
        else if ("finpay".equals(bankGatewayId)) param.put("finpay", true);
        else if ("flip".equals(bankGatewayId)) param.put("flip", true);
        else if ("otto".equals(bankGatewayId)) param.put("otto", true);
        else if ("briva".equals(bankGatewayId)) param.put("briva", true);
        else if ("bankaltimtara".equals(bankGatewayId)) param.put("bankaltimtara", true);

        // =========================================================================================
        // 4. PEMISAHAN LOGIKA BERDASARKAN GATEWAY
        // =========================================================================================
        String reqChannelCode = request.getParameter("channelCode");
        String reqTimeLimit = request.getParameter("timeLimit");
        String metodeBPD = request.getParameter("metodeBPD");
        
        boolean isSmartlink = "smartlink".equals(bankGatewayId);
        boolean isBankaltimtara = "bankaltimtara".equals(bankGatewayId);
        
        if (isSmartlink) {
            // -------------------------------------------------------------------------------------
            // LOGIKA KHUSUS SMARTLINK (Fase 1 - UI & Fase 2 - Eksekusi)
            // -------------------------------------------------------------------------------------
            if (reqChannelCode == null || reqChannelCode.isEmpty()) {
                System.out.println("[SMARTLINK DEBUG] Fase 1: Menyiapkan UI Pengganti Smartlink ZK Window");
                String channelJsonResponse = "{}";
                
                try {
                    String channelUrl = baseHostUrl + (baseHostUrl.endsWith("/") ? "" : "/") + "rest/payment/channel/v1";
                    java.net.URL url = new java.net.URL(channelUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    if (conn.getResponseCode() == 200) {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        channelJsonResponse = sb.toString();
                    } else {
                        channelJsonResponse = "{\"error\": \"HTTP " + conn.getResponseCode() + "\"}";
                    }
                } catch (Exception ex) {
                    channelJsonResponse = "{\"error\": \"" + ex.getMessage().replace("\"", "\\\"") + "\"}";
                }

                String configSmartlinkStr = Common.getKonfigurasi("channel_biaya_e_smartlink", "VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart").getNilai();
                boolean isTimeShown = Common.getKonfigurasi("jangka_waktu_default_ditampilkan", Konfigurasi.AKTIF).getNilai().trim().equalsIgnoreCase(Konfigurasi.AKTIF);
                String defaultTime = Common.getKonfigurasi("jangka_waktu_default", "").getNilai().trim();
                %>
                <div id="serviceWrapper<%=rnd%>" class="w-100 p-4 animate__animated animate__fadeIn">
                    <div class="text-center border-bottom pb-3 mb-4">
                        <div class="bg-primary bg-opacity-10 text-primary rounded-circle d-inline-flex align-items-center justify-content-center mb-2 shadow-sm" style="width: 60px; height: 60px;">
                            <i class="fas fa-university fs-2"></i>
                        </div>
                        <h5 class="fw-bold text-dark mb-0"><%= Common.getBahasaConfig("Proses Pembayaran Daring") %></h5>
                        <p class="text-muted small mb-0"><%= Common.getBahasaConfig("Pilih Saluran Bayar Bank & Durasi Jatuh Tempo") %></p>
                    </div>
                    
                    <div class="row gx-4">
                        <div class="col-md-5 border-end">
                            <div class="bg-light p-3 rounded-4 shadow-sm mb-4">
                                <label class="small text-muted fw-bold text-uppercase d-block mb-1"><%= isMahasiswa ? "NIM Mahasiswa" : "No. Registrasi Calon" %></label>
                                <h6 class="fw-bold text-dark"><%= identitasMhs %></h6>
                                
                                <label class="small text-muted fw-bold text-uppercase d-block mt-3 mb-1"><%= Common.getBahasaConfig("Nama Lengkap") %></label>
                                <h6 class="fw-bold text-dark"><%= namaPelanggan %></h6>
                                
                                <label class="small text-muted fw-bold text-uppercase d-block mt-3 mb-1"><%= Common.getBahasaConfig("Program Studi") %></label>
                                <h6 class="fw-bold text-dark"><%= (isMahasiswa ? ((Mahasiswa)person).getJurusan().getNama() : ((BiodataCalonMahasiswa)person).ambilJurusan() != null ? ((BiodataCalonMahasiswa)person).ambilJurusan().getNama() : "-") %></h6>
                            </div>
                            
                            <div class="bg-primary bg-opacity-10 p-3 rounded-4 shadow-sm border border-primary border-opacity-25">
                                <label class="small text-primary fw-bold text-uppercase d-block mb-1"><%= Common.getBahasaConfig("Deskripsi Tagihan") %></label>
                                <h6 class="fw-bold text-dark"><%= jenisKegiatan != null ? jenisKegiatan.getNamaKegiatan() : "Pembayaran Terpadu" %></h6>
                                
                                <label class="small text-primary fw-bold text-uppercase d-block mt-3 mb-1"><%= Common.getBahasaConfig("Sub Total Tagihan") %></label>
                                <h4 class="fw-bolder text-primary mb-0">Rp <%= Common.numberFormat.get().format(totalTagihanTanpaAdmin) %></h4>
                            </div>
                        </div>
                        
                        <div class="col-md-7 ps-md-4">
                            <form id="frmChannelSmartlink<%=rnd%>">
                                <h6 class="fw-bold text-dark mb-3"><i class="fas fa-list-ul text-info me-2"></i><%= Common.getBahasaConfig("Pilihan Channel Pembayaran") %></h6>
                                <div class="list-group mb-4 shadow-sm rounded-4" style="max-height: 250px; overflow-y: auto;">
                                    <% 
                                    String[] chList = configSmartlinkStr.split(";");
                                    for(String cl : chList) {
                                        String[] d = cl.trim().split(":");
                                        if(d.length >= 3) {
                                            String cKode = d[0].trim();
                                            Double cAdmin = Double.parseDouble(d[1].trim());
                                            String cNama = d[2].trim();
                                    %>
                                    <label class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-3">
                                        <div class="d-flex align-items-center">
                                            <input class="form-check-input me-3" type="radio" name="channelSelection" value="<%= cKode %>" data-admin="<%= cAdmin %>" required onchange="window.kalkulasiUlangTotal<%=rnd%>()">
                                            <span class="fw-bold text-dark"><%= cNama %></span>
                                        </div>
                                        <span class="badge bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25"><%= Common.getBahasaConfig("Admin:") %> Rp <%= Common.numberFormat.get().format(cAdmin) %></span>
                                    </label>
                                    <% } } %>
                                </div>

                                <% if (isTimeShown) { %>
                                    <h6 class="fw-bold text-dark mb-3"><i class="fas fa-clock text-warning me-2"></i><%= Common.getBahasaConfig("Batas Waktu Pembayaran") %></h6>
                                    <select class="form-select border-secondary border-opacity-25 shadow-sm fw-bold mb-4" name="timeLimit" required>
                                        <option value="15 Menit" <%= "15 Menit".equals(defaultTime) ? "selected" : "" %>><%= Common.getBahasaConfig("15 Menit") %></option>
                                        <option value="30 Menit" <%= "30 Menit".equals(defaultTime) ? "selected" : "" %>><%= Common.getBahasaConfig("30 Menit") %></option>
                                        <option value="1 Jam" <%= "1 Jam".equals(defaultTime) ? "selected" : "" %>><%= Common.getBahasaConfig("1 Jam") %></option>
                                        <option value="3 Jam" <%= "3 Jam".equals(defaultTime) ? "selected" : "" %>><%= Common.getBahasaConfig("3 Jam") %></option>
                                        <option value="6 Jam" <%= "6 Jam".equals(defaultTime) ? "selected" : "" %>><%= Common.getBahasaConfig("6 Jam") %></option>
                                        <option value="24 Jam" <%= "24 Jam".equals(defaultTime) ? "selected" : "" %>><%= Common.getBahasaConfig("24 Jam (1 Hari)") %></option>
                                        <option value="3 Hari" <%= "3 Hari".equals(defaultTime) ? "selected" : "" %>><%= Common.getBahasaConfig("3 Hari") %></option>
                                        <option value="1 Minggu" <%= "1 Minggu".equals(defaultTime) ? "selected" : "" %>><%= Common.getBahasaConfig("1 Minggu") %></option>
                                        <option value="1 Bulan" <%= "1 Bulan".equals(defaultTime) ? "selected" : "" %>><%= Common.getBahasaConfig("1 Bulan") %></option>
                                    </select>
                                <% } else { %>
                                    <input type="hidden" name="timeLimit" value="<%= defaultTime.isEmpty() ? "24 Jam" : defaultTime %>">
                                <% } %>

                                <div class="bg-success bg-opacity-10 p-3 rounded-4 shadow-sm border border-success border-opacity-25 d-flex justify-content-between align-items-center mb-4">
                                    <span class="fw-bold text-success"><%= Common.getBahasaConfig("Total Bayar Akhir :") %></span>
                                    <h3 class="fw-bolder text-success mb-0" id="lblGrandTotal<%=rnd%>">Rp <%= Common.numberFormat.get().format(totalTagihanTanpaAdmin) %></h3>
                                 </div>

                                <div class="d-flex justify-content-between border-top pt-3">
                                    <button type="button" class="btn btn-light fw-bold shadow-sm rounded-pill px-4" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Batal") %></button>
                                    <button type="submit" class="btn btn-success fw-bold shadow-sm rounded-pill px-5" id="btnSubmitSmartlink<%=rnd%>" disabled>
                                        <i class="fas fa-money-check-alt me-2"></i><%= Common.getBahasaConfig("Proses Bayar") %>
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                <script>
                    window.kalkulasiUlangTotal<%=rnd%> = function() {
                        const rdo = document.querySelector('input[name="channelSelection"]:checked');
                        if (rdo) {
                            const adminFee = parseFloat(rdo.getAttribute('data-admin')) || 0;
                            const totalBase = <%= totalTagihanTanpaAdmin %>;
                            const grandTotal = totalBase + adminFee;
                            document.getElementById('lblGrandTotal<%=rnd%>').innerText = "Rp " + new Intl.NumberFormat('id-ID').format(grandTotal);
                            document.getElementById('btnSubmitSmartlink<%=rnd%>').disabled = false;
                        }
                    };

                    document.getElementById('frmChannelSmartlink<%=rnd%>').addEventListener('submit', function(e) {
                        e.preventDefault();
                        const formData = new FormData(this);
                        const selChannel = formData.get('channelSelection');
                        const selTime = formData.get('timeLimit');

                        document.getElementById('serviceWrapper<%=rnd%>').innerHTML = '<div class="text-center py-5 animate__animated animate__fadeIn"><div class="spinner-border text-primary mb-3" style="width: 4rem; height: 4rem; border-width: 0.35em;"></div><h5 class="fw-bold text-secondary"><%= Common.getBahasaConfig("Meminta Kode VA Bank dari Smartlink API...") %></h5></div>';

                        const payloadData = '<%=payload%>';
                        const checkoutDaringUrl = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_lanjut_bayar_services' + 
                                            '&gateway=<%=gateway%>' + 
                                            '&id=<%=idStr%>&jkId=<%=jkIdStr%>&smt=<%=smtStr%>&isMahasiswa=<%=isMahasiswaStr%>' + 
                                            '&rnd=<%=rnd%>' +
                                            '&payload=' + encodeURIComponent(payloadData) +
                                            '&channelCode=' + encodeURIComponent(selChannel) +
                                            '&timeLimit=' + encodeURIComponent(selTime);

                        fetch(checkoutDaringUrl)
                            .then(res => res.text())
                            .then(html => {
                                const wrapper = document.getElementById('serviceWrapper<%=rnd%>');
                                if(wrapper) {
                                    wrapper.outerHTML = html;
                                    const tempDiv = document.createElement('div');
                                    tempDiv.innerHTML = html;
                                    tempDiv.querySelectorAll('script').forEach(s => {
                                        const ns = document.createElement('script');
                                        if (s.src) ns.src = s.src; else ns.textContent = s.textContent;
                                        document.body.appendChild(ns); document.body.removeChild(ns);
                                    });
                                }
                            })
                            .catch(err => {
                                const wrapper = document.getElementById('serviceWrapper<%=rnd%>');
                                if(wrapper) wrapper.innerHTML = '<div class="alert alert-danger m-4 shadow-sm rounded-4 text-center"><i class="fas fa-exclamation-triangle fa-2x mb-2 text-danger"></i><br><b><%= Common.getBahasaConfig("Kegagalan Jaringan API") %></b><br><%= Common.getBahasaConfig("Gagal memproses saluran pembayaran.") %></div>';
                            });
                    });
                </script>
                <% 
                System.out.println("[SMARTLINK DEBUG] Fase 1 Selesai. Menunggu pilihan UI.");
                return; 
            } else {
                System.out.println("[SMARTLINK DEBUG] Fase 2: Menjalankan API Call ke Smartlink...");
                System.out.println("[SMARTLINK DEBUG] Saluran Bank: " + reqChannelCode + " | Waktu: " + reqTimeLimit);
                
                Double feeAdminCari = 0.0;
                String configSmartlinkStr = Common.getKonfigurasi("channel_biaya_e_smartlink", "VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart").getNilai();

                for(String cl : configSmartlinkStr.split(";")) {
                    String[] d = cl.trim().split(":");
                    if(d.length >= 2 && d[0].trim().equals(reqChannelCode)) { feeAdminCari = Double.parseDouble(d[1].trim()); break; }
                }
                
                System.out.println("[SMARTLINK DEBUG] Biaya Admin Terkalkulasi: " + feeAdminCari);

                Date expired_date = null;
                Calendar cal = WaktuUtil.getCalendar();
                if ("15 Menit".equalsIgnoreCase(reqTimeLimit)) cal.add(Calendar.MINUTE, 15);
                else if ("30 Menit".equalsIgnoreCase(reqTimeLimit)) cal.add(Calendar.MINUTE, 30);
                else if ("1 Jam".equalsIgnoreCase(reqTimeLimit)) cal.add(Calendar.HOUR, 1);
                else if ("3 Jam".equalsIgnoreCase(reqTimeLimit)) cal.add(Calendar.HOUR, 3);
                else if ("6 Jam".equalsIgnoreCase(reqTimeLimit)) cal.add(Calendar.HOUR, 6);
                else if ("24 Jam".equalsIgnoreCase(reqTimeLimit)) cal.add(Calendar.HOUR, 24);
                else if ("3 Hari".equalsIgnoreCase(reqTimeLimit)) cal.add(Calendar.DATE, 3);
                else if ("1 Minggu".equalsIgnoreCase(reqTimeLimit)) cal.add(Calendar.DATE, 7);
                else if ("1 Bulan".equalsIgnoreCase(reqTimeLimit)) cal.add(Calendar.MONTH, 1);
                else cal.add(Calendar.HOUR, 24); 
                
                expired_date = cal.getTime();
                System.out.println("[SMARTLINK DEBUG] Waktu Kadaluwarsa: " + expired_date.toString());

                String vaCode = Common.getGeneratedBarCode(30);
                int mytotal = totalTagihanTanpaAdmin.intValue() + feeAdminCari.intValue();

                JSONObject postData = new JSONObject();
                postData.put("order_id", vaCode);
                postData.put("amount", mytotal);
                postData.put("description", jenisKegiatan != null ? jenisKegiatan.getNamaKegiatan() : "Pembayaran Akademik");
                
                JSONObject customer = new JSONObject();
                customer.put("name", namaPelanggan.replaceAll("[^\\sa-zA-Z0-9]", ""));

                String custEmail = ""; String custPhone = "";
                if (isMahasiswa) {
                    Mahasiswa m = (Mahasiswa) person;
                    custEmail = m.getEmail(); custPhone = m.getTelp();
                } else {
                    BiodataCalonMahasiswa c = (BiodataCalonMahasiswa) person;
                    custEmail = c.getEmail(); custPhone = c.getHp();
                }
                if (custEmail == null || custEmail.trim().isEmpty()) custEmail = "no-reply@domain.com";
                else custEmail = custEmail.split(",")[0].split(";")[0];
                if (custPhone == null || custPhone.trim().isEmpty()) custPhone = "080000000000";
                
                customer.put("email", custEmail);
                customer.put("phone", custPhone);
                postData.put("customer", customer);

                if (feeAdminCari.intValue() > 0) {
                    JSONObject adminJson = new JSONObject();
                    adminJson.put("name", "Biaya Admin");
                    adminJson.put("amount", feeAdminCari.intValue());
                    adminJson.put("qty", 1);
                    itemsSmartlink.put(adminJson);
                }

                postData.put("item", itemsSmartlink);

                JSONArray channelArr = new JSONArray();
                channelArr.put(reqChannelCode);
                postData.put("channel", channelArr);

                String linkBase = Common.getRequestHostWithProtocol();
                if (Common.getKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                    linkBase = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
                }

                postData.put("type", "payment-page");
                postData.put("payment_mode", "CLOSE");
                postData.put("expired_time", Common.iso8601.get().format(expired_date));
                postData.put("callback_url", linkBase + "/Esmartlink");
                postData.put("success_redirect_url", linkBase + "/PembayaranSukses");
                postData.put("failed_redirect_url", linkBase + "/PembayaranGagal");

                String strURL = Common.getKonfigurasi("gateway_url_va_e_smartlink", "https://payment-service-sbx.pakar-digital.com/api/payment/create-order").getNilai();
                String username_va = Common.getKonfigurasi("username_va_e_smartlink", "api-smartlink-sbx@budi-mulia.com").getNilai().trim();
                String password_va = Common.getKonfigurasi("password_va_e_smartlink", "sQ3f2PMbGWvNxvi").getNilai().trim();

                System.out.println("[SMARTLINK DEBUG] Mengirim POST Request ke: " + strURL);
                String hasil = VirtualAccountBank.curlSmartlink(strURL, username_va, password_va, postData);
                System.out.println("[SMARTLINK DEBUG] Response Bank: " + hasil);

                JSONObject jSONObject = new JSONObject(hasil);

                if (!(jSONObject.get("code") + "").equals("0")) {
                    throw new Exception(jSONObject.has("message") ? jSONObject.getString("message") : "Terjadi kesalahan integrasi ke Smartlink API");
                }

                JSONObject dataApi = jSONObject.getJSONObject("data");

                va = new VirtualAccountBank();
                va.setRequest(postData.toString());
                va.setResponse(jSONObject.toString());
                va.setLink(dataApi.has("payment_url") ? dataApi.getString("payment_url") : "");
                va.setKode(vaCode);
                va.setBank("Esmartlink");
                va.setOtomatis(false);
                va.setKadaluarsa(expired_date);
                va.setJenisKegiatan(jenisKegiatan);
                va.setTotal(totalTagihanTanpaAdmin);
                va.setBiayaAdmin(feeAdminCari);
                
                // MENGAPLIKASIKAN KETERANGAN DAN CICILAN BERDASARKAN HASIL MOCK GRID
                va.setCicilan(cicilanStr);
                va.setKeterangan(pembStr.isEmpty() ? "Pembayaran Terpadu Smartlink (" + reqChannelCode + ")" : pembStr);
                va.setDetailbiaya(detailbiayaStr);
                
                if (isMahasiswa) va.setMahasiswa((Mahasiswa) person);
                else va.setBiodataCalonMahasiswa((BiodataCalonMahasiswa) person);
                va.setJadwalPembayaran(jadwal);
                va.setSemester(smtInt);
                va.setTahunAkademik(tahunAkademikStr);
                va.setBankHost(bankHost);

                Transaction tx = sess.beginTransaction();
                sess.save(va);
                tx.commit();
                
                System.out.println("[SMARTLINK DEBUG] VA Berhasil Disimpan. ID: " + va.getId());
            }
        } else if (isBankaltimtara && (metodeBPD == null || metodeBPD.trim().isEmpty())) {
            // -------------------------------------------------------------------------------------
            // LOGIKA KHUSUS BANKALTIMTARA (Fase 1 - Pemilihan Antarmuka)
            // -------------------------------------------------------------------------------------
            %>
            <div id="serviceWrapper<%=rnd%>" class="w-100 p-4 animate__animated animate__fadeIn">
                <div class="text-center border-bottom pb-3 mb-4">
                    <div class="bg-primary bg-opacity-10 text-primary rounded-circle d-inline-flex align-items-center justify-content-center mb-2 shadow-sm" style="width: 60px; height: 60px;">
                        <i class="fas fa-university fs-2"></i>
                    </div>
                    <h5 class="fw-bold text-dark mb-0"><%= Common.getBahasaConfig("Proses Pembayaran Bankaltimtara") %></h5>
                    <p class="text-muted small mb-0"><%= Common.getBahasaConfig("Silakan Pilih Metode Pembayaran Anda") %></p>
                </div>

                <div class="row gx-4 justify-content-center">
                    <div class="col-md-8">
                        <form id="frmChannelAltimtara<%=rnd%>">
                            <div class="list-group mb-4 shadow-sm rounded-4">
                                <label class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-3">
                                    <div class="d-flex align-items-center">
                                        <input class="form-check-input me-3" type="radio" name="metodeBPD" value="Virtual Account" required>
                                        <span class="fw-bold text-dark"><%= Common.getBahasaConfig("Virtual Account") %></span>
                                    </div>
                                    <i class="fas fa-money-check-alt text-primary fa-lg"></i>
                                </label>
                                <label class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-3">
                                    <div class="d-flex align-items-center">
                                        <input class="form-check-input me-3" type="radio" name="metodeBPD" value="QRIS" required>
                                        <span class="fw-bold text-dark"><%= Common.getBahasaConfig("QRIS") %></span>
                                    </div>
                                    <i class="fas fa-qrcode text-primary fa-lg"></i>
                                </label>
                            </div>

                            <div class="d-flex justify-content-between border-top pt-3">
                                <button type="button" class="btn btn-light fw-bold shadow-sm rounded-pill px-4" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Batal") %></button>
                                <button type="submit" class="btn btn-success fw-bold shadow-sm rounded-pill px-5">
                                    <i class="fas fa-check-circle me-2"></i><%= Common.getBahasaConfig("Lanjutkan Pembayaran") %>
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <script>
                document.getElementById('frmChannelAltimtara<%=rnd%>').addEventListener('submit', function(e) {
                    e.preventDefault();
                    const formData = new FormData(this);
                    const selMetode = formData.get('metodeBPD');

                    document.getElementById('serviceWrapper<%=rnd%>').innerHTML = '<div class="text-center py-5 animate__animated animate__fadeIn"><div class="spinner-border text-primary mb-3" style="width: 4rem; height: 4rem; border-width: 0.35em;"></div><h5 class="fw-bold text-secondary"><%= Common.getBahasaConfig("Memproses metode pembayaran...") %></h5></div>';

                    const payloadData = '<%=payload%>';
                    const checkoutDaringUrl = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayarmhs&s=_lanjut_bayar_services' +
                                        '&gateway=<%=gateway%>' +
                                        '&id=<%=idStr%>&jkId=<%=jkIdStr%>&smt=<%=smtStr%>&isMahasiswa=<%=isMahasiswaStr%>' +
                                        '&rnd=<%=rnd%>' +
                                        '&payload=' + encodeURIComponent(payloadData) +
                                        '&metodeBPD=' + encodeURIComponent(selMetode);

                    fetch(checkoutDaringUrl)
                        .then(res => res.text())
                        .then(html => {
                            const wrapper = document.getElementById('serviceWrapper<%=rnd%>');
                            if(wrapper) {
                                wrapper.outerHTML = html;
                                const tempDiv = document.createElement('div');
                                tempDiv.innerHTML = html;
                                tempDiv.querySelectorAll('script').forEach(s => {
                                    const ns = document.createElement('script');
                                    if (s.src) ns.src = s.src; else ns.textContent = s.textContent;
                                    document.body.appendChild(ns); document.body.removeChild(ns);
                                });
                            }
                        })
                        .catch(err => {
                            const wrapper = document.getElementById('serviceWrapper<%=rnd%>');
                            if(wrapper) wrapper.innerHTML = '<div class="alert alert-danger m-4 shadow-sm rounded-4 text-center"><i class="fas fa-exclamation-triangle fa-2x mb-2 text-danger"></i><br><b><%= Common.getBahasaConfig("Galat Jaringan") %></b><br><%= Common.getBahasaConfig("Gagal memproses saluran pembayaran.") %></div>';
                        });
                });
            </script>
            <% 
            System.out.println("[BANKALTIMTARA DEBUG] Fase 1 Selesai. Menunggu pilihan UI.");
            return;
        } else { 
            // -------------------------------------------------------------------------------------
            // LOGIKA PEMBAYARAN GATEWAY LAIN & BANKALTIMTARA FASE 2 - Class Java Standar
            // -------------------------------------------------------------------------------------
            System.out.println("[PAYMENT DEBUG] Memproses Gateway Non-Smartlink menggunakan Java Class.");
            
            if (isBankaltimtara && metodeBPD != null) {
                boolean isVa = "Virtual Account".equalsIgnoreCase(metodeBPD);
                Double biayaAdm = 0.0;
                try {
                    biayaAdm = Double.parseDouble(Common.getKonfigurasi("bankaltimtara_biaya_administrasi", "0.0").getNilai());
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar_services.jsp:939");}

                // =================================================================================
                // MANUAL LOOKUP VA LAMA YANG ANTI-GAGAL (TANPA SEMESTER)
                // =================================================================================
                JenisKegiatan targetJk = jadwal != null && jadwal.getJenisKegiatan() != null ? jadwal.getJenisKegiatan() : jenisKegiatan;
                
                System.out.println("\n[PAYMENT DEBUG] === DETIL PARAMETER PENCARIAN VA LAMA ===");
                System.out.println("1. Entitas Pelanggan : " + (isMahasiswa ? "Mahasiswa (ID: " + ((Mahasiswa)person).getId() + ")" : "Calon Mahasiswa (ID: " + ((BiodataCalonMahasiswa)person).getId() + ")"));
                System.out.println("2. Batas Waktu Min   : " + WaktuUtil.getDate());
                System.out.println("3. Jenis Kegiatan ID : " + (targetJk != null ? targetJk.getId() : "NULL"));
                System.out.println("4. Total Tagihan     : " + totalTagihanTanpaAdmin);
                System.out.println("5. Metode Dipilih    : " + (isVa ? "VA" : "QRIS"));

                VirtualAccountBank existingVa = null;
                try {
                    Double batasBawah = totalTagihanTanpaAdmin - 5.0;
                    Double batasAtas = totalTagihanTanpaAdmin + 5.0;

                    org.hibernate.Criteria critVa = sess.createCriteria(VirtualAccountBank.class)
                        .add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
                        .add(Restrictions.or(Restrictions.isNull("terjadiKendala"), Restrictions.eq("terjadiKendala", false)))
                        .add(Restrictions.between("total", batasBawah, batasAtas))
                        .add(Restrictions.isNull("kegiatan"));
                        
                    if (isMahasiswa) {
                        critVa.add(Restrictions.eq("mahasiswa", person));
                    } else {
                        critVa.add(Restrictions.eq("biodataCalonMahasiswa", person));
                    }
                    
                    if (targetJk != null) {
                        critVa.add(Restrictions.eq("jenisKegiatan", targetJk));
                    }
                        
                    critVa.add(Restrictions.or(Restrictions.isNull("pakaiva"), Restrictions.eq("pakaiva", isVa)));
                        
                    existingVa = (VirtualAccountBank) critVa.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
                    
                    if (existingVa != null) {
                        System.out.println("[PAYMENT DEBUG] >> HASIL: DITEMUKAN VA LAMA AKTIF!");
                        System.out.println("   -> ID VA   : " + existingVa.getId());
                        System.out.println("   -> Kode VA : " + existingVa.getKode());
                        System.out.println("   -> Total DB: " + existingVa.getTotal());
                        System.out.println("   -> Expired : " + existingVa.getKadaluarsaWaktu());
                    } else {
                        System.out.println("[PAYMENT DEBUG] >> HASIL: TIDAK DITEMUKAN VA LAMA AKTIF. Akan Dibuat Baru.");
                    }
                } catch (Exception e) {
                    System.out.println("[PAYMENT DEBUG] EXCEPTION SAAT MENCARI VA LAMA: " + e.getMessage());
                }

                if (existingVa != null) {
                    va = existingVa;
                } else {
                    System.out.println("[PAYMENT DEBUG] Mengeksekusi pembuatan VA baru melalui Legacy Class...");
                    if (person instanceof Mahasiswa) {
                        va = ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBankaltimtara.downloadData(
                                (Mahasiswa)person, smtInt, jadwal, detailBiayasToPay, gridCicilanJava,
                                biayaAdm, isVa);
                    } else if (person instanceof BiodataCalonMahasiswa) {
                        va = ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankBankaltimtara.downloadData(
                                (BiodataCalonMahasiswa)person, jadwal, detailBiayasToPay, gridCicilanJava,
                                smtInt, biayaAdm, isVa);
                    }
                }

                if (va != null && va.getId() != null && (va.getBarcode() == null || va.getBarcode().isEmpty())) {
                    try {
                        File fileBarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + va.getId() + ".png");
                        if (va.getBarcode() != null) {
                            BarcodeCommon.generateCRCode(va.getBarcode(), fileBarcode, 600, 600);
                        }
                    } catch(Exception e) {
                        System.out.println("[PAYMENT DEBUG] Gagal generate barcode Bankaltimtara: " + e.getMessage());
                    }
                }
            } else {
                if (person instanceof Mahasiswa) {
                    va = DownloadTagihanMahasiswaBankOnline.downloadData(
                            (Mahasiswa)person, smtInt, jadwal, detailBiayasToPay, gridCicilanJava, 
                            param, biayaAdministrasi, null, null, bankHost);
                } else if (person instanceof BiodataCalonMahasiswa) {
                    if (jkIdStr != null && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && 
                            ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId().toString().equalsIgnoreCase(jkIdStr)) {
                        va = DownloadNoUjianCalonMahasiswaBankOnline.downloadData(
                                (BiodataCalonMahasiswa)person, jadwal, detailBiayasToPay, gridCicilanJava, 
                                smtInt, param, biayaAdministrasi, bankHost);
                    } else if (jkIdStr != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && 
                            ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().toString().equalsIgnoreCase(jkIdStr)) {
                        String waktuSampai = null;
                        va = DownloadNoRegistrasiCalonMahasiswaBankOnline.downloadData(
                                (BiodataCalonMahasiswa)person, jadwal, detailBiayasToPay, 
                                param, biayaAdministrasi, bankHost, waktuSampai);
                    } else {
                        va = DownloadNoUjianCalonMahasiswaBankOnline.downloadData(
                                (BiodataCalonMahasiswa)person, jadwal, detailBiayasToPay, gridCicilanJava, 
                                smtInt, param, biayaAdministrasi, bankHost);
                    }
                }
            }
        }

        // ====================================================================
        // 5. MEMPROSES HASIL & MENGONSTRUKSI URL PENGALIHAN / EMBED
        // ====================================================================
        if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
            out.print("<div class='alert alert-info m-4 shadow-sm rounded-4'><i class='fas fa-info-circle me-2'></i>" + Common.getBahasaConfig("Instruksi pembayaran berhasil dibuat secara senyap.") + "</div>");
            return;
        }

        if (va != null && va.getLink() != null && !va.getLink().trim().isEmpty()) {
            redirectLink = va.getLink();
            isExternalLink = true; 
            
        } else if (va != null && popupUrlPrefix != null) {
            String prefixKode = "";
            String kodeVaUtama = va.getKode();
            
            if (prefixKodeLainConfig != null && !prefixKodeLainConfig.trim().isEmpty()) {
                prefixKode = Common.getKonfigurasi(prefixKodeLainConfig, "").getNilai();
                if (va.getKanalPembayaran() != null && va.getKanalPembayaran().getBsiUsername() != null && !va.getKanalPembayaran().getBsiUsername().isEmpty()) {
                    kodeVaUtama = va.getKanalPembayaran().getBsiUsername() + kodeVaUtama;
                }
                prefixKode = prefixKode + "" + kodeVaUtama;
            }

            Double totalAkhir = va.getTotal() + (va.getBiayaAdmin() != null ? va.getBiayaAdmin() : biayaAdministrasi);
            String qrUrl = Common.getRequestHostWithProtocol() + "/report/crcode_" + va.getId() + ".png";
            
            String rawBarcode = va.getBarcode() != null ? va.getBarcode() : "";
            boolean isQrisSelected = "QRIS".equalsIgnoreCase(metodeBPD) || (!rawBarcode.isEmpty() && rawBarcode.length() > 20);

            redirectLink = Common.ROOT + popupUrlPrefix 
                + "&va=" + URLEncoder.encode(kodeVaUtama != null ? kodeVaUtama : "", "UTF-8") 
                + "&kodeBankLain=" + URLEncoder.encode(prefixKode, "UTF-8")
                + "&nama=" + URLEncoder.encode(namaPelanggan, "UTF-8") 
                + "&nominal=" + URLEncoder.encode("Rp " + Common.numberFormat.get().format(va.getTotal()), "UTF-8") 
                + "&biayaAdministrasi=" + URLEncoder.encode("Rp " + Common.numberFormat.get().format(va.getBiayaAdmin() != null ? va.getBiayaAdmin() : biayaAdministrasi), "UTF-8") 
                + "&biayaTotal=" + URLEncoder.encode("Rp " + Common.numberFormat.get().format(totalAkhir), "UTF-8") 
                + "&terbilang=" + URLEncoder.encode(IndonesianNumberToWords.convert(totalAkhir.longValue()), "UTF-8") 
                + "&kadalurasa=" + URLEncoder.encode(va.getKadaluarsaWaktu() != null ? Common.dateFormat.get().format(va.getKadaluarsaWaktu()) : "-", "UTF-8") 
                + "&qr=" + URLEncoder.encode(qrUrl, "UTF-8") 
                + "&rawBarcode=" + URLEncoder.encode(rawBarcode, "UTF-8")
                + "&isQRIS=" + isQrisSelected
                + "&tampilBiayaAdministrasi=" + (va.getBiayaAdmin() != null ? va.getBiayaAdmin() > 0.1 : biayaAdministrasi > 0.1);
                
            isExternalLink = false;
        }

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar_services.jsp:1090");
        System.out.println("[PAYMENT DEBUG] FATAL EXCEPTION: " + e.getMessage());
        out.print("<div class='alert alert-danger m-4 shadow-sm rounded-4'><i class='fas fa-exclamation-circle me-2'></i>" + Common.getBahasaConfig("Terjadi galat teknis saat menyambungkan layanan: ") + e.getMessage() + "</div>");
        return;
    } finally {
        if (sess != null && sess.isOpen()) {
            sess.disconnect();
            sess.close();
        }
        try { HibernateUtil.closeSessionQuietly(sess); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_lanjut_bayar_services.jsp:1099");}
    }
%>

    <div class="animate__animated animate__fadeIn w-100 p-3 pt-0">
        <% if (va == null && redirectLink == null) { %>
            <div class="text-center py-5">
                <div class="text-danger mb-4"><i class="fas fa-times-circle fa-4x opacity-75"></i></div>
                <h4 class="fw-bold text-dark"><%= Common.getBahasaConfig("Transaksi Gagal Diproses") %></h4>
                <p class="text-muted"><%= Common.getBahasaConfig("Gagal menghubungi layanan penyedia gerbang pembayaran. Silakan periksa kembali rincian tagihan atau coba beberapa saat lagi.") %></p>
                <button type="button" class="btn btn-secondary mt-4 px-5 py-2 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal">
                    <i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Tutup") %>
                </button>
            </div>
        <% } else { %>
            <div class="text-center mb-3">
                <div class="text-success mb-2"><i class="fas fa-check-circle fa-3x"></i></div>
                <h5 class="fw-bold text-dark"><%= Common.getBahasaConfig("Saluran Pembayaran Berhasil Disiapkan") %></h5>
            </div>
            
            <% if (isExternalLink) { %>
                <div class="w-100 shadow-sm border rounded-4 overflow-hidden bg-white mb-4" style="height: 65vh; min-height: 400px;">
                    <iframe src="<%= redirectLink %>" width="100%" height="100%" style="border: none;" allowfullscreen></iframe>
                </div>
            <% } else { %>
                <div id="embeddedVaContainer<%=rnd%>" class="w-100 mb-4 text-start">
                    <div class="text-center py-5">
                        <div class="spinner-border text-primary mb-3" style="width: 3rem; height: 3rem; border-width: 0.35em;"></div>
                        <p class="text-muted fw-bold"><%= Common.getBahasaConfig("Memuat instruksi Virtual Account...") %></p>
                    </div>
                </div>
                
                <script>
                    setTimeout(function() {
                        fetch('<%= redirectLink %>')
                            .then(res => res.text())
                            .then(html => {
                                const container = document.getElementById('embeddedVaContainer<%=rnd%>');
                                if(container) {
                                    container.innerHTML = html;
                                    container.querySelectorAll('script').forEach(s => {
                                        const ns = document.createElement('script');
                                        if (s.src) ns.src = s.src; else ns.textContent = s.textContent;
                                        document.body.appendChild(ns);
                                        document.body.removeChild(ns);
                                    });
                                }
                            })
                            .catch(err => {
                                const container = document.getElementById('embeddedVaContainer<%=rnd%>');
                                if(container) container.innerHTML = '<div class="alert alert-danger m-0 shadow-sm rounded-4 text-center"><i class="fas fa-exclamation-triangle me-2"></i><%= Common.getBahasaConfig("Gagal memuat rincian Virtual Account.") %></div>';
                            });
                    }, 300);
                </script>
            <% } %>

            <div class="d-flex justify-content-center border-top pt-3">
                <button type="button" class="btn btn-secondary px-5 py-2 rounded-pill fw-bold shadow-sm" onclick="if(typeof window.loadTagihanMhs<%=rnd%> === 'function') window.loadTagihanMhs<%=rnd%>(true);" data-bs-dismiss="modal">
                    <i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Tutup & Segarkan Riwayat") %>
                </button>
            </div>
        <% } %>
    </div>