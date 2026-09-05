<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
    <%@page import="ais.common.Common"%>
	   
	
	
<%
// Teks terjemahan untuk dialog konfirmasi — di-escape agar aman di dalam string JS
String _dlgKonfirmasi  = Common.getBahasaConfig("Konfirmasi").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _dlgTutup       = Common.getBahasaConfig("Tutup").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _dlgBatal       = Common.getBahasaConfig("Batal").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _dlgYaLanjutkan = Common.getBahasaConfig("Ya, Lanjutkan").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
String _dlgPemberitahuan = Common.getBahasaConfig("Pemberitahuan").replace("\\", "\\\\").replace("'", "\\'").replace("</", "<\\/");
%>
	<script src="<%=request.getContextPath()%>/js/pesan-formal.js?v=20260906"></script>
	<script type="text/javascript">
	    (function(){
	        if (window.__aisDialogModalInitialized) return;
	        window.__aisDialogModalInitialized = true;

	        window.pemberitahuanModal = function(msg, jenis) {
	            var pesan = (msg == null ? "" : String(msg));
	            if (window.AisAlert) {
	                return window.AisAlert.show({
	                    title: '<%= _dlgPemberitahuan %>',
	                    message: pesan,
	                    type: jenis || "info",
	                    closeLabel: '<%= _dlgTutup %>'
	                });
	            }
	            window.__nativeAlert(pesan);
	        };

	        window.konfirmasiModal = function(msg) {
	            var pesan = (msg == null ? "" : String(msg));
	            if (window.AisAlert) {
	                return window.AisAlert.confirm({
	                    title: '<%= _dlgKonfirmasi %>',
	                    message: pesan,
	                    type: "question",
	                    confirmLabel: '<%= _dlgYaLanjutkan %>',
	                    cancelLabel: '<%= _dlgBatal %>',
	                    closeLabel: '<%= _dlgTutup %>'
	                });
	            }
	            return Promise.resolve(window.__nativeConfirm(pesan));
	        };

	        window.confirmAsync = window.konfirmasiModal;
	        // Pertahankan kontrak native alert(): tidak mengembalikan nilai.
	        window.alert = function(msg) { window.pemberitahuanModal(msg, "info"); };
	    })();
	
		
	
	
		function generateRandomText(length) {
		    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
		    let result = '';
		    
		    for (let i = 0; i < length; i++) {
		        const randomIndex = Math.floor(Math.random() * characters.length);
		        result += characters.charAt(randomIndex);
		    }
		    
		    return result;
		}

	    function updateBulkFiles(idVal) {
	        // --- BAGIAN 1: MENGAMBIL DATA ---
	
	        // 1. Cari semua elemen yang ID-nya dimulai dengan "input_file_"
	        const inputElements = document.querySelectorAll('[id^="input_file_"]');
	        
	        let valuesArray = [];
	
	        // 2. Loop setiap elemen untuk mencari pasangannya
	        inputElements.forEach(inputEl => {
	            // Ambil ID lengkap (contoh: "input_file_123")
	            const fullId = inputEl.id;
	            
	            // Ambil suffix kodenya (contoh: "123") dengan membuang "input_file_"
	            const kodeSama = fullId.replace("input_file_", "");
	            
	            console.log("kodeSama:", kodeSama);
	
	            // Cari elemen pasangannya berdasarkan kode tersebut
	            const clazzEl = document.getElementById("clazz_file_" + kodeSama);
	            const jenisEl = document.getElementById("jenis_file_" + kodeSama);
	
	            // Pastikan elemen pasangan ditemukan sebelum mengambil value
	            if (clazzEl && jenisEl) {
	                // Masukkan ke array sebagai object (agar lebih rapi daripada array murni)
	                // .value digunakan jika elemen adalah input/select. 
	                // Gunakan .innerText atau .textContent jika elemen adalah span/div.
	                valuesArray.push({
	                    id_val: inputEl.value,
	                    class_val: clazzEl.value, 
	                    jenis_val: jenisEl.value
	                });
	            }
	        });
	
	        console.log("Data yang terkumpul:", valuesArray);
	
	        // --- BAGIAN 2: MENGIRIM KE API ---
	
	        // Asumsi: Variabel 'data' sudah ada di scope global (sesuai snippet Java Anda)
	        // Jika tidak ada, kita buat dummy agar tidak error
	        if (typeof data === 'undefined') {
	            var data = { id: null }; 
	        }
	
	        // 3. Loop array hasil data tadi dan kirim ke API
	        valuesArray.forEach(item => {
	            
	            // Membangun Object sesuai permintaan snippet Java Anda
	            var reqObj = {
	                "action": "update_file",
	                "id": item.id_val,       // ID_DARI_ARRAY
	                "class": item.class_val, // CLASS_DARI_ARRAY
	                "jenis": item.jenis_val, // JENIS_DARI_ARRAY
	                "ref": idVal
	            };
	
	            // Konversi ke JSON String
	            var reqJson = JSON.stringify(reqObj);
	            
	            // PERUBAHAN: URL bersih tanpa parameter datasearch
	            const servletUrl = '<%=Common.ROOT%>/Data';
	
	            // Kirim menggunakan fetch POST dengan tipe data JSON
	            fetch(servletUrl, {
	                method: 'POST',
	                headers: {
	                    'Content-Type': 'application/json'
	                },
	                body: reqJson // Langsung kirim JSON string
	            })
	            .then(response => response.json())
	            .then(result => {
	                console.log("Sukses update untuk ID: " + item.id_val, result);
	            })
	            .catch(error => {
	                console.error("Gagal update untuk ID: " + item.id_val, error);
	            });
	        });
	    }
	    
	    
	 // =================================================================
	 // FUNGSI BARU: POPUP KONFIRMASI DINAMIS MENGGANTIKAN WINDOW.CONFIRM
	 // =================================================================
	 const showConfirmModal = (msg, callbackYes) => {
	     if (window.AisAlert) {
	         return window.AisAlert.confirm({
	             title: '<%= _dlgKonfirmasi %>',
	             message: (msg == null ? "" : String(msg)),
	             confirmLabel: '<%= _dlgYaLanjutkan %>',
	             cancelLabel: '<%= _dlgBatal %>',
	             closeLabel: '<%= _dlgTutup %>'
	         }).then(function(disetujui) {
	             if (disetujui && typeof callbackYes === 'function') callbackYes();
	             return disetujui;
	         });
	     }
	     window.confirmVar = window.confirmVar || 0;
	     window.confirmVar++;
	     var cVar = window.confirmVar;
	     var modalId = 'confirmModal_' + cVar;
	     var zIndexModal = 1100 + cVar; // Dipastikan berada di atas modal lain namun di bawah Toast
	     
	     var modalHtml = 
	     '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" style="z-index: ' + zIndexModal + ';">' +
	         // PERUBAHAN DI SINI: modal-sm dihapus, diganti dengan style dinamis (fit-content)
	         '<div class="modal-dialog modal-dialog-centered animate__animated animate__zoomIn" style="max-width: fit-content; min-width: 350px; margin-left: auto; margin-right: auto;">' +
	             '<div class="modal-content shadow-lg border-0 rounded-4">' +
	                 '<div class="modal-header bg-warning border-bottom-0 pb-0">' +
	                     '<h6 class="modal-title fw-bold text-dark"><i class="fas fa-exclamation-triangle me-2"></i><%= _dlgKonfirmasi %></h6>' +
	                     '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<%= _dlgTutup %>"></button>' +
	                 '</div>' +
	                 '<div class="modal-body p-4 text-center">' +
	                     // PERUBAHAN DI SINI: Menambahkan text-wrap agar jika teks kelewat panjang tidak meluber ke luar layar
	                     '<p class="mb-0 fw-medium text-dark text-wrap">' + msg + '</p>' +
	                 '</div>' +
	                 '<div class="modal-footer bg-light border-top-0 justify-content-center pt-2 pb-3">' +
	                     '<button type="button" class="btn btn-light border fw-bold shadow-sm px-4 rounded-pill" data-bs-dismiss="modal">' +
	                         '<i class="fas fa-times text-danger me-2"></i><%= _dlgBatal %>' +
	                     '</button>' +
	                     '<button type="button" class="btn btn-primary fw-bold shadow-sm px-4 rounded-pill" id="btnYes' + modalId + '">' +
	                         '<i class="fas fa-check me-2"></i><%= _dlgYaLanjutkan %>' +
	                     '</button>' +
	                 '</div>' +
	             '</div>' +
	         '</div>' +
	     '</div>';

	     // 1. Suntikkan HTML ke dalam body
	     document.body.insertAdjacentHTML('beforeend', modalHtml);
	     
	     var modalEl = document.getElementById(modalId);
	     var myModal = new bootstrap.Modal(modalEl);
	     
	     // 2. Pasang Event Listener jika "Ya" ditekan
	     document.getElementById('btnYes' + modalId).addEventListener('click', function() {
	         myModal.hide();
	         setTimeout(function() {
	             if(typeof callbackYes === 'function') callbackYes();
	         }, 300); // Tunggu animasi tutup selesai sebelum mengeksekusi
	     });
	     
	     // 3. Hapus modal dari DOM setelah benar-benar tertutup (agar tidak menumpuk)
	     modalEl.addEventListener('hidden.bs.modal', function () {
	         setTimeout(function() { modalEl.remove(); }, 300);
	     });
	     
	     // 4. Tampilkan
	     myModal.show();
	 };
	    
	    function prosesDeleteDataTapiUpdate(masterModelClass, id, dataObjcallback, callback) {
	        variable ++;
	        const modalHtml = `<div class="modal fade" id="modalHapus_`+variable+`" tabindex="-1" aria-hidden="true">
	                <div class="modal-dialog modal-dialog-centered modal-sm animate__animated animate__jackInTheBox animate__faster">
	                <div class="modal-content border-0 shadow rounded-4">
	                    <div class="modal-header border-0 pb-0">
	                        <h5 class="modal-title fw-bold text-danger"><%=Common.getBahasaConfig("Konfirmasi") %></h5>
	                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
	                    </div>
	                    <div class="modal-body text-center py-4">
	                        <i class="fas fa-exclamation-triangle text-warning mb-3" style="font-size: 2rem;"></i>
	                        <p class="mb-0 text-dark">
	                            <%=Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini ?") %>
	                        </p>
	                    </div>
	                    <div class="modal-footer border-0 pt-0 justify-content-center">
	                        <button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal") %></button>
	                        <button type="button" class="btn btn-danger rounded-pill px-4" 
	                                onclick="doUpdate('`+masterModelClass+`', '`+id+`', `+dataObjcallback+`, `+callback+`);">
	                            <%=Common.getBahasaConfig("Hapus") %>
	                        </button>
	                    </div>
	                </div>
	            </div>
	        </div>`;
	        
	        document.body.insertAdjacentHTML('beforeend', modalHtml);
	        
	        let myModalElement = document.getElementById('modalHapus_'+variable);
	        // Tampilkan Modal
	        const myModal = new bootstrap.Modal(myModalElement);
	        
	        // 1. Ambil Dialog untuk di-reset animasinya
	        let modalDialog = myModalElement.querySelector('.modal-dialog');
	        
	        // 3. Trik agar animasi "Masuk" selalu main setiap kali dibuka
	        // Kita hapus dulu class animasinya, lalu pasang lagi
	        modalDialog.classList.remove('animate__jackInTheBox');
	        modalDialog.classList.remove('animate__zoomOut'); // Hapus animasi keluar (jika ada)
	        
	        // Force Reflow (trik browser agar merestart animasi)
	        void modalDialog.offsetWidth; 
	
	        // Pasang animasi MASUK (Berputar & Zoom)
	        modalDialog.classList.add('animate__jackInTheBox');
	        
	        myModal.show();
	    }
	
	    function proseshapusDataRinci(masterModelClass, id, callback) {
	    	prosesDeleteData(masterModelClass, id, callback);
	    }
	
	    function prosesDeleteData(masterModelClass, id, callback) {
	        variable ++;
	        const modalHtml = `<div class="modal fade" id="modalHapus_`+variable+`" tabindex="-1" aria-hidden="true">
	                <div class="modal-dialog modal-dialog-centered modal-sm animate__animated animate__jackInTheBox animate__faster">
	                <div class="modal-content border-0 shadow rounded-4">
	                    <div class="modal-header border-0 pb-0">
	                        <h5 class="modal-title fw-bold text-danger"><%=Common.getBahasaConfig("Konfirmasi") %></h5>
	                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
	                    </div>
	                    <div class="modal-body text-center py-4">
	                        <i class="fas fa-exclamation-triangle text-warning mb-3" style="font-size: 2rem;"></i>
	                        <p class="mb-0 text-dark">
	                            <%=Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini ?") %>
	                        </p>
	                    </div>
	                    <div class="modal-footer border-0 pt-0 justify-content-center">
	                        <button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal") %></button>
	                        <button type="button" class="btn btn-danger rounded-pill px-4" 
	                                onclick="doDeleteData('`+masterModelClass+`', '`+id+`', 'modalHapus_`+variable+`', `+callback+`);">
	                            <%=Common.getBahasaConfig("Hapus") %>
	                        </button>
	                    </div>
	                </div>
	            </div>
	        </div>`;
	        
	        document.body.insertAdjacentHTML('beforeend', modalHtml);
	        
	        let myModalElement = document.getElementById('modalHapus_'+variable);
	        // Tampilkan Modal
	        const myModal = new bootstrap.Modal(myModalElement);
	        
	        // 1. Ambil Dialog untuk di-reset animasinya
	        let modalDialog = myModalElement.querySelector('.modal-dialog');
	        
	        // 3. Trik agar animasi "Masuk" selalu main setiap kali dibuka
	        // Kita hapus dulu class animasinya, lalu pasang lagi
	        modalDialog.classList.remove('animate__jackInTheBox');
	        modalDialog.classList.remove('animate__zoomOut'); // Hapus animasi keluar (jika ada)
	        
	        // Force Reflow (trik browser agar merestart animasi)
	        void modalDialog.offsetWidth; 
	
	        // Pasang animasi MASUK (Berputar & Zoom)
	        modalDialog.classList.add('animate__jackInTheBox');
	        
	        myModal.show();
	    }
	    
	
	    async function doDeleteData(masterModelClass, id, modalId, callback) {
	        var reqObj = { 
	            "action": "hapusDataRinci", 
	            "class": masterModelClass, 
	            "id": id 
	        };
	        var reqJson = JSON.stringify(reqObj);
	        
	        // PERUBAHAN: URL bersih dan mode POST JSON
	        const servletUrl = '<%=Common.ROOT%>/Data';
	        
	        try {
	            const response = await fetch(servletUrl, {
	                method: 'POST',
	                headers: {
	                    'Content-Type': 'application/json'
	                },
	                body: reqJson
	            });
	            const dataResponse = await response.json();
	            
	            if(dataResponse.status == '00'){ 
	                tampilkanToast('<%=Common.getBahasaConfigJS("Berhasil dihapus")%>', 'bg-success');
	                
	                // Tutup modal secara otomatis
	                const modalElement = document.getElementById(modalId);
	                const modalInstance = bootstrap.Modal.getInstance(modalElement);
	                if (modalInstance) modalInstance.hide();
	                
	                // Jalankan callback jika ada (misal: reload list)
	                if (typeof callback === "function") callback();
	                
	            } else { 
	                tampilkanToast(dataResponse.description, 'bg-danger'); 
	            }
	        } catch (error) {
	            console.error("Error deleting data:", error);
	            tampilkanToast("Terjadi kesalahan sistem", "bg-danger");
	        }
	    }
	    
	    
	    function loadContentIntoContainer(url, containerId) {
	        fetch(url).then(function(res) { return res.text(); }).then(function(html) {
	            var container = document.getElementById(containerId);
	            container.innerHTML = html;
	            Array.from(container.querySelectorAll("script")).forEach(function(oldScript) {
	                var newScript = document.createElement("script");
	                Array.from(oldScript.attributes).forEach(function(attr) { if (attr.name.toLowerCase() !== 'type') newScript.setAttribute(attr.name, attr.value); });
	                newScript.type = 'text/javascript';
	                newScript.appendChild(document.createTextNode(oldScript.innerHTML));
	                oldScript.parentNode.replaceChild(newScript, oldScript);
	            });
	        }).catch(function(err) { console.error(err); });
	    }
	    
	    
	    function prosesDeleteDataFile(masterModelClass, jenis, id, callback) {
	        variable ++;
	        const modalHtml = `<div class="modal fade" id="modalHapus_`+variable+`" tabindex="-1" aria-hidden="true">
	                <div class="modal-dialog modal-dialog-centered modal-sm animate__animated animate__jackInTheBox animate__faster">
	                <div class="modal-content border-0 shadow rounded-4">
	                    <div class="modal-header border-0 pb-0">
	                        <h5 class="modal-title fw-bold text-danger"><%=Common.getBahasaConfig("Konfirmasi") %></h5>
	                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
	                    </div>
	                    <div class="modal-body text-center py-4">
	                        <i class="fas fa-exclamation-triangle text-warning mb-3" style="font-size: 2rem;"></i>
	                        <p class="mb-0 text-dark">
	                            <%=Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini ?") %>
	                        </p>
	                    </div>
	                    <div class="modal-footer border-0 pt-0 justify-content-center">
	                        <button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal") %></button>
	                        <button type="button" class="btn btn-danger rounded-pill px-4" 
	                                onclick="doDeleteDataFile('`+masterModelClass+`', '`+jenis+`', '`+id+`', 'modalHapus_`+variable+`', `+callback+`);">
	                            <%=Common.getBahasaConfig("Hapus") %>
	                        </button>
	                    </div>
	                </div>
	            </div>
	        </div>`;
	        
	        document.body.insertAdjacentHTML('beforeend', modalHtml);
	        
	        let myModalElement = document.getElementById('modalHapus_'+variable);
	        // Tampilkan Modal
	        const myModal = new bootstrap.Modal(myModalElement);
	        
	        // 1. Ambil Dialog untuk di-reset animasinya
	        let modalDialog = myModalElement.querySelector('.modal-dialog');
	        
	        // 3. Trik agar animasi "Masuk" selalu main setiap kali dibuka
	        // Kita hapus dulu class animasinya, lalu pasang lagi
	        modalDialog.classList.remove('animate__jackInTheBox');
	        modalDialog.classList.remove('animate__zoomOut'); // Hapus animasi keluar (jika ada)
	        
	        // Force Reflow (trik browser agar merestart animasi)
	        void modalDialog.offsetWidth; 
	
	        // Pasang animasi MASUK (Berputar & Zoom)
	        modalDialog.classList.add('animate__jackInTheBox');
	        
	        myModal.show();
	    }
	    
	
	    async function doDeleteDataFile(masterModelClass, jenis, id, modalId, callback) {
	        var reqObj = { 
	            "action": "hapus_file", 
	            "clazz": masterModelClass, 
	            "jenis": jenis, 
	            "ref": id 
	        };
	        var reqJson = JSON.stringify(reqObj);
	        
	        // PERUBAHAN: URL bersih dan mode POST JSON
	        const servletUrl = '<%=Common.ROOT%>/Data';
	        
	        try {
	            const response = await fetch(servletUrl, {
	                method: 'POST',
	                headers: {
	                    'Content-Type': 'application/json'
	                },
	                body: reqJson
	            });
	            const dataResponse = await response.json();
	            
	            if(dataResponse.status == '00'){ 
	                tampilkanToast('<%=Common.getBahasaConfigJS("Berhasil dihapus")%>', 'bg-success');
	                
	                // Tutup modal secara otomatis
	                const modalElement = document.getElementById(modalId);
	                const modalInstance = bootstrap.Modal.getInstance(modalElement);
	                if (modalInstance) modalInstance.hide();
	                
	                // Jalankan callback jika ada (misal: reload list)
	                if (typeof callback === "function") callback();
	                
	            } else { 
	                tampilkanToast(dataResponse.description, 'bg-danger'); 
	            }
	        } catch (error) {
	            console.error("Error deleting data:", error);
	            tampilkanToast("Terjadi kesalahan sistem", "bg-danger");
	        }
	    }
	    
	    
	    async function doUpdate(masterModelClass, id, dataObjcallback, callback) {
	    	var data = null;
	    	if (typeof dataObjcallback === "function") {
	    		data = dataObjcallback();
	    	} else {
	    		data = dataObjcallback;
	    	}
	        var reqObj = { 
	            "action": "simpanDataRinci", 
	            "class": masterModelClass, 
	            "data": data, 
	            "id": id 
	        };
	        var reqJson = JSON.stringify(reqObj);
	        
	        // PERUBAHAN: URL bersih dan mode POST JSON
	        const servletUrl = '<%=Common.ROOT%>/Data';
	        
	        try {
	            const response = await fetch(servletUrl, {
	                method: 'POST',
	                headers: {
	                    'Content-Type': 'application/json'
	                },
	                body: reqJson
	            });
	            const dataResponse = await response.json();
	            
	            if(dataResponse.status == '00'){ 
	                tampilkanToast('<%=Common.getBahasaConfigJS("Berhasil diupdate")%>', 'bg-success');
	                
	                // Jalankan callback jika ada (misal: reload list)
	                if (typeof callback === "function") callback();
	                
	            } else { 
	                tampilkanToast(dataResponse.description, 'bg-danger'); 
	            }
	        } catch (error) {
	            console.error("Error deleting data:", error);
	            tampilkanToast("Terjadi kesalahan sistem", "bg-danger");
	        }
	    }
	    
	    
	    function prosesDeleteDataFileById(masterModelClass, jenis, id, callback) {
	        variable ++;
	        const modalHtml = `<div class="modal fade" id="modalHapus_`+variable+`" tabindex="-1" aria-hidden="true">
	                <div class="modal-dialog modal-dialog-centered modal-sm animate__animated animate__jackInTheBox animate__faster">
	                <div class="modal-content border-0 shadow rounded-4">
	                    <div class="modal-header border-0 pb-0">
	                        <h5 class="modal-title fw-bold text-danger"><%=Common.getBahasaConfig("Konfirmasi") %></h5>
	                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
	                    </div>
	                    <div class="modal-body text-center py-4">
	                        <i class="fas fa-exclamation-triangle text-warning mb-3" style="font-size: 2rem;"></i>
	                        <p class="mb-0 text-dark">
	                            <%=Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini ?") %>
	                        </p>
	                    </div>
	                    <div class="modal-footer border-0 pt-0 justify-content-center">
	                        <button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal") %></button>
	                        <button type="button" class="btn btn-danger rounded-pill px-4" 
	                                onclick="doDeleteDataFileById('`+masterModelClass+`', '`+jenis+`', '`+id+`', 'modalHapus_`+variable+`', `+callback+`);">
	                            <%=Common.getBahasaConfig("Hapus") %>
	                        </button>
	                    </div>
	                </div>
	            </div>
	        </div>`;
	        
	        document.body.insertAdjacentHTML('beforeend', modalHtml);
	        
	        let myModalElement = document.getElementById('modalHapus_'+variable);
	        // Tampilkan Modal
	        const myModal = new bootstrap.Modal(myModalElement);
	        
	        // 1. Ambil Dialog untuk di-reset animasinya
	        let modalDialog = myModalElement.querySelector('.modal-dialog');
	        
	        // 3. Trik agar animasi "Masuk" selalu main setiap kali dibuka
	        // Kita hapus dulu class animasinya, lalu pasang lagi
	        modalDialog.classList.remove('animate__jackInTheBox');
	        modalDialog.classList.remove('animate__zoomOut'); // Hapus animasi keluar (jika ada)
	        
	        // Force Reflow (trik browser agar merestart animasi)
	        void modalDialog.offsetWidth; 
	
	        // Pasang animasi MASUK (Berputar & Zoom)
	        modalDialog.classList.add('animate__jackInTheBox');
	        
	        myModal.show();
	    }
	    
	
	    async function doDeleteDataFileById(masterModelClass, jenis, id, modalId, callback) {
	        var reqObj = { 
	            "action": "hapus_file_by_id", 
	            "clazz": masterModelClass, 
	            "jenis": jenis, 
	            "id": id 
	        };
	        var reqJson = JSON.stringify(reqObj);
	        
	        // PERUBAHAN: URL bersih dan mode POST JSON
	        const servletUrl = '<%=Common.ROOT%>/Data';
	        
	        try {
	            const response = await fetch(servletUrl, {
	                method: 'POST',
	                headers: {
	                    'Content-Type': 'application/json'
	                },
	                body: reqJson
	            });
	            const dataResponse = await response.json();
	            
	            if(dataResponse.status == '00'){ 
	                tampilkanToast('<%=Common.getBahasaConfigJS("Berhasil dihapus")%>', 'bg-success');
	                
	                // Tutup modal secara otomatis
	                const modalElement = document.getElementById(modalId);
	                const modalInstance = bootstrap.Modal.getInstance(modalElement);
	                if (modalInstance) modalInstance.hide();
	                
	                // Jalankan callback jika ada (misal: reload list)
	                if (typeof callback === "function") callback();
	                
	            } else { 
	                tampilkanToast(dataResponse.description, 'bg-danger'); 
	            }
	        } catch (error) {
	            console.error("Error deleting data:", error);
	            tampilkanToast("Terjadi kesalahan sistem", "bg-danger");
	        }
	    }
	    
	
	    function hidupkanUlangPagingList(itemId){
	        const targetElement = document.getElementById(itemId);
	        
	        if (targetElement && window.List) {
	            // Ambil options dari atribut HTML
	            const rawOptions = targetElement.getAttribute('data-list');
	            
	            if (rawOptions) { // Cek null safety
	                const options = JSON.parse(rawOptions); 
	                
	                // --- PERBAIKAN 3: Gunakan itemId (bukan targetId yang undefined) ---
	                const myList = new List(itemId, options);
	                
	                // ============================================================
	                // MANUAL BINDING TOMBOL
	                // ============================================================
	                const nextBtn = targetElement.querySelector('[data-list-pagination="next"]');
	                const prevBtn = targetElement.querySelector('[data-list-pagination="prev"]');
	                const viewAllBtn = targetElement.querySelector('[data-list-view="*"]');
	                const viewLessBtn = targetElement.querySelector('[data-list-view="less"]');
	                const listInfo = targetElement.querySelector('[data-list-info]'); 
	                const defaultPageSize = options.page || 5;
	        
	                // Event Listener Tombol NEXT
	                if (nextBtn) {
	                    nextBtn.addEventListener('click', function(e) {
	                        e.preventDefault();
	                        const nextIndex = myList.i + myList.page;
	                        if (nextIndex <= myList.size()) {
	                            myList.show(nextIndex, myList.page);
	                        }
	                    });
	                }
	        
	                // Event Listener Tombol PREV
	                if (prevBtn) {
	                    prevBtn.addEventListener('click', function(e) {
	                        e.preventDefault();
	                        const prevIndex = myList.i - myList.page;
	                        if (prevIndex > 0) {
	                            myList.show(prevIndex, myList.page);
	                        } else {
	                            myList.show(1, myList.page); 
	                        }
	                    });
	                }
	                
	                // Logika Tombol "Lihat Semua"
	                if (viewAllBtn) {
	                    viewAllBtn.addEventListener('click', function(e) {
	                        e.preventDefault();
	                        myList.page = myList.size();
	                        myList.update();
	                        viewAllBtn.classList.add('d-none');
	                        if (viewLessBtn) viewLessBtn.classList.remove('d-none');
	                    });
	                }
	        
	                // Logika Tombol "Lihat Ringkas"
	                if (viewLessBtn) {
	                    viewLessBtn.addEventListener('click', function(e) {
	                        e.preventDefault();
	                        myList.page = defaultPageSize;
	                        myList.update();
	                        viewLessBtn.classList.add('d-none');
	                        if (viewAllBtn) viewAllBtn.classList.remove('d-none');
	                    });
	                }
	                
	                // Update Info Text
	                if (listInfo) {
	                     myList.on('updated', function(list) {
	                        var start = list.i;
	                        var end = list.i + list.visibleItems.length - 1;
	                        if (list.visibleItems.length === 0) { start = 0; end = 0; }
	                        listInfo.innerHTML = start + " to " + end + " of " + list.items.length;
	                     });
	                }
	
	                // Paksa update di akhir
	                myList.update();
	            }
	        }
	    }
	   
	
	    // Fungsi untuk membuka modal berdasarkan ID
	    function bukaModal(contentModal) {
	        
	        // Inisialisasi Modal Bootstrap sesuai permintaan Anda
	        const myModal = new bootstrap.Modal(document.getElementById(contentModal));
	        
	        // Tampilkan modal
	        myModal.show();
	    }
	    
	    
	   
	    
	    function tampilGambar(elementGambar) {
	        
	        variable ++;
	        const modalHtml = `<div class="modal fade" id="modalGambar`+variable+`" tabindex="-1" aria-hidden="true">
	                <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable animate__animated animate__jackInTheBox animate__faster"> <div class="modal-content shadow-lg"> <div class="modal-header border-0">
	                <h5 class="modal-title" style="font-weight: bold;font-size: 22px;" id="modalLabel`+variable+`"><i class="far fa-file-image"></i> <label id="labelHeader`+variable+`" class="modal-title" ><%=Common.getBahasaConfig("Gambar") %></label></h5>
	                        <button type="button" class="btn-close ms-auto" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup") %>"></button>
	                    </div>
	        
	                    <div class="modal-body text-center p-0">
	                        <img id="gambarPreview`+variable+`" src="" class="img-fluid rounded shadow-lg" alt="Preview Gambar">
	                        <p id="captionGambar`+variable+`" class="mt-2 fw-bold"></p>
	                    </div>
	        
	                </div>
	            </div>
	        </div>`;
	        
	        document.body.insertAdjacentHTML('beforeend', modalHtml);
	        
	        // Ambil URL dari gambar yang diklik
	        const srcGambar = elementGambar.src;
	        const judulGambar = !elementGambar.alt || elementGambar.alt == '' ? elementGambar.title : elementGambar.alt;
	        
	        var labelHeader = 'labelHeader'+variable;
	        document.getElementById(labelHeader).innerText = !elementGambar.alt || elementGambar.alt == '' ? elementGambar.title : elementGambar.alt;
	
	        // Masukkan URL ke dalam gambar di Modal
	        document.getElementById('gambarPreview'+variable).src = srcGambar;
	        document.getElementById('captionGambar'+variable).innerText = judulGambar;
	
	        let myModalElement = document.getElementById('modalGambar'+variable);
	        // Tampilkan Modal
	        const myModal = new bootstrap.Modal(myModalElement);
	        
	        // 1. Ambil Dialog untuk di-reset animasinya
	        let modalDialog = myModalElement.querySelector('.modal-dialog');
	        
	        // 3. Trik agar animasi "Masuk" selalu main setiap kali dibuka
	        // Kita hapus dulu class animasinya, lalu pasang lagi
	        modalDialog.classList.remove('animate__jackInTheBox');
	        modalDialog.classList.remove('animate__zoomOut'); // Hapus animasi keluar (jika ada)
	        
	        // Force Reflow (trik browser agar merestart animasi)
	        void modalDialog.offsetWidth; 
	
	        // Pasang animasi MASUK (Berputar & Zoom)
	        modalDialog.classList.add('animate__jackInTheBox');
	        
	        myModal.show();
	        
	        
	        // Event Listener: Saat modal akan ditutup (tombol close diklik/klik luar)
	        myModalElement.addEventListener('hide.bs.modal', function (e) {
	
	                // Cek apakah animasi "Keluar" sudah berjalan?
	                if (!modalDialog.classList.contains('animate__zoomOut')) {
	                    // Jika belum, JANGAN tutup dulu modalnya (preventDefault)
	                    e.preventDefault();
	
	                    // Ganti animasi Masuk menjadi animasi KELUAR (Zoom Out / Hilang)
	                    modalDialog.classList.remove('animate__jackInTheBox');
	                    modalDialog.classList.add('animate__zoomOut'); // Efek mengecil hilang
	
	                    // Tunggu animasi selesai (sekitar 200ms), baru tutup beneran
	                    setTimeout(() => {
	                        myModal.hide();
	                    }, 200);
	                }
	            });
	        }
	        
	    const lebarPopupDefault = '95%'; // Parameter lebar popup sesuai permintaan
	        
	     // Fungsi Pembantu untuk Mengatur CSS Lebar secara Dinamis
	        function aturLebarModal(elementDialog, lebarPopup) {
	            // Cek apakah layar mobile (kurang dari 768px/md)
	            if (window.innerWidth < 768) {
	                // Tampilan Mobile: Full 100%
	                elementDialog.style.maxWidth = '100%';
	                elementDialog.style.width = '100%';
	                elementDialog.style.margin = '0'; // Hilangkan margin bawaan bootstrap
	                elementDialog.style.height = '100%'; // Opsional: full height
	            } else {
	                // Tampilan Desktop: Sesuai parameter (95%)
	                elementDialog.style.maxWidth = lebarPopup;
	                elementDialog.style.width = lebarPopup;
	            }
	        }
	
	        // Listener opsional: Agar lebar menyesuaikan jika layar di-resize saat modal terbuka
	        window.addEventListener('resize', () => {
	            document.querySelectorAll('.modal.show .modal-dialog').forEach(dialog => {
	                aturLebarModal(dialog, lebarPopupDefault);
	            });
	        });
	        let variable = 0;
	        
	        
	        function loadModalContent(title, fileUrl) {
	            loadModalContentCustom(title, fileUrl, lebarPopupDefault);
	        }
	
	        
	        function loadModalContentCustom(title, fileUrl, lebarPopup) {
	            let simpandata = null;
	            loadModalContentCustomSimpan(title, fileUrl, lebarPopup, simpandata, null, null);
	        }
	        
	        function tutupSemuaModal() {
	            // 1. Cari semua elemen modal yang sedang terbuka
	            const openModals = document.querySelectorAll('.modal.show');
	            
	            // 2. Looping dan tutup satu per satu
	            openModals.forEach(modalElement => {
	                // Ambil instance yang sudah ada, BUKAN membuat baru
	                const modalInstance = bootstrap.Modal.getInstance(modalElement);
	                
	                // Jika instance ditemukan, sembunyikan modalnya
	                if (modalInstance) {
	                    modalInstance.hide();
	                }
	            });
	        }
	        
	        function simpanDanTutup(elemenTombol) {
	            // 1. Lakukan proses JS Anda di sini (misal: simpan data)
	            console.log("Data sedang disimpan...");

	            // 2. Cari elemen '.modal' terdekat yang membungkus tombol ini
	            let currentModalElement = elemenTombol.closest('.modal');

	            // 3. Panggil instance modal Bootstrap dan sembunyikan
	            if (currentModalElement) {
	                let modalInstance = bootstrap.Modal.getInstance(currentModalElement);
	                modalInstance.hide();
	            }
	        }
	        
	        function loadModalContentCustomSimpan(title, fileUrl, lebarPopup, simpandata, fungsiSimpan, modalId) {
	        	if (typeof window.variable === 'undefined') { window.variable = 0; }
	            window.variable++;
	            
	            var contentModal = modalId != null && modalId != '' ? modalId : 'contentModal' + variable;
	            const simpanButton = (simpandata == null ? "" : "<button type=\"button\" onclick=\"" + fungsiSimpan + "\" class=\"btn btn-primary\"><i class=\"far fa-save\"></i> <%=Common.getBahasaConfig("Simpan") %></button>");
	            
	            // Hitung z-index dinamis untuk modal ini (Dimulai dari 1050 + variable)
	            // Karena Toast memiliki z-index 99999, modal ini PASTI selalu di bawah Toast
	            var zIndexModal = 1050 + (window.variable * 2);
	            
	            const modalHtml = `<div class="modal fade" id="` + contentModal + `" tabindex="-1" aria-labelledby="modalLabel" aria-hidden="true" ` + (simpandata != null && simpandata == 'hidden' ? "" : `data-bs-backdrop="static"`) + ` style="z-index: ` + zIndexModal + `;">
	                    <div class="modal-dialog modal-dialog-scrollable mt-5 animate__animated animate__rotateInDownLeft">
	                        <div class="modal-content shadow-lg" >
	                            <div class="modal-header" style="background-color: rgb(180 212 255) !important;font-size: 22px;">
	                                <h5 class="modal-title" style="font-weight: bold;font-size: 22px;" id="modalLabel` + variable + `"><i class="far fa-list-alt"></i> <label id="labelHeader` + variable + `" class="modal-title" ><%=Common.getBahasaConfig("Daftar Pertemuan") %></label></h5>
	                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<%=Common.getBahasaConfig("Tutup") %>"></button>
	                            </div>
	                            <div class="modal-body" id="modalBodyContent` + variable + `">
	                                <div class="text-center py-5">
	                                    <div class="spinner-border text-primary" role="status">
	                                        <span class="visually-hidden"><%=Common.getBahasaConfig("Loading") %>...</span>
	                                    </div>
	                                </div>
	                            </div>
	                            ` + (simpandata != null && simpandata == 'hidden' ? `` : `
	                            <div class="modal-footer">
	                                ` + simpanButton + `
	                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><i class="far fa-window-close"></i> <%=Common.getBahasaConfig("Tutup") %></button>
	                            </div>`) + `
	                        </div>
	                    </div>
	           </div>`;

	            // 3. Masukkan HTML ke dalam Body
	            document.body.insertAdjacentHTML('beforeend', modalHtml);
	            
	            var labelHeader = 'labelHeader' + variable;
	            var modalBodyContent = 'modalBodyContent' + variable;
	            
	            // Inisialisasi Modal Bootstrap
	            const modalElement = document.getElementById(contentModal);
	            const myModal = new bootstrap.Modal(modalElement);
	            const modalDialog = modalElement.querySelector('.modal-dialog');
	            
	            // TRIK Z-INDEX BACKDROP: Sesuaikan lapisan gelap agar berada persis 1 level di bawah modal
	            modalElement.addEventListener('show.bs.modal', function () {
	                setTimeout(function() {
	                    var backdrops = document.querySelectorAll('.modal-backdrop');
	                    if (backdrops.length > 0) {
	                        backdrops[backdrops.length - 1].style.zIndex = zIndexModal - 1;
	                    }
	                }, 10);
	            });

	            // 3. Trik agar animasi "Masuk" selalu main setiap kali dibuka
	            modalDialog.classList.remove('animate__rotateInDownLeft');
	            modalDialog.classList.remove('animate__jackInTheBox');
	            modalDialog.classList.remove('animate__zoomOut'); // Hapus animasi keluar (jika ada)
	            
	            // Force Reflow (trik browser agar merestart animasi)
	            void modalDialog.offsetWidth; 

	            // Pasang animasi MASUK (Berputar & Zoom)
	            modalDialog.classList.add('animate__jackInTheBox');
	            modalDialog.classList.add('animate__rotateInDownLeft');
	            
	            if (typeof aturLebarModal === 'function') {
	                aturLebarModal(modalDialog, lebarPopup);
	            }
	            
	            // 1. Set Judul
	            document.getElementById(labelHeader).innerText = title;
	            
	            // 2. Tampilkan Loading State
	            document.getElementById(modalBodyContent).innerHTML = `
	                <div class="text-center py-4">
	                    <div class="spinner-border text-primary" role="status"></div>
	                    <p class="mt-2">Memuat data...</p>
	                </div>`;

	            // 3. Tampilkan Modal
	            myModal.show();
	            
	            // --- LOGIKA GOYANG (SHAKE) SAAT KLIK DI LUAR ---
	            modalElement.addEventListener('hidePrevented.bs.modal', function () {
	                modalDialog.classList.add('animate__headShake');
	                modalDialog.classList.remove('animate__rotateInDownLeft');
	                modalDialog.classList.remove('animate__jackInTheBox');
	                modalDialog.classList.remove('animate__zoomOut');

	                modalDialog.addEventListener('animationend', function () {
	                    modalDialog.classList.remove('animate__headShake');
	                }, { once: true });
	            });
	            
	            // Event Listener: Saat modal akan ditutup (tombol close diklik/klik luar)
	            modalElement.addEventListener('hide.bs.modal', function (e) {
	                modalDialog.classList.add('animate__rotateInDownLeft');
	                modalDialog.classList.add('animate__jackInTheBox');
	                
	                if (!modalDialog.classList.contains('animate__zoomOut')) {
	                    e.preventDefault();

	                    modalDialog.classList.add('animate__zoomOut'); // Efek mengecil hilang

	                    if (modalDialog._cleanup && typeof modalDialog._cleanup === 'function') {
	                        modalDialog._cleanup(); 
	                        delete modalDialog._cleanup; 
	                    }

	                    modalDialog.innerHTML = '';

	                    setTimeout(() => {
	                        myModal.hide();
	                        // Hapus elemen dari DOM setelah selesai transisi
	                        setTimeout(() => { modalElement.remove(); }, 300);
	                    }, 500);
	                }
	            });

	            // 4. Fetch Konten dari File Eksternal
	            fetch(fileUrl)
	                .then(response => {
	                    if (!response.ok) {
	                        throw new Error('Jaringan bermasalah atau file tidak ditemukan.');
	                    }
	                    return response.text();
	                })
	                .then(html => {
	                    const container = document.getElementById(modalBodyContent);
	                    container.innerHTML = ''; 

	                    const range = document.createRange();
	                    range.selectNode(container); 

	                    const fragment = range.createContextualFragment(html);
	                    container.appendChild(fragment);
	                    
	                })
	                .catch(error => {
	                    console.error('Error:', error);
	                    // Murni menggunakan string concatenation untuk menghindari Error JSP EL
	                    document.getElementById(modalBodyContent).innerHTML = `
	                        <div class="alert alert-danger">
	                            <strong>Gagal memuat konten.</strong><br>
	                            Pastikan Anda menjalankan file ini menggunakan <em>Local Server</em> (bukan klik ganda file html), 
	                            karena browser memblokir akses file lokal (CORS Policy).
	                            <hr>
	                            <small>Error: ` + error.message + `</small>
	                        </div>`;
	                });
	        }


	        function tampilkanToast(isiToast, jenis) {
	            variable++;
	            // Z-index Toast ke 99999 agar SUPER AMAN
	            const modalHtml = `<div class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 99999;">
	                
	                <div id="toast_` + variable + `" class="toast align-items-center text-white border-0 ` + jenis + ` fade" role="alert" aria-live="assertive" aria-atomic="true">
	                    <div class="d-flex">
	                        <div class="toast-body" id="toastMsg_` + variable + `">
	                            ` + isiToast + `
	                        </div>
	                        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="<%=Common.getBahasaConfig("Tutup") %>"></button>
	                    </div>
	                </div>

	            </div>`;
	            
	            document.body.insertAdjacentHTML('beforeend', modalHtml);

	            // B. Tampilkan Toast
	            const elemenToast = document.getElementById('toast_' + variable);
	            const toastBootstrap = new bootstrap.Toast(elemenToast);
	            toastBootstrap.show();
	            
	            // C. Hapus dari DOM secara otomatis saat toast hilang
	            elemenToast.addEventListener('hidden.bs.toast', function () {
	                elemenToast.parentElement.remove();
	            });
	        }
	</script>
	
    <div class="modal fade" id="authentication-modal" tabindex="-1" role="dialog" aria-labelledby="authentication-modal-label" aria-hidden="true">
          <div class="modal-dialog mt-6" role="document">
            <div class="modal-content border-0">
              <div class="modal-header px-5 position-relative modal-shape-header bg-shape">
                <div class="position-relative z-1">
                  <h4 class="mb-0 text-white" id="authentication-modal-label">Register</h4>
                  <p class="fs-10 mb-0 text-white">Please create your free Falcon account</p>
                </div>
                <div data-bs-theme="dark">
                  <button class="btn-close position-absolute top-0 end-0 mt-2 me-2" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
              </div>
              <div class="modal-body py-4 px-5">
                <form>
                  <div class="mb-3">
                    <label class="form-label" for="modal-auth-name"><%= Common.getBahasaConfig("Name") %></label>
                    <input class="form-control" type="text" autocomplete="on" id="modal-auth-name" />
                  </div>
                  <div class="mb-3">
                    <label class="form-label" for="modal-auth-email"><%= Common.getBahasaConfig("Email address") %></label>
                    <input class="form-control" type="email" autocomplete="on" id="modal-auth-email" />
                  </div>
                  <div class="row gx-2">
                    <div class="mb-3 col-sm-6">
                      <label class="form-label" for="modal-auth-password"><%= Common.getBahasaConfig("Password") %></label>
                      <input class="form-control" type="password" autocomplete="on" id="modal-auth-password" />
                    </div>
                    <div class="mb-3 col-sm-6">
                      <label class="form-label" for="modal-auth-confirm-password"><%= Common.getBahasaConfig("Confirm Password") %></label>
                      <input class="form-control" type="password" autocomplete="on" id="modal-auth-confirm-password" />
                    </div>
                  </div>
                  <div class="form-check">
                    <input class="form-check-input" type="checkbox" id="modal-auth-register-checkbox" />
                    <label class="form-label" for="modal-auth-register-checkbox">I accept the <a href="#!">terms </a>and <a class="white-space-nowrap" href="#!">privacy policy</a></label>
                  </div>
                  <div class="mb-3">
                    <button class="btn btn-primary d-block w-100 mt-3" type="submit" name="submit"><%= Common.getBahasaConfig("Register") %></button>
                  </div>
                </form>
                <div class="position-relative mt-5">
                  <hr />
                  <div class="divider-content-center">or register with</div>
                </div>
                <div class="row g-2 mt-2">
                  <div class="col-sm-6"><a class="btn btn-outline-google-plus btn-sm d-block w-100" href="#"><span class="fab fa-google-plus-g me-2" data-fa-transform="grow-8"></span> google</a></div>
                  <div class="col-sm-6"><a class="btn btn-outline-facebook btn-sm d-block w-100" href="#"><span class="fab fa-facebook-square me-2" data-fa-transform="grow-8"></span> facebook</a></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
    <jsp:include page="/WEB-INF/baru/modul/common/_ganti_password.jsp"/>
    <jsp:include page="/WEB-INF/baru/modul/common/_profile.jsp"/>
