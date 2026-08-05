<%@page import="java.util.Calendar"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.ambilGuru() == null) return;

    Guru guru = tbmuser.ambilGuru();

    int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    String waktu = (jam < 10) ? "Pagi" : (jam < 15) ? "Siang" : (jam < 18) ? "Sore" : "Malam";

    List<String> orgInfo = new ArrayList<String>();
    if (tbmuser.ambilYayasan() != null) orgInfo.add(tbmuser.ambilYayasan().getNama());
    if (tbmuser.ambilSekolah() != null) orgInfo.add(tbmuser.ambilSekolah().getNama());

    StringBuilder sbOrg = new StringBuilder();
    for (int i = 0; i < orgInfo.size(); i++) {
        sbOrg.append(orgInfo.get(i));
        if (i < orgInfo.size() - 1) sbOrg.append(" &bull; ");
    }
    String subHeader = sbOrg.toString();
%>

<div class="row g-4 mb-4 animate__animated animate__fadeInUp">
    <div class="col-xl-6 col-lg-12">
        <div class="card border-0 shadow-sm h-100 overflow-hidden rounded-3">
            <div class="card-header border-0 pt-4 pb-3 bg-light"
                 style="background-image:url('<%=request.getContextPath()%>/img/corner-4.png');background-position:right top;background-repeat:no-repeat;background-size:contain;">
                <h5 class="text-success fw-bold mb-1">
                    <i class="fas fa-sun me-2 text-warning"></i><%=Common.getBahasaConfig("Selamat")%> <%=Common.getBahasaConfig(waktu)%>,
                    <%=guru.getNama() != null ? guru.getNama() : tbmuser.getUserNama()%>
                </h5>
                <div class="text-muted small fw-bold text-uppercase" style="letter-spacing:.5px">
                    <%=Common.getBahasaConfig("Guru")%><%=subHeader.isEmpty() ? "" : " &bull; " + subHeader%>
                </div>
            </div>
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-success bg-opacity-10 p-3 rounded-circle text-success">
                                    <i class="fas fa-chalkboard-teacher fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("Jadwal Pelajaran")%></h6>
                                <a href="<%=Common.ROOT%>/baru?p=pagesmasterjadwalpelajaranzul&s=index"
                                   class="btn btn-outline-success btn-sm mt-1">
                                    <i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Lihat")%>
                                </a>
                            </div>
                        </div>
                    </div>
                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-primary bg-opacity-10 p-3 rounded-circle text-primary">
                                    <i class="fas fa-clipboard-list fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("Absensi")%></h6>
                                <a href="<%=Common.ROOT%>/baru?p=pagesmasterabsensigurusekolahzul&s=index"
                                   class="btn btn-outline-primary btn-sm mt-1">
                                    <i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Lihat")%>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card-footer bg-white border-top-0 pb-3">
                <div class="d-flex align-items-center p-2 border rounded bg-light shadow-sm">
                    <div class="flex-shrink-0 me-3">
                        <div class="bg-secondary bg-opacity-10 p-2 rounded-circle text-secondary">
                            <i class="fas fa-id-card"></i>
                        </div>
                    </div>
                    <div>
                        <h6 class="text-muted small text-uppercase mb-0" style="font-size:.7rem">NIP / NIK</h6>
                        <div class="fw-bold text-dark">
                            <%=guru.getNip() != null && !guru.getNip().isEmpty() ? guru.getNip() : "-"%>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="col-xl-6 col-lg-12">
        <div class="card border-0 shadow-sm h-100 rounded-3">
            <div class="card-header bg-white pt-4 pb-3 border-bottom-0"
                 style="background-image:url('<%=request.getContextPath()%>/img/corner-6.png');background-position:right top;background-repeat:no-repeat;background-size:contain;">
                <h5 class="text-success fw-bold mb-0"><%=Common.getBahasaConfig("Pintasan")%></h5>
                <small class="text-muted"><%=Common.getBahasaConfig("Akses cepat modul pengajaran")%></small>
            </div>
            <div class="card-body">
                <div class="d-flex flex-wrap gap-2">
                    <a href="<%=Common.ROOT%>/baru?p=pagesmasterjadwalpelajaranzul&s=index"
                       class="btn btn-outline-success btn-sm">
                        <i class="fas fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Jadwal Pelajaran")%>
                    </a>
                    <a href="<%=Common.ROOT%>/baru?p=pagesmasterpenilaiansiswasekolahzul&s=index"
                       class="btn btn-outline-primary btn-sm">
                        <i class="far fa-check-circle me-1"></i><%=Common.getBahasaConfig("Input Nilai")%>
                    </a>
                    <a href="<%=Common.ROOT%>/baru?p=pagesmasterabsensikelassekolahzul&s=index"
                       class="btn btn-outline-info btn-sm">
                        <i class="fas fa-fingerprint me-1"></i><%=Common.getBahasaConfig("Absensi Kelas")%>
                    </a>
                    <a href="<%=Common.ROOT%>/baru?p=pagesmasterbukuagendagurusekolahzul&s=index"
                       class="btn btn-outline-secondary btn-sm">
                        <i class="fas fa-book me-1"></i><%=Common.getBahasaConfig("Agenda Guru")%>
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>
