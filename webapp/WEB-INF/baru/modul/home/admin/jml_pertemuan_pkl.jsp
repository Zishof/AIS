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
    // Build SQL Query - Lebih rapi dan efisien
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT cc.tahunakademik, cc.semester, COUNT(*) AS jumlah_siswa ");
    sb.append("FROM pertemuan aa ");
    sb.append("INNER JOIN kelompok_pkl bb ON (aa.kelompok_pkl = bb.id) ");
    sb.append("INNER JOIN pkl cc ON (bb.pkl = cc.id) ");
    sb.append("WHERE aa.aktif ");
    sb.append("GROUP BY cc.tahunakademik, cc.semester ");
    sb.append("ORDER BY cc.tahunakademik DESC, cc.semester");

    List<Object[]> jurusans = Common.ambilSql(sb.toString());
    if (jurusans == null) jurusans = new ArrayList<Object[]>();
%>

<div class="col-lg-6 col-12 mb-4 animate__animated animate__fadeInUp">
    <div class="card shadow-sm border-0 h-100">
        
        <div class="card-header bg-white py-3 border-bottom-0 d-flex align-items-center">
            <div class="icon-shape icon-sm bg-warning-soft text-warning rounded-pill me-2">
                <i class="fas fa-map-marked-alt"></i>
            </div>
            <h5 class="mb-0 fw-bold text-dark">
                <%=Common.getBahasaConfig("Jumlah kegiatan PKL") %>
            </h5>
        </div>
        
        <div class="card-body p-0">
            <div id="Jml_pertemuan_pkl" data-list='{"valueNames":["tahun","smt","jumlahMhs"],"page":5,"pagination":true}'>
                
                <div class="table-responsive" style="min-height: 250px;">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="bg-light text-uppercase small">
                            <tr>
                                <th class="sort ps-4 text-secondary py-3" data-sort="tahun" style="cursor:pointer;"><%=Common.getBahasaConfig("TA") %></th>
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
                                    <td colspan="3" class="text-center py-5 text-muted fst-italic">
                                        <%=Common.getBahasaConfig("Belum ada kegiatan PKL") %>
                                    </td>
                                    <td class="tahun d-none"></td><td class="smt d-none"></td><td class="jumlahMhs d-none"></td>
                                </tr>
                            <%
                            } else {
                                for (Object[] obj : jurusans) {
                                    String ta = (obj[0] == null ? "-" : obj[0].toString());
                                    String smt = (obj[1] == null ? "-" : obj[1].toString());
                                    int jumlah = (obj[2] instanceof Number) ? ((Number) obj[2]).intValue() : 0;
                                    total += jumlah;
                            %>
                                <tr>
                                    <td class="tahun ps-4 fw-medium text-dark"><%=ta %></td>
                                    <td class="smt text-secondary"><%=smt %></td>
                                    <td class="jumlahMhs text-end pe-4">
                                        <span class="badge bg-warning rounded-pill px-3 py-2 text-dark fw-bold">
                                            <%=jumlah %>
                                        </span>
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
                                <td></td>
                                <td class="text-end pe-4 fw-bold text-warning fs-6"><%=total %></td>
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

<style>
    /* Utility pendukung jika belum ada di template */
    .bg-warning-soft { background-color: rgba(245, 128, 21, 0.15) !important; }
    .icon-shape { width: 32px; height: 32px; display: inline-flex; align-items: center; justify-content: center; }
</style>