<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>
<%@page import="ais.database.model.obe.CapaianPembelajaranLulusan"%>
<%@page import="ais.database.model.obe.BahanKajian"%>
<%@page import="ais.database.model.obe.ReferensiLulusan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
String keyData = request.getParameter("keyData") != null ? request.getParameter("keyData") : "";

KurikulumPunyaMatakuliah kpm = null;
Matakuliah mk = null;

List<CapaianPembelajaranLulusan> listCpmkMaster = new ArrayList<CapaianPembelajaranLulusan>();
List<BahanKajian> listBahanKajian = new ArrayList<BahanKajian>();
List<ReferensiLulusan> listPustakaUtama = new ArrayList<ReferensiLulusan>();
List<ReferensiLulusan> listPustakaPend = new ArrayList<ReferensiLulusan>();

Session sess = HibernateUtil.openSession();
try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
    if(kpm != null) {
        mk = kpm.getMatakuliah();
        if(mk != null && mk.getCapaianPembelajaranLulusan() != null && !mk.getCapaianPembelajaranLulusan().trim().isEmpty()) {
            Set<Long> longs = new HashSet<Long>();
            for(String s : mk.getCapaianPembelajaranLulusan().split(",")) {
                if(!s.trim().isEmpty()) { try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rincian_pertemuan_form.jsp:38");} }
            }
            if(!longs.isEmpty()) {
                listCpmkMaster = ConstantValues.simpleList(sess.createCriteria(CapaianPembelajaranLulusan.class).add(Restrictions.in("id", longs)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama")), CapaianPembelajaranLulusan.class);
            }
        }
        if (mk != null && mk.getBahanKajian() != null && !mk.getBahanKajian().trim().isEmpty()) {
            Set<Long> longs = new HashSet<Long>();
            for(String s : mk.getBahanKajian().split(",")) { if(!s.trim().isEmpty()) { try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rincian_pertemuan_form.jsp:46");} } }
            if(!longs.isEmpty()) listBahanKajian = ConstantValues.simpleList(sess.createCriteria(BahanKajian.class).add(Restrictions.in("id", longs)).addOrder(Order.asc("nama")), BahanKajian.class);
        }
        if (kpm.getPustaka() != null && !kpm.getPustaka().trim().isEmpty()) {
            Set<Long> longs = new HashSet<Long>();
            for(String s : kpm.getPustaka().split(",")) { if(!s.trim().isEmpty()) { try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rincian_pertemuan_form.jsp:51");} } }
            if(!longs.isEmpty()) listPustakaUtama = ConstantValues.simpleList(sess.createCriteria(ReferensiLulusan.class).add(Restrictions.in("id", longs)).addOrder(Order.asc("nama")), ReferensiLulusan.class);
        }
        if (kpm.getPustakaPendukung() != null && !kpm.getPustakaPendukung().trim().isEmpty()) {
            Set<Long> longs = new HashSet<Long>();
            for(String s : kpm.getPustakaPendukung().split(",")) { if(!s.trim().isEmpty()) { try { longs.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rincian_pertemuan_form.jsp:56");} } }
            if(!longs.isEmpty()) listPustakaPend = ConstantValues.simpleList(sess.createCriteria(ReferensiLulusan.class).add(Restrictions.in("id", longs)).addOrder(Order.asc("nama")), ReferensiLulusan.class);
        }
    }
} finally {
    if (sess.isOpen()) { sess.disconnect(); sess.close(); }
    HibernateUtil.closeSessionQuietly(sess);
}
if(kpm == null) return;

boolean isNilaiCpmk = kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk();
JSONObject jsonMap = new JSONObject();
try { jsonMap = new JSONObject(kpm.getRincian() != null ? kpm.getRincian() : "{}"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rincian_pertemuan_form.jsp:68");}

JSONObject objData = keyData.isEmpty() ? new JSONObject() : jsonMap.optJSONObject(keyData);
if(objData == null) objData = new JSONObject();

// BUILD OPTIONS
JSONArray arrOpsiCpmk = new JSONArray();
if(isNilaiCpmk) {
    for(CapaianPembelajaranLulusan cpl : listCpmkMaster) {
        JSONObject o = new JSONObject(); o.put("val", cpl.getId()); o.put("label", cpl.getKode() + " " + cpl.getNama()); o.put("desc", ""); arrOpsiCpmk.put(o);
    }
} else {
    for(CapaianPembelajaranLulusan cpl : listCpmkMaster) {
        if(cpl.getFormula() != null && !cpl.getFormula().trim().isEmpty()) {
            JSONArray arrFormula = new JSONArray();
            try { arrFormula = new JSONArray(cpl.getFormula()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rincian_pertemuan_form.jsp:83");}
            for(int i=0; i<arrFormula.length(); i++) {
                JSONObject fd = arrFormula.optJSONObject(i);
                if(fd == null || fd.optString("key", "").isEmpty()) continue;
                JSONObject o = new JSONObject();
                o.put("val", fd.optString("key", "") + "_" + cpl.getId());
                o.put("label", fd.optString("kode", "") + " " + fd.optString("nama", ""));
                o.put("desc", cpl.getKode() + " " + cpl.getNama());
                arrOpsiCpmk.put(o);
            }
        }
    }
}

JSONArray arrOpsiBahan = new JSONArray(); for(BahanKajian bk : listBahanKajian) { JSONObject o = new JSONObject(); o.put("val", bk.getId()); o.put("label", bk.getNama()); arrOpsiBahan.put(o); }
JSONArray arrOpsiUtama = new JSONArray(); for(ReferensiLulusan rf : listPustakaUtama) { JSONObject o = new JSONObject(); o.put("val", rf.getId()); o.put("label", rf.getNama()); arrOpsiUtama.put(o); }
JSONArray arrOpsiPend = new JSONArray(); for(ReferensiLulusan rf : listPustakaPend) { JSONObject o = new JSONObject(); o.put("val", rf.getId()); o.put("label", rf.getNama()); arrOpsiPend.put(o); }

// PARSE DEFAULTS
int mulai = objData.optInt("mulaiMingguKe", 1);
int sampai = objData.optInt("sampaiMingguKe", 1);
int jmlCpmk = objData.optInt("jumlahCpmk", 1);

String ind = objData.optString("indikator", Common.getBahasaConfig("Ketepatan .."));
String tek = objData.optString("teknikDanKriteria", "- Kriteria: ...\n\n- Bentuk: ...");
String met = objData.optString("metodePembelajaran", Common.getBahasaConfig("Project Based Learning atau lainnya .."));
String lur = objData.optString("pembelajaranLuring", "- Kuliah: ...\n\n- Diskusi: ...\n\n- Tugas Individu: ...\n\n- Membentuk kelompok: ...");
String dar = objData.optString("pembelajaranDaring", Common.getBahasaConfig("e-Learning: dilakukan dengan memanfaatkan aplikasi e-campus"));

JSONObject bahanSel = objData.optJSONObject("bahanKajians") != null ? objData.optJSONObject("bahanKajians") : new JSONObject();
JSONObject utamaSel = objData.optJSONObject("pustakaUtamas") != null ? objData.optJSONObject("pustakaUtamas") : new JSONObject();
JSONObject pendSel  = objData.optJSONObject("pustakaPendukungs") != null ? objData.optJSONObject("pustakaPendukungs") : new JSONObject();

String titleText = keyData.isEmpty() ? Common.getBahasaConfig("Tambah Rincian Pertemuan") : Common.getBahasaConfig("Edit Rincian Pertemuan");
String labelCpmk = Common.getBahasaConfig(isNilaiCpmk ? "CPMK *" : "Sub-CPMK *");
%>

<div class="modal fade" id="modalFormRinci_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content shadow-lg border-0 rounded-4">
            <div class="modal-header bg-primary text-white border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold"><i class="fas fa-list-alt me-2"></i><%=titleText%></h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body p-4 bg-light">
                <div class="row g-4">
                    
                    <div class="col-md-4">
                        <div class="card border-0 shadow-sm rounded-4 h-100">
                            <div class="card-body">
                                <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><%=Common.getBahasaConfig("Waktu & Metode")%></h6>
                                <div class="row g-2 mb-3">
                                    <div class="col-6">
                                        <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Mulai Mg Ke")%> *</label>
                                        <input type="number" class="form-control" id="inpMulai_<%=rnd%>" value="<%=mulai%>" min="1" max="24">
                                    </div>
                                    <div class="col-6">
                                        <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Sampai Mg Ke")%> *</label>
                                        <input type="number" class="form-control" id="inpSampai_<%=rnd%>" value="<%=sampai%>" min="1" max="24">
                                    </div>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Metode Pembelajaran")%> *</label>
                                    <textarea class="form-control" id="inpMetode_<%=rnd%>" rows="3"><%=met%></textarea>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Pembelajaran Luring")%> *</label>
                                    <textarea class="form-control" id="inpLuring_<%=rnd%>" rows="3"><%=lur%></textarea>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Pembelajaran Daring")%> *</label>
                                    <textarea class="form-control" id="inpDaring_<%=rnd%>" rows="3"><%=dar%></textarea>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="col-md-4">
                        <div class="card border-0 shadow-sm rounded-4 h-100">
                            <div class="card-body">
                                <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><%=Common.getBahasaConfig("Target & Evaluasi")%></h6>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Jumlah Target")%> *</label>
                                    <select class="form-select bg-light" id="inpJml_<%=rnd%>" onchange="window.toggleCpmkDropdownRinci<%=rnd%>()">
                                        <% for(int i=1; i<=5; i++) { %>
                                            <option value="<%=i%>" <%=i == jmlCpmk ? "selected" : ""%>><%=i%> <%=isNilaiCpmk ? "CPMK" : "Sub-CPMK"%></option>
                                        <% } %>
                                    </select>
                                </div>
                                <div class="p-2 border rounded bg-white mb-4" id="wrapOpsiCpmk_<%=rnd%>">
                                    </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Indikator")%> *</label>
                                    <textarea class="form-control" id="inpIndikator_<%=rnd%>" rows="3"><%=ind%></textarea>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small"><%=Common.getBahasaConfig("Teknik & Kriteria")%> *</label>
                                    <textarea class="form-control" id="inpTeknik_<%=rnd%>" rows="3"><%=tek%></textarea>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="col-md-4">
                        <div class="card border-0 shadow-sm rounded-4 h-100">
                            <div class="card-body">
                                <h6 class="fw-bold text-secondary mb-3 border-bottom pb-2"><%=Common.getBahasaConfig("Materi & Pustaka")%></h6>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small text-primary"><%=Common.getBahasaConfig("Bahan Kajian (Materi)")%></label>
                                    <div class="p-2 border rounded bg-white" id="wrapBahan_<%=rnd%>" style="max-height: 150px; overflow-y: auto;"></div>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small text-success"><%=Common.getBahasaConfig("Pustaka Utama")%></label>
                                    <div class="p-2 border rounded bg-white" id="wrapUtama_<%=rnd%>" style="max-height: 150px; overflow-y: auto;"></div>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-bold small text-warning"><%=Common.getBahasaConfig("Pustaka Pendukung")%></label>
                                    <div class="p-2 border rounded bg-white" id="wrapPend_<%=rnd%>" style="max-height: 150px; overflow-y: auto;"></div>
                                </div>
                            </div>
                        </div>
                    </div>

                </div>
            </div>
            <div class="modal-footer bg-white border-top p-3 justify-content-end">
                <button type="button" class="btn btn-light fw-bold rounded-pill px-4" data-bs-dismiss="modal"><i class="fas fa-times me-2 text-danger"></i><%=Common.getBahasaConfig("Batal")%></button>
                <button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanDataRinciServer<%=rnd%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Rincian")%></button>
            </div>
        </div>
    </div>
</div>

<script>
    var keyDataEdit<%=rnd%> = "<%=keyData%>";
    var masterJson<%=rnd%> = <%=jsonMap.toString()%>;
    var objData<%=rnd%> = <%=objData.toString()%>;

    var arrOpsiCpmk<%=rnd%> = <%=arrOpsiCpmk.toString()%>;
    var arrOpsiBahan<%=rnd%> = <%=arrOpsiBahan.toString()%>;
    var arrOpsiUtama<%=rnd%> = <%=arrOpsiUtama.toString()%>;
    var arrOpsiPend<%=rnd%> = <%=arrOpsiPend.toString()%>;

    window.renderSelectHtml<%=rnd%> = function(idSelect, labelTxt, opsiArr, selectedVal) {
        var html = '<div class="mb-3" id="wrap_'+idSelect+'"><label class="form-label fw-bold small text-primary">'+labelTxt+'</label>';
        html += '<select class="form-select" id="'+idSelect+'"><option value="">-- Pilih --</option>';
        opsiArr.forEach(function(o) {
            html += '<option value="'+o.val+'" '+(o.val === selectedVal ? 'selected' : '')+'>'+o.label+'</option>';
        });
        html += '</select></div>';
        return html;
    };

    window.renderCheckboxHtml<%=rnd%> = function(groupId, opsiArr, selectedMap) {
        if(!opsiArr || opsiArr.length === 0) return '<div class="text-muted fst-italic small"><%=Common.getBahasaConfig("Data kosong")%></div>';
        var html = '';
        opsiArr.forEach(function(o) {
            var isChecked = selectedMap.hasOwnProperty(String(o.val));
            var idChk = "chk_" + groupId + "_" + o.val;
            html += '<div class="form-check">';
            html += '<input class="form-check-input chk-group-'+groupId+'" type="checkbox" id="'+idChk+'" value="'+o.val+'" data-name="'+o.label+'" '+(isChecked ? 'checked' : '')+'>';
            html += '<label class="form-check-label" for="'+idChk+'">' + o.label + '</label>';
            html += '</div>';
        });
        return html;
    };

    document.getElementById('wrapOpsiCpmk_<%=rnd%>').innerHTML = 
        window.renderSelectHtml<%=rnd%>('inpCpmk1_<%=rnd%>', '<%=labelCpmk%>', arrOpsiCpmk<%=rnd%>, objData<%=rnd%>.sub_cpmk || "") +
        window.renderSelectHtml<%=rnd%>('inpCpmk2_<%=rnd%>', '<%=labelCpmk%> Ke-2', arrOpsiCpmk<%=rnd%>, objData<%=rnd%>.sub_cpmk2 || "") +
        window.renderSelectHtml<%=rnd%>('inpCpmk3_<%=rnd%>', '<%=labelCpmk%> Ke-3', arrOpsiCpmk<%=rnd%>, objData<%=rnd%>.sub_cpmk3 || "") +
        window.renderSelectHtml<%=rnd%>('inpCpmk4_<%=rnd%>', '<%=labelCpmk%> Ke-4', arrOpsiCpmk<%=rnd%>, objData<%=rnd%>.sub_cpmk4 || "") +
        window.renderSelectHtml<%=rnd%>('inpCpmk5_<%=rnd%>', '<%=labelCpmk%> Ke-5', arrOpsiCpmk<%=rnd%>, objData<%=rnd%>.sub_cpmk5 || "");

    document.getElementById('wrapBahan_<%=rnd%>').innerHTML = window.renderCheckboxHtml<%=rnd%>('bahan', arrOpsiBahan<%=rnd%>, objData<%=rnd%>.bahanKajians || {});
    document.getElementById('wrapUtama_<%=rnd%>').innerHTML = window.renderCheckboxHtml<%=rnd%>('utama', arrOpsiUtama<%=rnd%>, objData<%=rnd%>.pustakaUtamas || {});
    document.getElementById('wrapPend_<%=rnd%>').innerHTML = window.renderCheckboxHtml<%=rnd%>('pend', arrOpsiPend<%=rnd%>, objData<%=rnd%>.pustakaPendukungs || {});

    window.toggleCpmkDropdownRinci<%=rnd%> = function() {
        var val = parseInt(document.getElementById('inpJml_<%=rnd%>').value);
        document.getElementById('wrap_inpCpmk1_<%=rnd%>').style.display = 'block';
        document.getElementById('wrap_inpCpmk2_<%=rnd%>').style.display = (val >= 2) ? 'block' : 'none';
        document.getElementById('wrap_inpCpmk3_<%=rnd%>').style.display = (val >= 3) ? 'block' : 'none';
        document.getElementById('wrap_inpCpmk4_<%=rnd%>').style.display = (val >= 4) ? 'block' : 'none';
        document.getElementById('wrap_inpCpmk5_<%=rnd%>').style.display = (val >= 5) ? 'block' : 'none';
    };

    window.toggleCpmkDropdownRinci<%=rnd%>();

    var modalEl = document.getElementById('modalFormRinci_<%=rnd%>');
    var myModalRinci = new bootstrap.Modal(modalEl);
    myModalRinci.show();

    window.simpanDataRinciServer<%=rnd%> = async function() {
        var mulai = parseInt(document.getElementById('inpMulai_<%=rnd%>').value);
        var sampai = parseInt(document.getElementById('inpSampai_<%=rnd%>').value);
        if(!mulai || !sampai) { if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Minggu Mulai & Sampai wajib diisi.")%>', 'bg-warning text-dark'); return; }
        if(sampai < mulai) sampai = mulai;

        var sub1 = document.getElementById('inpCpmk1_<%=rnd%>').value;
        if (!sub1) { if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("CPMK/Sub-CPMK Pertama wajib diisi.")%>', 'bg-warning text-dark'); return; }

        var indikator = document.getElementById('inpIndikator_<%=rnd%>').value.trim();
        var teknik = document.getElementById('inpTeknik_<%=rnd%>').value.trim();
        var metode = document.getElementById('inpMetode_<%=rnd%>').value.trim();
        var luring = document.getElementById('inpLuring_<%=rnd%>').value.trim();
        var daring = document.getElementById('inpDaring_<%=rnd%>').value.trim();

        if(!indikator || !teknik || !metode || !luring || !daring) {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Mohon lengkapi semua isian teks.")%>', 'bg-warning text-dark'); return;
        }

        // CLASH VALIDATION (Hanya cek jika buat baru, bukan saat edit)
        if(keyDataEdit<%=rnd%> === "") {
            for(var k in masterJson<%=rnd%>) {
                if(masterJson<%=rnd%>.hasOwnProperty(k)) {
                    var objLama = masterJson<%=rnd%>[k];
                    var mM = objLama.mulaiMingguKe || 0;
                    var mS = objLama.sampaiMingguKe || 0;
                    if((mM <= mulai && mS >= mulai) || (mM <= sampai && mS >= sampai)) {
                        if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Minggu pertemuan sudah terdaftar dalam rincian.")%>', 'bg-danger text-white'); return;
                    }
                }
            }
        }

        var newData = {};
        newData.mulaiMingguKe = mulai;
        newData.sampaiMingguKe = sampai;
        newData.jumlahCpmk = parseInt(document.getElementById('inpJml_<%=rnd%>').value);
        
        newData.indikator = indikator;
        newData.teknikDanKriteria = teknik;
        newData.metodePembelajaran = metode;
        newData.pembelajaranLuring = luring;
        newData.pembelajaranDaring = daring;

        var getCpmkDesc = function(val) {
            var found = arrOpsiCpmk<%=rnd%>.find(function(x) { return String(x.val) === String(val); });
            return found ? found.label : "";
        };

        newData.sub_cpmk = sub1; newData.sub_cpmk_des = getCpmkDesc(sub1);
        if(newData.jumlahCpmk >= 2) { var v = document.getElementById('inpCpmk2_<%=rnd%>').value; newData.sub_cpmk2 = v; newData.sub_cpmk_des2 = getCpmkDesc(v); }
        if(newData.jumlahCpmk >= 3) { var v = document.getElementById('inpCpmk3_<%=rnd%>').value; newData.sub_cpmk3 = v; newData.sub_cpmk_des3 = getCpmkDesc(v); }
        if(newData.jumlahCpmk >= 4) { var v = document.getElementById('inpCpmk4_<%=rnd%>').value; newData.sub_cpmk4 = v; newData.sub_cpmk_des4 = getCpmkDesc(v); }
        if(newData.jumlahCpmk >= 5) { var v = document.getElementById('inpCpmk5_<%=rnd%>').value; newData.sub_cpmk5 = v; newData.sub_cpmk_des5 = getCpmkDesc(v); }

        var getCheckboxMap = function(groupClass) {
            var map = {};
            document.querySelectorAll('.' + groupClass + ':checked').forEach(function(el) {
                map[el.value] = { nama: el.getAttribute('data-name') };
            });
            return map;
        };

        newData.bahanKajians = getCheckboxMap('chk-group-bahan');
        newData.pustakaUtamas = getCheckboxMap('chk-group-utama');
        newData.pustakaPendukungs = getCheckboxMap('chk-group-pend');

        var finalKey = keyDataEdit<%=rnd%> === "" ? "<%=Common.getGeneratedBarCode(15)%>" : keyDataEdit<%=rnd%>;
        masterJson<%=rnd%>[finalKey] = newData;

        try {
            var payload = { action: "simpanDataRinci", class: "<%=KurikulumPunyaMatakuliah.class.getName()%>", id: "<%=kpm.getId()%>", data: { rincian: JSON.stringify(masterJson<%=rnd%>) }, tanpaLogin: "true" };
            var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            var result = await res.json();
            
            if (result.status === '00' || result.status === 'success') {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Rincian berhasil disimpan.")%>', 'bg-success text-white');
                
                // EKIVALEN DARI Common.createDefaultTimer (Memberi jeda penutupan modal & render ulang)
                myModalRinci.hide();
                setTimeout(function() {
                    if (typeof window.reloadListRincian<%=rnd%> === 'function') {
                        window.reloadListRincian<%=rnd%>();
                    }
                }, 350); 
                
            } else {
                if(typeof tampilkanToast === 'function') tampilkanToast(result.description || '<%=Common.getBahasaConfigJS("Gagal menyimpan rincian.")%>', 'bg-danger text-white');
            }
        } catch(e) {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
        }
    };

    modalEl.addEventListener('hidden.bs.modal', function () {
        this.remove(); // Bersihkan DOM secara otomatis setelah modal tertutup
    });
</script>