<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.io.*" %>
<%
Mahasiswa mahasiswa = (Mahasiswa) (request.getParameter("mahasiswa") == null || request.getParameter("mahasiswa").equalsIgnoreCase("null") ? null : GeneralValueObject.ambilData(Mahasiswa.class, request.getParameter("mahasiswa").trim(), true));
Dosen dosen = (Dosen) (request.getParameter("dosen") == null || request.getParameter("dosen").equalsIgnoreCase("null") ? null : GeneralValueObject.ambilData(Dosen.class, request.getParameter("dosen").trim(), true));
Siswa siswa = (Siswa) (request.getParameter("siswa") == null || request.getParameter("siswa").equalsIgnoreCase("null") ? null : GeneralValueObject.ambilData(Siswa.class, request.getParameter("siswa").trim(), true));

Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, request.getParameter("pertemuan").trim(), true);
String contentModal = request.getParameter("contentModal").trim();

String _siswaParam = request.getParameter("siswa");
String currentUrl = Common.ROOT+"/baru?hanya_tampil_jsp=true&p=elearning&s=ubah_status_kehadiran&pertemuan=" + request.getParameter("pertemuan").toString()+"&mahasiswa="+request.getParameter("mahasiswa")+"&dosen="+request.getParameter("dosen")+(_siswaParam != null && !_siswaParam.equalsIgnoreCase("null") ? "&siswa="+_siswaParam : "")+"&contentModal="+contentModal;


if(pertemuan != null && pertemuan.getId() != null){
	

	
	Date m = null;
	try {
		m = mahasiswa != null ? Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(mahasiswa.getId())) : dosen != null ? Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(dosen.getId())) : siswa != null ? Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiMulai(siswa.getId())) : null;
	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:34");
	}
	
	if(m == null){
		try {
			m = Common.timeFormat2.get().parse(pertemuan.getWaktuMulai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:40");
		}
	}
	if(m == null){
		try {
			m = Common.timeFormat.get().parse(pertemuan.getWaktuMulai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:46");
		}
	}
	Date s = null;
	try {
		s = mahasiswa != null ? Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(mahasiswa.getId())) : dosen != null ? Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(dosen.getId())) : siswa != null ? Common.timeFormat2.get().parse(pertemuan.retreiveAbsensiSampai(siswa.getId())) : null;
	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:52");
	}
	
	if(s == null){
		try {
			s = Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:58");
		}
	}
	if(s == null){
		try {
			s = Common.timeFormat.get().parse(pertemuan.getWaktuSelesai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:64");
		}
	}
	
	Statusabsensi statusabsensi = (Statusabsensi) (mahasiswa != null ? ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(mahasiswa.getId())) : dosen != null ? ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(dosen.getId())) : siswa != null ? ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(siswa.getId())) : null);
	if (statusabsensi == null) {
		statusabsensi = ConstantValues.BELUM_ABSEN;
	}
	
	String ket = mahasiswa != null ? pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId()) : dosen != null ? pertemuan.retreiveAbsensiKeterangan(dosen.getId()) : siswa != null ? pertemuan.retreiveAbsensiKeterangan(siswa.getId()) : "";
	ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
    String method = request.getMethod();
    if ("POST".equalsIgnoreCase(method)) {
        // Set response type ke JSON agar mudah dibaca Javascript
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter outJson = response.getWriter();

        try {
            // 1. Ambil data dari Parameter Request (Pengganti .getValue() komponen UI)
            String statusKode = request.getParameter("status_kehadiran"); 
            String waktuMulaiStr = request.getParameter("waktu_mulai");
            String waktuSelesaiStr = request.getParameter("waktu_selesai");
            String keteranganStr = request.getParameter("keterangan");
            
            // --- MULAI ADAPTASI LOGIKA JAVA ANDA ---
            
            // Simulasi instansiasi objek (Sesuaikan dengan Class asli di project Anda)
            // Pertemuan pertemuan = new Pertemuan(); 
            // Mahasiswa mahasiswa = new Mahasiswa(); 
            
            // Format waktu (Asumsi Common.timeFormat2.get() adalah HH:mm)
           
            Date waktuMulaiDate = null;
            Date waktuSelesaiDate = null;

            if (waktuMulaiStr != null && !waktuMulaiStr.isEmpty()) {
                try {
                   waktuMulaiDate = Common.dateFormat1.get().parse(waktuMulaiStr);
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:103"); /* Handle error */ }
                try {
                    waktuMulaiDate = Common.timeFormat.get().parse(waktuMulaiStr);
                 } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:106"); /* Handle error */ }
           } else {
               // Logika fallback jika waktu kosong (ambil dari default pertemuan)
               // if (pertemuan.getWaktuMulai() != null) ...
           }

           if (waktuSelesaiStr != null && !waktuSelesaiStr.isEmpty()) {
                try {
                   waktuSelesaiDate = Common.dateFormat1.get().parse(waktuSelesaiStr);
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:115"); /* Handle error */ }
                try {
                    waktuSelesaiDate = Common.timeFormat.get().parse(waktuSelesaiStr);
                 } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:118"); /* Handle error */ }
           } else {
               // Logika fallback
               // if (pertemuan.getWaktuSelesai() != null) ...
           }
           
            // Simulasi populate dan save (Ganti dengan method asli Anda)
            
            Statusabsensi statusObj = (Statusabsensi) GeneralValueObject.ambilData(Statusabsensi.class, statusKode.trim(), true);
            if(statusObj != null){
            	Session mySession = HibernateUtil.openSession();
            	try {
            	mySession.refresh(pertemuan);
            	if(mahasiswa != null){
		            pertemuan.populate(
		                mahasiswa.getId(),
		                statusObj,
		                keteranganStr,
		                null,
		                waktuMulaiDate == null ? "" : Common.timeFormat2.get().format(waktuMulaiDate),
		                waktuSelesaiDate == null ? "" : Common.timeFormat2.get().format(waktuSelesaiDate),
		                "Mahasiswa"
		            );
            	} else if(dosen != null){
            		pertemuan.populate(
            				dosen.getId(),
    		                statusObj,
    		                keteranganStr,
    		                null,
    		                waktuMulaiDate == null ? "" : Common.timeFormat2.get().format(waktuMulaiDate),
    		                waktuSelesaiDate == null ? "" : Common.timeFormat2.get().format(waktuSelesaiDate),
    		                "Dosen"
    		            );
            	} else if(siswa != null){
            		pertemuan.populate(
            				siswa.getId(),
    		                statusObj,
    		                keteranganStr,
    		                null,
    		                waktuMulaiDate == null ? "" : Common.timeFormat2.get().format(waktuMulaiDate),
    		                waktuSelesaiDate == null ? "" : Common.timeFormat2.get().format(waktuSelesaiDate),
    		                "Siswa"
    		            );
            	}
	            mySession.getTransaction().begin();
	            Common.refreshUpdate(mySession, pertemuan);
	            mySession.getTransaction().commit();
            	} finally {
	            // Pastikan session selalu tertutup walau terjadi exception di atas
	            try { if (mySession.getTransaction() != null && mySession.getTransaction().isActive()) mySession.getTransaction().rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:167");}
	            HibernateUtil.closeSessionQuietly(mySession);
            	}
            }
            // --- SELESAI ADAPTASI LOGIKA JAVA ANDA ---

            // Kirim respon sukses ke AJAX
            outJson.print("{\"status\": \"success\", \"message\": \""+Common.getBahasaConfig("Ubah data kehadiran berhasil dilakukan!")+"\"}");
            
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/ubah_status_kehadiran.jsp:177");
            outJson.print("{\"status\": \"error\", \"message\": \"Gagal menyimpan data kehadiran: " + e.getMessage() + "\"}");
        }
        
        outJson.flush();
        return; // Hentikan eksekusi agar HTML tidak ikut ter-render saat request AJAX
    }
%>



    <div class="card" style="max-width: 500px; margin: auto;">
        <div class="card-header bg-primary text-white">
            <%=Common.getBahasaConfig("Form Kehadiran") %>
        </div>
        <div class="card-body">
            

            <form id="formAbsensi<%=contentModal%>">
                <div class="mb-3">
                    <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Status Kehadiran") %></label>
                    <select class="form-select" id="statusKehadiran<%=contentModal%>" name="status_kehadiran" onchange="cekStatus<%=contentModal%>()">
                        <option <%=statusabsensi.getId().equals(ConstantValues.MASUK.getId()) ? "selected" : "" %> value="<%=ConstantValues.MASUK.getId()%>"><%=Common.getBahasaConfig("Hadir") %></option>
                        <option <%=statusabsensi.getId().equals(ConstantValues.IZIN.getId()) ? "selected" : "" %> value="<%=ConstantValues.IZIN.getId()%>"><%=Common.getBahasaConfig("Izin") %></option>
                        <option <%=statusabsensi.getId().equals(ConstantValues.SAKIT.getId()) ? "selected" : "" %> value="<%=ConstantValues.SAKIT.getId()%>"><%=Common.getBahasaConfig("Sakit") %></option>
                        <option <%=statusabsensi.getId().equals(ConstantValues.TIDAK_ADA_ALASAN.getId()) ? "selected" : "" %> value="<%=ConstantValues.TIDAK_ADA_ALASAN.getId()%>"><%=Common.getBahasaConfig("Alpha/Tanpa Keterangan") %></option>
                        <option <%=statusabsensi.getId().equals(ConstantValues.BELUM_ABSEN.getId()) ? "selected" : "" %> value="<%=ConstantValues.BELUM_ABSEN.getId()%>"><%=Common.getBahasaConfig("Belum Ditentukan") %></option>
                    </select>
                </div>

                <div class="mb-3" id="groupWaktu<%=contentModal%>">
                    <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Waktu") %></label>
                    <div class="input-group input-group-sm mt-2">
                        <span class="input-group-text"><%=Common.getBahasaConfig("Pukul") %></span>
                        <input type="time" class="form-control" id="waktuMulai<%=contentModal%>" name="waktu_mulai" value="<%=(m==null?"":Common.dateFormat1.get().format(m))%>">
                        <span class="input-group-text">s.d</span>
                        <input type="time" class="form-control" id="waktuSelesai<%=contentModal%>" name="waktu_selesai" value="<%=(s==null?"":Common.dateFormat1.get().format(s))%>">
                    </div>
                </div>

                <div class="mb-3">
                    <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Keterangan Tambahan") %></label>
                    <textarea class="form-control" id="keterangan<%=contentModal%>" name="keterangan" rows="3"><%=ket %></textarea>
                </div>
            </form>

        </div>
    </div>

    <script>
        // Fungsi Opsional: Disable waktu jika status bukan Hadir (sesuai logika Java)
        function cekStatus<%=contentModal%>() {
            var status = document.getElementById("statusKehadiran<%=contentModal%>").value;
            var inputMulai = document.getElementById("waktuMulai<%=contentModal%>");
            var inputSelesai = document.getElementById("waktuSelesai<%=contentModal%>");

            if (status !== "<%=ConstantValues.MASUK.getId()%>") {
                inputMulai.disabled = true;
                inputSelesai.disabled = true;
                inputMulai.value = "";
                inputSelesai.value = "";
            } else {
                inputMulai.disabled = false;
                inputSelesai.disabled = false;
                if(inputMulai.value == ''){
                	inputMulai.value = '<%=(m==null?"":Common.dateFormat1.get().format(m))%>';
                }
                if(inputSelesai.value == ''){
                	inputSelesai.value = '<%=(s==null?"":Common.dateFormat1.get().format(s))%>';
                }
                // Set default value back if needed
            }
        }
        
        
        setTimeout(() => {
        	cekStatus<%=contentModal%>();
		}, 500);

        // FUNGSI UTAMA: Simpan Data via AJAX
        function simpanDataAbsen<%=contentModal%>() {
            // 1. Ambil Form Data
            var form = document.getElementById("formAbsensi<%=contentModal%>");
            var formData = new FormData(form);
            
	         // KONVERSI KE URLSearchParams
	         // Ini mengubah format dari multipart/form-data menjadi application/x-www-form-urlencoded
	         // agar request.getParameter() di Java bisa membacanya.
	         var dataKirim = new URLSearchParams(formData);
            
            // 2. Kirim via Fetch API (AJAX Modern)
            fetch("<%=currentUrl%>", {
                method: 'POST',
                body: dataKirim
            })
            .then(response => response.json()) // Harap respon berupa JSON
            .then(data => {
            	
            	// --- CARA 1: LOG DI SINI ---
                console.info(data); // <--- Tambahkan baris ini
                // ---------------------------
                if (data.status === "success") {
                    tampilkanToast(data.message,'bg-success');
                    reloadAbsen<%=pertemuan.getId()%>(true);
                 // 1. Ambil elemen modal berdasarkan ID
                    var myModalElement = document.getElementById('<%=contentModal%>');

                    // 2. Dapatkan instance modal yang sedang aktif
                    // Gunakan 'getInstance' jika modal sudah terbuka.
                    var modalInstance = bootstrap.Modal.getInstance(myModalElement); 

                    // ATAU gunakan 'getOrCreateInstance' jika Anda ragu apakah instance-nya sudah ada atau belum
                    // var modalInstance = bootstrap.Modal.getOrCreateInstance(myModalElement);

                    // 3. Panggil fungsi hide()
                    if (modalInstance) {
                        modalInstance.hide();
                    }
                    
                } else {
                   
                    tampilkanToast(data.message,'bg-danger');
                }
            })
            .catch(error => {
                //console.error('Error:', error);
                tampilkanToast("Terjadi kesalahan "+error,'bg-danger');
            })
            .finally(() => {
               
            });
        }
    </script>
<%
}
%>