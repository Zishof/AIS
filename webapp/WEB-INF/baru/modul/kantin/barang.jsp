<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%
String rnd = Common.getGeneratedBarCode(7);
// Tab "Grup Produk (Harga Terpusat)": fail-closed mengikuti kunci grup_produk
// (EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF berisi kunci ini) -- gate UI saja;
// permission sesungguhnya tetap dicek server-side di GrupProdukApiHelper.
Tbmuser tbmuserGp = Common.getCurrentUser(request);
boolean bolehGrupProduk = Common.getApakahAdminLain(tbmuserGp);
if (!bolehGrupProduk && tbmuserGp != null) {
	Tbmrole roleGp = tbmuserGp.hakAkses();
	if (roleGp == null) {
		bolehGrupProduk = true;
	} else {
		org.json.JSONObject menuGp = ais.common.EbisnisMenuKatalog.urai(roleGp.getEbisnisMenu()).optJSONObject("menu");
		bolehGrupProduk = menuGp != null && menuGp.optBoolean("grup_produk", false);
	}
}
%>
<h5 class="fw-bold mb-3"><%=Common.getBahasaConfig("Manajemen Barang")%></h5>
<ul class="nav nav-tabs mb-3" id="subTabBarang<%=rnd%>" role="tablist">
	<li class="nav-item" role="presentation">
		<button class="nav-link active" data-bs-toggle="tab"
			data-bs-target="#sub-data-barang<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Katalog Barang")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-jenis-barang<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Jenis / Kategori Barang")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-pengajuan-harga<%=rnd%>" type="button" role="tab"><%=Common.getBahasaConfig("Pengajuan Perubahan Harga")%></button>
	</li>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-pricetag<%=rnd%>" type="button" role="tab"><i class="fas fa-tags me-1"></i><%=Common.getBahasaConfig("Cetak Price Tag")%></button>
	</li>
	<% if (bolehGrupProduk) { %>
	<li class="nav-item" role="presentation">
		<button class="nav-link" data-bs-toggle="tab"
			data-bs-target="#sub-grup-produk<%=rnd%>" type="button" role="tab"><i class="fas fa-layer-group me-1"></i><%=Common.getBahasaConfig("Grup Produk (Harga Terpusat)")%></button>
	</li>
	<% } %>
</ul>
<div class="tab-content">
	<div class="tab-pane fade show active" id="sub-data-barang<%=rnd%>"
		role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/barang/index.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-jenis-barang<%=rnd%>"
		role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/barang/jenis_produk.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-pengajuan-harga<%=rnd%>"
		role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/barang/pengajuan_perubahan_harga_produk.jsp" />
	</div>
	<div class="tab-pane fade" id="sub-pricetag<%=rnd%>" role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/barang/pricetag.jsp" />
	</div>
	<% if (bolehGrupProduk) { %>
	<div class="tab-pane fade" id="sub-grup-produk<%=rnd%>" role="tabpanel">
		<jsp:include page="/WEB-INF/baru/modul/kantin/barang/grup_produk.jsp" />
	</div>
	<% } %>
</div>