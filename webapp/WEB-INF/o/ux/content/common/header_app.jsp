<%@page import="ais.database.model.PerguruanTinggi"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/logoff");
	return;
}

/* Ganti Bahasa (Indonesia/English/Arab): ?lang=id|en|ar -> set + simpan permanen (bila login) -> reload. */
String _pilihBahasa = request.getParameter("lang");
if (_pilihBahasa != null && _pilihBahasa.trim().length() > 0) {
	Common.initBahasaParameter(_pilihBahasa.trim());
	response.sendRedirect(request.getRequestURI());
	return;
}
String _lgAktif = Common.currentLang();
String _lgKode = Common.kodeBahasaAktif();
String _lgFlag = "en".equals(_lgKode) ? "united-kingdom"
		: ("ar".equals(_lgKode) ? "saudi-arabia" : ("zh".equals(_lgKode) ? "china" : "indonesia"));

PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);

String foto = CommonMedia.getUrlFotoPengguna(tbmuser, 152, 114);

String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
		"logo_perguruanTinggi_");
if (logo_PerguruanTinggi == null || logo_PerguruanTinggi.trim().isEmpty()) {
	logo_PerguruanTinggi = "/img/logo.png";
}
%>

<!--begin::Header-->
<div id="kt_app_header" class="app-header">
	<!--begin::Header container-->
	<div
		class="app-container container-fluid d-flex align-items-stretch justify-content-between">
		<!--begin::sidebar mobile toggle-->
		<div class="d-flex align-items-center d-lg-none ms-n2 me-2"
			title="Show sidebar menu">
			<div class="btn btn-icon btn-active-color-primary w-35px h-35px"
				id="kt_app_sidebar_mobile_toggle">
				<!--begin::Svg Icon | path: icons/duotune/abstract/abs015.svg-->
				<span class="svg-icon svg-icon-1"> <svg width="24"
						height="24" viewBox="0 0 24 24" fill="none"
						xmlns="http://www.w3.org/2000/svg">
										<path
							d="M21 7H3C2.4 7 2 6.6 2 6V4C2 3.4 2.4 3 3 3H21C21.6 3 22 3.4 22 4V6C22 6.6 21.6 7 21 7Z"
							fill="currentColor" />
										<path opacity="0.3"
							d="M21 14H3C2.4 14 2 13.6 2 13V11C2 10.4 2.4 10 3 10H21C21.6 10 22 10.4 22 11V13C22 13.6 21.6 14 21 14ZM22 20V18C22 17.4 21.6 17 21 17H3C2.4 17 2 17.4 2 18V20C2 20.6 2.4 21 3 21H21C21.6 21 22 20.6 22 20Z"
							fill="currentColor" />
									</svg>
				</span>
				<!--end::Svg Icon-->
			</div>
		</div>
		<!--end::sidebar mobile toggle-->
		<!--begin::Mobile logo-->
		<div class="d-flex align-items-center flex-grow-1 flex-lg-grow-0">
			<a href="<%=request.getContextPath()%>/pages/ux/index.jsp"
				class="d-lg-none" > <img alt="Logo"
				src="<%=logo_PerguruanTinggi%>" class="h-30px" /> <%=perguruanTinggi.getNama()%>
			</a>

		</div>
		<!--end::Mobile logo-->
		<!--begin::Header wrapper-->
		<div
			class="d-flex align-items-stretch justify-content-between flex-lg-grow-1"
			id="kt_app_header_wrapper">
			<!--begin::Menu wrapper-->
			<div
				class="app-header-menu app-header-mobile-drawer align-items-stretch"
				data-kt-drawer="true" data-kt-drawer-name="app-header-menu"
				data-kt-drawer-activate="{default: true, lg: false}"
				data-kt-drawer-overlay="true" data-kt-drawer-width="225px"
				data-kt-drawer-direction="end"
				data-kt-drawer-toggle="#kt_app_header_menu_toggle"
				data-kt-swapper="true"
				data-kt-swapper-mode="{default: 'append', lg: 'prepend'}"
				data-kt-swapper-parent="{default: '#kt_app_body', lg: '#kt_app_header_wrapper'}">
				<!--begin::Menu-->
				<div
					class="menu menu-rounded menu-column menu-lg-row my-5 my-lg-0 align-items-stretch fw-semibold px-2 px-lg-0"
					id="kt_app_header_menu" data-kt-menu="true">
					<!--begin:Menu item-->


					<%
					String current = request.getRequestURI();
					%>


					<div
						class="<%=(current.endsWith("index.jsp")) ? "menu-item here menu-here-bg me-0 me-lg-2" : "menu-item me-0 me-lg-2"%>">
						<!--begin:Menu link-->
						<a href="<%=request.getContextPath()%>/pages/ux/index.jsp"
							class="menu-link"> <span class="menu-title">Beranda</span>
						</a>
						<!--end:Menu link-->

					</div>
					<!--end:Menu item-->

					<!--begin:Menu item-->
					<div
						class="<%=(current.contains("elearning")) ? "menu-item here menu-here-bg me-0 me-lg-2" : "menu-item me-0 me-lg-2"%>">
						<!--begin:Menu link-->
						<a href="<%=request.getContextPath()%>/pages/ux/elearning.jsp"
							class="menu-link"> <span class="menu-title">e-Learning</span>
						</a>
					</div>
					<!--end:Menu item-->

					<!--begin:Menu item-->
					<div
						class="<%=(current.endsWith("info.jsp") || current.contains("pengumuman")) ? "menu-item here menu-here-bg me-0 me-lg-2"
		: "menu-item me-0 me-lg-2"%>">
						<!--begin:Menu link-->
						<a href="<%=request.getContextPath()%>/pages/ux/info.jsp"
							class="menu-link"> <span class="menu-title">Pengumuman</span>
						</a>
					</div>
					<!--end:Menu item-->

					<!--begin:Menu item-->
					<div
						class="<%=(current.endsWith("kehadiran.jsp") || current.contains("hadir")) ? "menu-item here menu-here-bg me-0 me-lg-2"
		: "menu-item me-0 me-lg-2"%>">
						<!--begin:Menu link-->
						<a href="<%=request.getContextPath()%>/pages/ux/kehadiran.jsp"
							class="menu-link"> <span class="menu-title">Kehadiran</span>
						</a>
					</div>
					<!--end:Menu item-->

					<!--begin:Menu item-->
					<div class="menu-item me-0 me-lg-2">
						<!--begin:Menu link-->
						<a href="javascript:void(0)" class="menu-link"> <span
							class="menu-title">Referensi</span>
						</a>
					</div>
					<!--end:Menu item-->
				</div>
				<!--end::Menu-->
			</div>
			<!--end::Menu wrapper-->

			<!--begin::Navbar-->
			<div class="app-navbar flex-shrink-0">

				<!--begin::Language switcher (flag dropdown)-->
				<div class="app-navbar-item ms-1 ms-lg-3 ais-lang-dd" style="position:relative;" title="Pilih Bahasa / Language">
					<a href="javascript:void(0)" onclick="aisLangDD(this)" style="display:inline-flex;align-items:center;gap:3px;cursor:pointer;text-decoration:none;">
						<img src="<%=request.getContextPath()%>/component/assets/media/flags/<%=_lgFlag%>.svg" style="width:20px;height:13px;border-radius:3px;box-shadow:0 0 0 1px rgba(0,0,0,.15);display:block;">
						<span style="font-size:10px;color:#94a3b8;">&#9662;</span>
					</a>
					<div class="ais-lang-menu" style="display:none;position:absolute;right:0;top:100%;margin-top:6px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;box-shadow:0 6px 18px rgba(0,0,0,.18);padding:6px;z-index:30000;">
						<a href="?lang=id" title="Indonesia" style="display:block;padding:5px;"><img src="<%=request.getContextPath()%>/component/assets/media/flags/indonesia.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
						<a href="?lang=en" title="English" style="display:block;padding:5px;"><img src="<%=request.getContextPath()%>/component/assets/media/flags/united-kingdom.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
						<a href="?lang=ar" title="Arabic" style="display:block;padding:5px;"><img src="<%=request.getContextPath()%>/component/assets/media/flags/saudi-arabia.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
						<a href="?lang=zh" title="Mandarin" style="display:block;padding:5px;"><img src="<%=request.getContextPath()%>/component/assets/media/flags/china.svg" style="width:30px;height:20px;border-radius:3px;display:block;"></a>
					</div>
				</div>
				<script>
				function aisLangDD(el){var m=el.parentNode.querySelector('.ais-lang-menu');if(!m)return;m.style.display=(m.style.display==='block')?'none':'block';}
				if(!window.__aisLangDDInit){window.__aisLangDDInit=true;document.addEventListener('click',function(e){var ms=document.getElementsByClassName('ais-lang-menu');for(var i=0;i<ms.length;i++){if(!ms[i].parentNode.contains(e.target))ms[i].style.display='none';}});}
				</script>
				<!--end::Language switcher-->

				<!--begin::Notifications-->
				<div class="app-navbar-item ms-1 ms-lg-3">
					<!--begin::Menu- wrapper-->
					<div
						class="btn btn-icon btn-custom btn-icon-muted btn-active-light btn-active-color-primary w-35px h-35px w-md-40px h-md-40px"
						data-kt-menu-trigger="click" data-kt-menu-attach="parent"
						data-kt-menu-placement="bottom-end">
						<!--begin::Svg Icon | path: icons/duotune/general/gen022.svg-->
						<span class="svg-icon svg-icon-1  icon-dashboard	"> <i
							class="fad fa-bell-exclamation"></i>
						</span>
						<!--end::Svg Icon-->
					</div>
					<!--begin::Menu-->
					<div
						class="menu menu-sub menu-sub-dropdown menu-column w-350px w-lg-375px"
						data-kt-menu="true">
						<!--begin::Heading-->
						<div class="d-flex flex-column bgi-no-repeat rounded-top"
							style="background-image: url('<%=request.getContextPath()%>/component/assets/media/misc/pattern-1.jpg')">
							<!--begin::Title-->
							<h3 class="text-white fw-semibold px-9 mt-10 mb-6">
								Notifications

								<!--end::Tabs-->
						</div>
						<!--end::Heading-->
						<!--begin::Tab content-->
						<div class="tab-content">
							<!--begin::Tab panel-->
							<div class="tab-pane fade show active"
								id="kt_topbar_notifications_3" role="tabpanel">
								<!--begin::Items-->
								<div class="scroll-y mh-325px my-5 px-8">
									<!--begin::Item-->
									<div class="d-flex flex-stack py-4">
										<!--begin::Section-->
										<div class="d-flex align-items-center me-2">
											<!--begin::Code-->
											<span class="w-70px badge badge-light-success me-4">Tugas</span>
											<!--end::Code-->
											<!--begin::Title-->
											<a href="javascript:void(0)0"
												class="text-gray-800 text-hover-primary fw-semibold text-limit-2-row">Tugas
												Baru Pendidikan Kewarganegaraan</a>
											<!--end::Title-->
										</div>
										<!--end::Section-->
										<!--begin::Label-->
										<span class="badge badge-light fs-8">Just now</span>
										<!--end::Label-->
									</div>
									<!--end::Item-->
									<!--begin::Item-->
									<div class="d-flex flex-stack py-4">
										<!--begin::Section-->
										<div class="d-flex align-items-center me-2">
											<!--begin::Code-->
											<span class="w-70px badge badge-light-danger me-4">Diskusi</span>
											<!--end::Code-->
											<!--begin::Title-->
											<a href="javascript:void(0)0"
												class="text-gray-800 text-hover-primary fw-semibold text-limit-2-row">Diskusi
												Tugas ABC</a>
											<!--end::Title-->
										</div>
										<!--end::Section-->
										<!--begin::Label-->
										<span class="badge badge-light fs-8">2 hrs</span>
										<!--end::Label-->
									</div>
									<!--end::Item-->
									<!--begin::Item-->
									<div class="d-flex flex-stack py-4">
										<!--begin::Section-->
										<div class="d-flex align-items-center me-2">
											<!--begin::Code-->
											<span class="w-70px badge badge-light-success me-4">Tugas</span>
											<!--end::Code-->
											<!--begin::Title-->
											<a href="javascript:void(0)0"
												class="text-gray-800 text-hover-primary fw-semibold text-limit-2-row">Upload
												Tugas ABC Berhasil</a>
											<!--end::Title-->
										</div>
										<!--end::Section-->
										<!--begin::Label-->
										<span class="badge badge-light fs-8">5 hrs</span>
										<!--end::Label-->
									</div>
									<!--end::Item-->
									<!--begin::Item-->
									<div class="d-flex flex-stack py-4">
										<!--begin::Section-->
										<div class="d-flex align-items-center me-2">
											<!--begin::Code-->
											<span class="w-70px badge badge-light-warning me-4">Info</span>
											<!--end::Code-->
											<!--begin::Title-->
											<a href="javascript:void(0)0"
												class="text-gray-800 text-hover-primary fw-semibold text-limit-2-row">Pengumuman
												Libur Tahun Baru</a>
											<!--end::Title-->
										</div>
										<!--end::Section-->
										<!--begin::Label-->
										<span class="badge badge-light fs-8">2 days</span>
										<!--end::Label-->
									</div>
									<!--end::Item-->
									<!--begin::Item-->
									<div class="d-flex flex-stack py-4">
										<!--begin::Section-->
										<div class="d-flex align-items-center me-2">
											<!--begin::Code-->
											<span class="w-70px badge badge-light-success me-4">Tugas</span>
											<!--end::Code-->
											<!--begin::Title-->
											<a href="javascript:void(0)0"
												class="text-gray-800 text-hover-primary fw-semibold text-limit-2-row">Upload
												Tugas Berhasil</a>
											<!--end::Title-->
										</div>
										<!--end::Section-->
										<!--begin::Label-->
										<span class="badge badge-light fs-8">1 week</span>
										<!--end::Label-->
									</div>
									<!--end::Item-->
									<!--begin::Item-->
									<div class="d-flex flex-stack py-4">
										<!--begin::Section-->
										<div class="d-flex align-items-center me-2">
											<!--begin::Code-->
											<span class="w-70px badge badge-light-success me-4">Tugas</span>
											<!--end::Code-->
											<!--begin::Title-->
											<a href="javascript:void(0)0"
												class="text-gray-800 text-hover-primary fw-semibold text-limit-2-row">Upload
												Tugas Berhasil</a>
											<!--end::Title-->
										</div>
										<!--end::Section-->
										<!--begin::Label-->
										<span class="badge badge-light fs-8">Mar 5</span>
										<!--end::Label-->
									</div>
									<!--end::Item-->
									<!--begin::Item-->
									<div class="d-flex flex-stack py-4">
										<!--begin::Section-->
										<div class="d-flex align-items-center me-2">
											<!--begin::Code-->
											<span class="w-70px badge badge-light-warning me-4">Info</span>
											<!--end::Code-->
											<!--begin::Title-->
											<a href="javascript:void(0)0"
												class="text-gray-800 text-hover-primary fw-semibold text-limit-2-row">Kalender
												Akademik 2022 Genap</a>
											<!--end::Title-->
										</div>
										<!--end::Section-->
										<!--begin::Label-->
										<span class="badge badge-light fs-8">May 15</span>
										<!--end::Label-->
									</div>
									<!--end::Item-->
									<!--begin::Item-->
									<div class="d-flex flex-stack py-4">
										<!--begin::Section-->
										<div class="d-flex align-items-center me-2">
											<!--begin::Code-->
											<span class="w-70px badge badge-light-warning me-4">Info</span>
											<!--end::Code-->
											<!--begin::Title-->
											<a href="javascript:void(0)0"
												class="text-gray-800 text-hover-primary fw-semibold text-limit-2-row">Pembayaran
												Semester Genap</a>
											<!--end::Title-->
										</div>
										<!--end::Section-->
										<!--begin::Label-->
										<span class="badge badge-light fs-8">Apr 3</span>
										<!--end::Label-->
									</div>
									<!--end::Item-->
									<!--begin::Item-->
									<div class="d-flex flex-stack py-4">
										<!--begin::Section-->
										<div class="d-flex align-items-center me-2">
											<!--begin::Code-->
											<span class="w-70px badge badge-light-warning me-4">Info</span>
											<!--end::Code-->
											<!--begin::Title-->
											<a href="javascript:void(0)0"
												class="text-gray-800 text-hover-primary fw-semibold text-limit-2-row">UAS
												Tahun Ajaran 2021/2022 | 2022 Genap</a>
											<!--end::Title-->
										</div>
										<!--end::Section-->
										<!--begin::Label-->
										<span class="badge badge-light fs-8">Jun 30</span>
										<!--end::Label-->
									</div>
									<!--end::Item-->

								</div>
								<!--end::Items-->
								<!--begin::View more-->
								<div class="py-3 text-center border-top">
									<a href="javascript:void(0);"
										class="btn btn-color-gray-600 btn-active-color-primary">View
										All <!--begin::Svg Icon | path: icons/duotune/arrows/arr064.svg-->
										<span class="svg-icon svg-icon-5"> <svg width="24"
												height="24" viewBox="0 0 24 24" fill="none"
												xmlns="http://www.w3.org/2000/svg">
															<rect opacity="0.5" x="18" y="13" width="13" height="2"
													rx="1" transform="rotate(-180 18 13)" fill="currentColor" />
															<path
													d="M15.4343 12.5657L11.25 16.75C10.8358 17.1642 10.8358 17.8358 11.25 18.25C11.6642 18.6642 12.3358 18.6642 12.75 18.25L18.2929 12.7071C18.6834 12.3166 18.6834 11.6834 18.2929 11.2929L12.75 5.75C12.3358 5.33579 11.6642 5.33579 11.25 5.75C10.8358 6.16421 10.8358 6.83579 11.25 7.25L15.4343 11.4343C15.7467 11.7467 15.7467 12.2533 15.4343 12.5657Z"
													fill="currentColor" />
														</svg>
									</span> <!--end::Svg Icon-->
									</a>
								</div>
								<!--end::View more-->
							</div>
							<!--end::Tab panel-->
						</div>
						<!--end::Tab content-->
					</div>
					<!--end::Menu-->
					<!--end::Menu wrapper-->
				</div>
				<!--end::Notifications-->


				<!--begin::User menu-->
				<div class="app-navbar-item ms-1 ms-lg-3"
					id="kt_header_user_menu_toggle">
					<!--begin::Menu wrapper-->
					<div class="cursor-pointer symbol symbol-35px symbol-md-40px"
						data-kt-menu-trigger="click" data-kt-menu-attach="parent"
						data-kt-menu-placement="bottom-end">
						<img src="<%=foto%>" alt="user" />
					</div>
					<!--begin::User account menu-->
					<div
						class="menu menu-sub menu-sub-dropdown menu-column menu-rounded menu-gray-800 menu-state-bg menu-state-primary fw-semibold py-4 fs-6 w-275px"
						data-kt-menu="true">
						<!--begin::Menu item-->
						<div class="menu-item px-3">
							<div class="menu-content d-flex align-items-center px-3">
								<!--begin::Avatar-->
								<div class="symbol symbol-50px me-5">
									<img alt="Logo" src="<%=foto%>" />
								</div>
								<!--end::Avatar-->
								<!--begin::Username-->
								<div class="d-flex flex-column">
									<div
										class="fw-bold d-flex align-items-center fs-5 text-limit-1-row"><%=tbmuser.getUserNama()%></div>
									<a href="javascript:void(0);"
										class="fw-semibold text-muted text-hover-primary fs-7"> <%=(tbmuser.ambilJurusan() != null
		? tbmuser.ambilJurusan().getJenjang().getNama() + " " + tbmuser.ambilJurusan().getNama()
		: (tbmuser.ambilSekolah() != null ? tbmuser.ambilSekolah().getNama() : "-"))%>
									</a>
								</div>
								<!--end::Username-->
							</div>
						</div>
						<!--end::Menu item-->
						<!--begin::Menu separator-->
						<div class="separator my-2"></div>
						<!--end::Menu separator-->
						<!--begin::Menu item-->
						<div class="menu-item px-5">
							<a href="<%=request.getContextPath()%>/pages/ux/profile.jsp" class="menu-link px-5"><i class="fad fa-address-card"></i>&nbsp; Profil</a>
						</div>
						<!--end::Menu item-->
						<!--begin::Menu item-->
						<div class="menu-item px-5">
							<a href="<%=request.getContextPath()%>/pages/ux/android.jsp" class="menu-link px-5"><i class="fab fa-android"></i>&nbsp; Android</a>
						</div>
						<!--end::Menu item-->
						<!--begin::Menu item-->
						<div class="menu-item px-5">
							<a href="<%=request.getContextPath()%>/pages/ux/apple.jsp" class="menu-link px-5"><i class="fab fa-apple"></i>&nbsp; Apple</a>
						</div>
						<!--end::Menu item-->
						<!--begin::Menu item-->
						<div class="menu-item px-5">
							<a href="elearning.jsp" class="menu-link px-5"> <span
								class="menu-text">Mata Kuliah Aktif</span> <span
								class="menu-badge"> <span
									class="badge badge-light-danger badge-circle fw-bold fs-7">5</span>
							</span>
							</a>
						</div>
						<!--end::Menu item-->

						<!--begin::Menu separator-->
						<div class="separator my-2"></div>
						<!--end::Menu separator-->

						<!--begin::Menu item-->
						<div class="menu-item px-5 my-1">
							<a href="javascript:void(0)" class="menu-link px-5"><i class="fad fa-cogs"></i>&nbsp; Setting
								Akun</a>
						</div>
						<!--end::Menu item-->
						<!--begin::Menu item-->
						<div class="menu-item px-5">
							<a href="<%=request.getContextPath()%>/logoff"
								class="menu-link px-5"><i class="fad fa-power-off"></i>&nbsp; Keluar</a>
						</div>
						<!--end::Menu item-->
					</div>
					<!--end::User account menu-->
					<!--end::Menu wrapper-->
				</div>
				<!--end::User menu-->
				<!--begin::Header menu toggle-->
				<div class="app-navbar-item d-lg-none ms-2 me-n3"
					title="Show header menu">
					<div class="btn btn-icon btn-active-color-primary w-35px h-35px"
						id="kt_app_header_menu_toggle">
						<!--begin::Svg Icon | path: icons/duotune/text/txt001.svg-->
						<span class="svg-icon svg-icon-1"> <svg width="24"
								height="24" viewBox="0 0 24 24" fill="none"
								xmlns="http://www.w3.org/2000/svg">
												<path
									d="M13 11H3C2.4 11 2 10.6 2 10V9C2 8.4 2.4 8 3 8H13C13.6 8 14 8.4 14 9V10C14 10.6 13.6 11 13 11ZM22 5V4C22 3.4 21.6 3 21 3H3C2.4 3 2 3.4 2 4V5C2 5.6 2.4 6 3 6H21C21.6 6 22 5.6 22 5Z"
									fill="currentColor" />
												<path opacity="0.3"
									d="M21 16H3C2.4 16 2 15.6 2 15V14C2 13.4 2.4 13 3 13H21C21.6 13 22 13.4 22 14V15C22 15.6 21.6 16 21 16ZM14 20V19C14 18.4 13.6 18 13 18H3C2.4 18 2 18.4 2 19V20C2 20.6 2.4 21 3 21H13C13.6 21 14 20.6 14 20Z"
									fill="currentColor" />
											</svg>
						</span>
						<!--end::Svg Icon-->
					</div>
				</div>
				<!--end::Header menu toggle-->
			</div>
			<!--end::Navbar-->
		</div>
		<!--end::Header wrapper-->
	</div>
	<!--end::Header container-->
</div>
<!--end::Header-->