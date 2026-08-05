<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.BankSoal"%>
<%@page import="ais.database.model.BankSoalDetail"%>
<%@page import="ais.database.model.UjianPunyaSoal"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.PenjelasanBankSoal"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="java.util.Date"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%><%

response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");

// 1. Cek Autentikasi
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali.") + "\"}");
    return;
}

// 2. Tangkap Parameter dari AJAX
String idSoalStr = request.getParameter("idSoal");
String ppuStr = request.getParameter("ppu");
String jenisKoreksi = request.getParameter("jenisKoreksi");
String teksSoal = request.getParameter("teksSoal");
String[] opsiJawaban = request.getParameterValues("opsiJawaban[]");
String indeksKunciStr = request.getParameter("indeksKunciJawaban");

// 3. Validasi Parameter Wajib
if (ppuStr == null || ppuStr.trim().isEmpty() || teksSoal == null || teksSoal.trim().isEmpty()) {
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data parameter wajib (PPU atau Teks Soal) tidak lengkap.") + "\"}");
    return;
}

Session mySession = null;
Transaction tx = null;

try {
    mySession = HibernateUtil.openSession();
    tx = mySession.beginTransaction();

    PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) GeneralValueObject.ambilData(PertemuanPunyaUjian.class, ppuStr.trim(), true);
    
    if (ppu == null) {
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data Ujian tidak ditemukan di dalam sistem.") + "\"}");
        tx.rollback();
        return;
    }

    BankSoal bankSoal = null;
    UjianPunyaSoal ups = null;
    boolean isBaru = (idSoalStr == null || idSoalStr.trim().isEmpty());

    if (isBaru) {
        // --- PROSES TAMBAH SOAL BARU ---
        bankSoal = new BankSoal();
        bankSoal.setSoal(teksSoal);
        bankSoal.setJenisKoreksi(jenisKoreksi);
        mySession.save(bankSoal);
        
        ups = new UjianPunyaSoal();
        ups.setUjian(ppu.getUjian());
        ups.setBankSoal(bankSoal);
        mySession.save(ups);
        
    } else {
        // --- PROSES UBAH SOAL LAMA ---
        Long idSoal;
        try {
            idSoal = Long.parseLong(idSoalStr.trim());
        } catch (NumberFormatException e) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Format ID Soal tidak valid.") + "\"}");
            tx.rollback();
            return;
        }

        ups = (UjianPunyaSoal) mySession.get(UjianPunyaSoal.class, idSoal);
        
        if (ups == null || ups.getBankSoal() == null) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Data soal yang akan diubah tidak ditemukan.") + "\"}");
            tx.rollback();
            return;
        }
        
        bankSoal = ups.getBankSoal();
        bankSoal.setSoal(teksSoal);
        // Pastikan jenis koreksi juga terupdate jika terjadi perubahan
        if (jenisKoreksi != null) {
            bankSoal.setJenisKoreksi(jenisKoreksi);
        }
        mySession.update(bankSoal);
    }

    // --- PROSES OPSI JAWABAN (PILIHAN GANDA) ---
    if (PenjelasanBankSoal.KOREKSI_OTOMATIS.equals(jenisKoreksi) && opsiJawaban != null && indeksKunciStr != null) {
        
        int indeksKunci;
        try {
            indeksKunci = Integer.parseInt(indeksKunciStr.trim());
        } catch (NumberFormatException e) {
            out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Format indeks kunci jawaban tidak valid.") + "\"}");
            tx.rollback();
            return;
        }

        // OPTIMISASI: Tarik semua detail opsi yang sudah ada dalam satu kali query (Menghindari N+1 Problem)
        List<BankSoalDetail> existingDetails = mySession.createCriteria(BankSoalDetail.class)
                .add(Restrictions.eq("bankSoal", bankSoal)).list();
        
        Map<String, BankSoalDetail> detailMap = new HashMap<String, BankSoalDetail>();
        for (BankSoalDetail d : existingDetails) {
            detailMap.put(d.getHuruf().toUpperCase(), d);
        }

        String[] daftarHuruf = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
        List<String> hurufTerpakai = new ArrayList<String>();

        // Simpan atau perbarui opsi jawaban
        for (int i = 0; i < opsiJawaban.length; i++) {
            if (i >= daftarHuruf.length) break; // Batasan pengaman array
            
            String huruf = daftarHuruf[i];
            hurufTerpakai.add(huruf);

            // Ambil dari map jika sudah ada, jika belum buat objek baru
            BankSoalDetail opsi = detailMap.get(huruf);
            if (opsi == null) {
                opsi = new BankSoalDetail();
                opsi.setBankSoal(bankSoal);
                opsi.setHuruf(huruf);
            }
            
            opsi.setJawaban(opsiJawaban[i]);
            opsi.setBetul(i == indeksKunci);
            mySession.saveOrUpdate(opsi);
        }
        
        // Hapus opsi jawaban lama yang sudah tidak terpakai
        for (Map.Entry<String, BankSoalDetail> entry : detailMap.entrySet()) {
            if (!hurufTerpakai.contains(entry.getKey())) {
                mySession.delete(entry.getValue());
            }
        }
    }

    // Lakukan flush satu kali saja sebelum commit
    mySession.flush();
    tx.commit();
    out.print("{\"status\":\"success\", \"message\":\"" + Common.getBahasaConfig("Data soal berhasil disimpan dengan baik.") + "\"}");

} catch (Exception e) {
    if (tx != null && tx.isActive()) {
        tx.rollback();
    }
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_simpan_soal.jsp:165");
    String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"").replace("\n", " ") : "Unknown Error";
    out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Terjadi kesalahan pada server internal:") + " " + errorMsg + "\"}");
} finally {
    // Penutupan koneksi secara aman
    if (mySession != null) {
        try {
            if (mySession.isConnected()) {
                mySession.disconnect();
            }
            if (mySession.isOpen()) {
                mySession.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit webapp/WEB-INF/baru/modul/elearning/ujian/_simpan_soal.jsp:179"); // Log error penutupan tanpa mengganggu response ke user
        }
    }
}%>