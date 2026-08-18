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
    // Inisialisasi User & Scope
    Tbmuser tbmuser = Common.getCurrentUser(request);
    Fakultas fakultas = (tbmuser != null) ? tbmuser.ambilFakultas() : null;
    Jurusan jurusana = (tbmuser != null) ? tbmuser.ambilJurusan() : null;
    PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

    // Build Query secara Dinamis & Optimal
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT b.id AS jurusan_id, b.nama AS jurusan, a.tahun_ajaran, a.ganjil_genap, COUNT(*) AS jumlah_siswa ");
    sb.append("FROM pertemuan aa ");
    sb.append("INNER JOIN perkuliahan a ON (aa.perkuliahan = a.id) ");
    sb.append("INNER JOIN jurusan b ON (a.jurusan = b.id) ");
    sb.append("INNER JOIN fakultas c ON (b.fakultas = c.id) ");
    sb.append("WHERE b.aktif AND a.aktif AND aa.aktif ");

    if (jurusana != null) {
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

    List<Object[]> jurusans = Common.ambilSql(sb.toString());
    if (jurusans == null) jurusans = new ArrayList<Object[]>();
%>

<div class="col-lg-6 col-12 mb-4 animate__animated animate__fadeInUp">
    <div class="card shadow-sm border-0 h-100">
        <div class="card-header bg-white py-3 border-bottom-0">
            <h5 class="mb-0 text-primary fw-bold">
                <i class="fas fa-calendar-check me-2"></i><%=Common.getBahasaConfig("Jumlah pertemuan") %>
            </h5>
        </div>

        <div class="card-body p-0">
            <div id="Jml_pertemuan" data-list='{"valueNames":["prodi","tahun","smt","jumlahMhs"],"page":5,"pagination":true}'>
                
                <div class="table-responsive" style="min-height: 280px;">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="bg-light text-uppercase small">
                            <tr>
                                <th class="sort ps-4 text-secondary py-3" data-sort="prodi"><%=Common.getBahasaConfig("Prodi") %></th>
                                <th class="sort text-secondary py-3" data-sort="tahun"><%=Common.getBahasaConfig("TA") %></th>
                                <th class="sort text-secondary py-3" data-sort="smt"><%=Common.getBahasaConfig("Smt") %></th>
                                <th class="sort text-secondary py-3 text-end pe-4" data-sort="jumlahMhs"><%=Common.getBahasaConfig("Jumlah") %></th>
                            </tr>
                        </thead>
                        <tbody class="list small">
                            <%
                            int total = 0;
                            if (jurusans.isEmpty()) {
                            %>
                                <tr>
                                    <td colspan="4" class="text-center py-5 text-muted">
                                        <i class="fas fa-info-circle me-1"></i> <%=Common.getBahasaConfig("Belum ada pertemuan") %>
                                    </td>
                                </tr>
                            <%
                            } else {
                                for (Object[] obj : jurusans) {
                                    String jurusan = (obj[1] == null ? "-" : obj[1].toString());
                                    String ta = (obj[2] == null ? "-" : obj[2].toString());
                                    String smt = (obj[3] == null ? "-" : obj[3].toString());
                                    int jumlah = (obj[4] instanceof Number) ? ((Number) obj[4]).intValue() : 0;
                                    total += jumlah;
                            %>
                                <tr>
                                    <td class="prodi ps-4 fw-medium text-dark"><%=jurusan %></td>
                                    <td class="tahun text-secondary"><%=ta %></td>
                                    <td class="smt text-secondary"><%=smt %></td>
                                    <td class="jumlahMhs text-end pe-4">
                                        <span class="badge bg-primary-soft text-primary rounded-pill"><%=jumlah %></span>
                                    </td>
                                </tr>
                            <%
                                }
                            }
                            %>
                        </tbody>
                    </table>
                </div>

                <jsp:include page="/WEB-INF/baru/componen/paging_default.jsp">
		            <jsp:param name="size" value="<%=jurusans.size() %>" />
		        </jsp:include>

            </div>
        </div>
    </div>
</div>

<style>
    /* Utility class tambahan jika Bootstrap-soft-badges belum tersedia di template */
    .bg-primary-soft { background-color: rgba(13, 110, 253, 0.1); }
</style>