<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataMahasiswa"%>
<%@page import="ais.database.model.KelompokParameterTambahanAlumni"%>
<%@page import="ais.database.model.ParameterTambahan"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.database.model.ChecklistPenilaianUmum"%>
<%@page import="ais.database.model.ChecklistHasilPenilaianUmum"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="java.util.Map"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    out.clearBuffer();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    Mahasiswa mahasiswaLog = (Mahasiswa) request.getSession().getAttribute("alumni_logged_in");
    
    if (mahasiswaLog == null) {
        out.print("{\"status\":\"gagal\", \"pesan\":\"" + Common.getBahasaConfig("Sesi telah habis, proses penyimpanan dibatalkan.") + "\"}");
        out.flush(); 
        return;
    }

    Session sess = null;
    Transaction tx = null;
    
    try {
        sess = HibernateUtil.openSession();
        tx = sess.beginTransaction();
        
        BiodataMahasiswa biodata = mahasiswaLog.ambilBiodata();
        if (biodata == null) {
             out.print("{\"status\":\"gagal\", \"pesan\":\"" + Common.getBahasaConfig("Data rekam jejak biodata tidak valid.") + "\"}");
             out.flush(); 
             return;
        }
        
        // Optimasi Memori: Pre-size StringBuilder jika memungkinkan (Hemat Alokasi Ulang Heap)
        StringBuilder parameterTambahanStr = new StringBuilder(2048);
        StringBuilder parameterTambahanIndsAlumni = new StringBuilder(1024);
        
        // Peringatan Warning Generics Java 1.6 ditangani
        @SuppressWarnings("unchecked")
        Map<String, String[]> mapParams = (Map<String, String[]>) request.getParameterMap();
        Long indexKe = 0L; 
        
        // =========================================================================
        // 1. PROSES PENYIMPANAN KUESIONER UTAMA (Iterasi Memori Cepat via EntrySet)
        // =========================================================================
        for (Map.Entry<String, String[]> entry : mapParams.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("param_")) {
                String[] idParts = key.split("_");
                if (idParts.length == 3) {
                    Long kelompokId = Long.parseLong(idParts[1]);
                    Long paramId = Long.parseLong(idParts[2]);
                    
                    ParameterTambahan parameterTambahan = (ParameterTambahan) sess.get(ParameterTambahan.class, paramId);
                    KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni = (KelompokParameterTambahanAlumni) sess.get(KelompokParameterTambahanAlumni.class, kelompokId);
                    
                    if (parameterTambahan != null && kelompokParameterTambahanAlumni != null) {
                        String jenis = kelompokId + "->" + paramId;
                        String[] vals = entry.getValue();
                        String val = StringUtils.join(vals, ";"); 
                        if (val == null) val = "";
                        
                        String ket = ""; 
                        String url = "";
                        
                        if (parameterTambahan.getHarusMenyertakanLampiran() != null && parameterTambahan.getHarusMenyertakanLampiran()) {
                            LampiranLain lam = LampiranLain.ambil(biodata.getId(), jenis);
                            if (lam != null) {
                                try { url = lam.createLinkUri(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_simpan_isi_kuesioner_alumni.jsp:79");}
                            }
                        }

                        String s = kelompokParameterTambahanAlumni.getNama() + "->" + parameterTambahan.getLabelInputan()
                                + "<=>" + val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
                                + parameterTambahan.getId() + "<=>" + kelompokParameterTambahanAlumni.getId() + "<=>"
                                + indexKe + "<=>" + ket;
                        
                        if (parameterTambahanStr.length() > 0) parameterTambahanStr.append("\n");
                        parameterTambahanStr.append(s);

                        String sIds = kelompokParameterTambahanAlumni.getId() + "->" + parameterTambahan.getId() + "<=>"
                                + val + "<=>" + url + "<=>" + ket;
                        
                        if (parameterTambahanIndsAlumni.length() > 0) parameterTambahanIndsAlumni.append("\n");
                        parameterTambahanIndsAlumni.append(sIds);
                    }
                }
            }
        }
		System.out.println("parameterTambahanIndsAlumni -> "+parameterTambahanIndsAlumni);
        biodata.setParameterTambahanIndsAlumni(parameterTambahanIndsAlumni.toString());
        biodata.setParameterTambahanAlumni(parameterTambahanStr.toString());
        sess.update(biodata);

        // =========================================================================
        // 1b. SIMPAN DAFTAR ATASAN (JSON) ke kolom Mahasiswa.atasans
        // =========================================================================
        String atasansJson = request.getParameter("atasans_json");
        if (atasansJson != null) {
            Mahasiswa mhsManaged = (Mahasiswa) sess.get(Mahasiswa.class, mahasiswaLog.getId());
            if (mhsManaged != null) {
                mhsManaged.setAtasans(atasansJson.trim());
                sess.update(mhsManaged);
            }
        }
        
        // =========================================================================
        // 2. PROSES PENYIMPANAN ANGKET PENILAIAN UMUM (CHECKLIST UMUM)
        // =========================================================================
        for (Map.Entry<String, String[]> entry : mapParams.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("chk_val_")) {
                String[] parts = key.split("_");
                if (parts.length >= 5) {
                    Long idChecklist = Long.parseLong(parts[2]);
                    String tahunAkademik = parts[3];
                    String semester = parts[4];
                    
                    String valStr = entry.getValue()[0];
                    int nilaiRadio = 0;
                    try { nilaiRadio = Integer.parseInt(valStr); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_simpan_isi_kuesioner_alumni.jsp:131");}
                    
                    String ketKey = "chk_ket_" + idChecklist + "_" + tahunAkademik + "_" + semester;
                    String ketVal = "";
                    if (mapParams.containsKey(ketKey)) {
                        ketVal = StringUtils.join(mapParams.get(ketKey), ", ");
                    }
                    
                    ChecklistPenilaianUmum cpu = (ChecklistPenilaianUmum) sess.get(ChecklistPenilaianUmum.class, idChecklist);
                    
                    if (cpu != null) {
                        ChecklistHasilPenilaianUmum hasil = (ChecklistHasilPenilaianUmum) sess.createCriteria(ChecklistHasilPenilaianUmum.class)
                            .add(Restrictions.isNull("tbmuserDinilai"))
                            .add(Restrictions.isNull("pertemuanId"))
                            .add(Restrictions.eq("mahasiswa", mahasiswaLog))
                            .add(Restrictions.eq("checklistPenilaianUmum", cpu))
                            .add(Restrictions.eq("semesterStr", semester))
                            .add(Restrictions.eq("tahunAkademik", tahunAkademik))
                            .setMaxResults(1).uniqueResult();
                            
                        if (hasil == null) {
                            hasil = new ChecklistHasilPenilaianUmum();
                            hasil.setMahasiswa(mahasiswaLog);
                            hasil.setChecklistPenilaianUmum(cpu);
                            hasil.setSemesterStr(semester);
                            hasil.setTahunAkademik(tahunAkademik);
                        }
                        
                        hasil.setNilai(nilaiRadio);
                        hasil.setKeterangan(ketVal);
                        
                        sess.saveOrUpdate(hasil);
                    }
                }
            }
        }
        
        tx.commit();
        
        // Sinkronisasi Sesi RAM (termasuk daftar atasan agar tampil setelah reload)
        mahasiswaLog.biodataMahasiswa = biodata;
        if (atasansJson != null) {
            mahasiswaLog.setAtasans(atasansJson.trim());
        }
        request.getSession().setAttribute("alumni_logged_in", mahasiswaLog);

        out.print("{\"status\":\"sukses\", \"pesan\":\"" + Common.getBahasaConfig("Terima kasih. Jawaban kuesioner berhasil direkam dengan sukses.") + "\"}");

    } catch (Exception e) {
        if (tx != null) {
            try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_simpan_isi_kuesioner_alumni.jsp:181");}
        }
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/alumni/_simpan_isi_kuesioner_alumni.jsp:183");
        out.print("{\"status\":\"gagal\", \"pesan\":\"" + Common.getBahasaConfig("Kesalahan server: ") + e.getMessage() + "\"}");
    } finally {
        if (sess != null) {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_simpan_isi_kuesioner_alumni.jsp:187");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_simpan_isi_kuesioner_alumni.jsp:188");}
        }
        HibernateUtil.closeSessionQuietly(sess);
    }
    out.flush();
%>