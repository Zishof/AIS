<%@page import="ais.action.report.Report"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.common.Common"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.*"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
JSONObject resp = new JSONObject();
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) {
    resp.put("status", "error");
    resp.put("message", Common.getBahasaConfig("Sesi Anda telah berakhir."));
    out.print(resp.toString()); return;
}

String idParam = request.getParameter("perkuliahanId");
if (idParam == null || idParam.trim().isEmpty()) {
    resp.put("status", "error");
    resp.put("message", Common.getBahasaConfig("ID Perkuliahan tidak valid."));
    out.print(resp.toString()); return;
}

Session mySession = null;

try {
    mySession = HibernateUtil.openSession();
    Long perkuliahanId = Long.parseLong(idParam.trim());
    Perkuliahan perkuliahan = (Perkuliahan) mySession.get(Perkuliahan.class, perkuliahanId);

    if (perkuliahan == null) {
        throw new Exception(Common.getBahasaConfig("Data perkuliahan tidak ditemukan."));
    }

    // 1. Ekstraksi Format Nilai
    List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan, true); // true = refresh
    Map<String, Object> parameters = ais.common.HashMapGenerator.getRand();
    
    // 2. Menyisipkan Parameter Dasar Laporan
    Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "perkuliahan");

    if (perkuliahan.getJurusan() != null) {
        Common.insertProperty(Jurusan.class, perkuliahan.getJurusan(), parameters, "jur");
        if (perkuliahan.getJurusan().getFakultas() != null) {
            Common.insertProperty(Fakultas.class, perkuliahan.getJurusan().getFakultas(), parameters, "fak");
            if (perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() != null) {
                Common.insertProperty(PerguruanTinggi.class, perkuliahan.getJurusan().getFakultas().getPerguruanTinggi(), parameters, "pt");
            }
        }
    }

    String judulLaporan = (perkuliahan.getStatusSemesterPendek() != null && perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)) 
            ? "Daftar Nilai Ujian Semester Pendek" : "Daftar Nilai Ujian";

    parameters.put("judul_laporan_nilai", judulLaporan);
    parameters.put("perkuliahan", perkuliahan.getId());
    parameters.put("kelas", perkuliahan.getSemester() + " " + (perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas()));
    parameters.put("tanggal_dibuat", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
    parameters.put("tampil_nilai", 1);
    parameters.put("fakultas", (perkuliahan.getJurusan() == null || perkuliahan.getJurusan().getFakultas() == null) ? "" : perkuliahan.getJurusan().getFakultas().getNama());
    parameters.put("jenis_semester", ((Integer) perkuliahan.getSemester()) % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
    parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
    parameters.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
    parameters.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());

    String dosen = "";
    int indexDosen = 1;
    for (Dosen d : perkuliahan.populateDosenBuNama()) {
        Common.insertProperty(Dosen.class, d, parameters, "dosen_" + indexDosen);
        dosen += dosen.isEmpty() ? d.getNama().toUpperCase() : " / " + d.getNama().toUpperCase();
        indexDosen++;
    }
    parameters.put("dosen", dosen);
    parameters.put("nip_dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());
    parameters.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
    parameters.put("program", perkuliahan.getProgram());

    if (perkuliahan.getKurikulum() != null && perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getGrupJurusan() != null && perkuliahan.getJurusan().getGrupJurusan().getKajur() != null) {
        parameters.put("nama_kajur", perkuliahan.getJurusan().getGrupJurusan().getKajur().getNama());
        parameters.put("nip_kajur", perkuliahan.getJurusan().getGrupJurusan().getKajur().getCode());
    }

    int i = 1;
    for (FormatNilai formatNilai : formatNilais) {
        parameters.put("col" + i, formatNilai.getNama() + "\n" + formatNilai.getPersen() + "%");
        parameters.put("col_nama_" + i, formatNilai.getNama());
        parameters.put("col_persen_" + i, Common.numberFormat.get().format(formatNilai.getPersen()) + "%");
        parameters.put("persen_" + i, formatNilai.getPersen());
        i++;
    }

    // 3. Membangun List Map untuk Data Mahasiswa (Rows)
    List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
    Collection<Long> terdaftar = perkuliahan.ambilDetailperkuliahan();
    
    for (Long dpId : terdaftar) {
        Detailperkuliahan dp = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, dpId.toString());
        if (dp != null && dp.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
            Map<String, Object> map = new HashMap<String, Object>();
            Mahasiswa mhs = dp.getMahasiswa();
            map.put("nim", mhs.getNim());
            map.put("nama", mhs.getNama().toUpperCase());
            map.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());

            Common.insertProperty(Detailperkuliahan.class, dp, map, "detailperkuliahan");

            int j = 1;
            for (FormatNilai fn : formatNilais) {
                if (perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi() != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi() && dp.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
                    map.put("nilai_" + j, dp.retreiveDetailNilaiBelumVerify(fn));
                } else {
                    map.put("nilai_" + j, dp.retreiveDetailNilai(fn));
                }
                j++;
            }

            if (perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi() != null && perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi() && dp.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
                map.put("nilai", dp.getTotalNilaiSementara());
                map.put("nilai_huruf", dp.getNilaiHurufSementara());
            } else {
                map.put("nilai", dp.getTotalNilai());
                map.put("nilai_huruf", dp.getNilaiHuruf());
            }

            try { map.put("nip_kajur", mhs.getJurusan().getFakultas().getDekan().getCode()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_cetak_laporan_nilai.jsp:131");}
            try { map.put("nama_kajur", mhs.getJurusan().getFakultas().getDekan().getNama()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_cetak_laporan_nilai.jsp:132");}
            try { map.put("nip_kaprodi", mhs.getJurusan().getKaprodi().getCode()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_cetak_laporan_nilai.jsp:133");}
            try { map.put("nama_kaprodi", mhs.getJurusan().getKaprodi().getNama()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_cetak_laporan_nilai.jsp:134");}
            try { map.put("id_fakultas", mhs.getJurusan().getFakultas().getId()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_cetak_laporan_nilai.jsp:135");}

            maps.add(map);
        }
    }

    parameters.put("terdaftar", terdaftar.size());
    String tahunAkademik = perkuliahan.getTahunAjaran();
    parameters.put("bar", "3-" + tahunAkademik + "-" + perkuliahan.getSemester() + "-" + perkuliahan.getId());

    // 4. Ekstraksi File Tanda Tangan
    String ttd = null;
    Dosen kaprodi = perkuliahan.getJurusan() != null ? perkuliahan.getJurusan().getKaprodi() : null;
    if (kaprodi != null) {
        LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
        if (lam != null && lam.getNama() != null && lam.getNama().matches(".*(?i)\\.(jpg|png|jpeg|gif|tif|bmp)$")) {
            ttd = lam.ambilFile().getAbsolutePath();
            parameters.put("ttd_kaprodi", ttd);
        }
    }

    int d = 1;
    for (Dosen dosena : perkuliahan.populateDosenBuNama()) {
        LampiranLain lam = LampiranLain.ambil(dosena.getId(), LampiranLain.TTD_DOSEN);
        if (lam != null && lam.getNama() != null && lam.getNama().matches(".*(?i)\\.(jpg|png|jpeg|gif|tif|bmp)$")) {
            ttd = lam.ambilFile().getAbsolutePath();
            parameters.put("ttd_dosen_" + d, ttd);
        }
        d++;
    }

    if (kaprodi != null) {
        LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
        if (lam != null && lam.getNama() != null && lam.getNama().matches(".*(?i)\\.(jpg|png|jpeg|gif|tif|bmp)$")) {
            ttd = lam.ambilFile().getAbsolutePath();
            parameters.put("ttd_dosen_" + d, ttd);
        }
    }

    // 5. MENGHASILKAN FILE PDF
    String templateNama = formatNilais.size() > 9 || (perkuliahan.getHanyaInputNilaiHuruf() != null && perkuliahan.getHanyaInputNilaiHuruf()) ? "Daftar_Nilai_1" : (formatNilais.size() == 3 ? "Daftar_Nilai" : "Daftar_Nilai_" + formatNilais.size());
    
    File filePdfBaru = Report.generatePDFReport(Report.PDF, parameters, templateNama, ais.ui.util.WaktuUtil.getDate(), maps, Common.locale, null);

    // 6. Enkripsi Path untuk Tautan Aman
    String urlAman = Common.ROOT + "/pdf?p=" + URLEncoder.encode(Common.desEncrypter.get().encrypt(filePdfBaru.getName()), "UTF-8");
    
    resp.put("status", "success");
    resp.put("url", urlAman);

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_cetak_laporan_nilai.jsp:186");
    resp.put("status", "error");
    resp.put("message", "Error: " + e.getMessage());
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(resp.toString());
out.flush();
%>