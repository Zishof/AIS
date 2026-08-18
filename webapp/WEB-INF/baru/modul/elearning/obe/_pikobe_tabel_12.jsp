<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.obe.CapaianLulusan"%>
<%@page import="ais.database.model.obe.CapaianPembelajaranLulusan"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");
String keyword = request.getParameter("keyword");

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) return;

Long idJurusan = null;
try { if(idJurusanStr != null && !idJurusanStr.trim().isEmpty()) idJurusan = Long.parseLong(idJurusanStr); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_12.jsp:30");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

if ("loadTabel12".equals(action)) {
    List<KurikulumPunyaMatakuliah> listKpm = new ArrayList<KurikulumPunyaMatakuliah>();
    List<CapaianLulusan> allCpl = new ArrayList<CapaianLulusan>();
    List<CapaianPembelajaranLulusan> allCpmk = new ArrayList<CapaianPembelajaranLulusan>();
    Kurikulum kurikulumAktif = null;
    Session sess = null;
    
    try {
        sess = HibernateUtil.openSession();
        
        allCpl = ConstantValues.simpleList(sess.createCriteria(CapaianLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("kode")), CapaianLulusan.class);
        allCpmk = ConstantValues.simpleList(sess.createCriteria(CapaianPembelajaranLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("kode")), CapaianPembelajaranLulusan.class);

        kurikulumAktif = (Kurikulum) sess.createCriteria(Kurikulum.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.eq("obe", true)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
        
        if(kurikulumAktif != null) {
            Criteria critKpm = sess.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "mk");
            critKpm.add(Restrictions.eq("kurikulum.id", kurikulumAktif.getId()));
            critKpm.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                Disjunction orMk = Restrictions.disjunction();
                orMk.add(Restrictions.ilike("mk.kode", keyword, MatchMode.ANYWHERE));
                orMk.add(Restrictions.ilike("mk.nama", keyword, MatchMode.ANYWHERE));
                critKpm.add(orMk);
            }
            
            critKpm.addOrder(Order.asc("mk.kode"));
            listKpm = ConstantValues.simpleList(critKpm, KurikulumPunyaMatakuliah.class);
        }
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_12.jsp:67"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
%>

    <% if(kurikulumAktif == null) { %>
        <div class="alert alert-warning text-center"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Kurikulum OBE tidak ditemukan.")%></div>
    <% } else if(allCpl.isEmpty()) { %>
        <div class="alert alert-warning text-center"><i class="fas fa-folder-open me-2"></i><%=Common.getBahasaConfig("Belum ada data Capaian Lulusan (CPL).")%></div>
    <% } else { %>
        <div class="table-responsive border rounded-3 animate__animated animate__fadeIn" style="max-height: 700px; overflow-y: auto;">
            <table class="table table-bordered align-middle mb-0 text-start small">
                <thead class="table-light sticky-top shadow-sm" style="z-index: 10;">
                    <tr class="text-center">
                        <th style="min-width: 150px;"><%=Common.getBahasaConfig("CPL")%></th>
                        <th style="min-width: 250px;"><%=Common.getBahasaConfig("Matakuliah / Kurikulum")%></th>
                        <th style="width: 120px;"><%=Common.getBahasaConfig("Induk CPMK")%></th>
                        <th style="min-width: 150px;"><%=Common.getBahasaConfig("Kode CPMK/Sub")%></th>
                        <th style="width: 100px;"><%=Common.getBahasaConfig("Bobot (%)")%></th>
                        <th style="width: 120px;"><%=Common.getBahasaConfig("Total di MK")%></th>
                        <th style="width: 120px;"><%=Common.getBahasaConfig("Total CPL")%></th>
                    </tr>
                </thead>
                <tbody>
                    <% 
                    int cplCounter = 0;
                    List<Map<String, Object>> finalRenderRows = new ArrayList<Map<String, Object>>();

                    for (CapaianLulusan cpl : allCpl) {
                        cplCounter++;
                        String cplBgColor = (cplCounter % 2 == 0) ? "background-color: #f8fbf8;" : "background-color: #ffffff;";

                        // Cari Matakuliah yang menunjang CPL ini
                        List<KurikulumPunyaMatakuliah> mappedKpm = new ArrayList<KurikulumPunyaMatakuliah>();
                        for(KurikulumPunyaMatakuliah kpm : listKpm) {
                            String mkCpl = kpm.getMatakuliah().getCapaianLulusan();
                            if(mkCpl != null && mkCpl.contains("," + cpl.getId() + ",")) {
                                mappedKpm.add(kpm);
                            }
                        }

                        List<Map<String, Object>> tempCplRows = new ArrayList<Map<String, Object>>();
                        double totalBobotCplGlobal = 0.0;
                        int cplRowCount = 0;

                        if (mappedKpm.isEmpty()) {
                            Map<String, Object> r = new HashMap<String, Object>();
                            r.put("cpl", cpl); r.put("mk", null); r.put("kpm", null);
                            r.put("induk", "-"); r.put("kode", "-"); r.put("bobot", 0.0); r.put("isDuplicate", false);
                            r.put("isFirstMkRow", true); r.put("mkRowCount", 1); r.put("totalBobotMkLocal", 0.0);
                            r.put("isFirstIndukRow", true); r.put("indukRowCount", 1);
                            tempCplRows.add(r);
                            cplRowCount++;
                        } else {
                            for (KurikulumPunyaMatakuliah kpm : mappedKpm) {
                                Matakuliah mk = kpm.getMatakuliah();
                                boolean isCpmkBase = kpm.getNilaiMenggunakanCpmk() != null ? kpm.getNilaiMenggunakanCpmk() : false;
                                String mkCpmkStr = mk.getCapaianPembelajaranLulusan() != null ? mk.getCapaianPembelajaranLulusan() : "";
                                String cplCpmkStr = cpl.getCapaianPembelajaranLulusan() != null ? cpl.getCapaianPembelajaranLulusan() : "";

                                // Ambil CPMK yang merupakan irisan antara CPL dan MK
                                List<CapaianPembelajaranLulusan> mappedCpmk = new ArrayList<CapaianPembelajaranLulusan>();
                                for(CapaianPembelajaranLulusan cpmk : allCpmk) {
                                    if(cplCpmkStr.contains("," + cpmk.getId() + ",") && mkCpmkStr.contains("," + cpmk.getId() + ",")) {
                                        mappedCpmk.add(cpmk);
                                    }
                                }

                                List<Map<String, Object>> tempMkRows = new ArrayList<Map<String, Object>>();
                                double totalBobotMkLocal = 0.0;
                                int mkRowCount = 0;
                                Set<String> processedItemsForMk = new HashSet<String>();

                                if (mappedCpmk.isEmpty()) {
                                    Map<String, Object> r = new HashMap<String, Object>();
                                    r.put("cpl", cpl); r.put("mk", mk); r.put("kpm", kpm); r.put("isCpmkBase", isCpmkBase);
                                    r.put("induk", "-"); r.put("kode", "-"); r.put("bobot", 0.0); r.put("isDuplicate", false);
                                    r.put("isFirstIndukRow", true); r.put("indukRowCount", 1);
                                    tempMkRows.add(r);
                                    mkRowCount++;
                                } else {
                                    for (CapaianPembelajaranLulusan cpmk : mappedCpmk) {
                                        if (isCpmkBase) {
                                            double b = 0.0;
                                            boolean isDuplicate = false;
                                            String uniqueId = cpmk.getId().toString();
                                            
                                            b = cpmk.getBobot() != null ? cpmk.getBobot() : 0.0;
                                            if (processedItemsForMk.contains(uniqueId)) {
                                                isDuplicate = true;
                                            } else {
                                                totalBobotMkLocal += b;
                                                totalBobotCplGlobal += b;
                                                processedItemsForMk.add(uniqueId);
                                            }

                                            Map<String, Object> r = new HashMap<String, Object>();
                                            r.put("cpl", cpl); r.put("mk", mk); r.put("kpm", kpm); r.put("isCpmkBase", isCpmkBase);
                                            r.put("induk", "-"); r.put("kode", cpmk.getKode()); r.put("bobot", b); r.put("isDuplicate", isDuplicate);
                                            r.put("isFirstIndukRow", true); r.put("indukRowCount", 1);
                                            tempMkRows.add(r);
                                            mkRowCount++;
                                        } else {
                                            String formulaJson = cpmk.getFormula();
                                            boolean hasSub = false;
                                            List<Map<String, Object>> tempIndukRows = new ArrayList<Map<String, Object>>();
                                            
                                            try {
                                                JSONArray arr = new JSONArray(formulaJson);
                                                for(int i=0; i<arr.length(); i++) {
                                                    JSONObject obj = arr.getJSONObject(i);
                                                    if(obj.isNull("key")) continue;
                                                    
                                                    double b = !obj.isNull("bobot") ? Double.parseDouble(obj.getString("bobot")) : 0.0;
                                                    boolean isDuplicate = false;
                                                    String keyId = obj.getString("key") + "_" + cpmk.getId().toString(); // Deduplikasi Sub-CPMK
                                                    
                                                    if (processedItemsForMk.contains(keyId)) {
                                                        isDuplicate = true;
                                                    } else {
                                                        totalBobotMkLocal += b;
                                                        totalBobotCplGlobal += b;
                                                        processedItemsForMk.add(keyId);
                                                    }

                                                    String kodeS = !obj.isNull("kode") ? obj.getString("kode") : "";
                                                    Map<String, Object> r = new HashMap<String, Object>();
                                                    r.put("cpl", cpl); r.put("mk", mk); r.put("kpm", kpm); r.put("isCpmkBase", isCpmkBase);
                                                    r.put("induk", cpmk.getKode()); r.put("kode", kodeS); r.put("bobot", b); r.put("isDuplicate", isDuplicate);
                                                    tempIndukRows.add(r);
                                                    mkRowCount++;
                                                    hasSub = true;
                                                }
                                            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_12.jsp:198");}

                                            if (!hasSub) {
                                                Map<String, Object> r = new HashMap<String, Object>();
                                                r.put("cpl", cpl); r.put("mk", mk); r.put("kpm", kpm); r.put("isCpmkBase", isCpmkBase);
                                                r.put("induk", cpmk.getKode()); r.put("kode", "-"); r.put("bobot", 0.0); r.put("isDuplicate", false);
                                                r.put("isFirstIndukRow", true); r.put("indukRowCount", 1);
                                                tempMkRows.add(r);
                                                mkRowCount++;
                                            } else {
                                                // Handle Rowspan Induk CPMK
                                                int countInduk = tempIndukRows.size();
                                                for(int j=0; j<countInduk; j++) {
                                                    Map<String, Object> rowI = tempIndukRows.get(j);
                                                    rowI.put("isFirstIndukRow", j == 0);
                                                    rowI.put("indukRowCount", countInduk);
                                                    tempMkRows.add(rowI);
                                                }
                                            }
                                        }
                                    }
                                }

                                // Handle Rowspan Matakuliah
                                totalBobotMkLocal = Math.round(totalBobotMkLocal * 100.0) / 100.0;
                                for (int i = 0; i < mkRowCount; i++) {
                                    Map<String, Object> r = tempMkRows.get(i);
                                    r.put("isFirstMkRow", i == 0);
                                    r.put("mkRowCount", mkRowCount);
                                    r.put("totalBobotMkLocal", totalBobotMkLocal);
                                    tempCplRows.add(r);
                                    cplRowCount++;
                                }
                            }
                        }

                        // Handle Rowspan CPL Level (Paling Depan)
                        totalBobotCplGlobal = Math.round(totalBobotCplGlobal * 100.0) / 100.0;
                        for (int i = 0; i < cplRowCount; i++) {
                            Map<String, Object> r = tempCplRows.get(i);
                            r.put("isFirstCplRow", i == 0);
                            r.put("cplRowCount", cplRowCount);
                            r.put("totalBobotCplGlobal", totalBobotCplGlobal);
                            r.put("cplBgColor", cplBgColor);
                            finalRenderRows.add(r);
                        }
                    }

                    // ============================== RENDER KE HTML ==============================
                    for(int r = 0; r < finalRenderRows.size(); r++) { 
                        Map<String, Object> rowData = finalRenderRows.get(r);
                        CapaianLulusan cpl = (CapaianLulusan) rowData.get("cpl");
                        Matakuliah mk = (Matakuliah) rowData.get("mk");
                        KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) rowData.get("kpm");
                        
                        boolean isFirstCplRow = (Boolean) rowData.get("isFirstCplRow");
                        int cplRowCount = (Integer) rowData.get("cplRowCount");
                        boolean isFirstMkRow = (Boolean) rowData.get("isFirstMkRow");
                        int mkRowCount = (Integer) rowData.get("mkRowCount");
                        boolean isFirstIndukRow = (Boolean) rowData.get("isFirstIndukRow");
                        int indukRowCount = (Integer) rowData.get("indukRowCount");
                        
                        boolean isCpmkBase = rowData.get("isCpmkBase") != null ? (Boolean) rowData.get("isCpmkBase") : false;
                        boolean isDuplicate = (Boolean) rowData.get("isDuplicate");
                        String rowBgColor = (String) rowData.get("cplBgColor");
                        String baseLabel = isCpmkBase ? "CPMK" : "Sub-CPMK";
                    %>
                        <tr class="border-bottom" style="<%=rowBgColor%>">
                            
                            <% if (isFirstCplRow) { %>
                                <td rowspan="<%=cplRowCount%>" class="align-top border-end pt-3 text-center">
                                    <span class="fw-bold text-success fs-6 d-block mb-1"><%=cpl.getKode()%></span>
                                    <div class="small text-muted text-start text-wrap px-2" style="max-height:100px; overflow-y:auto; line-height: 1.2; text-align: justify !important;"><%=cpl.getNama()%></div>
                                </td>
                            <% } %>
                            
                            <% if (isFirstMkRow) { %>
                                <td rowspan="<%=mkRowCount%>" class="align-middle border-end">
                                    <% if(mk != null) { %>
                                        <div class="fw-bold text-dark"><%=mk.getKode()%> - <%=mk.getNama()%></div>
                                        <div class="small text-muted"><span class="badge bg-secondary opacity-75"><%=baseLabel%></span> | Smt <%=kpm.getSemester()%> | <%=mk.getSks()%> SKS</div>
                                    <% } else { %>
                                        <span class="text-muted fst-italic"><%=Common.getBahasaConfig("Belum ditautkan ke MK manapun")%></span>
                                    <% } %>
                                </td>
                            <% } %>

                            <% if (!isCpmkBase && isFirstIndukRow) { %>
                                <td rowspan="<%=indukRowCount%>" class="align-middle text-center text-muted fw-bold border-end">
                                    <%=rowData.get("induk")%>
                                </td>
                            <% } else if (isCpmkBase) { %>
                                <td class="align-middle text-center text-muted border-end">-</td>
                            <% } %>

                            <td class="align-middle text-center fw-bold text-dark"><%=rowData.get("kode")%></td>

                            <td class="align-middle text-center">
                                <% if(isDuplicate) { %>
                                    <span class="text-muted small" title="<%=Common.getBahasaConfig("Bobot sudah dijumlahkan pada baris sebelumnya.")%>">-</span>
                                <% } else { %>
                                    <span class="fw-bold"><%=rowData.get("bobot")%></span>
                                <% } %>
                            </td>

                            <% if (isFirstMkRow) { %>
                                <td rowspan="<%=mkRowCount%>" class="align-middle text-center fw-bold text-primary border-start border-end">
                                    <%=rowData.get("totalBobotMkLocal")%>
                                </td>
                            <% } %>

                            <% if (isFirstCplRow) { %>
                                <td rowspan="<%=cplRowCount%>" class="align-middle text-center fw-bold text-danger fs-6 border-start">
                                    <%=rowData.get("totalBobotCplGlobal")%>
                                </td>
                            <% } %>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    <% } %>

<%
}
%>