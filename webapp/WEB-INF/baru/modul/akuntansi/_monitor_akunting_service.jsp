<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, java.text.*, org.hibernate.*, org.hibernate.criterion.*, org.json.*" %>
<%@ page import="ais.common.*, ais.database.hibernate.*, ais.database.model.akunting.*, ais.database.model.rab.*" %>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    JSONObject jsonResponse = new JSONObject();
    Session sess = null;

    try {
        String action = request.getParameter("action");
        sess = HibernateUtil.openSession();

        // =================================================================================
        // ACTION: INIT FILTER 
        // =================================================================================
        if ("init_filter".equals(action)) {
            Criteria critSk = sess.createCriteria(SatuanKerja.class).addOrder(Order.asc("nama"));
            List<SatuanKerja> listSk = critSk.list();
            JSONArray arrSk = new JSONArray();
            for(SatuanKerja sk : listSk) {
                JSONObject o = new JSONObject();
                o.put("id", sk.getId());
                o.put("nama", sk.getNama());
                arrSk.put(o);
            }
            
            Criteria critJl = sess.createCriteria(JenisLaporan.class)
                                  .add(Restrictions.eq("tampilDiDashboard", true))
                                  .addOrder(Order.asc("nama"));
            List<JenisLaporan> listJl = critJl.list();
            JSONArray arrJl = new JSONArray();
            for(JenisLaporan jl : listJl) {
                JSONObject o = new JSONObject();
                o.put("id", jl.getId());
                o.put("nama", jl.getNama());
                o.put("checked", jl.getTampilDiDashboard());
                arrJl.put(o);
            }
            
            jsonResponse.put("status", "success");
            jsonResponse.put("satuan_kerja", arrSk);
            jsonResponse.put("jenis_laporan", arrJl);
            out.print(jsonResponse.toString());
            out.flush();
        }
        
        // =================================================================================
        // ACTION: GET DATA (Laporan Utama / Komparasi Bulan / Komparasi Tahun)
        // =================================================================================
        else if ("get_data_laporan".equals(action) || "get_data_komparasi_bulan".equals(action) || "get_data_komparasi_tahun".equals(action)) {
            String skIdStr = request.getParameter("satuan_kerja_id");
            String jenisLaporanIdStr = request.getParameter("jenis_laporan_id");
            
            Long satuan_kerja = (skIdStr != null && !skIdStr.trim().isEmpty()) ? Long.parseLong(skIdStr) : -1L;
            Long jenis_laporan_id = (jenisLaporanIdStr != null && !jenisLaporanIdStr.trim().isEmpty()) ? Long.parseLong(jenisLaporanIdStr) : -1L;
            
            String sqlSelectCols = "";
            int N = 0; // Jumlah kolom nilai yang akan diagregasi
            String maxDateFilter = "";
            JSONArray kolomLabel = new JSONArray();

            // 1. Persiapan Logika Kolom SQL berdasarkan Tipe Laporan
            if ("get_data_laporan".equals(action)) {
                Date m = Common.dateFormat.get().parse(request.getParameter("tanggal_mulai"));
                Date s = Common.dateFormat.get().parse(request.getParameter("tanggal_sampai"));
                
                Calendar calSaldoAwal = Calendar.getInstance();
                calSaldoAwal.setTime(m);
                calSaldoAwal.set(Calendar.DATE, calSaldoAwal.get(Calendar.DATE) - 1);
                
                String tglSaldoAwalDB = Common.databaseDateFormat.get().format(calSaldoAwal.getTime());
                String tglMulaiDB = Common.databaseDateFormat.get().format(m);
                String tglSampaiDB = Common.databaseDateFormat.get().format(s);
                maxDateFilter = tglSampaiDB;
                
                sqlSelectCols = "(sum(case when date(a1.tanggal_transaksi) between date('1970-01-01') and date('" + tglSaldoAwalDB + "') then (debet-kredit) else 0 end)) as v1, "
                              + "(sum(case when date(a1.tanggal_transaksi) between date('" + tglMulaiDB + "') and date('" + tglSampaiDB + "') then (debet) else 0 end)) as v2, "
                              + "(sum(case when date(a1.tanggal_transaksi) between date('" + tglMulaiDB + "') and date('" + tglSampaiDB + "') then (kredit) else 0 end)) as v3, "
                              + "(sum(case when date(a1.tanggal_transaksi) <= date('" + tglSampaiDB + "') then (debet-kredit) else 0 end)) as v4";
                N = 4;
            } 
            else if ("get_data_komparasi_bulan".equals(action)) {
                int tahun = Integer.parseInt(request.getParameter("tahun"));
                maxDateFilter = tahun + "-12-31";
                
                StringBuilder sbCols = new StringBuilder();
                Calendar calStr = Calendar.getInstance();
                calStr.set(Calendar.YEAR, tahun);
                for(int i=0; i<12; i++) {
                    calStr.set(Calendar.MONTH, i);
                    calStr.set(Calendar.DATE, calStr.getActualMaximum(Calendar.DATE));
                    String eomDB = Common.databaseDateFormat.get().format(calStr.getTime());
                    sbCols.append("(sum(case when date(a1.tanggal_transaksi) <= date('").append(eomDB).append("') then (debet-kredit) else 0 end)) as v").append(i+1);
                    if(i < 11) sbCols.append(", ");
                }
                sqlSelectCols = sbCols.toString();
                N = 12;
            }
            else if ("get_data_komparasi_tahun".equals(action)) {
                int tMulai = Integer.parseInt(request.getParameter("tahun_mulai"));
                int tSampai = Integer.parseInt(request.getParameter("tahun_sampai"));
                if(tMulai > tSampai) { int temp = tMulai; tMulai = tSampai; tSampai = temp; }
                
                maxDateFilter = tSampai + "-12-31";
                N = tSampai - tMulai + 1;
                
                StringBuilder sbCols = new StringBuilder();
                for(int i=0; i<N; i++) {
                    int y = tMulai + i;
                    kolomLabel.put(y); // Untuk dirender frontend
                    sbCols.append("(sum(case when date(a1.tanggal_transaksi) <= date('").append(y).append("-12-31') then (debet-kredit) else 0 end)) as v").append(i+1);
                    if(i < N-1) sbCols.append(", ");
                }
                sqlSelectCols = sbCols.toString();
            }

            // 2. Eksekusi Master Query
            String sql = "select f.id as urut_laporan, c.id as kelompok, "
                    + "c.urut as urut, max(f.keterangan) as laporan, "
                    + "max((trim(e.nama))) as jenis_laporan1, "
                    + "max((trim(e.keterangan))) as jenis_laporan2, "
                    + "max(c.keterangan) as kelompok_laporan, "
                    + "max(c.keterangan1) as kelompok_laporan1, d.kode as kode_akun, d.nama as nama_akun, "
                    + sqlSelectCols + ", d.id as id_akun "
                    + "from akunting.transaksi a "
                    + "inner join akunting.grup_transaksi a1 on (a1.id=a.grup_transaksi) "
                    + "inner join akunting.kelompok_laporan_punya_akun b on (a.akun = b.akun) "
                    + "inner join akunting.kelompok_laporan c on (c.id = b.kelompok_laporan) "
                    + "inner join akunting.akun d on (a.akun = d.id) "
                    + "inner join akunting.master_grup_laporan e on (c.master_grup_laporan = e.id) "
                    + "inner join akunting.jenis_laporan f on (f.id = c.jenis_laporan) "
                    + "where (c.aktif is null or c.aktif) and a1.posting_history is not null ";

            if (!satuan_kerja.equals(-1L)) { sql += "and a1.satuan_kerja = " + satuan_kerja + " "; }
            
            sql += "and c.jenis_laporan = " + jenis_laporan_id + " "
                 + "and date(a1.tanggal_transaksi) between date('1970-01-01') and date('" + maxDateFilter + "') "
                 + "group by f.id, e.id, c.id, d.id "
                 + "order by f.id, e.nomor_urut, e.id, c.urut, c.id, max(b.nomorurut), urut, d.kode";

            List<Object[]> dataAkunting = sess.createSQLQuery(sql).list();
            
            JenisLaporan jlObj = (JenisLaporan) sess.get(JenisLaporan.class, jenis_laporan_id);
            String jlNama = jlObj != null ? jlObj.getNama() : Common.getBahasaConfig("Laporan");

            // 3. LOGIKA AGREGASI TREE HIERARKIS (Dinamis untuk N kolom)
            TreeMap<String, Object[]> mapData = new TreeMap<String, Object[]>();
            Set<String> keys = new HashSet<String>();
            int indexUrut = 89;
            
            // Pass 1: Node Daun (Akun Detail)
            for (Object[] obj : dataAkunting) {
                String jl1 = (obj[4] != null && !obj[4].toString().trim().isEmpty() && !obj[4].toString().trim().equalsIgnoreCase("null")) ? obj[4].toString() : jlNama;
                String jl2 = (obj[5] != null && !obj[5].toString().trim().isEmpty() && !obj[5].toString().trim().equalsIgnoreCase("null")) ? obj[5].toString() : jl1;
                String kl = (obj[6] != null && !obj[6].toString().trim().isEmpty() && !obj[6].toString().trim().equalsIgnoreCase("null")) ? obj[6].toString() : jl2;
                String kl1 = (obj[7] != null && !obj[7].toString().trim().isEmpty() && !obj[7].toString().trim().equalsIgnoreCase("null")) ? obj[7].toString() : kl;
                
                String kode_akun = obj[8] + "";
                String nama_akun = obj[9] + "";
                
                Object[] rowData = new Object[N + 3];
                rowData[0] = kode_akun + " " + nama_akun;
                for(int v=0; v<N; v++) { rowData[v+1] = ((Number) obj[10 + v]).doubleValue(); }
                Long id_akun = ((Number) obj[10 + N]).longValue();

                String u1 = "000000" + indexUrut;
                String urutan = u1.substring(u1.length() - 4);
                String key = urutan + "-" + jl1 + "__" + jl2 + "__" + kl + "__" + kl1 + "__" + kode_akun + " " + nama_akun;
                
                rowData[N+1] = key.trim();
                rowData[N+2] = id_akun + "";
                
                mapData.put(key.trim(), rowData);
                indexUrut++;
            }

            // Pass 2: Agregasi Level Induk (Parent)
            for(int pass = 0; pass < 4; pass++) {
                keys.clear();
                for (Object[] obj : dataAkunting) {
                    String jl1 = (obj[4] != null && !obj[4].toString().trim().isEmpty() && !obj[4].toString().trim().equalsIgnoreCase("null")) ? obj[4].toString() : jlNama;
                    String jl2 = (obj[5] != null && !obj[5].toString().trim().isEmpty() && !obj[5].toString().trim().equalsIgnoreCase("null")) ? obj[5].toString() : jl1;
                    String kl = (obj[6] != null && !obj[6].toString().trim().isEmpty() && !obj[6].toString().trim().equalsIgnoreCase("null")) ? obj[6].toString() : jl2;
                    String kl1 = (obj[7] != null && !obj[7].toString().trim().isEmpty() && !obj[7].toString().trim().equalsIgnoreCase("null")) ? obj[7].toString() : kl;
                    
                    Double[] values = new Double[N];
                    for(int v=0; v<N; v++) { values[v] = ((Number) obj[10 + v]).doubleValue(); }
                    Long id_akun = ((Number) obj[10 + N]).longValue();

                    String keyTarget = "";
                    String namaTarget = "";
                    
                    if(pass == 0) { keyTarget = jl1 + "__" + jl2 + "__" + kl + "__" + kl1; namaTarget = kl1; }
                    else if(pass == 1) { keyTarget = jl1 + "__" + jl2 + "__" + kl; namaTarget = kl; }
                    else if(pass == 2) { keyTarget = jl1 + "__" + jl2; namaTarget = jl2; }
                    else if(pass == 3) { keyTarget = jl1; namaTarget = jl1; }
                    
                    keyTarget = keyTarget.trim();
                    keys.add(keyTarget);
                    
                    String u1 = "000000" + keys.size();
                    String urutan = u1.substring(u1.length() - 4);
                    String finalKey = urutan + "-" + keyTarget;

                    Object[] existingData = mapData.get(finalKey);
                    if (existingData == null) {
                        existingData = new Object[N + 3];
                        existingData[0] = namaTarget;
                        for(int v=0; v<N; v++) existingData[v+1] = values[v];
                        existingData[N+1] = finalKey;
                        existingData[N+2] = id_akun + "";
                    } else {
                        for(int v=0; v<N; v++) {
                            existingData[v+1] = (Double)existingData[v+1] + values[v];
                        }
                        existingData[N+2] = existingData[N+2] + "," + id_akun;
                    }
                    mapData.put(finalKey, existingData);
                }
            }

            // 4. Pembentukan Response JSON
            JSONArray dataArray = new JSONArray();
            for (Map.Entry<String, Object[]> entry : mapData.entrySet()) {
                Object[] val = entry.getValue();
                String rawKey = val[N+1].toString();
                String cleanKey = rawKey.contains("-") ? rawKey.split("-", 2)[1] : rawKey;
                int level = cleanKey.split("__").length - 1; 
                
                JSONObject row = new JSONObject();
                row.put("uraian", val[0].toString());
                
                JSONArray valsArr = new JSONArray();
                for(int v=0; v<N; v++) { valsArr.put((Double)val[v+1]); }
                row.put("values", valsArr);
                
                row.put("id_akun", val[N+2].toString());
                row.put("level", level);
                row.put("is_leaf", level == 4);
                
                dataArray.put(row);
            }

            jsonResponse.put("status", "success");
            jsonResponse.put("data", dataArray);
            if("get_data_komparasi_tahun".equals(action)) {
                jsonResponse.put("kolom_tahun", kolomLabel);
            }
            
            out.print(jsonResponse.toString());
            out.flush();
        }

    } catch (Exception e) {
        JSONObject errorObj = new JSONObject();
        errorObj.put("error", Common.getBahasaConfig("Terjadi kesalahan sistem: ") + e.getMessage());
        out.print(errorObj.toString());
        out.flush();
    } finally {
        if (sess != null && sess.isOpen()) {
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/akuntansi/_monitor_akunting_service.jsp:263"); }
        }
    }
%>