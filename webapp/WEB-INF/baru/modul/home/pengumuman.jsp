<%@page import="ais.common.Common"%>
<%
    // ID Unik untuk mencegah konflik antar widget dalam satu halaman
    String uid = Common.getGeneratedAngkaDigit(5);
%>

<div class="card border-0 shadow-sm mb-4 rounded-3 animate__animated animate__fadeInUp">
    <div class="card-header bg-white py-3">
        <div class="row align-items-center g-2">
            <div class="col">
                <h6 class="mb-0 fw-bold text-primary d-flex align-items-center">
                    <i class="fas fa-bullhorn me-2"></i>
                    <%= Common.getBahasaConfig("Pengumuman Akademik") %>
                </h6>
            </div>
            
            <div class="col-sm-auto">
                <div class="input-group input-group-sm">
                    <input type="text" 
                           id="input_cari_<%=uid%>" 
                           onkeyup="if(event.keyCode===13){loadPengumuman<%=uid%>(this.value)}"
                           class="form-control border-primary" 
                           placeholder="<%=Common.getBahasaConfig("Cari") %>..."
                           aria-label="Search">
                    <button class="btn btn-primary" 
                            type="button" 
                            id="btn_cari_<%=uid%>"
                            onclick="triggerSearch<%=uid%>()">
                        <i class="fas fa-search"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="card-body p-0" id="container_pengumuman_<%=uid%>" style="min-height: 150px;">
        <div class="d-flex justify-content-center align-items-center py-5">
            <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
            <span class="small text-muted"><%=Common.getBahasaConfig("Memuat data") %>...</span>
        </div>
    </div>
</div>

<script>
    /**
     * Fungsi utama untuk memuat data
     * Menggunakan parameter 'search' untuk fungsionalitas pencarian
     */
    function loadPengumuman<%=uid%>(keyword) {
        const container = document.getElementById("container_pengumuman_<%=uid%>");
        let url = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=home%2Fadmin&s=_pengumuman";
        
        // Tambahkan parameter cari jika keyword ada
        if (keyword) {
            url += "&cari=" + encodeURIComponent(keyword);
        }

        // Tampilkan loading saat proses fetch
        container.style.opacity = "0.6";

        fetch(url)
            .then(response => {
                if (!response.ok) throw new Error("Network error");
                return response.text();
            })
            .then(html => {
                container.innerHTML = html;
                container.style.opacity = "1";

                // Inisialisasi ulang plugin jika ada
                if (typeof executeScripts === "function") executeScripts(container);
                if (typeof hidupkanUlangPagingList === "function") {
                    hidupkanUlangPagingList("pengumuan_akademik"); 
                }
            })
            .catch(error => {
                console.error("Widget Error:", error);
                container.innerHTML = `
                    <div class="py-5 text-center">
                        <p class="text-danger small mb-2">
                            <i class="fas fa-exclamation-triangle me-1"></i> 
                            <%=Common.getBahasaConfig("Gagal memuat pengumuman") %>
                        </p>
                        <button onclick="loadPengumuman<%=uid%>()" class="btn btn-xs btn-outline-danger rounded-pill">
                            <%=Common.getBahasaConfig("Coba Lagi") %>
                        </button>
                    </div>
                `;
            });
    }

    /**
     * Trigger pencarian dari tombol atau enter
     */
    function triggerSearch<%=uid%>() {
        const keyword = document.getElementById("input_cari_<%=uid%>").value;
        loadPengumuman<%=uid%>(keyword);
    }

    loadPengumuman<%=uid%>();

    // Listener Enter Key pada input cari
    document.getElementById("input_cari_<%=uid%>").addEventListener("keypress", function(e) {
        if (e.key === 'Enter') {
            triggerSearch<%=uid%>();
        }
    });
</script>