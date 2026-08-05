<%@page import="java.util.Collection"%>
<%@page import="java.util.Date"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.PengajuanIzinTidakMasukPerkuliahan"%>
<%@page import="ais.database.model.Statusabsensi"%>

<%
    // --- 1. SETUP & SECURITY ---
    String reqPertemuan = request.getParameter("pertemuan");
    if (reqPertemuan == null || reqPertemuan.isEmpty()) return;

    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(request.getContextPath() + "/logoff");
        return;
    }

    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, reqPertemuan, true);
    if (pertemuan == null) return;

    boolean bolehUbah = pertemuan.bolehUbahAbsenSaja(tbmuser);
    Collection<PengajuanIzinTidakMasukPerkuliahan> listPengajuan = pertemuan.ambilPengajuanIzinTidakMasukPerkuliahanTotal();

    // --- 2. RENDER DATA ---
    if (listPengajuan != null && !listPengajuan.isEmpty()) {
        for (PengajuanIzinTidakMasukPerkuliahan izin : listPengajuan) {
            Mahasiswa mhs = izin.getMahasiswa();
            Statusabsensi status = izin.getStatusabsensi();

            if (mhs == null || status == null) continue;

            // Variabel Data
            String linkPengajuan = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=pengajuan_izin_atau_sakit&pertemuan=" + reqPertemuan + "&id=" + izin.getId();
            String urlFoto = CommonMedia.getUrlFotoPengguna(new Tbmuser(mhs));
            String namaStatus = Common.getBahasaConfig(status.getNama());
            String ket = (izin.getKeterangan() != null && !izin.getKeterangan().isEmpty()) ? izin.getKeterangan() : "-";
            Long statusId = status.getId();

            // --- 3. LOGIKA WARNA (THEMING) ---
            // Kita tentukan satu 'base color' bootstrap, sisanya mengikuti otomatis
            String themeColor = "warning"; // Default (Abu-abu)
            String iconStatus = "fa-question-circle";
            
            

            if (ConstantValues.MASUK != null && statusId.equals(ConstantValues.MASUK.getId())) {
                iconStatus = "fa-check-circle";
            } else if (ConstantValues.SAKIT != null && statusId.equals(ConstantValues.SAKIT.getId())) {
                iconStatus = "fa-procedures";
            } else if (ConstantValues.IZIN != null && statusId.equals(ConstantValues.IZIN.getId())) {
                iconStatus = "fa-envelope-open-text";
            } else if (ConstantValues.TIDAK_ADA_ALASAN != null && statusId.equals(ConstantValues.TIDAK_ADA_ALASAN.getId())) {
                iconStatus = "fa-times-circle";
            } else if (ConstantValues.BELUM_ABSEN != null && statusId.equals(ConstantValues.BELUM_ABSEN.getId())) {
                iconStatus = "fa-clock";
            }
            
            if(izin.getDiizinkan()) {
            	themeColor = "success"; // Hijau
            }
%>

<div class="card mb-3 border-0 shadow-sm rounded-4 bg-<%=themeColor%>-subtle">
    <div class="card-body p-3">
        <div class="d-flex align-items-start gap-3">
            
            <div class="flex-shrink-0">
                <div class="position-relative">
                    <img onclick="tampilGambar(this)" 
                         src="<%=urlFoto %>" 
                         class="rounded-4 border border-2 border-<%=themeColor%> shadow-sm bg-white" 
                         alt="<%=mhs.getNama() %>" 
                         style="width: 75px; height: 75px; object-fit: cover; cursor: pointer;"
                         title="Lihat Foto">
                    <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-<%=themeColor%> border border-light">
                         <i class="fas <%=iconStatus%>"></i>
                    </span>
                </div>
            </div>

            <div class="flex-grow-1">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h6 class="fw-bold text-dark mb-1"><%=mhs.getNama() %></h6>
                        <div class="text-secondary small d-flex flex-wrap gap-2">
                            <span class="badge bg-white text-dark border border-<%=themeColor%>-subtle"><%=mhs.getNim() %></span>
                            <span class="border-start border-secondary mx-1"></span>
                            <span><%=mhs.getProgram() %></span>
                        </div>
                    </div>
                </div>

                <div class="mt-2 p-2 rounded-3 bg-white bg-opacity-75 border border-<%=themeColor%>-subtle">
                    <div class="row g-1">
                        <div class="col-md-4 col-12">
                            <small class="text-muted d-block text-uppercase" style="font-size: 0.65rem; font-weight: 700;"><%=Common.getBahasaConfig("Status") %></small>
                            <span class="text-<%=themeColor%>-emphasis fw-bold"><i class="fas <%=iconStatus%> me-1"></i><%=namaStatus %></span>
                        </div>
                        <div class="col-md-8 col-12 border-start-md border-<%=themeColor%>-subtle ps-md-2">
                            <small class="text-muted d-block text-uppercase" style="font-size: 0.65rem; font-weight: 700;"><%=Common.getBahasaConfig("Keterangan") %></small>
                            <span class="fst-italic text-dark small"><%=ket %></span>
                        </div>
                    </div>
                </div>

                <div class="d-flex justify-content-between align-items-center mt-2">
                    <div>
                        <% if(izin.getDiizinkan()) { %>
                            <span class="badge rounded-pill bg-success text-white px-2 py-1 shadow-sm">
                                <i class="fas fa-check me-1"></i> <%=Common.getBahasaConfig("Disetujui") %>
                            </span>
                        <% } else { %>
                            <span class="badge rounded-pill bg-secondary text-white px-2 py-1 shadow-sm">
                                <i class="fas fa-hourglass-half me-1"></i> <%=Common.getBahasaConfig("Menunggu") %>
                            </span>
                        <% } %>
                    </div>

                    <% if(bolehUbah) { %>
                    <div>
                        <button class="btn btn-sm btn-white border border-<%=themeColor%> text-<%=themeColor%>-emphasis hover-shadow fw-bold rounded-pill px-3" 
                                onclick="var contentModal='contentModal_'+(++variable);loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ubah Status Izin") %>', '<%=linkPengajuan%>&contentModal='+contentModal, '800px', true, 'saveData_'+contentModal+'();reloadAbsen<%=pertemuan.getId()%>(true);',contentModal);">
                            <i class="far fa-edit me-1"></i> <%=Common.getBahasaConfig("Ubah") %>
                        </button>
                        <% if(!izin.getDiizinkan()) { %>
	                        <button type="button" class="btn btn-sm btn-danger rounded-pill hover-shadow fw-bold rounded-pill px-3" 
	                                            onclick="prosesDeleteData('<%=PengajuanIzinTidakMasukPerkuliahan.class.getName() %>', '<%=izin.getId()%>', function(){ reloadAbsen<%=pertemuan.getId()%>(true); });">
	                             <i class="fas fa-trash-alt me-1"></i> <%=Common.getBahasaConfig("Hapus") %>
	                        </button>
                        <% } %>
                    </div>
                    <% } %>
                </div>

            </div>
        </div>
    </div>
</div>

<%
        }
    }
%>