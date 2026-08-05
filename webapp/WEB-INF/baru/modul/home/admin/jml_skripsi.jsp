<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. Inisialisasi Scope & User
    Tbmuser tbmuser = Common.getCurrentUser(request);
    Fakultas fakultas = (tbmuser != null) ? tbmuser.ambilFakultas() : null;
    Jurusan jurusana = (tbmuser != null) ? tbmuser.ambilJurusan() : null;
    PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

    // 2. Query Builder - Menggunakan StringBuilder untuk efisiensi di Java 1.7
    StringBuilder sb = new StringBuilder();
    String smtCase = "(CASE WHEN aa.semester % 2 = 0 THEN 'Genap' ELSE 'Ganjil' END)";
    
    sb.append("SELECT b.id, b.nama, aa.tahun_akademik, ").append(smtCase).append(", COUNT(*) ");
    sb.append("FROM skripsi aa ");
    sb.append("INNER JOIN mahasiswa a ON (aa.mahasiswa = a.id) ");
    sb.append("INNER JOIN jurusan b ON (a.jurusan = b.id) ");
    sb.append("INNER JOIN fakultas c ON (b.fakultas = c.id) ");
    sb.append("WHERE b.aktif AND a.aktif AND aa.setujuisidang ");

    // Filter Dinamis
    if (jurusana != null) {
        sb.append("AND a.jurusan = ").append(jurusana.getId()).append(" ");
    }
    if (fakultas != null && fakultas.getId() != null) {
        sb.append("AND b.fakultas = ").append(fakultas.getId()).append(" ");
    }
    if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
        sb.append("AND c.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
    }

    sb.append("GROUP BY b.id, b.nama, aa.tahun_akademik, ").append(smtCase).append(" ");
    sb.append("ORDER BY b.id, aa.tahun_akademik DESC, ").append(smtCase);

    List<Object[]> jurusans = Common.ambilSql(sb.toString());
    if (jurusans == null) jurusans = new ArrayList<Object[]>();
%>

<div class="col-lg-6 col-12 mb-4 animate__animated animate__fadeInUp">
    <div class="card shadow-sm border-0 h-100">
        
        <div class="card-header bg-white py-3 border-bottom-0">
            <h5 class="mb-0 text-dark fw-bold">
                <i class="fas fa-book-reader text-primary me-2"></i>
                <%=Common.getBahasaConfig("Jumlah TA/Skripsi/Thesis/Disertasi") %>
            </h5>
        </div>

        <div class="card-body p-0">
            <div id="Jml_tugas_akhir" data-list='{"valueNames":["prodi","tahun","smt","jumlahMhs"],"page":5,"pagination":true}'>
                
                <div class="table-responsive" style="min-height: 280px;">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="bg-light text-uppercase small">
                            <tr>
                                <th class="sort ps-4 text-secondary py-3" data-sort="prodi" style="cursor:pointer;"><%=Common.getBahasaConfig("Prodi") %></th>
                                <th class="sort text-secondary py-3" data-sort="tahun" style="cursor:pointer;"><%=Common.getBahasaConfig("TA") %></th>
                                <th class="sort text-secondary py-3" data-sort="smt" style="cursor:pointer;"><%=Common.getBahasaConfig("Smt") %></th>
                                <th class="sort text-end pe-4 text-secondary py-3" data-sort="jumlahMhs" style="cursor:pointer;"><%=Common.getBahasaConfig("Jumlah") %></th>
                            </tr>
                        </thead>
                        <tbody class="list small">
                            <%
                            int total = 0;
                            if (jurusans.isEmpty()) {
                            %>
                                <tr>
                                    <td colspan="4" class="text-center py-5 text-muted fst-italic">
                                        <%=Common.getBahasaConfig("Belum ada TA/Skripsi/Thesis/Disertasi") %>
                                    </td>
                                    <td class="prodi d-none"></td><td class="tahun d-none"></td><td class="smt d-none"></td><td class="jumlahMhs d-none"></td>
                                </tr>
                            <%
                            } else {
                                for (Object[] obj : jurusans) {
                                    String prodi = (obj[1] == null ? "-" : obj[1].toString());
                                    String ta = (obj[2] == null ? "-" : obj[2].toString());
                                    String smt = (obj[3] == null ? "-" : obj[3].toString());
                                    int jumlah = (obj[4] instanceof Number) ? ((Number) obj[4]).intValue() : 0;
                                    total += jumlah;
                            %>
                                <tr>
                                    <td class="prodi ps-4 fw-medium text-dark"><%=prodi %></td>
                                    <td class="tahun text-secondary"><%=ta %></td>
                                    <td class="smt text-secondary"><%=smt %></td>
                                    <td class="jumlahMhs text-end pe-4">
                                        <span class="badge bg-primary rounded-pill px-3 py-2"><%=jumlah %></span>
                                    </td>
                                </tr>
                            <%
                                }
                            }
                            %>
                        </tbody>
                        <tfoot class="bg-light border-top">
                            <tr class="fw-bold">
                                <td class="ps-4"><%=Common.getBahasaConfig("Total") %></td>
                                <td></td>
                                <td></td>
                                <td class="text-end pe-4 text-primary fs-6"><%=total %></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>

                <jsp:include page="/WEB-INF/baru/componen/paging_default.jsp">
		            <jsp:param name="size" value="<%=jurusans.size() %>" />
		        </jsp:include>

            </div> </div>
    </div>
</div>