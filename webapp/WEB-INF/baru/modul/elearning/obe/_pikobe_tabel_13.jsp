<%@page import="java.util.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Kurikulum"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.KelompokMatakuliah"%>
<%@page import="ais.database.model.KurikulumPunyaMatakuliah"%>
<%@page import="ais.database.model.Matakuliah"%>

<%
String rnd = request.getParameter("var") != null ? request.getParameter("var") : Common.getGeneratedBarCode(7);
String action = request.getParameter("action");
String idJurusanStr = request.getParameter("idJurusan");

Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) return;

Long idJurusan = null;
try {
    if (idJurusanStr != null && !idJurusanStr.trim().isEmpty()) {
        idJurusan = Long.parseLong(idJurusanStr);
    }
} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_13.jsp:parseIdJurusan"); }

if (idJurusan == null) {
    out.print("<div class='alert alert-warning text-center'>" + Common.getBahasaConfig("Program Studi tidak valid.") + "</div>");
    return;
}

// ==========================================
// ACTION: LOAD TABEL 13 (ORGANISASI MK PER SEMESTER)
// ==========================================
if ("loadTabel13".equals(action)) {
    List<KurikulumPunyaMatakuliah> listKpm = new ArrayList<KurikulumPunyaMatakuliah>();
    Kurikulum kurikulumAktif = null;
    int maxSemester = 8;
    Session sess = null;

    try {
        sess = HibernateUtil.openSession();

        Criteria critKurikulum = sess.createCriteria(Kurikulum.class);
        critKurikulum.add(Restrictions.eq("jurusan.id", idJurusan));
        critKurikulum.add(Restrictions.eq("obe", true));
        critKurikulum.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        critKurikulum.addOrder(Order.desc("id"));

        List<Kurikulum> kList = ConstantValues.simpleList(critKurikulum, Kurikulum.class);
        if (!kList.isEmpty()) {
            kurikulumAktif = kList.get(0);

            Jurusan jur = kurikulumAktif.getJurusan();
            try {
                if (jur != null && jur.getJenjang() != null && jur.getJenjang().getJumlahSemester() != null) {
                    maxSemester = jur.getJenjang().getJumlahSemester();
                }
            } catch (Exception ex) { }

            Criteria critKpm = sess.createCriteria(KurikulumPunyaMatakuliah.class);
            critKpm.createAlias("matakuliah", "mk");
            critKpm.add(Restrictions.eq("kurikulum.id", kurikulumAktif.getId()));
            critKpm.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            critKpm.add(Restrictions.or(Restrictions.isNull("mk.aktif"), Restrictions.eq("mk.aktif", true)));
            critKpm.addOrder(Order.asc("semester")).addOrder(Order.asc("mk.kode"));
            listKpm = ConstantValues.simpleList(critKpm, KurikulumPunyaMatakuliah.class);
        }
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/obe/_pikobe_tabel_13.jsp:loadTabel13");
    } finally {
        if (sess != null) { sess.clear(); sess.disconnect(); sess.close(); }
    }

    // Kelompokkan per semester
    Map<Integer, List<KurikulumPunyaMatakuliah>> perSmt = new LinkedHashMap<Integer, List<KurikulumPunyaMatakuliah>>();
    for (int s = 1; s <= maxSemester; s++) {
        perSmt.put(s, new ArrayList<KurikulumPunyaMatakuliah>());
    }
    for (KurikulumPunyaMatakuliah kpm : listKpm) {
        Integer s = kpm.getSemester();
        if (s == null || s < 1) s = 1;
        if (s > maxSemester) s = maxSemester;
        if (perSmt.containsKey(s)) perSmt.get(s).add(kpm);
    }

    // Hitung total keseluruhan
    int totalMk = listKpm.size();
    int totalSks = 0;
    for (KurikulumPunyaMatakuliah kpm : listKpm) {
        Matakuliah mk = kpm.getMatakuliah();
        if (mk != null && mk.getSks() != null) totalSks += mk.getSks();
    }

    String[] smtLabel = {"I","II","III","IV","V","VI","VII","VIII","IX","X","XI","XII"};
%>

    <% if (kurikulumAktif == null) { %>
        <div class="alert alert-warning text-center py-4 border-0 shadow-sm rounded-3">
            <i class="fas fa-exclamation-triangle fa-2x mb-2 d-block text-warning"></i>
            <%=Common.getBahasaConfig("Kurikulum OBE aktif untuk Program Studi ini tidak ditemukan. Silakan atur di Master Kurikulum.")%>
        </div>
    <% } else { %>
        <div class="d-flex justify-content-between align-items-center mb-3 bg-white p-3 rounded shadow-sm border flex-wrap gap-2">
            <div>
                <span class="text-muted small fw-bold d-block"><%=Common.getBahasaConfig("Kurikulum Aktif:")%></span>
                <span class="text-primary fw-bold"><%=kurikulumAktif.getNama()%></span>
            </div>
            <div class="d-flex gap-4">
                <div class="text-center">
                    <div class="fw-bold text-primary" style="font-size:1.4rem;"><%=totalMk%></div>
                    <div class="text-muted small"><%=Common.getBahasaConfig("Total MK")%></div>
                </div>
                <div class="text-center">
                    <div class="fw-bold text-success" style="font-size:1.4rem;"><%=totalSks%></div>
                    <div class="text-muted small"><%=Common.getBahasaConfig("Total SKS")%></div>
                </div>
                <div class="text-center">
                    <div class="fw-bold text-secondary" style="font-size:1.4rem;"><%=maxSemester%></div>
                    <div class="text-muted small"><%=Common.getBahasaConfig("Semester")%></div>
                </div>
            </div>
        </div>

        <div class="table-responsive border rounded-3 animate__animated animate__fadeIn" style="max-height:680px;">
            <table class="table table-bordered align-top mb-0 small">
                <thead class="table-primary sticky-top" style="z-index:10;">
                    <tr>
                        <th class="text-center fw-bold align-middle" style="width:90px;"><%=Common.getBahasaConfig("Semester")%></th>
                        <th class="text-center fw-bold align-middle" style="width:70px;"><%=Common.getBahasaConfig("Jml MK")%></th>
                        <th class="text-center fw-bold align-middle" style="width:80px;"><%=Common.getBahasaConfig("Total SKS")%></th>
                        <th class="fw-bold align-middle"><%=Common.getBahasaConfig("Daftar Matakuliah (per Kelompok)")%></th>
                    </tr>
                </thead>
                <tbody>
                <%
                int rowNo = 0;
                for (Map.Entry<Integer, List<KurikulumPunyaMatakuliah>> entry : perSmt.entrySet()) {
                    int smtNo = entry.getKey();
                    List<KurikulumPunyaMatakuliah> listSmt = entry.getValue();
                    String smtStr = (smtNo >= 1 && smtNo <= smtLabel.length) ? smtLabel[smtNo - 1] : String.valueOf(smtNo);
                    int smtSks = 0;
                    for (KurikulumPunyaMatakuliah kpm : listSmt) {
                        Matakuliah mk = kpm.getMatakuliah();
                        if (mk != null && mk.getSks() != null) smtSks += mk.getSks();
                    }
                    String rowBg = rowNo % 2 == 0 ? "#f8faff" : "#ffffff";
                    rowNo++;

                    // Kelompokkan per KelompokMatakuliah
                    Map<String, List<KurikulumPunyaMatakuliah>> byKelompok = new LinkedHashMap<String, List<KurikulumPunyaMatakuliah>>();
                    for (KurikulumPunyaMatakuliah kpm : listSmt) {
                        Matakuliah mk = kpm.getMatakuliah();
                        String kelKey = "-|Tanpa Kelompok";
                        if (mk != null) {
                            KelompokMatakuliah kel = null;
                            try { kel = mk.getKelompokMatakuliah(); } catch (Exception ek) { }
                            if (kel != null) {
                                String kk = kel.getKode() != null ? kel.getKode().trim() : "";
                                String kn = kel.getNama() != null ? kel.getNama().trim() : "";
                                if (!kk.isEmpty() || !kn.isEmpty()) kelKey = kk + "|" + kn;
                            }
                        }
                        if (!byKelompok.containsKey(kelKey)) byKelompok.put(kelKey, new ArrayList<KurikulumPunyaMatakuliah>());
                        byKelompok.get(kelKey).add(kpm);
                    }
                %>
                    <tr style="background:<%=rowBg%>;">
                        <td class="text-center fw-bold text-primary align-middle" style="font-size:1.1rem;">Smt&nbsp;<%=smtStr%></td>
                        <td class="text-center fw-bold align-middle">
                            <span class="badge bg-primary rounded-pill px-2"><%=listSmt.size()%></span>
                        </td>
                        <td class="text-center fw-bold text-success align-middle">
                            <span class="badge bg-success rounded-pill px-2"><%=smtSks%></span>
                        </td>
                        <td class="py-2">
                        <% if (listSmt.isEmpty()) { %>
                            <span class="text-muted fst-italic"><%=Common.getBahasaConfig("Belum ada matakuliah untuk semester ini. Atur di Tabel 7.")%></span>
                        <% } else {
                            for (Map.Entry<String, List<KurikulumPunyaMatakuliah>> kEntry : byKelompok.entrySet()) {
                                String[] parts = kEntry.getKey().split("\\|", 2);
                                String kelKode = parts[0];
                                String kelNama = parts.length > 1 ? parts[1] : "";
                                List<KurikulumPunyaMatakuliah> kpmGrp = kEntry.getValue();
                        %>
                            <div class="mb-2">
                                <span class="badge bg-secondary me-1"><%=kelKode.isEmpty() ? "-" : kelKode%></span>
                                <span class="fw-bold small text-dark"><%=kelNama%></span>
                                <div class="d-flex flex-wrap gap-1 mt-1 ms-2">
                                <% for (KurikulumPunyaMatakuliah kpm2 : kpmGrp) {
                                    Matakuliah mk2 = kpm2.getMatakuliah();
                                    if (mk2 == null) continue;
                                    int sks2 = mk2.getSks() != null ? mk2.getSks() : 0;
                                %>
                                    <div class="border rounded px-2 py-1 bg-white" style="font-size:11px; white-space:nowrap;">
                                        <span class="badge bg-primary" style="font-size:10px;"><%=mk2.getKode()%></span>
                                        <span class="text-dark ms-1"><%=mk2.getNama()%></span>
                                        <span class="badge bg-success ms-1" style="font-size:10px;"><%=sks2%>&nbsp;SKS</span>
                                    </div>
                                <% } %>
                                </div>
                            </div>
                        <% } %>
                        <% } %>
                        </td>
                    </tr>
                <% } %>
                </tbody>
                <tfoot class="table-secondary">
                    <tr class="fw-bold">
                        <td class="text-center">TOTAL</td>
                        <td class="text-center"><span class="badge bg-primary rounded-pill px-2"><%=totalMk%></span></td>
                        <td class="text-center"><span class="badge bg-success rounded-pill px-2"><%=totalSks%></span></td>
                        <td class="text-muted small fst-italic">
                            <%=totalMk%> <%=Common.getBahasaConfig("matakuliah")%>, <%=totalSks%> SKS
                            — <%=Common.getBahasaConfig("Rata-rata")%> <%=(maxSemester > 0 ? totalSks / maxSemester : 0)%> SKS/semester
                        </td>
                    </tr>
                </tfoot>
            </table>
        </div>
        <div class="mt-2 text-muted small">
            <i class="fas fa-info-circle me-1 text-info"></i>
            <%=Common.getBahasaConfig("Tampilan di atas menunjukkan distribusi matakuliah per semester berdasarkan kurikulum OBE aktif. Untuk mengubah semester matakuliah, gunakan Tabel 7 (Susunan Matakuliah).")%>
        </div>
    <% } %>

<%
}
%>
