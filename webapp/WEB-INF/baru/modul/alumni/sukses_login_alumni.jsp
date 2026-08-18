<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataMahasiswa"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.KrsMahasiswa"%>
<%@page import="ais.database.model.ChecklistHasilPenilaianUmum"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ProfileImageUtil"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.LinkedHashMap"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<jsp:include page="/WEB-INF/baru/modul/alumni/header_alumni.jsp"></jsp:include>

<%
    String rnd = Common.getGeneratedBarCode(7);
    Mahasiswa mahasiswaLog = (Mahasiswa) request.getSession().getAttribute("alumni_logged_in");
    
    if (mahasiswaLog == null) {
        out.print("<div class='container mt-5 mb-5 min-vh-50'><div class='alert alert-danger fw-bold text-center p-5 rounded-4 shadow-sm'><i class='fas fa-exclamation-triangle fa-2x mb-3 d-block'></i>" + Common.getBahasaConfig("Sesi telah berakhir. Silakan melakukan autentikasi ulang.") + "</div></div>");
%>
        <jsp:include page="/WEB-INF/baru/modul/alumni/footer_alumni.jsp"></jsp:include>
<%
        return;
    }

    BiodataMahasiswa biodata = mahasiswaLog.ambilBiodata();
    if (biodata == null) {
        out.print("<div class='container mt-5 mb-5 min-vh-50'><div class='alert alert-warning fw-bold text-center p-5 rounded-4 shadow-sm'><i class='fas fa-info-circle fa-2x mb-3 d-block'></i>" + Common.getBahasaConfig("Data rekam jejak biodata belum tersedia di pangkalan data.") + "</div></div>");
%>
        <jsp:include page="/WEB-INF/baru/modul/alumni/footer_alumni.jsp"></jsp:include>
<%
        return;
    }

    String urlFoto = ProfileImageUtil.getUrlFotoPengguna(new Tbmuser(mahasiswaLog), 180, 220);
    Session sess = null;
    
    Map<String, List<String[]>> mapTracer = new LinkedHashMap<String, List<String[]>>();
    Map<String, Map<String, List<ChecklistHasilPenilaianUmum>>> mapAngket = new LinkedHashMap<String, Map<String, List<ChecklistHasilPenilaianUmum>>>();

    try {
        sess = HibernateUtil.openSession();
        
        String tracerData = biodata.getParameterTambahanAlumni();
        if (tracerData != null && !tracerData.trim().isEmpty()) {
            String[] lines = tracerData.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split("<=>");
                if (parts.length > 0) {
                    String[] kelLabel = parts[0].split("->");
                    String kelompok = kelLabel[0].trim();
                    String label = kelLabel.length > 1 ? kelLabel[1].trim() : "";
                    String value = parts.length > 1 ? parts[1].trim() : "-";
                    
                    if (!mapTracer.containsKey(kelompok)) {
                        mapTracer.put(kelompok, new ArrayList<String[]>());
                    }
                    mapTracer.get(kelompok).add(new String[]{label, value});
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<ChecklistHasilPenilaianUmum> listAngketHasil = sess.createCriteria(ChecklistHasilPenilaianUmum.class)
            .add(Restrictions.isNull("tbmuserDinilai"))
            .add(Restrictions.isNull("pertemuanId"))
            .add(Restrictions.eq("mahasiswa", mahasiswaLog))
            .createAlias("checklistPenilaianUmum", "cpu")
            .createAlias("cpu.grupChecklistPenilaianUmum", "grup")
            .addOrder(Order.asc("tahunAkademik"))
            .addOrder(Order.desc("semesterStr"))
            .addOrder(Order.asc("grup.id"))
            .addOrder(Order.asc("cpu.isi"))
            .list();
            
        for (int i = 0; i < listAngketHasil.size(); i++) {
            ChecklistHasilPenilaianUmum h = listAngketHasil.get(i);
            String periode = h.getTahunAkademik() + " - " + h.getSemesterStr();
            String grup = h.getChecklistPenilaianUmum().getGrupChecklistPenilaianUmum().getIsi();
            
            if (!mapAngket.containsKey(periode)) mapAngket.put(periode, new LinkedHashMap<String, List<ChecklistHasilPenilaianUmum>>());
            if (!mapAngket.get(periode).containsKey(grup)) mapAngket.get(periode).put(grup, new ArrayList<ChecklistHasilPenilaianUmum>());
            
            mapAngket.get(periode).get(grup).add(h);
        }
        listAngketHasil.clear(); 

%>

<%! 
    private String renderRow(String label, String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("null")) return "";
        return "<tr>" +
               "<td class='text-secondary fw-bold align-middle py-2' style='width: 38%; border-bottom: 1px dashed #e9ecef;'>" + label + "</td>" +
               "<td class='text-dark align-middle py-2' style='border-bottom: 1px dashed #e9ecef;'><span class='me-2'>:</span> <span class='fw-medium'>" + value + "</span></td>" +
               "</tr>";
    }
%>

<style>
    .card-profile-<%=rnd%> { border: none; border-radius: 1.5rem; transition: all 0.3s ease; }
    .sticky-sidebar-<%=rnd%> { position: sticky; top: 25px; }
    .btn-action-<%=rnd%> { transition: transform 0.2s ease, box-shadow 0.2s ease; border-radius: 12px; }
    .btn-action-<%=rnd%>:hover { transform: translateY(-3px); box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important; }
    
    .group-header-<%=rnd%> { background: linear-gradient(90deg, #f8f9fa 0%, #ffffff 100%); border-left: 5px solid #0d6efd; padding: 12px 18px; margin-top: 35px; margin-bottom: 15px; font-weight: 800; color: #2c3e50; text-transform: uppercase; letter-spacing: 0.8px; font-size: 0.9rem; border-radius: 0 8px 8px 0; }
    .sub-header-<%=rnd%> { background: #fdfdfd; border-left: 4px solid #198754; padding: 8px 15px; font-weight: 700; color: #198754; font-size: 0.85rem; text-transform: uppercase; margin-top: 20px; margin-bottom: 10px; }
    .angket-header-<%=rnd%> { background: #fff8e1; border-left: 4px solid #ffc107; padding: 8px 15px; font-weight: 700; color: #b78a00; font-size: 0.85rem; text-transform: uppercase; margin-top: 20px; margin-bottom: 10px; }
    
    @media print {
        @page { margin: 1.5cm; size: A4 portrait; }
        body { background-color: #fff !important; }
        .container { width: 100% !important; max-width: 100% !important; padding: 0 !important; margin: 0 !important; }
        .d-print-none { display: none !important; }
        .col-lg-8 { width: 100% !important; flex: 0 0 100% !important; max-width: 100% !important; }
        .card { box-shadow: none !important; border: 1px solid #ddd !important; border-radius: 0 !important; padding: 20px !important; }
        .table { width: 100% !important; }
        .group-header-<%=rnd%> { background: #f0f0f0 !important; border-left: 4px solid #333 !important; color: #000 !important; -webkit-print-color-adjust: exact; }
    }
</style>

<div class="container py-4 py-md-5 animate__animated animate__fadeIn">
    <div class="row g-4">
        
        <div class="col-lg-4 d-print-none">
            <div class="sticky-sidebar-<%=rnd%>">
                <div class="card card-profile-<%=rnd%> shadow-lg overflow-hidden text-center p-4 p-md-5 bg-white">
                    <div class="mb-4">
                        <img src="<%= urlFoto %>" alt="<%= Common.getBahasaConfig("Pasfoto Profil Lulusan") %>" class="rounded-4 shadow-sm" style="width: 170px; height: 210px; object-fit: cover; border: 4px solid #0d6efd; padding: 3px;">
                    </div>
                    <h4 class="fw-bold text-dark mb-1 lh-base"><%= mahasiswaLog.getNama() %></h4>
                    <p class="text-primary fw-bold mb-3 fs-5"><i class="fas fa-id-card me-2"></i><%= mahasiswaLog.getNim() %></p>
                    
                    <div class="badge bg-success bg-opacity-10 text-success rounded-pill px-4 py-2 mb-4 fw-bold border border-success border-opacity-25" style="letter-spacing: 0.5px;">
                        <i class="fas fa-user-check me-2"></i><%= Common.getBahasaConfig("Autentikasi Lulusan Valid") %>
                    </div>

                    <div class="d-grid gap-3">
                        <button class="btn btn-primary btn-lg fw-bold shadow-sm btn-action-<%=rnd%> py-3" onclick="loadMenuKuesioner<%=rnd%>()">
                            <i class="fas fa-file-signature me-2"></i><%= Common.getBahasaConfig("Isi / Perbarui Kuesioner") %>
                        </button>
                        
                        <div class="row g-2">
                            <div class="col-6">
                                <button class="btn btn-outline-secondary fw-bold w-100 rounded-3 py-2 btn-action-<%=rnd%>" onclick="window.print()">
                                    <i class="fas fa-print me-2"></i><%= Common.getBahasaConfig("Cetak") %>
                                </button>
                            </div>
                            <div class="col-6">
                                <button class="btn btn-outline-danger fw-bold w-100 rounded-3 py-2 btn-action-<%=rnd%>" onclick="logoutAlumni<%=rnd%>()">
                                    <i class="fas fa-power-off me-2"></i><%= Common.getBahasaConfig("Keluar") %>
                                </button>
                            </div>
                        </div>
                    </div>
                    
                    <hr class="my-4 border-secondary opacity-25">
                    <div class="text-center px-2">
                        <p class="small text-muted mb-0 fst-italic lh-lg">"<%= Common.getBahasaConfig("Partisipasi Anda dalam pengisian Tracer Study berkontribusi besar bagi peningkatan akreditasi dan kualitas almamater kita tercinta.") %>"</p>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-8">
            <div class="card shadow-sm border-0 rounded-4 p-4 p-md-5 bg-white mb-4">
                
                <div class="d-flex align-items-center mb-4 pb-3 border-bottom border-light border-3">
                    <div class="bg-primary bg-opacity-10 p-3 rounded-circle me-4 text-primary shadow-sm d-print-none">
                        <i class="fas fa-file-contract fa-2x"></i>
                    </div>
                    <div>
                        <h3 class="fw-bold text-dark mb-1" style="letter-spacing: -0.5px;"><%= Common.getBahasaConfig("Lembar Informasi Profil & Rekam Jejak Lulusan") %></h3>
                        <p class="text-secondary small mb-0 fw-medium"><i class="fas fa-database me-2"></i><%= Common.getBahasaConfig("Data di bawah ini ditarik secara langsung dari Pangkalan Data Sistem Akademik Terpadu.") %></p>
                    </div>
                </div>

                <div class="table-responsive px-1">
                    <table class="table table-borderless w-100 mb-0" style="font-size: 0.95rem;">
                        
                        <tr><td colspan="2" class="p-0"><div class="group-header-<%=rnd%> mt-1"><i class="fas fa-graduation-cap me-2 text-primary"></i><%= Common.getBahasaConfig("Informasi Akademik Utama") %></div></td></tr>
                        <%= renderRow(Common.getBahasaConfig("Nomor Induk Mahasiswa"), mahasiswaLog.getNim()) %>
                        <%= renderRow(Common.getBahasaConfig("Nama Lengkap Lulusan"), mahasiswaLog.getNama()) %>
                        <%= renderRow(Common.getBahasaConfig("Tahun Angkatan"), (mahasiswaLog.getTahunangkatan() != null ? mahasiswaLog.getTahunangkatan().toString() : "")) %>
                        <%= renderRow(Common.getBahasaConfig("Fakultas / Departemen"), (mahasiswaLog.getJurusan() != null && mahasiswaLog.getJurusan().getFakultas() != null ? mahasiswaLog.getJurusan().getFakultas().getNama() : "")) %>
                        <%= renderRow(Common.getBahasaConfig("Program Studi"), (mahasiswaLog.getJurusan() != null ? mahasiswaLog.getJurusan().getNama() : "")) %>
                        <%= renderRow(Common.getBahasaConfig("Jenjang Studi"), (mahasiswaLog.getJenjang() != null ? mahasiswaLog.getJenjang().getNama() : "")) %>
                        
                        <tr><td colspan="2" class="p-0"><div class="group-header-<%=rnd%>"><i class="fas fa-user-circle me-2 text-primary"></i><%= Common.getBahasaConfig("Data Demografi Pribadi") %></div></td></tr>
                        <%= renderRow(Common.getBahasaConfig("Tempat, Tanggal Lahir"), mahasiswaLog.getTempatlahir() + ", " + (mahasiswaLog.getTanggallahir() != null ? Common.dateFormat6.get().format(mahasiswaLog.getTanggallahir()) : "")) %>
                        <%= renderRow(Common.getBahasaConfig("Jenis Kelamin"), mahasiswaLog.getKelamin()) %>
                        <%= renderRow(Common.getBahasaConfig("Nomor Induk Kependudukan (NIK)"), biodata.getNoIdentitas()) %>
                        <%= renderRow(Common.getBahasaConfig("Alamat Email Aktif"), mahasiswaLog.getEmail()) %>
                        <%= renderRow(Common.getBahasaConfig("Nomor Kontak / WhatsApp"), biodata.getHp()) %>
                        <%= renderRow(Common.getBahasaConfig("Alamat Domisili"), biodata.getAlamat() + (biodata.getKota() != null ? ", " + biodata.getKota().getNama() : "")) %>
                        
                        <% if (!mapTracer.isEmpty()) { %>
                            <tr><td colspan="2" class="p-0"><div class="group-header-<%=rnd%>"><i class="fas fa-chart-line me-2 text-primary"></i><%= Common.getBahasaConfig("Hasil Rekam Tracer Study Lulusan") %></div></td></tr>
                            
                            <% for (Map.Entry<String, List<String[]>> entryTracer : mapTracer.entrySet()) { %>
                                <tr><td colspan="2" class="p-0"><div class="sub-header-<%=rnd%>"><i class="fas fa-caret-right me-2"></i><%= entryTracer.getKey() %></div></td></tr>
                                <% for (int i = 0; i < entryTracer.getValue().size(); i++) { 
                                    String[] rowData = entryTracer.getValue().get(i);
                                    String safeValue = rowData[1].replace(";", ", "); 
                                    if(safeValue.endsWith(", ")) safeValue = safeValue.substring(0, safeValue.length() - 2);
                                %>
                                    <%= renderRow(rowData[0], safeValue) %>
                                <% } %>
                            <% } %>
                        <% } %>

                        <% if (!mapAngket.isEmpty()) { %>
                            <tr><td colspan="2" class="p-0"><div class="group-header-<%=rnd%>"><i class="fas fa-clipboard-check me-2 text-primary"></i><%= Common.getBahasaConfig("Hasil Rekam Angket & Evaluasi") %></div></td></tr>
                            
                            <% for (Map.Entry<String, Map<String, List<ChecklistHasilPenilaianUmum>>> entryAngket : mapAngket.entrySet()) { %>
                                <% for (Map.Entry<String, List<ChecklistHasilPenilaianUmum>> entryGrup : entryAngket.getValue().entrySet()) { %>
                                    <tr>
                                        <td colspan="2" class="p-0">
                                            <div class="angket-header-<%=rnd%>">
                                                <i class="fas fa-bookmark me-2"></i><%= entryGrup.getKey() %> 
                                                <span class="badge bg-warning text-dark ms-2 fw-bold" style="font-size:0.75rem;"><%= entryAngket.getKey() %></span>
                                            </div>
                                        </td>
                                    </tr>
                                    <% for (int i = 0; i < entryGrup.getValue().size(); i++) { 
                                        ChecklistHasilPenilaianUmum hasil = entryGrup.getValue().get(i);
                                        String pertanyaan = hasil.getChecklistPenilaianUmum() != null ? hasil.getChecklistPenilaianUmum().getIsi() : Common.getBahasaConfig("Pertanyaan Tidak Diketahui");
                                        String nilaiStr = "<span class='badge bg-primary rounded-pill px-3 py-1 fs-6'>" + (hasil.getNilai() != null ? hasil.getNilai() : "-") + "</span>";
                                        if (hasil.getKeterangan() != null && !hasil.getKeterangan().trim().isEmpty()) {
                                            nilaiStr += " <br><small class='text-muted mt-2 d-block fst-italic'>\"" + hasil.getKeterangan() + "\"</small>";
                                        }
                                    %>
                                        <%= renderRow(pertanyaan, nilaiStr) %>
                                    <% } %>
                                <% } %>
                            <% } %>
                        <% } %>
                    </table>
                </div>

                <div class="mt-5 p-3 rounded-3 bg-light text-center border border-light shadow-sm d-print-none">
                    <p class="text-secondary small mb-0 fw-bold">
                        <i class="fas fa-clock me-2 text-info"></i><%= Common.getBahasaConfig("Pembaruan Laporan Terakhir") %>: <%= Common.dateFormat2.get().format(new java.util.Date()) %>
                    </p>
                </div>
            </div>
        </div>
    </div>
</div>

<%
    } catch (Exception ex) {
        out.print("<div class='container mt-5'><div class='alert alert-danger m-4 text-center fw-bold'><i class='fas fa-times-circle me-2'></i>" + Common.getBahasaConfig("Terjadi kesalahan sistem saat memproses profil lulusan: ") + ex.getMessage() + "</div></div>");
        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit webapp/WEB-INF/baru/modul/alumni/sukses_login_alumni.jsp:263");
    } finally {
        if (sess != null) {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/sukses_login_alumni.jsp:266");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/sukses_login_alumni.jsp:267");}
        }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>

<script>
	const loadMenuKuesioner<%=rnd%> = async () => {
	    const modalId = 'modalKuesionerAlumni<%=rnd%>';
	    const contentId = 'contentKuesioner<%=rnd%>';
	    
	    if(document.getElementById(modalId)) { document.getElementById(modalId).remove(); }
	    
	    const modalHtml = 
	        '<div class="modal fade" id="' + modalId + '" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">' +
	            '<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">' +
	                '<div class="modal-content shadow-lg border-0 rounded-4" id="' + contentId + '">' +
	                    '<div class="p-5 text-center">' +
	                        '<div class="spinner-border text-primary mb-3" style="width: 3rem; height: 3rem;"></div>' +
	                        '<h5 class="text-muted"><%= Common.getBahasaConfig("Mempersiapkan Lembar Kuesioner...") %></h5>' +
	                    '</div>' +
	                '</div>' +
	            '</div>' +
	        '</div>';
	    
	    document.body.insertAdjacentHTML('beforeend', modalHtml);
	    
	    const modalElement = document.getElementById(modalId);
	    const modalInstance = new bootstrap.Modal(modalElement);
	    modalInstance.show();
	    
	    modalElement.addEventListener('hidden.bs.modal', function () { this.remove(); });
	    
        try {
            const res = await fetch('<%= Common.ROOT %>/alumni?hanya_tampil_jsp=true&p=alumni&s=_isi_kuesioner_alumni&baru=true');
            if (res.ok) {
                const html = await res.text();
                const container = document.getElementById(contentId);
                container.innerHTML = html;
                
                const scriptsArray = Array.from(container.getElementsByTagName('script'));
                for (let i = 0; i < scriptsArray.length; i++) {
                    const oldScript = scriptsArray[i];
                    const scriptNode = document.createElement('script');
                    var srcEff = oldScript.src || oldScript.getAttribute('data-rocketlazyloadscript') || '';
                    Array.from(oldScript.attributes).forEach(attr => { if (attr.name.toLowerCase() !== 'type') scriptNode.setAttribute(attr.name, attr.value); });
                    scriptNode.type = 'text/javascript';
                    if (srcEff) {
                        scriptNode.src = srcEff;
                        document.body.appendChild(scriptNode);
                    } else {
                        scriptNode.text = oldScript.innerHTML;
                        document.body.appendChild(scriptNode).parentNode.removeChild(scriptNode);
                    }
                }
            } else {
                throw new Error("Gagal load JSP");
            }
        } catch(e) {
            document.getElementById(contentId).innerHTML = 
                '<div class="p-5 text-center text-danger">' +
                    '<i class="fas fa-exclamation-triangle fa-3x mb-3"></i>' +
                    '<h5><%= Common.getBahasaConfig("Gagal memuat formulir kuesioner dari pangkalan data.") %></h5>' +
                    '<button class="btn btn-sm btn-outline-secondary mt-3" data-bs-dismiss="modal"><%= Common.getBahasaConfig("Tutup") %></button>' +
                '</div>';
        }
	};

    const logoutAlumni<%=rnd%> = () => {
        const pesanKonfirmasi = '<%= Common.getBahasaConfigJS("Apakah Anda yakin ingin mengakhiri sesi dan keluar dari portal?") %>';
        const urlLogout = '<%=Common.ROOT%>/alumni?hanya_tampil_jsp=true&p=alumni&s=tracer_study&action=logout&baru=true';
        
        const eksekusiLogout = async () => {
            try {
                const res = await fetch(urlLogout, { method: 'POST' });
                const textResp = await res.text();
                let result;
                try { result = JSON.parse(textResp); } catch(err) { throw new Error("JSON Parse Error"); }
                
                if (result && result.status === 'sukses') {
                    if (typeof tampilkanToast === "function") tampilkanToast(result.pesan, 'bg-success text-white');
                    setTimeout(() => { window.location.href = '<%=Common.ROOT%>/alumni'; }, 700);
                } else {
                    if (typeof tampilkanToast === "function") tampilkanToast('<%= Common.getBahasaConfigJS("Gagal memproses permintaan keluar.") %>', 'bg-danger text-white');
                }
            } catch (e) {
                if (typeof tampilkanToast === "function") tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi kesalahan pada peladen saat mencoba keluar.") %>', 'bg-danger text-white');
            }
        };

        if (typeof showConfirmModal === "function") {
            showConfirmModal(pesanKonfirmasi, eksekusiLogout);
        } else {
            if (confirm(pesanKonfirmasi)) { eksekusiLogout(); }
        }
    };
    
    if (typeof tampilkanToast !== 'function') {
        window.tampilkanToast = function(msg, bgClass) { alert(msg); };
    }
</script>

<jsp:include page="/WEB-INF/baru/modul/alumni/footer_alumni.jsp"></jsp:include>