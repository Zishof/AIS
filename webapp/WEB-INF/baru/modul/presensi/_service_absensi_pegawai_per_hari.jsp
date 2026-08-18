<%@page import="ais.database.model.StatuskehadiranKaryawanHarian"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="ais.database.model.rab.SatuanKerja"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Mahasiswa"%>
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
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_hari.jsp:23");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    
    Tbmuser currentUser = Common.getCurrentUser(request);
    if (currentUser == null) { 
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir.") + "\"}"); return; 
    }

    String startDateStr = request.getParameter("startDate");
    String endDateStr = request.getParameter("endDate");
    String q = request.getParameter("q");
    String jenisEntitas = request.getParameter("jenisEntitas");
    String satuanKerjaId = request.getParameter("satuanKerjaId");
    String[] hariAktifArr = request.getParameterValues("hariAktif");
    
    List<Integer> activeDays = new ArrayList<>();
    if (hariAktifArr != null) {
        for (String h : hariAktifArr) { try { activeDays.add(Integer.parseInt(h)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_hari.jsp:40");} }
    }

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        Criteria crit = sess.createCriteria(StatuskehadiranKaryawanHarian.class);
        
        // --- HAK AKSES ---
        Tbmrole tbmrole = currentUser.hakAkses();
        boolean bolehLihatPegawaiLain = tbmrole != null && Boolean.TRUE.equals(tbmrole.getMelihatDataPegawaiLain());
        boolean bolehLihatSatuanKerja = tbmrole != null && Boolean.TRUE.equals(tbmrole.getMelihatDataSatkerLain());
        
        Pegawai myPegawai = currentUser.ambilPegawai(); Dosen myDosen = currentUser.ambilDosen(); Guru myGuru = currentUser.ambilGuru();
        Mahasiswa myMhs = currentUser.getMahasiswa(); Siswa mySiswa = currentUser.getSiswa(); SatuanKerja mySatker = currentUser.ambilSatuanKerja();

        if (!bolehLihatPegawaiLain) {
            if (myPegawai == null && myDosen == null && myGuru == null && myMhs == null && mySiswa == null) { crit.add(Restrictions.sqlRestriction("1=0")); } 
            else {
                Disjunction meOr = Restrictions.disjunction();
                if (myPegawai != null) meOr.add(Restrictions.eq("pegawai", myPegawai));
                if (myDosen != null) meOr.add(Restrictions.eq("dosen", myDosen));
                if (myGuru != null) meOr.add(Restrictions.eq("guru", myGuru));
                if (myMhs != null) meOr.add(Restrictions.eq("mahasiswa", myMhs));
                if (mySiswa != null) meOr.add(Restrictions.eq("siswa", mySiswa));
                crit.add(meOr);
            }
        } else if (!bolehLihatSatuanKerja) {
            if (mySatker != null) crit.add(Restrictions.or(Restrictions.eq("satuanKerja", mySatker), Restrictions.isNull("satuanKerja")));
            else crit.add(Restrictions.isNull("satuanKerja"));
        }

        // --- FILTER PARAMETER ---
        Date dateStart = null; Date dateEnd = null;
        if (startDateStr != null && endDateStr != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            dateStart = sdf.parse(startDateStr); dateEnd = sdf.parse(endDateStr);
            crit.add(Restrictions.between("tanggal", dateStart, dateEnd));
        }
        if (satuanKerjaId != null && !satuanKerjaId.isEmpty()) crit.add(Restrictions.eq("satuanKerja.id", Long.parseLong(satuanKerjaId)));
        if (jenisEntitas != null && !jenisEntitas.equals("ALL")) {
            if (jenisEntitas.equals("PEGAWAI")) crit.add(Restrictions.isNotNull("pegawai"));
            else if (jenisEntitas.equals("DOSEN")) crit.add(Restrictions.isNotNull("dosen"));
            else if (jenisEntitas.equals("GURU")) crit.add(Restrictions.isNotNull("guru"));
            else if (jenisEntitas.equals("MAHASISWA")) crit.add(Restrictions.isNotNull("mahasiswa"));
            else if (jenisEntitas.equals("SISWA")) crit.add(Restrictions.isNotNull("siswa"));
        }
        if (q != null && !q.trim().isEmpty()) {
            crit.createAlias("pegawai", "p", Criteria.LEFT_JOIN).createAlias("dosen", "d", Criteria.LEFT_JOIN)
                .createAlias("guru", "g", Criteria.LEFT_JOIN).createAlias("mahasiswa", "m", Criteria.LEFT_JOIN).createAlias("siswa", "s", Criteria.LEFT_JOIN);
            Disjunction or = Restrictions.disjunction();
            or.add(Restrictions.ilike("p.nama", q, MatchMode.ANYWHERE)).add(Restrictions.ilike("p.mycode", q, MatchMode.ANYWHERE))
              .add(Restrictions.ilike("d.nama", q, MatchMode.ANYWHERE)).add(Restrictions.ilike("g.nama", q, MatchMode.ANYWHERE))
              .add(Restrictions.ilike("m.nama", q, MatchMode.ANYWHERE)).add(Restrictions.ilike("s.namaSiswa", q, MatchMode.ANYWHERE));
            crit.add(or);
        }

        // Ambil Data DB
        List<StatuskehadiranKaryawanHarian> list = crit.list();
        
        // 1. Identifikasi Seluruh Personil Unik & Petakan Data Berdasarkan "Tgl_Personil"
        Map<String, JSONObject> mapPersons = new HashMap<>();
        Map<String, StatuskehadiranKaryawanHarian> mapLogs = new HashMap<>();
        
        int statTepat = 0, statTelat = 0, statCepat = 0, statLain = 0;
        int jPeg = 0, jDos = 0, jGur = 0, jMhs = 0, jSis = 0;

        for (StatuskehadiranKaryawanHarian sk : list) {
            if (sk.getTanggal() == null) continue;
            String keyId = ""; String nama = "-"; String nip = "-"; String entitas = "Unknown";
            
            if (sk.getSiswa() != null) { keyId = "S_" + sk.getSiswa().getId(); entitas = "Siswa"; nama = sk.getSiswa().getNamaSiswa(); nip = sk.getSiswa().getNomorInduk(); }
            else if (sk.getMahasiswa() != null) { keyId = "M_" + sk.getMahasiswa().getId(); entitas = "Mahasiswa"; nama = sk.getMahasiswa().getNama(); nip = sk.getMahasiswa().getNim(); }
            else if (sk.getGuru() != null) { keyId = "G_" + sk.getGuru().getId(); entitas = "Guru"; nama = sk.getGuru().getNama(); nip = sk.getGuru().getNuptk(); }
            else if (sk.getDosen() != null) { keyId = "D_" + sk.getDosen().getId(); entitas = "Dosen"; nama = sk.getDosen().getNama(); nip = sk.getDosen().getMycode(); }
            else if (sk.getPegawai() != null) { keyId = "P_" + sk.getPegawai().getId(); entitas = "Pegawai"; nama = sk.getPegawai().getNama(); nip = sk.getPegawai().getMycode(); }

            if (keyId.isEmpty()) continue;
            
            if (!mapPersons.containsKey(keyId)) {
                JSONObject p = new JSONObject();
                p.put("nama", nama); p.put("nip", nip); p.put("entitas", entitas);
                mapPersons.put(keyId, p);
                
                if (entitas.equals("Siswa")) jSis++; else if (entitas.equals("Mahasiswa")) jMhs++;
                else if (entitas.equals("Guru")) jGur++; else if (entitas.equals("Dosen")) jDos++;
                else if (entitas.equals("Pegawai")) jPeg++;
            }
            
            String tglKey = Common.dateFormat83.get().format(sk.getTanggal());
            mapLogs.put(tglKey + "_" + keyId, sk);

            // Statistik Global
            boolean isTerlambat = Boolean.TRUE.equals(sk.getDatangTerlambat());
            boolean isCepatPulang = Boolean.TRUE.equals(sk.getPulangCepat());
            if (isTerlambat) statTelat++; else if (isCepatPulang) statCepat++;
            else if (sk.getMasukjam() != null) statTepat++; else statLain++;
        }

        // Sort Personil berdasarkan Nama
        List<Map.Entry<String, JSONObject>> sortedPersons = new ArrayList<>(mapPersons.entrySet());
        Collections.sort(sortedPersons, new Comparator<Map.Entry<String, JSONObject>>() {
            public int compare(Map.Entry<String, JSONObject> e1, Map.Entry<String, JSONObject> e2) {
                return e1.getValue().optString("nama").compareToIgnoreCase(e2.getValue().optString("nama"));
            }
        });

        // 2. Loop Kalender (Group By Tanggal)
        JSONArray dataArr = new JSONArray();
        Date hariIni = ais.ui.util.WaktuUtil.getDate();
        
        Calendar cMulai = Calendar.getInstance(); cMulai.setTime(dateStart);
        Calendar cSampai = Calendar.getInstance(); cSampai.setTime(dateEnd);
        
        while (!cMulai.getTime().after(cSampai.getTime())) {
            Date currDate = cMulai.getTime();
            int dayOfWeek = cMulai.get(Calendar.DAY_OF_WEEK);
            
            // Terapkan Filter Hari Aktif
            if (!activeDays.isEmpty() && !activeDays.contains(dayOfWeek)) {
                cMulai.add(Calendar.DATE, 1); continue;
            }
            
            String tglKey = Common.dateFormat83.get().format(currDate);
            String tglLabel = Common.dateFormat6.get().format(currDate); // Senin, 16 Maret 2026
            
            JSONObject dateObj = new JSONObject();
            dateObj.put("tanggalStr", tglLabel);
            JSONArray rincianArr = new JSONArray();
            
            for (Map.Entry<String, JSONObject> pEntry : sortedPersons) {
                String pKey = pEntry.getKey();
                JSONObject pInfo = pEntry.getValue();
                
                StatuskehadiranKaryawanHarian sk = mapLogs.get(tglKey + "_" + pKey);
                
                JSONObject detail = new JSONObject();
                detail.put("nama", pInfo.getString("nama"));
                detail.put("nip", pInfo.getString("nip"));
                detail.put("entitas", pInfo.getString("entitas"));
                
                if (sk != null) {
                    String jamKerja = (sk.getDetailJenisShiftPegawai() != null && sk.getDetailJenisShiftPegawai().getMulai() != null) ? 
                        (Common.timeFormat.get().format(sk.getDetailJenisShiftPegawai().getMulai()) + " - " + Common.timeFormat.get().format(sk.getDetailJenisShiftPegawai().getSampai())) : "";
                    
                    detail.put("jamKerja", jamKerja);
                    detail.put("jamMasuk", sk.ambilMasukjam() != null ? Common.timeFormat.get().format(sk.ambilMasukjam()) : "");
                    detail.put("jamKeluar", sk.ambilPulangjam() != null ? Common.timeFormat.get().format(sk.ambilPulangjam()) : "");
                    detail.put("terlambat", sk.getJumlahTerlambat() != null ? sk.getJumlahTerlambat() : 0.0);
                    detail.put("cepatPulang", sk.getJumlahCepatKeluar() != null ? sk.getJumlahCepatKeluar() : 0.0);
                    detail.put("lembur", sk.getJumlahLemburMasuk() != null ? sk.getJumlahLemburMasuk() : 0.0);
                    
                    Statusabsensi sAbsen = sk.getStatusabsensi();
                    String ket = (sAbsen != null ? sAbsen.getNama() : "") + " " + (sk.getKeterangan() != null ? sk.getKeterangan() : "");
                    detail.put("catatan", ket.trim());
                } else {
                    detail.put("jamKerja", ""); detail.put("jamMasuk", ""); detail.put("jamKeluar", "");
                    detail.put("terlambat", 0.0); detail.put("cepatPulang", 0.0); detail.put("lembur", 0.0);
                    if (currDate.before(hariIni)) detail.put("catatan", Common.getBahasaConfig("Alpha / Tanpa Keterangan"));
                    else detail.put("catatan", "-");
                }
                rincianArr.put(detail);
            }
            
            if (rincianArr.length() > 0) {
                dateObj.put("rincian", rincianArr);
                dataArr.put(dateObj);
            }
            cMulai.add(Calendar.DATE, 1);
        }

        JSONObject result = new JSONObject();
        result.put("status", "00");
        result.put("data", dataArr);
        
        JSONObject statObj = new JSONObject();
        statObj.put("tepatWaktu", statTepat); statObj.put("terlambat", statTelat); statObj.put("cepatPulang", statCepat);
        statObj.put("tidakAbsen", statLain); statObj.put("jmlPegawai", jPeg); statObj.put("jmlDosen", jDos);
        statObj.put("jmlGuru", jGur); statObj.put("jmlMahasiswa", jMhs); statObj.put("jmlSiswa", jSis);
        result.put("statistik", statObj);

        out.print(result.toString());

    } catch (Exception e) { 
        out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}"); 
    } finally { 
        if (sess != null && sess.isOpen()) { try { sess.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_hari.jsp:226");} }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>