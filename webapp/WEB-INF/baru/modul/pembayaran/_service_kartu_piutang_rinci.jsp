<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.ItemBiaya"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.math.BigInteger"%>
<%
    System.out.println("\n[DEBUG_KARTU_PIUTANG] === MULAI PROSES _service_kartu_piutang_rinci.jsp ===");
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kartu_piutang_rinci.jsp:14");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    
    if (Common.getCurrentUser(request) == null) { 
        System.out.println("[DEBUG_KARTU_PIUTANG] ERROR: Sesi Pengguna Berakhir / Null.");
        out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); 
        return; 
    }

    String ta = request.getParameter("tahunAkademik");
    String smt = request.getParameter("semester");
    String fakId = request.getParameter("fakultasId"); 
    String jurId = request.getParameter("jurusanId");
    String jkId = request.getParameter("jkId");
    String q = request.getParameter("q"); 
    
    String startStr = request.getParameter("start");
    String limitStr = request.getParameter("limit");
    
    System.out.println("[DEBUG_KARTU_PIUTANG] Parameter Diterima -> TA: " + ta + ", SMT: " + smt + 
                       ", FAK: " + fakId + ", JUR: " + jurId + ", JK: " + jkId + ", Q: " + q + 
                       ", START: " + startStr + ", LIMIT: " + limitStr);

    int offsetNum = 0; 
    int limitNum = 500; 
    
    try { if (startStr != null && !startStr.trim().isEmpty()) offsetNum = Integer.parseInt(startStr); } catch(Exception e){ System.out.println("[DEBUG_KARTU_PIUTANG] WARN: Gagal parsing startStr"); }
    try { if (limitStr != null && !limitStr.trim().isEmpty()) limitNum = Integer.parseInt(limitStr); } catch(Exception e){ System.out.println("[DEBUG_KARTU_PIUTANG] WARN: Gagal parsing limitStr"); }
    
    if (limitNum > 5000) {
        System.out.println("[DEBUG_KARTU_PIUTANG] ERROR: Limit melebihi batas maksimal (> 5000).");
        out.print("{\"status\":\"error\", \"message\":\"Batas maksimal penampilan data adalah 5.000 baris.\"}");
        out.flush();
        return;
    }

    Session sess = null;
    try {
        System.out.println("[DEBUG_KARTU_PIUTANG] Membuka Session Hibernate...");
        sess = HibernateUtil.getSessionFactory().openSession();

        // 1. Membangun Kriteria SQL Dasar
        String baseWhereClause = " from kegiatan a left join biodata_calon_mahasiswa b on (a.calon_mahasiswa = b.id) "
                + " left join mahasiswa c on (a.mahasiswa = c.id) "
                + " inner join jenis_kegiatan d on (a.jenis_kegiatan=d.id) "
                + " left join jurusan x on (a.jurusan = x.id) "
                + " where a.aktif=true and (a.dibayar > 0.1 or a.tagihan > 0.1) ";

        if (q != null && !q.trim().isEmpty()) {
            baseWhereClause += " and (c.nama ilike :buktilike or b.nama ilike :buktilike or c.nim ilike :buktilike or b.no_registrasi ilike :buktilike) ";
        }

        if (jurId != null && !jurId.isEmpty()) {
            baseWhereClause += " and a.jurusan = " + jurId;
        } else if (fakId != null && !fakId.isEmpty()) {
            baseWhereClause += " and x.fakultas = " + fakId;
        }

        if (jkId != null && !jkId.isEmpty()) {
            baseWhereClause += " and a.jenis_kegiatan = " + jkId;
        }
        
        if (ta != null && !ta.isEmpty()) {
            baseWhereClause += " and a.tahun_akademik = :tahunAkademik ";
        }
        
        if (smt != null && !smt.isEmpty()) {
            baseWhereClause += (smt.equals("Genap") ? " and a.semster % 2 = 0 " : " and a.semster % 2 = 1 ");
        }

        System.out.println("[DEBUG_KARTU_PIUTANG] baseWhereClause -> " + baseWhereClause);

        // 2. Hitung Total Records
        String sqlCountStr = "select count(*) " + baseWhereClause;
        System.out.println("[DEBUG_KARTU_PIUTANG] Eksekusi SQL Count -> " + sqlCountStr);
        
        org.hibernate.SQLQuery qCount = sess.createSQLQuery(sqlCountStr);
        if (q != null && !q.trim().isEmpty()) qCount.setString("buktilike", "%" + q.trim() + "%");
        if (ta != null && !ta.isEmpty()) qCount.setString("tahunAkademik", ta);
        
        BigInteger totalRecordsObj = (BigInteger) qCount.uniqueResult();
        int totalRecords = totalRecordsObj != null ? totalRecordsObj.intValue() : 0;
        int totalPages = (int) Math.ceil((double) totalRecords / limitNum);
        int currentPage = (offsetNum / limitNum) + 1;
        
        System.out.println("[DEBUG_KARTU_PIUTANG] Total Records: " + totalRecords + " | Total Pages: " + totalPages + " | Current Page: " + currentPage);

        // 3. Ambil Data
        String sqlData = "select (case when c.nim is not null then c.nim else b.no_registrasi end) as kode_transaksi, "
                + "(case when c.nama is not null then c.nama else b.nama end) as nama, "
                + "d.nama_kegiatan as nama_jenis_kegiatan, "
                + "a.tahun_akademik, a.semster, a.jenis_kegiatan, a.tagihans, a.bulans "
                + baseWhereClause
                + " order by d.nama_kegiatan, (case when c.nama is not null then c.nama else b.nama end) "
                + " limit " + limitNum + " offset " + offsetNum;

        System.out.println("[DEBUG_KARTU_PIUTANG] Eksekusi SQL Data -> " + sqlData);

        org.hibernate.SQLQuery qData = sess.createSQLQuery(sqlData);
        if (q != null && !q.trim().isEmpty()) qData.setString("buktilike", "%" + q.trim() + "%");
        if (ta != null && !ta.isEmpty()) qData.setString("tahunAkademik", ta);
        
        List<Object[]> objects = qData.list();
        System.out.println("[DEBUG_KARTU_PIUTANG] Jumlah Data Fetched (List Size): " + objects.size());
        
        JSONArray dataArray = new JSONArray();

        // 4. Looping data dan membedah tagihans/bulans
        int loopIndex = 0;
        for (Object[] o : objects) {
            loopIndex++;
            try {
                String kode_transaksi = o[0] == null ? "" : o[0].toString();
                String nama = o[1] == null ? "" : o[1].toString();
                String nama_jenis_kegiatan = o[2] == null ? "" : o[2].toString();
                String tahun_akademik = o[3] == null ? "" : o[3].toString();
                Integer semster = o[4] == null ? 0 : ((Number) o[4]).intValue();
                Number idJkNum = o[5] == null ? 0 : (Number) o[5];
                String tagihansJsonStr = o[6] == null || o[6].toString().isEmpty() ? "{}" : o[6].toString();
                String bulansJsonStr = o[7] == null || o[7].toString().isEmpty() ? "{}" : o[7].toString();

                JenisKegiatan jenisKegiatanObj = null;
                if (idJkNum.longValue() > 0) {
                    jenisKegiatanObj = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(), idJkNum.longValue(), true);
                }

                JSONObject jTagihan = new JSONObject(tagihansJsonStr);
                JSONObject jDibayar = new JSONObject(bulansJsonStr);
                
                System.out.println("[DEBUG_KARTU_PIUTANG] jTagihan: " +jTagihan);
                System.out.println("[DEBUG_KARTU_PIUTANG] jDibayar: " +jDibayar);

                int b = 0; int a = 0;
                Iterator<String> iterCount = jTagihan.keys();
                while (iterCount.hasNext()) {
                    String k = iterCount.next();
                    String v = jTagihan.optString(k, "");
                    if (!v.isEmpty() && !v.equals("null")) {
                        if (k.contains("_")) { b++; } else { a++; }
                    }
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
                            
                            // Akumulasi Pembayaran (Logic: split length 3 dan prefix matching)
                            Iterator<String> pKeys = jDibayar.keys();
                            while(pKeys.hasNext()) {
                                String pk = pKeys.next();
                                String pValStr = jDibayar.optString(pk, "");
                                if (!pValStr.isEmpty() && !pValStr.equals("null") && pk.split("_").length == 3) {
                                    if (pk.startsWith(key + "_")) {
                                        double v = Double.parseDouble(pValStr);
                                        if (v > 0.0) valDibayarItem += v;
                                    }
                                }
                            }

                            if (valTagihanItem == 0.0 && valDibayarItem > 0.0) valTagihanItem = valDibayarItem;
                            if (valDibayarItem > valTagihanItem) valDibayarItem = valTagihanItem;

                            double sisaV = valTagihanItem - valDibayarItem;
                            if (sisaV < 0) sisaV = 0.0;
                            
                            if (valTagihanItem > 0 || valDibayarItem > 0) {
                                // =========================================================================
                                // LOGIKA PENGAMBILAN NAMA ITEM BIAYA BERDASARKAN ID (SPLIT FRONT POSITION)
                                // =========================================================================
                                String idItemStr = key.split("_")[0];
                                String namaItemTampil = "Komponen " + idItemStr;
                                try {
                                    Long idItem = Long.parseLong(idItemStr);
                                    ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.ambil(ItemBiaya.class.getName(), idItem);
                                    if (itemBiaya != null) {
                                        namaItemTampil = itemBiaya.getNama();
                                    }
                                } catch (Exception ex) {
                                    System.err.println("[DEBUG_KARTU_PIUTANG] WARN: Gagal ambil ItemBiaya untuk ID: " + idItemStr);
                                }

                                JSONObject barisData = new JSONObject();
                                barisData.put("kodeTransaksi", kode_transaksi);
                                barisData.put("nama", nama);
                                barisData.put("jenisKegiatan", nama_jenis_kegiatan);
                                barisData.put("itemBiaya", namaItemTampil); 
                                barisData.put("bulan", "-"); 
                                barisData.put("ta", tahun_akademik);
                                barisData.put("smt", semster);
                                barisData.put("tagihan", valTagihanItem);
                                barisData.put("dibayar", valDibayarItem);
                                barisData.put("sisa", sisaV);
                                dataArray.put(barisData);
                            }
                        }
                    } catch(Exception e) {
                    	e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_kartu_piutang_rinci.jsp:231");
                        System.err.println("[DEBUG_KARTU_PIUTANG] ERROR saat parsing data tagihan iterasi ke-" + loopIndex + ": " + e.getMessage());
                    }
                }
                jTagihan = null; jDibayar = null;
            } catch (Exception e) { 
                System.err.println("[DEBUG_KARTU_PIUTANG] ERROR Fatal di baris data ke-" + loopIndex + ": " + e.getMessage());
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_kartu_piutang_rinci.jsp:238"); 
            }
        }
        
        System.out.println("[DEBUG_KARTU_PIUTANG] Semua data berhasil diparsing. Jumlah Baris JSON yg dirakit: " + dataArray.length());

        JSONObject res = new JSONObject();
        res.put("status", "00"); 
        res.put("data", dataArray);
        res.put("totalRecords", totalRecords);
        res.put("totalPages", totalPages);
        res.put("currentPage", currentPage);
        res.put("offset", offsetNum);
        res.put("limit", limitNum);
        
        out.print(res.toString()); 
        out.flush();

    } catch (Exception e) {
        System.err.println("[DEBUG_KARTU_PIUTANG] EXCEPTION CAUGHT DI BLOK UTAMA: " + e.getMessage());
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_kartu_piutang_rinci.jsp:258");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat sistem.\"}");
    } finally {
        System.out.println("[DEBUG_KARTU_PIUTANG] Membersihkan dan Menutup Session Hibernate...");
        if(sess != null) {
            try { sess.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kartu_piutang_rinci.jsp:263");}
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kartu_piutang_rinci.jsp:264");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kartu_piutang_rinci.jsp:265");}
        }
        try { HibernateUtil.closeSession(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_kartu_piutang_rinci.jsp:267");}
        System.out.println("[DEBUG_KARTU_PIUTANG] === SELESAI PROSES ===\n");
    }
%>