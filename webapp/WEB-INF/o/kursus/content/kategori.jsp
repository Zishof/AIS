<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.kursus.KategoriProdukKursus"%>
<%@page import="java.util.Map"%>
<div class="container margin_60_35">
	<div class="main_title_2">
		<span><em></em></span>
		<h2>Kategori</h2>
		<p>Temukan Kurus anda Bersama Kami</p>
	</div>
	<div class="row">

		<%
		Map<Long, KategoriProdukKursus> kategoriProdukKursuses = ais.common.ConstantValues
				.ambilBerdasarClass(KategoriProdukKursus.class);
		List<KategoriProdukKursus> kategoriProdukKursuss = new ArrayList<KategoriProdukKursus>();
		for (KategoriProdukKursus kategoriProdukKursus : kategoriProdukKursuses.values()) {
			if (kategoriProdukKursus.getAktif()) {
				kategoriProdukKursuss.add(kategoriProdukKursus);
			}
		}
		Collections.sort(kategoriProdukKursuss);

		for (KategoriProdukKursus kategoriProdukKursus : kategoriProdukKursuss) {
		%>

		<div class="col-lg-3 col-md-4">
			<a class="box_topic" href="<%=request.getContextPath()%>/kursus/list.jsp?kategoriProdukKursus=<%=kategoriProdukKursus.getId()%>"> <span><i
					class="<%=kategoriProdukKursus.getIcon()%>"></i></span>
				<h3><%=kategoriProdukKursus.getNama()%></h3>
				<p><%=kategoriProdukKursus.getKeterangan()%></p>
			</a>
		</div>
		<%
		}
		kategoriProdukKursuss = null;
		%>
	</div>
	<!--/row-->
</div>