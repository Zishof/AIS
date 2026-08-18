<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataMahasiswa"%>
<%@page import="ais.database.model.KelompokParameterTambahanAlumni"%>
<%@page import="ais.database.model.ParameterTambahanAlumni"%>
<%@page import="ais.database.model.ParameterTambahan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.StatusMahasiswa"%>
<%@page import="ais.database.model.JadwalChecklistPenilaianUmum"%>
<%@page import="ais.database.model.GrupChecklistPenilaianUmum"%>
<%@page import="ais.database.model.ChecklistPenilaianUmum"%>
<%@page import="ais.database.model.SubGrupChecklistPenilaianUmum"%>
<%@page import="ais.database.model.ChecklistHasilPenilaianUmum"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.ParameterTambahanHtmlHelper"%>
<%@page import="ais.common.ChecklistPenilaianHelper"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.criterion.Criterion"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Collections"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String rnd = Common.getGeneratedBarCode(7);
    Mahasiswa mahasiswa = (Mahasiswa) request.getSession().getAttribute("alumni_logged_in");

    if (mahasiswa == null) {
        out.print("<div class='p-5 text-center text-danger fw-bold'><i class='fas fa-exclamation-triangle fa-3x mb-3 d-block'></i>" + Common.getBahasaConfig("Sesi Anda telah berakhir, silakan masuk kembali ke sistem.") + "</div>");
        return;
    }

    Session sess = null;
    
    // =========================================================================
    // MASTER TRY-BLOCK
    // =========================================================================
    try {
        sess = HibernateUtil.openSession();
        
        List<KelompokParameterTambahanAlumni> listKelompok = null;
        Map<String, String> mapJawabanLama = new HashMap<String, String>();
        
        Integer tahunAngkatan = mahasiswa.getTahunangkatan();
        Integer gel = tahunAngkatan;
        
        Boolean digunakanUntukPenggunaAlumni = request.getParameter("digunakanUntukPenggunaAlumni") == null 
                ? false 
                : Boolean.valueOf(request.getParameter("digunakanUntukPenggunaAlumni"));
        
        listKelompok = ConstantValues.simpleList(
            sess.createCriteria(ParameterTambahanAlumni.class)
                .add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", Boolean.TRUE),
                        gel == null ? Restrictions.sqlRestriction("false")
                                : Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
                .createAlias("parameterTambahan", "parameterTambahan")
                .createAlias("kelompokParameterTambahanAlumni", "kelompokAlias")
                .add(digunakanUntukPenggunaAlumni
                        ? Restrictions.eq("kelompokAlias.digunakanUntukPenggunaAlumni", Boolean.TRUE)
                        : Restrictions.or(
                                Restrictions.eq("kelompokAlias.digunakanUntukPenggunaAlumni", Boolean.FALSE),
                                Restrictions.isNull("kelompokAlias.digunakanUntukPenggunaAlumni")))
                .add(Restrictions.eq("parameterTambahan.aktif", Boolean.TRUE))
                .add(Restrictions.eq("kelompokAlias.aktif", Boolean.TRUE))
                .setProjection(Projections.groupProperty("kelompokAlias.id")),
            KelompokParameterTambahanAlumni.class, false
        );
        
        if (listKelompok != null) {
            Collections.sort(listKelompok);
        }

        BiodataMahasiswa biodata = mahasiswa.ambilBiodata();
        if (biodata != null && biodata.getParameterTambahanIndsAlumni() != null) {
            String rawParam = biodata.getParameterTambahanIndsAlumni();
            String[] spl = rawParam.split("\n");
            for (int i = 0; i < spl.length; i++) {
                String d = spl[i];
                if (!d.trim().isEmpty()) {
                    String[] val = d.split("<=>");
                    if (val.length > 1) {
                        mapJawabanLama.put(val[0].trim().toLowerCase(), val[1].trim());
                    }
                }
            }
        }
%>

<style>
    .form-label-kuesioner-<%=rnd%> { font-weight: 600; color: #34495e; font-size: 0.95rem; }
    .group-title-<%=rnd%> { background: linear-gradient(90deg, #f8f9fa 0%, #ffffff 100%); padding: 12px 18px; border-left: 5px solid #0d6efd; margin-bottom: 20px; font-weight: 800; color: #2c3e50; text-transform: uppercase; letter-spacing: 0.5px; border-radius: 0 8px 8px 0; }
    .group-title-angket-<%=rnd%> { background: linear-gradient(90deg, #fff3cd 0%, #ffffff 100%); padding: 12px 18px; border-left: 5px solid #ffc107; margin-top: 30px; margin-bottom: 20px; font-weight: 800; color: #664d03; text-transform: uppercase; letter-spacing: 0.5px; border-radius: 0 8px 8px 0; }
    .required-mark-<%=rnd%> { color: #dc3545; margin-left: 4px; font-weight: bold; }
    .param-dinamis:focus { border-color: #0d6efd; box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.15); }
    .angket-radio-box { border: 1px solid #dee2e6; border-radius: 8px; padding: 10px 15px; cursor: pointer; transition: 0.2s; background: #fff; text-align: center; }
    .angket-radio-box:hover { border-color: #0d6efd; background: #f8f9fa; }
    .angket-radio-box input:checked ~ label { color: #0d6efd; font-weight: bold; }
</style>

<div class="modal-header bg-primary text-white border-0 py-3 px-4 shadow-sm">
    <h5 class="modal-title fw-bold" style="letter-spacing: 0.5px;">
        <i class="fas fa-clipboard-list me-2"></i><%= Common.getBahasaConfig("Instrumen Kuesioner Tracer Study") %>
    </h5>
    <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal" aria-label="Close"></button>
</div>

<div class="modal-body p-4 p-md-5 bg-light">
    <div class="alert alert-info border-0 shadow-sm rounded-4 mb-4 d-flex align-items-center">
        <i class="fas fa-info-circle fa-2x me-3 text-info"></i>
        <div>
            <strong><%= Common.getBahasaConfig("Perhatian:") %></strong> <%= Common.getBahasaConfig("Mohon isi instrumen ini dengan data yang sebenarnya. Pertanyaan dengan tanda bintang merah") %> (<span class="required-mark-<%=rnd%>">*</span>) <%= Common.getBahasaConfig("bersifat wajib diisi.") %>
        </div>
    </div>

    <form id="formKuesioner<%=rnd%>">
        
        <% 
        if (listKelompok != null && !listKelompok.isEmpty()) {
            boolean adaData = false;

            for (int i = 0; i < listKelompok.size(); i++) {
                KelompokParameterTambahanAlumni kelompok = listKelompok.get(i);
                List<ParameterTambahanAlumni> listParams = ConstantValues.simpleList(
                    sess.createCriteria(ParameterTambahanAlumni.class)
                        .add(Restrictions.eq("kelompokParameterTambahanAlumni", kelompok))
                        .add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", Boolean.TRUE),
                                gel == null ? Restrictions.sqlRestriction("false") : Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
                        .createAlias("parameterTambahan", "parameterTambahan")
                        .createAlias("kelompokParameterTambahanAlumni", "kelompokAlias")
                        .add(digunakanUntukPenggunaAlumni ? Restrictions.eq("kelompokAlias.digunakanUntukPenggunaAlumni", Boolean.TRUE) : Restrictions.or(Restrictions.eq("kelompokAlias.digunakanUntukPenggunaAlumni", Boolean.FALSE), Restrictions.isNull("kelompokAlias.digunakanUntukPenggunaAlumni")))
                        .add(Restrictions.eq("parameterTambahan.aktif", Boolean.TRUE))
                        .add(Restrictions.eq("kelompokAlias.aktif", Boolean.TRUE)),
                    ParameterTambahanAlumni.class
                );
                
                if (listParams != null) {
                    Collections.sort(listParams);
                }

                if (listParams != null && !listParams.isEmpty()) {
                    adaData = true;
        %>
                    <div class="group-title-<%=rnd%> shadow-sm">
                        <i class="fas fa-folder-open me-2 text-primary"></i><%= kelompok.getNama() %>
                    </div>
                    <div class="row g-4 mb-5 px-1 px-md-3">
                    <% 
                        for (int k = 0; k < listParams.size(); k++) { 
                            ParameterTambahanAlumni pta = listParams.get(k);
                            ParameterTambahan param = pta.getParameterTambahan();
                            boolean isRequired = pta.getWajibDiisi() != null && pta.getWajibDiisi();

                            // Bila parameter TIDAK memakai tipe inputan ("Tidak ada data yang diinput"),
                            // tampilkan HANYA label/teksnya — tanpa kotak input dan tanpa validasi wajib
                            // (agar tidak muncul "Please fill out this field" untuk sesuatu yang memang
                            // tak perlu diisi).
                            boolean tanpaInput = param.getTipeDataInputan() != null
                                    && ParameterTambahan.TIDAK_ADA.equals(param.getTipeDataInputan().trim());
                            if (tanpaInput) { isRequired = false; }

                            String paramKey = (kelompok.getId() + "->" + param.getId()).toLowerCase();
                            String formInputId = "param_" + kelompok.getId() + "_" + param.getId();
                            String jawabanLama = mapJawabanLama.containsKey(paramKey) ? mapJawabanLama.get(paramKey) : "";
                            
                            String reqStr = isRequired ? " required=\"required\"" : "";
                            String extraAttributes = "name=\"" + formInputId + "\"" + reqStr;
                    %>
                        <div class="col-12 bg-white p-4 rounded-4 shadow-sm border border-light param-skip-wrapper"<%= ParameterTambahanHtmlHelper.syaratTampilAttr(param) %>>
                            <label class="form-label form-label-kuesioner-<%=rnd%> mb-3">
                                <%= param.getLabelInputan() %>
                                <% if(isRequired) { %><span class="required-mark-<%=rnd%>">*</span><% } %>
                            </label>
                            <% if (param.getKeterangan() != null && !param.getKeterangan().trim().isEmpty()) { %>
                                <p class="text-muted small mb-3 fst-italic"><i class="fas fa-quote-left me-1 text-black-50"></i> <%= param.getKeterangan() %></p>
                            <% } %>
                            <% if (!tanpaInput) { %>
                            <div class="mt-2">
                                <%= ParameterTambahanHtmlHelper.generateHtmlComponent(param, jawabanLama, false, formInputId, extraAttributes) %>
                            </div>
                            <% } %>
                        </div>
                    <% 
                        } 
                        listParams.clear(); 
                    %>
                    </div>
        <% 
                } 
            }
            if(!adaData) out.print("<div class='p-5 text-center text-muted'><h6>" + Common.getBahasaConfig("Belum ada instrumen kuesioner yang aktif untuk angkatan Anda.") + "</h6></div>");
            
            listKelompok.clear();
            listKelompok = null;
        } 
        %>

        <%
            Tbmuser tbmuserTemp = new Tbmuser();
            tbmuserTemp.setMahasiswa(mahasiswa);
            List<Object[]> datasChecklist = ChecklistPenilaianHelper.getJadwalChecklistUmum(tbmuserTemp);
            
            if (datasChecklist != null && !datasChecklist.isEmpty()) {
                
                StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
                Criterion criterionUmum = Restrictions.sqlRestriction("false");
                
                if (mahasiswa.getId() != null && statusMahasiswa != null && statusMahasiswa.getNama() != null && statusMahasiswa.getNama().toLowerCase().trim().contains("lulus")) {
                    criterionUmum = Restrictions.sqlRestriction(
                            " ( (diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_ALUMNI + "' and (status_mahasiswa="
                                    + statusMahasiswa.getId() + " or status_mahasiswa is null) and (mulai_angkatan<="
                                    + mahasiswa.getTahunangkatan() + " or mulai_angkatan is null)  and (sampai_angkatan>="
                                    + mahasiswa.getTahunangkatan() + " or sampai_angkatan is null) and (fakultas="
                                    + mahasiswa.getJurusan().getFakultas().getId() + " or fakultas is null) and (jurusan="
                                    + mahasiswa.getJurusan().getId() + " or jurusan is null) ))");
                } else if (mahasiswa.getId() != null) {
                    criterionUmum = Restrictions.sqlRestriction(" ( (diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_MAHASISWA
                            + "' and (status_mahasiswa=" + (statusMahasiswa == null ? "null" : statusMahasiswa.getId())
                            + " or status_mahasiswa is null) and (mulai_angkatan<=" + mahasiswa.getTahunangkatan()
                            + " or mulai_angkatan is null)  and (sampai_angkatan>=" + mahasiswa.getTahunangkatan()
                            + " or sampai_angkatan is null) and (fakultas=" + mahasiswa.getJurusan().getFakultas().getId()
                            + " or fakultas is null) and (jurusan=" + mahasiswa.getJurusan().getId()
                            + " or jurusan is null) )  or diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_UMUM + "') ");
                }

                @SuppressWarnings("unchecked")
                List<ChecklistHasilPenilaianUmum> existingHasil = sess.createCriteria(ChecklistHasilPenilaianUmum.class)
                    .add(Restrictions.isNull("tbmuserDinilai")).add(Restrictions.isNull("pertemuanId"))
                    .add(Restrictions.eq("mahasiswa", mahasiswa)).list();
                
                Map<String, ChecklistHasilPenilaianUmum> mapHasil = new HashMap<String, ChecklistHasilPenilaianUmum>();
                for (int i = 0; i < existingHasil.size(); i++) {
                    ChecklistHasilPenilaianUmum h = existingHasil.get(i);
                    mapHasil.put(h.getTahunAkademik() + "_" + h.getSemesterStr() + "_" + h.getChecklistPenilaianUmum().getId(), h);
                }
                
                existingHasil.clear(); 
                
                for (int i = 0; i < datasChecklist.size(); i++) {
                    Object[] obj = datasChecklist.get(i);
                    String tahunAkademik = (String) obj[0];
                    String semester = (String) obj[1];
                    
                    @SuppressWarnings("unchecked")
                    List<JadwalChecklistPenilaianUmum> jadwalList = sess.createCriteria(JadwalChecklistPenilaianUmum.class)
                            .add(Restrictions.eq("tahunAkademik", tahunAkademik))
                            .add(Restrictions.eq("semester", semester))
                            .createCriteria("grupChecklistPenilaianUmum")
                            .add(criterionUmum)
                            .add(Restrictions.or(Restrictions.eq("aktif", Boolean.TRUE), Restrictions.isNull("aktif"))).list();
                    
                    for (int j = 0; j < jadwalList.size(); j++) {
                        JadwalChecklistPenilaianUmum jadwal = jadwalList.get(j);
                        GrupChecklistPenilaianUmum g = jadwal.getGrupChecklistPenilaianUmum();
                        
                        List<ChecklistPenilaianUmum> checklistList = ConstantValues.simpleList(
                                sess.createCriteria(ChecklistPenilaianUmum.class)
                                    .createAlias("subGrupChecklistPenilaianUmum", "subGrupChecklistPenilaianUmum", Criteria.LEFT_JOIN)
                                    .addOrder(Order.asc("subGrupChecklistPenilaianUmum.nama"))
                                    .addOrder(Order.asc("isi"))
                                    .add(Restrictions.or(Restrictions.eq("aktif", Boolean.TRUE), Restrictions.isNull("aktif")))
                                    .add(Restrictions.eq("grupChecklistPenilaianUmum", g)),
                                ChecklistPenilaianUmum.class);
                                
                        if (!checklistList.isEmpty()) {
                            Integer jumlahPilihan = 5;
                            try { jumlahPilihan = g.getAngketPenilaianUmum().getJumlahPilihan(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_isi_kuesioner_alumni.jsp:274");}
        %>
                            <div class="group-title-angket-<%=rnd%> shadow-sm">
                                <i class="fas fa-tasks me-2 text-warning"></i><%= Common.getBahasaConfig("Angket / Evaluasi Khusus") %>: <%= g.getIsi() %> 
                                <span class="badge bg-secondary ms-2" style="font-size:10px;"><%= tahunAkademik %> - <%= semester %></span>
                            </div>
                            
                            <% if (g.getAngketPenilaianUmum() != null && g.getAngketPenilaianUmum().getPetunjuk() != null) { %>
                                <div class="alert alert-warning mb-4 shadow-sm" style="font-size: 0.9rem;">
                                    <%= g.getAngketPenilaianUmum().getPetunjuk().replaceAll("\n", "<br>") %>
                                </div>
                            <% } %>
                            
                            <div class="row g-4 mb-5 px-1 px-md-3">
                            <% 
                                String lastSubGroup = "";
                                for (int k = 0; k < checklistList.size(); k++) { 
                                    ChecklistPenilaianUmum c = checklistList.get(k);
                                    String curSubGroup = c.getSubGrupChecklistPenilaianUmum() != null ? c.getSubGrupChecklistPenilaianUmum().getNama() : "";
                                    if (!curSubGroup.equals(lastSubGroup) && !curSubGroup.isEmpty()) {
                                        lastSubGroup = curSubGroup;
                            %>
                                        <div class="col-12 mt-4 mb-1">
                                            <h6 class="fw-bold text-secondary text-uppercase border-bottom pb-2"><%= curSubGroup %></h6>
                                        </div>
                            <%      } 
                                    
                                    String idParam = c.getId() + "_" + tahunAkademik + "_" + semester;
                                    String inputNameRadio = "chk_val_" + idParam;
                                    String inputNameKet = "chk_ket_" + idParam;
                                    
                                    ChecklistHasilPenilaianUmum jawabanAda = mapHasil.get(tahunAkademik + "_" + semester + "_" + c.getId());
                                    Integer nilaiTerjawab = jawabanAda != null ? jawabanAda.getNilai() : null;
                                    String ketTerjawab = jawabanAda != null && jawabanAda.getKeterangan() != null ? jawabanAda.getKeterangan() : "";
                                    
                                    JSONObject pilihan = new JSONObject(c.getPilihan());
                            %>
                                <div class="col-12 bg-white p-4 rounded-4 shadow-sm border border-light">
                                    <p class="form-label-kuesioner-<%=rnd%> mb-3"><i class="fas fa-caret-right me-2 text-warning"></i><%= c.getIsi() %></p>
                                    
                                    <div class="d-flex flex-wrap gap-2 mt-3">
                                        <% for (Integer optIdx = 1; optIdx <= jumlahPilihan; optIdx++) { 
                                            String labelOpt = pilihan.isNull(optIdx + "") ? optIdx + "" : pilihan.getString(optIdx + "");
                                            boolean isChecked = (nilaiTerjawab != null && nilaiTerjawab.equals(optIdx));
                                            String radioId = inputNameRadio + "_" + optIdx;
                                        %>
                                            <div class="angket-radio-box flex-fill">
                                                <input type="radio" class="form-check-input mb-2" 
                                                       name="<%= inputNameRadio %>" id="<%= radioId %>" 
                                                       value="<%= optIdx %>" <%= isChecked ? "checked" : "" %> required>
                                                <label class="d-block w-100 text-secondary" style="font-size:0.85rem; cursor:pointer;" for="<%= radioId %>"><%= labelOpt %></label>
                                            </div>
                                        <% } %>
                                    </div>
                                    
                                    <% if (g.getAngketPenilaianUmum() != null && g.getAngketPenilaianUmum().getTampilKeterangan() != null && g.getAngketPenilaianUmum().getTampilKeterangan()) { %>
                                        <div class="mt-3">
                                            <label class="small text-muted mb-1"><%= Common.getBahasaConfig("Keterangan Tambahan / Alasan") %>:</label>
                                            <textarea name="<%= inputNameKet %>" class="form-control form-control-sm border-secondary-subtle bg-light shadow-none" rows="2" placeholder="<%= Common.getBahasaConfig("Tuliskan di sini...") %>"><%= ketTerjawab %></textarea>
                                        </div>
                                    <% } %>
                                </div>
                            <%  } 
                                checklistList.clear(); 
                            %>
                            </div>
        <%
                        }
                    }
                    jadwalList.clear();
                }
            }
        
        mapJawabanLama.clear();

%>
    <!-- ===================== SECTION: DAFTAR ATASAN / PENGGUNA LULUSAN ===================== -->
    <div class="group-title-<%=rnd%> shadow-sm mt-4">
        <i class="fas fa-user-tie me-2 text-primary"></i><%= Common.getBahasaConfig("Daftar Atasan / Pengguna Lulusan") %>
    </div>
    <div class="px-1 px-md-3 mb-4">
        <div class="table-responsive">
            <table class="table table-sm table-bordered align-middle bg-white mb-2" id="tblAtasan<%=rnd%>">
                <thead class="table-light"><tr>
                    <th><%= Common.getBahasaConfig("Nama") %></th>
                    <th><%= Common.getBahasaConfig("Peran") %></th>
                    <th><%= Common.getBahasaConfig("Email") %></th>
                    <th><%= Common.getBahasaConfig("Telp.") %></th>
                    <th><%= Common.getBahasaConfig("Aktif") %></th>
                    <th style="width:110px;"><%= Common.getBahasaConfig("Aksi") %></th>
                </tr></thead>
                <tbody></tbody>
            </table>
        </div>
        <button type="button" class="btn btn-outline-primary btn-sm rounded-pill fw-bold shadow-sm" onclick="bukaModalAtasan<%=rnd%>(-1)">
            <i class="fas fa-plus me-1"></i><%= Common.getBahasaConfig("Tambah Atasan") %>
        </button>
        <input type="hidden" name="atasans_json" id="atasans_json<%=rnd%>">
    </div>
    </form>

    <%-- Data awal atasan (JSON) di ATRIBUT div (bukan <script>, agar tak dieksekusi ulang oleh loader) --%>
    <div id="atasanInit<%=rnd%>" style="display:none" data-json="<%= (mahasiswa.getAtasans() == null || mahasiswa.getAtasans().trim().isEmpty()) ? "[]" : mahasiswa.getAtasans().replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;") %>"></div>

    <%-- Popup form ATASAN (overlay ringan, tanpa modal Bootstrap agar tak bentrok modal kuesioner) --%>
    <div id="ovAtasan<%=rnd%>" style="display:none; position:fixed; inset:0; background:rgba(0,0,0,.55); z-index:20000;">
        <div style="max-width:540px; margin:5vh auto; background:#fff; border-radius:14px; padding:22px; max-height:90vh; overflow:auto; box-shadow:0 10px 40px rgba(0,0,0,.35);">
            <h5 class="fw-bold mb-3" id="ovAtasanTitle<%=rnd%>"><%= Common.getBahasaConfig("Tambah Atasan") %></h5>
            <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%= Common.getBahasaConfig("Nama Atasan") %> <span class="text-danger">*</span></label><input type="text" class="form-control form-control-sm" id="atNama<%=rnd%>"></div>
            <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%= Common.getBahasaConfig("Email Atasan") %></label><input type="text" class="form-control form-control-sm" id="atEmail<%=rnd%>"></div>
            <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%= Common.getBahasaConfig("Telp. Atasan") %></label><input type="text" class="form-control form-control-sm" id="atTelp<%=rnd%>"></div>
            <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%= Common.getBahasaConfig("Alamat Atasan") %></label><textarea class="form-control form-control-sm" rows="2" id="atAlamat<%=rnd%>"></textarea></div>
            <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%= Common.getBahasaConfig("Peran Atasan Sebagai") %></label><input type="text" class="form-control form-control-sm" id="atPeran<%=rnd%>"></div>
            <div class="mb-2 form-check"><input type="checkbox" class="form-check-input" id="atAktif<%=rnd%>" checked><label class="form-check-label small fw-semibold" for="atAktif<%=rnd%>"><%= Common.getBahasaConfig("Saat ini masih aktif") %></label></div>
            <div class="mb-2"><label class="form-label small fw-semibold mb-1"><%= Common.getBahasaConfig("Tanggal Mulai Jadi Atasan") %></label><input type="date" class="form-control form-control-sm" id="atTgl<%=rnd%>"></div>
            <div class="mb-3"><label class="form-label small fw-semibold mb-1"><%= Common.getBahasaConfig("Keterangan Tambahan") %></label><textarea class="form-control form-control-sm" rows="3" id="atKet<%=rnd%>"></textarea></div>
            <div class="d-flex justify-content-end gap-2">
                <button type="button" class="btn btn-light btn-sm border rounded-pill px-3" onclick="tutupModalAtasan<%=rnd%>()"><%= Common.getBahasaConfig("Batal") %></button>
                <button type="button" class="btn btn-primary btn-sm fw-bold rounded-pill px-4" onclick="simpanModalAtasan<%=rnd%>()"><%= Common.getBahasaConfig("Simpan") %></button>
            </div>
        </div>
    </div>

    <script>
    (function(){
        var rnd = '<%=rnd%>';
        var initEl = document.getElementById('atasanInit'+rnd);
        var arr = [];
        try { arr = JSON.parse((initEl && initEl.getAttribute('data-json')) || '[]'); if(!Array.isArray(arr)) arr = []; } catch(e){ arr = []; }
        var editIdx = -1;
        function esc(s){ s=(s==null?'':''+s); return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
        function sync(){ var h=document.getElementById('atasans_json'+rnd); if(h) h.value = arr.length ? JSON.stringify(arr) : ''; }
        function render(){
            var tb = document.querySelector('#tblAtasan'+rnd+' tbody'); if(!tb) return;
            if(!arr.length){ tb.innerHTML = '<tr><td colspan="6" class="text-center text-muted small py-3"><%= Common.getBahasaConfig("Belum ada atasan. Klik tombol Tambah Atasan di bawah.") %></td></tr>'; sync(); return; }
            var h='';
            for(var i=0;i<arr.length;i++){ var a=arr[i]||{};
                h += '<tr>'
                    + '<td>'+esc(a.nama)+'</td>'
                    + '<td>'+esc(a.peran)+'</td>'
                    + '<td>'+esc(a.email)+'</td>'
                    + '<td>'+esc(a.telp)+'</td>'
                    + '<td>'+(a.masihAktif===false?'Tidak':'Ya')+'</td>'
                    + '<td><button type="button" class="btn btn-sm btn-outline-secondary me-1" onclick="bukaModalAtasan'+rnd+'('+i+')"><i class="fas fa-pen"></i></button>'
                    + '<button type="button" class="btn btn-sm btn-outline-danger" onclick="hapusAtasan'+rnd+'('+i+')"><i class="fas fa-trash"></i></button></td>'
                    + '</tr>';
            }
            tb.innerHTML = h; sync();
        }
        window['bukaModalAtasan'+rnd] = function(idx){
            editIdx = idx;
            var a = (idx>=0 && idx<arr.length) ? arr[idx] : {masihAktif:true};
            document.getElementById('ovAtasanTitle'+rnd).textContent = (idx<0 ? '<%= Common.getBahasaConfigJS("Tambah Atasan") %>' : '<%= Common.getBahasaConfigJS("Ubah Atasan") %>');
            document.getElementById('atNama'+rnd).value = a.nama||'';
            document.getElementById('atEmail'+rnd).value = a.email||'';
            document.getElementById('atTelp'+rnd).value = a.telp||'';
            document.getElementById('atAlamat'+rnd).value = a.alamat||'';
            document.getElementById('atPeran'+rnd).value = a.peran||'';
            document.getElementById('atAktif'+rnd).checked = (a.masihAktif!==false);
            document.getElementById('atTgl'+rnd).value = a.tanggalMulai||'';
            document.getElementById('atKet'+rnd).value = a.keterangan||'';
            document.getElementById('ovAtasan'+rnd).style.display='block';
        };
        window['tutupModalAtasan'+rnd] = function(){ document.getElementById('ovAtasan'+rnd).style.display='none'; };
        window['simpanModalAtasan'+rnd] = function(){
            var nama = document.getElementById('atNama'+rnd).value.trim();
            if(!nama){ alert('<%= Common.getBahasaConfigJS("Nama Atasan wajib diisi.") %>'); return; }
            var obj = {
                nama: nama,
                email: document.getElementById('atEmail'+rnd).value.trim(),
                telp: document.getElementById('atTelp'+rnd).value.trim(),
                alamat: document.getElementById('atAlamat'+rnd).value.trim(),
                peran: document.getElementById('atPeran'+rnd).value.trim(),
                masihAktif: document.getElementById('atAktif'+rnd).checked,
                tanggalMulai: document.getElementById('atTgl'+rnd).value,
                keterangan: document.getElementById('atKet'+rnd).value.trim()
            };
            if(editIdx>=0 && editIdx<arr.length) arr[editIdx]=obj; else arr.push(obj);
            document.getElementById('ovAtasan'+rnd).style.display='none';
            render();
        };
        window['hapusAtasan'+rnd] = function(idx){ if(idx>=0 && idx<arr.length){ arr.splice(idx,1); render(); } };
        render();
    })();
    </script>

    <%-- Mesin skip-logic LIVE: sembunyikan/tampilkan parameter bersyarat saat jawaban acuan berubah --%>
    <%= ParameterTambahanHtmlHelper.skipLogicScript() %>
</div>

<div class="modal-footer bg-white border-top py-3 px-4 shadow-sm d-flex justify-content-between">
    <button type="button" class="btn btn-light border-secondary-subtle rounded-pill px-4 fw-bold text-secondary shadow-sm" data-bs-dismiss="modal">
        <i class="fas fa-times me-2"></i><%= Common.getBahasaConfig("Batal Jawab") %>
    </button>
    <button type="button" class="btn btn-primary rounded-pill px-5 fw-bold shadow-sm" onclick="prosesSimpanKuesioner<%=rnd%>()" id="btnSimpanKuesioner<%=rnd%>">
        <i class="fas fa-save me-2"></i><%= Common.getBahasaConfig("Rekam Jawaban Kuesioner") %>
    </button>
</div>

<%
    } catch (Exception mainEx) {
        out.print("<div class='p-5 text-center text-danger'>Terjadi Kesalahan Internal: " + mainEx.getMessage() + "</div>");
        mainEx.printStackTrace(); ais.common.ErrorAuditUtil.record(mainEx, "auto-audit webapp/WEB-INF/baru/modul/alumni/_isi_kuesioner_alumni.jsp:476");
    } finally {
        if (sess != null) {
            try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_isi_kuesioner_alumni.jsp:479");}
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/alumni/_isi_kuesioner_alumni.jsp:480");}
        }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>

<script>
    // ==========================================================================================
    // FUNGSI GLOBAL SINKRONISASI CHECKBOX
    // ==========================================================================================
    if (typeof window.syncPilihanBanyakParameterTambahan === 'undefined') {
        window.syncPilihanBanyakParameterTambahan = function(paramId) {
            const checkboxes = document.querySelectorAll('.chk-param-' + paramId);
            let selectedVals = [];
            for (let i = 0; i < checkboxes.length; i++) {
                if (checkboxes[i].checked) {
                    selectedVals.push(checkboxes[i].value);
                }
            }
            if (checkboxes.length > 0) {
                const containerDiv = checkboxes[0].parentNode.parentNode;
                if (containerDiv && containerDiv.nextElementSibling) {
                    const hiddenInput = containerDiv.nextElementSibling;
                    if (hiddenInput && hiddenInput.tagName === 'INPUT' && hiddenInput.type === 'hidden') {
                        hiddenInput.value = selectedVals.join(';');
                    }
                }
            }
        };
    }

    // ==========================================================================================
    // FUNGSI UTAMA PENYIMPANAN DATA
    // ==========================================================================================
    window.prosesSimpanKuesioner<%=rnd%> = async function() {
        const form = document.getElementById('formKuesioner<%=rnd%>');
        const btn = document.getElementById('btnSimpanKuesioner<%=rnd%>');
        
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const oriText = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%= Common.getBahasaConfig("Menyimpan...") %>';
        btn.disabled = true;

        const formData = new FormData(form);
        const params = new URLSearchParams(formData);

        try {
            const res = await fetch('<%= Common.ROOT %>/alumni?hanya_tampil_jsp=true&p=alumni&s=_simpan_isi_kuesioner_alumni&baru=true', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: params.toString()
            });

            const textResp = await res.text();
            let result;
            try { result = JSON.parse(textResp); } catch(e) { throw new Error("JSON Parse Error"); }

            if (result && result.status === 'sukses') {
                if(typeof tampilkanToast === "function") tampilkanToast(result.pesan, 'bg-success text-white');
                
                const modalEl = document.querySelector('.modal.show');
                if(modalEl) {
                    const modalInstance = bootstrap.Modal.getInstance(modalEl);
                    modalInstance.hide();
                }
                
                // =======================================================================
                // PERUBAHAN: Memuat ulang (reload) halaman secara otomatis setelah sukses
                // Diberi jeda 1.2 detik agar notifikasi (toast) sukses sempat terbaca
                // =======================================================================
                setTimeout(function() {
                    window.location.reload();
                }, 1200);
                
            } else {
                if(typeof tampilkanToast === "function") tampilkanToast(result.pesan || '<%= Common.getBahasaConfigJS("Gagal menyimpan kuesioner.") %>', 'bg-danger text-white');
                btn.innerHTML = oriText;
                btn.disabled = false;
            }
        } catch (e) {
            if(typeof tampilkanToast === "function") tampilkanToast('<%= Common.getBahasaConfigJS("Terjadi kesalahan jaringan saat menyimpan data ke peladen.") %>', 'bg-danger text-white');
            btn.innerHTML = oriText;
            btn.disabled = false;
        }
    };
</script>