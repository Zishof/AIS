<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, org.hibernate.*, org.hibernate.criterion.*, org.json.*" %>
<%@ page import="ais.common.*, ais.database.hibernate.*" %>
<%@ page import="ais.database.model.*" %>
<%
    // Membersihkan buffer untuk memastikan output JSON bersih
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
        
        // Logika Identifikasi Semester dari PeriodeId (misal: 20261)
        if (periodeId != null && periodeId.length() >= 5) {
            try {
                String strTahun = periodeId.substring(0, 4);
                String strJenis = periodeId.substring(4, 5);
                Integer tahun = Integer.parseInt(strTahun);
                String semesterMulaiCalc = strJenis.equals("1") ? Perkuliahan.GANJIL : strJenis.equals("2") ? Perkuliahan.GENAP : Perkuliahan.SP;
                Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan() != null ? mahasiswa.getTahunangkatan() : tahun;
                
                semester = Common.getSemester(tahunAngkatanMhs, semesterMulaiCalc, mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
                semesterPendek = strJenis.equals("3") ? Perkuliahan.SEMESTER_PENDEK : null;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/penilaian/_penilaian_service.jsp:43"); }
        } else {
            semester = mahasiswa.currentSemester();
        }

        // --- ACTION: GET FILTER SMT ---
        if ("get_filter_smt".equals(action)) {
            JSONArray filterArray = new JSONArray();
            String currentTa = Common.getCurrentTahunAkademik();
            Boolean isGanjil = Common.isNowSemensterGanjil();
            Integer currentTahun = (currentTa != null && !currentTa.isEmpty()) ? Integer.parseInt(currentTa.split("/")[0]) : Calendar.getInstance().get(Calendar.YEAR);
            String defaultIdSmt = currentTahun + (isGanjil != null && isGanjil ? "1" : "2");
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
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/penilaian/_penilaian_service.jsp:73");}

                JSONObject obj = new JSONObject(); 
                obj.put("idSmt", pId); obj.put("label", entry.getValue()); 
                if (calcSmt != null) obj.put("smt", calcSmt);
                if (calcSp != null) obj.put("sp", calcSp);
                filterArray.put(obj);
            }
            jsonResponse.put("status", "success");
            jsonResponse.put("data", filterArray);
            jsonResponse.put("default_id", defaultIdSmt);
        }

        // --- ACTION: GET NILAI ---
        else if ("get_nilai".equals(action)) {
            JSONArray dataSudah = new JSONArray();
            JSONArray dataBelum = new JSONArray();

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
                Matakuliah matakuliah = perkuliahan != null ? perkuliahan.getMatakuliah() : dp.getMatakuliahKonversi();
                if (matakuliah == null) continue;

                Double totalNilai = dp.getTotalNilai() != null ? dp.getTotalNilai() : 0.0;
                String nilaiHuruf = dp.getNilaiHuruf() != null ? dp.getNilaiHuruf().trim() : "";
                
                if (totalNilai < 0.01 && dp.getTotalNilaiSementara() != null && dp.getTotalNilaiSementara() > 0.1) {
                    totalNilai = dp.getTotalNilaiSementara();
                    nilaiHuruf = dp.getNilaiHurufSementara() != null ? dp.getNilaiHurufSementara().trim() : "";
                }

                JSONObject obj = new JSONObject();
                obj.put("matakuliah", "<span class='fw-bold text-dark'>" + matakuliah.getKode() + "</span><br>" + matakuliah.getNama());
                obj.put("sks", "<span class='badge bg-light text-dark border'>" + matakuliah.getSks() + " " + Common.getBahasaConfig("SKS") + "</span>");
                obj.put("semester", dp.getSemester() != null ? dp.getSemester().toString() : "-");

                if (totalNilai > 0.1 || (!nilaiHuruf.isEmpty() && !nilaiHuruf.equalsIgnoreCase("E") && !nilaiHuruf.equalsIgnoreCase("T"))) {
                    obj.put("nilai_angka", "<span class='fw-bold text-primary'>" + Common.formatNumber(totalNilai, 2) + "</span>");
                    obj.put("nilai_huruf", "<span class='badge bg-success fs-6 px-3'>" + nilaiHuruf + "</span>");
                    dataSudah.put(obj);
                } else {
                    String statusAcc = (dp.getPersetujuan() != null && dp.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) 
                                       ? "<span class='badge bg-info'>" + Common.getBahasaConfig("KRS Disetujui") + "</span>"
                                       : "<span class='badge bg-warning text-dark'>" + Common.getBahasaConfig("KRS Belum Disetujui") + "</span>";
                    obj.put("status_krs", statusAcc);
                    obj.put("keterangan", "<span class='text-danger small'><i class='fas fa-clock me-1'></i>" + Common.getBahasaConfig("Dosen belum memublikasikan nilai") + "</span>");
                    dataBelum.put(obj);
                }
            }
            jsonResponse.put("status", "success");
            jsonResponse.put("data_sudah", dataSudah); 
            jsonResponse.put("data_belum", dataBelum); 
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
        if (sess != null && sess.isOpen()) { try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/penilaian/_penilaian_service.jsp:153");} }
    }
%>