<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, org.hibernate.*, org.hibernate.criterion.*"%>
<%@ page import="ais.database.hibernate.HibernateUtil, ais.database.model.*, ais.database.model.sekolah.Sekolah"%>
<%@ page import="ais.action.master.PengumumanAkademisAction, ais.action.master.sekolah.util.SekolahUtil"%>
<%@ page import="ais.common.*, ais.ui.util.WaktuUtil"%>

<%
    // --- 1. LOGIC SERVER SIDE ---
    Session sess = null;
    List<Pertemuan> listPertemuan = new ArrayList<Pertemuan>();
    boolean dataTersedia = false;
    
    try {
        sess = HibernateUtil.openSession();
        Tbmuser user = Common.getCurrentUser(request);
        
        // Filter Hierarki
        Jurusan jur = (user != null) ? user.ambilJurusan() : null;
        Fakultas fak = (user != null) ? user.ambilFakultas() : null;
        Mahasiswa mhs = (user != null) ? user.getMahasiswa() : null;
        Dosen dsn = (user != null) ? user.ambilDosen() : null;

        String tglStr = Common.dateFormat8.get().format(WaktuUtil.getDate());
        
        Map<Long, Pertemuan> cache = PengumumanAkademisAction.pertemuansHarian.get(tglStr);
        if (cache == null) {
            List<Pertemuan> dbs = sess.createCriteria(Pertemuan.class)
                    .add(Restrictions.eq("aktif", true))
                    .add(Restrictions.eq("tanggal", WaktuUtil.getDate()))
                    .add(Restrictions.or(Restrictions.isNotNull("jadwalPelajaran"), Restrictions.isNotNull("perkuliahan")))
                    .addOrder(Order.asc("waktuMulai")).list();

            cache = new HashMap<Long, Pertemuan>();
            for (Pertemuan p : dbs) cache.put(p.getId(), p);
            PengumumanAkademisAction.pertemuansHarian.put(tglStr, cache);
        }

        for (Pertemuan p : cache.values()) {
            if (p.getPerkuliahan() == null || p.getPerkuliahan().getJumlahDosen() <= 0) continue;

            // OPTIMASI: Filter User (Mahasiswa/Dosen) menggunakan field String (Sangat cepat & hemat memori)
            if (mhs != null && (p.getMahasiswas() == null || !p.getMahasiswas().contains("," + mhs.getId() + ","))) continue;
            if (dsn != null && (p.getDosens() == null || !p.getDosens().contains("," + dsn.getId() + ","))) continue;

            // Filter Prodi/Fakultas
            if (jur != null && (p.getPerkuliahan().getJurusan() == null || !jur.getId().equals(p.getPerkuliahan().getJurusan().getId()))) continue;
            if (fak != null && (p.getPerkuliahan().getJurusan() == null || p.getPerkuliahan().getJurusan().getFakultas() == null || !fak.getId().equals(p.getPerkuliahan().getJurusan().getFakultas().getId()))) continue;

            if (listPertemuan.size() < 500) listPertemuan.add(p);
        }
        
        if (!listPertemuan.isEmpty()) {
            Collections.sort(listPertemuan);
            dataTersedia = true;
        }
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/home/info_kehadiran_dosen.jsp:58");
    } finally {
        if (sess != null && sess.isOpen()) {
            sess.disconnect();
            sess.close();
        }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>

<div class="card border-0 shadow-sm mb-4 animate__animated animate__fadeInUp" id="tableKehadiranDosen" 
     data-list='{"valueNames":["dosen-name","status-label","subject-name"],"page":10,"pagination":true}'>
    
    <div class="card-header bg-white border-bottom py-3">
        <div class="row align-items-center g-2">
            <div class="col-12 col-md-6">
                <h5 class="mb-0 d-flex align-items-center text-primary fw-bold">
                    <span class="fas fa-user-check me-2"></span>
                    <%= Common.getBahasaConfig("Kehadiran Dosen") %>
                </h5>
                <small class="text-muted"><%= Common.dateFormat6.get().format(WaktuUtil.getDate()) %></small>
            </div>
            <div class="col-12 col-md-6">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-light border-end-0"><i class="fas fa-search text-muted"></i></span>
                    <input class="form-control search border-start-0 bg-light" type="search" placeholder="<%= Common.getBahasaConfig("Cari Dosen atau Matakuliah") %>..." />
                </div>
            </div>
        </div>
    </div>

    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="bg-light">
                    <tr class="text-muted fw-bold fs-10 text-uppercase">
                        <th class="sort ps-4 py-3" data-sort="dosen-name" style="width: 35%"><%= Common.getBahasaConfig("Dosen") %></th>
                        <th class="sort py-3" data-sort="status-label" style="width: 15%"><%= Common.getBahasaConfig("Status") %></th>
                        <th class="sort py-3" data-sort="subject-name"><%= Common.getBahasaConfig("Perkuliahan") %></th>
                        <th class="text-end pe-4 py-3"><%= Common.getBahasaConfig("Waktu") %></th>
                    </tr>
                </thead>
                <tbody class="list">
                    <%
                    if (dataTersedia) {
                        for (Pertemuan p : listPertemuan) {
                            for (Dosen d : p.ambilDosen()) {
                                Statusabsensi st = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), p.retreiveAbsensiId(d.getId()));
                                if(st==null){
                                    st = ConstantValues.BELUM_ABSEN;
                                }
                                String badgeColor = "bg-light text-dark border";
                                if (st.getNama().toLowerCase().contains("hadir")) badgeColor = "bg-success-subtle text-success border border-success-subtle";
                                else if (st.getNama().toLowerCase().contains("izin")) badgeColor = "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
                                else if (st.getNama().toLowerCase().contains("alpa")) badgeColor = "bg-danger-subtle text-danger border border-danger-subtle";
                    %>
                    <tr>
                        <td class="ps-4 py-3">
                            <div class="d-flex align-items-center">
                                <div class="avatar avatar-l me-3">
                                    <img class="rounded-circle shadow-sm border" src="<%= CommonMedia.getUrlFotoPengguna(new Tbmuser(d), 100, 100) %>" width="40" height="40" style="object-fit: cover" />
                                </div>
                                <div>
                                    <div class="dosen-name fw-bold text-dark"><%= d.getNama() %></div>
                                    <div class="text-muted small">NIDN: <%= (d.getNidn() != null ? d.getNidn() : "-") %></div>
                                </div>
                            </div>
                        </td>
                        <td>
                            <span class="status-label badge rounded-pill fw-medium px-3 <%= badgeColor %>">
                                <%= st.getNama() %>
                            </span>
                        </td>
                        <td>
                            <div class="subject-name fw-semi-bold text-800"><%= p.getPerkuliahan().getMatakuliah().getNama() %></div>
                            <div class="text-muted small d-flex align-items-center">
                                <span class="badge bg-secondary-subtle text-secondary border me-2"><%= p.getPerkuliahan().getKelas() %></span>
                                <span><%= p.getStatusPertemuan().getNama() %></span>
                            </div>
                        </td>
                        <td class="text-end pe-4">
                            <div class="fw-bold text-primary">
                                <%= (p.getWaktuMulai() != null ? p.getWaktuMulai() : "--:--") %>
                            </div>
                            <div class="text-muted small"><%= (p.getWaktuSelesai() != null ? p.getWaktuSelesai() : "") %></div>
                        </td>
                    </tr>
                    <%      }
                        }
                    } else { %>
                    <tr>
                        <td colspan="4" class="text-center py-5">
                            <div class="py-3">
                                <i class="fas fa-calendar-times fa-3x text-light mb-3"></i>
                                <h6 class="text-secondary fw-normal"><%= Common.getBahasaConfig("Tidak ada jadwal perkuliahan hari ini") %></h6>
                            </div>
                        </td>
                    </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    </div>

    <jsp:include page="/WEB-INF/baru/componen/paging_default.jsp">
        <jsp:param name="size" value="<%=listPertemuan.size() %>" />
    </jsp:include>
</div>

<style>
    .bg-success-subtle { background-color: #d1e7dd !important; }
    .bg-warning-subtle { background-color: #fff3cd !important; }
    .bg-danger-subtle { background-color: #f8d7da !important; }
    .bg-secondary-subtle { background-color: #f8f9fa !important; }
    .text-warning-emphasis { color: #664d03 !important; }
    .fw-semi-bold { font-weight: 600; }
    .fs-10 { font-size: 0.75rem; }
    .avatar-l img { transition: transform 0.2s; }
    .avatar-l img:hover { transform: scale(1.1); }
</style>