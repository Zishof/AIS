<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
String rndDash = Common.getGeneratedBarCode(7);

// Asumsi method mapping hak akses pada Tbmuser untuk environment Sekolah
Yayasan loginSebagaiYayasan = null; // tbmuser.getYayasan();
Sekolah loginSebagaiSekolah = null; // tbmuser.getSekolah();
Guru loginSebagaiGuru = null; // tbmuser.getGuru();

Long idGuruLogin = (loginSebagaiGuru != null) ? loginSebagaiGuru.getId() : null;
Long idYayasanLogin = null;
Long idSekolahLogin = null;

if (loginSebagaiSekolah != null) {
	idSekolahLogin = loginSebagaiSekolah.getId();
	if (loginSebagaiSekolah.getYayasan() != null) {
		idYayasanLogin = loginSebagaiSekolah.getYayasan().getId();
	}
} else if (loginSebagaiYayasan != null) {
	idYayasanLogin = loginSebagaiYayasan.getId();
}

// Susun Base Where SQL agar data dashboard aman sesuai Hak Akses
String baseWhere = " WHERE p.id IS NOT NULL ";
if (idGuruLogin != null) {
    baseWhere += " AND p.guru_id = " + idGuruLogin;
} else if (idSekolahLogin != null) {
    baseWhere += " AND p.sekolah_id = " + idSekolahLogin;
} else if (idYayasanLogin != null) {
    baseWhere += " AND p.yayasan_id = " + idYayasanLogin;
}
%>

<div id="wrapperDashboard<%=rndDash%>"
	class="animate__animated animate__fadeIn">

	<div
		class="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-3 border-bottom border-light gap-3">
		<h5 class="fw-bold text-dark mb-0">
			<i class="fas fa-chart-pie text-primary me-2"></i><%=Common.getBahasaConfig("Statistik & Rekapitulasi Prestasi Guru")%>
		</h5>

		<div class="d-flex align-items-center gap-2 flex-wrap">
			<select id="dashFilterTA<%=rndDash%>"
				class="form-select form-select-sm shadow-sm" style="width: auto;">
				<option value=""><%=Common.getBahasaConfig("-- Semua TP --")%></option>
				<%
				if (Common.tahunAngkatans != null) {
					for (String ta : Common.tahunAngkatans) {
				%>
				<option value="<%=ta%>"><%=ta%></option>
				<%
				}
				}
				%>
			</select> <select id="dashFilterSmt<%=rndDash%>"
				class="form-select form-select-sm shadow-sm" style="width: auto;">
				<option value=""><%=Common.getBahasaConfig("-- Semua SMT --")%></option>
				<option value="<%=Perkuliahan.GANJIL%>"><%=Common.getBahasaConfig("Ganjil")%></option>
				<option value="<%=Perkuliahan.GENAP%>"><%=Common.getBahasaConfig("Genap")%></option>
			</select>

			<button
				class="btn btn-sm btn-primary fw-bold shadow-sm rounded-pill px-4 text-nowrap"
				onclick="loadDashboard<%=rndDash%>()"
				id="btnRefreshDash<%=rndDash%>">
				<i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan")%>
			</button>
		</div>
	</div>

	<div class="row g-3 mb-4">
		<div class="col-md-3">
			<div class="card bg-primary bg-gradient text-white border-0 shadow-sm h-100 rounded-4">
				<div class="card-body p-4 d-flex justify-content-between align-items-center">
					<div>
						<h6 class="text-uppercase fw-semibold mb-1 text-white"><%=Common.getBahasaConfig("Total Prestasi")%></h6>
						<h2 class="fw-bold mb-0 text-white" id="dashTotal<%=rndDash%>">
							<i class="fas fa-spinner fa-spin"></i>
						</h2>
					</div>
					<i class="fas fa-medal fa-3x opacity-50"></i>
				</div>
			</div>
		</div>
		<div class="col-md-3">
			<div class="card bg-success bg-gradient text-white border-0 shadow-sm h-100 rounded-4">
				<div class="card-body p-4 d-flex justify-content-between align-items-center">
					<div>
						<h6 class="text-uppercase fw-semibold mb-1 text-white"><%=Common.getBahasaConfig("Disetujui")%></h6>
						<h2 class="fw-bold mb-0 text-white" id="dashDisetujui<%=rndDash%>">
							<i class="fas fa-spinner fa-spin"></i>
						</h2>
					</div>
					<i class="fas fa-check-circle fa-3x opacity-50"></i>
				</div>
			</div>
		</div>
		<div class="col-md-3">
			<div class="card bg-info bg-gradient text-white border-0 shadow-sm h-100 rounded-4">
				<div class="card-body p-4 d-flex justify-content-between align-items-center">
					<div>
						<h6 class="text-uppercase fw-semibold mb-1 text-white"><%=Common.getBahasaConfig("Sedang Diproses")%></h6>
						<h2 class="fw-bold mb-0 text-white" id="dashProses<%=rndDash%>">
							<i class="fas fa-spinner fa-spin"></i>
						</h2>
					</div>
					<i class="fas fa-sync-alt fa-3x opacity-50"></i>
				</div>
			</div>
		</div>
		<div class="col-md-3">
			<div class="card bg-warning bg-gradient text-white border-0 shadow-sm h-100 rounded-4">
				<div class="card-body p-4 d-flex justify-content-between align-items-center">
					<div>
						<h6 class="text-uppercase fw-semibold mb-1 text-white"><%=Common.getBahasaConfig("Belum Diproses")%></h6>
						<h2 class="fw-bold mb-0 text-white" id="dashBelum<%=rndDash%>">
							<i class="fas fa-spinner fa-spin"></i>
						</h2>
					</div>
					<i class="fas fa-hourglass-half fa-3x opacity-50"></i>
				</div>
			</div>
		</div>
	</div>

	<div class="row g-4 mb-4">
		<div class="col-md-8">
			<div class="card border-0 shadow-sm rounded-4 h-100">
				<div class="card-header bg-white border-0 pt-4 pb-2 px-4">
					<h6 class="fw-bold text-dark mb-0">
						<i class="fas fa-chart-line text-primary me-2"></i><%=Common.getBahasaConfig("Tren Prestasi (Disetujui) TP & Semester")%></h6>
				</div>
				<div class="card-body p-4">
					<canvas id="chartTrend<%=rndDash%>"
						style="height: 300px; width: 100%;"></canvas>
				</div>
			</div>
		</div>
		<div class="col-md-4">
			<div class="card border-0 shadow-sm rounded-4 h-100">
				<div class="card-header bg-white border-0 pt-4 pb-2 px-4">
					<h6 class="fw-bold text-dark mb-0">
						<i class="fas fa-chart-pie text-success me-2"></i><%=Common.getBahasaConfig("Kategori Prestasi")%></h6>
				</div>
				<div
					class="card-body p-4 d-flex justify-content-center align-items-center">
					<canvas id="chartKategori<%=rndDash%>" style="max-height: 250px;"></canvas>
				</div>
			</div>
		</div>
	</div>

	<div class="row g-4 mb-4">

		<div class="col-md-6">
			<div
				class="card border-0 shadow-sm rounded-4 h-100 border-top border-primary border-3">
				<div
					class="card-header bg-white border-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center">
					<h6 class="fw-bold text-dark mb-0">
						<i class="fas fa-calendar-alt text-primary me-2"></i><%=Common.getBahasaConfig("Rekap Per TP & Semester")%></h6>
					<button
						class="btn btn-sm btn-light fw-bold rounded-pill shadow-sm border"
						onclick="downloadExcel<%=rndDash%>('tblTaSmt<%=rndDash%>', 'Rekap_TP_Semester')">
						<i class="fas fa-file-excel text-success me-1"></i> Excel
					</button>
				</div>
				<div class="card-body p-4 pt-2">
					<div class="table-responsive"
						style="max-height: 250px; overflow-y: auto;">
						<table class="table table-hover table-striped align-middle border"
							id="tblTaSmt<%=rndDash%>">
							<thead class="table-dark text-center"
								style="position: sticky; top: 0; z-index: 1;">
								<tr>
									<th><%=Common.getBahasaConfig("TP - SMT")%></th>
									<th><%=Common.getBahasaConfig("Total Diajukan")%></th>
									<th><%=Common.getBahasaConfig("Disetujui")%></th>
								</tr>
							</thead>
							<tbody id="bodyTaSmt<%=rndDash%>">
								<tr>
									<td colspan="3" class="text-center"><div
											class="spinner-border spinner-border-sm text-primary"></div></td>
								</tr>
							</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>

		<div class="col-md-6">
			<div
				class="card border-0 shadow-sm rounded-4 h-100 border-top border-warning border-3">
				<div
					class="card-header bg-white border-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center">
					<h6 class="fw-bold text-dark mb-0">
						<i class="fas fa-layer-group text-warning me-2"></i><%=Common.getBahasaConfig("Rekap Per Cabang Prestasi")%></h6>
					<button
						class="btn btn-sm btn-light fw-bold rounded-pill shadow-sm border"
						onclick="downloadExcel<%=rndDash%>('tblCabang<%=rndDash%>', 'Rekap_Cabang_Prestasi')">
						<i class="fas fa-file-excel text-success me-1"></i> Excel
					</button>
				</div>
				<div class="card-body p-4 pt-2">
					<div class="table-responsive"
						style="max-height: 250px; overflow-y: auto;">
						<table class="table table-hover table-striped align-middle border"
							id="tblCabang<%=rndDash%>">
							<thead class="table-dark text-center"
								style="position: sticky; top: 0; z-index: 1;">
								<tr>
									<th><%=Common.getBahasaConfig("Cabang Prestasi")%></th>
									<th><%=Common.getBahasaConfig("Total Diajukan")%></th>
									<th><%=Common.getBahasaConfig("Disetujui")%></th>
								</tr>
							</thead>
							<tbody id="bodyCabang<%=rndDash%>">
								<tr>
									<td colspan="3" class="text-center"><div
											class="spinner-border spinner-border-sm text-warning"></div></td>
								</tr>
							</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>

		<div class="col-md-12">
			<div
				class="card border-0 shadow-sm rounded-4 h-100 border-top border-info border-3">
				<div
					class="card-header bg-white border-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center">
					<h6 class="fw-bold text-dark mb-0">
						<i class="fas fa-tags text-info me-2"></i><%=Common.getBahasaConfig("Rekap Per Kategori Prestasi")%></h6>
					<button
						class="btn btn-sm btn-light fw-bold rounded-pill shadow-sm border"
						onclick="downloadExcel<%=rndDash%>('tblKategoriRekap<%=rndDash%>', 'Rekap_Kategori_Prestasi')">
						<i class="fas fa-file-excel text-success me-1"></i> Excel
					</button>
				</div>
				<div class="card-body p-4 pt-2">
					<div class="table-responsive"
						style="max-height: 250px; overflow-y: auto;">
						<table class="table table-hover table-striped align-middle border"
							id="tblKategoriRekap<%=rndDash%>">
							<thead class="table-dark text-center"
								style="position: sticky; top: 0; z-index: 1;">
								<tr>
									<th><%=Common.getBahasaConfig("Kategori Prestasi")%></th>
									<th><%=Common.getBahasaConfig("Total Diajukan")%></th>
									<th><%=Common.getBahasaConfig("Disetujui")%></th>
								</tr>
							</thead>
							<tbody id="bodyKategoriRekap<%=rndDash%>">
								<tr>
									<td colspan="3" class="text-center"><div
											class="spinner-border spinner-border-sm text-info"></div></td>
								</tr>
							</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>

	</div>

	<div class="row g-4 mb-4">

		<%
		if (idGuruLogin == null) {
		%>
		<div class="col-md-6">
			<div
				class="card border-0 shadow-sm rounded-4 h-100 border-top border-secondary border-3">
				<div
					class="card-header bg-white border-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center">
					<h6 class="fw-bold text-dark mb-0">
						<i class="fas fa-building text-secondary me-2"></i><%=Common.getBahasaConfig("Rekap Per Yayasan")%></h6>
					<button
						class="btn btn-sm btn-light fw-bold rounded-pill shadow-sm border"
						onclick="downloadExcel<%=rndDash%>('tblYayasan<%=rndDash%>', 'Rekap_Yayasan')">
						<i class="fas fa-file-excel text-success me-1"></i> Excel
					</button>
				</div>
				<div class="card-body p-4 pt-2">
					<div class="table-responsive"
						style="max-height: 300px; overflow-y: auto;">
						<table class="table table-hover table-striped align-middle border"
							id="tblYayasan<%=rndDash%>">
							<thead class="table-dark text-center"
								style="position: sticky; top: 0; z-index: 1;">
								<tr>
									<th><%=Common.getBahasaConfig("Yayasan")%></th>
									<th><%=Common.getBahasaConfig("Total")%></th>
									<th><%=Common.getBahasaConfig("Disetujui")%></th>
								</tr>
							</thead>
							<tbody id="bodyYayasan<%=rndDash%>">
								<tr>
									<td colspan="3" class="text-center"><div
											class="spinner-border spinner-border-sm text-secondary"></div></td>
								</tr>
							</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>

		<div class="col-md-6">
			<div
				class="card border-0 shadow-sm rounded-4 h-100 border-top border-danger border-3">
				<div
					class="card-header bg-white border-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center">
					<h6 class="fw-bold text-dark mb-0">
						<i class="fas fa-school text-danger me-2"></i><%=Common.getBahasaConfig("Rekap Per Sekolah")%></h6>
					<button
						class="btn btn-sm btn-light fw-bold rounded-pill shadow-sm border"
						onclick="downloadExcel<%=rndDash%>('tblSekolah<%=rndDash%>', 'Rekap_Sekolah')">
						<i class="fas fa-file-excel text-success me-1"></i> Excel
					</button>
				</div>
				<div class="card-body p-4 pt-2">
					<div class="table-responsive"
						style="max-height: 300px; overflow-y: auto;">
						<table class="table table-hover table-striped align-middle border"
							id="tblSekolah<%=rndDash%>">
							<thead class="table-dark text-center"
								style="position: sticky; top: 0; z-index: 1;">
								<tr>
									<th><%=Common.getBahasaConfig("Sekolah")%></th>
									<th><%=Common.getBahasaConfig("Total")%></th>
									<th><%=Common.getBahasaConfig("Disetujui")%></th>
								</tr>
							</thead>
							<tbody id="bodySekolah<%=rndDash%>">
								<tr>
									<td colspan="3" class="text-center"><div
											class="spinner-border spinner-border-sm text-danger"></div></td>
								</tr>
							</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>
		<%
		}
		%>

		<div class="col-md-12">
			<div
				class="card border-0 shadow-sm rounded-4 h-100 border-top border-dark border-3">
				<div
					class="card-header bg-white border-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center">
					<h6 class="fw-bold text-dark mb-0"><i class="fas fa-medal text-dark me-2"></i><%=Common.getBahasaConfig("Rekap Prestasi Per Guru Utama")%></h6>
					<button
						class="btn btn-sm btn-light fw-bold rounded-pill shadow-sm border"
						onclick="downloadExcel<%=rndDash%>('tblGuruPendamping<%=rndDash%>', 'Rekap_Guru_Pendamping')">
						<i class="fas fa-file-excel text-success me-1"></i> Excel
					</button>
				</div>
				<div class="card-body p-4 pt-2">
					<div class="table-responsive"
						style="max-height: 350px; overflow-y: auto;">
						<table class="table table-hover table-striped align-middle border"
							id="tblGuruPendamping<%=rndDash%>">
							<thead class="table-dark text-center"
								style="position: sticky; top: 0; z-index: 1;">
								<tr>
									<th class="text-start"><%=Common.getBahasaConfig("Nama Guru")%></th>
									<th><%=Common.getBahasaConfig("NUPTK")%></th>
									<th><%=Common.getBahasaConfig("Total Pendampingan")%></th>
									<th><%=Common.getBahasaConfig("Prestasi Disetujui")%></th>
								</tr>
							</thead>
							<tbody id="bodyGuruPendamping<%=rndDash%>">
								<tr>
									<td colspan="4" class="text-center"><div
											class="spinner-border spinner-border-sm text-dark"></div></td>
								</tr>
							</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>

	</div>
</div>

<script>
    // Variable global Chart instance
    let chartTrendInstance<%=rndDash%> = null;
    let chartKatInstance<%=rndDash%> = null;

    // Fungsi helper fetch SQL via API
    const fetchSqlDashboard<%=rndDash%> = async (sqlString) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: "sql", sql: sqlString })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("Fetch SQL Dashboard Error:", e);
            return []; 
        }
    };

    // Fungsi helper render Row Table Rekap standar (3 kolom)
    const renderTableRekapHtml<%=rndDash%> = (data, colName1, bodyId) => {
        let html = '';
        if(data.length === 0) {
            html = '<tr><td colspan="3" class="text-center text-muted fw-medium"><i class="fas fa-folder-open me-2 opacity-50"></i><%=Common.getBahasaConfig("Tidak ada data")%></td></tr>';
        } else {
            data.forEach(row => {
                html += '<tr>' +
                        '  <td class="fw-medium text-start">' + (row[colName1] || '-') + '</td>' +
                        '  <td class="text-center fw-bold text-dark">' + row.total + '</td>' +
                        '  <td class="text-center text-success fw-bold">' + row.disetujui + '</td>' +
                        '</tr>';
            });
        }
        document.getElementById(bodyId).innerHTML = html;
    };

    // FUNGSI UTAMA LOAD DASHBOARD
    const loadDashboard<%=rndDash%> = async () => {
        
        // Setup tombol refresh UI
        const btnRef = document.getElementById('btnRefreshDash<%=rndDash%>');
        const oriBtnText = btnRef.innerHTML;
        btnRef.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i><%=Common.getBahasaConfig("Memuat...")%>';
        btnRef.disabled = true;

        // Ambil Filter Tambahan
        const fTa = document.getElementById('dashFilterTA<%=rndDash%>').value;
        const fSmt = document.getElementById('dashFilterSmt<%=rndDash%>').value;

        let baseWhere = `<%=baseWhere%>`;
        
        if (fTa !== '') {
            baseWhere += " AND p.tahunakademik = '" + fTa + "' ";
        }
        if (fSmt !== '') {
            baseWhere += " AND p.jenissemester = '" + fSmt + "' ";
        }

        try {
            // 1. Load Summary Cards
            const sqlSummary = "SELECT " +
                               " COUNT(*) as total, " +
                               " SUM(CASE WHEN status = 'Disetujui' THEN 1 ELSE 0 END) as disetujui, " +
                               " SUM(CASE WHEN status = 'Sedang diproses' THEN 1 ELSE 0 END) as proses, " +
                               " SUM(CASE WHEN status = 'Belum diproses' THEN 1 ELSE 0 END) as belum " +
                               " FROM sekolah.prestasi_guru p " + baseWhere;

            const resSum = await fetchSqlDashboard<%=rndDash%>(sqlSummary);
            if(resSum && resSum.length > 0) {
                document.getElementById('dashTotal<%=rndDash%>').innerText = resSum[0].total || 0;
                document.getElementById('dashDisetujui<%=rndDash%>').innerText = resSum[0].disetujui || 0;
                document.getElementById('dashProses<%=rndDash%>').innerText = resSum[0].proses || 0;
                document.getElementById('dashBelum<%=rndDash%>').innerText = resSum[0].belum || 0;
            }

            // 2. Load Chart Trend Tahun Pelajaran & Semester (Hanya yang disetujui)
            const sqlTrend = "SELECT CONCAT(tahunakademik, ' - ', jenissemester) as label, count(id) as value " +
                             " FROM sekolah.prestasi_guru p " + 
                             baseWhere + " AND tahunakademik IS NOT NULL AND status = 'Disetujui' " +
                             " GROUP BY tahunakademik, jenissemester ORDER BY tahunakademik ASC, jenissemester ASC";
            
            const resTrend = await fetchSqlDashboard<%=rndDash%>(sqlTrend);
            const labelsTrend = resTrend.map(item => item.label);
            const valuesTrend = resTrend.map(item => item.value);

            if (chartTrendInstance<%=rndDash%>) chartTrendInstance<%=rndDash%>.destroy();
            const ctxTrend = document.getElementById('chartTrend<%=rndDash%>').getContext('2d');
            chartTrendInstance<%=rndDash%> = new Chart(ctxTrend, {
                type: 'line',
                data: {
                    labels: labelsTrend,
                    datasets: [{
                        label: '<%=Common.getBahasaConfigJS("Prestasi Disetujui")%>',
                        data: valuesTrend,
                        borderColor: '#0d6efd',
                        backgroundColor: 'rgba(13, 110, 253, 0.2)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.3,
                        pointBackgroundColor: '#0d6efd',
                        pointRadius: 4
                    }]
                },
                options: { 
                    responsive: true, 
                    maintainAspectRatio: false,
                    scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
                }
            });

            // 3. Load Chart Pie Kategori
            const sqlKat = "SELECT k.nama as label, count(p.id) as value " +
                           " FROM sekolah.prestasi_guru p " +
                           " LEFT JOIN sekolah.kategori_prestasi_guru k ON p.kategori_prestasi_guru = k.id " +
                           baseWhere + " AND p.status = 'Disetujui' " +
                           " GROUP BY k.nama ORDER BY value DESC";

            const resKat = await fetchSqlDashboard<%=rndDash%>(sqlKat);
            const labelsKat = resKat.map(item => item.label || '<%=Common.getBahasaConfigJS("Lainnya")%>');
            const valuesKat = resKat.map(item => item.value);

            if (chartKatInstance<%=rndDash%>) chartKatInstance<%=rndDash%>.destroy();
            const ctxKat = document.getElementById('chartKategori<%=rndDash%>').getContext('2d');
            chartKatInstance<%=rndDash%> = new Chart(ctxKat, {
                type: 'doughnut',
                data: {
                    labels: labelsKat,
                    datasets: [{
                        data: valuesKat,
                        backgroundColor: ['#198754', '#ffc107', '#0dcaf0', '#dc3545', '#6610f2', '#6c757d', '#fd7e14', '#20c997']
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false }
            });

            // 4. Load Tabel Rekap TP & Semester
            const sqlTaSmt = "SELECT CONCAT(tahunakademik, ' - ', jenissemester) as tasmt, count(id) as total, " +
                             " sum(case when status='Disetujui' then 1 else 0 end) as disetujui " +
                             " FROM sekolah.prestasi_guru p " +
                             baseWhere + " AND tahunakademik IS NOT NULL " +
                             " GROUP BY tahunakademik, jenissemester ORDER BY tahunakademik DESC, jenissemester DESC";

            const resTaSmt = await fetchSqlDashboard<%=rndDash%>(sqlTaSmt);
            renderTableRekapHtml<%=rndDash%>(resTaSmt, 'tasmt', 'bodyTaSmt<%=rndDash%>');

            // 5. Load Tabel Rekap Cabang
            const sqlCbg = "SELECT c.nama as cabang, count(p.id) as total, " +
                           " sum(case when p.status='Disetujui' then 1 else 0 end) as disetujui " +
                           " FROM sekolah.prestasi_guru p " +
                           " JOIN sekolah.cabang_prestasi_guru c ON p.cabang_prestasi_guru = c.id " +
                           baseWhere +
                           " GROUP BY c.nama ORDER BY total DESC";

            const resCbg = await fetchSqlDashboard<%=rndDash%>(sqlCbg);
            renderTableRekapHtml<%=rndDash%>(resCbg, 'cabang', 'bodyCabang<%=rndDash%>');

            // 6. Load Tabel Rekap Kategori
            const sqlKategori = "SELECT k.nama as kategori, count(p.id) as total, " +
                                " sum(case when p.status='Disetujui' then 1 else 0 end) as disetujui " +
                                " FROM sekolah.prestasi_guru p " +
                                " JOIN sekolah.kategori_prestasi_guru k ON p.kategori_prestasi_guru = k.id " +
                                baseWhere +
                                " GROUP BY k.nama ORDER BY total DESC";

            const resKategori = await fetchSqlDashboard<%=rndDash%>(sqlKategori);
            renderTableRekapHtml<%=rndDash%>(resKategori, 'kategori', 'bodyKategoriRekap<%=rndDash%>');

            // 7. Load Tabel Rekap Struktural (Yayasan & Sekolah) Jika bukan Guru Biasa
            <%if (idGuruLogin == null) {%>
            const sqlYayasan = "SELECT f.nama as yayasan, count(p.id) as total, " +
                               " sum(case when p.status='Disetujui' then 1 else 0 end) as disetujui " +
                               " FROM sekolah.prestasi_guru p " +
                               " JOIN sekolah.yayasan f ON p.yayasan = f.id " +
                               baseWhere +
                               " GROUP BY f.nama ORDER BY total DESC";

            const resYayasan = await fetchSqlDashboard<%=rndDash%>(sqlYayasan);
            renderTableRekapHtml<%=rndDash%>(resYayasan, 'yayasan', 'bodyYayasan<%=rndDash%>');

            const sqlSekolah = "SELECT j.nama as sekolah, count(p.id) as total, " +
                               " sum(case when p.status='Disetujui' then 1 else 0 end) as disetujui " +
                               " FROM sekolah.prestasi_guru p " +
                               " JOIN sekolah.sekolah j ON p.sekolah = j.id " +
                               baseWhere +
                               " GROUP BY j.nama ORDER BY total DESC";

            const resSekolah = await fetchSqlDashboard<%=rndDash%>(sqlSekolah);
            renderTableRekapHtml<%=rndDash%>(resSekolah, 'sekolah', 'bodySekolah<%=rndDash%>');
            <%}%>

        	 // 8. Load Tabel Rekap Per Guru Utama (Kolom 'guru')
            const sqlGuruPendamping = "SELECT d.nama_guru as nama_guru, d.nuptk as nuptk, count(p.id) as total, " +
                                      " sum(case when p.status='Disetujui' then 1 else 0 end) as disetujui " +
                                      " FROM sekolah.prestasi_guru p " +
                                      " JOIN sekolah.guru d ON p.guru = d.id " +
                                      baseWhere +
                                      " GROUP BY d.id, d.nama_guru, d.nuptk ORDER BY total DESC, d.nama_guru ASC";

            const resGuruPendamping = await fetchSqlDashboard<%=rndDash%>(sqlGuruPendamping);
            let htmlGuru = '';
            if(resGuruPendamping.length === 0) {
                htmlGuru = '<tr><td colspan="4" class="text-center text-muted fw-medium"><i class="fas fa-folder-open me-2 opacity-50"></i><%=Common.getBahasaConfig("Tidak ada data pendampingan")%></td></tr>';
            } else {
                resGuruPendamping.forEach(row => {
                    htmlGuru += '<tr>' +
                            '  <td class="fw-bold text-start text-dark">' + (row.nama_guru || '-') + '</td>' +
                            '  <td class="text-center">' + (row.nuptk || '-') + '</td>' +
                            '  <td class="text-center fw-bold text-primary">' + row.total + '</td>' +
                            '  <td class="text-center text-success fw-bold">' + row.disetujui + '</td>' +
                            '</tr>';
                });
            }
            document.getElementById('bodyGuruPendamping<%=rndDash%>').innerHTML = htmlGuru;

        } catch (error) {
            console.error("Gagal memuat dashboard:", error);
        } finally {
            // Re-enable button
            btnRef.innerHTML = '<i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Segarkan Data")%>';
            btnRef.disabled = false;
        }
    };

    // Auto-reload saat filter diubah
    document.getElementById('dashFilterTA<%=rndDash%>').addEventListener('change', loadDashboard<%=rndDash%>);
    document.getElementById('dashFilterSmt<%=rndDash%>').addEventListener('change', loadDashboard<%=rndDash%>);

    // Fungsi Download Excel dari Table HTML menggunakan SheetJS
    const downloadExcel<%=rndDash%> = (tableId, filename) => {
        const table = document.getElementById(tableId);
        if(!table) return;
        const wb = XLSX.utils.table_to_book(table, {sheet: "<%=Common.getBahasaConfig("Rekap")%>"});
        XLSX.writeFile(wb, filename + "_" + new Date().getTime() + ".xlsx");
    };

    // Init otomatis saat pertama kali dibuka
    document.addEventListener("DOMContentLoaded", () => {
        loadDashboard<%=rndDash%>();
    });
</script>