<%@page import="java.util.Calendar"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.kursus.ProdukKursus"%>
<%@page import="java.util.Map"%>
<%@page import="ais.database.model.file.LampiranLain"%>


<div class="container-fluid margin_120_0">
	<div class="main_title_2">
		<span><em></em></span>
		<h2>Kursus Terbaru</h2>
		<p>Pembelajaran biaya yang murah dengan pembahasan yang tepat.</p>
	</div>
	<div id="reccomended" class="owl-carousel owl-theme">


		<%
		List<ProdukKursus> produkKursuss = new ArrayList<ProdukKursus>();
		try {
			Map<Long, ProdukKursus> produkKursuses = ais.common.ConstantValues.ambilBerdasarClass(ProdukKursus.class);

			for (ProdukKursus produkKursus : produkKursuses.values()) {
				if (produkKursus.getAktif()) {
			produkKursuss.add(produkKursus);
				}
			}
			Collections.sort(produkKursuss, Collections.reverseOrder());

			for (ProdukKursus produkKursus : produkKursuss) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(produkKursus.getMulai());

				LampiranLain d = LampiranLain.ambil(produkKursus.getId(), "Gambar Produk Kursus");
				String img = "";
				if (d != null) {
			img = d.createLinkUri();
				}
		%>

		<div class="item">
			<div class="box_grid">
				<figure>
					<a href="#0" class="wish_bt"></a>
					<a href="<%=request.getContextPath() %>/kursus/detail.jsp?id=<%=produkKursus.getId()%>">
						<div class="preview">
							<span>Detail</span>
						</div> <img src="<%=img%>" class="img-fluid" alt="">
					</a>
					<div class="price">
						Rp.
						<%=ais.common.Common.numberFormat.get().format(produkKursus.getHargaTotal())%></div>
				</figure>
				<div class="wrapper">
					<small><%=(produkKursus.getKategoriProdukKursus() == null ? "" : produkKursus.getKategoriProdukKursus().getNama())%></small>
					<h3><%=produkKursus.getNama()%></h3>
					<p><%=produkKursus.getKeterangan()%></p>
					<div class="">
						<i class=" icon-users">&nbsp;&nbsp;Peserta</i> <small>(3445)</small>
					</div>
				</div>
				<ul>
					<li><i class="icon_clock_alt"></i> 1h 30min</li>
					<li><i class="icon_like"></i> 890</li>
					<li><a href="<%=request.getContextPath()%>/kursus/cart1.jsp?id=<%=produkKursus.getId()%>">Daftar</a></li>
				</ul>
			</div>
		</div>
		<!-- /item -->
		<%
		}
		produkKursuss = null;

		} catch (Exception e) {
		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/kursus/content/populer.jsp:76");
		}
		%>
	</div>
	<!-- /carousel -->
	<div class="container">
		<p class="btn_home_align">
			<a href="<%=request.getContextPath() %>/kursus/list.jsp" class="btn_1 rounded">Lihat Semua</a>
		</p>
	</div>
	<!-- /container -->
	<hr>
</div>
<!-- /container -->