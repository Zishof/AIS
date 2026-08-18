<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.KelasSiswa"%>

<%
    // --- 1. SETUP & SECURITY ---
    String reqPertemuan = request.getParameter("pertemuan");
    String filter_status = request.getParameter("filter_status");
    String cari = request.getParameter("cari");
    if (reqPertemuan == null || reqPertemuan.isEmpty()) return;

    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.sendRedirect(request.getContextPath() + "/logoff");
        return;
    }

    Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, reqPertemuan, true);
    if (pertemuan == null) return;

    VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
    List<Long> mahasiswas = pembelajaran.ambilMahasiswaById();
    boolean bolehUbah = pertemuan.bolehUbahAbsen(tbmuser);
    
    // --- 2. LOGIKA TERLEWAT (DEADLINE) ---
    if (pertemuan.apakahTerlewat()) {
        Date currentDate = WaktuUtil.getDate();
        int selisih = pertemuan.getTanggal() == null ? 0 : Math.abs(Common.getBetweenTwoDates(currentDate, pertemuan.getTanggal())) - 1;
        int toleransiHari = (pertemuan.getPerkuliahan() != null) ? pertemuan.getPerkuliahan().getBatasWaktuBolehAbsenKehadiran() : 1000;
        
        if (Konfigurasi.AKTIF.equals(Common.getKonfigurasi("jumlah_hari_batas_waktu_pakai_default", Konfigurasi.TIDAK_AKTIF).getNilai())) {
            try {
                toleransiHari = Integer.parseInt(Common.getKonfigurasi("jumlah_hari_batas_waktu_dalam_hari", "0").getNilai().trim());
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/_daftar_peserta_absen_reload_mahasiswa.jsp:46");}
        }
%>
    <div class="alert alert-warning border-0 shadow-sm rounded-4 mb-4 d-flex align-items-center" role="alert">
        <div class="display-6 me-3"><i class="fas fa-exclamation-triangle"></i></div>
        <div>
            <h6 class="alert-heading fw-bold mb-1"><%=Common.getBahasaConfig("Periode Pengisian Berakhir") %></h6>
            <small>
            	<%=Common.getBahasaConfig("Tanggal") %>: <%=Common.dateFormat5.get().format(currentDate)%>. <%=Common.getBahasaConfig("Selisih") %> <strong><%=selisih%> <%=Common.getBahasaConfig("hari") %></strong> <%=Common.getBahasaConfig("dari jadwal")%>.
                <%=Common.getBahasaConfig("Batas toleransi") %>: <strong><%=toleransiHari%> <%=Common.getBahasaConfig("hari") %></strong>.
            </small>
        </div>
    </div>
<%
    }

    // --- 3. LOOP DATA MAHASISWA ---
    if (mahasiswas != null) {
        for (Long mahasiswaid : mahasiswas) {
            Mahasiswa mhs = (Mahasiswa) GeneralValueObject.ambilData(Mahasiswa.class, mahasiswaid.toString(), true);
            if (mhs == null) continue;
            
            if(cari==null||cari.trim().isEmpty() || mhs.getNama().toLowerCase().contains(cari.trim().toLowerCase()) || mhs.getNim().toLowerCase().contains(cari.trim().toLowerCase())){

	            // Variabel Data
	            String linkUbah = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=ubah_status_kehadiran&pertemuan=" + reqPertemuan + "&mahasiswa=" + mhs.getId();
	            String urlFoto = CommonMedia.getUrlFotoPengguna(new Tbmuser(mhs));
	            
	            // Format Waktu
	            String mul = pertemuan.retreiveAbsensiMulai(mhs.getId());
	            String sam = pertemuan.retreiveAbsensiSampai(mhs.getId());
	            String wkt = (!mul.isEmpty()) ? mul + (!sam.isEmpty() ? " s.d " + sam : "") : "";
	
	            // Format Keterangan & Link
	            String rawKet = pertemuan.retreiveAbsensiKeterangan(mhs.getId());
	            String ket = org.apache.commons.lang3.StringUtils.replace(rawKet, "_", ",");
	            ket = (ket.trim().isEmpty()) ? "-" : ket.replaceAll("(https?://\\S+)", "<a target='_blank' class='text-decoration-underline' href=\"$1\">Tautan</a>");
	
	            // Status Absensi
	            Statusabsensi status = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(mhs.getId()));
	            if (status == null) status = ConstantValues.BELUM_ABSEN;
	            
	            if(filter_status==null || filter_status.trim().isEmpty() ||  status.getId().toString().equalsIgnoreCase(filter_status.trim()) ){
	
		            // --- THEME ENGINE (Logika Warna Modern) ---
		            String theme = "secondary"; // Default (Abu-abu)
		            String icon = "fa-user";
		            Long statusId = status.getId();
		
		            if (ConstantValues.MASUK != null && statusId.equals(ConstantValues.MASUK.getId())) {
		                theme = "success"; // Hijau
		                icon = "fa-check-circle";
		            } else if (ConstantValues.SAKIT != null && statusId.equals(ConstantValues.SAKIT.getId())) {
		                theme = "warning"; // Kuning
		                icon = "fa-procedures";
		            } else if (ConstantValues.IZIN != null && statusId.equals(ConstantValues.IZIN.getId())) {
		                theme = "info"; // Biru Muda
		                icon = "fa-envelope-open";
		            } else if (ConstantValues.TIDAK_ADA_ALASAN != null && statusId.equals(ConstantValues.TIDAK_ADA_ALASAN.getId())) {
		                theme = "danger"; // Merah
		                icon = "fa-times-circle";
		            } else if (ConstantValues.BELUM_ABSEN != null && statusId.equals(ConstantValues.BELUM_ABSEN.getId())) {
		                theme = "primary"; // Biru
		                icon = "fa-question-circle";
		            }
		            
		%>
		
		    <div class="card mb-3 border-0 shadow-sm rounded-4 bg-<%=theme%>-subtle overflow-hidden">
		        <div class="card-body p-3">
		            <div class="d-flex align-items-start gap-3">
		                
		                <div class="flex-shrink-0 text-center">
		                    <div class="position-relative d-inline-block">
		                        <img onclick="tampilGambar(this)" 
		                             src="<%=urlFoto %>" 
		                             class="rounded-4 shadow-sm border border-2 border-white" 
		                             alt="<%=mhs.getNama() %>" 
		                             style="width: 85px; height: 85px; object-fit: cover; cursor: pointer;"
		                             title="Klik untuk zoom">
		                        <span class="position-absolute bottom-0 start-100 translate-middle badge rounded-pill bg-<%=theme%> border border-2 border-white p-2">
		                             <i class="fas <%=icon%>"></i>
		                        </span>
		                    </div>
		                </div>
		
		                <div class="flex-grow-1">
		                    <div class="d-flex justify-content-between align-items-start mb-2">
		                        <div>
		                            <h6 class="fw-bold text-dark mb-0" style="font-size: 1.1rem;"><%=mhs.getNama() %></h6>
		                            <small class="text-secondary fw-semibold">
		                                <%=mhs.getNim() %> &bull; <%=mhs.getProgram() %>
		                            </small>
		                        </div>
		                    </div>
		
		                    <div class="bg-white bg-opacity-75 rounded-3 p-2 border border-<%=theme%>-subtle">
		                        <div class="row g-2 align-items-center">
		                            <div class="col-md-4 col-12 border-end border-<%=theme%>-subtle">
		                                <small class="text-uppercase text-muted fw-bold" style="font-size: 0.65rem;">Status</small>
		                                <div class="text-<%=theme%>-emphasis fw-bold">
		                                    <%=Common.getBahasaConfig(status.getNama()) %>
		                                </div>
		                            </div>
		                            <div class="col-md-8 col-12 ps-md-3">
		                                <div class="d-flex justify-content-between">
		                                    <div>
		                                        <small class="text-uppercase text-muted fw-bold" style="font-size: 0.65rem;">Keterangan</small>
		                                        <div class="text-dark small lh-sm text-break"><%=ket %></div>
		                                    </div>
		                                    <% if(!wkt.isEmpty()) { %>
		                                    <div class="text-end ms-2">
		                                        <small class="text-uppercase text-muted fw-bold" style="font-size: 0.65rem;">Waktu</small>
		                                        <div class="badge bg-light text-dark border"><i class="far fa-clock me-1"></i><%=wkt %></div>
		                                    </div>
		                                    <% } %>
		                                </div>
		                            </div>
		                        </div>
		                    </div>
		
		                    <% if(bolehUbah) { %>
		                    <div class="mt-2 text-end">
		                        <button class="btn btn-sm btn-white border border-<%=theme%> text-<%=theme%>-emphasis fw-bold rounded-pill px-3 shadow-sm" 
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
        }
    }

    // --- 4. LOOP DATA SISWA (Sekolah) ---
    List<Long> siswas = pembelajaran.ambilSiswaById();
    if (siswas != null) {
        for (Long siswaid : siswas) {
            Siswa sw = (Siswa) GeneralValueObject.ambilData(Siswa.class, siswaid.toString(), true);
            if (sw == null) continue;

            String swNama = sw.getNama() != null ? sw.getNama() : "-";
            String swNis  = sw.getNomorInduk() != null ? sw.getNomorInduk() : "-";
            KelasSiswa swKelas = sw.getKelas();
            String swKelasNama = (swKelas != null && swKelas.getNama() != null) ? swKelas.getNama() : "";
            String swCariKey = swNama.toLowerCase() + " " + swNis.toLowerCase();

            if (cari != null && !cari.trim().isEmpty() && !swCariKey.contains(cari.trim().toLowerCase())) {
                continue;
            }

            String linkUbahSw = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=ubah_status_kehadiran&pertemuan=" + reqPertemuan + "&siswa=" + sw.getId();
            String urlFotoSw = CommonMedia.getUrlFotoPengguna(new Tbmuser(sw));

            String mulSw = pertemuan.retreiveAbsensiMulai(sw.getId());
            String samSw = pertemuan.retreiveAbsensiSampai(sw.getId());
            String wktSw = (!mulSw.isEmpty()) ? mulSw + (!samSw.isEmpty() ? " s.d " + samSw : "") : "";

            String rawKetSw = pertemuan.retreiveAbsensiKeterangan(sw.getId());
            String ketSw = org.apache.commons.lang3.StringUtils.replace(rawKetSw, "_", ",");
            ketSw = (ketSw.trim().isEmpty()) ? "-" : ketSw.replaceAll("(https?://\\S+)", "<a target='_blank' class='text-decoration-underline' href=\"$1\">Tautan</a>");

            Statusabsensi statusSw = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(sw.getId()));
            if (statusSw == null) statusSw = ConstantValues.BELUM_ABSEN;

            if (filter_status != null && !filter_status.trim().isEmpty() && !statusSw.getId().toString().equalsIgnoreCase(filter_status.trim())) {
                continue;
            }

            String themeSw = "secondary";
            String iconSw  = "fa-user";
            Long statusSwId = statusSw.getId();
            if (ConstantValues.MASUK != null && statusSwId.equals(ConstantValues.MASUK.getId())) {
                themeSw = "success"; iconSw = "fa-check-circle";
            } else if (ConstantValues.SAKIT != null && statusSwId.equals(ConstantValues.SAKIT.getId())) {
                themeSw = "warning"; iconSw = "fa-procedures";
            } else if (ConstantValues.IZIN != null && statusSwId.equals(ConstantValues.IZIN.getId())) {
                themeSw = "info"; iconSw = "fa-envelope-open";
            } else if (ConstantValues.TIDAK_ADA_ALASAN != null && statusSwId.equals(ConstantValues.TIDAK_ADA_ALASAN.getId())) {
                themeSw = "danger"; iconSw = "fa-times-circle";
            } else if (ConstantValues.BELUM_ABSEN != null && statusSwId.equals(ConstantValues.BELUM_ABSEN.getId())) {
                themeSw = "primary"; iconSw = "fa-question-circle";
            }
%>

    <div class="card mb-3 border-0 shadow-sm rounded-4 bg-<%=themeSw%>-subtle overflow-hidden">
        <div class="card-body p-3">
            <div class="d-flex align-items-start gap-3">
                <div class="flex-shrink-0 text-center">
                    <div class="position-relative d-inline-block">
                        <img onclick="tampilGambar(this)"
                             src="<%=urlFotoSw%>"
                             class="rounded-4 shadow-sm border border-2 border-white"
                             alt="<%=swNama%>"
                             style="width: 85px; height: 85px; object-fit: cover; cursor: pointer;"
                             title="Klik untuk zoom">
                        <span class="position-absolute bottom-0 start-100 translate-middle badge rounded-pill bg-<%=themeSw%> border border-2 border-white p-2">
                            <i class="fas <%=iconSw%>"></i>
                        </span>
                    </div>
                </div>
                <div class="flex-grow-1">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            <h6 class="fw-bold text-dark mb-0" style="font-size: 1.1rem;"><%=swNama%></h6>
                            <small class="text-secondary fw-semibold"><%=swNis%><%=!swKelasNama.isEmpty() ? " &bull; " + swKelasNama : ""%></small>
                        </div>
                    </div>
                    <div class="bg-white bg-opacity-75 rounded-3 p-2 border border-<%=themeSw%>-subtle">
                        <div class="row g-2 align-items-center">
                            <div class="col-md-4 col-12 border-end border-<%=themeSw%>-subtle">
                                <small class="text-uppercase text-muted fw-bold" style="font-size: 0.65rem;">Status</small>
                                <div class="text-<%=themeSw%>-emphasis fw-bold"><%=Common.getBahasaConfig(statusSw.getNama())%></div>
                            </div>
                            <div class="col-md-8 col-12 ps-md-3">
                                <div class="d-flex justify-content-between">
                                    <div>
                                        <small class="text-uppercase text-muted fw-bold" style="font-size: 0.65rem;">Keterangan</small>
                                        <div class="text-dark small lh-sm text-break"><%=ketSw%></div>
                                    </div>
                                    <% if(!wktSw.isEmpty()) { %>
                                    <div class="text-end ms-2">
                                        <small class="text-uppercase text-muted fw-bold" style="font-size: 0.65rem;">Waktu</small>
                                        <div class="badge bg-light text-dark border"><i class="far fa-clock me-1"></i><%=wktSw%></div>
                                    </div>
                                    <% } %>
                                </div>
                            </div>
                        </div>
                    </div>
                    <% if(bolehUbah) { %>
                    <div class="mt-2 text-end">
                        <button class="btn btn-sm btn-white border border-<%=themeSw%> text-<%=themeSw%>-emphasis fw-bold rounded-pill px-3 shadow-sm"
                                onclick="var contentModal='contentModal_'+(++variable);loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Ubah Status Kehadiran")%>', '<%=linkUbahSw%>&contentModal='+contentModal, '500px', true, 'simpanDataAbsen'+contentModal+'();',contentModal);">
                            <i class="far fa-edit me-1"></i> <%=Common.getBahasaConfig("Ubah")%>
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