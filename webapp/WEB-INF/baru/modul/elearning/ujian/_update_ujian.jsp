<%@page import="ais.database.model.PenjelasanBankSoal"%>
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
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
// 1. SETTING HEADER JSON
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
PrintWriter outWriter = response.getWriter();

// 2. SECURITY CHECK
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
    outWriter.flush();
    return;
}

// 3. AMBIL PARAMETER
String idParam = request.getParameter("myHasilUjianMahasiswaDetailid");
String ujianPunyaSoalIdParam = request.getParameter("ujianPunyaSoalid");
String bankSoalDetailIdParam = request.getParameter("bankSoalDetailid");
String waktuParam = request.getParameter("waktu");
String ppuParam = request.getParameter("ppu");

// Mengakomodasi parameter esai yang mungkin dikirim sebagai "jawabanEsai" atau "jawaban"
String jawabanEsai = request.getParameter("jawabanEsai");
String jawabanText = request.getParameter("jawaban"); 
String jawabanFinal = (jawabanEsai != null && !jawabanEsai.trim().isEmpty()) ? jawabanEsai : (jawabanText != null ? jawabanText : "");

// 4. VALIDASI PARAMETER UTAMA (bankSoalDetailIdParam tidak diwajibkan di awal karena ujian esai mungkin mengirim ID 0 atau kosong)
if (idParam == null || idParam.trim().isEmpty() || waktuParam == null || waktuParam.trim().isEmpty()) {
    outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Parameter yang dikirimkan tidak lengkap atau tidak valid.") + "\"}");
    outWriter.flush();
    return;
}

Session mySession = null;
try {
    // 5. PARSING PARAMETER ID
    Long myHasilUjianMahasiswaDetailid = Long.parseLong(idParam.trim());

    // "waktu" = sisa waktu dari timer, formatnya HH:mm:ss (mis. "00:59:54") — BUKAN dd-MM-yyyy.
    // Parse TOLERAN: jika gagal, JANGAN gagalkan penyimpanan jawaban (jawaban WAJIB tersimpan
    // walau format waktu tidak terbaca). waktu hanya untuk sisaWaktuPengerjaan (sekunder).
    Date waktu = null;
    try {
        if (waktuParam != null && !waktuParam.trim().isEmpty()) {
            waktu = new java.text.SimpleDateFormat("HH:mm:ss").parse(waktuParam.trim());
        }
    } catch (Exception eWaktu) {
        try { waktu = Common.dateFormat1.get().parse(waktuParam.trim()); } catch (Exception ig) { waktu = null; }
    }

    // 6. TRANSAKSI DATABASE MENGGUNAKAN HIBERNATE SESSION
    mySession = HibernateUtil.openSession();
    mySession.getTransaction().begin();

    // 7. AMBIL DATA DETAIL UJIAN
    HasilUjianMahasiswaDetail myDetail = (HasilUjianMahasiswaDetail) mySession.get(HasilUjianMahasiswaDetail.class, myHasilUjianMahasiswaDetailid);
    
    // Validasi data tidak ditemukan
    if (myDetail == null) {
        outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data rincian ujian tidak ditemukan di dalam sistem.") + "\"}");
        mySession.getTransaction().rollback();
        outWriter.flush();
        return;
    }

    // 8. LOGIKA PEMISAHAN JENIS KOREKSI (PILIHAN GANDA VS ESAI)
    String jenisKoreksi = myDetail.getBankSoal().getJenisKoreksi();
    
    if (PenjelasanBankSoal.KOREKSI_MANUAL.equals(jenisKoreksi)) {
        // -- LOGIKA UNTUK UJIAN ESAI --
        myDetail.setJawaban(jawabanFinal);
        
        // (Opsional) Jika di model Anda ada setter spesifik untuk esai:
        // myDetail.setJawabanEsai(jawabanFinal); 
        
    } else {
        // -- LOGIKA UNTUK UJIAN PILIHAN GANDA (OTOMATIS) --
        if (bankSoalDetailIdParam == null || bankSoalDetailIdParam.trim().isEmpty() || bankSoalDetailIdParam.trim().equals("0")) {
            outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Parameter pilihan jawaban tidak ditemukan.") + "\"}");
            mySession.getTransaction().rollback();
            outWriter.flush();
            return;
        }
        
        Long bankSoalDetailid = Long.parseLong(bankSoalDetailIdParam.trim());
        BankSoalDetail bankSoalDetail = (BankSoalDetail) mySession.get(BankSoalDetail.class, bankSoalDetailid);
        
        if (bankSoalDetail == null) {
            outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Pilihan jawaban tidak valid atau tidak terdaftar di pangkalan data.") + "\"}");
            mySession.getTransaction().rollback();
            outWriter.flush();
            return;
        }

        // Cegah lintas-soal: bankSoalDetail yang dikirim HARUS milik BankSoal yang sama
        // dengan soal yang sedang dijawab (myDetail.getBankSoal()). Tanpa ini, id
        // BankSoalDetail dari soal/ujian LAIN (termasuk dari modul kursus non-formal,
        // karena tabel ini dipakai bersama seluruh sistem CBT) bisa dikirim untuk
        // memalsukan jawaban benar bernilai tinggi. Pola sama seperti _kursus_service.jsp.
        if (bankSoalDetail.getBankSoal() == null || myDetail.getBankSoal() == null
                || !bankSoalDetail.getBankSoal().getId().equals(myDetail.getBankSoal().getId())) {
            outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Pilihan jawaban tidak valid untuk soal ini.") + "\"}");
            mySession.getTransaction().rollback();
            outWriter.flush();
            return;
        }
        myDetail.setBankSoalDetail(bankSoalDetail);
    }

    // 9. UPDATE WAKTU PENGERJAAN & SIMPAN KE DATABASE
    myDetail.setWaktuJawab(ais.ui.util.WaktuUtil.getDate());
    
    HasilUjianMahasiswa hasilUjianMahasiswa = myDetail.getHasilUjianMahasiswa();
    if (hasilUjianMahasiswa != null) {
        if (waktu != null) {
            hasilUjianMahasiswa.setSisaWaktuPengerjaan(waktu);
        }
        mySession.update(hasilUjianMahasiswa);
    }
    
    mySession.update(myDetail);
    mySession.getTransaction().commit();

    // 9b. KOHERENSI CACHE: tulis-balik detail terbaru ke cache MapDB (HasilUjianMahasiswaDetail
    // termasuk CLASS_IZINKAN). Tanpa ini cache menyimpan salinan lama (bankSoalDetail null) →
    // pembacaan skor 0 walau benar. Dilakukan selagi session masih terbuka.
    try {
        ais.database.model.GeneralValueObject.masukkanData(HasilUjianMahasiswaDetail.class, myDetail);
    } catch (Exception eCache) {
        eCache.printStackTrace(); ais.common.ErrorAuditUtil.record(eCache, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_update_ujian.jsp:131");
    }

    // 9c. HITUNG-ULANG SKOR DI LATAR (THREAD) — DI-THROTTLE: hanya tiap kelipatan N
    // jawaban (default 10) supaya performa tak turun saat ujian massal. Hitung penuh
    // tetap terjadi saat "Akhiri Ujian" (_hasil_ujian.jsp).
    if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getId() != null) {
        ais.action.master.helper.UjianRecomputeUtil.tandaiJawabanTersimpan(hasilUjianMahasiswa.getId());
    }

    // 10. OUTPUT SUKSES
    outWriter.print("{\"status\":\"success\", \"message\":\"" + Common.getBahasaConfig("Jawaban Anda telah berhasil disimpan.") + "\"}");

} catch (NumberFormatException e) {
    outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Format data angka atau waktu yang dikirimkan tidak valid.") + "\"}");
} catch (Exception e) {
    // Rollback jika terjadi kegagalan di tengah transaksi
    if (mySession != null && mySession.getTransaction() != null && mySession.getTransaction().isActive()) {
        mySession.getTransaction().rollback();
    }
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_update_ujian.jsp:151"); 
    outWriter.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Terjadi kesalahan pada sistem saat memproses penyimpanan data.") + "\"}");
} finally {
    // 11. TUTUP SESSION DI FINALLY BLOCK DENGAN AMAN
    if (mySession != null) {
        if (mySession.isConnected()) {
            try { mySession.disconnect(); } catch (Exception ex) { ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_update_ujian.jsp:157"); }
        }
        if (mySession.isOpen()) {
            ais.common.ElearningSessionUtil.closeQuietly(mySession);
        }
    }
    outWriter.flush();
}
%>