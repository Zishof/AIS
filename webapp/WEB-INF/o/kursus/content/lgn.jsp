
<%@page import="java.util.Calendar"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%
PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
%>
<nav id="menu" class="fake_menu"></nav>

<div id="preloader">
	<div data-loader="circle-side"></div>
</div>
<!-- End Preload -->

<div id="login">
	<aside>
		<figure>
			<a href="<%=request.getContextPath()%>/kursus"><img
				src="<%=request.getContextPath()%>/img/logo.png"
				width="auto" height="42" data-retina="true" alt=""></a>

			<%
			if (request.getParameter("login_error") != null) {
			%>
			<font color="red"><%=request.getParameter("login_error")%></font>
			<%
			}
			%>

		</figure>
		<form method="post" id="kt_sign_in_form"
			data-kt-redirect-url="<%=request.getContextPath()%>/j_spring_security_check"
			action="<%=request.getContextPath()%>/j_spring_security_check">
			<div class="access_social">
				<a href="<%=request.getContextPath()%>/facebook.zul"
					class="social_bt facebook">Login with Facebook</a> <a
					href="<%=request.getContextPath()%>/google.zul"
					class="social_bt google">Login with Google</a>
			</div>
			<div class="divider">
				<span>Or</span>
			</div>
			<div class="form-group">
				<span class="input"> <input class="input_field" type="text"
					autocomplete="off" name="j_username"> <label
					class="input_label"> <span class="input__label-content">ID
							Pengguna</span>
				</label>
				</span> <span class="input"> <input class="input_field"
					type="password" autocomplete="new-password" name="j_password">
					<label class="input_label"> <span
						class="input__label-content">Password</span>
				</label>
				</span> <small><a href="#0">Lupa password?</a></small>
			</div>

			<button type="submit" id="kt_sign_in_submit"
				class="btn_1 rounded full-width add_top_60">Login</button>
			<div class="text-center add_top_10">
				Baru di sistem ini? <strong><a href="<%=request.getContextPath()%>/kursus/register.jsp">Daftar!</a></strong>
			</div>
		</form>
		<div class="copy">
			©
			<%=Calendar.getInstance().get(Calendar.YEAR)%>
			<%=perguruanTinggi.getNama()%></div>
	</aside>
</div>
<!-- /login -->