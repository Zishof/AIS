<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonPrivilages"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%
String rnd = Common.getGeneratedBarCode(7);
Tbmuser tbmuser = Common.getCurrentUser(request);

if (tbmuser == null || tbmuser.getUserId() == null) return;

boolean isMahasiswa = tbmuser.getMahasiswa() != null;
boolean isDosen = tbmuser.getDosen() != null;

String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");
String idPerkuliahanStr = request.getParameter("perkuliahan");
String variable = request.getParameter("var") != null ? request.getParameter("var") : rnd;

if(idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) return;

KurikulumPunyaMatakuliah kpm = null;
Perkuliahan perkuliahan = null;
List<String> validItemIds = new ArrayList<String>();
List<Perkuliahan> listPerkuliahanTersedia = new ArrayList<Perkuliahan>();

try {
    kpm = (KurikulumPunyaMatakuliah) GeneralValueObject.ambilData(KurikulumPunyaMatakuliah.class, idKpmStr, true);
    if (idPerkuliahanStr != null && !idPerkuliahanStr.trim().isEmpty() && !idPerkuliahanStr.equals("null")) {
        perkuliahan = (Perkuliahan) GeneralValueObject.ambilData(Perkuliahan.class, idPerkuliahanStr, true);
    }
} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pertemuan.jsp:42"); }

if (kpm == null) return;

Session sess = HibernateUtil.openSession();
Transaction tx = null;

try {
    org.hibernate.Criteria crit = sess.createCriteria(Perkuliahan.class)
        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
        .add(Restrictions.eq("kurikulumPunyaMatakuliah.id", kpm.getId()));

    if (isDosen) {
        List<Long> perkuliahansIds = tbmuser.getDosen().ambilPerkuliahan();
        if (perkuliahansIds != null && !perkuliahansIds.isEmpty()) {
            crit.add(Restrictions.in("id", perkuliahansIds));
        } else {
            crit.add(Restrictions.isNull("id")); // Hindari ambil semua jika tidak ada yang diajar
        }
    } else if (isMahasiswa) {
        List<Long> perkuliahansIds = tbmuser.getMahasiswa().ambilPerkuliahanDanParalel();
        if (perkuliahansIds != null && !perkuliahansIds.isEmpty()) {
            crit.add(Restrictions.in("id", perkuliahansIds));
        } else {
            crit.add(Restrictions.isNull("id")); // Hindari ambil semua jika tidak ada KRS
        }
    }

    crit.addOrder(Order.desc("idSmt")).addOrder(Order.desc("id"));
    listPerkuliahanTersedia = ConstantValues.simpleList(crit, Perkuliahan.class);

    // ====================================================================
    // LOGIKA AUTO-SELECT JIKA DATA HANYA 1 (Berlaku untuk Dosen/Mahasiswa)
    // ====================================================================
    if (perkuliahan == null && listPerkuliahanTersedia.size() == 1) {
        perkuliahan = listPerkuliahanTersedia.get(0);
    }

    if (perkuliahan != null) {
        int banyak = 0;
        JSONObject jsonArrayKpm = null;
        try {
            String rincianDataStr = kpm.getRincian() != null ? kpm.getRincian().trim() : "{}";
            if (rincianDataStr.startsWith("[")) {
                JSONArray tempArray = new JSONArray(rincianDataStr);
                jsonArrayKpm = new JSONObject();
                for(int i = 0; i < tempArray.length(); i++) {
                    JSONObject item = tempArray.getJSONObject(i);
                    String key = item.optString("keyData", "key_" + System.currentTimeMillis() + "_" + i);
                    jsonArrayKpm.put(key, item);
                }
            } else {
                jsonArrayKpm = new JSONObject(rincianDataStr);
            }
            
            TreeMap<Integer, Map> mapsRinci = kpm.populateRinci(jsonArrayKpm);
            for (Map map : mapsRinci.values()) {
                JSONObject jsonObj = (JSONObject) map.get("jsonObject");
                if (jsonObj != null) {
                    int sampaiMgg = jsonObj.optInt("sampaiMingguKe", 0);
                    if (sampaiMgg > banyak) banyak = sampaiMgg;
                }
            }
        } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pertemuan.jsp:105"); }

        if (banyak == 0 && perkuliahan.getJumlahMaksimalPertemuan() != null) {
            banyak = perkuliahan.getJumlahMaksimalPertemuan();
        }
        if (banyak == 0) banyak = 16; 

        tx = sess.beginTransaction();
        
        List<Pertemuan> pertemuansTemp = sess.createCriteria(Pertemuan.class)
            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
            .addOrder(perkuliahan.getUrutkanotomatis() != null && !perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
            .add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
            .add(Restrictions.eq("perkuliahan", perkuliahan)).list();

        Date curr = perkuliahan.getTanggalMulaiPerkuliahan();
        Calendar myCalendar = ais.ui.util.WaktuUtil.getCalendar();
        if (curr != null) myCalendar.setTime(curr);

        StringBuilder sqlIdsBuilder = new StringBuilder();

        for (int minggu = 1; minggu <= banyak; minggu++) {
            Pertemuan pert = null;
            for (Pertemuan p : pertemuansTemp) {
                if (p != null && p.getPertemuanKe() != null && p.getPertemuanKe().equals(minggu)) {
                    pert = p; break;
                }
            }

            if (perkuliahan.getLewatiTanggalMerahNasional() != null && perkuliahan.getLewatiTanggalMerahNasional()) {
                myCalendar = Common.tanggalMerahAja(perkuliahan.getJenis(), myCalendar);
            }
            Date currDate = myCalendar.getTime();

            if (pert == null) {
                pert = new Pertemuan();
                pert.setPerkuliahan(perkuliahan);
                pert.setPertemuanManual(minggu);
                pert.setPertemuanKe(minggu);
                
                int maxMgg = perkuliahan.getJumlahMaksimalPertemuan() != null ? perkuliahan.getJumlahMaksimalPertemuan() : banyak;
                
                if (minggu == maxMgg) {
                    pert.setStatusPertemuan(ConstantValues.UAS);
                    pert.setTopik(Common.getBahasaConfig("Pertemuan Ke-") + minggu + " : UAS");
                    pert.setMetodePembelajaran(Common.getBahasaConfig("Mengerjakan soal UAS"));
                } else if (minggu == (maxMgg / 2)) {
                    pert.setStatusPertemuan(ConstantValues.UTS);
                    pert.setTopik(Common.getBahasaConfig("Pertemuan Ke-") + minggu + " : UTS");
                    pert.setMetodePembelajaran(Common.getBahasaConfig("Mengerjakan soal UTS"));
                } else {
                    pert.setStatusPertemuan(ConstantValues.TATAP_MUKA);
                }
                
                pert.setWaktuMulai(perkuliahan.getWaktuMulai());
                pert.setWaktuSelesai(perkuliahan.getWaktuSelesai());
                pert.setTanggal(currDate);
                pert.setMulai(currDate);
                pert.setSelesai(null);
                pert.setRuang(perkuliahan.getRuang());
                pert.setAktif(true);
                
                sess.save(pert);
                sess.flush();
            }

            validItemIds.add(pert.getId().toString());

            if (sqlIdsBuilder.length() > 0) sqlIdsBuilder.append(",");
            sqlIdsBuilder.append(pert.getId().toString());

            if (curr != null) {
                myCalendar = Common.curreDate(perkuliahan.getJenis(), myCalendar);
            }
        }

        if (sqlIdsBuilder.length() > 0) {
            String sqlUpdate = "update pertemuan set aktif=false where id not in (" + sqlIdsBuilder.toString() + ") and perkuliahan=" + perkuliahan.getId();
            sess.createSQLQuery(sqlUpdate).executeUpdate();
        }
        
        tx.commit();
    }
} catch(Exception e) {
    if (tx != null) tx.rollback();
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pertemuan.jsp:190");
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pertemuan.jsp:192");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
}
%>

<style>
    .btn-minggu {
        width: 45px; 
        height: 45px;
        font-size: 1.1rem;
        background-color: #f8f9fa;
        color: #6c757d;
        border: 1px solid #dee2e6;
        transition: all 0.2s ease-in-out;
    }
    .btn-minggu:hover {
        background-color: #e2e6ea;
        color: #495057;
    }
    .nav-pills .nav-link.btn-minggu.active {
        background-color: #0d6efd;
        color: #ffffff;
        border-color: #0d6efd;
        box-shadow: 0 0.125rem 0.25rem rgba(0,0,0,0.075);
    }
</style>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4">
        
        <div class="d-flex justify-content-between align-items-end mb-3 border-bottom pb-3">
            <div>
                <h6 class="fw-bold text-secondary mb-1"><i class="fas fa-calendar-check me-2"></i><%=Common.getBahasaConfig("Daftar Pertemuan")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Pertemuan kelas otomatis disinkronkan dengan rincian OBE.")%></small>
            </div>
        </div>
        
        <% if (!listPerkuliahanTersedia.isEmpty()) { %>
            <div class="card bg-light border-0 shadow-none rounded-3 p-3 mb-4 d-flex flex-row align-items-center justify-content-between flex-wrap gap-3">
                <div>
                    <label class="form-label fw-bold text-secondary mb-0"><i class="fas fa-chalkboard text-primary me-2"></i><%=Common.getBahasaConfig("Kelas / Jadwal Perkuliahan")%></label>
                    <small class="text-muted d-block"><%=Common.getBahasaConfig("Pilih jadwal kelas untuk melihat atau mengatur pertemuan")%></small>
                </div>
                <div style="min-width: 350px; flex-grow: 1; max-width: 500px;">
                    <select class="form-select shadow-sm border-primary fw-bold text-dark" onchange="if(this.value) { window.pilihPerkuliahanUntukPertemuan<%=rnd%>(this.value); }" <%= listPerkuliahanTersedia.size() == 1 ? "disabled" : "" %>>
                        <% if (listPerkuliahanTersedia.size() > 1) { %>
                            <option value=""><%=Common.getBahasaConfig("-- Pilih Jadwal Perkuliahan --")%></option>
                        <% } %>
                        <% for(Perkuliahan p : listPerkuliahanTersedia) { 
                            String namaKelas = p.infoSimple();
                            String smt = p.getIdSmt() != null ? p.getIdSmt() : "";
                            String selected = (perkuliahan != null && p.getId().equals(perkuliahan.getId())) ? "selected" : "";
                        %>
                            <option value="<%=p.getId()%>" <%=selected%>><%=smt%> - <%=namaKelas%></option>
                        <% } %>
                    </select>
                </div>
            </div>
            
            <script>
                window.pilihPerkuliahanUntukPertemuan<%=rnd%> = function(idPerkuliahanPilih) {
                    var urlPertemuanLoad = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning%2Fobe&s=_pertemuan&kurikulumPunyaMatakuliah=<%=kpm.getId()%>&var=<%=variable%>&perkuliahan=" + idPerkuliahanPilih;
                    if (typeof loadContentIntoContainer === 'function') {
                        loadContentIntoContainer(urlPertemuanLoad, 'containerPertemuan<%=variable%>');
                    } else {
                        if(typeof tampilkanToast === 'function') {
                            tampilkanToast('<%=Common.getBahasaConfigJS("Fungsi pemuat halaman tidak ditemukan.")%>', 'bg-danger text-white');
                        }
                    }
                };
            </script>
        <% } else { %>
            <div class="alert alert-warning border-0 shadow-sm rounded-3 py-3 d-flex align-items-center mb-4">
                <i class="fas fa-exclamation-triangle fs-3 me-3 text-warning"></i>
                <div>
                    <strong class="text-dark"><%=Common.getBahasaConfig("Belum Ada Kelas")%></strong><br>
                    <small class="text-dark"><%=Common.getBahasaConfig("Belum ada jadwal perkuliahan aktif yang berelasi dengan Anda untuk RPS ini.")%></small>
                </div>
            </div>
        <% } %>

        <% if (perkuliahan == null) { %>
            <div class="text-center py-5">
                <i class="fas fa-hand-pointer text-muted fs-1 mb-3 opacity-50"></i>
                <p class="text-muted mb-0"><%=Common.getBahasaConfig("Silakan pilih Jadwal Perkuliahan di atas terlebih dahulu.")%></p>
            </div>
        <% } else if (validItemIds.isEmpty()) { %>
            <div class="text-center py-5">
                <i class="fas fa-box-open text-muted fs-1 mb-3 opacity-50"></i>
                <p class="text-muted mb-0"><%=Common.getBahasaConfig("Pertemuan belum tersedia. Harap isi Rincian Pertemuan terlebih dahulu.")%></p>
            </div>
        <% } else { %>
            
            <div class="mb-4">
                <ul class="nav nav-pills d-flex flex-wrap gap-2 justify-content-center" id="pills-tab-pertemuan" role="tablist">
                    <% for (int i = 0; i < validItemIds.size(); i++) { 
                        String activeClass = (i == 0) ? "active" : "";
                    %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link rounded-circle fw-bold d-flex align-items-center justify-content-center p-0 btn-minggu <%=activeClass%>" 
                                id="tab-<%=validItemIds.get(i)%>-btn" 
                                data-bs-toggle="pill" 
                                data-bs-target="#content-<%=validItemIds.get(i)%>" 
                                type="button" role="tab" 
                                title="<%=Common.getBahasaConfig("Minggu Ke-")%><%=i+1%>">
                            <%=i+1%>
                        </button>
                    </li>
                    <% } %>
                </ul>
            </div>
            
            <div class="tab-content" id="pills-tabContent-pertemuan">
                <% for (int i = 0; i < validItemIds.size(); i++) { 
                    String itemId = validItemIds.get(i);
                    String showActive = (i == 0) ? "show active" : "";
                %>
                <div class="tab-pane fade <%=showActive%>" id="content-<%=itemId%>" role="tabpanel" aria-labelledby="tab-<%=itemId%>-btn">
                    <jsp:include page="/WEB-INF/baru/modul/elearning/pertemuan.jsp">
                        <jsp:param name="pertemuan" value="<%=itemId%>" />
                    </jsp:include>
                </div>
                <% } %>
            </div>

        <% } %>
        
    </div>
</div>