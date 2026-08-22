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
        String debugInfo = "Mode Default";
        
        if (mahasiswa != null && periodeId != null && periodeId.length() >= 5) {
            try {
                String strTahun = periodeId.substring(0, 4);
                String strJenis = periodeId.substring(4, 5);
                
                Integer tahun = Integer.parseInt(strTahun);
                String semesterMulaiCalc = strJenis.equals("1") ? Perkuliahan.GANJIL : strJenis.equals("2") ? Perkuliahan.GENAP : Perkuliahan.SP;
                
                Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan() != null ? mahasiswa.getTahunangkatan() : tahun;
                
                Integer smt = Common.getSemester(tahunAngkatanMhs, semesterMulaiCalc, mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
                
                semester = smt;
                semesterPendek = strJenis.equals("3") ? Perkuliahan.SEMESTER_PENDEK : null;
                tahunAjaran = tahun + "/" + (tahun + 1);
                
                debugInfo = "Periode: " + periodeId + " | Tahun: " + tahun + " | JenisSmt: " + semesterMulaiCalc + " | Calc Semester: " + semester + " | SP: " + semesterPendek;
                
                if (isDebug) {
                    System.out.println("==================================================");
                    System.out.println("DEBUG KRS -> " + debugInfo);
                    System.out.println("==================================================");
                }
            } catch(Exception e) {
                if (isDebug) System.out.println("DEBUG KRS ERROR -> Gagal mem-parsing periodeId: " + periodeId);
            }
        } else if (mahasiswa != null) {
            semester = mahasiswa.currentSemester();
        }
        
        Integer tahapan = request.getParameter("tahapan") != null && !request.getParameter("tahapan").isEmpty() ? Integer.parseInt(request.getParameter("tahapan")) : 0;
        boolean remedial = "true".equals(request.getParameter("remedial"));
        boolean isRefresh = "true".equals(request.getParameter("refresh"));
        
        // =================================================================================
        // ACTION 1: VALIDASI SEBELUM AMBIL KRS
        // =================================================================================
        if ("validasi_ambil_krs".equals(action)) {
            if (mahasiswa == null) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Sesi login Mahasiswa tidak ditemukan.")); out.print(jsonResponse.toString()); return;
            }

            KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, false);
            Dosen dosenPembimbingAkademik = krsMahasiswa.getDosenPa();
            
            Integer persetujuan = null;
            Boolean hitungSemua = semesterPendek == null || !remedial;
            Boolean saring = true;
            Boolean reload = isRefresh;
            
            List<Long> detailperkuliahans;
            if (periodeId != null && !periodeId.isEmpty()) {
                detailperkuliahans = Common.getDetailperkuliahans(mahasiswa, semester, tahapan, persetujuan, semesterPendek, remedial, hitungSemua, saring, reload);
            } else {
                detailperkuliahans = Common.getDetailperkuliahans(mahasiswa, null, null, null, null, remedial, hitungSemua, saring, reload);
            }
            
            Konfigurasi konfigurasi = Common.getKonfigurasi(remedial ? Konfigurasi.KRS_REMEDIAL : semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP, tahunAjaran, (semester != null && semester % 2 == 0) ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
            Konfigurasi konfigurasiPerbaikan = Common.getKonfigurasi(remedial ? Konfigurasi.PERBAIKAN_KRS_REMEDIAL : semesterPendek == null ? Konfigurasi.PERBAIKAN_KRS : Konfigurasi.PERBAIKAN_KRS_SP, tahunAjaran, (semester != null && semester % 2 == 0) ? Perkuliahan.GENAP : Perkuliahan.GANJIL);

            List<String> warnings = new ArrayList<String>();
            Criteria critSyarat = sess.createCriteria(SyaratUjian.class).add(Restrictions.eq("krs", true)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            List<SyaratUjian> syaratUjians = ConstantValues.simpleList(critSyarat, SyaratUjian.class);
            if (syaratUjians != null) {
                for (SyaratUjian syaratUjian : syaratUjians) SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester, "Ambil KRS", warnings);
            }
            if (!warnings.isEmpty()) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", String.join("\n\n", warnings)); out.print(jsonResponse.toString()); return;
            }

            boolean masaKrsAktif = (konfigurasi != null && konfigurasi.getNilai() != null && konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
            boolean masaPerbaikanKrsAktif = (konfigurasiPerbaikan != null && konfigurasiPerbaikan.getNilai() != null && konfigurasiPerbaikan.getNilai().equals(Konfigurasi.AKTIF));
            if (!masaKrsAktif) {
                if (masaPerbaikanKrsAktif) {
                    if (detailperkuliahans == null || detailperkuliahans.isEmpty()) {
                        jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Anda belum pernah mengambil KRS sehingga tidak dapat melakukan perbaikan. Silakan hubungi bagian Akademik.")); out.print(jsonResponse.toString()); return;
                    }
                } else {
                    jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Saat ini bukan masa pengambilan atau perbaikan KRS berdasarkan Kalender Akademik.")); out.print(jsonResponse.toString()); return;
                }
            }

            if (dosenPembimbingAkademik == null && Common.getKonfigurasi("dosen_pa_harus_ada_sebelum_isi_krs", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Anda belum memiliki Dosen Pembimbing Akademik.")); out.print(jsonResponse.toString()); return;
            }

            if (Common.getKonfigurasi("kelas_harus_ada_sebelum_isi_krs", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                if (krsMahasiswa.getKelas() == null || krsMahasiswa.getKelas().trim().isEmpty()) {
                    jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Anda belum terdaftar dalam kelas mana pun.")); out.print(jsonResponse.toString()); return;
                }
            }

            if (semesterPendek == null) {
                if (Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                    if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null || tahapan.equals(0)) {
                        if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false, false) && semester != null && semester >= 1) {
                            jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Anda belum menyelesaikan pembayaran biaya perkuliahan.")); out.print(jsonResponse.toString()); return;
                        }
                    }
                    if (!UtsDanUasCheckerHelper.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahapan)) {
                        jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Syarat pembayaran sebelum pengambilan KRS belum terpenuhi.")); out.print(jsonResponse.toString()); return;
                    }
                }
            } else {
                if (Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs_sp", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                    if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null || tahapan.equals(0)) {
                        if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false, true)) {
                            jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Anda belum menyelesaikan pembayaran biaya perkuliahan semester pendek.")); out.print(jsonResponse.toString()); return;
                        }
                    }
                }
            }

            if (Common.getKonfigurasi("status_mahasiswa_harus_aktif_sebelum_isi_krs", Konfigurasi.AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa).ambilStatusMahasiswa(semester);
                if (statusMahasiswa == null || !statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId())) {
                    jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Status kemahasiswaan Anda saat ini tidak aktif. Anda tidak diizinkan untuk mengambil KRS.")); out.print(jsonResponse.toString()); return;
                }
            }

            if (semesterPendek == null && !Common.checkStatusPembayaranMahasiswaSebelumnya(semester, tahapan, mahasiswa)) {
                String pct = Common.getKonfigurasi("batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_mengisi_krs", "90").getNilai().trim();
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Anda belum melunasi minimal") + " " + pct + "% " + Common.getBahasaConfig("dari total biaya perkuliahan semester sebelumnya.")); out.print(jsonResponse.toString()); return;
            }

            Criteria critBlokir = sess.createCriteria(BlokirMahasiswa.class)
                    .add(Restrictions.isNotNull("keterangan")).add(Restrictions.ne("keterangan", ""))
                    .add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("krs", true))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            List<BlokirMahasiswa> listBlokir = ConstantValues.simpleList(critBlokir, BlokirMahasiswa.class);
            if (listBlokir != null && !listBlokir.isEmpty()) {
                List<String> alasans = new ArrayList<String>();
                for (BlokirMahasiswa bm : listBlokir) alasans.add(bm.getKeterangan());
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Pemberitahuan Sistem:") + "\n" + String.join("\n\n", alasans)); out.print(jsonResponse.toString()); return;
            }

            Double[] batasSksArr = Common.getMinDanMaxIPK(mahasiswa, semester, semesterPendek);
            int batasMaksimalSks = batasSksArr[0].intValue();
            
            HashMap<Long, Perkuliahan> hashMapEmpty = new HashMap<>(); 
            int sisaSksDiambil = KrsUtilHelper.hitungSksYangTelahDiambil(hashMapEmpty, mahasiswa, tahapan, semester, semesterPendek);

            jsonResponse.put("status", "success");
            jsonResponse.put("max_sks", batasMaksimalSks);
            jsonResponse.put("sks_terambil", sisaSksDiambil);
            if(isDebug) jsonResponse.put("debug_info", debugInfo);
            out.print(jsonResponse.toString());
            out.flush();
        }

        // =================================================================================
        // ACTION 1.5: AMBIL MASTER FILTER (Fakultas, Jurusan, Program)
        // =================================================================================
        else if ("get_master_filters".equals(action)) {
            Jurusan currentJurusan = tbmuser.ambilJurusan();
            Fakultas currentFakultas = tbmuser.ambilFakultas();
            Program programUser = tbmuser.ambilProgram();
            String defProgram = mahasiswa != null ? mahasiswa.getProgram() : (programUser != null ? programUser.getNama() : "");
            
            List<Fakultas> listFakultas = ConstantValues.simpleList(sess.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Fakultas.class);
            List<Jurusan> listJurusan = ConstantValues.simpleList(sess.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Jurusan.class);
            List<Program> listProgram = ConstantValues.simpleList(sess.createCriteria(Program.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Program.class);
            
            JSONArray arrFakultas = new JSONArray();
            if(listFakultas != null) {
                for(Fakultas f : listFakultas) { JSONObject o = new JSONObject(); o.put("id", f.getId()); o.put("nama", f.getNama()); arrFakultas.put(o); }
            }
            
            JSONArray arrJurusan = new JSONArray();
            if(listJurusan != null) {
                for(Jurusan j : listJurusan) { JSONObject o = new JSONObject(); o.put("id", j.getId()); o.put("nama", j.getNama()); o.put("fakultas_id", j.getFakultas() != null ? j.getFakultas().getId() : null); arrJurusan.put(o); }
            }
            
            JSONArray arrProgram = new JSONArray();
            if(listProgram != null) {
                for(Program p : listProgram) { JSONObject o = new JSONObject(); o.put("id", p.getNama()); o.put("nama", p.getNamaBaru() != null ? p.getNamaBaru() : p.getNama()); arrProgram.put(o); }
            }
            
            jsonResponse.put("status", "success");
            jsonResponse.put("fakultas", arrFakultas);
            jsonResponse.put("jurusan", arrJurusan);
            jsonResponse.put("program", arrProgram);
            
            jsonResponse.put("def_fakultas", currentFakultas != null ? currentFakultas.getId() : null);
            jsonResponse.put("def_jurusan", currentJurusan != null ? currentJurusan.getId() : null);
            jsonResponse.put("def_program", defProgram);
            
            out.print(jsonResponse.toString());
            out.flush();
        }
        
        // =================================================================================
        // ACTION 2: AMBIL DAFTAR PERKULIAHAN DENGAN FILTER LANJUTAN
        // =================================================================================
        else if ("get_perkuliahan".equals(action)) {
            String qMk = request.getParameter("q_mk");
            String fFakultasId = request.getParameter("fak_id");
            String fJurusanId = request.getParameter("jur_id");
            String fProgram = request.getParameter("prog");
            String fKelas = request.getParameter("kls");
            
            // LOGIKA PERSIS DARI AmbilDataPerkuliahanHelper.java
            Set<Long> matakuliahTelahDiambil = new HashSet<Long>();
            Set<Long> perkuliahanTelahDiambil = new HashSet<Long>();
            
            for (Long oid : mahasiswa.ambilDetailperkuliahan()) {
                Detailperkuliahan o = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, oid.toString());
                if (o != null && o.getPerkuliahan() != null) {
                    perkuliahanTelahDiambil.add(o.getPerkuliahan().getId());
                    
                    boolean smtMatch = false;
                    if(semester != null && o.getSemester() != null) smtMatch = o.getSemester().equals(semester);
                    
                    if (smtMatch && (
                        (semesterPendek == null && o.getPerkuliahan().getStatusSemesterPendek() == null) || 
                        (semesterPendek != null && o.getPerkuliahan().getStatusSemesterPendek() != null)
                    )) {
                        matakuliahTelahDiambil.add(o.getPerkuliahan().getMatakuliah().getId());
                    }
                }
            }

            if (isDebug) {
                System.out.println("mahasiswa=>" + mahasiswa + ", semester=>" + semester + ", perkuliahanTelahDiambil=>" + perkuliahanTelahDiambil + ", matakuliahTelahDiambil=>" + matakuliahTelahDiambil);
            }

            Criteria criteria = sess.createCriteria(Perkuliahan.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(remedial ? Restrictions.eq("merupakanRemedial", true) : Restrictions.or(Restrictions.eq("merupakanRemedial", false), Restrictions.isNull("merupakanRemedial")))
                    .add(Restrictions.or(Restrictions.isNull("tampilkanSaatPengambilanKrs"), Restrictions.eq("tampilkanSaatPengambilanKrs", true)))
                    .add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek") : Restrictions.eq("statusSemesterPendek", semesterPendek))
                    .add(Restrictions.eq("tahunAjaran", tahunAjaran))
                    .add(Restrictions.or(Restrictions.eq("merupakan_paralel", false), Restrictions.isNull("merupakan_paralel")))
                    .createAlias("matakuliah", "matakuliah");
                    
            if (fFakultasId != null && !fFakultasId.trim().isEmpty() || fJurusanId != null && !fJurusanId.trim().isEmpty()) {
                criteria.createAlias("jurusan", "jurusan");
                if (fFakultasId != null && !fFakultasId.trim().isEmpty()) criteria.add(Restrictions.eq("jurusan.fakultas.id", Long.parseLong(fFakultasId)));
                if (fJurusanId != null && !fJurusanId.trim().isEmpty()) criteria.add(Restrictions.eq("jurusan.id", Long.parseLong(fJurusanId)));
            }
            if (fProgram != null && !fProgram.trim().isEmpty()) criteria.add(Restrictions.eq("program", fProgram));
            if (fKelas != null && !fKelas.trim().isEmpty()) criteria.add(Restrictions.ilike("kelas", fKelas.trim(), MatchMode.ANYWHERE));
            if (qMk != null && !qMk.trim().isEmpty()) {
                criteria.add(Restrictions.or(Restrictions.ilike("matakuliah.kode", qMk.trim(), MatchMode.ANYWHERE), Restrictions.ilike("matakuliah.nama", qMk.trim(), MatchMode.ANYWHERE)));
            }
            criteria.add(Restrictions.sqlRestriction("1=1 order by case hari when 'Senin' then 1 when 'Selasa' then 2 when 'Rabu' then 3 when 'Kamis' then 4 when 'Jumat' then 5 when 'Sabtu' then 6 when 'Minggu' then 7 else 5 end, waktu_mulai_d"));

            List<Perkuliahan> listPerkuliahan = ConstantValues.simpleList(criteria, Perkuliahan.class);
            JSONArray dataArray = new JSONArray();

            if (listPerkuliahan != null) {
                for (Perkuliahan p : listPerkuliahan) {
                    Matakuliah mk = p.getMatakuliah();
                    if (mk == null) continue;

                    boolean kurikulumBolehAmbil = (p.getKurikulum() != null && p.getKurikulum().bolehAmbil(mahasiswa));
                    
                    // Logic: Apakah sudah diambil
                    boolean isRemedial = p.getMerupakanRemedial() != null ? p.getMerupakanRemedial() : false;
                    boolean jmlMk = isRemedial ? false : (matakuliahTelahDiambil.contains(mk.getId()) || perkuliahanTelahDiambil.contains(p.getId()));
                    boolean jml = perkuliahanTelahDiambil.contains(p.getId());
                    
                    boolean tampilCheckbox = !jmlMk || !kurikulumBolehAmbil;
                    
                    Integer kapasitasKelas = p.getKapasitasKelas();
                    PembagianKuotaPerkuliahanBerdasarkantahunAngkatan kuotaTahun = KrsUtilHelper.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(sess, p, mahasiswa.getTahunangkatan(), false);
                    if (kuotaTahun != null && kuotaTahun.getKuota() != null) kapasitasKelas = kuotaTahun.getKuota().intValue();
                    
                    Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(sess, p, false);
                    
                    boolean checkboxDisabled = false;
                    if (!jml) { 
                        if (jumlahUdahMasuk >= kapasitasKelas) checkboxDisabled = true;
                    } else {
                        checkboxDisabled = true; // Lock in modal so they can't uncheck it here.
                    }
                    
                    // Prasyarat Check
                    if (!checkboxDisabled && !Common.checkMatakuliahPrasyarat(mk, mahasiswa, semester)) {
                        checkboxDisabled = true;
                    }

                    // Teks Status (Mengikuti MatakuliahRenderer)
                    String txStatus = ""; String badgeClass = ""; 
                    if (!kurikulumBolehAmbil) {
                        txStatus = Common.getBahasaConfig("Kurikulum ini Anda tidak boleh ambil"); badgeClass = "bg-danger"; checkboxDisabled = true;
                    } else if (jml) {
                        txStatus = Common.getBahasaConfig("Terpilih"); badgeClass = "bg-secondary"; // brown eq
                    } else if (jmlMk) {
                        txStatus = Common.getBahasaConfig("Anda telah mengambil matkul ini tapi mungkin di jadwal dan kelas yang berbeda"); badgeClass = "bg-success"; // green eq
                    } else if (jumlahUdahMasuk < kapasitasKelas) {
                        txStatus = Common.getBahasaConfig("Tersedia"); badgeClass = "bg-primary"; // blue eq
                    } else {
                        txStatus = Common.getBahasaConfig("Penuh"); badgeClass = "bg-danger"; // red eq
                    }

                    JSONObject obj = new JSONObject();
                    obj.put("id", p.getId());
                    obj.put("matakuliah", mk.getKode() + " - " + mk.getNama() + (p.getMerupakan_paralel() != null && p.getMerupakan_paralel() ? " <span class='badge bg-info'>Paralel</span>" : ""));
                    obj.put("sks", mk.getSks());
                    obj.put("sks_raw", mk.getSks()); 
                    obj.put("dosen", PerkuliahanUIHelper.generateTeksDosenPerkuliahan(p));
                    obj.put("jadwal", PerkuliahanUIHelper.generateHariJamRuanganPerkuliahanUmumText(p));
                    obj.put("smt", p.getSemester() == null ? "-" : p.getSemester());
                    obj.put("kelas", p.getKelas() == null ? "-" : p.getKelas());
                    obj.put("kapasitas", jumlahUdahMasuk + " / " + kapasitasKelas);
                    obj.put("status_html", "<span class='badge " + badgeClass + "'>" + txStatus + "</span>");
                    obj.put("disabled", checkboxDisabled);
                    obj.put("checked", jml);
                    obj.put("show_checkbox", tampilCheckbox);
                    
                    dataArray.put(obj);
                }
            }

            JSONObject tableJson = new JSONObject(); tableJson.put("data", dataArray); if(isDebug) tableJson.put("debug_info", debugInfo); out.print(tableJson.toString()); out.flush();
        }
        
        // =================================================================================
        // ACTION 3: SIMPAN KRS TERPILIH DENGAN SEMUA VALIDASI KOMPLEKS
        // =================================================================================
        else if ("simpan_krs".equals(action)) {
            String[] selectedIds = request.getParameterValues("perkuliahan_ids[]");
            
            if (selectedIds == null || selectedIds.length == 0) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Tidak ada matakuliah yang dipilih.")); out.print(jsonResponse.toString()); return;
            }

            HashMap<Long, Perkuliahan> hashMap = new HashMap<>();
            for (String pidStr : selectedIds) {
                Long pid = Long.parseLong(pidStr);
                Perkuliahan p = (Perkuliahan) sess.get(Perkuliahan.class, pid);
                if (p != null) hashMap.put(p.getId(), p);
            }

            sess.refresh(mahasiswa);
            Integer jumlahSksTerpilih = KrsUtilHelper.hitungSksYangTelahDiambil(hashMap, mahasiswa, tahapan, semester, semesterPendek);
            boolean isMelebihiKetentuan = Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, semester, jumlahSksTerpilih, semesterPendek);
            if (isMelebihiKetentuan) {
                Double[] batas = Common.getMinDanMaxIPK(mahasiswa, semester, semesterPendek);
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Jumlah SKS yang diambil melebihi batas maksimal yang diizinkan (Maksimal:") + " " + batas[0].intValue() + " SKS)."); out.print(jsonResponse.toString()); return;
            }

            if (Common.getKonfigurasi("saat_pengambilan_krs_tidak_diperbolehkan_ada_jam_bentrok", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
                List<Long> detailperkuliahansid = mahasiswa.ambilPerkuliahanDanParalel(semester, null);
                List<Detailperkuliahan> allDps = new ArrayList<>();
                for (Long detailperkuliahanid : detailperkuliahansid) {
                    Detailperkuliahan dpExisting = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
                    if (dpExisting != null && dpExisting.getPerkuliahan() != null) allDps.add(dpExisting);
                }
                for (Perkuliahan perkuliahan : hashMap.values()) {
                    Detailperkuliahan dpNew = new Detailperkuliahan(tbmuser, AmbilDataPerkuliahanHelper.class);
                    dpNew.setPerkuliahan(perkuliahan); dpNew.setSemester(semester); dpNew.setTahap(tahapan); allDps.add(dpNew);
                }
                if (!Common.checkJamBentrok(allDps)) {
                    jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Terdeteksi jadwal perkuliahan yang bentrok. Silakan periksa kembali jadwal Anda.")); out.print(jsonResponse.toString()); return;
                }
            }

            Transaction txStore = sess.beginTransaction();
            String peringatanWarning = "";
            Set<Long> matakuliahs = new HashSet<Long>();
            
            for (Perkuliahan perkuliahan : hashMap.values()) {
                if (matakuliahs.contains(perkuliahan.getMatakuliah().getId())) continue;
                matakuliahs.add(perkuliahan.getMatakuliah().getId());

                if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa, semester)) {
                    peringatanWarning += "- " + perkuliahan.getMatakuliah().getNama() + " (" + Common.getBahasaConfig("Prasyarat Belum Terpenuhi") + ")\n";
                    continue;
                }
                
                int count = KrsUtilHelper.ambilJumlahDetailperkuliahan(sess, perkuliahan, mahasiswa, false);
                if (count == 0) {
                    Integer kapasitasKelas = perkuliahan.getKapasitasKelas();
                    PembagianKuotaPerkuliahanBerdasarkantahunAngkatan kuotaTahun = KrsUtilHelper.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(sess, perkuliahan, mahasiswa.getTahunangkatan(), false);
                    if (kuotaTahun != null && kuotaTahun.getKuota() != null) kapasitasKelas = kuotaTahun.getKuota().intValue();
                    
                    Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(sess, perkuliahan, false);
                    if ((jumlahUdahMasuk + 1) > kapasitasKelas) {
                        peringatanWarning += "- " + perkuliahan.getMatakuliah().getNama() + " (" + Common.getBahasaConfig("Kapasitas Penuh") + ")\n";
                        continue;
                    }
                    
                    Detailperkuliahan dp = new Detailperkuliahan(tbmuser, AmbilDataPerkuliahanHelper.class);
                    dp.setNilaiHuruf(""); dp.setTotalNilai(0.0); dp.setMahasiswa(mahasiswa);
                    dp.setPerkuliahan(perkuliahan); dp.setTahap(tahapan); dp.setSemester(semester);
                    
					KrsUtilHelper.simpanKrsJikaBelumAda(sess, dp);
                }
            }
            txStore.commit();
            
            if (!peringatanWarning.isEmpty()) {
                jsonResponse.put("status", "warning"); jsonResponse.put("message", Common.getBahasaConfig("Disimpan dengan peringatan. Beberapa matakuliah gagal ditambahkan:\n") + peringatanWarning);
            } else {
                jsonResponse.put("status", "success"); jsonResponse.put("message", Common.getBahasaConfig("KRS berhasil disimpan ke dalam sistem."));
            }
            if(isDebug) jsonResponse.put("debug_info", debugInfo);
            out.print(jsonResponse.toString()); out.flush();
        }

        // =================================================================================
        // ACTION 4: OPSI FILTER TAHUN AKADEMIK & SEMESTER
        // =================================================================================
        else if ("get_filter_smt".equals(action)) {
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
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/krs/_krs_service.jsp:463");}
                }
                
                JSONObject obj = new JSONObject(); obj.put("idSmt", pId); obj.put("label", entry.getValue()); 
                if (calcSmt != null) obj.put("smt", calcSmt); if (calcSp != null) obj.put("sp", calcSp);
                filterArray.put(obj);
            }
            
            JSONObject root = new JSONObject(); root.put("data", filterArray); root.put("default_id", defaultIdSmt); out.print(root.toString()); out.flush();
        }

        // =================================================================================
        // ACTION 5: AMBIL DATA KRS YANG TELAH DIAMBIL
        // =================================================================================
        else if ("get_krs_diambil".equals(action)) {
            if (mahasiswa == null) { out.print("{\"data\": []}"); return; }

            Integer persetujuan = null;
            Boolean hitungSemua = semesterPendek == null || !remedial;
            Boolean saring = true;
            Boolean reload = isRefresh;
            
            List<Long> dpsIds;
            if (periodeId != null && !periodeId.isEmpty()) {
                dpsIds = Common.getDetailperkuliahans(mahasiswa, semester, tahapan, persetujuan, semesterPendek, remedial, hitungSemua, saring, reload);
            } else {
                dpsIds = Common.getDetailperkuliahans(mahasiswa, null, null, null, null, remedial, hitungSemua, saring, reload);
            }

            JSONArray dataArray = new JSONArray();

            if (dpsIds != null) {
                for (Long id : dpsIds) {
                    Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, id.toString());
                    if (detailperkuliahan == null) continue;

                    Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
                    Kurikulum kurikulum = perkuliahan == null ? null : perkuliahan.getKurikulum();
                    Matakuliah matakuliah = perkuliahan == null ? detailperkuliahan.getMatakuliahKonversi() : perkuliahan.getMatakuliah();

                    Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah, mahasiswa.getNim(), false);
                    matakuliah = matakuliahs[0]; Matakuliah matakuliahAsli = matakuliahs[1];

                    if (matakuliah == null) continue;

                    String resKode = matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getKode() + " (" + matakuliahAsli.getId() + ")" : matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ") (" + matakuliah.getId() + ")";
                    String resNama = matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama() : (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")");
                    String namaHtml = "<div><span class='fw-bold text-dark'>" + resKode + "</span><br>" + resNama + (kurikulum == null ? "" : " <br><small class='text-muted'>(Kurikulum: " + kurikulum.getTahun() + ")</small>") + "</div>";
                    String resSks = matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "") : (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")");
                    String resDosen = PerkuliahanUIHelper.generateTeksDosenPerkuliahan(perkuliahan);
                    String resJadwalRuang = PerkuliahanUIHelper.generateHariJamRuanganPerkuliahanUmumText(perkuliahan);
                    String resKelas = perkuliahan == null ? "" : perkuliahan.getKelas();

                    String smtKeterangan = "Smt: " + detailperkuliahan.getSemester();
                    String badgeTahunAjaran = "";
                    try {
                        if (perkuliahan != null) {
                            String namaSmtBadge = (perkuliahan.getStatusSemesterPendek() != null && perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)) ? Common.getBahasaConfig("Semester Pendek") : perkuliahan.getGanjilGenap();
                            badgeTahunAjaran = "<div class='text-secondary small mt-1'>(" + perkuliahan.getTahunAjaran() + " - " + namaSmtBadge + ")</div>";
                            
                            if (!detailperkuliahan.getSemester().equals(perkuliahan.getSemester())) {
                                String txtTipe = detailperkuliahan.getSemester() > perkuliahan.getSemester() ? Common.getBahasaConfig("Mengulang") : Common.getBahasaConfig("Menabung");
                                smtKeterangan = "Smt: " + detailperkuliahan.getSemester() + " / " + perkuliahan.getSemester() + " <br><span class='badge bg-warning text-dark'>" + txtTipe + "</span>";
                            }
                        }
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/krs/_krs_service.jsp:528");}

                    boolean isBelum = detailperkuliahan.getPersetujuan() == null || detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI);
                    String txtPersetujuan = isBelum ? Common.getBahasaConfig("Belum") : Common.getBahasaConfig("Ya");
                    String statusAcc = isBelum ? "<span class='badge bg-danger'><i class='fas fa-times me-1'></i> " + txtPersetujuan + "</span>" : "<span class='badge bg-primary'><i class='fas fa-check me-1'></i> " + txtPersetujuan + "</span>";

                    JSONObject obj = new JSONObject();
                    obj.put("id", detailperkuliahan.getId());
                    obj.put("matakuliah", namaHtml);
                    obj.put("sks", resSks);
                    obj.put("dosen", resDosen);
                    obj.put("jadwal", resJadwalRuang);
                    obj.put("smt_kelas", smtKeterangan + "<br>Kelas: " + resKelas + badgeTahunAjaran);
                    obj.put("status_acc", statusAcc);
                    obj.put("is_belum", isBelum); 
                    dataArray.put(obj);
                }
            }

            JSONObject tableJson = new JSONObject(); tableJson.put("data", dataArray); if(isDebug) tableJson.put("debug_info", debugInfo); out.print(tableJson.toString()); out.flush();
        }

    } catch (Exception e) {
        if (tx != null && tx.isActive()) tx.rollback();
        jsonResponse.put("status", "error");
        jsonResponse.put("message", Common.getBahasaConfig("Terjadi kesalahan sistem internal:") + " " + e.getMessage());
        out.print(jsonResponse.toString());
        out.flush();
    } finally {
        if (sess != null && sess.isOpen()) { try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/krs/_krs_service.jsp:557");} }
    }
%>
