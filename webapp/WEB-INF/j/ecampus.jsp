<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.Common"%>
<%
Common.encripSemua();
if (ConstantValues.AKTIF == null) {
	ConstantValues.hasbeeninit = false;
	ConstantValues.init();
}
Common.getRequestHostWithProtocol(request);
Common.CURRENT_URL = (request.isSecure() ? "https://" : "http://") + request.getServerName()
		+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
		+ request.getContextPath();
ais.database.model.PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
		.getPerguruanTinggi(request);
String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "background_login_perguruanTinggi_");
	if(Common.isMobile(request)){
		background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
	}
String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(request,
		"logo_perguruanTinggi_");
if (logo_PerguruanTinggi == null || logo_PerguruanTinggi.trim().isEmpty()) {
	logo_PerguruanTinggi = "/img/logo.png";
}
%>
<!DOCTYPE html>
<html lang="id">

<jsp:include page="/WEB-INF/o/ux/content/common/header.jsp"></jsp:include>
<!--begin::Body-->
<body data-kt-name="metronic" id="kt_body"
	class="app-blank app-blank bgi-size-cover bgi-position-center bgi-no-repeat">
	<jsp:include page="/WEB-INF/o/ux/content/common/mode.jsp"></jsp:include>
	<!--begin::Root-->
	<div class="d-flex flex-column flex-root" id="kt_app_root">
		<!--begin::Page bg image-->
		<style>
body {
	background-image: url('<%=background_PerguruanTinggi%>');
}

[data-theme="dark"] body {
	background-image: url('<%=background_PerguruanTinggi%>');
}
</style>
		<!--end::Page bg image-->
		<!--begin::Authentication - Sign-in -->
		<div class="d-flex flex-column flex-column-fluid flex-lg-row">
			<!--begin::Aside-->
			<div class="d-flex flex-center w-lg-10 pt-15 pt-lg-0 px-10">
				
				<!--begin::Card-->
				<div class="card rounded-3 w-md-450px" style="border: 2px;border-style: solid;border-color: blue;">
					<!--begin::Card body-->
					<div class="card-body p-10 p-lg-20">
						<!--begin::Form-->
						<form class="form w-100" novalidate="novalidate" method="post"
							id="kt_sign_in_form"
							data-kt-redirect-url="j_spring_security_check"
							action="j_spring_security_check">
							<!--begin::Heading-->
							<div class="text-center mb-11">
								<!--begin::Title-->
								<h1 class="text-dark fw-bolder mb-3">Masuk</h1>
								
									<%
			if (request.getParameter("login_error") != null) {
					%>
					<font color="red"><%=request.getParameter("login_error")%></font>
					<%
						}
					%>
								
								<!--end::Title-->
								<!--begin::Subtitle-->
								<div class="text-gray-500 fw-semibold fs-6">Login
									menggunakan Media Sosial</div>
								<!--end::Subtitle=-->
							</div>
							<!--begin::Heading-->
							<!--begin::Login options-->
							<div class="row g-3 mb-9">
								<!--begin::Col-->
								<div class="col-md-12">
									<!--begin::Google link=-->
									<a href="google.zul" class="btn btn-flex btn-outline btn-text-gray-700 btn-active-color-primary bg-state-light flex-center text-nowrap w-100">
										<img alt="Logo" src="<%=request.getContextPath()%>/component/assets/media/svg/brand-logos/google-icon.svg" class="h-15px me-3">Login menggunakan akun Google
									</a>
									<!--end::Google link=-->
								</div>
								<!--end::Col-->

							</div>
							<!--end::Login options-->
							<!--begin::Separator-->
							<div class="separator separator-content my-14">
							<span class="w-200px text-gray-500 fw-semibold fs-7">Atau Menggunakan</span>
							</div>
							<!--end::Separator-->
							<!--begin::Input group=-->
							<div class="fv-row mb-8">
								<!--begin::Email-->
								<input type="text" placeholder="ID Pengguna" name="j_username"
									autocomplete="off" class="form-control bg-transparent" />
								<!--end::Email-->
							</div>
							<!--end::Input group=-->
							<div class="fv-row mb-3">
								<!--begin::Password-->
								<input type="password" placeholder="Password" name="j_password"
									autocomplete="off" class="form-control bg-transparent" />
								<!--end::Password-->
							</div>
							<!--end::Input group=-->
							

							<!--begin::Wrapper-->
							<div class="row g-3 mb-8">
								<!--begin::Col-->
								<div class="col-8">
									<!--begin::Accept-->
									<div class="fv-row">
										<label class="form-check form-check-inline"> <input
											id="checkbox" value="true" name="rememberMe"
											class="form-check-input" type="checkbox" name="toc" /> <span
											class="form-check-label fw-semibold text-gray-700 fs-base ms-1">Ingat
												akun saya di browser ini</span>
										</label>
									</div>
									<!--end::Accept-->
								</div>
								<!--end::Col-->
								<!--begin::Col-->
								<div class="col-4">
									<!--begin::Link-->
									<a href="reset-password.html" class="link-primary float-end">Lupa
										Password</a>
									<!--end::Link-->
								</div>
								<!--end::Col-->
							</div>
							<!--end::Wrapper-->

							<!--begin::Submit button-->
							<div class="d-grid mb-10">
								<button type="submit" id="kt_sign_in_submit"
									class="btn btn-primary">
									<!--begin::Indicator label-->
									<span class="indicator-label">Sign In</span>
									<!--end::Indicator label-->
									<!--begin::Indicator progress-->
									<span class="indicator-progress">Please wait... <span
										class="spinner-border spinner-border-sm align-middle ms-2"></span></span>
									<!--end::Indicator progress-->
								</button>
							</div>
							<!--end::Submit button-->

						</form>
						<!--end::Form-->
					</div>
					<!--end::Card body-->
				</div>
				<!--end::Card-->
				
				
			</div>
			<!--begin::Aside-->
			<!--begin::Body-->
			<div class="d-flex flex-center w-lg-40 p-5">
				
			</div>
			<!--end::Body-->
		</div>
		<!--end::Authentication - Sign-in-->
	</div>
	<!--end::Root-->
	<jsp:include page="/WEB-INF/o/ux/content/common/footer.jsp"></jsp:include>
	<%=ais.common.Common.remember(request, response) %>
<jsp:include page="/WEB-INF/baru/include/bantuan_button.jsp"/>
</body>
<!--end::Body-->
</html>