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
    // 1. Inisialisasi & Null Safety
    Tbmuser tbmuser = Common.getCurrentUser(request);
    Fakultas fakultas = (tbmuser != null) ? tbmuser.ambilFakultas() : null;
    Jurusan jurusana = (tbmuser != null) ? tbmuser.ambilJurusan() : null;
    PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

    // 2. Query Builder yang Lebih Rapi (Menggunakan StringBuilder untuk performa & keterbacaan)
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT b.id AS jurusan_id, b.nama AS jurusan, a.tahun, count(*) AS jumlah_siswa ");
    sb.append("FROM biodata_calon_mahasiswa a ");
    sb.append("INNER JOIN jurusan b ON (a.prodi_lulus = b.id) ");
    sb.append("INNER JOIN fakultas c ON (b.fakultas = c.id) ");
    sb.append("WHERE b.aktif "); // Asumsi aktif adalah boolean/int 1
    sb.append("AND a.tahun > 1900 AND a.tahun < 2100 "); 

    if (jurusana != null && jurusana.getId() != null) {
        sb.append("AND a.prodi_lulus = ").append(jurusana.getId()).append(" ");
    } 

    if (fakultas != null && fakultas.getId() != null) {
        sb.append("AND b.fakultas = ").append(fakultas.getId()).append(" ");
    }

    if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
        sb.append("AND c.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
    }

    sb.append("GROUP BY b.id, a.tahun ");
    sb.append("ORDER BY b.id, a.tahun DESC");

    // Eksekusi Query
    List<Object[]> jurusans = Common.ambilSql(sb.toString());
    if (jurusans == null) {
        jurusans = new ArrayList<Object[]>(); // Prevent NullPointer di loop
    }
%>

<div class="col-lg-6 col-12 mb-4 animate__animated animate__fadeInUp">
    <div class="card shadow-sm h-100 border-0">
        <div class="card-header bg-white py-3 d-flex align-items-center justify-content-between border-bottom-0">
            <h5 class="mb-0 text-primary fw-bold">
                <i class="fas fa-users me-2"></i><%=Common.getBahasaConfig("Jml calon mahasiswa") %>
            </h5>
        </div>
        
        <div class="card-body p-0">
            <div id="Jml_calon_mahasiswa_per_prodi" data-list='{"valueNames":["prodi","tahun","jumlahMhs"],"page":5,"pagination":true}'>
                
                <div class="table-responsive">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="bg-light text-uppercase small text-muted">
                            <tr>
                                <th class="sort ps-4" data-sort="prodi" style="cursor:pointer;"><%=Common.getBahasaConfig("Prodi") %></th>
                                <th class="sort" data-sort="tahun" style="cursor:pointer;"><%=Common.getBahasaConfig("Tahun") %></th>
                                <th class="sort text-end pe-4" data-sort="jumlahMhs" style="cursor:pointer;"><%=Common.getBahasaConfig("Jumlah Mhs") %></th>
                            </tr>
                        </thead>
                        <tbody class="list small">
                            <%
                            int total = 0;
                            if (jurusans.isEmpty()) {
                            %>
                                <tr>
                                    <td colspan="3" class="text-center py-4 text-muted">
                                        <%=Common.getBahasaConfig("Belum ada mahasiswa lulus") %>
                                    </td>
                                    <td class="prodi d-none"></td>
                                    <td class="tahun d-none"></td>
                                    <td class="jumlahMhs d-none"></td>
                                </tr>
                            <%
                            } else {
                                for (Object[] obj : jurusans) {
                                    String namaJurusan = (obj[1] != null) ? obj[1].toString() : "-";
                                    // Parsing number yang aman
                                    int tahunMasuk = (obj[2] instanceof Number) ? ((Number) obj[2]).intValue() : 0;
                                    int jumlahSiswa = (obj[3] instanceof Number) ? ((Number) obj[3]).intValue() : 0;
                                    
                                    total += jumlahSiswa;
                            %>
                                <tr>
                                    <td class="prodi ps-4 fw-medium text-dark"><%=namaJurusan %></td>
                                    <td class="tahun text-secondary"><%=tahunMasuk %></td>
                                    <td class="jumlahMhs text-end pe-4">
                                        <span class="badge bg-soft-primary text-primary rounded-pill"><%=jumlahSiswa %></span>
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
                                <td class="text-end pe-4 fw-bold text-primary"><%=total %></td>
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