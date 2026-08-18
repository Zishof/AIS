<%@page import="ais.database.model.bni.BniRequest"%>
<%@page import="ais.common.BniCommon"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="java.util.List"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.model.kursus.PesertaPunyaProdukKursus"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.kursus.ProdukKursus"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
	response.sendRedirect(request.getContextPath() + "/kursus/login.jsp");
	return;
}
Session sessionD = HibernateUtil.currentNativeSession();

Double total = (Double) (session.getAttribute("total") == null ? 0.0 : session.getAttribute("total"));

List<PesertaPunyaProdukKursus> punyaProdukKursus = (List<PesertaPunyaProdukKursus>) (session
		.getAttribute("punyaProdukKursus") == null ? null : session.getAttribute("punyaProdukKursus"));
BniRequest bniRequest = null;
if (total > 0.1) {
	bniRequest = BniCommon.onSaveBni(punyaProdukKursus, total, false);
}
%>

<main>
	<section id="hero_in" class="cart_section">
		<div class="wrapper">
			<div class="container">
				<div class="bs-wizard clearfix">
					<div class="bs-wizard-step">
						<div class="text-center bs-wizard-stepnum">Produk Pilihan</div>
						<div class="progress">
							<div class="progress-bar"></div>
						</div>
						<a href="cart-1.html" class="bs-wizard-dot"></a>
					</div>

					<div class="bs-wizard-step">
						<div class="text-center bs-wizard-stepnum">Pembayaran</div>
						<div class="progress">
							<div class="progress-bar"></div>
						</div>
						<a href="cart-2.html" class="bs-wizard-dot"></a>
					</div>

					<div class="bs-wizard-step active">
						<div class="text-center bs-wizard-stepnum">Selesai</div>
						<div class="progress">
							<div class="progress-bar"></div>
						</div>
						<a href="#0" class="bs-wizard-dot"></a>
					</div>
				</div>
				<!-- End bs-wizard -->
				<div id="confirm">
					<h4>Pemesanan Anda Sudah Selesai</h4>
					<p>
						Nomor VA :
						<%=(bniRequest == null ? "" : bniRequest.getVa())%></p>
					<p>
						Nilai Tagihan : Rp.
						<%=Common.numberFormat.get().format(total)%></p>
					<p>
						Waktu Kadaluwarsa :
						<%=(bniRequest == null || bniRequest.getBillExpired() == null ? ""
		: Common.dateFormat3.get().format(bniRequest.getBillExpired()))%></p>
				</div>
			</div>
		</div>
	</section>
	<!--/hero_in-->
</main>
<!--/main-->

<%
sessionD.disconnect();
sessionD.close();
HibernateUtil.closeSession();
%>