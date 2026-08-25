<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="org.hibernate.Session"%>
<%@ page import="org.hibernate.Criteria"%>
<%@ page import="org.hibernate.criterion.*"%>
<%@ page import="ais.database.hibernate.HibernateUtil"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.database.model.*"%>
<%@ page import="ais.database.model.obe.*"%>
<%@ page import="org.json.*"%>

<%!
    private int countUniqueRelationIds(String csv) {
        Set<Long> ids = new HashSet<Long>();
        if (csv == null || csv.trim().isEmpty()) return 0;
        for (String token : csv.split(",")) {
            String value = token != null ? token.trim() : "";
            if (value.isEmpty()) continue;
            try { ids.add(Long.valueOf(value)); }
            catch (NumberFormatException ignored) { /* token rusak tidak dihitung */ }
        }
        return ids.size();
    }
%>

<%
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return;
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        out.print("{\"status\":\"error\",\"msg\":\"" + Common.getBahasaConfig("Sesi telah berakhir.") + "\"}");
        return;
    }

    String q_ta  = request.getParameter("q_ta")  != null ? request.getParameter("q_ta").trim()  : "";
    String q_smt = request.getParameter("q_smt") != null ? request.getParameter("q_smt").trim() : "";
    String q_fakultas = request.getParameter("q_fakultas") != null ? request.getParameter("q_fakultas").trim() : "";
    String q_jurusan  = request.getParameter("q_jurusan")  != null ? request.getParameter("q_jurusan").trim()  : "";

    if (q_ta.isEmpty() || q_smt.isEmpty()) {
        out.print("{\"status\":\"error\",\"msg\":\"" + Common.getBahasaConfig("Tahun Akademik dan Semester wajib dipilih.") + "\"}");
        return;
    }

    boolean isMhs = tbmuser.getMahasiswa() != null;
    boolean isDsn = tbmuser.ambilDosen() != null;

    Session sess = null;
    JSONObject resp = new JSONObject();

    try {
        sess = HibernateUtil.openSession();

        // ─── KUERI UTAMA: Perkuliahan OBE ────────────────────────────────────────
        @SuppressWarnings("deprecation")
        Criteria crit = sess.createCriteria(Perkuliahan.class, "p")
            .add(Restrictions.eq("p.tahunAjaran", q_ta))
            .add(Restrictions.eq("p.ganjilGenap", q_smt))
            .add(Restrictions.isNotNull("p.kurikulumPunyaMatakuliah"))
            .createAlias("p.kurikulumPunyaMatakuliah", "kpm")
            .createAlias("kpm.kurikulum", "kur")
            .add(Restrictions.eq("kur.obe", Boolean.TRUE))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        if (isDsn) {
            crit.add(Restrictions.eq("p.dosen", tbmuser.ambilDosen()));
        }
        if (!q_jurusan.isEmpty()) {
            try { crit.add(Restrictions.eq("p.jurusan.id", Long.parseLong(q_jurusan))); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_obe_service.jsp:60");}
        } else if (!q_fakultas.isEmpty() && !isDsn && !isMhs) {
            try {
                crit.createAlias("p.jurusan", "jur")
                    .createAlias("jur.fakultas", "fak")
                    .add(Restrictions.eq("fak.id", Long.parseLong(q_fakultas)));
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_obe_service.jsp:66");}
        }

        @SuppressWarnings("unchecked")
        List<Perkuliahan> perkuliahans = (List<Perkuliahan>) crit.list();

        // ─── FILTER MAHASISWA: hanya MK yang dia ikuti ───────────────────────────
        if (isMhs && tbmuser.getMahasiswa() != null) {
            final Long mhsId = tbmuser.getMahasiswa().getId();
            @SuppressWarnings("deprecation")
            List<Long> joinedPerkIds = (List<Long>) sess.createCriteria(Detailperkuliahan.class, "dp")
                .add(Restrictions.eq("dp.mahasiswa.id", mhsId))
                .add(Restrictions.eq("dp.persetujuan", Detailperkuliahan.DISETUJUI))
                .setProjection(Projections.property("dp.perkuliahan.id"))
                .list();
            Set<Long> joinedSet = new HashSet<Long>(joinedPerkIds);
            List<Perkuliahan> filtered = new ArrayList<Perkuliahan>();
            for (Perkuliahan p : perkuliahans) {
                if (joinedSet.contains(p.getId())) filtered.add(p);
            }
            perkuliahans = filtered;
        }

        // ─── BANGUN RESPONSE ─────────────────────────────────────────────────────
        int totalMk      = perkuliahans.size();
        int sudahRps     = 0;
        int belumRps     = 0;
        int mkPunyaCpl   = 0;
        int mkPunyaCpmk  = 0;

        JSONArray arrRps  = new JSONArray();
        JSONArray arrCpl  = new JSONArray();
        // Gap #5: hitung berapa MK yang cover tiap CPL (untuk heatmap)
        Map<String, Integer> cplCoverageCount = new java.util.LinkedHashMap<String, Integer>();
        Map<String, Double>  cplBobotSum      = new java.util.LinkedHashMap<String, Double>();

        for (Perkuliahan p : perkuliahans) {
            try {
                KurikulumPunyaMatakuliah kpm = p.getKurikulumPunyaMatakuliah();
                Matakuliah mk = p.getMatakuliah();
                if (kpm == null || mk == null) continue;

                boolean punyaRps = kpm.getRincian() != null && !kpm.getRincian().trim().isEmpty()
                                   && kpm.getTanggalPenyusunan() != null;
                boolean punyaCpl  = mk.getCapaianLulusan() != null  && !mk.getCapaianLulusan().trim().isEmpty();
                boolean punyaCpmk = mk.getCapaianPembelajaranLulusan() != null && !mk.getCapaianPembelajaranLulusan().trim().isEmpty();

                if (punyaRps) sudahRps++; else belumRps++;
                if (punyaCpl)  mkPunyaCpl++;
                if (punyaCpmk) mkPunyaCpmk++;

                // Hitung jumlah CPL dan CPMK
                int jmlCpl  = 0, jmlCpmk = 0, jmlBk = 0;
                if (punyaCpl)  jmlCpl  = countUniqueRelationIds(mk.getCapaianLulusan());
                if (punyaCpmk) jmlCpmk = countUniqueRelationIds(mk.getCapaianPembelajaranLulusan());
                if (mk.getBahanKajian() != null && !mk.getBahanKajian().trim().isEmpty())
                    jmlBk = countUniqueRelationIds(mk.getBahanKajian());

                // Nama dosen
                String namaDosen = "";
                if (p.getDosen1() != null) namaDosen = p.getDosen1().getNama() != null ? p.getDosen1().getNama() : "";

                // Nama prodi
                String namaProdi = p.getJurusan() != null ? (p.getJurusan().getNama() != null ? p.getJurusan().getNama() : "") : "";

                // ─── Baris rekap RPS ───────────────────────────────────────────
                JSONObject rowRps = new JSONObject();
                rowRps.put("id",       p.getId());
                rowRps.put("kode",     mk.getKode() != null ? mk.getKode() : "");
                rowRps.put("nama",     mk.getNama() != null ? mk.getNama() : "");
                rowRps.put("prodi",    namaProdi);
                rowRps.put("dosen",    namaDosen);
                rowRps.put("punyaRps", punyaRps);
                rowRps.put("tglRps",   punyaRps && kpm.getTanggalPenyusunan() != null
                                       ? Common.dateFormat6.get().format(kpm.getTanggalPenyusunan()) : "-");
                rowRps.put("dikunci",  kpm.getDikunci() != null);
                rowRps.put("jmlCpl",   jmlCpl);
                rowRps.put("jmlCpmk",  jmlCpmk);
                rowRps.put("jmlBk",    jmlBk);
                rowRps.put("sks",      mk.getSks());
                rowRps.put("kpmId",    kpm.getId());
                arrRps.put(rowRps);

                // ─── Baris CPL ────────────────────────────────────────────────
                if (punyaCpl) {
                    String cplBobotStr = kpm.getCplBobot() != null ? kpm.getCplBobot() : "";
                    // Gap #5: akumulasi coverage & bobot tiap CPL
                    if (!cplBobotStr.isEmpty()) {
                        for (String cp : cplBobotStr.split(",")) {
                            cp = cp.trim();
                            String cplKey = cp.contains(":") ? cp.split(":", 2)[0].trim().toUpperCase() : cp.toUpperCase();
                            if (cplKey.isEmpty()) continue;
                            cplCoverageCount.put(cplKey, cplCoverageCount.containsKey(cplKey) ? cplCoverageCount.get(cplKey) + 1 : 1);
                            if (cp.contains(":")) {
                                double bobotVal = 0;
                                try { bobotVal = Double.parseDouble(cp.split(":", 2)[1].trim()); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_obe_service.jsp:161");}
                                cplBobotSum.put(cplKey, (cplBobotSum.containsKey(cplKey) ? cplBobotSum.get(cplKey) : 0.0) + bobotVal);
                            }
                        }
                    } else if (!mk.getCapaianLulusan().trim().isEmpty()) {
                        // Fallback: hitung dari daftar CPL MK tanpa bobot
                        for (String cplId : mk.getCapaianLulusan().split(",")) {
                            String cplKey = cplId.trim().toUpperCase();
                            if (cplKey.isEmpty()) continue;
                            cplCoverageCount.put(cplKey, cplCoverageCount.containsKey(cplKey) ? cplCoverageCount.get(cplKey) + 1 : 1);
                        }
                    }
                    JSONObject rowCpl = new JSONObject();
                    rowCpl.put("mk",     (mk.getKode() != null ? mk.getKode() : "") + " - " + (mk.getNama() != null ? mk.getNama() : ""));
                    rowCpl.put("prodi",  namaProdi);
                    rowCpl.put("jmlCpl", jmlCpl);
                    rowCpl.put("jmlCpmk",jmlCpmk);
                    rowCpl.put("jmlBk",  jmlBk);
                    rowCpl.put("punyaRps", punyaRps);
                    rowCpl.put("cplBobot", cplBobotStr);
                    rowCpl.put("minKetercapaian", kpm.getMinimalKetercapaian() != null ? kpm.getMinimalKetercapaian() : 0.0);
                    arrCpl.put(rowCpl);
                }

            } catch (Exception ePerk) { ais.common.ErrorAuditUtil.record(ePerk, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_obe_service.jsp:185");
                // Lewati baris bermasalah
            }
        }

        // Gap #5: susun array heatmap CPL coverage
        JSONArray arrCplCoverage = new JSONArray();
        for (Map.Entry<String, Integer> entry : cplCoverageCount.entrySet()) {
            JSONObject item = new JSONObject();
            item.put("cpl",    entry.getKey());
            item.put("count",  entry.getValue());
            item.put("bobotTotal", cplBobotSum.containsKey(entry.getKey()) ? cplBobotSum.get(entry.getKey()) : 0.0);
            arrCplCoverage.put(item);
        }

        JSONObject stats = new JSONObject();
        stats.put("totalMk",    totalMk);
        stats.put("sudahRps",   sudahRps);
        stats.put("belumRps",   belumRps);
        stats.put("mkPunyaCpl", mkPunyaCpl);
        stats.put("cplCoverage", arrCplCoverage);

        JSONObject rekap = new JSONObject();
        rekap.put("rps",  arrRps);
        rekap.put("cpl",  arrCpl);

        resp.put("status", "ok");
        resp.put("stats",  stats);
        resp.put("rekap",  rekap);

    } catch (Exception e) {
        resp.put("status", "error");
        resp.put("msg", e.getMessage() != null ? e.getMessage() : "Terjadi kesalahan.");
        if (Common.getApakahAdmin()) resp.put("trace", e.getClass().getName());
    } finally {
        if (sess != null && sess.isOpen()) {
            try { sess.clear(); sess.disconnect(); sess.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/services/_obe_service.jsp:221");}
        }
        HibernateUtil.closeSessionQuietly(sess);
    }

    out.print(resp.toString());
%>
