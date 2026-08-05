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
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:40");}
    }

    private <T> Double sumNilai(Collection<T> items, Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData, Long mhsId, Long formatId, String prefix) {
        Double sum = 0.0;
        for (T item : items) {
            try {
                Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
                String key = prefix + "_" + itemId;
                Double nilia = 0.0;
                try { nilia = data.get(mhsId).get(formatId).get(key); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:50");}
                if(nilia == null) nilia = 0.0;
                Double persenData = 0.0;
                try { persenData = (pData.get(key + "_" + formatId) * 0.01) * nilia; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:53");}
                if(persenData != null) sum += persenData;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:55");}
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
        double persenFn = formatNilai.getPersen() != null ? formatNilai.getPersen() : 0.0;
        BigDecimal b15 = new BigDecimal(persenFn / 100.0);
        BigDecimal bn34 = new BigDecimal(totalNilaiPerformatNilai);
        BigDecimal divisor = (b15.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ONE : b15;
        return bn34.divide(divisor, 2, RoundingMode.HALF_UP).doubleValue();
    }

    private Set<Long> parseIdsToSet(String ids) {
        Set<Long> longs = new HashSet<Long>();
        if (ids != null && !ids.trim().isEmpty()) {
            for (String s : ids.split(",")) {
                if (!s.trim().isEmpty()) {
                    try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:82");}
                }
            }
        }
        return longs;
    }
%>
<%
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean isMahasiswa = tbmuser.getMahasiswa() != null;
boolean isDosen = tbmuser.getDosen() != null;

String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
String idPerkuliahanStr = request.getParameter("perkuliahan");
String variable = request.getParameter("var") != null ? request.getParameter("var") : rnd;

if(idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) return;

KurikulumPunyaMatakuliah kpm = null;
Perkuliahan perkuliahan = null;
List<Perkuliahan> listPerkuliahanTersedia = new ArrayList<Perkuliahan>();

StringBuilder htmlRekapTable = new StringBuilder();
String namaFileLaporan = "Rekap_Penilaian_OBE";

Session sess = HibernateUtil.openSession();
boolean isNilaiCpmk = false;

JSONArray cpmkLabels = new JSONArray();
JSONArray cpmkDataList = new JSONArray();
JSONArray cplLabels = new JSONArray();
JSONArray cplDataList = new JSONArray();
JSONArray plLabels = new JSONArray();
JSONArray plDataList = new JSONArray();

try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
    if (idPerkuliahanStr != null && !idPerkuliahanStr.trim().isEmpty() && !idPerkuliahanStr.equals("null")) {
        perkuliahan = (Perkuliahan) sess.get(Perkuliahan.class, Long.parseLong(idPerkuliahanStr));
    }
    
    if (kpm != null) {
        isNilaiCpmk = kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk();

        org.hibernate.Criteria crit = sess.createCriteria(Perkuliahan.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
            .add(Restrictions.eq("kurikulumPunyaMatakuliah.id", kpm.getId()));

        if (isDosen) {
            List<Long> perkuliahansIds = tbmuser.getDosen().ambilPerkuliahan();
            if (perkuliahansIds != null && !perkuliahansIds.isEmpty()) crit.add(Restrictions.in("id", perkuliahansIds));
            else crit.add(Restrictions.isNull("id")); 
        } else if (isMahasiswa) {
            List<Long> perkuliahansIds = tbmuser.getMahasiswa().ambilPerkuliahanDanParalel();
            if (perkuliahansIds != null && !perkuliahansIds.isEmpty()) crit.add(Restrictions.in("id", perkuliahansIds));
            else crit.add(Restrictions.isNull("id"));
        }
        crit.addOrder(Order.desc("idSmt")).addOrder(Order.desc("id"));
        listPerkuliahanTersedia = ConstantValues.simpleList(crit, Perkuliahan.class);
    }

    if (kpm == null) {
        out.print("<div class='alert alert-danger m-4'>" + Common.getBahasaConfig("Data Rencana Pembelajaran Semester tidak valid.") + "</div>");
        return;
    }

    if (perkuliahan == null && listPerkuliahanTersedia.size() == 1) {
        perkuliahan = listPerkuliahanTersedia.get(0);
    }

    if (perkuliahan != null) {
        namaFileLaporan = "Rekap_Penilaian_OBE_" + perkuliahan.infoSimple().replaceAll("[^a-zA-Z0-9_\\\\-]", "_");
        List<Mahasiswa> hasilUjianMahasiswas = perkuliahan.ambilMahasiswa();
        
        // LIMITASI DATA UNTUK MAHASISWA
        if(isMahasiswa) {
            List<Mahasiswa> tempMhs = new ArrayList<Mahasiswa>();
            for(Mahasiswa m : hasilUjianMahasiswas) {
                if(m.getId().equals(tbmuser.getMahasiswa().getId())) { tempMhs.add(m); break; }
            }
            hasilUjianMahasiswas = tempMhs;
        }

        if (hasilUjianMahasiswas.isEmpty()) {
            htmlRekapTable.append("<tr><td colspan='100%' class='text-center py-5 fst-italic text-muted'>").append(Common.getBahasaConfig("Belum ada mahasiswa yang mengambil kelas ini / Akses dibatasi.")).append("</td></tr>");
        } else {
            Matakuliah matakuliah = kpm.getMatakuliah();

            // 1. Ekstrak CPL & PL Berdasarkan Korelasi ZK Asli
            List<CapaianLulusan> capaianLulusans = new ArrayList<CapaianLulusan>();
            List<ProfilLulusan> profilLulusans = new ArrayList<ProfilLulusan>();
            Map<Long, List<CapaianLulusan>> listProfilLulusanToCpl = new HashMap<Long, List<CapaianLulusan>>();

            if(matakuliah != null) {
                final Set<Long> longs = parseIdsToSet(matakuliah.getCapaianLulusan());
                capaianLulusans = ConstantValues.simpleList(
                        sess.createCriteria(CapaianLulusan.class)
                                .add(longs.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longs))
                                .addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                        CapaianLulusan.class);

                Set<Long> longsProfile = parseIdsToSet(matakuliah.getProfilLulusan());
                profilLulusans = ConstantValues.simpleList(
                        sess.createCriteria(ProfilLulusan.class)
                                .add(longsProfile.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longsProfile))
                                .addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                        ProfilLulusan.class);

                // Mengaitkan CPL ke PL
                for (ProfilLulusan pl : profilLulusans) {
                    final String idBaru = pl.getId() + "_" + kpm.getId();
                    for (CapaianLulusan cpl : capaianLulusans) {
                        String p = cpl.getProfil() != null ? cpl.getProfil() : "";
                        boolean isChecked = p.contains("," + pl.getId() + ",") || p.contains("," + idBaru + ",");

                        if (isChecked) {
                            List<CapaianLulusan> mappedCpls = listProfilLulusanToCpl.get(pl.getId());
                            if (mappedCpls == null) { 
                                mappedCpls = new ArrayList<CapaianLulusan>(); 
                                listProfilLulusanToCpl.put(pl.getId(), mappedCpls); 
                            }
                            mappedCpls.add(cpl);
                        }
                    }
                }
            }

            // 2. Ekstrak Pertemuan & Format Nilai
            List<PertemuanPunyaUjian> pertemuanPunyaUjians = ConstantValues.simpleList(
                sess.createCriteria(PertemuanPunyaUjian.class)
                    .createAlias("pertemuan", "pertemuan").addOrder(Order.asc("mulaiUjian")).addOrder(Order.asc("sampaiUjian"))
                    .createAlias("ujian", "ujian").add(Restrictions.eq("ujian.aktif", true)).add(Restrictions.eq("pertemuan.perkuliahan", perkuliahan)), 
                PertemuanPunyaUjian.class
            );

            List<Pertemuan> pertemuansTugas = ConstantValues.simpleList(
                sess.createCriteria(Pertemuan.class)
                    .add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas"))
                    .addOrder(Order.asc("mulai")).add(Restrictions.eq("aktif", true)).add(Restrictions.eq("perkuliahan", perkuliahan)), 
                Pertemuan.class
            );

            Collection<Long> pertemuansList = perkuliahan.ambilPertemuan().values();
            List<TugasPertemuan> pertemuansTugasLanjut = pertemuansList.isEmpty() ? new ArrayList<TugasPertemuan>() : ConstantValues.simpleList(
                sess.createCriteria(TugasPertemuan.class)
                    .add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas")).addOrder(Order.asc("mulai"))
                    .add(Restrictions.in("pertemuan", pertemuansList)), 
                TugasPertemuan.class
            );

            List<TugasKelompok> pertemuansTugasKelompoks = ConstantValues.simpleList(
                sess.createCriteria(TugasKelompok.class)
                    .add(Restrictions.ne("judul", "")).add(Restrictions.isNotNull("judul"))
                    .addOrder(Order.asc("mulai")).add(Restrictions.eq("perkuliahan", perkuliahan)), 
                TugasKelompok.class
            );

            List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan);

            // 3. Mapping Agregasi CPMK (Jika isNilaiCpmk == false)
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
                Collections.sort(listCpmkUnique, new Comparator<CapaianPembelajaranLulusan>() {
                    public int compare(CapaianPembelajaranLulusan a, CapaianPembelajaranLulusan b) {
                        String kA = a.getKode() == null ? "" : a.getKode();
                        String kB = b.getKode() == null ? "" : b.getKode();
                        return kA.compareTo(kB);
                    }
                });
            }

            // 4. Kalkulasi Nilai Internal
            Map<Long, Map<Long, Map<String, Double>>> dataNiliasUtama = new HashMap<Long, Map<Long, Map<String, Double>>>();
            Map<Long, Map<String, Double>> dataBobot = new HashMap<Long, Map<String, Double>>();
            Map<Long, Map<Long, PertemuanPunyaUjian>> mapPertemuanPunyaUjian = new HashMap<Long, Map<Long, PertemuanPunyaUjian>>();
            Map<Long, Map<Long, Pertemuan>> mapTugas = new HashMap<Long, Map<Long, Pertemuan>>();
            Map<Long, Map<Long, TugasPertemuan>> mapTugasLanjut = new HashMap<Long, Map<Long, TugasPertemuan>>();
            Map<Long, Map<Long, TugasKelompok>> mapTugasKelompok = new HashMap<Long, Map<Long, TugasKelompok>>();
            Map<String, String> mapHasilObe = new HashMap<String, String>();

            if (!pertemuanPunyaUjians.isEmpty() && !hasilUjianMahasiswas.isEmpty()) {
                List<HasilUjianMahasiswa> listHasil = ConstantValues.simpleList(
                    sess.createCriteria(HasilUjianMahasiswa.class)
                        .add(Restrictions.isNotNull("keyhasil")).add(Restrictions.in("pertemuanPunyaUjian", pertemuanPunyaUjians))
                        .add(Restrictions.in("mahasiswa", hasilUjianMahasiswas)), 
                    HasilUjianMahasiswa.class
                );
                for (HasilUjianMahasiswa hum : listHasil) {
                    if (hum.getNilaiObe() != null) mapHasilObe.put(hum.getPertemuanPunyaUjian().getId() + "_" + hum.getMahasiswa().getId(), hum.getNilaiObe());
                }
            }

            for (Pertemuan p : pertemuansTugas) {
                if (p.getKeteranganNilai() == null || p.getKeteranganNilai().trim().isEmpty()) continue;
                try {
                    JSONObject jsonObject = new JSONObject(p.getKeteranganNilai());
                    for (FormatNilai fn : formatNilais) {
                        boolean hasScore = false;
                        for (Mahasiswa mhs : hasilUjianMahasiswas) {
                            String keyJson = mhs.getId() + "_mhs_nilai_" + fn.getId();
                            if (!jsonObject.isNull(keyJson)) {
                                hasScore = true; addNilaiUtama(dataNiliasUtama, mhs.getId(), fn.getId(), Pertemuan.class.getName() + "_" + p.getId(), jsonObject.getDouble(keyJson));
                            }
                        }
                        if (hasScore) addMapType(mapTugas, fn.getId(), p.getId(), p);
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:305");}
            }
            for (TugasPertemuan tp : pertemuansTugasLanjut) {
                if (tp.getKeteranganNilai() == null || tp.getKeteranganNilai().trim().isEmpty()) continue;
                try {
                    JSONObject jsonObject = new JSONObject(tp.getKeteranganNilai());
                    for (FormatNilai fn : formatNilais) {
                        boolean hasScore = false;
                        for (Mahasiswa mhs : hasilUjianMahasiswas) {
                            String keyJson = mhs.getId() + "_mhs_nilai_" + fn.getId();
                            if (!jsonObject.isNull(keyJson)) {
                                hasScore = true; addNilaiUtama(dataNiliasUtama, mhs.getId(), fn.getId(), TugasPertemuan.class.getName() + "_" + tp.getId(), jsonObject.getDouble(keyJson));
                            }
                        }
                        if (hasScore) addMapType(mapTugasLanjut, fn.getId(), tp.getId(), tp);
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:321");}
            }
            for (TugasKelompok tk : pertemuansTugasKelompoks) {
                if (tk.getKeteranganNilai() == null || tk.getKeteranganNilai().trim().isEmpty()) continue;
                try {
                    JSONObject jsonObject = new JSONObject(tk.getKeteranganNilai());
                    for (FormatNilai fn : formatNilais) {
                        boolean hasScore = false;
                        for (Mahasiswa mhs : hasilUjianMahasiswas) {
                            String keyJson = mhs.getId() + "_mhs_nilai_" + fn.getId();
                            if (!jsonObject.isNull(keyJson)) {
                                hasScore = true; addNilaiUtama(dataNiliasUtama, mhs.getId(), fn.getId(), TugasKelompok.class.getName() + "_" + tk.getId(), jsonObject.getDouble(keyJson));
                            }
                        }
                        if (hasScore) addMapType(mapTugasKelompok, fn.getId(), tk.getId(), tk);
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:337");}
            }

            for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
                for (Mahasiswa mhs : hasilUjianMahasiswas) {
                    String nilaiObe = mapHasilObe.get(ppu.getId() + "_" + mhs.getId());
                    if (nilaiObe != null && !nilaiObe.trim().isEmpty()) {
                        try {
                            JSONObject jsonObjectHasil = new JSONObject(nilaiObe);
                            for (FormatNilai fn : formatNilais) {
                                if (!jsonObjectHasil.isNull(fn.getId().toString())) {
                                    addMapType(mapPertemuanPunyaUjian, fn.getId(), ppu.getId(), ppu);
                                    Double nilaiSkor = jsonObjectHasil.getDouble(fn.getId().toString());
                                    Double nilaiMax = jsonObjectHasil.isNull(fn.getId() + "_max") ? 0.0 : jsonObjectHasil.getDouble(fn.getId() + "_max");
                                    Double nilaiDidapat = nilaiMax.equals(0.0) ? 0.0 : (nilaiSkor * 100.0) / nilaiMax;
                                    addNilaiUtama(dataNiliasUtama, mhs.getId(), fn.getId(), PertemuanPunyaUjian.class.getName() + "_" + ppu.getId(), nilaiDidapat);
                                }
                            }
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:355");}
                    }
                }
            }

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
            // MEMBANGUN MATRIX REKAP KESELURUHAN (SESUAI EXCEL + PL)
            // ========================================================================
            int colCpmk = isNilaiCpmk ? formatNilais.size() : listCpmkUnique.size();
            int colCpl = capaianLulusans.size();
            int colPl = profilLulusans.size();
            int numStudents = hasilUjianMahasiswas.size();

            // Variabel penyimpan akumulasi nilai kelas untuk Charts
            Map<Long, Double> sumCpmkClass = new HashMap<Long, Double>();
            Map<Long, Double> sumCplClass = new HashMap<Long, Double>();
            Map<Long, Double> sumPlClass = new HashMap<Long, Double>();

            StringBuilder thead = new StringBuilder("<thead class='table-light'><tr>");
            thead.append("<th rowspan='2' class='align-middle text-center' style='width: 50px;'>No.</th>");
            thead.append("<th rowspan='2' class='align-middle' style='width: 120px;'>").append(Common.getBahasaConfig("NIM Mahasiswa")).append("</th>");
            thead.append("<th rowspan='2' class='align-middle' style='min-width: 200px;'>").append(Common.getBahasaConfig("Nama Mhs")).append("</th>");
            thead.append("<th rowspan='2' class='align-middle text-center'>").append(Common.getBahasaConfig("Nilai")).append("</th>");
            thead.append("<th rowspan='2' class='align-middle text-center'>").append(Common.getBahasaConfig("Lulus/Tidak")).append("</th>");
            
            if(colCpmk > 0) thead.append("<th colspan='").append(colCpmk).append("' class='text-center align-middle border-bottom border-2 border-primary'>").append(Common.getBahasaConfig("Ketercapaian Nilai Pada Masing CPMK")).append("</th>");
            if(colCpl > 0) thead.append("<th colspan='").append(colCpl).append("' class='text-center align-middle border-bottom border-2 border-primary'>").append(Common.getBahasaConfig("Ketercapaian Nilai Pada Masing CPL")).append("</th>");
            if(colPl > 0) thead.append("<th colspan='").append(colPl).append("' class='text-center align-middle border-bottom border-2 border-primary'>").append(Common.getBahasaConfig("Ketercapaian Nilai Pada Masing PL")).append("</th>");
            thead.append("</tr><tr>");

            if (isNilaiCpmk) {
                for(FormatNilai fn : formatNilais) {
                    thead.append("<th class='text-center small text-primary'>").append(fn.getNama()).append("</th>");
                }
            } else {
                for(CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                    thead.append("<th class='text-center small text-primary'>").append(cpmk.getKode()).append("</th>");
                }
            }
            
            for(CapaianLulusan cpl : capaianLulusans) {
                thead.append("<th class='text-center small text-primary'>").append(cpl.getKode()).append("</th>");
            }
            
            for(ProfilLulusan pl : profilLulusans) {
                thead.append("<th class='text-center small text-primary'>").append(pl.getKode()).append("</th>");
            }
            
            thead.append("</tr></thead>");
            htmlRekapTable.append(thead.toString()).append("<tbody>");

            int indexMhs = 1;
            Double minimalKelulusanMk = kpm.getMinimalKetercapaian() != null ? kpm.getMinimalKetercapaian() : 60.0;

            for (Mahasiswa mahasiswa : hasilUjianMahasiswas) {
                Double totalNilaiAkhirMhs = 0.0;
                Map<Long, Double> konversiFormatNilai = new HashMap<Long, Double>();

                for (FormatNilai formatNilai : formatNilais) {
                    Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(formatNilai.getId());
                    Map<Long, Pertemuan> mapdTgs = mapTugas.get(formatNilai.getId());
                    Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(formatNilai.getId());
                    Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(formatNilai.getId());

                    if (mapd != null || mapdTgs != null || mapdTgsLanjut != null || mapdTgsKelompok != null) {
                        Double totalNilaiPerformatNilai = recalculateTotalPerFormat(dataNiliasUtama, persensData, mahasiswa.getId(), formatNilai.getId(), mapd, mapdTgs, mapdTgsLanjut, mapdTgsKelompok);
                        Double hasilKonversi = hitungKonversi(formatNilai, totalNilaiPerformatNilai);
                        
                        konversiFormatNilai.put(formatNilai.getId(), hasilKonversi);
                        totalNilaiAkhirMhs += totalNilaiPerformatNilai;
                    }
                }

                String statusLulus = totalNilaiAkhirMhs >= minimalKelulusanMk ? "<span class='text-success fw-bold'>" + Common.getBahasaConfig("Lulus") + "</span>" : "<span class='text-danger fw-bold'>" + Common.getBahasaConfig("Tidak Lulus") + "</span>";

                StringBuilder rowMhs = new StringBuilder("<tr>");
                rowMhs.append("<td class='text-center text-muted'>").append(indexMhs++).append("</td>");
                
                // MENGUBAH NIM MENJADI LINK UNTUK MUNCULKAN POPUP DETAIL
                rowMhs.append("<td class='fw-bold text-dark'><a href='javascript:void(0);' class='text-decoration-none' onclick='window.lihatDetailObe").append(rnd).append("(\"").append(mahasiswa.getId()).append("\")'>").append(mahasiswa.getNim()).append("</a></td>");
                
                rowMhs.append("<td>").append(mahasiswa.getNama()).append("</td>");
                rowMhs.append("<td class='text-center fw-bold'>").append(Common.numberFormat.get().format(totalNilaiAkhirMhs)).append("</td>");
                rowMhs.append("<td class='text-center'>").append(statusLulus).append("</td>");

                // KOLOM CPMK
                if (isNilaiCpmk) {
                    for(FormatNilai fn : formatNilais) {
                        Double val = konversiFormatNilai.get(fn.getId());
                        if (val == null) val = 0.0;
                        sumCpmkClass.put(fn.getId(), (sumCpmkClass.get(fn.getId()) == null ? 0.0 : sumCpmkClass.get(fn.getId())) + val);
                        rowMhs.append("<td class='text-center bg-light'>").append(Common.numberFormat.get().format(val)).append("</td>");
                    }
                } else {
                    for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                        List<FormatNilai> fns = mapCpmkIdToFns.get(cpmk.getId());
                        Double sumKonvWeight = 0.0, sumWeight = 0.0;
                        if(fns != null) {
                            for (FormatNilai fn : fns) {
                                Double kVal = konversiFormatNilai.get(fn.getId()) != null ? konversiFormatNilai.get(fn.getId()) : 0.0;
                                sumKonvWeight += (kVal * (fn.getPersen() != null ? fn.getPersen() : 0.0));
                                sumWeight += (fn.getPersen() != null ? fn.getPersen() : 0.0);
                            }
                        }
                        Double scoreCpmk = sumWeight > 0 ? (sumKonvWeight / sumWeight) : 0.0;
                        sumCpmkClass.put(cpmk.getId(), (sumCpmkClass.get(cpmk.getId()) == null ? 0.0 : sumCpmkClass.get(cpmk.getId())) + scoreCpmk);
                        rowMhs.append("<td class='text-center bg-light'>").append(Common.numberFormat.get().format(scoreCpmk)).append("</td>");
                    }
                }

                // KOLOM CPL
                Map<Long, Double> cplScoresMhs = new HashMap<Long, Double>();
                for (CapaianLulusan cpl : capaianLulusans) {
                    Double sumCplConv = 0.0, sumCplWeight = 0.0;
                    for(FormatNilai fn : formatNilais) {
                        if (fn.getCapaianPembelajaranLulusan() != null && cpl.getCapaianPembelajaranLulusan() != null) {
                            String keyId = fn.getCapaianPembelajaranLulusan().getId() + "";
                            if (cpl.getCapaianPembelajaranLulusan().contains("," + keyId + ",")) {
                                Double kVal = konversiFormatNilai.get(fn.getId()) != null ? konversiFormatNilai.get(fn.getId()) : 0.0;
                                sumCplConv += (kVal * (fn.getPersen() != null ? fn.getPersen() : 0.0));
                                sumCplWeight += (fn.getPersen() != null ? fn.getPersen() : 0.0);
                            }
                        }
                    }
                    Double finalCplConv = sumCplWeight > 0 ? (sumCplConv / sumCplWeight) : 0.0;
                    cplScoresMhs.put(cpl.getId(), finalCplConv);
                    sumCplClass.put(cpl.getId(), (sumCplClass.get(cpl.getId()) == null ? 0.0 : sumCplClass.get(cpl.getId())) + finalCplConv);
                    rowMhs.append("<td class='text-center bg-light'>").append(Common.numberFormat.get().format(finalCplConv)).append("</td>");
                }

                // KOLOM PL
                for (ProfilLulusan pl : profilLulusans) {
                    List<CapaianLulusan> mappedCpls = listProfilLulusanToCpl.get(pl.getId());
                    Double sumPl = 0.0;
                    int countPl = 0;
                    if (mappedCpls != null) {
                        for(CapaianLulusan cpl : mappedCpls) {
                            Double score = cplScoresMhs.get(cpl.getId());
                            if(score != null) {
                                sumPl += score;
                                countPl++;
                            }
                        }
                    }
                    Double finalPlConv = countPl > 0 ? (sumPl / countPl) : 0.0;
                    sumPlClass.put(pl.getId(), (sumPlClass.get(pl.getId()) == null ? 0.0 : sumPlClass.get(pl.getId())) + finalPlConv);
                    rowMhs.append("<td class='text-center bg-light fw-bold text-secondary'>").append(Common.numberFormat.get().format(finalPlConv)).append("</td>");
                }

                rowMhs.append("</tr>");
                htmlRekapTable.append(rowMhs.toString());
            }
            htmlRekapTable.append("</tbody>");

            // Persiapkan Data Array JSON Untuk Charts
            if (isNilaiCpmk) {
                for (FormatNilai fn : formatNilais) {
                    cpmkLabels.put(fn.getNama());
                    Double total = sumCpmkClass.get(fn.getId()) == null ? 0.0 : sumCpmkClass.get(fn.getId());
                    cpmkDataList.put(numStudents > 0 ? (total / numStudents) : 0.0);
                }
            } else {
                for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                    cpmkLabels.put(cpmk.getKode());
                    Double total = sumCpmkClass.get(cpmk.getId()) == null ? 0.0 : sumCpmkClass.get(cpmk.getId());
                    cpmkDataList.put(numStudents > 0 ? (total / numStudents) : 0.0);
                }
            }

            for (CapaianLulusan cpl : capaianLulusans) {
                cplLabels.put(cpl.getKode());
                Double total = sumCplClass.get(cpl.getId()) == null ? 0.0 : sumCplClass.get(cpl.getId());
                cplDataList.put(numStudents > 0 ? (total / numStudents) : 0.0);
            }

            for (ProfilLulusan pl : profilLulusans) {
                plLabels.put(pl.getKode());
                Double total = sumPlClass.get(pl.getId()) == null ? 0.0 : sumPlClass.get(pl.getId());
                plDataList.put(numStudents > 0 ? (total / numStudents) : 0.0);
            }
        }
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:583");
    htmlRekapTable.append("<tr><td colspan='100%' class='text-center text-danger py-4'><i class='fas fa-exclamation-triangle me-2'></i>").append(Common.getBahasaConfig("Terjadi kesalahan saat memuat data evaluasi.")).append("</td></tr>");
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rekap.jsp:586");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
}
%>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4">
        
        <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3 flex-wrap gap-3">
            <div>
                <h6 class="fw-bold text-primary mb-1"><i class="fas fa-clipboard-list me-2"></i><%=Common.getBahasaConfig("Rekapitulasi Capaian Mahasiswa")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Matriks ketercapaian nilai CPMK, CPL, dan PL untuk masing-masing mahasiswa dalam satu kelas.")%></small>
            </div>
            <% if (perkuliahan != null && htmlRekapTable.length() > 0) { %>
            <div class="d-flex gap-2">
                <button type="button" class="btn btn-sm btn-outline-success fw-bold px-3 shadow-sm rounded-pill" onclick="downloadExcelRekap<%=rnd%>()">
                    <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%>
                </button>
            </div>
            <% } %>
        </div>

        <% if (!listPerkuliahanTersedia.isEmpty() && (!isMahasiswa || (isMahasiswa && listPerkuliahanTersedia.size() > 1))) { %>
            <div class="card bg-light border-0 shadow-none rounded-3 p-3 mb-4 d-flex flex-row align-items-center justify-content-between flex-wrap gap-3">
                <div>
                    <label class="form-label fw-bold text-secondary mb-0"><i class="fas fa-chalkboard text-info me-2"></i><%=Common.getBahasaConfig("Pilih Jadwal Perkuliahan")%></label>
                    <small class="text-muted d-block"><%=Common.getBahasaConfig("Tentukan jadwal kelas untuk menampilkan rekapitulasi evaluasinya.")%></small>
                </div>
                <div style="min-width: 350px; flex-grow: 1; max-width: 500px;">
                    <select class="form-select shadow-sm border-info fw-bold text-dark" onchange="if(this.value) { window.pilihPerkuliahanRekap<%=rnd%>(this.value); }" <%= listPerkuliahanTersedia.size() == 1 ? "disabled" : "" %>>
                        <% if (listPerkuliahanTersedia.size() > 1) { %><option value=""><%=Common.getBahasaConfig("-- Pilih Jadwal Perkuliahan --")%></option><% } %>
                        <% for(Perkuliahan p : listPerkuliahanTersedia) { 
                            String namaKelas = p.infoSimple();
                            String smt = p.getIdSmt() != null ? p.getIdSmt() : "";
                            String selected = (perkuliahan != null && p.getId().equals(perkuliahan.getId())) ? "selected" : "";
                        %>
                            <option value="<%=p.getId()%>" <%=selected%>><%=smt%> - <%=namaKelas%></option>
                        <% } %>
                    </select>
                </div>
            </div>
            <script>
                window.pilihPerkuliahanRekap<%=rnd%> = function(idPerkuliahanPilih) {
                    var urlLoad = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_rekap&kurikulumPunyaMatakuliah=<%=kpm.getId()%>&var=<%=variable%>&perkuliahan=" + idPerkuliahanPilih;
                    if (typeof loadContentIntoContainer === 'function') {
                        loadContentIntoContainer(urlLoad, 'containerRekap<%=variable%>');
                    } else {
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Fungsi pemuat halaman tidak ditemukan.")%>', 'bg-danger text-white');
                    }
                };
            </script>
        <% } else if (listPerkuliahanTersedia.isEmpty()) { %>
            <div class="alert alert-warning border-0 shadow-sm rounded-3 py-3 d-flex align-items-center mb-4">
                <i class="fas fa-exclamation-triangle fs-3 me-3 text-warning"></i>
                <div>
                    <strong class="text-dark"><%=Common.getBahasaConfig("Tidak Ada Kelas Terkait")%></strong><br>
                    <small class="text-dark"><%=Common.getBahasaConfig("Anda tidak terdaftar pada jadwal perkuliahan aktif mana pun yang menggunakan RPS ini.")%></small>
                </div>
            </div>
        <% } %>

        <% if (perkuliahan == null) { %>
            <div class="text-center py-5">
                <i class="fas fa-clipboard-list text-muted fs-1 mb-3 opacity-50"></i>
                <p class="text-muted mb-0"><%=Common.getBahasaConfig("Silakan pilih Jadwal Perkuliahan di atas terlebih dahulu untuk menampilkan Rekap.")%></p>
            </div>
        <% } else if (htmlRekapTable.length() > 0) { %>

            <div class="table-responsive border rounded-4 shadow-sm pb-2 mb-5" style="max-height: 600px; overflow-y: auto;">
                <table class="table table-bordered table-hover align-middle mb-0 small text-nowrap" id="tableRekapObe<%=rnd%>">
                    <%= htmlRekapTable.toString() %>
                </table>
            </div>

            <div class="row g-4 mt-2">
                <div class="col-lg-6 col-md-12">
                    <div class="card shadow-sm border-0 h-100">
                        <div class="card-header bg-white text-center fw-bold py-3 border-0" style="color: #4e73df;">
                            <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Rata-rata CPMK")%>
                        </div>
                        <div class="card-body pt-0 pb-4 px-4">
                            <div id="chartCpmk<%=rnd%>" class="el-css-chart"></div>
                        </div>
                    </div>
                </div>
                <div class="col-lg-6 col-md-12">
                    <div class="card shadow-sm border-0 h-100">
                        <div class="card-header bg-white text-center fw-bold py-3 border-0" style="color: #1cc88a;">
                            <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Rata-rata CPL")%>
                        </div>
                        <div class="card-body pt-0 pb-4 px-4">
                            <div id="chartCpl<%=rnd%>" class="el-css-chart"></div>
                        </div>
                    </div>
                </div>
                <div class="col-lg-6 col-md-12">
                    <div class="card shadow-sm border-0 h-100">
                        <div class="card-header bg-white text-center fw-bold py-3 border-0" style="color: #1cc88a;">
                            <i class="fas fa-spider me-2"></i><%=Common.getBahasaConfig("Spider Web CPL")%>
                        </div>
                        <div class="card-body pt-0 pb-4 px-4 d-flex justify-content-center align-items-center">
                            <div id="chartRadarCpl<%=rnd%>" class="el-css-chart"></div>
                        </div>
                    </div>
                </div>
                <div class="col-lg-6 col-md-12">
                    <div class="card shadow-sm border-0 h-100">
                        <div class="card-header bg-white text-center fw-bold py-3 border-0" style="color: #f6c23e;">
                            <i class="fas fa-spider me-2"></i><%=Common.getBahasaConfig("Spider Web Profil Lulusan (PL)")%>
                        </div>
                        <div class="card-body pt-0 pb-4 px-4 d-flex justify-content-center align-items-center">
                            <div id="chartRadarPl<%=rnd%>" class="el-css-chart"></div>
                        </div>
                    </div>
                </div>
            </div>

        <% } %>
    </div>
</div>

<script>
    window.lihatDetailObe<%=rnd%> = async function(idMhs) {
        try {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Memuat rincian penilaian...")%>', 'bg-info text-white');
            
            let url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_penilaian_obe&kurikulumPunyaMatakuliah=<%=kpm != null ? kpm.getId() : ""%>&perkuliahan=<%=perkuliahan != null ? perkuliahan.getId() : ""%>&mahasiswa=' + idMhs;
            let response = await fetch(url);
            let htmlContent = await response.text();
            
            if (typeof showConfirmModal === 'function') {
                showConfirmModal(htmlContent, function(){});
                setTimeout(function() {
                    var modalDialog = document.querySelector('.modal-dialog');
                    if (modalDialog) {
                        modalDialog.style.maxWidth = '98%';
                        modalDialog.style.width = '98%';
                    }
                }, 100);
            } else {
                tampilkanToast('<%=Common.getBahasaConfigJS("Fungsi modal tidak ditemukan.")%>', "bg-warning");
            }
        } catch(e) {
            console.error(e);
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal memuat detail nilai.")%>', "bg-danger text-white");
        }
    };

    const downloadExcelRekap<%=rnd%> = () => {
        try {
            if(typeof XLSX !== 'undefined') {
                const wb = XLSX.utils.book_new();
                if (document.getElementById('tableRekapObe<%=rnd%>')) {
                    const ws1 = XLSX.utils.table_to_sheet(document.getElementById('tableRekapObe<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws1, "<%=Common.getBahasaConfig("Rekap Ketercapaian Mahasiswa")%>");
                }
                XLSX.writeFile(wb, "<%=namaFileLaporan%>.xlsx");
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Library Excel tidak dimuat. Gagal mengunduh.")%>', 'bg-warning text-dark');
            }
        } catch(e) { console.error(e); }
    };

    // FUNGSI UNTUK MERENDER GRAFIK HTML/CSS (TANPA CHART.JS)

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

    function renderBarCharts<%=rnd%>() {
        <% if (perkuliahan != null && cpmkLabels.length() > 0) { %>
        try {
            elRenderBar('chartCpmk<%=rnd%>', <%=cpmkLabels.toString()%>, <%=cpmkDataList.toString()%>);
            elRenderBar('chartCpl<%=rnd%>', <%=cplLabels.toString()%>, <%=cplDataList.toString()%>);
            elRenderRadar('chartRadarCpl<%=rnd%>', <%=cplLabels.toString()%>, <%=cplDataList.toString()%>);
            elRenderRadar('chartRadarPl<%=rnd%>', <%=plLabels.toString()%>, <%=plDataList.toString()%>);
        } catch(e) { console.error('Error rendering HTML/CSS charts:', e); }
        <% } %>
    }
    setTimeout(renderBarCharts<%=rnd%>, 120)
</script>