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
try { if(idJurusanStr != null && !idJurusanStr.trim().isEmpty()) idJurusan = Long.parseLong(idJurusanStr); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_11.jsp:30");}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

if ("loadTabel11".equals(action)) {
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
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_11.jsp:67"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
%>

    <% if(kurikulumAktif == null) { %>
        <div class="alert alert-warning text-center"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Kurikulum OBE tidak ditemukan.")%></div>
    <% } else if(listKpm.isEmpty()) { %>
        <div class="alert alert-warning text-center"><i class="fas fa-folder-open me-2"></i><%=Common.getBahasaConfig("Belum ada matakuliah dalam kurikulum ini.")%></div>
    <% } else { %>
        <div class="table-responsive border rounded-3 animate__animated animate__fadeIn" style="max-height: 700px; overflow-y: auto;">
            <table class="table table-bordered align-middle mb-0 text-start small">
                <thead class="table-light sticky-top shadow-sm" style="z-index: 10;">
                    <tr class="text-center">
                        <th style="min-width: 250px;"><%=Common.getBahasaConfig("Matakuliah / Kurikulum")%></th>
                        <th style="width: 120px;"><%=Common.getBahasaConfig("CPL")%></th>
                        <th style="min-width: 150px;"><%=Common.getBahasaConfig("CPMK / Sub-CPMK")%></th>
                        <th style="width: 120px;"><%=Common.getBahasaConfig("Skor Maks (Bobot)")%></th>
                        <th style="width: 100px;"><%=Common.getBahasaConfig("Total")%></th>
                    </tr>
                </thead>
                <tbody>
                    <% 
                    int mkCounter = 0; 
                    for(KurikulumPunyaMatakuliah kpm : listKpm) { 
                        mkCounter++;
                        String rowBgColor = (mkCounter % 2 == 0) ? "background-color: #fcfcfc;" : "background-color: #ffffff;";
                        
                        Matakuliah mk = kpm.getMatakuliah();
                        boolean isCpmkBase = kpm.getNilaiMenggunakanCpmk() != null ? kpm.getNilaiMenggunakanCpmk() : false;
                        
                        String mkCplStr = mk.getCapaianLulusan() != null ? mk.getCapaianLulusan() : "";
                        String mkCpmkStr = mk.getCapaianPembelajaranLulusan() != null ? mk.getCapaianPembelajaranLulusan() : "";
                        
                        List<CapaianLulusan> mappedCpl = new ArrayList<CapaianLulusan>();
                        for(CapaianLulusan cpl : allCpl) {
                            if(mkCplStr.contains("," + cpl.getId() + ",")) mappedCpl.add(cpl);
                        }

                        double totalBobotMk = 0.0;
                        int totalRows = 0; 
                        List<Map<String, Object>> renderRows = new ArrayList<Map<String, Object>>();
                        
                        // Set untuk mencegah duplikasi penjumlahan bobot di satu MK
                        Set<String> processedItemsForMk = new HashSet<String>();

                        for(CapaianLulusan cpl : mappedCpl) {
                            String cplCpmkStr = cpl.getCapaianPembelajaranLulusan() != null ? cpl.getCapaianPembelajaranLulusan() : "";
                            
                            List<CapaianPembelajaranLulusan> mappedCpmkForThisCpl = new ArrayList<CapaianPembelajaranLulusan>();
                            for(CapaianPembelajaranLulusan cpmk : allCpmk) {
                                if(cplCpmkStr.contains("," + cpmk.getId() + ",")) {
                                    mappedCpmkForThisCpl.add(cpmk);
                                }
                            }

                            if(!mappedCpmkForThisCpl.isEmpty()) {
                                for(CapaianPembelajaranLulusan cpmk : mappedCpmkForThisCpl) {
                                    boolean mkHasThisCpmk = mkCpmkStr.contains("," + cpmk.getId() + ",");
                                    
                                    if(isCpmkBase) {
                                        // BASIS: CPMK
                                        double b = 0.0;
                                        boolean isDuplicate = false;
                                        
                                        if (mkHasThisCpmk) {
                                            String uniqueId = cpmk.getId().toString();
                                            b = cpmk.getBobot() != null ? cpmk.getBobot() : 0.0;
                                            if (processedItemsForMk.contains(uniqueId)) {
                                                isDuplicate = true;
                                            } else {
                                                totalBobotMk += b;
                                                processedItemsForMk.add(uniqueId);
                                            }
                                        }
                                        
                                        if(mkHasThisCpmk) {
                                            Map<String, Object> r = new HashMap<String, Object>();
                                            r.put("cplKode", cpl.getKode()); 
                                            r.put("cpmkKode", cpmk.getKode()); 
                                            r.put("bobot", b); 
                                            r.put("isDuplicate", isDuplicate);
                                            renderRows.add(r);
                                            totalRows++;
                                        }
                                    } else {
                                        // BASIS: SUB-CPMK
                                        String formulaJson = cpmk.getFormula();
                                        try {
                                            JSONArray arr = new JSONArray(formulaJson);
                                            for(int i=0; i<arr.length(); i++) {
                                                JSONObject obj = arr.getJSONObject(i);
                                                if(obj.isNull("key")) continue;
                                                
                                                double b = 0.0;
                                                boolean isDuplicate = false;
                                                String keyId = obj.getString("key") + "_" + cpmk.getId().toString();
                                                
                                                if (mkHasThisCpmk) {
                                                    b = !obj.isNull("bobot") ? Double.parseDouble(obj.getString("bobot")) : 0.0;
                                                    if (processedItemsForMk.contains(keyId)) {
                                                        isDuplicate = true;
                                                    } else {
                                                        totalBobotMk += b;
                                                        processedItemsForMk.add(keyId);
                                                    }
                                                }
                                                
                                                String kodeS = !obj.isNull("kode") ? obj.getString("kode") : "";
                                                
                                                if(mkHasThisCpmk) {
                                                    Map<String, Object> r = new HashMap<String, Object>();
                                                    r.put("cplKode", cpl.getKode()); 
                                                    r.put("cpmkKode", kodeS); 
                                                    r.put("bobot", b); 
                                                    r.put("isDuplicate", isDuplicate);
                                                    renderRows.add(r);
                                                    totalRows++;
                                                }
                                            }
                                        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_11.jsp:185");}
                                    }
                                }
                            }
                        }

                        if(renderRows.isEmpty()) {
                            totalRows = 1;
                            Map<String, Object> r = new HashMap<String, Object>();
                            r.put("cplKode", "-"); r.put("cpmkKode", "-"); r.put("bobot", 0.0); r.put("isDuplicate", false);
                            renderRows.add(r);
                        }

                        totalBobotMk = Math.round(totalBobotMk * 100.0) / 100.0;
                        String totalClass = totalBobotMk == 100.0 ? "text-success" : "text-danger fw-bold";
                        String baseLabel = isCpmkBase ? "CPMK" : "Sub-CPMK";
                    %>
                        
                        <% 
                        boolean firstMk = true;
                        for(int r = 0; r < renderRows.size(); r++) { 
                            Map<String, Object> rowData = renderRows.get(r);
                            boolean isDuplicate = (Boolean) rowData.get("isDuplicate");
                        %>
                            <tr class="border-bottom" style="<%=rowBgColor%>">
                                <% if(firstMk) { %>
                                    <td rowspan="<%=totalRows%>" class="align-middle border-end">
                                        <div class="fw-bold text-dark"><%=mk.getKode()%> - <%=mk.getNama()%></div>
                                        <div class="small text-muted"><span class="badge bg-secondary opacity-75"><%=baseLabel%></span> | Smt <%=kpm.getSemester()%> | <%=mk.getSks()%> SKS</div>
                                    </td>
                                <% } %>
                                
                                <td class="text-center fw-bold text-primary align-middle"><%=rowData.get("cplKode")%></td>
                                <td class="fw-bold text-dark align-middle"><%=rowData.get("cpmkKode")%></td>
                                
                                <td class="text-center align-middle">
                                    <% if(isDuplicate) { %>
                                        <span class="text-muted small" title="<%=Common.getBahasaConfig("Bobot sudah dijumlahkan pada baris sebelumnya.")%>">-</span>
                                    <% } else { %>
                                        <span class="fw-bold"><%=rowData.get("bobot")%></span>
                                    <% } %>
                                </td>
                                
                                <% if(firstMk) { %>
                                    <td rowspan="<%=totalRows%>" class="text-center align-middle border-start">
                                        <span class="<%=totalClass%> fs-6"><%=totalBobotMk%></span>
                                    </td>
                                <% firstMk = false; } %>
                            </tr>
                        <% } %>
                    <% } %>
                </tbody>
            </table>
        </div>
    <% } %>

<%
}
%>