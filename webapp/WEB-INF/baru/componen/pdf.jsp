<%@page import="java.util.concurrent.ThreadLocalRandom"%>
<%
    // Generate Random ID untuk elemen unik
    Long rdn = ThreadLocalRandom.current().nextLong(0, 999);
    String uniqueCanvasId = "canvasPDFPreviewData" + rdn;
    
    // Ambil URL dari parameter
    String pdfUrl = request.getParameter("pdf_url");
%>

<div class="container-fluid">
    <div class="row mb-3">
        <div class="col-12 text-center">
            <% if (pdfUrl != null && !pdfUrl.isEmpty()) { %>
                <a href="<%=pdfUrl%>" target="_blank" class="btn btn-primary btn-lg shadow-sm">
                    <i class="fas fa-download me-2"></i> Download / Buka PDF
                </a>
            <% } %>
        </div>
    </div>

    <div class="row justify-content-center">
        <div class="col-12 d-flex justify-content-center">
            <canvas id="<%=uniqueCanvasId%>" 
                    style="width: 90%; box-shadow: rgba(0, 0, 0, 0.25) 0px 54px 55px, rgba(0, 0, 0, 0.12) 0px -12px 30px; border: 1px solid #ddd; border-radius: 8px;">
            </canvas>
        </div>
    </div>
</div>

<script type="module">
// 1. Ganti URL import library utama ke versi spesifik (misal: v4.0.379)
  import * as pdfjsLib from 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.0.379/pdf.mjs';

  // 2. Ganti URL worker ke versi YANG SAMA PERSIS
  pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.0.379/pdf.worker.mjs';

  var urlPDF = '<%=pdfUrl%>';

  if (!urlPDF || urlPDF === 'null' || urlPDF === '') {
      console.warn("URL PDF kosong.");
  } else {
      var loadingTask = pdfjsLib.getDocument(urlPDF);
      
      loadingTask.promise.then(function(pdf) {
        // Ambil halaman pertama
        var pageNumber = 1;
        pdf.getPage(pageNumber).then(function(page) {
          
          var scale = 1.5;
          var viewport = page.getViewport({scale: scale});

          var canvas = document.getElementById('<%=uniqueCanvasId%>');
          
          if(canvas) {
              var context = canvas.getContext('2d');
              canvas.height = viewport.height;
              canvas.width = viewport.width;

              var renderContext = {
                canvasContext: context,
                viewport: viewport
              };
              
              page.render(renderContext);
          }
        });
      }, function (reason) {
        console.error('Error loading PDF:', reason);
      });
  }
</script>