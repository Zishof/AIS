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
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_orang.jsp:24");}
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
        for (String h : hariAktifArr) { try { activeDays.add(Integer.parseInt(h)); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_orang.jsp:41");} }
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

        // Ambil Data dari DB
        List<StatuskehadiranKaryawanHarian> list = crit.list();
        
        // 1. Kelompokkan Data per Individu per Tanggal
        Map<String, Map<String, StatuskehadiranKaryawanHarian>> mapPersonData = new LinkedHashMap<>();
        Map<String, String> mapPersonNames = new HashMap<>(); // Untuk menyimpan identitas judul tabel
        
        for (StatuskehadiranKaryawanHarian sk : list) {
            if (sk.getTanggal() == null) continue;
            
            String keyId = ""; String nama = "-";
            if (sk.getSiswa() != null) { keyId = "S_" + sk.getSiswa().getId(); nama = sk.getSiswa().getNamaSiswa(); }
            else if (sk.getMahasiswa() != null) { keyId = "M_" + sk.getMahasiswa().getId(); nama = sk.getMahasiswa().getNama(); }
            else if (sk.getGuru() != null) { keyId = "G_" + sk.getGuru().getId(); nama = sk.getGuru().getNama(); }
            else if (sk.getDosen() != null) { keyId = "D_" + sk.getDosen().getId(); nama = sk.getDosen().getNama(); }
            else if (sk.getPegawai() != null) { keyId = "P_" + sk.getPegawai().getId(); nama = sk.getPegawai().getNama(); }

            if (keyId.isEmpty()) continue;
            
            if (!mapPersonData.containsKey(keyId)) {
                mapPersonData.put(keyId, new HashMap<String, StatuskehadiranKaryawanHarian>());
                mapPersonNames.put(keyId, nama);
            }
            
            String tglKey = Common.dateFormat83.get().format(sk.getTanggal());
            mapPersonData.get(keyId).put(tglKey, sk);
        }

        // 2. Loop Kalender untuk setiap Individu guna mengisi Matriks Harian
        JSONArray dataArr = new JSONArray();
        Date hariIni = ais.ui.util.WaktuUtil.getDate();
        
        for (Map.Entry<String, Map<String, StatuskehadiranKaryawanHarian>> entry : mapPersonData.entrySet()) {
            String pKey = entry.getKey();
            String pName = mapPersonNames.get(pKey);
            Map<String, StatuskehadiranKaryawanHarian> logHarian = entry.getValue();
            
            JSONObject personObj = new JSONObject();
            personObj.put("nama", pName);
            JSONArray rincianArr = new JSONArray();
            
            Calendar cMulai = Calendar.getInstance(); cMulai.setTime(dateStart);
            Calendar cSampai = Calendar.getInstance(); cSampai.setTime(dateEnd);
            
            while (!cMulai.getTime().after(cSampai.getTime())) {
                Date currDate = cMulai.getTime();
                int dayOfWeek = cMulai.get(Calendar.DAY_OF_WEEK);
                
                // Lewati jika hari tidak dicentang di saringan
                if (!activeDays.isEmpty() && !activeDays.contains(dayOfWeek)) {
                    cMulai.add(Calendar.DATE, 1); continue;
                }
                
                String tglKey = Common.dateFormat83.get().format(currDate);
                StatuskehadiranKaryawanHarian sk = logHarian.get(tglKey);
                
                JSONObject detail = new JSONObject();
                detail.put("hari", Common.dateFormat6.get().format(currDate)); // cth: Senin, 16 Maret 2026
                
                if (sk != null) {
                    // Ada Data Presensi
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
                    // Kosong / Mangkir / Belum Absen
                    detail.put("jamKerja", ""); detail.put("jamMasuk", ""); detail.put("jamKeluar", "");
                    detail.put("terlambat", 0.0); detail.put("cepatPulang", 0.0); detail.put("lembur", 0.0);
                    
                    if (currDate.before(hariIni)) detail.put("catatan", Common.getBahasaConfig("Alpha / Tanpa Keterangan"));
                    else detail.put("catatan", "-");
                }
                
                rincianArr.put(detail);
                cMulai.add(Calendar.DATE, 1); // Lanjut hari berikutnya
            }
            
            // Jika individu ini memiliki log setelah difilter hari aktif
            if (rincianArr.length() > 0) {
                personObj.put("rincian", rincianArr);
                dataArr.put(personObj);
            }
        }

        JSONObject result = new JSONObject();
        result.put("status", "00");
        result.put("data", dataArr);
        out.print(result.toString());

    } catch (Exception e) { 
        out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}"); 
    } finally { 
        if (sess != null && sess.isOpen()) { try { sess.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_absensi_pegawai_per_orang.jsp:201");} }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>