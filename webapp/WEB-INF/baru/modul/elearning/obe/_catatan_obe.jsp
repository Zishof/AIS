<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean edit = false;
if (tbmuser != null && tbmuser.getUserId() != null) {
    boolean isNotStudentTeacher = tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
            && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null && tbmuser.getGuru() == null;
            
    if (isNotStudentTeacher) {
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    }
    if (Common.getApakahAdminLain(tbmuser)) {
        edit = true;
    }
}

String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
String variable = request.getParameter("var") != null ? request.getParameter("var") : rnd;

if(idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) return;

KurikulumPunyaMatakuliah kpm = null;

Session sess = HibernateUtil.openSession();
try {
    kpm = (KurikulumPunyaMatakuliah) sess.get(KurikulumPunyaMatakuliah.class, Long.parseLong(idKpmStr));
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_catatan_obe.jsp:39");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
}

if (kpm == null) return;

boolean isLocked = kpm.getDikunci() != null;
boolean disableForm = isLocked || !edit;

String catatanIsi = kpm.getCatatan() != null ? kpm.getCatatan() : "";
%>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4">
        
        <div class="d-flex justify-content-between align-items-end mb-3 border-bottom pb-2 flex-wrap gap-2">
            <div>
                <h6 class="fw-bold text-secondary mb-1"><i class="fas fa-clipboard me-2"></i><%=Common.getBahasaConfig("Catatan RPS")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Tuliskan catatan tambahan mengenai Rencana Pembelajaran Semester ini.")%></small>
            </div>
            <% if(!disableForm) { %>
            <button type="button" id="btnGenDataObeAi<%=rnd%>" class="btn btn-sm fw-bold rounded-pill px-3 shadow-sm text-white" style="background-color:#7c3aed;" onclick="window.generateDataObeAi<%=rnd%>()">
                <i class="fas fa-magic me-2"></i><%=Common.getBahasaConfig("Generate Catatan & Data OBE via AI")%>
            </button>
            <% } %>
        </div>

        <% if (disableForm) { %>
            <div class="border rounded-3 bg-light p-4" style="min-height: 200px;">
                <% if(catatanIsi.trim().isEmpty()) { %>
                    <span class="text-muted fst-italic"><%=Common.getBahasaConfig("Belum ada catatan.")%></span>
                <% } else { %>
                    <%= catatanIsi %>
                <% } %>
            </div>
        <% } else { %>
            <%
                String placeholderCatatan = Common.getBahasaConfig("Tuliskan catatan secara jelas dan terperinci di sini...");
                String jenisUpload = "catatanObe_" + rnd;
                String idTextCatatan = "catatan_" + rnd;
                String onchangeScript = "window.simpanCatatanKpm" + rnd + "();";
            %>
            <div class="border rounded-3 shadow-sm p-1">
                <jsp:include page="/WEB-INF/baru/modul/common/text_area.jsp">
                    <jsp:param name="placeholder" value="<%= placeholderCatatan %>" />
                    <jsp:param name="idtextarea" value="<%= idTextCatatan %>" />
                    <jsp:param name="jenis" value="<%= jenisUpload %>" />
                    <jsp:param name="isi" value="<%= catatanIsi %>" />
                    <jsp:param name="namatextarea" value="<%= idTextCatatan %>" />
                    <jsp:param name="onchange" value="<%= onchangeScript %>" />
                </jsp:include>
            </div>
        <% } %>

    </div>
</div>

<script>
    window.simpanCatatanKpm<%=rnd%> = async function() {
        var el = document.getElementById('catatan_<%=rnd%>');
        if (!el) return;

        var value = el.value;
        var dataUpdate = { catatan: value.trim() || null };

        try {
            var payload = { 
                action: "simpanDataRinci", 
                class: "<%=KurikulumPunyaMatakuliah.class.getName()%>", 
                id: "<%=kpm.getId()%>", 
                data: dataUpdate 
            };
            var res = await fetch('<%=Common.ROOT%>/Data', { 
                method: 'POST', 
                headers: { 'Content-Type': 'application/json' }, 
                body: JSON.stringify(payload) 
            });
            var result = await res.json();
            
            if (result.status === '00' || result.status === 'success') {
                if(typeof tampilkanToast === "function") {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Catatan berhasil disimpan otomatis.")%>', 'bg-success text-white');
                }
            } else {
                if(typeof tampilkanToast === "function") {
                    tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menyimpan catatan.")%>', 'bg-danger text-white');
                }
            }
        } catch (e) {
            console.error(e);
        }
    };

    // GENERATE Catatan & Data OBE via AI (mengisi catatan, bobot CPL, komponen, teknik, rubrik, pemetaan soal)
    window.generateDataObeAi<%=rnd%> = function() {
        var btn = document.getElementById('btnGenDataObeAi<%=rnd%>');
        var asli = btn ? btn.innerHTML : '';
        if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("AI menyusun data OBE...")%>'; }
        var body = new URLSearchParams(); body.append('kurikulumPunyaMatakuliah', '<%=kpm.getId()%>');
        fetch('<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_generate_catatan_obe_ai', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body.toString() })
        .then(function(r){ return r.json(); })
        .then(function(d){
            if (!d || d.status !== 'success') throw new Error(d && d.message ? d.message : '<%=Common.getBahasaConfigJS("Gagal generate data OBE.")%>');
            if (typeof tampilkanToast === 'function') tampilkanToast(d.message || '<%=Common.getBahasaConfigJS("Data OBE berhasil dibuat via AI.")%>', 'bg-success text-white');
            if (typeof window.muatUlangSemuaTabel === 'function') window.muatUlangSemuaTabel();
            else if (typeof window.reloadTabCatatan<%=variable%> === 'function') window.reloadTabCatatan<%=variable%>();
        })
        .catch(function(err){ console.error(err); if (typeof tampilkanToast === 'function') tampilkanToast('AI: ' + (err && err.message ? err.message : err), 'bg-danger text-white'); })
        .finally(function(){ if (btn) { btn.disabled = false; btn.innerHTML = asli; } });
    };
</script>