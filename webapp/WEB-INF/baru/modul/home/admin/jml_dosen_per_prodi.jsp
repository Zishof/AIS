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
    // 1. Inisialisasi Variable & Null Safety
    Tbmuser tbmuser = Common.getCurrentUser(request);
    // Menggunakan ternary operator untuk mempersingkat null check
    Fakultas fakultas = (tbmuser != null) ? tbmuser.ambilFakultas() : null;
    Jurusan jurusana = (tbmuser != null) ? tbmuser.ambilJurusan() : null;
    PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

    // 2. Query Builder yang Efisien (StringBuilder)
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT b.id AS jurusan_id, b.nama AS jurusan, count(*) AS jumlah_siswa ");
    sb.append("FROM dosen a ");
    sb.append("INNER JOIN jurusan b ON (a.jurusan = b.id) ");
    sb.append("WHERE a.aktif "); // Asumsi aktif bernilai 1/true

    // Filter berdasarkan User Login / Scope
    if (jurusana != null) {
        sb.append("AND a.jurusan = ").append(jurusana.getId()).append(" ");
    }
    if (fakultas != null && fakultas.getId() != null) {
        sb.append("AND a.fakultas = ").append(fakultas.getId()).append(" ");
    }
    if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
        sb.append("AND a.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
    }

    sb.append("GROUP BY b.id, b.nama ORDER BY b.id DESC");
 
    // 3. Eksekusi Query dengan Safety Check
    List<Object[]> jurusans = Common.ambilSql(sb.toString());
    if (jurusans == null) {
        jurusans = new ArrayList<Object[]>(); // Mencegah NullPointerException pada loop
    }
%>

<div class="col-lg-6 col-12 mb-4 animate__animated animate__fadeInUp">
    <div class="card shadow-sm border-0 h-100">
        
        <div class="card-header bg-white py-3 border-bottom-0 d-flex align-items-center justify-content-between">
            <h5 class="mb-0 text-primary fw-bold">
                <i class="fas fa-chalkboard-teacher me-2"></i><%=Common.getBahasaConfig("Jml dosen per prodi") %>
            </h5>
        </div>

        <div class="card-body p-0">
            <div id="Jml_dosen_per_prodi" data-list='{"valueNames":["prodi","jumlahDosen"],"page":5,"pagination":true}'>
                
                <div class="table-responsive">
                    <table class="table table-striped table-hover align-middle mb-0">
                        <thead class="bg-light text-uppercase small text-muted">
                            <tr>
                                <th class="sort ps-4" data-sort="prodi" style="cursor:pointer; border-top:none;">
                                    <%=Common.getBahasaConfig("Prodi") %>
                                </th>
                                <th class="sort text-end pe-4" data-sort="jumlahDosen" style="cursor:pointer; border-top:none;">
                                    <%=Common.getBahasaConfig("Jumlah Dosen") %>
                                </th>
                            </tr>
                        </thead>
                        <tbody class="list">
                            <%
                            int total = 0;
                            if (jurusans.isEmpty()) {
                            %>
                                <tr>
                                    <td colspan="2" class="text-center py-4 text-muted fst-italic">
                                        <%=Common.getBahasaConfig("Data tidak ditemukan") %>
                                    </td>
                                    <td class="prodi d-none"></td>
                                    <td class="jumlahDosen d-none"></td>
                                </tr>
                            <%
                            } else {
                                for (Object[] obj : jurusans) {
                                    String namaJurusan = (obj[1] != null) ? obj[1].toString() : "-";
                                    // Casting number yang aman untuk tipe data Integer/Long/BigInt
                                    int jml = (obj[2] instanceof Number) ? ((Number) obj[2]).intValue() : 0;
                                    total += jml;
                            %>
                                <tr>
                                    <td class="prodi ps-4 fw-medium text-dark"><%=namaJurusan %></td>
                                    <td class="jumlahDosen text-end pe-4">
                                        <span class="badge bg-primary rounded-pill"><%=jml %></span>
                                    </td>
                                </tr>
                            <%
                                }
                            }
                            %>
                        </tbody>
                        <tfoot class="bg-light border-top">
                            <tr>
                                <td class="ps-4 fw-bold text-dark"><%=Common.getBahasaConfig("Total") %></td>
                                <td class="text-end pe-4 fw-bold text-primary"><%=total %></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
                
                
                <jsp:include page="/WEB-INF/baru/componen/paging_default.jsp">
		            <jsp:param name="size" value="<%=jurusans.size() %>" />
		        </jsp:include>

                

            </div> 
         </div>
    </div>
</div>