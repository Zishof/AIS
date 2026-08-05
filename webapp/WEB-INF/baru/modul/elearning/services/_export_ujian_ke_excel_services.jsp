<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.*"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.common.*"%>
<%@page import="org.json.JSONObject"%>
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
    mySession = HibernateUtil.openSession();
    PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, Long.parseLong(ppuParam));
    
    if (ppu == null) throw new Exception("Data Ujian tidak ditemukan.");

    List<HasilUjianMahasiswa> listHum = mySession.createCriteria(HasilUjianMahasiswa.class)
            .add(Restrictions.eq("pertemuanPunyaUjian", ppu))
            .add(Restrictions.isNotNull("keyhasil"))
            .addOrder(Order.asc("nilai")).list();
            
    Collections.reverse(listHum); // Urutkan dari nilai tertinggi

    String filename = "Rekap_Ujian_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx";
    String dirPath = request.getServletContext().getRealPath("/tmp/");
    File dir = new File(dirPath);
    if (!dir.exists()) dir.mkdirs();
    File file = new File(dirPath + filename);

    XSSFWorkbook workbook = new XSSFWorkbook();
    XSSFSheet sheet = workbook.createSheet("Rekapitulasi Ujian");
    sheet.setDefaultColumnWidth(20);

    XSSFRow rowhead = sheet.createRow(0);
    rowhead.createCell(0).setCellValue("No.");
    rowhead.createCell(1).setCellValue("NIM / No.Registrasi");
    rowhead.createCell(2).setCellValue("Nama Peserta");
    rowhead.createCell(3).setCellValue("Waktu Mulai");
    rowhead.createCell(4).setCellValue("Waktu Selesai");
    rowhead.createCell(5).setCellValue("Terjawab");
    rowhead.createCell(6).setCellValue("Nilai Akhir");

    int rowIndex = 1;
    for (HasilUjianMahasiswa hum : listHum) {
        XSSFRow row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(rowIndex);
        
        if (hum.getMahasiswa() != null) {
            row.createCell(1).setCellValue(hum.getMahasiswa().getNim());
            row.createCell(2).setCellValue(hum.getMahasiswa().getNama());
        } else if (hum.getBiodataCalonMahasiswa() != null) {
            row.createCell(1).setCellValue(hum.getBiodataCalonMahasiswa().getNoRegistrasi());
            row.createCell(2).setCellValue(hum.getBiodataCalonMahasiswa().getNama());
        }
        
        row.createCell(3).setCellValue(hum.getMulaiPada() != null ? Common.dateFormat5.get().format(hum.getMulaiPada()) : "-");
        row.createCell(4).setCellValue(hum.getSelesaiPada() != null ? Common.dateFormat5.get().format(hum.getSelesaiPada()) : "-");
        
        double totalSoal = hum.getJumlahSoal() != null ? hum.getJumlahSoal().intValue() : (ppu.getJmlDitampilkan() != null ? ppu.getJmlDitampilkan() : 0);
        double terjawab = hum.getJawabanBenar() != null ? hum.getJawabanBenar() + (hum.getJawabanBenarMax() != null ? hum.getJawabanBenarMax() - hum.getJawabanBenar() : 0) : 0;
        
        row.createCell(5).setCellValue(terjawab + " / " + totalSoal);
        row.createCell(6).setCellValue(hum.getNilai() != null ? hum.getNilai() : 0.0);
        
        rowIndex++;
        if(rowIndex % 50 == 0) mySession.clear();
    }
    
    Common.setStyled(sheet);

    try {
        fileOut = new FileOutputStream(file);
        workbook.write(fileOut);
    } finally {
        if(fileOut != null) fileOut.close();
    }

    resp.put("status", "success");
    resp.put("url", Common.ROOT + "/tmp/" + filename);

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_export_ujian_ke_excel_services.jsp:90");
    resp.put("status", "error");
    resp.put("message", e.getMessage());
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(resp.toString());
out.flush();
%>