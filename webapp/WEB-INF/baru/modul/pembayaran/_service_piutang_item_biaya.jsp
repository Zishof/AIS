<%@page import="ais.database.model.ItemBiaya"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Iterator"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>

<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:16");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    if (Common.getCurrentUser(request) == null) { out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; }

    String ta = request.getParameter("tahunAkademik");
    String smt = request.getParameter("semester");
    String fakId = request.getParameter("fakultasId");
    String jurId = request.getParameter("jurusanId");
    String jkId = request.getParameter("jkId");
    String q = request.getParameter("q"); 
    
    Session sess = null;
    try {
        // Menggunakan openSession() untuk isolasi memori yang lebih optimal
        sess = HibernateUtil.getSessionFactory().openSession();

        // 1. Susun Kueri Pencarian (Native SQL)
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

        // Tambahkan a.jenis_kegiatan ke klausul select untuk cek Angsuran
        String sqlData = "select a.tagihans, a.bulans, d.nama_kegiatan, a.jenis_kegiatan " + baseWhereClause;
        org.hibernate.SQLQuery qData = sess.createSQLQuery(sqlData);
        
        if (q != null && !q.trim().isEmpty()) qData.setString("buktilike", "%" + q.trim() + "%");
        if (ta != null && !ta.isEmpty()) qData.setString("tahunAkademik", ta);
        
        List<Object[]> objects = qData.list();
        
        // 2. Persiapan Penampung Akumulasi di Memori Peladen
        class ItemRecord {
            String namaItem;
            String namaJenisKegiatan;
            double tagihan = 0;
            double dibayar = 0;
            int jmlTagihan = 0;
            int jmlDibayar = 0;
            int jmlPiutang = 0;
        }

        Map<String, ItemRecord> mapAgregat = new HashMap<String, ItemRecord>();
        
        // 3. Proses Ekstraksi JSON ke Agregat Grup dengan Logika Parsing Baru
        for (Object[] o : objects) {
            try {
                String tagihansJsonStr = o[0] == null || o[0].toString().isEmpty() ? "{}" : o[0].toString();
                String bulansJsonStr = o[1] == null || o[1].toString().isEmpty() ? "{}" : o[1].toString();
                String nama_jenis_kegiatan = o[2] == null ? "" : o[2].toString();
                Number idJkNum = o[3] == null ? 0 : (Number) o[3];

                JenisKegiatan jenisKegiatanObj = null;
                if (idJkNum.longValue() > 0) {
                    jenisKegiatanObj = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(), idJkNum.longValue(), true);
                }

                JSONObject jTagihan = new JSONObject(tagihansJsonStr);
                JSONObject jDibayar = new JSONObject(bulansJsonStr);

                int b = 0; int a = 0;
                Iterator<String> iterCount = jTagihan.keys();
                while (iterCount.hasNext()) {
                    try {
                        String k = iterCount.next();
                        String v = jTagihan.optString(k, "");
                        if (!v.isEmpty() && !v.equals("null")) {
                            if (k.contains("_")) { b++; } else { a++; }
                        }
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:108");}
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
                            
                            // Akumulasi Pembayaran dari kolom bulans
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
                                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:148");}
                            }

                            if (valTagihanItem == 0.0 && valDibayarItem > 0.0) valTagihanItem = valDibayarItem;
                            if (valDibayarItem > valTagihanItem) valDibayarItem = valTagihanItem;

                            double sisaV = valTagihanItem - valDibayarItem;
                            if (sisaV < 0) sisaV = 0.0;
                            
                            if (valTagihanItem > 0 || valDibayarItem > 0) {
                                // Ekstraksi nama Item Biaya dengan aman & hemat memori
                                String idItemStr = key.split("_")[0];
                                String namaItemTampil = "Komponen " + idItemStr;
                                try {
                                    Long idItem = Long.parseLong(idItemStr);
                                    ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.ambil(ItemBiaya.class, idItem);
                                    if (itemBiaya != null) {
                                        namaItemTampil = itemBiaya.getNama();
                                    }
                                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:167");}

                                // Penggabungan Aggregate Map Key
                                String mapKey = idItemStr + "_" + nama_jenis_kegiatan;
                                ItemRecord rec = mapAgregat.get(mapKey);
                                if(rec == null) {
                                    rec = new ItemRecord();
                                    rec.namaItem = namaItemTampil;
                                    rec.namaJenisKegiatan = nama_jenis_kegiatan;
                                    mapAgregat.put(mapKey, rec);
                                }
                                
                                rec.tagihan += valTagihanItem;
                                rec.dibayar += valDibayarItem;
                                rec.jmlTagihan++;
                                if (valDibayarItem > 0.01) rec.jmlDibayar++;
                                if (sisaV > 0.01) rec.jmlPiutang++;
                            }
                        }
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:186");}
                }
                
                // Mendorong garbage collector dengan melepaskan variabel json
                jTagihan = null;
                jDibayar = null;
                
            } catch (Exception e) {
                // Jangan batalkan semua baris hanya karena 1 baris yang corrupt
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:195");
            }
        }
        
        objects.clear(); // Bersihkan memori List Data
        
        // 4. Perakitan Hasil ke Format Front-end 
        JSONArray dataArray = new JSONArray();
        for (ItemRecord r : mapAgregat.values()) {
            JSONObject barisData = new JSONObject();
            barisData.put("kelompok", r.namaItem);
            barisData.put("jenisKegiatan", r.namaJenisKegiatan);
            barisData.put("tagihan", r.tagihan);
            barisData.put("dibayar", r.dibayar);
            barisData.put("piutang", r.tagihan - r.dibayar);
            barisData.put("jmlTagihan", r.jmlTagihan);
            barisData.put("jmlDibayar", r.jmlDibayar);
            barisData.put("jmlPiutang", r.jmlPiutang);
            barisData.put("persen", r.tagihan > 0 ? (r.dibayar * 100.0) / r.tagihan : 0.0);
            dataArray.put(barisData);
        }
        
        JSONObject res = new JSONObject(); 
        res.put("status", "00"); 
        res.put("data", dataArray);
        
        out.print(res.toString()); 
        out.flush();
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:225");
        out.print("{\"status\":\"error\", \"message\":\"Terjadi galat pada peladen.\"}");
    } finally {
        // Blok Pembersihan Memori yang Sangat Ketat dan Aman (Java 1.6 & 1.7 friendly)
        if(sess != null) { 
            try { if (sess.isOpen()) sess.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:230");} 
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:231");} 
            try { if (sess.isOpen()) sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:232");} 
        }
        try { HibernateUtil.closeSession(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_piutang_item_biaya.jsp:234");}
    }
%>