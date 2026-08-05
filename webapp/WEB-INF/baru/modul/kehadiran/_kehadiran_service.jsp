<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, java.text.SimpleDateFormat, org.hibernate.*, org.hibernate.criterion.*, org.json.*" %>
<%@ page import="ais.common.*, ais.database.hibernate.*" %>
<%@ page import="ais.database.model.*, ais.action.master.helper.*" %>
<%
    out.clear(); 
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    JSONObject jsonResponse = new JSONObject();
    Session sess = null;
    
    try {
        String action = request.getParameter("action");
        sess = HibernateUtil.openSession();
        
        Tbmuser tbmuser = Common.getCurrentUser(request);
        Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa(); 
        
        if (mahasiswa == null) {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", Common.getBahasaConfig("Sesi berakhir, silakan login kembali."));
            out.print(jsonResponse.toString());
            return;
        }

        String periodeId = request.getParameter("filter_id_smt"); 
        Integer semester = null;
        Integer semesterPendek = null;
        
        if (periodeId != null && periodeId.length() >= 5) {
            try {
                String strTahun = periodeId.substring(0, 4);
                String strJenis = periodeId.substring(4, 5);
                Integer tahun = Integer.parseInt(strTahun);
                String semesterMulaiCalc = strJenis.equals("1") ? Perkuliahan.GANJIL : strJenis.equals("2") ? Perkuliahan.GENAP : Perkuliahan.SP;
                Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan() != null ? mahasiswa.getTahunangkatan() : tahun;
                
                semester = Common.getSemester(tahunAngkatanMhs, semesterMulaiCalc, mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
                semesterPendek = strJenis.equals("3") ? Perkuliahan.SEMESTER_PENDEK : null;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kehadiran/_kehadiran_service.jsp:41"); }
        } else {
            semester = mahasiswa.currentSemester();
        }

        // =================================================================================
        // ACTION 1: GET FILTER SMT
        // =================================================================================
        if ("get_filter_smt".equals(action)) {
            JSONArray filterArray = new JSONArray();
            String currentTa = Common.getCurrentTahunAkademik();
            Boolean isGanjil = Common.isNowSemensterGanjil();
            Integer currentTahun = (currentTa != null && !currentTa.isEmpty()) ? Integer.parseInt(currentTa.split("/")[0]) : Calendar.getInstance().get(Calendar.YEAR);
            Integer tahunAngkatan = mahasiswa.getTahunangkatan() != null ? mahasiswa.getTahunangkatan() : currentTahun;

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
                try {
                    String strT = pId.substring(0, 4); String strJ = pId.substring(4, 5); Integer t = Integer.parseInt(strT);
                    String smtMulaiStr = strJ.equals("1") ? Perkuliahan.GANJIL : strJ.equals("2") ? Perkuliahan.GENAP : Perkuliahan.SP;
                    calcSmt = Common.getSemester(mahasiswa.getTahunangkatan(), smtMulaiStr, mahasiswa.getPindahKeKampusIniMasukSemester(), t, mahasiswa.getSemesterMulai());
                    if (strJ.equals("3")) calcSp = 1;
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kehadiran/_kehadiran_service.jsp:72");}

                JSONObject obj = new JSONObject(); 
                obj.put("idSmt", pId); obj.put("label", entry.getValue()); 
                if (calcSmt != null) obj.put("smt", calcSmt);
                if (calcSp != null) obj.put("sp", calcSp);
                filterArray.put(obj);
            }
            String defaultIdSmt = currentTahun + (isGanjil != null && isGanjil ? "1" : "2");
            jsonResponse.put("status", "success");
            jsonResponse.put("data", filterArray);
            jsonResponse.put("default_id", defaultIdSmt);
        }

        // =================================================================================
        // ACTION 2: GET KEHADIRAN (Rekapitulasi Absensi)
        // =================================================================================
        else if ("get_kehadiran".equals(action)) {
            JSONArray dataKehadiran = new JSONArray();

            for (Long oid : mahasiswa.ambilDetailperkuliahan()) {
                Detailperkuliahan dp = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, oid.toString());
                if (dp == null) continue;

                boolean isMatchSmt = false;
                if (periodeId != null && !periodeId.isEmpty()) {
                    if (dp.getSemester() != null && dp.getSemester().equals(semester)) {
                        if (semesterPendek == null) {
                            if (dp.getPerkuliahan() == null || dp.getPerkuliahan().getStatusSemesterPendek() == null) isMatchSmt = true;
                        } else if (dp.getPerkuliahan() != null && semesterPendek.equals(dp.getPerkuliahan().getStatusSemesterPendek())) {
                            isMatchSmt = true;
                        }
                    }
                } else { isMatchSmt = true; }

                if (!isMatchSmt) continue;

                Perkuliahan perkuliahan = dp.getPerkuliahan();
                if (perkuliahan == null) continue; 
                
                Matakuliah matakuliah = perkuliahan.getMatakuliah();
                if (matakuliah == null) continue;

                int hadir = 0, ijin = 0, sakit = 0, alpa = 0;
                
                TreeMap<String, Long> mapPertemuan = perkuliahan.ambilPertemuan(false);
                List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
                if (mapPertemuan != null) {
                    for (Long pid : mapPertemuan.values()) {
                        Pertemuan pt = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pid.toString());
                        if (pt != null) pertemuans.add(pt);
                    }
                }
                
                Object[] jml = perkuliahan.ambilJumlahPertemuanStatistik(pertemuans, mahasiswa, null, true, true);
                
                @SuppressWarnings("unchecked")
                Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml.length <= 4 || jml[4] == null ? null : jml[4]);

                if (statuses != null) {
                    hadir = !statuses.containsKey("M") ? 0 : statuses.get("M");
                    sakit = !statuses.containsKey("S") ? 0 : statuses.get("S");
                    ijin  = !statuses.containsKey("I") ? 0 : statuses.get("I");
                    alpa  = !statuses.containsKey("A") ? 0 : statuses.get("A");
                }

                String mkHtml = "<span class='fw-bold text-dark'>" + matakuliah.getKode() + "</span><br>" + matakuliah.getNama();
                String sksHtml = "<span class='badge bg-light text-dark border'>" + matakuliah.getSks() + " " + Common.getBahasaConfig("SKS") + "</span>";
                String resDosen = PerkuliahanUIHelper.generateTeksDosenPerkuliahan(perkuliahan);
                String resJadwal = PerkuliahanUIHelper.generateHariJamRuanganPerkuliahanUmumText(perkuliahan);
                
                String rekapHtml = "<div class='d-flex justify-content-center gap-1 flex-wrap'>" +
                                   "<span class='badge bg-success' title='" + Common.getBahasaConfig("Hadir") + "'>H: " + hadir + "</span>" +
                                   "<span class='badge bg-info' title='" + Common.getBahasaConfig("Ijin") + "'>I: " + ijin + "</span>" +
                                   "<span class='badge bg-warning text-dark' title='" + Common.getBahasaConfig("Sakit") + "'>S: " + sakit + "</span>" +
                                   "<span class='badge bg-danger' title='" + Common.getBahasaConfig("Alpa") + "'>A: " + alpa + "</span>" +
                                   "</div>";

                JSONObject obj = new JSONObject();
                obj.put("perkuliahan_id", perkuliahan.getId());
                obj.put("matakuliah", mkHtml);
                obj.put("sks", sksHtml);
                obj.put("dosen_jadwal", resDosen + "<br>" + resJadwal);
                obj.put("rekap", rekapHtml);
                dataKehadiran.put(obj);
            }
            
            jsonResponse.put("status", "success");
            jsonResponse.put("data", dataKehadiran); 
        }
        
        // =================================================================================
        // ACTION 3: GET DETAIL KEHADIRAN PER PERTEMUAN (Mengikuti Modifikasi Anda)
        // =================================================================================
        else if ("get_detail_kehadiran".equals(action)) {
            Long perkuliahanId = Long.parseLong(request.getParameter("perkuliahan_id"));
            Perkuliahan p = (Perkuliahan) sess.get(Perkuliahan.class, perkuliahanId);
            
            if (p == null) {
                jsonResponse.put("status", "error"); jsonResponse.put("message", Common.getBahasaConfig("Data perkuliahan tidak ditemukan."));
                out.print(jsonResponse.toString()); return;
            }
            
            TreeMap<String, Long> mapPertemuan = p.ambilPertemuan(false);
            List<Pertemuan> listPertemuan = new ArrayList<Pertemuan>();
            if (mapPertemuan != null) {
                for (Long pid : mapPertemuan.values()) {
                    Pertemuan pt = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pid.toString());
                    if (pt != null) listPertemuan.add(pt);
                }
            }
            
            Collections.sort(listPertemuan, new Comparator<Pertemuan>() {
                public int compare(Pertemuan p1, Pertemuan p2) {
                    Integer pk1 = p1.getPertemuanKe() != null ? p1.getPertemuanKe() : 0;
                    Integer pk2 = p2.getPertemuanKe() != null ? p2.getPertemuanKe() : 0;
                    return pk1.compareTo(pk2);
                }
            });

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
            SimpleDateFormat sdfJam = new SimpleDateFormat("HH:mm");
            JSONArray dataDetail = new JSONArray();

            for (Pertemuan pert : listPertemuan) {
                String statusAbsen = "<span class='text-muted fst-italic'>- " + Common.getBahasaConfig("Kosong") + " -</span>";
                
                String kodeAbsen = pert.retreiveAbsensiKode(mahasiswa.getId());
                
                if (kodeAbsen != null && !kodeAbsen.trim().isEmpty()) {
                    if (kodeAbsen.equalsIgnoreCase("M")) statusAbsen = "<span class='badge bg-success px-3 py-2'><i class='fas fa-check me-1'></i>" + Common.getBahasaConfig("Hadir") + "</span>";
                    else if (kodeAbsen.equalsIgnoreCase("I")) statusAbsen = "<span class='badge bg-info px-3 py-2'><i class='fas fa-envelope me-1'></i>" + Common.getBahasaConfig("Ijin") + "</span>";
                    else if (kodeAbsen.equalsIgnoreCase("S")) statusAbsen = "<span class='badge bg-warning text-dark px-3 py-2'><i class='fas fa-medkit me-1'></i>" + Common.getBahasaConfig("Sakit") + "</span>";
                    else if (kodeAbsen.equalsIgnoreCase("A")) statusAbsen = "<span class='badge bg-danger px-3 py-2'><i class='fas fa-times me-1'></i>" + Common.getBahasaConfig("Alpa") + "</span>";
                }

                // --- MENGADOPSI LOGIKA ANDA ---
                Date wkt = pert.getTanggal();
                String jamMulai = pert.getWaktuMulai() != null ? sdfJam.format(pert.getWaktuMulai()) : "00:00";
                String jamSelesai = pert.getWaktuSelesai() != null ? sdfJam.format(pert.getWaktuSelesai()) : "00:00";
                String waktu = jamMulai + " sd " + jamSelesai;
                
                String ruang = pert.getRuang() != null ? pert.getRuang().getKode() : "-";
                String materi = pert.getTopik();
                String dosen = pert.getDosens();
                // ------------------------------

                String resTanggal = (wkt != null ? sdf.format(wkt) : "-") + "<br><small class='text-muted'>" + waktu + "</small>";

                JSONObject obj = new JSONObject();
                obj.put("pertemuan_ke", "<span class='fw-bold text-dark'>" + (pert.getPertemuanKe() != null ? pert.getPertemuanKe() : "-") + "</span>");
                obj.put("tanggal", resTanggal);
                obj.put("ruang", ruang);
                obj.put("materi", materi != null ? materi : "-");
                obj.put("dosen", dosen != null ? dosen : "-");
                obj.put("kehadiran", statusAbsen);
                dataDetail.put(obj);
            }
            
            Matakuliah mk = p.getMatakuliah();
            jsonResponse.put("status", "success");
            jsonResponse.put("mk_title", mk != null ? mk.getKode() + " - " + mk.getNama() : "");
            jsonResponse.put("data", dataDetail); 
        }

        out.print(jsonResponse.toString());
        out.flush();

    } catch (Exception e) {
        JSONObject err = new JSONObject();
        err.put("status", "error");
        err.put("message", "Internal Error: " + e.getMessage());
        out.print(err.toString());
        out.flush();
    } finally {
        if (sess != null && sess.isOpen()) { try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kehadiran/_kehadiran_service.jsp:247");} }
    }
%>