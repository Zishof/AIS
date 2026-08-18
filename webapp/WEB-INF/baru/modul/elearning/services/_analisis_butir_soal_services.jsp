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

    // =========================================================================
    // 1. PENGUMPULAN DATA (SOAL, PESERTA, & KALKULASI AWAL)
    // =========================================================================
    Object[] objects = ppu.getUjian().ambilUjianPunyaSoal(true, ppu, "", 0, 1000);
    List<Long> ujianPunyaSoalsData = (List<Long>) objects[0];
    
    List<HasilUjianMahasiswa> listHum = mySession.createCriteria(HasilUjianMahasiswa.class)
            .add(Restrictions.eq("pertemuanPunyaUjian", ppu))
            .add(Restrictions.isNotNull("keyhasil")).list();

    TreeSet<Double> treeMapRangking = new TreeSet<Double>(Collections.reverseOrder());
    for (HasilUjianMahasiswa hum : listHum) {
        treeMapRangking.add(hum.getNilai());
    }
    int jumlahPeserta = treeMapRangking.size();

    TreeMap<String, Integer> hurufsJawab = new TreeMap<String, Integer>();
    TreeMap<Long, Integer> jumlahBenar = new TreeMap<Long, Integer>();
    TreeMap<Long, Integer> jumlahSalah = new TreeMap<Long, Integer>();
    TreeMap<String, Integer> jumlahPosisi = new TreeMap<String, Integer>();
    double jumlahAtas = 0.0;
    
    TreeSet<String> masterHurufs = new TreeSet<String>();
    
    // Siapkan Metadata Soal & Kunci Jawaban
    Map<Long, String> mapKunciJawaban = new HashMap<Long, String>();
    Map<Long, String> mapSoalContent = new HashMap<Long, String>();
    
    for (Long d : ujianPunyaSoalsData) {
        UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
        if (ups != null) {
            String content = ups.getBankSoal().getSoal();
            try { content = Jsoup.parse(content).text(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_analisis_butir_soal_services.jsp:62");}
            String displayContent = ppu.getRandom() ? (content.length() > 10 ? content.substring(0, 9) + ".." : content) : ups.getNomorUrut() + "";
            mapSoalContent.put(ups.getBankSoal().getId(), displayContent);

            List<Long> bankSoalDetails = ups.getBankSoal().ambilBankSoalDetail(false);
            StringBuilder keys = new StringBuilder();
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
    }
    masterHurufs.add("-");

    // =========================================================================
    // 1b. BUILD bankSoalId -> Sub-CPMK NAME MAP FROM ppu.getFormatNilais()
    // =========================================================================
    Map<Long, String> bankSoalToSubCpmk = new HashMap<Long, String>();
    try {
        Map<Integer, Long> nomorToSoalId = new HashMap<Integer, Long>();
        int soalIdx = 1;
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) {
                int noUrut = ups.getNomorUrut() != null ? ups.getNomorUrut().intValue() : soalIdx;
                nomorToSoalId.put(noUrut, ups.getBankSoal().getId());
            }
            soalIdx++;
        }
        JSONObject fnJson = new JSONObject(ppu.getFormatNilais() != null ? ppu.getFormatNilais() : "{}");
        java.util.Iterator<String> fnIter = fnJson.keys();
        while (fnIter.hasNext()) {
            String fnIdStr = fnIter.next();
            String nomorStr = fnJson.optString(fnIdStr, "");
            String subCpmkName = fnIdStr;
            try {
                Long fnId = Long.parseLong(fnIdStr);
                FormatNilai fnObj = (FormatNilai) mySession.get(FormatNilai.class, fnId);
                if (fnObj != null && fnObj.getNama() != null) subCpmkName = fnObj.getNama();
            } catch (Exception exx) { /* ignore single-key lookup failure */ }
            for (String nomor : nomorStr.split(",")) {
                try {
                    Long bsId = nomorToSoalId.get(Integer.parseInt(nomor.trim()));
                    if (bsId != null) bankSoalToSubCpmk.put(bsId, subCpmkName);
                } catch (NumberFormatException nfe) { /* skip non-numeric nomor */ }
            }
        }
    } catch (Exception exx) { /* ignore malformed formatNilais */ }

    // =========================================================================
    // 2. PEMROSESAN EXCEL MENGGUNAKAN ZKOSS POI
    // =========================================================================
    if ("excel".equals(action)) {
        String filename = "Analisis_Soal_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx";
        String dirPath = request.getServletContext().getRealPath("/tmp/");
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dirPath + filename);

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Analisis Butir Soal");
        sheet.setDefaultColumnWidth(20);

        int rowIndex = 0;
        XSSFRow rowhead = sheet.createRow(rowIndex);
        rowhead.createCell(0).setCellValue("No.");
        rowhead.createCell(1).setCellValue("Kode");
        rowhead.createCell(2).setCellValue("Nama");
        rowhead.createCell(3).setCellValue("Kelas/Prodi");
        
        int col = 4;
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) {
                rowhead.createCell(col).setCellValue(mapSoalContent.get(ups.getBankSoal().getId()));
                col++;
            }
        }
        rowhead.createCell(col).setCellValue("Benar");
        rowhead.createCell(col+1).setCellValue("Salah");
        rowhead.createCell(col+2).setCellValue("Skor");
        rowhead.createCell(col+3).setCellValue("Rangking");
        rowhead.createCell(col+4).setCellValue("Kelompok Rangking");

        // Loop Peserta
        rowIndex = 1;
        for (HasilUjianMahasiswa hum : listHum) {
            int rangking = 0;
            for (Double nilai : treeMapRangking) {
                rangking++;
                if (Common.numberFormat.get().format(nilai).equals(Common.numberFormat.get().format(hum.getNilai()))) break;
            }
            String posisi = rangking <= (jumlahPeserta / 2) ? "Atas" : rangking > ((jumlahPeserta + 1) / 2) ? "Bawah" : "Tengah";
            if (posisi.equalsIgnoreCase("Atas")) jumlahAtas += 1.0;

            ais.ui.util.MyArrayList<Long> ujianPunyaSoals = hum.ambilUjianPunyaSoals(ppu.getJmlDitampilkan(), null, true);
            Map<Long, Set<Long>> humDetails = hum.ambilHasilUjianMahasiswaDetail(ppu.getJmlDitampilkan(), ujianPunyaSoals, false);

            XSSFRow row = sheet.createRow(rowIndex);
            row.createCell(0).setCellValue(rowIndex);
            
            if (hum.getMahasiswa() != null) {
                row.createCell(1).setCellValue(hum.getMahasiswa().getNim());
                row.createCell(2).setCellValue(hum.getMahasiswa().getNama());
                row.createCell(3).setCellValue(hum.getMahasiswa().getJurusan().getNama());
            } else if (hum.getBiodataCalonMahasiswa() != null) {
                row.createCell(1).setCellValue(hum.getBiodataCalonMahasiswa().getNoRegistrasi());
                row.createCell(2).setCellValue(hum.getBiodataCalonMahasiswa().getNama());
                Jurusan jur = hum.getBiodataCalonMahasiswa().getProdiLulus() != null ? hum.getBiodataCalonMahasiswa().getProdiLulus() : hum.getBiodataCalonMahasiswa().getProdi1();
                row.createCell(3).setCellValue(jur == null ? "" : jur.getNama());
            }

            col = 4;
            for (Long d : ujianPunyaSoalsData) {
                UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
                if (ups != null) {
                    Long bankSoalId = ups.getBankSoal().getId();
                    List<Long> jawaban = new ArrayList<Long>();
                    for (Set<Long> aa : humDetails.values()) {
                        for (Long hId : aa) {
                            HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, hId.toString());
                            if (humd != null && humd.getBankSoal() != null && humd.getBankSoal().getId().equals(bankSoalId)) {
                                jawaban.add(hId);
                            }
                        }
                    }

                    if (jawaban.isEmpty()) {
                        row.createCell(col).setCellValue("");
                        String key = bankSoalId + "_-";
                        hurufsJawab.put(key, hurufsJawab.getOrDefault(key, 0) + 1);
                    } else {
                        String huruf = "";
                        for (Long idData : jawaban) {
                            HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, idData.toString());
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
                        row.createCell(col).setCellValue(huruf);
                    }
                    col++;
                }
            }

            row.createCell(col).setCellValue(hum.getJawabanBenar());
            row.createCell(col + 1).setCellValue(hum.getJawabanBenarMax() - hum.getJawabanBenar());
            row.createCell(col + 2).setCellValue(hum.getNilai());
            row.createCell(col + 3).setCellValue(rangking);
            row.createCell(col + 4).setCellValue(posisi);

            rowIndex++;
            if(rowIndex % 50 == 0) mySession.clear();
        }

        // FOOTER SUMMARY EXCEL
        rowhead = sheet.createRow(rowIndex+1);
        rowhead.createCell(1).setCellValue("**Kunci Jawaban");
        col = 4;
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) {
                rowhead.createCell(col).setCellValue("**" + mapKunciJawaban.get(ups.getBankSoal().getId()));
                col++;
            }
        }

        int penambaan = 2;
        for (String huruf : masterHurufs) {
            rowhead = sheet.createRow(rowIndex + penambaan);
            rowhead.createCell(1).setCellValue(huruf.equals("-") ? "**Tidak Dijawab" : "**Jawaban " + huruf);
            col = 4;
            for (Long d : ujianPunyaSoalsData) {
                UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
                if (ups != null) {
                    rowhead.createCell(col).setCellValue("**" + hurufsJawab.getOrDefault(ups.getBankSoal().getId() + "_" + huruf, 0));
                    col++;
                }
            }
            penambaan++;
        }

        rowhead = sheet.createRow(rowIndex + penambaan);
        rowhead.createCell(1).setCellValue("**Jawaban Benar");
        col = 4;
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) { rowhead.createCell(col++).setCellValue("**" + jumlahBenar.getOrDefault(ups.getBankSoal().getId(), 0)); }
        }

        rowhead = sheet.createRow(rowIndex + penambaan + 1);
        rowhead.createCell(1).setCellValue("**Jawaban Salah");
        col = 4;
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) { rowhead.createCell(col++).setCellValue("**" + jumlahSalah.getOrDefault(ups.getBankSoal().getId(), 0)); }
        }

        rowhead = sheet.createRow(rowIndex + penambaan + 2);
        rowhead.createCell(1).setCellValue("**Daya Pembeda");
        XSSFRow rowheadKriteria = sheet.createRow(rowIndex + penambaan + 3);
        rowheadKriteria.createCell(1).setCellValue("**Kriteria");

        col = 4;
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) {
                Long id = ups.getBankSoal().getId();
                int jmlAtas = jumlahPosisi.getOrDefault(id + "_Atas", 0);
                int jmlBawah = jumlahPosisi.getOrDefault(id + "_Bawah", 0);
                int benar = jumlahBenar.getOrDefault(id, 0);
                int salah = jumlahSalah.getOrDefault(id, 0);

                double jml = jumlahAtas > 0 ? (jmlAtas - (double)jmlBawah) / jumlahAtas : 0;
                rowhead.createCell(col).setCellValue("**" + Common.numberFormat.get().format(jml));

                String ni = (benar == 0 && salah == 0) ? "Blm dikerjakan" : (jml < 0.05 ? "Ganti" : (jml < 0.1 ? "Revisi" : "Gunakan"));
                rowheadKriteria.createCell(col).setCellValue("**" + ni);
                col++;
            }
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
        // 3. PEMROSESAN HTML TABLE (VIEWER DI POPUP)
        // =========================================================================
        StringBuilder html = new StringBuilder();
        html.append("<div class='table-responsive border shadow-sm'>");
        html.append("<table class='table table-bordered table-hover table-striped text-center align-middle mb-0' style='white-space: nowrap; font-size: 13px;'>");
        
        // Header
        html.append("<thead class='table-dark'>");
        html.append("<tr><th width='5%'>No.</th><th>NIM / No.Reg</th><th class='text-start'>Nama Peserta</th><th>Kelas / Prodi</th>");
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) html.append("<th title='Soal No. "+ups.getNomorUrut()+"' class='text-truncate' style='max-width: 60px;'>" + mapSoalContent.get(ups.getBankSoal().getId()) + "</th>");
        }
        html.append("<th class='bg-success bg-opacity-25'>Benar</th><th class='bg-danger bg-opacity-25'>Salah</th><th class='bg-primary bg-opacity-25'>Skor</th><th>Rank</th><th>Kel. Rank</th>");
        html.append("</tr></thead><tbody>");

        // Loop Mahasiswa/Peserta
        int rIndex = 1;
        for (HasilUjianMahasiswa hum : listHum) {
            int rangking = 0;
            for (Double nilai : treeMapRangking) {
                rangking++;
                if (Common.numberFormat.get().format(nilai).equals(Common.numberFormat.get().format(hum.getNilai()))) break;
            }
            String posisi = rangking <= (jumlahPeserta / 2) ? "Atas" : rangking > ((jumlahPeserta + 1) / 2) ? "Bawah" : "Tengah";
            if (posisi.equalsIgnoreCase("Atas")) jumlahAtas += 1.0;

            ais.ui.util.MyArrayList<Long> ujianPunyaSoals = hum.ambilUjianPunyaSoals(ppu.getJmlDitampilkan(), null, true);
            Map<Long, Set<Long>> humDetails = hum.ambilHasilUjianMahasiswaDetail(ppu.getJmlDitampilkan(), ujianPunyaSoals, false);

            html.append("<tr>");
            html.append("<td class='text-muted fw-bold'>" + rIndex + "</td>");
            
            if (hum.getMahasiswa() != null) {
                html.append("<td>" + hum.getMahasiswa().getNim() + "</td><td class='text-start fw-bold'>" + hum.getMahasiswa().getNama() + "</td><td>" + hum.getMahasiswa().getJurusan().getNama() + "</td>");
            } else if (hum.getBiodataCalonMahasiswa() != null) {
                html.append("<td>" + hum.getBiodataCalonMahasiswa().getNoRegistrasi() + "</td><td class='text-start fw-bold'>" + hum.getBiodataCalonMahasiswa().getNama() + "</td>");
                Jurusan jur = hum.getBiodataCalonMahasiswa().getProdiLulus() != null ? hum.getBiodataCalonMahasiswa().getProdiLulus() : hum.getBiodataCalonMahasiswa().getProdi1();
                html.append("<td>" + (jur == null ? "-" : jur.getNama()) + "</td>");
            }

            for (Long d : ujianPunyaSoalsData) {
                UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
                if (ups != null) {
                    Long bankSoalId = ups.getBankSoal().getId();
                    List<Long> jawaban = new ArrayList<Long>();
                    for (Set<Long> aa : humDetails.values()) {
                        for (Long hId : aa) {
                            HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, hId.toString());
                            if (humd != null && humd.getBankSoal() != null && humd.getBankSoal().getId().equals(bankSoalId)) {
                                jawaban.add(hId);
                            }
                        }
                    }

                    if (jawaban.isEmpty()) {
                        html.append("<td class='text-muted'>-</td>");
                        String key = bankSoalId + "_-";
                        hurufsJawab.put(key, hurufsJawab.getOrDefault(key, 0) + 1);
                    } else {
                        String huruf = "";
                        boolean isBenar = false;
                        for (Long idData : jawaban) {
                            HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, idData.toString());
                            if (humd != null && humd.getBankSoalDetail() != null) {
                                huruf += huruf.isEmpty() ? humd.getBankSoalDetail().getHuruf() : "," + humd.getBankSoalDetail().getHuruf();
                                String key = bankSoalId + "_" + humd.getBankSoalDetail().getHuruf();
                                hurufsJawab.put(key, hurufsJawab.getOrDefault(key, 0) + 1);

                                if (humd.getBankSoalDetail().getBetul()) {
                                    isBenar = true;
                                    jumlahBenar.put(bankSoalId, jumlahBenar.getOrDefault(bankSoalId, 0) + 1);
                                    jumlahPosisi.put(bankSoalId + "_" + posisi, jumlahPosisi.getOrDefault(bankSoalId + "_" + posisi, 0) + 1);
                                } else {
                                    jumlahSalah.put(bankSoalId, jumlahSalah.getOrDefault(bankSoalId, 0) + 1);
                                }
                            }
                        }
                        String cssColor = isBenar ? "text-success fw-bold" : "text-danger";
                        html.append("<td class='"+cssColor+"'>" + huruf + "</td>");
                    }
                }
            }

            html.append("<td class='fw-bold text-success'>" + hum.getJawabanBenar() + "</td>");
            html.append("<td class='fw-bold text-danger'>" + (hum.getJawabanBenarMax() - hum.getJawabanBenar()) + "</td>");
            html.append("<td class='fw-bold text-primary'>" + hum.getNilai() + "</td>");
            html.append("<td><span class='badge bg-secondary rounded-pill'>" + rangking + "</span></td>");
            
            String posBadge = posisi.equals("Atas") ? "bg-success" : (posisi.equals("Bawah") ? "bg-danger" : "bg-warning text-dark");
            html.append("<td><span class='badge "+posBadge+"'>" + posisi + "</span></td>");
            html.append("</tr>");

            rIndex++;
            if(rIndex % 50 == 0) mySession.clear();
        }
        
        html.append("</tbody>");

        // Footer Summary Analisis (Sama seperti ZKoss Sheet)
        html.append("<tfoot class='table-light border-top-2 fw-bold'>");
        html.append("<tr><td colspan='4' class='text-end text-info fw-bold'>Kesesuaian Sub-CPMK</td>");
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) {
                String sc = bankSoalToSubCpmk.get(ups.getBankSoal().getId());
                html.append(sc != null ? "<td><span style='color:#2563eb;font-weight:700;font-size:0.82em;'>" + sc + "</span></td>" : "<td><span class='text-muted'>&#8212;</span></td>");
            }
        }
        html.append("<td colspan='5'></td></tr>");
        html.append("<tr><td colspan='4' class='text-end'>Kunci Jawaban</td>");
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) html.append("<td class='text-primary'>" + mapKunciJawaban.get(ups.getBankSoal().getId()) + "</td>");
        }
        html.append("<td colspan='5'></td></tr>");

        for (String huruf : masterHurufs) {
            html.append("<tr><td colspan='4' class='text-end'>" + (huruf.equals("-") ? "Tidak Dijawab" : "Jawaban " + huruf) + "</td>");
            for (Long d : ujianPunyaSoalsData) {
                UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
                if (ups != null) html.append("<td>" + hurufsJawab.getOrDefault(ups.getBankSoal().getId() + "_" + huruf, 0) + "</td>");
            }
            html.append("<td colspan='5'></td></tr>");
        }

        html.append("<tr><td colspan='4' class='text-end text-success'>Jawaban Benar</td>");
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) html.append("<td class='text-success'>" + jumlahBenar.getOrDefault(ups.getBankSoal().getId(), 0) + "</td>");
        }
        html.append("<td colspan='5'></td></tr>");

        html.append("<tr><td colspan='4' class='text-end text-danger'>Jawaban Salah</td>");
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) html.append("<td class='text-danger'>" + jumlahSalah.getOrDefault(ups.getBankSoal().getId(), 0) + "</td>");
        }
        html.append("<td colspan='5'></td></tr>");

        html.append("<tr><td colspan='4' class='text-end text-primary'>Daya Pembeda</td>");
        StringBuilder kriteriaHtml = new StringBuilder();
        kriteriaHtml.append("<tr><td colspan='4' class='text-end text-dark'>Kriteria Penggunaan</td>");
        
        for (Long d : ujianPunyaSoalsData) {
            UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, d.toString());
            if (ups != null) {
                Long id = ups.getBankSoal().getId();
                int jmlAtas = jumlahPosisi.getOrDefault(id + "_Atas", 0);
                int jmlBawah = jumlahPosisi.getOrDefault(id + "_Bawah", 0);
                int benar = jumlahBenar.getOrDefault(id, 0);
                int salah = jumlahSalah.getOrDefault(id, 0);

                double jml = jumlahAtas > 0 ? (jmlAtas - (double)jmlBawah) / jumlahAtas : 0;
                html.append("<td class='text-primary'>" + Common.numberFormat.get().format(jml) + "</td>");

                String kriteriaCss = "";
                String ni = "";
                if (benar == 0 && salah == 0) {
                    ni = "Blm dikerjakan"; kriteriaCss = "text-muted";
                } else if (jml < 0.05) {
                    ni = "Ganti"; kriteriaCss = "text-danger bg-danger bg-opacity-10";
                } else if (jml < 0.1) {
                    ni = "Revisi"; kriteriaCss = "text-warning bg-warning bg-opacity-10";
                } else {
                    ni = "Gunakan"; kriteriaCss = "text-success bg-success bg-opacity-10";
                }
                kriteriaHtml.append("<td class='"+kriteriaCss+"'>" + ni + "</td>");
            }
        }
        html.append("<td colspan='5'></td></tr>");
        kriteriaHtml.append("<td colspan='5'></td></tr>");
        html.append(kriteriaHtml.toString());
        
        html.append("</tfoot></table></div>");

        resp.put("status", "success");
        resp.put("html", html.toString());
    }

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_analisis_butir_soal_services.jsp:450");
    resp.put("status", "error");
    resp.put("message", e.getMessage());
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(resp.toString());
out.flush();
%>