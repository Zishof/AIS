<%@page import="ais.database.model.rab.SatuanKerja"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.StatuskehadiranKaryawanHarian"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.payroll.CutiDanIzin"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.*"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai.jsp:24");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    
    Tbmuser currentUser = Common.getCurrentUser(request);
    if (currentUser == null) { 
        out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; 
    }

    String startDateStr = request.getParameter("startDate");
    String endDateStr = request.getParameter("endDate");
    String q = request.getParameter("q");
    String jenisEntitas = request.getParameter("jenisEntitas");
    String statusAbsensiId = request.getParameter("statusAbsensiId");
    String satuanKerjaId = request.getParameter("satuanKerjaId");
    String[] hariAktifArr = request.getParameterValues("hariAktif");
    
    // Membaca array checkbox Hari Aktif dari Front-End
    List<Integer> activeDays = new ArrayList<Integer>();
    if (hariAktifArr != null) {
        for (String h : hariAktifArr) { 
            try { activeDays.add(Integer.parseInt(h)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai.jsp:44");} 
        }
    }

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
		Criteria crit = sess.createCriteria(StatuskehadiranKaryawanHarian.class);
        
        // --- BLOK 1: HAK AKSES & OTORISASI (Kompatibel dengan Hibernate 3.6) ---
        Tbmrole tbmrole = currentUser.hakAkses();
        boolean bolehLihatPegawaiLain = tbmrole != null && Boolean.TRUE.equals(tbmrole.getMelihatDataPegawaiLain());
        boolean bolehLihatSatuanKerja = tbmrole != null && Boolean.TRUE.equals(tbmrole.getMelihatDataSatkerLain());
        
        Pegawai myPegawai = currentUser.ambilPegawai();
        Dosen myDosen = currentUser.ambilDosen();
        Guru myGuru = currentUser.ambilGuru();
        Mahasiswa myMhs = currentUser.getMahasiswa();
        Siswa mySiswa = currentUser.getSiswa();
        SatuanKerja mySatker = currentUser.ambilSatuanKerja();

        if (!bolehLihatPegawaiLain) {
            // Perbaikan Logika: Jika semua entitas user bernilai null, langsung batasi data
            if (myPegawai == null && myDosen == null && myGuru == null && myMhs == null && mySiswa == null) {
                crit.add(Restrictions.sqlRestriction("1=0"));
            } else {
                // Merangkai kondisi OR tanpa menggunakan .conditions()
                Disjunction meOr = Restrictions.disjunction();
                if (myPegawai != null) meOr.add(Restrictions.eq("pegawai", myPegawai));
                if (myDosen != null) meOr.add(Restrictions.eq("dosen", myDosen));
                if (myGuru != null) meOr.add(Restrictions.eq("guru", myGuru));
                if (myMhs != null) meOr.add(Restrictions.eq("mahasiswa", myMhs));
                if (mySiswa != null) meOr.add(Restrictions.eq("siswa", mySiswa));
                
                crit.add(meOr);
            }
        } else {
            // Boleh melihat data orang lain, lakukan pemeriksaan tingkat Satuan Kerja
            if (!bolehLihatSatuanKerja) {
                if (mySatker != null) {
                    crit.add(Restrictions.or(Restrictions.eq("satuanKerja", mySatker), Restrictions.isNull("satuanKerja")));
                } else {
                    crit.add(Restrictions.isNull("satuanKerja"));
                }
            }
        }

        // --- BLOK 2: FILTER FORM (Tanggal, Satker, Status, Entitas) ---
        if (startDateStr != null && endDateStr != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            crit.add(Restrictions.between("tanggal", sdf.parse(startDateStr), sdf.parse(endDateStr)));
        }
        if (satuanKerjaId != null && !satuanKerjaId.isEmpty()) {
            crit.add(Restrictions.eq("satuanKerja.id", Long.parseLong(satuanKerjaId)));
        }
        if (statusAbsensiId != null && !statusAbsensiId.isEmpty()) {
            crit.add(Restrictions.eq("statusabsensi.id", Long.parseLong(statusAbsensiId)));
        }
        if (jenisEntitas != null && !jenisEntitas.equals("ALL")) {
            if (jenisEntitas.equals("PEGAWAI")) crit.add(Restrictions.isNotNull("pegawai"));
            else if (jenisEntitas.equals("DOSEN")) crit.add(Restrictions.isNotNull("dosen"));
            else if (jenisEntitas.equals("GURU")) crit.add(Restrictions.isNotNull("guru"));
            else if (jenisEntitas.equals("MAHASISWA")) crit.add(Restrictions.isNotNull("mahasiswa"));
            else if (jenisEntitas.equals("SISWA")) crit.add(Restrictions.isNotNull("siswa"));
        }

        // --- BLOK 3: FILTER PENCARIAN DIPERLUAS ---
        if (q != null && !q.trim().isEmpty()) {
            crit.createAlias("pegawai", "p", Criteria.LEFT_JOIN);
            crit.createAlias("dosen", "d", Criteria.LEFT_JOIN);
            crit.createAlias("guru", "g", Criteria.LEFT_JOIN);
            crit.createAlias("mahasiswa", "m", Criteria.LEFT_JOIN);
            crit.createAlias("siswa", "s", Criteria.LEFT_JOIN);
            
            Disjunction or = Restrictions.disjunction();
            
            or.add(Restrictions.ilike("p.nama", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("p.kode", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("p.mycode", q, MatchMode.ANYWHERE));
            
            or.add(Restrictions.ilike("d.nama", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("d.nidn", q, MatchMode.ANYWHERE));
            
            or.add(Restrictions.ilike("g.nama", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("g.nuptk", q, MatchMode.ANYWHERE));
            
            or.add(Restrictions.ilike("m.nama", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("m.nim", q, MatchMode.ANYWHERE));
            
            or.add(Restrictions.ilike("s.namaSiswa", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("s.nomorInduk", q, MatchMode.ANYWHERE));
            
            crit.add(or);
        }

        // ... [Lanjutkan dengan pengurutan Data (Order) dan pengambilan data list()] ...

        crit.addOrder(Order.desc("tanggal")).addOrder(Order.desc("id")).setMaxResults(500);

        List<StatuskehadiranKaryawanHarian> list = crit.list();
        
        // Map untuk mengelompokkan data per Individu
        Map<String, JSONObject> mapRekap = new LinkedHashMap<>();
        
        int statTepat = 0, statTelat = 0, statCepat = 0, statLain = 0;
        int jPeg = 0, jDos = 0, jGur = 0, jMhs = 0, jSis = 0;

        for (StatuskehadiranKaryawanHarian sk : list) {
        	// --- BLOK PENYARINGAN HARI AKTIF (SUDAH DIPERBAIKI) ---
            if (sk.getTanggal() != null && !activeDays.isEmpty()) {
                Calendar c = Calendar.getInstance();
                c.setTime(sk.getTanggal());
                
                // Karena Common.haris (Minggu=1 ... Sabtu=7) identik dengan Java Calendar,
                // kita bisa langsung mengambil nilainya tanpa dikurangi.
                int mapHari = c.get(Calendar.DAY_OF_WEEK);
                
                // Jika hari aktual tidak ada di dalam daftar checkbox yang dicentang, LEWATI
                if (!activeDays.contains(mapHari)) {
                    continue; 
                }
            }
            // --- AKHIR BLOK PENYARINGAN HARI AKTIF ---
            String keyId = "";
            String nmEntitas = "Unknown";
            String nama = "-";
            String nip = "-";

            // Identifikasi Entitas Unik
            if (sk.getSiswa() != null) { keyId = "S_" + sk.getSiswa().getId(); nmEntitas = Common.getBahasaConfig("Siswa"); nama = sk.getSiswa().getNamaSiswa(); nip = sk.getSiswa().getNomorInduk(); }
            else if (sk.getMahasiswa() != null) { keyId = "M_" + sk.getMahasiswa().getId(); nmEntitas = Common.getBahasaConfig("Mahasiswa"); nama = sk.getMahasiswa().getNama(); nip = sk.getMahasiswa().getNim(); }
            else if (sk.getGuru() != null) { keyId = "G_" + sk.getGuru().getId(); nmEntitas = Common.getBahasaConfig("Guru"); nama = sk.getGuru().getNama(); nip = sk.getGuru().getNuptk(); }
            else if (sk.getDosen() != null) { keyId = "D_" + sk.getDosen().getId(); nmEntitas = Common.getBahasaConfig("Dosen"); nama = sk.getDosen().getNama(); nip = sk.getDosen().getMycode(); }
            else if (sk.getPegawai() != null) { keyId = "P_" + sk.getPegawai().getId(); nmEntitas = Common.getBahasaConfig("Pegawai"); nama = sk.getPegawai().getNama(); nip = sk.getPegawai().getMycode(); }

            if (keyId.isEmpty()) continue;

            // Inisialisasi Data Agregat per Orang jika belum ada
            if (!mapRekap.containsKey(keyId)) {
                JSONObject o = new JSONObject();
                o.put("jenisEntitas", nmEntitas);
                o.put("nama", nama != null ? nama : "-");
                o.put("nip", nip != null ? nip : "-");
                o.put("totalHariAktif", 0);
                o.put("hadir", 0);
                o.put("tepatWaktu", 0);
                o.put("terlambat", 0);
                o.put("pulangCepat", 0);
                o.put("tidakAbsenPulang", 0);
                o.put("sakit", 0);
                o.put("izin", 0);
                o.put("alpa", 0);
                o.put("cuti", 0);
                mapRekap.put(keyId, o);
                
                // Kalkulasi untuk Grafik Dashboard
                if (nmEntitas.equals(Common.getBahasaConfig("Siswa"))) jSis++;
                else if (nmEntitas.equals(Common.getBahasaConfig("Mahasiswa"))) jMhs++;
                else if (nmEntitas.equals(Common.getBahasaConfig("Guru"))) jGur++;
                else if (nmEntitas.equals(Common.getBahasaConfig("Dosen"))) jDos++;
                else if (nmEntitas.equals(Common.getBahasaConfig("Pegawai"))) jPeg++;
            }

            JSONObject agg = mapRekap.get(keyId);
            agg.put("totalHariAktif", agg.getInt("totalHariAktif") + 1);

            // Pengecekan Kondisi Aktual
            CutiDanIzin cuti = sk.getCutiDanIzin();
            boolean isDisetujui = cuti != null && Boolean.TRUE.equals(cuti.getSetujui());
            boolean isTerlambat = Boolean.TRUE.equals(sk.getDatangTerlambat());
            boolean isCepatPulang = Boolean.TRUE.equals(sk.getPulangCepat());
            Date masuk = sk.getMasukjam();
            Date pulang = sk.getPulangJam();
            Statusabsensi sAbsen = sk.getStatusabsensi();
            Long idAbsen = sAbsen != null ? sAbsen.getId() : -1L;
            boolean isCuti = sAbsen != null && Boolean.TRUE.equals(sAbsen.getMerupakanCuti());

            // Agregasi Status Absen
            if (masuk != null || idAbsen == 1L) {
                agg.put("hadir", agg.getInt("hadir") + 1);
                
                if (isTerlambat && !isDisetujui) { agg.put("terlambat", agg.getInt("terlambat") + 1); statTelat++; }
                else if (isCepatPulang && !isDisetujui) { agg.put("pulangCepat", agg.getInt("pulangCepat") + 1); statCepat++; }
                else { agg.put("tepatWaktu", agg.getInt("tepatWaktu") + 1); statTepat++; }
                
                if (masuk != null && pulang == null) agg.put("tidakAbsenPulang", agg.getInt("tidakAbsenPulang") + 1);
            } else {
                if (idAbsen == 3L) agg.put("sakit", agg.getInt("sakit") + 1);
                else if (idAbsen == 4L) agg.put("izin", agg.getInt("izin") + 1);
                else if (idAbsen == 2L) agg.put("alpa", agg.getInt("alpa") + 1);
                
                if (isCuti) agg.put("cuti", agg.getInt("cuti") + 1);
                statLain++;
            }
        }

        // Konversi Map ke List dan Sort berdasarkan Nama
        List<JSONObject> sortedList = new ArrayList<>(mapRekap.values());
        Collections.sort(sortedList, new Comparator<JSONObject>() {
            public int compare(JSONObject o1, JSONObject o2) {
                return o1.optString("nama").compareToIgnoreCase(o2.optString("nama"));
            }
        });
        
        JSONArray dataArr = new JSONArray();
        for (JSONObject obj : sortedList) dataArr.put(obj);

        JSONObject result = new JSONObject();
        result.put("status", "00");
        result.put("data", dataArr);
        
        JSONObject statObj = new JSONObject();
        statObj.put("tepatWaktu", statTepat);
        statObj.put("terlambat", statTelat);
        statObj.put("cepatPulang", statCepat);
        statObj.put("tidakAbsen", statLain);
        statObj.put("jmlPegawai", jPeg);
        statObj.put("jmlDosen", jDos);
        statObj.put("jmlGuru", jGur);
        statObj.put("jmlMahasiswa", jMhs);
        statObj.put("jmlSiswa", jSis);
        result.put("statistik", statObj);

        out.print(result.toString());
    } catch (Exception e) { 
        out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}"); 
    } finally { 
        if (sess != null && sess.isOpen()) { try { sess.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai.jsp:271");} }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>