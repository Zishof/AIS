<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page isELIgnored="true"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi berakhir.") + "\"}");
        return;
    }
    String rnd = Common.getGeneratedBarCode(7);
    boolean canAdd = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser) || Common.getApakahAdminLain(tbmuser);
    boolean canEdit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser) || Common.getApakahAdminLain(tbmuser);

    String pageTitle = Common.getBahasaConfig("Semua Mahasiswa");
    String searchPh = Common.getBahasaConfig("Cari NIM / Nama...");
    
    // FORMAT BARU: Label | Alignment | Width | Kolom Database (Untuk Sorting)
    String tableHeaders = 
        Common.getBahasaConfig("Foto") + " | text-center | 70px | ," +
        Common.getBahasaConfig("NIM") + " | text-start | | nim," +
        Common.getBahasaConfig("Nama Mahasiswa") + " | text-start | 250px | nama," +
        Common.getBahasaConfig("Prodi") + " | text-start | | jurusan.nama," +
        Common.getBahasaConfig("Angkatan") + " | text-center | | tahunangkatan," +
        Common.getBahasaConfig("Status") + " | text-center | | aktif";

    JSONObject lk = new JSONObject(); lk.put("id", "Laki-laki"); lk.put("nama", "Laki-laki");
    JSONObject pr = new JSONObject(); pr.put("id", "Perempuan"); pr.put("nama", "Perempuan");
    JSONArray jsonKelamin = new JSONArray(); jsonKelamin.put(lk); jsonKelamin.put(pr);

    String searchColsConfig = "nim || nama || tahunangkatan || program || jurusan || kelamin;" + jsonKelamin.toString() + " || aktif";
%>

<jsp:include page="/WEB-INF/baru/modul/common/_table_component.jsp">
    <jsp:param name="rnd" value="<%=rnd%>" />
    <jsp:param name="title" value="<%=pageTitle%>" />
    <jsp:param name="searchPlaceholder" value="<%=searchPh%>" />
    <jsp:param name="canAdd" value="<%=String.valueOf(canAdd)%>" />
    <jsp:param name="canExport" value="true" />
    <jsp:param name="headers" value="<%=tableHeaders%>" />
    <jsp:param name="searchCols" value="<%=searchColsConfig%>" />
    <jsp:param name="modelClass" value="<%=Mahasiswa.class.getName()%>" />
</jsp:include>

<jsp:include page="_form.jsp">
    <jsp:param name="rnd" value="<%=rnd%>" />
    <jsp:param name="canAdd" value="<%=String.valueOf(canAdd)%>" />
    <jsp:param name="canEdit" value="<%=String.valueOf(canEdit)%>" />
    <jsp:param name="masterClass" value="<%=Mahasiswa.class.getName()%>" />
</jsp:include>

<script>
    const masterClass<%=rnd%> = "<%=Mahasiswa.class.getName()%>";
    var currentPage<%=rnd%> = 1;
    const limit<%=rnd%> = 10;
    var totalRecords<%=rnd%> = 0;
    
    // VARIABEL DEFAULT SORTING
    var sortCol<%=rnd%> = "id"; 
    var sortOrder<%=rnd%> = "desc";

    const downloadExcelData<%=rnd%> = () => { alert('<%= Common.getBahasaConfigJS("Fitur ekspor Excel akan segera dipanggil.") %>'); };

    // --- SORTING LOGIC ---
    const changeSort<%=rnd%> = (colName) => {
        if (sortCol<%=rnd%> === colName) {
            // Jika klik kolom yang sama, balik urutannya
            sortOrder<%=rnd%> = (sortOrder<%=rnd%> === 'asc') ? 'desc' : 'asc';
        } else {
            // Jika klik kolom baru, set default ke asc
            sortCol<%=rnd%> = colName;
            sortOrder<%=rnd%> = 'asc';
        }
        updateSortIcons<%=rnd%>();
        currentPage<%=rnd%> = 1; // Reset ke halaman 1 saat sort berubah
        loadData<%=rnd%>();
    };

    const updateSortIcons<%=rnd%> = () => {
        // Reset semua icon jadi panah abu-abu
        document.querySelectorAll('[id^="sortIcon_"]').forEach(el => {
            el.innerHTML = '<i class="fas fa-sort text-muted opacity-50"></i>';
        });
        // Beri panah atas/bawah pada kolom yang aktif
        const activeIcon = document.getElementById('sortIcon_' + sortCol<%=rnd%> + '_<%=rnd%>');
        if (activeIcon) {
            activeIcon.innerHTML = sortOrder<%=rnd%> === 'asc' ? '<i class="fas fa-sort-up text-primary"></i>' : '<i class="fas fa-sort-down text-primary"></i>';
        }
    };

    // --- PAGING LOGIC ---
    const goToPage<%=rnd%> = (page) => { currentPage<%=rnd%> = page; loadData<%=rnd%>(); };
    const changePage<%=rnd%> = (nav) => { currentPage<%=rnd%> += nav; loadData<%=rnd%>(); };

    const updatePaging<%=rnd%> = () => {
        var totalPage = Math.ceil(totalRecords<%=rnd%> / limit<%=rnd%>);
        var pagingInfo = document.getElementById('pagingInfo<%=rnd%>');
        if (pagingInfo) pagingInfo.innerText = 'Menampilkan ' + totalRecords<%=rnd%> + ' data (' + totalPage + ' Halaman)';
        
        var ulContainer = document.getElementById('paginationUl<%=rnd%>');
        if (ulContainer) {
            var htmlPaging = '';
            var batasTampil = 5;
            var startPage = Math.max(1, currentPage<%=rnd%> - batasTampil);
            var endPage = Math.min(totalPage, currentPage<%=rnd%> + batasTampil);
            
            if (startPage > 1) {
                htmlPaging += '<li class="page-item"><button class="page-link border-0 text-muted" onclick="goToPage<%=rnd%>(1)">1</button></li>';
                if (startPage > 2) htmlPaging += '<li class="page-item disabled"><span class="page-link border-0 bg-transparent text-muted">...</span></li>';
            }
            for (var p = startPage; p <= endPage; p++) {
                if (p === currentPage<%=rnd%>) htmlPaging += '<li class="page-item active"><span class="page-link border-0 fw-bold shadow-sm">' + p + '</span></li>';
                else htmlPaging += '<li class="page-item"><button class="page-link border-0 text-dark" onclick="goToPage<%=rnd%>(' + p + ')">' + p + '</button></li>';
            }
            if (endPage < totalPage) {
                if (endPage < totalPage - 1) htmlPaging += '<li class="page-item disabled"><span class="page-link border-0 bg-transparent text-muted">...</span></li>';
                htmlPaging += '<li class="page-item"><button class="page-link border-0 text-muted" onclick="goToPage<%=rnd%>(' + totalPage + ')">' + totalPage + '</button></li>';
            }
            ulContainer.innerHTML = htmlPaging;
        }
        document.getElementById('btnPrev<%=rnd%>').disabled = (currentPage<%=rnd%> <= 1);
        document.getElementById('btnNext<%=rnd%>').disabled = (currentPage<%=rnd%> >= totalPage || totalPage === 0);
    };

    const loadData<%=rnd%> = async () => {
        const keyword = document.getElementById('searchData<%=rnd%>').value.trim();
        const tbody = document.getElementById('tabelData<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="10" class="text-center py-5"><div class="spinner-border text-primary"></div></td></tr>';
        
        const reqObj = {
            action: "daftar", class: masterClass<%=rnd%>, deep: "1",
            max: limit<%=rnd%>, halaman: (currentPage<%=rnd%> - 1),
            // UPDATE: Menggunakan variabel sorting dinamis
            order1: sortOrder<%=rnd%>, sort1: sortCol<%=rnd%>, count: "true"
        };
        
        let finalWhere = [];
        if (keyword) finalWhere.push("(nim ILIKE '%" + keyword + "%' OR nama ILIKE '%" + keyword + "%')");
        if (typeof getSearchCriteria<%=rnd%> === 'function') {
            let advCriteria = getSearchCriteria<%=rnd%>();
            if (advCriteria) finalWhere.push(advCriteria);
        }
        if (finalWhere.length > 0) reqObj.where1 = finalWhere.join(' AND ');

        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(reqObj)
            });
            const result = await res.json();
            totalRecords<%=rnd%> = parseInt(result.count || 0);
            
            let html = "";
            (result.data || []).forEach((row, i) => {
                var noUrut = (currentPage<%=rnd%> - 1) * limit<%=rnd%> + (i + 1);
                var defaultAvatar = '<%=Common.ROOT%>/img/administrator-icon_default.png';
                var thumbUrl = row.foto_kecil ? row.foto_kecil : defaultAvatar;
                var previewUrl = row.foto_besar ? row.foto_besar : (row.foto_kecil ? row.foto_kecil : defaultAvatar);
                
                var imgHtml = '<div class="avatar avatar-xl">' +
                                '<img src="' + thumbUrl + '" class="rounded-3 shadow-sm" style="object-fit: cover; cursor: pointer;" title="Klik untuk memperbesar" ' +
                                'onclick="bukaPreviewFoto<%=rnd%>(\'' + previewUrl + '\', \'' + (row.nama || '-').replace(/'/g, "\\'") + '\')" onerror="this.src=\'' + defaultAvatar + '\'">' +
                              '</div>';

                var badge = (row.aktif === true || row.aktif === "true") ? '<small class="badge rounded bg-success text-white">Aktif</small>' : '<small class="badge rounded bg-danger text-white">Non-Aktif</small>';

                html += '<tr>' +
                    '<td class="align-middle fs-0 py-3 text-center">' + noUrut + '</td>' +
                    '<td class="align-middle text-center">' + imgHtml + '</td>' +
                    '<td class="align-middle fw-bold">' + (row.nim || '-') + '</td>' +
                    '<td class="align-middle text-900">' + (row.nama || '-') + '</td>' +
                    '<td class="align-middle">' + (row["jurusan.nama"] || (row.jurusan ? row.jurusan.nama : '-')) + '</td>' +
                    '<td class="align-middle text-center">' + (row.tahunangkatan || '-') + '</td>' +
                    '<td class="align-middle text-center">' + badge + '</td>' +
                    '<td class="align-middle text-end pe-4 text-nowrap">' +
                        '<button class="btn btn-sm btn-falcon-default text-warning me-1" onclick="editData<%=rnd%>(' + row.id + ')" title="Edit"><i class="fas fa-edit"></i></button>' +
                        '<button class="btn btn-sm btn-falcon-default text-danger" onclick="hapusData<%=rnd%>(' + row.id + ')" title="Hapus"><i class="fas fa-trash"></i></button>' +
                    '</td>' +
                '</tr>';
            });
            tbody.innerHTML = html || '<tr><td colspan="10" class="text-center py-4 text-muted">Data tidak ditemukan.</td></tr>';
            updatePaging<%=rnd%>();
        } catch (e) { console.error(e); }
    };

    const hapusData<%=rnd%> = async (id) => {
        if(confirm('<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data ini?") %>')) {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({action:"hapus", class:masterClass<%=rnd%>, id:id})
            });
            const result = await res.json();
            if(result.status === '00' || result.status === 'success') loadData<%=rnd%>();
            else alert('<%= Common.getBahasaConfigJS("Data gagal dihapus. Silakan coba lagi.") %>');
        }
    };

    document.getElementById('searchData<%=rnd%>').addEventListener("keydown", (e) => {
        if (e.key === "Enter") { e.preventDefault(); currentPage<%=rnd%> = 1; loadData<%=rnd%>(); }
    });

    document.addEventListener("DOMContentLoaded", () => { 
        updateSortIcons<%=rnd%>(); // Jalankan set icon awal
        loadData<%=rnd%>(); 
    });
</script>