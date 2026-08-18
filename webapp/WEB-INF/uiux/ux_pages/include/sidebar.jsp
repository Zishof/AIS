<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%
        ais.database.model.PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
        		.getPerguruanTinggi(request);
        String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
        		"banner_perguruanTinggi_");
        if (background_PerguruanTinggi == null || background_PerguruanTinggi.trim().isEmpty()) {
        	background_PerguruanTinggi = ais.common.Common.getRequestHostWithProtocol() + "/img/header.jpg";
        }
        String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
        		"logo_perguruanTinggi_");
        if (logo_PerguruanTinggi == null || logo_PerguruanTinggi.trim().isEmpty()) {
        	logo_PerguruanTinggi = "/img/logo.png";
        }

        ais.database.model.sekolah.Sekolah sekolah = ais.action.master.sekolah.util.SekolahUtil.getSekolah(request);
        String judul = perguruanTinggi == null ? "" : perguruanTinggi.getNama();
        String Motto = perguruanTinggi == null || perguruanTinggi.getMotto() == null
        		|| perguruanTinggi.getMotto().trim().isEmpty() ? "eCampus Information System" : perguruanTinggi.getMotto();
        String Alamat1 = perguruanTinggi == null ? "" : perguruanTinggi.getAlamat1();
        String Telepon = perguruanTinggi.getTelepon();
        String Email = perguruanTinggi.getEmail();
        if (sekolah != null && sekolah.getId() != null) {
        	if (sekolah.getNama() != null && !sekolah.getNama().trim().isEmpty()) {
        		judul = sekolah.getNama();
        	}

        	if (sekolah.getMotto() != null && !sekolah.getMotto().trim().isEmpty()) {
        		Motto = sekolah.getMotto();
        	}

        	if (sekolah.getAlamat() != null && !sekolah.getAlamat().trim().isEmpty()) {
        		Alamat1 = sekolah.getAlamat();
        	}

        	if (sekolah.getTelp() != null && !sekolah.getTelp().trim().isEmpty()) {
        		Telepon = sekolah.getTelp();
        	}

        	if (sekolah.getEmail() != null && !sekolah.getEmail().trim().isEmpty()) {
        		Email = sekolah.getEmail();
        	}

        	String logo_PerguruanTinggi_local = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(request,
        	"logo_sekolah_");
        if (logo_PerguruanTinggi_local != null && !logo_PerguruanTinggi_local.endsWith("logo.png")) {
        		logo_PerguruanTinggi = logo_PerguruanTinggi_local;
        		}
        	

        }
        String pageData = request.getParameter("page");
        pageData = pageData == null ? "": pageData;
        Tbmuser tbmuser = Common.getCurrentUser(request);
        Pegawai pegawai = tbmuser==null?null:tbmuser.getPegawai();
    	String jenisPenguna = tbmuser==null||tbmuser.hakAkses()==null?"":tbmuser.hakAkses().getRoleId();
        %>

<aside class="app-sidebar shadow" data-bs-theme="light">
	<!--begin::Sidebar Brand-->
	<div class="sidebar-brand">
	<!--begin::Brand Text--> <span > <i class="bi bi-mortarboard-fill"></i> <span class="text-custom text-primary-emphasis fw-bold"> eSchool </span> <%-- <%=judul %>--%> </span>
		
		<!--end::Brand Text-->
		
	
			
		</a>
		<!--end::Brand Link-->
	</div>
	<!--end::Sidebar Brand-->
	<!--begin::Sidebar Wrapper-->
	<div class="sidebar-wrapper">
		<nav class="mt-2">
			<!--begin::Sidebar Menu-->
			<ul class="nav sidebar-menu flex-column" data-lte-toggle="treeview"
				role="menu" data-accordion="false">
				
				<li class="nav-item menu-open"><a href="#" class="nav-link active"> 
				<i class="nav-icon bi bi-house-down-fill"></i>
						<p>
							Menu Utama <i class="nav-arrow bi bi-chevron-right"></i>
						</p>
				</a>
				
				<ul class="nav nav-treeview">
					<li class="nav-item"><a
							href="<%=request.getContextPath() %>/main2?page=beranda"
							class="nav-link <%=pageData.equalsIgnoreCase("beranda") ? "active" : "" %>">
								<i class="nav-icon bi bi-house-fill"></i>
								<p>Beranda</p>
						</a></li>
						
						<li class="nav-item">
						<a
							href="<%=request.getContextPath() %>/main2?page=e-Learning"
							class="nav-link <%=pageData.equalsIgnoreCase("e-Learning") ? "active" : "" %>">
								<i class="nav-icon bi bi-mortarboard-fill"></i>
								<p>e-Learning</p>
						</a></li>
						 
						<li class="nav-item"><a
							href="<%=request.getContextPath() %>/main2?page=prestasi"
							class="nav-link <%=pageData.equalsIgnoreCase("prestasi") ? "active" : "" %>">
								<i class="nav-icon bi bi-trophy"></i>
								<p>Prestasi</p>
						</a></li>
						
						<li class="nav-item"><a
							href="<%=request.getContextPath() %>/main2?page=pustaka"
							class="nav-link <%=pageData.equalsIgnoreCase("pustaka") ? "active" : "" %>">
								<i class="nav-icon bi bi-journals"></i>
								<p>Pustaka</p>
						</a></li>
						<li class="nav-item"><a
							href="<%=request.getContextPath() %>/main2?page=presensi"
							class="nav-link <%=pageData.equalsIgnoreCase("presensi") ? "active" : "" %>">
								<i class="nav-icon bi bi-fingerprint"></i>
								<p>Presensi</p>
						</a></li>
						
					</ul>
					
					</li>

				<%
                        if(jenisPenguna.equalsIgnoreCase(Tbmrole.MAHASISWA)){
                        %>
				<li class="nav-item"><a href="#" class="nav-link"> <i
						class="nav-icon bi bi-box-seam-fill"></i>
						<p>
							Beranda Mahasiswa <i class="nav-arrow bi bi-chevron-right"></i>
						</p>
				</a>
					<ul class="nav nav-treeview">
						<li class="nav-item"><a href="./widgets/small-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Small Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/info-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>info Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/cards.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Cards</p>
						</a></li>
					</ul></li>
				<%
                        }
                       
                        else if(jenisPenguna.equalsIgnoreCase(Tbmrole.DOSEN)){
                        %>
				<li class="nav-item"><a href="#" class="nav-link"> <i
						class="nav-icon bi bi-box-seam-fill"></i>
						<p>
							Beranda Dosen <i class="nav-arrow bi bi-chevron-right"></i>
						</p>
				</a>
					<ul class="nav nav-treeview">
						<li class="nav-item"><a href="./widgets/small-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Small Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/info-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>info Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/cards.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Cards</p>
						</a></li>
					</ul></li>
				<%
                        }
                       
                        else if(jenisPenguna.equalsIgnoreCase(Tbmrole.GURU)){
                        %>
				<li class="nav-item"><a href="#" class="nav-link"> <i
						class="nav-icon bi bi-box-seam-fill"></i>
						<p>
							Beranda Guru <i class="nav-arrow bi bi-chevron-right"></i>
						</p>
				</a>
					<ul class="nav nav-treeview">
						<li class="nav-item"><a href="./widgets/small-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Small Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/info-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>info Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/cards.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Cards</p>
						</a></li>
					</ul></li>
				<%
                        }
                       
                        else if(jenisPenguna.equalsIgnoreCase(Tbmrole.SISWA)){
                        %>
				<li class="nav-item"><a href="#" class="nav-link"> <i
						class="nav-icon bi bi-box-seam-fill"></i>
						<p>
							Beranda Guru <i class="nav-arrow bi bi-chevron-right"></i>
						</p>
				</a>
					<ul class="nav nav-treeview">
						<li class="nav-item"><a href="./widgets/small-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Small Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/info-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>info Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/cards.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Cards</p>
						</a></li>
					</ul></li>
				<%
                        } else if(jenisPenguna.equalsIgnoreCase(Tbmrole.ADMINISTRATOR)){
                            %>
				<li class="nav-item"><a href="#" class="nav-link"> <i
						class="nav-icon bi bi-box-seam-fill"></i>
						<p>
							Beranda Admin <i class="nav-arrow bi bi-chevron-right"></i>
						</p>
				</a>
					<ul class="nav nav-treeview">
						<li class="nav-item"><a href="./widgets/small-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Small Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/info-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>info Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/cards.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Cards</p>
						</a></li>
					</ul></li>
				<%
                            } else if(pegawai != null){
                            %>
				<li class="nav-item"><a href="#" class="nav-link"> <i
						class="nav-icon bi bi-box-seam-fill"></i>
						<p>
							Beranda Pegawai <i class="nav-arrow bi bi-chevron-right"></i>
						</p>
				</a>
					<ul class="nav nav-treeview">
						<li class="nav-item"><a href="./widgets/small-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Small Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/info-box.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>info Box</p>
						</a></li>
						<li class="nav-item"><a href="./widgets/cards.html"
							class="nav-link"> <i class="nav-icon bi bi-circle"></i>
								<p>Cards</p>
						</a></li>
					</ul></li>
				<%
                            } 
                            %>
			</ul>
			<!--end::Sidebar Menu-->
		</nav>
	</div>
	<!--end::Sidebar Wrapper-->
</aside>
<!--end::Sidebar-->
<!--begin::App Main-->