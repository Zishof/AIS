<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.database.model.kursus.ProdukPeserta"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.kursus.ProdukKursus"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%
ProdukKursus produkKursus = (ProdukKursus) ConstantValues.ambil(ProdukKursus.class.getName(),
		Long.parseLong(request.getParameter("id")));

if (produkKursus != null) {

	boolean peserta = false;

	Tbmuser tbmuser = Common.getCurrentUser(request);
	if (tbmuser != null && tbmuser.getPesertaKursus() != null) {
		Session mySession = HibernateUtil.currentNativeSession();
		int jml = ((Number) mySession.createCriteria(ProdukPeserta.class).setProjection(Projections.rowCount())
		.add(Restrictions.eq("produkKursus", produkKursus))
		.add(Restrictions.eq("pesertaKursus", tbmuser.getPesertaKursus())).uniqueResult()).intValue();

		peserta = jml > 0;
		// mySession.disconnect();
		if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
		HibernateUtil.closeSession();

	}
	request.setAttribute("peserta", peserta);
	request.setAttribute("produkKursus", produkKursus);

	LampiranLain d = LampiranLain.ambil(produkKursus.getId(), "Gambar Produk Kursus");
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
					<span></span><%=produkKursus.getNama()%>
				</h1>
			</div>
		</div>
	</section>
	<!--/hero_in-->

	<div class="bg_color_1">
		<nav class="secondary_nav sticky_horizontal">
			<div class="container">
				<ul class="clearfix">
					<li><a href="#deskripsi" class="active">Deskripsi</a></li>
					<li><a href="#Pelajaran">Pelajaran</a></li>
					<li><a href="#ulasan">Ulasan</a></li>
				</ul>
			</div>
		</nav>
		<div class="container margin_60_35">
			<div class="row">
				<div class="col-lg-<%=peserta ? "12" : "8"%>">

					<section id="description">
						<h2>Deskripsi</h2>
						<%=produkKursus.getDeskripsi()%>
						<!-- /row -->
					</section>
					<!-- /section -->

					<section id="Pelajaran">
						<div class="intro_title">
							<h2>Materi Pembelajaran</h2>

						</div>


						<jsp:include page="/WEB-INF/o/kursus/content/materi.jsp">
							<jsp:param value="${produkKursus}" name="produkKursus" />
							<jsp:param value="${peserta}" name="peserta" />
						</jsp:include>

					</section>
					<!-- /section -->


					<section id="Pelajaran">
						<div class="intro_title">
							<h2>Pertemuan Tatap Muka</h2>

						</div>


						<jsp:include page="/WEB-INF/o/kursus/content/tatap_muka.jsp">
							<jsp:param value="${produkKursus}" name="produkKursus" />
							<jsp:param value="${peserta}" name="peserta" />
						</jsp:include>

					</section>
					<!-- /section -->

					<section id="Pelajaran">
						<div class="intro_title">
							<h2>Pertemuan Jarak Jauh</h2>

						</div>

						<jsp:include page="/WEB-INF/o/kursus/content/jarak_jauh.jsp">
							<jsp:param value="${produkKursus}" name="produkKursus" />
							<jsp:param value="${peserta}" name="peserta" />
						</jsp:include>

					</section>
					<!-- /section -->

					<%--
					<section id="ulasan">
						<h2>Ulasan</h2>
						<div class="reviews-container">
							<div class="row">
								<div class="col-lg-3">
									<div id="review_summary">
										<strong>4.8</strong>
										<div class="rating">
											<i class="icon_star voted"></i><i class="icon_star voted"></i><i
												class="icon_star voted"></i><i class="icon_star voted"></i><i
												class="icon_star"></i>
										</div>
										<small>Ulasan Bintang 4</small>
									</div>
								</div>
								<div class="col-lg-9">
									<div class="row">
										<div class="col-lg-10 col-9">
											<div class="progress">
												<div class="progress-bar" role="progressbar"
													style="width: 90%" aria-valuenow="90" aria-valuemin="0"
													aria-valuemax="100"></div>
											</div>
										</div>
										<div class="col-lg-2 col-3">
											<small><strong>5 stars</strong></small>
										</div>
									</div>
									<!-- /row -->
									<div class="row">
										<div class="col-lg-10 col-9">
											<div class="progress">
												<div class="progress-bar" role="progressbar"
													style="width: 95%" aria-valuenow="95" aria-valuemin="0"
													aria-valuemax="100"></div>
											</div>
										</div>
										<div class="col-lg-2 col-3">
											<small><strong>4 stars</strong></small>
										</div>
									</div>
									<!-- /row -->
									<div class="row">
										<div class="col-lg-10 col-9">
											<div class="progress">
												<div class="progress-bar" role="progressbar"
													style="width: 60%" aria-valuenow="60" aria-valuemin="0"
													aria-valuemax="100"></div>
											</div>
										</div>
										<div class="col-lg-2 col-3">
											<small><strong>3 stars</strong></small>
										</div>
									</div>
									<!-- /row -->
									<div class="row">
										<div class="col-lg-10 col-9">
											<div class="progress">
												<div class="progress-bar" role="progressbar"
													style="width: 20%" aria-valuenow="20" aria-valuemin="0"
													aria-valuemax="100"></div>
											</div>
										</div>
										<div class="col-lg-2 col-3">
											<small><strong>2 stars</strong></small>
										</div>
									</div>
									<!-- /row -->
									<div class="row">
										<div class="col-lg-10 col-9">
											<div class="progress">
												<div class="progress-bar" role="progressbar"
													style="width: 0" aria-valuenow="0" aria-valuemin="0"
													aria-valuemax="100"></div>
											</div>
										</div>
										<div class="col-lg-2 col-3">
											<small><strong>1 stars</strong></small>
										</div>
									</div>
									<!-- /row -->
								</div>
							</div>
							<!-- /row -->
						</div>

						<hr>

						<div class="reviews-container">

							<div class="review-box clearfix">
								<figure class="rev-thumb">
									<img src="img/avatar1.jpg" alt="">
								</figure>
								<div class="rev-content">
									<div class="rating">
										<i class="icon_star voted"></i><i class="icon_star voted"></i><i
											class="icon_star voted"></i><i class="icon_star voted"></i><i
											class="icon_star"></i>
									</div>
									<div class="rev-info">Moch. Hatta – 03 Sep 2018:</div>
									<div class="rev-text">
										<p>keren mas, saya baru menginjak ke spring, dan mencoba
											CI/CD ke heroku pakai gitlab</p>
									</div>
								</div>
							</div>
							<!-- /review-box -->
							<div class="review-box clearfix">
								<figure class="rev-thumb">
									<img src="img/avatar2.jpg" alt="">
								</figure>
								<div class="rev-content">
									<div class="rating">
										<i class="icon-star voted"></i><i class="icon_star voted"></i><i
											class="icon_star voted"></i><i class="icon_star voted"></i><i
											class="icon_star"></i>
									</div>
									<div class="rev-info">Den Baguse – 25 Agu 2018:</div>
									<div class="rev-text">
										<p>
											Wah makasih min,lengkap bener<br>Saya setia
											menunggu,kalo bisa sih dipercepat tapi gk pp deh gini aja<br>Yg
											penting semangat min dan keep on the level
										</p>
									</div>
								</div>
							</div>
							<!-- /review-box -->
							<div class="review-box clearfix">
								<figure class="rev-thumb">
									<img src="img/avatar3.jpg" alt="">
								</figure>
								<div class="rev-content">
									<div class="rating">
										<i class="icon-star voted"></i><i class="icon_star voted"></i><i
											class="icon_star voted"></i><i class="icon_star voted"></i><i
											class="icon_star"></i>
									</div>
									<div class="rev-info">Reza Rahman – 10 Agu 2018:</div>
									<div class="rev-text">
										<p>mantep, gaya penyampaiannya mudah dipahami. berbeda
											ketika membaca ebook malah pusing sendiri :D</p>
									</div>
								</div>
							</div>
							<!-- /review-box -->
						</div>
						<!-- /review-container -->
					</section>
					<!-- /section -->
				 --%>
				</div>
				<!-- /col -->

				<%
				if (!peserta) {
				%>
				<aside class="col-lg-4" id="sidebar">
					<div class="box_detail">
						<figure>
							<a href="video/intro.mp4" class="video"><i
								class="arrow_triangle-right"></i><img src="img/bg_courses.jpg"
								alt="" class="img-fluid"><span>Pratinjau Kursus</span></a>
						</figure>
						<div class="price">
							Rp. 150.000
							<p>
								<span class="original_price"><em>Rp. 1.500.000</em>90%
									discoun</span>
							</p>
						</div>
						<a
							href="<%=request.getContextPath()%>/kursus/cart1.jsp?id=<%=produkKursus.getId()%>"
							class="btn_1 full-width">Daftar</a> <a href="#0"
							class="btn_1 full-width outline"><i class="icon_heart"></i>
							Daftar Keinginan</a>
						<div id="list_feat">
							<h3>Apa yang didapat</h3>
							<ul>
								<li><i class="icon_mobile"></i>Mobile support</li>
								<li><i class="icon_archive_alt"></i>Arsip Pelajaran</li>
								<li><i class="icon_mobile"></i>Diskusi</li>
								<li><i class="icon_chat_alt"></i>Diskusi Materi</li>
								<li><i class="icon_document_alt"></i>Materi Contoh</li>
							</ul>
						</div>
					</div>
				</aside>
				<%
				}
				%>
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