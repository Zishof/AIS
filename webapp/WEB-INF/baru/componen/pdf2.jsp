<%@page import="ais.common.Common"%>
<%
    String pdfUrl = request.getParameter("pdf_url");
    // Default jika null agar JS tidak error total (opsional)
    if (pdfUrl == null) pdfUrl = "";
%>

<div class="container-fluid p-3">
    
    <div class="card mb-3 shadow-sm">
        <div class="card-body py-2">
            <div class="row align-items-center">
                
                <div class="col-12 col-md-4 text-center text-md-start mb-2 mb-md-0">
                    <% if (pdfUrl != null && !pdfUrl.isEmpty()) { %>
                        <a href="<%=pdfUrl%>" target="_blank" class="btn btn-primary btn-sm">
                            <i class="fas fa-download me-1"></i> <%=Common.getBahasaConfig("Download")%>
                        </a>
                    <% } %>
                </div>

                <div class="col-12 col-md-4 text-center mb-2 mb-md-0 fw-bold text-secondary">
                    <span>
                        <%=Common.getBahasaConfig("Halaman")%> 
                        <span id="page_num">0</span> / <span id="page_count">0</span>
                    </span>
                </div>

                <div class="col-12 col-md-4 text-center text-md-end">
                    <div class="btn-group" role="group">
                        <button id="prev" type="button" class="btn btn-outline-secondary btn-sm">
                            <i class="fas fa-chevron-left me-1"></i> <%=Common.getBahasaConfig("Sebelumnya")%>
                        </button>
                        <button id="next" type="button" class="btn btn-outline-secondary btn-sm">
                            <%=Common.getBahasaConfig("Lanjut")%> <i class="fas fa-chevron-right ms-1"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-auto">
            <canvas id="the-canvas" class="shadow-lg border rounded w-100"></canvas>
        </div>
    </div>
</div>

<script type="module">
// 1. Ganti URL import library utama ke versi spesifik (misal: v4.0.379)
  import * as pdfjsLib from 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.0.379/pdf.mjs';

  // 2. Ganti URL worker ke versi YANG SAMA PERSIS
  pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.0.379/pdf.worker.mjs';

    var url = '<%=pdfUrl%>';
    
    // Variabel state
    var pdfDoc = null,
        pageNum = 1,
        pageRendering = false,
        pageNumPending = null,
        scale = 1.5, // Skala diperbesar agar hasil render lebih tajam
        canvas = document.getElementById('the-canvas'),
        ctx = canvas.getContext('2d');

    /**
     * Render halaman ke dalam canvas
     */
    function renderPage(num) {
        pageRendering = true;
        
        // Ambil halaman
        pdfDoc.getPage(num).then(function(page) {
            var viewport = page.getViewport({scale: scale});
            
            // Set dimensi canvas sesuai viewport
            canvas.height = viewport.height;
            canvas.width = viewport.width;

            // Render
            var renderContext = {
                canvasContext: ctx,
                viewport: viewport
            };
            
            var renderTask = page.render(renderContext);

            // Tunggu render selesai
            renderTask.promise.then(function() {
                pageRendering = false;
                if (pageNumPending !== null) {
                    renderPage(pageNumPending);
                    pageNumPending = null;
                }
            });
        });

        // Update info halaman di UI
        document.getElementById('page_num').textContent = num;
    }

    /**
     * Queue render (jika user klik cepat)
     */
    function queueRenderPage(num) {
        if (pageRendering) {
            pageNumPending = num;
        } else {
            renderPage(num);
        }
    }

    /**
     * Event Listeners untuk Tombol
     */
    document.getElementById('prev').addEventListener('click', function() {
        if (pageNum <= 1) return;
        pageNum--;
        queueRenderPage(pageNum);
    });

    document.getElementById('next').addEventListener('click', function() {
        if (pageNum >= pdfDoc.numPages) return;
        pageNum++;
        queueRenderPage(pageNum);
    });

    /**
     * Inisialisasi Load PDF
     */
    if (url) {
        pdfjsLib.getDocument(url).promise.then(function(pdfDoc_) {
            pdfDoc = pdfDoc_;
            document.getElementById('page_count').textContent = pdfDoc.numPages;
            renderPage(pageNum);
        }).catch(function(error) {
            console.error('Error loading PDF:', error);
            // Opsional: Tampilkan pesan error di canvas context jika mau
        });
    } else {
        console.warn("URL PDF kosong");
    }
</script>