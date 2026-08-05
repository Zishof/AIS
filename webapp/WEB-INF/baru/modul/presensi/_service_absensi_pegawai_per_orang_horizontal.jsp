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
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_orang_horizontal.jsp:23");}
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
        for (String h : hariAktifArr) { try { activeDays.add(Integer.parseInt(h)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_orang_horizontal.jsp:40");} }
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
        
        // 1. BUAT ARRAY KOLOM TANGGAL (Sumbu X)
        JSONArray colArr = new JSONArray();
        List<String> listTglAktif = new ArrayList<>();
        
        Calendar cMulai = Calendar.getInstance(); cMulai.setTime(dateStart);
        Calendar cSampai = Calendar.getInstance(); cSampai.setTime(dateEnd);
        
        while (!cMulai.getTime().after(cSampai.getTime())) {
            int dayOfWeek = cMulai.get(Calendar.DAY_OF_WEEK);
            if (!activeDays.isEmpty() && !activeDays.contains(dayOfWeek)) {
                cMulai.add(Calendar.DATE, 1); continue;
            }
            
            String tglKey = Common.dateFormat83.get().format(cMulai.getTime()); // Kunci Map (yyyy-MM-dd)
            String label = String.valueOf(cMulai.get(Calendar.DATE)); // Label Angka (16, 17, 18)
            
            JSONObject c = new JSONObject();
            c.put("tglKey", tglKey);
            c.put("label", label);
            c.put("fullFormat", Common.dateFormat6.get().format(cMulai.getTime()));
            colArr.put(c);
            listTglAktif.add(tglKey);
            
            cMulai.add(Calendar.DATE, 1);
        }

        // 2. KELOMPOKKAN DATA LOG PER PERSONIL (Sumbu Y)
        Map<String, JSONObject> mapPegawai = new LinkedHashMap<>();
        int statTepat = 0, statTelat = 0, statCepat = 0, statLain = 0;
        int jPeg = 0, jDos = 0, jGur = 0, jMhs = 0, jSis = 0;
        
        java.text.SimpleDateFormat sdfJam = new java.text.SimpleDateFormat("HH.mm"); // Format Titik

        for (StatuskehadiranKaryawanHarian sk : list) {
            if (sk.getTanggal() == null) continue;
            String tglStr = Common.dateFormat83.get().format(sk.getTanggal());
            if (!listTglAktif.contains(tglStr)) continue; // Abaikan jika hari tidak aktif
            
            String keyId = ""; String nama = "-"; String nip = ""; String dept = "-"; String nmEntitas = "Unknown";
            
            // Resolusi Identitas
            if (sk.getSiswa() != null) { keyId = "S_" + sk.getSiswa().getId(); nmEntitas = "Siswa"; nama = sk.getSiswa().getNamaSiswa(); nip = sk.getSiswa().getNomorInduk(); dept = sk.getSiswa().getSekolah() != null ? sk.getSiswa().getSekolah().getNama() : "-"; }
            else if (sk.getMahasiswa() != null) { keyId = "M_" + sk.getMahasiswa().getId(); nmEntitas = "Mahasiswa"; nama = sk.getMahasiswa().getNama(); nip = sk.getMahasiswa().getNim(); dept = sk.getMahasiswa().getJurusan() != null ? sk.getMahasiswa().getJurusan().getNama() : "-"; }
            else if (sk.getGuru() != null) { keyId = "G_" + sk.getGuru().getId(); nmEntitas = "Guru"; nama = sk.getGuru().getNama(); nip = sk.getGuru().getNuptk(); dept = sk.getSatuanKerja() != null ? sk.getGuru().getNama() : "-"; }
            else if (sk.getDosen() != null) { keyId = "D_" + sk.getDosen().getId(); nmEntitas = "Dosen"; nama = sk.getDosen().getNama(); nip = sk.getDosen().getMycode(); dept = sk.getSatuanKerja() != null ? sk.getSatuanKerja().getNama() : "-"; }
            else if (sk.getPegawai() != null) { keyId = "P_" + sk.getPegawai().getId(); nmEntitas = "Pegawai"; nama = sk.getPegawai().getNama(); nip = sk.getPegawai().getMycode(); dept = sk.getPegawai().getSatuanKerja() != null ? sk.getPegawai().getSatuanKerja().getNama() : "-"; }

            if (keyId.isEmpty()) continue;
            
            // Inisialisasi Pegawai
            if (!mapPegawai.containsKey(keyId)) {
                JSONObject p = new JSONObject();
                p.put("identitas", "ID : " + nip + ", Nama : " + nama + ", Dept : " + dept);
                p.put("nama", nama);
                p.put("log", new JSONObject()); // Penampung Log per Tanggal
                mapPegawai.put(keyId, p);
                
                // Stat Dashboard
                if (nmEntitas.equals("Siswa")) jSis++; else if (nmEntitas.equals("Mahasiswa")) jMhs++;
                else if (nmEntitas.equals("Guru")) jGur++; else if (nmEntitas.equals("Dosen")) jDos++;
                else if (nmEntitas.equals("Pegawai")) jPeg++;
            }
            
            JSONObject pObj = mapPegawai.get(keyId);
            JSONObject logsObj = pObj.getJSONObject("log");
            
            // Pengolahan Raw Log Absensi (Mengumpulkan seluruh jam log dan mengurutkannya)
            TreeSet<String> treeTimes = new TreeSet<>();
            if (sk.getLogAbsensi() != null && !sk.getLogAbsensi().trim().isEmpty()) {
                String[] d = sk.getLogAbsensi().split(";");
                for (String ss : d) {
                    if (!ss.trim().isEmpty() && !ss.startsWith("700")) {
                        try { treeTimes.add(sdfJam.format(Common.dateFormat84.get().parse(ss))); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_orang_horizontal.jsp:172");}
                    }
                }
            }
            if (sk.ambilMasukjam() != null) { try { treeTimes.add(sdfJam.format(sk.ambilMasukjam())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_orang_horizontal.jsp:176");} }
            if (sk.ambilPulangjam() != null) { try { treeTimes.add(sdfJam.format(sk.ambilPulangjam())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_orang_horizontal.jsp:177");} }
            
            // Gabungkan jam dengan tag <br> agar turun ke bawah
            String gabunganJam = "";
            for(String t : treeTimes) { gabunganJam += gabunganJam.isEmpty() ? t : "<br/>" + t; }
            
            logsObj.put(tglStr, gabunganJam);

            // Perhitungan Statistik Dasar
            boolean isTerlambat = Boolean.TRUE.equals(sk.getDatangTerlambat());
            boolean isCepatPulang = Boolean.TRUE.equals(sk.getPulangCepat());
            if (isTerlambat) statTelat++; else if (isCepatPulang) statCepat++;
            else if (sk.getMasukjam() != null) statTepat++; else statLain++;
        }

        // Sort Data Pegawai berdasar nama
        List<JSONObject> sortedList = new ArrayList<>(mapPegawai.values());
        Collections.sort(sortedList, new Comparator<JSONObject>() {
            public int compare(JSONObject o1, JSONObject o2) { return o1.optString("nama").compareToIgnoreCase(o2.optString("nama")); }
        });
        
        JSONArray dataArr = new JSONArray();
        for (JSONObject obj : sortedList) dataArr.put(obj);

        JSONObject result = new JSONObject();
        result.put("status", "00");
        result.put("columns", colArr);
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
        if (sess != null && sess.isOpen()) { try { sess.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_orang_horizontal.jsp:216");} }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>