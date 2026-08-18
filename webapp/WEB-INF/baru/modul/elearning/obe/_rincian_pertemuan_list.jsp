<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null) return;

boolean edit = false, delete = false;
if (tbmuser.getUserId() != null) {
    boolean isNotStudentTeacher = tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
            && tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null && tbmuser.getGuru() == null;
    if (isNotStudentTeacher) {
        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
    }
    if (Common.getApakahAdminLain(tbmuser) || tbmuser.getDosen() != null) { edit = true; delete = true; }
}
boolean disableForm = !edit || tbmuser.getMahasiswa() != null || tbmuser.getDosen() != null;
String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");

KurikulumPunyaMatakuliah kpm = null;
Session sess = HibernateUtil.openSession();
try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
} finally {
    if (sess.isOpen()) { sess.disconnect(); sess.close(); }
    HibernateUtil.closeSessionQuietly(sess);
}
if(kpm == null) return;

boolean isNilaiCpmk = kpm.getNilaiMenggunakanCpmk() != null && kpm.getNilaiMenggunakanCpmk();
String jsonStr = kpm.getRincian() != null && !kpm.getRincian().trim().isEmpty() ? kpm.getRincian() : "{}";
JSONObject mapRinci = new JSONObject();
try { mapRinci = new JSONObject(jsonStr); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_rincian_pertemuan_list.jsp:42");}

List<JSONObject> listUrut = new ArrayList<JSONObject>();
Iterator<String> keys = mapRinci.keys();
while(keys.hasNext()) {
    String k = keys.next();
    JSONObject o = mapRinci.getJSONObject(k);
    if(o.has("mulaiMingguKe")) { 
        o.put("keyData", k);
        listUrut.add(o);
    }
}
Collections.sort(listUrut, new Comparator<JSONObject>() {
    public int compare(JSONObject a, JSONObject b) {
        return Integer.compare(a.optInt("mulaiMingguKe", 0), b.optInt("mulaiMingguKe", 0));
    }
});
%>

<div class="table-responsive border rounded-3">
    <table class="table table-hover align-middle mb-0 text-start small">
        <thead class="table-light">
            <tr>
                <th class="text-center" style="width: 70px;"><%=Common.getBahasaConfig("Mg Ke")%></th>
                <th style="width: 15%;"><%=Common.getBahasaConfig(isNilaiCpmk ? "CPMK" : "Sub-CPMK")%></th>
                <th style="width: 20%;"><%=Common.getBahasaConfig("Penilaian")%></th>
                <th style="width: 25%;"><%=Common.getBahasaConfig("Bentuk, Metode & Penugasan")%></th>
                <th style="width: 15%;"><%=Common.getBahasaConfig("Materi Pembelajaran")%></th>
                <th style="width: 15%;"><%=Common.getBahasaConfig("Referensi")%></th>
                <% if(!disableForm) { %><th class="text-center" style="width: 80px;"><%=Common.getBahasaConfig("Aksi")%></th><% } %>
            </tr>
        </thead>
        <tbody>
            <% if(listUrut.isEmpty()) { %>
                <tr><td colspan="<%=!disableForm ? "7" : "6"%>" class="text-center py-5 text-muted fst-italic"><%=Common.getBahasaConfig("Belum ada rincian pertemuan yang ditambahkan.")%></td></tr>
            <% } else { 
                for(JSONObject obj : listUrut) {
                    String keyRow = obj.optString("keyData", "");
                    int mggMulai = obj.optInt("mulaiMingguKe", 0);
                    int mggSampai = obj.optInt("sampaiMingguKe", 0);
                    String strMinggu = (mggMulai == mggSampai) ? String.valueOf(mggMulai) : (mggMulai + " s.d " + mggSampai);

                    // Ambil CPMK/Sub-CPMK Description
                    int jmlCpmk = obj.optInt("jumlahCpmk", 1);
                    StringBuilder cpStr = new StringBuilder();
                    for(int i=1; i<=jmlCpmk; i++) {
                        String k = "sub_cpmk_des" + (i==1 ? "" : i);
                        String des = obj.optString(k, "");
                        if(!des.isEmpty()) cpStr.append("<span class='badge bg-primary mb-1 text-wrap text-start'>").append(des).append("</span><br>");
                    }

                    // Ambil Bahan Kajian
                    JSONObject bkObj = obj.optJSONObject("bahanKajians");
                    StringBuilder bkStr = new StringBuilder();
                    if(bkObj != null) {
                        Iterator<String> bkKeys = bkObj.keys();
                        while(bkKeys.hasNext()) {
                            JSONObject d = bkObj.optJSONObject(bkKeys.next());
                            if(d != null) bkStr.append("<i class='fas fa-check text-success me-1'></i>").append(d.optString("nama")).append("<br>");
                        }
                    }

                    // Ambil Pustaka
                    JSONObject puObj = obj.optJSONObject("pustakaUtamas");
                    JSONObject ppObj = obj.optJSONObject("pustakaPendukungs");
                    StringBuilder refStr = new StringBuilder();
                    if(puObj != null) { Iterator<String> pK = puObj.keys(); while(pK.hasNext()){ JSONObject d = puObj.optJSONObject(pK.next()); if(d!=null) refStr.append(d.optString("nama")).append(", "); } }
                    if(ppObj != null) { Iterator<String> pK = ppObj.keys(); while(pK.hasNext()){ JSONObject d = ppObj.optJSONObject(pK.next()); if(d!=null) refStr.append(d.optString("nama")).append(", "); } }
            %>
                    <tr>
                        <td class="text-center text-muted fw-bold bg-light"><%=strMinggu%></td>
                        <td><%=cpStr.length() > 0 ? cpStr.toString() : "-"%></td>
                        <td>
                            <strong><%=Common.getBahasaConfig("Indikator:")%></strong><br><%=obj.optString("indikator").replaceAll("\n", "<br>")%><br><br>
                            <strong><%=Common.getBahasaConfig("Kriteria:")%></strong><br><%=obj.optString("teknikDanKriteria").replaceAll("\n", "<br>")%>
                        </td>
                        <td>
                            <strong><%=Common.getBahasaConfig("Metode:")%></strong><br><%=obj.optString("metodePembelajaran").replaceAll("\n", "<br>")%><br><br>
                            <strong><%=Common.getBahasaConfig("Luring:")%></strong><br><%=obj.optString("pembelajaranLuring").replaceAll("\n", "<br>")%><br><br>
                            <strong><%=Common.getBahasaConfig("Daring:")%></strong><br><%=obj.optString("pembelajaranDaring").replaceAll("\n", "<br>")%>
                        </td>
                        <td><%=bkStr.length() > 0 ? bkStr.toString() : "-"%></td>
                        <td><%=refStr.length() > 0 ? refStr.toString() : "-"%></td>
                        <% if(!disableForm) { %>
                        <td class="text-center text-nowrap">
                            <button type="button" class="btn btn-sm btn-outline-primary rounded-circle shadow-sm me-1" onclick="window.bukaFormRincian<%=rnd%>('<%=keyRow%>')" title="<%=Common.getBahasaConfig("Edit")%>"><i class="fas fa-edit"></i></button>
                            <button type="button" class="btn btn-sm btn-outline-danger rounded-circle shadow-sm" onclick="window.hapusRincianList<%=rnd%>('<%=keyRow%>')" title="<%=Common.getBahasaConfig("Hapus")%>"><i class="fas fa-trash"></i></button>
                        </td>
                        <% } %>
                    </tr>
            <%  }
            } %>
        </tbody>
    </table>
</div>

<script>
    window.hapusRincianList<%=rnd%> = function(keyHapus) {
        showConfirmModal('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data ini?")%>', async function() {
            try {
                var fetchPayload = { action: "ambilDataRinci", class: "<%=KurikulumPunyaMatakuliah.class.getName()%>", id: "<%=kpm.getId()%>", tanpaLogin: "true" };
                var resFetch = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(fetchPayload) });
                var resultFetch = await resFetch.json();
                
                var currentJsonStr = resultFetch.data.rincian || "{}";
                var jsonMap = JSON.parse(currentJsonStr);
                
                // Menghapus data rincian secara logikal seperti ZK (membuang key utamanya)
                if(jsonMap[keyHapus]) {
                    delete jsonMap[keyHapus].mulaiMingguKe;
                    delete jsonMap[keyHapus].sampaiMingguKe;
                }

                var savePayload = { action: "simpanDataRinci", class: "<%=KurikulumPunyaMatakuliah.class.getName()%>", id: "<%=kpm.getId()%>", data: { rincian: JSON.stringify(jsonMap) }, tanpaLogin: "true" };
                var resSave = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(savePayload) });
                var resultSave = await resSave.json();
                
                if (resultSave.status === '00' || resultSave.status === 'success') {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Data rincian berhasil dihapus.")%>', 'bg-success text-white');
                    // Panggil fungsi reload di parent dengan cache buster
                    if(typeof window.reloadListRincian<%=rnd%> === 'function') window.reloadListRincian<%=rnd%>();
                } else {
                    if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menghapus rincian.")%>', 'bg-danger text-white');
                }
            } catch(e) {
                if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan.")%>', 'bg-danger text-white');
            }
        });
    };
</script>