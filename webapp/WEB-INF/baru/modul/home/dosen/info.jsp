<%@page import="java.util.Calendar"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.ambilDosen() == null) return;

    Dosen dosen = tbmuser.ambilDosen();

    int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    String waktu = (jam < 10) ? "Pagi" : (jam < 15) ? "Siang" : (jam < 18) ? "Sore" : "Malam";

    String currentTA = Common.getCurrentTahunAkademik();
    if (currentTA == null) currentTA = "";

    // Hitung statistik dosen
    int jmlPerkuliahan = 0;
    int jmlHariIni    = 0;

    String[] hariNames = {"Minggu","Senin","Selasa","Rabu","Kamis","Jumat","Sabtu"};
    String hariIni = hariNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1];

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();

        Number nPk = (Number) sess.createCriteria(Perkuliahan.class)
            .add(Restrictions.eq("aktif", true))
            .add(Restrictions.eq("tahunAjaran", currentTA))
            .add(Restrictions.or(
                Restrictions.eq("dosen1", dosen),
                Restrictions.eq("dosen2", dosen)
            ))
            .setProjection(Projections.rowCount()).uniqueResult();
        jmlPerkuliahan = nPk != null ? nPk.intValue() : 0;

        Number nHari = (Number) sess.createCriteria(Perkuliahan.class)
            .add(Restrictions.eq("aktif", true))
            .add(Restrictions.eq("tahunAjaran", currentTA))
            .add(Restrictions.eq("hari", hariIni))
            .add(Restrictions.or(
                Restrictions.eq("dosen1", dosen),
                Restrictions.eq("dosen2", dosen)
            ))
            .setProjection(Projections.rowCount()).uniqueResult();
        jmlHariIni = nHari != null ? nHari.intValue() : 0;

    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/dosen/info.jsp:57");
        // fail silently — statistics optional
    } finally {
        if (sess != null && sess.isOpen()) try { sess.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/home/dosen/info.jsp:60");}
    }

    // Org info
    List<String> orgInfo = new ArrayList<String>();
    if (tbmuser.ambilFakultas() != null) orgInfo.add(tbmuser.ambilFakultas().getNama());
    if (tbmuser.ambilJurusan()  != null) orgInfo.add(tbmuser.ambilJurusan().getNama());
    if (tbmuser.ambilYayasan()  != null) orgInfo.add(tbmuser.ambilYayasan().getNama());

    StringBuilder sbOrg = new StringBuilder();
    for (int i = 0; i < orgInfo.size(); i++) {
        sbOrg.append(orgInfo.get(i));
        if (i < orgInfo.size() - 1) sbOrg.append(" &bull; ");
    }
    String subHeader = sbOrg.toString();
%>

<div class="row g-4 mb-4 animate__animated animate__fadeInUp">

    <%-- Kartu identitas + statistik --%>
    <div class="col-xl-6 col-lg-12">
        <div class="card border-0 shadow-sm h-100 overflow-hidden rounded-3">
            <div class="card-header border-0 pt-4 pb-3 bg-light"
                 style="background-image:url('<%=request.getContextPath()%>/img/corner-3.png');background-position:right top;background-repeat:no-repeat;background-size:contain;">
                <h5 class="text-primary fw-bold mb-1">
                    <i class="fas fa-sun me-2 text-warning"></i><%=Common.getBahasaConfig("Selamat")%> <%=Common.getBahasaConfig(waktu)%>,
                    <%=dosen.getNama() != null ? dosen.getNama() : tbmuser.getUserNama()%>
                </h5>
                <div class="text-muted small fw-bold text-uppercase" style="letter-spacing:.5px">
                    <%=Common.getBahasaConfig("Dosen")%><%=subHeader.isEmpty() ? "" : " &bull; " + subHeader%>
                </div>
            </div>

            <div class="card-body">
                <div class="row g-3">
                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-primary bg-opacity-10 p-3 rounded-circle text-primary">
                                    <i class="fas fa-chalkboard-teacher fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("Perkuliahan Aktif")%></h6>
                                <h4 class="mb-0 fw-bold text-dark"><%=jmlPerkuliahan%></h4>
                            </div>
                        </div>
                    </div>
                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-success bg-opacity-10 p-3 rounded-circle text-success">
                                    <i class="fas fa-calendar-day fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("Kelas Hari Ini")%></h6>
                                <h4 class="mb-0 fw-bold text-dark"><%=jmlHariIni%></h4>
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
                        <h6 class="text-muted small text-uppercase mb-0" style="font-size:.7rem">NIP / NIDN</h6>
                        <div class="fw-bold text-dark"><%=dosen.getNidn() != null && !dosen.getNidn().isEmpty() ? dosen.getNidn() : "-"%></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <%-- Info jadwal hari ini --%>
    <div class="col-xl-6 col-lg-12">
        <div class="card border-0 shadow-sm h-100 rounded-3">
            <div class="card-header bg-white pt-4 pb-3 border-bottom-0 d-flex flex-wrap justify-content-between align-items-center"
                 style="background-image:url('<%=request.getContextPath()%>/img/corner-5.png');background-position:right top;background-repeat:no-repeat;background-size:contain;">
                <div>
                    <h5 class="text-primary fw-bold mb-0"><i class="fas fa-calendar-day me-2"></i><%=Common.getBahasaConfig("Jadwal Hari Ini")%></h5>
                    <small class="text-muted fw-bold"><%=hariIni%> &bull; TA <%=currentTA%></small>
                </div>
                <a href="<%=Common.ROOT%>/baru?p=pagesmasterperkuliahanzul&s=laporan_jadwal_perhari&hari=<%=java.net.URLEncoder.encode(hariIni,"UTF-8")%>&tahunAjaran=<%=java.net.URLEncoder.encode(currentTA,"UTF-8")%>"
                   class="btn btn-outline-primary btn-sm">
                    <i class="fas fa-external-link-alt me-1"></i>Lihat Jadwal Lengkap
                </a>
            </div>
            <div class="card-body">
                <% if (jmlHariIni == 0) { %>
                <div class="d-flex flex-column align-items-center justify-content-center h-100 py-4 text-muted">
                    <i class="fas fa-coffee fa-2x mb-3"></i>
                    <div class="fw-bold"><%=Common.getBahasaConfig("Tidak ada jadwal mengajar hari ini")%></div>
                    <div class="small mt-1"><%=Common.getBahasaConfig("Nikmati waktu luang Anda")%></div>
                </div>
                <% } else { %>
                <div class="alert alert-primary border-0 d-flex align-items-center">
                    <i class="fas fa-chalkboard-teacher fa-2x me-3"></i>
                    <div>
                        <div class="fw-bold"><%=jmlHariIni%> <%=Common.getBahasaConfig("kelas terjadwal hari ini")%></div>
                        <div class="small"><%=Common.getBahasaConfig("Klik Lihat Jadwal Lengkap untuk rincian")%></div>
                    </div>
                </div>
                <a href="<%=Common.ROOT%>/baru?p=pagesmasterperkuliahanzul&s=index" class="btn btn-primary btn-sm me-2">
                    <i class="fas fa-chalkboard me-1"></i><%=Common.getBahasaConfig("Kelola Perkuliahan")%>
                </a>
                <% } %>
                <div class="mt-3">
                    <a href="<%=Common.ROOT%>/baru?p=pagesmasterperkuliahanzul&s=index"
                       class="btn btn-outline-secondary btn-sm">
                        <i class="fas fa-list me-1"></i><%=Common.getBahasaConfig("Semua Perkuliahan")%>
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>
