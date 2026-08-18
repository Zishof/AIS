<%@page import="java.util.*"%>
<%@page import="java.net.URLDecoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%
// 1. PENANGKAPAN PARAMETER DASAR
String action = request.getParameter("action");
String className = request.getParameter("class");
String colsStr = request.getParameter("cols") != null ? URLDecoder.decode(request.getParameter("cols"), "UTF-8") : "id,nama";
String aktif = request.getParameter("aktif");
String orderStr = request.getParameter("order") != null ? URLDecoder.decode(request.getParameter("order"), "UTF-8") : "id";
String[] columns = colsStr.split(",");

// 2. BLOK PEMROSESAN DATA (SERVER-SIDE PAGING & FILTERING)
if ("get_data".equals(action)) {
    String keyword = request.getParameter("keyword") != null ? request.getParameter("keyword").trim() : "";
    int pageNo = request.getParameter("page") != null ? Integer.parseInt(request.getParameter("page")) : 1;
    int limit = 10;
    int start = (pageNo - 1) * limit;

    Session sessionDb = HibernateUtil.openSession();
    List<GeneralValueObject> dataList = new ArrayList<GeneralValueObject>();
    long totalCount = 0;

    try {
        Class<?> targetClass = Class.forName(className);
        org.hibernate.Criteria criteria = sessionDb.createCriteria(targetClass);

        if ("true".equalsIgnoreCase(aktif)) {
            criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        } else {
            criteria.add(Restrictions.sqlRestriction("true"));
        }

        if (!keyword.isEmpty()) {
            Disjunction or = Restrictions.disjunction();
            for (String col : columns) {
                or.add(Restrictions.ilike(col, keyword, MatchMode.ANYWHERE));
            }
            criteria.add(or);
        }

        criteria.setProjection(Projections.rowCount());
        totalCount = ((Number) criteria.uniqueResult()).longValue();

        criteria.setProjection(null);
        criteria.setResultTransformer(org.hibernate.Criteria.ROOT_ENTITY);
        if (orderStr != null && !orderStr.trim().isEmpty()) {
            criteria.addOrder(Order.asc(orderStr));
        }

        criteria.setFirstResult(start);
        criteria.setMaxResults(limit);

        dataList = ais.common.ConstantValues.simpleList(criteria, targetClass);
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/common/_data_picker_banyak.jsp:66");
    } finally {
        if (sessionDb.isOpen()) {
            sessionDb.disconnect();
            sessionDb.close();
        }
        HibernateUtil.closeSessionQuietly(sessionDb);
    }

    JSONObject jsonResult = new JSONObject();
    jsonResult.put("total", totalCount);
    JSONArray jsonArray = new JSONArray();
    
    for (GeneralValueObject row : dataList) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("id", row.getId());
        for (String col : columns) {
            try {
                String methodName = "get" + col.substring(0, 1).toUpperCase() + col.substring(1);
                java.lang.reflect.Method method = row.getClass().getMethod(methodName);
                Object res = method.invoke(row);
                jsonObj.put(col, res != null ? res.toString() : "-");
            } catch(Exception e) {
                jsonObj.put(col, "-");
            }
        }
        jsonArray.put(jsonObj);
    }
    jsonResult.put("data", jsonArray);

    response.setContentType("application/json;charset=UTF-8");
    out.print(jsonResult.toString());
    return; 
}

// 3. PEMBUATAN ANTARMUKA HTML
String rnd = Common.getGeneratedBarCode(5);
String title = request.getParameter("title") != null ? URLDecoder.decode(request.getParameter("title"), "UTF-8") : "Pilih Data";
String callback = request.getParameter("callback") != null ? request.getParameter("callback") : "onDataPickerSelected";

// === PARAMETER DATA TERPILIH (PRE-SELECTED) ===
String idsTerpilih = request.getParameter("idsTerpilih") != null ? URLDecoder.decode(request.getParameter("idsTerpilih"), "UTF-8") : "";
%>

<div class="card border-0 shadow-sm d-flex flex-column h-100 w-100 bg-white">
    <div class="card-header bg-light border-bottom p-3 d-flex justify-content-between align-items-center">
        <h6 class="fw-bold text-dark m-0"><i class="fas fa-list-ul me-2"></i><%=Common.getBahasaConfig(title)%></h6>
        <div class="input-group input-group-sm w-50 shadow-sm">
            <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span>
            <input type="text" id="searchInputPicker<%=rnd%>" class="form-control border-start-0 ps-0" placeholder="<%=Common.getBahasaConfig("Cari data (Tekan Enter)...")%>">
            <button class="btn btn-primary fw-bold px-3" onclick="terapkanPencarian<%=rnd%>()"><%=Common.getBahasaConfig("Cari")%></button>
        </div>
    </div>
    
    <div class="card-body p-0 d-flex flex-column position-relative" style="height: 50vh; overflow-y: auto;">
        <div id="loadingPicker<%=rnd%>" class="position-absolute w-100 h-100 d-flex justify-content-center align-items-center bg-white d-none" style="z-index: 10; opacity: 0.8;">
            <div class="spinner-border text-primary" role="status"></div>
        </div>

        <table class="table table-hover table-striped mb-0 align-middle small">
            <thead class="table-light sticky-top shadow-sm" style="z-index: 1;">
                <tr>
                    <th style="width: 50px;" class="text-center">
                        <input type="checkbox" id="chkAll<%=rnd%>" class="form-check-input border-secondary shadow-sm" style="cursor:pointer;" onclick="toggleAllPicker<%=rnd%>(this)">
                    </th>
                    <% 
                    for(String col : columns) {
                        String headerName = col.substring(0, 1).toUpperCase() + col.substring(1);
                        out.print("<th>" + Common.getBahasaConfig(headerName) + "</th>");
                    } 
                    %>
                </tr>
            </thead>
            <tbody id="tbodyPicker<%=rnd%>">
            </tbody>
        </table>
    </div>

    <div class="card-footer bg-white border-top d-flex justify-content-between align-items-center p-2 px-3">
        <span class="small text-muted fw-medium" id="pagingInfo<%=rnd%>"><%=Common.getBahasaConfig("Memuat data...")%></span>
        <div class="btn-group shadow-sm">
            <button type="button" class="btn btn-sm btn-outline-primary fw-bold px-3" id="btnPrev<%=rnd%>" onclick="gantiHalaman<%=rnd%>(-1)"><i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Sebelumnya")%></button>
            <button type="button" class="btn btn-sm btn-outline-primary fw-bold px-3" id="btnNext<%=rnd%>" onclick="gantiHalaman<%=rnd%>(1)"><%=Common.getBahasaConfig("Selanjutnya")%><i class="fas fa-chevron-right ms-1"></i></button>
        </div>
    </div>

    <div class="card-footer bg-light border-top d-flex justify-content-end gap-2 p-3">
        <button type="button" class="btn btn-light border fw-bold shadow-sm px-4 rounded-pill" onclick="closePickerModal<%=rnd%>()">
            <i class="far fa-window-close me-2 text-danger"></i><%=Common.getBahasaConfig("Batal")%>
        </button>
        <button type="button" class="btn btn-success fw-bold shadow-sm px-4 rounded-pill" onclick="submitPicker<%=rnd%>()">
            <i class="fas fa-check-circle me-2"></i><%=Common.getBahasaConfig("Pilih Data")%>
        </button>
    </div>
</div>

<script>
    let currentPage<%=rnd%> = 1;
    let currentKeyword<%=rnd%> = "";
    let totalPages<%=rnd%> = 1;
    const pageSize<%=rnd%> = 10;
    
    const columnsList<%=rnd%> = "<%=colsStr%>".split(",");
    const baseUrlPicker<%=rnd%> = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker_banyak&class=<%=className%>&cols=<%=colsStr%>&aktif=<%=aktif%>&order=<%=orderStr%>";

    // === INISIALISASI ID TERPILIH KE DALAM MEMORI (SET) ===
    let selectedIds<%=rnd%> = new Set();
    const initialIds<%=rnd%> = "<%=idsTerpilih%>".split(",").filter(id => id.trim() !== "");
    initialIds<%=rnd%>.forEach(id => selectedIds<%=rnd%>.add(id));

    const loadPickerData<%=rnd%> = async () => {
        const tbody = document.getElementById('tbodyPicker<%=rnd%>');
        const loading = document.getElementById('loadingPicker<%=rnd%>');
        
        loading.classList.remove('d-none');
        
        try {
            const fetchUrl = baseUrlPicker<%=rnd%> + "&action=get_data&page=" + currentPage<%=rnd%> + "&keyword=" + encodeURIComponent(currentKeyword<%=rnd%>);
            const response = await fetch(fetchUrl);
            const result = await response.json();
            
            const totalData = result.total || 0;
            const dataArray = result.data || [];
            
            totalPages<%=rnd%> = Math.ceil(totalData / pageSize<%=rnd%>) || 1;
            
            tbody.innerHTML = '';
            if (dataArray.length === 0) {
                tbody.innerHTML = '<tr><td colspan="' + (columnsList<%=rnd%>.length + 1) + '" class="text-center py-5 text-muted fst-italic"><i class="fas fa-folder-open fa-2x mb-2 d-block opacity-50"></i><%=Common.getBahasaConfig("Tidak ada data ditemukan.")%></td></tr>';
            } else {
                dataArray.forEach(row => {
                    const isChecked = selectedIds<%=rnd%>.has(row.id.toString()) ? 'checked' : '';
                    let tr = '<tr>';
                    tr += '<td class="text-center"><input type="checkbox" class="form-check-input border-primary shadow-sm chk-item-picker<%=rnd%>" style="cursor:pointer; width:1.1rem; height:1.1rem;" value="' + row.id + '" ' + isChecked + ' onchange="handleCheck<%=rnd%>(this)"></td>';
                    
                    columnsList<%=rnd%>.forEach(col => {
                        tr += '<td class="text-dark">' + (row[col] || '-') + '</td>';
                    });
                    
                    tr += '</tr>';
                    tbody.insertAdjacentHTML('beforeend', tr);
                });
            }

            const infoText = '<%=Common.getBahasaConfigJS("Halaman")%> ' + currentPage<%=rnd%> + ' <%=Common.getBahasaConfig("dari")%> ' + totalPages<%=rnd%> + ' (' + totalData + ' <%=Common.getBahasaConfig("Data")%>)';
            document.getElementById('pagingInfo<%=rnd%>').innerText = infoText;
            
            document.getElementById('btnPrev<%=rnd%>').disabled = (currentPage<%=rnd%> <= 1);
            document.getElementById('btnNext<%=rnd%>').disabled = (currentPage<%=rnd%> >= totalPages<%=rnd%>);

            updateCheckAllState<%=rnd%>();

        } catch (error) {
            console.error(error);
            tbody.innerHTML = '<tr><td colspan="' + (columnsList<%=rnd%>.length + 1) + '" class="text-center py-5 text-danger"><i class="fas fa-exclamation-triangle me-2"></i><%=Common.getBahasaConfig("Terjadi kesalahan koneksi.")%></td></tr>';
        } finally {
            loading.classList.add('d-none');
        }
    };

    const terapkanPencarian<%=rnd%> = () => {
        currentKeyword<%=rnd%> = document.getElementById('searchInputPicker<%=rnd%>').value.trim();
        currentPage<%=rnd%> = 1;
        loadPickerData<%=rnd%>();
    };

    document.getElementById('searchInputPicker<%=rnd%>').addEventListener("keydown", (e) => {
        if (e.key === "Enter") { e.preventDefault(); terapkanPencarian<%=rnd%>(); }
    });

    const gantiHalaman<%=rnd%> = (direction) => {
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadPickerData<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages<%=rnd%>) {
            currentPage<%=rnd%>++;
            loadPickerData<%=rnd%>();
        }
    };

    const handleCheck<%=rnd%> = (checkbox) => {
        if(checkbox.checked) {
            selectedIds<%=rnd%>.add(checkbox.value.toString());
        } else {
            selectedIds<%=rnd%>.delete(checkbox.value.toString());
        }
        updateCheckAllState<%=rnd%>();
    };

    const toggleAllPicker<%=rnd%> = (source) => {
        const checkboxes = document.querySelectorAll('.chk-item-picker<%=rnd%>');
        checkboxes.forEach(chk => {
            chk.checked = source.checked;
            handleCheck<%=rnd%>(chk);
        });
    };

    const updateCheckAllState<%=rnd%> = () => {
        const checkboxes = document.querySelectorAll('.chk-item-picker<%=rnd%>');
        const checkAllBtn = document.getElementById('chkAll<%=rnd%>');
        if(checkboxes.length === 0) {
            checkAllBtn.checked = false;
            return;
        }
        let allChecked = true;
        checkboxes.forEach(chk => { if(!chk.checked) allChecked = false; });
        checkAllBtn.checked = allChecked;
    };

    const submitPicker<%=rnd%> = () => {
        const selectedArray = Array.from(selectedIds<%=rnd%>);
        if(typeof window['<%=callback%>'] === 'function') {
            window['<%=callback%>'](selectedArray);
        } else {
            console.error("Fungsi callback <%=callback%> tidak ditemukan di scope window.");
        }
        closePickerModal<%=rnd%>();
    };

    const closePickerModal<%=rnd%> = () => {
        const modalEl = document.getElementById('tbodyPicker<%=rnd%>').closest('.modal');
        if(modalEl) {
            bootstrap.Modal.getInstance(modalEl).hide();
            setTimeout(() => modalEl.remove(), 500);
        }
    };

    loadPickerData<%=rnd%>();
</script>