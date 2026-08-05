<%@page import="ais.database.model.sekolah.KelasSiswa"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.Perkuliahan"%>

<%
    // 1. Validasi & Inisialisasi Data
    String clazzParam = request.getParameter("clazz");
    String idParam = request.getParameter("id");
    
    if (idParam == null) return; 

    Class clazz = (clazzParam == null || clazzParam.isEmpty()) ? Perkuliahan.class : Class.forName(clazzParam);
    VOPembelajaran pembelajaran = (VOPembelajaran) GeneralValueObject.ambilData(clazz, idParam, true);
    
    if (pembelajaran == null) return;

    List<Long> mahasiswas = pembelajaran.ambilMahasiswaById();
    List<Long> siswas = pembelajaran.ambilSiswaById();
    
    boolean isMahasiswa = !mahasiswas.isEmpty();
    boolean isSiswa = !siswas.isEmpty();
    boolean adaData = isMahasiswa || isSiswa;
%>

<% if (adaData) { %>
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden animate__animated animate__fadeInUp">
        <div class="card-header bg-primary bg-gradient text-white p-3 border-0">
            <div class="d-flex align-items-center">
                <i class="fas fa-chalkboard-teacher me-2 fs-4"></i>
                <h6 class="mb-0 fw-bold text-uppercase"><%=pembelajaran.infoSimple() %></h6>
            </div>
        </div>

        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover table-borderless align-middle mb-0">
                    <thead class="bg-white text-secondary small text-uppercase fw-bold border-bottom">
                        <tr>
                            <th class="ps-4" style="width: 80px;"><%=Common.getBahasaConfig("Foto") %></th>
                            <th><%= isMahasiswa ? Common.getBahasaConfig("NIM") : Common.getBahasaConfig("NIS") %></th>
                            <th><%=Common.getBahasaConfig("Nama") %></th>
                            <th><%=Common.getBahasaConfig("Thn Masuk") %></th>
                            <th class="pe-4 text-end"><%=Common.getBahasaConfig("Kelas") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                        // --- LOGIKA LOOP MAHASISWA ---
                        if (isMahasiswa) {
                            int i = 0; // Inisialisasi Counter
                            for (Long mid : mahasiswas) {
                                Mahasiswa m = (Mahasiswa) GeneralValueObject.ambilData(Mahasiswa.class, mid.toString(), true);
                                if (m == null) continue;
                                
                                String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(m));
                                // Logika Ganjil (Odd) / Genap (Even)
                                // Jika Genap: Putih, Jika Ganjil: Abu-abu muda (bg-light)
                                String rowClass = (i % 2 == 0) ? "bg-white" : "bg-light"; 
                        %>
                        <tr class="<%=rowClass%>">
                            <td class="ps-4">
                                <img onclick="tampilGambar(this)" 
                                     src="<%=url%>" 
                                     class="rounded-circle shadow-sm border border-2 border-white" 
                                     style="width: 45px; height: 45px; object-fit: cover; cursor: pointer;" 
                                     title="<%=m.getNama()%>"
                                     alt="Foto">
                            </td>
                            <td class="fw-bold text-primary"><%=m.getNim()%></td>
                            <td class="fw-semibold text-dark"><%=m.getNama()%></td>
                            <td class="text-muted"><%=m.getTahunangkatan()%></td>
                            <td class="pe-4 text-end">
                                <span class="badge bg-white text-dark border shadow-sm"><%=m.getKelas()%></span>
                            </td>
                        </tr>
                        <% 
                                i++; // Increment Counter
                            } 
                        } 
                        // --- LOGIKA LOOP SISWA ---
                        else if (isSiswa) {
                            int i = 0; // Inisialisasi Counter
                            for (Long sid : siswas) {
                                Siswa s = (Siswa) GeneralValueObject.ambilData(Siswa.class, sid.toString(), true);
                                if (s == null) continue;
                                
                                KelasSiswa ks = s.getKelas();
                                String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(s));
                                // Logika Ganjil (Odd) / Genap (Even)
                                String rowClass = (i % 2 == 0) ? "bg-white" : "bg-light"; 
                        %>
                        <tr class="<%=rowClass%>">
                            <td class="ps-4">
                                <img onclick="tampilGambar(this)" 
                                     src="<%=url%>" 
                                     class="rounded-circle shadow-sm border border-2 border-white" 
                                     style="width: 45px; height: 45px; object-fit: cover; cursor: pointer;" 
                                     title="<%=s.getNama()%>"
                                     alt="Foto">
                            </td>
                            <td class="fw-bold text-primary"><%=s.getNomorInduk()%></td>
                            <td class="fw-semibold text-dark"><%=s.getNama()%></td>
                            <td class="text-muted"><%=s.getTahunMasuk()%></td>
                            <td class="pe-4 text-end">
                                <span class="badge bg-white text-dark border shadow-sm"><%=ks != null ? ks.getNama() : "-"%></span>
                            </td>
                        </tr>
                        <% 
                                i++; // Increment Counter
                            } 
                        } 
                        %>
                    </tbody>
                </table>
            </div>
        </div>
        
        <div class="card-footer bg-white border-top p-3 text-end">
            <small class="text-muted">
                <%=Common.getBahasaConfig("Total") %>: 
                <span class="fw-bold text-dark"><%= isMahasiswa ? mahasiswas.size() : siswas.size() %></span> 
                <%= isMahasiswa ? Common.getBahasaConfig("Mahasiswa") : Common.getBahasaConfig("Siswa") %>
            </small>
        </div>
    </div>

<% } else { %>

 	<jsp:include page="/WEB-INF/baru/componen/tidak_ketemu.jsp">
                    <jsp:param name="pesan" value="Belum ada data mahasiswa atau siswa yang terdaftar pada pertemuan/kelas ini." />
     </jsp:include>
<% } %>