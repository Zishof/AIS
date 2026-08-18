<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
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
    String ppuParam = request.getParameter("ppu");
    if (ppuParam == null || ppuParam.trim().isEmpty()) {
        throw new Exception("ID Evaluasi Ujian tidak valid.");
    }
    
    Long ppuId = Long.parseLong(ppuParam.trim());

    mySession = HibernateUtil.getSessionFactory().openSession();
    tx = mySession.beginTransaction();
    
    PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, ppuId);
    if (ppu == null) throw new Exception("Data Pertemuan Punya Ujian tidak ditemukan.");

    // Cek Jenis & Status OBE
    boolean isPG = BankSoal.PILIHAN_GANDA.equals(ppu.getUjian().getJenis());
    boolean isObe = false;
    
    if (ppu.getPertemuan() != null &&
        ppu.getPertemuan().getPerkuliahan() != null &&
        ppu.getPertemuan().getPerkuliahan().getKurikulum() != null &&
        ppu.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
            ppu.getPertemuan().getPerkuliahan().getTahunAjaran(),
            ppu.getPertemuan().getPerkuliahan().getGanjilGenap()
        )) {
        isObe = true;
    }

    // Ambil Seluruh Data Peserta yang memiliki nilai (Keyhasil NOT NULL)
    List<HasilUjianMahasiswa> listHum = mySession.createCriteria(HasilUjianMahasiswa.class)
            .add(Restrictions.eq("pertemuanPunyaUjian", ppu))
            .add(Restrictions.isNotNull("keyhasil"))
            .list();

    if (listHum == null || listHum.isEmpty()) {
        resp.put("status", "warning");
        resp.put("message", "Belum ada peserta yang mengumpulkan ujian.");
        out.print(resp.toString()); out.flush(); return;
    }

    int rowCount = 0;
    
    for (HasilUjianMahasiswa hum : listHum) {
        // Ambil Data Soal & Detail Jawaban per anak
        ais.ui.util.MyArrayList<Long> ujianPunyaSoals = hum.ambilUjianPunyaSoals(ppu.getJmlDitampilkan(), new Label(), true);
        Map<Long, Set<Long>> humDetails = hum.ambilHasilUjianMahasiswaDetail(ppu.getJmlDitampilkan(), ujianPunyaSoals, false);

        if (isPG) {
            // Kalkulasi Pilihan Ganda
            if (isObe) {
                ProsesUjianHelper.hitungObe(hum, humDetails);
            }
            ProsesUjianHelper.hitungPilihanGanda(hum, humDetails);
            
        } else {
            // Kalkulasi Esai
            if (isObe) {
                ProsesUjianHelper.hitungObe(hum, humDetails);
            }
            
            Double sumNilai = 0.0;
            for (Long upsId : ujianPunyaSoals) {
                UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, upsId.toString());
                if (ups != null) {
                    BankSoal bs = ups.getBankSoal();
                    Set<Long> ansSet = humDetails.get(bs.getId());
                    
                    if (ansSet != null && !ansSet.isEmpty()) {
                        HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) GeneralValueObject.ambilData(
                                HasilUjianMahasiswaDetail.class, ansSet.iterator().next().toString());
                                
                        if (humd != null && humd.getNilai() != null && bs.getSkor() != null && bs.getSkor() > 0) {
                            sumNilai += (humd.getNilai() * 100.0) / bs.getSkor();
                        }
                    }
                }
            }

            if (sumNilai != null && sumNilai.doubleValue() > 0.1) {
                Double newNilai = sumNilai.doubleValue() / ujianPunyaSoals.size();
                hum.setNilai(newNilai);
            }
        }
        
        mySession.update(hum);
        
        rowCount++;
        // Lakukan pembersihan memori setiap 50 iterasi agar tidak OutOfMemory error
        if (rowCount % 50 == 0) {
            mySession.flush();
            mySession.clear();
        }
    }
    
    tx.commit(); tx = null;

    resp.put("status", "success");
    resp.put("message", "Perhitungan ulang nilai untuk " + rowCount + " peserta berhasil diselesaikan.");

} catch (Exception e) {
    if (tx != null) tx.rollback();
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_hitung_ulang_semua_ujian_services.jsp:119");
    resp.put("status", "error");
    resp.put("message", "Terjadi kesalahan sistem: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(resp.toString());
out.flush();
%>