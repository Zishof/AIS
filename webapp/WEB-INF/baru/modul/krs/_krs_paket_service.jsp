<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, org.hibernate.*, org.hibernate.criterion.*, org.json.*" %>
<%@ page import="ais.common.*, ais.database.hibernate.*" %>
<%@ page import="ais.database.model.*, ais.action.master.SyaratUjianAction" %>
<%@ page import="ais.action.master.helper.*" %>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    JSONObject jsonResponse = new JSONObject();
    Session sess = null;
    Transaction tx = null;
    
    try {
        String action = request.getParameter("action");
        sess = HibernateUtil.openSession();
        
        Tbmuser tbmuser = Common.getCurrentUser(request);
        Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa(); 
        
        boolean isDebug = Common.getKonfigurasi("debug_ambil_ambilDetailperkuliahan", Konfigurasi.TIDAK_AKTIF).getNilai().trim().equalsIgnoreCase(Konfigurasi.AKTIF);
        
        String periodeId = request.getParameter("filter_id_smt"); 
        Integer semester = null;
        Integer semesterPendek = null;
        String tahunAjaran = Common.getCurrentTahunAkademik(); 
        
        if (mahasiswa != null && periodeId != null && periodeId.length() >= 5) {
            try {
                String strTahun = periodeId.substring(0, 4);
                String strJenis = periodeId.substring(4, 5);
                Integer tahun = Integer.parseInt(strTahun);
                String semesterMulaiCalc = strJenis.equals("1") ? Perkuliahan.GANJIL : strJenis.equals("2") ? Perkuliahan.GENAP : Perkuliahan.SP;
                Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan() != null ? mahasiswa.getTahunangkatan() : tahun;
                
                semester = Common.getSemester(tahunAngkatanMhs, semesterMulaiCalc, mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
                semesterPendek = strJenis.equals("3") ? Perkuliahan.SEMESTER_PENDEK : null;
                tahunAjaran = tahun + "/" + (tahun + 1);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/krs/_krs_paket_service.jsp:39"); }
        } else if (mahasiswa != null) {
            semester = mahasiswa.currentSemester();
        }
        
        Integer tahapan = request.getParameter("tahapan") != null && !request.getParameter("tahapan").isEmpty() ? Integer.parseInt(request.getParameter("tahapan")) : 0;
        boolean remedial = "true".equals(request.getParameter("remedial"));
        boolean isRefresh = "true".equals(request.getParameter("refresh"));
        String mhsKelas = mahasiswa != null && mahasiswa.getKelas() != null ? mahasiswa.getKelas().trim() : "";

        // =================================================================================
        // ACTION 1: GET FILTER SMT (Sama seperti KRS Reguler)
        // =================================================================================
        if ("get_filter_smt".equals(action)) {
            JSONArray filterArray = new JSONArray();
            String currentTa = Common.getCurrentTahunAkademik();
            Boolean isGanjil = Common.isNowSemensterGanjil();
            Integer currentTahun = (currentTa != null && !currentTa.isEmpty()) ? Integer.parseInt(currentTa.split("/")[0]) : Calendar.getInstance().get(Calendar.YEAR);
            String defaultIdSmt = currentTahun + (isGanjil != null && isGanjil ? "1" : "2");
            Integer tahunAngkatan = mahasiswa != null && mahasiswa.getTahunangkatan() != null ? mahasiswa.getTahunangkatan() : currentTahun;

            Map<String, String> mapFilters = new TreeMap<>(Collections.reverseOrder());
            for (int y = currentTahun + 1; y >= tahunAngkatan; y--) {
                String taLabel = y + "/" + (y + 1);
                mapFilters.put(y + "3", taLabel + " - " + Common.getBahasaConfig("Semester Pendek"));
                mapFilters.put(y + "2", taLabel + " - " + Common.getBahasaConfig("Genap"));
                mapFilters.put(y + "1", taLabel + " - " + Common.getBahasaConfig("Ganjil"));
            }
            
            for (Map.Entry<String, String> entry : mapFilters.entrySet()) {
                String pId = entry.getKey();
                Integer calcSmt = null; Integer calcSp = null;
                if(mahasiswa != null && pId != null && pId.length() >= 5) {
                    try {
                        String strTahun = pId.substring(0, 4); String strJenis = pId.substring(4, 5); Integer tahun = Integer.parseInt(strTahun);
                        String semesterMulaiCalc = strJenis.equals("1") ? Perkuliahan.GANJIL : strJenis.equals("2") ? Perkuliahan.GENAP : Perkuliahan.SP;
                        Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan() != null ? mahasiswa.getTahunangkatan() : tahun;
                        calcSmt = Common.getSemester(tahunAngkatanMhs, semesterMulaiCalc, mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
                        if (strJenis.equals("3")) calcSp = 1;
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/krs/_krs_paket_service.jsp:78");}
                }
                JSONObject obj = new JSONObject(); obj.put("idSmt", pId); obj.put("label", entry.getValue()); 
                if (calcSmt != null) obj.put("smt", calcSmt); if (calcSp != null) obj.put("sp", calcSp);
                filterArray.put(obj);
            }
            JSONObject root = new JSONObject(); root.put("data", filterArray); root.put("default_id", defaultIdSmt); out.print(root.toString()); out.flush();
        }
        
        // =================================================================================
        // ACTION 2: VALIDASI KRS
        // =================================================================================
        else if ("validasi_ambil_krs".equals(action)) {
            if (mahasiswa == null) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Sesi login Mahasiswa tidak ditemukan.")); out.print(jsonResponse.toString()); return;
            }

            KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, false);
            Dosen dosenPembimbingAkademik = krsMahasiswa.getDosenPa();
            
            Konfigurasi konfigurasi = Common.getKonfigurasi(remedial ? Konfigurasi.KRS_REMEDIAL : semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP, tahunAjaran, (semester != null && semester % 2 == 0) ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
            boolean masaKrsAktif = (konfigurasi != null && konfigurasi.getNilai() != null && konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
            if (!masaKrsAktif) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Saat ini bukan masa pengambilan KRS berdasarkan Kalender Akademik.")); out.print(jsonResponse.toString()); return;
            }

            if (dosenPembimbingAkademik == null && Common.getKonfigurasi("dosen_pa_harus_ada_sebelum_isi_krs", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Anda belum memiliki Dosen Pembimbing Akademik.")); out.print(jsonResponse.toString()); return;
            }

            if (Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false, semesterPendek != null)) {
                    jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Anda belum menyelesaikan pembayaran biaya perkuliahan.")); out.print(jsonResponse.toString()); return;
                }
            }

            Double[] batasSksArr = Common.getMinDanMaxIPK(mahasiswa, semester, semesterPendek);
            HashMap<Long, Perkuliahan> hashMapEmpty = new HashMap<>(); 
            int sisaSksDiambil = KrsUtilHelper.hitungSksYangTelahDiambil(hashMapEmpty, mahasiswa, tahapan, semester, semesterPendek);

            jsonResponse.put("status", "success");
            jsonResponse.put("max_sks", batasSksArr[0].intValue());
            jsonResponse.put("sks_terambil", sisaSksDiambil);
            out.print(jsonResponse.toString()); out.flush();
        }

        // =================================================================================
        // ACTION 3: AMBIL DAFTAR PERKULIAHAN PAKET (Sesuai Helper Paket)
        // =================================================================================
        else if ("get_perkuliahan_paket".equals(action)) {
            
            // PERBAIKAN: Menyesuaikan parameter relasi join Hibernate (kurikulum.jurusan)
            Criteria criteriaPaket = sess.createCriteria(PaketPerkuliahan.class)
                    .add(Restrictions.sqlRestriction(semester + " between minsmt and maxsmt"))
                    .add(Restrictions.sqlRestriction(mahasiswa.getTahunangkatan() + " between mulai and sampai"))
                    .add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek") : Restrictions.eq("statusSemesterPendek", semesterPendek))
                    .createAlias("kurikulum", "kurikulum", Criteria.LEFT_JOIN)
                    .createAlias("kurikulum.program", "program", Criteria.LEFT_JOIN)
                    .add(Restrictions.eq("kurikulum.jurusan", mahasiswa.getJurusan())) // Fixed from "jurusan"
                    .add(Restrictions.or(Restrictions.isNull("kurikulum.program"), Restrictions.eq("program.nama", mahasiswa.getProgram())))
                    .add(Restrictions.eq("tahunAkademik", tahunAjaran))
                    .addOrder(Order.desc("angkatanMulai"))
                    .addOrder(Order.desc("angkatanSampai"))
                    .addOrder(Order.desc("id"))
                    .setMaxResults(1);

            PaketPerkuliahan paket = (PaketPerkuliahan) criteriaPaket.uniqueResult();
            if (paket == null) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Paket perkuliahan untuk semester dan program studi Anda tidak ditemukan di sistem.")); out.print(jsonResponse.toString()); return;
            }

            List<KurikulumPunyaMatakuliah> kpms = sess.createCriteria(KurikulumPunyaMatakuliah.class)
                    .add(Restrictions.eq("kurikulum", paket.getKurikulum()))
                    .add(Restrictions.eq("semester", semester)).list();

            if (kpms.isEmpty()) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Paket perkuliahan kosong. Tidak ada matakuliah yang terdaftar untuk paket semester ini.")); out.print(jsonResponse.toString()); return;
            }

            JSONArray dataArray = new JSONArray();
            for (KurikulumPunyaMatakuliah kpm : kpms) {
                Matakuliah mk = kpm.getMatakuliah();
                if (mk == null) continue;

                List<Perkuliahan> perkuliahans = sess.createCriteria(Perkuliahan.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .add(Restrictions.or(Restrictions.isNull("tampilkanSaatPengambilanKrs"), Restrictions.eq("tampilkanSaatPengambilanKrs", true)))
                        .add(Restrictions.eq("matakuliah", mk))
                        .add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek") : Restrictions.eq("statusSemesterPendek", semesterPendek))
                        .createAlias("jurusan", "jurusan")
                        .add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
                        .add(Restrictions.eq("program", mahasiswa.getProgram()))
                        .add(Restrictions.ilike("kelas", mhsKelas, MatchMode.EXACT))
                        .add(Restrictions.eq("semester", semester))
                        .add(Restrictions.eq("tahunAjaran", tahunAjaran))
                        .add(Restrictions.or(Restrictions.eq("merupakan_paralel", false), Restrictions.isNull("merupakan_paralel")))
                        .list();

                String resDosenJadwal = "<span class='text-danger small'><i class='fas fa-exclamation-circle me-1'></i>" + Common.getBahasaConfig("Jadwal belum dibuat") + "</span>";
                String kapasitas = "-";
                
                if (perkuliahans != null && !perkuliahans.isEmpty()) {
                    Perkuliahan p = perkuliahans.get(0); // Ambil jadwal pertama yang cocok
                    resDosenJadwal = PerkuliahanUIHelper.generateTeksDosenPerkuliahan(p) + "<br>" + PerkuliahanUIHelper.generateHariJamRuanganPerkuliahanUmumText(p);
                    
                    Integer kap = p.getKapasitasKelas();
                    PembagianKuotaPerkuliahanBerdasarkantahunAngkatan kuotaTahun = KrsUtilHelper.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(sess, p, mahasiswa.getTahunangkatan(), false);
                    if (kuotaTahun != null && kuotaTahun.getKuota() != null) kap = kuotaTahun.getKuota().intValue();
                    Integer isi = KrsUtilHelper.ambilJumlahDetailperkuliahan(sess, p, false);
                    kapasitas = isi + " / " + kap;
                }

                JSONObject obj = new JSONObject();
                obj.put("kpm_id", kpm.getId());
                obj.put("matakuliah", "<span class='fw-bold text-dark'>" + mk.getKode() + "</span><br>" + mk.getNama());
                obj.put("sks", mk.getSks());
                obj.put("sks_raw", mk.getSks()); 
                obj.put("dosen_jadwal", resDosenJadwal);
                obj.put("kapasitas", kapasitas);
                dataArray.put(obj);
            }

            JSONObject tableJson = new JSONObject(); 
            tableJson.put("status", "success");
            tableJson.put("data", dataArray); 
            out.print(tableJson.toString()); out.flush();
        }

        // =================================================================================
        // ACTION 4: SIMPAN KRS PAKET
        // =================================================================================
        else if ("simpan_krs_paket".equals(action)) {
            String[] selectedKpmIds = request.getParameterValues("kpm_ids[]");
            if (selectedKpmIds == null || selectedKpmIds.length == 0) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Gagal memproses. Paket matakuliah kosong.")); out.print(jsonResponse.toString()); return;
            }

            boolean autoCreate = Common.getKonfigurasi("untuk_pengambilan_krs_paket_jika_jadwal_belum_dibuat_otomatis_membuat_jadwal_dengan_waktu_ruang_dosen_yang_kosong", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF);
            Map<Long, Perkuliahan> mapPerkuliahan = new HashMap<>();
            
            tx = sess.beginTransaction();
            
            // 1. Tentukan/Buat Jadwal (Perkuliahan)
            for (String kpmIdStr : selectedKpmIds) {
                Long kpmId = Long.parseLong(kpmIdStr);
                KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) sess.get(KurikulumPunyaMatakuliah.class, kpmId);
                if (kpm == null || kpm.getMatakuliah() == null) continue;

                List<Perkuliahan> perkuliahans = sess.createCriteria(Perkuliahan.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .add(Restrictions.eq("matakuliah", kpm.getMatakuliah()))
                        .add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek") : Restrictions.eq("statusSemesterPendek", semesterPendek))
                        .createAlias("jurusan", "jurusan") // Perkuliahan has direct jurusan
                        .add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
                        .add(Restrictions.eq("program", mahasiswa.getProgram()))
                        .add(Restrictions.ilike("kelas", mhsKelas, MatchMode.EXACT))
                        .add(Restrictions.eq("semester", semester))
                        .add(Restrictions.eq("tahunAjaran", tahunAjaran))
                        .add(Restrictions.or(Restrictions.eq("merupakan_paralel", false), Restrictions.isNull("merupakan_paralel")))
                        .list();

                if (perkuliahans != null && !perkuliahans.isEmpty()) {
                    mapPerkuliahan.put(kpm.getMatakuliah().getId(), perkuliahans.get(0));
                } else if (autoCreate) {
                    Perkuliahan pBaru = new Perkuliahan();
                    pBaru.setMatakuliah(kpm.getMatakuliah());
                    pBaru.setKurikulum(kpm.getKurikulum());
                    pBaru.setJurusan(mahasiswa.getJurusan());
                    pBaru.setProgram(mahasiswa.getProgram());
                    pBaru.setKelas(mhsKelas);
                    pBaru.setSemester(semester);
                    pBaru.setTahunAjaran(tahunAjaran);
                    pBaru.setMerupakan_paralel(false);
                    pBaru.setMerupakan_tanpa_dosen(true);
                    pBaru.setMerupakan_tanpa_jadwal_perkuliahan(true);
                    pBaru.setMerupakan_tanpa_ruangan(true);
                    pBaru.populateKurikulumPunyaMatakuliah();
                    Common.refreshSaveOrUpdate(sess, pBaru);
                    mapPerkuliahan.put(kpm.getMatakuliah().getId(), pBaru);
                }
            }

            // 2. Evaluasi SKS
            sess.refresh(mahasiswa);
            Integer jumlahSksTerpilih = KrsUtilHelper.hitungSksYangTelahDiambil(mapPerkuliahan, mahasiswa, tahapan, semester, semesterPendek);
            boolean isMelebihiKetentuan = Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, semester, jumlahSksTerpilih, semesterPendek);
            if (isMelebihiKetentuan) {
                tx.rollback();
                Double[] batas = Common.getMinDanMaxIPK(mahasiswa, semester, semesterPendek);
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Jumlah SKS paket melebihi batas maksimal yang diizinkan (Maksimal:") + " " + batas[0].intValue() + " SKS)."); out.print(jsonResponse.toString()); return;
            }

            // 3. Simpan Detailperkuliahan (KRS)
            String peringatanWarning = "";
            for (Perkuliahan perkuliahan : mapPerkuliahan.values()) {
                if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa, semester)) {
                    peringatanWarning += "- " + perkuliahan.getMatakuliah().getNama() + " (" + Common.getBahasaConfig("Prasyarat Belum Terpenuhi") + ")\n";
                    continue;
                }
                
                int existCheck = KrsUtilHelper.ambilJumlahDetailperkuliahan(sess, perkuliahan, mahasiswa, false);
                if (existCheck == 0) {
                    Integer kapasitasKelas = perkuliahan.getKapasitasKelas();
                    PembagianKuotaPerkuliahanBerdasarkantahunAngkatan kuotaTahun = KrsUtilHelper.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(sess, perkuliahan, mahasiswa.getTahunangkatan(), false);
                    if (kuotaTahun != null && kuotaTahun.getKuota() != null) kapasitasKelas = kuotaTahun.getKuota().intValue();
                    
                    Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(sess, perkuliahan, false);
                    if ((jumlahUdahMasuk + 1) > kapasitasKelas) {
                        peringatanWarning += "- " + perkuliahan.getMatakuliah().getNama() + " (" + Common.getBahasaConfig("Kapasitas Penuh") + ")\n";
                        continue;
                    }
                    
                    Detailperkuliahan dp = new Detailperkuliahan(tbmuser, AmbilDataPaketPerkuliahanHelper.class);
                    dp.setNilaiHuruf(""); dp.setTotalNilai(0.0); dp.setMahasiswa(mahasiswa);
                    dp.setPerkuliahan(perkuliahan); dp.setTahap(tahapan); dp.setSemester(semester);
                    dp.setPersetujuan(Detailperkuliahan.DISETUJUI); // KRS Paket langsung disetujui
                    
                    Common.refreshSaveOrUpdate(sess, dp);
                }
            }
            tx.commit();
            
            if (!peringatanWarning.isEmpty()) {
                jsonResponse.put("status", "warning"); jsonResponse.put("message", Common.getBahasaConfig("Disimpan dengan peringatan. Beberapa matakuliah gagal ditambahkan:\n") + peringatanWarning);
            } else {
                jsonResponse.put("status", "success"); jsonResponse.put("message", Common.getBahasaConfig("Paket KRS berhasil disimpan ke dalam sistem dan telah disetujui."));
            }
            out.print(jsonResponse.toString()); out.flush();
        }

        // =================================================================================
        // ACTION 5: GET KRS DIAMBIL & HAPUS KRS (IDENTIK DENGAN REGULER)
        // =================================================================================
        else if ("get_krs_diambil".equals(action)) {
            if (mahasiswa == null) { out.print("{\"data\": []}"); return; }
            List<Long> dpsIds = (periodeId != null && !periodeId.isEmpty()) ? 
                Common.getDetailperkuliahans(mahasiswa, semester, tahapan, null, semesterPendek, remedial, (semesterPendek == null || !remedial), true, isRefresh) : 
                Common.getDetailperkuliahans(mahasiswa, null, null, null, null, remedial, (semesterPendek == null || !remedial), true, isRefresh);

            JSONArray dataArray = new JSONArray();
            if (dpsIds != null) {
                for (Long id : dpsIds) {
                    Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, id.toString());
                    if (detailperkuliahan == null) continue;
                    Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
                    Kurikulum kurikulum = perkuliahan == null ? null : perkuliahan.getKurikulum();
                    Matakuliah matakuliah = perkuliahan == null ? detailperkuliahan.getMatakuliahKonversi() : perkuliahan.getMatakuliah();
                    if (matakuliah == null) continue;

                    String resKode = matakuliah.getKode(); String resNama = matakuliah.getNama();
                    String namaHtml = "<div><span class='fw-bold text-dark'>" + resKode + "</span><br>" + resNama + (kurikulum == null ? "" : " <br><small class='text-muted'>(" + Common.getBahasaConfig("Kurikulum") + ": " + kurikulum.getTahun() + ")</small>") + "</div>";
                    String resSks = matakuliah.getSks() + "";
                    String resDosen = PerkuliahanUIHelper.generateTeksDosenPerkuliahan(perkuliahan);
                    String resJadwalRuang = PerkuliahanUIHelper.generateHariJamRuanganPerkuliahanUmumText(perkuliahan);
                    String resKelas = perkuliahan == null ? "" : perkuliahan.getKelas();

                    String smtKeterangan = "Smt: " + detailperkuliahan.getSemester();
                    if (perkuliahan != null) {
                        String txtTipe = detailperkuliahan.getSemester() > perkuliahan.getSemester() ? Common.getBahasaConfig("Mengulang") : Common.getBahasaConfig("Menabung");
                        if (!detailperkuliahan.getSemester().equals(perkuliahan.getSemester())) smtKeterangan += " / " + perkuliahan.getSemester() + " <br><span class='badge bg-warning text-dark'>" + txtTipe + "</span>";
                    }

                    boolean isBelum = detailperkuliahan.getPersetujuan() == null || detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI);
                    String txtPersetujuan = isBelum ? Common.getBahasaConfig("Belum") : Common.getBahasaConfig("Ya");
                    String statusAcc = isBelum ? "<span class='badge bg-danger'><i class='fas fa-times me-1'></i> " + txtPersetujuan + "</span>" : "<span class='badge bg-primary'><i class='fas fa-check me-1'></i> " + txtPersetujuan + "</span>";

                    JSONObject obj = new JSONObject();
                    obj.put("id", detailperkuliahan.getId());
                    obj.put("matakuliah", namaHtml); obj.put("sks", resSks); obj.put("dosen", resDosen);
                    obj.put("jadwal", resJadwalRuang); obj.put("smt_kelas", smtKeterangan + "<br>" + Common.getBahasaConfig("Kelas") + ": " + resKelas);
                    obj.put("status_acc", statusAcc); obj.put("is_belum", isBelum); 
                    dataArray.put(obj);
                }
            }
            JSONObject tableJson = new JSONObject(); tableJson.put("data", dataArray); out.print(tableJson.toString()); out.flush();
        }

    } catch (Exception e) {
        if (tx != null && tx.isActive()) tx.rollback();
        jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Terjadi kesalahan sistem internal:") + " " + e.getMessage()); out.print(jsonResponse.toString()); out.flush();
    } finally {
        if (sess != null && sess.isOpen()) { try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/krs/_krs_paket_service.jsp:359");} }
    }
%>