<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, org.hibernate.*, org.hibernate.criterion.*, org.json.*" %>
<%@ page import="ais.common.*, ais.database.hibernate.*, ais.database.model.*, ais.database.model.lkp.*, ais.ui.util.*" %>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    JSONObject jsonResponse = new JSONObject();
    Session sess = null;

    try {
        String action = request.getParameter("action");
        sess = HibernateUtil.openSession();
        Tbmuser tbmuser = Common.getCurrentUser();

        // =================================================================================
        // ACTION: GET SUMMARY (Menghitung KPI Kinerja Pegawai)
        // =================================================================================
        if ("get_summary".equals(action)) {
            Criteria critBase = sess.createCriteria(RealisasiKerjaPegawai.class).createAlias("targetKerjaPegawai", "targetKerjaPegawai");
            if (tbmuser.getPegawai() != null && (tbmuser.hakAkses() == null || !tbmuser.hakAkses().getMelihatDataPegawaiLain())) {
                critBase.add(Restrictions.or(
						Restrictions.eq("targetKerjaPegawai.pegawai", tbmuser.getPegawai()),
						Restrictions.eq("pegawai", tbmuser.getPegawai())));
            }

            int totalCatatan = ((Number) sess.createCriteria(RealisasiKerjaPegawai.class)
                .add(tbmuser.getPegawai() != null && (tbmuser.hakAkses() == null || !tbmuser.hakAkses().getMelihatDataPegawaiLain()) ? Restrictions.or(
						Restrictions.eq("targetKerjaPegawai.pegawai", tbmuser.getPegawai()),
						Restrictions.eq("pegawai", tbmuser.getPegawai())) : Restrictions.sqlRestriction("1=1"))
                .setProjection(Projections.rowCount()).uniqueResult()).intValue();

            // Asumsi field verifikasi, sesuaikan dengan nama kolom/field asli di entitas jika berbeda
            int totalVerifikasi = 0; 
            int totalBelumVerifikasi = totalCatatan - totalVerifikasi;

            jsonResponse.put("status", "success");
            jsonResponse.put("total_catatan", totalCatatan);
            jsonResponse.put("total_verifikasi", totalVerifikasi);
            jsonResponse.put("total_belum_verifikasi", totalBelumVerifikasi);

            out.print(jsonResponse.toString());
            out.flush();
        }
        
        // =================================================================================
        // ACTION: GET DATA TABEL (Data Json untuk DataTables Server-Side)
        // =================================================================================
        else if ("get_data_tabel".equals(action)) {
            String draw = request.getParameter("draw");
            int start = Integer.parseInt(request.getParameter("start") != null ? request.getParameter("start") : "0");
            int length = Integer.parseInt(request.getParameter("length") != null ? request.getParameter("length") : "10");
            String searchValue = request.getParameter("search[value]");

            Criteria crit = sess.createCriteria(RealisasiKerjaPegawai.class).createAlias("targetKerjaPegawai", "targetKerjaPegawai");
            Criteria critCount = sess.createCriteria(RealisasiKerjaPegawai.class).createAlias("targetKerjaPegawai", "targetKerjaPegawai");

            // Filter Hak Akses Pegawai
            if (tbmuser.getPegawai() != null && (tbmuser.hakAkses() == null || !tbmuser.hakAkses().getMelihatDataPegawaiLain())) {
                crit.add(Restrictions.or(
						Restrictions.eq("targetKerjaPegawai.pegawai", tbmuser.getPegawai()),
						Restrictions.eq("pegawai", tbmuser.getPegawai())));
                critCount.add(Restrictions.or(
						Restrictions.eq("targetKerjaPegawai.pegawai", tbmuser.getPegawai()),
						Restrictions.eq("pegawai", tbmuser.getPegawai())));
            }

            // Fitur Pencarian Data (Telah diupdate dengan Criteria.LEFT_JOIN)
            if (searchValue != null && !searchValue.trim().isEmpty()) {
                String searchLike = "%" + searchValue.trim().toLowerCase() + "%";
                
                crit
                    .createAlias("targetKerjaPegawai.kegiatanTugasJabatan", "kegiatanTugasJabatan", Criteria.LEFT_JOIN);
                    
                critCount
                         .createAlias("targetKerjaPegawai.kegiatanTugasJabatan", "kegiatanTugasJabatan", Criteria.LEFT_JOIN);

                Criterion searchCondition = Restrictions.or(
                    Restrictions.ilike("kegiatanTugasJabatan.nama", searchValue.trim(), MatchMode.ANYWHERE),
                    Restrictions.or(
                        Restrictions.ilike("keterangan", searchValue.trim(), MatchMode.ANYWHERE),
                        Restrictions.ilike("catatan", searchValue.trim(), MatchMode.ANYWHERE)
                    )
                );

                crit.add(searchCondition);
                critCount.add(searchCondition);
            }

            // Hitung Total Data (Filtered & Unfiltered)
            long recordsTotal = ((Number) sess.createCriteria(RealisasiKerjaPegawai.class).createAlias("targetKerjaPegawai", "targetKerjaPegawai")
                .add(tbmuser.getPegawai() != null && (tbmuser.hakAkses() == null || !tbmuser.hakAkses().getMelihatDataPegawaiLain()) ? Restrictions.or(
						Restrictions.eq("targetKerjaPegawai.pegawai", tbmuser.getPegawai()),
						Restrictions.eq("pegawai", tbmuser.getPegawai())) : Restrictions.sqlRestriction("1=1"))
                .setProjection(Projections.rowCount()).uniqueResult()).longValue();
                
            long recordsFiltered = ((Number) critCount.setProjection(Projections.rowCount()).uniqueResult()).longValue();

            // Paging & Sorting
            crit.addOrder(Order.desc("id"));
            crit.setFirstResult(start);
            crit.setMaxResults(length);

            List<RealisasiKerjaPegawai> list = crit.list();
            JSONArray dataArray = new JSONArray();

            for(RealisasiKerjaPegawai d : list) {
                JSONObject o = new JSONObject();
                
                // Konfigurasi URL Foto terpusat (Telah diupdate)
                String urlFoto = ProfileImageUtil.getUrlFotoDariObject(d.getPegawai(), true);
                
                // Pengecekan Null untuk Relasi
                String namaPegawai = (d.getPegawai() != null) ? d.getPegawai().getNama() : "-";
                String fotoPegawai = (d.getPegawai() != null) ? "<img src='"+urlFoto+"' class='rounded-circle shadow-sm' style='width: 40px; height: 40px; object-fit: cover;' alt='Foto'>" : "<div class='bg-secondary text-white rounded-circle d-flex align-items-center justify-content-center shadow-sm' style='width: 40px; height: 40px;'><i class='fas fa-user'></i></div>";
                
                String namaKegiatan = (d.getTargetKerjaPegawai() != null && d.getTargetKerjaPegawai().getKegiatanTugasJabatan() != null) ? d.getTargetKerjaPegawai().getKegiatanTugasJabatan().getNama() : "-";
                
                o.put("id", d.getId());
                o.put("foto", fotoPegawai);
                o.put("pegawai", namaPegawai);
                o.put("kegiatan", namaKegiatan + "<br><small class='text-muted'>" + (d.getKeterangan() != null ? d.getKeterangan() : "") + "</small>");
                o.put("kuantitas", d.getKuantitas() != null ? Common.numberFormat.get().format(d.getKuantitas()) : "-");
                o.put("waktu", d.getWaktu() != null ? d.getWaktu().toString() : "-"); // Sesuaikan format waktu
                o.put("biaya", d.getBiaya() != null ? Common.numberFormat.get().format(d.getBiaya()) : "-");
                
                // Placeholder untuk verifikasi dan catatan asesor jika ada di entitas
                o.put("verifikasi_asesor", "<span class='badge bg-warning text-dark'><i class='fas fa-clock'></i> " + Common.getBahasaConfig("Menunggu") + "</span>");
                o.put("catatan_asesor", d.getCatatan() != null ? d.getCatatan() : "-");
                
                // Action Buttons
                String actionButtons = "<button class='btn btn-sm btn-outline-primary me-1 btn-edit' data-id='"+d.getId()+"' title='"+Common.getBahasaConfig("Ubah")+"'><i class='fas fa-edit'></i></button>" +
                                       "<button class='btn btn-sm btn-outline-danger btn-delete' data-id='"+d.getId()+"' title='"+Common.getBahasaConfig("Hapus")+"'><i class='fas fa-trash-alt'></i></button>";
                o.put("aksi", actionButtons);

                dataArray.put(o);
            }

            JSONObject tableJson = new JSONObject();
            tableJson.put("draw", draw);
            tableJson.put("recordsTotal", recordsTotal);
            tableJson.put("recordsFiltered", recordsFiltered);
            tableJson.put("data", dataArray);
            
            out.print(tableJson.toString());
            out.flush();
        } // =================================================================================
        // ACTION: GET PENILAIAN CAPAIAN (Sesuai Query LKP Pegawai JRXML)
        // =================================================================================
        else if ("get_penilaian_capaian".equals(action)) {
            String search = request.getParameter("search");
            int bulan = Integer.parseInt(request.getParameter("bulan") != null && !request.getParameter("bulan").isEmpty() ? request.getParameter("bulan") : "-1");
            int tahun = Integer.parseInt(request.getParameter("tahun") != null && !request.getParameter("tahun").isEmpty() ? request.getParameter("tahun") : "-1");

            // Jika tahun/bulan belum dipilih, gunakan waktu saat ini
            if(tahun == -1) tahun = Calendar.getInstance().get(Calendar.YEAR);
            if(bulan == -1) bulan = Calendar.getInstance().get(Calendar.MONTH) + 1; // Calendar.MONTH dimulai dari 0

            // Menyusun Native SQL berdasarkan referensi JRXML
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ")
               .append("a.bulan, a.tahun, a.pegawai as id_pegawai, c.nama as nama_pegawai, c.code as nip, c.mycode as mycode, ")
               .append("d.nama as spesifikasi_jabatan, e.nama as golongan, c.jabatan, f.nama as jabatan_struktural, ")
               .append("g.nama as jabatan_fungsional, h.nama as satuan_kerja, c.telp, c.email, ")
               .append("b.nama as kegiatan, b.angkakredit, a.kuantitas, i.nama as satuan_kuantitas, ")
               .append("a.kualitas, a.waktu, b.satuanwaktu, a.biaya, ")
               .append("j.kuantitas as realisasi_kuantitas, a.kualitasrealisasi, ")
               .append("CASE WHEN j.waktu IS NULL THEN 0 ELSE j.waktu END AS realisasi_waktu, ")
               .append("CASE WHEN j.biaya IS NULL THEN 0 ELSE j.biaya END AS realisasi_biaya, ")
               .append("k.nama as asesor, k.nip as asesor_nip ")
               .append("FROM target_kerja_pegawai a ")
               .append("INNER JOIN kegiatan_tugas_jabatan b ON (a.kegiatan_tugas_jabatan=b.id) ")
               .append("INNER JOIN pegawai c ON (a.pegawai = c.id) ")
               .append("LEFT JOIN jabatan d ON (d.id = c.spesifikasi_jabatan) ")
               .append("LEFT JOIN employ.golongan e ON (c.golongan_pegawai=e.id) ")
               .append("LEFT JOIN employ.jabatan_struktural f ON (f.id = c.jabatan_struktural) ")
               .append("LEFT JOIN employ.jabatan_fungsional g ON (g.id = c.jabatan_fungsional) ")
               .append("LEFT JOIN rab.satuan_kerja h ON (h.id = b.satuan_kerja) ")
               .append("LEFT JOIN satuan_kegiatan_tugas_jabatan i ON (i.id=b.satuan_kuantitas) ")
               .append("LEFT JOIN ( ")
               .append("    SELECT aa.target_kerja_pegawai, SUM(aa.kuantitas) as kuantitas, SUM(aa.waktu) as waktu, SUM(aa.biaya) as biaya ")
               .append("    FROM realisasi_kerja_pegawai aa GROUP BY aa.target_kerja_pegawai ")
               .append(") j ON (a.id=j.target_kerja_pegawai) ")
               .append("LEFT JOIN ( ")
               .append("    SELECT aa.pegawai, MAX(CASE WHEN dd.nama IS NULL THEN cc.usernama ELSE dd.nama END) as nama, MAX(dd.code) as nip ")
               .append("    FROM asesor_pegawai aa ")
               .append("    INNER JOIN asesor bb ON (aa.asesor = bb.id) ")
               .append("    INNER JOIN tbmuser cc ON (bb.tbmuser=cc.userid) ")
               .append("    LEFT JOIN pegawai dd ON (dd.id=cc.pegawai) ")
               .append("    GROUP BY aa.pegawai ")
               .append(") k ON (k.pegawai=a.pegawai) ")
               .append("WHERE a.tahun = :tahun AND a.bulan = :bulan ");

            // Filter Pegawai (NIP, Nama, MyCode) atau default ke user login
            if (search != null && !search.trim().isEmpty()) {
                sql.append("AND (c.nama ILIKE :search OR c.code ILIKE :search OR c.mycode ILIKE :search) ");
            } else if (tbmuser.getPegawai() != null && (tbmuser.hakAkses() == null || !tbmuser.hakAkses().getMelihatDataPegawaiLain())) {
                sql.append("AND a.pegawai = :idPegawaiLogin ");
            }

            sql.append("ORDER BY c.id, b.nourut");

            SQLQuery query = sess.createSQLQuery(sql.toString());
            query.setParameter("tahun", tahun);
            query.setParameter("bulan", bulan);
            
            if (search != null && !search.trim().isEmpty()) {
                query.setParameter("search", "%" + search.trim() + "%");
            } else if (tbmuser.getPegawai() != null && (tbmuser.hakAkses() == null || !tbmuser.hakAkses().getMelihatDataPegawaiLain())) {
                query.setParameter("idPegawaiLogin", tbmuser.getPegawai().getId());
            }

            query.setResultTransformer(org.hibernate.transform.Transformers.ALIAS_TO_ENTITY_MAP);
            List<Map<String, Object>> resultList = query.list();

            JSONArray dataArray = new JSONArray();
            if (!resultList.isEmpty()) {
                // Untuk kesederhanaan, kita mengelompokkan data berdasarkan pegawai pertama yang ditemukan dari hasil pencarian
                Object firstPegawaiId = resultList.get(0).get("id_pegawai");
                
                for (Map<String, Object> row : resultList) {
                    if (!row.get("id_pegawai").equals(firstPegawaiId)) continue; // Hanya ambil 1 pegawai jika pencarian mengembalikan banyak

                    JSONObject o = new JSONObject();
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        o.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
                    }
                    
                    // Inject Foto URL
                    Pegawai p = (Pegawai) sess.get(Pegawai.class, Long.parseLong(row.get("id_pegawai").toString()));
                    if(p != null) {
                         o.put("url_foto", ProfileImageUtil.getUrlFotoDariObject(p, true));
                    }
                    
                    dataArray.put(o);
                }
            }

            JSONObject resp = new JSONObject();
            resp.put("status", "success");
            resp.put("data", dataArray);
            
            out.print(resp.toString());
            out.flush();
        }

    } catch (Exception e) {
        out.print("{\"error\": \"Terjadi kesalahan sistem: " + e.getMessage() + "\"}");
        out.flush();
    } finally {
        if (sess != null && sess.isOpen()) {
            try { 
                sess.close();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kinerja/_monitor_kinerja_service.jsp:254");}
        }
    }
%>