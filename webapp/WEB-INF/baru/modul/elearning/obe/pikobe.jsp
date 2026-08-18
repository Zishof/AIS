<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Jurusan"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean edit = false;
boolean add = false;
boolean delete = false;
boolean isAdmin = false;

if(tbmuser != null && tbmuser.getUserId() != null){
    edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE, tbmuser);
    delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE, tbmuser);
    add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE, tbmuser);
    if(Common.getApakahAdminLain(tbmuser)){ edit = true; delete = true; add = true; isAdmin = true; }
}

Fakultas currentFakultas = tbmuser.ambilFakultas();
Jurusan currentJurusan = tbmuser.ambilJurusan();

Long idFakultasTerpilih = currentFakultas != null ? currentFakultas.getId() : null;
Long idJurusanTerpilih = currentJurusan != null ? currentJurusan.getId() : null;

if (idFakultasTerpilih == null && currentJurusan != null && currentJurusan.getFakultas() != null) {
    idFakultasTerpilih = currentJurusan.getFakultas().getId();
}

List<Fakultas> listFakultas = new ArrayList<Fakultas>();
List<Jurusan> listJurusan = new ArrayList<Jurusan>();
Session sess = null;

try {
    sess = HibernateUtil.openSession();
    listFakultas = ConstantValues.simpleList(sess.createCriteria(Fakultas.class).add(Restrictions.eq("aktif", true)).addOrder(Order.asc("nama")), Fakultas.class);
    listJurusan = ConstantValues.simpleList(sess.createCriteria(Jurusan.class).add(Restrictions.eq("aktif", true)).addOrder(Order.asc("nama")), Jurusan.class);
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/pikobe.jsp:50");
} finally {
    if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
}
%>

<style>
/* ROMBAK MOBILE PIKOBE: 14 tab jadi 1 baris yang bisa digeser (bukan menumpuk),
   padding lebih rapat, dan matriks tetap bisa digeser horizontal. */
@media (max-width: 767.98px) {
    #pikobeTabs_<%=rnd%> { flex-wrap: nowrap; overflow-x: auto; -webkit-overflow-scrolling: touch; scrollbar-width: none; padding-bottom: 2px; }
    #pikobeTabs_<%=rnd%>::-webkit-scrollbar { display: none; }
    #pikobeTabs_<%=rnd%> .nav-item { flex: 0 0 auto; }
    #pikobeTabs_<%=rnd%> .nav-link { white-space: nowrap; font-size: .78rem; padding: .4rem .6rem; }
    #wrapPikobe_<%=rnd%> .card-header.px-4 { padding-left: 1rem !important; padding-right: 1rem !important; }
    #wrapPikobe_<%=rnd%> .card-body.p-4 { padding: 1rem !important; }
    #wrapPikobe_<%=rnd%> .bg-light.p-3 { padding: .75rem !important; }
    #wrapPikobe_<%=rnd%> .btn { min-height: 40px; }
}
</style>
<div id="wrapPikobe_<%=rnd%>" class="card border-0 shadow-sm rounded-4 animate__animated animate__fadeIn mb-4">
    <div class="card-header bg-white border-bottom pt-4 pb-3 px-4">
        <h5 class="fw-bold text-primary mb-3"><i class="fas fa-chart-line me-2"></i><%=Common.getBahasaConfig("Pusat Indeks Kematangan OBE (PIKOBE)")%></h5>
        
        <div class="bg-light border rounded-3 p-3 mb-4 shadow-sm">
            <div class="row g-3">
                <div class="col-md-6">
                    <label class="form-label fw-bold small text-secondary"><i class="fas fa-university me-1"></i><%=Common.getBahasaConfig("Fakultas")%></label>
                    <select class="form-select border-primary shadow-sm bg-white" id="filterFakultas_<%=rnd%>" onchange="window.ubahFakultas<%=rnd%>()" <%= idFakultasTerpilih != null ? "disabled" : "" %>>
                        <option value="">-- <%=Common.getBahasaConfig("Pilih Fakultas")%> --</option>
                        <% for(Fakultas fak : listFakultas) { %>
                            <option value="<%=fak.getId()%>" <%= fak.getId().equals(idFakultasTerpilih) ? "selected" : "" %>><%=fak.getNama()%></option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-bold small text-secondary"><i class="fas fa-graduation-cap me-1"></i><%=Common.getBahasaConfig("Program Studi (Jurusan)")%></label>
                    <select class="form-select border-primary shadow-sm bg-white" id="filterJurusan_<%=rnd%>" onchange="window.muatUlangSemuaTabel<%=rnd%>()" <%= (idJurusanTerpilih != null || idFakultasTerpilih == null) ? "disabled" : "" %>>
                        <option value="">-- <%=Common.getBahasaConfig("Pilih Program Studi")%> --</option>
                    </select>
                </div>
            </div>
        </div>

        <ul class="nav nav-tabs nav-tabs-custom border-bottom-0" id="pikobeTabs_<%=rnd%>" role="tablist">
            <li class="nav-item" role="presentation"><button class="nav-link active fw-bold" data-bs-toggle="tab" data-bs-target="#tab0_<%=rnd%>" type="button" role="tab"><i class="fas fa-briefcase me-1"></i> <%=Common.getBahasaConfig("Profesi Lulusan")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab1_<%=rnd%>" type="button" role="tab"><i class="fas fa-user-tie me-1"></i> <%=Common.getBahasaConfig("Tabel 1")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab2_<%=rnd%>" type="button" role="tab"><i class="fas fa-graduation-cap me-1"></i> <%=Common.getBahasaConfig("Tabel 2")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab3_<%=rnd%>" type="button" role="tab"><i class="fas fa-th me-1"></i> <%=Common.getBahasaConfig("Tabel 3")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab4_<%=rnd%>" type="button" role="tab"><i class="fas fa-book-open me-1"></i> <%=Common.getBahasaConfig("Tabel 4")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab5_<%=rnd%>" type="button" role="tab"><i class="fas fa-project-diagram me-1"></i> <%=Common.getBahasaConfig("Tabel 5")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab6_<%=rnd%>" type="button" role="tab"><i class="fas fa-sitemap me-1"></i> <%=Common.getBahasaConfig("Tabel 6")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab7_<%=rnd%>" type="button" role="tab"><i class="fas fa-list-ol me-1"></i> <%=Common.getBahasaConfig("Tabel 7")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab8_<%=rnd%>" type="button" role="tab"><i class="fas fa-map me-1"></i> <%=Common.getBahasaConfig("Tabel 8")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab9_<%=rnd%>" type="button" role="tab"><i class="fas fa-network-wired me-1"></i> <%=Common.getBahasaConfig("Tabel 9")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab10_<%=rnd%>" type="button" role="tab"><i class="fas fa-sliders-h me-1"></i> <%=Common.getBahasaConfig("Tabel 10")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab11_<%=rnd%>" type="button" role="tab"><i class="fas fa-clipboard-check me-1"></i> <%=Common.getBahasaConfig("Tabel 11")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab12_<%=rnd%>" type="button" role="tab"><i class="fas fa-list-alt me-1"></i> <%=Common.getBahasaConfig("Tabel 12")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold" data-bs-toggle="tab" data-bs-target="#tab13_<%=rnd%>" type="button" role="tab"><i class="fas fa-calendar-alt me-1"></i> <%=Common.getBahasaConfig("Tabel 13")%></button></li>
            <li class="nav-item" role="presentation"><button class="nav-link fw-bold text-success" data-bs-toggle="tab" data-bs-target="#tabAsesmen_<%=rnd%>" type="button" role="tab"><i class="fas fa-file-invoice me-1"></i> <%=Common.getBahasaConfig("Asesmen")%></button></li>
        </ul>
    </div>
    
    <div class="card-body p-4">
        <div class="tab-content" id="pikobeTabsContent_<%=rnd%>">

            <!-- TABEL 0-12 -->
            <div class="tab-pane fade show active" id="tab0_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-table me-1"></i> <%=Common.getBahasaConfig("Profesi Lulusan")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;"><input type="text" id="pencarianTabel0_<%=rnd%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Cari...")%>" disabled><button type="button" class="btn btn-primary" onclick="window.loadTabel0<%=rnd%>()" id="btnCari0_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button></div><button type="button" class="btn btn-outline-secondary btn-sm" onclick="window.loadTabel0<%=rnd%>()" id="btnRefresh0_<%=rnd%>" disabled><i class="fas fa-sync-alt"></i></button><% if(add) { %><button type="button" class="btn btn-primary btn-sm" onclick="window.bukaModalProfesi<%=rnd%>('', '', '', '', true)" id="btnAdd0_<%=rnd%>" disabled><i class="fas fa-plus"></i></button><% } %></div></div><div id="wadahTabel0_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab1_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-table me-1"></i> <%=Common.getBahasaConfig("Tabel 1. Profil Lulusan")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;"><input type="text" id="pencarianTabel1_<%=rnd%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Cari...")%>" disabled><button type="button" class="btn btn-primary" onclick="window.loadTabel1<%=rnd%>()" id="btnCari1_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button></div><button type="button" class="btn btn-outline-secondary btn-sm" onclick="window.loadTabel1<%=rnd%>()" id="btnRefresh1_<%=rnd%>" disabled><i class="fas fa-sync-alt"></i></button><% if(add) { %><button type="button" class="btn btn-primary btn-sm" onclick="window.bukaModalPL<%=rnd%>('')" id="btnAdd1_<%=rnd%>" disabled><i class="fas fa-plus"></i></button><% } %></div></div><div id="wadahTabel1_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab2_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-table me-1"></i> <%=Common.getBahasaConfig("Tabel 2. Capaian Lulusan (CPL)")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;"><input type="text" id="pencarianTabel2_<%=rnd%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Cari...")%>" disabled><button type="button" class="btn btn-primary" onclick="window.loadTabel2<%=rnd%>()" id="btnCari2_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button></div><button type="button" class="btn btn-outline-secondary btn-sm" onclick="window.loadTabel2<%=rnd%>()" id="btnRefresh2_<%=rnd%>" disabled><i class="fas fa-sync-alt"></i></button><% if(add) { %><button type="button" class="btn btn-primary btn-sm" onclick="window.bukaModalCPL<%=rnd%>('')" id="btnAdd2_<%=rnd%>" disabled><i class="fas fa-plus"></i></button><% } %></div></div><div id="wadahTabel2_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab3_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-th me-1"></i> <%=Common.getBahasaConfig("Tabel 3. Matriks CPL vs PL")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><input type="text" id="pencarianTabel3Cpl_<%=rnd%>" class="form-control form-control-sm shadow-sm" placeholder="Cari CPL..." style="width:130px;" disabled><input type="text" id="pencarianTabel3Pl_<%=rnd%>" class="form-control form-control-sm shadow-sm" placeholder="Cari PL..." style="width:130px;" disabled><button type="button" class="btn btn-primary btn-sm fw-bold shadow-sm" onclick="window.loadTabel3<%=rnd%>()" id="btnCari3_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button><button type="button" class="btn btn-outline-secondary btn-sm fw-bold shadow-sm" onclick="window.loadTabel3<%=rnd%>()" id="btnRefresh3_<%=rnd%>" disabled><i class="fas fa-sync-alt"></i></button></div></div><div id="wadahTabel3_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab4_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-book-open me-1"></i> <%=Common.getBahasaConfig("Tabel 4. Bahan Kajian")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;"><input type="text" id="pencarianTabel4_<%=rnd%>" class="form-control" placeholder="<%=Common.getBahasaConfig("Cari...")%>" disabled><button type="button" class="btn btn-primary" onclick="window.loadTabel4<%=rnd%>()" id="btnCari4_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button></div><button type="button" class="btn btn-outline-secondary btn-sm" onclick="window.loadTabel4<%=rnd%>()" id="btnRefresh4_<%=rnd%>" disabled><i class="fas fa-sync-alt"></i></button><% if(add) { %><button type="button" class="btn btn-primary btn-sm" onclick="window.bukaModalBK<%=rnd%>('')" id="btnAdd4_<%=rnd%>" disabled><i class="fas fa-plus"></i></button><% } %></div></div><div id="wadahTabel4_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab5_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-project-diagram me-1"></i> <%=Common.getBahasaConfig("Tabel 5. BK vs MK")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><input type="text" id="pencarianTabel5Mk_<%=rnd%>" class="form-control form-control-sm shadow-sm" placeholder="Cari MK..." style="width:130px;" disabled><input type="text" id="pencarianTabel5Bk_<%=rnd%>" class="form-control form-control-sm shadow-sm" placeholder="Cari BK..." style="width:130px;" disabled><button type="button" class="btn btn-primary btn-sm fw-bold shadow-sm" onclick="window.loadTabel5<%=rnd%>()" id="btnCari5_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button><button type="button" class="btn btn-outline-secondary btn-sm fw-bold shadow-sm" onclick="window.loadTabel5<%=rnd%>()" id="btnRefresh5_<%=rnd%>" disabled><i class="fas fa-sync-alt"></i></button></div></div><div id="wadahTabel5_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab6_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-sitemap me-1"></i> <%=Common.getBahasaConfig("Tabel 6. CPL vs BK vs MK")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><button type="button" class="btn btn-outline-secondary btn-sm fw-bold shadow-sm" onclick="window.loadTabel6<%=rnd%>()" id="btnRefresh6_<%=rnd%>" disabled><i class="fas fa-sync-alt me-1"></i> <%=Common.getBahasaConfig("Refresh")%></button></div></div><div id="wadahTabel6_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab7_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-list-ol me-1"></i> <%=Common.getBahasaConfig("Tabel 7. Susunan Matakuliah")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;"><input type="text" id="pencarianTabel7_<%=rnd%>" class="form-control border-start-0 ps-0" placeholder="<%=Common.getBahasaConfig("Cari matakuliah...")%>" disabled><button type="button" class="btn btn-primary" onclick="window.loadTabel7<%=rnd%>()" id="btnCari7_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button></div><button type="button" class="btn btn-outline-secondary btn-sm" onclick="window.loadTabel7<%=rnd%>()" id="btnRefresh7_<%=rnd%>" disabled><i class="fas fa-sync-alt"></i></button></div></div><div id="wadahTabel7_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab8_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-map me-1"></i> <%=Common.getBahasaConfig("Tabel 8. Peta Pemenuhan CPL")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;"><span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span><input type="text" id="pencarianTabel8_<%=rnd%>" class="form-control border-start-0 ps-0" placeholder="<%=Common.getBahasaConfig("Cari CPL...")%>" disabled><button type="button" class="btn btn-primary fw-bold px-3" onclick="window.loadTabel8<%=rnd%>()" id="btnCari8_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button></div><button type="button" class="btn btn-outline-secondary rounded-pill btn-sm fw-bold px-3 shadow-sm" onclick="window.loadTabel8<%=rnd%>()" id="btnRefresh8_<%=rnd%>" disabled><i class="fas fa-sync-alt me-1"></i> <%=Common.getBahasaConfig("Refresh")%></button></div></div><div id="wadahTabel8_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab9_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-network-wired me-1"></i> <%=Common.getBahasaConfig("Tabel 9. Pemenuhan CPL - CPMK - MK")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><input type="text" id="pencarianTabel9Cpl_<%=rnd%>" class="form-control form-control-sm shadow-sm" placeholder="<%=Common.getBahasaConfig("Cari CPL...")%>" style="width: 130px;" disabled><button type="button" class="btn btn-primary btn-sm fw-bold shadow-sm" onclick="window.loadTabel9<%=rnd%>()" id="btnCari9_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button><button type="button" class="btn btn-outline-secondary btn-sm fw-bold shadow-sm" onclick="window.loadTabel9<%=rnd%>()" id="btnRefresh9_<%=rnd%>" disabled><i class="fas fa-sync-alt"></i></button></div></div><div id="wadahTabel9_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab10_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-sliders-h me-1"></i> <%=Common.getBahasaConfig("Tabel 10. Pemetaan Bobot CPMK / Sub-CPMK pada MK")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;"><span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span><input type="text" id="pencarianTabel10_<%=rnd%>" class="form-control border-start-0 ps-0" placeholder="<%=Common.getBahasaConfig("Cari Matakuliah/CPL/CPMK...")%>" disabled><button type="button" class="btn btn-primary fw-bold px-3" onclick="window.loadTabel10<%=rnd%>()" id="btnCari10_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button></div><button type="button" class="btn btn-outline-secondary rounded-pill btn-sm fw-bold px-3 shadow-sm" onclick="window.loadTabel10<%=rnd%>()" id="btnRefresh10_<%=rnd%>" disabled><i class="fas fa-sync-alt me-1"></i> <%=Common.getBahasaConfig("Refresh")%></button></div></div><div id="wadahTabel10_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab11_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-clipboard-check me-1"></i> <%=Common.getBahasaConfig("Tabel 11. Rekapitulasi Skor Maksimal / Bobot MK")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;"><span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span><input type="text" id="pencarianTabel11_<%=rnd%>" class="form-control border-start-0 ps-0" placeholder="<%=Common.getBahasaConfig("Cari Matakuliah...")%>" disabled><button type="button" class="btn btn-primary fw-bold px-3" onclick="window.loadTabel11<%=rnd%>()" id="btnCari11_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button></div><button type="button" class="btn btn-outline-secondary rounded-pill btn-sm fw-bold px-3 shadow-sm" onclick="window.loadTabel11<%=rnd%>()" id="btnRefresh11_<%=rnd%>" disabled><i class="fas fa-sync-alt me-1"></i> <%=Common.getBahasaConfig("Refresh")%></button></div></div><div id="wadahTabel11_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab12_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-list-alt me-1"></i> <%=Common.getBahasaConfig("Tabel 12. Rekapitulasi Skor Maksimal / Bobot CPL")%></h6></div><div class="d-flex align-items-center gap-2 flex-wrap"><div class="input-group input-group-sm shadow-sm" style="min-width:180px;flex:1 1 200px;"><span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-muted"></i></span><input type="text" id="pencarianTabel12_<%=rnd%>" class="form-control border-start-0 ps-0" placeholder="<%=Common.getBahasaConfig("Cari Matakuliah...")%>" disabled><button type="button" class="btn btn-primary fw-bold px-3" onclick="window.loadTabel12<%=rnd%>()" id="btnCari12_<%=rnd%>" disabled><%=Common.getBahasaConfig("Cari")%></button></div><button type="button" class="btn btn-outline-secondary rounded-pill btn-sm fw-bold px-3 shadow-sm" onclick="window.loadTabel12<%=rnd%>()" id="btnRefresh12_<%=rnd%>" disabled><i class="fas fa-sync-alt me-1"></i> <%=Common.getBahasaConfig("Refresh")%></button></div></div><div id="wadahTabel12_<%=rnd%>"></div></div>
            <div class="tab-pane fade" id="tab13_<%=rnd%>" role="tabpanel"><div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"><div><h6 class="fw-bold text-dark mb-0"><i class="fas fa-calendar-alt me-1"></i> <%=Common.getBahasaConfig("Tabel 13. Organisasi Matakuliah per Semester")%></h6><small class="text-muted"><%=Common.getBahasaConfig("Distribusi matakuliah dalam kurikulum OBE per semester, dikelompokkan berdasarkan kelompok MK.")%></small></div><div class="d-flex align-items-center gap-2 flex-wrap"><button type="button" class="btn btn-outline-secondary rounded-pill btn-sm fw-bold px-3 shadow-sm" onclick="window.loadTabel13<%=rnd%>()" id="btnRefresh13_<%=rnd%>" disabled><i class="fas fa-sync-alt me-1"></i> <%=Common.getBahasaConfig("Refresh")%></button></div></div><div id="wadahTabel13_<%=rnd%>"></div></div>

            <!-- ========================================== -->
            <!-- TAB ASESMEN                                -->
            <!-- ========================================== -->
            <div class="tab-pane fade" id="tabAsesmen_<%=rnd%>" role="tabpanel">
                <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
                    <div>
                        <h6 class="fw-bold text-dark mb-0"><i class="fas fa-file-invoice me-1"></i> <%=Common.getBahasaConfig("Asesmen")%></h6>
                        <small class="text-muted"><%=Common.getBahasaConfig("Pemantauan dan laporan asesmen nilai mahasiswa per Matakuliah.")%></small>
                    </div>
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <select id="selAsesmenKpm_<%=rnd%>" class="form-select form-select-sm shadow-sm" style="min-width: 200px;" onchange="window.loadAsesmen<%=rnd%>()" disabled>
                            <option value="">-- <%=Common.getBahasaConfig("Pilih Mata Kuliah")%> --</option>
                        </select>
                        <button type="button" class="btn btn-outline-secondary btn-sm fw-bold shadow-sm" onclick="window.loadAsesmen<%=rnd%>()" id="btnRefreshAsesmen_<%=rnd%>" disabled><i class="fas fa-sync-alt me-1"></i> <%=Common.getBahasaConfig("Refresh")%></button>
                    </div>
                </div>
                <div id="wadahAsesmen_<%=rnd%>"></div>
            </div>

        </div>
    </div>
</div>

<div id="wadahModalDinamic_<%=rnd%>"></div>

<!-- MODALS UNTUK TABEL 0, 1, 2, 4 (Disembunyikan agar script fokus) -->
<div class="modal fade" id="modalProfesi_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static"><div class="modal-dialog modal-dialog-centered"><div class="modal-content shadow-lg border-0 rounded-4"><div class="modal-header bg-primary text-white border-bottom-0 pb-3"><h6 class="modal-title fw-bold" id="modalTitleProfesi_<%=rnd%>"></h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button></div><div class="modal-body p-4 bg-light"><input type="hidden" id="inpIdProfesi_<%=rnd%>"><div class="mb-3"><label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Kode Profesi")%></label><input type="text" class="form-control" id="inpKodeProfesi_<%=rnd%>"></div><div class="mb-3"><label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Nama Profesi Lulusan")%> *</label><input type="text" class="form-control" id="inpNamaProfesi_<%=rnd%>"></div><div class="mb-3"><label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Keterangan")%></label><textarea class="form-control" id="inpKetProfesi_<%=rnd%>" rows="3"></textarea></div><div class="form-check form-switch mt-3"><input class="form-check-input" type="checkbox" id="inpAktifProfesi_<%=rnd%>" checked><label class="form-check-label fw-bold small text-dark" for="inpAktifProfesi_<%=rnd%>"><%=Common.getBahasaConfig("Status Aktif")%></label></div></div><div class="modal-footer bg-white border-top p-3 justify-content-end"><button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button><button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanProfesi<%=rnd%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%></button></div></div></div></div>
<div class="modal fade" id="modalPL_<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static"><div class="modal-dialog modal-dialog-centered"><div class="modal-content shadow-lg border-0 rounded-4"><div class="modal-header bg-primary text-white border-bottom-0 pb-3"><h6 class="modal-title fw-bold" id="modalTitlePL_<%=rnd%>"></h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button></div><div class="modal-body p-4 bg-light"><input type="hidden" id="inpIdPL_<%=rnd%>"><div class="mb-3"><label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Kode Profil Lulusan")%> *</label><input type="text" class="form-control" id="inpKodePL_<%=rnd%>"></div><div class="mb-3"><label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Deskripsi Profil Lulusan")%> *</label><textarea class="form-control" id="inpNamaPL_<%=rnd%>" rows="3"></textarea></div><div class="mb-3"><label class="form-label fw-bold small text-dark"><%=Common.getBahasaConfig("Profesi Terkait")%></label><select class="form-select" id="inpProfesiPL_<%=rnd%>"></select></div></div><div class="modal-footer bg-white border-top p-3 justify-content-end"><button type="button" class="btn btn-light border fw-bold rounded-pill px-4 shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button><button type="button" class="btn btn-primary fw-bold rounded-pill px-4 shadow-sm" onclick="window.simpanPL<%=rnd%>()"><i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data")%></button></div></div></div></div>

<script>
    var rootUrlPikobe<%=rnd%> = "<%=Common.ROOT%>";
    var classProfesi<%=rnd%> = "ais.database.model.obe.ProfesiLulusan";
    var classPL<%=rnd%> = "ais.database.model.obe.ProfilLulusan";
    var classCPL<%=rnd%> = "ais.database.model.obe.CapaianLulusan";
    var classBK<%=rnd%> = "ais.database.model.obe.BahanKajian";
    var classMK<%=rnd%> = "ais.database.model.Matakuliah";
    var classKPM<%=rnd%> = "ais.database.model.KurikulumPunyaMatakuliah";
    var classCPMK<%=rnd%> = "ais.database.model.obe.CapaianPembelajaranLulusan";

    var listDataJurusan<%=rnd%> = [
        <% for(int i=0; i<listJurusan.size(); i++){
            Jurusan j = listJurusan.get(i);
            Long idFak = null;
            try { idFak = j.getFakultas().getId(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/pikobe.jsp:173");}
            out.print("{ id: '" + j.getId() + "', nama: '" + j.getNama().replace("'", "\\'") + "', idFak: '" + idFak + "' }");
            if(i < listJurusan.size() - 1) out.print(",");
        } %>
    ];

    window.ubahFakultas<%=rnd%> = function(preselectJurusanId) {
        var idFakultas = document.getElementById('filterFakultas_<%=rnd%>').value;
        var selectJurusan = document.getElementById('filterJurusan_<%=rnd%>');
        selectJurusan.innerHTML = '<option value="">-- <%=Common.getBahasaConfig("Pilih Program Studi")%> --</option>';
        if(idFakultas === "") {
            selectJurusan.disabled = true;
        } else {
            var isLocked = <%= idJurusanTerpilih != null ? "true" : "false" %>;
            selectJurusan.disabled = isLocked;
            for(var i=0; i<listDataJurusan<%=rnd%>.length; i++) {
                var jur = listDataJurusan<%=rnd%>[i];
                if(String(jur.idFak) === String(idFakultas)) {
                    var opt = document.createElement('option');
                    opt.value = jur.id; opt.innerHTML = jur.nama;
                    if(preselectJurusanId && String(jur.id) === String(preselectJurusanId)) opt.selected = true;
                    selectJurusan.appendChild(opt);
                }
            }
        }
        window.muatUlangSemuaTabel<%=rnd%>();
    };

    window.setAtributTabel<%=rnd%> = function(isDisabled) {
        var els = [
            'btnAdd0_', 'btnRefresh0_', 'pencarianTabel0_', 'btnCari0_', 
            'btnAdd1_', 'btnRefresh1_', 'pencarianTabel1_', 'btnCari1_', 
            'btnAdd2_', 'btnRefresh2_', 'pencarianTabel2_', 'btnCari2_',
            'btnRefresh3_', 'pencarianTabel3Cpl_', 'pencarianTabel3Pl_', 'btnCari3_',
            'btnAdd4_', 'btnRefresh4_', 'pencarianTabel4_', 'btnCari4_',
            'btnRefresh5_', 'pencarianTabel5Mk_', 'pencarianTabel5Bk_', 'btnCari5_',
            'btnRefresh6_',
            'btnRefresh7_', 'pencarianTabel7_', 'btnCari7_',
            'btnRefresh8_', 'pencarianTabel8_', 'btnCari8_',
            'btnRefresh9_', 'pencarianTabel9Cpl_', 'btnCari9_',
            'btnRefresh10_', 'pencarianTabel10_', 'btnCari10_',
            'btnRefresh11_', 'pencarianTabel11_', 'btnCari11_',
            'btnRefresh12_', 'pencarianTabel12_', 'btnCari12_',
            'btnRefresh13_',
            'btnRefreshAsesmen_', 'selAsesmenKpm_'
        ];
        els.forEach(function(elId) {
            var el = document.getElementById(elId + '<%=rnd%>');
            if(el) el.disabled = isDisabled;
        });

        if(isDisabled) {
            var infoMsg = '<div class="alert alert-info text-center py-4 border-0 shadow-sm rounded-3"><i class="fas fa-info-circle fa-2x mb-2 d-block text-info"></i><span class="fw-bold"><%=Common.getBahasaConfig("Informasi")%></span><br><%=Common.getBahasaConfig("Silakan pilih Fakultas dan Program Studi terlebih dahulu.")%></div>';
            for(var i=0; i<=13; i++) {
                var wd = document.getElementById('wadahTabel' + i + '_<%=rnd%>');
                if(wd) wd.innerHTML = infoMsg;
            }
            var wdAsesmen = document.getElementById('wadahAsesmen_<%=rnd%>');
            if(wdAsesmen) wdAsesmen.innerHTML = infoMsg;
        }
    };

    window.muatUlangSemuaTabel<%=rnd%> = function() {
        var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value;
        if(idJurusan === "") {
            window.setAtributTabel<%=rnd%>(true);
        } else {
            window.setAtributTabel<%=rnd%>(false);
            window.loadTabel0<%=rnd%>();
            window.loadTabel1<%=rnd%>();
            window.loadTabel2<%=rnd%>();
            window.loadTabel3<%=rnd%>();
            window.loadTabel4<%=rnd%>();
            window.loadTabel5<%=rnd%>();
            window.loadTabel6<%=rnd%>();
            window.loadTabel7<%=rnd%>();
            window.loadTabel8<%=rnd%>();
            window.loadTabel9<%=rnd%>();
            window.loadTabel10<%=rnd%>();
            window.loadTabel11<%=rnd%>();
            window.loadTabel12<%=rnd%>();
            window.loadTabel13<%=rnd%>();
            window.loadOpsiAsesmenKpm<%=rnd%>();
        }
    };

    var fetchTabel = function(jsp, wadahId, action, kwId1, kwId2) {
        var idJur = document.getElementById('filterJurusan_<%=rnd%>').value;
        var kw1 = kwId1 && document.getElementById(kwId1) ? document.getElementById(kwId1).value.trim() : "";
        var kw2 = kwId2 && document.getElementById(kwId2) ? document.getElementById(kwId2).value.trim() : "";
        var wd = document.getElementById(wadahId);
        wd.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary"></div></div>';
        
        var qry = '&keyword=' + encodeURIComponent(kw1);
        if(kwId2) qry = '&keywordCpl=' + encodeURIComponent(kw1) + '&keywordPl=' + encodeURIComponent(kw2);
        if(action==='loadTabel5') qry = '&keywordMk=' + encodeURIComponent(kw1) + '&keywordBk=' + encodeURIComponent(kw2);
        if(action==='loadTabel9') qry = '&keywordCpl=' + encodeURIComponent(kw1);

        fetch(rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=' + jsp + '&action=' + action + '&idJurusan=' + idJur + qry + '&var=<%=rnd%>&t=' + new Date().getTime())
        .then(res => res.text()).then(html => { wd.innerHTML = html; }).catch(e => { wd.innerHTML = '<div class="alert alert-danger text-center">Gagal memuat.</div>'; });
    };

    window.loadTabel0<%=rnd%> = function() { fetchTabel('_pikobe_tabel_0', 'wadahTabel0_<%=rnd%>', 'loadTabel', 'pencarianTabel0_<%=rnd%>'); };
    window.loadTabel1<%=rnd%> = function() { fetchTabel('_pikobe_tabel_1', 'wadahTabel1_<%=rnd%>', 'loadTabel', 'pencarianTabel1_<%=rnd%>'); };
    window.loadTabel2<%=rnd%> = function() { fetchTabel('_pikobe_tabel_2', 'wadahTabel2_<%=rnd%>', 'loadTabel', 'pencarianTabel2_<%=rnd%>'); };
    window.loadTabel3<%=rnd%> = function() { fetchTabel('_pikobe_tabel_3', 'wadahTabel3_<%=rnd%>', 'loadTabel3', 'pencarianTabel3Cpl_<%=rnd%>', 'pencarianTabel3Pl_<%=rnd%>'); };
    window.loadTabel4<%=rnd%> = function() { fetchTabel('_pikobe_tabel_4', 'wadahTabel4_<%=rnd%>', 'loadTabel', 'pencarianTabel4_<%=rnd%>'); };
    window.loadTabel5<%=rnd%> = function() { fetchTabel('_pikobe_tabel_5', 'wadahTabel5_<%=rnd%>', 'loadTabel5', 'pencarianTabel5Mk_<%=rnd%>', 'pencarianTabel5Bk_<%=rnd%>'); };
    window.loadTabel6<%=rnd%> = function() { fetchTabel('_pikobe_tabel_6', 'wadahTabel6_<%=rnd%>', 'loadTabel6', null); };
    window.loadTabel7<%=rnd%> = function() { fetchTabel('_pikobe_tabel_7', 'wadahTabel7_<%=rnd%>', 'loadTabel7', 'pencarianTabel7_<%=rnd%>'); };
    window.loadTabel8<%=rnd%> = function() { fetchTabel('_pikobe_tabel_8', 'wadahTabel8_<%=rnd%>', 'loadTabel8', 'pencarianTabel8_<%=rnd%>'); };
    window.loadTabel9<%=rnd%> = function() { fetchTabel('_pikobe_tabel_9', 'wadahTabel9_<%=rnd%>', 'loadTabel9', 'pencarianTabel9Cpl_<%=rnd%>'); };
    window.loadTabel10<%=rnd%> = function() { fetchTabel('_pikobe_tabel_10', 'wadahTabel10_<%=rnd%>', 'loadTabel10', 'pencarianTabel10_<%=rnd%>'); };
    window.loadTabel11<%=rnd%> = function() { fetchTabel('_pikobe_tabel_11', 'wadahTabel11_<%=rnd%>', 'loadTabel11', 'pencarianTabel11_<%=rnd%>'); };
    window.loadTabel12<%=rnd%> = function() { fetchTabel('_pikobe_tabel_12', 'wadahTabel12_<%=rnd%>', 'loadTabel12', 'pencarianTabel12_<%=rnd%>'); };
    window.loadTabel13<%=rnd%> = function() { fetchTabel('_pikobe_tabel_13', 'wadahTabel13_<%=rnd%>', 'loadTabel13', null); };
    
    // LOADER OPSI MATA KULIAH (KPM) UNTUK TAB ASESMEN
    window.loadOpsiAsesmenKpm<%=rnd%> = function() {
        var idJur = document.getElementById('filterJurusan_<%=rnd%>').value;
        var sel = document.getElementById('selAsesmenKpm_<%=rnd%>');
        var wd = document.getElementById('wadahAsesmen_<%=rnd%>');
        if (!sel) return;
        if (idJur === "") { sel.innerHTML = '<option value="">-- <%=Common.getBahasaConfig("Pilih Mata Kuliah")%> --</option>'; return; }
        sel.innerHTML = '<option value="">-- <%=Common.getBahasaConfig("Memuat...")%> --</option>';
        var url = rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_service&action=loadOpsiKpm&idJurusan=' + idJur + '&var=<%=rnd%>&t=' + new Date().getTime();
        fetch(url).then(res => res.text()).then(html => {
            sel.innerHTML = html;
            if (wd) wd.innerHTML = '<div class="alert alert-info text-center py-4 border-0 shadow-sm rounded-3"><i class="fas fa-hand-pointer fa-2x mb-2 d-block text-info"></i><%=Common.getBahasaConfig("Silakan pilih Mata Kuliah untuk menampilkan laporan asesmen.")%></div>';
        }).catch(function(e){ sel.innerHTML = '<option value="">-- <%=Common.getBahasaConfig("Gagal memuat")%> --</option>'; });
    };

    // LOADER UNTUK TAB ASESMEN (butuh KurikulumPunyaMatakuliah/MK yang dipilih)
    window.loadAsesmen<%=rnd%> = function() {
        var sel = document.getElementById('selAsesmenKpm_<%=rnd%>');
        var idKpm = sel ? sel.value : "";
        var wd = document.getElementById('wadahAsesmen_<%=rnd%>');
        if (!wd) return;
        if (!idKpm) {
            wd.innerHTML = '<div class="alert alert-info text-center py-4 border-0 shadow-sm rounded-3"><i class="fas fa-hand-pointer fa-2x mb-2 d-block text-info"></i><%=Common.getBahasaConfig("Silakan pilih Mata Kuliah untuk menampilkan laporan asesmen.")%></div>';
            return;
        }
        wd.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary"></div></div>';
        var url = rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_asesmen&kurikulumPunyaMatakuliah=' + idKpm + '&var=<%=rnd%>&t=' + new Date().getTime();
        fetch(url).then(res => res.text()).then(html => { wd.innerHTML = html; }).catch(e => { wd.innerHTML = '<div class="alert alert-danger text-center"><%=Common.getBahasaConfig("Gagal memuat Asesmen.")%></div>'; });
    };

    // FUNGSI MODAL & SIMPAN TABEL
    window.bukaModalProfesi<%=rnd%> = function(id, kode, nama, ket, aktif) { document.getElementById('inpIdProfesi_<%=rnd%>').value = id; document.getElementById('inpKodeProfesi_<%=rnd%>').value = kode; document.getElementById('inpNamaProfesi_<%=rnd%>').value = nama; document.getElementById('inpKetProfesi_<%=rnd%>').value = ket; document.getElementById('inpAktifProfesi_<%=rnd%>').checked = aktif; document.getElementById('modalTitleProfesi_<%=rnd%>').innerHTML = id === '' ? '<i class="fas fa-briefcase me-2"></i><%=Common.getBahasaConfig("Tambah Profesi")%>' : '<i class="fas fa-edit me-2"></i><%=Common.getBahasaConfig("Ubah Profesi")%>'; var myModal = new bootstrap.Modal(document.getElementById('modalProfesi_<%=rnd%>')); myModal.show(); };
    window.simpanProfesi<%=rnd%> = async function() { var idPrf = document.getElementById('inpIdProfesi_<%=rnd%>') ? document.getElementById('inpIdProfesi_<%=rnd%>').value : ""; var dataObj = { kode: document.getElementById('inpKodeProfesi_<%=rnd%>') ? document.getElementById('inpKodeProfesi_<%=rnd%>').value.trim() : null, nama: document.getElementById('inpNamaProfesi_<%=rnd%>') ? document.getElementById('inpNamaProfesi_<%=rnd%>').value.trim() : null, keterangan: document.getElementById('inpKetProfesi_<%=rnd%>') ? document.getElementById('inpKetProfesi_<%=rnd%>').value.trim() : null, aktif: document.getElementById('inpAktifProfesi_<%=rnd%>') ? document.getElementById('inpAktifProfesi_<%=rnd%>').checked : true, jurusan: document.getElementById('filterJurusan_<%=rnd%>') ? document.getElementById('filterJurusan_<%=rnd%>').value : null }; if(!dataObj.nama) { if(typeof tampilkanToast === 'function') tampilkanToast('Nama Profesi wajib diisi.', 'bg-warning text-dark'); return; } var payload = { action: "simpanDataRinci", log: "true", class: classProfesi<%=rnd%>, data: dataObj, tanpaLogin: "true" }; if(idPrf !== "") payload.id = idPrf; try { var res = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }); var result = await res.json(); if (result.status === '00' || result.status === 'success') { if(typeof tampilkanToast === 'function') tampilkanToast('Berhasil disimpan.', 'bg-success text-white'); bootstrap.Modal.getInstance(document.getElementById('modalProfesi_<%=rnd%>')).hide(); setTimeout(function(){ window.loadTabel0<%=rnd%>(); window.loadOpsiProfesi<%=rnd%>(); }, 350); } else { if(typeof tampilkanToast === 'function') tampilkanToast('Gagal.', 'bg-danger text-white'); } } catch (e) {} };
    window.hapusProfesi<%=rnd%> = function(idHapus) { if (typeof prosesDeleteData === 'function') prosesDeleteData(classProfesi<%=rnd%>, idHapus, function() { window.loadTabel0<%=rnd%>(); window.loadOpsiProfesi<%=rnd%>(); }); };

    window.loadOpsiProfesi<%=rnd%> = function() { var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value; var url = rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_service&action=loadOpsiProfesi&idJurusan=' + idJurusan + '&var=<%=rnd%>&t=' + new Date().getTime(); fetch(url).then(res => res.text()).then(html => { document.getElementById('inpProfesiPL_<%=rnd%>').innerHTML = html; }); };
    window.bukaModalPL<%=rnd%> = function(idData) { var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value; var wadahModal = document.getElementById('wadahModalDinamic_<%=rnd%>'); wadahModal.innerHTML = '<div class="text-center p-3"><div class="spinner-border text-primary"></div></div>'; fetch(rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_tabel_1&action=loadForm&idJurusan=' + idJurusan + '&id=' + idData + '&var=<%=rnd%>&t=' + new Date().getTime()).then(res => res.text()).then(html => { wadahModal.innerHTML = html; var scripts = wadahModal.querySelectorAll('script'); scripts.forEach(s => { var ns = document.createElement('script'); if(s.src) ns.src = s.src; else ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); }); }); };
    window.hapusPL<%=rnd%> = function(idHapus) { if (typeof prosesDeleteData === 'function') prosesDeleteData(classPL<%=rnd%>, idHapus, function() { window.loadTabel1<%=rnd%>(); window.loadTabel3<%=rnd%>(); }); };

    window.bukaModalCPL<%=rnd%> = function(idData) { var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value; var wadahModal = document.getElementById('wadahModalDinamic_<%=rnd%>'); wadahModal.innerHTML = '<div class="text-center p-3"><div class="spinner-border text-primary"></div></div>'; fetch(rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_tabel_2&action=loadForm&idJurusan=' + idJurusan + '&id=' + idData + '&var=<%=rnd%>&t=' + new Date().getTime()).then(res => res.text()).then(html => { wadahModal.innerHTML = html; var scripts = wadahModal.querySelectorAll('script'); scripts.forEach(s => { var ns = document.createElement('script'); if(s.src) ns.src = s.src; else ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); }); }); };
    window.hapusCPL<%=rnd%> = function(idHapus) { if (typeof prosesDeleteData === 'function') prosesDeleteData(classCPL<%=rnd%>, idHapus, function() { window.loadTabel2<%=rnd%>(); window.loadTabel3<%=rnd%>(); window.loadTabel6<%=rnd%>(); window.loadTabel8<%=rnd%>(); window.loadTabel9<%=rnd%>(); window.loadTabel10<%=rnd%>(); window.loadTabel11<%=rnd%>(); window.loadTabel12<%=rnd%>(); }); };

    window.bukaModalBK<%=rnd%> = function(idData) { var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value; var wadahModal = document.getElementById('wadahModalDinamic_<%=rnd%>'); wadahModal.innerHTML = '<div class="text-center p-3"><div class="spinner-border text-primary"></div></div>'; fetch(rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_tabel_4&action=loadForm&idJurusan=' + idJurusan + '&id=' + idData + '&var=<%=rnd%>&t=' + new Date().getTime()).then(res => res.text()).then(html => { wadahModal.innerHTML = html; var scripts = wadahModal.querySelectorAll('script'); scripts.forEach(s => { var ns = document.createElement('script'); if(s.src) ns.src = s.src; else ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); }); }); };
    window.hapusBK<%=rnd%> = function(idHapus) { if (typeof prosesDeleteData === 'function') prosesDeleteData(classBK<%=rnd%>, idHapus, function() { window.loadTabel4<%=rnd%>(); window.loadTabel5<%=rnd%>(); window.loadTabel6<%=rnd%>(); }); };

    window.updateKorelasiCPLPL<%=rnd%> = async function(chk, idCpl, idPl) { chk.disabled = true; try { var f = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "load", class: classCPL<%=rnd%>, id: idCpl, tanpaLogin: "true"}) }); var r = await f.json(); if(r.status === '00' || r.status === 'success') { var eArr = (r.data.profil || "").split(",").filter(x => x.trim() !== ""); if(chk.checked) { if(eArr.indexOf(String(idPl)) === -1) eArr.push(String(idPl)); } else { eArr = eArr.filter(x => x !== String(idPl)); } var p = eArr.length > 0 ? "," + eArr.join(",") + "," : ""; await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "simpanDataRinci", class: classCPL<%=rnd%>, id: idCpl, data: { profil: p }, tanpaLogin: "true"}) }); } } catch(e) {} finally { chk.disabled = false; window.loadTabel2<%=rnd%>(); window.loadTabel3<%=rnd%>(); window.loadTabel8<%=rnd%>(); window.loadTabel9<%=rnd%>(); } };
    window.updateKorelasiMKBK<%=rnd%> = async function(chk, idMk, idBk) { chk.disabled = true; try { var f = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "load", class: classMK<%=rnd%>, id: idMk, tanpaLogin: "true"}) }); var r = await f.json(); if(r.status === '00' || r.status === 'success') { var eArr = (r.data.bahanKajian || "").split(",").filter(x => x.trim() !== ""); if(chk.checked) { if(eArr.indexOf(String(idBk)) === -1) eArr.push(String(idBk)); } else { eArr = eArr.filter(x => x !== String(idBk)); } var p = eArr.length > 0 ? "," + eArr.join(",") + "," : ""; await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "simpanDataRinci", class: classMK<%=rnd%>, id: idMk, data: { bahanKajian: p }, tanpaLogin: "true"}) }); } } catch(e) {} finally { chk.disabled = false; window.loadTabel5<%=rnd%>(); window.loadTabel6<%=rnd%>(); } };

    window.bukaModalTambahMK7<%=rnd%> = function(idKurikulum) { var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value; var wadahModal = document.getElementById('wadahModalDinamic_<%=rnd%>'); wadahModal.innerHTML = '<div class="text-center p-3"><div class="spinner-border text-primary"></div></div>'; fetch(rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_tabel_7&action=loadModalTambahMK&idJurusan=' + idJurusan + '&idKurikulum=' + idKurikulum + '&var=<%=rnd%>&t=' + new Date().getTime()).then(res => res.text()).then(html => { wadahModal.innerHTML = html; var scripts = wadahModal.querySelectorAll('script'); scripts.forEach(s => { var ns = document.createElement('script'); if(s.src) ns.src = s.src; else ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); }); }); };
    window.simpanMatakuliahBaru_7<%=rnd%> = async function(idKurikulum) { var cks = document.querySelectorAll('.chk-mkbaru-7'); var requests = []; for (var i = 0; i < cks.length; i++) { var chk = cks[i]; if(chk.checked) { requests.push(fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: classKPM<%=rnd%>, data: { matakuliah: chk.value, kurikulum: idKurikulum, semester: 1, aktif: true }, tanpaLogin: "true" }) })); } } if(requests.length > 0) { await Promise.all(requests); bootstrap.Modal.getInstance(document.getElementById('modalTambahMK_7_<%=rnd%>')).hide(); window.loadTabel7<%=rnd%>(); window.loadTabel8<%=rnd%>(); window.loadTabel9<%=rnd%>(); window.loadTabel10<%=rnd%>(); window.loadTabel11<%=rnd%>(); window.loadTabel12<%=rnd%>(); } };
    window.updateSemesterKPM<%=rnd%> = async function(radioEl, idKpm) { try { await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ action: "simpanDataRinci", log: "true", class: classKPM<%=rnd%>, id: idKpm, data: { semester: parseInt(radioEl.value) }, tanpaLogin: "true" }) }); window.loadTabel8<%=rnd%>(); window.loadTabel9<%=rnd%>(); window.loadTabel10<%=rnd%>(); window.loadTabel11<%=rnd%>(); window.loadTabel12<%=rnd%>(); } catch(e) {} };
    window.hapusKPM7<%=rnd%> = async function(idKpm) { if(typeof showConfirmModal === 'function') { showConfirmModal('<%=Common.getBahasaConfigJS("Keluarkan matakuliah ini dari kurikulum?")%>', async function() { if (typeof prosesDeleteData === 'function') { prosesDeleteData(classKPM<%=rnd%>, idKpm, function() { window.loadTabel7<%=rnd%>(); window.loadTabel8<%=rnd%>(); window.loadTabel9<%=rnd%>(); window.loadTabel10<%=rnd%>(); window.loadTabel11<%=rnd%>(); window.loadTabel12<%=rnd%>(); }); } }); } };

    window.bukaModalPilihMK_6<%=rnd%> = function(titleRelasi, kodeKorelasi) { var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value; var wadahModal = document.getElementById('wadahModalDinamic_<%=rnd%>'); wadahModal.innerHTML = '<div class="text-center p-3"><div class="spinner-border text-primary"></div></div>'; fetch(rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_tabel_6&action=loadModalMK&idJurusan=' + idJurusan + '&kodeKorelasi=' + encodeURIComponent(kodeKorelasi) + '&titleRelasi=' + encodeURIComponent(titleRelasi) + '&var=<%=rnd%>&t=' + new Date().getTime()).then(res => res.text()).then(html => { wadahModal.innerHTML = html; var scripts = wadahModal.querySelectorAll('script'); scripts.forEach(s => { var ns = document.createElement('script'); if(s.src) ns.src = s.src; else ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); }); }); };
    window.simpanKorelasiMK_6<%=rnd%> = async function(kodeKorelasi) { var cks = document.querySelectorAll('.chk-mk-6'); var requests = []; for (var i = 0; i < cks.length; i++) { var chk = cks[i]; var idMk = chk.value; var isChecked = chk.checked; var req = (async function(id, checked) { var f = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "load", class: classMK<%=rnd%>, id: id, tanpaLogin: "true"}) }); var r = await f.json(); if(r.status === '00' || r.status === 'success') { var mkData = r.data; var cplArr = (mkData.capaianLulusan || "").split(",").filter(x => x.trim() !== ""); var bkArr = (mkData.bahanKajian || "").split(",").filter(x => x.trim() !== ""); var changed = false; if(checked) { if(cplArr.indexOf(String(kodeKorelasi)) === -1) { cplArr.push(String(kodeKorelasi)); changed = true; } if(bkArr.indexOf(String(kodeKorelasi)) === -1) { bkArr.push(String(kodeKorelasi)); changed = true; } } else { if(cplArr.indexOf(String(kodeKorelasi)) > -1) { cplArr = cplArr.filter(x => x !== String(kodeKorelasi)); changed = true; } if(bkArr.indexOf(String(kodeKorelasi)) > -1) { bkArr = bkArr.filter(x => x !== String(kodeKorelasi)); changed = true; } } if(changed) { var pCpl = cplArr.length > 0 ? "," + cplArr.join(",") + "," : ""; var pBk = bkArr.length > 0 ? "," + bkArr.join(",") + "," : ""; await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "simpanDataRinci", class: classMK<%=rnd%>, id: id, data: { capaianLulusan: pCpl, bahanKajian: pBk }, tanpaLogin: "true"}) }); } } })(idMk, isChecked); requests.push(req); } await Promise.all(requests); bootstrap.Modal.getInstance(document.getElementById('modalPilihMK_6_<%=rnd%>')).hide(); window.loadTabel6<%=rnd%>(); window.loadTabel8<%=rnd%>(); };
    window.hapusKorelasiMK_6<%=rnd%> = async function(idMk, kodeKorelasi) { if(confirm('<%=Common.getBahasaConfigJS("Hapus Matakuliah dari irisan ini?")%>')) { var parts = kodeKorelasi.split("|"); var idCpl = parts[0]; var idCpmk = parts[1]; var f = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "load", class: classMK<%=rnd%>, id: idMk, tanpaLogin: "true"}) }); var r = await f.json(); if(r.status === '00' || r.status === 'success') { var cplArr = (r.data.capaianLulusan || "").split(",").filter(x => x.trim() !== "").filter(x => x !== String(kodeKorelasi)); var bkArr = (r.data.bahanKajian || "").split(",").filter(x => x.trim() !== "").filter(x => x !== String(kodeKorelasi)); var pCpl = cplArr.length > 0 ? "," + cplArr.join(",") + "," : ""; var pBk = bkArr.length > 0 ? "," + bkArr.join(",") + "," : ""; await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "simpanDataRinci", class: classMK<%=rnd%>, id: idMk, data: { capaianLulusan: pCpl, bahanKajian: pBk }, tanpaLogin: "true"}) }); window.loadTabel6<%=rnd%>(); window.loadTabel8<%=rnd%>(); } } };

    window.bukaModalKelolaCPMK9<%=rnd%> = function(idCpl, kodeCpl, idMk) { var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value; var wadahModal = document.getElementById('wadahModalDinamic_<%=rnd%>'); wadahModal.innerHTML = '<div class="text-center p-3"><div class="spinner-border text-primary"></div></div>'; fetch(rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_tabel_9&action=loadModalKelolaCpmk&idJurusan=' + idJurusan + '&idCpl=' + idCpl + '&kodeCpl=' + encodeURIComponent(kodeCpl) + '&idMk=' + (idMk || '') + '&var=<%=rnd%>&t=' + new Date().getTime()).then(res => res.text()).then(html => { wadahModal.innerHTML = html; var scripts = wadahModal.querySelectorAll('script'); scripts.forEach(s => { var ns = document.createElement('script'); if(s.src) ns.src = s.src; else ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); }); }); };
    window.simpanKorelasiCPL_CPMK9<%=rnd%> = async function(idCpl, idMk) { var cks = document.querySelectorAll('.chk-cpmk-9'); var ids = []; cks.forEach(function(el) { if(el.checked) ids.push(el.value); }); var p = ids.length > 0 ? "," + ids.join(",") + "," : ""; try { await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "simpanDataRinci", class: classCPL<%=rnd%>, id: idCpl, data: { capaianPembelajaranLulusan: p }, tanpaLogin: "true"}) }); if(idMk && idMk !== "") { var f = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "load", class: classMK<%=rnd%>, id: idMk, tanpaLogin: "true"}) }); var r = await f.json(); if(r.status === '00' || r.status === 'success') { var mkData = r.data; var mkCpmkArr = (mkData.capaianPembelajaranLulusan || "").split(",").filter(x => x.trim() !== ""); var changed = false; ids.forEach(function(newId) { if(!mkCpmkArr.includes(String(newId))) { mkCpmkArr.push(String(newId)); changed = true; } }); if(changed) { var pCpmk = mkCpmkArr.length > 0 ? "," + mkCpmkArr.join(",") + "," : ""; await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "simpanDataRinci", class: classMK<%=rnd%>, id: idMk, data: { capaianPembelajaranLulusan: pCpmk }, tanpaLogin: "true"}) }); } } } bootstrap.Modal.getInstance(document.getElementById('modalKelolaCpmk_9_<%=rnd%>')).hide(); window.loadTabel9<%=rnd%>(); window.loadTabel10<%=rnd%>(); window.loadTabel11<%=rnd%>(); window.loadTabel12<%=rnd%>(); } catch(e) {} };
    window.bukaModalPilihMK_9<%=rnd%> = function(idCpl, idCpmk, labelRelasi, smt, idKurikulum) { var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value; var wadahModal = document.getElementById('wadahModalDinamic_<%=rnd%>'); wadahModal.innerHTML = '<div class="text-center p-3"><div class="spinner-border text-primary"></div></div>'; fetch(rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_tabel_9&action=loadModalMK&idJurusan=' + idJurusan + '&idCpl=' + idCpl + '&idCpmk=' + idCpmk + '&labelRelasi=' + encodeURIComponent(labelRelasi) + '&semester=' + smt + '&idKurikulum=' + idKurikulum + '&var=<%=rnd%>&t=' + new Date().getTime()).then(res => res.text()).then(html => { wadahModal.innerHTML = html; var scripts = wadahModal.querySelectorAll('script'); scripts.forEach(s => { var ns = document.createElement('script'); if(s.src) ns.src = s.src; else ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); }); }); };
    window.simpanKorelasiMK_9<%=rnd%> = async function(idCpl, idCpmk) { var cks = document.querySelectorAll('.chk-mk-9'); var requests = []; for (var i = 0; i < cks.length; i++) { var idMk = cks[i].value; var isChecked = cks[i].checked; var req = (async function(id, checked) { var f = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "load", class: classMK<%=rnd%>, id: id, tanpaLogin: "true"}) }); var r = await f.json(); if(r.status === '00' || r.status === 'success') { var mkData = r.data; var cplArr = (mkData.capaianLulusan || "").split(",").filter(x => x.trim() !== ""); var cpmkArr = (mkData.capaianPembelajaranLulusan || "").split(",").filter(x => x.trim() !== ""); var changed = false; if(checked) { if(!cplArr.includes(String(idCpl))) { cplArr.push(String(idCpl)); changed = true; } if(!cpmkArr.includes(String(idCpmk))) { cpmkArr.push(String(idCpmk)); changed = true; } } else { if(cpmkArr.includes(String(idCpmk))) { cpmkArr = cpmkArr.filter(x => x !== String(idCpmk)); changed = true; } if(cplArr.includes(String(idCpl))) { cplArr = cplArr.filter(x => x !== String(idCpl)); changed = true; } } if(changed) { var pCpl = cplArr.length > 0 ? "," + cplArr.join(",") + "," : ""; var pCpmk = cpmkArr.length > 0 ? "," + cpmkArr.join(",") + "," : ""; await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "simpanDataRinci", class: classMK<%=rnd%>, id: id, data: { capaianLulusan: pCpl, capaianPembelajaranLulusan: pCpmk }, tanpaLogin: "true"}) }); } } })(idMk, isChecked); requests.push(req); } if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Menyimpan...")%>', 'bg-info text-white'); await Promise.all(requests); if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Korelasi berhasil diterapkan.")%>', 'bg-success text-white'); bootstrap.Modal.getInstance(document.getElementById('modalPilihMK_9_<%=rnd%>')).hide(); window.loadTabel9<%=rnd%>(); window.loadTabel10<%=rnd%>(); window.loadTabel11<%=rnd%>(); window.loadTabel12<%=rnd%>(); };
    window.hapusKorelasiMK_9<%=rnd%> = async function(idMk, kodeKorelasi) { if(confirm('<%=Common.getBahasaConfigJS("Hapus Matakuliah dari irisan ini?")%>')) { var parts = kodeKorelasi.split("|"); var idCpl = parts[0]; var idCpmk = parts[1]; var f = await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "load", class: classMK<%=rnd%>, id: idMk, tanpaLogin: "true"}) }); var r = await f.json(); if(r.status === '00' || r.status === 'success') { var cplArr = (r.data.capaianLulusan || "").split(",").filter(x => x.trim() !== "").filter(x => x !== idCpl); var cpmkArr = (r.data.capaianPembelajaranLulusan || "").split(",").filter(x => x.trim() !== "").filter(x => x !== idCpmk); var pCpl = cplArr.length > 0 ? "," + cplArr.join(",") + "," : ""; var pCpmk = cpmkArr.length > 0 ? "," + cpmkArr.join(",") + "," : ""; await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "simpanDataRinci", class: classMK<%=rnd%>, id: idMk, data: { capaianLulusan: pCpl, capaianPembelajaranLulusan: pCpmk }, tanpaLogin: "true"}) }); window.loadTabel9<%=rnd%>(); window.loadTabel8<%=rnd%>(); window.loadTabel6<%=rnd%>(); window.loadTabel10<%=rnd%>(); window.loadTabel11<%=rnd%>(); window.loadTabel12<%=rnd%>(); } } };

    // ================== TABEL 10 (BOBOT CPMK/SUB-CPMK) ==================
    window.bukaModalKelolaCPL10<%=rnd%> = function(idMk, kodeMk) { var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value; var wadahModal = document.getElementById('wadahModalDinamic_<%=rnd%>'); wadahModal.innerHTML = '<div class="text-center p-3"><div class="spinner-border text-primary"></div></div>'; fetch(rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_tabel_10&action=loadModalKelolaCpl&idJurusan=' + idJurusan + '&idMk=' + idMk + '&kodeMk=' + encodeURIComponent(kodeMk) + '&var=<%=rnd%>&t=' + new Date().getTime()).then(res => res.text()).then(html => { wadahModal.innerHTML = html; var scripts = wadahModal.querySelectorAll('script'); scripts.forEach(s => { var ns = document.createElement('script'); if(s.src) ns.src = s.src; else ns.textContent = s.textContent; document.body.appendChild(ns); document.body.removeChild(ns); }); }); };
    window.simpanKorelasiMK_CPL10<%=rnd%> = async function(idMk) { var cks = document.querySelectorAll('.chk-cpl-10'); var ids = []; cks.forEach(function(el) { if(el.checked) ids.push(el.value); }); var p = ids.length > 0 ? "," + ids.join(",") + "," : ""; try { await fetch('<%=Common.ROOT%>/Data', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({action: "simpanDataRinci", class: classMK<%=rnd%>, id: idMk, data: { capaianLulusan: p }, tanpaLogin: "true"}) }); bootstrap.Modal.getInstance(document.getElementById('modalKelolaCpl_10_<%=rnd%>')).hide(); window.loadTabel10<%=rnd%>(); window.loadTabel9<%=rnd%>(); window.loadTabel8<%=rnd%>(); window.loadTabel11<%=rnd%>(); window.loadTabel12<%=rnd%>(); } catch(e) {} };
    
    window.kalkulasiBobot10<%=rnd%> = function(idMk) {
        var tbody = document.getElementById('tbodyTabel10_<%=rnd%>');
        if(tbody) {
            var totalBobot = 0;
            var rowsMk = tbody.querySelectorAll('tr[data-mk="' + idMk + '"]');
            rowsMk.forEach(function(row) {
                var bobotInput = row.querySelector('input[onchange*="\'bobot\'"]');
                if(bobotInput) {
                    totalBobot += parseFloat(bobotInput.value) || 0;
                }
            });

            totalBobot = Math.round(totalBobot * 100) / 100;
            var wdh = document.getElementById('wdhBobotMk_' + idMk);
            if(wdh) {
                var cls = totalBobot === 100 ? "text-success fw-bold" : "text-danger fw-bold";
                var alt = totalBobot === 100 ? "" : "<br><small class='text-danger fw-bold' style='font-size:11px;'><i class='fas fa-exclamation-triangle'></i> Total Bobot Harus 100%</small>";
                wdh.innerHTML = '<span class="' + cls + '">Total Bobot: ' + totalBobot + '%</span>' + alt;
            }
        }
    };

    window.updateNilai10<%=rnd%> = async function(inputEl, fieldName, idCpmk, jsonKey, isCpmkBase, idMk) {
        if (!idCpmk || idCpmk === "") {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal! CPMK/Sub-CPMK belum ditautkan pada matakuliah ini.")%>', 'bg-danger text-white');
            inputEl.value = 0; return;
        }
        if (!isCpmkBase && (!jsonKey || jsonKey === "")) {
            if(typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal! Sub-CPMK belum didefinisikan dalam JSON formula.")%>', 'bg-danger text-white');
            inputEl.value = 0; return;
        }

        var val = parseFloat(inputEl.value) || 0.0;
        var idJurusan = document.getElementById('filterJurusan_<%=rnd%>').value;
        inputEl.style.backgroundColor = "#e8f0fe";
        
        try {
            var url = rootUrlPikobe<%=rnd%> + '/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pikobe_tabel_10&action=updateNilai&idJurusan=' + idJurusan;
            var formData = new FormData();
            formData.append('idCpmk', idCpmk);
            formData.append('isCpmkBase', isCpmkBase);
            formData.append('jsonKey', jsonKey);
            formData.append('field', fieldName);
            formData.append('value', val);
            
            var resFetch = await fetch(url, { method: 'POST', body: new URLSearchParams(formData) });
            var result = await resFetch.json();
            
            if(result.status === 'success') {
                inputEl.style.backgroundColor = "#d4edda";
                setTimeout(function(){ inputEl.style.backgroundColor = ""; }, 1000);
                if (fieldName === 'bobot') { window.kalkulasiBobot10<%=rnd%>(idMk); window.loadTabel11<%=rnd%>(); window.loadTabel12<%=rnd%>(); }
            } else {
                inputEl.style.backgroundColor = "#f8d7da";
                if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Gagal menyimpan nilai")%>', 'bg-danger text-white');
            }
        } catch(e) {
            inputEl.style.backgroundColor = "#f8d7da";
            if (typeof tampilkanToast === 'function') tampilkanToast('<%=Common.getBahasaConfigJS("Terjadi kesalahan jaringan")%>', 'bg-danger text-white');
        }
    };

    var bindEnter = function(elId, fn) { var el = document.getElementById(elId); if(el) el.addEventListener("keydown", function(e) { if(e.key === "Enter") { e.preventDefault(); fn(); } }); };
    bindEnter('pencarianTabel0_<%=rnd%>', window.loadTabel0<%=rnd%>);
    bindEnter('pencarianTabel1_<%=rnd%>', window.loadTabel1<%=rnd%>);
    bindEnter('pencarianTabel2_<%=rnd%>', window.loadTabel2<%=rnd%>);
    bindEnter('pencarianTabel3Cpl_<%=rnd%>', window.loadTabel3<%=rnd%>);
    bindEnter('pencarianTabel3Pl_<%=rnd%>', window.loadTabel3<%=rnd%>);
    bindEnter('pencarianTabel4_<%=rnd%>', window.loadTabel4<%=rnd%>);
    bindEnter('pencarianTabel5Mk_<%=rnd%>', window.loadTabel5<%=rnd%>);
    bindEnter('pencarianTabel5Bk_<%=rnd%>', window.loadTabel5<%=rnd%>);
    bindEnter('pencarianTabel7_<%=rnd%>', window.loadTabel7<%=rnd%>);
    bindEnter('pencarianTabel8_<%=rnd%>', window.loadTabel8<%=rnd%>);
    bindEnter('pencarianTabel9Cpl_<%=rnd%>', window.loadTabel9<%=rnd%>);
    bindEnter('pencarianTabel10_<%=rnd%>', window.loadTabel10<%=rnd%>);
    bindEnter('pencarianTabel11_<%=rnd%>', window.loadTabel11<%=rnd%>);
    bindEnter('pencarianTabel12_<%=rnd%>', window.loadTabel12<%=rnd%>);

    // INIT AUTO-SELECT & LOCK
    var idFakAwal<%=rnd%> = "<%=idFakultasTerpilih != null ? idFakultasTerpilih : ""%>";
    var idJurAwal<%=rnd%> = "<%=idJurusanTerpilih != null ? idJurusanTerpilih : ""%>";

    if (idFakAwal<%=rnd%> !== "") {
        document.getElementById('filterFakultas_<%=rnd%>').value = idFakAwal<%=rnd%>;
        window.ubahFakultas<%=rnd%>(idJurAwal<%=rnd%>);
    } else {
        window.muatUlangSemuaTabel<%=rnd%>();
    }
</script>