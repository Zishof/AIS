<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.kursus.BeritaKursus"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%
BeritaKursus beritaKursus = (BeritaKursus) ConstantValues.ambil(BeritaKursus.class.getName(),
		Long.parseLong(request.getParameter("id")));

if (beritaKursus != null) {

	request.setAttribute("beritaKursus", beritaKursus);

	LampiranLain d = LampiranLain.ambil(beritaKursus.getId(), "Gambar Berita Produk Kursus");
	String img = "";
	if (d != null) {
		img = d.createLinkUri();
	}
%>

<style>
#hero_in.courses:before {
	background: url('<%=img%>') center center no-repeat;
	-webkit-background-size: cover;
	-moz-background-size: cover;
	-o-background-size: cover;
	background-size: cover;
}
</style>

<main>
	<section id="hero_in" class="courses">
		<div class="wrapper">
			<div class="container">
				<h1 class="fadeInUp">
					<span></span><%=beritaKursus.getKeterangan()%>
				</h1>
			</div>
		</div>
	</section>
	<!--/hero_in-->

	<div class="bg_color_1">
		<div class="container margin_60_35">
			<div class="row">
				<div class="col-lg-12">
					<section id="description">
						<h2><%=beritaKursus.getNama()%></h2>
						<%=beritaKursus.getIsi()%>
						<!-- /row -->
					</section>
					<!-- /section -->
				</div>
				<!-- /col -->

				
			</div>
			<!-- /row -->
		</div>
		<!-- /container -->
	</div>
	<!-- /bg_color_1 -->
</main>
<!--/main-->

<%
}
%>