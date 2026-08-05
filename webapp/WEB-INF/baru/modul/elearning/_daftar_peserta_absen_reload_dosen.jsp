<%@page import="java.util.List"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="ais.database.model.VOPembelajaran"%>

<%
    // --- 1. SETUP & VALIDASI ---
    String reqPertemuan = request.getParameter("pertemuan");
    if (reqPertemuan == null || reqPertemuan.isEmpty()) return;

    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(request.getContextPath() + "/logoff");
        return;
    }

    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, reqPertemuan, true);
    if (pertemuan == null) return;

    VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
    // Menggunakan populateDosenBuId sesuai kode asli (meski terlihat typo, kita pertahankan agar tidak error)
    List<Long> dosens = pembelajaran.populateDosenBuId(); 
    boolean bolehUbah = pertemuan.bolehUbahAbsen(tbmuser);

    // --- 2. LOOP DATA DOSEN ---
    if (dosens != null && !dosens.isEmpty()) {
        for (Long dosenid : dosens) {
            Dosen dosen = (Dosen) GeneralValueObject.ambilData(Dosen.class, dosenid.toString(), true);
            if (dosen == null) continue;

            // --- Persiapan Data ---
            String linkUbah = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=ubah_status_kehadiran&pertemuan=" + reqPertemuan + "&dosen=" + dosen.getId();
            String urlFoto = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen));
            
            // Format Waktu
            String mul = pertemuan.retreiveAbsensiMulai(dosen.getId());
            String sam = pertemuan.retreiveAbsensiSampai(dosen.getId());
            String wkt = (!mul.isEmpty()) ? mul + (!sam.isEmpty() ? " s.d " + sam : "") : "";

            // Format Keterangan
            String rawKet = pertemuan.retreiveAbsensiKeterangan(dosen.getId());
            String ket = org.apache.commons.lang3.StringUtils.replace(rawKet, "_", ",");
            ket = (ket.trim().isEmpty()) ? "-" : ket.replaceAll("(https?://\\S+)", "<a target='_blank' class='text-decoration-underline' href=\"$1\">Tautan</a>");

            // Status Absensi
            Statusabsensi status = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(dosen.getId()));
            if (status == null) status = ConstantValues.BELUM_ABSEN;

            // --- THEME ENGINE (Warna & Icon Otomatis) ---
            String theme = "secondary"; // Default (Abu-abu)
            String icon = "fa-user-tie";
            Long statusId = status.getId();

            if (ConstantValues.MASUK != null && statusId.equals(ConstantValues.MASUK.getId())) {
                theme = "success"; // Hijau
                icon = "fa-chalkboard-teacher";
            } else if (ConstantValues.SAKIT != null && statusId.equals(ConstantValues.SAKIT.getId())) {
                theme = "warning"; // Kuning
                icon = "fa-procedures";
            } else if (ConstantValues.IZIN != null && statusId.equals(ConstantValues.IZIN.getId())) {
                theme = "info"; // Biru Muda
                icon = "fa-envelope-open-text";
            } else if (ConstantValues.TIDAK_ADA_ALASAN != null && statusId.equals(ConstantValues.TIDAK_ADA_ALASAN.getId())) {
                theme = "danger"; // Merah
                icon = "fa-times-circle";
            } else if (ConstantValues.BELUM_ABSEN != null && statusId.equals(ConstantValues.BELUM_ABSEN.getId())) {
                theme = "primary"; // Biru Utama
                icon = "fa-clock";
            }
%>

<div class="card mb-3 border-0 shadow-sm rounded-4 bg-<%=theme%>-subtle overflow-hidden">
    <div class="card-body p-3">
        <div class="d-flex align-items-start gap-3">
            
            <div class="flex-shrink-0 text-center">
                <div class="position-relative d-inline-block">
                    <img onclick="tampilGambar(this)" 
                         src="<%=urlFoto %>" 
                         class="rounded-circle shadow-sm border border-2 border-white" 
                         alt="<%=dosen.getNama() %>" 
                         style="width: 80px; height: 80px; object-fit: cover; cursor: pointer;"
                         title="Lihat Foto">
                    <span class="position-absolute bottom-0 end-0 translate-middle-x badge rounded-pill bg-<%=theme%> border border-white">
                         <i class="fas <%=icon%>"></i>
                    </span>
                </div>
            </div>

            <div class="flex-grow-1">
                <div class="mb-2">
                    <h5 class="fw-bold text-dark mb-0"><%=dosen.getNama() %></h5>
                    <span class="badge bg-white text-secondary border border-secondary-subtle mt-1">
                        <i class="fas fa-id-badge me-1"></i><%=Common.getBahasaConfig("DOSEN / PENGAJAR") %>
                    </span>
                </div>

                <div class="bg-white bg-opacity-75 rounded-3 p-2 border border-<%=theme%>-subtle">
                    <div class="row g-2">
                        <div class="col-md-5 col-12 border-end border-<%=theme%>-subtle">
                            <small class="text-uppercase text-muted fw-bold" style="font-size: 0.65rem;"><%=Common.getBahasaConfig("Status Kehadiran") %></small>
                            <div class="text-<%=theme%>-emphasis fw-bold">
                                <%=Common.getBahasaConfig(status.getNama()) %>
                            </div>
                        </div>
                        
                        <div class="col-md-7 col-12 ps-md-3">
                            <div class="d-flex flex-column h-100 justify-content-center">
                                <% if(!wkt.isEmpty()) { %>
                                    <div class="mb-1">
                                        <small class="badge bg-light text-dark border"><i class="far fa-clock me-1"></i><%=wkt %></small>
                                    </div>
                                <% } %>
                                <div>
                                    <small class="text-muted fst-italic"><%=ket %></small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <% if(bolehUbah) { %>
                <div class="mt-2 text-end">
                    <button class="btn btn-sm btn-white border border-<%=theme%> text-<%=theme%>-emphasis fw-bold rounded-pill px-4 shadow-sm hover-shadow" 
                            onclick="var contentModal='contentModal_'+(++variable);loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ubah Status Kehadiran") %>', '<%=linkUbah%>&contentModal='+contentModal, '500px', true, 'simpanDataAbsen'+contentModal+'();',contentModal);">
                        <i class="far fa-edit me-1"></i> <%=Common.getBahasaConfig("Ubah") %>
                    </button>
                </div>
                <% } %>
            </div>
        </div>
    </div>
</div>

<%
        }
    }
%>