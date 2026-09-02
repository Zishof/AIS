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
    // ==========================================
    // DEBUG MODE HELPERS
    // ==========================================
    private void debugLog(String message) { /* debug logging disabled in production */ }

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
                debugLog("Extracted Bobot for Key " + key + " (Format " + formatId + ") -> " + bobot);
            }
        } catch (Exception e) {
            debugLog("ERROR extracting bobot from JSON: " + jsonStr);
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_asesmen.jsp:48");
        }
    }

    private <T> Double sumNilai(Collection<T> items, Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData, Long mhsId, Long formatId, String prefix) {
        Double sum = 0.0;
        for (T item : items) {
            try {
                Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
                String key = prefix + "_" + itemId;
                Double nilia = 0.0;
                try { nilia = data.get(mhsId).get(formatId).get(key); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_asesmen.jsp:59");}
                if(nilia == null) nilia = 0.0;
                Double persenData = 0.0;
                try { persenData = (pData.get(key + "_" + formatId) * 0.01) * nilia; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_asesmen.jsp:62");}
                if(persenData != null) sum += persenData;
            } catch (Exception e) {
                debugLog("ERROR sumNilai on item " + prefix);
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_asesmen.jsp:66");
            }
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
                    try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_asesmen.jsp:93");}
                }
            }
        }
        return longs;
    }
    
    // Konversi Angka ke Huruf Mutu Standar
    private String getHurufMutu(Double nilai) {
        if(nilai >= 85) return "A";
        if(nilai >= 80) return "A-";
        if(nilai >= 75) return "B+";
        if(nilai >= 70) return "B";
        if(nilai >= 65) return "B-";
        if(nilai >= 60) return "C+";
        if(nilai >= 55) return "C";
        if(nilai >= 45) return "D";
        return "E";
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

debugLog("--- START LOAD ASESMEN --- KPM ID: " + idKpmStr + " | Perkuliahan ID: " + idPerkuliahanStr);

KurikulumPunyaMatakuliah kpm = null;
Perkuliahan perkuliahan = null;
List<Perkuliahan> listPerkuliahanTersedia = new ArrayList<Perkuliahan>();

StringBuilder htmlAsesmenTable = new StringBuilder();
String namaFileLaporan = "Laporan_Asesmen_OBE";

Session sess = HibernateUtil.openSession();
boolean isNilaiCpmk = false;

try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
    if (idPerkuliahanStr != null && !idPerkuliahanStr.trim().isEmpty() && !idPerkuliahanStr.equals("null")) {
        perkuliahan = (Perkuliahan) sess.get(Perkuliahan.class, Long.parseLong(idPerkuliahanStr));
    }
    
    if (kpm != null) {
        isNilaiCpmk = kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk();
        debugLog("isNilaiCpmk: " + isNilaiCpmk);

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
        debugLog("List Perkuliahan Available: " + listPerkuliahanTersedia.size());
    }

    if (kpm == null) {
        debugLog("ERROR: KPM is Null");
        out.print("<div class='alert alert-danger m-4'>" + Common.getBahasaConfig("Data Rencana Pembelajaran Semester tidak valid.") + "</div>");
        return;
    }

    if (perkuliahan == null && listPerkuliahanTersedia.size() == 1) {
        perkuliahan = listPerkuliahanTersedia.get(0);
        debugLog("Auto-selected single Perkuliahan: " + perkuliahan.getId());
    }

    if (perkuliahan != null) {
        namaFileLaporan = "Laporan_Asesmen_OBE_" + perkuliahan.infoSimple().replaceAll("[^a-zA-Z0-9_\\\\-]", "_");
        List<Mahasiswa> hasilUjianMahasiswas = perkuliahan.ambilMahasiswa();
        debugLog("Total Mahasiswa in Class: " + hasilUjianMahasiswas.size());
        
        if(isMahasiswa) {
            List<Mahasiswa> tempMhs = new ArrayList<Mahasiswa>();
            for(Mahasiswa m : hasilUjianMahasiswas) {
                if(m.getId().equals(tbmuser.getMahasiswa().getId())) { tempMhs.add(m); break; }
            }
            hasilUjianMahasiswas = tempMhs;
            debugLog("Filtered for Mahasiswa view. Total: " + hasilUjianMahasiswas.size());
        }

        if (hasilUjianMahasiswas.isEmpty()) {
            htmlAsesmenTable.append("<tr><td colspan='100%' class='text-center py-5 fst-italic text-muted'>").append(Common.getBahasaConfig("Belum ada mahasiswa yang mengambil kelas ini / Akses dibatasi.")).append("</td></tr>");
        } else {
            Matakuliah matakuliah = kpm.getMatakuliah();

            // 1. Ekstrak CPL
            List<CapaianLulusan> capaianLulusans = new ArrayList<CapaianLulusan>();
            if(matakuliah != null) {
                final Set<Long> longs = parseIdsToSet(matakuliah.getCapaianLulusan());
                capaianLulusans = ConstantValues.simpleList(
                        sess.createCriteria(CapaianLulusan.class)
                                .add(longs.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longs))
                                .addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                        CapaianLulusan.class);
                debugLog("Extracted CPL count: " + capaianLulusans.size());
            }

            // 2. Ekstrak Penilaian
            List<PertemuanPunyaUjian> pertemuanPunyaUjians = ConstantValues.simpleList(
                sess.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "pertemuan").addOrder(Order.asc("mulaiUjian")).addOrder(Order.asc("sampaiUjian")).createAlias("ujian", "ujian").add(Restrictions.eq("ujian.aktif", true)).add(Restrictions.eq("pertemuan.perkuliahan", perkuliahan)), PertemuanPunyaUjian.class
            );

            List<Pertemuan> pertemuansTugas = ConstantValues.simpleList(
                sess.createCriteria(Pertemuan.class).add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas")).addOrder(Order.asc("mulai")).add(Restrictions.eq("aktif", true)).add(Restrictions.eq("perkuliahan", perkuliahan)), Pertemuan.class
            );

            Collection<Long> pertemuansList = perkuliahan.ambilPertemuan().values();
            List<TugasPertemuan> pertemuansTugasLanjut = pertemuansList.isEmpty() ? new ArrayList<TugasPertemuan>() : ConstantValues.simpleList(
                sess.createCriteria(TugasPertemuan.class).add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas")).addOrder(Order.asc("mulai")).add(Restrictions.in("pertemuan", pertemuansList)), TugasPertemuan.class
            );

            List<TugasKelompok> pertemuansTugasKelompoks = ConstantValues.simpleList(
                sess.createCriteria(TugasKelompok.class).add(Restrictions.ne("judul", "")).add(Restrictions.isNotNull("judul")).addOrder(Order.asc("mulai")).add(Restrictions.eq("perkuliahan", perkuliahan)), TugasKelompok.class
            );

            List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan);
            debugLog("Extracted Format Nilai count: " + formatNilais.size());

            // 3. Mapping CPMK
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
                        return (a.getKode() == null ? "" : a.getKode()).compareTo(b.getKode() == null ? "" : b.getKode());
                    }
                });
                debugLog("Unique CPMK Mapped from Sub-CPMK: " + listCpmkUnique.size());
            }

            // 4. Hitung Nilai Mentah & Konversi
            Map<Long, Map<Long, Map<String, Double>>> dataNiliasUtama = new HashMap<Long, Map<Long, Map<String, Double>>>();
            Map<Long, Map<String, Double>> dataBobot = new HashMap<Long, Map<String, Double>>();
            Map<Long, Map<Long, PertemuanPunyaUjian>> mapPertemuanPunyaUjian = new HashMap<Long, Map<Long, PertemuanPunyaUjian>>();
            Map<Long, Map<Long, Pertemuan>> mapTugas = new HashMap<Long, Map<Long, Pertemuan>>();
            Map<Long, Map<Long, TugasPertemuan>> mapTugasLanjut = new HashMap<Long, Map<Long, TugasPertemuan>>();
            Map<Long, Map<Long, TugasKelompok>> mapTugasKelompok = new HashMap<Long, Map<Long, TugasKelompok>>();
            Map<String, String> mapHasilObe = new HashMap<String, String>();

            if (!pertemuanPunyaUjians.isEmpty() && !hasilUjianMahasiswas.isEmpty()) {
                List<HasilUjianMahasiswa> listHasil = ConstantValues.simpleList(
                    sess.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.or(Restrictions.isNotNull("keyhasil"), Restrictions.isNotNull("nilaiObe"))).add(Restrictions.in("pertemuanPunyaUjian", pertemuanPunyaUjians)).add(Restrictions.in("mahasiswa", hasilUjianMahasiswas)), HasilUjianMahasiswa.class
                );
                for (HasilUjianMahasiswa hum : listHasil) {
                    if (hum.getNilaiObe() != null) mapHasilObe.put(hum.getPertemuanPunyaUjian().getId() + "_" + hum.getMahasiswa().getId(), hum.getNilaiObe());
                }
                debugLog("Results Ujian extracted: " + listHasil.size());
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
                } catch (Exception e) {
                    debugLog("Error Parsing KeteranganNilai on Pertemuan: " + p.getId());
                }
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
                } catch (Exception e) {
                    debugLog("Error Parsing KeteranganNilai on TugasPertemuan: " + tp.getId());
                }
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
                } catch (Exception e) {
                    debugLog("Error Parsing KeteranganNilai on TugasKelompok: " + tk.getId());
                }
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
                        } catch(Exception e){
                            debugLog("Error Parsing JSON Hasil Ujian OBE untuk Mhs " + mhs.getId());
                        }
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
                                persen = (persen / 100.0) * formatNilai.getPersen();
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
            // BUILD THE TABLE HTML
            // ========================================================================
            int colCpmk = isNilaiCpmk ? formatNilais.size() : listCpmkUnique.size();
            int colCpl = capaianLulusans.size();

            StringBuilder thead = new StringBuilder("<thead class='table-light align-middle text-center sticky-top'>");
            thead.append("<tr>");
            thead.append("<th rowspan='2' class='bg-danger bg-opacity-10' style='width: 40px;'>No</th>");
            thead.append("<th rowspan='2' class='bg-danger bg-opacity-10' style='width: 120px;'>NIM</th>");
            thead.append("<th rowspan='2' class='bg-danger bg-opacity-10' style='min-width: 200px;'>Nama Mahasiswa</th>");
            
            if(formatNilais.size() > 0) {
                thead.append("<th colspan='").append(formatNilais.size()).append("' class='bg-primary bg-opacity-10'>Komponen Penilaian (Format)</th>");
            }
            
            thead.append("<th colspan='2' class='bg-warning bg-opacity-10'>Nilai Akhir</th>");
            
            if(colCpmk > 0) {
                thead.append("<th colspan='").append(colCpmk).append("' class='bg-success bg-opacity-10'>Capaian Matakuliah (CPMK)</th>");
            }
            
            if(colCpl > 0) {
                thead.append("<th colspan='").append(colCpl).append("' class='bg-info bg-opacity-10' style='background-color: #ffe8cc;'>Capaian Lulusan (CPL)</th>");
            }
            thead.append("</tr><tr>");

            for(FormatNilai fn : formatNilais) {
                thead.append("<th class='bg-primary bg-opacity-10 small fw-bold'>").append(fn.getNama()).append("<br><small class='fw-normal'>(Bobot ").append(Common.numberFormat.get().format(fn.getPersen())).append("%)</small></th>");
            }
            
            thead.append("<th class='bg-warning bg-opacity-10 small fw-bold'>Angka</th>");
            thead.append("<th class='bg-warning bg-opacity-10 small fw-bold'>Huruf</th>");
            
            if (isNilaiCpmk) {
                for(FormatNilai fn : formatNilais) {
                    thead.append("<th class='bg-success bg-opacity-10 small fw-bold'>").append(fn.getNama()).append("</th>");
                }
            } else {
                for(CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                    thead.append("<th class='bg-success bg-opacity-10 small fw-bold' title='").append(cpmk.getNama()).append("'>").append(cpmk.getKode()).append("</th>");
                }
            }
            
            for(CapaianLulusan cpl : capaianLulusans) {
                thead.append("<th class='small fw-bold' style='background-color: #ffe8cc;' title='").append(cpl.getNama()).append("'>").append(cpl.getKode()).append("</th>");
            }
            thead.append("</tr></thead>");

            htmlAsesmenTable.append(thead.toString()).append("<tbody>");

            int indexMhs = 1;

            debugLog("--- START RENDERING MAHASISWA SCORES ---");
            for (Mahasiswa mahasiswa : hasilUjianMahasiswas) {
                Double totalNilaiAkhirMhs = 0.0;
                Map<Long, Double> baseScoresFn = new HashMap<Long, Double>();

                StringBuilder rowMhs = new StringBuilder("<tr>");
                rowMhs.append("<td class='text-center text-muted'>").append(indexMhs++).append("</td>");
                rowMhs.append("<td class='fw-bold text-dark'>").append(mahasiswa.getNim()).append("</td>");
                rowMhs.append("<td>").append(mahasiswa.getNama()).append("</td>");

                // Calculate Raw Scores and Weighted Scores
                for (FormatNilai formatNilai : formatNilais) {
                    Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(formatNilai.getId());
                    Map<Long, Pertemuan> mapdTgs = mapTugas.get(formatNilai.getId());
                    Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(formatNilai.getId());
                    Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(formatNilai.getId());

                    Double totalNilaiPerformatNilai = 0.0; // Weighted score (contribution to total)
                    Double baseScore = 0.0; // Raw base score out of 100

                    if (mapd != null || mapdTgs != null || mapdTgsLanjut != null || mapdTgsKelompok != null) {
                        totalNilaiPerformatNilai = recalculateTotalPerFormat(dataNiliasUtama, persensData, mahasiswa.getId(), formatNilai.getId(), mapd, mapdTgs, mapdTgsLanjut, mapdTgsKelompok);
                        baseScore = hitungKonversi(formatNilai, totalNilaiPerformatNilai);
                    }
                    
                    baseScoresFn.put(formatNilai.getId(), baseScore);
                    totalNilaiAkhirMhs += totalNilaiPerformatNilai;
                    
                    rowMhs.append("<td class='text-center bg-primary bg-opacity-10'>").append(Common.numberFormat.get().format(baseScore)).append("</td>");
                }

                // Final Score and Grade
                String hurufMutu = getHurufMutu(totalNilaiAkhirMhs);
                rowMhs.append("<td class='text-center fw-bold bg-warning bg-opacity-10 fs-6'>").append(Common.numberFormat.get().format(totalNilaiAkhirMhs)).append("</td>");
                rowMhs.append("<td class='text-center fw-bold bg-warning bg-opacity-10 fs-6'>").append(hurufMutu).append("</td>");

                // CPMK Scores
                if (isNilaiCpmk) {
                    for(FormatNilai fn : formatNilais) {
                        Double val = baseScoresFn.get(fn.getId());
                        if (val == null) val = 0.0;
                        rowMhs.append("<td class='text-center bg-success bg-opacity-10'>").append(Common.numberFormat.get().format(val)).append("</td>");
                    }
                } else {
                    for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                        List<FormatNilai> fns = mapCpmkIdToFns.get(cpmk.getId());
                        Double sumKonvWeight = 0.0, sumWeight = 0.0;
                        if(fns != null) {
                            for (FormatNilai fn : fns) {
                                Double kVal = baseScoresFn.get(fn.getId()) != null ? baseScoresFn.get(fn.getId()) : 0.0;
                                sumKonvWeight += (kVal * (fn.getPersen() != null ? fn.getPersen() : 0.0));
                                sumWeight += (fn.getPersen() != null ? fn.getPersen() : 0.0);
                            }
                        }
                        Double scoreCpmk = sumWeight > 0 ? (sumKonvWeight / sumWeight) : 0.0;
                        rowMhs.append("<td class='text-center bg-success bg-opacity-10'>").append(Common.numberFormat.get().format(scoreCpmk)).append("</td>");
                    }
                }

                // CPL Scores
                for (CapaianLulusan cpl : capaianLulusans) {
                    Double sumCplConv = 0.0, sumCplWeight = 0.0;
                    for(FormatNilai fn : formatNilais) {
                        if (fn.getCapaianPembelajaranLulusan() != null && cpl.getCapaianPembelajaranLulusan() != null) {
                            String keyId = fn.getCapaianPembelajaranLulusan().getId() + "";
                            if (cpl.getCapaianPembelajaranLulusan().contains("," + keyId + ",")) {
                                Double kVal = baseScoresFn.get(fn.getId()) != null ? baseScoresFn.get(fn.getId()) : 0.0;
                                sumCplConv += (kVal * (fn.getPersen() != null ? fn.getPersen() : 0.0));
                                sumCplWeight += (fn.getPersen() != null ? fn.getPersen() : 0.0);
                            }
                        }
                    }
                    Double finalCplConv = sumCplWeight > 0 ? (sumCplConv / sumCplWeight) : 0.0;
                    rowMhs.append("<td class='text-center' style='background-color: #fff3e6;'>").append(Common.numberFormat.get().format(finalCplConv)).append("</td>");
                }

                rowMhs.append("</tr>");
                htmlAsesmenTable.append(rowMhs.toString());
            }
            debugLog("--- END RENDERING MAHASISWA SCORES ---");
            htmlAsesmenTable.append("</tbody>");
        }
    }
} catch (Exception e) {
    debugLog("FATAL ERROR IN ASESMEN: " + e.getMessage());
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_asesmen.jsp:533");
    htmlAsesmenTable.append("<tr><td colspan='100%' class='text-center text-danger py-4'><i class='fas fa-exclamation-triangle me-2'></i>").append(Common.getBahasaConfig("Terjadi kesalahan saat memuat data evaluasi.")).append("</td></tr>");
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_asesmen.jsp:536");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
}
%>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4">
        
        <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3 flex-wrap gap-3">
            <div>
                <h6 class="fw-bold text-primary mb-1"><i class="fas fa-file-invoice me-2"></i><%=Common.getBahasaConfig("Asesmen / Laporan Penilaian Kelas")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Matriks detail nilai komponen mentah (Tugas, UTS, UAS) yang diekstrapolasi menjadi pencapaian CPMK dan CPL.")%></small>
            </div>
            <% if (perkuliahan != null && htmlAsesmenTable.length() > 0) { %>
            <div class="d-flex gap-2">
                <button type="button" class="btn btn-sm btn-outline-success fw-bold px-3 shadow-sm rounded-pill" onclick="downloadExcelAsesmen<%=rnd%>()">
                    <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Laporan Asesmen")%>
                </button>
            </div>
            <% } %>
        </div>

        <% if (!listPerkuliahanTersedia.isEmpty() && (!isMahasiswa || (isMahasiswa && listPerkuliahanTersedia.size() > 1))) { %>
            <div class="card bg-light border-0 shadow-none rounded-3 p-3 mb-4 d-flex flex-row align-items-center justify-content-between flex-wrap gap-3">
                <div>
                    <label class="form-label fw-bold text-secondary mb-0"><i class="fas fa-chalkboard text-info me-2"></i><%=Common.getBahasaConfig("Pilih Jadwal Perkuliahan")%></label>
                    <small class="text-muted d-block"><%=Common.getBahasaConfig("Tentukan jadwal kelas untuk menampilkan laporan asesmen nilainya.")%></small>
                </div>
                <div style="min-width: 350px; flex-grow: 1; max-width: 500px;">
                    <select class="form-select shadow-sm border-info fw-bold text-dark" onchange="if(this.value) { window.pilihPerkuliahanAsesmen<%=rnd%>(this.value); }" <%= listPerkuliahanTersedia.size() == 1 ? "disabled" : "" %>>
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
                window.pilihPerkuliahanAsesmen<%=rnd%> = function(idPerkuliahanPilih) {
                    var urlLoad = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_asesmen&kurikulumPunyaMatakuliah=<%=kpm.getId()%>&var=<%=variable%>&perkuliahan=" + idPerkuliahanPilih;
                    if (typeof loadContentIntoContainer === 'function') {
                        loadContentIntoContainer(urlLoad, 'wadahAsesmen_<%=variable%>'); 
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
                <i class="fas fa-file-signature text-muted fs-1 mb-3 opacity-50"></i>
                <p class="text-muted mb-0"><%=Common.getBahasaConfig("Silakan pilih Jadwal Perkuliahan di atas terlebih dahulu untuk menampilkan Asesmen Laporan.")%></p>
            </div>
        <% } else if (htmlAsesmenTable.length() > 0) { %>
            <div class="table-responsive border rounded-4 shadow-sm pb-2 mb-5" style="max-height: 650px; overflow-x: auto; white-space: nowrap;">
                <table class="table table-bordered table-hover align-middle mb-0 small" id="tableAsesmenObe<%=rnd%>">
                    <%= htmlAsesmenTable.toString() %>
                </table>
            </div>
        <% } %>
    </div>
</div>

<script>
    const downloadExcelAsesmen<%=rnd%> = () => {
        try {
            if(typeof XLSX !== 'undefined') {
                const wb = XLSX.utils.book_new();
                if (document.getElementById('tableAsesmenObe<%=rnd%>')) {
                    const ws1 = XLSX.utils.table_to_sheet(document.getElementById('tableAsesmenObe<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws1, "<%=Common.getBahasaConfig("Laporan Asesmen")%>");
                }
                XLSX.writeFile(wb, "<%=namaFileLaporan%>.xlsx");
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Library Excel tidak dimuat. Gagal mengunduh.")%>', 'bg-warning text-dark');
            }
        } catch(e) { console.error(e); }
    };
</script>
