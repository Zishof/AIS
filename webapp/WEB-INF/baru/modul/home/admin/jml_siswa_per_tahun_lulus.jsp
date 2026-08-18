<%@page import="java.util.List"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.SQLQuery"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Yayasan"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // Inisialisasi User & Scope Context
    Tbmuser tbmuser = Common.getCurrentUser(request);
    Yayasan yayasan = (tbmuser != null) ? tbmuser.ambilYayasan() : null;
    Sekolah sekolahContext = (tbmuser != null) ? tbmuser.ambilSekolah() : null;
    
    List<Object[]> dataList = null;
    Session sess = null;
    int total = 0;

    try {
        // Membuka Session (Wajib ditutup di blok finally)
        sess = HibernateUtil.getSessionFactory().openSession();
        StringBuilder sb = new StringBuilder();
        
        // PERBAIKAN: Mengganti tahun_lulus menjadi tahunlulus sesuai fisik tabel di database
        sb.append("SELECT b.id AS sekolah_id, b.nama AS sekolah, a.tahunlulus, count(a.id) AS jumlah_siswa ");
        sb.append("FROM sekolah.siswa a ");
        sb.append("INNER JOIN sekolah.sekolah b ON (a.sekolah_id = b.id) ");
        sb.append("WHERE a.tahunlulus IS NOT NULL AND a.tahunlulus > 0 ");
        
        if (yayasan != null) {
            sb.append("AND b.yayasan_id = ").append(yayasan.getId()).append(" ");
        }
        if (sekolahContext != null) {
            sb.append("AND b.id = ").append(sekolahContext.getId()).append(" ");
        }
        
        sb.append("GROUP BY b.id, b.nama, a.tahunlulus ");
        sb.append("ORDER BY a.tahunlulus DESC, b.nama ASC");

        SQLQuery query = sess.createSQLQuery(sb.toString());
        dataList = query.list();
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/home/admin/jml_siswa_per_tahun_lulus.jsp:46");
    } finally {
        // PENUTUPAN MUTLAK SESSION DI BLOK FINALLY (Mencegah Connection Leak)
        if (sess != null && sess.isOpen()) {
            try { sess.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/admin/jml_siswa_per_tahun_lulus.jsp:50");}
        }
    }
%>

<div class="col-12 col-xl-6" id="Jml_siswa_per_tahun_lulus">
    <div class="card h-100 border-0 shadow-sm" style="border-radius: 12px; overflow: hidden;">
        <div class="card-header bg-white border-bottom py-3 d-flex align-items-center">
            <div class="bg-success text-white rounded-circle d-flex align-items-center justify-content-center me-3 shadow-sm" style="width: 45px; height: 45px;">
                <i class="fas fa-award fs-5"></i>
            </div>
            <div>
                <h6 class="mb-0 text-dark fw-bold"><%=Common.getBahasaConfig("Jumlah Siswa Lulus") %></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Siswa yang telah menyelesaikan studi") %></small>
            </div>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive" style="max-height: 350px; overflow-y: auto;">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light sticky-top shadow-sm" style="z-index: 10;">
                        <tr>
                            <th class="ps-4 text-secondary text-uppercase fw-semibold" style="font-size: 0.8rem; letter-spacing: 0.5px;"><%=Common.getBahasaConfig("Sekolah") %></th>
                            <th class="text-secondary text-center text-uppercase fw-semibold" style="font-size: 0.8rem; letter-spacing: 0.5px;"><%=Common.getBahasaConfig("Tahun Lulus") %></th>
                            <th class="text-end pe-4 text-secondary text-uppercase fw-semibold" style="font-size: 0.8rem; letter-spacing: 0.5px;"><%=Common.getBahasaConfig("Jumlah") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                        if (dataList != null && !dataList.isEmpty()) {
                            for (Object[] row : dataList) {
                                String namaSekolah = (row[1] != null) ? row[1].toString() : "-";
                                String tahunLulus = (row[2] != null) ? row[2].toString() : "-";
                                int jml = (row[3] instanceof Number) ? ((Number) row[3]).intValue() : 0;
                                total += jml;
                        %>
                        <tr style="transition: background-color 0.2s;">
                            <td class="ps-4 fw-medium text-dark">
                                <i class="fas fa-school text-success me-2 opacity-50"></i><%=namaSekolah %>
                            </td>
                            <td class="text-secondary text-center">
                                <span class="badge bg-light text-dark border px-2 py-1"><%=tahunLulus %></span>
                            </td>
                            <td class="text-end pe-4">
                                <span class="badge bg-success rounded-pill px-3 py-2 shadow-sm"><%=jml %></span>
                            </td>
                        </tr>
                        <%
                            }
                        } else {
                        %>
                        <tr>
                            <td colspan="3" class="text-center py-5 text-muted">
                                <div class="d-flex flex-column align-items-center justify-content-center">
                                    <i class="fas fa-box-open fa-3x mb-3 text-secondary opacity-25"></i>
                                    <span class="fw-medium"><%=Common.getBahasaConfig("Tidak ada data kelulusan")%></span>
                                </div>
                            </td>
                        </tr>
                        <% } %>
                    </tbody>
                    <tfoot class="bg-light border-top sticky-bottom" style="z-index: 10;">
                        <tr>
                            <td class="ps-4 fw-bold text-dark text-uppercase" style="font-size: 0.85rem;"><%=Common.getBahasaConfig("Total Keseluruhan") %></td>
                            <td></td>
                            <td class="text-end pe-4 fw-bold text-success fs-5"><%=total %></td>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </div>
    </div>
</div>