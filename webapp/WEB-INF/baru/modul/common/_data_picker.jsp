<%@page import="java.util.*"%>
<%@page import="java.net.URLDecoder"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.apache.commons.beanutils.PropertyUtils"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>

<%
    // --- 1. PENGAMBILAN PARAMETER DASAR ---
    String className = request.getParameter("class");
    String colsStr = request.getParameter("cols") != null ? URLDecoder.decode(request.getParameter("cols"), "UTF-8") : "id,nama";
    String callback = request.getParameter("callback");
    
    // --- PENGAMAN VARIABEL RND ---
    String rnd = request.getParameter("rnd");
    if (rnd == null || rnd.trim().isEmpty() || rnd.equals("null")) {
        rnd = "";
    }
    
    String radioParam = request.getParameter("radio");
    boolean isRadio = (radioParam != null && radioParam.equalsIgnoreCase("true"));
    
    // --- 2. PARAMETER PAGING & PENCARIAN ---
    String keyword = request.getParameter("keyword") != null ? request.getParameter("keyword").trim() : "";
    int pageNum = request.getParameter("page") != null ? Integer.parseInt(request.getParameter("page")) : 1;
    int pageSize = 10;
    int firstResult = (pageNum - 1) * pageSize;

    // --- 3. PARAMETER DATA TERPILIH ---
    String selectedIdsParam = request.getParameter("selectedIds");
    List<Long> selectedIds = new ArrayList<Long>();
    if (selectedIdsParam != null && !selectedIdsParam.trim().isEmpty()) {
        for (String idStr : selectedIdsParam.split(",")) {
            try { selectedIds.add(Long.parseLong(idStr.trim()));
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_data_picker.jsp:48");}
        }
    }

    // --- 4. LOGIKA PENGURUTAN (MULTI-ORDER) ---
    List<String[]> sortOrders = new ArrayList<String[]>();
    String orderStr = request.getParameter("order") != null ? URLDecoder.decode(request.getParameter("order"), "UTF-8") : "";
    if (orderStr != null && !orderStr.trim().isEmpty()) {
        String[] parts = orderStr.split(",");
        for (String part : parts) {
            String[] subPart = part.trim().split("\\s+");
            if (subPart.length > 0 && !subPart[0].isEmpty()) {
                sortOrders.add(new String[]{subPart[0], (subPart.length > 1) ? subPart[1].toLowerCase() : "asc"});
            }
        }
    }

    // --- 5. REFLEKSI & PARAMETER PENGUNCI FILTER ---
    Class<?> clazz = Class.forName(className);
    boolean hasYayasan = false; boolean hasSekolah = false;
    boolean hasFakultas = false; boolean hasJurusan = false;
    
    Class<?> currentClass = clazz;
    while (currentClass != null && currentClass != Object.class) {
        try { currentClass.getDeclaredField("yayasan"); hasYayasan = true; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_data_picker.jsp:72");}
        try { currentClass.getDeclaredField("sekolah"); hasSekolah = true; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_data_picker.jsp:73");}
        try { currentClass.getDeclaredField("fakultas"); hasFakultas = true; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_data_picker.jsp:74");}
        try { currentClass.getDeclaredField("jurusan"); hasJurusan = true; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_data_picker.jsp:75");}
        currentClass = currentClass.getSuperclass();
    }

    String selectedYayasan = request.getParameter("yayasan");
    String selectedSekolah = request.getParameter("sekolah");
    String selectedFakultas = request.getParameter("fakultas");
    String selectedJurusan = request.getParameter("jurusan");
    
    String filterYayasan = (selectedYayasan != null && !selectedYayasan.isEmpty()) ? selectedYayasan : request.getParameter("filterYayasan");
    String filterSekolah = (selectedSekolah != null && !selectedSekolah.isEmpty()) ? selectedSekolah : request.getParameter("filterSekolah");
    String filterFakultas = (selectedFakultas != null && !selectedFakultas.isEmpty()) ? selectedFakultas : request.getParameter("filterFakultas");
    String filterJurusan = (selectedJurusan != null && !selectedJurusan.isEmpty()) ? selectedJurusan : request.getParameter("filterJurusan");

    Tbmuser tbmuser = Common.getCurrentUser(request);
    List<Sekolah> listSekolahRyg = new ArrayList<Sekolah>();
    List<Yayasan> listYayasanRyg = new ArrayList<Yayasan>();
    List<Fakultas> listFakultasRyg = new ArrayList<Fakultas>();
    List<Jurusan> listJurusanRyg = new ArrayList<Jurusan>();
    
    Session sessFilterRyg = null;
    try {
        sessFilterRyg = HibernateUtil.openSession();
        if (hasSekolah) {
            org.hibernate.Criteria cSek = sessFilterRyg.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
            if (selectedSekolah != null && !selectedSekolah.isEmpty()) {
                cSek.add(Restrictions.eq("id", Long.parseLong(selectedSekolah)));
            } else if(tbmuser != null && tbmuser.ambilSekolah() != null) {
                cSek.add(Restrictions.eq("id", tbmuser.ambilSekolah().getId()));
            } else if(tbmuser != null && tbmuser.ambilYayasan() != null) {
                cSek.add(Restrictions.eq("yayasan.id", tbmuser.ambilYayasan().getId()));
            }
            listSekolahRyg = ConstantValues.simpleList(cSek, Sekolah.class);
        }
        if (hasYayasan) {
            org.hibernate.Criteria cYay = sessFilterRyg.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
            if (selectedYayasan != null && !selectedYayasan.isEmpty()) {
                cYay.add(Restrictions.eq("id", Long.parseLong(selectedYayasan)));
            } else if(tbmuser != null && tbmuser.ambilYayasan() != null) {
                cYay.add(Restrictions.eq("id", tbmuser.ambilYayasan().getId()));
            }
            listYayasanRyg = ConstantValues.simpleList(cYay, Yayasan.class);
        }
        if (hasFakultas) {
            org.hibernate.Criteria cFak = sessFilterRyg.createCriteria(Fakultas.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
            if (selectedFakultas != null && !selectedFakultas.isEmpty()) {
                cFak.add(Restrictions.eq("id", Long.parseLong(selectedFakultas)));
            }
            listFakultasRyg = ConstantValues.simpleList(cFak, Fakultas.class);
        }
        if (hasJurusan) {
            org.hibernate.Criteria cJur = sessFilterRyg.createCriteria(Jurusan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
            if (selectedJurusan != null && !selectedJurusan.isEmpty()) {
                cJur.add(Restrictions.eq("id", Long.parseLong(selectedJurusan)));
            }
            listJurusanRyg = ConstantValues.simpleList(cJur, Jurusan.class);
        }
    } finally {
        if(sessFilterRyg != null && sessFilterRyg.isOpen()) sessFilterRyg.close();
    }

    String[] cols = colsStr.split(",");
    
    // safeListData untuk dirender ke HTML Tabel
    List<Map<String, Object>> safeListData = new ArrayList<Map<String, Object>>();
    
    // PERBAIKAN BUG UTAMA: Merakit string JSON secara manual menggunakan StringBuilder
    // Ini menggaransi syntax yang valid tanpa bergantung pada library org.json
    StringBuilder jsonArrayDataStr = new StringBuilder();
    jsonArrayDataStr.append("[");
    boolean isFirstRow = true;
    
    long totalRows = 0;
    int totalPages = 1;

    Session sessDP = null;
    try {
        sessDP = HibernateUtil.openSession();
        List listDataTerpilihRaw = new ArrayList();
        if (!selectedIds.isEmpty()) {
            Criteria critSelected = sessDP.createCriteria(Class.forName(className));
            critSelected.add(Restrictions.in("id", selectedIds));
            listDataTerpilihRaw = ConstantValues.simpleList(critSelected, Class.forName(className));
        }

        Criteria crit = sessDP.createCriteria(Class.forName(className));
        Criteria critCount = sessDP.createCriteria(Class.forName(className));

        if (!selectedIds.isEmpty()) {
            crit.add(Restrictions.not(Restrictions.in("id", selectedIds)));
            critCount.add(Restrictions.not(Restrictions.in("id", selectedIds)));
        }

        // TERAPKAN FILTER
        if (hasYayasan && filterYayasan != null && !filterYayasan.isEmpty()) {
            crit.add(Restrictions.eq("yayasan.id", Long.parseLong(filterYayasan)));
            critCount.add(Restrictions.eq("yayasan.id", Long.parseLong(filterYayasan)));
        } else if (hasYayasan && tbmuser != null && tbmuser.ambilYayasan() != null && (selectedYayasan == null || selectedYayasan.isEmpty())) {
            crit.add(Restrictions.eq("yayasan.id", tbmuser.ambilYayasan().getId()));
            critCount.add(Restrictions.eq("yayasan.id", tbmuser.ambilYayasan().getId()));
        }
        
        if (hasSekolah && filterSekolah != null && !filterSekolah.isEmpty()) {
            crit.add(Restrictions.eq("sekolah.id", Long.parseLong(filterSekolah)));
            critCount.add(Restrictions.eq("sekolah.id", Long.parseLong(filterSekolah)));
        } else if (hasSekolah && tbmuser != null && tbmuser.ambilSekolah() != null && (selectedSekolah == null || selectedSekolah.isEmpty())) {
            crit.add(Restrictions.eq("sekolah.id", tbmuser.ambilSekolah().getId()));
            critCount.add(Restrictions.eq("sekolah.id", tbmuser.ambilSekolah().getId()));
        }
        
        if (hasFakultas && filterFakultas != null && !filterFakultas.isEmpty()) {
            crit.add(Restrictions.eq("fakultas.id", Long.parseLong(filterFakultas)));
            critCount.add(Restrictions.eq("fakultas.id", Long.parseLong(filterFakultas)));
        }
        
        if (hasJurusan && filterJurusan != null && !filterJurusan.isEmpty()) {
            crit.add(Restrictions.eq("jurusan.id", Long.parseLong(filterJurusan)));
            critCount.add(Restrictions.eq("jurusan.id", Long.parseLong(filterJurusan)));
        }

        // TERAPKAN PENCARIAN
        if (keyword != null && !keyword.isEmpty()) {
            Disjunction or = Restrictions.disjunction();
            boolean isSearchAdded = false;
            
            for (String col : cols) {
                String colName = col.trim();
                if (!colName.contains(".")) { 
                    if (colName.equalsIgnoreCase("nama") || colName.equalsIgnoreCase("nim") || 
                        colName.equalsIgnoreCase("noRegistrasi") || colName.equalsIgnoreCase("kode") ||
                        colName.equalsIgnoreCase("keterangan") || colName.equalsIgnoreCase("deskripsi")) {
                        
                        or.add(Restrictions.ilike(colName, keyword, MatchMode.ANYWHERE));
                        isSearchAdded = true;
                    }
                }
            }
            
            try { 
                long idKey = Long.parseLong(keyword);
                or.add(Restrictions.eq("id", idKey)); 
                isSearchAdded = true;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_data_picker.jsp:217");}
            
            if (isSearchAdded) {
                crit.add(or);
                critCount.add(or);
            }
        }

        // TERAPKAN PENGURUTAN
        if (!sortOrders.isEmpty()) {
            for (String[] sort : sortOrders) {
                if (sort[1].equals("desc")) crit.addOrder(Order.desc(sort[0]));
                else crit.addOrder(Order.asc(sort[0]));
            }
        } else {
            crit.addOrder(Order.asc("id"));
        }

        // HITUNG TOTAL HALAMAN
        critCount.setProjection(Projections.rowCount());
        totalRows = (Long) critCount.uniqueResult();
        totalPages = (int) Math.ceil((double) totalRows / pageSize);
        if (totalPages == 0) totalPages = 1;
        
        // TERAPKAN PAGING
        crit.setFirstResult(firstResult);
        crit.setMaxResults(pageSize);
        
        List listDataRaw = ConstantValues.simpleList(crit, Class.forName(className));
        
        // PENGGABUNGAN & PEMETAAN DATA
        List combinedRawList = new ArrayList();
        combinedRawList.addAll(listDataTerpilihRaw);
        combinedRawList.addAll(listDataRaw);
        
        for (Object obj : combinedRawList) {
            Map<String, Object> map = new HashMap<String, Object>();
            
            if (!isFirstRow) {
                jsonArrayDataStr.append(", ");
            }
            jsonArrayDataStr.append("{");
            
            // Proses ID
            Object idVal = "";
            try { 
                idVal = PropertyUtils.getNestedProperty(obj, "id");
                map.put("id", idVal); 
            } catch (Exception e) { 
                map.put("id", ""); 
            }
            
            // Format ID untuk JSON (Escaping Tanda Kutip & Karakter Spesial)
            String safeId = idVal != null ? idVal.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") : "";
            jsonArrayDataStr.append("\"id\": \"").append(safeId).append("\"");
            
            // Proses Kolom-Kolom Lainnya
            for (String col : cols) {
                String colName = col.trim();
                Object val = "";
                try {
                    val = PropertyUtils.getNestedProperty(obj, colName);
                    map.put(colName, val != null ? val : "");
                } catch (Exception e) { 
                    map.put(colName, "-"); 
                }
                
                // Format Value untuk JSON dengan aman
                String safeVal = val != null ? val.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") : "";
                jsonArrayDataStr.append(", \"").append(colName).append("\": \"").append(safeVal).append("\"");
            }
            
            jsonArrayDataStr.append("}");
            safeListData.add(map);
            isFirstRow = false;
        }
        jsonArrayDataStr.append("]");

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/common/_data_picker.jsp:296");
        // Fallback array kosong jika error
        jsonArrayDataStr.setLength(0);
        jsonArrayDataStr.append("[]");
    } finally {
        if (sessDP != null && sessDP.isOpen()) {
            sessDP.disconnect();
            sessDP.close();
        }
    }
%>

<div id="dp_wrapper_<%=rnd%>">
    <div class="p-4 bg-light rounded-bottom-4">
        
        <div class="row g-2 mb-3">
            <% if (hasYayasan) { %>
            <div class="col-md-3">
                <select id="filterYayasan<%=rnd%>" class="form-select border-0 shadow-sm" onchange="window.muatUlangPicker<%=rnd%>(1)" <%= selectedYayasan != null ? "disabled" : "" %>>
                    <option value=""><%=Common.getBahasaConfig("-- Semua Yayasan --")%></option>
                    <% for(Yayasan y : listYayasanRyg) { %>
                        <option value="<%=y.getId()%>" <%= (filterYayasan != null && filterYayasan.equals(String.valueOf(y.getId()))) ? "selected" : "" %>><%=y.getNama()%></option>
                    <% } %>
                </select>
            </div>
            <% } %>
            
            <% if (hasSekolah) { %>
            <div class="col-md-3">
                <select id="filterSekolah<%=rnd%>" class="form-select border-0 shadow-sm" onchange="window.muatUlangPicker<%=rnd%>(1)" <%= selectedSekolah != null ? "disabled" : "" %>>
                    <option value=""><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>
                    <% for(Sekolah s : listSekolahRyg) { %>
                        <option value="<%=s.getId()%>" <%= (filterSekolah != null && filterSekolah.equals(String.valueOf(s.getId()))) ? "selected" : "" %>><%=s.getNama()%></option>
                    <% } %>
                </select>
            </div>
            <% } %>

            <% if (hasFakultas) { %>
            <div class="col-md-3">
                <select id="filterFakultas<%=rnd%>" class="form-select border-0 shadow-sm" onchange="window.muatUlangPicker<%=rnd%>(1)" <%= selectedFakultas != null ? "disabled" : "" %>>
                    <option value=""><%=Common.getBahasaConfig("-- Semua Fakultas --")%></option>
                    <% for(Fakultas f : listFakultasRyg) { %>
                        <option value="<%=f.getId()%>" <%= (filterFakultas != null && filterFakultas.equals(String.valueOf(f.getId()))) ? "selected" : "" %>><%=f.getNama()%></option>
                    <% } %>
                </select>
            </div>
            <% } %>

            <% if (hasJurusan) { %>
            <div class="col-md-3">
                <select id="filterJurusan<%=rnd%>" class="form-select border-0 shadow-sm" onchange="window.muatUlangPicker<%=rnd%>(1)" <%= selectedJurusan != null ? "disabled" : "" %>>
                    <option value=""><%=Common.getBahasaConfig("-- Semua Jurusan --")%></option>
                    <% for(Jurusan j : listJurusanRyg) { %>
                        <option value="<%=j.getId()%>" <%= (filterJurusan != null && filterJurusan.equals(String.valueOf(j.getId()))) ? "selected" : "" %>><%=j.getNama()%></option>
                    <% } %>
                </select>
            </div>
            <% } %>
        </div>

        <div class="input-group input-group-lg mb-4 shadow-sm rounded-3 overflow-hidden">
            <span class="input-group-text bg-white border-0"><i class="fas fa-search text-primary"></i></span>
            <input type="text" class="form-control border-0 bg-white fs-6" id="txtCariPicker<%=rnd%>" value="<%=keyword%>" placeholder="<%=Common.getBahasaConfig("Ketik nama, identitas, atau kode...")%>" onkeyup="if(event.keyCode===13) window.muatUlangPicker<%=rnd%>(1)">
            <button class="btn btn-primary px-4 fw-bold transition-all" onclick="window.muatUlangPicker<%=rnd%>(1)">
                <%=Common.getBahasaConfig("Cari Data")%>
            </button>
        </div>

        <div class="table-responsive rounded-3 shadow-sm border border-light bg-white mb-3" style="min-height: 250px; max-height: 450px;">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-dark sticky-top" style="z-index: 1;">
                    <tr>
                        <% if (!isRadio) { %>
                        <th class="text-center py-3" style="width: 50px;">
                            <input class="form-check-input" type="checkbox" id="chkAll<%=rnd%>" onclick="window.toggleSemuaCentang<%=rnd%>(this)">
                        </th>
                        <% } else { %>
                        <th class="text-center py-3" style="width: 50px;">#</th>
                        <% } %>

                        <% for(String col : cols) { %>
                        <th class="text-uppercase small fw-bold py-3"><%= Common.getBahasaConfig(col) %></th>
                        <% } %>

                        <% if (isRadio) { %>
                        <th class="text-center py-3" style="width: 120px;"><%= Common.getBahasaConfig("Aksi") %></th>
                        <% } %>
                    </tr>
                </thead>
                <tbody id="tbodyPicker<%=rnd%>">
                    <% if(safeListData.isEmpty()) { %>
                    <tr>
                        <td colspan="<%= cols.length + 2 %>" class="text-center py-5 text-muted">
                            <i class="fas fa-folder-open fa-3x opacity-25 mb-3 d-block"></i>
                            <%= Common.getBahasaConfig("Tidak ada data yang tersedia atau ditemukan.") %>
                        </td>
                    </tr>
                    <% } else {
                        int no = firstResult + 1;
                        for(Map<String, Object> row : safeListData) { 
                            boolean isChecked = selectedIds.contains(Long.parseLong(row.get("id").toString()));
                    %>
                    <tr class="<%= isChecked ? "table-primary" : "" %>">
                        <% if (!isRadio) { %>
                        <td class="text-center">
                            <input class="form-check-input chk-item<%=rnd%>" type="checkbox" value="<%= row.get("id") %>" <%= isChecked ? "checked" : "" %> onclick="window.evaluasiCentangUtama<%=rnd%>()">
                        </td>
                        <% } else { %>
                        <td class="text-center text-muted fw-bold"><%= isChecked ? "<i class='fas fa-star text-warning'></i>" : no++ %></td>
                        <% } %>

                        <% for(String col : cols) { %>
                            <td class="fw-semibold text-dark fs-6"><%= row.get(col.trim()) %></td>
                        <% } %>

                        <% if (isRadio) { %>
                        <td class="text-center">
                            <button class="btn btn-sm <%= isChecked ? "btn-success" : "btn-outline-primary" %> rounded-pill px-4 fw-bold shadow-sm" onclick="window.pilihSatuData<%=rnd%>('<%= row.get("id") %>')">
                                <i class="fas <%= isChecked ? "fa-check-circle" : "fa-check" %> me-1"></i><%= Common.getBahasaConfig(isChecked ? "Terpilih" : "Pilih") %>
                            </button>
                        </td>
                        <% } %>
                    </tr>
                    <% 
                        }
                    } 
                    %>
                </tbody>
            </table>
        </div>
        
        <div class="d-flex justify-content-between align-items-center mt-4">
            
            <nav aria-label="Navigasi Halaman Data Picker">
                <ul class="pagination pagination-sm mb-0 shadow-sm">
                    <li class="page-item <%= pageNum <= 1 ? "disabled" : "" %>">
                        <a class="page-link" href="javascript:void(0)" onclick="window.muatUlangPicker<%=rnd%>(<%= pageNum - 1 %>)"><%= Common.getBahasaConfig("Seb.") %></a>
                    </li>
                    
                    <% 
                        int startPage = Math.max(1, pageNum - 2);
                        int endPage = Math.min(totalPages, pageNum + 2);
                        for (int i = startPage; i <= endPage; i++) { 
                    %>
                    <li class="page-item <%= i == pageNum ? "active" : "" %>">
                        <a class="page-link" href="javascript:void(0)" onclick="window.muatUlangPicker<%=rnd%>(<%= i %>)"><%= i %></a>
                    </li>
                    <% } %>
                    
                    <li class="page-item <%= pageNum >= totalPages ? "disabled" : "" %>">
                        <a class="page-link" href="javascript:void(0)" onclick="window.muatUlangPicker<%=rnd%>(<%= pageNum + 1 %>)"><%= Common.getBahasaConfig("Sel.") %></a>
                    </li>
                </ul>
            </nav>
            
            <% if (!isRadio) { %>
            <div class="d-flex gap-2">
                <button class="btn btn-outline-secondary px-4 py-2 fw-bold rounded-pill shadow-sm transition-all" onclick="window.tutupPicker<%=rnd%>()">
                    <i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Batal") %>
                </button>
                <button class="btn btn-success px-4 py-2 fw-bold rounded-pill shadow-sm transition-all" onclick="window.kumpulkanDataTercentang<%=rnd%>()">
                    <i class="fas fa-save me-2"></i><%= Common.getBahasaConfig("Simpan") %>
                </button>
            </div>
            <% } %>
        </div>
    </div>

    <script>
        // Mendaftarkan variabel ke Objek Window
        // HASIL PERBAIKAN: Memanggil jsonArrayDataStr yang dirakit manual di Java
        // Hal ini menghilangkan bug "Invalid Shorthand Property" secara permanen
        window.pickerDataCache<%=rnd%> = <%= jsonArrayDataStr.toString() %>;
        
        if (typeof window.globalSelectedIds<%=rnd%> === 'undefined') {
            window.globalSelectedIds<%=rnd%> = new Set();
        }
        
        <% for(Long sid : selectedIds) { %>
            window.globalSelectedIds<%=rnd%>.add('<%=sid%>');
        <% } %>

        window.simpanStateCentang<%=rnd%> = function() {
            <% if (!isRadio) { %>
            document.querySelectorAll('.chk-item<%=rnd%>').forEach(chk => {
                if (chk.checked) window.globalSelectedIds<%=rnd%>.add(chk.value);
                else window.globalSelectedIds<%=rnd%>.delete(chk.value);
            });
            <% } %>
        };

        // --- PENGGUNAAN ASYNC/AWAIT SAFE SCRIPT INJECTION ---
        window.muatUlangPicker<%=rnd%> = async function(targetPage) {
            window.simpanStateCentang<%=rnd%>(); 
            
            const keyword = document.getElementById('txtCariPicker<%=rnd%>').value;
            const selectedIdsStr = Array.from(window.globalSelectedIds<%=rnd%>).join(',');

            let extraParams = '';
            
            const lockYayasan = '<%= selectedYayasan != null ? selectedYayasan : "" %>';
            if (lockYayasan) extraParams += '&yayasan=' + encodeURIComponent(lockYayasan);
            else {
                const fYayasan = document.getElementById('filterYayasan<%=rnd%>');
                if (fYayasan) extraParams += '&filterYayasan=' + encodeURIComponent(fYayasan.value);
            }

            const lockSekolah = '<%= selectedSekolah != null ? selectedSekolah : "" %>';
            if (lockSekolah) extraParams += '&sekolah=' + encodeURIComponent(lockSekolah);
            else {
                const fSekolah = document.getElementById('filterSekolah<%=rnd%>');
                if (fSekolah) extraParams += '&filterSekolah=' + encodeURIComponent(fSekolah.value);
            }

            const lockFakultas = '<%= selectedFakultas != null ? selectedFakultas : "" %>';
            if (lockFakultas) extraParams += '&fakultas=' + encodeURIComponent(lockFakultas);
            else {
                const fFakultas = document.getElementById('filterFakultas<%=rnd%>');
                if (fFakultas) extraParams += '&filterFakultas=' + encodeURIComponent(fFakultas.value);
            }

            const lockJurusan = '<%= selectedJurusan != null ? selectedJurusan : "" %>';
            if (lockJurusan) extraParams += '&jurusan=' + encodeURIComponent(lockJurusan);
            else {
                const fJurusan = document.getElementById('filterJurusan<%=rnd%>');
                if (fJurusan) extraParams += '&filterJurusan=' + encodeURIComponent(fJurusan.value);
            }

            const url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker' +
                        '&class=<%=className%>&cols=<%=URLEncoder.encode(colsStr, "UTF-8")%>' +
                        '&order=<%=URLEncoder.encode(orderStr, "UTF-8")%>' +
                        '&radio=<%=isRadio%>&rnd=<%=rnd%>&callback=<%=callback%>' +
                        '&page=' + targetPage + 
                        '&keyword=' + encodeURIComponent(keyword) + extraParams +
                        '&selectedIds=' + encodeURIComponent(selectedIdsStr);
                        
            const wrapper = document.getElementById('dp_wrapper_<%=rnd%>');
            const container = wrapper ? wrapper.parentNode : document.getElementById('modalBodyPicker<%=rnd%>');
            
            if (container) {
                container.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary mb-3" style="width: 3rem; height: 3rem;"></div><h5 class="fw-bold text-secondary"><%=Common.getBahasaConfig("Mencari data...")%></h5></div>';
                
                try {
                    const response = await fetch(url);
                    if (!response.ok) throw new Error("HTTP Error");
                    const html = await response.text();
                    
                    container.innerHTML = html;
                    
                    // Eksekusi script In-Place Replacement (Aman & Tidak Menumpuk)
                    const scripts = Array.from(container.querySelectorAll('script'));
                    for (const s of scripts) {
                        await new Promise((resolve) => {
                            const ns = document.createElement('script');
                            var srcEffNs = s.src || s.getAttribute('data-rocketlazyloadscript') || '';
                            Array.from(s.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') ns.setAttribute(attr.name, attr.value); });
                            ns.type = 'text/javascript';
                            if (srcEffNs) {
                                ns.onload = resolve;
                                ns.onerror = resolve;
                                ns.src = srcEffNs;
                                if (s.parentNode) s.parentNode.replaceChild(ns, s);
                                else resolve();
                            } else {
                                ns.textContent = s.textContent;
                                if (s.parentNode) s.parentNode.replaceChild(ns, s);
                                resolve();
                            }
                        });
                    }
                } catch (err) {
                    container.innerHTML = '<div class="alert alert-danger shadow-sm border-0 m-4"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Gagal menghubungi peladen.")%></div>';
                }
            }
        };

        window.pilihSatuData<%=rnd%> = function(id) {
            const data = window.pickerDataCache<%=rnd%>.filter(item => String(item.id) === String(id));
            window.eksekusiCallback<%=rnd%>(data);
        };

        window.toggleSemuaCentang<%=rnd%> = function(source) {
            const checkboxes = document.querySelectorAll('.chk-item<%=rnd%>');
            checkboxes.forEach(chk => chk.checked = source.checked);
            window.evaluasiCentangUtama<%=rnd%>();
        };

        window.evaluasiCentangUtama<%=rnd%> = function() {
            const checkboxes = document.querySelectorAll('.chk-item<%=rnd%>');
            const checkAllBtn = document.getElementById('chkAll<%=rnd%>');
            if (!checkAllBtn) return;
            let semuaTercentang = checkboxes.length > 0;
            checkboxes.forEach(chk => { if(!chk.checked) semuaTercentang = false; });
            checkAllBtn.checked = semuaTercentang;
        };
        
        window.kumpulkanDataTercentang<%=rnd%> = function() {
            window.simpanStateCentang<%=rnd%>();
            if (window.globalSelectedIds<%=rnd%>.size === 0) {
                return;
            }

            let parsedDataList = [];
            window.globalSelectedIds<%=rnd%>.forEach(id => {
                let rowData = window.pickerDataCache<%=rnd%>.find(item => String(item.id) === String(id));
                if (rowData) parsedDataList.push(rowData);
                else parsedDataList.push({ id: id }); 
            });
            window.eksekusiCallback<%=rnd%>(parsedDataList);
        };

        window.tutupPicker<%=rnd%> = function() {
            const wrapper = document.getElementById('dp_wrapper_<%=rnd%>');
            if (wrapper) {
                const modalEl = wrapper.closest('.modal');
                if(modalEl) {
                    try { bootstrap.Modal.getInstance(modalEl).hide();
                    } catch(e){}
                }
            }
            if (window.globalSelectedIds<%=rnd%>) window.globalSelectedIds<%=rnd%>.clear();
        };

        window.eksekusiCallback<%=rnd%> = function(dataArray) {
            if (typeof window['<%=callback%>'] === 'function') {
                window['<%=callback%>'](dataArray);
            }
            window.tutupPicker<%=rnd%>();
        };
    </script>
</div>