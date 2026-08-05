<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
//==========================================
// 1. SECURITY & PARAMETER HANDLING
// ==========================================
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    return; 
}

String itemId = request.getParameter("pertemuan");
String paramTugas = request.getParameter("tugas");
String paramJenis = request.getParameter("jenis");

if (itemId == null || itemId.trim().isEmpty()) {
    out.print(Common.getBahasaConfig("Parameter pertemuan tidak ditemukan"));
    return;
}
String linkTugas = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning%2Ftugas&s=_tugas&pertemuan=" + itemId+"&tugas="+paramTugas+"&jenis="+paramJenis;
%>
<div class="container-fluid p-4" id="content_tugas_<%=itemId%>">
	
</div>

<script>

	async function loadDataTugas<%=itemId%>() {
		loadDataTugasData<%=itemId%>(false);
	}

	// Simulasi AJAX Request
	async function loadDataTugasData<%=itemId%>(refresh) {
		
		// 2. Tampilkan Loading State
	    document.getElementById('content_tugas_<%=itemId%>').innerHTML = `
	        <div class="text-center py-4">
	            <div class="spinner-border text-primary" role="status"></div>
	            <p class="mt-2"><%=Common.getBahasaConfig("Memuat data...")%></p>
	        </div>`;
	        
	    fetch('<%=linkTugas%>&refresh='+refresh)
	    .then(response => {
	        if (!response.ok) {
	            throw new Error('<%=Common.getBahasaConfigJS("Jaringan bermasalah atau file tidak ditemukan.")%>');
	        }
	        return response.text();
	    })
	    .then(html => {
	        const container = document.getElementById('content_tugas_<%=itemId%>');
	        
	        // Kosongkan container dulu
	        container.innerHTML = ''; 
	
	        // Teknik agar <script> di dalam 'html' bisa jalan
	        const range = document.createRange();
	        
	        // Set range ke dalam container agar konteksnya benar
	        range.selectNode(container); // atau document.body
	
	        // Parse string HTML menjadi elemen DOM yang 'hidup'
	        const fragment = range.createContextualFragment(html);
	
	        // Masukkan ke dalam DOM / Modal
	        container.appendChild(fragment);
	        
	    })
	    .catch(error => {
	        console.error('Error:', error);
	        document.getElementById('content_tugas_<%=itemId%>').innerHTML = `
	            <div class="alert alert-danger">
	                <strong><%=Common.getBahasaConfig("Gagal memuat konten.")%></strong><br>
	                <%=Common.getBahasaConfig("Pastikan Anda menjalankan file ini menggunakan Local Server (bukan klik ganda file html), karena browser memblokir akses file lokal (CORS Policy).")%>
	                <hr>
	                <small><%=Common.getBahasaConfig("Error:")%> `+error.message+`</small>
	            </div>`;
	    });
	}

    // ======================================================================================
    // FUNGSI GLOBAL UNTUK RELOAD DATA (Dipanggil oleh afterSave atau fungsi di _tugas.jsp)
    // ======================================================================================
    
    // Fungsi ini biasanya dipanggil setelah aksi simpan data (Create / Update)
    window.generatePertemuanHTML = function(idPertemuan) {
        // Melakukan fetch ulang dengan parameter refresh = true
        loadDataTugasData<%=itemId%>(true);
    };

    // Fungsi ini digunakan setelah aksi Hapus Tugas (Delete) pada file _tugas.jsp
    window.generatePertemuanHTMLRefresh = function(idPertemuan, isRefresh) {
        loadDataTugasData<%=itemId%>(isRefresh);
    };
    
    // ======================================================================================

	// Panggil Data saat load pertama kali
	loadDataTugas<%=itemId%>();
	
</script>