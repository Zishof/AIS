<%@page import="java.util.List"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Query"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    Yayasan yayasan = (tbmuser != null) ? tbmuser.ambilYayasan() : null;
    Sekolah sekolahContext = (tbmuser != null) ? tbmuser.ambilSekolah() : null;
    
    List<Object[]> dataList = null;
    Session sess = null;
    int total = 0;

    try {
        sess = HibernateUtil.getSessionFactory().openSession();
        
        // OPTIMASI MEMORI & KECEPATAN: 
        // Menggunakan HQL (Hibernate Query Language) agar database langsung melakukan perhitungan (COUNT).
        // Ini menghindari OutOfMemory karena tidak meload ribuan baris data/objek KelasSiswaPunyaSiswa ke dalam RAM.
        StringBuilder hql = new StringBuilder();
        hql.append("SELECT b.nama, k.nama, count(a.id) ");
        hql.append("FROM KelasSiswaPunyaSiswa kps ");
        hql.append("JOIN kps.siswa a ");
        hql.append("JOIN kps.kelasSiswa k ");
        hql.append("JOIN a.sekolah b ");
        hql.append("WHERE a.aktif = true "); // Hanya menghitung siswa yang masih aktif
        
        if (yayasan != null) {
            hql.append("AND b.yayasan.id = ").append(yayasan.getId()).append(" ");
        }
        if (sekolahContext != null) {
            hql.append("AND b.id = ").append(sekolahContext.getId()).append(" ");
        }
        
        hql.append("GROUP BY b.nama, k.nama ");
        hql.append("ORDER BY b.nama ASC, k.nama ASC");

        Query query = sess.createQuery(hql.toString());
        dataList = query.list();
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/home/admin/jml_siswa_cuti.jsp:48");
    } finally {
        // PENUTUPAN KONEKSI MUTLAK: Mencegah Connection Pool Exhaustion (Server Hang)
        if (sess != null && sess.isOpen()) {
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/admin/jml_siswa_cuti.jsp:52");}
        }
    }
%>

<div class="col-12 col-xl-6" id="Jml_siswa_per_kelas">
    <div class="card h-100 border-0 shadow-sm">
        <div class="card-header bg-white border-bottom py-3">
            <h6 class="mb-0 text-primary fw-bold">
                <i class="fas fa-users me-2"></i><%=Common.getBahasaConfig("Distribusi Jumlah Siswa Per Kelas") %>
            </h6>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive" style="max-height: 350px; overflow-y: auto;">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light sticky-top">
                        <tr>
                            <th class="ps-4 text-secondary"><%=Common.getBahasaConfig("Sekolah") %></th>
                            <th class="text-secondary text-center"><%=Common.getBahasaConfig("Kelas") %></th>
                            <th class="text-end pe-4 text-secondary"><%=Common.getBahasaConfig("Jumlah Siswa") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                        if (dataList != null && !dataList.isEmpty()) {
                            for (Object[] row : dataList) {
                                String namaSekolah = (row[0] != null) ? row[0].toString() : "-";
                                String namaKelas = (row[1] != null) ? row[1].toString() : "-";
                                int jml = (row[2] instanceof Number) ? ((Number) row[2]).intValue() : 0;
                                total += jml;
                        %>
                        <tr>
                            <td class="ps-4 fw-medium text-dark"><%=namaSekolah %></td>
                            <td class="text-secondary text-center"><%=namaKelas %></td>
                            <td class="text-end pe-4">
                                <span class="badge bg-primary rounded-pill px-3 py-2"><%=jml %></span>
                            </td>
                        </tr>
                        <%
                            }
                        } else {
                        %>
                        <tr>
                            <td colspan="3" class="text-center py-4 text-muted">
                                <i class="fas fa-info-circle mb-2 fa-2x opacity-50"></i><br>
                                <%=Common.getBahasaConfig("Tidak ada data siswa dan kelas yang ditemukan")%>
                            </td>
                        </tr>
                        <% } %>
                    </tbody>
                    <tfoot class="bg-light border-top sticky-bottom">
                        <tr>
                            <td class="ps-4 fw-bold text-dark"><%=Common.getBahasaConfig("Total Keseluruhan") %></td>
                            <td></td>
                            <td class="text-end pe-4 fw-bold text-primary fs-6"><%=total %></td>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </div>
    </div>
</div>