<%@page import="de.undercouch.underline.Command"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.kursus.BeritaKursus"%>
<%@page import="java.util.Map"%>
<%@page import="ais.database.model.file.LampiranLain"%>

<div class="bg_color_1">
	<div class="container margin_120_95">
		<div class="main_title_2">
			<span><em></em></span>
			<h2>Blog and Events</h2>
			<p>Berita dan informasi event.</p>
		</div>
		<div class="row">

			<%
			Map<Long, BeritaKursus> beritaKursuses = ais.common.ConstantValues.ambilBerdasarClass(BeritaKursus.class);
			List<BeritaKursus> beritaKursuss = new ArrayList<BeritaKursus>();
			for (BeritaKursus beritaKursus : beritaKursuses.values()) {
				if (beritaKursus.getAktif()) {
					beritaKursuss.add(beritaKursus);
				}
			}
			Collections.sort(beritaKursuss);

			for (BeritaKursus beritaKursus : beritaKursuss) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(beritaKursus.getWaktuPublikasi());

				LampiranLain d = LampiranLain.ambil(beritaKursus.getId(), "Gambar Berita Produk Kursus");
				String img = "";
				if (d != null) {
					img = d.createLinkUri();
				}
			%>



			<div class="col-lg-6">
				<a class="box_news" href="<%=request.getContextPath() %>/kursus/detail_berita.jsp?id=<%=beritaKursus.getId()%>">
					<figure>
						<img src="<%=img%>" alt="">
						<figcaption>
							<strong><%=calendar.get(Calendar.DATE)%></strong><%=ais.common.Common.monthFormat21.format(calendar.getTime())%>
						</figcaption>
					</figure>
					<ul>
						<li><%=beritaKursus.getPublikasiOleh()%></li>
						<li><%=ais.common.Common.dateFormat1.get().format(calendar.getTime())%></li>
					</ul>
					<h4><%=beritaKursus.getNama()%></h4>
					<p><%=beritaKursus.getKeterangan()%></p>
				</a>
			</div>
			<!-- /box_news -->
			<%
			}
			beritaKursuss = null;
			%>
		</div>
		<!-- /row -->
		<p class="btn_home_align">
			<a href="<%=request.getContextPath() %>/kursus/berita.jsp" class="btn_1 rounded">Semua Berita dan Event</a>
		</p>
	</div>
	<!-- /container -->
</div>
<!-- /bg_color_1 -->