<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.common.Common"%>
<%
    // =================================================================================
    // 1. SETUP VARIABEL & IDENTIFIKATOR UNIK (HANYA MENGGUNAKAN 'rnd')
    // =================================================================================
    String rnd = request.getParameter("rnd");
    if(rnd == null || rnd.trim().isEmpty()) {
        rnd = Common.getGeneratedBarCode(7);
    }
    
    // Ambil Data Mahasiswa (Jika sudah disematkan melalui parameter)
	Mahasiswa mahasiswa = (Mahasiswa) GeneralValueObject.ambilData(Mahasiswa.class, request.getParameter("mahasiswa"), true);
    
    // Setup Semester (Default ke semester aktif mahasiswa jika ada)
    String paramSmt = request.getParameter("smt");
    Integer smt = (paramSmt == null || paramSmt.trim().isEmpty())
        ? (mahasiswa != null ? mahasiswa.currentSemester() : 1)
        : Integer.parseInt(paramSmt.trim());
    
    String judulLaporan = request.getParameter("judulLaporan");
    String fileLaporan = request.getParameter("fileLaporan");
%>

<div class="card shadow-sm border-0 mb-4 animate__animated animate__fadeIn">
    <div class="card-header bg-white py-3 border-bottom d-flex align-items-center">
        <h5 class="mb-0 text-primary fw-bold">
            <i class="fas fa-file-pdf me-2 text-danger"></i><%=Common.getBahasaConfig(judulLaporan)%>
        </h5>
    </div>

    <div class="card-body bg-light rounded-bottom">
        <div class="row g-3 align-items-end mb-4 bg-white p-3 rounded-3 shadow-sm border border-light border-opacity-50">
            
            <!-- Hidden Input untuk menampung ID Mahasiswa -->
            <input type="hidden" id="fixed_mhs_<%=rnd%>" value="<%= (mahasiswa != null) ? mahasiswa.getId() : "" %>" />

            <div class="col-md-3">
                <label class="form-label fw-bold small text-uppercase text-secondary"><i class="fas fa-layer-group me-1"></i><%=Common.getBahasaConfig("Semester")%></label>
                <select id="smt_<%=rnd%>" class="form-select border-primary border-opacity-25 shadow-sm" onchange="window.reloadLaporan<%=rnd%>()">
                    <option value="">-- <%=Common.getBahasaConfig("Pilih Semester")%> --</option>
                    <% for (int i = 1; i <= 14; i++) { %>
                        <option value="<%=i%>" <%= (smt != null && smt.equals(i)) ? "selected" : "" %> >
                            <%=Common.getBahasaConfig("Semester")%> <%=i%>
                        </option>
                    <% } %>
                </select>
            </div>

            <% 
            // ---------------------------------------------------------------------------------
            // JIKA MAHASISWA NULL (Maka Pengguna adalah Admin yang butuh mencari Mahasiswa)
            // ---------------------------------------------------------------------------------
            if (mahasiswa == null) { 
            %>
                <div class="col-md-7">
                    <label class="form-label fw-bold small text-uppercase text-secondary"><i class="fas fa-user-graduate me-1"></i><%=Common.getBahasaConfig("Cari Mahasiswa")%></label>
                    <div class="input-group shadow-sm">
                        <!-- Input penampung teks nama mahasiswa yang terpilih -->
                        <input type="text" id="display_mhs_<%=rnd%>" class="form-control bg-white" placeholder="<%=Common.getBahasaConfig("Klik tombol cari untuk memilih Mahasiswa...")%>" readonly>
                        
                        <!-- Tombol Buka Data Picker -->
                        <button class="btn btn-primary px-4 fw-bold" type="button" onclick="window.bukaDataPicker<%=rnd%>()">
                            <i class="fas fa-search me-1"></i> <%=Common.getBahasaConfig("Cari Data")%>
                        </button>
                    </div>
                </div>
            <% } %>

            <div class="col-md-auto ms-auto">
                <button class="btn btn-outline-primary px-4 w-100 fw-bold shadow-sm" type="button" onclick="window.reloadLaporan<%=rnd%>()">
                    <i class="fas fa-sync-alt me-1"></i> <%=Common.getBahasaConfig("Refresh Laporan")%>
                </button>
            </div>
        </div>

        <!-- Wadah Render Konten Laporan (PDF Iframe) -->
        <div id="data_render_<%=rnd%>" class="min-vh-50 bg-white rounded-3 shadow-sm border border-light p-2">
            <div class="text-center text-muted py-5 my-5">
                <i class="fas fa-file-invoice fa-4x mb-3 text-secondary opacity-25"></i>
                <h5 class="fw-bold"><%=Common.getBahasaConfig("Laporan Belum Dimuat")%></h5>
                <p><%=Common.getBahasaConfig("Silakan lengkapi parameter pencarian di atas untuk melihat dokumen.")%></p>
            </div>
        </div>
    </div>
</div>

<!-- ======================================================================= -->
<!-- BAGIAN JAVASCRIPT: DATA PICKER & FUNGSI RELOAD (TERKUNCI KE WINDOW) -->
<!-- ======================================================================= -->
<script>

    <% if (mahasiswa == null) { %>
    // -------------------------------------------------------------------------
    // 1. FUNGSI DATA PICKER MAHASISWA (Hanya aktif jika user adalah Admin)
    // -------------------------------------------------------------------------
    
    window.injectModalPicker<%=rnd%> = function() {
        if (!document.getElementById('modalPicker<%=rnd%>')) {
            const modalHtml = 
                '<div class="modal fade" id="modalPicker<%=rnd%>" tabindex="-1" aria-hidden="true" style="z-index: 9999;">' +
                    '<div class="modal-dialog modal-xl modal-dialog-scrollable modal-dialog-centered">' +
                        '<div class="modal-content shadow-lg border-0" style="border-radius: 16px; overflow: hidden;">' +
                            '<div class="modal-header bg-primary text-white p-3">' +
                                '<h5 class="modal-title fw-bold"><i class="fas fa-list-ul me-2"></i> <%=Common.getBahasaConfig("Papan Pilihan Mahasiswa")%></h5>' +
                                '<button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup")%>"></button>' +
                            '</div>' +
                            '<div class="modal-body p-0" id="modalBodyPicker<%=rnd%>" style="background-color: #f8fafc; min-height: 400px;">' +
                            '</div>' +
                            '<div class="modal-footer bg-light p-3">' +
                                '<button type="button" class="btn btn-secondary fw-bold px-4" data-bs-dismiss="modal"><i class="fas fa-times me-2"></i><%=Common.getBahasaConfig("Tutup")%></button>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }
    };

    window.bukaDataPicker<%=rnd%> = function() {
        window.injectModalPicker<%=rnd%>();

        const paramClass = '<%=Mahasiswa.class.getName()%>';
        const paramCols = 'id,nim,nama'; 
        const paramOrder = 'nama';
        const paramTitle = encodeURIComponent('<%=Common.getBahasaConfigJS("Pilih Mahasiswa")%>');
        const cbName = 'callbackPicker<%=rnd%>';
        const paramIdsTerpilih = '';
        
        const urlPicker = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&radio=true&p=common&s=_data_picker&class=' + paramClass + '&cols=' + paramCols + '&aktif=true&order=' + paramOrder + '&title=' + paramTitle + '&idsTerpilih=' + paramIdsTerpilih + '&callback=' + cbName + '&rnd=<%=rnd%>';

        const container = document.getElementById('modalBodyPicker<%=rnd%>');
        
        container.innerHTML = 
            '<div class="text-center py-5 my-5 text-primary">' +
                '<i class="fas fa-spinner fa-spin fa-3x mb-3"></i>' +
                '<h5 class="fw-bold"><%=Common.getBahasaConfig("Mempersiapkan Papan Pilihan...")%></h5>' +
            '</div>';
            
        const modalEl = document.getElementById('modalPicker<%=rnd%>');
        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        modal.show();
        
        fetch(urlPicker, { credentials: 'same-origin' })
            .then(function(response) { 
                if(!response.ok) throw new Error("Gagal mengambil data picker");
                return response.text(); 
            })
            .then(function(html) {
                container.innerHTML = html;
                
                Array.from(container.querySelectorAll("script")).forEach(function(oldScript) {
                    var newScript = document.createElement("script");
                    Array.from(oldScript.attributes).forEach(function(attr) {
                        if (attr.name.toLowerCase() !== 'type') newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.type = 'text/javascript';
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                });
            })
            .catch(function(err) {
                container.innerHTML = '<div class="text-center p-5 text-danger"><i class="fas fa-exclamation-triangle fa-3x mb-3"></i><br><h5><%=Common.getBahasaConfig("Gagal memuat Data Picker.")%></h5></div>';
            });
    };

    window.callbackPicker<%=rnd%> = function(dataArrayTerpilih) {
        // 1. Ekstrak data (Karena picker mengirim Array of Object)
        if (dataArrayTerpilih && dataArrayTerpilih.length > 0) {
            const dataPilihan = dataArrayTerpilih[0]; // Ambil elemen pertama
            
            // Simpan ID Mahasiswa terpilih ke hidden input
            const mhsId = dataPilihan.id || dataPilihan[0] || '';
            document.getElementById('fixed_mhs_<%=rnd%>').value = mhsId;
            
            // Tampilkan Info Singkat Mahasiswa di Textbox
            // Kita coba ekstrak nim dan nama, atau fallback ke properties lain
            const namaTampil = dataPilihan.nama || dataPilihan[2] || 'Data Terpilih';
            const nimTampil = dataPilihan.nim || dataPilihan[1] || mhsId;
            
            document.getElementById('display_mhs_<%=rnd%>').value = nimTampil + " - " + namaTampil;
        }

        // 2. Tutup Modal Picker
        const modalEl = document.getElementById('modalPicker<%=rnd%>');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if(modal) modal.hide();

        // 3. Langsung otomatis eksekusi Reload Laporan
        window.reloadLaporan<%=rnd%>();
    };
    <% } %>

    // -------------------------------------------------------------------------
    // 2. FUNGSI RELOAD (MENAMPILKAN LAPORAN KE DALAM WADAH)
    // -------------------------------------------------------------------------
    window.reloadLaporan<%=rnd%> = async function() {
        const container = document.getElementById("data_render_<%=rnd%>");
        
        const smt = document.getElementById("smt_<%=rnd%>").value;
        const mhsId = document.getElementById("fixed_mhs_<%=rnd%>").value;

        if (!mhsId || mhsId.trim() === "") {
            container.innerHTML = 
                '<div class="alert alert-warning border-0 shadow-sm text-center p-4 m-3" style="border-radius: 12px;">' +
                    '<i class="fas fa-search fa-2x mb-2 text-warning"></i>' +
                    '<h6 class="fw-bold mb-0"><%=Common.getBahasaConfig("Harap lengkapi parameter.")%></h6>' +
                    '<small class="text-muted"><%=Common.getBahasaConfig("Silakan cari dan pilih mahasiswa terlebih dahulu untuk mencetak dokumen ini.")%></small>' +
                '</div>';
            return;
        }
        
        if (!smt || smt.trim() === "") {
            container.innerHTML = 
                '<div class="alert alert-warning border-0 shadow-sm text-center p-4 m-3" style="border-radius: 12px;">' +
                    '<i class="fas fa-layer-group fa-2x mb-2 text-warning"></i>' +
                    '<h6 class="fw-bold mb-0"><%=Common.getBahasaConfig("Pilih Semester.")%></h6>' +
                '</div>';
            return;
        }

        container.innerHTML = 
            '<div class="d-flex flex-column justify-content-center align-items-center py-5 my-5 text-primary">' +
                '<i class="fas fa-cog fa-spin fa-3x mb-3"></i>' +
                '<h5 class="fw-bold"><%=Common.getBahasaConfig("Memproses Laporan Akademik...")%></h5>' +
            '</div>';

        try {
            const url = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=laporan%2Fgeneral_mahasiswa&s=_index_pdf&mahasiswa=" + encodeURIComponent(mhsId) + "&smt=" + encodeURIComponent(smt) + "&fileLaporan=" + encodeURIComponent('<%=fileLaporan%>');
            console.info("URL ", url);
            const response = await fetch(url, { credentials: 'same-origin' });
            if (!response.ok) throw new Error('<%=Common.getBahasaConfigJS("Terjadi masalah saat menghubungi server laporan.")%>');
            
            const html = await response.text();
            
            container.innerHTML = html; 

            const scripts = container.querySelectorAll("script");
            scripts.forEach(oldScript => {
                const newScript = document.createElement("script");
                Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') newScript.setAttribute(attr.name, attr.value); });
                newScript.type = 'text/javascript';
                newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                oldScript.parentNode.replaceChild(newScript, oldScript);
            });

        } catch (error) {
            container.innerHTML = 
                '<div class="alert alert-danger text-center shadow-sm border-0 p-4 m-3" style="border-radius: 12px;">' +
                    '<i class="fas fa-exclamation-triangle fa-3x mb-3 text-danger"></i>' +
                    '<h5 class="fw-bold"><%=Common.getBahasaConfig("Gagal Memuat Laporan")%></h5>' +
                    '<p class="mb-0 text-muted small">' + error.message + '</p>' +
                '</div>';
            console.error(error);
        }
    };

    // Auto-reload jika mhsId sudah terisi sejak awal (Kasus login sebagai Mahasiswa)
    <% if (mahasiswa != null) { %>
        setTimeout(function() {
            window.reloadLaporan<%=rnd%>();
        }, 500);
    <% } %>

</script>