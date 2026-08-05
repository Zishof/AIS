<%@page import="ais.common.Common"%>
<%
    // --- SERVER SIDE (JAVA 1.7) ---
    String reqTa = request.getParameter("ta");
    String taSelected = (reqTa != null && !reqTa.isEmpty()) ? reqTa : Common.getCurrentTahunAkademik();
    
    // ID Unik untuk widget
    String uid = Common.getGeneratedAngkaDigit(5);
%>

<div class="card shadow-sm border-0 rounded-3 mb-4 animate__animated animate__fadeInUp">
    <div class="card-header bg-primary py-3 d-flex justify-content-between align-items-center border-0">
        <div class="d-flex align-items-center">
            <i class="fas fa-calendar-day text-white fa-lg me-2"></i>
            <h6 class="mb-0 fw-bold text-white text-uppercase" style="letter-spacing: 0.5px;">
                <%= Common.getBahasaConfig("Kalender Akademik") %>
            </h6>
        </div>

        <div class="ms-2">
            <select id="ta_<%=uid%>" class="form-select form-select-sm border-0 fw-bold" 
                    onchange="loadKalender<%=uid%>()" style="min-width: 120px; cursor: pointer;">
                <option value=""><%= Common.getBahasaConfig("Semua Tahun") %></option>
                <% 
                if (Common.tahunAngkatans != null) {
                    for (String ta : Common.tahunAngkatans) {
                %>
                    <option value="<%=ta%>" <%= ta.equals(taSelected) ? "selected" : "" %>><%=ta%></option>
                <% 
                    } 
                } 
                %>
            </select>
        </div>
    </div>

    <div class="card-body p-0" id="container_<%=uid%>" style="min-height: 250px;">
        <div class="d-flex flex-column justify-content-center align-items-center py-5">
            <div class="spinner-border text-primary mb-3" role="status"></div>
            <span class="text-muted small fw-bold"><%= Common.getBahasaConfig("Memuat Data") %>...</span>
        </div>
    </div>
</div>

<script>
    /**
     * Load Kalender menggunakan Fetch API
     */
    window.loadKalender<%=uid%> = function() {
        const container = document.getElementById("container_<%=uid%>");
        const dropdown  = document.getElementById("ta_<%=uid%>");
        const taVal     = dropdown.value;

        // Tampilan Loading Modern
        container.innerHTML = `
            <div class="d-flex flex-column justify-content-center align-items-center py-5">
                <div class="spinner-border text-primary mb-2" role="status"></div>
                <div class="text-muted small"><%= Common.getBahasaConfig("Memuat Data") %>...</div>
            </div>`;

        dropdown.disabled = true;

        const targetUrl = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=home%2Fadmin&s=_kalender_akademik&ta=" + encodeURIComponent(taVal);
        
        fetch(targetUrl)
            .then(function(response) {
                if (!response.ok) throw new Error("Network error");
                return response.text();
            })
            .then(function(htmlContent) {
                // Efek transisi sederhana menggunakan class bootstrap
                container.style.opacity = '0';
                container.innerHTML = htmlContent;
                
                // Menghidupkan kembali paging jika ada di konten yang dimuat
                if (typeof hidupkanUlangPagingList === "function") {
                    hidupkanUlangPagingList();
                }

                // Eksekusi script tambahan jika ada
                if (typeof executeScripts === 'function') {
                    executeScripts(container);
                }
                
                container.classList.add('fade');
                setTimeout(function() { 
                    container.style.opacity = '1'; 
                    container.classList.add('show');
                }, 100);
            })
            .catch(function(err) {
                console.error("Widget Error:", err);
                container.innerHTML = `
                    <div class="text-center py-5">
                        <i class="fas fa-exclamation-circle text-danger fa-2x mb-3"></i>
                        <p class="text-muted small fw-bold"><%= Common.getBahasaConfig("Gagal Terhubung") %></p>
                        <button onclick="loadKalender<%=uid%>()" class="btn btn-sm btn-outline-primary rounded-pill px-4">
                            <i class="fas fa-sync-alt me-1"></i> <%= Common.getBahasaConfig("Coba Lagi") %>
                        </button>
                    </div>`;
            })
            .finally(function() {
                dropdown.disabled = false;
            });
    };

    // Jalankan otomatis saat halaman siap
    if (document.readyState === 'complete') {
        loadKalender<%=uid%>();
    } else {
        window.addEventListener('load', loadKalender<%=uid%>);
    }
</script>