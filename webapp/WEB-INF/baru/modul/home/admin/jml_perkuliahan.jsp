<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. Inisialisasi Scope & User
    Tbmuser tbmuser = Common.getCurrentUser(request);
    Fakultas fakultas = (tbmuser != null) ? tbmuser.ambilFakultas() : null;
    Jurusan jurusana = (tbmuser != null) ? tbmuser.ambilJurusan() : null;
    PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

    // 2. Query Builder (Efisien & Rapi)
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT b.id AS jurusan_id, b.nama AS jurusan, a.tahun_ajaran, a.ganjil_genap, count(*) AS jumlah_siswa ");
    sb.append("FROM perkuliahan a ");
    sb.append("INNER JOIN jurusan b ON (a.jurusan = b.id) ");
    sb.append("INNER JOIN fakultas c ON (b.fakultas = c.id) ");
    sb.append("WHERE b.aktif AND a.aktif ");

    // Filter Scope
    if (jurusana != null && jurusana.getId() != null) {
        sb.append("AND a.jurusan = ").append(jurusana.getId()).append(" ");
    }
    if (fakultas != null && fakultas.getId() != null) {
        sb.append("AND b.fakultas = ").append(fakultas.getId()).append(" ");
    }
    if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
        sb.append("AND c.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
    }

    sb.append("GROUP BY b.id, a.tahun_ajaran, a.ganjil_genap ");
    sb.append("ORDER BY b.id, a.tahun_ajaran DESC, a.ganjil_genap");

    // 3. Eksekusi Query dengan Null Safety
    List<Object[]> jurusans = Common.ambilSql(sb.toString());
    if (jurusans == null) {
        jurusans = new ArrayList<Object[]>(); // Mencegah NullPointerException
    }
%>

<div class="col-lg-6 col-12 mb-4 animate__animated animate__fadeInUp">
    <div class="card shadow-sm border-0 h-100">
        
        <div class="card-header bg-white py-3 border-bottom-0 d-flex align-items-center justify-content-between">
            <h5 class="mb-0 text-info fw-bold">
                <i class="fas fa-chalkboard-teacher me-2"></i><%=Common.getBahasaConfig("Jumlah perkuliahan") %>
            </h5>
        </div>
        
        <div class="card-body p-0">
            <div id="Jml_perkuliahan" data-list='{"valueNames":["prodi","tahun","smt","jumlahMhs"],"page":5,"pagination":true}'>
                
                <div class="table-responsive" style="min-height: 250px;">
                    <table class="table table-striped table-hover align-middle mb-0">
                        <thead class="bg-light text-uppercase small text-muted">
                            <tr>
                                <th class="sort ps-4" data-sort="prodi" style="cursor:pointer;"><%=Common.getBahasaConfig("Prodi") %></th>
                                <th class="sort" data-sort="tahun" style="cursor:pointer;"><%=Common.getBahasaConfig("TA") %></th>
                                <th class="sort" data-sort="smt" style="cursor:pointer;"><%=Common.getBahasaConfig("Smt") %></th>
                                <th class="sort text-end pe-4" data-sort="jumlahMhs" style="cursor:pointer;"><%=Common.getBahasaConfig("Jumlah") %></th>
                            </tr>
                        </thead>
                        <tbody class="list small">
                            <%
                            int total = 0;
                            if (jurusans.isEmpty()) {
                            %>
                                <tr>
                                    <td colspan="4" class="text-center py-4 text-muted fst-italic">
                                        <%=Common.getBahasaConfig("Belum ada perkuliahan") %>
                                    </td>
                                    <td class="prodi d-none"></td><td class="tahun d-none"></td>
                                    <td class="smt d-none"></td><td class="jumlahMhs d-none"></td>
                                </tr>
                            <%
                            } else {
                                for (Object[] obj : jurusans) {
                                    String namaJurusan = (obj[1] != null) ? obj[1].toString() : "-";
                                    String tahunAjaran = (obj[2] != null) ? obj[2].toString() : "";
                                    String semester = (obj[3] != null) ? obj[3].toString() : "";
                                    
                                    // Safety casting for numbers
                                    int jumlah = (obj[4] instanceof Number) ? ((Number) obj[4]).intValue() : 0;
                                    total += jumlah;
                            %>
                                <tr>
                                    <td class="prodi ps-4 fw-medium text-dark"><%=namaJurusan %></td>
                                    <td class="tahun text-secondary"><%=tahunAjaran %></td>
                                    <td class="smt text-secondary"><%=semester %></td>
                                    <td class="jumlahMhs text-end pe-4">
                                        <span class="badge bg-info text-dark rounded-pill"><%=jumlah %></span>
                                    </td>
                                </tr>
                            <%
                                }
                            }
                            %>
                        </tbody>
                        <tfoot class="bg-light border-top">
                            <tr>
                                <td class="ps-4 fw-bold"><%=Common.getBahasaConfig("Total") %></td>
                                <td></td>
                                <td></td>
                                <td class="text-end pe-4 fw-bold text-info fs-6"><%=total %></td>
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