<%@page import="org.json.JSONObject"%>
<%@page import="ais.action.master.helper.ProsesUjianHelper"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Map"%>
<%@page import="ais.ui.util.MyArrayList"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.BankSoalDetail"%>
<%@page import="java.util.Date"%>
<%@page import="ais.database.model.HasilUjianMahasiswa"%>
<%@page import="ais.database.model.UjianPunyaSoal"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.HasilUjianMahasiswaDetail"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%
// 1. SETTING HEADER JSON
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
PrintWriter outWriter = response.getWriter();

// 2. SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    JSONObject errAuth = new JSONObject();
    errAuth.put("status", "error");
    errAuth.put("message", Common.getBahasaConfig("Sesi Anda telah habis. Silakan login kembali."));
    outWriter.print(errAuth.toString());
    outWriter.flush();
    return;
}

// 3. AMBIL & VALIDASI PARAMETER
String ppuParam = request.getParameter("ppu");
if (ppuParam == null || ppuParam.trim().isEmpty()) {
    JSONObject errParam = new JSONObject();
    errParam.put("status", "error");
    errParam.put("message", Common.getBahasaConfig("Parameter tidak lengkap."));
    outWriter.print(errParam.toString());
    outWriter.flush();
    return;
}

// 4. FETCH BASE DATA DENGAN PENCEGAHAN NULL
PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) GeneralValueObject.ambilData(PertemuanPunyaUjian.class, ppuParam.trim(), true);
if (pertemuanPunyaUjian == null) {
    JSONObject errData = new JSONObject();
    errData.put("status", "error");
    errData.put("message", Common.getBahasaConfig("Data Ujian tidak ditemukan."));
    outWriter.print(errData.toString());
    outWriter.flush();
    return;
}

HasilUjianMahasiswa hasilUjianMahasiswa = null;
try{
	hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(pertemuanPunyaUjian, tbmuser.getMahasiswa(),
	        tbmuser.getBiodataCalonMahasiswa(), tbmuser.getSiswa(), tbmuser.getCalonSiswa());
}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/_hasil_ujian.jsp:62");
	
}

if (hasilUjianMahasiswa == null) {
	// 6. OUTPUT SUKSES
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("status", "success");
    jsonObject.put("message", Common.getBahasaConfig("Data berhasil diupdate"));
    
    // Null safety untuk angka
    jsonObject.put("nilai", "0");
    jsonObject.put("nilai_obe", 0);
    jsonObject.put("jumlahSoal",  0);
    jsonObject.put("jawabanBenar", 0);
    jsonObject.put("jawabanBenarMasimal", 0);
    outWriter.print(jsonObject.toString());
    outWriter.flush();
    return;
}

Session mySession = null;
try {
    // 5. TRANSAKSI DATABASE (Optimasi fetch langsung menggunakan Hibernate Session)
    mySession = HibernateUtil.openSession();
    mySession.getTransaction().begin(); // Mulai transaksi di awal

    mySession.refresh(hasilUjianMahasiswa);
    hasilUjianMahasiswa.setTelahIkutUjian(true);
    // Catatan: Parameter "new Label()" diganti null untuk mencegah error UiException di luar eksekusi ZK
    MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(pertemuanPunyaUjian.getJmlDitampilkan(), null, true);
    Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals, true);
    
    // Kalkulasi
    ProsesUjianHelper.hitungPilihanGanda(hasilUjianMahasiswa, hasilUjianMahasiswaDetails);
    
    // Update data
    Common.refreshUpdate(mySession, hasilUjianMahasiswa);
    mySession.getTransaction().commit();
    
    // 6. OUTPUT SUKSES
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("status", "success");
    jsonObject.put("message", Common.getBahasaConfig("Data berhasil diupdate"));
    
    // Null safety untuk angka
    Double nilai = hasilUjianMahasiswa.getNilai();
    jsonObject.put("nilai", nilai != null ? Common.numberFormat.get().format(nilai) : "0");
    jsonObject.put("nilai_obe", hasilUjianMahasiswa.getNilaiObe() != null ? hasilUjianMahasiswa.getNilaiObe() : 0);
    jsonObject.put("jumlahSoal", hasilUjianMahasiswa.getJumlahSoal() != null ? hasilUjianMahasiswa.getJumlahSoal() : 0);
    jsonObject.put("jawabanBenar", hasilUjianMahasiswa.getJawabanBenar() != null ? hasilUjianMahasiswa.getJawabanBenar() : 0);
    jsonObject.put("jawabanBenarMasimal", hasilUjianMahasiswa.getJawabanBenarMax() != null ? hasilUjianMahasiswa.getJawabanBenarMax() : 0);
    
    outWriter.print(jsonObject.toString());

} catch (Exception e) {
    // 7. ROLLBACK JIKA GAGAL
    if (mySession != null && mySession.getTransaction() != null && mySession.getTransaction().isActive()) {
        mySession.getTransaction().rollback();
    }
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_hasil_ujian.jsp:122");
    
    JSONObject errSystem = new JSONObject();
    errSystem.put("status", "error");
    errSystem.put("message", Common.getBahasaConfig("Terjadi kesalahan pada sistem saat memproses data."));
    outWriter.print(errSystem.toString());
} finally {
    // 8. TUTUP SESSION DI FINALLY BLOCK DENGAN AMAN
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
    try { HibernateUtil.closeSessionQuietly(mySession); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ujian/_hasil_ujian.jsp:131"); /* Abaikan error saat menutup */ }
    
    outWriter.flush();
}%>