<%@page import="java.util.*"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.math.RoundingMode"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.obe.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%!
    // =========================================================================
    // HELPER METHODS UNTUK KALKULASI OBE
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
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:42");}
    }

    private <T> Double sumNilai(Collection<T> items, Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData, Long mhsId, Long formatId, String prefix) {
        Double sum = 0.0;
        for (T item : items) {
            try {
                Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
                String key = prefix + "_" + itemId;
                Double nilia = 0.0;
                try { nilia = data.get(mhsId).get(formatId).get(key); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:52");}
                if(nilia == null) nilia = 0.0;
                Double persenData = 0.0;
                try { persenData = (pData.get(key + "_" + formatId) * 0.01) * nilia; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:55");}
                if(persenData != null) sum += persenData;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:57");}
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

    private String hitungHurufObe(Double hasilKonversi, Mahasiswa mahasiswa, Perkuliahan perkuliahan) {
        ais.database.model.Jurusan jrsHuruf = (mahasiswa == null) ? perkuliahan.getJurusan() : mahasiswa.getJurusan();
        ais.database.model.Fakultas fakHuruf = (jrsHuruf == null) ? null : jrsHuruf.getFakultas();
        NilaiHuruf huruf = Common.getNilaiHuruf(hasilKonversi, mahasiswa == null ? 0 : mahasiswa.getTahunangkatan(),
                jrsHuruf, fakHuruf,
                perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap(), perkuliahan.getMatakuliah().getKode(), true,
                true, perkuliahan.getMatakuliah().getJenisNilaiHuruf());
        return huruf == null ? "-" : huruf.getNilaiHuruf();
    }

    private Double hitungKonversi(FormatNilai formatNilai, Double totalNilaiPerformatNilai) {
        double persenFn = formatNilai.getPersen() != null ? formatNilai.getPersen() : 0.0;
        BigDecimal b15 = new BigDecimal(persenFn / 100.0);
        BigDecimal bn34 = new BigDecimal(totalNilaiPerformatNilai);
        BigDecimal divisor = (b15.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ONE : b15;
        return bn34.divide(divisor, 2, RoundingMode.HALF_UP).doubleValue();
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

StringBuilder htmlTabel1 = new StringBuilder();
StringBuilder htmlTabel2 = new StringBuilder();
StringBuilder htmlTabel3 = new StringBuilder();
StringBuilder htmlTabel4 = new StringBuilder();
StringBuilder htmlTabel5 = new StringBuilder();
StringBuilder htmlHasilCpmk = new StringBuilder();
StringBuilder htmlExportHasil = new StringBuilder();

// VARIABEL UNTUK TAB AGREGASI CPMK (Jika isNilaiCpmk == false)
StringBuilder htmlTabel1_Cpmk = new StringBuilder();
StringBuilder htmlTabel2_Cpmk = new StringBuilder();
StringBuilder htmlHasil_CpmkAgg = new StringBuilder();
StringBuilder htmlExportCpmkAgg = new StringBuilder();

// VARIABEL UNTUK TAB CPL
StringBuilder htmlHasilCpl = new StringBuilder();
StringBuilder htmlExportHasilCpl = new StringBuilder();

// VARIABEL UNTUK TAB PL (PROFIL LULUSAN)
StringBuilder htmlTabel6 = new StringBuilder(); // Penilaian PL
StringBuilder htmlTabel7 = new StringBuilder(); // Rangkuman PL
StringBuilder htmlHasilPl = new StringBuilder(); // Grafik PL
StringBuilder htmlExportHasilPl = new StringBuilder(); // Export PL

String namaFileLaporan = "Laporan_OBE_Data";

Session sess = HibernateUtil.openSession();
boolean isNilaiCpmk = false;

try {
    kpm = (KurikulumPunyaMatakuliah) sess.get(KurikulumPunyaMatakuliah.class, Long.parseLong(idKpmStr));
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

    // --------------------------------------------------------------------------------------------------
    // ENGINE PENARIKAN DATA JIKA PERKULIAHAN SUDAH DIPILIH
    // --------------------------------------------------------------------------------------------------
    if (perkuliahan != null) {
        namaFileLaporan = "Laporan_OBE_" + perkuliahan.infoSimple().replaceAll("[^a-zA-Z0-9_\\\\-]", "_");
        List<Mahasiswa> hasilUjianMahasiswas = perkuliahan.ambilMahasiswa();
        
        if(isMahasiswa) {
            List<Mahasiswa> tempMhs = new ArrayList<Mahasiswa>();
            for(Mahasiswa m : hasilUjianMahasiswas) {
                if(m.getId().equals(tbmuser.getMahasiswa().getId())) { tempMhs.add(m); break; }
            }
            hasilUjianMahasiswas = tempMhs;
        }

        if (hasilUjianMahasiswas.isEmpty()) {
            htmlTabel1.append("<tr><td colspan='100%' class='text-center py-5 fst-italic text-muted'>").append(Common.getBahasaConfig("Belum ada mahasiswa yang mengambil kelas ini.")).append("</td></tr>");
        } else {
            int size = hasilUjianMahasiswas.size();
            HashSet<Long> longsCPL = new HashSet<Long>();
            if (perkuliahan.getMatakuliah().getCapaianLulusan() != null) {
                for (String d : perkuliahan.getMatakuliah().getCapaianLulusan().split(",")) {
                    if (!d.trim().isEmpty()) { try { longsCPL.add(Long.parseLong(d.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:193");} }
                }
            }
            
            List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(
                sess.createCriteria(CapaianLulusan.class)
                    .add(longsCPL.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longsCPL))
                    .addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), 
                CapaianLulusan.class
            );

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

            // AGREGASI CPL KE PL (PROFIL LULUSAN)
            List<ProfilLulusan> profilLulusans = new ArrayList<ProfilLulusan>();
            Map<Long, List<CapaianLulusan>> listProfilLulusanToCpl = new HashMap<Long, List<CapaianLulusan>>();
            try {
                // Fetch all active PLs safely
                List<ProfilLulusan> allPls = ConstantValues.simpleList(sess.createCriteria(ProfilLulusan.class)
                    .addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), ProfilLulusan.class);
                
                // Map cross-references using Java Reflection securely
                for(ProfilLulusan pl : allPls) {
                    for(CapaianLulusan cpl : capaianLulusans) {
                    	String idBaru = pl.getId() + "_" + kpm.getId();
                    	String pStr = cpl.getProfil() != null ? cpl.getProfil() : "";
                        String paddedStr = "," + pStr + ",";
                        boolean isMapped = paddedStr.contains("," + cpl.getId() + ",") || paddedStr.contains("," + idBaru + ",");
                        if(isMapped) {
                            List<CapaianLulusan> cpls = listProfilLulusanToCpl.get(pl.getId());
                            if(cpls == null) { cpls = new ArrayList<CapaianLulusan>(); listProfilLulusanToCpl.put(pl.getId(), cpls); }
                            cpls.add(cpl);
                        }
                    }
                    if(listProfilLulusanToCpl.containsKey(pl.getId())) profilLulusans.add(pl);
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:261");}

            // Mapping Agregasi CPMK (Jika isNilaiCpmk == false)
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

            // Parsing Data
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
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:322");}
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
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:338");}
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
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:354");}
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
                        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:372");}
                    }
                }
            }

            Map<Long, List<FormatNilai>> listCapaianLulusan = new HashMap<Long, List<FormatNilai>>();
            for (FormatNilai formatNilai : formatNilais) {
                if (formatNilai.getCapaianPembelajaranLulusan() != null) {
                    String key = formatNilai.getCapaianPembelajaranLulusan().getId() + "";
                    for (CapaianLulusan capaianLulusan : capaianLulusans) {
                        if (capaianLulusan.getCapaianPembelajaranLulusan() != null && capaianLulusan.getCapaianPembelajaranLulusan().contains("," + key + ",")) {
                            List<FormatNilai> formatNilaisData = listCapaianLulusan.get(capaianLulusan.getId());
                            if (formatNilaisData == null) { formatNilaisData = new ArrayList<FormatNilai>(); listCapaianLulusan.put(capaianLulusan.getId(), formatNilaisData); }
                            formatNilaisData.add(formatNilai);
                        }
                    }
                }

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

            // ========================================================================
            // MEMBANGUN HEADER TABEL 1: REKAPITULASI UTAMA (CPMK ATAU SUB-CPMK)
            // ========================================================================
            StringBuilder hTop = new StringBuilder("<tr><th rowspan='3' class='align-middle text-center' style='width: 50px;'>No.</th><th rowspan='3' class='align-middle' style='width: 120px;'>" + Common.getBahasaConfig("NIM") + "</th><th rowspan='3' class='align-middle' style='min-width: 200px;'>" + Common.getBahasaConfig("Nama Mahasiswa") + "</th><th rowspan='3' class='align-middle'>" + Common.getBahasaConfig("Prodi") + "</th>");
            StringBuilder hSub1 = new StringBuilder("<tr>");
            StringBuilder hSub2 = new StringBuilder("<tr>");

            for (FormatNilai formatNilai : formatNilais) {
                Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(formatNilai.getId());
                Map<Long, Pertemuan> mapdTgs = mapTugas.get(formatNilai.getId());
                Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(formatNilai.getId());
                Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(formatNilai.getId());

                if (mapd != null || mapdTgs != null || mapdTgsLanjut != null || mapdTgsKelompok != null) {
                    int colSpan = 3; 

                    StringBuilder tempSub1 = new StringBuilder();
                    StringBuilder tempSub2 = new StringBuilder();

                    class SubHeadFiller {
                        void fill(int cSpan, String name, String keyPrefix, Long itemId) {
                            tempSub1.append("<th class='text-center align-middle'>").append(name).append("</th>");
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
                            tempSub2.append("<th class='text-center text-muted fw-normal'>").append(Common.numberFormat.get().format(persen)).append("%</th>");
                        }
                    }
                    SubHeadFiller filler = new SubHeadFiller();

                    if (mapd != null) for (PertemuanPunyaUjian ppu : mapd.values()) { filler.fill(colSpan, ppu.getUjian().getNama(), PertemuanPunyaUjian.class.getName(), ppu.getId()); colSpan++; }
                    if (mapdTgs != null) for (Pertemuan pt : mapdTgs.values()) { filler.fill(colSpan, pt.getJudultugas(), Pertemuan.class.getName(), pt.getId()); colSpan++; }
                    if (mapdTgsLanjut != null) for (TugasPertemuan tp : mapdTgsLanjut.values()) { filler.fill(colSpan, tp.getJudultugas(), TugasPertemuan.class.getName(), tp.getId()); colSpan++; }
                    if (mapdTgsKelompok != null) for (TugasKelompok tk : mapdTgsKelompok.values()) { filler.fill(colSpan, tk.getJudultugas(), TugasKelompok.class.getName(), tk.getId()); colSpan++; }

                    tempSub1.append("<th class='text-center align-middle bg-light text-primary' title='" + Common.getBahasaConfig("Skala 0-100") + "'>").append(Common.getBahasaConfig("Nilai Capaian")).append("</th>");
                    tempSub1.append("<th class='text-center align-middle bg-light text-primary'>").append(Common.getBahasaConfig("Skala")).append("<br><small class='fw-normal'>(Huruf)</small></th>");
                    tempSub1.append("<th class='text-center align-middle bg-light text-primary'>").append(Common.getBahasaConfig("Status")).append("<br><small class='fw-normal'>(L/TL)</small></th>");
                    
                    tempSub2.append("<th class='text-center text-primary bg-light'>").append(Common.numberFormat.get().format(formatNilai.getPersen())).append("%</th>");
                    tempSub2.append("<th class='bg-light'></th><th class='bg-light'></th>");

                    hTop.append("<th colspan='").append(colSpan).append("' class='text-center align-middle'>").append(formatNilai.getNama()).append("<br><small class='fw-normal text-muted'>Target: ").append(formatNilai.ambilMinimal()).append("</small></th>");
                    hSub1.append(tempSub1.toString());
                    hSub2.append(tempSub2.toString());
                }
            }

            hTop.append("<th colspan='3' class='text-center align-middle text-white bg-secondary'>").append(Common.getBahasaConfig("Nilai Akhir Mata Kuliah")).append("</th></tr>");
            hSub1.append("<th class='text-center align-middle text-white bg-secondary'>").append(Common.getBahasaConfig("Nilai Akhir")).append("<br><small class='fw-normal'>(0-100)</small></th><th class='text-center align-middle text-white bg-secondary'>").append(Common.getBahasaConfig("Huruf Mutu")).append("</th><th class='text-center align-middle text-white bg-secondary'>").append(Common.getBahasaConfig("Status Kelulusan")).append("</th></tr>");
            hSub2.append("<th class='text-white bg-secondary'>100%</th><th class='text-white bg-secondary'></th><th class='text-white bg-secondary'></th></tr>");

            htmlTabel1.append("<thead class='table-light'>").append(hTop.toString()).append(hSub1.toString()).append(hSub2.toString()).append("</thead><tbody>");

            Map<Long, Double> totalKonversi = new HashMap<Long, Double>();
            Map<Long, Double> totalNilai = new HashMap<Long, Double>();
            Map<Long, Map<Long, Double>> totalKonversiMahasiswa = new HashMap<Long, Map<Long, Double>>();
            Map<Long, Map<Long, Double>> totalNilaiMahasiswa = new HashMap<Long, Map<Long, Double>>();
            Map<Long, Integer> totalMemenuhi = new HashMap<Long, Integer>();
            Map<Long, String> blmMemenuhi = new HashMap<Long, String>();
            
            Map<Long, TreeMap<String, Integer>> totalNilaiHuruf = new HashMap<Long, TreeMap<String, Integer>>();
            TreeMap<String, Integer> sebaranHurufAkhirMhs = new TreeMap<String, Integer>();

            Double sumNilaiAkhirKelas = 0.0;
            int indexMhs = 1;
            int totalEcampusLulus = 0;
            int totalEcampusTidakLulus = 0;
            
            for (Mahasiswa mahasiswa : hasilUjianMahasiswas) {
                StringBuilder rowMhs = new StringBuilder("<tr><td class='text-center text-muted'>").append(indexMhs).append("</td><td class='fw-bold text-dark'>").append(mahasiswa.getNim()).append("</td><td>").append(mahasiswa.getNama()).append("</td><td class='small'>").append(mahasiswa.getJurusan() != null ? mahasiswa.getJurusan().getNama() : "-").append("</td>");

                Double totalNilaiAkhirMhs = 0.0;

                for (FormatNilai formatNilai : formatNilais) {
                    Double totalNilaiPerformatNilai = 0.0; 
                    Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(formatNilai.getId());
                    Map<Long, Pertemuan> mapdTgs = mapTugas.get(formatNilai.getId());
                    Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(formatNilai.getId());
                    Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(formatNilai.getId());

                    if (mapd != null || mapdTgs != null || mapdTgsLanjut != null || mapdTgsKelompok != null) {
                        Double minimal = formatNilai.ambilMinimal();

                        class CellWriter {
                            void write(Collection<?> items, String prefix) {
                                for (Object item : items) {
                                    try {
                                        Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
                                        String key = prefix + "_" + itemId;
                                        Double nilia = 0.0;
                                        try { nilia = dataNiliasUtama.get(mahasiswa.getId()).get(formatNilai.getId()).get(key); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:504");}
                                        if(nilia == null) nilia = 0.0;
                                        Double persenData = 0.0;
                                        try { persenData = (persensData.get(key + "_" + formatNilai.getId()) * 0.01) * nilia; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:507");}
                                        rowMhs.append("<td class='text-center'>").append(Common.numberFormat.get().format(nilia)).append("<br><small class='text-muted'>(").append(Common.numberFormat.get().format(persenData)).append(")</small></td>");
                                    } catch (Exception e) { rowMhs.append("<td class='text-center'>0</td>"); }
                                }
                            }
                        }
                        CellWriter cw = new CellWriter();
                        if (mapd != null) cw.write(mapd.values(), PertemuanPunyaUjian.class.getName());
                        if (mapdTgs != null) cw.write(mapdTgs.values(), Pertemuan.class.getName());
                        if (mapdTgsLanjut != null) cw.write(mapdTgsLanjut.values(), TugasPertemuan.class.getName());
                        if (mapdTgsKelompok != null) cw.write(mapdTgsKelompok.values(), TugasKelompok.class.getName());

                        totalNilaiPerformatNilai = recalculateTotalPerFormat(dataNiliasUtama, persensData, mahasiswa.getId(), formatNilai.getId(), mapd, mapdTgs, mapdTgsLanjut, mapdTgsKelompok);
                        
                        Map<Long, Double> tnm = totalNilaiMahasiswa.get(mahasiswa.getId());
                        if (tnm == null) { tnm = new HashMap<Long, Double>(); totalNilaiMahasiswa.put(mahasiswa.getId(), tnm); }
                        tnm.put(formatNilai.getId(), totalNilaiPerformatNilai);

                        Double hasilKonversi = hitungKonversi(formatNilai, totalNilaiPerformatNilai);
                        totalNilaiAkhirMhs += totalNilaiPerformatNilai; 
                        
                        rowMhs.append("<td class='text-center bg-light fw-bold text-primary'>").append(Common.numberFormat.get().format(hasilKonversi)).append("</td>");
                        
                        Map<Long, Double> tkm = totalKonversiMahasiswa.get(mahasiswa.getId());
                        if (tkm == null) { tkm = new HashMap<Long, Double>(); totalKonversiMahasiswa.put(mahasiswa.getId(), tkm); }
                        tkm.put(formatNilai.getId(), hasilKonversi);

                        Double cKonv = totalKonversi.get(formatNilai.getId());
                        totalKonversi.put(formatNilai.getId(), (cKonv == null ? 0.0 : cKonv) + hasilKonversi);

                        Double cNil = totalNilai.get(formatNilai.getId());
                        totalNilai.put(formatNilai.getId(), (cNil == null ? 0.0 : cNil) + totalNilaiPerformatNilai);

                        String resultHasil = hitungHurufObe(hasilKonversi, mahasiswa, perkuliahan);
                        rowMhs.append("<td class='text-center bg-light fw-bold'>").append(resultHasil).append("</td>");

                        TreeMap<String, Integer> h = totalNilaiHuruf.get(formatNilai.getId());
                        if (h == null) { h = new TreeMap<String, Integer>(); totalNilaiHuruf.put(formatNilai.getId(), h); }
                        h.put(resultHasil, (h.get(resultHasil) == null ? 0 : h.get(resultHasil)) + 1);

                        boolean isLulus = hasilKonversi >= minimal;
                        String badgeStatus = isLulus ? "<span class='badge bg-success'>L</span>" : "<span class='badge bg-danger'>TL</span>";
                        rowMhs.append("<td class='text-center bg-light'>").append(badgeStatus).append("</td>");

                        if (isLulus) {
                            Integer me = totalMemenuhi.get(formatNilai.getId());
                            totalMemenuhi.put(formatNilai.getId(), me == null ? 1 : me + 1);
                        } else {
                            String mhsStr = mahasiswa.getNim() + " - " + mahasiswa.getNama();
                            blmMemenuhi.put(formatNilai.getId(), blmMemenuhi.get(formatNilai.getId()) == null ? mhsStr : blmMemenuhi.get(formatNilai.getId()) + "<br>" + mhsStr);
                        }
                    }
                }

                sumNilaiAkhirKelas += totalNilaiAkhirMhs;
                rowMhs.append("<td class='text-center bg-secondary text-white fw-bold'>").append(Common.numberFormat.get().format(totalNilaiAkhirMhs)).append("</td>");

                String resultHasilFinal = hitungHurufObe(totalNilaiAkhirMhs, mahasiswa, perkuliahan);
                sebaranHurufAkhirMhs.put(resultHasilFinal, (sebaranHurufAkhirMhs.get(resultHasilFinal) == null ? 0 : sebaranHurufAkhirMhs.get(resultHasilFinal)) + 1);
                
                rowMhs.append("<td class='text-center bg-secondary text-white fw-bold'>").append(resultHasilFinal).append("</td>");

                boolean finalLulus = totalNilaiAkhirMhs >= (perkuliahan.getKurikulumPunyaMatakuliah().getMinimalKetercapaian() != null ? perkuliahan.getKurikulumPunyaMatakuliah().getMinimalKetercapaian() : 0.0);
                if (finalLulus) totalEcampusLulus++; else totalEcampusTidakLulus++;

                String badgeFinal = finalLulus ? "<span class='badge bg-success border border-white'>" + Common.getBahasaConfig("L") + "</span>" : "<span class='badge bg-danger border border-white'>" + Common.getBahasaConfig("TL") + "</span>";
                rowMhs.append("<td class='text-center bg-secondary text-white'>").append(badgeFinal).append("</td></tr>");

                htmlTabel1.append(rowMhs.toString());
                indexMhs++;
            }
            
            StringBuilder rowAvg = new StringBuilder("<tr><td colspan='4' class='text-end fw-bold pe-3'>").append(Common.getBahasaConfig("RATA-RATA KELAS")).append("</td>");
            for (FormatNilai formatNilai : formatNilais) {
                Double konf = totalNilai.get(formatNilai.getId());
                if (konf != null) {
                    Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(formatNilai.getId());
                    Map<Long, Pertemuan> mapdTgs = mapTugas.get(formatNilai.getId());
                    Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(formatNilai.getId());
                    Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(formatNilai.getId());
                    int loopCount = (mapd != null ? mapd.size() : 0) + (mapdTgs != null ? mapdTgs.size() : 0) + (mapdTgsLanjut != null ? mapdTgsLanjut.size() : 0) + (mapdTgsKelompok != null ? mapdTgsKelompok.size() : 0);
                    
                    if(loopCount > 0) rowAvg.append("<td colspan='").append(loopCount).append("' class='bg-light text-center fw-bold text-muted'>Avg ").append(formatNilai.getNama()).append("</td>");
                    
                    Double ratarataKonversi = totalKonversi.get(formatNilai.getId()) / (size == 0 ? 1 : size); 
                    String ratarataHuruf = hitungHurufObe(ratarataKonversi, null, perkuliahan);
                    boolean isAvgLulus = ratarataKonversi >= formatNilai.ambilMinimal();

                    rowAvg.append("<td class='text-center bg-light fw-bold text-primary'>").append(Common.numberFormat.get().format(ratarataKonversi)).append("</td>");
                    rowAvg.append("<td class='text-center bg-light fw-bold'>").append(ratarataHuruf).append("</td>");
                    rowAvg.append("<td class='text-center bg-light fw-bold'>").append(isAvgLulus ? "<span class='text-success'>L</span>" : "<span class='text-danger'>TL</span>").append("</td>");
                }
            }
            
            Double rataRataAkhirKelas = sumNilaiAkhirKelas / (size == 0 ? 1 : size);
            String rataHurufAkhir = hitungHurufObe(rataRataAkhirKelas, null, perkuliahan);
            boolean rataAkhirLulus = rataRataAkhirKelas >= (perkuliahan.getKurikulumPunyaMatakuliah().getMinimalKetercapaian() != null ? perkuliahan.getKurikulumPunyaMatakuliah().getMinimalKetercapaian() : 0.0);
            
            rowAvg.append("<td class='text-center bg-secondary text-white fw-bold'>").append(Common.numberFormat.get().format(rataRataAkhirKelas)).append("</td>");
            rowAvg.append("<td class='text-center bg-secondary text-white fw-bold'>").append(rataHurufAkhir).append("</td>");
            rowAvg.append("<td class='text-center bg-secondary text-white fw-bold'>").append(rataAkhirLulus ? "<span class='text-success'>L</span>" : "<span class='text-danger'>TL</span>").append("</td></tr>");
            
            htmlTabel1.append("</tbody><tfoot>").append(rowAvg.toString()).append("</tfoot>");

            // ========================================================================
            // AGREGASI CPMK (HANYA DIEKSEKUSI JIKA MENGGUNAKAN SUB-CPMK)
            // ========================================================================
            if (!isNilaiCpmk && !listCpmkUnique.isEmpty()) {
                StringBuilder hTopC = new StringBuilder("<tr><th rowspan='2' class='align-middle text-center' style='width: 50px;'>No.</th><th rowspan='2' class='align-middle' style='width: 120px;'>" + Common.getBahasaConfig("NIM") + "</th><th rowspan='2' class='align-middle' style='min-width: 200px;'>" + Common.getBahasaConfig("Nama Mahasiswa") + "</th><th rowspan='2' class='align-middle'>" + Common.getBahasaConfig("Prodi") + "</th>");
                StringBuilder hSub1C = new StringBuilder("<tr>");
                
                for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                    hTopC.append("<th colspan='3' class='text-center align-middle bg-light text-dark border-bottom border-2 border-primary'>").append(cpmk.getKode()).append("<br><small class='fw-normal'>").append(cpmk.getNama()).append("</small></th>");
                    hSub1C.append("<th class='text-center align-middle small text-primary'>").append(Common.getBahasaConfig("Nilai Capaian")).append("</th>");
                    hSub1C.append("<th class='text-center align-middle small'>").append(Common.getBahasaConfig("Skala/Huruf")).append("</th>");
                    hSub1C.append("<th class='text-center align-middle small'>").append(Common.getBahasaConfig("Status Lulus")).append("</th>");
                }
                hTopC.append("</tr>"); hSub1C.append("</tr>");
                htmlTabel1_Cpmk.append("<thead class='table-light'>").append(hTopC.toString()).append(hSub1C.toString()).append("</thead><tbody>");

                Map<Long, Double> totalCpmkScoreAllMhs = new HashMap<Long, Double>();
                Map<Long, Integer> cpmkLulusCount = new HashMap<Long, Integer>();
                Map<Long, String> cpmkBlmMemenuhiStr = new HashMap<Long, String>();
                Map<Long, TreeMap<String, Integer>> cpmkHurufDist = new HashMap<Long, TreeMap<String, Integer>>();

                int idxC = 1;
                for (Mahasiswa mahasiswa : hasilUjianMahasiswas) {
                    Map<Long, Double> konvMhs = totalKonversiMahasiswa.get(mahasiswa.getId());
                    htmlTabel1_Cpmk.append("<tr><td class='text-center text-muted'>").append(idxC++).append("</td><td class='fw-bold text-dark'>").append(mahasiswa.getNim()).append("</td><td>").append(mahasiswa.getNama()).append("</td><td class='small'>").append(mahasiswa.getJurusan() != null ? mahasiswa.getJurusan().getNama() : "-").append("</td>");
                    
                    for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                        List<FormatNilai> fns = mapCpmkIdToFns.get(cpmk.getId());
                        Double sumKonvWeight = 0.0;
                        Double sumWeight = 0.0;
                        for (FormatNilai fn : fns) {
                            Double kVal = (konvMhs != null && konvMhs.get(fn.getId()) != null) ? konvMhs.get(fn.getId()) : 0.0;
                            sumKonvWeight += (kVal * fn.getPersen());
                            sumWeight += fn.getPersen();
                        }
                        Double scoreCpmk = sumWeight > 0 ? (sumKonvWeight / sumWeight) : 0.0;
                        Double targetCpmk = 60.0; 
                        
                        totalCpmkScoreAllMhs.put(cpmk.getId(), (totalCpmkScoreAllMhs.get(cpmk.getId()) == null ? 0.0 : totalCpmkScoreAllMhs.get(cpmk.getId())) + scoreCpmk);
                        
                        String hurufCpmk = hitungHurufObe(scoreCpmk, mahasiswa, perkuliahan);
                        boolean isLulus = scoreCpmk >= targetCpmk;
                        
                        if (isLulus) {
                            cpmkLulusCount.put(cpmk.getId(), (cpmkLulusCount.get(cpmk.getId()) == null ? 0 : cpmkLulusCount.get(cpmk.getId())) + 1);
                        } else {
                            String mhsNameStr = mahasiswa.getNim() + " - " + mahasiswa.getNama();
                            cpmkBlmMemenuhiStr.put(cpmk.getId(), cpmkBlmMemenuhiStr.get(cpmk.getId()) == null ? mhsNameStr : cpmkBlmMemenuhiStr.get(cpmk.getId()) + "<br>" + mhsNameStr);
                        }
                        
                        TreeMap<String, Integer> hDist = cpmkHurufDist.get(cpmk.getId());
                        if (hDist == null) { hDist = new TreeMap<String, Integer>(); cpmkHurufDist.put(cpmk.getId(), hDist); }
                        hDist.put(hurufCpmk, (hDist.get(hurufCpmk) == null ? 0 : hDist.get(hurufCpmk)) + 1);
                        
                        htmlTabel1_Cpmk.append("<td class='text-center fw-bold text-primary bg-light'>").append(Common.numberFormat.get().format(scoreCpmk)).append("</td>");
                        htmlTabel1_Cpmk.append("<td class='text-center fw-bold'>").append(hurufCpmk).append("</td>");
                        htmlTabel1_Cpmk.append("<td class='text-center'>").append(isLulus ? "<span class='text-success'>L</span>" : "<span class='text-danger'>TL</span>").append("</td>");
                    }
                    htmlTabel1_Cpmk.append("</tr>");
                }

                StringBuilder avgCpmk = new StringBuilder("<tr><td colspan='4' class='text-end fw-bold pe-3 bg-light'>").append(Common.getBahasaConfig("Rata-Rata Kelas")).append("</td>");
                for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                    Double totScore = totalCpmkScoreAllMhs.get(cpmk.getId()) == null ? 0.0 : totalCpmkScoreAllMhs.get(cpmk.getId());
                    Double avgScore = totScore / (size == 0 ? 1 : size);
                    avgCpmk.append("<td class='text-center fw-bold text-primary bg-light border-start'>").append(Common.numberFormat.get().format(avgScore)).append("</td>");
                    avgCpmk.append("<td class='text-center fw-bold bg-light'>").append(hitungHurufObe(avgScore, null, perkuliahan)).append("</td>");
                    avgCpmk.append("<td class='text-center fw-bold bg-light'>").append(avgScore >= 60.0 ? "<span class='text-success'>L</span>" : "<span class='text-danger'>TL</span>").append("</td>");
                }
                avgCpmk.append("</tr>");
                htmlTabel1_Cpmk.append("</tbody><tfoot>").append(avgCpmk.toString()).append("</tfoot>");

                // TABEL 2 CPMK (Rangkuman Ketercapaian CPMK)
                htmlTabel2_Cpmk.append("<thead class='table-light'><tr>")
                    .append("<th class='text-center'>No.</th><th>").append(Common.getBahasaConfig("KODE CPMK")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Target CPMK")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Rata - Rata Nilai Capaian CPMK")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Keterangan")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Jumlah mhs. memenuhi target")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Jumlah mhs. belum memenuhi target")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("% Ketercapaian")).append("</th>")
                    .append("<th>").append(Common.getBahasaConfig("Analisis Pelaksanaan Pembelajaran")).append("</th>")
                    .append("<th>").append(Common.getBahasaConfig("Rencana Perbaikan")).append("</th>")
                    .append("<th>").append(Common.getBahasaConfig("Daftar Belum Memenuhi")).append("</th>")
                    .append("</tr></thead><tbody>");

                int idxT2 = 1;
                for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                    Double totScore = totalCpmkScoreAllMhs.get(cpmk.getId()) == null ? 0.0 : totalCpmkScoreAllMhs.get(cpmk.getId());
                    Double avgScore = totScore / (size == 0 ? 1 : size);
                    Integer me = cpmkLulusCount.get(cpmk.getId()) == null ? 0 : cpmkLulusCount.get(cpmk.getId());
                    int blm = size - me;
                    Double min = 60.0;
                    
                    String statusTercapai = avgScore >= min ? "<span class='badge bg-success'>" + Common.getBahasaConfig("Tercapai") + "</span>" : "<span class='badge bg-danger'>" + Common.getBahasaConfig("Tidak Tercapai") + "</span>";
                    String persentase = size == 0 ? "0%" : Common.numberFormat.get().format((me * 100.0) / size) + "%";
                    String daftarMhs = cpmkBlmMemenuhiStr.get(cpmk.getId());
                    
                    String analisis = blm > 0 ? blm + " " + Common.getBahasaConfig("Mahasiswa belum memahami materi pembelajaran untuk") + " " + cpmk.getKode() : "";
                    String perbaikan = blm > 0 ? Common.getBahasaConfig("Dilakukan remedial") : "";

                    htmlTabel2_Cpmk.append("<tr><td class='text-center text-muted'>").append(idxT2++).append("</td>")
                        .append("<td class='fw-bold text-dark'>").append(cpmk.getKode()).append(" - ").append(cpmk.getNama()).append("</td>")
                        .append("<td class='text-center fw-medium text-secondary'>").append(min).append("</td>")
                        .append("<td class='text-center fw-bold text-primary'>").append(Common.numberFormat.get().format(avgScore)).append("</td>")
                        .append("<td class='text-center'>").append(statusTercapai).append("</td>")
                        .append("<td class='text-center text-success fw-bold'>").append(me).append("</td>")
                        .append("<td class='text-center text-danger fw-bold'>").append(blm).append("</td>")
                        .append("<td class='text-center fw-bold'>").append(persentase).append("</td>")
                        .append("<td class='small text-muted'>").append(analisis).append("</td>")
                        .append("<td class='small text-muted'>").append(perbaikan).append("</td>")
                        .append("<td class='small text-danger'>").append(daftarMhs == null ? "-" : daftarMhs).append("</td></tr>");
                }
                htmlTabel2_Cpmk.append("</tbody>");

                // TAB HASIL CPMK (Grafik)
                htmlHasil_CpmkAgg.append("<div class='row g-4'>");
                htmlExportCpmkAgg.append("<table id='exportHasilCpmkAgg").append(rnd).append("' class='d-none'>");

                for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                    TreeMap<String, Integer> hDist = cpmkHurufDist.get(cpmk.getId());
                    if (hDist == null) hDist = new TreeMap<String, Integer>();
                    int cLulus = cpmkLulusCount.get(cpmk.getId()) == null ? 0 : cpmkLulusCount.get(cpmk.getId());
                    int cTidakLulus = size - cLulus;

                    htmlExportCpmkAgg.append("<tr><td colspan='2'>Rekap Penilaian OBE ").append(cpmk.getKode()).append("</td></tr>");
                    htmlExportCpmkAgg.append("<tr><td>Skala Nilai</td><td>Jumlah Mahasiswa</td></tr>");

                    htmlHasil_CpmkAgg.append("<div class='col-md-6 col-lg-4'><div class='card shadow-sm border-0 h-100'><div class='card-header bg-light fw-bold text-secondary text-center'>")
                        .append(Common.getBahasaConfig("Rekap Penilaian OBE")).append(" ").append(cpmk.getKode()).append("</div><div class='card-body p-4'>");

                    htmlHasil_CpmkAgg.append("<table class='table table-sm table-bordered text-center mb-4'><thead><tr class='bg-light text-secondary'><th>").append(Common.getBahasaConfig("Skala Nilai")).append("</th><th>").append(Common.getBahasaConfig("Jumlah Mahasiswa")).append("</th></tr></thead><tbody>");
                    StringBuilder chartCpmk = new StringBuilder("<h6 class='fw-bold text-secondary mb-3 mt-4'><i class='fas fa-chart-bar me-2'></i>").append(Common.getBahasaConfig("Grafik Sebaran Nilai")).append("</h6>");

                    for(Map.Entry<String, Integer> entry : hDist.entrySet()) {
                        htmlExportCpmkAgg.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                        htmlHasil_CpmkAgg.append("<tr><td class='fw-bold'>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                        int pct = size > 0 ? (entry.getValue() * 100 / size) : 0;
                        chartCpmk.append("<div class='mb-2'><div class='d-flex justify-content-between small fw-bold text-dark mb-1'><span>").append(entry.getKey()).append("</span><span>").append(entry.getValue()).append(" ").append(Common.getBahasaConfig("Orang")).append("</span></div>")
                            .append("<div class='progress shadow-sm' style='height: 10px;'><div class='progress-bar bg-info' style='width: ").append(pct).append("%'></div></div></div>");
                    }

                    htmlExportCpmkAgg.append("<tr><td>Lulus</td><td>").append(cLulus).append("</td></tr>");
                    htmlExportCpmkAgg.append("<tr><td>Tidak Lulus</td><td>").append(cTidakLulus).append("</td></tr><tr><td></td><td></td></tr>");

                    htmlHasil_CpmkAgg.append("<tr class='fw-bold bg-light'><td class='text-success'>").append(Common.getBahasaConfig("Lulus")).append("</td><td class='text-success'>").append(cLulus).append("</td></tr>");
                    htmlHasil_CpmkAgg.append("<tr class='fw-bold bg-light'><td class='text-danger'>").append(Common.getBahasaConfig("Tidak Lulus")).append("</td><td class='text-danger'>").append(cTidakLulus).append("</td></tr>");
                    htmlHasil_CpmkAgg.append("</tbody></table>").append(chartCpmk.toString()).append("</div></div></div>");
                }
                htmlHasil_CpmkAgg.append("</div>");
                htmlExportCpmkAgg.append("</table>");
            }

            // ========================================================================
            // TABEL 2 (Asli) RANGKUMAN KETERCAPAIAN FORMAT NILAI (CPMK/SUB-CPMK)
            // ========================================================================
            htmlTabel2.append("<thead class='table-light'><tr>")
                .append("<th class='text-center'>No.</th><th>").append(Common.getBahasaConfig(isNilaiCpmk ? "KODE CPMK" : "KODE SUB-CPMK")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig(isNilaiCpmk ? "Target CPMK" : "Target Sub-CPMK")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig(isNilaiCpmk ? "Rata - Rata Nilai Capaian CPMK" : "Rata - Rata Nilai Capaian Sub-CPMK")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("Keterangan")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("Jumlah mhs. memenuhi target")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("Jumlah mhs. belum memenuhi target")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("% Ketercapaian")).append("</th>")
                .append("<th>").append(Common.getBahasaConfig("Analisis Pelaksanaan Pembelajaran")).append("</th>")
                .append("<th>").append(Common.getBahasaConfig("Rencana Perbaikan")).append("</th>")
                .append("<th>").append(Common.getBahasaConfig("Daftar Belum Memenuhi")).append("</th>")
                .append("</tr></thead><tbody>");

            int idx2 = 1;
            for (FormatNilai formatNilai : formatNilais) {
                Double konf = null;
                try { konf = totalKonversi.get(formatNilai.getId()) / (size == 0 ? 1 : size); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:783");}
                if (konf != null) {
                    Double min = formatNilai.ambilMinimal();
                    Integer me = totalMemenuhi.get(formatNilai.getId());
                    if (me == null) me = 0;
                    int blm = size - me;
                    
                    String statusTercapai = konf >= min ? "<span class='badge bg-success'>" + Common.getBahasaConfig("Tercapai") + "</span>" : "<span class='badge bg-danger'>" + Common.getBahasaConfig("Tidak Tercapai") + "</span>";
                    String persentase = size == 0 ? "0%" : Common.numberFormat.get().format((me * 100.0) / size) + "%";
                    String daftarMhs = blmMemenuhi.get(formatNilai.getId());
                    
                    String analisis = "";
                    String perbaikan = "";
                    if (blm > 0) {
                        analisis = blm + " " + Common.getBahasaConfig("Mahasiswa belum memahami materi pembelajaran untuk") + " " + formatNilai.getNama();
                        perbaikan = Common.getBahasaConfig("Dilakukan remedial");
                    }

                    htmlTabel2.append("<tr><td class='text-center text-muted'>").append(idx2++).append("</td>")
                        .append("<td class='fw-bold text-dark'>").append(formatNilai.getNama()).append("</td>")
                        .append("<td class='text-center fw-medium text-secondary'>").append(min).append("</td>")
                        .append("<td class='text-center fw-bold text-primary'>").append(Common.numberFormat.get().format(konf)).append("</td>")
                        .append("<td class='text-center'>").append(statusTercapai).append("</td>")
                        .append("<td class='text-center text-success fw-bold'>").append(me).append("</td>")
                        .append("<td class='text-center text-danger fw-bold'>").append(blm).append("</td>")
                        .append("<td class='text-center fw-bold'>").append(persentase).append("</td>")
                        .append("<td class='small text-muted'>").append(analisis).append("</td>")
                        .append("<td class='small text-muted'>").append(perbaikan).append("</td>")
                        .append("<td class='small text-danger'>").append(daftarMhs == null ? "-" : daftarMhs).append("</td></tr>");
                }
            }
            htmlTabel2.append("</tbody>");

            // TAB HASIL FORMAT NILAI ASLI (GRAFIK)
            htmlHasilCpmk.append("<div class='row g-4'>");
            htmlExportHasil.append("<table id='exportHasilCpmk").append(rnd).append("' class='d-none'>");
            htmlExportHasil.append("<tr><td colspan='2'>Rekap Penilaian E-CAMPUS</td></tr>");
            htmlExportHasil.append("<tr><td>Nilai</td><td>Jumlah Mahasiswa</td></tr>");

            htmlHasilCpmk.append("<div class='col-md-6 col-lg-4'><div class='card shadow-sm border-0 h-100'><div class='card-header bg-primary text-white fw-bold text-center'>")
                .append(Common.getBahasaConfig("Rekap Penilaian E-CAMPUS")).append("</div><div class='card-body p-4'>");
            htmlHasilCpmk.append("<table class='table table-sm table-bordered text-center mb-4'><thead><tr class='bg-light text-secondary'><th>").append(Common.getBahasaConfig("Nilai Huruf")).append("</th><th>").append(Common.getBahasaConfig("Jumlah Mahasiswa")).append("</th></tr></thead><tbody>");
            
            int maxEcampus = 0;
            for(Integer count : sebaranHurufAkhirMhs.values()) if(count > maxEcampus) maxEcampus = count;

            StringBuilder chartEcampus = new StringBuilder("<h6 class='fw-bold text-secondary mb-3 mt-4'><i class='fas fa-chart-bar me-2'></i>").append(Common.getBahasaConfig("Grafik Sebaran Nilai")).append("</h6>");
            for(Map.Entry<String, Integer> entry : sebaranHurufAkhirMhs.entrySet()) {
                htmlExportHasil.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                htmlHasilCpmk.append("<tr><td class='fw-bold'>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                int pct = maxEcampus > 0 ? (entry.getValue() * 100 / size) : 0;
                chartEcampus.append("<div class='mb-2'><div class='d-flex justify-content-between small fw-bold text-dark mb-1'><span>").append(entry.getKey()).append("</span><span>").append(entry.getValue()).append(" ").append(Common.getBahasaConfig("Orang")).append("</span></div>")
                    .append("<div class='progress shadow-sm' style='height: 10px;'><div class='progress-bar bg-primary' style='width: ").append(pct).append("%'></div></div></div>");
            }

            double pctLulusEcampus = size > 0 ? (totalEcampusLulus * 100.0 / size) : 0;
            htmlExportHasil.append("<tr><td>Lulus</td><td>").append(totalEcampusLulus).append("</td></tr>");
            htmlExportHasil.append("<tr><td>Tidak Lulus</td><td>").append(totalEcampusTidakLulus).append("</td></tr>");
            htmlExportHasil.append("<tr><td>Persentase Lulus</td><td>").append(Common.numberFormat.get().format(pctLulusEcampus)).append("%</td></tr><tr><td></td><td></td></tr>");

            htmlHasilCpmk.append("<tr class='fw-bold bg-light'><td class='text-success'>").append(Common.getBahasaConfig("Lulus")).append("</td><td class='text-success'>").append(totalEcampusLulus).append("</td></tr>");
            htmlHasilCpmk.append("<tr class='fw-bold bg-light'><td class='text-danger'>").append(Common.getBahasaConfig("Tidak Lulus")).append("</td><td class='text-danger'>").append(totalEcampusTidakLulus).append("</td></tr>");
            htmlHasilCpmk.append("<tr class='fw-bold bg-light'><td class='text-primary'>").append(Common.getBahasaConfig("Persentase Lulus")).append("</td><td class='text-primary'>").append(Common.numberFormat.get().format(pctLulusEcampus)).append("%</td></tr>");
            htmlHasilCpmk.append("</tbody></table>").append(chartEcampus.toString()).append("</div></div></div>");

            for (FormatNilai formatNilai : formatNilais) {
                TreeMap<String, Integer> h = totalNilaiHuruf.get(formatNilai.getId());
                if (h == null) h = new TreeMap<String, Integer>();
                int cpmkLulus = totalMemenuhi.get(formatNilai.getId()) == null ? 0 : totalMemenuhi.get(formatNilai.getId());
                int cpmkTidakLulus = size - cpmkLulus;

                htmlExportHasil.append("<tr><td colspan='2'>Rekap Penilaian OBE ").append(formatNilai.getNama()).append("</td></tr>");
                htmlExportHasil.append("<tr><td>Skala Nilai</td><td>Jumlah Mahasiswa</td></tr>");

                htmlHasilCpmk.append("<div class='col-md-6 col-lg-4'><div class='card shadow-sm border-0 h-100'><div class='card-header bg-light fw-bold text-secondary text-center'>")
                    .append(Common.getBahasaConfig("Rekap Penilaian OBE")).append(" ").append(formatNilai.getNama()).append("</div><div class='card-body p-4'>");

                htmlHasilCpmk.append("<table class='table table-sm table-bordered text-center mb-4'><thead><tr class='bg-light text-secondary'><th>").append(Common.getBahasaConfig("Skala Nilai")).append("</th><th>").append(Common.getBahasaConfig("Jumlah Mahasiswa")).append("</th></tr></thead><tbody>");
                StringBuilder chartCpmk = new StringBuilder("<h6 class='fw-bold text-secondary mb-3 mt-4'><i class='fas fa-chart-bar me-2'></i>").append(Common.getBahasaConfig("Grafik Sebaran Nilai")).append("</h6>");

                for(Map.Entry<String, Integer> entry : h.entrySet()) {
                    htmlExportHasil.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                    htmlHasilCpmk.append("<tr><td class='fw-bold'>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                    int pct = size > 0 ? (entry.getValue() * 100 / size) : 0;
                    chartCpmk.append("<div class='mb-2'><div class='d-flex justify-content-between small fw-bold text-dark mb-1'><span>").append(entry.getKey()).append("</span><span>").append(entry.getValue()).append(" ").append(Common.getBahasaConfig("Orang")).append("</span></div>")
                        .append("<div class='progress shadow-sm' style='height: 10px;'><div class='progress-bar bg-info' style='width: ").append(pct).append("%'></div></div></div>");
                }

                htmlExportHasil.append("<tr><td>Lulus</td><td>").append(cpmkLulus).append("</td></tr>");
                htmlExportHasil.append("<tr><td>Tidak Lulus</td><td>").append(cpmkTidakLulus).append("</td></tr><tr><td></td><td></td></tr>");

                htmlHasilCpmk.append("<tr class='fw-bold bg-light'><td class='text-success'>").append(Common.getBahasaConfig("Lulus")).append("</td><td class='text-success'>").append(cpmkLulus).append("</td></tr>");
                htmlHasilCpmk.append("<tr class='fw-bold bg-light'><td class='text-danger'>").append(Common.getBahasaConfig("Tidak Lulus")).append("</td><td class='text-danger'>").append(cpmkTidakLulus).append("</td></tr>");
                htmlHasilCpmk.append("</tbody></table>").append(chartCpmk.toString()).append("</div></div></div>");
            }
            htmlHasilCpmk.append("</div>");
            htmlExportHasil.append("</table>");


            // ========================================================================
            // MEMBANGUN TABEL 4: PENILAIAN CPL
            // ========================================================================
            StringBuilder hCpl1 = new StringBuilder("<tr><th rowspan='2' class='align-middle text-center' style='width: 50px;'>No.</th><th rowspan='2' class='align-middle' style='width: 120px;'>" + Common.getBahasaConfig("NIM") + "</th><th rowspan='2' class='align-middle'>" + Common.getBahasaConfig("Nama Mahasiswa") + "</th>");
            StringBuilder hCpl2 = new StringBuilder("<tr>");

            for (CapaianLulusan capaianLulusan : capaianLulusans) {
                List<FormatNilai> formatNilaisData = listCapaianLulusan.get(capaianLulusan.getId());
                if (formatNilaisData != null && !formatNilaisData.isEmpty()) {
                    hCpl1.append("<th colspan='4' class='text-center bg-light text-dark border-bottom border-2 border-primary'>").append(capaianLulusan.getKode()).append("<br><small class='fw-normal'>").append(capaianLulusan.getNama()).append("</small></th>");
                    hCpl2.append("<th class='text-center align-middle small text-primary'>").append(Common.getBahasaConfig("Nilai CPL")).append("</th>");
                    hCpl2.append("<th class='text-center align-middle small text-primary'>").append(Common.getBahasaConfig("Nilai Capaian")).append("</th>");
                    hCpl2.append("<th class='text-center align-middle small'>").append(Common.getBahasaConfig("Skala Nilai")).append("</th>");
                    hCpl2.append("<th class='text-center align-middle small'>").append(Common.getBahasaConfig("Status Kelulusan (L/TL)")).append("</th>");
                }
            }
            hCpl1.append("</tr>"); hCpl2.append("</tr>");
            htmlTabel4.append("<thead class='table-light'>").append(hCpl1.toString()).append(hCpl2.toString()).append("</thead><tbody>");

            Map<String, Double> totalCplAvgSatuKelas = new HashMap<String, Double>();
            Map<String, Double> totalCplScoreSatuKelas = new HashMap<String, Double>();
            Map<String, Integer> cplMemenuhiKelas = new HashMap<String, Integer>(); 
            Map<String, String> cplBlmMemenuhiKelas = new HashMap<String, String>();
            Map<String, TreeMap<String, Integer>> cplHurufDist = new HashMap<String, TreeMap<String, Integer>>();
            
            // Map CPL score per mahasiswa untuk digunakan di PL
            Map<Long, Map<Long, Double>> mapCplScoreMhs = new HashMap<Long, Map<Long, Double>>();

            int idx4 = 1;
            for (Mahasiswa mahasiswa : hasilUjianMahasiswas) {
                Map<Long, Double> nilaiMahasiswasAsli = totalNilaiMahasiswa.get(mahasiswa.getId()); 
                Map<Long, Double> cplMhs = new HashMap<Long, Double>();
                mapCplScoreMhs.put(mahasiswa.getId(), cplMhs);

                htmlTabel4.append("<tr><td class='text-center text-muted'>").append(idx4++).append("</td><td class='fw-bold text-dark'>").append(mahasiswa.getNim()).append("</td><td>").append(mahasiswa.getNama()).append("</td>");

                for (CapaianLulusan capaianLulusan : capaianLulusans) {
                    List<FormatNilai> formatNilaisData = listCapaianLulusan.get(capaianLulusan.getId());
                    if (formatNilaisData != null && !formatNilaisData.isEmpty()) {
                        Double sumRawScore = 0.0;
                        Double sumBobotFormat = 0.0;
                        
                        for (FormatNilai formatNilai : formatNilaisData) {
                            Double raw = (nilaiMahasiswasAsli == null || nilaiMahasiswasAsli.get(formatNilai.getId()) == null) ? 0.0 : nilaiMahasiswasAsli.get(formatNilai.getId());
                            sumRawScore += raw;
                            sumBobotFormat += formatNilai.getPersen();
                        }

                        Double nilaiCPL = sumRawScore;
                        Double capaianCPL = (sumBobotFormat > 0) ? (sumRawScore / (sumBobotFormat / 100.0)) : 0.0;
                        Double targetMinimal = 60.0; 
                        
                        cplMhs.put(capaianLulusan.getId(), capaianCPL); // Simpan untuk PL

                        String hurufCPL = hitungHurufObe(capaianCPL, mahasiswa, perkuliahan);

                        String key = capaianLulusan.getId() + "";
                        totalCplScoreSatuKelas.put(key, (totalCplScoreSatuKelas.get(key) == null ? 0.0 : totalCplScoreSatuKelas.get(key)) + nilaiCPL);
                        totalCplAvgSatuKelas.put(key, (totalCplAvgSatuKelas.get(key) == null ? 0.0 : totalCplAvgSatuKelas.get(key)) + capaianCPL);
                        
                        TreeMap<String, Integer> hDistCPL = cplHurufDist.get(key);
                        if (hDistCPL == null) { hDistCPL = new TreeMap<String, Integer>(); cplHurufDist.put(key, hDistCPL); }
                        hDistCPL.put(hurufCPL, (hDistCPL.get(hurufCPL) == null ? 0 : hDistCPL.get(hurufCPL)) + 1);

                        if (capaianCPL >= targetMinimal) {
                            cplMemenuhiKelas.put(key, (cplMemenuhiKelas.get(key) == null ? 0 : cplMemenuhiKelas.get(key)) + 1);
                        } else {
                            String mhsNameStr = mahasiswa.getNim() + " - " + mahasiswa.getNama();
                            cplBlmMemenuhiKelas.put(key, cplBlmMemenuhiKelas.get(key) == null ? mhsNameStr : cplBlmMemenuhiKelas.get(key) + "<br>" + mhsNameStr);
                        }

                        htmlTabel4.append("<td class='text-center fw-bold text-primary bg-light'>").append(Common.numberFormat.get().format(nilaiCPL)).append("</td>");
                        htmlTabel4.append("<td class='text-center fw-bold text-primary bg-light'>").append(Common.numberFormat.get().format(capaianCPL)).append("</td>");
                        htmlTabel4.append("<td class='text-center fw-bold'>").append(hurufCPL).append("</td>");
                        htmlTabel4.append("<td class='text-center'>").append(capaianCPL >= targetMinimal ? "<span class='text-success'>" + Common.getBahasaConfig("L") + "</span>" : "<span class='text-danger'>" + Common.getBahasaConfig("TL") + "</span>").append("</td>");
                    }
                }
                htmlTabel4.append("</tr>");
            }
            
            // Footer CPL
            StringBuilder avgCpl = new StringBuilder("<tr><td colspan='3' class='text-end fw-bold pe-3 bg-light'>").append(Common.getBahasaConfig("Rata-Rata Kelas")).append("</td>");
            for (CapaianLulusan capaianLulusan : capaianLulusans) {
                List<FormatNilai> formatNilaisData = listCapaianLulusan.get(capaianLulusan.getId());
                if (formatNilaisData != null && !formatNilaisData.isEmpty()) {
                    String key = capaianLulusan.getId() + "";
                    Double totalAccumScore = totalCplScoreSatuKelas.get(key) == null ? 0.0 : totalCplScoreSatuKelas.get(key);
                    Double totalAccumAvg = totalCplAvgSatuKelas.get(key) == null ? 0.0 : totalCplAvgSatuKelas.get(key);
                    Double ratarataScoreCPL = totalAccumScore / (size == 0 ? 1 : size);
                    Double ratarataCPL = totalAccumAvg / (size == 0 ? 1 : size);
                    
                    avgCpl.append("<td class='text-center fw-bold text-primary bg-light border-start'>").append(Common.numberFormat.get().format(ratarataScoreCPL)).append("</td>");
                    avgCpl.append("<td class='text-center fw-bold text-primary bg-light'>").append(Common.numberFormat.get().format(ratarataCPL)).append("</td>");
                    avgCpl.append("<td class='text-center fw-bold bg-light'>").append(hitungHurufObe(ratarataCPL, null, perkuliahan)).append("</td>");
                    avgCpl.append("<td class='text-center fw-bold bg-light'>").append(ratarataCPL >= 60.0 ? "<span class='text-success'>" + Common.getBahasaConfig("L") + "</span>" : "<span class='text-danger'>" + Common.getBahasaConfig("TL") + "</span>").append("</td>");
                }
            }
            avgCpl.append("</tr>");
            htmlTabel4.append("</tbody><tfoot>").append(avgCpl.toString()).append("</tfoot>");

            // ========================================================================
            // MEMBANGUN TABEL BARU: HASIL CPL (GRAFIK DISTRIBUSI)
            // ========================================================================
            htmlHasilCpl.append("<div class='row g-4'>");
            htmlExportHasilCpl.append("<table id='exportHasilCpl").append(rnd).append("' class='d-none'>");

            for (CapaianLulusan capaianLulusan : capaianLulusans) {
                List<FormatNilai> formatNilaisData = listCapaianLulusan.get(capaianLulusan.getId());
                if (formatNilaisData != null && !formatNilaisData.isEmpty()) {
                    String key = capaianLulusan.getId() + "";
                    TreeMap<String, Integer> hDistCPL = cplHurufDist.get(key);
                    if (hDistCPL == null) hDistCPL = new TreeMap<String, Integer>();
                    
                    int cLulusCPL = cplMemenuhiKelas.get(key) == null ? 0 : cplMemenuhiKelas.get(key);
                    int cTidakLulusCPL = size - cLulusCPL;

                    htmlExportHasilCpl.append("<tr><td colspan='2'>Rekap Outcome OBE ").append(capaianLulusan.getKode()).append("</td></tr>");
                    htmlExportHasilCpl.append("<tr><td>Skala Nilai</td><td>Jumlah Mahasiswa</td></tr>");

                    htmlHasilCpl.append("<div class='col-md-6 col-lg-4'><div class='card shadow-sm border-0 h-100'><div class='card-header bg-light fw-bold text-secondary text-center'>")
                        .append(Common.getBahasaConfig("Rekap Outcome OBE")).append(" ").append(capaianLulusan.getKode()).append("</div><div class='card-body p-4'>");

                    htmlHasilCpl.append("<table class='table table-sm table-bordered text-center mb-4'><thead><tr class='bg-light text-secondary'><th>").append(Common.getBahasaConfig("Skala Nilai")).append("</th><th>").append(Common.getBahasaConfig("Jumlah Mahasiswa")).append("</th></tr></thead><tbody>");
                    StringBuilder chartCpl = new StringBuilder("<h6 class='fw-bold text-secondary mb-3 mt-4'><i class='fas fa-chart-bar me-2'></i>").append(Common.getBahasaConfig("Grafik Sebaran Nilai")).append("</h6>");

                    for(Map.Entry<String, Integer> entry : hDistCPL.entrySet()) {
                        htmlExportHasilCpl.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                        htmlHasilCpl.append("<tr><td class='fw-bold'>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                        int pct = size > 0 ? (entry.getValue() * 100 / size) : 0;
                        chartCpl.append("<div class='mb-2'><div class='d-flex justify-content-between small fw-bold text-dark mb-1'><span>").append(entry.getKey()).append("</span><span>").append(entry.getValue()).append(" ").append(Common.getBahasaConfig("Orang")).append("</span></div>")
                            .append("<div class='progress shadow-sm' style='height: 10px;'><div class='progress-bar bg-success' style='width: ").append(pct).append("%'></div></div></div>");
                    }

                    htmlExportHasilCpl.append("<tr><td>Lulus</td><td>").append(cLulusCPL).append("</td></tr>");
                    htmlExportHasilCpl.append("<tr><td>Tidak Lulus</td><td>").append(cTidakLulusCPL).append("</td></tr><tr><td></td><td></td></tr>");

                    htmlHasilCpl.append("<tr class='fw-bold bg-light'><td class='text-success'>").append(Common.getBahasaConfig("Lulus")).append("</td><td class='text-success'>").append(cLulusCPL).append("</td></tr>");
                    htmlHasilCpl.append("<tr class='fw-bold bg-light'><td class='text-danger'>").append(Common.getBahasaConfig("Tidak Lulus")).append("</td><td class='text-danger'>").append(cTidakLulusCPL).append("</td></tr>");
                    htmlHasilCpl.append("</tbody></table>").append(chartCpl.toString()).append("</div></div></div>");
                }
            }
            htmlHasilCpl.append("</div>");
            htmlExportHasilCpl.append("</table>");


            // ========================================================================
            // MEMBANGUN TABEL 5: RANGKUMAN KETERCAPAIAN CPL
            // ========================================================================
            htmlTabel5.append("<thead class='table-light'><tr>")
                .append("<th class='text-center'>No.</th><th>").append(Common.getBahasaConfig("KODE CPL")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("Target CPL")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("Rata - Rata Nilai Capaian CPL")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("Keterangan")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("Jumlah mhs. memenuhi target")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("Jumlah mhs. belum memenuhi target")).append("</th>")
                .append("<th class='text-center'>").append(Common.getBahasaConfig("% Ketercapaian")).append("</th>")
                .append("<th>").append(Common.getBahasaConfig("Analisis Pelaksanaan Pembelajaran")).append("</th>")
                .append("<th>").append(Common.getBahasaConfig("Rencana Perbaikan")).append("</th>")
                .append("<th>").append(Common.getBahasaConfig("Daftar Belum Memenuhi")).append("</th>")
                .append("</tr></thead><tbody>");

            int idx5 = 1;
            for (CapaianLulusan capaianLulusan : capaianLulusans) {
                List<FormatNilai> formatNilaisData = listCapaianLulusan.get(capaianLulusan.getId());
                if (formatNilaisData != null && !formatNilaisData.isEmpty()) {
                    String key = capaianLulusan.getId() + "";
                    Double totalAccum = totalCplAvgSatuKelas.get(key) == null ? 0.0 : totalCplAvgSatuKelas.get(key);
                    Double ratarataCPL = totalAccum / (size == 0 ? 1 : size);
                    
                    Double min = 60.0; 
                    Integer me = cplMemenuhiKelas.get(key);
                    if (me == null) me = 0;
                    int blm = size - me;
                    
                    String statusTercapai = ratarataCPL >= min ? "<span class='badge bg-success'>" + Common.getBahasaConfig("Tercapai") + "</span>" : "<span class='badge bg-danger'>" + Common.getBahasaConfig("Tidak Tercapai") + "</span>";
                    String persentase = size == 0 ? "0%" : Common.numberFormat.get().format((me * 100.0) / size) + "%";
                    String daftarMhs = cplBlmMemenuhiKelas.get(key);
                    
                    String analisis = "";
                    String perbaikan = "";
                    if (blm > 0) {
                        analisis = blm + " " + Common.getBahasaConfig("Mahasiswa belum mencapai target kompetensi pada") + " " + capaianLulusan.getKode();
                        perbaikan = Common.getBahasaConfig("Diperlukan peninjauan metode atau materi pembelajaran");
                    }

                    htmlTabel5.append("<tr><td class='text-center text-muted'>").append(idx5++).append("</td>")
                        .append("<td class='fw-bold text-dark'>").append(capaianLulusan.getKode()).append("</td>")
                        .append("<td class='text-center fw-medium text-secondary'>").append(min).append("</td>")
                        .append("<td class='text-center fw-bold text-primary'>").append(Common.numberFormat.get().format(ratarataCPL)).append("</td>")
                        .append("<td class='text-center'>").append(statusTercapai).append("</td>")
                        .append("<td class='text-center text-success fw-bold'>").append(me).append("</td>")
                        .append("<td class='text-center text-danger fw-bold'>").append(blm).append("</td>")
                        .append("<td class='text-center fw-bold'>").append(persentase).append("</td>")
                        .append("<td class='small text-muted'>").append(analisis).append("</td>")
                        .append("<td class='small text-muted'>").append(perbaikan).append("</td>")
                        .append("<td class='small text-danger'>").append(daftarMhs == null ? "-" : daftarMhs).append("</td></tr>");
                }
            }
            htmlTabel5.append("</tbody>");

            // ========================================================================
            // MEMBANGUN TABEL 6: PENILAIAN PL (PROFIL LULUSAN)
            // ========================================================================
            if (!profilLulusans.isEmpty()) {
                StringBuilder hPl1 = new StringBuilder("<tr><th rowspan='2' class='align-middle text-center' style='width: 50px;'>No.</th><th rowspan='2' class='align-middle' style='width: 120px;'>" + Common.getBahasaConfig("NIM") + "</th><th rowspan='2' class='align-middle'>" + Common.getBahasaConfig("Nama Mahasiswa") + "</th>");
                StringBuilder hPl2 = new StringBuilder("<tr>");

                for (ProfilLulusan pl : profilLulusans) {
                    hPl1.append("<th colspan='3' class='text-center bg-light text-dark border-bottom border-2 border-primary'>").append(pl.getKode()).append("<br><small class='fw-normal'>").append(pl.getNama()).append("</small></th>");
                    hPl2.append("<th class='text-center align-middle small text-primary'>").append(Common.getBahasaConfig("Nilai Capaian")).append("</th>");
                    hPl2.append("<th class='text-center align-middle small'>").append(Common.getBahasaConfig("Skala Nilai")).append("</th>");
                    hPl2.append("<th class='text-center align-middle small'>").append(Common.getBahasaConfig("Status Kelulusan (L/TL)")).append("</th>");
                }
                hPl1.append("</tr>"); hPl2.append("</tr>");
                htmlTabel6.append("<thead class='table-light'>").append(hPl1.toString()).append(hPl2.toString()).append("</thead><tbody>");

                Map<String, Double> totalPlAvgSatuKelas = new HashMap<String, Double>();
                Map<String, Integer> plMemenuhiKelas = new HashMap<String, Integer>(); 
                Map<String, String> plBlmMemenuhiKelas = new HashMap<String, String>();
                Map<String, TreeMap<String, Integer>> plHurufDist = new HashMap<String, TreeMap<String, Integer>>();

                int idx6 = 1;
                for (Mahasiswa mahasiswa : hasilUjianMahasiswas) {
                    htmlTabel6.append("<tr><td class='text-center text-muted'>").append(idx6++).append("</td><td class='fw-bold text-dark'>").append(mahasiswa.getNim()).append("</td><td>").append(mahasiswa.getNama()).append("</td>");

                    for (ProfilLulusan pl : profilLulusans) {
                        List<CapaianLulusan> mappedCpls = listProfilLulusanToCpl.get(pl.getId());
                        Double sumCpl = 0.0;
                        int countCpl = 0;
                        if(mappedCpls != null) {
                            for(CapaianLulusan cpl : mappedCpls) {
                                Double score = mapCplScoreMhs.get(mahasiswa.getId()) != null ? mapCplScoreMhs.get(mahasiswa.getId()).get(cpl.getId()) : null;
                                if(score != null) {
                                    sumCpl += score;
                                    countCpl++;
                                }
                            }
                        }
                        Double capaianPL = countCpl > 0 ? (sumCpl / countCpl) : 0.0;
                        Double targetMinimal = 60.0; 

                        String hurufPL = hitungHurufObe(capaianPL, mahasiswa, perkuliahan);
                        String key = pl.getId() + "";

                        totalPlAvgSatuKelas.put(key, (totalPlAvgSatuKelas.get(key) == null ? 0.0 : totalPlAvgSatuKelas.get(key)) + capaianPL);
                        
                        TreeMap<String, Integer> hDistPL = plHurufDist.get(key);
                        if (hDistPL == null) { hDistPL = new TreeMap<String, Integer>(); plHurufDist.put(key, hDistPL); }
                        hDistPL.put(hurufPL, (hDistPL.get(hurufPL) == null ? 0 : hDistPL.get(hurufPL)) + 1);

                        if (capaianPL >= targetMinimal) {
                            plMemenuhiKelas.put(key, (plMemenuhiKelas.get(key) == null ? 0 : plMemenuhiKelas.get(key)) + 1);
                        } else {
                            String mhsNameStr = mahasiswa.getNim() + " - " + mahasiswa.getNama();
                            plBlmMemenuhiKelas.put(key, plBlmMemenuhiKelas.get(key) == null ? mhsNameStr : plBlmMemenuhiKelas.get(key) + "<br>" + mhsNameStr);
                        }

                        htmlTabel6.append("<td class='text-center fw-bold text-primary bg-light'>").append(Common.numberFormat.get().format(capaianPL)).append("</td>");
                        htmlTabel6.append("<td class='text-center fw-bold'>").append(hurufPL).append("</td>");
                        htmlTabel6.append("<td class='text-center'>").append(capaianPL >= targetMinimal ? "<span class='text-success'>" + Common.getBahasaConfig("L") + "</span>" : "<span class='text-danger'>" + Common.getBahasaConfig("TL") + "</span>").append("</td>");
                    }
                    htmlTabel6.append("</tr>");
                }
                
                StringBuilder avgPl = new StringBuilder("<tr><td colspan='3' class='text-end fw-bold pe-3 bg-light'>").append(Common.getBahasaConfig("Rata-Rata Kelas")).append("</td>");
                for (ProfilLulusan pl : profilLulusans) {
                    String key = pl.getId() + "";
                    Double totalAccumAvg = totalPlAvgSatuKelas.get(key) == null ? 0.0 : totalPlAvgSatuKelas.get(key);
                    Double ratarataPL = totalAccumAvg / (size == 0 ? 1 : size);
                    
                    avgPl.append("<td class='text-center fw-bold text-primary bg-light border-start'>").append(Common.numberFormat.get().format(ratarataPL)).append("</td>");
                    avgPl.append("<td class='text-center fw-bold bg-light'>").append(hitungHurufObe(ratarataPL, null, perkuliahan)).append("</td>");
                    avgPl.append("<td class='text-center fw-bold bg-light'>").append(ratarataPL >= 60.0 ? "<span class='text-success'>" + Common.getBahasaConfig("L") + "</span>" : "<span class='text-danger'>" + Common.getBahasaConfig("TL") + "</span>").append("</td>");
                }
                avgPl.append("</tr>");
                htmlTabel6.append("</tbody><tfoot>").append(avgPl.toString()).append("</tfoot>");

                // ========================================================================
                // MEMBANGUN TABEL BARU: HASIL PL (GRAFIK DISTRIBUSI)
                // ========================================================================
                htmlHasilPl.append("<div class='row g-4'>");
                htmlExportHasilPl.append("<table id='exportHasilPl").append(rnd).append("' class='d-none'>");

                for (ProfilLulusan pl : profilLulusans) {
                    String key = pl.getId() + "";
                    TreeMap<String, Integer> hDistPL = plHurufDist.get(key);
                    if (hDistPL == null) hDistPL = new TreeMap<String, Integer>();
                    
                    int cLulusPL = plMemenuhiKelas.get(key) == null ? 0 : plMemenuhiKelas.get(key);
                    int cTidakLulusPL = size - cLulusPL;

                    htmlExportHasilPl.append("<tr><td colspan='2'>Rekap Evaluasi PL ").append(pl.getKode()).append("</td></tr>");
                    htmlExportHasilPl.append("<tr><td>Skala Nilai</td><td>Jumlah Mahasiswa</td></tr>");

                    htmlHasilPl.append("<div class='col-md-6 col-lg-4'><div class='card shadow-sm border-0 h-100'><div class='card-header bg-light fw-bold text-secondary text-center'>")
                        .append(Common.getBahasaConfig("Rekap Evaluasi PL")).append(" ").append(pl.getKode()).append("</div><div class='card-body p-4'>");

                    htmlHasilPl.append("<table class='table table-sm table-bordered text-center mb-4'><thead><tr class='bg-light text-secondary'><th>").append(Common.getBahasaConfig("Skala Nilai")).append("</th><th>").append(Common.getBahasaConfig("Jumlah Mahasiswa")).append("</th></tr></thead><tbody>");
                    StringBuilder chartPl = new StringBuilder("<h6 class='fw-bold text-secondary mb-3 mt-4'><i class='fas fa-chart-bar me-2'></i>").append(Common.getBahasaConfig("Grafik Sebaran Nilai")).append("</h6>");

                    for(Map.Entry<String, Integer> entry : hDistPL.entrySet()) {
                        htmlExportHasilPl.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                        htmlHasilPl.append("<tr><td class='fw-bold'>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                        int pct = size > 0 ? (entry.getValue() * 100 / size) : 0;
                        chartPl.append("<div class='mb-2'><div class='d-flex justify-content-between small fw-bold text-dark mb-1'><span>").append(entry.getKey()).append("</span><span>").append(entry.getValue()).append(" ").append(Common.getBahasaConfig("Orang")).append("</span></div>")
                            .append("<div class='progress shadow-sm' style='height: 10px;'><div class='progress-bar bg-warning' style='width: ").append(pct).append("%'></div></div></div>");
                    }

                    htmlExportHasilPl.append("<tr><td>Lulus</td><td>").append(cLulusPL).append("</td></tr>");
                    htmlExportHasilPl.append("<tr><td>Tidak Lulus</td><td>").append(cTidakLulusPL).append("</td></tr><tr><td></td><td></td></tr>");

                    htmlHasilPl.append("<tr class='fw-bold bg-light'><td class='text-success'>").append(Common.getBahasaConfig("Lulus")).append("</td><td class='text-success'>").append(cLulusPL).append("</td></tr>");
                    htmlHasilPl.append("<tr class='fw-bold bg-light'><td class='text-danger'>").append(Common.getBahasaConfig("Tidak Lulus")).append("</td><td class='text-danger'>").append(cTidakLulusPL).append("</td></tr>");
                    htmlHasilPl.append("</tbody></table>").append(chartPl.toString()).append("</div></div></div>");
                }
                htmlHasilPl.append("</div>");
                htmlExportHasilPl.append("</table>");

                // ========================================================================
                // MEMBANGUN TABEL 7: RANGKUMAN KETERCAPAIAN PL
                // ========================================================================
                htmlTabel7.append("<thead class='table-light'><tr>")
                    .append("<th class='text-center'>No.</th><th>").append(Common.getBahasaConfig("KODE PL")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Target PL")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Rata - Rata Nilai Capaian PL")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Keterangan")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Jumlah mhs. memenuhi target")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("Jumlah mhs. belum memenuhi target")).append("</th>")
                    .append("<th class='text-center'>").append(Common.getBahasaConfig("% Ketercapaian")).append("</th>")
                    .append("<th>").append(Common.getBahasaConfig("Analisis Pelaksanaan Pembelajaran")).append("</th>")
                    .append("<th>").append(Common.getBahasaConfig("Rencana Perbaikan")).append("</th>")
                    .append("<th>").append(Common.getBahasaConfig("Daftar Belum Memenuhi")).append("</th>")
                    .append("</tr></thead><tbody>");

                int idx7 = 1;
                for (ProfilLulusan pl : profilLulusans) {
                    String key = pl.getId() + "";
                    Double totalAccum = totalPlAvgSatuKelas.get(key) == null ? 0.0 : totalPlAvgSatuKelas.get(key);
                    Double ratarataPL = totalAccum / (size == 0 ? 1 : size);
                    
                    Double min = 60.0; 
                    Integer me = plMemenuhiKelas.get(key);
                    if (me == null) me = 0;
                    int blm = size - me;
                    
                    String statusTercapai = ratarataPL >= min ? "<span class='badge bg-success'>" + Common.getBahasaConfig("Tercapai") + "</span>" : "<span class='badge bg-danger'>" + Common.getBahasaConfig("Tidak Tercapai") + "</span>";
                    String persentase = size == 0 ? "0%" : Common.numberFormat.get().format((me * 100.0) / size) + "%";
                    String daftarMhs = plBlmMemenuhiKelas.get(key);
                    
                    String analisis = "";
                    String perbaikan = "";
                    if (blm > 0) {
                        analisis = blm + " " + Common.getBahasaConfig("Mahasiswa belum mencapai target Profil Lulusan pada") + " " + pl.getKode();
                        perbaikan = Common.getBahasaConfig("Diperlukan peninjauan menyeluruh terhadap materi CPL terkait");
                    }

                    htmlTabel7.append("<tr><td class='text-center text-muted'>").append(idx7++).append("</td>")
                        .append("<td class='fw-bold text-dark'>").append(pl.getKode()).append("</td>")
                        .append("<td class='text-center fw-medium text-secondary'>").append(min).append("</td>")
                        .append("<td class='text-center fw-bold text-primary'>").append(Common.numberFormat.get().format(ratarataPL)).append("</td>")
                        .append("<td class='text-center'>").append(statusTercapai).append("</td>")
                        .append("<td class='text-center text-success fw-bold'>").append(me).append("</td>")
                        .append("<td class='text-center text-danger fw-bold'>").append(blm).append("</td>")
                        .append("<td class='text-center fw-bold'>").append(persentase).append("</td>")
                        .append("<td class='small text-muted'>").append(analisis).append("</td>")
                        .append("<td class='small text-muted'>").append(perbaikan).append("</td>")
                        .append("<td class='small text-danger'>").append(daftarMhs == null ? "-" : daftarMhs).append("</td></tr>");
                }
                htmlTabel7.append("</tbody>");
            } else {
                htmlTabel6.append("<tr><td colspan='100%' class='text-center py-5 text-muted'><i class='fas fa-info-circle me-2'></i>").append(Common.getBahasaConfig("Data Profil Lulusan belum dipetakan ke CPL.")).append("</td></tr>");
                htmlTabel7.append("<tr><td colspan='100%' class='text-center py-5 text-muted'><i class='fas fa-info-circle me-2'></i>").append(Common.getBahasaConfig("Data Rangkuman Profil Lulusan belum tersedia.")).append("</td></tr>");
                htmlHasilPl.append("<div class='alert alert-secondary text-center py-4'><i class='fas fa-chart-pie me-2'></i>").append(Common.getBahasaConfig("Grafik Hasil Profil Lulusan belum tersedia.")).append("</div>");
            }
        }
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:1259");
    htmlTabel1.append("<tr><td colspan='100%' class='text-center text-danger py-4'><i class='fas fa-exclamation-triangle me-2'></i>").append(Common.getBahasaConfig("Terjadi kesalahan saat memuat data evaluasi.")).append("</td></tr>");
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_laporan.jsp:1262");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
}
%>

<style>
    .nav-tabs.main-tabs .nav-link.active {
        color: #0d6efd;
        border-bottom: 3px solid #0d6efd;
        background-color: transparent;
    }
    .nav-tabs.main-tabs .nav-link {
        color: #495057;
        border: none;
        border-bottom: 3px solid transparent;
    }
    .nav-tabs.main-tabs .nav-link:hover {
        border-bottom: 3px solid #dee2e6;
    }
</style>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4">
        
        <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3 flex-wrap gap-3">
            <div>
                <h6 class="fw-bold text-primary mb-1"><i class="fas fa-chart-line me-2"></i><%=Common.getBahasaConfig("Laporan Rekapitulasi Penilaian OBE")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Pantau ketercapaian nilai berdasarkan komponen pembelajaran dan CPL mahasiswa secara komprehensif.")%></small>
            </div>
            <% if (perkuliahan != null && htmlTabel1.length() > 0) { %>
            <div class="d-flex gap-2">
                <button type="button" class="btn btn-sm btn-outline-success fw-bold px-3 shadow-sm rounded-pill" onclick="downloadExcelLaporan<%=rnd%>()">
                    <i class="fas fa-file-excel me-1"></i><%=Common.getBahasaConfig("Unduh Excel")%>
                </button>
            </div>
            <% } %>
        </div>

        <% if (!listPerkuliahanTersedia.isEmpty() && (!isMahasiswa || (isMahasiswa && listPerkuliahanTersedia.size() > 1))) { %>
            <div class="card bg-light border-0 shadow-none rounded-3 p-3 mb-4 d-flex flex-row align-items-center justify-content-between flex-wrap gap-3">
                <div>
                    <label class="form-label fw-bold text-secondary mb-0"><i class="fas fa-chalkboard text-info me-2"></i><%=Common.getBahasaConfig("Pilih Jadwal Perkuliahan")%></label>
                    <small class="text-muted d-block"><%=Common.getBahasaConfig("Tentukan jadwal kelas untuk menampilkan laporan evaluasinya.")%></small>
                </div>
                <div style="min-width: 350px; flex-grow: 1; max-width: 500px;">
                    <select class="form-select shadow-sm border-info fw-bold text-dark" onchange="if(this.value) { window.pilihPerkuliahanLaporan<%=rnd%>(this.value); }" <%= listPerkuliahanTersedia.size() == 1 ? "disabled" : "" %>>
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
                window.pilihPerkuliahanLaporan<%=rnd%> = function(idPerkuliahanPilih) {
                    var urlLoad = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_laporan&kurikulumPunyaMatakuliah=<%=kpm.getId()%>&var=<%=variable%>&perkuliahan=" + idPerkuliahanPilih;
                    if (typeof loadContentIntoContainer === 'function') {
                        loadContentIntoContainer(urlLoad, 'containerLaporan<%=variable%>');
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
                <i class="fas fa-chart-pie text-muted fs-1 mb-3 opacity-50"></i>
                <p class="text-muted mb-0"><%=Common.getBahasaConfig("Silakan pilih Jadwal Perkuliahan di atas terlebih dahulu untuk menampilkan laporan OBE.")%></p>
            </div>
        <% } else if (htmlTabel1.length() > 0) { %>

            <ul class="nav nav-tabs main-tabs mb-4" id="mainLaporanTabs<%=rnd%>" role="tablist">
                <% if (!isNilaiCpmk) { %>
                <li class="nav-item" role="presentation">
                    <button class="nav-link active fw-bold px-4 py-2" data-bs-toggle="tab" data-bs-target="#main-subcpmk-<%=rnd%>" type="button" role="tab"><i class="fas fa-layer-group me-2"></i><%=Common.getBahasaConfig("Evaluasi Sub-CPMK")%></button>
                </li>
                <% } %>
                
                <li class="nav-item" role="presentation">
                    <button class="nav-link <%= isNilaiCpmk ? "active" : "" %> fw-bold px-4 py-2" data-bs-toggle="tab" data-bs-target="#main-cpmk-<%=rnd%>" type="button" role="tab"><i class="fas fa-bullseye me-2"></i><%=Common.getBahasaConfig("Evaluasi CPMK")%></button>
                </li>
                
                <li class="nav-item" role="presentation">
                    <button class="nav-link fw-bold px-4 py-2" data-bs-toggle="tab" data-bs-target="#main-cpl-<%=rnd%>" type="button" role="tab"><i class="fas fa-graduation-cap me-2"></i><%=Common.getBahasaConfig("Evaluasi CPL")%></button>
                </li>

                <li class="nav-item" role="presentation">
                    <button class="nav-link fw-bold px-4 py-2" data-bs-toggle="tab" data-bs-target="#main-pl-<%=rnd%>" type="button" role="tab"><i class="fas fa-user-graduate me-2"></i><%=Common.getBahasaConfig("Evaluasi PL / Profil Lulusan")%></button>
                </li>
            </ul>

            <div class="tab-content" id="mainLaporanContent<%=rnd%>">
                
                <% if (!isNilaiCpmk) { %>
                <div class="tab-pane fade show active" id="main-subcpmk-<%=rnd%>" role="tabpanel">
                    <ul class="nav nav-pills d-flex flex-wrap gap-2 mb-4" role="tablist">
                        <li class="nav-item"><button class="nav-link active rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#sub-rekap-<%=rnd%>" type="button"><i class="fas fa-table me-2"></i><%=Common.getBahasaConfig("Rekapitulasi Penilaian")%></button></li>
                        <li class="nav-item"><button class="nav-link rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#sub-hasil-<%=rnd%>" type="button"><i class="fas fa-chart-pie me-2"></i><%=Common.getBahasaConfig("Hasil Sub-CPMK")%></button></li>
                        <li class="nav-item"><button class="nav-link rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#sub-rangkuman-<%=rnd%>" type="button"><i class="fas fa-list-alt me-2"></i><%=Common.getBahasaConfig("Rangkuman Ketercapaian")%></button></li>
                    </ul>
                    
                    <div class="tab-content">
                        <div class="tab-pane fade show active" id="sub-rekap-<%=rnd%>">
                            <div class="table-responsive border rounded-4 shadow-sm pb-2 mb-4" style="max-height: 600px; overflow-y: auto;">
                                <table class="table table-bordered table-hover align-middle mb-0 small text-nowrap" id="tableLaporanUtama<%=rnd%>" style="min-width: 1200px;">
                                    <%= htmlTabel1.toString() %>
                                </table>
                            </div>
                        </div>
                        <div class="tab-pane fade" id="sub-hasil-<%=rnd%>">
                            <div class="alert alert-info border-0 shadow-sm rounded-3 py-3 mb-4 d-flex align-items-center">
                                <i class="fas fa-info-circle fs-4 me-3 text-info"></i><div><small class="text-dark"><%=Common.getBahasaConfig("Laporan distribusi nilai akhir kelas dan nilai per Sub-CPMK secara visual.")%></small></div>
                            </div>
                            <%= htmlHasilCpmk.toString() %>
                            <%= htmlExportHasil.toString() %>
                        </div>
                        <div class="tab-pane fade" id="sub-rangkuman-<%=rnd%>">
                            <div class="table-responsive border rounded-4 shadow-sm mb-4">
                                <table class="table table-bordered table-hover align-middle mb-0 small text-nowrap" id="tableLaporanKetercapaian<%=rnd%>">
                                    <%= htmlTabel2.toString() %>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
                <% } %>

                <div class="tab-pane fade <%= isNilaiCpmk ? "show active" : "" %>" id="main-cpmk-<%=rnd%>" role="tabpanel">
                    <ul class="nav nav-pills d-flex flex-wrap gap-2 mb-4" role="tablist">
                        <li class="nav-item"><button class="nav-link active rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#cpmk-rekap-<%=rnd%>" type="button"><i class="fas fa-table me-2"></i><%=Common.getBahasaConfig("Rekapitulasi Penilaian")%></button></li>
                        <li class="nav-item"><button class="nav-link rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#cpmk-hasil-<%=rnd%>" type="button"><i class="fas fa-chart-pie me-2"></i><%=Common.getBahasaConfig("Hasil CPMK")%></button></li>
                        <li class="nav-item"><button class="nav-link rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#cpmk-rangkuman-<%=rnd%>" type="button"><i class="fas fa-list-alt me-2"></i><%=Common.getBahasaConfig("Rangkuman Ketercapaian")%></button></li>
                    </ul>
                    
                    <div class="tab-content">
                        <div class="tab-pane fade show active" id="cpmk-rekap-<%=rnd%>">
                            <div class="table-responsive border rounded-4 shadow-sm pb-2 mb-4" style="max-height: 600px; overflow-y: auto;">
                                <table class="table table-bordered table-hover align-middle mb-0 small text-nowrap" id="<%= isNilaiCpmk ? "tableLaporanUtama" + rnd : "tableLaporanUtamaCpmk" + rnd %>" style="min-width: 1200px;">
                                    <%= isNilaiCpmk ? htmlTabel1.toString() : htmlTabel1_Cpmk.toString() %>
                                </table>
                            </div>
                        </div>
                        <div class="tab-pane fade" id="cpmk-hasil-<%=rnd%>">
                            <div class="alert alert-info border-0 shadow-sm rounded-3 py-3 mb-4 d-flex align-items-center">
                                <i class="fas fa-info-circle fs-4 me-3 text-info"></i><div><small class="text-dark"><%=Common.getBahasaConfig(isNilaiCpmk ? "Laporan distribusi nilai akhir kelas dan nilai per CPMK secara visual." : "Laporan distribusi rata-rata per CPMK dari hasil penggabungan Sub-CPMK.")%></small></div>
                            </div>
                            <%= isNilaiCpmk ? htmlHasilCpmk.toString() : htmlHasil_CpmkAgg.toString() %>
                            <%= isNilaiCpmk ? htmlExportHasil.toString() : htmlExportCpmkAgg.toString() %>
                        </div>
                        <div class="tab-pane fade" id="cpmk-rangkuman-<%=rnd%>">
                            <div class="table-responsive border rounded-4 shadow-sm mb-4">
                                <table class="table table-bordered table-hover align-middle mb-0 small text-nowrap" id="<%= isNilaiCpmk ? "tableLaporanKetercapaian" + rnd : "tableLaporanKetercapaianCpmk" + rnd %>">
                                    <%= isNilaiCpmk ? htmlTabel2.toString() : htmlTabel2_Cpmk.toString() %>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="tab-pane fade" id="main-cpl-<%=rnd%>" role="tabpanel">
                    <ul class="nav nav-pills d-flex flex-wrap gap-2 mb-4" role="tablist">
                        <li class="nav-item"><button class="nav-link active rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#cpl-rekap-<%=rnd%>" type="button"><i class="fas fa-table me-2"></i><%=Common.getBahasaConfig("Penilaian CPL")%></button></li>
                        <li class="nav-item"><button class="nav-link rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#cpl-hasil-<%=rnd%>" type="button"><i class="fas fa-chart-pie me-2"></i><%=Common.getBahasaConfig("Hasil CPL")%></button></li>
                        <li class="nav-item"><button class="nav-link rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#cpl-rangkuman-<%=rnd%>" type="button"><i class="fas fa-list-alt me-2"></i><%=Common.getBahasaConfig("Rangkuman CPL")%></button></li>
                    </ul>
                    
                    <div class="tab-content">
                        <div class="tab-pane fade show active" id="cpl-rekap-<%=rnd%>">
                            <div class="alert alert-info border-0 shadow-sm rounded-3 py-3 mb-4 d-flex align-items-center">
                                <i class="fas fa-info-circle fs-4 me-3 text-info"></i><div><small class="text-dark"><%=Common.getBahasaConfig("Laporan ini menunjukkan hasil kalkulasi ketercapaian setiap Capaian Pembelajaran Lulusan (CPL) yang dibebankan pada matakuliah ini.")%></small></div>
                            </div>
                            <div class="table-responsive border rounded-4 shadow-sm pb-2 mb-4" style="max-height: 600px; overflow-y: auto;">
                                <table class="table table-bordered table-hover align-middle mb-0 small text-nowrap" id="tableLaporanCPL<%=rnd%>" style="min-width: 1000px;">
                                    <%= htmlTabel4.toString() %>
                                </table>
                            </div>
                        </div>
                        <div class="tab-pane fade" id="cpl-hasil-<%=rnd%>">
                            <div class="alert alert-info border-0 shadow-sm rounded-3 py-3 mb-4 d-flex align-items-center">
                                <i class="fas fa-info-circle fs-4 me-3 text-info"></i><div><small class="text-dark"><%=Common.getBahasaConfig("Laporan sebaran dan persentase kelulusan mahasiswa pada tiap CPL secara visual.")%></small></div>
                            </div>
                            <%= htmlHasilCpl.toString() %>
                            <%= htmlExportHasilCpl.toString() %>
                        </div>
                        <div class="tab-pane fade" id="cpl-rangkuman-<%=rnd%>">
                            <div class="table-responsive border rounded-4 shadow-sm mb-4">
                                <table class="table table-bordered table-hover align-middle mb-0 small text-nowrap" id="tableLaporanRangkumanCPL<%=rnd%>">
                                    <%= htmlTabel5.toString() %>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="tab-pane fade" id="main-pl-<%=rnd%>" role="tabpanel">
                    <ul class="nav nav-pills d-flex flex-wrap gap-2 mb-4" role="tablist">
                        <li class="nav-item"><button class="nav-link active rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#pl-rekap-<%=rnd%>" type="button"><i class="fas fa-table me-2"></i><%=Common.getBahasaConfig("Penilaian PL")%></button></li>
                        <li class="nav-item"><button class="nav-link rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#pl-hasil-<%=rnd%>" type="button"><i class="fas fa-chart-pie me-2"></i><%=Common.getBahasaConfig("Hasil PL")%></button></li>
                        <li class="nav-item"><button class="nav-link rounded-pill fw-bold border px-3 shadow-sm" data-bs-toggle="pill" data-bs-target="#pl-rangkuman-<%=rnd%>" type="button"><i class="fas fa-list-alt me-2"></i><%=Common.getBahasaConfig("Rangkuman PL")%></button></li>
                    </ul>
                    
                    <div class="tab-content">
                        <div class="tab-pane fade show active" id="pl-rekap-<%=rnd%>">
                            <div class="alert alert-info border-0 shadow-sm rounded-3 py-3 mb-4 d-flex align-items-center">
                                <i class="fas fa-info-circle fs-4 me-3 text-info"></i><div><small class="text-dark"><%=Common.getBahasaConfig("Laporan ini menunjukkan hasil kalkulasi ketercapaian setiap Profil Lulusan (PL) yang diagregasi dari nilai CPL pembentuknya.")%></small></div>
                            </div>
                            <div class="table-responsive border rounded-4 shadow-sm pb-2 mb-4" style="max-height: 600px; overflow-y: auto;">
                                <table class="table table-bordered table-hover align-middle mb-0 small text-nowrap" id="tableLaporanPL<%=rnd%>" style="min-width: 1000px;">
                                    <%= htmlTabel6.toString() %>
                                </table>
                            </div>
                        </div>
                        <div class="tab-pane fade" id="pl-hasil-<%=rnd%>">
                            <div class="alert alert-info border-0 shadow-sm rounded-3 py-3 mb-4 d-flex align-items-center">
                                <i class="fas fa-info-circle fs-4 me-3 text-info"></i><div><small class="text-dark"><%=Common.getBahasaConfig("Laporan sebaran dan persentase kelulusan mahasiswa pada tiap Profil Lulusan secara visual.")%></small></div>
                            </div>
                            <%= htmlHasilPl.toString() %>
                            <%= htmlExportHasilPl.toString() %>
                        </div>
                        <div class="tab-pane fade" id="pl-rangkuman-<%=rnd%>">
                            <div class="table-responsive border rounded-4 shadow-sm mb-4">
                                <table class="table table-bordered table-hover align-middle mb-0 small text-nowrap" id="tableLaporanRangkumanPL<%=rnd%>">
                                    <%= htmlTabel7.toString() %>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>

            </div>

        <% } %>
    </div>
</div>

<script>
    const downloadExcelLaporan<%=rnd%> = () => {
        try {
            if(typeof XLSX !== 'undefined') {
                const wb = XLSX.utils.book_new();
                
                if (document.getElementById('tableLaporanUtama<%=rnd%>')) {
                    const ws1 = XLSX.utils.table_to_sheet(document.getElementById('tableLaporanUtama<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws1, "<%= isNilaiCpmk ? "Penilaian_CPMK" : "Penilaian_Sub_CPMK" %>");
                }
                <% if (!isNilaiCpmk) { %>
                if (document.getElementById('tableLaporanUtamaCpmk<%=rnd%>')) {
                    const ws1c = XLSX.utils.table_to_sheet(document.getElementById('tableLaporanUtamaCpmk<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws1c, "Penilaian_CPMK");
                }
                <% } %>

                if (document.getElementById('exportHasilCpmk<%=rnd%>')) {
                    const ws3 = XLSX.utils.table_to_sheet(document.getElementById('exportHasilCpmk<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws3, "<%= isNilaiCpmk ? "Hasil_CPMK" : "Hasil_Sub_CPMK" %>");
                }
                <% if (!isNilaiCpmk) { %>
                if (document.getElementById('exportHasilCpmkAgg<%=rnd%>')) {
                    const ws3c = XLSX.utils.table_to_sheet(document.getElementById('exportHasilCpmkAgg<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws3c, "Hasil_CPMK");
                }
                <% } %>

                if (document.getElementById('tableLaporanKetercapaian<%=rnd%>')) {
                    const ws2 = XLSX.utils.table_to_sheet(document.getElementById('tableLaporanKetercapaian<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws2, "<%= isNilaiCpmk ? "Rangkuman_CPMK" : "Rangkuman_Sub_CPMK" %>");
                }
                <% if (!isNilaiCpmk) { %>
                if (document.getElementById('tableLaporanKetercapaianCpmk<%=rnd%>')) {
                    const ws2c = XLSX.utils.table_to_sheet(document.getElementById('tableLaporanKetercapaianCpmk<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws2c, "Rangkuman_CPMK");
                }
                <% } %>

                if (document.getElementById('tableLaporanCPL<%=rnd%>')) {
                    const ws4 = XLSX.utils.table_to_sheet(document.getElementById('tableLaporanCPL<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws4, "Penilaian_CPL");
                }
                if (document.getElementById('exportHasilCpl<%=rnd%>')) {
                    const wshc = XLSX.utils.table_to_sheet(document.getElementById('exportHasilCpl<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, wshc, "Hasil_CPL");
                }
                if (document.getElementById('tableLaporanRangkumanCPL<%=rnd%>')) {
                    const ws5 = XLSX.utils.table_to_sheet(document.getElementById('tableLaporanRangkumanCPL<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws5, "Rangkuman_CPL");
                }

                if (document.getElementById('tableLaporanPL<%=rnd%>')) {
                    const ws6 = XLSX.utils.table_to_sheet(document.getElementById('tableLaporanPL<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws6, "Penilaian_PL");
                }
                if (document.getElementById('exportHasilPl<%=rnd%>')) {
                    const wshp = XLSX.utils.table_to_sheet(document.getElementById('exportHasilPl<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, wshp, "Hasil_PL");
                }
                if (document.getElementById('tableLaporanRangkumanPL<%=rnd%>')) {
                    const ws7 = XLSX.utils.table_to_sheet(document.getElementById('tableLaporanRangkumanPL<%=rnd%>'));
                    XLSX.utils.book_append_sheet(wb, ws7, "Rangkuman_PL");
                }
                
                XLSX.writeFile(wb, "<%=namaFileLaporan%>.xlsx");
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Library Excel tidak dimuat. Gagal mengunduh.")%>', 'bg-warning text-dark');
            }
        } catch(e) { console.error(e); }
    };
</script>