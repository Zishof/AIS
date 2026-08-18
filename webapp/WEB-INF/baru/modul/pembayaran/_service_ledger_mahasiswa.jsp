<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.ItemBiaya"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.math.BigDecimal"%>

<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:15");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    if (Common.getCurrentUser(request) == null) { out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; }

    String qNIM = request.getParameter("q"); // NIM atau Nama
    String ta = request.getParameter("tahunAkademik");
    String smt = request.getParameter("semester");
    String jkId = request.getParameter("jkId");
    String strHanyaBelumLunas = request.getParameter("hanyaBelumLunas");
    String action = request.getParameter("action");
    
    boolean hanyaBelumLunas = "true".equalsIgnoreCase(strHanyaBelumLunas);

    if (qNIM == null || qNIM.trim().isEmpty()) {
        out.print("{\"status\":\"error\", \"message\":\"Identitas (NIM atau Nama) wajib diisi.\"}");
        return;
    }

    Session sess = null;

    try {
        sess = HibernateUtil.getSessionFactory().openSession();

        // =========================================================================
        // BLOK PENCARIAN AUTOCOMPLETE
        // =========================================================================
        if ("SEARCH_MHS".equals(action)) {
            String sqlSearch = "SELECT nim, nama FROM mahasiswa WHERE nim ILIKE :q OR nama ILIKE :q " +
                               "UNION " +
                               "SELECT no_registrasi as nim, nama FROM biodata_calon_mahasiswa WHERE no_registrasi ILIKE :q OR nama ILIKE :q " +
                               "LIMIT 15";
            org.hibernate.SQLQuery qry = sess.createSQLQuery(sqlSearch);
            qry.setString("q", "%" + qNIM.trim() + "%");
            List<Object[]> rows = qry.list();
            
            JSONArray arr = new JSONArray();
            for(Object[] row : rows) {
                JSONObject o = new JSONObject();
                o.put("nim", row[0] != null ? row[0].toString() : "");
                o.put("nama", row[1] != null ? row[1].toString() : "");
                arr.put(o);
            }
            
            JSONObject res = new JSONObject();
            res.put("status", "00");
            res.put("data", arr);
            out.print(res.toString());
            out.flush();
            return;
        }

        // =========================================================================
        // 1. Ekstrak Identitas Mahasiswa Utama
        // =========================================================================
        String sqlCariMhs = "SELECT id, nim, nama, (select nama from jurusan where id=jurusan) as prodi, " +
                "(select nama from jenjang where id=(select jenjang from jurusan where id=jurusan)) as jenjang, " +
                "tahunangkatan, program, (select nama from status_awal_mahasiswa where id=status_awal_mahasiswa) as st_awal " +
                "FROM mahasiswa WHERE nim = :nim OR nama ILIKE :nimLike limit 1";
                
        org.hibernate.SQLQuery qMhs = sess.createSQLQuery(sqlCariMhs);
        qMhs.setString("nim", qNIM.trim());
        qMhs.setString("nimLike", "%" + qNIM.trim() + "%");
        Object[] mhsRow = (Object[]) qMhs.uniqueResult();

        Long idMhs = null;
        JSONObject profilMhs = new JSONObject();

        if (mhsRow != null) {
            idMhs = ((Number) mhsRow[0]).longValue();
            profilMhs.put("id", idMhs);
            profilMhs.put("nim", mhsRow[1] != null ? mhsRow[1].toString() : "-");
            profilMhs.put("nama", mhsRow[2] != null ? mhsRow[2].toString() : "-");
            profilMhs.put("prodi", mhsRow[3] != null ? mhsRow[3].toString() : "-");
            profilMhs.put("jenjang", mhsRow[4] != null ? mhsRow[4].toString() : "-");
            profilMhs.put("tahunMasuk", mhsRow[5] != null ? mhsRow[5].toString() : "-");
            profilMhs.put("program", mhsRow[6] != null ? mhsRow[6].toString() : "-");
            profilMhs.put("statusAwal", mhsRow[7] != null ? mhsRow[7].toString() : "-");
        } else {
            String sqlCariCalon = "SELECT id, no_registrasi, nama, " +
                "(select nama from jurusan where id=coalesce(prodi_lulus, prodi_1, prodi_2)) as prodi, " +
                "(select nama from jenjang where id=jenjang) as jenjang, " +
                "tahunakademik, program, 'Pendaftar Baru' as st_awal " +
                "FROM biodata_calon_mahasiswa WHERE no_registrasi = :nim OR nama ILIKE :nimLike limit 1";
            org.hibernate.SQLQuery qCalon = sess.createSQLQuery(sqlCariCalon);
            qCalon.setString("nim", qNIM.trim());
            qCalon.setString("nimLike", "%" + qNIM.trim() + "%");
            Object[] calonRow = (Object[]) qCalon.uniqueResult();
            
            if (calonRow != null) {
                idMhs = ((Number) calonRow[0]).longValue();
                profilMhs.put("id", idMhs);
                profilMhs.put("nim", calonRow[1] != null ? calonRow[1].toString() : "-");
                profilMhs.put("nama", calonRow[2] != null ? calonRow[2].toString() : "-");
                profilMhs.put("prodi", calonRow[3] != null ? calonRow[3].toString() : "-");
                profilMhs.put("jenjang", calonRow[4] != null ? calonRow[4].toString() : "-");
                profilMhs.put("tahunMasuk", calonRow[5] != null ? calonRow[5].toString() : "-");
                profilMhs.put("program", calonRow[6] != null ? calonRow[6].toString() : "-");
                profilMhs.put("statusAwal", calonRow[7] != null ? calonRow[7].toString() : "-");
            } else {
                out.print("{\"status\":\"error\", \"message\":\"Data mahasiswa dengan identitas '" + qNIM + "' tidak ditemukan.\"}");
                return;
            }
        }

        // =========================================================================
        // 2. Tarik Data Utama JSON dari Tabel Kegiatan
        // =========================================================================
        String sqlKegiatan = "SELECT k.id, k.semster, jk.nama_kegiatan, k.tahun_akademik, " +
                "k.tagihans, k.bulans, k.jenis_kegiatan " +
                "FROM kegiatan k JOIN jenis_kegiatan jk ON k.jenis_kegiatan = jk.id " +
                "WHERE k.aktif=true AND (k.mahasiswa = :idm OR k.calon_mahasiswa = :idm) ";

        if (jkId != null && !jkId.isEmpty()) sqlKegiatan += " AND jk.id = " + jkId;
        if (ta != null && !ta.isEmpty()) sqlKegiatan += " AND k.tahun_akademik = '" + ta.replace("'", "''") + "' ";
        if (smt != null && !smt.isEmpty()) {
            sqlKegiatan += (smt.equals("Genap") ? " AND k.semster % 2 = 0 " : " AND k.semster % 2 = 1 ");
        }
        sqlKegiatan += " ORDER BY k.tahun_akademik ASC, k.semster ASC";

        org.hibernate.SQLQuery qKeg = sess.createSQLQuery(sqlKegiatan);
        qKeg.setLong("idm", idMhs);
        List<Object[]> kegiatanRows = qKeg.list();

        JSONArray outputGrupArray = new JSONArray();

        // =========================================================================
        // 3. Pembelahan (*Parsing*) JSON Berdasarkan Logika Key 
        // =========================================================================
        for (Object[] rKeg : kegiatanRows) {
            String semsterStr = rKeg[1] != null ? rKeg[1].toString() : "0";
            String namaJk = rKeg[2] != null ? rKeg[2].toString() : "-";
            String tahunAka = rKeg[3] != null ? rKeg[3].toString() : "-";
            
            String tagihansJsonStr = rKeg[4] != null && !rKeg[4].toString().isEmpty() ? rKeg[4].toString() : "{}";
            String bulansJsonStr = rKeg[5] != null && !rKeg[5].toString().isEmpty() ? rKeg[5].toString() : "{}";
            Number idJkNum = rKeg[6] != null ? (Number) rKeg[6] : 0;

            JenisKegiatan jenisKegiatanObj = null;
            if (idJkNum.longValue() > 0) {
                jenisKegiatanObj = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(), idJkNum.longValue(), true);
            }

            JSONObject jTagihan = new JSONObject(tagihansJsonStr);
            JSONObject jDibayar = new JSONObject(bulansJsonStr);

            double grupTagihanAkhir = 0;
            double grupDibayar = 0;
            double grupSisa = 0;
            
            JSONArray rincianItemArray = new JSONArray();

            int b = 0; int a = 0;
            Iterator<String> iterCount = jTagihan.keys();
            while (iterCount.hasNext()) {
                try {
                    String k = iterCount.next();
                    String v = jTagihan.optString(k, "");
                    if (!v.isEmpty() && !v.equals("null")) {
                        if (k.contains("_")) { b++; } else { a++; }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:175");}
            }

            boolean isAngsuran = jenisKegiatanObj != null && Boolean.TRUE.equals(jenisKegiatanObj.getHanyaBerupaAngsuran());
            
            Iterator<String> iterData = jTagihan.keys();
            while (iterData.hasNext()) {
                try {
                    String key = iterData.next();
                    String valTagihanStr = jTagihan.optString(key, "");
                    
                    boolean validKey = false;
                    if (isAngsuran) {
                        if (!valTagihanStr.isEmpty() && !valTagihanStr.equals("null") && key.contains("_")) validKey = true;
                    } else {
                        if (!valTagihanStr.isEmpty() && !valTagihanStr.equals("null")) {
                            if (b > a) {
                                if (key.contains("_")) validKey = true;
                            } else {
                                validKey = true;
                            }
                        }
                    }

                    if (validKey) {
                        double valTagihanItem = Double.parseDouble(valTagihanStr);
                        double valDibayarItem = 0.0;
                        
                        // Akumulasi Pembayaran (Mencocokkan panjang split dan awalan)
                        Iterator<String> pKeys = jDibayar.keys();
                        while(pKeys.hasNext()) {
                            try {
                                String pk = pKeys.next();
                                String pValStr = jDibayar.optString(pk, "");
                                if (!pValStr.isEmpty() && !pValStr.equals("null") && pk.split("_").length == 3) {
                                    if (pk.startsWith(key + "_")) {
                                        double v = Double.parseDouble(pValStr);
                                        if (v > 0.0) valDibayarItem += v;
                                    }
                                }
                            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:215");}
                        }

                        if (valTagihanItem == 0.0 && valDibayarItem > 0.0) valTagihanItem = valDibayarItem;
                        if (valDibayarItem > valTagihanItem) valDibayarItem = valTagihanItem;

                        double sisaHutang = valTagihanItem - valDibayarItem;
                        if (sisaHutang < 0) sisaHutang = 0.0;
                        
                        if (hanyaBelumLunas && sisaHutang <= 0.01) continue; 

                        if (valTagihanItem > 0 || valDibayarItem > 0) {
                            grupTagihanAkhir += valTagihanItem;
                            grupDibayar += valDibayarItem;
                            grupSisa += sisaHutang;

                            String idItemStr = key.split("_")[0];
                            String namaItemTampil = "Komponen " + idItemStr;
                            try {
                                Long idItem = Long.parseLong(idItemStr);
                                ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.ambil(ItemBiaya.class.getName(), idItem);
                                if (itemBiaya != null) {
                                    namaItemTampil = itemBiaya.getNama();
                                }
                            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:239");}

                            JSONObject objRinci = new JSONObject();
                            objRinci.put("keyId", key);
                            objRinci.put("namaItem", namaItemTampil);
                            objRinci.put("tagihanAwal", valTagihanItem);
                            objRinci.put("potonganBeasiswa", 0.0); 
                            objRinci.put("tagihanAkhir", valTagihanItem);
                            objRinci.put("dibayar", valDibayarItem);
                            objRinci.put("sisa", sisaHutang);
                            objRinci.put("denda", 0); 
                            objRinci.put("persen", valTagihanItem > 0 ? Math.round((valDibayarItem / valTagihanItem) * 100.0) : 0);
                            
                            rincianItemArray.put(objRinci);
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:255");}
            }
            jTagihan = null; jDibayar = null;

            if (rincianItemArray.length() > 0) {
                JSONObject objGrup = new JSONObject();
                objGrup.put("jenisKegiatanNama", namaJk);
                objGrup.put("semester", semsterStr);
                objGrup.put("tahunAkademik", tahunAka);
                objGrup.put("totalTagihanAkhir", grupTagihanAkhir);
                objGrup.put("totalDibayar", grupDibayar);
                objGrup.put("totalSisa", grupSisa);
                objGrup.put("persenTotal", grupTagihanAkhir > 0 ? Math.round((grupDibayar / grupTagihanAkhir) * 100.0) : 0);
                objGrup.put("rincian", rincianItemArray);
                
                outputGrupArray.put(objGrup);
            }
        }
        
        kegiatanRows.clear(); // Bersihkan memori

        JSONObject res = new JSONObject(); 
        res.put("status", "00"); 
        res.put("profilMahasiswa", profilMhs);
        res.put("data", outputGrupArray);
        
        out.print(res.toString()); 
        out.flush();
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:285");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat sistem saat membaca Buku Besar Ledger mahasiswa: " + e.getMessage() + "\"}");
    } finally {
        if(sess != null) {
            try { if (sess.isOpen()) sess.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:289");}
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:290");}
            try { if (sess.isOpen()) sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:291");}
        }
        try { HibernateUtil.closeSession(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_ledger_mahasiswa.jsp:293");}
    }
%>