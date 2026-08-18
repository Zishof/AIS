<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.obe.BahanKajian"%>
<%@page import="ais.database.model.obe.ReferensiLulusan"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%!
    // Helper function untuk menarik Nama/Judul berdasarkan deretan ID
    private Map<String, String> getNamesForIds(Session sess, Class<?> clazz, String ids) {
        Map<String, String> map = new HashMap<String, String>();
        if (ids == null || ids.trim().isEmpty()) return map;
        
        Set<Long> idSet = new HashSet<Long>();
        for (String s : ids.split(",")) {
            if (!s.trim().isEmpty()) {
                try { idSet.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_deskripsi_dan_pustaka.jsp:25");}
            }
        }
        
        if (idSet.isEmpty()) return map;
        
        try {
            List<?> list = ConstantValues.simpleList(sess.createCriteria(clazz).add(Restrictions.in("id", idSet)), clazz);
            for (Object obj : list) {
                Long id = (Long) obj.getClass().getMethod("getId").invoke(obj);
                String nama = "";
                try {
                    nama = (String) obj.getClass().getMethod("getNama").invoke(obj);
                } catch(Exception e) {
                    try {
                        nama = (String) obj.getClass().getMethod("getJudul").invoke(obj);
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_deskripsi_dan_pustaka.jsp:41");}
                }
                if (nama == null || nama.trim().isEmpty()) nama = "Data ID: " + id;
                map.put(String.valueOf(id), nama);
            }
        } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_deskripsi_dan_pustaka.jsp:46"); }
        
        return map;
    }
%>
<%
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean edit = false;
boolean delete = false;
if (tbmuser != null && tbmuser.getUserId() != null) {
    boolean isNotStudentTeacher = tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
            && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null && tbmuser.getGuru() == null;
            
    if (isNotStudentTeacher) {
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    }
    
    if (Common.getApakahAdminLain(tbmuser)) {
        edit = true;
    }
    if (tbmuser.getDosen() != null) {
        edit = true;
    }
}

boolean isMahasiswa = tbmuser.getMahasiswa() != null;
boolean disableForm = !edit || isMahasiswa;

String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
String variable = request.getParameter("var") != null ? request.getParameter("var") : rnd;

if(idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) return;

KurikulumPunyaMatakuliah kpm = null;
Matakuliah mk = null;
Long idJurusanKpm = null;

Map<String, String> mapBahanKajian = new HashMap<String, String>();
Map<String, String> mapPustakaUtama = new HashMap<String, String>();
Map<String, String> mapPustakaPend = new HashMap<String, String>();
Map<String, String> mapDosen = new HashMap<String, String>();
Map<String, String> mapPrasyarat = new HashMap<String, String>();

Session sess = HibernateUtil.openSession();
try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
    if(kpm != null) {
        mk = kpm.getMatakuliah();
        if(kpm.getKurikulum() != null && kpm.getKurikulum().getJurusan() != null) {
            idJurusanKpm = kpm.getKurikulum().getJurusan().getId();
        }
        
        // Memuat nama referensi untuk ditampilkan
        if (mk != null) mapBahanKajian = getNamesForIds(sess, BahanKajian.class, mk.getBahanKajian());
        mapPustakaUtama = getNamesForIds(sess, ReferensiLulusan.class, kpm.getPustaka());
        mapPustakaPend = getNamesForIds(sess, ReferensiLulusan.class, kpm.getPustakaPendukung());
        mapDosen = getNamesForIds(sess, Dosen.class, kpm.getDosen());
        mapPrasyarat = getNamesForIds(sess, Matakuliah.class, kpm.getMkPrasyarat());
    }
} finally {
    if (sess.isOpen()) { sess.disconnect(); sess.close(); }
    HibernateUtil.closeSessionQuietly(sess);
}

if(kpm == null || mk == null) return;
%>

<div class="row g-4 animate__animated animate__fadeIn">

    <% if(!disableForm) { %>
    <div class="col-md-12">
        <div class="alert d-flex justify-content-between align-items-center flex-wrap gap-2 border-0 shadow-sm rounded-4 mb-0" style="background-color:#f5f3ff;">
            <div class="small text-secondary"><i class="fas fa-magic me-2" style="color:#7c3aed;"></i><%=Common.getBahasaConfig("Hasilkan deskripsi, mitra pengembang, bahan kajian, dan pustaka sekaligus dengan AI.")%></div>
            <button type="button" id="btnGenDeskLengkapAi<%=rnd%>" class="btn fw-bold rounded-pill px-4 shadow-sm text-white" style="background-color:#7c3aed;" onclick="window.generateDeskripsiPustakaAi<%=rnd%>()">
                <i class="fas fa-wand-magic-sparkles me-2"></i><%=Common.getBahasaConfig("Generate Deskripsi, Bahan Kajian & Pustaka via AI")%>
            </button>
        </div>
    </div>
    <% } %>

    <div class="col-md-12">
        <div class="card border-0 shadow-sm rounded-4 mb-3">
            <div class="card-header bg-white border-0 pt-4 pb-0 px-4 d-flex justify-content-between align-items-center">
                <h6 class="fw-bold text-primary mb-0"><i class="fas fa-align-left me-2"></i><%=Common.getBahasaConfig("Deskripsi Singkat MK")%></h6>
            </div>
            <div class="card-body p-4">
                <textarea class="form-control bg-light border-0" id="inpDeskripsiMk<%=rnd%>" rows="4" <%=disableForm ? "disabled" : ""%> placeholder="<%=Common.getBahasaConfig("Masukkan deskripsi singkat mengenai matakuliah ini...")%>"><%=kpm.getDeskripsiPembelajaran() != null ? kpm.getDeskripsiPembelajaran() : ""%></textarea>
                <% if(!disableForm) { %>
                <div class="text-end mt-3">
                    <button type="button" id="btnAiDeskripsi<%=rnd%>" class="btn fw-bold rounded-pill px-4 shadow-sm text-white me-2" style="background-color:#16a34a;" onclick="window.generateDeskripsiMkAi<%=rnd%>()"><i class="fas fa-magic me-2"></i><%=Common.getBahasaConfig("Isi via AI")%></button>
                    <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanDeskripsiMk<%=rnd%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Deskripsi")%></button>
                </div>
                <% } %>
            </div>
        </div>
    </div>

    <%
        Object[][] listModules = {
            {"BahanKajian", Common.getBahasaConfig("Bahan Kajian / Materi Pembelajaran"), "fas fa-book-open", mk.getBahanKajian(), mapBahanKajian},
            {"PustakaUtama", Common.getBahasaConfig("Pustaka Utama"), "fas fa-bookmark", kpm.getPustaka(), mapPustakaUtama},
            {"PustakaPendukung", Common.getBahasaConfig("Pustaka Pendukung"), "fas fa-book", kpm.getPustakaPendukung(), mapPustakaPend},
            {"Dosen", Common.getBahasaConfig("Team Teaching / Dosen Pengampu"), "fas fa-users", kpm.getDosen(), mapDosen},
            {"Prasyarat", Common.getBahasaConfig("Matakuliah Prasyarat"), "fas fa-project-diagram", kpm.getMkPrasyarat(), mapPrasyarat}
        };

        for(Object[] mod : listModules) {
            String tipe = (String) mod[0];
            String judul = (String) mod[1];
            String icon = (String) mod[2];
            String dataIds = mod[3] != null ? (String) mod[3] : "";
            Map<String, String> mapData = (Map<String, String>) mod[4];
            int countData = dataIds.isEmpty() ? 0 : dataIds.split(",").length;
    %>
    <div class="col-md-6">
        <div class="card border-0 shadow-sm rounded-4 h-100">
            <div class="card-header bg-white border-bottom-0 pt-4 pb-2 px-4 d-flex justify-content-between align-items-center">
                <h6 class="fw-bold text-secondary mb-0"><i class="<%=icon%> me-2 text-primary"></i><%=judul%></h6>
                <span class="badge bg-primary rounded-pill px-3"><%=countData%> <%=Common.getBahasaConfig("Data")%></span>
            </div>
            <div class="card-body p-4 pt-2">
                <div class="bg-light rounded-3 p-3 border mb-3" style="min-height: 80px; max-height: 200px; overflow-y: auto;">
                    <% if(countData == 0) { %>
                        <div class="text-muted text-center fst-italic small mt-2"><%=Common.getBahasaConfig("Belum ada data yang dipilih.")%></div>
                    <% } else { %>
                        <div class="d-flex flex-wrap gap-2">
                            <% for(String id : dataIds.split(",")) { if(!id.trim().isEmpty()) { 
                                String displayName = mapData.get(id) != null ? mapData.get(id) : ("ID: " + id);
                            %>
                                <span class="badge bg-white text-dark border shadow-sm px-3 py-2 me-1 mb-1">
                                    <%=displayName%>
                                    <% if(!disableForm) { %>
                                    <i class="fas fa-times text-danger ms-2" style="cursor:pointer;" onclick="window.hapusRelasiDeskripsi<%=rnd%>('<%=tipe%>', '<%=id%>', '<%=dataIds%>')" title="<%=Common.getBahasaConfig("Hapus data ini")%>"></i>
                                    <% } %>
                                </span>
                            <% }} %>
                        </div>
                    <% } %>
                </div>
                <% if(!disableForm) { %>
                <button type="button" class="btn btn-outline-primary fw-bold rounded-pill w-100 shadow-sm" onclick="window.bukaDataPickerDesk<%=rnd%>('<%=tipe%>', '<%=dataIds%>')">
                    <i class="fas fa-search me-2"></i><%=Common.getBahasaConfig("Atur " + judul)%>
                </button>
                <% } %>
            </div>
        </div>
    </div>
    <% } %>

</div>

<script>
    var rootUrlDesk<%=rnd%> = "<%=Common.ROOT%>";
    var idKpmDesk<%=rnd%> = "<%=kpm.getId()%>";
    var classKpmDesk<%=rnd%> = "<%=KurikulumPunyaMatakuliah.class.getName()%>";
    var namaMkDesk<%=rnd%> = <%= org.json.JSONObject.quote(mk.getNama() != null ? mk.getNama() : "") %>;
    var prodiMkDesk<%=rnd%> = <%= org.json.JSONObject.quote(mk.getJurusan() != null && mk.getJurusan().getNama() != null ? mk.getJurusan().getNama() : "") %>;
    var sksMkDesk<%=rnd%> = "<%= mk.getSks() != null ? mk.getSks() : 0 %>";

    window.generateDeskripsiMkAi<%=rnd%> = function() {
        var btn = document.getElementById('btnAiDeskripsi<%=rnd%>');
        var teksAsli = btn ? btn.innerHTML : '';
        if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses...")%>'; }
        var prompt = 'Buatkan deskripsi singkat mata kuliah "' + namaMkDesk<%=rnd%> + '"'
            + (prodiMkDesk<%=rnd%> ? ' pada program studi "' + prodiMkDesk<%=rnd%> + '"' : '')
            + (sksMkDesk<%=rnd%> && sksMkDesk<%=rnd%> !== '0' ? ' berbobot ' + sksMkDesk<%=rnd%> + ' SKS' : '')
            + '. Tulis 3-5 kalimat, Bahasa Indonesia akademik formal, menjelaskan cakupan, tujuan, dan relevansi mata kuliah. Jangan gunakan markdown atau HTML.';
        fetch(rootUrlDesk<%=rnd%> + '/Ai', { method:'POST', headers:{'Content-Type':'application/json; charset=UTF-8'}, body: JSON.stringify({ instruksi: prompt, prompt: prompt, temperature: 0.5, source:'obe_deskripsi' }) })
        .then(function(r){ return r.json(); })
        .then(function(da){
            var teks = (da && (da.text || da.response || da.message)) ? (da.text || da.response || da.message) : '';
            if (!da || da.success === false || !teks) throw new Error(da && (da.error || da.raw) ? (da.error || da.raw) : '<%=Common.getBahasaConfigJS("Respons AI kosong.")%>');
            var ta = document.getElementById('inpDeskripsiMk<%=rnd%>');
            if (ta) ta.value = teks.trim();
            if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Deskripsi dihasilkan AI. Silakan tinjau lalu Simpan.")%>', 'bg-success text-white');
        })
        .catch(function(err){ console.error(err); if (typeof tampilkanToast === 'function') tampilkanToast('AI: ' + (err && err.message ? err.message : err), 'bg-danger text-white'); })
        .finally(function(){ if (btn) { btn.disabled = false; btn.innerHTML = teksAsli; } });
    };

    window.generateDeskripsiPustakaAi<%=rnd%> = function() {
        var btn = document.getElementById('btnGenDeskLengkapAi<%=rnd%>');
        var teksAsli = btn ? btn.innerHTML : '';
        if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("AI sedang menyusun konten...")%>'; }
        var body = new URLSearchParams(); body.append('kurikulumPunyaMatakuliah', idKpmDesk<%=rnd%>);
        fetch(rootUrlDesk<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_generate_deskripsi_pustaka_ai', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body.toString() })
        .then(function(r){ return r.json(); })
        .then(function(d){
            if (!d || d.status !== 'success') throw new Error(d && d.message ? d.message : '<%=Common.getBahasaConfigJS("Gagal generate konten.")%>');
            if (typeof tampilkanToast === 'function') tampilkanToast(d.message || '<%=Common.getBahasaConfigJS("Konten berhasil dibuat via AI.")%>', 'bg-success text-white');
            if (typeof window.reloadTabDeskripsi<%=variable%> === 'function') window.reloadTabDeskripsi<%=variable%>();
        })
        .catch(function(err){ console.error(err); if (typeof tampilkanToast === 'function') tampilkanToast('AI: ' + (err && err.message ? err.message : err), 'bg-danger text-white'); })
        .finally(function(){ if (btn) { btn.disabled = false; btn.innerHTML = teksAsli; } });
    };

    window.simpanDeskripsiMk<%=rnd%> = async function() {
        var val = document.getElementById('inpDeskripsiMk<%=rnd%>').value;
        var payload = {
            action: "simpanDataRinci",
            class: classKpmDesk<%=rnd%>,
            id: idKpmDesk<%=rnd%>,
            data: { deskripsiPembelajaran: val },
            tanpaLogin: "true"
        };
        try {
            var res = await fetch(rootUrlDesk<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            var result = await res.json();
            if (result.status === '00' || result.status === 'success') {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Deskripsi berhasil disimpan.")%>', 'bg-success text-white');
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan deskripsi.")%>', 'bg-danger text-white');
            }
        } catch (e) {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
        }
    };

    window.bukaDataPickerDesk<%=rnd%> = function(tipeList, existingIds) {
        var className = '';
        var title = '';
        
        if(tipeList === 'BahanKajian') {
            className = '<%=BahanKajian.class.getName()%>';
            title = '<%=Common.getBahasaConfigJS("Bahan Kajian")%>';
        } else if(tipeList === 'PustakaUtama') {
            className = '<%=ReferensiLulusan.class.getName()%>';
            title = '<%=Common.getBahasaConfigJS("Pustaka Utama")%>';
        } else if(tipeList === 'PustakaPendukung') {
            className = '<%=ReferensiLulusan.class.getName()%>';
            title = '<%=Common.getBahasaConfigJS("Pustaka Pendukung")%>';
        } else if(tipeList === 'Dosen') {
            className = '<%=Dosen.class.getName()%>';
            title = '<%=Common.getBahasaConfigJS("Dosen Pengampu")%>';
        } else if(tipeList === 'Prasyarat') {
            className = '<%=Matakuliah.class.getName()%>';
            title = '<%=Common.getBahasaConfigJS("Mata Kuliah Prasyarat")%>';
        }

        var jsRnd = Math.random().toString(36).substring(2, 9);
        window['cbDeskripsi' + jsRnd] = function(selIds) { window.simpanRelasiDeskripsi<%=rnd%>(tipeList, selIds, existingIds); };
        
        var idJurusan = '<%=idJurusanKpm != null ? idJurusanKpm : ""%>';

        var url = '<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=common&s=_data_picker_banyak' +
                  '&class=' + encodeURIComponent(className) +
                  '&title=' + encodeURIComponent(title) +
                  '&idsTerpilih=' + encodeURIComponent(existingIds) +
                  '&callback=cbDeskripsi' + jsRnd +
                  '&jurusan=' + idJurusan +
                  '&var=' + jsRnd;
                  
        var modalHtml = '<div class="modal fade" id="modalPicker' + jsRnd + '" tabindex="-1" aria-hidden="true">' +
                        '<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">' +
                        '<div class="modal-content" id="modalContent' + jsRnd + '">' +
                        '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></div>' +
                        '</div></div></div>';
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        var modalEl = document.getElementById('modalPicker' + jsRnd);
        var modalObj = new bootstrap.Modal(modalEl);
        modalObj.show();
        
        fetch(url).then(res => res.text()).then(html => {
            document.getElementById('modalContent' + jsRnd).innerHTML = html;
            var scripts = document.getElementById('modalContent' + jsRnd).querySelectorAll('script');
            scripts.forEach(s => {
                var ns = document.createElement('script');
                if(s.src) ns.src = s.src;
                else ns.textContent = s.textContent;
                document.body.appendChild(ns);
                document.body.removeChild(ns);
            });
        }).catch(e => {
            document.getElementById('modalContent' + jsRnd).innerHTML = '<div class="alert alert-danger m-3"><%=Common.getBahasaConfig("Gagal memuat data picker.")%></div>';
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            modalEl.remove();
        });
    };

    window.simpanRelasiDeskripsi<%=rnd%> = async function(tipeList, selectedIds, existingIds) {
        var idTarget = idKpmDesk<%=rnd%>;
        var classTarget = classKpmDesk<%=rnd%>;
        var dataUpdate = {};
        
        var existingArr = existingIds ? existingIds.split(",").filter(function(x) { return x.trim() !== ""; }) : [];
        
        if(selectedIds && selectedIds.length > 0) {
            selectedIds.forEach(function(id) {
                if(existingArr.indexOf(String(id)) === -1) {
                    existingArr.push(String(id));
                }
            });
        }
        
        var p = existingArr.length > 0 ? "," + existingArr.join(",") + "," : "";
        
        if(tipeList === 'BahanKajian') { 
            dataUpdate['bahanKajian'] = p; 
            idTarget = '<%=mk.getId()%>';
            classTarget = '<%=Matakuliah.class.getName()%>';
        }
        else if(tipeList === 'PustakaUtama') { dataUpdate['pustaka'] = p; }
        else if(tipeList === 'PustakaPendukung') { dataUpdate['pustakaPendukung'] = p; }
        else if(tipeList === 'Dosen') { dataUpdate['dosen'] = p; }
        else if(tipeList === 'Prasyarat') { dataUpdate['mkPrasyarat'] = p; }

        try {
            var payload = { action: "simpanDataRinci", class: classTarget, id: idTarget, data: dataUpdate, tanpaLogin: "true" };
            var res = await fetch(rootUrlDesk<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            var result = await res.json();
            
            if (result.status === '00' || result.status === 'success') {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Daftar referensi berhasil diperbarui.")%>', 'bg-success text-white');
                if (typeof window.reloadTabDeskripsi<%=variable%> === 'function') window.reloadTabDeskripsi<%=variable%>();
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast(result.description || '<%=Common.getBahasaConfigJS("Gagal memperbarui referensi.")%>', 'bg-danger text-white');
            }
        } catch (e) { 
            console.error(e); 
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
        }
    };

    window.hapusRelasiDeskripsi<%=rnd%> = function(tipeList, idHapus, existingIds) {
        if(typeof showConfirmModal === 'function') {
            showConfirmModal('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus referensi ini?")%>', async function() {
                var existingArr = existingIds ? existingIds.split(",").filter(function(x) { return x.trim() !== ""; }) : [];
                existingArr = existingArr.filter(function(x) { return x !== String(idHapus); });
                var p = existingArr.length > 0 ? "," + existingArr.join(",") + "," : "";

                var idTarget = idKpmDesk<%=rnd%>;
                var classTarget = classKpmDesk<%=rnd%>;
                var dataUpdate = {};
                
                if(tipeList === 'BahanKajian') { 
                    dataUpdate['bahanKajian'] = p; 
                    idTarget = '<%=mk.getId()%>';
                    classTarget = '<%=Matakuliah.class.getName()%>';
                }
                else if(tipeList === 'PustakaUtama') { dataUpdate['pustaka'] = p; }
                else if(tipeList === 'PustakaPendukung') { dataUpdate['pustakaPendukung'] = p; }
                else if(tipeList === 'Dosen') { dataUpdate['dosen'] = p; }
                else if(tipeList === 'Prasyarat') { dataUpdate['mkPrasyarat'] = p; }

                try {
                    var payload = { action: "simpanDataRinci", class: classTarget, id: idTarget, data: dataUpdate, tanpaLogin: "true" };
                    var res = await fetch(rootUrlDesk<%=rnd%> + '/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
                    var result = await res.json();
                    
                    if (result.status === '00' || result.status === 'success') {
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Data referensi berhasil dihapus.")%>', 'bg-success text-white');
                        if (typeof window.reloadTabDeskripsi<%=variable%> === 'function') window.reloadTabDeskripsi<%=variable%>();
                    } else {
                        if(typeof tampilkanToast === 'function') tampilkanToast(result.description || '<%=Common.getBahasaConfigJS("Gagal menghapus data referensi.")%>', 'bg-danger text-white');
                    }
                } catch (e) { 
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
                }
            });
        }
    };
</script>