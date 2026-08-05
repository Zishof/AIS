<%@page import="ais.common.ConstantValues"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.UjianPunyaSoal"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.BankSoal"%>
<%@page import="ais.database.model.BankSoalDetail"%>
<%@page import="ais.database.model.PenjelasanBankSoal"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%
// 1. SETTING HEADER JSON
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
PrintWriter outWriter = response.getWriter();

// 2. CEK OTORISASI SESI PENGGUNA
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
    outWriter.flush();
    return;
}

// 3. AMBIL PARAMETER (PPU, PAGE, KEYWORD)
String ppuParam = request.getParameter("ppu");
String pageParam = request.getParameter("page");
String keyword = request.getParameter("keyword") != null ? request.getParameter("keyword").trim() : "";
Boolean refreshData = request.getParameter("refresh") != null && request.getParameter("refresh").trim().equalsIgnoreCase("true");
if (ppuParam == null || ppuParam.trim().isEmpty()) {
    outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Parameter referensi ujian tidak ditemukan.") + "\"}");
    outWriter.flush();
    return;
}

int pageNo = 1;
try {
    if (pageParam != null && !pageParam.isEmpty()) {
        pageNo = Integer.parseInt(pageParam);
    }
} catch (NumberFormatException e) {
    pageNo = 1;
}

int limit = 10; // Jumlah maksimal soal per halaman
int firstResult = (pageNo - 1) * limit;

Session mySession = null;
try {
    
    
    PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) GeneralValueObject.ambilData(PertemuanPunyaUjian.class, ppuParam.trim(), true);
    if (ppu == null || ppu.getUjian() == null) {
        outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data sesi ujian tidak tersedia di pangkalan data.") + "\"}");
        outWriter.flush();
        return;
    }
    
 // 4. BUKA KONEKSI DATABASE
    mySession = HibernateUtil.openSession();
    // 5. MENGHITUNG TOTAL DATA (UNTUK PAGING)
    Criteria countCrit = mySession.createCriteria(UjianPunyaSoal.class, "ups");
    countCrit.createAlias("ups.bankSoal", "bs");
    countCrit.add(Restrictions.eq("ups.ujian", ppu.getUjian()));
    
    if (!keyword.isEmpty()) {
        countCrit.add(Restrictions.ilike("bs.soal", keyword, MatchMode.ANYWHERE));
    }
    
    countCrit.setProjection(Projections.rowCount());
    Long totalRecords = (Long) countCrit.uniqueResult();
    int totalPages = (int) Math.ceil((double) totalRecords / limit);

    // 6. MENGAMBIL DATA SOAL (LIMIT & OFFSET)
    Criteria dataCrit = mySession.createCriteria(UjianPunyaSoal.class, "ups");
    dataCrit.createAlias("ups.bankSoal", "bs");
    dataCrit.add(Restrictions.eq("ups.ujian", ppu.getUjian()));
    
    if (!keyword.isEmpty()) {
        dataCrit.add(Restrictions.ilike("bs.soal", keyword, MatchMode.ANYWHERE));
    }
    
    dataCrit.addOrder(Order.asc("ups.id")); // Urutkan berdasarkan ID
    dataCrit.setFirstResult(firstResult);
    dataCrit.setMaxResults(limit);
    
    List<UjianPunyaSoal> listUps = ConstantValues.simpleList(dataCrit,UjianPunyaSoal.class);
    

    // 7. MEMBANGUN STRUKTUR JSON PENGEMBALIAN
    JSONObject responseObj = new JSONObject();
    responseObj.put("status", "success");
    responseObj.put("currentPage", pageNo);
    responseObj.put("totalPages", totalPages);
    responseObj.put("totalRecords", totalRecords);
    responseObj.put("startNumber", firstResult + 1);

    JSONArray dataArray = new JSONArray();
    for (UjianPunyaSoal ups : listUps) {
        BankSoal bankSoal = ups.getBankSoal();
        JSONObject soalObj = new JSONObject();
        
        soalObj.put("id", ups.getId());
        soalObj.put("idBankSoal", bankSoal.getId());
        soalObj.put("teksSoal", bankSoal.getSoal());
        soalObj.put("jenisKoreksi", bankSoal.getJenisKoreksi());
        
        // Mengambil Detail Opsi Jawaban (Jika Pilihan Ganda)
        JSONArray opsiArray = new JSONArray();
        if (PenjelasanBankSoal.KOREKSI_OTOMATIS.equals(bankSoal.getJenisKoreksi())) {
            List<Long> detailIds = bankSoal.ambilBankSoalDetail(refreshData);
            if (detailIds != null) {
                for (Long detailId : detailIds) {
                    BankSoalDetail bsd = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class, detailId.toString());
                    if (bsd != null) {
                        JSONObject opsiObj = new JSONObject();
                        opsiObj.put("id", bsd.getId());
                        opsiObj.put("teks", bsd.getJawaban());
                        opsiObj.put("isBenar", bsd.getBetul() != null ? bsd.getBetul() : false);
                        opsiArray.put(opsiObj);
                    }
                }
            }
        }
        soalObj.put("opsi", opsiArray);
        dataArray.put(soalObj);
    }
    
    responseObj.put("data", dataArray);
    outWriter.print(responseObj.toString());

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_ambil_daftar_soal.jsp:144");
    outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Terjadi kesalahan teknis saat mengambil data daftar soal.") + "\"}");
} finally {
    // 8. PENGAMANAN PENUTUPAN KONEKSI DATABASE
    if (mySession != null) {
        if (mySession.isConnected()) {
            try { mySession.disconnect(); } catch (Exception ex) { ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_ambil_daftar_soal.jsp:150"); }
        }
        if (mySession.isOpen()) {
            ais.common.ElearningSessionUtil.closeQuietly(mySession);
        }
    }
    outWriter.flush();
}%>