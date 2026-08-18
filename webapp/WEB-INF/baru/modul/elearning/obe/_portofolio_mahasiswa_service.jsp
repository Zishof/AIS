<%@page import="java.util.*"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.math.RoundingMode"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
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
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_service.jsp:42");}
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
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_service.jsp:57");}
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
                    try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_service.jsp:84");}
                }
            }
        }
        return longs;
    }
%>
<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String type = request.getParameter("type");
String idMahasiswaStr = request.getParameter("mahasiswa");
String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
String idPerkuliahanStr = request.getParameter("perkuliahan");
String keywordRaw = request.getParameter("keyword");

if (type == null) {
    out.print("<div class='alert alert-warning m-3'>" + Common.getBahasaConfig("Permintaan data tidak sah.") + "</div>");
    return;
}

Session sess = HibernateUtil.openSession();
try {

    // =========================================================================================
    // KONDISI 0: PENCARIAN MAHASISWA AJAX
    // =========================================================================================
    if (type.equals("search_mhs")) {
        if (keywordRaw != null && keywordRaw.length() >= 3) {
            String keyword = keywordRaw.trim();
            List<Mahasiswa> listMhs = ConstantValues.simpleList(
                sess.createCriteria(Mahasiswa.class)
                    .add(Restrictions.or(Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE), Restrictions.ilike("nim", keyword, MatchMode.ANYWHERE)))
                    .addOrder(Order.asc("nim")).setMaxResults(15), 
                Mahasiswa.class
            );

            if (listMhs.isEmpty()) {
                out.print("<div class='text-center text-danger py-4'><i class='fas fa-search-minus fs-4 mb-2'></i><br>" + Common.getBahasaConfig("Data mahasiswa tidak ditemukan.") + "</div>");
            } else {
                StringBuilder sb = new StringBuilder();
                for (Mahasiswa m : listMhs) {
                    String cleanName = m.getNama().replace("'", "\\'").replace("\"", "&quot;");
                    String namaJurusan = m.getJurusan() != null ? m.getJurusan().getNama() : "-";
                    sb.append("<a href='javascript:void(0)' class='list-group-item list-group-item-action border-start-0 border-end-0 py-3' ")
                      .append("onclick='window.pilihMahasiswaDariPencarian").append(rnd).append("(\"").append(m.getId()).append("\", \"").append(cleanName).append("\", \"").append(m.getNim()).append("\")'>")
                      .append("<div class='d-flex w-100 justify-content-between align-items-center'>")
                      .append("<div><h6 class='mb-1 fw-bold text-primary'><i class='fas fa-user me-2'></i>").append(m.getNama()).append("</h6>")
                      .append("<small class='text-muted d-block'><i class='fas fa-id-card me-1'></i>").append(m.getNim()).append(" | <i class='fas fa-graduation-cap me-1'></i>").append(namaJurusan).append("</small></div>")
                      .append("<div><i class='fas fa-chevron-right text-muted'></i></div>")
                      .append("</div></a>");
                }
                out.print(sb.toString());
            }
        }
        return;
    }

    if (idMahasiswaStr == null || idMahasiswaStr.trim().isEmpty()) return;
    Mahasiswa mahasiswa = (Mahasiswa) sess.get(Mahasiswa.class, Long.parseLong(idMahasiswaStr));
    if (mahasiswa == null) return;

    // =========================================================================================
    // KONDISI 1: TAMPILKAN DAFTAR MATAKULIAH (KPM) YANG DIAMBIL MAHASISWA
    // =========================================================================================
    if (type.equals("list")) {
        List<Long> kpmIdsRaw = sess.createCriteria(Detailperkuliahan.class)
            .createAlias("perkuliahan", "p")
            .add(Restrictions.eq("mahasiswa", mahasiswa))
            .setProjection(Projections.groupProperty("p.kurikulumPunyaMatakuliah.id"))
            .list();

        List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = new ArrayList<KurikulumPunyaMatakuliah>();
        if (kpmIdsRaw != null && !kpmIdsRaw.isEmpty()) {
            kurikulumPunyaMatakuliahs = ConstantValues.simpleList(
                sess.createCriteria(KurikulumPunyaMatakuliah.class).add(Restrictions.in("id", kpmIdsRaw)), 
                KurikulumPunyaMatakuliah.class
            );
        }

        Map<Long, Long> mapKpmToPerkuliahan = new HashMap<Long, Long>();
        List<Detailperkuliahan> dps = ConstantValues.simpleList(
            sess.createCriteria(Detailperkuliahan.class).createAlias("perkuliahan", "p").add(Restrictions.eq("mahasiswa", mahasiswa)), 
            Detailperkuliahan.class
        );
        for (Detailperkuliahan dp : dps) {
            if (dp.getPerkuliahan() != null && dp.getPerkuliahan().getKurikulumPunyaMatakuliah() != null) {
                mapKpmToPerkuliahan.put(dp.getPerkuliahan().getKurikulumPunyaMatakuliah().getId(), dp.getPerkuliahan().getId());
            }
        }
        %>
        <div class="card border-0 shadow-sm rounded-4 mb-4">
            <div class="card-header bg-white border-bottom-0 pt-4 pb-0 px-4 d-flex justify-content-between align-items-center flex-wrap gap-2">
                <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-book-open me-2"></i><%=Common.getBahasaConfig("Daftar Mata Kuliah Diambil")%> - <span class="text-primary"><%=mahasiswa.getNama()%></span></h6>
                <button class="btn btn-sm btn-info text-white rounded-pill fw-bold shadow-sm px-3" onclick="window.lihatPortofolioGabungan<%=rnd%>('<%=mahasiswa.getId()%>')">
                    <i class="fas fa-chart-line me-2"></i><%=Common.getBahasaConfig("Lihat Gabungan Portofolio")%>
                </button>
            </div>
            <div class="card-body p-4">
                <div class="table-responsive border rounded-3">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th class="text-center" width="50"><%=Common.getBahasaConfig("No.")%></th>
                                <th><%=Common.getBahasaConfig("Kode Mata Kuliah")%></th>
                                <th><%=Common.getBahasaConfig("Nama Mata Kuliah")%></th>
                                <th class="text-center"><%=Common.getBahasaConfig("SKS")%></th>
                                <th class="text-center"><%=Common.getBahasaConfig("Semester")%></th>
                                <th class="text-center"><%=Common.getBahasaConfig("Aksi")%></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (kurikulumPunyaMatakuliahs.isEmpty()) { %>
                                <tr><td colspan="6" class="text-center py-4 text-muted fst-italic"><i class="fas fa-folder-open me-2"></i><%=Common.getBahasaConfig("Mahasiswa belum mengambil mata kuliah yang memiliki evaluasi OBE.")%></td></tr>
                            <% } else { 
                                int noKpm = 1;
                                for (KurikulumPunyaMatakuliah currKpm : kurikulumPunyaMatakuliahs) {
                                    Matakuliah currMk = currKpm.getMatakuliah();
                                    Long perkuliahanId = mapKpmToPerkuliahan.get(currKpm.getId());
                                    if (perkuliahanId != null && currMk != null) {
                            %>
                                <tr>
                                    <td class="text-center text-muted"><%=noKpm++%></td>
                                    <td class="fw-bold text-dark"><%=currMk.getKode()%></td>
                                    <td><%=currMk.getNama()%></td>
                                    <td class="text-center"><span class="badge bg-secondary"><%=Common.numberFormat.get().format(currMk.getSks())%></span></td>
                                    <td class="text-center"><%=currKpm.getSemester() != null ? currKpm.getSemester() : "-"%></td>
                                    <td class="text-center">
                                        <button class="btn btn-sm btn-outline-primary rounded-pill fw-bold" onclick="window.lihatPortofolioKpm<%=rnd%>('<%=mahasiswa.getId()%>', '<%=currKpm.getId()%>', '<%=perkuliahanId%>')">
                                            <i class="fas fa-chart-pie me-1"></i><%=Common.getBahasaConfig("Lihat Portofolio")%>
                                        </button>
                                    </td>
                                </tr>
                            <%      }
                                }
                            } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <%
    }

    // =========================================================================================
    // KONDISI 2: TAMPILKAN RINCIAN PORTOFOLIO (PER MK) DENGAN GRAFIK LENGKAP
    // =========================================================================================
    else if (type.equals("detail")) {
        if (idKpmStr == null || idPerkuliahanStr == null) return;
        
        KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
        Perkuliahan perkuliahan = (Perkuliahan) sess.get(Perkuliahan.class, Long.parseLong(idPerkuliahanStr));
        
        if (kpm == null || perkuliahan == null) return;

        Matakuliah mk = kpm.getMatakuliah();
        boolean isNilaiCpmk = kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk();
        String kodeMk = mk.getKode();
        String namaMk = mk.getNama();

        List<CapaianLulusan> capaianLulusans = new ArrayList<CapaianLulusan>();
        List<ProfilLulusan> profilLulusans = new ArrayList<ProfilLulusan>();
        if(mk != null) {
            Set<Long> longs = parseIdsToSet(mk.getCapaianLulusan());
            capaianLulusans = ConstantValues.simpleList(sess.createCriteria(CapaianLulusan.class).add(longs.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longs)).addOrder(Order.asc("kode")), CapaianLulusan.class);

            Set<Long> longsProfile = parseIdsToSet(mk.getProfilLulusan());
            profilLulusans = ConstantValues.simpleList(sess.createCriteria(ProfilLulusan.class).add(longsProfile.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longsProfile)).addOrder(Order.asc("kode")), ProfilLulusan.class);
        }

        Map<Long, List<CapaianLulusan>> plToCplMap = new HashMap<Long, List<CapaianLulusan>>();
        for (ProfilLulusan pl : profilLulusans) {
            final String idBaru = pl.getId() + "_" + kpm.getId();
            for (CapaianLulusan cpl : capaianLulusans) {
                String p = cpl.getProfil() != null ? cpl.getProfil() : "";
                if (p.contains("," + pl.getId() + ",") || p.contains("," + idBaru + ",")) {
                    List<CapaianLulusan> cpls = plToCplMap.get(pl.getId());
                    if (cpls == null) { cpls = new ArrayList<CapaianLulusan>(); plToCplMap.put(pl.getId(), cpls); }
                    cpls.add(cpl);
                }
            }
        }

        List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan);
        List<PertemuanPunyaUjian> pertemuanPunyaUjians = ConstantValues.simpleList(sess.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "p").createAlias("ujian", "u").add(Restrictions.eq("u.aktif", true)).add(Restrictions.eq("p.perkuliahan", perkuliahan)), PertemuanPunyaUjian.class);
        List<Pertemuan> pertemuansTugas = ConstantValues.simpleList(sess.createCriteria(Pertemuan.class).add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas")).add(Restrictions.eq("aktif", true)).add(Restrictions.eq("perkuliahan", perkuliahan)), Pertemuan.class);
        Collection<Long> pertemuansList = perkuliahan.ambilPertemuan().values();
        List<TugasPertemuan> pertemuansTugasLanjut = pertemuansList.isEmpty() ? new ArrayList<TugasPertemuan>() : ConstantValues.simpleList(sess.createCriteria(TugasPertemuan.class).add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas")).add(Restrictions.in("pertemuan", pertemuansList)), TugasPertemuan.class);
        List<TugasKelompok> pertemuansTugasKelompoks = ConstantValues.simpleList(sess.createCriteria(TugasKelompok.class).add(Restrictions.ne("judul", "")).add(Restrictions.isNotNull("judul")).add(Restrictions.eq("perkuliahan", perkuliahan)), TugasKelompok.class);

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
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_service.jsp:294");}
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
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_service.jsp:305");}
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
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_service.jsp:316");}
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
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_service.jsp:332");}
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

        // Agregasi Nilai Hierarki
        Map<Long, Double> nilaiFormatMhs = new HashMap<Long, Double>();
        for (FormatNilai fn : formatNilais) {
            Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(fn.getId());
            Map<Long, Pertemuan> mapdTgs = mapTugas.get(fn.getId());
            Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(fn.getId());
            Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(fn.getId());
            
            Double rawTotal = recalculateTotalPerFormat(dataNiliasUtama, persensData, mahasiswa.getId(), fn.getId(), mapd, mapdTgs, mapdTgsLanjut, mapdTgsKelompok);
            Double valKonversi = hitungKonversi(fn, rawTotal); 
            nilaiFormatMhs.put(fn.getId(), valKonversi);
        }

        // CPMK mapping Helper
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
                public int compare(CapaianPembelajaranLulusan a, CapaianPembelajaranLulusan b) { return (a.getKode()==null?"":a.getKode()).compareTo((b.getKode()==null?"":b.getKode())); }
            });
        }

        Map<Long, Double> totalCpmkScores = new HashMap<Long, Double>();
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
        }

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
        }

        Map<Long, Double> totalPlScores = new HashMap<Long, Double>();
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
            totalPlScores.put(pl.getId(), finalPlConv);
        }

        // PERSIAPAN ARRAY DATA UNTUK CHART
        JSONArray subCpmkLabels = new JSONArray();
        JSONArray subCpmkData = new JSONArray();
        JSONArray cpmkLabels = new JSONArray();
        JSONArray cpmkData = new JSONArray();
        JSONArray cplLabels = new JSONArray();
        JSONArray cplData = new JSONArray();
        JSONArray plLabels = new JSONArray();
        JSONArray plData = new JSONArray();

        // 1. ARRAY SUB-CPMK / CPMK
        if (!isNilaiCpmk) {
            for (FormatNilai fn : formatNilais) {
                Double nVal = nilaiFormatMhs.get(fn.getId()) != null ? nilaiFormatMhs.get(fn.getId()) : 0.0;
                if (nVal > 0) {
                    subCpmkLabels.put(fn.getNama());
                    subCpmkData.put(nVal);
                }
            }
            for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                Double nVal = totalCpmkScores.get(cpmk.getId()) != null ? totalCpmkScores.get(cpmk.getId()) : 0.0;
                if (nVal > 0) {
                    cpmkLabels.put(cpmk.getKode());
                    cpmkData.put(nVal);
                }
            }
        } else {
            for (FormatNilai fn : formatNilais) {
                Double nVal = nilaiFormatMhs.get(fn.getId()) != null ? nilaiFormatMhs.get(fn.getId()) : 0.0;
                if (nVal > 0) {
                    cpmkLabels.put(fn.getNama());
                    cpmkData.put(nVal);
                }
            }
        }

        // 2. ARRAY CPL
        for (CapaianLulusan cpl : capaianLulusans) {
            Double nVal = totalCplScores.get(cpl.getId()) != null ? totalCplScores.get(cpl.getId()) : 0.0;
            if (nVal > 0) {
                cplLabels.put(cpl.getKode());
                cplData.put(nVal);
            }
        }

        // 3. ARRAY PL & PROFESI
        for (ProfilLulusan pl : profilLulusans) {
            Double nVal = totalPlScores.get(pl.getId()) != null ? totalPlScores.get(pl.getId()) : 0.0;
            if (nVal > 0) {
                plLabels.put(pl.getKode());
                plData.put(nVal);
            }
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
                        <h4 class="fw-bold mb-1 text-uppercase border-bottom border-2 border-dark d-inline-block pb-2 px-3"><%=Common.getBahasaConfig("Portofolio Ketercapaian Mahasiswa (OBE)")%></h4>
                    </div>

                    <table class="table-portofolio border-0 mb-4" style="width: 70%;">
                        <tr><td class="border-0 p-1 fw-bold text-uppercase" style="width: 150px;"><%=Common.getBahasaConfig("Nama Mahasiswa")%></td><td class="border-0 p-1 fw-bold">: <%=mahasiswa.getNama()%></td></tr>
                        <tr><td class="border-0 p-1 fw-bold text-uppercase"><%=Common.getBahasaConfig("NIM")%></td><td class="border-0 p-1 fw-bold">: <%=mahasiswa.getNim()%></td></tr>
                        <tr><td class="border-0 p-1 fw-bold text-uppercase"><%=Common.getBahasaConfig("Program Studi")%></td><td class="border-0 p-1 fw-bold">: <%=mahasiswa.getJurusan() != null ? mahasiswa.getJurusan().getNama() : "-"%></td></tr>
                        <tr><td class="border-0 p-1 fw-bold text-uppercase mt-2"><%=Common.getBahasaConfig("Mata Kuliah")%></td><td class="border-0 p-1 fw-bold mt-2">: <%=kodeMk%> - <%=namaMk%></td></tr>
                    </table>

                    <% if (!isNilaiCpmk) { %>
                    <h6 class="fw-bold mt-4 mb-2"><i class="fas fa-layer-group me-2 no-print"></i>1. <%=Common.getBahasaConfig("Rekapitulasi Sub-CPMK")%></h6>
                    <table class="table-portofolio">
                        <thead>
                            <tr>
                                <th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Kode Sub-CPMK")%></th>
                                <th style="width: 25%;"><%=Common.getBahasaConfig("Nama Sub-CPMK")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% 
                            int idx1 = 1;
                            boolean adaDataSub = false;
                            for (FormatNilai fn : formatNilais) {
                                Double nVal = nilaiFormatMhs.get(fn.getId()) != null ? nilaiFormatMhs.get(fn.getId()) : 0.0;
                                if (nVal > 0) {
                                    adaDataSub = true;
                                    Double nMin = fn.ambilMinimal() != null ? fn.ambilMinimal() : 60.0;
                                    boolean isLulus = nVal >= nMin;
                            %>
                            <tr>
                                <td class="text-center"><%=idx1++%></td>
                                <td class="text-center fw-bold text-primary"><%=fn.getNama()%></td>
                                <td><%=fn.getNama()%></td>
                                <td class="text-center"><%=kodeMk%></td>
                                <td><%=namaMk%></td>
                                <td class="text-center"><%=nMin%></td>
                                <td class="text-center fw-bold"><%=Common.numberFormat.get().format(nVal)%></td>
                                <td class="text-center <%=isLulus ? "status-tercapai" : "status-gagal"%>"><%=isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")%></td>
                            </tr>
                            <%  }
                            }
                            if(!adaDataSub) { out.print("<tr><td colspan='8' class='text-center py-3'>" + Common.getBahasaConfig("Data ketercapaian belum tersedia / bernilai 0") + "</td></tr>"); }
                            %>
                        </tbody>
                    </table>
                    <% } %>

                    <h6 class="fw-bold mt-5 mb-2"><i class="fas fa-bullseye me-2 no-print"></i><%= isNilaiCpmk ? "1." : "2." %> <%=Common.getBahasaConfig("Rekapitulasi CPMK")%></h6>
                    <table class="table-portofolio">
                        <thead>
                            <tr>
                                <th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Kode CPMK")%></th>
                                <th style="width: 25%;"><%=Common.getBahasaConfig("Nama CPMK")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% 
                            int idx2 = 1;
                            boolean adaDataCpmk = false;

                            if (isNilaiCpmk) {
                                for (FormatNilai fn : formatNilais) {
                                    Double nVal = nilaiFormatMhs.get(fn.getId()) != null ? nilaiFormatMhs.get(fn.getId()) : 0.0;
                                    if (nVal > 0) {
                                        adaDataCpmk = true;
                                        Double nMin = fn.ambilMinimal() != null ? fn.ambilMinimal() : 60.0;
                                        boolean isLulus = nVal >= nMin;
                            %>
                                <tr>
                                    <td class="text-center"><%=idx2++%></td>
                                    <td class="text-center fw-bold text-primary"><%=fn.getNama()%></td>
                                    <td><%=fn.getNama()%></td>
                                    <td class="text-center"><%=kodeMk%></td>
                                    <td><%=namaMk%></td>
                                    <td class="text-center"><%=nMin%></td>
                                    <td class="text-center fw-bold"><%=Common.numberFormat.get().format(nVal)%></td>
                                    <td class="text-center <%=isLulus ? "status-tercapai" : "status-gagal"%>"><%=isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")%></td>
                                </tr>
                            <%      }
                                }
                            } else {
                                for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
                                    Double nVal = totalCpmkScores.get(cpmk.getId()) != null ? totalCpmkScores.get(cpmk.getId()) : 0.0;
                                    if (nVal > 0) {
                                        adaDataCpmk = true;
                                        Double nMin = 60.0; 
                                        boolean isLulus = nVal >= nMin;
                            %>
                                <tr>
                                    <td class="text-center"><%=idx2++%></td>
                                    <td class="text-center fw-bold text-primary"><%=cpmk.getKode()%></td>
                                    <td><%=cpmk.getNama()%></td>
                                    <td class="text-center"><%=kodeMk%></td>
                                    <td><%=namaMk%></td>
                                    <td class="text-center"><%=nMin%></td>
                                    <td class="text-center fw-bold"><%=Common.numberFormat.get().format(nVal)%></td>
                                    <td class="text-center <%=isLulus ? "status-tercapai" : "status-gagal"%>"><%=isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")%></td>
                                </tr>
                            <%      }
                                }
                            } 
                            if (!adaDataCpmk) { out.print("<tr><td colspan='8' class='text-center py-3'>" + Common.getBahasaConfig("Data ketercapaian belum tersedia / bernilai 0") + "</td></tr>"); }
                            %>
                        </tbody>
                    </table>

                    <h6 class="fw-bold mt-5 mb-2"><i class="fas fa-graduation-cap me-2 no-print"></i><%= isNilaiCpmk ? "2." : "3." %> <%=Common.getBahasaConfig("Rekapitulasi CPL")%></h6>
                    <table class="table-portofolio">
                        <thead>
                            <tr>
                                <th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Kode CPL")%></th>
                                <th style="width: 25%;"><%=Common.getBahasaConfig("Nama CPL")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% 
                            int idx3 = 1;
                            boolean adaDataCpl = false;
                            for (CapaianLulusan cpl : capaianLulusans) {
                                Double nVal = totalCplScores.get(cpl.getId()) != null ? totalCplScores.get(cpl.getId()) : 0.0;
                                if (nVal > 0) {
                                    adaDataCpl = true;
                                    Double nMin = 60.0; 
                                    boolean isLulus = nVal >= nMin;
                            %>
                            <tr>
                                <td class="text-center"><%=idx3++%></td>
                                <td class="text-center fw-bold text-primary"><%=cpl.getKode()%></td>
                                <td><%=cpl.getNama()%></td>
                                <td class="text-center"><%=kodeMk%></td>
                                <td><%=namaMk%></td>
                                <td class="text-center"><%=nMin%></td>
                                <td class="text-center fw-bold"><%=Common.numberFormat.get().format(nVal)%></td>
                                <td class="text-center <%=isLulus ? "status-tercapai" : "status-gagal"%>"><%=isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")%></td>
                            </tr>
                            <%  }
                            }
                            if (!adaDataCpl) { out.print("<tr><td colspan='8' class='text-center py-3'>" + Common.getBahasaConfig("Data ketercapaian belum tersedia / bernilai 0") + "</td></tr>"); }
                            %>
                        </tbody>
                    </table>

                    <h6 class="fw-bold mt-5 mb-2"><i class="fas fa-user-tie me-2 no-print"></i><%= isNilaiCpmk ? "3." : "4." %> <%=Common.getBahasaConfig("Rekapitulasi Profil Lulusan")%></h6>
                    <table class="table-portofolio">
                        <thead>
                            <tr>
                                <th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Kode PL")%></th>
                                <th style="width: 25%;"><%=Common.getBahasaConfig("Nama Profil Lulusan")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% 
                            int idx4 = 1;
                            boolean adaDataPl = false;
                            for (ProfilLulusan pl : profilLulusans) {
                                Double nVal = totalPlScores.get(pl.getId()) != null ? totalPlScores.get(pl.getId()) : 0.0;
                                if (nVal > 0) {
                                    adaDataPl = true;
                                    Double nMin = 60.0; 
                                    boolean isLulus = nVal >= nMin;
                            %>
                            <tr>
                                <td class="text-center"><%=idx4++%></td>
                                <td class="text-center fw-bold text-primary"><%=pl.getKode()%></td>
                                <td><%=pl.getNama()%></td>
                                <td class="text-center"><%=kodeMk%></td>
                                <td><%=namaMk%></td>
                                <td class="text-center"><%=nMin%></td>
                                <td class="text-center fw-bold"><%=Common.numberFormat.get().format(nVal)%></td>
                                <td class="text-center <%=isLulus ? "status-tercapai" : "status-gagal"%>"><%=isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")%></td>
                            </tr>
                            <%  }
                            }
                            if (!adaDataPl) { out.print("<tr><td colspan='8' class='text-center py-3'>" + Common.getBahasaConfig("Data ketercapaian belum tersedia / bernilai 0") + "</td></tr>"); }
                            %>
                        </tbody>
                    </table>

                    <h6 class="fw-bold mt-5 mb-2"><i class="fas fa-briefcase me-2 no-print"></i><%= isNilaiCpmk ? "4." : "5." %> <%=Common.getBahasaConfig("Rekapitulasi Profesi Lulusan")%></h6>
                    <table class="table-portofolio mb-5">
                        <thead>
                            <tr>
                                <th style="width: 5%;"><%=Common.getBahasaConfig("No.")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Kode Profesi")%></th>
                                <th style="width: 25%;"><%=Common.getBahasaConfig("Nama Profesi")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Kode MK")%></th>
                                <th style="width: 15%;"><%=Common.getBahasaConfig("Nama MK")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai Minimal")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Nilai")%></th>
                                <th style="width: 10%;"><%=Common.getBahasaConfig("Status")%></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% 
                            int idx5 = 1;
                            boolean adaDataProfesi = false;
                            for (ProfilLulusan pl : profilLulusans) {
                                Double nVal = totalPlScores.get(pl.getId()) != null ? totalPlScores.get(pl.getId()) : 0.0;
                                if (nVal > 0) {
                                    adaDataProfesi = true;
                                    Double nMin = 60.0; 
                                    boolean isLulus = nVal >= nMin;
                            %>
                            <tr>
                                <td class="text-center"><%=idx5++%></td>
                                <td class="text-center fw-bold text-primary"><%=pl.getKode()%></td>
                                <td><%=pl.getNama()%></td>
                                <td class="text-center"><%=kodeMk%></td>
                                <td><%=namaMk%></td>
                                <td class="text-center"><%=nMin%></td>
                                <td class="text-center fw-bold"><%=Common.numberFormat.get().format(nVal)%></td>
                                <td class="text-center <%=isLulus ? "status-tercapai" : "status-gagal"%>"><%=isLulus ? Common.getBahasaConfig("Tercapai") : Common.getBahasaConfig("Tidak Tercapai")%></td>
                            </tr>
                            <%  }
                            }
                            if (!adaDataProfesi) { out.print("<tr><td colspan='8' class='text-center py-3'>" + Common.getBahasaConfig("Data ketercapaian belum tersedia / bernilai 0") + "</td></tr>"); }
                            %>
                        </tbody>
                    </table>

                    <div class="row g-4 mt-2 mb-4 no-print" style="page-break-inside: avoid;">
                        <% if (!isNilaiCpmk && subCpmkLabels.length() > 0) { %>
                        <div class="col-md-6">
                            <div class="card shadow-sm border-0 h-100 bg-light">
                                <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-info">
                                    <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Grafik Ketercapaian Sub-CPMK")%>
                                </div>
                                <div class="card-body pt-0 pb-4 px-4">
                                    <div id="chartPortoSubCpmk<%=rnd%>" class="el-css-chart"></div>
                                </div>
                            </div>
                        </div>
                        <% } %>

                        <% if (cpmkLabels.length() > 0) { %>
                        <div class="col-md-6">
                            <div class="card shadow-sm border-0 h-100 bg-light">
                                <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-primary">
                                    <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Grafik Ketercapaian CPMK")%>
                                </div>
                                <div class="card-body pt-0 pb-4 px-4">
                                    <div id="chartPortoCpmk<%=rnd%>" class="el-css-chart"></div>
                                </div>
                            </div>
                        </div>
                        <% } %>
                        
                        <% if (cplLabels.length() > 0) { %>
                        <div class="col-md-6 mt-4">
                            <div class="card shadow-sm border-0 h-100 bg-light">
                                <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-success">
                                    <i class="fas fa-chart-bar me-2"></i><%=Common.getBahasaConfig("Grafik Ketercapaian CPL")%>
                                </div>
                                <div class="card-body pt-0 pb-4 px-4">
                                    <div id="chartPortoCpl<%=rnd%>" class="el-css-chart"></div>
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
                                    <div id="chartPortoPl<%=rnd%>" class="el-css-chart"></div>
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
                                    <div id="chartPortoProfesi<%=rnd%>" class="el-css-chart"></div>
                                </div>
                            </div>
                        </div>
                        <% } %>

                        <% if (cplLabels.length() > 0) { %>
                        <div class="col-md-6 mt-4">
                            <div class="card shadow-sm border-0 h-100 bg-light">
                                <div class="card-header bg-transparent text-center fw-bold py-3 border-0" style="color: #1cc88a;">
                                    <i class="fas fa-spider me-2"></i><%=Common.getBahasaConfig("Spider Web CPL")%>
                                </div>
                                <div class="card-body pt-0 pb-4 px-4 d-flex justify-content-center align-items-center">
                                    <div id="chartRadarPortoCpl<%=rnd%>" class="el-css-chart"></div>
                                </div>
                            </div>
                        </div>
                        <% } %>

                        <% if (plLabels.length() > 0) { %>
                        <div class="col-md-6 mt-4">
                            <div class="card shadow-sm border-0 h-100 bg-light">
                                <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-warning">
                                    <i class="fas fa-spider me-2"></i><%=Common.getBahasaConfig("Spider Web Profil Lulusan")%>
                                </div>
                                <div class="card-body pt-0 pb-4 px-4 d-flex justify-content-center align-items-center">
                                    <div id="chartRadarPortoPl<%=rnd%>" class="el-css-chart"></div>
                                </div>
                            </div>
                        </div>
                        <% } %>

                        <% if (plLabels.length() > 0) { %>
                        <div class="col-md-6 mt-4">
                            <div class="card shadow-sm border-0 h-100 bg-light">
                                <div class="card-header bg-transparent text-center fw-bold py-3 border-0 text-danger">
                                    <i class="fas fa-spider me-2"></i><%=Common.getBahasaConfig("Spider Web Profesi Lulusan")%>
                                </div>
                                <div class="card-body pt-0 pb-4 px-4 d-flex justify-content-center align-items-center">
                                    <div id="chartRadarPortoProfesi<%=rnd%>" class="el-css-chart"></div>
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

            function renderPortoCharts<%=rnd%>() {
                try {
                    <% if (subCpmkLabels.length() > 0) { %>elRenderBar('chartPortoSubCpmk<%=rnd%>', <%=subCpmkLabels.toString()%>, <%=subCpmkData.toString()%>);<% } %>
                    <% if (cpmkLabels.length() > 0) { %>elRenderBar('chartPortoCpmk<%=rnd%>', <%=cpmkLabels.toString()%>, <%=cpmkData.toString()%>);<% } %>
                    <% if (cplLabels.length() > 0) { %>elRenderBar('chartPortoCpl<%=rnd%>', <%=cplLabels.toString()%>, <%=cplData.toString()%>); elRenderRadar('chartRadarPortoCpl<%=rnd%>', <%=cplLabels.toString()%>, <%=cplData.toString()%>);<% } %>
                    <% if (plLabels.length() > 0) { %>elRenderBar('chartPortoPl<%=rnd%>', <%=plLabels.toString()%>, <%=plData.toString()%>); elRenderBar('chartPortoProfesi<%=rnd%>', <%=plLabels.toString()%>, <%=plData.toString()%>); elRenderRadar('chartRadarPortoPl<%=rnd%>', <%=plLabels.toString()%>, <%=plData.toString()%>); elRenderRadar('chartRadarPortoProfesi<%=rnd%>', <%=plLabels.toString()%>, <%=plData.toString()%>);<% } %>
                } catch(e) { console.error('Error rendering HTML/CSS charts: ', e); }
            }
            setTimeout(renderPortoCharts<%=rnd%>, 120);
        </script>
        <%
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_service.jsp:953");
    out.print("<div class='alert alert-danger m-4 shadow-sm'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Terjadi kesalahan saat memproses data layanan.") + "</div>");
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_portofolio_mahasiswa_service.jsp:956");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
}
%>