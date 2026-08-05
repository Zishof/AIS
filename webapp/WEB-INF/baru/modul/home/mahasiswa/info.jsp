<%@ page import="java.util.Calendar"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.List"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.CommonMedia"%>
<%@ page import="ais.database.model.*"%>
<%@ page import="ais.database.model.sekolah.*"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    // 1. Inisialisasi & Validasi User
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null || tbmuser.getMahasiswa() == null) {
        return; 
    }

    // 2. Logika Waktu (Java 1.7 Compatible)
    int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    String waktu = (jam < 10) ? "Pagi" : (jam < 15) ? "Siang" : (jam < 18) ? "Sore" : "Malam";

    // 3. Persiapan Data Akademik
    String taa = request.getParameter("ta") == null ? Common.getCurrentTahunAkademik() : request.getParameter("ta");
    Mahasiswa mhs = tbmuser.getMahasiswa();
    Integer smt = mhs.currentSemester();
    KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mhs, smt, null, null);

    // 4. Membangun Info Organisasi (Fakultas -> Jurusan -> Prodi)
    // Menggunakan List untuk menampung data non-null agar tampilan rapi
    List<String> orgInfo = new ArrayList<String>();
    if (tbmuser.ambilFakultas() != null) orgInfo.add(tbmuser.ambilFakultas().getNama());
    if (tbmuser.ambilJurusan() != null) orgInfo.add(tbmuser.ambilJurusan().getNama());
    if (tbmuser.ambilProgram() != null) orgInfo.add(tbmuser.ambilProgram().getNama());
    if (tbmuser.ambilYayasan() != null) orgInfo.add(tbmuser.ambilYayasan().getNama());
    if (tbmuser.ambilSekolah() != null) orgInfo.add(tbmuser.ambilSekolah().getNama());
    
    String subHeader = "";
    if(!orgInfo.isEmpty()){
        // Manual join untuk Java 1.7
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<orgInfo.size(); i++){
            sb.append(orgInfo.get(i));
            if(i < orgInfo.size()-1) sb.append(" &bull; ");
        }
        subHeader = sb.toString();
    }
%>

<div class="row g-4 mb-4 animate__animated animate__fadeInUp">
    
    <div class="col-xl-6 col-lg-12">
        <div class="card border-0 shadow-sm h-100 overflow-hidden rounded-3">
            <div class="card-header border-0 pt-4 pb-3 bg-light" 
                 style="background-image: url('<%=request.getContextPath()%>/img/corner-3.png'); background-position: right top; background-repeat: no-repeat; background-size: contain;">
                
                <h5 class="text-primary fw-bold mb-1">
                    <i class="fas fa-sun me-2 text-warning"></i><%=Common.getBahasaConfig("Selamat")%> <%=Common.getBahasaConfig(waktu)%>, <%=tbmuser.getUserNama()%>
                </h5>
                <div class="text-muted small fw-bold text-uppercase" style="letter-spacing: 0.5px;">
                    <%=subHeader%>
                </div>
            </div>

            <div class="card-body">
                <div class="row g-3">
                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-primary bg-opacity-10 p-3 rounded-circle text-primary">
                                    <i class="fas fa-chess-rook fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("IP Semester")%></h6>
                                <h4 class="mb-0 fw-bold text-dark"><%=Common.numberFormat.get().format(krs.getIps())%></h4>
                            </div>
                        </div>
                    </div>

                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-warning bg-opacity-10 p-3 rounded-circle text-warning">
                                    <i class="fas fa-crown fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("IP Kumulatif")%></h6>
                                <h4 class="mb-0 fw-bold text-dark"><%=Common.numberFormat.get().format(krs.getIpk())%></h4>
                            </div>
                        </div>
                    </div>

                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-success bg-opacity-10 p-3 rounded-circle text-success">
                                    <i class="fas fa-video fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("SKS Sem")%></h6>
                                <h4 class="mb-0 fw-bold text-dark"><%=Common.numberFormat.get().format(krs.getSksYangDiambil())%></h4>
                            </div>
                        </div>
                    </div>

                    <div class="col-sm-6">
                        <div class="d-flex align-items-center p-3 border rounded bg-white h-100 shadow-sm">
                            <div class="flex-shrink-0 me-3">
                                <div class="bg-info bg-opacity-10 p-3 rounded-circle text-info">
                                    <i class="fas fa-user-graduate fa-lg"></i>
                                </div>
                            </div>
                            <div>
                                <h6 class="text-muted small text-uppercase mb-1"><%=Common.getBahasaConfig("SKS Total")%></h6>
                                <h4 class="mb-0 fw-bold text-dark"><%=Common.numberFormat.get().format(krs.getSksk())%></h4>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="card-footer bg-white border-top-0 pb-3">
                <div class="d-flex align-items-center p-2 border rounded bg-light shadow-sm">
                    <div class="flex-shrink-0 me-3">
                        <div class="bg-secondary bg-opacity-10 p-2 rounded-circle text-secondary">
                            <i class="fas fa-clock"></i>
                        </div>
                    </div>
                    <div class="flex-grow-1">
                        <h6 class="text-muted small text-uppercase mb-0" style="font-size: 0.7rem;">
                            <%=Common.getBahasaConfig("Masa Studi")%>
                        </h6>
                        <div class="fw-bold text-dark text-wrap" style="line-height: 1.2;">
                            <%=mhs.ambilMasaStudi()%>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="col-xl-6 col-lg-12">
        <div class="card border-0 shadow-sm h-100 rounded-3">
            <div class="card-header bg-white pt-4 pb-3 border-bottom-0 d-flex flex-wrap justify-content-between align-items-center"
                 style="background-image: url('<%=request.getContextPath()%>/img/corner-5.png'); background-position: right top; background-repeat: no-repeat; background-size: contain;">
                
                <div class="mb-2 mb-md-0">
                    <h5 class="text-primary fw-bold mb-0"><%=Common.getBahasaConfig("Perkuliahan") %></h5>
                    <small class="text-muted fw-bold">TA <%=krs.getTahunAkademik()%> / Sem. <%=krs.getSemester()%></small>
                </div>

                <form action="<%=request.getContextPath()%>/baru" method="GET" class="d-inline-block">
                    <% 
                    // Menjaga parameter query string lain (selain 'ta') agar tidak hilang
                    java.util.Enumeration paramNames = request.getParameterNames();
                    while(paramNames.hasMoreElements()) {
                        String paramName = (String)paramNames.nextElement();
                        if(!"ta".equals(paramName)) {
                    %>
                        <input type="hidden" name="<%=paramName%>" value="<%=request.getParameter(paramName)%>">
                    <% 
                        }
                    } 
                    %>
                    
                    <select name="ta" onchange="this.form.submit();" class="form-select form-select-sm border-primary shadow-none fw-bold text-primary" style="width: auto; min-width: 150px;">
                        <option value="" <%=taa == null || taa.equals("") ? "selected" : ""%>><%=Common.getBahasaConfig("Semua Periode") %></option>
                        <% for (String ta : Common.tahunAngkatans) { %>
                            <option value="<%=ta%>" <%=taa != null && taa.equals(ta) ? "selected" : ""%>><%=ta%></option>
                        <% } %>
                    </select>
                </form>
            </div>

            <div class="card-body">
                <div class="alert alert-primary d-flex align-items-center border-0 shadow-sm mb-3" role="alert">
                    <i class="fas fa-info-circle fa-2x me-3"></i>
                    <div>
                        <div class="small text-uppercase text-primary-emphasis fw-bold"><%=Common.getBahasaConfig("Status Pengambilan KRS") %></div>
                        <div class="fw-bold"><%=mhs.rubahKeteranganPengambilanKRS(krs.getSemester(), krs.getTahapan(), krs.getSemesterPendek(), krs, false)%></div>
                    </div>
                </div>

                <% if(krs.getCatatan() != null && !krs.getCatatan().isEmpty()) { %>
                <div class="alert alert-warning d-flex align-items-start border-0 shadow-sm mb-2" role="alert">
                    <i class="fas fa-sticky-note me-2 mt-1"></i>
                    <div>
                        <strong><%=Common.getBahasaConfig("Catatan KRS") %>:</strong> <%=krs.getCatatan()%>
                    </div>
                </div>
                <% } %>

                <% if(krs.getCatatanKhs() != null && !krs.getCatatanKhs().isEmpty()) { %>
                <div class="alert alert-info d-flex align-items-start border-0 shadow-sm" role="alert">
                    <i class="fas fa-clipboard-list me-2 mt-1"></i>
                    <div>
                        <strong><%=Common.getBahasaConfig("Catatan KHS") %>:</strong> <%=krs.getCatatanKhs()%>
                    </div>
                </div>
                <% } %>
            </div>
        </div>
    </div>
</div>