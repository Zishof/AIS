<%@page import="java.util.*"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.math.RoundingMode"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.obe.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%!
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
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_gabungan_service.jsp:41");}
    }

    private <T> Double sumNilai(Collection<T> items, Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData, Long mhsId, Long formatId, String prefix) {
        Double sum = 0.0;
        if (data.get(mhsId) == null || data.get(mhsId).get(formatId) == null) return sum;
        for (T item : items) {
            try {
                Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
                String key = prefix + "_" + itemId;
                Double nilia = data.get(mhsId).get(formatId).get(key);
                if(nilia == null) nilia = 0.0;
                Double persenData = pData.get(key + "_" + formatId);
                if(persenData == null) persenData = 0.0;
                sum += ((persenData * 0.01) * nilia);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_gabungan_service.jsp:56");}
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

    private Double hitungKonversi(FormatNilai formatNilai, Double totalNilaiPerformatNilai) {
        if (formatNilai == null || formatNilai.getPersen() == null || formatNilai.getPersen() == 0.0) return 0.0;
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
                    try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_gabungan_service.jsp:83");}
                }
            }
        }
        return longs;
    }
%>
<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String idMahasiswaStr = request.getParameter("mahasiswa");

if (idMahasiswaStr == null || idMahasiswaStr.trim().isEmpty()) {
    out.print("<div class='alert alert-warning m-3'>" + Common.getBahasaConfig("Permintaan data mahasiswa tidak sah.") + "</div>");
    return;
}

Session sess = HibernateUtil.openSession();
try {
    Mahasiswa mahasiswa = (Mahasiswa) sess.get(Mahasiswa.class, Long.parseLong(idMahasiswaStr));
    if (mahasiswa == null) {
        out.print("<div class='alert alert-danger m-3'>" + Common.getBahasaConfig("Data mahasiswa tidak ditemukan.") + "</div>");
        return;
    }

    // CARI SEMUA PERKULIAHAN YANG DIAMBIL MHS
    List<Detailperkuliahan> allDps = ConstantValues.simpleList(
        sess.createCriteria(Detailperkuliahan.class).createAlias("perkuliahan", "p").add(Restrictions.eq("mahasiswa", mahasiswa)), 
        Detailperkuliahan.class
    );

    // VARIABEL PENAMPUNG HTML TABEL
    StringBuilder rowSubCpmk = new StringBuilder();
    StringBuilder rowCpmk = new StringBuilder();
    StringBuilder rowCpl = new StringBuilder();
    StringBuilder rowPl = new StringBuilder();

    // VARIABEL PENAMPUNG AGREGASI UNTUK CHART KESELURUHAN (RATA-RATA DARI SELURUH MK)
    Map<String, Double> mapAvgSubCpmkClass = new TreeMap<String, Double>();
    Map<String, Integer> mapCountSubCpmkClass = new TreeMap<String, Integer>();
    
    Map<String, Double> mapAvgCpmkClass = new TreeMap<String, Double>();
    Map<String, Integer> mapCountCpmkClass = new TreeMap<String, Integer>();

    Map<String, Double> mapAvgCplClass = new TreeMap<String, Double>();
    Map<String, Integer> mapCountCplClass = new TreeMap<String, Integer>();
    
    Map<String, Double> mapAvgPlClass = new TreeMap<String, Double>();
    Map<String, Integer> mapCountPlClass = new TreeMap<String, Integer>();

    int idxSub = 1;
    int idxCpmk = 1;
    int idxCpl = 1;
    int idxPl = 1;

    // ITERASI SETIAP MATAKULIAH
    for (Detailperkuliahan dp : allDps) {
        Perkuliahan perkuliahan = dp.getPerkuliahan();
        if (perkuliahan == null) continue;
        KurikulumPunyaMatakuliah kpm = perkuliahan.getKurikulumPunyaMatakuliah();
        if (kpm == null) continue;
        Matakuliah mk = kpm.getMatakuliah();
        if (mk == null) continue;

        boolean isNilaiCpmk = kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk();
        String kodeMk = mk.getKode();
        String namaMk = mk.getNama();

        // Ekstrak CPL & PL
        Set<Long> longsCpl = parseIdsToSet(mk.getCapaianLulusan());
        List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(sess.createCriteria(CapaianLulusan.class).add(longsCpl.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longsCpl)).addOrder(Order.asc("kode")), CapaianLulusan.class);
        Set<Long> longsPl = parseIdsToSet(mk.getProfilLulusan());
        List<ProfilLulusan> profilLulusans = ConstantValues.simpleList(sess.createCriteria(ProfilLulusan.class).add(longsPl.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longsPl)).addOrder(Order.asc("kode")), ProfilLulusan.class);

        Map<Long, List<CapaianLulusan>> plToCplMap = new HashMap<Long, List<CapaianLulusan>>();
        for (ProfilLulusan pl : profilLulusans) {
            String idBaru = pl.getId() + "_" + kpm.getId();
            for (CapaianLulusan cpl : capaianLulusans) {
                String p = cpl.getProfil() != null ? cpl.getProfil() : "";
                if (p.contains("," + pl.getId() + ",") || p.contains("," + idBaru + ",")) {
                    List<CapaianLulusan> cpls = plToCplMap.get(pl.getId());
                    if (cpls == null) { cpls = new ArrayList<CapaianLulusan>(); plToCplMap.put(pl.getId(), cpls); }
                    cpls.add(cpl);
                }
            }
        }

        // Ekstrak Format Nilai & Pertemuan
        List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan);
        List<PertemuanPunyaUjian> pertemuanPunyaUjians = ConstantValues.simpleList(sess.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "p").createAlias("ujian", "u").add(Restrictions.eq("u.aktif", true)).add(Restrictions.eq("p.perkuliahan", perkuliahan)), PertemuanPunyaUjian.class);
        List<Pertemuan> pertemuansTugas = ConstantValues.simpleList(sess.createCriteria(Pertemuan.class).add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas")).add(Restrictions.eq("aktif", true)).add(Restrictions.eq("perkuliahan", perkuliahan)), Pertemuan.class);
        Collection<Long> pertemuansList = perkuliahan.ambilPertemuan().values();
        List<TugasPertemuan> pertemuansTugasLanjut = pertemuansList.isEmpty() ? new ArrayList<TugasPertemuan>() : ConstantValues.simpleList(sess.createCriteria(TugasPertemuan.class).add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas")).add(Restrictions.in("pertemuan", pertemuansList)), TugasPertemuan.class);
        List<TugasKelompok> pertemuansTugasKelompoks = ConstantValues.simpleList(sess.createCriteria(TugasKelompok.class).add(Restrictions.ne("judul", "")).add(Restrictions.isNotNull("judul")).add(Restrictions.eq("perkuliahan", perkuliahan)), TugasKelompok.class);

        // Filter Cpmk
        List<CapaianPembelajaranLulusan> listCpmkUnique = new ArrayList<CapaianPembelajaranLulusan>();
        Map<Long, List<FormatNilai>> mapCpmkIdToFns = new HashMap<Long, List<FormatNilai>>();
        if (!isNilaiCpmk) {
            Map<Long, CapaianPembelajaranLulusan> mapTempCpmk = new HashMap<Long, CapaianPembelajaranLulusan>();
            for (FormatNilai fn : formatNilais) {
                CapaianPembelajaranLulusan cpmkObj = fn.getCapaianPembelajaranLulusan();
                if (cpmkObj != null) {
                    mapTempCpmk.put(cpmkObj.getId(), cpmkObj);
                    List<FormatNilai> fns = mapCpmkIdToFns.get(cpmkObj.getId());
                    if (fns == null) { fns = new ArrayList<FormatNilai>(); mapCpmkIdToFns.put(cpmkObj.getId(), fns); }
                    fns.add(fn);
                }
            }
            listCpmkUnique.addAll(mapTempCpmk.values());
        }

        // Kalkulasi Dasar
        Map<Long, Map<Long, Map<String, Double>>> dataNiliasUtama = new HashMap<Long, Map<Long, Map<String, Double>>>();
        Map<Long, Map<String, Double>> dataBobot = new HashMap<Long, Map<String, Double>>();
        Map<Long, Map<Long, PertemuanPunyaUjian>> mapPertemuanPunyaUjian = new HashMap<Long, Map<Long, PertemuanPunyaUjian>>();
        Map<Long, Map<Long, Pertemuan>> mapTugas = new HashMap<Long, Map<Long, Pertemuan>>();
        Map<Long, Map<Long, TugasPertemuan>> mapTugasLanjut = new HashMap<Long, Map<Long, TugasPertemuan>>();
        Map<Long, Map<Long, TugasKelompok>> mapTugasKelompok = new HashMap<Long, Map<Long, TugasKelompok>>();
        Map<String, String> mapHasilObe = new HashMap<String, String>();

        if (!pertemuanPunyaUjians.isEmpty()) {
            List<HasilUjianMahasiswa> listHasil = ConstantValues.simpleList(sess.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil")).add(Restrictions.in("pertemuanPunyaUjian", pertemuanPunyaUjians)).add(Restrictions.eq("mahasiswa", mahasiswa)), HasilUjianMahasiswa.class);
            for (HasilUjianMahasiswa hum : listHasil) if (hum.getNilaiObe() != null) mapHasilObe.put(hum.getPertemuanPunyaUjian().getId() + "_" + hum.getMahasiswa().getId(), hum.getNilaiObe());
        }

        for (Pertemuan p : pertemuansTugas) {
            if (p.getKeteranganNilai() != null && !p.getKeteranganNilai().trim().isEmpty()) {
                try {
                    JSONObject jObj = new JSONObject(p.getKeteranganNilai());
                    for (FormatNilai fn : formatNilais) {
                        String key = mahasiswa.getId() + "_mhs_nilai_" + fn.getId();
                        if (!jObj.isNull(key)) { addNilaiUtama(dataNiliasUtama, mahasiswa.getId(), fn.getId(), Pertemuan.class.getName() + "_" + p.getId(), jObj.getDouble(key)); addMapType(mapTugas, fn.getId(), p.getId(), p); }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_gabungan_service.jsp:216");}
            }
        }
        for (TugasPertemuan tp : pertemuansTugasLanjut) {
            if (tp.getKeteranganNilai() != null && !tp.getKeteranganNilai().trim().isEmpty()) {
                try {
                    JSONObject jObj = new JSONObject(tp.getKeteranganNilai());
                    for (FormatNilai fn : formatNilais) {
                        String key = mahasiswa.getId() + "_mhs_nilai_" + fn.getId();
                        if (!jObj.isNull(key)) { addNilaiUtama(dataNiliasUtama, mahasiswa.getId(), fn.getId(), TugasPertemuan.class.getName() + "_" + tp.getId(), jObj.getDouble(key)); addMapType(mapTugasLanjut, fn.getId(), tp.getId(), tp); }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_gabungan_service.jsp:227");}
            }
        }
        for (TugasKelompok tk : pertemuansTugasKelompoks) {
            if (tk.getKeteranganNilai() != null && !tk.getKeteranganNilai().trim().isEmpty()) {
                try {
                    JSONObject jObj = new JSONObject(tk.getKeteranganNilai());
                    for (FormatNilai fn : formatNilais) {
                        String key = mahasiswa.getId() + "_mhs_nilai_" + fn.getId();
                        if (!jObj.isNull(key)) { addNilaiUtama(dataNiliasUtama, mahasiswa.getId(), fn.getId(), TugasKelompok.class.getName() + "_" + tk.getId(), jObj.getDouble(key)); addMapType(mapTugasKelompok, fn.getId(), tk.getId(), tk); }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_gabungan_service.jsp:238");}
            }
        }
        for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
            String nilaiObe = mapHasilObe.get(ppu.getId() + "_" + mahasiswa.getId());
            if (nilaiObe != null && !nilaiObe.trim().isEmpty()) {
                try {
                    JSONObject jObj = new JSONObject(nilaiObe);
                    for (FormatNilai fn : formatNilais) {
                        if (!jObj.isNull(fn.getId().toString())) {
                            addMapType(mapPertemuanPunyaUjian, fn.getId(), ppu.getId(), ppu);
                            Double sk = jObj.getDouble(fn.getId().toString());
                            Double max = jObj.isNull(fn.getId() + "_max") ? 0.0 : jObj.getDouble(fn.getId() + "_max");
                            addNilaiUtama(dataNiliasUtama, mahasiswa.getId(), fn.getId(), PertemuanPunyaUjian.class.getName() + "_" + ppu.getId(), max.equals(0.0) ? 0.0 : (sk * 100.0) / max);
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_gabungan_service.jsp:254");}
            }
        }

        for (FormatNilai fn : formatNilais) {
            Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(fn.getId());
            Map<Long, Pertemuan> mapdTgs = mapTugas.get(fn.getId());
            Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(fn.getId());
            Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(fn.getId());

            if (mapd != null) for (PertemuanPunyaUjian ppu : mapd.values()) extractBobot(ppu.getFormatNilais(), fn.getId(), PertemuanPunyaUjian.class.getName() + "_" + ppu.getId(), dataBobot);
            if (mapdTgs != null) for (Pertemuan pt : mapdTgs.values()) extractBobot(pt.getFormatNilais(), fn.getId(), Pertemuan.class.getName() + "_" + pt.getId(), dataBobot);
            if (mapdTgsLanjut != null) for (TugasPertemuan tp : mapdTgsLanjut.values()) extractBobot(tp.getFormatNilais(), fn.getId(), TugasPertemuan.class.getName() + "_" + tp.getId(), dataBobot);
            if (mapdTgsKelompok != null) for (TugasKelompok tk : mapdTgsKelompok.values()) extractBobot(tk.getFormatNilais(), fn.getId(), TugasKelompok.class.getName() + "_" + tk.getId(), dataBobot);
        }

        Map<String, Double> persensData = new HashMap<String, Double>();
        for (FormatNilai fn : formatNilais) {
            Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(fn.getId());
            Map<Long, Pertemuan> mapdTgs = mapTugas.get(fn.getId());
            Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(fn.getId());
            Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(fn.getId());

            if (mapd != null || mapdTgs != null || mapdTgsLanjut != null || mapdTgsKelompok != null) {
                class SubHeadFiller {
                    void fill(String keyPrefix, Long itemId) {
                        Double persen = 0.0;
                        Map<String, Double> mapB = dataBobot.get(fn.getId());
                        if (mapB != null) {
                            Double totalB = 0.0, nilaiB = 0.0;
                            for (String keyd : mapB.keySet()) { Double d = mapB.get(keyd); if (keyd.equalsIgnoreCase(keyPrefix + "_" + itemId)) nilaiB += d; totalB += d; }
                            if(totalB > 0) persen = (nilaiB * 100.0) / totalB;
                            persen = (persen / 100.0) * (fn.getPersen() != null ? fn.getPersen() : 0.0);
                            persensData.put(keyPrefix + "_" + itemId + "_" + fn.getId(), persen);
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

        // Kumpulkan & Agregasi Nilai
        Map<Long, Double> nilaiFormatMhs = new HashMap<Long, Double>();
        for (FormatNilai fn : formatNilais) {
            Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(fn.getId());
            Map<Long, Pertemuan> mapdTgs = mapTugas.get(fn.getId());
            Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(fn.getId());
            Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(fn.getId());
            Double rawTotal = recalculateTotalPerFormat(dataNiliasUtama, persensData, mahasiswa.getId(), fn.getId(), mapd, mapdTgs, mapdTgsLanjut, mapdTgsKelompok);
            Double valKonversi = hitungKonversi(fn, rawTotal); 
            nilaiFormatMhs.put(fn.getId(), valKonversi);

            if (!isNilaiCpmk && valKonversi > 0) {
                Double nMin = fn.ambilMinimal() != null ? fn.ambilMinimal() : 60.0;
                boolean isLulus = valKonversi >= nMin;
                rowSubCpmk.append("<tr>")
                    .append("<td class='text-center'>").append(idxSub++).append("</td>")
                    .append("<td class='text-center fw-bold text-primary'>").append(fn.getNama()).append("</td>")
                    .append("<td>").append(fn.getNama()).append("</td>")
                    .append("<td class='text-center'>").append(kodeMk).append("</td>")
                    .append("<td>").append(namaMk).append("</td>")
                    .append("<td class='text-center'>").append(nMin).append("</td>")
                    .append("<td class='text-center fw-bold'>").append(Common.numberFormat.get().format(valKonversi)).append("</td>")
                    .append("<td class='text-center ").append(isLulus ? "status-tercapai" : "status-gagal").append("'>").append(isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")).append("</td>")
                    .append("</tr>");
                
                // Tambahkan ke Chart Array Sub-CPMK
                mapAvgSubCpmkClass.put(fn.getNama(), (mapAvgSubCpmkClass.get(fn.getNama()) == null ? 0.0 : mapAvgSubCpmkClass.get(fn.getNama())) + valKonversi);
                mapCountSubCpmkClass.put(fn.getNama(), (mapCountSubCpmkClass.get(fn.getNama()) == null ? 0 : mapCountSubCpmkClass.get(fn.getNama())) + 1);
            }
        }

        Map<Long, Double> totalCpmkScores = new HashMap<Long, Double>();
        if (isNilaiCpmk) {
            for (FormatNilai fn : formatNilais) {
                Double nVal = nilaiFormatMhs.get(fn.getId()) != null ? nilaiFormatMhs.get(fn.getId()) : 0.0;
                if (nVal > 0) {
                    Double nMin = fn.ambilMinimal() != null ? fn.ambilMinimal() : 60.0;
                    boolean isLulus = nVal >= nMin;
                    rowCpmk.append("<tr>")
                        .append("<td class='text-center'>").append(idxCpmk++).append("</td>")
                        .append("<td class='text-center fw-bold text-primary'>").append(fn.getNama()).append("</td>")
                        .append("<td>").append(fn.getNama()).append("</td>")
                        .append("<td class='text-center'>").append(kodeMk).append("</td>")
                        .append("<td>").append(namaMk).append("</td>")
                        .append("<td class='text-center'>").append(nMin).append("</td>")
                        .append("<td class='text-center fw-bold'>").append(Common.numberFormat.get().format(nVal)).append("</td>")
                        .append("<td class='text-center ").append(isLulus ? "status-tercapai" : "status-gagal").append("'>").append(isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")).append("</td>")
                        .append("</tr>");

                    mapAvgCpmkClass.put(fn.getNama(), (mapAvgCpmkClass.get(fn.getNama()) == null ? 0.0 : mapAvgCpmkClass.get(fn.getNama())) + nVal);
                    mapCountCpmkClass.put(fn.getNama(), (mapCountCpmkClass.get(fn.getNama()) == null ? 0 : mapCountCpmkClass.get(fn.getNama())) + 1);
                }
            }
        } else {
            for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                Double sumKonvWeight = 0.0, sumWeight = 0.0;
                List<FormatNilai> fns = mapCpmkIdToFns.get(cpmk.getId());
                if(fns != null) {
                    for (FormatNilai fn : fns) {
                        Double kVal = nilaiFormatMhs.get(fn.getId()) != null ? nilaiFormatMhs.get(fn.getId()) : 0.0;
                        sumKonvWeight += (kVal * (fn.getPersen() != null ? fn.getPersen() : 0.0));
                        sumWeight += (fn.getPersen() != null ? fn.getPersen() : 0.0);
                    }
                }
                Double scoreCpmk = sumWeight > 0 ? (sumKonvWeight / sumWeight) : 0.0;
                totalCpmkScores.put(cpmk.getId(), scoreCpmk);
                
                if (scoreCpmk > 0) {
                    Double nMin = 60.0;
                    boolean isLulus = scoreCpmk >= nMin;
                    rowCpmk.append("<tr>")
                        .append("<td class='text-center'>").append(idxCpmk++).append("</td>")
                        .append("<td class='text-center fw-bold text-primary'>").append(cpmk.getKode()).append("</td>")
                        .append("<td>").append(cpmk.getNama()).append("</td>")
                        .append("<td class='text-center'>").append(kodeMk).append("</td>")
                        .append("<td>").append(namaMk).append("</td>")
                        .append("<td class='text-center'>").append(nMin).append("</td>")
                        .append("<td class='text-center fw-bold'>").append(Common.numberFormat.get().format(scoreCpmk)).append("</td>")
                        .append("<td class='text-center ").append(isLulus ? "status-tercapai" : "status-gagal").append("'>").append(isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")).append("</td>")
                        .append("</tr>");

                    mapAvgCpmkClass.put(cpmk.getKode(), (mapAvgCpmkClass.get(cpmk.getKode()) == null ? 0.0 : mapAvgCpmkClass.get(cpmk.getKode())) + scoreCpmk);
                    mapCountCpmkClass.put(cpmk.getKode(), (mapCountCpmkClass.get(cpmk.getKode()) == null ? 0 : mapCountCpmkClass.get(cpmk.getKode())) + 1);
                }
            }
        }

        // EVALUASI CPL
        Map<Long, Double> totalCplScores = new HashMap<Long, Double>();
        for (CapaianLulusan cpl : capaianLulusans) {
            Double sumCplConv = 0.0, sumCplWeight = 0.0;
            for(FormatNilai fn : formatNilais) {
                if (fn.getCapaianPembelajaranLulusan() != null && cpl.getCapaianPembelajaranLulusan() != null) {
                    String keyId = fn.getCapaianPembelajaranLulusan().getId() + "";
                    if (cpl.getCapaianPembelajaranLulusan().contains("," + keyId + ",")) {
                        Double kVal = nilaiFormatMhs.get(fn.getId()) != null ? nilaiFormatMhs.get(fn.getId()) : 0.0;
                        sumCplConv += (kVal * (fn.getPersen() != null ? fn.getPersen() : 0.0));
                        sumCplWeight += (fn.getPersen() != null ? fn.getPersen() : 0.0);
                    }
                }
            }
            Double finalCplConv = sumCplWeight > 0 ? (sumCplConv / sumCplWeight) : 0.0;
            totalCplScores.put(cpl.getId(), finalCplConv);

            if (finalCplConv > 0) {
                Double nMin = 60.0;
                boolean isLulus = finalCplConv >= nMin;
                rowCpl.append("<tr>")
                    .append("<td class='text-center'>").append(idxCpl++).append("</td>")
                    .append("<td class='text-center fw-bold text-primary'>").append(cpl.getKode()).append("</td>")
                    .append("<td>").append(cpl.getNama()).append("</td>")
                    .append("<td class='text-center'>").append(kodeMk).append("</td>")
                    .append("<td>").append(namaMk).append("</td>")
                    .append("<td class='text-center'>").append(nMin).append("</td>")
                    .append("<td class='text-center fw-bold'>").append(Common.numberFormat.get().format(finalCplConv)).append("</td>")
                    .append("<td class='text-center ").append(isLulus ? "status-tercapai" : "status-gagal").append("'>").append(isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")).append("</td>")
                    .append("</tr>");
                
                mapAvgCplClass.put(cpl.getKode(), (mapAvgCplClass.get(cpl.getKode()) == null ? 0.0 : mapAvgCplClass.get(cpl.getKode())) + finalCplConv);
                mapCountCplClass.put(cpl.getKode(), (mapCountCplClass.get(cpl.getKode()) == null ? 0 : mapCountCplClass.get(cpl.getKode())) + 1);
            }
        }

        // EVALUASI PL / PROFESI LULUSAN
        for (ProfilLulusan pl : profilLulusans) {
            List<CapaianLulusan> mappedCpls = plToCplMap.get(pl.getId());
            Double sumScore = 0.0;
            int count = 0;
            if(mappedCpls != null) {
                for(CapaianLulusan cpl : mappedCpls) {
                    Double score = totalCplScores.get(cpl.getId());
                    if(score != null && score > 0) { sumScore += score; count++; }
                }
            }
            Double finalPlConv = count > 0 ? (sumScore / count) : 0.0;

            if (finalPlConv > 0) {
                Double nMin = 60.0;
                boolean isLulus = finalPlConv >= nMin;
                rowPl.append("<tr>")
                    .append("<td class='text-center'>").append(idxPl++).append("</td>")
                    .append("<td class='text-center fw-bold text-primary'>").append(pl.getKode()).append("</td>")
                    .append("<td>").append(pl.getNama()).append("</td>")
                    .append("<td class='text-center'>").append(kodeMk).append("</td>")
                    .append("<td>").append(namaMk).append("</td>")
                    .append("<td class='text-center'>").append(nMin).append("</td>")
                    .append("<td class='text-center fw-bold'>").append(Common.numberFormat.get().format(finalPlConv)).append("</td>")
                    .append("<td class='text-center ").append(isLulus ? "status-tercapai" : "status-gagal").append("'>").append(isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")).append("</td>")
                    .append("</tr>");

                mapAvgPlClass.put(pl.getKode(), (mapAvgPlClass.get(pl.getKode()) == null ? 0.0 : mapAvgPlClass.get(pl.getKode())) + finalPlConv);
                mapCountPlClass.put(pl.getKode(), (mapCountPlClass.get(pl.getKode()) == null ? 0 : mapCountPlClass.get(pl.getKode())) + 1);
            }
        }
    } // Akhir dari Loop Detailperkuliahan

    // SUSUN DATA UNTUK SEMUA CHART (BAR & SPIDER WEB)
    JSONArray subCpmkLabels = new JSONArray();
    JSONArray subCpmkData = new JSONArray();
    for(String key : mapAvgSubCpmkClass.keySet()) {
        subCpmkLabels.put(key);
        subCpmkData.put(mapAvgSubCpmkClass.get(key) / mapCountSubCpmkClass.get(key));
    }

    JSONArray cpmkLabels = new JSONArray();
    JSONArray cpmkData = new JSONArray();
    for(String key : mapAvgCpmkClass.keySet()) {
        cpmkLabels.put(key);
        cpmkData.put(mapAvgCpmkClass.get(key) / mapCountCpmkClass.get(key));
    }

    JSONArray cplLabels = new JSONArray();
    JSONArray cplData = new JSONArray();
    for(String key : mapAvgCplClass.keySet()) {
        cplLabels.put(key);
        cplData.put(mapAvgCplClass.get(key) / mapCountCplClass.get(key));
    }

    JSONArray plLabels = new JSONArray();
    JSONArray plData = new JSONArray();
    for(String key : mapAvgPlClass.keySet()) {
        plLabels.put(key);
        plData.put(mapAvgPlClass.get(key) / mapCountPlClass.get(key));
    }

    %>
    <div class="card border-0 shadow-sm rounded-4 mb-4 mt-3">
        <div class="card-body p-4">
            
            <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3 no-print">
                <button class="btn btn-outline-secondary shadow-sm fw-bold px-4 rounded-pill" onclick="window.setMahasiswaPorto<%=rnd%>('<%=mahasiswa.getId()%>')">
                    <i class="fas fa-arrow-left me-2"></i><%=Common.getBahasaConfig("Kembali ke Daftar MK")%>
                </button>
                <button class="btn btn-primary shadow-sm fw-bold px-4 rounded-pill" onclick="window.print()">
                    <i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Cetak Portofolio")%>
                </button>
            </div>

            <div id="printAreaPortofolio<%=rnd%>" class="bg-white text-dark mt-2">
                <div class="text-center mb-5">
                    <h4 class="fw-bold mb-1 text-uppercase border-bottom border-2 border-dark d-inline-block pb-2 px-3"><%=Common.getBahasaConfig("PORTOFOLIO GABUNGAN (OBE)")%></h4>
                </div>

                <table class="table-portofolio border-0 mb-4" style="width: 70%;">
                    <tr><td class="border-0 p-1 fw-bold text-uppercase" style="width: 150px;"><%=Common.getBahasaConfig("Nama Mahasiswa")%></td><td class="border-0 p-1 fw-bold">: <%=mahasiswa.getNama()%></td></tr>
                    <tr><td class="border-0 p-1 fw-bold text-uppercase"><%=Common.getBahasaConfig("NIM")%></td><td class="border-0 p-1 fw-bold">: <%=mahasiswa.getNim()%></td></tr>
                    <tr><td class="border-0 p-1 fw-bold text-uppercase"><%=Common.getBahasaConfig("Program Studi")%></td><td class="border-0 p-1 fw-bold">: <%=mahasiswa.getJurusan() != null ? mahasiswa.getJurusan().getNama() : "-"%></td></tr>
                </table>

                <% if(rowSubCpmk.length() > 0) { %>
                <h6 class="fw-bold mt-4 mb-2"><i class="fas fa-layer-group me-2 no-print"></i>1. <%=Common.getBahasaConfig("Rekapitulasi Sub-CPMK")%></h6>
                <table class="table-portofolio">
                    <thead><tr><th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Kode Sub-CPMK")%></th><th style="width: 25%;"><%=Common.getBahasaConfig("Nama Sub-CPMK")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th></tr></thead>
                    <tbody><%=rowSubCpmk.toString()%></tbody>
                </table>
                <% } %>

                <h6 class="fw-bold mt-5 mb-2"><i class="fas fa-bullseye me-2 no-print"></i><%= rowSubCpmk.length() > 0 ? "2." : "1." %> <%=Common.getBahasaConfig("Rekapitulasi CPMK")%></h6>
                <table class="table-portofolio">
                    <thead><tr><th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Kode CPMK")%></th><th style="width: 25%;"><%=Common.getBahasaConfig("Nama CPMK")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th></tr></thead>
                    <tbody><%=rowCpmk.length() > 0 ? rowCpmk.toString() : "<tr><td colspan='8' class='text-center py-3'>" + Common.getBahasaConfig("Data ketercapaian belum tersedia / bernilai 0") + "</td></tr>"%></tbody>
                </table>

                <h6 class="fw-bold mt-5 mb-2"><i class="fas fa-graduation-cap me-2 no-print"></i><%= rowSubCpmk.length() > 0 ? "3." : "2." %> <%=Common.getBahasaConfig("Rekapitulasi CPL")%></h6>
                <table class="table-portofolio">
                    <thead><tr><th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Kode CPL")%></th><th style="width: 25%;"><%=Common.getBahasaConfig("Nama CPL")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th></tr></thead>
                    <tbody><%=rowCpl.length() > 0 ? rowCpl.toString() : "<tr><td colspan='8' class='text-center py-3'>" + Common.getBahasaConfig("Data ketercapaian belum tersedia / bernilai 0") + "</td></tr>"%></tbody>
                </table>

                <h6 class="fw-bold mt-5 mb-2"><i class="fas fa-user-tie me-2 no-print"></i><%= rowSubCpmk.length() > 0 ? "4." : "3." %> <%=Common.getBahasaConfig("Rekapitulasi Profil Lulusan")%></h6>
                <table class="table-portofolio">
                    <thead><tr><th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Kode PL")%></th><th style="width: 25%;"><%=Common.getBahasaConfig("Nama Profil")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th></tr></thead>
                    <tbody><%=rowPl.length() > 0 ? rowPl.toString() : "<tr><td colspan='8' class='text-center py-3'>" + Common.getBahasaConfig("Data ketercapaian belum tersedia / bernilai 0") + "</td></tr>"%></tbody>
                </table>

                <h6 class="fw-bold mt-5 mb-2"><i class="fas fa-briefcase me-2 no-print"></i><%= rowSubCpmk.length() > 0 ? "5." : "4." %> <%=Common.getBahasaConfig("Rekapitulasi Profesi Lulusan")%></h6>
                <table class="table-portofolio">
                    <thead><tr><th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Kode Profesi")%></th><th style="width: 25%;"><%=Common.getBahasaConfig("Nama Profesi")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th><th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th><th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th></tr></thead>
                    <tbody><%=rowPl.length() > 0 ? rowPl.toString() : "<tr><td colspan='8' class='text-center py-3'>" + Common.getBahasaConfig("Data ketercapaian belum tersedia / bernilai 0") + "</td></tr>"%></tbody>
                </table>

                <div class="row g-4 mt-5 no-print" style="page-break-inside: avoid;">
                    
                    <% if (subCpmkLabels.length() > 0) { %>
                    <div class="col-md-6">
                        <div class="card shadow-sm border-0 h-100 bg-light">
                            <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-info">
                                <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Grafik Ketercapaian Sub-CPMK Keseluruhan")%>
                            </div>
                            <div class="card-body pt-0 pb-4 px-4">
                                <div id="chartGabunganSubCpmk<%=rnd%>" class="el-css-chart"></div>
                            </div>
                        </div>
                    </div>
                    <% } %>

                    <% if (cpmkLabels.length() > 0) { %>
                    <div class="col-md-6">
                        <div class="card shadow-sm border-0 h-100 bg-light">
                            <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-primary">
                                <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Grafik Ketercapaian CPMK Keseluruhan")%>
                            </div>
                            <div class="card-body pt-0 pb-4 px-4">
                                <div id="chartGabunganCpmk<%=rnd%>" class="el-css-chart"></div>
                            </div>
                        </div>
                    </div>
                    <% } %>

                    <% if (cplLabels.length() > 0) { %>
                    <div class="col-md-6 mt-4">
                        <div class="card shadow-sm border-0 h-100 bg-light">
                            <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-success">
                                <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Grafik Ketercapaian CPL Keseluruhan")%>
                            </div>
                            <div class="card-body pt-0 pb-4 px-4">
                                <div id="chartGabunganCpl<%=rnd%>" class="el-css-chart"></div>
                            </div>
                        </div>
                    </div>
                    <% } %>

                    <% if (plLabels.length() > 0) { %>
                    <div class="col-md-6 mt-4">
                        <div class="card shadow-sm border-0 h-100 bg-light">
                            <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-warning">
                                <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Grafik Ketercapaian Profil Lulusan")%>
                            </div>
                            <div class="card-body pt-0 pb-4 px-4">
                                <div id="chartGabunganPl<%=rnd%>" class="el-css-chart"></div>
                            </div>
                        </div>
                    </div>
                    <% } %>

                    <% if (plLabels.length() > 0) { %>
                    <div class="col-md-6 mt-4">
                        <div class="card shadow-sm border-0 h-100 bg-light">
                            <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-danger">
                                <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Grafik Ketercapaian Profesi Lulusan")%>
                            </div>
                            <div class="card-body pt-0 pb-4 px-4">
                                <div id="chartGabunganProfesi<%=rnd%>" class="el-css-chart"></div>
                            </div>
                        </div>
                    </div>
                    <% } %>

                    <% if (cplLabels.length() > 0) { %>
                    <div class="col-md-6 mt-4">
                        <div class="card shadow-sm border-0 h-100 bg-light">
                            <div class="card-header bg-transparent text-center fw-bold py-3 border-0" style="color: #1cc88a;">
                                <i class="fas fa-spider me-2"></i><%=Common.getBahasaConfig("Spider Web Rata-Rata CPL Keseluruhan")%>
                            </div>
                            <div class="card-body pt-0 pb-4 px-4 d-flex justify-content-center align-items-center">
                                <div id="chartRadarGabunganCpl<%=rnd%>" class="el-css-chart"></div>
                            </div>
                        </div>
                    </div>
                    <% } %>

                    <% if (plLabels.length() > 0) { %>
                    <div class="col-md-6 mt-4">
                        <div class="card shadow-sm border-0 h-100 bg-light">
                            <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-warning">
                                <i class="fas fa-spider me-2"></i><%=Common.getBahasaConfig("Spider Web Rata-Rata Profil Lulusan")%>
                            </div>
                            <div class="card-body pt-0 pb-4 px-4 d-flex justify-content-center align-items-center">
                                <div id="chartRadarGabunganPl<%=rnd%>" class="el-css-chart"></div>
                            </div>
                        </div>
                    </div>
                    <% } %>

                    <% if (plLabels.length() > 0) { %>
                    <div class="col-md-6 mt-4">
                        <div class="card shadow-sm border-0 h-100 bg-light">
                            <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-danger">
                                <i class="fas fa-spider me-2"></i><%=Common.getBahasaConfig("Spider Web Rata-Rata Profesi Lulusan")%>
                            </div>
                            <div class="card-body pt-0 pb-4 px-4 d-flex justify-content-center align-items-center">
                                <div id="chartRadarGabunganProfesi<%=rnd%>" class="el-css-chart"></div>
                            </div>
                        </div>
                    </div>
                    <% } %>

                </div>
                
                <div class="text-end text-muted small mt-5 pt-4">
                    <i><%=Common.getBahasaConfig("Dicetak pada:")%> <%=new java.text.SimpleDateFormat("dd MMMM yyyy HH:mm").format(new Date())%></i>
                </div>

            </div>
        </div>
    </div>

    <script>

        function elSafeNumber(val) {
            var num = parseFloat(val);
            if (isNaN(num) || !isFinite(num)) return 0;
            if (num < 0) return 0;
            if (num > 100) return 100;
            return num;
        }
        function elEscape(value) {
            return String(value == null ? '' : value).replace(/[&<>"]/g, function(ch) {
                return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[ch];
            });
        }
        function elRenderBar(targetId, labels, values) {
            var el = document.getElementById(targetId);
            if (!el) return;
            labels = labels || [];
            values = values || [];
            if (!labels.length) {
                el.innerHTML = '<div class="el-chart-empty">Belum ada data yang dapat ditampilkan.</div>';
                return;
            }
            var html = '<div class="el-css-chart">';
            for (var i = 0; i < labels.length; i++) {
                var score = elSafeNumber(values[i]);
                html += '<div class="el-css-chart-row"><div class="el-css-chart-label" title="' + elEscape(labels[i]) + '">' + elEscape(labels[i]) + '</div>';
                html += '<div class="el-css-chart-track"><div class="el-css-chart-fill" style="width:' + score + '%"></div></div>';
                html += '<div class="el-css-chart-value">' + score.toFixed(score % 1 === 0 ? 0 : 1) + '%</div></div>';
            }
            html += '</div>';
            el.innerHTML = html;
        }
        function elRenderRadar(targetId, labels, values) {
            var el = document.getElementById(targetId);
            if (!el) return;
            labels = labels || [];
            values = values || [];
            if (!labels.length) {
                el.innerHTML = '<div class="el-chart-empty">Belum ada data yang dapat ditampilkan.</div>';
                return;
            }
            var total = 0;
            for (var i = 0; i < values.length; i++) total += elSafeNumber(values[i]);
            var avg = values.length ? total / values.length : 0;
            var html = '<div class="el-css-radar"><div class="el-css-radar-core" style="--score:' + avg.toFixed(1) + '"><div class="el-css-radar-score">' + avg.toFixed(0) + '%</div></div><div class="el-css-radar-list">';
            for (var j = 0; j < labels.length; j++) {
                var score = elSafeNumber(values[j]);
                html += '<div class="el-css-radar-item"><strong title="' + elEscape(labels[j]) + '">' + elEscape(labels[j]) + '</strong><span>' + score.toFixed(score % 1 === 0 ? 0 : 1) + '%</span></div>';
            }
            html += '</div></div>';
            el.innerHTML = html;
        }

        function renderGabunganCharts<%=rnd%>() {
            try {
                <% if (subCpmkLabels.length() > 0) { %>elRenderBar('chartGabunganSubCpmk<%=rnd%>', <%=subCpmkLabels.toString()%>, <%=subCpmkData.toString()%>);<% } %>
                <% if (cpmkLabels.length() > 0) { %>elRenderBar('chartGabunganCpmk<%=rnd%>', <%=cpmkLabels.toString()%>, <%=cpmkData.toString()%>);<% } %>
                <% if (cplLabels.length() > 0) { %>elRenderBar('chartGabunganCpl<%=rnd%>', <%=cplLabels.toString()%>, <%=cplData.toString()%>); elRenderRadar('chartRadarGabunganCpl<%=rnd%>', <%=cplLabels.toString()%>, <%=cplData.toString()%>);<% } %>
                <% if (plLabels.length() > 0) { %>elRenderBar('chartGabunganPl<%=rnd%>', <%=plLabels.toString()%>, <%=plData.toString()%>); elRenderBar('chartGabunganProfesi<%=rnd%>', <%=plLabels.toString()%>, <%=plData.toString()%>); elRenderRadar('chartRadarGabunganPl<%=rnd%>', <%=plLabels.toString()%>, <%=plData.toString()%>); elRenderRadar('chartRadarGabunganProfesi<%=rnd%>', <%=plLabels.toString()%>, <%=plData.toString()%>);<% } %>
            } catch(e) { console.error('Error rendering HTML/CSS charts: ', e); }
        }
        setTimeout(renderGabunganCharts<%=rnd%>, 120);
    </script>
    <%
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_gabungan_service.jsp:722");
    out.print("<div class='alert alert-danger m-4 shadow-sm'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Terjadi kesalahan saat memproses data layanan gabungan.") + "</div>");
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_gabungan_service.jsp:725");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
}
%>