<%@page import="ais.common.Common"%>
<%@page import="ais.common.JSPCurdGenerator"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.metadata.ClassMetadata"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
String rnd = request.getParameter("rnd");
String modelClass = request.getParameter("modelClass");

// Kita gunakan || sebagai pemisah agar aman dari tanda koma di dalam JSON
String searchColsParam = request.getParameter("searchCols");
String[] searchCols = searchColsParam != null ? searchColsParam.split("\\|\\|") : new String[0];

ClassMetadata masterMetadata = null;
try {
	Class clazz = Class.forName(modelClass);
	masterMetadata = HibernateUtil.getClassMetadata(clazz);
} catch (Exception e) {
	out.print("");
}
%>

<div id="searchPanel_<%=rnd%>"
	class="mb-3 animate__animated animate__fadeIn"
	style="display: none; border-bottom: 1px dashed #dee2e6; padding-bottom: 1rem;">
	<div class="row g-3 p-4">
            <%
            if (searchCols == null || searchCols.length == 0 || searchCols[0].isEmpty()) {
            %>
                <div class="col-12 text-danger fw-bold">
                    <i class="fas fa-exclamation-triangle"></i> Parameter 'searchCols' kosong atau tidak terbaca dari index.jsp!
                </div>
            <%
            } else if (masterMetadata == null) {
            %>
                <div class="col-12 text-danger fw-bold">
                    <i class="fas fa-exclamation-triangle"></i> Gagal memuat ClassMetadata. Pastikan 'modelClass' valid: <%=modelClass%>
                </div>
            <%
            } else {
                // ... (SISA KODE LOOPING SEPERTI SEBELUMNYA) ...
                for (String rawCol : searchCols) {
                    if (rawCol == null || rawCol.trim().isEmpty()) continue;
                    
                    String col = rawCol.trim();
                    String type = "";
                    try { 
                        String[] ss = col.split(";", 2); 
                        col = ss.length > 0 ? ss[0].trim() : ""; 
                        type = ss.length > 1 ? ss[1].trim() : "";  
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_search_component.jsp:54");}

                    String returnName = null;
                    try { returnName = masterMetadata.getPropertyType(col).getReturnedClass().getName(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_search_component.jsp:57");}

                    String textWithSpace = col.replaceAll("([a-z])([A-Z]+)", "$1 $2");
                    String label = textWithSpace.substring(0, 1).toUpperCase() + textWithSpace.substring(1);
                    label = Common.getBahasaConfig(label);

                    String elId = "search_" + col + "_" + rnd;
            %>
                <div class="col-md-4 col-lg-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Cari")%> <%=label%></label>
                    <%
                    JSONArray colConf = Common.validJsonArray(type);
                    if (colConf != null) {
                    %>
                        <select class="form-select form-select-sm shadow-none" id="<%=elId%>" onchange="triggerSearch<%=rnd%>()">
                            <option value="">-- <%=Common.getBahasaConfig("Semua")%> --</option>
                            <% for (int k = 0; k < colConf.length(); k++) {
                                JSONObject opt = colConf.optJSONObject(k);
                                if (opt != null) { %>
                                    <option value="<%=opt.optString("id", "")%>"><%=opt.optString("nama", "")%></option>
                            <%  }
                            } %>
                        </select>
                    <%
                    } else if(returnName != null && returnName.startsWith("ais.database.model")) {
                        String s = JSPCurdGenerator.pilihan(rnd, col, label, returnName, false, "id", "nama", "", "search", "triggerSearch" + rnd + "();");
                    %>
                        <%=s%>
                    <%
                    } else if (returnName != null && returnName.equals(Boolean.class.getName())) {
                    %>
                        <select class="form-select form-select-sm shadow-none" id="<%=elId%>" onchange="triggerSearch<%=rnd%>()">
                            <option value=""><%=Common.getBahasaConfig("Semua")%></option>
                            <option value="true"><%=Common.getBahasaConfig("Aktif")%></option>
                            <option value="false"><%=Common.getBahasaConfig("Tidak Aktif")%></option>
                        </select>
                    <%
                    } else {
                    %>
                        <input type="text" class="form-control form-control-sm shadow-none" id="<%=elId%>" onchange="triggerSearch<%=rnd%>()" placeholder="...">
                    <%
                    }
                    %>
                </div>
            <%
                } // akhir for
            } // akhir else
            %>
        </div>

</div>

<script>
    // Toggle untuk menampilkan/menyembunyikan panel pencarian
	function toggleSearchPanel<%=rnd%>() { // Pastikan ID ini sama dengan yang ada di error (8A52F5)
        const panel = document.getElementById('searchPanel_<%=rnd%>');
        if (panel) panel.style.display = (panel.style.display === 'none') ? 'block' : 'none';
    }

    // Trigger pencarian saat input berubah
    function triggerSearch<%=rnd%>() {
        if(typeof resetAndLoadData<%=rnd%> === 'function') resetAndLoadData<%=rnd%>();
    }

    // DYNAMIC SCRIPT: Mengambil kriteria pencarian yang aktif berdasarkan kolom
    function getSearchCriteria<%=rnd%>() {
        let criteria = [];
        <%if (masterMetadata != null) {
	for (String rawCol : searchCols) {
		if (rawCol == null || rawCol.trim().isEmpty())
			continue;
		String col = rawCol.split(";", 2)[0].trim();
		String returnName = null;
		try {
			returnName = masterMetadata.getPropertyType(col).getReturnedClass().getName();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/common/_search_component.jsp:132");
		}
		String elId = "search_" + col + "_" + rnd;

		if (returnName != null && returnName.startsWith("ais.database.model")) {%>
                    var val_<%=col%> = document.getElementById('<%=elId%>') ? document.getElementById('<%=elId%>').value : '';
                    if(val_<%=col%>) criteria.push("<%=col%> = " + val_<%=col%>);
        <%} else if (returnName != null && returnName.equals(Boolean.class.getName())) {%>
                    var val_<%=col%> = document.getElementById('<%=elId%>') ? document.getElementById('<%=elId%>').value : '';
                    if(val_<%=col%> !== '') criteria.push("<%=col%> = " + val_<%=col%>);
        <%} else {%>
                    var val_<%=col%> = document.getElementById('<%=elId%>') ? document.getElementById('<%=elId%>').value.trim() : '';
                    if(val_<%=col%>) criteria.push("<%=col%> ILIKE '%" + val_<%=col%> + "%'");
        <%}
}
}%>
        return criteria.join(' AND ');
    }
</script>