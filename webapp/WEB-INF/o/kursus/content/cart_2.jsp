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
%>


<main style="transform: none;">
	<section id="hero_in" class="cart_section start_bg_zoom">
		<div class="wrapper">
			<div class="container">
				<div class="bs-wizard clearfix">
					<div class="bs-wizard-step active">
						<div class="text-center bs-wizard-stepnum">Your cart</div>
						<div class="progress">
							<div class="progress-bar"></div>
						</div>
						<a href="#0" class="bs-wizard-dot"></a>
					</div>

					<div class="bs-wizard-step active">
						<div class="text-center bs-wizard-stepnum">Payment</div>
						<div class="progress">
							<div class="progress-bar"></div>
						</div>
						<a href="#0" class="bs-wizard-dot"></a>
					</div>

					<div class="bs-wizard-step disabled">
						<div class="text-center bs-wizard-stepnum">Finish!</div>
						<div class="progress">
							<div class="progress-bar"></div>
						</div>
						<a href="#0" class="bs-wizard-dot"></a>
					</div>
				</div>
				<!-- End bs-wizard -->
			</div>
		</div>
	</section>
	<!--/hero_in-->

	<div class="bg_color_1" style="transform: none;">
		<div class="container margin_60_35" style="transform: none;">
			<div class="row" style="transform: none;">
				<div class="col-lg-8">
					<div class="box_cart">
						<div class="form_title">
							<h3>
								<strong>1</strong>Pilih Cara Pembayaran
							</h3>
							<p>
								<input id="pilihBank" type="radio" checked> <label
									for="pilihBank">Bayar Via BNI / Bank Transfer ke BNI</label>
							</p>
						</div>
					</div>
				</div>
				<!-- /col -->

				<aside class="col-lg-4" id="sidebar"
					style="position: relative; overflow: visible; box-sizing: border-box; min-height: 452.562px;">

					<div class="theiaStickySidebar"
						style="padding-top: 0px; padding-bottom: 1px; position: fixed; transform: translateY(150px); top: 0px; left: 1183.5px; width: 416px;">
						<div class="box_detail">
							<div id="total_cart">
								Total <span class="float-right">Rp. <%=Common.numberFormat.get().format(total)%></span>
							</div>
							<%--
							<div class="add_bottom_30">
								Lorem ipsum dolor sit amet, sed vide <strong>moderatius</strong>
								ad. Ex eius soleat sanctus pro, enim conceptam in quo, <a
									href="#0">brute convenire</a> appellantur an mei.
							</div>
							--%>
							<a href="<%=request.getContextPath()%>/kursus/cart3.jsp" class="btn_1 full-width">Bayar Sekarang</a> <a
								href="<%=request.getContextPath()%>/kursus/list.jsp"
								class="btn_1 full-width outline"><i class="icon-right"></i>
								Belanja Lagi</a>
						</div>
						<div class="resize-sensor"
							style="position: absolute; inset: 0px; overflow: hidden; z-index: -1; visibility: hidden;">
							<div class="resize-sensor-expand"
								style="position: absolute; left: 0; top: 0; right: 0; bottom: 0; overflow: hidden; z-index: -1; visibility: hidden;">
								<div
									style="position: absolute; left: 0px; top: 0px; transition: all 0s ease 0s; width: 426px; height: 323px;"></div>
							</div>
							<div class="resize-sensor-shrink"
								style="position: absolute; left: 0; top: 0; right: 0; bottom: 0; overflow: hidden; z-index: -1; visibility: hidden;">
								<div
									style="position: absolute; left: 0; top: 0; transition: 0s; width: 200%; height: 200%"></div>
							</div>
						</div>
					</div>
				</aside>
			</div>
			<!-- /row -->
		</div>
		<!-- /container -->
	</div>
	<!-- /bg_color_1 -->
</main>


<%
sessionD.disconnect();
sessionD.close();
HibernateUtil.closeSession();
%>