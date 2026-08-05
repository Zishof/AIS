<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.StatusPertemuan"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // ====================================================================================
    // 1. VALIDASI PENGGUNA DAN IDENTITAS PARAMETER
    // ====================================================================================
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) { return; }

    String contentModal = request.getParameter("contentModal");
    String rnd = contentModal != null && !contentModal.trim().isEmpty() ? contentModal : Common.getGeneratedBarCode(6);
    
    String idPerkuliahan = request.getParameter("perkuliahan");
    if (idPerkuliahan == null || idPerkuliahan.trim().isEmpty()) {
        out.print("<div class='alert alert-danger shadow-sm border-0 rounded-3 m-3'><i class='fas fa-exclamation-circle me-2'></i>" + Common.getBahasaConfig("Parameter perkuliahan tidak valid atau kosong.") + "</div>");
        return;
    }
    
    String paramHanyaAktif = request.getParameter("hanyaYangAktif");
    boolean isHanyaAktif = (paramHanyaAktif == null || paramHanyaAktif.equalsIgnoreCase("true"));

    // FORMAT BAKU HTML5 UNTUK INPUT TYPE DATE
    SimpleDateFormat html5DateFormat = new SimpleDateFormat("yyyy-MM-dd");

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
        Perkuliahan perkuliahan = null;
        List<Pertemuan> listPertemuan = new ArrayList<Pertemuan>();
        List<StatusPertemuan> listStatus = null;
        
        try {
            perkuliahan = (Perkuliahan) sess.get(Perkuliahan.class, Long.parseLong(idPerkuliahan));
        } catch(NumberFormatException ne) {
            out.print("<div class='alert alert-danger shadow-sm border-0 rounded-3 m-3'><i class='fas fa-exclamation-circle me-2'></i>" + Common.getBahasaConfig("Format identitas perkuliahan tidak sesuai.") + "</div>");
            return;
        }
        
        if (perkuliahan == null) {
            out.print("<div class='alert alert-danger shadow-sm border-0 rounded-3 m-3'><i class='fas fa-exclamation-triangle me-2'></i>" + Common.getBahasaConfig("Data perkuliahan tidak ditemukan di dalam sistem pangkalan data.") + "</div>");
            return;
        }

        // ==============================================================================
        // 2. LOGIKA PENGAMBILAN DATA PERTEMUAN
        // ==============================================================================
        boolean urutManual = perkuliahan.getUrutkanotomatis() != null && !perkuliahan.getUrutkanotomatis();
        List<Long> pertemuanssId = new ArrayList<Long>();
        
        if (!isHanyaAktif) {
            pertemuanssId = sess.createCriteria(Pertemuan.class)
                .setProjection(Projections.property("id"))
                .addOrder(urutManual ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
                .add(Restrictions.isNotNull("tanggal"))
                .addOrder(Order.asc("id"))
                .add(Restrictions.eq("perkuliahan", perkuliahan))
                .list();
        } else {
            if (perkuliahan.udah()) {
                Object[] a = perkuliahan.ambilPertemuan(0, 1000, false);
                if (a != null && a.length > 0 && a[0] != null) {
                    pertemuanssId = (List<Long>) a[0];
                }
            } else {
                List<Pertemuan> pertemuansTemp = sess.createCriteria(Pertemuan.class)
                    .add(isHanyaAktif ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)) : Restrictions.sqlRestriction("true"))
                    .addOrder(urutManual ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
                    .add(Restrictions.isNotNull("tanggal"))
                    .addOrder(Order.asc("id"))
                    .add(Restrictions.eq("perkuliahan", perkuliahan))
                    .list();
                    
                perkuliahan.reInitPertemuan(pertemuansTemp, sess);
                pertemuansTemp.clear();
                pertemuansTemp = null;

                Object[] a = perkuliahan.ambilPertemuan(0, 1000, false);
                if (a != null && a.length > 0 && a[0] != null) {
                    pertemuanssId = (List<Long>) a[0];
                }
            }
        }
        
        if (pertemuanssId != null && !pertemuanssId.isEmpty()) {
            for (Long pId : pertemuanssId) {
                if (pId != null) {
                    Pertemuan p = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pId.toString(), true);
                    if (p != null && p.getId() != null) {
                        listPertemuan.add(p);
                    }
                }
            }
        }
            
        listStatus = sess.createCriteria(StatusPertemuan.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
            .list();
        
        // ==============================================================================
        // 3. VARIABEL PENGATURAN FORMULIR MASSAL (DENGAN PENYESUAIAN FORMAT HTML5)
        // ==============================================================================
        String tglMulai = perkuliahan.getTanggalMulaiPerkuliahan() != null ? html5DateFormat.format(perkuliahan.getTanggalMulaiPerkuliahan()) : "";
        
        // PENYESUAIAN WAKTU PERKULIAHAN (Mengubah '.' menjadi ':' untuk <input type="time">)
        String wMulaiPerkuliahan = perkuliahan.getWaktuMulai() != null ? perkuliahan.getWaktuMulai().replace(".", ":").trim() : "";
        if (wMulaiPerkuliahan.length() >= 5) wMulaiPerkuliahan = wMulaiPerkuliahan.substring(0, 5);
        
        String wSelesaiPerkuliahan = perkuliahan.getWaktuSelesai() != null ? perkuliahan.getWaktuSelesai().replace(".", ":").trim() : "";
        if (wSelesaiPerkuliahan.length() >= 5) wSelesaiPerkuliahan = wSelesaiPerkuliahan.substring(0, 5);
%>

<style>
    .grid-table-<%=rnd%> th { white-space: nowrap; background-color: #f4f6f9; border-bottom: 2px solid #dee2e6; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.5px; color: #495057;}
    .grid-table-<%=rnd%> td { vertical-align: top; padding: 0.5rem; }
    .grid-table-<%=rnd%> .form-control, .grid-table-<%=rnd%> .form-select { font-size: 0.85rem; border-radius: 6px; border: 1px solid #ced4da; transition: all 0.2s ease-in-out; }
    .grid-table-<%=rnd%> .form-control:focus, .grid-table-<%=rnd%> .form-select:focus { border-color: #0d6efd; box-shadow: 0 0 0 0.25rem rgba(13,110,253,.15); background-color: #fff; }
    .grid-table-<%=rnd%> textarea.form-control { min-height: 65px; resize: vertical; background-color: #fcfcfc; }
    .grid-table-<%=rnd%> textarea.form-control:focus { background-color: #ffffff; }
    .w-min-100 { min-width: 100px !important; }
    .w-min-150 { min-width: 150px !important; }
    .w-min-200 { min-width: 200px !important; }
    .w-min-250 { min-width: 250px !important; }
    .custom-shadow-<%=rnd%> { box-shadow: 0 0.25rem 0.75rem rgba(0,0,0,.05) !important; }
</style>

<div class="container-fluid p-3" id="form-container-<%=rnd%>">
    
    <div class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-4 flex-wrap gap-3">
        <div>
            <h5 class="fw-bold mb-1 text-primary"><i class="fas fa-calendar-alt me-2"></i><%=Common.getBahasaConfig("Konfigurasi Jadwal Pertemuan")%></h5>
            <span class="text-muted small fw-semibold bg-light px-2 py-1 rounded border"><%=perkuliahan.getMatakuliah() != null ? perkuliahan.getMatakuliah().getKode() + " - " + perkuliahan.getMatakuliah().getNama() : "-"%></span>
        </div>
        
        <div class="d-flex flex-wrap align-items-center gap-3">
            <div class="form-check form-switch bg-white border px-4 py-2 rounded-pill custom-shadow-<%=rnd%> mb-0 d-flex align-items-center">
                <input class="form-check-input mt-0 me-2" type="checkbox" id="chkHanyaAktif_<%=rnd%>" style="cursor: pointer; width: 35px; height: 18px;" <%=isHanyaAktif ? "checked" : ""%> onchange="reloadDataPertemuan<%=rnd%>()">
                <label class="form-check-label small fw-bold text-secondary mb-0" for="chkHanyaAktif_<%=rnd%>" style="cursor: pointer;"><%=Common.getBahasaConfig("Hanya Tampilkan Yang Aktif")%></label>
            </div>
            
            <div class="btn-group custom-shadow-<%=rnd%> rounded-pill p-1 bg-white border">
                <button type="button" class="btn btn-primary btn-sm rounded-pill fw-bold px-3 border-0" onclick="bukaModalTambah<%=rnd%>()">
                    <i class="fas fa-plus-circle me-1"></i> <%=Common.getBahasaConfig("Tambah Pertemuan")%>
                </button>
                <button type="button" class="btn btn-info btn-sm text-white rounded-pill fw-bold px-3 ms-1 border-0" data-bs-toggle="collapse" data-bs-target="#collapseGenerateMassal_<%=rnd%>">
                    <i class="fas fa-magic me-1"></i> <%=Common.getBahasaConfig("Hasilkan Massal")%>
                </button>
                <button type="button" id="btnHapusSemua_<%=rnd%>" class="btn btn-danger btn-sm rounded-pill fw-bold px-3 ms-1 border-0" onclick="hapusSemuaPertemuan<%=rnd%>()">
                    <i class="fas fa-trash-alt me-1"></i> <%=Common.getBahasaConfig("Hapus Semua")%>
                </button>
                <button type="button" class="btn btn-light btn-sm text-secondary rounded-circle ms-1 border" onclick="reloadDataPertemuan<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Data")%>">
                    <i class="fas fa-sync-alt"></i>
                </button>
            </div>
        </div>
    </div>

    <div class="collapse mb-4" id="collapseGenerateMassal_<%=rnd%>">
        <div class="card card-body bg-white border-0 custom-shadow-<%=rnd%> rounded-4">
            <h6 class="fw-bold text-info border-bottom pb-2 mb-3"><i class="fas fa-cogs me-2"></i><%=Common.getBahasaConfig("Pembuatan Jadwal Pertemuan Secara Otomatis")%></h6>
            <form id="formPenjadwalan_<%=rnd%>" onsubmit="submitGenerateMassal<%=rnd%>(event)">
                <input type="hidden" name="action" value="generate_massal">
                <input type="hidden" name="idPerkuliahan" value="<%=perkuliahan.getId()%>">
                
                <div class="row g-3 align-items-end">
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Mulai")%> <span class="text-danger">*</span></label>
                        <input type="date" class="form-control form-control-sm border-info" name="tanggalMulai" value="<%=tglMulai%>" required>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Rentang Waktu")%></label>
                        <div class="input-group input-group-sm">
                            <input type="time" class="form-control border-info" name="waktuMulai" value="<%=wMulaiPerkuliahan%>">
                            <span class="input-group-text bg-light text-muted">-</span>
                            <input type="time" class="form-control border-info" name="waktuSelesai" value="<%=wSelesaiPerkuliahan%>">
                        </div>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Siklus Pertemuan")%></label>
                        <select class="form-select form-select-sm border-info" name="jenisPertemuan">
                            <% 
                                String[] jenisArr = {"Harian", "Mingguan", "Bulanan"}; 
                                String curJenis = perkuliahan.getJenis() != null ? perkuliahan.getJenis() : "Mingguan"; 
                                for(String jns : jenisArr) { 
                            %>
                                <option value="<%=jns%>" <%=curJenis.equalsIgnoreCase(jns) ? "selected" : ""%>><%=Common.getBahasaConfig(jns)%></option>
                            <% } %>
                        </select>
                    </div>
                    <div class="col-md-3 d-flex flex-column justify-content-end gap-2">
                        <div class="form-check form-switch ps-4">
                            <input class="form-check-input border-danger" style="cursor: pointer;" type="checkbox" name="isHapusLama" id="chkHapusLama_<%=rnd%>" checked>
                            <label class="form-check-label small fw-bold text-danger" style="cursor: pointer;" for="chkHapusLama_<%=rnd%>"><%=Common.getBahasaConfig("Hapus jadwal sebelumnya")%></label>
                        </div>
                        <button type="submit" class="btn btn-sm btn-info text-white rounded-pill px-4 fw-bold shadow-sm w-100" id="btnProsesGen_<%=rnd%>">
                            <i class="fas fa-magic me-1"></i> <%=Common.getBahasaConfig("Mulai Proses Pembuatan")%>
                        </button>
                    </div>
                </div>
            </form>
        </div>
    </div>

    <div class="table-responsive custom-shadow-<%=rnd%> border-0 rounded-4 bg-white" style="max-height: 65vh; overflow-y: auto;">
        <table class="table table-bordered table-hover grid-table-<%=rnd%> mb-0">
            <thead class="sticky-top shadow-sm text-secondary" style="z-index: 5;">
                <tr>
                    <th class="text-center align-middle" width="50"><%=Common.getBahasaConfig("No.")%></th>
                    <th class="text-center align-middle"><%=Common.getBahasaConfig("Aksi")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Kemampuan Akhir Pembelajaran")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Kriteria, Indikator & Bobot Penilaian")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Pengalaman Belajar")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Tugas dan Penilaian")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Bahan Kajian")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Referensi")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Metode Pembelajaran")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Jenis Pertemuan")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Tanggal Pelaksanaan")%></th>
                    <th class="align-middle"><%=Common.getBahasaConfig("Waktu")%></th>
                    <th class="text-center align-middle"><%=Common.getBahasaConfig("Aktif")%></th>
                </tr>
            </thead>
            <tbody>
                <% if (listPertemuan != null && !listPertemuan.isEmpty()) { 
                    int no = 1;
                    for (Pertemuan p : listPertemuan) { 
                        // Format tanggal untuk HTML5
                        String tgl = p.getTanggal() != null ? html5DateFormat.format(p.getTanggal()) : "";
                        Long idStatus = p.getStatusPertemuan() != null ? p.getStatusPertemuan().getId() : null;
                        
                        // PENYESUAIAN WAKTU: Mengubah karakter titik (.) yang disimpan database Java menjadi titik dua (:) untuk HTML5
                        String wMulai = p.getWaktuMulai() != null ? p.getWaktuMulai().replace(".", ":").trim() : "";
                        if (wMulai.length() >= 5) wMulai = wMulai.substring(0, 5);
                        
                        String wSelesai = p.getWaktuSelesai() != null ? p.getWaktuSelesai().replace(".", ":").trim() : "";
                        if (wSelesai.length() >= 5) wSelesai = wSelesai.substring(0, 5);
                %>
                <tr id="row_pert_<%=p.getId()%>">
                    <td class="text-center align-middle fw-bold text-secondary"><%=no++%></td>
                    <td class="text-center align-middle">
                        <button type="button" class="btn btn-outline-danger btn-sm rounded-circle px-2 py-1 shadow-sm" onclick="hapusSatuPertemuan<%=rnd%>('<%=p.getId()%>')" title="<%=Common.getBahasaConfig("Hapus Jadwal Pertemuan Ini")%>">
                            <i class="fas fa-trash-alt"></i>
                        </button>
                    </td>
                    <td><textarea class="form-control form-control-sm w-min-250 shadow-sm border-0 bg-light" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'topik', this.value)"><%=p.getTopik()!=null?p.getTopik():""%></textarea></td>
                    <td><textarea class="form-control form-control-sm w-min-250 shadow-sm border-0 bg-light" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'indikator', this.value)"><%=p.getIndikator()!=null?p.getIndikator():""%></textarea></td>
                    <td><textarea class="form-control form-control-sm w-min-250 shadow-sm border-0 bg-light" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'pengalamanBelajar', this.value)"><%=p.getPengalamanBelajar()!=null?p.getPengalamanBelajar():""%></textarea></td>
                    <td><textarea class="form-control form-control-sm w-min-200 shadow-sm border-0 bg-light" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'tugasDanPenilaian', this.value)"><%=p.getTugasDanPenilaian()!=null?p.getTugasDanPenilaian():""%></textarea></td>
                    <td><textarea class="form-control form-control-sm w-min-200 shadow-sm border-0 bg-light" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'bukuRujukan1', this.value)"><%=p.getBukuRujukan1()!=null?p.getBukuRujukan1():""%></textarea></td>
                    <td><textarea class="form-control form-control-sm w-min-200 shadow-sm border-0 bg-light" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'bukuRujukan2', this.value)"><%=p.getBukuRujukan2()!=null?p.getBukuRujukan2():""%></textarea></td>
                    <td><textarea class="form-control form-control-sm w-min-200 shadow-sm border-0 bg-light" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'metodePembelajaran', this.value)"><%=p.getMetodePembelajaran()!=null?p.getMetodePembelajaran():""%></textarea></td>
                    <td>
                        <select class="form-select form-select-sm w-min-150 shadow-sm border-0 bg-light" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'statusPertemuan', this.value)">
                            <option value="">-</option>
                            <% for(StatusPertemuan sp : listStatus) { %>
                                <option value="<%=sp.getId()%>" <%=(idStatus != null && idStatus.equals(sp.getId())) ? "selected" : ""%>><%=sp.getNama()%></option>
                            <% } %>
                        </select>
                    </td>
                    <td><input type="date" class="form-control form-control-sm w-min-150 shadow-sm border-0 bg-light" value="<%=tgl%>" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'tanggal', this.value)"></td>
                    <td>
                        <div class="d-flex gap-1">
                            <input type="time" class="form-control form-control-sm w-min-100 px-1 text-center shadow-sm border-0 bg-light" value="<%=wMulai%>" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'waktuMulai', this.value)">
                            <input type="time" class="form-control form-control-sm w-min-100 px-1 text-center shadow-sm border-0 bg-light" value="<%=wSelesai%>" onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'waktuSelesai', this.value)">
                        </div>
                    </td>
                    <td class="text-center align-middle">
                        <div class="form-check form-switch d-flex justify-content-center">
                            <input class="form-check-input" style="cursor:pointer;" type="checkbox" <%=(p.getAktif()!=null && p.getAktif())?"checked":""%> onchange="autoSave<%=rnd%>('<%=p.getId()%>', 'aktif', this.checked)">
                        </div>
                    </td>
                </tr>
                <%  } 
                   } else { %>
                <tr>
                    <td colspan="14" class="text-center py-5 text-muted bg-light">
                        <i class="fas fa-folder-open fa-3x mb-3 text-secondary opacity-50"></i><br>
                        <span class="fw-semibold"><%=Common.getBahasaConfig("Belum ada jadwal pertemuan yang dikonfigurasi.")%></span><br>
                        <small><%=Common.getBahasaConfig("Silakan gunakan fitur 'Hasilkan Massal' atau 'Tambah Pertemuan' untuk mulai menyusun jadwal.")%></small>
                    </td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </div>
</div>

<div class="modal fade" id="modalTambahPert_<%=rnd%>" tabindex="-1" aria-hidden="true" style="z-index: 1065;">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-4">
            <div class="modal-header bg-primary text-white py-3 border-0">
                <h6 class="modal-title fw-bold"><i class="fas fa-plus-circle me-2"></i><%=Common.getBahasaConfig("Penambahan Jadwal Pertemuan")%></h6>
                <button type="button" class="btn-close btn-close-white" onclick="tutupModalTambah<%=rnd%>(event)" aria-label="<%=Common.getBahasaConfig("Tutup")%>"></button>
            </div>
            <div class="modal-body bg-light p-4 rounded-bottom-4">
                <form id="formAddSatu_<%=rnd%>" onsubmit="submitTambahSatu<%=rnd%>(event)">
                    <input type="hidden" name="action" value="add_one">
                    <input type="hidden" name="idPerkuliahan" value="<%=perkuliahan.getId()%>">
                    
                    <div class="row g-3 bg-white p-3 rounded-3 custom-shadow-<%=rnd%> mb-3 border">
                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Tanggal Pelaksanaan")%> <span class="text-danger">*</span></label>
                            <input type="date" class="form-control border-primary" name="add_tanggal" required value="<%=html5DateFormat.format(new java.util.Date())%>">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Waktu Mulai")%></label>
                            <input type="time" class="form-control border-primary" name="add_waktuMulai" value="<%=wMulaiPerkuliahan%>">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Waktu Selesai")%></label>
                            <input type="time" class="form-control border-primary" name="add_waktuSelesai" value="<%=wSelesaiPerkuliahan%>">
                        </div>
                        <div class="col-md-12">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kategori / Jenis Pertemuan")%></label>
                            <select class="form-select border-primary" name="add_statusPertemuan">
                                <% for(StatusPertemuan sp : listStatus) { %>
                                    <option value="<%=sp.getId()%>" <%="Tatap Muka".equalsIgnoreCase(sp.getNama()) ? "selected" : ""%>><%=sp.getNama()%></option>
                                <% } %>
                            </select>
                        </div>
                        <div class="col-md-12">
                            <label class="form-label small fw-bold text-secondary"><%=Common.getBahasaConfig("Kemampuan Akhir Pembelajaran / Topik Utama")%></label>
                            <textarea class="form-control bg-light border-primary" name="add_topik" rows="3" placeholder="<%=Common.getBahasaConfig("Tuliskan deskripsi topik utama pertemuan...")%>"></textarea>
                        </div>
                    </div>
                    
                    <div class="d-flex justify-content-end gap-2 mt-4 pt-2 border-top">
                        <button type="button" class="btn btn-light border px-4 rounded-pill fw-bold text-secondary shadow-sm" onclick="tutupModalTambah<%=rnd%>(event)"><%=Common.getBahasaConfig("Batalkan")%></button>
                        <button type="submit" class="btn btn-success px-5 rounded-pill fw-bold shadow-sm" id="btnAddSatu_<%=rnd%>"><i class="fas fa-save me-2"></i> <%=Common.getBahasaConfig("Simpan Pertemuan")%></button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
    // ==============================================================================
    // FUNGSI ISOLASI MODAL (MENCEGAH MODAL INDUK IKUT TERTUTUP)
    // ==============================================================================
    var modalTambahEl_<%=rnd%> = document.getElementById('modalTambahPert_<%=rnd%>');
    if (modalTambahEl_<%=rnd%>) {
        modalTambahEl_<%=rnd%>.addEventListener('hide.bs.modal', function(event) {
            event.stopPropagation();
        });
    }

    if (typeof $ !== 'undefined') {
        $('#modalTambahPert_<%=rnd%>').on('hide.bs.modal', function(event) {
            event.stopPropagation();
        });
    }

    function tutupModalTambah<%=rnd%>(e) {
        if(e) { e.preventDefault(); e.stopPropagation(); }
        var modalEl = document.getElementById('modalTambahPert_<%=rnd%>');
        if (modalEl) {
            try {
                if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
                    var modalInstance = bootstrap.Modal.getInstance(modalEl);
                    if (modalInstance) modalInstance.hide();
                    return;
                }
            } catch(ex) {}
            
            if (typeof $ !== 'undefined') {
                $('#modalTambahPert_<%=rnd%>').modal('hide');
            } else {
                modalEl.classList.remove('show');
                modalEl.style.display = 'none';
            }
        }
    }

    // ==============================================================================
    // FUNGSI JEMBATAN SINKRONISASI CACHE JAVA SETELAH PENGHAPUSAN ATAU PERUBAHAN TANGGAL/AKTIF
    // ==============================================================================
    function syncStatePerkuliahan<%=rnd%>() {
        var isAktif = document.getElementById('chkHanyaAktif_<%=rnd%>').checked;
        var params = new URLSearchParams();
        params.append("action", "sync_state");
        params.append("idPerkuliahan", "<%=perkuliahan.getId()%>");
        params.append("hanyaYangAktif", isAktif ? "true" : "false");

        return fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_buat_pertemuan_services', {
            method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params.toString()
        });
    }

    // ==============================================================================
    // FUNGSI MUAT ULANG TAMPILAN ANTARMUKA
    // ==============================================================================
    function reloadDataPertemuan<%=rnd%>() {
        var isAktif = document.getElementById('chkHanyaAktif_<%=rnd%>').checked;
        
        if (typeof window['onSearchDefault'] === 'function') {
            window['onSearchDefault'](null);
        } else if (typeof resetAndLoadData<%=rnd%> === 'function') {
            resetAndLoadData<%=rnd%>();
        } else {
            var cm = '<%=contentModal%>';
            var urlApi = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=buat_pertemuan&perkuliahan=<%=perkuliahan.getId()%>&contentModal=' + cm + '&hanyaYangAktif=' + isAktif;
            loadContentIntoContainer(urlApi, 'form-container-<%=rnd%>');
        }
    }

    // ==============================================================================
    // FUNGSI PENYIMPANAN OTOMATIS SAAT PENGEDITAN LANGSUNG (INLINE AUTO-SAVE)
    // ==============================================================================
    function autoSave<%=rnd%>(idPertemuan, fieldName, fieldValue) {
        var dataObj = {};
        
        if (fieldName === 'statusPertemuan') {
            dataObj[fieldName] = fieldValue;
        } else if (fieldName === 'aktif') {
            dataObj[fieldName] = (fieldValue === true || fieldValue === 'true');
        } else {
            dataObj[fieldName] = fieldValue;
        }

        const payload = { 
            action: "simpanDataRinci", 
            class: "ais.database.model.Pertemuan",
            id: idPertemuan,
            data: dataObj
        };

        fetch('<%=Common.ROOT%>/Data', {
            method: 'POST', 
            headers: { 'Content-Type': 'application/json' }, 
            body: JSON.stringify(payload)
        })
        .then(function(r) {
            if(!r.ok) throw new Error("HTTP " + r.status);
            return r.json();
        })
        .then(function(res) {
            if(res.status === '00' || res.status === 'success' || res.status === 'ok' || res.id) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Pembaruan data berhasil disimpan.")%>', 'bg-success text-white');
                
                // Singkronisasi cache server agar urutan tabel tetap rapi saat tanggal diubah
                if (fieldName === 'aktif' || fieldName === 'tanggal') {
                    syncStatePerkuliahan<%=rnd%>();
                }
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast(res.description || res.msg || '<%=Common.getBahasaConfigJS("Gagal menyimpan pembaruan.")%>', 'bg-danger text-white');
            }
        })
        .catch(function(err) {
            console.error(err);
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan pada server.")%>', 'bg-danger text-white');
        });
    }

    // ==============================================================================
    // FUNGSI PENGHASILAN JADWAL SECARA MASSAL (GENERATE MASSAL)
    // ==============================================================================
    function submitGenerateMassal<%=rnd%>(e) {
        e.preventDefault();
        var form = document.getElementById("formPenjadwalan_<%=rnd%>");
        var btn = document.getElementById("btnProsesGen_<%=rnd%>");
        var orHTML = btn.innerHTML;
        
        showConfirmModal('<%=Common.getBahasaConfigJS("Tindakan ini akan memodifikasi data jadwal dan menghasilkan rentetan pertemuan secara massal. Apakah Anda ingin melanjutkan?")%>', function() {
            btn.innerHTML = '<i class="fas fa-circle-notch fa-spin me-2"></i><%=Common.getBahasaConfig("Sedang Memproses...")%>';
            btn.disabled = true;

            var params = new URLSearchParams(new FormData(form));
            fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_buat_pertemuan_services', {
                method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params.toString()
            })
            .then(function(r) {
                if(!r.ok) throw new Error("HTTP " + r.status);
                return r.json();
            })
            .then(function(res) {
                if (res.status === 'ok') {
                    syncStatePerkuliahan<%=rnd%>().then(function(){
                        if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Sistem berhasil membuat jadwal massal secara otomatis.")%>', 'bg-success text-white');
                        reloadDataPertemuan<%=rnd%>();
                    });
                } else {
                    if (typeof tampilkanToast === 'function') tampilkanToast(res.msg, 'bg-danger text-white');
                }
            })
            .catch(function(err) {
                if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal memproses permintaan pada server.")%>', 'bg-danger text-white');
            })
            .finally(function() { 
                btn.innerHTML = orHTML; 
                btn.disabled = false; 
            });
        });
    }

    // ==============================================================================
    // FUNGSI MEMBUKA JENDELA DIALOG PENAMBAHAN 1 PERTEMUAN
    // ==============================================================================
    function bukaModalTambah<%=rnd%>() {
        var modalEl = document.getElementById('modalTambahPert_<%=rnd%>');
        if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
            var mdl = bootstrap.Modal.getInstance(modalEl);
            if (!mdl) mdl = new bootstrap.Modal(modalEl);
            mdl.show();
        } else if (typeof $ !== 'undefined') {
            $('#modalTambahPert_<%=rnd%>').modal('show');
        }
    }

    // ==============================================================================
    // FUNGSI PENYIMPANAN SATU PERTEMUAN BARU
    // ==============================================================================
    function submitTambahSatu<%=rnd%>(e) {
        e.preventDefault();
        var form = document.getElementById("formAddSatu_<%=rnd%>");
        var btn = document.getElementById("btnAddSatu_<%=rnd%>");
        var orHTML = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i><%=Common.getBahasaConfig("Menyimpan Data...")%>';
        btn.disabled = true;

        var params = new URLSearchParams(new FormData(form));
        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fservices&s=_buat_pertemuan_services', {
            method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params.toString()
        })
        .then(function(r) {
            if(!r.ok) throw new Error("HTTP " + r.status);
            return r.json();
        })
        .then(function(res) {
            if (res.status === 'ok') {
                tutupModalTambah<%=rnd%>(null); 
                syncStatePerkuliahan<%=rnd%>().then(function(){
                    if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Jadwal pertemuan baru telah berhasil ditambahkan ke dalam sistem.")%>', 'bg-success text-white');
                    form.reset();
                    reloadDataPertemuan<%=rnd%>();
                });
            } else {
                if (typeof tampilkanToast === 'function') tampilkanToast(res.msg, 'bg-danger text-white');
            }
        })
        .catch(function(err) {
            if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi jaringan.")%>', 'bg-danger text-white');
        })
        .finally(function() { 
            btn.innerHTML = orHTML; 
            btn.disabled = false; 
        });
    }

    // ==============================================================================
    // FUNGSI PENGHAPUSAN SATU BARIS PERTEMUAN
    // ==============================================================================
    function hapusSatuPertemuan<%=rnd%>(id) {
        showConfirmModal('<%=Common.getBahasaConfigJS("PERINGATAN: Apakah Anda yakin ingin menghapus baris jadwal pertemuan ini secara permanen?")%>', function() {
            var sqlQuery = "DELETE FROM pertemuan WHERE id = " + id;
            const payload = { action: "update_data", sql: sqlQuery };

            fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            })
            .then(function(r) {
                if(!r.ok) throw new Error("HTTP " + r.status);
                return r.json();
            })
            .then(function(res) {
                if (res.status === '00' || res.status === 'success' || res.status === 'ok') {
                    syncStatePerkuliahan<%=rnd%>().then(function() {
                        if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Pertemuan berhasil dihapus dari sistem.")%>', 'bg-success text-white');
                        var tr = document.getElementById('row_pert_' + id);
                        if (tr) tr.remove();
                        // Memuat ulang jika tabel menjadi kosong
                        if(document.querySelectorAll('.grid-table-<%=rnd%> tbody tr').length === 0) reloadDataPertemuan<%=rnd%>(); 
                    });
                } else {
                    if (typeof tampilkanToast === 'function') tampilkanToast(res.description || res.msg || '<%=Common.getBahasaConfigJS("Gagal menghapus. Data kemungkinan sudah terkunci oleh relasi sistem lainnya.")%>', 'bg-danger text-white');
                }
            })
            .catch(function(err) {
                if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan saat menghapus data.")%>', 'bg-danger text-white');
            });
        });
    }

    // ==============================================================================
    // FUNGSI PENGHAPUSAN KESELURUHAN PERTEMUAN
    // ==============================================================================
    function hapusSemuaPertemuan<%=rnd%>() {
        showConfirmModal('<%=Common.getBahasaConfigJS("TINDAKAN BERBAHAYA: Apakah Anda benar-benar yakin ingin menghapus SELURUH jadwal pertemuan pada mata kuliah ini? Proses ini bersifat permanen dan tidak dapat dibatalkan.")%>', function() {
            var btn = document.getElementById("btnHapusSemua_<%=rnd%>");
            var orHTML = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-circle-notch fa-spin me-2"></i><%=Common.getBahasaConfig("Menghapus...")%>';
            btn.disabled = true;

            var sqlQuery = "DELETE FROM pertemuan WHERE perkuliahan = <%=perkuliahan.getId()%>";
            const payload = { action: "update_data", sql: sqlQuery };

            fetch('<%=Common.ROOT%>/Data', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            })
            .then(function(r) {
                if(!r.ok) throw new Error("HTTP " + r.status);
                return r.json();
            })
            .then(function(res) {
                if (res.status === '00' || res.status === 'success' || res.status === 'ok') {
                    syncStatePerkuliahan<%=rnd%>().then(function() {
                        if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Pembersihan sistem berhasil. Seluruh jadwal pertemuan telah dihapus.")%>', 'bg-success text-white');
                        reloadDataPertemuan<%=rnd%>();
                    });
                } else {
                    if (typeof tampilkanToast === 'function') tampilkanToast(res.description || res.msg || '<%=Common.getBahasaConfigJS("Gagal menghapus data secara massal.")%>', 'bg-danger text-white');
                }
            })
            .catch(function(err) {
                if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan koneksi.")%>', 'bg-danger text-white');
            })
            .finally(function() { 
                btn.innerHTML = orHTML; 
                btn.disabled = false; 
            });
        });
    }
</script>
<%
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/buat_pertemuan.jsp:638");
        out.print("<div class='alert alert-danger shadow-sm border-0 rounded-3 m-3'><i class='fas fa-times-circle me-2'></i>" + Common.getBahasaConfig("Terjadi kesalahan internal pada sistem peladen: ") + e.getMessage() + "</div>");
    } finally {
        ais.common.ElearningSessionUtil.closeQuietly(sess);
        HibernateUtil.closeSessionQuietly(sess);
    }
%>