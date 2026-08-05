<%@page import="java.util.Calendar"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getSiswa() == null) return;

    Siswa siswa = tbmuser.getSiswa();

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
                 style="background-image:url('<%=request.getContextPath()%>/img/corner-3.png');background-position:right top;background-repeat:no-repeat;background-size:contain;">
                <h5 class="text-warning fw-bold mb-1">
                    <i class="fas fa-sun me-2 text-warning"></i><%=Common.getBahasaConfig("Selamat")%> <%=Common.getBahasaConfig(waktu)%>,
                    <%=siswa.getNama() != null ? siswa.getNama() : tbmuser.getUserNama()%>
                </h5>
                <div class="text-muted small fw-bold text-uppercase" style="letter-spacing:.5px">
                    <%=Common.getBahasaConfig("Siswa")%><%=subHeader.isEmpty() ? "" : " &bull; " + subHeader%>
                </div>
            </div>
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-warning bg-opacity-10 p-3 rounded-circle text-warning">
                                    <i class="fas fa-book-open fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("Jadwal Pelajaran")%></h6>
                                <a href="<%=Common.ROOT%>/baru?p=pagesmasterjadwalpelajaransiswasekolahzul&s=index"
                                   class="btn btn-outline-warning btn-sm mt-1">
                                    <i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Lihat")%>
                                </a>
                            </div>
                        </div>
                    </div>
                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-info bg-opacity-10 p-3 rounded-circle text-info">
                                    <i class="fas fa-clipboard-check fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("Nilai Rapor")%></h6>
                                <a href="<%=Common.ROOT%>/baru?p=pagesmasterraporzul&s=index"
                                   class="btn btn-outline-info btn-sm mt-1">
                                    <i class="fas fa-eye me-1"></i><%=Common.getBahasaConfig("Lihat")%>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card-footer bg-white border-top-0 pb-3">
                <div class="d-flex gap-2 flex-wrap">
                    <a href="<%=Common.ROOT%>/baru?p=pagesmastertagihanpembayaransiswazul&s=index"
                       class="btn btn-primary btn-sm">
                        <i class="fas fa-money-bill-wave me-1"></i><%=Common.getBahasaConfig("Informasi Tagihan")%>
                    </a>
                    <a href="<%=Common.ROOT%>/baru?p=pagesmasterabsensikelasdaftarhadirzul&s=index"
                       class="btn btn-outline-secondary btn-sm">
                        <i class="fas fa-fingerprint me-1"></i><%=Common.getBahasaConfig("Kehadiran")%>
                    </a>
                </div>
            </div>
        </div>
    </div>

    <div class="col-xl-6 col-lg-12">
        <div class="card border-0 shadow-sm h-100 rounded-3">
            <div class="card-header bg-white pt-4 pb-3 border-bottom-0"
                 style="background-image:url('<%=request.getContextPath()%>/img/corner-5.png');background-position:right top;background-repeat:no-repeat;background-size:contain;">
                <h5 class="text-warning fw-bold mb-0"><%=Common.getBahasaConfig("Status Belajar")%></h5>
                <small class="text-muted"><%=Common.getBahasaConfig("Informasi kegiatan belajar mengajar")%></small>
            </div>
            <div class="card-body">
                <div class="alert alert-warning border-0 shadow-sm d-flex align-items-center">
                    <i class="fas fa-user-graduate fa-2x me-3"></i>
                    <div>
                        <div class="fw-bold"><%=siswa.getNama()%></div>
                        <div class="small text-muted"><%=subHeader%></div>
                    </div>
                </div>
                <div class="d-flex flex-wrap gap-2 mt-2">
                    <a href="<%=Common.ROOT%>/baru?p=pagesmasterpengumumansekolahzul&s=index"
                       class="btn btn-outline-warning btn-sm">
                        <i class="fas fa-bullhorn me-1"></i><%=Common.getBahasaConfig("Pengumuman")%>
                    </a>
                    <a href="<%=Common.ROOT%>/baru?p=pagesmasterkalenderakademiksekolahzul&s=index"
                       class="btn btn-outline-info btn-sm">
                        <i class="fas fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Kalender Akademik")%>
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>
