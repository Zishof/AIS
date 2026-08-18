<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.common.*"%>
<%@page import="ais.action.master.helper.ProsesUjianHelper"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.*"%>
<%@page import="org.zkoss.zul.Label"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
JSONObject resp = new JSONObject();
Session mySession = null;
Transaction tx = null;

try {
    String idHasilParam = request.getParameter("idHasil");
    if (idHasilParam == null || idHasilParam.trim().isEmpty()) {
        throw new Exception("ID Hasil Ujian tidak valid.");
    }
    
    Long idHasil = Long.parseLong(idHasilParam.trim());

    mySession = HibernateUtil.getSessionFactory().openSession();
    tx = mySession.beginTransaction();
    
    HasilUjianMahasiswa tempHasilUjianMahasiswa = (HasilUjianMahasiswa) mySession.get(HasilUjianMahasiswa.class, idHasil);
    
    if (tempHasilUjianMahasiswa != null) {
        PertemuanPunyaUjian ppu = tempHasilUjianMahasiswa.getPertemuanPunyaUjian();
        boolean isPG = ppu != null && ppu.getUjian() != null && BankSoal.PILIHAN_GANDA.equals(ppu.getUjian().getJenis());
        
        boolean isObe = false;
        if (tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan() != null &&
            tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan() != null &&
            tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulum() != null &&
            tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
                tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getTahunAjaran(),
                tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getGanjilGenap()
            )) {
            isObe = true;
        }

        // 1. Mengambil soal ujian
        ais.ui.util.MyArrayList<Long> ujianPunyaSoals = tempHasilUjianMahasiswa.ambilUjianPunyaSoals(
                tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);
                
        // 2. Mengambil detail jawaban mahasiswa
        Map<Long, Set<Long>> hasilUjianMahasiswaDetailsa = tempHasilUjianMahasiswa.ambilHasilUjianMahasiswaDetail(
                tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), ujianPunyaSoals, false);

        // ====================================================================
        // LOGIKA UNTUK PILIHAN GANDA
        // ====================================================================
        if (isPG) {
            if (isObe) {
                ProsesUjianHelper.hitungObe(tempHasilUjianMahasiswa, hasilUjianMahasiswaDetailsa);
            }
            ProsesUjianHelper.hitungPilihanGanda(tempHasilUjianMahasiswa, hasilUjianMahasiswaDetailsa);
            
            mySession.update(tempHasilUjianMahasiswa);
            tx.commit(); tx = null;

            resp.put("status", "success");
            resp.put("message", "Perhitungan ulang Pilihan Ganda berhasil.");
            resp.put("new_nilai", Common.numberFormat.get().format(tempHasilUjianMahasiswa.getNilai()));

        // ====================================================================
        // LOGIKA UNTUK ESAI / MANUAL
        // ====================================================================
        } else {
            if (isObe) {
                ProsesUjianHelper.hitungObe(tempHasilUjianMahasiswa, hasilUjianMahasiswaDetailsa);
            }
            
            Double sumNilai = 0.0;
            for (Long ujianPunyaSoalid : ujianPunyaSoals) {
                UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, ujianPunyaSoalid.toString());
                if (ujianPunyaSoal != null) {
                    BankSoal bankSoal = ujianPunyaSoal.getBankSoal();
                    Set<Long> hasilUjianMahasiswaDetails = hasilUjianMahasiswaDetailsa.get(bankSoal.getId());
                    
                    if (hasilUjianMahasiswaDetails != null && !hasilUjianMahasiswaDetails.isEmpty()) {
                        HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(
                                HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetails.iterator().next().toString());
                                
                        if (hasilUjianMahasiswaDetail != null && hasilUjianMahasiswaDetail.getNilai() != null && bankSoal.getSkor() != null && bankSoal.getSkor() > 0) {
                            Double nilai = hasilUjianMahasiswaDetail.getNilai();
                            Double skor = bankSoal.getSkor();
                            sumNilai += (nilai * 100.0) / skor;
                        }
                    }
                }
            }

            if (sumNilai != null && sumNilai.doubleValue() > 0.1) {
                Double n = sumNilai.doubleValue() / ujianPunyaSoals.size();
                tempHasilUjianMahasiswa.setNilai(n);
                
                mySession.update(tempHasilUjianMahasiswa);
                tx.commit(); tx = null;

                resp.put("status", "success");
                resp.put("message", "Perhitungan ulang Essay berhasil.");
                resp.put("new_nilai", Common.numberFormat.get().format(n));
                
            } else {
                resp.put("status", "warning");
                resp.put("message", "Hasil ujian mahasiswa belum Anda koreksi sepenuhnya.");
            }
        }
        
    } else {
        resp.put("status", "error");
        resp.put("message", "Data Hasil Ujian tidak ditemukan di database.");
    }

} catch (Exception e) {
    if (tx != null) tx.rollback();
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_hitung_ulang_ujian_services.jsp:121");
    resp.put("status", "error");
    resp.put("message", "Terjadi kesalahan sistem: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(resp.toString());
out.flush();
%>