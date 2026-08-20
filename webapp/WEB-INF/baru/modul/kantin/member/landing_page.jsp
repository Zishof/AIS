


<jsp:include page="/WEB-INF/baru/include/header.jsp"></jsp:include>

<body>

	<!-- ===============================================-->
	<!--    Main Content-->
	<!-- ===============================================-->
	<main class="main" id="top">
		<div class="container-fluid">
			

			<div class="content">


				<%
				try {
					%>
					<jsp:include page="/WEB-INF/baru/include/navbar.jsp"></jsp:include>
					<%
				} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/landing_page.jsp:24");
				}
				%>
				<jsp:include page="/WEB-INF/baru/modul/kantin/beranda.jsp"></jsp:include>
				<jsp:include page="/WEB-INF/baru/include/footer.jsp"></jsp:include>

			</div>

			<jsp:include page="/WEB-INF/baru/include/dialog-modal.jsp"></jsp:include>

		</div>
	</main>
	<!-- ===============================================-->
	<!--    End of Main Content-->
	<!-- ===============================================-->


	<jsp:include page="/WEB-INF/baru/include/foot.jsp"></jsp:include>

</body>

</html>