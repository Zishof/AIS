<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Criterion"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
// Pengecekan Sesi Pengguna
Tbmuser tbmuser = Common.getCurrentUser(request);
if(tbmuser != null && tbmuser.getPedagang() != null) {
    out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Akses Ditolak") + "</h5><p>" + Common.getBahasaConfig("Halaman ini tidak boleh diakses oleh pedagang.") + "</p></div>");
    return;
}
AnggotaKoperasi curentAnggota = tbmuser == null ? null : tbmuser.getAnggotaKoperasi();

boolean aktifkanTopupDiAnggota = Common.getKonfigurasi("aktifkan_topup_di_anggota", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);

if(Common.getKonfigurasi("jika_pengguna_login_secara_otomatis_jadi_anggota", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF)){
//Logika sinkronisasi/pembuatan otomatis Anggota Koperasi jika user login belum punya data anggota
	if(tbmuser != null && curentAnggota == null){
		
		Session mySession = null;
		try {
			mySession = HibernateUtil.openSession();
			
			Criterion criterion = Restrictions.sqlRestriction("false");
			if(tbmuser.getMahasiswa()!=null){
				criterion = Restrictions.eq("mahasiswa", tbmuser.getMahasiswa());
			} else if(tbmuser.getSiswa()!=null){
				criterion = Restrictions.eq("siswa", tbmuser.getSiswa());
			} else if(tbmuser.getDosen()!=null){
				criterion = Restrictions.eq("dosen", tbmuser.getDosen());
			} else if(tbmuser.getGuru()!=null){
				criterion = Restrictions.eq("guru", tbmuser.getGuru());
			} else if(tbmuser.getPegawai()!=null){
				criterion = Restrictions.eq("pegawai", tbmuser.getPegawai());
			} else if(tbmuser.getMahasiswa()==null && tbmuser.getSiswa()==null){
				criterion = Restrictions.eq("tbmuser", tbmuser);
			} 
			
			curentAnggota = (AnggotaKoperasi) mySession.createCriteria(AnggotaKoperasi.class)
					.add(criterion)
					.setMaxResults(1).uniqueResult();
					
			boolean baru = false;
			if(curentAnggota==null){
				curentAnggota = new AnggotaKoperasi();
				baru = true;
			}
			curentAnggota.setAktif(true);
			curentAnggota.setTbmuser(tbmuser);
			curentAnggota.setMahasiswa(tbmuser.getMahasiswa());
			curentAnggota.setSiswa(tbmuser.getSiswa());
			curentAnggota.setGuru(tbmuser.getGuru());
			curentAnggota.setDosen(tbmuser.getDosen());
			curentAnggota.setPegawai(tbmuser.getPegawai());
			curentAnggota.setUserid(tbmuser.getUserId());
			
			try {
				curentAnggota.setPass(Common.desEncrypter.get().decrypt(tbmuser.getUserPassword())); 
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/beranda.jsp:64");
				// Abaikan jika dekripsi gagal
			}
		
			if(baru){
				curentAnggota.setKode(curentAnggota.generateKodeMember(mySession, curentAnggota.getTanggal())); 
			}
		
			mySession.getTransaction().begin();
			mySession.saveOrUpdate(curentAnggota);
			tbmuser.setAnggotaKoperasi(curentAnggota);
			mySession.update(tbmuser);
			
			mySession.getTransaction().commit();
			request.getSession(true).setAttribute("mytbmuser", tbmuser);
			request.getSession(true).setAttribute("usersTemp", tbmuser);
		} catch(Exception ee) {
			if (mySession != null && mySession.getTransaction().isActive()) {
				mySession.getTransaction().rollback();
			}
			ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit webapp/WEB-INF/baru/modul/kantin/beranda.jsp:84"); 
		} finally {
			if (mySession != null) {
				try { mySession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/beranda.jsp:87");}
				try { mySession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/beranda.jsp:88");}
			}
			HibernateUtil.closeSessionQuietly(mySession);
		}
	}
}
if(curentAnggota == null) {
    out.print("<div class='text-center p-5'><i class='fas fa-exclamation-triangle fa-3x text-warning mb-3'></i><br><h5 class='text-muted'>" + Common.getBahasaConfig("Akses Ditolak") + "</h5><p>" + Common.getBahasaConfig("Halaman ini hanya dapat diakses oleh Anggota Koperasi yang sah.") + "</p></div>");
    return;
}

String rnd = Common.getGeneratedBarCode(7);
Long idMemberAktif = curentAnggota.getId();
String namaMember = curentAnggota.getNama();
String urlFotoDB = CommonMedia.getUrlFotoPengguna(tbmuser);
String linkFoto = (urlFotoDB == null || urlFotoDB.trim().isEmpty()) 
                ? request.getContextPath() + "/component/uiux/assets/img/team/avatar.png" 
                : urlFotoDB;
%>

<style>
    /* CSS Khusus untuk mengatasi padding berlebih di versi Mobile (Menghilangkan ruang kosong samping) */
    @media (max-width: 768px) {
        .parent-mobile-stretch-<%=rnd%> {
            padding-left: 0 !important;
            padding-right: 0 !important;
            overflow-x: hidden;
        }
        /* Menghilangkan padding pada card pembungkus sub-tab (Belanja, Riwayat, VA) */
        #mainTabContent<%=rnd%> > .tab-pane > .card {
            border-radius: 0 !important;
            border-left: 0 !important;
            border-right: 0 !important;
        }
        #mainTabContent<%=rnd%> > .tab-pane > .card > .card-body {
            padding-left: 4px !important;
            padding-right: 4px !important;
            padding-top: 10px !important;
        }
        /* Penyesuaian header nav pills agar tidak terlalu mepet tepi layar tapi tetap efisien */
        #mainTab<%=rnd%> {
            margin-left: 8px;
            margin-right: 8px;
        }
    }
</style>

<div class="container-fluid py-3 py-md-4 pb-5 mb-5 parent-mobile-stretch-<%=rnd%> animate__animated animate__fadeIn" style="background-color: #f4f7f6; min-height: 100vh; padding-bottom: 120px !important;">

    <ul class="nav nav-pills nav-justified mb-4 shadow-sm rounded-pill p-1 bg-white" id="mainTab<%=rnd%>" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active rounded-pill fw-bold" id="tab-belanja-<%=rnd%>" data-bs-toggle="pill" data-bs-target="#pane-belanja-<%=rnd%>" type="button" role="tab">
                <i class="fas fa-shopping-bag me-1 d-md-none"></i><span class="d-none d-md-inline"><i class="fas fa-shopping-bag me-2"></i></span><%=Common.getBahasaConfig("Belanja Online")%>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-riwayat-<%=rnd%>" data-bs-toggle="pill" data-bs-target="#pane-riwayat-<%=rnd%>" type="button" role="tab">
                <i class="fas fa-history me-1 d-md-none"></i><span class="d-none d-md-inline"><i class="fas fa-history me-2"></i></span><%=Common.getBahasaConfig("Riwayat")%>
            </button>
        </li>
        <%
		if(aktifkanTopupDiAnggota){
		%>
        <li class="nav-item" role="presentation">
            <button class="nav-link rounded-pill fw-bold" id="tab-va-<%=rnd%>" data-bs-toggle="pill" data-bs-target="#pane-va-<%=rnd%>" type="button" role="tab">
                <i class="fas fa-file-invoice-dollar me-1 d-md-none"></i><span class="d-none d-md-inline"><i class="fas fa-file-invoice-dollar me-2"></i></span><%=Common.getBahasaConfig("Nomor VA")%>
            </button>
        </li>
        <%
		}
        %>
    </ul>

    <div class="tab-content" id="mainTabContent<%=rnd%>">
        
        <div class="tab-pane fade show active" id="pane-belanja-<%=rnd%>" role="tabpanel" tabindex="0">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 text-center">
                    <jsp:include page="/WEB-INF/baru/modul/kantin/_beranda_anggota.jsp" />
                </div>
            </div>
        </div>

        <div class="tab-pane fade" id="pane-riwayat-<%=rnd%>" role="tabpanel" tabindex="0">
            <div class="card border-0 shadow-sm rounded-4 border-top border-info border-3">
                <div class="card-body p-3 p-md-4">
                    
                    <ul class="nav nav-tabs mb-4 border-bottom-0 flex-nowrap overflow-auto" id="subTabRiwayat<%=rnd%>" role="tablist" style="scrollbar-width: none;">
                    	<li class="nav-item" role="presentation">
                            <button class="nav-link active fw-bold text-nowrap border-bottom border-3 border-primary text-primary" id="subtab-draft_riwayat_transaksi-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#subpane-draft_riwayat_transaksi-<%=rnd%>" type="button" role="tab">
                                <i class="fas fa-receipt me-1"></i> <%=Common.getBahasaConfig("Pesanan")%>
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link fw-bold text-secondary text-nowrap border-0" id="subtab-transaksi-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#subpane-transaksi-<%=rnd%>" type="button" role="tab">
                                <i class="fas fa-receipt me-1"></i> <%=Common.getBahasaConfig("Transaksi")%>
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link fw-bold text-secondary text-nowrap border-0" id="subtab-barang-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#subpane-barang-<%=rnd%>" type="button" role="tab">
                                <i class="fas fa-box-open me-1"></i> <%=Common.getBahasaConfig("Barang Dibeli")%>
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link fw-bold text-secondary text-nowrap border-0" id="subtab-dashboard-<%=rnd%>" data-bs-toggle="tab" data-bs-target="#subpane-dashboard-<%=rnd%>" type="button" role="tab">
                                <i class="fas fa-chart-pie me-1"></i> <%=Common.getBahasaConfig("Dashboard")%>
                            </button>
                        </li>
                    </ul>

                    <div class="tab-content" id="subTabRiwayatContent<%=rnd%>">
                        <div class="tab-pane fade show active" id="subpane-draft_riwayat_transaksi-<%=rnd%>" role="tabpanel" tabindex="0">
                            <jsp:include page="/WEB-INF/baru/modul/kantin/member/_draft_riwayat_transaksi_member.jsp" />
                        </div>
                        <div class="tab-pane fade" id="subpane-transaksi-<%=rnd%>" role="tabpanel" tabindex="0">
                            <jsp:include page="/WEB-INF/baru/modul/kantin/member/_riwayar_transaksi_member.jsp" />
                        </div>

                        <div class="tab-pane fade" id="subpane-barang-<%=rnd%>" role="tabpanel" tabindex="0">
                            <jsp:include page="/WEB-INF/baru/modul/kantin/member/_riwayar_barang_yang_dibeli.jsp" />
                        </div>

                        <div class="tab-pane fade" id="subpane-dashboard-<%=rnd%>" role="tabpanel" tabindex="0">
                            <jsp:include page="/WEB-INF/baru/modul/kantin/member/_dashboard_member.jsp" />
                        </div>

                    </div>
                </div>
            </div>
        </div>
		<%
		if(aktifkanTopupDiAnggota){
		%>
        <div class="tab-pane fade" id="pane-va-<%=rnd%>" role="tabpanel" tabindex="0">
            <jsp:include page="/WEB-INF/baru/modul/kantin/member/_transaksi_va.jsp" />
        </div>
		<%
		}
        %>
    </div>

</div>

<script>
    document.addEventListener("DOMContentLoaded", () => {
        const subTabs = document.querySelectorAll('#subTabRiwayat<%=rnd%> .nav-link');
        subTabs.forEach(tab => {
            tab.addEventListener('show.bs.tab', function (e) {
                // Menghapus styling khusus dari tab sebelumnya
                subTabs.forEach(t => {
                    t.classList.remove('border-bottom', 'border-3', 'border-primary', 'text-primary');
                    t.classList.add('text-secondary', 'border-0');
                });
                // Menambahkan styling khusus untuk tab yang aktif
                e.target.classList.remove('text-secondary', 'border-0');
                e.target.classList.add('border-bottom', 'border-3', 'border-primary', 'text-primary');
            });
        });

        // Listener khusus agar saat tab utama "Riwayat" diklik, sub-tab "Pesanan" otomatis aktif
        const tabRiwayatMain = document.getElementById('tab-riwayat-<%=rnd%>');
        if (tabRiwayatMain) {
            tabRiwayatMain.addEventListener('shown.bs.tab', function (e) {
                const subtabPesanan = document.getElementById('subtab-draft_riwayat_transaksi-<%=rnd%>');
                if (subtabPesanan) {
                    // Paksa update UI langsung jikalau Bootstrap return early (sudah active)
                    subTabs.forEach(t => {
                        t.classList.remove('border-bottom', 'border-3', 'border-primary', 'text-primary');
                        t.classList.add('text-secondary', 'border-0');
                    });
                    subtabPesanan.classList.remove('text-secondary', 'border-0');
                    subtabPesanan.classList.add('border-bottom', 'border-3', 'border-primary', 'text-primary');

                    // Panggil fungsi Bootstrap Tab secara programatik
                    const bsTab = new bootstrap.Tab(subtabPesanan);
                    bsTab.show();
                }
            });
        }
    });
</script>