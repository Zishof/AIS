<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="ais.database.model.kursus.ProdukPeserta"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.kursus.ProdukKursus"%>
<%@page import="java.util.Map"%>
<%@page import="ais.database.model.file.LampiranLain"%>


<div class="container margin_60_35">
	<div class="row">
		<%
		Session mySession = HibernateUtil.currentNativeSession();
		Long kat = request.getParameter("kategoriProdukKursus") == null
				|| request.getParameter("kategoriProdukKursus").isEmpty() ? null
				: Long.parseLong(request.getParameter("kategoriProdukKursus"));
		
		List<Long> produkPesertas = null;
		if(request.getParameter("pesertaKursus") != null
				&& !request.getParameter("pesertaKursus").isEmpty()){
			
			produkPesertas = mySession.createCriteria(ProdukPeserta.class)
					.setProjection(Projections.groupProperty("produkKursus.id"))
					.add(Restrictions.eq("pesertaKursus.id", Long.parseLong(request.getParameter("pesertaKursus")) ))
					.list();
		}
		

		List<ProdukKursus> produkKursuss = new ArrayList<ProdukKursus>();
		try {
			Map<Long, ProdukKursus> produkKursuses = ais.common.ConstantValues.ambilBerdasarClass(ProdukKursus.class);

			for (ProdukKursus produkKursus : produkKursuses.values()) {
				if (produkKursus.getAktif() && (produkKursus.getKategoriProdukKursus() != null
				&& (kat == null || kat.equals(produkKursus.getKategoriProdukKursus().getId())))) {
					if(produkPesertas==null || produkPesertas.contains(produkKursus.getId())){
						produkKursuss.add(produkKursus);
					}
				}
			}
			Collections.sort(produkKursuss, Collections.reverseOrder());

			for (ProdukKursus produkKursus : produkKursuss) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(produkKursus.getMulai());
				
				int jmlPeserta = ((Number)mySession.createCriteria(ProdukPeserta.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("produkKursus", produkKursus))
						.uniqueResult()).intValue();

				LampiranLain d = LampiranLain.ambil(produkKursus.getId(), "Gambar Produk Kursus");
				String img = "";
				if (d != null) {
			img = d.createLinkUri();
				}
		%>

		<div class="col-xl-4 col-lg-6 col-md-6">
			<div class="box_grid wow">
				<figure class="block-reveal">
					<div class="block-horizzontal"></div>
					<a href="#0" class="wish_bt"></a>
					<a
						href="<%=request.getContextPath()%>/kursus/detail.jsp?id=<%=produkKursus.getId()%>"><img
						src="<%=img%>" class="img-fluid" alt=""></a>
					<div class="price">
						Rp.
						<%=ais.common.Common.numberFormat.get().format(produkKursus.getHargaTotal())%></div>
					<div class="preview">
						<span>Detail</span>
					</div>
				</figure>
				<div class="wrapper">
					<small><%=(produkKursus.getKategoriProdukKursus() == null ? "" : produkKursus.getKategoriProdukKursus().getNama())%></small>
					<h3><%=produkKursus.getNama()%></h3>
					<p><%=produkKursus.getKeterangan()%></p>
					<div class="">
						<i class=" icon-users">&nbsp;&nbsp;Peserta</i> <small>(<%=jmlPeserta %>)</small>
					</div>
				</div>
				<ul>
					<%
					
					if(produkPesertas != null && produkPesertas.contains(produkKursus.getId() )){
					
					%>
					<li><a
						href="<%=request.getContextPath()%>/kursus/detail.jsp?id=<%=produkKursus.getId()%>">Lihat</a></li>
					
					<%
					} else {
					%>
					<li><a
						href="<%=request.getContextPath()%>/kursus/cart1.jsp?id=<%=produkKursus.getId()%>">Daftar</a></li>
						
					<%
					}
					%>
						
				</ul>
			</div>
		</div>
		<!-- /box_grid -->
		<%
		}
		produkKursuss = null;

		} catch (Exception e) {
		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/o/kursus/content/produks.jsp:115");
		}
		
		// mySession.disconnect();
		if (mySession.isOpen()) {mySession.disconnect();mySession.close();}
		HibernateUtil.closeSession();
		%>
	</div>
	<!-- /row -->
	<p class="text-center">
		<a href="#0" class="btn_1 rounded add_top_30">Load more</a>
	</p>
</div>
<!-- /container -->