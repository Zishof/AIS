<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Collections"%>
<%@page import="ais.database.model.kursus.KategoriProdukKursus"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.PerguruanTinggi"%>
<%
PerguruanTinggi perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi(request);
Tbmuser tbmuser = Common.getCurrentUser(request);
%>
<header class="header menu_2">
	<div class="container">
		<div id="preloader">
			<div data-loader="circle-side"></div>
		</div>
		<!-- /Preload -->
		<div id="logo">
			<a href="index.jsp"><img
				src="<%=request.getContextPath()%>/img/logo.png" width="auto"
				height="42" data-retina="true" alt=""></a>
			<span class="pl-2 text-dark"><b><%=perguruanTinggi.getNama()%></b></span>
		</div>

		<ul id="top_menu">

			<%
			if (tbmuser == null || tbmuser.getUserId() == null) {
			%>

			<li><a href="<%=request.getContextPath()%>/kursus/login.jsp"
				class="login">Login</a></li>

			<%
			}
			%>

			<li><a href="#0" class="search-overlay-menu-btn">Search</a></li>

			<%
			if (tbmuser == null || tbmuser.getUserId() == null) {
			%>
			<li class="hidden_tablet"><a
				href="<%=request.getContextPath()%>/kursus/register.jsp"
				class="btn_1 rounded">Daftar Sekarang</a></li>

			<%
			}
			%>

		</ul>
		<!-- /top_menu -->
		<a href="#menu" class="btn_mobile">
			<div class="hamburger hamburger--spin" id="hamburger">
				<div class="hamburger-box">
					<div class="hamburger-inner"></div>
				</div>
			</div>
		</a>
		<nav id="menu" class="main-menu">
			<ul>
				<li><span><a href="<%=request.getContextPath()%>/kursus/index.jsp">Beranda</a></span></li>
				<li><span><a href="#0">Kategori</a></span>
				
				
					<ul>

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
		
		<li><a href="<%=request.getContextPath()%>/kursus/list.jsp?kategoriProdukKursus=<%=kategoriProdukKursus.getId()%>"><%=kategoriProdukKursus.getNama() %></a></li>
		<%
		}
		kategoriProdukKursuss = null;
		%>
					</ul></li>
				<li><span><a href="<%=request.getContextPath()%>/kursus/berita.jsp">Blog</a></span></li>
				<%
				if (tbmuser != null) {
					String url = CommonMedia.getUrlFotoPengguna(tbmuser);
				%>
				<li><span><a href="#"><img style="max-width: 128px"
							class="rounded-circle w-25 hidden_tablet" src="<%=url%>" alt="" /></a></span>
					<ul>
						<%
						if(tbmuser != null && tbmuser.getPesertaKursus() != null ){
						%>
						<li><a href="<%=request.getContextPath()%>/kursus/list.jsp?pesertaKursus=<%=tbmuser.getPesertaKursus().getId()%>">Kursus Saya</a></li>
						<%
						}
						%>
						<li><a href="<%=request.getContextPath()%>/logoff">Logout</a></li>
					</ul></li>

				<%
				}
				%>

			</ul>
		</nav>
		<!-- Search Menu -->
		<div class="search-overlay-menu">
			<span class="search-overlay-close"><span class="closebt"><i
					class="ti-close"></i></span></span>
			<form role="search" id="searchform" method="get">
				<input value="" name="q" type="search" placeholder="Search..." />
				<button type="submit">
					<i class="icon_search"></i>
				</button>
			</form>
		</div>
		<!-- End Search Menu -->
	</div>
</header>
<!-- /header -->