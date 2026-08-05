<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.*"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.sekolah.*"%>
<%@page import="ais.common.*"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.jsoup.Jsoup"%>
<%@page import="org.zkoss.poi.xssf.usermodel.*"%>
<%@page import="java.util.*"%>
<%@page import="java.io.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
JSONObject resp = new JSONObject();
Session mySession = null;
FileOutputStream fileOut = null;

try {
    String ppuParam = request.getParameter("ppu");
    String action = request.getParameter("action");
    
    mySession = HibernateUtil.openSession();
    PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, Long.parseLong(ppuParam.trim()));
    if (ppu == null) throw new Exception("Data Ujian tidak ditemukan.");
    if (ppu.getUjian() == null) throw new Exception("Data soal ujian belum tersedia untuk sesi ini.");
    if (ppu.getPertemuan() == null || ppu.getPertemuan().getPerkuliahan() == null) throw new Exception("Data perkuliahan untuk sesi ujian ini tidak lengkap.");

    // =========================================================================
    // 1. MAPPING SOAL KE MASING-MASING SUB-CPMK (FormatNilai)
    // =========================================================================
    List<FormatNilai> activeFormatNilais = new ArrayList<FormatNilai>();
    Map<Integer, FormatNilai> soalToCpmk = new HashMap<Integer, FormatNilai>();
    JSONObject mappingObe = new JSONObject(ppu.getFormatNilais() != null ? ppu.getFormatNilais() : "{}");
    
    List<FormatNilai> dbFormatNilais = Common.getFormatNilais(mySession, ppu.getPertemuan().getPerkuliahan());
    for (FormatNilai fn : dbFormatNilais) {
        if (fn.getStatusPertemuan() != null && !mappingObe.isNull(fn.getId().toString())) {
            activeFormatNilais.add(fn);
            String soalNums = mappingObe.getString(fn.getId().toString()); 
            for(String sNum : soalNums.split(",")) {
                try { soalToCpmk.put(Integer.parseInt(sNum.trim()), fn); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_analisis_soal_obe_services.jsp:43");}
            }
        }
    }

    Object[] objects = ppu.getUjian().ambilUjianPunyaSoal(true, ppu, "", 0, 1000);
    List<Long> ujianPunyaSoalsData = (List<Long>) objects[0];

    // Struktur Penyimpanan Berkelompok
    Map<FormatNilai, List<UjianPunyaSoal>> cpmkToUps = new LinkedHashMap<FormatNilai, List<UjianPunyaSoal>>();
    for(FormatNilai fn : activeFormatNilais) cpmkToUps.put(fn, new ArrayList<UjianPunyaSoal>());
    List<UjianPunyaSoal> unassignedUps = new ArrayList<UjianPunyaSoal>();
    
    Map<Long, String> mapKunciJawaban = new HashMap<Long, String>();
    TreeSet<String> masterHurufs = new TreeSet<String>();
    
    int indexSoalManual = 1;
    for(Long upsId : ujianPunyaSoalsData) {
        UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, upsId.toString());
        if(ups != null) {
            int noUrut = ups.getNomorUrut() != null ? ups.getNomorUrut() : indexSoalManual;
            FormatNilai fn = soalToCpmk.get(noUrut);
            if(fn != null && cpmkToUps.containsKey(fn)) cpmkToUps.get(fn).add(ups);
            else unassignedUps.add(ups);

            // Ekstraksi Kunci
            StringBuilder keys = new StringBuilder();
            List<Long> bankSoalDetails = ups.getBankSoal().ambilBankSoalDetail(false);
            for (Long bankSoalDetailid : bankSoalDetails) {
                BankSoalDetail bsd = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
                if (bsd != null && !bsd.getHuruf().isEmpty() && !bsd.getJawaban().trim().isEmpty()) {
                    masterHurufs.add(bsd.getHuruf());
                    if (bsd.getBetul()) {
                        if(keys.length() > 0) keys.append(",");
                        keys.append(bsd.getHuruf());
                    }
                }
            }
            mapKunciJawaban.put(ups.getBankSoal().getId(), keys.toString());
        }
        indexSoalManual++;
    }
    masterHurufs.add("-");

    // =========================================================================
    // 2. AMBIL DATA PESERTA & STATISTIK JAWABAN
    // =========================================================================
    List<HasilUjianMahasiswa> listHum = mySession.createCriteria(HasilUjianMahasiswa.class)
            .add(Restrictions.eq("pertemuanPunyaUjian", ppu))
            .add(Restrictions.isNotNull("keyhasil")).list();

    TreeSet<Double> treeMapRangking = new TreeSet<Double>(Collections.reverseOrder());
    for (HasilUjianMahasiswa hum : listHum) treeMapRangking.add(hum.getNilai());
    int jumlahPeserta = treeMapRangking.size();

    TreeMap<String, Integer> hurufsJawab = new TreeMap<String, Integer>();
    TreeMap<Long, Integer> jumlahBenar = new TreeMap<Long, Integer>();
    TreeMap<Long, Integer> jumlahSalah = new TreeMap<Long, Integer>();
    TreeMap<String, Integer> jumlahPosisi = new TreeMap<String, Integer>();
    double jumlahAtas = 0.0;

    // =========================================================================
    // 3. PEMBUATAN EXCEL (ZKOSS POI)
    // =========================================================================
    if ("excel".equals(action)) {
        String filename = "Analisis_Soal_OBE_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx";
        String dirPath = request.getServletContext().getRealPath("/tmp/");
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dirPath + filename);

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Analisis OBE");
        sheet.setDefaultColumnWidth(15);

        int rowIndex = 0;
        XSSFRow rowhead0 = sheet.createRow(rowIndex);
        XSSFRow rowhead1 = sheet.createRow(rowIndex+1);
        
        rowhead0.createCell(0).setCellValue("No.");
        rowhead0.createCell(1).setCellValue("NIM");
        rowhead0.createCell(2).setCellValue("Nama Peserta");

        int col = 3;
        for (FormatNilai fn : activeFormatNilais) {
            rowhead0.createCell(col).setCellValue(fn.getNama());
            for (UjianPunyaSoal ups : cpmkToUps.get(fn)) {
                rowhead1.createCell(col).setCellValue("S-" + ups.getNomorUrut());
                col++;
            }
            rowhead1.createCell(col).setCellValue("Skor CPMK");
            col++;
        }
        
        if (!unassignedUps.isEmpty()) {
            rowhead0.createCell(col).setCellValue("Tanpa Sub-CPMK");
            for (UjianPunyaSoal ups : unassignedUps) {
                rowhead1.createCell(col).setCellValue("S-" + ups.getNomorUrut());
                col++;
            }
        }
        
        rowhead0.createCell(col).setCellValue("Nilai Akhir");

        rowIndex = 2;
        for (HasilUjianMahasiswa hum : listHum) {
            int rangking = 0;
            for (Double nilai : treeMapRangking) {
                rangking++;
                if (Common.numberFormat.get().format(nilai).equals(Common.numberFormat.get().format(hum.getNilai()))) break;
            }
            String posisi = rangking <= (jumlahPeserta / 2) ? "Atas" : rangking > ((jumlahPeserta + 1) / 2) ? "Bawah" : "Tengah";
            if (posisi.equalsIgnoreCase("Atas")) jumlahAtas += 1.0;

            JSONObject humObe = new JSONObject(hum.getNilaiObe() != null ? hum.getNilaiObe() : "{}");
            ais.ui.util.MyArrayList<Long> ujianPunyaSoals = hum.ambilUjianPunyaSoals(ppu.getJmlDitampilkan(), null, true);
            Map<Long, Set<Long>> humDetails = hum.ambilHasilUjianMahasiswaDetail(ppu.getJmlDitampilkan(), ujianPunyaSoals, false);

            XSSFRow row = sheet.createRow(rowIndex);
            row.createCell(0).setCellValue(rowIndex - 1);
            
            if (hum.getMahasiswa() != null) {
                row.createCell(1).setCellValue(hum.getMahasiswa().getNim());
                row.createCell(2).setCellValue(hum.getMahasiswa().getNama());
            } else if (hum.getBiodataCalonMahasiswa() != null) {
                row.createCell(1).setCellValue(hum.getBiodataCalonMahasiswa().getNoRegistrasi());
                row.createCell(2).setCellValue(hum.getBiodataCalonMahasiswa().getNama());
            }

            col = 3;
            for (FormatNilai fn : activeFormatNilais) {
                for (UjianPunyaSoal ups : cpmkToUps.get(fn)) {
                    String huruf = processJawabanPeserta(ups, humDetails, posisi, hurufsJawab, jumlahBenar, jumlahSalah, jumlahPosisi);
                    row.createCell(col).setCellValue(huruf);
                    col++;
                }
                Double score = humObe.isNull(fn.getId().toString()) ? 0.0 : humObe.getDouble(fn.getId().toString());
                Double max = humObe.isNull(fn.getId().toString()+"_max") ? 0.0 : humObe.getDouble(fn.getId().toString()+"_max");
                String scoreStr = max > 0 ? Common.numberFormat.get().format((score * 100.0) / max) : "0";
                row.createCell(col).setCellValue(scoreStr);
                col++;
            }

            for (UjianPunyaSoal ups : unassignedUps) {
                String huruf = processJawabanPeserta(ups, humDetails, posisi, hurufsJawab, jumlahBenar, jumlahSalah, jumlahPosisi);
                row.createCell(col).setCellValue(huruf);
                col++;
            }

            row.createCell(col).setCellValue(hum.getNilai());
            rowIndex++;
            if(rowIndex % 50 == 0) mySession.clear();
        }

        // --- FOOTER EXCEL ---
        XSSFRow rowKunci = sheet.createRow(rowIndex+1);
        rowKunci.createCell(1).setCellValue("**Kunci Jawaban");
        
        int fCol = 3;
        for (FormatNilai fn : activeFormatNilais) {
            for (UjianPunyaSoal ups : cpmkToUps.get(fn)) {
                rowKunci.createCell(fCol++).setCellValue("**" + mapKunciJawaban.get(ups.getBankSoal().getId()));
            }
            fCol++; // Lompati kolom nilai CPMK
        }

        int pIdx = 2;
        for (String huruf : masterHurufs) {
            XSSFRow rOps = sheet.createRow(rowIndex + pIdx);
            rOps.createCell(1).setCellValue(huruf.equals("-") ? "**Tidak Dijawab" : "**Jawaban " + huruf);
            fCol = 3;
            for (FormatNilai fn : activeFormatNilais) {
                for (UjianPunyaSoal ups : cpmkToUps.get(fn)) rOps.createCell(fCol++).setCellValue("**" + hurufsJawab.getOrDefault(ups.getBankSoal().getId() + "_" + huruf, 0));
                fCol++;
            }
            pIdx++;
        }

        XSSFRow rBenar = sheet.createRow(rowIndex + pIdx++);
        rBenar.createCell(1).setCellValue("**Jawaban Benar");
        fCol = 3;
        for (FormatNilai fn : activeFormatNilais) {
            for (UjianPunyaSoal ups : cpmkToUps.get(fn)) rBenar.createCell(fCol++).setCellValue("**" + jumlahBenar.getOrDefault(ups.getBankSoal().getId(), 0));
            fCol++;
        }

        XSSFRow rSalah = sheet.createRow(rowIndex + pIdx++);
        rSalah.createCell(1).setCellValue("**Jawaban Salah");
        fCol = 3;
        for (FormatNilai fn : activeFormatNilais) {
            for (UjianPunyaSoal ups : cpmkToUps.get(fn)) rSalah.createCell(fCol++).setCellValue("**" + jumlahSalah.getOrDefault(ups.getBankSoal().getId(), 0));
            fCol++;
        }

        XSSFRow rDaya = sheet.createRow(rowIndex + pIdx++);
        rDaya.createCell(1).setCellValue("**Daya Pembeda");
        XSSFRow rKrit = sheet.createRow(rowIndex + pIdx++);
        rKrit.createCell(1).setCellValue("**Kriteria");
        fCol = 3;
        for (FormatNilai fn : activeFormatNilais) {
            for (UjianPunyaSoal ups : cpmkToUps.get(fn)) {
                Long id = ups.getBankSoal().getId();
                int jA = jumlahPosisi.getOrDefault(id + "_Atas", 0);
                int jB = jumlahPosisi.getOrDefault(id + "_Bawah", 0);
                int br = jumlahBenar.getOrDefault(id, 0);
                int sl = jumlahSalah.getOrDefault(id, 0);

                double jml = jA > 0 ? (jA - (double)jB) / jA : 0;
                rDaya.createCell(fCol).setCellValue("**" + Common.numberFormat.get().format(jml));

                String ni = (br == 0 && sl == 0) ? "Blm dikerjakan" : (jml < 0.05 ? "Ganti" : (jml < 0.1 ? "Revisi" : "Gunakan"));
                rKrit.createCell(fCol).setCellValue("**" + ni);
                fCol++;
            }
            fCol++;
        }

        try {
            fileOut = new FileOutputStream(file);
            workbook.write(fileOut);
        } finally {
            if(fileOut != null) fileOut.close();
        }

        resp.put("status", "success");
        resp.put("url", Common.ROOT + "/tmp/" + filename);

    } else {
        // =========================================================================
        // 4. PEMBUATAN HTML TABLE UNTUK TAMPILAN POPUP (VIEW)
        // =========================================================================
        StringBuilder html = new StringBuilder();
        html.append("<div class='table-responsive border shadow-sm'>");
        html.append("<table class='table table-bordered table-hover text-center align-middle mb-0' style='white-space: nowrap; font-size: 12px;'>");
        
        // Header Baris 1: Pengelompokan Sub-CPMK
        html.append("<thead class='table-dark'><tr>");
        html.append("<th rowspan='2' width='3%'>No</th><th rowspan='2'>NIM/No.Reg</th><th rowspan='2' class='text-start'>Nama Peserta</th>");
        
        for(FormatNilai fn : activeFormatNilais) {
            int span = cpmkToUps.get(fn).size() + 1; // +1 untuk skor
            html.append("<th colspan='").append(span).append("' class='border-end border-light'>").append(fn.getNama()).append("</th>");
        }
        if(!unassignedUps.isEmpty()) {
            html.append("<th colspan='").append(unassignedUps.size()).append("' class='bg-secondary'>Tanpa CPMK</th>");
        }
        html.append("<th rowspan='2' class='bg-primary bg-opacity-75'>Nilai Akhir</th>");
        html.append("</tr>");

        // Header Baris 2: Nomor Soal
        html.append("<tr>");
        for(FormatNilai fn : activeFormatNilais) {
            for(UjianPunyaSoal ups : cpmkToUps.get(fn)) {
                html.append("<th class='table-secondary text-dark' title='Soal ID: "+ups.getId()+"'>S-").append(ups.getNomorUrut()).append("</th>");
            }
            html.append("<th class='bg-primary bg-opacity-25 border-end border-dark text-dark'>Skor</th>");
        }
        for(UjianPunyaSoal ups : unassignedUps) {
            html.append("<th class='table-secondary text-dark'>S-").append(ups.getNomorUrut()).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        // LOOP PESERTA HTML
        int rIndex = 1;
        for (HasilUjianMahasiswa hum : listHum) {
            int rangking = 0;
            for (Double nilai : treeMapRangking) {
                rangking++;
                if (Common.numberFormat.get().format(nilai).equals(Common.numberFormat.get().format(hum.getNilai()))) break;
            }
            String posisi = rangking <= (jumlahPeserta / 2) ? "Atas" : rangking > ((jumlahPeserta + 1) / 2) ? "Bawah" : "Tengah";
            if (posisi.equalsIgnoreCase("Atas")) jumlahAtas += 1.0;

            JSONObject humObe = new JSONObject(hum.getNilaiObe() != null ? hum.getNilaiObe() : "{}");
            ais.ui.util.MyArrayList<Long> ujianPunyaSoals = hum.ambilUjianPunyaSoals(ppu.getJmlDitampilkan(), null, true);
            Map<Long, Set<Long>> humDetails = hum.ambilHasilUjianMahasiswaDetail(ppu.getJmlDitampilkan(), ujianPunyaSoals, false);

            html.append("<tr><td class='text-muted fw-bold'>").append(rIndex).append("</td>");
            if (hum.getMahasiswa() != null) {
                html.append("<td>").append(hum.getMahasiswa().getNim()).append("</td><td class='text-start fw-bold'>").append(hum.getMahasiswa().getNama()).append("</td>");
            } else if (hum.getBiodataCalonMahasiswa() != null) {
                html.append("<td>").append(hum.getBiodataCalonMahasiswa().getNoRegistrasi()).append("</td><td class='text-start fw-bold'>").append(hum.getBiodataCalonMahasiswa().getNama()).append("</td>");
            }

            for(FormatNilai fn : activeFormatNilais) {
                for(UjianPunyaSoal ups : cpmkToUps.get(fn)) {
                    String huruf = processJawabanPeserta(ups, humDetails, posisi, hurufsJawab, jumlahBenar, jumlahSalah, jumlahPosisi);
                    html.append("<td class='"+(huruf.equals("-")?"text-muted":"fw-bold")+"'>").append(huruf).append("</td>");
                }
                Double score = humObe.isNull(fn.getId().toString()) ? 0.0 : humObe.getDouble(fn.getId().toString());
                Double max = humObe.isNull(fn.getId().toString()+"_max") ? 0.0 : humObe.getDouble(fn.getId().toString()+"_max");
                String scoreStr = max > 0 ? Common.numberFormat.get().format((score * 100.0) / max) : "0";
                html.append("<td class='bg-primary bg-opacity-10 fw-bold text-primary border-end border-secondary'>").append(scoreStr).append("</td>");
            }

            for(UjianPunyaSoal ups : unassignedUps) {
                String huruf = processJawabanPeserta(ups, humDetails, posisi, hurufsJawab, jumlahBenar, jumlahSalah, jumlahPosisi);
                html.append("<td class='"+(huruf.equals("-")?"text-muted":"fw-bold")+"'>").append(huruf).append("</td>");
            }

            html.append("<td class='bg-success bg-opacity-25 fw-bold text-success fs-6'>").append(Common.numberFormat.get().format(hum.getNilai())).append("</td>");
            html.append("</tr>");

            rIndex++;
            if(rIndex % 50 == 0) mySession.clear();
        }
        
        html.append("</tbody><tfoot class='table-light border-top-2 fw-bold text-muted'>");
        
        // --- FOOTER KUNCI JAWABAN HTML ---
        html.append("<tr><td colspan='3' class='text-end'>Kunci Jawaban</td>");
        for(FormatNilai fn : activeFormatNilais) {
            for(UjianPunyaSoal ups : cpmkToUps.get(fn)) { html.append("<td class='text-primary'>").append(mapKunciJawaban.get(ups.getBankSoal().getId())).append("</td>"); }
            html.append("<td class='bg-light border-end'></td>");
        }
        for(UjianPunyaSoal ups : unassignedUps) { html.append("<td class='text-primary'>").append(mapKunciJawaban.get(ups.getBankSoal().getId())).append("</td>"); }
        html.append("<td></td></tr>");

        // --- FOOTER OPSI JAWABAN HTML ---
        for(String huruf : masterHurufs) {
            html.append("<tr><td colspan='3' class='text-end'>").append(huruf.equals("-") ? "Tidak Dijawab" : "Menjawab "+huruf).append("</td>");
            for(FormatNilai fn : activeFormatNilais) {
                for(UjianPunyaSoal ups : cpmkToUps.get(fn)) { html.append("<td>").append(hurufsJawab.getOrDefault(ups.getBankSoal().getId() + "_" + huruf, 0)).append("</td>"); }
                html.append("<td class='bg-light border-end'></td>");
            }
            for(UjianPunyaSoal ups : unassignedUps) { html.append("<td>").append(hurufsJawab.getOrDefault(ups.getBankSoal().getId() + "_" + huruf, 0)).append("</td>"); }
            html.append("<td></td></tr>");
        }

        // --- FOOTER BENAR & SALAH HTML ---
        html.append("<tr><td colspan='3' class='text-end text-success'>Jawaban Benar</td>");
        for(FormatNilai fn : activeFormatNilais) {
            for(UjianPunyaSoal ups : cpmkToUps.get(fn)) { html.append("<td class='text-success'>").append(jumlahBenar.getOrDefault(ups.getBankSoal().getId(), 0)).append("</td>"); }
            html.append("<td class='bg-light border-end'></td>");
        }
        for(UjianPunyaSoal ups : unassignedUps) { html.append("<td class='text-success'>").append(jumlahBenar.getOrDefault(ups.getBankSoal().getId(), 0)).append("</td>"); }
        html.append("<td></td></tr>");

        html.append("<tr><td colspan='3' class='text-end text-danger'>Jawaban Salah</td>");
        for(FormatNilai fn : activeFormatNilais) {
            for(UjianPunyaSoal ups : cpmkToUps.get(fn)) { html.append("<td class='text-danger'>").append(jumlahSalah.getOrDefault(ups.getBankSoal().getId(), 0)).append("</td>"); }
            html.append("<td class='bg-light border-end'></td>");
        }
        for(UjianPunyaSoal ups : unassignedUps) { html.append("<td class='text-danger'>").append(jumlahSalah.getOrDefault(ups.getBankSoal().getId(), 0)).append("</td>"); }
        html.append("<td></td></tr>");

        // --- FOOTER DAYA PEMBEDA & KRITERIA HTML ---
        html.append("<tr><td colspan='3' class='text-end text-primary'>Daya Pembeda</td>");
        StringBuilder kritHtml = new StringBuilder("<tr><td colspan='3' class='text-end text-dark'>Kriteria Penggunaan</td>");
        
        for(FormatNilai fn : activeFormatNilais) {
            for(UjianPunyaSoal ups : cpmkToUps.get(fn)) {
                Long id = ups.getBankSoal().getId();
                int jA = jumlahPosisi.getOrDefault(id + "_Atas", 0);
                int jB = jumlahPosisi.getOrDefault(id + "_Bawah", 0);
                int br = jumlahBenar.getOrDefault(id, 0);
                int sl = jumlahSalah.getOrDefault(id, 0);

                double jml = jA > 0 ? (jA - (double)jB) / jA : 0;
                html.append("<td class='text-primary'>").append(Common.numberFormat.get().format(jml)).append("</td>");

                String ni = (br == 0 && sl == 0) ? "Kosong" : (jml < 0.05 ? "Ganti" : (jml < 0.1 ? "Revisi" : "Gunakan"));
                String bgKrit = ni.equals("Ganti") ? "bg-danger bg-opacity-25 text-danger" : (ni.equals("Revisi") ? "bg-warning bg-opacity-25 text-dark" : (ni.equals("Gunakan") ? "bg-success bg-opacity-25 text-success" : "text-muted"));
                kritHtml.append("<td class='").append(bgKrit).append("'>").append(ni).append("</td>");
            }
            html.append("<td class='bg-light border-end'></td>");
            kritHtml.append("<td class='bg-light border-end'></td>");
        }
        
        html.append("<td></td></tr>");
        kritHtml.append("<td></td></tr>");
        html.append(kritHtml.toString());

        html.append("</tfoot></table></div>");

        resp.put("status", "success");
        resp.put("html", html.toString());
    }

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_analisis_soal_obe_services.jsp:423");
    resp.put("status", "error");
    resp.put("message", e.getMessage());
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(resp.toString());
out.flush();
%>

<%!
// Fungsi Rekursif/Utility untuk Memproses Jawaban Peserta
private String processJawabanPeserta(UjianPunyaSoal ups, Map<Long, Set<Long>> humDetails, String posisi, 
                                     TreeMap<String, Integer> hurufsJawab, TreeMap<Long, Integer> jumlahBenar, 
                                     TreeMap<Long, Integer> jumlahSalah, TreeMap<String, Integer> jumlahPosisi) {
    Long bankSoalId = ups.getBankSoal().getId();
    List<Long> jawaban = new ArrayList<Long>();
    for (Set<Long> aa : humDetails.values()) {
        for (Long hId : aa) {
            HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) ais.database.model.GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, hId.toString());
            if (humd != null && humd.getBankSoal() != null && humd.getBankSoal().getId().equals(bankSoalId)) {
                jawaban.add(hId);
            }
        }
    }

    if (jawaban.isEmpty()) {
        String key = bankSoalId + "_-";
        hurufsJawab.put(key, hurufsJawab.getOrDefault(key, 0) + 1);
        return "-";
    } else {
        String huruf = "";
        for (Long idData : jawaban) {
            HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) ais.database.model.GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, idData.toString());
            if (humd != null && humd.getBankSoalDetail() != null) {
                huruf += huruf.isEmpty() ? humd.getBankSoalDetail().getHuruf() : "," + humd.getBankSoalDetail().getHuruf();
                String key = bankSoalId + "_" + humd.getBankSoalDetail().getHuruf();
                hurufsJawab.put(key, hurufsJawab.getOrDefault(key, 0) + 1);

                if (humd.getBankSoalDetail().getBetul()) {
                    jumlahBenar.put(bankSoalId, jumlahBenar.getOrDefault(bankSoalId, 0) + 1);
                    jumlahPosisi.put(bankSoalId + "_" + posisi, jumlahPosisi.getOrDefault(bankSoalId + "_" + posisi, 0) + 1);
                } else {
                    jumlahSalah.put(bankSoalId, jumlahSalah.getOrDefault(bankSoalId, 0) + 1);
                }
            }
        }
        return huruf;
    }
}
%>