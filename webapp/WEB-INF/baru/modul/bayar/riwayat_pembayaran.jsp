<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.*"%>
<%@page import="java.util.*"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        out.print("<div class='alert alert-danger'>Sesi Anda telah berakhir.</div>");
        return;
    }

    boolean edit = false;
    boolean add = false;
    boolean delete = false;
    boolean isAdmin = false;

    if(tbmuser != null && tbmuser.getUserId() != null){
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
        add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser);
        if(Common.getApakahAdminLain(tbmuser)){
            edit = true; delete = true; add = true; isAdmin = true;
        }
    }

    String paramHanyaSiswa = request.getParameter("hanya_untuk_siswa");
    boolean isParamHanyaSiswaSet = paramHanyaSiswa != null && !paramHanyaSiswa.trim().isEmpty();
    boolean valHanyaSiswa = "true".equalsIgnoreCase(paramHanyaSiswa);

    String rndRyg = Common.getGeneratedBarCode(8);
    int currTahunRyg = WaktuUtil.getCalendar().get(Calendar.YEAR);

    List<Sekolah> listSekolahRyg = new ArrayList<Sekolah>();
    List<Yayasan> listYayasanRyg = new ArrayList<Yayasan>();
    Session sessFilterRyg = HibernateUtil.openSession();
    try {
        org.hibernate.Criteria cSek = sessFilterRyg.createCriteria(Sekolah.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
        if(tbmuser.ambilSekolah() != null) cSek.add(Restrictions.eq("id", tbmuser.ambilSekolah().getId()));
        else if(tbmuser.ambilYayasan() != null) cSek.add(Restrictions.eq("yayasan.id", tbmuser.ambilYayasan().getId()));
        listSekolahRyg = ConstantValues.simpleList(cSek, Sekolah.class);
        
        org.hibernate.Criteria cYay = sessFilterRyg.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama"));
        if(tbmuser.ambilYayasan() != null) cYay.add(Restrictions.eq("id", tbmuser.ambilYayasan().getId()));
        listYayasanRyg = ConstantValues.simpleList(cYay, Yayasan.class);
    } finally {
        if(sessFilterRyg != null) sessFilterRyg.close();
    }
%>

<div class="card border-0 shadow-sm rounded-4 bg-white animate__animated animate__fadeIn">
    <div class="card-header bg-white border-bottom p-4 d-flex justify-content-between align-items-center">
        <h5 class="fw-bold text-dark mb-0"><i class="fas fa-search me-2 text-primary"></i><%=Common.getBahasaConfig("Filter Pencarian Riwayat")%></h5>
        <button class="btn btn-success btn-sm fw-bold shadow-sm" onclick="exportRiwayatGlobalCSV<%=rndRyg%>()">
            <i class="fas fa-file-excel me-2"></i><%=Common.getBahasaConfig("Export Excel")%>
        </button>
    </div>
    
    <div class="card-body p-4 bg-light border-bottom">
        <div class="row g-3">
            <div class="col-md-3">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Yayasan")%></label>
                <select class="form-select shadow-sm filter-enter-<%=rndRyg%>" id="rygYayasan<%=rndRyg%>" onchange="filterRygYayasan<%=rndRyg%>()">
                    <option value="0"><%=Common.getBahasaConfig("-- Semua Yayasan --")%></option>
                    <% for(Yayasan y : listYayasanRyg) { %>
                        <option value="<%=y.getId()%>"><%=y.getNama()%></option>
                    <% } %>
                </select>
            </div>
            <div class="col-md-3">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Sekolah")%></label>
                <select class="form-select shadow-sm filter-enter-<%=rndRyg%>" id="rygSekolah<%=rndRyg%>">
                    <option value="0"><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>
                    <% for(Sekolah s : listSekolahRyg) { %>
                        <option value="<%=s.getId()%>"><%=s.getNama()%></option>
                    <% } %>
                </select>
            </div>
            
            <% if(isParamHanyaSiswaSet) { %>
                <input type="hidden" id="rygKategori<%=rndRyg%>" value="<%=valHanyaSiswa%>">
            <% } else { %>
                <div class="col-md-2">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Kategori")%></label>
                    <select class="form-select shadow-sm filter-enter-<%=rndRyg%>" id="rygKategori<%=rndRyg%>">
                        <option value="true"><%=Common.getBahasaConfig("Siswa Aktif")%></option>
                        <option value="false"><%=Common.getBahasaConfig("Calon Siswa")%></option>
                    </select>
                </div>
            <% } %>
            
            <div class="col-md-4">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Cari Pelanggan (NIS / Nama)")%></label>
                <input type="text" class="form-control shadow-sm filter-enter-<%=rndRyg%>" id="rygPelanggan<%=rndRyg%>" placeholder="<%=Common.getBahasaConfig("Ketik di sini...")%>">
            </div>
            
            <div class="col-md-3">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Mulai Tanggal Transaksi")%></label>
                <input type="date" class="form-control shadow-sm filter-enter-<%=rndRyg%>" id="rygStart<%=rndRyg%>">
            </div>
            <div class="col-md-3">
                <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Sampai Tanggal Transaksi")%></label>
                <input type="date" class="form-control shadow-sm filter-enter-<%=rndRyg%>" id="rygEnd<%=rndRyg%>">
            </div>
            <div class="col-md-6">
                <div class="row g-2">
                    <div class="col-md-6">
                        <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Cari Item Biaya")%></label>
                        <input type="text" class="form-control shadow-sm filter-enter-<%=rndRyg%>" id="rygItem<%=rndRyg%>" placeholder="<%=Common.getBahasaConfig("Nama / Kode Biaya...")%>">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Bulan")%></label>
                        <select class="form-select shadow-sm filter-enter-<%=rndRyg%>" id="rygBulan<%=rndRyg%>">
                            <option value="0"><%=Common.getBahasaConfig("Semua")%></option>
                            <% for(int i=1; i<=12; i++) { %>
                                <option value="<%=i%>"><%=Common.BULAN[i-1]%></option>
                            <% } %>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Tahun")%></label>
                        <select class="form-select shadow-sm filter-enter-<%=rndRyg%>" id="rygTahun<%=rndRyg%>">
                            <option value="0"><%=Common.getBahasaConfig("Semua")%></option>
                            <% for(int i=currTahunRyg-5; i<=currTahunRyg+5; i++) { %>
                                <option value="<%=i%>"><%=i%></option>
                            <% } %>
                        </select>
                    </div>
                </div>
            </div>
            
            <div class="col-12 d-flex justify-content-end gap-2 mt-4 border-top pt-3">
                <button class="btn btn-light border fw-bold px-4 shadow-sm" onclick="resetFilterRyg<%=rndRyg%>()"><i class="fas fa-sync-alt me-2"></i><%=Common.getBahasaConfig("Reset")%></button>
                <button class="btn btn-primary fw-bold px-5 shadow-sm" onclick="applyFilterRyg<%=rndRyg%>()"><i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Tampilkan Data")%></button>
            </div>
        </div>
    </div>

    <div class="card-body p-0" id="containerRiwayatGlobal<%=rndRyg%>">
        <div class="text-center py-5 text-muted">
            <i class="fas fa-filter fa-4x mb-3 opacity-25"></i><br>
            <%=Common.getBahasaConfig("Silakan gunakan filter dan tekan 'Tampilkan Data' untuk melihat riwayat.")%>
        </div>
    </div>

    <div class="card-footer bg-light border-top p-3 d-flex justify-content-between align-items-center" id="paginationRyg<%=rndRyg%>" style="display:none !important;">
        <span class="small fw-bold text-secondary" id="rygPageInfo<%=rndRyg%>"></span>
        <div class="btn-group shadow-sm">
            <button class="btn btn-sm btn-outline-primary fw-bold px-3" id="btnPrevRyg<%=rndRyg%>" onclick="changePageRyg<%=rndRyg%>(-1)"><i class="fas fa-chevron-left me-1"></i><%=Common.getBahasaConfig("Prev")%></button>
            <button class="btn btn-sm btn-outline-primary fw-bold px-3" id="btnNextRyg<%=rndRyg%>" onclick="changePageRyg<%=rndRyg%>(1)"><%=Common.getBahasaConfig("Next")%><i class="fas fa-chevron-right ms-1"></i></button>
        </div>
    </div>
</div>

<script>
    var pDeleteRyg<%=rndRyg%> = <%=delete%>;
    var rygPage<%=rndRyg%> = 1;
    var rygTotalPages<%=rndRyg%> = 1;
    var isRiwayatGlobalLoaded = false;
    var arrRiwayatRawData<%=rndRyg%> = [];

    var listSekolahLengkapRyg<%=rndRyg%> = [
        <% for(Sekolah s : listSekolahRyg) { %>
            {id: <%=s.getId()%>, nama: '<%=s.getNama().replace("'", "\\'")%>', yayasanId: <%=s.getYayasan() != null ? s.getYayasan().getId() : "null"%>},
        <% } %>
    ];

    var formatRpRyg<%=rndRyg%> = function(angka) {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka || 0);
    };

    var filterRygYayasan<%=rndRyg%> = function() {
        const yayasanId = document.getElementById('rygYayasan<%=rndRyg%>').value;
        const selSekolah = document.getElementById('rygSekolah<%=rndRyg%>');
        selSekolah.innerHTML = '<option value="0"><%=Common.getBahasaConfig("-- Semua Sekolah --")%></option>';
        
        listSekolahLengkapRyg<%=rndRyg%>.forEach(s => {
            if(yayasanId == "0" || s.yayasanId == yayasanId) {
                selSekolah.innerHTML += '<option value="' + s.id + '">' + s.nama + '</option>';
            }
        });
    };

    var initFiltersRyg<%=rndRyg%> = function() {
        let dEnd = new Date();
        let dStart = new Date();
        dStart.setFullYear(dStart.getFullYear() - 3);
        document.getElementById('rygStart<%=rndRyg%>').valueAsDate = dStart;
        document.getElementById('rygEnd<%=rndRyg%>').valueAsDate = dEnd;

        var selYayasan = document.getElementById('rygYayasan<%=rndRyg%>');
        var selSekolah = document.getElementById('rygSekolah<%=rndRyg%>');
        
        if (selYayasan && selYayasan.options.length === 2) {
            selYayasan.selectedIndex = 1;
            selYayasan.disabled = true;
            filterRygYayasan<%=rndRyg%>();
        }
        if (selSekolah && selSekolah.options.length === 2) {
            selSekolah.selectedIndex = 1;
            selSekolah.disabled = true;
        }

        // Action Listener untuk tombol Enter
        var filterInputs = document.querySelectorAll('.filter-enter-<%=rndRyg%>');
        filterInputs.forEach(function(el) {
            el.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') { applyFilterRyg<%=rndRyg%>(); }
            });
        });

        applyFilterRyg<%=rndRyg%>();
    };

    var applyFilterRyg<%=rndRyg%> = async function() {
        document.getElementById('containerRiwayatGlobal<%=rndRyg%>').innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary" style="width: 3rem; height: 3rem;"></div></div>';
        document.getElementById('paginationRyg<%=rndRyg%>').setAttribute('style', 'display:none !important;');
        
        const payload = {
            action: 'loadRiwayatGlobal',
            idYayasan: document.getElementById('rygYayasan<%=rndRyg%>').value,
            idSekolah: document.getElementById('rygSekolah<%=rndRyg%>').value,
            isSiswa: (document.getElementById('rygKategori<%=rndRyg%>').value === 'true'),
            keywordPelanggan: document.getElementById('rygPelanggan<%=rndRyg%>').value,
            keywordItem: document.getElementById('rygItem<%=rndRyg%>').value, 
            startDate: document.getElementById('rygStart<%=rndRyg%>').value,
            endDate: document.getElementById('rygEnd<%=rndRyg%>').value,
            bulan: document.getElementById('rygBulan<%=rndRyg%>').value,
            tahun: document.getElementById('rygTahun<%=rndRyg%>').value,
            page: rygPage<%=rndRyg%>
        };

        try {
            const res = await fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayar&s=_pembayaran_online_services', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            });
            const result = await res.json();
            
            if(result && result.status === '00') {
                arrRiwayatRawData<%=rndRyg%> = result.data || [];
                rygTotalPages<%=rndRyg%> = result.totalPages || 1;
                renderTableRyg<%=rndRyg%>(result.data || [], result.total || 0);
            } else {
                document.getElementById('containerRiwayatGlobal<%=rndRyg%>').innerHTML = '<div class="alert alert-danger text-center m-4"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><br>Gagal memuat riwayat.</div>';
            }
        } catch (e) {
            document.getElementById('containerRiwayatGlobal<%=rndRyg%>').innerHTML = '<div class="alert alert-danger text-center m-4"><i class="fas fa-wifi fa-2x mb-2"></i><br>Koneksi ke peladen terputus.</div>';
        }
    };

    var resetFilterRyg<%=rndRyg%> = function() {
        document.getElementById('rygPelanggan<%=rndRyg%>').value = '';
        document.getElementById('rygItem<%=rndRyg%>').value = '';
        document.getElementById('rygBulan<%=rndRyg%>').value = '0';
        document.getElementById('rygTahun<%=rndRyg%>').value = '0';
        
        var selYayasan = document.getElementById('rygYayasan<%=rndRyg%>');
        if(!selYayasan.disabled) selYayasan.value = '0';
        
        var selSekolah = document.getElementById('rygSekolah<%=rndRyg%>');
        if(!selSekolah.disabled) {
            filterRygYayasan<%=rndRyg%>();
            selSekolah.value = '0';
        }
        
        let dEnd = new Date();
        let dStart = new Date();
        dStart.setFullYear(dStart.getFullYear() - 3);
        document.getElementById('rygStart<%=rndRyg%>').valueAsDate = dStart;
        document.getElementById('rygEnd<%=rndRyg%>').valueAsDate = dEnd;

        rygPage<%=rndRyg%> = 1;
        applyFilterRyg<%=rndRyg%>();
    };

    var changePageRyg<%=rndRyg%> = function(dir) {
        if (dir === -1 && rygPage<%=rndRyg%> > 1) { rygPage<%=rndRyg%>--; applyFilterRyg<%=rndRyg%>(); }
        else if (dir === 1 && rygPage<%=rndRyg%> < rygTotalPages<%=rndRyg%>) { rygPage<%=rndRyg%>++; applyFilterRyg<%=rndRyg%>(); }
    };

    var exportRiwayatGlobalCSV<%=rndRyg%> = function() {
        if (!arrRiwayatRawData<%=rndRyg%> || arrRiwayatRawData<%=rndRyg%>.length === 0) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Tidak ada data untuk di-export.")%>', 'bg-warning text-dark');
            return;
        }

        var csv = '\uFEFF'; 
        csv += 'Tanggal,Sekolah,NIS,Nama Pelanggan,Metode,Bulan Tagihan,Item Biaya,Nominal\n';
        
        arrRiwayatRawData<%=rndRyg%>.forEach(function(row) {
            var skl = '"' + String(row.sekolah || '').replace(/"/g, '""') + '"';
            var nma = '"' + String(row.namaSiswa || '').replace(/"/g, '""') + '"';
            var itm = '"' + String(row.item || '').replace(/"/g, '""') + '"';
            
            csv += row.waktu + ',' + skl + ',' + row.nis + ',' + nma + ',' + row.via + ',' + row.bln + ',' + itm + ',' + row.nominal + '\n';
        });

        var blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        var link = document.createElement("a");
        var url = URL.createObjectURL(blob);
        link.setAttribute("href", url);
        link.setAttribute("download", "Riwayat_Global_Pembayaran.csv");
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    var renderTableRyg<%=rndRyg%> = function(dataArr, totalData) {
        const container = document.getElementById('containerRiwayatGlobal<%=rndRyg%>');
        const paginator = document.getElementById('paginationRyg<%=rndRyg%>');
        
        if(!dataArr || dataArr.length === 0) {
            container.innerHTML = '<div class="text-center py-5 text-muted"><i class="fas fa-folder-open fa-4x mb-3 opacity-25"></i><h5 class="fw-bold mt-2"><%=Common.getBahasaConfig("Tidak Ada Data")%></h5><p><%=Common.getBahasaConfig("Tidak ada transaksi untuk filter ini.")%></p></div>';
            paginator.setAttribute('style', 'display: none !important;');
            return;
        }

        let groupedRiwayat = [];
        let currentGroupId = null;
        let currentGroupObj = null;

        dataArr.forEach(r => {
            if(r.groupId !== currentGroupId) {
                currentGroupObj = {
                    groupId: r.groupId, waktu: r.waktu, via: r.via, idPembayaran: r.idPembayaran,
                    nis: r.nis, namaSiswa: r.namaSiswa, sekolah: r.sekolah, foto: r.foto, bisaEdit: r.bisaEdit,
                    totalNominal: 0, items: []
                };
                groupedRiwayat.push(currentGroupObj);
                currentGroupId = r.groupId;
            }
            currentGroupObj.totalNominal += r.nominal;
            currentGroupObj.items.push(r);
        });

        let html = '<div class="table-responsive"><table class="table table-hover align-middle mb-0"><thead class="table-light shadow-sm" style="z-index: 1;"><tr>' +
                   '<th style="width: 50px;">' + '<%=Common.getBahasaConfigJS("Foto")%>' + '</th>' +
                   '<th style="width: 25%;">' + '<%=Common.getBahasaConfigJS("Pelanggan (NIS & Nama)")%>' + '</th>' +
                   '<th>' + '<%=Common.getBahasaConfigJS("Sekolah / Detail Item Biaya")%>' + '</th>' +
                   '<th style="width: 20%;">' + '<%=Common.getBahasaConfigJS("Waktu & Metode")%>' + '</th>' +
                   '<th class="text-end" style="width: 15%;">' + '<%=Common.getBahasaConfigJS("Nominal")%>' + '</th>' +
                   '<th class="text-center" style="width: 90px;"><i class="fas fa-cogs"></i></th>' +
                   '</tr></thead><tbody>';

        groupedRiwayat.forEach(g => {
            let actionHtml = '<button class="btn btn-sm btn-outline-secondary rounded-circle shadow-sm" onclick="cetakStrukRyg<%=rndRyg%>(' + g.idPembayaran + ')" title="' + '<%=Common.getBahasaConfigJS("Cetak Struk")%>' + '"><i class="fas fa-print"></i></button>';
            if (pDeleteRyg<%=rndRyg%>) {
                actionHtml += ' <button class="btn btn-sm btn-outline-danger rounded-circle shadow-sm ms-1" onclick="hapusRiwayatRyg<%=rndRyg%>(' + g.idPembayaran + ')" title="' + '<%=Common.getBahasaConfigJS("Hapus Transaksi")%>' + '"><i class="fas fa-trash-alt"></i></button>';
            }

            html += '<tr class="bg-primary bg-opacity-10 border-bottom border-primary border-opacity-25">' +
                '<td class="text-center"><img src="' + g.foto + '" class="rounded-circle shadow-sm border bg-white" style="width: 45px; height: 45px; object-fit: cover;"></td>' +
                '<td>' + 
                    '<span class="fw-bold text-dark d-block">' + g.namaSiswa + '</span>' +
                    '<span class="small text-muted"><i class="fas fa-id-card me-1"></i>' + g.nis + '</span>' +
                '</td>' +
                '<td><span class="badge bg-secondary border shadow-sm">' + g.sekolah + '</span></td>' +
                '<td>' +
                    '<div class="fw-bold text-dark"><i class="fas fa-calendar-check text-primary me-2"></i>' + g.waktu + '</div>' +
                    '<div class="mt-1"><span class="badge bg-info text-dark shadow-sm"><i class="fas fa-wallet me-1"></i>' + g.via + '</span></div>' +
                '</td>' +
                '<td class="text-end fw-bold text-success fs-6">' + formatRpRyg<%=rndRyg%>(g.totalNominal) + '</td>' +
                '<td class="text-center text-nowrap">' + actionHtml + '</td>' +
                '</tr>';

            g.items.forEach(r => {
                let nominalHtml = '<div class="fw-bold text-success text-nowrap">' + formatRpRyg<%=rndRyg%>(r.nominal) + '</div>';
                if(r.nominalCoret > 0.01) {
                    nominalHtml += '<div class="small text-muted text-decoration-line-through text-nowrap">' + formatRpRyg<%=rndRyg%>(r.nominalCoret) + '</div>';
                }

                html += '<tr>' +
                    '<td></td>' +
                    '<td class="text-nowrap fw-medium text-secondary ps-4"><i class="fas fa-angle-right me-2 text-muted"></i>' + r.bln + '</td>' +
                    '<td colspan="2">' +
                        '<span class="badge bg-light text-secondary border mb-1">' + r.pengaturanBiaya + '</span><br>' + 
                        '<span class="fw-medium text-dark">' + r.item + '</span>' + 
                    '</td>' +
                    '<td class="text-end">' + nominalHtml + '</td>' +
                    '<td></td>' +
                    '</tr>';
            });
        });

        html += '</tbody></table></div>';
        container.innerHTML = html;
        
        paginator.setAttribute('style', 'display: flex !important;');
        document.getElementById('rygPageInfo<%=rndRyg%>').innerText = 'Halaman ' + rygPage<%=rndRyg%> + ' dari ' + rygTotalPages<%=rndRyg%> + ' (' + totalData + ' Transaksi)';
        document.getElementById('btnPrevRyg<%=rndRyg%>').disabled = rygPage<%=rndRyg%> <= 1;
        document.getElementById('btnNextRyg<%=rndRyg%>').disabled = rygPage<%=rndRyg%> >= rygTotalPages<%=rndRyg%>;
    };

    var cetakStrukRyg<%=rndRyg%> = async function(idPembayaran) {
        tampilkanToast('<%=Common.getBahasaConfigJS("Menyiapkan dokumen struk...")%>', 'bg-info text-white');
        try {
            const res = await fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=bayar&s=_pembayaran_online_services', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: 'cetakStrukUlang', idPembayaran: idPembayaran })
            });
            const textData = await res.text();
            const scriptContent = textData.replace(/<script>/gi, '').replace(/<\/script>/gi, '');
            const scriptEl = document.createElement('script');
            scriptEl.text = scriptContent;
            document.body.appendChild(scriptEl);
        } catch (e) {
            tampilkanToast('<%=Common.getBahasaConfigJS("Gagal mencetak ulang struk.")%>', 'bg-danger text-white');
        }
    };

    var hapusRiwayatRyg<%=rndRyg%> = function(idPembayaran) {
        if(typeof prosesDeleteData === 'function') {
            prosesDeleteData('ais.database.model.sekolah.PembayaranSiswa', idPembayaran, function() {
                tampilkanToast('<%=Common.getBahasaConfigJS("Riwayat pembayaran berhasil dihapus.")%>', 'bg-success text-white');
                applyFilterRyg<%=rndRyg%>();
            });
        } else {
            tampilkanToast('<%=Common.getBahasaConfigJS("Fungsi prosesDeleteData tidak ditemukan dalam sistem.")%>', 'bg-danger text-white');
        }
    };

    window.triggerLazyLoadRiwayatGlobal = function() {
        if(!isRiwayatGlobalLoaded) {
            initFiltersRyg<%=rndRyg%>();
            isRiwayatGlobalLoaded = true;
        }
    };
</script>