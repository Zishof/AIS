<%@page import="ais.database.model.Pertemuan"%>
<%
Pertemuan pertemuan = (Pertemuan) request.getAttribute("pertemuan");
%>

<div class="card-toolbar" style="padding-right: 20px">
	<!--begin::Action-->

	<!--begin::Menu-->
	<button
		class="btn btn-icon btn-color-gray-400 btn-active-color-primary justify-content-end"
		data-kt-menu-trigger="click" data-kt-menu-placement="bottom-end"
		data-kt-menu-overflow="true">
		<!--begin::Svg Icon | path: icons/duotune/general/gen023.svg-->
		<span class="svg-icon svg-icon-1"> <i
			class="fad fa-bars fs-4 me-2"></i>
		</span>
		<!--end::Svg Icon-->
	</button>
	<!--begin::Menu 2-->
	<div
		class="menu menu-sub menu-sub-dropdown menu-column menu-rounded menu-gray-800 menu-state-bg-light-primary fw-semibold w-200px"
		data-kt-menu="true">
		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<div class="menu-content fs-6 text-dark fw-bold px-3 py-4">
				Pertemuan ke
				<%=pertemuan.getPertemuanKe()%>
				<%=pertemuan.info()%></div>
		</div>
		<!--end::Menu item-->
		<!--begin::Menu separator-->
		<div class="separator mb-3 opacity-75"></div>
		<!--end::Menu separator-->
		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a href="#" class="menu-link px-3"><i
				class="far fa-sticky-note fs-4 me-2"></i> Catatan</a></a>
		</div>
		<!--end::Menu item-->
		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a
				href="<%=request.getContextPath()%>/pages/ux/content/elearning_detail/diskusi_action.jsp?id=<%=pertemuan.getId()%>"
				class="menu-link px-3"><i
				class="bi bi-chat-square-text-fill fs-4 me-2"></i> Diskusi</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a
				href="<%=request.getContextPath()%>/pages/ux/content/elearning_detail/materi_action.jsp?id=<%=pertemuan.getId()%>&index=2"
				class="menu-link px-3"><i class="bi bi-bookmarks-fill fs-4 me-2"></i>
				Materi</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a
				href="<%=request.getContextPath()%>/pages/ux/content/elearning_detail/pertemuan_action.jsp?id=<%=pertemuan.getId()%>&index=3"
				class="menu-link px-3"><i class="bi bi-check2-square fs-4 me-2"></i>
				Tugas</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a
				href="<%=request.getContextPath()%>/pages/ux/content/elearning_detail/pertemuan_action.jsp?id=<%=pertemuan.getId()%>&index=6"
				class="menu-link px-3"><i class="far fa-check-double fs-4 me-2"></i>
				Ujian</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a
				href="<%=request.getContextPath()%>/pages/ux/content/elearning_detail/pertemuan_action.jsp?id=<%=pertemuan.getId()%>&index=4"
				class="menu-link px-3"><i
				class="far fa-microphone-stand fs-4 me-2"></i> Audio</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a
				href="<%=request.getContextPath()%>/pages/ux/content/elearning_detail/pertemuan_action.jsp?id=<%=pertemuan.getId()%>&index=4"
				class="menu-link px-3"> <i class="fad fa-file-video fs-4 me-2"></i>
				Video
			</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a href="#" class="menu-link px-3"><i
				class="far fa-eye fs-4 me-2"></i> Akses</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a href="#" class="menu-link px-3"><i
				class="far fa-qrcode fs-4 me-2"></i> QRCode</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a href="#" class="menu-link px-3"><i
				class="fad fa-webcam fs-4 me-2"></i> Online</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a href="#" class="menu-link px-3"><i
				class="fad fa-calendar fs-4 me-2"></i> Agenda</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a
				href="<%=request.getContextPath()%>/pages/ux/content/elearning_detail/pertemuan_action.jsp?id=<%=pertemuan.getId()%>&index=0"
				class="menu-link px-3"><i class="far fa-user-check fs-4 me-2"></i>
				Kehadiran</a>
		</div>
		<!--end::Menu item-->

		<!--begin::Menu item-->
		<div class="menu-item px-3">
			<a href="#" class="menu-link px-3"><i
				class="fad fa-fingerprint  fs-4 me-2"></i> Absen Online</a>
		</div>
		<!--end::Menu item-->


	</div>
	<!--end::Menu 2-->
	<!--end::Menu-->

</div>