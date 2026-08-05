<%@page import="java.util.Calendar"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%

PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);

%>
<footer>
	<div class="container margin_60_35">
		<div class="row">
			<div class="col-lg-5 col-md-12 p-r-5">
				<p>
					<img src="<%=request.getContextPath() %>/img/logo.png" width="auto" height="42"
						data-retina="true" alt="">
				</p>
				<p><%=perguruanTinggi.getDeskripsi()%></p>
				<div class="follow_us">
					<ul>
						<li>Follow us</li>
						<li><a href="#0"><i class="ti-facebook"></i></a></li>
						<li><a href="#0"><i class="ti-twitter-alt"></i></a></li>
						<li><a href="#0"><i class="ti-instagram"></i></a></li>
					</ul>
				</div>
			</div>
			<div class="col-lg-3 col-md-6 ml-lg-auto">
				<h5>Tautan</h5>
				<ul class="links">
					<li><a href="#0">Admission</a></li>
					<li><a href="#0">About</a></li>
					<li><a href="#0">Login</a></li>
					<li><a href="#0">Register</a></li>
					<li><a href="#0">Blog &amp; Events</a></li>
					<li><a href="#0">Contacts</a></li>
				</ul>
			</div>
			<div class="col-lg-3 col-md-6">
				<h5>Kontak Kami</h5>
				<ul class="contacts">
					<li><a href="tel://<%=perguruanTinggi.getTelepon()%>"><i class="ti-mobile"></i>
							<%=perguruanTinggi.getTelepon()%></a></li>
					<li><a href="mailto:<%=perguruanTinggi.getEmail()%>"><i
							class="ti-email"></i> <%=perguruanTinggi.getEmail()%></a></li>
					<li><a href="#"><i class="ti-map"></i> <%=perguruanTinggi.getAlamat1()%></a></li>
				</ul>
			</div>
		</div>
		<!--/row-->
		<hr>
		<div class="row">
			<div class="col-md-8">
				<ul id="additional_links">
					<li><a href="#0">Syarat dan Ketentuan</a></li>
					<li><a href="#0">Bantuan</a></li>
				</ul>
			</div>
			<div class="col-md-4">
				<div id="copy">© <%=Calendar.getInstance().get(Calendar.YEAR) %> <%=perguruanTinggi.getNama() %></div>
			</div>
		</div>
	</div>
</footer>