<%@page import="java.util.*"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.math.RoundingMode"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.obe.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%!
    // =========================================================================
    // HELPER METHODS UNTUK KALKULASI DETAIL OBE MAHASISWA
    // =========================================================================
    private <T> void addMapType(Map<Long, Map<Long, T>> map, Long formatId, Long itemId, T item) {
        Map<Long, T> inner = map.get(formatId);
        if (inner == null) { inner = new HashMap<Long, T>(); map.put(formatId, inner); }
        inner.put(itemId, item);
    }

    private void addNilaiUtama(Map<Long, Map<Long, Map<String, Double>>> data, Long mhsId, Long formatId, String key, Double val) {
        Map<Long, Map<String, Double>> mhsMap = data.get(mhsId);
        if (mhsMap == null) { mhsMap = new HashMap<Long, Map<String, Double>>(); data.put(mhsId, mhsMap); }
        Map<String, Double> fmtMap = mhsMap.get(formatId);
        if (fmtMap == null) { fmtMap = new HashMap<String, Double>(); mhsMap.put(formatId, fmtMap); }
        Double current = fmtMap.get(key) == null ? 0.0 : fmtMap.get(key);
        fmtMap.put(key, current + val);
    }

    private void extractBobot(String jsonStr, Long formatId, String key, Map<Long, Map<String, Double>> dataBobot) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return;
        try {
            JSONObject jObj = new JSONObject(jsonStr);
            if (!jObj.isNull(formatId.toString())) {
                Double bobot = jObj.isNull(formatId.toString() + "_bobot") ? 100.0 : jObj.getDouble(formatId.toString() + "_bobot");
                Map<String, Double> mapB = dataBobot.get(formatId);
                if (mapB == null) { mapB = new HashMap<String, Double>(); dataBobot.put(formatId, mapB); }
                mapB.put(key, bobot);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_penilaian_obe.jsp:43");}
    }

    private <T> Double sumNilai(Collection<T> items, Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData, Long mhsId, Long formatId, String prefix) {
        Double sum = 0.0;
        if(data.get(mhsId) == null || data.get(mhsId).get(formatId) == null) return sum;
        
        for (T item : items) {
            try {
                Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
                String key = prefix + "_" + itemId;
                Double nilia = data.get(mhsId).get(formatId).get(key);
                if(nilia == null) nilia = 0.0;
                
                Double persenData = pData.get(key + "_" + formatId);
                if(persenData == null) persenData = 0.0;
                
                sum += ((persenData * 0.01) * nilia);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_penilaian_obe.jsp:61");}
        }
        return sum;
    }

    private Double recalculateTotalPerFormat(Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData, Long mhsId, Long formatId, Map<Long, PertemuanPunyaUjian> mapd, Map<Long, Pertemuan> mapdTgs, Map<Long, TugasPertemuan> mapdTgsLanjut, Map<Long, TugasKelompok> mapdTgsKelompok) {
        Double total = 0.0;
        if(mapd != null) total += sumNilai(mapd.values(), data, pData, mhsId, formatId, PertemuanPunyaUjian.class.getName());
        if(mapdTgs != null) total += sumNilai(mapdTgs.values(), data, pData, mhsId, formatId, Pertemuan.class.getName());
        if(mapdTgsLanjut != null) total += sumNilai(mapdTgsLanjut.values(), data, pData, mhsId, formatId, TugasPertemuan.class.getName());
        if(mapdTgsKelompok != null) total += sumNilai(mapdTgsKelompok.values(), data, pData, mhsId, formatId, TugasKelompok.class.getName());
        return total;
    }

    public Double hitungKonversi(FormatNilai formatNilai, Double totalNilaiPerformatNilai) {
        if(formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen() == 0.0) return 0.0;
        BigDecimal b15 = new BigDecimal(formatNilai.getPersen() / 100.0);
        BigDecimal bn34 = new BigDecimal(totalNilaiPerformatNilai);
        BigDecimal divisor = (b15.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ONE : b15;
        return bn34.divide(divisor, 2, RoundingMode.HALF_UP).doubleValue();
    }

    private Set<Long> parseIdsToSet(String ids) {
        Set<Long> longs = new HashSet<Long>();
        if (ids != null && !ids.trim().isEmpty()) {
            for (String s : ids.split(",")) {
                if (!s.trim().isEmpty()) {
                    try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_penilaian_obe.jsp:88");}
                }
            }
        }
        return longs;
    }
%>
<%
String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
String idPerkuliahanStr = request.getParameter("perkuliahan");
String idMahasiswaStr = request.getParameter("mahasiswa");

if(idKpmStr == null || idPerkuliahanStr == null || idMahasiswaStr == null) {
    out.print("<div class='alert alert-danger m-3'>" + Common.getBahasaConfig("Parameter data tidak lengkap.") + "</div>");
    return;
}

Session sess = HibernateUtil.openSession();
KurikulumPunyaMatakuliah kpm = null;
Perkuliahan perkuliahan = null;
Mahasiswa mahasiswa = null;
StringBuilder htmlContent = new StringBuilder();

try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
    perkuliahan = (Perkuliahan) sess.get(Perkuliahan.class, Long.parseLong(idPerkuliahanStr));
    mahasiswa = (Mahasiswa) sess.get(Mahasiswa.class, Long.parseLong(idMahasiswaStr));

    if (kpm != null && perkuliahan != null && mahasiswa != null) {
        Matakuliah mk = kpm.getMatakuliah();

        // 1. Ekstrak CPL & PL
        List<CapaianLulusan> capaianLulusans = new ArrayList<CapaianLulusan>();
        List<ProfilLulusan> profilLulusans = new ArrayList<ProfilLulusan>();
        if(mk != null) {
            Set<Long> longs = parseIdsToSet(mk.getCapaianLulusan());
            capaianLulusans = ConstantValues.simpleList(
                    sess.createCriteria(CapaianLulusan.class).add(longs.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longs)), CapaianLulusan.class);

            Set<Long> longsProfile = parseIdsToSet(mk.getProfilLulusan());
            profilLulusans = ConstantValues.simpleList(
                    sess.createCriteria(ProfilLulusan.class).add(longsProfile.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longsProfile)), ProfilLulusan.class);
        }

        // PL mapping Helper (Dari ZK Logic)
        Map<Long, List<ProfilLulusan>> cplToPlMap = new HashMap<Long, List<ProfilLulusan>>();
        for (ProfilLulusan pl : profilLulusans) {
            final String idBaru = pl.getId() + "_" + kpm.getId();
            for (CapaianLulusan cpl : capaianLulusans) {
                String p = cpl.getProfil() != null ? cpl.getProfil() : "";
                if (p.contains("," + pl.getId() + ",") || p.contains("," + idBaru + ",")) {
                    List<ProfilLulusan> pls = cplToPlMap.get(cpl.getId());
                    if (pls == null) { pls = new ArrayList<ProfilLulusan>(); cplToPlMap.put(cpl.getId(), pls); }
                    pls.add(pl);
                }
            }
        }

        // 2. Ekstrak Data Pertemuan
        List<PertemuanPunyaUjian> pertemuanPunyaUjians = ConstantValues.simpleList(
            sess.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "pertemuan").createAlias("ujian", "ujian").add(Restrictions.eq("ujian.aktif", true)).add(Restrictions.eq("pertemuan.perkuliahan", perkuliahan)), PertemuanPunyaUjian.class);
        List<Pertemuan> pertemuansTugas = ConstantValues.simpleList(
            sess.createCriteria(Pertemuan.class).add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas")).add(Restrictions.eq("aktif", true)).add(Restrictions.eq("perkuliahan", perkuliahan)), Pertemuan.class);
        Collection<Long> pertemuansList = perkuliahan.ambilPertemuan().values();
        List<TugasPertemuan> pertemuansTugasLanjut = pertemuansList.isEmpty() ? new ArrayList<TugasPertemuan>() : ConstantValues.simpleList(
            sess.createCriteria(TugasPertemuan.class).add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas")).add(Restrictions.in("pertemuan", pertemuansList)), TugasPertemuan.class);
        List<TugasKelompok> pertemuansTugasKelompoks = ConstantValues.simpleList(
            sess.createCriteria(TugasKelompok.class).add(Restrictions.ne("judul", "")).add(Restrictions.isNotNull("judul")).add(Restrictions.eq("perkuliahan", perkuliahan)), TugasKelompok.class);

        // 3. Ekstrak Format Nilai & Bobot
        List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan);
        Map<Long, Map<Long, Map<String, Double>>> dataNiliasUtama = new HashMap<Long, Map<Long, Map<String, Double>>>();
        Map<Long, Map<String, Double>> dataBobot = new HashMap<Long, Map<String, Double>>();
        Map<Long, Map<Long, PertemuanPunyaUjian>> mapPertemuanPunyaUjian = new HashMap<Long, Map<Long, PertemuanPunyaUjian>>();
        Map<Long, Map<Long, Pertemuan>> mapTugas = new HashMap<Long, Map<Long, Pertemuan>>();
        Map<Long, Map<Long, TugasPertemuan>> mapTugasLanjut = new HashMap<Long, Map<Long, TugasPertemuan>>();
        Map<Long, Map<Long, TugasKelompok>> mapTugasKelompok = new HashMap<Long, Map<Long, TugasKelompok>>();
        Map<String, String> mapHasilObe = new HashMap<String, String>();

        if (!pertemuanPunyaUjians.isEmpty()) {
            List<HasilUjianMahasiswa> listHasil = ConstantValues.simpleList(
                sess.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.or(Restrictions.isNotNull("keyhasil"), Restrictions.isNotNull("nilaiObe"))).add(Restrictions.in("pertemuanPunyaUjian", pertemuanPunyaUjians)).add(Restrictions.eq("mahasiswa", mahasiswa)), HasilUjianMahasiswa.class);
            for (HasilUjianMahasiswa hum : listHasil) if (hum.getNilaiObe() != null) mapHasilObe.put(hum.getPertemuanPunyaUjian().getId() + "_" + hum.getMahasiswa().getId(), hum.getNilaiObe());
        }

        // Parsing Nilai Utama Mhs
        for (Pertemuan p : pertemuansTugas) {
            if (p.getKeteranganNilai() != null && !p.getKeteranganNilai().trim().isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(p.getKeteranganNilai());
                    for (FormatNilai fn : formatNilais) {
                        String keyJson = mahasiswa.getId() + "_mhs_nilai_" + fn.getId();
                        if (!jsonObject.isNull(keyJson)) {
                            addNilaiUtama(dataNiliasUtama, mahasiswa.getId(), fn.getId(), Pertemuan.class.getName() + "_" + p.getId(), jsonObject.getDouble(keyJson));
                            addMapType(mapTugas, fn.getId(), p.getId(), p);
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_penilaian_obe.jsp:185");}
            }
        }
        for (TugasPertemuan tp : pertemuansTugasLanjut) {
            if (tp.getKeteranganNilai() != null && !tp.getKeteranganNilai().trim().isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(tp.getKeteranganNilai());
                    for (FormatNilai fn : formatNilais) {
                        String keyJson = mahasiswa.getId() + "_mhs_nilai_" + fn.getId();
                        if (!jsonObject.isNull(keyJson)) {
                            addNilaiUtama(dataNiliasUtama, mahasiswa.getId(), fn.getId(), TugasPertemuan.class.getName() + "_" + tp.getId(), jsonObject.getDouble(keyJson));
                            addMapType(mapTugasLanjut, fn.getId(), tp.getId(), tp);
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_penilaian_obe.jsp:199");}
            }
        }
        for (TugasKelompok tk : pertemuansTugasKelompoks) {
            if (tk.getKeteranganNilai() != null && !tk.getKeteranganNilai().trim().isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(tk.getKeteranganNilai());
                    for (FormatNilai fn : formatNilais) {
                        String keyJson = mahasiswa.getId() + "_mhs_nilai_" + fn.getId();
                        if (!jsonObject.isNull(keyJson)) {
                            addNilaiUtama(dataNiliasUtama, mahasiswa.getId(), fn.getId(), TugasKelompok.class.getName() + "_" + tk.getId(), jsonObject.getDouble(keyJson));
                            addMapType(mapTugasKelompok, fn.getId(), tk.getId(), tk);
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_penilaian_obe.jsp:213");}
            }
        }
        for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
            String nilaiObe = mapHasilObe.get(ppu.getId() + "_" + mahasiswa.getId());
            if (nilaiObe != null && !nilaiObe.trim().isEmpty()) {
                try {
                    JSONObject jsonObjectHasil = new JSONObject(nilaiObe);
                    for (FormatNilai fn : formatNilais) {
                        if (!jsonObjectHasil.isNull(fn.getId().toString())) {
                            addMapType(mapPertemuanPunyaUjian, fn.getId(), ppu.getId(), ppu);
                            Double nilaiSkor = jsonObjectHasil.getDouble(fn.getId().toString());
                            Double nilaiMax = jsonObjectHasil.isNull(fn.getId() + "_max") ? 0.0 : jsonObjectHasil.getDouble(fn.getId() + "_max");
                            Double nilaiDidapat = nilaiMax.equals(0.0) ? 0.0 : (nilaiSkor * 100.0) / nilaiMax;
                            addNilaiUtama(dataNiliasUtama, mahasiswa.getId(), fn.getId(), PertemuanPunyaUjian.class.getName() + "_" + ppu.getId(), nilaiDidapat);
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_penilaian_obe.jsp:230");}
            }
        }

        // Parsing Bobot Internal Format Nilai
        for (FormatNilai formatNilai : formatNilais) {
            Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(formatNilai.getId());
            Map<Long, Pertemuan> mapdTgs = mapTugas.get(formatNilai.getId());
            Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(formatNilai.getId());
            Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(formatNilai.getId());

            if (mapd != null) for (PertemuanPunyaUjian ppu : mapd.values()) extractBobot(ppu.getFormatNilais(), formatNilai.getId(), PertemuanPunyaUjian.class.getName() + "_" + ppu.getId(), dataBobot);
            if (mapdTgs != null) for (Pertemuan pt : mapdTgs.values()) extractBobot(pt.getFormatNilais(), formatNilai.getId(), Pertemuan.class.getName() + "_" + pt.getId(), dataBobot);
            if (mapdTgsLanjut != null) for (TugasPertemuan tp : mapdTgsLanjut.values()) extractBobot(tp.getFormatNilais(), formatNilai.getId(), TugasPertemuan.class.getName() + "_" + tp.getId(), dataBobot);
            if (mapdTgsKelompok != null) for (TugasKelompok tk : mapdTgsKelompok.values()) extractBobot(tk.getFormatNilais(), formatNilai.getId(), TugasKelompok.class.getName() + "_" + tk.getId(), dataBobot);
        }

        // Hitung Persentase Riil berbanding Bobot Utama
        Map<String, Double> persensData = new HashMap<String, Double>();
        for (FormatNilai formatNilai : formatNilais) {
            Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(formatNilai.getId());
            Map<Long, Pertemuan> mapdTgs = mapTugas.get(formatNilai.getId());
            Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(formatNilai.getId());
            Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(formatNilai.getId());

            if (mapd != null || mapdTgs != null || mapdTgsLanjut != null || mapdTgsKelompok != null) {
                class SubHeadFiller {
                    void fill(String keyPrefix, Long itemId) {
                        Double persen = 0.0;
                        Map<String, Double> mapB = dataBobot.get(formatNilai.getId());
                        if (mapB != null) {
                            Double totalB = 0.0, nilaiB = 0.0;
                            for (String keyd : mapB.keySet()) { 
                                Double d = mapB.get(keyd); 
                                if (keyd.equalsIgnoreCase(keyPrefix + "_" + itemId)) nilaiB += d; 
                                totalB += d; 
                            }
                            if(totalB > 0) persen = (nilaiB * 100.0) / totalB;
                            persen = (persen / 100.0) * (formatNilai.getPersen() != null ? formatNilai.getPersen() : 0.0);
                            persensData.put(keyPrefix + "_" + itemId + "_" + formatNilai.getId(), persen);
                        }
                    }
                }
                SubHeadFiller filler = new SubHeadFiller();
                if (mapd != null) for (PertemuanPunyaUjian ppu : mapd.values()) filler.fill(PertemuanPunyaUjian.class.getName(), ppu.getId());
                if (mapdTgs != null) for (Pertemuan pt : mapdTgs.values()) filler.fill(Pertemuan.class.getName(), pt.getId());
                if (mapdTgsLanjut != null) for (TugasPertemuan tp : mapdTgsLanjut.values()) filler.fill(TugasPertemuan.class.getName(), tp.getId());
                if (mapdTgsKelompok != null) for (TugasKelompok tk : mapdTgsKelompok.values()) filler.fill(TugasKelompok.class.getName(), tk.getId());
            }
        }

        // ========================================================================
        // TABEL DETAIL HTML 
        // ========================================================================
        htmlContent.append("<div class='mb-2'>");
        htmlContent.append("<h5 class='fw-bold text-primary mb-1'><i class='fas fa-user-graduate me-2'></i>").append(mk.getKode()).append(" - ").append(mk.getNama()).append("</h5>");
        htmlContent.append("<h6 class='fw-bold text-secondary mb-3'>").append(Common.getBahasaConfig("Mahasiswa:")).append(" <span class='text-dark'>").append(mahasiswa.getNama()).append(" (").append(mahasiswa.getNim()).append(")</span></h6>");
        
        htmlContent.append("<div class='table-responsive border rounded shadow-sm' style='max-height: 70vh;'>");
        htmlContent.append("<table class='table table-bordered table-hover table-sm text-center align-middle mb-0' style='font-size: 0.85rem; min-width: 1000px;'>");
        htmlContent.append("<thead class='table-light'>");
        htmlContent.append("<tr>");
        htmlContent.append("<th class='align-middle'>PL</th>");
        htmlContent.append("<th class='align-middle'>CPL</th>");
        htmlContent.append("<th class='align-middle'>Minggu</th>");
        htmlContent.append("<th class='align-middle'>CPMK</th>");
        htmlContent.append("<th class='align-middle'>Sub CPMK</th>");
        htmlContent.append("<th class='align-middle'>Indikator Penilaian</th>");
        htmlContent.append("<th class='align-middle'>Teknik</th>");
        htmlContent.append("<th class='align-middle'>Bobot CPMK</th>");
        htmlContent.append("<th class='align-middle'>Nilai Mhs</th>");
        htmlContent.append("<th class='align-middle'>Nilai/Teknik (bobot x nilai)</th>");
        htmlContent.append("<th class='align-middle'>Nilai Bobot tiap CPMK</th>");
        htmlContent.append("<th class='align-middle'>Ketercapaian Nilai CPL</th>");
        htmlContent.append("<th class='align-middle'>Ketercapaian CPL pada MK (%)</th>");
        htmlContent.append("<th class='align-middle'>Keterangan</th>");
        htmlContent.append("</tr>");
        htmlContent.append("</thead><tbody>");

        if (formatNilais.isEmpty()) {
            htmlContent.append("<tr><td colspan='14' class='text-muted py-5'><i class='fas fa-info-circle me-2'></i>").append(Common.getBahasaConfig("Belum ada format penilaian yang tersedia untuk kelas ini.")).append("</td></tr>");
        } else {
            // Group Data for calculation
            Map<Long, Double> totalCpmkScores = new HashMap<Long, Double>();
            Map<Long, Double> totalCplScores = new HashMap<Long, Double>();
            Map<Long, Double> totalCplWeights = new HashMap<Long, Double>();

            for (FormatNilai fn : formatNilais) {
                Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(fn.getId());
                Map<Long, Pertemuan> mapdTgs = mapTugas.get(fn.getId());
                Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(fn.getId());
                Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(fn.getId());
                
                Double rawTotal = recalculateTotalPerFormat(dataNiliasUtama, persensData, mahasiswa.getId(), fn.getId(), mapd, mapdTgs, mapdTgsLanjut, mapdTgsKelompok);
                Double valKonversi = hitungKonversi(fn, rawTotal); 

                if (fn.getCapaianPembelajaranLulusan() != null) {
                    Long cpmkId = fn.getCapaianPembelajaranLulusan().getId();
                    Double currentCpmkScore = totalCpmkScores.get(cpmkId) == null ? 0.0 : totalCpmkScores.get(cpmkId);
                    totalCpmkScores.put(cpmkId, currentCpmkScore + valKonversi);

                    for (CapaianLulusan cpl : capaianLulusans) {
                        if (cpl.getCapaianPembelajaranLulusan() != null && cpl.getCapaianPembelajaranLulusan().contains("," + cpmkId + ",")) {
                            Double currentCplScore = totalCplScores.get(cpl.getId()) == null ? 0.0 : totalCplScores.get(cpl.getId());
                            Double currentCplWeight = totalCplWeights.get(cpl.getId()) == null ? 0.0 : totalCplWeights.get(cpl.getId());
                            totalCplScores.put(cpl.getId(), currentCplScore + (valKonversi * (fn.getPersen() != null ? fn.getPersen() : 0.0)));
                            totalCplWeights.put(cpl.getId(), currentCplWeight + (fn.getPersen() != null ? fn.getPersen() : 0.0));
                        }
                    }
                }
            }

            // Render Rows Per Format Nilai (Komponen Penilaian)
            for (FormatNilai fn : formatNilais) {
                Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(fn.getId());
                Map<Long, Pertemuan> mapdTgs = mapTugas.get(fn.getId());
                Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(fn.getId());
                Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(fn.getId());

                Double rawTotal = recalculateTotalPerFormat(dataNiliasUtama, persensData, mahasiswa.getId(), fn.getId(), mapd, mapdTgs, mapdTgsLanjut, mapdTgsKelompok);
                Double valKonversi = hitungKonversi(fn, rawTotal);
                Double bobotDec = (fn.getPersen() != null ? fn.getPersen() : 0.0) / 100.0;

                String plStr = "-";
                String cplStr = "-";
                String cpmkStr = fn.getCapaianPembelajaranLulusan() != null ? fn.getCapaianPembelajaranLulusan().getKode() : "-";
                
                Double nilaiBobotCpmk = 0.0;
                Double ketNilaiCpl = 0.0;
                Double ketCplPercent = 0.0;
                
                if (fn.getCapaianPembelajaranLulusan() != null) {
                    Long cpmkId = fn.getCapaianPembelajaranLulusan().getId();
                    nilaiBobotCpmk = totalCpmkScores.get(cpmkId) == null ? 0.0 : totalCpmkScores.get(cpmkId);

                    for (CapaianLulusan cpl : capaianLulusans) {
                        if (cpl.getCapaianPembelajaranLulusan() != null && cpl.getCapaianPembelajaranLulusan().contains("," + cpmkId + ",")) {
                            cplStr = cpl.getKode();
                            Double sumW = totalCplWeights.get(cpl.getId());
                            Double sumS = totalCplScores.get(cpl.getId());
                            ketNilaiCpl = sumS != null ? sumS : 0.0;
                            ketCplPercent = (sumW != null && sumW > 0) ? (sumS / sumW) : 0.0;

                            List<ProfilLulusan> pls = cplToPlMap.get(cpl.getId());
                            if(pls != null && !pls.isEmpty()) {
                                StringBuilder sbPl = new StringBuilder();
                                for(int i=0; i<pls.size(); i++) {
                                    sbPl.append(pls.get(i).getKode());
                                    if(i < pls.size()-1) sbPl.append(", ");
                                }
                                plStr = sbPl.toString();
                            }
                            break; 
                        }
                    }
                }

                htmlContent.append("<tr>");
                htmlContent.append("<td>").append(plStr).append("</td>");
                htmlContent.append("<td><span class='badge bg-secondary'>").append(cplStr).append("</span></td>");
                htmlContent.append("<td class='text-muted'>-</td>"); 
                htmlContent.append("<td class='fw-bold text-dark'>").append(cpmkStr).append("</td>");
                htmlContent.append("<td class='text-muted'>-</td>"); 
                htmlContent.append("<td class='text-muted'>-</td>"); 
                htmlContent.append("<td>").append(fn.getNama()).append("</td>");
                htmlContent.append("<td>").append(Common.numberFormat.get().format(bobotDec)).append("</td>");
                htmlContent.append("<td class='fw-bold text-dark'>").append(Common.numberFormat.get().format(rawTotal)).append("</td>");
                htmlContent.append("<td class='fw-bold text-primary bg-light'>").append(Common.numberFormat.get().format(valKonversi)).append("</td>");
                htmlContent.append("<td>").append(Common.numberFormat.get().format(nilaiBobotCpmk)).append("</td>");
                htmlContent.append("<td>").append(Common.numberFormat.get().format(ketNilaiCpl)).append("</td>");
                
                String cssCplPct = ketCplPercent >= 60.0 ? "text-success" : "text-danger";
                htmlContent.append("<td class='fw-bold ").append(cssCplPct).append("'>").append(Common.numberFormat.get().format(ketCplPercent)).append("</td>");
                htmlContent.append("<td>").append(ketCplPercent >= 60.0 ? "<span class='badge bg-success'>" + Common.getBahasaConfig("Lulus") + "</span>" : "<span class='badge bg-danger'>" + Common.getBahasaConfig("Remedial") + "</span>").append("</td>");
                htmlContent.append("</tr>");
            }
        }
        
        htmlContent.append("</tbody></table></div></div>");
        
        // Export Excel Button
        htmlContent.append("<div class='d-flex justify-content-end mt-3'>");
        htmlContent.append("<button type='button' class='btn btn-outline-success rounded-pill fw-bold px-4' onclick='if(typeof downloadExcelRincianMhs === \"function\") downloadExcelRincianMhs()'>");
        htmlContent.append("<i class='fas fa-file-excel me-2'></i>").append(Common.getBahasaConfig("Unduh Excel"));
        htmlContent.append("</button>");
        htmlContent.append("</div>");
        
        htmlContent.append("<script>");
        htmlContent.append("function downloadExcelRincianMhs() {");
        htmlContent.append("  try {");
        htmlContent.append("    if(typeof XLSX !== 'undefined') {");
        htmlContent.append("      const table = document.querySelector('.table-responsive table');");
        htmlContent.append("      const wb = XLSX.utils.book_new();");
        htmlContent.append("      XLSX.utils.book_append_sheet(wb, XLSX.utils.table_to_sheet(table), 'Detail_Mhs');");
        htmlContent.append("      XLSX.writeFile(wb, 'Detail_OBE_").append(mahasiswa.getNim()).append(".xlsx');");
        htmlContent.append("    } else {");
        htmlContent.append("      if(typeof tampilkanToast === 'function') tampilkanToast('Library Excel belum dimuat.', 'bg-warning');");
        htmlContent.append("    }");
        htmlContent.append("  } catch(e) { console.error(e); }");
        htmlContent.append("}");
        htmlContent.append("</script>");

    }

    out.print(htmlContent.toString());

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_penilaian_obe.jsp:437");
    out.print("<div class='alert alert-danger m-3'>" + Common.getBahasaConfig("Terjadi kesalahan saat memuat rincian OBE.") + "</div>");
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_penilaian_obe.jsp:440");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
}
%>
