<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
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

boolean edit = false;
if(tbmuser != null && tbmuser.getUserId() != null){
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    if(Common.getApakahAdminLain(tbmuser)){ edit = true; }
}

Long idJurusan = null;
try { if(idJurusanStr != null && !idJurusanStr.trim().isEmpty()) idJurusan = Long.parseLong(idJurusanStr); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_10.jsp:37");}

// ==========================================
// ACTION 1: SERVER-SIDE UPDATE (NILAI CPMK/SUB-CPMK)
// ==========================================
if ("updateNilai".equals(action)) {
    response.setContentType("application/json");
    Session sessUpd = null;
    try {
        String idCpmkStr = request.getParameter("idCpmk");
        boolean isCpmkBase = Boolean.parseBoolean(request.getParameter("isCpmkBase"));
        String jsonKey = request.getParameter("jsonKey");
        String field = request.getParameter("field");
        double val = Double.parseDouble(request.getParameter("value"));

        sessUpd = HibernateUtil.openSession();
        CapaianPembelajaranLulusan cpl = (CapaianPembelajaranLulusan) sessUpd.get(CapaianPembelajaranLulusan.class, Long.parseLong(idCpmkStr));
        if (cpl == null) throw new Exception("Data CPMK tidak ditemukan: " + idCpmkStr);

        if (isCpmkBase) {
            if ("bobot".equals(field)) { cpl.setBobot(val); } else if ("minimal".equals(field)) { cpl.setMinimal(val); }
            Common.refreshUpdate(cpl);
        } else {
            JSONArray array = new JSONArray(cpl.getFormula());
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                if (!jsonObject.isNull("key") && jsonObject.getString("key").equals(jsonKey)) {
                    jsonObject.put(field, val);
                    break;
                }
            }
            cpl.setFormula(array.toString());
            Common.refreshUpdate(cpl);
        }
        out.print("{\"status\":\"success\"}");
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_10.jsp:73");
        out.print("{\"status\":\"error\", \"message\":\"" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()) + "\"}");
    } finally {
        ais.common.ElearningSessionUtil.closeQuietly(sessUpd);
        HibernateUtil.closeSessionQuietly(sessUpd);
    }
    return;
}

if(idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION 2: LOAD MATRIKS TABEL 10
// ==========================================
if ("loadTabel10".equals(action)) {
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

    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_10.jsp:121"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
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
                        <th style="min-width: 100px;"><%=Common.getBahasaConfig("CPL")%></th>
                        <th style="min-width: 100px;"><%=Common.getBahasaConfig("Induk CPMK")%></th>
                        <th style="min-width: 120px;"><%=Common.getBahasaConfig("Kode CPMK/Sub")%></th>
                        <th style="min-width: 200px;"><%=Common.getBahasaConfig("Deskripsi CPMK / Sub-CPMK")%></th>
                        <th style="width: 100px;"><%=Common.getBahasaConfig("Bobot (%)")%></th>
                        <th style="width: 100px;"><%=Common.getBahasaConfig("Minimal")%></th>
                    </tr>
                </thead>
                <tbody id="tbodyTabel10_<%=rnd%>">
                    <% 
                    int mkCounter = 0; 
                    for(KurikulumPunyaMatakuliah kpm : listKpm) { 
                        mkCounter++;
                        String rowBgColor = (mkCounter % 2 == 0) ? "background-color: #f7faff;" : "background-color: #ffffff;";
                        
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
                        String searchCorpus = (mk.getKode() + " " + mk.getNama()).toLowerCase();
                        
                        // SET UNTUK MENCEGAH DUPLIKASI INPUT BOBOT DI SATU MK
                        Set<String> processedItemsForMk = new HashSet<String>();

                        for(CapaianLulusan cpl : mappedCpl) {
                            String cplCpmkStr = cpl.getCapaianPembelajaranLulusan() != null ? cpl.getCapaianPembelajaranLulusan() : "";
                            
                            List<CapaianPembelajaranLulusan> mappedCpmkForThisCpl = new ArrayList<CapaianPembelajaranLulusan>();
                            for(CapaianPembelajaranLulusan cpmk : allCpmk) {
                                if(cplCpmkStr.contains("," + cpmk.getId() + ",")) {
                                    mappedCpmkForThisCpl.add(cpmk);
                                }
                            }

                            List<Map<String, Object>> tempCplRows = new ArrayList<Map<String, Object>>();

                            if(mappedCpmkForThisCpl.isEmpty()) {
                                Map<String, Object> r = new HashMap<String, Object>();
                                r.put("cpl", cpl); r.put("isCpmk", true); r.put("kode", "-"); r.put("nama", "-"); r.put("bobot", 0.0); r.put("minimal", ""); r.put("idData", ""); r.put("idRef", ""); r.put("isValid", false); r.put("indukCpmk", "-"); r.put("isFirstIndukRow", true); r.put("indukRowCount", 1); r.put("isDuplicate", false);
                                tempCplRows.add(r);
                                totalRows++;
                            } else {
                                for(CapaianPembelajaranLulusan cpmk : mappedCpmkForThisCpl) {
                                    boolean mkHasThisCpmk = mkCpmkStr.contains("," + cpmk.getId() + ",");
                                    
                                    if(isCpmkBase) {
                                        // BASIS: CPMK
                                        double b = 0.0;
                                        double m = 0.0;
                                        boolean isDuplicate = false;
                                        
                                        if (mkHasThisCpmk) {
                                            String uniqueId = cpmk.getId().toString();
                                            b = cpmk.getBobot() != null ? cpmk.getBobot() : 0.0;
                                            m = cpmk.getMinimal() != null ? cpmk.getMinimal() : 0.0;
                                            
                                            // Cek Duplikasi
                                            if (processedItemsForMk.contains(uniqueId)) {
                                                isDuplicate = true;
                                            } else {
                                                totalBobotMk += b;
                                                processedItemsForMk.add(uniqueId);
                                            }
                                        }
                                        
                                        Map<String, Object> r = new HashMap<String, Object>();
                                        r.put("cpl", cpl); r.put("isCpmk", true); r.put("kode", cpmk.getKode()); r.put("nama", cpmk.getNama()); r.put("bobot", b); r.put("minimal", m == 0.0 ? "" : String.valueOf(m)); r.put("idData", cpmk.getId()); r.put("idRef", ""); r.put("isValid", mkHasThisCpmk); r.put("indukCpmk", "-"); r.put("isFirstIndukRow", true); r.put("indukRowCount", 1); r.put("isDuplicate", isDuplicate);
                                        tempCplRows.add(r);
                                        totalRows++;
                                        searchCorpus += (" " + cpl.getKode() + " " + cpl.getNama() + " " + cpmk.getKode() + " " + cpmk.getNama()).toLowerCase();
                                    } else {
                                        // BASIS: SUB-CPMK
                                        String formulaJson = cpmk.getFormula();
                                        boolean hasSub = false;
                                        List<Map<String, Object>> tempIndukRows = new ArrayList<Map<String, Object>>();
                                        
                                        try {
                                            JSONArray arr = new JSONArray(formulaJson);
                                            for(int i=0; i<arr.length(); i++) {
                                                JSONObject obj = arr.getJSONObject(i);
                                                if(obj.isNull("key")) continue;
                                                
                                                double b = 0.0;
                                                double m = 0.0;
                                                boolean isDuplicate = false;
                                                
                                                // MENCEGAH DUPLIKASI KEY DI SUB-CPMK DARI CPMK YANG BERBEDA
                                                String rawKey = obj.getString("key");
                                                String uniqueId = cpmk.getId().toString();
                                                String keyId = rawKey + "_" + uniqueId;
                                                
                                                if (mkHasThisCpmk) {
                                                    b = !obj.isNull("bobot") ? Double.parseDouble(obj.getString("bobot")) : 0.0;
                                                    m = !obj.isNull("minimal") ? Double.parseDouble(obj.getString("minimal")) : 0.0;
                                                    
                                                    // Cek Duplikasi menggunakan ID Kombinasi
                                                    if (processedItemsForMk.contains(keyId)) {
                                                        isDuplicate = true;
                                                    } else {
                                                        totalBobotMk += b;
                                                        processedItemsForMk.add(keyId);
                                                    }
                                                }
                                                
                                                String namaS = !obj.isNull("nama") ? obj.getString("nama") : "";
                                                String kodeS = !obj.isNull("kode") ? obj.getString("kode") : "";

                                                Map<String, Object> r = new HashMap<String, Object>();
                                                // idRef dikirim menggunakan rawKey agar AJAX Backend dapat memproses update JSON
                                                r.put("cpl", cpl); r.put("isCpmk", false); r.put("kode", kodeS); r.put("nama", namaS); r.put("bobot", b); r.put("minimal", m == 0.0 ? "" : String.valueOf(m)); r.put("idData", cpmk.getId()); r.put("idRef", rawKey); r.put("isValid", mkHasThisCpmk); r.put("indukCpmk", cpmk.getKode()); r.put("isDuplicate", isDuplicate);
                                                tempIndukRows.add(r);
                                                totalRows++;
                                                hasSub = true;
                                                searchCorpus += (" " + cpl.getKode() + " " + cpl.getNama() + " " + kodeS + " " + namaS).toLowerCase();
                                            }
                                        } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_10.jsp:259");}
                                        
                                        if(!hasSub) {
                                            Map<String, Object> r = new HashMap<String, Object>();
                                            r.put("cpl", cpl); r.put("isCpmk", false); r.put("kode", "-"); r.put("nama", Common.getBahasaConfig("Belum ada Sub-CPMK")); r.put("bobot", 0.0); r.put("minimal", ""); r.put("idData", ""); r.put("idRef", ""); r.put("isValid", false); r.put("indukCpmk", cpmk.getKode()); r.put("isFirstIndukRow", true); r.put("indukRowCount", 1); r.put("isDuplicate", false);
                                            tempCplRows.add(r);
                                            totalRows++;
                                        } else {
                                            int countInduk = tempIndukRows.size();
                                            for(int j=0; j<countInduk; j++) {
                                                Map<String, Object> rowI = tempIndukRows.get(j);
                                                rowI.put("isFirstIndukRow", j == 0);
                                                rowI.put("indukRowCount", countInduk);
                                                tempCplRows.add(rowI);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            int countRowCpl = tempCplRows.size();
                            for(int i = 0; i < countRowCpl; i++) {
                                Map<String, Object> rowCpl = tempCplRows.get(i);
                                rowCpl.put("isFirstCplRow", i == 0);
                                rowCpl.put("cplRowCount", countRowCpl);
                                renderRows.add(rowCpl);
                            }
                        }

                        if(renderRows.isEmpty()) {
                            totalRows = 1;
                            Map<String, Object> r = new HashMap<String, Object>();
                            r.put("cpl", null); r.put("isCpmk", true); r.put("kode", "-"); r.put("nama", "-"); r.put("bobot", 0.0); r.put("minimal", ""); r.put("idData", ""); r.put("idRef", ""); r.put("isValid", false);
                            r.put("isFirstCplRow", true); r.put("cplRowCount", 1); r.put("indukCpmk", "-"); r.put("isFirstIndukRow", true); r.put("indukRowCount", 1); r.put("isDuplicate", false);
                            renderRows.add(r);
                        }

                        totalBobotMk = Math.round(totalBobotMk * 100.0) / 100.0;
                        String bobotClass = totalBobotMk == 100.0 ? "text-success" : "text-danger fw-bold";
                        String bobotAlert = totalBobotMk == 100.0 ? "" : "<br><small class='text-danger fw-bold' style='font-size:11px;'><i class='fas fa-exclamation-triangle'></i> Harus 100%</small>";
                        String baseLabel = isCpmkBase ? "<span class='badge bg-success mt-1' style='font-size:10px;'>Basis: CPMK</span>" : "<span class='badge bg-info text-dark mt-1' style='font-size:10px;'>Basis: Sub-CPMK</span>";
                        
                        boolean showRow = true;
                        if(keyword != null && !keyword.isEmpty() && !searchCorpus.contains(keyword.toLowerCase())) showRow = false;
                        
                        if(showRow) {
                            boolean firstMk = true;
                            for(int r = 0; r < renderRows.size(); r++) { 
                                Map<String, Object> rowData = renderRows.get(r);
                                CapaianLulusan cpl = (CapaianLulusan) rowData.get("cpl");
                                boolean isCpmkData = (Boolean) rowData.get("isCpmk");
                                boolean isValidData = (Boolean) rowData.get("isValid");
                                boolean isDuplicateData = (Boolean) rowData.get("isDuplicate");
                                boolean isFirstCplRow = (Boolean) rowData.get("isFirstCplRow");
                                int cplRowCount = (Integer) rowData.get("cplRowCount");
                                boolean isFirstIndukRow = (Boolean) rowData.get("isFirstIndukRow");
                                int indukRowCount = (Integer) rowData.get("indukRowCount");
                                String indukCpmk = (String) rowData.get("indukCpmk");
                                
                                String dataId = rowData.get("idData").toString(); 
                                String dataRefId = rowData.get("idRef").toString(); 
                                
                                String rowStyle = rowBgColor + (isValidData ? "" : " opacity: 0.5;");
                            %>
                                <tr class="item-baris-10 border-bottom" data-mk="<%=mk.getId()%>" style="<%=rowStyle%>">
                                    <% if(firstMk) { %>
                                        <td rowspan="<%=totalRows%>" class="align-top border-end" style="<%=rowBgColor%> opacity: 1 !important;">
                                            <div class="fw-bold text-dark" style="font-size: 14px;"><%=mk.getKode()%> - <%=mk.getNama()%></div>
                                            <div class="small text-muted mb-2"><%=Common.getBahasaConfig("Smt")%> <%=kpm.getSemester()%> | <%=mk.getSks()%> <%=Common.getBahasaConfig("SKS")%></div>
                                            <div id="wdhBobotMk_<%=mk.getId()%>" class="mb-1">
                                                <span class="<%=bobotClass%>" style="font-size: 13px;">Total Bobot: <%=Common.numberFormat.get().format(totalBobotMk)%>%</span> <%=bobotAlert%>
                                            </div>
                                            <%=baseLabel%>
                                            
                                            <% if(edit) { %>
                                                <div class="mt-4 border-top pt-2">
                                                    <button class="btn btn-outline-primary btn-sm shadow-sm rounded-pill w-100" style="font-size: 11px;" onclick="window.bukaModalKelolaCPL10<%=rnd%>('<%=mk.getId()%>', '<%=mk.getKode()%>')">
                                                        <i class="fas fa-link me-1"></i> <%=Common.getBahasaConfig("Kelola CPL")%>
                                                    </button>
                                                </div>
                                            <% } %>
                                        </td>
                                    <% firstMk = false; } %>
                                    
                                    <% if(isFirstCplRow) { %>
                                        <td rowspan="<%=cplRowCount%>" class="text-center align-top pt-3 border-end" style="<%=rowBgColor%> opacity: 1 !important;">
                                            <%= cpl != null ? "<span class='fw-bold text-success' title='"+cpl.getNama()+"'>" + cpl.getKode() + "</span>" : "<span class='text-muted'>-</span>" %>
                                            <% if(cpl != null && edit) { %>
                                                <div class="mt-2">
                                                    <button class="btn btn-outline-success btn-sm shadow-sm rounded-pill" style="font-size: 10px; padding: 2px 8px;" onclick="window.bukaModalKelolaCPMK9<%=rnd%>('<%=cpl.getId()%>', '<%=cpl.getKode()%>', '<%=mk.getId()%>')">
                                                        <i class="fas fa-cog me-1"></i> CPMK
                                                    </button>
                                                </div>
                                            <% } %>
                                        </td>
                                    <% } %>
                                    
                                    <% if(!isCpmkBase) { %>
                                        <% if(isFirstIndukRow) { %>
                                            <td rowspan="<%=indukRowCount%>" class="text-center align-middle border-end text-muted fw-bold">
                                                <%=indukCpmk%>
                                            </td>
                                        <% } %>
                                    <% } else { %>
                                        <td class="text-center align-middle border-end text-muted">-</td>
                                    <% } %>
                                    
                                    <td class="text-center fw-bold align-middle"><%=rowData.get("kode")%></td>
                                    <td class="align-middle"><%=rowData.get("nama")%></td>
                                    
                                    <td class="p-2 text-center align-middle">
                                        <% if(isDuplicateData && isValidData) { %>
                                            <div class="text-muted fst-italic mt-1" style="font-size: 10px; line-height: 1.2;">
                                                <i class="fas fa-level-up-alt d-block mb-1"></i>
                                                <%=Common.getBahasaConfig("Sdh terhitung")%><br><%=Common.getBahasaConfig("di atas")%> (<%=rowData.get("bobot")%>%)
                                            </div>
                                        <% } else if(edit) { %>
                                            <input type="number" step="0.1" class="form-control form-control-sm text-center border-secondary fw-bold" style="width: 80px; margin: 0 auto; transition: background-color 0.3s ease;" 
                                                   value="<%=rowData.get("bobot")%>" 
                                                   <%=isValidData ? "" : "disabled title='" + Common.getBahasaConfig("MK ini belum ditautkan ke CPMK terkait.") + "'"%>
                                                   oninput="window.kalkulasiBobot10<%=rnd%>('<%=mk.getId()%>')"
                                                   onchange="window.updateNilai10<%=rnd%>(this, 'bobot', '<%=dataId%>', '<%=dataRefId%>', <%=isCpmkBase%>, '<%=mk.getId()%>')">
                                        <% } else { %>
                                            <span class="fw-bold"><%=rowData.get("bobot")%>%</span>
                                        <% } %>
                                    </td>
                                    
                                    <td class="p-2 text-center align-middle">
                                        <% if(isDuplicateData && isValidData) { %>
                                            <span class="text-muted small">-</span>
                                        <% } else if(edit) { %>
                                            <input type="number" step="0.1" class="form-control form-control-sm text-center border-secondary" style="width: 80px; margin: 0 auto; transition: background-color 0.3s ease;" 
                                                   value="<%=rowData.get("minimal")%>" placeholder="-"
                                                   <%=isValidData ? "" : "disabled title='" + Common.getBahasaConfig("MK ini belum ditautkan ke CPMK terkait.") + "'"%>
                                                   onchange="window.updateNilai10<%=rnd%>(this, 'minimal', '<%=dataId%>', '<%=dataRefId%>', <%=isCpmkBase%>, '<%=mk.getId()%>')">
                                        <% } else { %>
                                            <span><%=rowData.get("minimal")%></span>
                                        <% } %>
                                    </td>
                                </tr>
                            <% } %>
                        <% } %>
                    <% } %>
                </tbody>
            </table>
        </div>
        <div class="mt-3 text-muted small">
            <i class="fas fa-info-circle me-1 text-primary"></i> <%=Common.getBahasaConfig("Pastikan Total Bobot per Matakuliah adalah 100%. Untuk menambah CPL pada Matakuliah, gunakan tombol [Kelola CPL].")%>
        </div>
    <% } %>

<%
}
// ==========================================
// ACTION 3: LOAD MODAL TAUTKAN CPL KE MK
// ==========================================
else if ("loadModalKelolaCpl".equals(action)) {
    String idMk = request.getParameter("idMk");
    String kodeMk = request.getParameter("kodeMk");
    
    Matakuliah mk = new Matakuliah();
    List<CapaianLulusan> allCPL = new ArrayList<CapaianLulusan>();
    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        mk = (Matakuliah) sess.get(Matakuliah.class, Long.parseLong(idMk));
        allCPL = ConstantValues.simpleList(sess.createCriteria(CapaianLulusan.class).add(Restrictions.eq("jurusan.id", idJurusan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("kode")), CapaianLulusan.class);
    } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_10.jsp:426"); } finally { if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); } }
    
    String cplMapped = mk.getCapaianLulusan() != null ? mk.getCapaianLulusan() : "";
%>
    <div class="modal fade" id="modalKelolaCpl_10_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content shadow-lg border-0 rounded-4">
                <div class="modal-header bg-primary text-white border-bottom-0 pb-3">
                    <h6 class="modal-title fw-bold"><i class="fas fa-link me-2"></i><%=Common.getBahasaConfig("Tautkan CPL ke")%> <%=kodeMk%></h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4 bg-light">
                    <div class="p-3 border rounded bg-white shadow-sm" style="max-height: 400px; overflow-y: auto;">
                        <% if(allCPL.isEmpty()) { %>
                            <span class="text-muted small"><%=Common.getBahasaConfig("Tidak ada data master CPL.")%></span>
                        <% } else { 
                            for(CapaianLulusan cpl : allCPL) {
                                boolean isChecked = cplMapped.contains("," + cpl.getId() + ",");
                        %>
                            <div class="form-check mb-2">
                                <input class="form-check-input chk-cpl-10 border-primary" type="checkbox" value="<%=cpl.getId()%>" id="chkCpl_10_<%=cpl.getId()%>" <%=isChecked ? "checked" : ""%>>
                                <label class="form-check-label small" for="chkCpl_10_<%=cpl.getId()%>"><strong><%=cpl.getKode()%></strong> - <%=cpl.getNama()%></label>
                            </div>
                        <% } } %>
                    </div>
                </div>
                <div class="modal-footer bg-white border-top p-3 justify-content-end">
                    <button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                    <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanKorelasiMK_CPL10<%=rnd%>('<%=idMk%>')"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Tautan")%></button>
                </div>
            </div>
        </div>
    </div>
    <script>
        var modalKelolaCpl10 = new bootstrap.Modal(document.getElementById('modalKelolaCpl_10_<%=rnd%>')); modalKelolaCpl10.show();
        document.getElementById('modalKelolaCpl_10_<%=rnd%>').addEventListener('hidden.bs.modal', function () { this.remove(); });
    </script>
<%
}
%>